/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailrepository.file;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailrepository.lib.AbstractMailRepository;
import org.apache.james.repository.file.FilePersistentObjectRepository;
import org.apache.james.repository.file.FilePersistentStreamRepository;
import org.apache.mailet.Mail;

/**
 * <p>
 * Implementation of a MailRepository on a FileSystem.
 * </p>
 * <p>
 * Requires a configuration element in the .conf.xml file of the form:
 * 
 * <pre>
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/&gt;
 * </pre>
 * 
 * Requires a logger called MailRepository.
 * </p>
 */
public class FileMailRepository extends AbstractMailRepository {

    private FilePersistentStreamRepository streamRepository;
    private FilePersistentObjectRepository objectRepository;
    private String destination;
    private Set keys;
    private boolean fifo;
    private boolean cacheKeys; // experimental: for use with write mostly
                               // repositories such as spam and error
    private FileSystem fs;

    @Resource(name = "filesystem")
    public void setFileSystem(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    protected void doConfigure(HierarchicalConfiguration config) throws org.apache.commons.configuration.ConfigurationException {
        super.doConfigure(config);
        destination = config.getString("[@destinationURL]");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("FileMailRepository.destinationURL: " + destination);
        }
        fifo = config.getBoolean("[@FIFO]", false);
        cacheKeys = config.getBoolean("[@CACHEKEYS]", true);
        // ignore model
    }

    @PostConstruct
    public void init() throws Exception {
        try {
            DefaultConfigurationBuilder reposConfiguration = new DefaultConfigurationBuilder();

            reposConfiguration.addProperty("[@destinationURL]", destination);
            objectRepository = new FilePersistentObjectRepository();
            objectRepository.setLog(getLogger());
            objectRepository.setFileSystem(fs);
            objectRepository.configure(reposConfiguration);
            objectRepository.init();

            streamRepository = new FilePersistentStreamRepository();
            streamRepository.setLog(getLogger());
            streamRepository.setFileSystem(fs);
            streamRepository.configure(reposConfiguration);
            streamRepository.init();

            if (cacheKeys)
                keys = Collections.synchronizedSet(new HashSet());

            // Finds non-matching pairs and deletes the extra files
            HashSet streamKeys = new HashSet();
            for (Iterator i = streamRepository.list(); i.hasNext();) {
                streamKeys.add(i.next());
            }
            HashSet objectKeys = new HashSet();
            for (Iterator i = objectRepository.list(); i.hasNext();) {
                objectKeys.add(i.next());
            }

            Collection strandedStreams = (Collection) streamKeys.clone();
            strandedStreams.removeAll(objectKeys);
            for (Iterator i = strandedStreams.iterator(); i.hasNext();) {
                String key = (String) i.next();
                remove(key);
            }

            Collection strandedObjects = (Collection) objectKeys.clone();
            strandedObjects.removeAll(streamKeys);
            for (Iterator i = strandedObjects.iterator(); i.hasNext();) {
                String key = (String) i.next();
                remove(key);
            }

            if (keys != null) {
                // Next get a list from the object repository
                // and use that for the list of keys
                keys.clear();
                for (Iterator i = objectRepository.list(); i.hasNext();) {
                    keys.add(i.next());
                }
            }
            if (getLogger().isDebugEnabled()) {
                StringBuffer logBuffer = new StringBuffer(128).append(getClass().getName()).append(" created in ").append(destination);
                getLogger().debug(logBuffer.toString());
            }
        } catch (Exception e) {
            final String message = "Failed to retrieve Store component:" + e.getMessage();
            getLogger().error(message, e);
            throw e;
        }
    }

    /**
     * @see org.apache.james.mailrepository.lib.AbstractMailRepository#internalStore(Mail)
     */
    protected void internalStore(Mail mc) throws MessagingException, IOException {
        String key = mc.getName();
        if (keys != null && !keys.contains(key)) {
            keys.add(key);
        }
        boolean saveStream = true;
        boolean update = true;

        MimeMessage message = mc.getMessage();
        // if the message is a Copy on Write proxy we check the wrapped message
        // to optimize the behaviour in case of MimeMessageWrapper
        if (message instanceof MimeMessageCopyOnWriteProxy) {
            MimeMessageCopyOnWriteProxy messageCow = (MimeMessageCopyOnWriteProxy) message;
            message = messageCow.getWrappedMessage();
        }
        if (message instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper) message;
            if (DEEP_DEBUG) {
                System.out.println("Retrieving from: " + wrapper.getSourceId());
                StringBuffer debugBuffer = new StringBuffer(64).append("Saving to:       ").append(destination).append("/").append(mc.getName());
                System.out.println(debugBuffer.toString());
                System.out.println("Modified: " + wrapper.isModified());
            }
            StringBuffer destinationBuffer = new StringBuffer(128).append(destination).append("/").append(mc.getName());
            if (destinationBuffer.toString().equals(wrapper.getSourceId())) {
                if (!wrapper.isModified()) {
                    // We're trying to save to the same place, and it's not
                    // modified... we shouldn't save.
                    // More importantly, if we try to save, we will create a
                    // 0-byte file since we're
                    // retrying to retrieve from a file we'll be overwriting.
                    saveStream = false;
                }

                // its an update
                update = true;
            }
        }
        if (saveStream) {
            OutputStream out = null;
            try {
                if (update && message instanceof MimeMessageWrapper) {
                    // we need to force the loading of the message from the
                    // stream as we want to override the old message
                    ((MimeMessageWrapper) message).loadMessage();
                    out = streamRepository.put(key);

                    ((MimeMessageWrapper) message).writeTo(out, out, null, true);

                } else {
                    out = streamRepository.put(key);
                    mc.getMessage().writeTo(out);

                }

            } finally {
                if (out != null)
                    out.close();
            }
        }
        // Always save the header information
        objectRepository.put(key, mc);
    }

    /**
     * @see org.apache.james.mailrepository.api.MailRepository#retrieve(String)
     */
    public Mail retrieve(String key) throws MessagingException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Retrieving mail: " + key);
        }
        try {
            Mail mc = null;
            try {
                mc = (Mail) objectRepository.get(key);
            } catch (RuntimeException re) {
                StringBuffer exceptionBuffer = new StringBuffer(128);
                if (re.getCause() instanceof Error) {
                    exceptionBuffer.append("Error when retrieving mail, not deleting: ").append(re.toString());
                } else {
                    exceptionBuffer.append("Exception retrieving mail: ").append(re.toString()).append(", so we're deleting it.");
                    remove(key);
                }
                final String errorMessage = exceptionBuffer.toString();
                getLogger().warn(errorMessage);
                getLogger().debug(errorMessage, re);
                return null;
            }
            MimeMessageStreamRepositorySource source = new MimeMessageStreamRepositorySource(streamRepository, destination, key);
            mc.setMessage(new MimeMessageCopyOnWriteProxy(source));

            return mc;
        } catch (Exception me) {
            getLogger().error("Exception retrieving mail: " + me);
            throw new MessagingException("Exception while retrieving mail: " + me.getMessage(), me);
        }
    }

    /**
     * @see org.apache.james.mailrepository.lib.AbstractMailRepository#internalRemove(String)
     */
    protected void internalRemove(String key) throws MessagingException {
        if (keys != null)
            keys.remove(key);
        streamRepository.remove(key);
        objectRepository.remove(key);
    }

    /**
     * @see org.apache.james.mailrepository.api.MailRepository#list()
     */
    public Iterator list() {
        // Fix ConcurrentModificationException by cloning
        // the keyset before getting an iterator
        final ArrayList clone;
        if (keys != null)
            synchronized (keys) {
                clone = new ArrayList(keys);
            }
        else {
            clone = new ArrayList();
            for (Iterator i = objectRepository.list(); i.hasNext();) {
                clone.add(i.next());
            }
        }
        if (fifo)
            Collections.sort(clone); // Keys is a HashSet; impose FIFO for apps
                                     // that need it
        return clone.iterator();
    }
}

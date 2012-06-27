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
package org.apache.james.mailbox.maildir.mail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxExistsException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.maildir.locator.MaildirLocator;
import org.apache.james.mailbox.maildir.mail.model.MaildirMailbox;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;

public class MaildirMailboxMapper extends NonTransactionalMapper implements MailboxMapper<Integer> {

    /**
     * The {@link MaildirStore} the mailboxes reside in
     */
    private final MaildirStore maildirStore;

    /**
     * A request-scoped list of mailboxes in order to refer to them via id
     */
    private ArrayList<MaildirMailbox<Integer>> mailboxCache = new ArrayList<MaildirMailbox<Integer>>();

    private final MailboxSession session;

    public MaildirMailboxMapper(MaildirStore maildirStore, MailboxSession session) {
        this.maildirStore = maildirStore;
        this.session = session;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#delete(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    @Override
    public void delete(Mailbox<Integer> mailbox) throws MailboxException {

        MaildirLocator maildirLocator = maildirStore.getMaildirLocator();
        MailboxNameResolver mailboxNameResolver = session.getMailboxNameResolver();
        MailboxName mailboxName = mailbox.getMailboxName();
        MailboxOwner owner = mailboxNameResolver.getOwner(mailboxName);
        File maildirPath = maildirLocator.locate(mailboxNameResolver, mailboxName, owner);
        if (maildirPath.exists() && maildirPath.isDirectory()) {
            try {
                File home = maildirLocator.getInbox(owner);
                if (home.exists()) {
                    if (home.equals(maildirPath)) {
                        /*
                         * We must only delete cur, new, tmp and metadata for top
                         * INBOX mailbox.
                         */
                        FileUtils.deleteDirectory(new File(maildirPath, MaildirFolder.CUR));
                        FileUtils.deleteDirectory(new File(maildirPath, MaildirFolder.NEW));
                        FileUtils.deleteDirectory(new File(maildirPath, MaildirFolder.TMP));
                        File uidListFile = new File(maildirPath, MaildirFolder.UIDLIST_FILE);
                        uidListFile.delete();
                        File validityFile = new File(maildirPath, MaildirFolder.VALIDITY_FILE);
                        validityFile.delete();
                    } else {
                        // We simply delete all the folder for non INBOX mailboxes.
                        FileUtils.deleteDirectory(maildirPath);
                    }
                }
            } catch (IOException e) {
                throw new MailboxException("Unable to delete " + mailbox.getMailboxName(), e);
            }
        } else {
            throw new MailboxNotFoundException(mailbox.getMailboxName());
        }
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByPath(org.apache.james.mailbox.model.MailboxPath)
     */
    @Override
    public Mailbox<Integer> findMailboxByPath(MailboxName mailboxPath) throws MailboxException, MailboxNotFoundException {
        MaildirMailbox<Integer> mailbox = maildirStore.loadMailbox(session, mailboxPath);
        return cacheMailbox(mailbox);
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithPathLike(org.apache.james.mailbox.model.MailboxPath)
     */
    @Override
    public List<Mailbox<Integer>> findMailboxWithPathLike(MailboxName mailboxPath) throws MailboxException {

        MailboxNameResolver mailboxNameResolver = session.getMailboxNameResolver();
        MailboxOwner owner = mailboxNameResolver.getOwner(mailboxPath);

        final List<Mailbox<Integer>> mailboxes;
        final List<Mailbox<Integer>> result;
        if (owner != null) {
            /*
             * reduce the search space if we are able to tell the owner out of
             * the mailbox name
             */
            try {
                mailboxes = maildirStore.listMailboxes(mailboxNameResolver, owner);
            } catch (IOException e) {
                throw new MailboxException("Could not list mailboxes.", e);
            }
            result = new ArrayList<Mailbox<Integer>>(mailboxes.size());
        } else {
            try {
                mailboxes = maildirStore.listMailboxes(mailboxNameResolver);
            } catch (IOException e) {
                throw new MailboxException("Could not list mailboxes.", e);
            }
            result = new ArrayList<Mailbox<Integer>>(StoreMailboxManager.estimateMailboxCountPerUser());
        }

        String patternName = MailboxNameCodec.SEARCH_PATTERN_NAME_CODEC.encode(mailboxPath);
        Pattern pattern = Pattern.compile(patternName);

        for (Mailbox<Integer> mailbox : mailboxes) {
            String subjectName = MailboxNameCodec.SEARCH_SUBJECT_NAME_CODEC.encode(mailbox.getMailboxName());
            if (pattern.matcher(subjectName).matches()) {
                result.add(cacheMailbox((MaildirMailbox<Integer>)mailbox));
            }
        }
        return result;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(org.apache.james.mailbox.store.mail.model.Mailbox,
     *      char)
     */
    @Override
    public boolean hasChildren(Mailbox<Integer> mailbox) throws MailboxException, MailboxNotFoundException {
        List<Mailbox<Integer>> children = findMailboxWithPathLike(mailbox.getMailboxName().child(MailboxQuery.LOCALWILDCARD_STRING));
        return !children.isEmpty();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    @Override
    public void save(Mailbox<Integer> mailbox) throws MailboxException {
        if (!(mailbox instanceof MaildirMailbox<?>)) {
            throw new IllegalArgumentException("'"+ MaildirMailbox.class.getName() +"' expected; found: '"+ mailbox.getClass().getName() +"'.");
        }
        else {
            MaildirMailbox<Integer> maildirMailbox = (MaildirMailbox<Integer>) mailbox;
            MaildirMailbox<Integer> originalMailbox = getCachedMailbox(maildirMailbox.getMailboxId());
            if (originalMailbox == null) {
                /* it cannot be found and is thus new */ 
                MaildirFolder folder = maildirMailbox.getMaildirFolder();
                if (!folder.exists()) {
                    try {
                        folder.create();
                    } catch (IOException e) {
                        throw new MailboxException("Failed to create Mailbox " + mailbox, e);
                    }
                }
                try {
                    folder.setUidValidity(mailbox.getUidValidity());
                } catch (IOException ioe) {
                    throw new MailboxException("Failed to save Mailbox " + mailbox, ioe);
                }

            }
            else {
                MailboxName originalMailboxName = originalMailbox.getMailboxName();
                if (!originalMailboxName.equals(maildirMailbox.getMailboxName())) {
                    MaildirFolder folder = maildirMailbox.getMaildirFolder();
                    if (folder.exists()) {
                        throw new MailboxExistsException(mailbox.getMailboxName().toString());
                    }
                    
                    MaildirFolder originalFolder = originalMailbox.getMaildirFolder();
                    MailboxNameResolver mailboxNameResolver = session.getMailboxNameResolver();
                    MailboxOwner owner = mailboxNameResolver.getOwner(originalMailboxName);
                    MailboxName originalInbox = mailboxNameResolver.getInbox(owner);
                    if (originalMailboxName.equals(originalInbox)) {
                        /* renaming the INBOX means to move its contents to the new folder */
                        
                        File inboxFolder = originalFolder.getPath();
                        File newFolder = folder.getPath();
                        if (!newFolder.mkdirs()) {
                            throw new MailboxException("Could not create folder " + newFolder);
                        }
                        if (!originalFolder.getCurFolder().renameTo(folder.getCurFolder())) {
                            throw new MailboxException("Could not rename folder " + originalFolder.getCurFolder() + " to " + folder.getCurFolder());
                        }
                        if (!originalFolder.getNewFolder().renameTo(folder.getNewFolder())) {
                            throw new MailboxException("Could not rename folder " + originalFolder.getNewFolder() + " to " + folder.getNewFolder());
                        }
                        if (!originalFolder.getTmpFolder().renameTo(folder.getTmpFolder())) {
                            throw new MailboxException("Could not rename folder " + originalFolder.getTmpFolder() + " to " + folder.getTmpFolder());
                        }
                        File oldUidListFile = new File(inboxFolder, MaildirFolder.UIDLIST_FILE);
                        File newUidListFile = new File(newFolder, MaildirFolder.UIDLIST_FILE);
                        if (!oldUidListFile.renameTo(newUidListFile)) {
                            throw new MailboxException("Could not rename file " + oldUidListFile + " to " + newUidListFile);
                        }
                        File oldValidityFile = new File(inboxFolder, MaildirFolder.VALIDITY_FILE);
                        File newValidityFile = new File(newFolder, MaildirFolder.VALIDITY_FILE);
                        if (!oldValidityFile.renameTo(newValidityFile)) {
                            throw new MailboxException("Could not rename file " + oldValidityFile + " to " + newValidityFile);
                        }
                        // recreate the INBOX folders, uidvalidity and uidlist
                        // will
                        // automatically be recreated later
                        if (!originalFolder.getCurFolder().mkdir()) {
                            throw new MailboxException("Could not create folder " + originalFolder.getCurFolder());
                        }
                        if (!originalFolder.getNewFolder().mkdir()) {
                            throw new MailboxException("Could not create folder " + originalFolder.getNewFolder());
                        }
                        if (!originalFolder.getTmpFolder().mkdir()) {
                            throw new MailboxException("Could not create folder " + originalFolder.getTmpFolder());
                        }

                    } else if (!originalFolder.getPath().renameTo(folder.getPath())) {
                        throw new MailboxException("Failed to save Mailbox " + mailbox, new IOException("Could not rename folder " + originalFolder));
                    }

                }
            }
            
        }
        
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    @Override
    public List<Mailbox<Integer>> list() throws MailboxException {
        try {
            return new ArrayList<Mailbox<Integer>>(maildirStore.listMailboxes(session.getMailboxNameResolver()));
        } catch (IOException e) {
            throw new MailboxException("Could not list mailboxes.", e);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#endRequest()
     */
    @Override
    public void endRequest() {
        mailboxCache.clear();
    }

    /**
     * Stores a copy of a mailbox in a cache valid for one request. This is to
     * enable referring to renamed mailboxes via id.
     * 
     * @param mailbox
     *            The mailbox to cache
     * @return The id of the cached mailbox
     * @throws MailboxException 
     */
    private Mailbox<Integer> cacheMailbox(MaildirMailbox<Integer> mailbox) throws MailboxException {
        try {
            mailboxCache.add(new MaildirMailbox<Integer>(mailbox));
            int id = mailboxCache.size() - 1;
            ((SimpleMailbox<Integer>) mailbox).setMailboxId(id);
            return mailbox;
        } catch (IOException e) {
            throw new MailboxException("Could not cache a "+ mailbox.getClass().getName() + " "+ mailbox);
        }
    }

    /**
     * Retrieves a mailbox from the cache
     * 
     * @param mailboxId
     *            The id of the mailbox to retrieve
     * @return The mailbox
     * @throws MailboxNotFoundException
     *             If the mailboxId is not in the cache
     */
    private MaildirMailbox<Integer> getCachedMailbox(Integer mailboxId) throws MailboxNotFoundException {
        if (mailboxId == null || mailboxId.intValue() < 0 || mailboxId.intValue() >= mailboxCache.size()) {
            return null;
        }
        else {
            return mailboxCache.get(mailboxId);
        }
    }

    public MaildirStore getMaildirStore() {
        return maildirStore;
    }

}

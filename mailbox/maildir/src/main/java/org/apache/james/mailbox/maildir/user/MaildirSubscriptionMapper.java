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
package org.apache.james.mailbox.maildir.user;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.model.Subscription;
import org.apache.james.mailbox.store.user.model.impl.SimpleSubscription;

public class MaildirSubscriptionMapper extends NonTransactionalMapper implements SubscriptionMapper {

    private static final String SUBSCRIPTION_FILE = "subscriptions";
    private static final String SUBSCRIPTION_FILE_ENCODING = "utf-8";
    private static final char EOL = '\n';
    private final MaildirStore store;
    private final MailboxNameCodec mailboxNameCodec;
    private final MailboxSession session;

    public MaildirSubscriptionMapper(MaildirStore store, MailboxSession session) {
        this.store = store;
        this.session = session;
        this.mailboxNameCodec = MailboxNameCodec.SAFE_STORE_NAME_CODEC;
    }
    
    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#delete(org.apache.james.mailbox.store.user.model.Subscription)
     */
    @Override
    public void delete(Subscription subscription) throws SubscriptionException {
        String user = subscription.getUser();
        final MailboxOwner owner;
        if (user.equals(session.getUser().getUserName())) {
            owner = session.getOwner();
        }
        else {
            owner = session.getMailboxNameResolver().getOwner(user, false);
        }
        // TODO: we need some kind of file locking here
        Set<MailboxName> subscriptionNames = readSubscriptionsForUser(owner);
        boolean changed = subscriptionNames.remove(subscription.getMailbox());
        if (changed) {
            try {
                writeSubscriptions(store.getMaildirLocator().getInbox(session.getMailboxNameResolver().getOwner(subscription.getUser(), false)), subscriptionNames);
            } catch (IOException e) {
                throw new SubscriptionException(e);
            }
        }
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findSubscriptionsForUser(java.lang.String)
     */
    @Override
    public List<Subscription> findSubscriptionsForUser(MailboxOwner owner) throws SubscriptionException {
        Set<MailboxName> subscriptionNames = readSubscriptionsForUser(owner);
        ArrayList<Subscription> subscriptions = new ArrayList<Subscription>();
        for (MailboxName subscription : subscriptionNames) {
            subscriptions.add(new SimpleSubscription(owner.getName(), subscription));
        }
        return Collections.unmodifiableList(subscriptions);
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    @Override
    public Subscription findMailboxSubscriptionForUser(MailboxOwner owner, MailboxName mailbox) throws SubscriptionException {
        try {
            Set<MailboxName> subscriptionNames = readSubscriptions(store.getMaildirLocator().getInbox(owner));
            if (subscriptionNames.contains(mailbox)) {
                return new SimpleSubscription(owner.getName(), mailbox);
            }
        } catch (IOException e) {
            throw new SubscriptionException(e);
        }
        return null;
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#save(org.apache.james.mailbox.store.user.model.Subscription)
     */
    @Override
    public void save(Subscription subscription) throws SubscriptionException {
        // TODO: we need some kind of file locking here
        String user = subscription.getUser();
        final MailboxOwner owner;
        if (user.equals(session.getUser().getUserName())) {
            owner = session.getOwner();
        }
        else {
            owner = session.getMailboxNameResolver().getOwner(user, false);
        }
        Set<MailboxName> subscriptionNames = readSubscriptionsForUser(owner);
        boolean changed = subscriptionNames.add(subscription.getMailbox());
        if (changed) {
            try {
                writeSubscriptions(store.getMaildirLocator().getInbox(session.getMailboxNameResolver().getOwner(subscription.getUser(), false)), subscriptionNames);
            } catch (IOException e) {
                throw new SubscriptionException(e);
            }
        }
    }

    /**
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#endRequest()
     */
    @Override
    public void endRequest() {
        // nothing to do
    }
    
    
    /**
     * Read the subscriptions for a particular user
     * @param user The user to get the subscriptions for
     * @return A Set of names of subscribed mailboxes of the user
     * @throws SubscriptionException
     */
    private Set<MailboxName> readSubscriptionsForUser(MailboxOwner owner) throws SubscriptionException { 
        try {
            return readSubscriptions(store.getMaildirLocator().getInbox(owner));
        } catch (IOException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * Read the names of the mailboxes which are subscribed from the specified folder
     * @param mailboxFolder The folder which contains the subscription file
     * @return A Set of names of subscribed mailboxes
     * @throws IOException
     */
    private Set<MailboxName> readSubscriptions(File mailboxFolder) throws IOException {
        
        if (mailboxFolder.exists()) {
            File subscriptionFile = new File(mailboxFolder, SUBSCRIPTION_FILE);
            Set<MailboxName> subscriptions = new LinkedHashSet<MailboxName>(StoreMailboxManager.estimateMailboxCountPerUser());
            if (!subscriptionFile.exists()) {
                return subscriptions;
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(subscriptionFile)), SUBSCRIPTION_FILE_ENCODING));
                String line = null;
                while ((line = in.readLine()) != null) {
                    subscriptions.add(mailboxNameCodec.decode(line, true));
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
            return subscriptions;
        }
        else {
            return new HashSet<MailboxName>(4);
        }
    }
    
    /**
     * Write the set of mailbox names into the subscriptions file in the specified folder
     * @param mailboxFolder Folder which contains the subscriptions file
     * @param subscriptions Set of names of subscribed mailboxes
     * @throws IOException
     */
    private void writeSubscriptions(File mailboxFolder, final Set<MailboxName> subscriptions) throws IOException {
        List<MailboxName> sortedSubscriptions = new ArrayList<MailboxName>(subscriptions);
        Collections.sort(sortedSubscriptions);
        if (!mailboxFolder.exists()) {
            if (!mailboxFolder.mkdirs()) {
                throw new IOException("Could not create folder " + mailboxFolder);
            }
        }
        
        File subscriptionFile = new File(mailboxFolder, SUBSCRIPTION_FILE);
        if (!subscriptionFile.exists()) {
            if (!subscriptionFile.createNewFile()) {
                throw new IOException("Could not create file " + subscriptionFile);
            }
        }
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(subscriptionFile)), SUBSCRIPTION_FILE_ENCODING);
            for (MailboxName subscription : sortedSubscriptions) {
                out.write(mailboxNameCodec.encode(subscription));
                out.write(EOL);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

}

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
package org.apache.james.mailbox.inmemory.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;

public class InMemoryMailboxMapper implements MailboxMapper<Long> {
    
    private static final int INITIAL_SIZE = 128;
    private final Map<Long, Mailbox<Long>> mailboxesById;
    private final static AtomicLong IDS = new AtomicLong();

    public InMemoryMailboxMapper() {
        mailboxesById = new ConcurrentHashMap<Long, Mailbox<Long>>(INITIAL_SIZE);
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#delete(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    @Override
    public void delete(Mailbox<Long> mailbox) throws MailboxException {
        mailboxesById.remove(mailbox.getMailboxId());
    }

    public void deleteAll() throws MailboxException {
        mailboxesById.clear();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByPath(org.apache.james.mailbox.model.MailboxPath)
     */
    @Override
    public synchronized Mailbox<Long> findMailboxByPath(MailboxName path) throws MailboxException, MailboxNotFoundException {
        for (final Mailbox<Long> mailbox : mailboxesById.values()) {
            if (path.equals(mailbox.getMailboxName())) {
                return mailbox;
            }
        }
        throw new MailboxNotFoundException(path);
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithPathLike(org.apache.james.mailbox.model.MailboxPath)
     */
    @Override
    public List<Mailbox<Long>> findMailboxWithPathLike(MailboxName path) throws MailboxException {
        MailboxQuery mailboxQuery = new MailboxQuery(path);
        List<Mailbox<Long>> results = new ArrayList<Mailbox<Long>>(StoreMailboxManager.estimateMailboxCountPerUser());
        for (final Mailbox<Long> mailbox : mailboxesById.values()) {
            if (mailboxQuery.isExpressionMatch(mailbox.getMailboxName())) {
                results.add(mailbox);
            }
        }
        return results;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    @Override
    public void save(Mailbox<Long> mailbox) throws MailboxException {
        Long id = mailbox.getMailboxId();
        if (id == null) {
            id = IDS.incrementAndGet();
            ((SimpleMailbox<Long>) mailbox).setMailboxId(id);
        }
        mailboxesById.put(id, mailbox);
    }

    /**
     * Do nothing
     */
    @Override
    public void endRequest() {
        // Do nothing
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(org.apache.james.mailbox.store.mail.model.Mailbox, char)
     */
    @Override
    public boolean hasChildren(Mailbox<Long> mailbox) throws MailboxException, MailboxNotFoundException {
        MailboxQuery mailboxQuery = new MailboxQuery(mailbox.getMailboxName().child(MailboxQuery.FREEWILDCARD_STRING));
        for (final Mailbox<Long> mbox : mailboxesById.values()) {
            if (mailboxQuery.isExpressionMatch(mbox.getMailboxName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    @Override
    public List<Mailbox<Long>> list() throws MailboxException {
        return new ArrayList<Mailbox<Long>>(mailboxesById.values());
    }

    @Override
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }
    
}

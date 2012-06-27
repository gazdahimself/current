/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.maildir;

import java.io.File;
import java.io.IOException;

import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.maildir.locator.MaildirLocator;
import org.apache.james.mailbox.maildir.mail.MaildirMailboxMapper;
import org.apache.james.mailbox.maildir.mail.model.MaildirMailbox;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * TODO MaildirMailboxManager.
 */
public class MaildirMailboxManager<Id> extends StoreMailboxManager<Id>{

    public MaildirMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, Authenticator authenticator, MailboxACLResolver aclResolver, GroupMembershipResolver groupMembershipResolver) {
        super(mailboxSessionMapperFactory, authenticator, aclResolver, groupMembershipResolver);
    }

    public MaildirMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, Authenticator authenticator, MailboxPathLocker locker, MailboxACLResolver aclResolver, GroupMembershipResolver groupMembershipResolver) {
        super(mailboxSessionMapperFactory, authenticator, locker, aclResolver, groupMembershipResolver);
    }

    @Override
    protected Mailbox<Id> doCreateMailbox(MailboxName mailboxPath, MailboxOwner owner, MailboxSession session) throws MailboxException {
        MaildirLocator maildirLocator = ((MaildirMailboxMapper)getMapperFactory().getMailboxMapper(session)).getMaildirStore().getMaildirLocator();
        File maildirPath = maildirLocator.locate(getMailboxNameResolver(), mailboxPath, owner);
        MaildirFolder maildirFolder = new MaildirFolder(maildirPath, mailboxPath, getLocker());
        try {
            if (!maildirFolder.exists()) {
                maildirFolder.create();
            }
            return new MaildirMailbox<Id>(mailboxPath, owner.getName(), owner.isGroup(), maildirFolder);
        } catch (IOException e) {
            throw new MailboxException("Could not instantiate a "+ MaildirMailbox.class.getName() +" for "+ mailboxPath, e);
        }
    }
    
    @Override
    public boolean mailboxExists(MailboxName mailboxPath, MailboxSession session) throws MailboxException {
        MailboxNameResolver mailboxNameResolver = getMailboxNameResolver();
        if (mailboxNameResolver.existsTrivially(mailboxPath)) {
            return true;
        }
        else {
            MaildirLocator maildirLocator = ((MaildirMailboxMapper)getMapperFactory().getMailboxMapper(session)).getMaildirStore().getMaildirLocator();
            MailboxOwner owner = mailboxNameResolver.getOwner(mailboxPath);
            File maildir = maildirLocator.locate(mailboxNameResolver, mailboxPath, owner);
            MaildirFolder maildirFolder = new MaildirFolder(maildir, mailboxPath, getLocker());
            return maildirFolder.exists();
        }
    }

}

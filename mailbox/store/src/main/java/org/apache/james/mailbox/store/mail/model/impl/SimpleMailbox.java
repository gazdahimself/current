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
package org.apache.james.mailbox.store.mail.model.impl;

import org.apache.james.mailbox.acl.MailboxACL;
import org.apache.james.mailbox.acl.SimpleMailboxACL;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.model.Mailbox;

public class SimpleMailbox<Id> implements Mailbox<Id> {

    private Id id = null;
    private String user;
    private MailboxName mailboxName;
    private long uidValidity;
    private MailboxACL acl = SimpleMailboxACL.EMPTY;
    private boolean ownerGroup;

    public SimpleMailbox(MailboxName mailboxName, String user, boolean ownerGroup, long uidValidity) {
        this.mailboxName = mailboxName;
        this.user = user;
        this.uidValidity = uidValidity;
        this.ownerGroup = ownerGroup;
    }
    
    public SimpleMailbox(Mailbox<Id> mailbox) {
        this.id = mailbox.getMailboxId();
        this.mailboxName = mailbox.getMailboxName();
        this.user = mailbox.getUser();
        this.ownerGroup = mailbox.isOwnerGroup();
        this.uidValidity = mailbox.getUidValidity();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getMailboxId()
     */
    @Override
    public Id getMailboxId() {
        return id;
    }

    @Override
    public MailboxName getMailboxName() {
        return mailboxName;
    }

    @Override
    public void setMailboxName(MailboxName mailboxName) {
        this.mailboxName = mailboxName;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getUser()
     */
    @Override
    public String getUser() {
        return user;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setUser(java.lang.String)
     */
    @Override
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getUidValidity()
     */
    @Override
    public long getUidValidity() {
        return uidValidity;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SimpleMailbox) {
            if (id != null) {
                if (id.equals(((SimpleMailbox<Id>) obj).getMailboxId()))
                    return true;
            } else {
                if (((SimpleMailbox<Id>) obj).getMailboxId() == null)
                    return true;
            }
        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + mailboxName.hashCode();
        result = PRIME * result + user.hashCode();
        return result;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return mailboxName + ":" + user;
    }


    public void setMailboxId(Id id) {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getACL()
     */
    @Override
    public MailboxACL getACL() {
        return acl;
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setACL(org.apache.james.mailbox.MailboxACL)
     */
    @Override
    public void setACL(MailboxACL acl) {
        this.acl = acl;
    }

    @Override
    public boolean isOwnerGroup() {
        return ownerGroup;
    }

    @Override
    public void setOwnerGroup(boolean ownerGroup) {
        this.ownerGroup = ownerGroup;
    }
    
}

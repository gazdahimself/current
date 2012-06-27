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
package org.apache.james.mailbox.hbase.mail.model;

import java.util.UUID;

import org.apache.james.mailbox.acl.MailboxACL;
import org.apache.james.mailbox.acl.SimpleMailboxACL;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * This class implements a mailbox. Most of the code is done after mailbox-jpa 
 * implementations.
 * 
 */
/**
 * TODO HBaseMailbox.
 */
public class HBaseMailbox implements Mailbox<UUID> {

    private static final String TAB = " ";
    /** The value for the mailboxId field */
    private UUID mailboxId;
    /** The value for the uidValidity field */
    private long uidValidity;
    private String user;
    private long lastUid;
    private long highestModSeq;
    private long messageCount;
    
    private MailboxName mailboxName;
    private boolean ownerGroup;

    public HBaseMailbox(MailboxName mailboxPath, String user, boolean ownerGroup, long uidValidity) {
        super();
        this.mailboxName = mailboxPath;
        this.user = user;
        this.ownerGroup = ownerGroup;
        this.uidValidity = uidValidity;
        //TODO: this has to change to something that can guarantee that mailboxId is unique
        this.mailboxId = UUID.randomUUID();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getMailboxId()
     */
    @Override
    public UUID getMailboxId() {
        return mailboxId;
    }

    public void setMailboxId(UUID mailboxId) {
        this.mailboxId = mailboxId;
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getMailboxName()
     */
    @Override
    public MailboxName getMailboxName() {
        return mailboxName;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setMailboxName(org.apache.james.mailbox.name.MailboxName)
     */
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
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#isOwnerGroup()
     */
    @Override
    public boolean isOwnerGroup() {
        return ownerGroup;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setOwnerGroup(boolean)
     */
    @Override
    public void setOwnerGroup(boolean ownerGroup) {
        this.ownerGroup = ownerGroup;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getUidValidity()
     */
    @Override
    public long getUidValidity() {
        return uidValidity;
    }

    @Override
    public String toString() {
        final String retValue = "Mailbox ( "
                + "mailboxId = " + this.mailboxId + TAB
//                + "namespace = " + this.namespace + TAB
                + "mailboxName = " + this.mailboxName + TAB
//                + "user = " + this.user + TAB
                + "uidValidity = " + this.uidValidity + TAB
                + " )";
        return retValue;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (mailboxId.getMostSignificantBits() ^ (mailboxId.getMostSignificantBits() >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HBaseMailbox other = (HBaseMailbox) obj;
        if (!mailboxId.equals(other.getMailboxId())) {
            return false;
        }
        return true;
    }

    public long getLastUid() {
        return lastUid;
    }

    public void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }

    public long getHighestModSeq() {
        return highestModSeq;
    }

    public void setHighestModSeq(long highestModSeq) {
        this.highestModSeq = highestModSeq;
    }

    public long consumeUid() {
        return ++lastUid;
    }

    public long consumeModSeq() {
        return ++highestModSeq;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getACL()
     */
    @Override
    public MailboxACL getACL() {
        // TODO ACL support
        return SimpleMailboxACL.OWNER_FULL_ACL;
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setACL(org.apache.james.mailbox.MailboxACL)
     */
    @Override
    public void setACL(MailboxACL acl) {
        // TODO ACL support
    }
    
}

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

package org.apache.james.mailbox.store;

import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.name.MailboxName;

public class SimpleMailboxMetaData implements MailboxMetaData {

    public static MailboxMetaData createNoSelect(MailboxName mailboxName, char delimiter) {
        return new SimpleMailboxMetaData(mailboxName, Children.CHILDREN_ALLOWED_BUT_UNKNOWN, Selectability.NOSELECT);
    }

    private final Children inferiors;

    private final Selectability selectability;
    
    private final MailboxName mailboxName;

    public SimpleMailboxMetaData(MailboxName mailboxName) {
        this(mailboxName, Children.CHILDREN_ALLOWED_BUT_UNKNOWN, Selectability.NONE);
    }

    public SimpleMailboxMetaData(final MailboxName mailboxName, final Children inferiors, final Selectability selectability) {
        super();
        if (mailboxName == null) {
            throw new IllegalArgumentException("Need a non-null "+ MailboxName.class.getName() +" to create a new "+ SimpleMailboxMetaData.class.getName() +".");
        }
        this.mailboxName = mailboxName;
        this.inferiors = inferiors;
        this.selectability = selectability;
    }

    /**
     * Is this mailbox <code>\Noinferiors</code> as per RFC3501.
     * 
     * @return true if marked, false otherwise
     */
    public final Children inferiors() {
        return inferiors;
    }

    /**
     * Gets the RFC3501 Selectability flag.
     */
    public final Selectability getSelectability() {
        return selectability;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "ListResult: " + mailboxName;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return mailboxName.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof MailboxMetaData) {
            MailboxMetaData other = (MailboxMetaData) o;
            return mailboxName.equals(other.getMailboxName());
        }
        else {
            return false;
        }
    }

//    /**
//     * @see java.lang.Comparable#compareTo(java.lang.Object)
//     */
//    public int compareTo(MailboxMetaData o) {
//        return StandardMailboxMetaDataComparator.order(this, o);
//    }

    /**
     * @see org.apache.james.mailbox.model.MailboxMetaData#getMailboxName()
     */
    public MailboxName getMailboxName() {
        return mailboxName;
    }

}

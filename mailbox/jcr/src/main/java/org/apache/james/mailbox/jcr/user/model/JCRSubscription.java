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

package org.apache.james.mailbox.jcr.user.model;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.james.mailbox.jcr.JCRImapConstants;
import org.apache.james.mailbox.jcr.Persistent;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.user.model.Subscription;
import org.slf4j.Logger;

/**
 * JCR implementation of a {@link Subscription}.
 */
public class JCRSubscription implements Subscription, Persistent, JCRImapConstants {
    private static final String TOSTRING_SEPARATOR = " ";

    private Node node;
    private final Logger log;
    private final MailboxName mailbox;
    private String username;
    private final MailboxNameCodec mailboxNameCodec;

    
    public JCRSubscription(Node node, MailboxName mailbox, MailboxNameCodec mailboxNameCodec, Logger log) {
        this.node = node;
        this.log = log;
        this.mailbox = mailbox;
        this.mailboxNameCodec = mailboxNameCodec;
    }

    public JCRSubscription(String username, MailboxName mailbox, MailboxNameCodec mailboxNameCodec, Logger log) {
        this.username = username;
        this.mailbox = mailbox;
        this.log = log;
        this.mailboxNameCodec = mailboxNameCodec;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.user.model.Subscription#getMailbox()
     */
    public MailboxName getMailbox() {
        return mailbox;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.user.model.Subscription#getUser()
     */
    public String getUser() {
        if (isPersistent()) {
            try {
                return node.getProperty(JCRImapConstants.SUBSCRIPTION_USER_PROP).getString();
            } catch (RepositoryException e) {
                log.error("Unable to access Property " + JCRImapConstants.SUBSCRIPTION_USER_PROP, e);
            }
            return null;
        }
        return username;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.jcr.NodeAware#getNode()
     */
    public Node getNode() {
        return node;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.jcr.IsPersistent#isPersistent()
     */
    public boolean isPersistent() {
        return node != null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.jcr.IsPersistent#merge(javax.jcr.Node)
     */
    public void merge(Node node) throws RepositoryException{
        node.setProperty(JCRImapConstants.SUBSCRIPTION_USER_PROP, getUser());
        String encodedMailboxName = mailboxNameCodec.encode(mailbox);
        if (node.hasProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP)) {
            Value[] mailboxes = node.getProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP).getValues();
            String[] newMailboxes = new String[mailboxes.length + 1];
            boolean found = false;
            int i = 0;
            for (; i< mailboxes.length; i++) {
                String m = mailboxes[i].getString();
                if (encodedMailboxName.equals(m)) {
                    found = true;
                    break;
                }
                newMailboxes[i] = m;
            }
            if (!found) {
                newMailboxes[i] = encodedMailboxName;
                node.setProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP, newMailboxes);
            }
        } else {
            node.setProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP, new String[] {encodedMailboxName});
        }
        this.node = node;
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getUser().hashCode();
        result = PRIME * result + getMailbox().hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final JCRSubscription other = (JCRSubscription) obj;
        if (getUser() != null) {
            if (!getUser().equals(other.getUser()))
        	return false;
        } else {
            if (other.getUser() != null)
        	return false;
        }
        if (getMailbox() != null) {
            if (!getMailbox().equals(other.getMailbox()))
        	return false;
        } else {
            if (other.getMailbox() != null)
        	return false;
        }
        return true;
    }

    /**
     * Renders output suitable for debugging.
     *
     * @return output suitable for debugging
     */
    public String toString() {
        final String result = "Subscription ( "
            + "user = " + this.getUser() + TOSTRING_SEPARATOR
            + "mailbox = " + this.getMailbox() + TOSTRING_SEPARATOR
            + " )";
    
        return result;
    }
    

}

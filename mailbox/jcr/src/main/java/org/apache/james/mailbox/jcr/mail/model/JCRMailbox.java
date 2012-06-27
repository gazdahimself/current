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
package org.apache.james.mailbox.jcr.mail.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.james.mailbox.acl.MailboxACL;
import org.apache.james.mailbox.acl.MailboxACLCodec;
import org.apache.james.mailbox.acl.SimpleMailboxACL;
import org.apache.james.mailbox.jcr.JCRImapConstants;
import org.apache.james.mailbox.jcr.Persistent;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.slf4j.Logger;


/**
 * JCR implementation of a {@link Mailbox}
 */
public class JCRMailbox implements Mailbox<String>, JCRImapConstants, Persistent{

    private static final String TAB = " ";

    
    public final static String ACL_PROPERTY = "jamesMailbox:mailboxAcl";
    public final static String USER_PROPERTY = "jamesMailbox:mailboxUser";
    public final static String IS_OWNER_GROUP_PROPERTY = "jamesMailbox:mailboxIsOwnerGroup";
    public final static String MAILBOX_NAME_PROPERTY = "jamesMailbox:mailboxName";
    public final static String UIDVALIDITY_PROPERTY = "jamesMailbox:mailboxUidValidity";
    public final static String LASTUID_PROPERTY = "jamesMailbox:mailboxLastUid";
    public final static String HIGHESTMODSEQ_PROPERTY = "jamesMailbox:mailboxHighestModSeq";

    private long uidValidity;
    private final Logger logger;
    private Node node;


    private String user;
    private long lastKnownUid;
    private long highestKnownModSeq;
    
    private MailboxName mailboxName;
    private boolean ownerGroup;
    
    private final MailboxNameCodec mailboxNameCodec;
    private final MailboxACLCodec mailboxACLCodec;
    
    private MailboxACL acl;
    
    public JCRMailbox( final MailboxName path, String user, boolean ownerGroup, final long uidValidity, Logger logger) {
        this.mailboxName = path;
        this.user = user;
        this.ownerGroup = ownerGroup;
        this.uidValidity = uidValidity;
        this.logger = logger;
        this.mailboxNameCodec = MailboxNameCodec.SAFE_STORE_NAME_CODEC;
        this.mailboxACLCodec = MailboxACLCodec.DEFAULT;
    }
    
    public JCRMailbox( final Node node, final Logger logger) {
        this.node = node;
        this.logger = logger;
        this.mailboxNameCodec = MailboxNameCodec.SAFE_STORE_NAME_CODEC;
        this.mailboxACLCodec = MailboxACLCodec.DEFAULT;
    }
    
    public Logger getLog() {
        return logger;
    }

    @Override
    public MailboxName getMailboxName() {
        if (isPersistent()) {
            try {
                String rawName = node.getProperty(MAILBOX_NAME_PROPERTY).getString();
                return mailboxNameCodec.decode(rawName, true);
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + MAILBOX_NAME_PROPERTY, e);
            }
        }
        return mailboxName;
    }

    @Override
    public void setMailboxName(MailboxName mailboxName) {
        if (isPersistent()) {
            try {
                String rawName = mailboxNameCodec.encode(mailboxName);
                node.setProperty(MAILBOX_NAME_PROPERTY, rawName);
                // move the node 
                // See https://issues.apache.org/jira/browse/IMAP-162
                node.getSession().move(node.getPath(), node.getParent().getPath() + NODE_DELIMITER + Text.escapePath(rawName));
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + MAILBOX_NAME_PROPERTY, e);
            }
        } else {
            this.mailboxName = mailboxName;
        }
    }


    @Override
    public boolean isOwnerGroup() {
        if (isPersistent()) {
            try {
                return node.getProperty(IS_OWNER_GROUP_PROPERTY).getBoolean();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + IS_OWNER_GROUP_PROPERTY, e);
            }
        }
        return this.ownerGroup;
    }

    @Override
    public void setOwnerGroup(boolean ownerGroup) {
        if (isPersistent()) {
            try {
                node.setProperty(IS_OWNER_GROUP_PROPERTY, ownerGroup);
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + IS_OWNER_GROUP_PROPERTY, e);
            }
        } else {
            this.ownerGroup = ownerGroup;
        }
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getUidValidity()
     */
    @Override
    public long getUidValidity() {
        if (isPersistent()) {
            try {
                return node.getProperty(UIDVALIDITY_PROPERTY).getLong();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + UIDVALIDITY_PROPERTY, e);
            }
        }
        return uidValidity;
    }

    /**
     * @see org.apache.james.mailbox.jcr.Persistent#getNode()
     */
    @Override
    public Node getNode() {
        return node;
    }

    /**
     * @see org.apache.james.mailbox.jcr.Persistent#isPersistent()
     */
    @Override
    public boolean isPersistent() {
        return node != null;
    }

    /**
     * @see org.apache.james.mailbox.jcr.Persistent#merge(javax.jcr.Node)
     */
    @Override
    public void  merge(Node node) throws RepositoryException {
        node.setProperty(MAILBOX_NAME_PROPERTY, mailboxNameCodec.encode(getMailboxName()));
        node.setProperty(UIDVALIDITY_PROPERTY, getUidValidity());
        String user = getUser();
        if (user == null) {
            user = "";
        }
        node.setProperty(USER_PROPERTY, user);
        node.setProperty(IS_OWNER_GROUP_PROPERTY, isOwnerGroup());
        node.setProperty(HIGHESTMODSEQ_PROPERTY, getHighestModSeq());
        node.setProperty(LASTUID_PROPERTY, getLastUid());
        node.setProperty(ACL_PROPERTY, mailboxACLCodec.encode(getACL()));
        this.node = node;
    }
    
    @Override
    public String toString() {
        final String retValue = "Mailbox ( "
            + "mailboxUID = " + this.getMailboxId() + TAB
            + "mailboxName = " + this.getMailboxName() + TAB
            + "uidValidity = " + this.getUidValidity() + TAB
            + " )";
        return retValue;
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) getMailboxId().hashCode();
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
        final JCRMailbox other = (JCRMailbox) obj;
        if (getMailboxId() != null) {
            if (!getMailboxId().equals(other.getMailboxId()))
        	return false;
        } else {
            if (other.getMailboxId() != null)
        	return false;
        }
        return true;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getMailboxId()
     */
    @Override
    public String getMailboxId() {
        if (isPersistent()) {
            try {
                return node.getIdentifier();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + JcrConstants.JCR_UUID, e);
            }
        }
        return null;      
    }


    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getUser()
     */
    public String getUser() {
        if (isPersistent()) {
            try {
                String user = node.getProperty(USER_PROPERTY).getString();
                if (user.trim().length() == 0) {
                    return null;
                } else {
                    return user;
                }
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + USER_PROPERTY, e);
            }
        }
        return user;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setUser(java.lang.String)
     */
    public void setUser(String user) {
        if (isPersistent()) {
            try {
                if (user == null) {
                    user = "";
                }
                node.setProperty(USER_PROPERTY, user);
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + USER_PROPERTY, e);
            }
        } else {
            this.user = user;
        }        
    }

    private long getLastUid() {
        if (isPersistent()) {
            try {
                return node.getProperty(LASTUID_PROPERTY).getLong();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + LASTUID_PROPERTY, e);
            }
        }
        return lastKnownUid;
    }

    private long getHighestModSeq() {
        if (isPersistent()) {
            try {
                return node.getProperty(HIGHESTMODSEQ_PROPERTY).getLong();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + HIGHESTMODSEQ_PROPERTY, e);
            }
        }
        return highestKnownModSeq;
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getACL()
     */
    @Override
    public MailboxACL getACL() {
        if (isPersistent()) {
            try {
                if (node.hasProperty(ACL_PROPERTY)) {
                    String serializedAcl = node.getProperty(ACL_PROPERTY).getString();
                    return mailboxACLCodec.decode(serializedAcl);
                }
                else {
                    return SimpleMailboxACL.EMPTY;
                }
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + MAILBOX_NAME_PROPERTY, e);
            }
        }
        return acl;
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#setACL(org.apache.james.mailbox.MailboxACL)
     */
    @Override
    public void setACL(MailboxACL acl) {
        if (isPersistent()) {
            try {
                String serializedAcl = mailboxACLCodec.encode(acl);
                node.setProperty(ACL_PROPERTY, serializedAcl);
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + ACL_PROPERTY, e);
            }
        } else {
            this.acl = acl;
        }                
    }
    
}

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
package org.apache.james.mailbox.jcr.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.jcr.AbstractJCRScalingMapper;
import org.apache.james.mailbox.jcr.JCRImapConstants;
import org.apache.james.mailbox.jcr.JCRXPathQueryBuilder;
import org.apache.james.mailbox.jcr.MailboxSessionJCRRepository;
import org.apache.james.mailbox.jcr.user.model.JCRSubscription;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager
 * 
 */
public class JCRSubscriptionMapper extends AbstractJCRScalingMapper implements SubscriptionMapper {

    public JCRSubscriptionMapper(final MailboxSessionJCRRepository repos, MailboxSession session, final int scaling) {
        super(repos, session, scaling);
    }

    /**
     * org.apache.james.mailbox.store.user.SubscriptionMapper#delete(org.apache
     * .james.imap.store.user.model.Subscription)
     */
    @Override
    public void delete(Subscription subscription) throws SubscriptionException {

        JCRSubscription sub = (JCRSubscription) subscription;
        try {

            Node node = sub.getNode();
            if (node != null) {
                String encodedMailbox = repository.getMailboxNameAttributeCodec().encode(sub.getMailbox());
                Property prop = node.getProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP);
                Value[] values = prop.getValues();
                List<String> newValues = new ArrayList<String>(values.length);
                for (int i = 0; i < values.length; i++) {
                    String m = values[i].getString();
                    if (!m.equals(encodedMailbox)) {
                        newValues.add(m);
                    }
                }
                if (!newValues.isEmpty()) {
                    prop.setValue(newValues.toArray(new String[newValues.size()]));
                } else {
                    prop.remove();
                }
            }
        } catch (PathNotFoundException e) {
            // do nothing
        } catch (RepositoryException e) {
            throw new SubscriptionException(e);
        }

    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findMailboxSubscriptionForUser(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public JCRSubscription findMailboxSubscriptionForUser(MailboxOwner owner, MailboxName mailbox) throws SubscriptionException {
        try {
            Node inboxNode = findInboxNode(owner);
            if (inboxNode != null && inboxNode.hasProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP)) {
                MailboxNameCodec mailboxNameCodec = repository.getMailboxNameAttributeCodec();
                String encodedMailboxName = mailboxNameCodec.encode(mailbox);
                Value[] values = inboxNode.getProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP).getValues();
                if (values != null) {
                    for (Value value : values) {
                        if (encodedMailboxName.equals(value.getString())) {
                            return new JCRSubscription(inboxNode, mailbox, mailboxNameCodec, getLogger());
                        }
                    }
                }
            }
            return null;
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new SubscriptionException(e);
        }
    }
    
    protected Node findSubscriptionNodeForUser(MailboxOwner owner, MailboxName mailbox) throws RepositoryException {
        
        
        MailboxNameCodec mailboxNameCodec = repository.getMailboxNameAttributeCodec();
        String encodedMailboxName = mailboxNameCodec.encode(mailbox);
        
        JCRXPathQueryBuilder pb = new JCRXPathQueryBuilder(128).mailboxes();
        appendInboxPath(pb, owner);
        
        pb.eq(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP, encodedMailboxName);

        Query query = pb.bind(getSession().getWorkspace());
        QueryResult result = query.execute();

        NodeIterator nodeIt = result.getNodes();
        if (nodeIt.hasNext()) {
            return nodeIt.nextNode();
        }
        else {
            return null;
        }
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findSubscriptionsForUser
     *      (java.lang.String)
     */
    @Override
    public List<Subscription> findSubscriptionsForUser(MailboxOwner owner) throws SubscriptionException {
        try {
            Node inboxNode = findInboxNode(owner);
            if (inboxNode != null && inboxNode.hasProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP)) {
                Value[] values = inboxNode.getProperty(JCRImapConstants.SUBSCRIPTION_MAILBOXES_PROP).getValues();
                if (values != null) {
                    MailboxNameCodec mailboxNameCodec = repository.getMailboxNameAttributeCodec();
                    List<Subscription> subscriptions = new ArrayList<Subscription>(values.length + 1);
                    for (Value value : values) {
                        MailboxName mailboxName = mailboxNameCodec.decode(value.getString(), true);
                        subscriptions.add(new JCRSubscription(inboxNode, mailboxName, mailboxNameCodec, getLogger()));
                    }
                    return subscriptions;
                }
            }
            return Collections.emptyList();
        } catch (PathNotFoundException e) {
            // Do nothing just return the empty list
            return Collections.emptyList();
        } catch (RepositoryException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#save(org.apache.james
     *      .imap.store.user.model.Subscription)
     */
    @Override
    public void save(Subscription subscription) throws SubscriptionException {
        try {
            String user = subscription.getUser();
            final MailboxOwner owner;
            if (user.equals(mSession.getUser().getUserName())) {
                owner = mSession.getOwner();
            }
            else {
                owner = mSession.getMailboxNameResolver().getOwner(user, false);
            }
            Node inboxNode = getOrAddInboxNode(owner);
            ((JCRSubscription) subscription).merge(inboxNode);
        } catch (RepositoryException e) {
            throw new SubscriptionException(e);
        }
    }

}

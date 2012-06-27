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
package org.apache.james.mailbox.jcr.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.jcr.AbstractJCRScalingMapper;
import org.apache.james.mailbox.jcr.JCRImapConstants;
import org.apache.james.mailbox.jcr.JCRXPathQueryBuilder;
import org.apache.james.mailbox.jcr.MailboxSessionJCRRepository;
import org.apache.james.mailbox.jcr.mail.model.JCRMailbox;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * JCR implementation of a MailboxMapper
 * 
 * 
 */
public class JCRMailboxMapper extends AbstractJCRScalingMapper implements MailboxMapper<String> {

    public JCRMailboxMapper(final MailboxSessionJCRRepository repos, MailboxSession session, final int scaling) {
        super(repos, session, scaling);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxMapper#delete(org.apache.james
     * .imap.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<String> mailbox) throws MailboxException {
        try {
            Node node = getSession().getNodeByIdentifier(((JCRMailbox) mailbox).getMailboxId());

            node.remove();

        } catch (PathNotFoundException e) {
            /* nothing to delete */
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to delete mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByPath(org
     * .apache.james.imap.api.MailboxPath)
     */
    public Mailbox<String> findMailboxByPath(MailboxName path) throws MailboxException, MailboxNotFoundException {

        try {
            Node node = findNodeByMailboxName(path);
            return new JCRMailbox(node, getLogger());
        } catch (PathNotFoundException e) {
            throw new MailboxNotFoundException(path);
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to find mailbox " + path, e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithPathLike
     * (org.apache.james.imap.api.MailboxPath)
     */
    public List<Mailbox<String>> findMailboxWithPathLike(MailboxName path) throws MailboxException {

        try {
            JCRXPathQueryBuilder pb = prepareMailboxQueryLike(path);

            Query query = pb.bind(getSession().getWorkspace());
            QueryResult result = query.execute();
            NodeIterator it = result.getNodes();
            long size = it.getSize();
            if (size < 0) {
                size = StoreMailboxManager.estimateMailboxCountPerUser();
            }
            List<Mailbox<String>> mailboxes = new ArrayList<Mailbox<String>>((int) size);
            MailboxNameCodec mailboxNameCodec = repository.getMailboxNameAttributeCodec();
            MailboxQuery mailboxQuery = new MailboxQuery(path);
            while (it.hasNext()) {
                Node mailboxNode = it.nextNode();
                MailboxName mailboxName = mailboxNameCodec.decode(mailboxNode.getProperty(JCRImapConstants.MAILBOX_NAME_PROP).getString(), true);
                if (mailboxQuery.isExpressionMatch(mailboxName)) {
                    mailboxes.add(new JCRMailbox(mailboxNode, getLogger()));
                }
            }
            return mailboxes;
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to find mailbox " + path, e);
        }

    }

    /**
     * TODO prepareMailboxQueryLike.
     * 
     * @param path
     * @return
     */
    private JCRXPathQueryBuilder prepareMailboxQueryLike(MailboxName path) {
        String encodedName = repository.getMailboxNamePatternCodec().encode(path);
        final JCRXPathQueryBuilder pb = new JCRXPathQueryBuilder(128)
        .jcrRoot()
        .mailboxes()
        .descendantOrSelf()
        .hasMixin(JCRImapConstants.MAILBOX_TYPE)
        .like(JCRImapConstants.MAILBOX_NAME_PROP, encodedName);
        return pb;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxMapper#save(org.apache.james.
     * imap.store.mail.model.Mailbox)
     */
    public void save(Mailbox<String> mailbox) throws MailboxException {

        try {
            final JCRMailbox jcrMailbox = (JCRMailbox) mailbox;

            Node mailboxNode = null;

            if (jcrMailbox.isPersistent()) {
                mailboxNode = getSession().getNodeByIdentifier(jcrMailbox.getMailboxId());
            }
            if (mailboxNode == null) {
                MailboxName mailboxName = mailbox.getMailboxName();
                MailboxOwner owner = mSession.getMailboxNameResolver().getOwner(mailboxName);
                Node inboxNode = getOrAddInboxNode(owner);

                String encodedName = repository.getMailboxNameAttributeCodec().encode(mailboxName);

                mailboxNode = JcrUtils.getOrAddNode(inboxNode, ISO9075.encodePath(encodedName), JcrConstants.NT_UNSTRUCTURED);
                mailboxNode.addMixin(JCRImapConstants.MAILBOX_TYPE);

            }
            jcrMailbox.merge(mailboxNode);

        } catch (RepositoryException e) {
            throw new MailboxException("Unable to save mailbox " + mailbox, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(org.apache
     * .james. imap.store.mail.model.Mailbox)
     */
    public boolean hasChildren(Mailbox<String> mailbox) throws MailboxException, MailboxNotFoundException {
        try {
            MailboxName likePath = mailbox.getMailboxName().child(MailboxQuery.FREEWILDCARD_STRING);
            JCRXPathQueryBuilder pb = prepareMailboxQueryLike(likePath);
            Query query = pb.bind(getSession().getWorkspace());
            QueryResult result = query.execute();
            NodeIterator it = result.getNodes();
            MailboxNameCodec mailboxNameCodec = repository.getMailboxNameAttributeCodec();
            MailboxQuery mailboxQuery = new MailboxQuery(likePath);
            while (it.hasNext()) {
                Node mailboxNode = it.nextNode();
                MailboxName mailboxName = mailboxNameCodec.decode(mailboxNode.getProperty(JCRImapConstants.MAILBOX_NAME_PROP).getString(), true);
                if (mailboxQuery.isExpressionMatch(mailboxName)) {
                    return true;
                }
            }
            return false;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to retrieve children for mailbox " + mailbox, e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    public List<Mailbox<String>> list() throws MailboxException {

        try {
            JCRXPathQueryBuilder pb = new JCRXPathQueryBuilder(128)
            .jcrRoot()
            .mailboxes()
            .descendantOrSelf()
            .hasMixin(JCRImapConstants.MAILBOX_TYPE);

            Query query = pb.bind(getSession().getWorkspace());
            QueryResult result = query.execute();
            NodeIterator it = result.getNodes();
            long size = it.getSize();
            if (size < 0) {
                size = StoreMailboxManager.estimateMailboxCountPerUser();
            }
            List<Mailbox<String>> mailboxes = new ArrayList<Mailbox<String>>((int) size);
            while (it.hasNext()) {
                Node mailboxNode = it.nextNode();
                mailboxes.add(new JCRMailbox(mailboxNode, getLogger()));
            }
            return mailboxes;
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to list mailboxes.", e);
        }

    }

}

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
package org.apache.james.mailbox.jcr;

import java.util.ListIterator;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;
import org.slf4j.Logger;

/**
 * Abstract base class for Mapper's which support scaling. This supports Level 1
 * Implementations of JCR. So no real transaction management is used.
 * 
 * The Session.save() will get called on commit() method, session.refresh(false)
 * on rollback, and session.refresh(true) on begin()
 * 
 */
public abstract class AbstractJCRScalingMapper extends TransactionalMapper implements JCRImapConstants {
    private interface NodeVisitor {
        void visit(String nodeName, String nodeType, String... mixin) throws RepositoryException;
    }

    private static class XPathNodeVisitor implements NodeVisitor {
        private JCRXPathQueryBuilder queryBuilder;

        public XPathNodeVisitor(JCRXPathQueryBuilder queryBuilder) {
            super();
            this.queryBuilder = queryBuilder;
        }

        @Override
        public void visit(String nodeName, String nodeType, String... mixin) throws RepositoryException {
        
            queryBuilder.delimiter().escapeName(nodeName);
            
        }
        
    }
    
    private static class GetOrAddNodeVisitor implements NodeVisitor {
        
        private Node result;
    
        public GetOrAddNodeVisitor(Node result) {
            super();
            this.result = result;
        }
    
        public Node getResult() {
            return result;
        }
    
        @Override
        public void visit(String nodeName, String nodeType, String... mixin) throws RepositoryException {
            result = JcrUtils.getOrAddNode(result, ISO9075.encodePath(nodeName), nodeType);
            if (mixin != null && mixin.length > 0) {
                for (String mix : mixin) {
                    result.addMixin(mix);
                }
            }
        }
    }


    protected final MailboxSessionJCRRepository repository;
    private final int scaling;

    protected final MailboxSession mSession;
    private final static char PAD = '_';

    public AbstractJCRScalingMapper(MailboxSessionJCRRepository repository, MailboxSession mSession, int scaling) {
        this.scaling = scaling;

        this.mSession = mSession;
        this.repository = repository;
    }

    /**
     * Return the logger
     * 
     * @return logger
     */
    protected Logger getLogger() {
        return mSession.getLog();
    }

    /**
     * Return the JCR Session
     * 
     * @return session
     */
    protected Session getSession() throws RepositoryException {
        return repository.login(mSession);
    }

    /**
     * Begin is not supported by level 1 JCR implementations, however we refresh
     * the session
     */
    protected void begin() throws MailboxException {
        try {
            getSession().refresh(true);
        } catch (RepositoryException e) {
            // do nothin on refresh
        }
        // Do nothing
    }

    /**
     * Just call save on the underlying JCR Session, because level 1 JCR
     * implementation does not offer Transactions
     */
    protected void commit() throws MailboxException {
        try {
            if (getSession().hasPendingChanges()) {
                getSession().save();
            }
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to commit", e);
        }
    }

    /**
     * Rollback is not supported by level 1 JCR implementations, so just do
     * nothing
     */
    protected void rollback() throws MailboxException {
        try {
            // just refresh session and discard all pending changes
            getSession().refresh(false);
        } catch (RepositoryException e) {
            // just catch on rollback by now
        }
    }

    /**
     * Logout from open JCR Session
     */
    public void endRequest() {
        repository.logout(mSession);
    }
    
    protected Node getOrAddInboxNode(MailboxOwner owner) throws RepositoryException {
        
        Session session = getSession();
        Node mailboxesNode = JcrUtils.getOrAddNode(session.getRootNode(), JCRImapConstants.MAILBOXES_PATH);
        mailboxesNode.addMixin(JcrConstants.MIX_LOCKABLE);
        session.save();

        GetOrAddNodeVisitor visitor = new GetOrAddNodeVisitor(mailboxesNode);
        buildInboxNode(owner, visitor);
        return visitor.getResult();
        
    }
    
    protected void buildInboxNode(MailboxOwner owner, NodeVisitor nodeVisitor) throws RepositoryException {
        MailboxNameResolver mailboxNameResolver = mSession.getMailboxNameResolver();
        MailboxName inbox = mailboxNameResolver.getInbox(owner);
        
        ListIterator<String> segmentsIt = inbox.segmentsIterator();
        while (segmentsIt.hasNext()) {
            String segment = segmentsIt.next();
            if (segmentsIt.hasNext()) {
                nodeVisitor.visit(segment, JcrConstants.NT_UNSTRUCTURED);
            }
            else {
                /* we assume here that the terminal node corresponds to user name 
                 * Note that we do not scale segments which correspond to domains */
                scale(segment, JCRImapConstants.USER_TYPE, nodeVisitor);
            }
        }
    }

    

    /**
     * Scales the given node name - i.e. it puts the given name behind several "scaling" node levels.
     * 
     * Examples for scaling = 2:
     * 
     * <table>
     * <tr><th>node name</th><th>scaled node hierarchy</th></tr>
     * <tr><td>foo</td><td>f/fo/foo</td></tr>
     * <tr><td>fo</td><td>f/fo/fo</td></tr>
     * <tr><td>f</td><td>f/f_/f</td></tr>
     * </table>
     *
     * @param segment
     * @return
     * @throws RepositoryException 
     */
    private void scale(String nameToScale, String terminalType, NodeVisitor nodeVisitor) throws RepositoryException {
        int nameToScaleLength = nameToScale.length();
        final StringBuilder sb = nameToScaleLength < scaling ? new StringBuilder(scaling) : null;
        for (int i = 0; i < scaling; i++) {
            final String scaledName;
            if (nameToScaleLength > i) {
                scaledName = nameToScale.substring(0, i + 1);
            } else {
                int j = 0;
                while (j < scaling && j < nameToScaleLength) {
                    sb.append(nameToScale.charAt(j++));
                }
                while (j < scaling) {
                    sb.append(PAD);
                }
                scaledName = sb.toString();
                sb.setLength(0);
            }
            nodeVisitor.visit(scaledName, JcrConstants.NT_UNSTRUCTURED);
        }
        nodeVisitor.visit(nameToScale, JcrConstants.NT_UNSTRUCTURED, terminalType);
    }
    
    protected Node findInboxNode(MailboxOwner owner) throws RepositoryException {
        JCRXPathQueryBuilder pb = new JCRXPathQueryBuilder(128).mailboxes();
        appendInboxPath(pb, owner);
        return getSession().getNode(pb.toString());
    }

    /**
     * Returns a {@link JCRXPathQueryBuilder} with the query common to
     * {@link #findMailboxSubscriptionForUser(String, MailboxName)} and
     * {@link #findSubscriptionsForUser(String)}.
     * 
     * @param user
     * @return a {@link JCRXPathQueryBuilder}
     */
    protected void appendInboxPath(JCRXPathQueryBuilder result, MailboxOwner owner) {
        try {
            buildInboxNode(owner, new XPathNodeVisitor(result));
        } catch (RepositoryException e) {
            /* should never happen as XPathNodeVisitor does not throw anything */
            throw new RuntimeException(e);
        }
    }
    

    /**
     * TODO findNodeByPath.
     *
     * @param path
     * @return
     * @throws RepositoryException 
     * @throws PathNotFoundException 
     */
    protected Node findNodeByMailboxName(MailboxName path) throws PathNotFoundException, RepositoryException {
        JCRXPathQueryBuilder pb = new JCRXPathQueryBuilder(128).mailboxes();
        MailboxOwner owner = mSession.getMailboxNameResolver().getOwner(path);
        appendInboxPath(pb, owner);
        
        String encodedName = repository.getMailboxNameAttributeCodec().encode(path);
        pb.delimiter().escapeName(encodedName);

        return getSession().getNode(pb.toString());
    }
    
}

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

package org.apache.james.imap.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.LsubRequest;
import org.apache.james.imap.message.response.LSubResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;

public class LSubProcessor extends AbstractSubscriptionProcessor<LsubRequest> {

    public LSubProcessor(ImapProcessor next, MailboxManager mailboxManager, SubscriptionManager subscriptionManager, StatusResponseFactory factory) {
        super(LsubRequest.class, next, mailboxManager, subscriptionManager, factory);
    }

    private void listSubscriptions(ImapSession session, Responder responder, final UnresolvedMailboxName referenceName, final UnresolvedMailboxName mailboxName) throws SubscriptionException, MailboxException {
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        
        MailboxNameResolver mailboxNameResolver = mailboxSession.getMailboxNameResolver();
        String currentUser = mailboxSession.getUser().getUserName();
        MailboxName qRef = mailboxNameResolver.resolve(referenceName, currentUser);
        MailboxName qPattern = mailboxNameResolver.resolve(mailboxName, currentUser);
        
        MailboxQuery mailboxQuery = new MailboxQuery(qRef, qPattern);
        final Collection<MailboxName> mailboxes = getSubscriptionManager().subscriptions(mailboxSession);
        
        final Map<MailboxName, Boolean> results = new TreeMap<MailboxName, Boolean>(mailboxNameResolver.getContextualizedComparator(currentUser));
        
        MailboxName resolvedPattern = mailboxQuery.getResolvedExpression();
        boolean endsWithLocalWildcard = false;
        if (resolvedPattern.getSegmentCount() > 0) {
            String lastSegment = resolvedPattern.getSegmentAt(resolvedPattern.getSegmentCount() - 1);
            endsWithLocalWildcard = MailboxQuery.LOCALWILDCARD_STRING.equals(lastSegment);
        }
        char delimiter = session.getMailboxNameCodec().getDelimiter();
        for (final MailboxName mailbox : mailboxes) {
            if (mailboxQuery.isExpressionMatch(mailbox)) {
                results.put(mailbox, Boolean.FALSE);
            }
            else if (endsWithLocalWildcard) {
                MailboxName currentMailbox = mailbox.getParent();
                while (currentMailbox != null) {
                    if (!results.containsKey(currentMailbox) && mailboxQuery.isExpressionMatch(currentMailbox)) {
                        results.put(currentMailbox, Boolean.TRUE);
                    }
                    currentMailbox = currentMailbox.getParent();
                }
            }
        }
        for (Entry<MailboxName, Boolean> result : results.entrySet()) {
            UnresolvedMailboxName uqName = mailboxNameResolver.unresolve(result.getKey(), currentUser);
            final LSubResponse response = new LSubResponse(uqName, result.getValue().booleanValue(), delimiter);
            responder.respond(response);
        }
    }

    /**
     * An empty mailboxPattern signifies a request for the hierarchy delimiter
     * and root name of the referenceName argument
     * 
     * @param referenceName
     *            IMAP reference name, possibly null
     */
    private void respondWithHierarchyDelimiter(final Responder responder, final char delimiter) {
        final LSubResponse response = new LSubResponse(UnresolvedMailboxName.EMPTY, true, delimiter);
        responder.respond(response);
    }

    /**
     * @see org.apache.james.imap.processor.AbstractSubscriptionProcessor
     * #doProcessRequest(org.apache.james.imap.api.message.request.ImapRequest,
     * org.apache.james.imap.api.process.ImapSession, java.lang.String,
     * org.apache.james.imap.api.ImapCommand,
     * org.apache.james.imap.api.process.ImapProcessor.Responder)
     */
    protected void doProcessRequest(LsubRequest request, ImapSession session, String tag, ImapCommand command, Responder responder) {
        final UnresolvedMailboxName referenceName = request.getBaseReferenceName();
        final UnresolvedMailboxName mailboxPattern = request.getMailboxPattern();

        try {
            if (mailboxPattern.isEmpty()) {
                respondWithHierarchyDelimiter(responder, session.getMailboxNameCodec().getDelimiter());
            } else {
                listSubscriptions(session, responder, referenceName, mailboxPattern);
            }

            okComplete(command, tag, responder);
        } catch (MailboxException e) {
            if (session.getLog().isInfoEnabled()) {
                session.getLog().info("LSub failed for reference " + referenceName + " and pattern " + mailboxPattern, e);
            }
            final HumanReadableText displayTextKey = HumanReadableText.GENERIC_LSUB_FAILURE;
            no(command, tag, responder, displayTextKey);
        }
    }
}

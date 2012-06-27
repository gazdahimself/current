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

import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.MailboxType;
import org.apache.james.imap.api.process.MailboxTyper;
import org.apache.james.imap.message.request.ListRequest;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxMetaData.Children;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;

public class ListProcessor extends AbstractMailboxProcessor<ListRequest> {

    public ListProcessor(final ImapProcessor next, final MailboxManager mailboxManager, final StatusResponseFactory factory) {
        super(ListRequest.class, next, mailboxManager, factory);
    }

    /**
     * @see org.apache.james.imap.processor.AbstractMailboxProcessor
     *      #doProcess(org.apache.james.imap.api.message.request.ImapRequest,
     *      org.apache.james.imap.api.process.ImapSession, java.lang.String,
     *      org.apache.james.imap.api.ImapCommand,
     *      org.apache.james.imap.api.process.ImapProcessor.Responder)
     */
    protected void doProcess(ListRequest request, ImapSession session, String tag, ImapCommand command, Responder responder) {
        final UnresolvedMailboxName baseReferenceName = request.getBaseReferenceName();
        final UnresolvedMailboxName mailboxPatternString = request.getMailboxPattern();
        doProcess(baseReferenceName, mailboxPatternString, session, tag, command, responder, null);
    }

    protected ImapResponseMessage createResponse(boolean noInferior, boolean noSelect, boolean marked, boolean unmarked, boolean hasChildren, boolean hasNoChildren, UnresolvedMailboxName mailboxName, char delimiter, MailboxType type) {
        return new ListResponse(noInferior, noSelect, marked, unmarked, hasChildren, hasNoChildren, mailboxName, delimiter);
    }

    /**
     * (from rfc3501)<br>
     * The LIST command returns a subset of names from the complete set of all
     * names available to the client. Zero or more untagged LIST replies are
     * returned, containing the name attributes, hierarchy delimiter, and name;
     * see the description of the LIST reply for more detail.<br>
     * ...<br>
     * An empty ("" string) mailbox name argument is a special request to return
     * the hierarchy delimiter and the root name of the name given in the
     * reference. The value returned as the root MAY be the empty string if the
     * reference is non-rooted or is an empty string.
     * 
     * @param referenceName
     * @param mailboxName
     * @param session
     * @param tag
     * @param command
     * @param responder
     */
    protected final void doProcess(final UnresolvedMailboxName referenceName, final UnresolvedMailboxName mailboxName, final ImapSession session, final String tag, ImapCommand command, final Responder responder, final MailboxTyper mailboxTyper) {
        try {

            final List<MailboxMetaData> results;

            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            final MailboxNameResolver nameResolver = mailboxSession.getMailboxNameResolver();
            char delimiter = session.getMailboxNameCodec().getDelimiter();

            if (mailboxName.isEmpty()) {
                /*
                 * From RFC 3501, Section 6.3.8.: An empty ("" string) mailbox
                 * name argument is a special request to return the hierarchy
                 * delimiter and the root name of the name given in the
                 * reference. The value returned as the root MAY be the empty
                 * string if the reference is non-rooted or is an empty string.
                 * In all cases, a hierarchy delimiter (or NIL if there is no
                 * hierarchy) is returned. This permits a client to get the
                 * hierarchy delimiter (or find out that the mailbox names are
                 * flat) even when no mailboxes by that name currently exist.
                 */
                final UnresolvedMailboxName root;
                if (!nameResolver.hasRoot(mailboxName)) {
                    root = UnresolvedMailboxName.EMPTY;
                }
                else {
                    root = nameResolver.getRoot(mailboxName); 
                }
                responder.respond(createResponse(false, false, false, false, false, false, root, delimiter, MailboxType.OTHER));
            } else {
                /*
                 * From RFC 3501, Section 6.3.8.: An empty ("" string) reference
                 * name argument indicates that the mailbox name is interpreted
                 * as by SELECT. The returned mailbox names MUST match the
                 * supplied mailbox name pattern. A non-empty reference name
                 * argument is the name of a mailbox or a level of mailbox
                 * hierarchy, and indicates the context in which the mailbox
                 * name is interpreted.
                 */
                User currentUser = mailboxSession.getUser();
                String userName = currentUser != null ? currentUser.getUserName() : null;
                MailboxName absReferenceName = nameResolver.resolve(referenceName, userName);
                
                MailboxName absExpression = nameResolver.resolve(referenceName, userName);
                MailboxQuery query = new MailboxQuery(absReferenceName, absExpression);
                MailboxManager manager = getMailboxManager();
                results = manager.search(query, mailboxSession);
                for (final MailboxMetaData metaData : results) {
                    processResult(responder, metaData, getMailboxType(session, mailboxTyper, metaData.getMailboxName()), nameResolver, userName, delimiter);
                }
            }

            okComplete(command, tag, responder);
        } catch (MailboxException e) {
            if (session.getLog().isInfoEnabled()) {
                session.getLog().info("List failed", e);
            }
            no(command, tag, responder, HumanReadableText.SEARCH_FAILED);
        }
    }

    void processResult(final Responder responder, final MailboxMetaData listResult, final MailboxType mailboxType, MailboxNameResolver nameResolver, String userName, char delimiter) {
        
        
        final Children inferiors = listResult.inferiors();
        final boolean noInferior = MailboxMetaData.Children.NO_INFERIORS.equals(inferiors);
        final boolean hasChildren = MailboxMetaData.Children.HAS_CHILDREN.equals(inferiors);
        final boolean hasNoChildren = MailboxMetaData.Children.HAS_NO_CHILDREN.equals(inferiors);
        boolean noSelect = false;
        boolean marked = false;
        boolean unmarked = false;
        switch (listResult.getSelectability()) {
        case MARKED:
            marked = true;
            break;
        case UNMARKED:
            unmarked = true;
            break;
        case NOSELECT:
            noSelect = true;
            break;
        default:
            break;
        }
        
        UnresolvedMailboxName mailboxName = nameResolver.unresolve(listResult.getMailboxName(), userName);
        
        responder.respond(createResponse(noInferior, noSelect, marked, unmarked, hasChildren, hasNoChildren, mailboxName, delimiter, mailboxType));
    }

    /**
     * retrieve mailboxType for specified mailboxPath using provided
     * MailboxTyper
     * 
     * @param session
     *            current imap session
     * @param mailboxTyper
     *            provided MailboxTyper used to retrieve mailbox type
     * @param path
     *            mailbox's path
     * @return MailboxType value
     */
    private MailboxType getMailboxType(ImapSession session, MailboxTyper mailboxTyper, MailboxName path) {
        MailboxType result = MailboxType.OTHER;
        if (mailboxTyper != null) {
            result = mailboxTyper.getMailboxType(session, path);
        }
        return result;
    }

    protected boolean isAcceptable(final ImapMessage message) {
        return ListRequest.class.equals(message.getClass());
    }
}

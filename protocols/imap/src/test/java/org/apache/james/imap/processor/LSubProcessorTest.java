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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.LsubRequest;
import org.apache.james.imap.message.response.LSubResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class LSubProcessorTest {
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final String USER_1 = "user1";
    private static final String USER_A = "userA";
    private static final String USER_B = "userB";
    private static final String USER_C = "userC";

    private static final char HIERARCHY_DELIMITER;

    private static final MailboxName MAILBOX_A;
    private static final MailboxName MAILBOX_B;
    private static final MailboxName MAILBOX_C;

    private static final String TAG = "TAG";
    private static final MailboxName INBOX;
    private static final MailboxName INBOX_SUB1;
    private static final MailboxName INBOX_SUB0;
    private static final MailboxName INBOX_SUB0_SUB;
    
    static {
        HIERARCHY_DELIMITER = MAILBOX_NAME_CODEC.getDelimiter();
        
        INBOX = MAILBOX_NAME_RESOLVER.getInbox(USER_1);
        INBOX_SUB1 = INBOX.child("sub1");
        INBOX_SUB0 = INBOX.child("sub0");
        INBOX_SUB0_SUB = INBOX_SUB0.child("sub");
        
        MAILBOX_A = MAILBOX_NAME_RESOLVER.getInbox(USER_A);
        MAILBOX_B = MAILBOX_NAME_RESOLVER.getInbox(USER_B);
        MAILBOX_C = MAILBOX_NAME_RESOLVER.getInbox(USER_C);
    }

    private LSubProcessor processor;

    private ImapProcessor next;

    private SubscriptionManager manager;

    private ImapProcessor.Responder responder;

    private ImapSession session;
    
    private MailboxSession mailboxSession;

    private StatusResponseFactory serverResponseFactory;

    private StatusResponse statusResponse;

    private Collection<MailboxName> subscriptions;

    private ImapCommand command;

    private ImapProcessor.Responder responderImpl;

    private Mockery mockery = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        subscriptions = new ArrayList<MailboxName>();
        serverResponseFactory = mockery.mock(StatusResponseFactory.class);
        session = mockery.mock(ImapSession.class);
        command = ImapCommand.anyStateCommand("Command");
        next = mockery.mock(ImapProcessor.class);
        responder = mockery.mock(ImapProcessor.Responder.class);
        statusResponse = mockery.mock(StatusResponse.class);
        responderImpl = responder;
        manager = mockery.mock(SubscriptionManager.class);
        mailboxSession = mockery.mock(MailboxSession.class);
        processor = new LSubProcessor(next, mockery.mock(MailboxManager.class), manager, serverResponseFactory);
    }

    @Test
    public void testHierarchy() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);

        mockery.checking(new Expectations() {{
            allowing(session).getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY); will(returnValue(mailboxSession));
            allowing(session).getMailboxNameCodec(); will(returnValue(MAILBOX_NAME_CODEC));

            oneOf(responder).respond(with(
                    equal(new LSubResponse(UnresolvedMailboxName.EMPTY, true, HIERARCHY_DELIMITER))));
        }});

        expectOk();

        LsubRequest request = new LsubRequest(command, UnresolvedMailboxName.EMPTY, UnresolvedMailboxName.EMPTY, TAG);
        processor.doProcessRequest(request, session, TAG, command, responderImpl);

    }

    @Test
    public void testShouldRespondToRegexWithSubscribedMailboxes()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(INBOX_SUB1);
        subscriptions.add(INBOX_SUB0);

        mockery.checking(new Expectations() {{

            oneOf(responder).respond(with(
                    equal(new LSubResponse(MAILBOX_NAME_RESOLVER.unresolve(INBOX_SUB1, USER_1), false, HIERARCHY_DELIMITER))));
            oneOf(responder).respond(with(
                    equal(new LSubResponse(MAILBOX_NAME_RESOLVER.unresolve(INBOX_SUB0, USER_1), false, HIERARCHY_DELIMITER))));
        }});
        
        expectSubscriptions();
        expectOk();

        UnresolvedMailboxName uqName = MAILBOX_NAME_RESOLVER.unresolve(INBOX.child(MailboxQuery.LOCALWILDCARD_STRING), USER_1);
        LsubRequest request = new LsubRequest(command, UnresolvedMailboxName.EMPTY, uqName , TAG);
        processor.doProcessRequest(request, session, TAG, command, responderImpl);

    }

    @Test
    public void testShouldRespondNoSelectToRegexWithParentsOfSubscribedMailboxes()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(INBOX_SUB0_SUB);
        
        final UnresolvedMailboxName uqName = MAILBOX_NAME_RESOLVER.unresolve(INBOX_SUB0, USER_1);

        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse(uqName, true, HIERARCHY_DELIMITER))));
        }});

        expectSubscriptions();
        expectOk();

        UnresolvedMailboxName pattern = MAILBOX_NAME_RESOLVER.unresolve(INBOX.child(MailboxQuery.LOCALWILDCARD_STRING), USER_1);

        LsubRequest request = new LsubRequest(command, UnresolvedMailboxName.EMPTY, pattern, TAG);
        processor.doProcessRequest(request, session, TAG, command, responderImpl);

    }

    @Test
    public void testShouldRespondSelectToRegexWithParentOfSubscribedMailboxesWhenParentSubscribed()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(INBOX);
        subscriptions.add(INBOX_SUB0);
        subscriptions.add(INBOX_SUB0_SUB);

        final UnresolvedMailboxName uqName = MAILBOX_NAME_RESOLVER.unresolve(INBOX_SUB0, USER_1);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse(uqName, false, HIERARCHY_DELIMITER))));
        }});

        expectSubscriptions();
        expectOk();
        
        UnresolvedMailboxName pattern = MAILBOX_NAME_RESOLVER.unresolve(INBOX.child(MailboxQuery.LOCALWILDCARD_STRING), USER_1);
        
        LsubRequest request = new LsubRequest(command, UnresolvedMailboxName.EMPTY, pattern, TAG);
        processor.doProcessRequest(request, session, TAG, command, responderImpl);

    }

    @Test
    public void testSelectAll() throws Exception {
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_NAME_RESOLVER.unresolve(MAILBOX_A, USER_1), false, HIERARCHY_DELIMITER))));
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_NAME_RESOLVER.unresolve(MAILBOX_B, USER_1), false, HIERARCHY_DELIMITER))));
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_NAME_RESOLVER.unresolve(MAILBOX_C, USER_1), false, HIERARCHY_DELIMITER))));
        }});
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);

        expectSubscriptions();
        expectOk();

        UnresolvedMailboxName pattern = new MailboxNameBuilder(1).add(MailboxQuery.FREEWILDCARD_STRING).unqualified();
        LsubRequest request = new LsubRequest(command, UnresolvedMailboxName.EMPTY, pattern , TAG);
        processor.doProcessRequest(request, session, TAG, command, responderImpl);

    }

    private void expectOk() {
        mockery.checking(new Expectations() {{
            oneOf(serverResponseFactory).taggedOk(
                    with(equal(TAG)),
                    with(same(command)),
                    with(equal(HumanReadableText.COMPLETED)));will(returnValue(statusResponse));
            oneOf(responder).respond(with(same(statusResponse)));          
        }});
    }

    private void expectSubscriptions() throws Exception {
        mockery.checking(new Expectations() {{
            allowing(session).getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY);
                    will(returnValue(mailboxSession));
            oneOf(mailboxSession).getUser(); will(returnValue(new MailboxSession.User() {

                /**
                 * @see org.apache.james.mailbox.MailboxSession.User#getLocalePreferences()
                 */
                public List<Locale> getLocalePreferences() {
                    return new ArrayList<Locale>();
                }

                /**
                 * @see org.apache.james.mailbox.MailboxSession.User#getPassword()
                 */
                public String getPassword() {
                    return "test";
                }

                /**
                 * @see org.apache.james.mailbox.MailboxSession.User#getUserName()
                 */
                public String getUserName() {
                    return USER_1;
                }
                
            }));     
            allowing(session).getMailboxNameCodec(); will(returnValue(MAILBOX_NAME_CODEC));
            allowing(mailboxSession).getMailboxNameResolver(); will(returnValue(MAILBOX_NAME_RESOLVER));

            oneOf(manager).subscriptions(with(same(mailboxSession)));will(returnValue(subscriptions));     
        }});
    }
}

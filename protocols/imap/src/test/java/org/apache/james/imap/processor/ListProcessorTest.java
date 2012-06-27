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

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.MailboxType;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
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
public class ListProcessorTest  {

    private static final String USER_1;
    private static final MailboxName MAILBOX_NAME;
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final char DELIMITER;

    private ListProcessor processor;

    private ImapProcessor next;

    private MailboxManager manager;

    private ImapProcessor.Responder responder;

    private MailboxMetaData result;

    private ImapSession session;

    private ImapCommand command;

    private StatusResponseFactory serverResponseFactory;

    private Mockery mockery = new JUnit4Mockery();
    
    static {
        USER_1 = "user1";
        DELIMITER = MAILBOX_NAME_CODEC.getDelimiter();
        MAILBOX_NAME = MAILBOX_NAME_RESOLVER.getInbox(USER_1);
    }
    
    @Before
    public void setUp() throws Exception {
        serverResponseFactory = mockery.mock(StatusResponseFactory.class);
        session = mockery.mock(ImapSession.class);
        command = ImapCommand.anyStateCommand("Command");
        next = mockery.mock(ImapProcessor.class);
        responder = mockery.mock(ImapProcessor.Responder.class);
        result = mockery.mock(MailboxMetaData.class);
        manager = mockery.mock(MailboxManager.class);
        processor = createProcessor(next, manager, serverResponseFactory);
    }

    private ListProcessor createProcessor(ImapProcessor next,
            MailboxManager manager, StatusResponseFactory factory) {
        return new ListProcessor(next, manager, factory);
    }

    private ListResponse createResponse(boolean noinferior, boolean noselect,
            boolean marked, boolean unmarked, boolean hasChildren,
            boolean hasNoChildren, char hierarchyDelimiter, MailboxName mailboxName) {
        return new ListResponse(noinferior, noselect, marked, unmarked,
                hasChildren, hasNoChildren, MAILBOX_NAME_RESOLVER.unresolve(mailboxName, USER_1), hierarchyDelimiter);
    }

    void setUpResult(final MailboxMetaData.Children children, final MailboxMetaData.Selectability selectability, final MailboxName path) {
        mockery.checking(new Expectations() {{
            oneOf(result).inferiors();will(returnValue(children));
            oneOf(result).getSelectability();will(returnValue(selectability));
            oneOf(result).getMailboxName();will(returnValue(path));
        }});
    }
    
    @Test
    public void testHasChildren() throws Exception {
        setUpResult(MailboxMetaData.Children.HAS_CHILDREN, MailboxMetaData.Selectability.NONE, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, false, true, false, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }

    @Test
    public void testHasNoChildren() throws Exception {
        setUpResult(MailboxMetaData.Children.HAS_NO_CHILDREN, MailboxMetaData.Selectability.NONE, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, false, false, true, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }
    
    @Test
    public void testNoInferiors() throws Exception {
        setUpResult(MailboxMetaData.Children.NO_INFERIORS, MailboxMetaData.Selectability.NONE, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(true, false, false, false, false, false, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }

    @Test
    public void testNoSelect() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.NOSELECT, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, true, false, false, false, false, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }

    @Test
    public void testUnMarked() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.UNMARKED, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, true, false, false, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }

    @Test
    public void testMarked() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.MARKED, MAILBOX_NAME);
        mockery.checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, true, false, false, false, DELIMITER, MAILBOX_NAME))));
        }});
        processor.processResult(responder, result, MailboxType.OTHER, MAILBOX_NAME_RESOLVER, USER_1, DELIMITER);
    }
}

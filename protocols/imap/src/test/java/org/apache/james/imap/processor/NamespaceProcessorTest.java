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
import java.util.Collections;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.NamespaceRequest;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNamespaceType;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class NamespaceProcessorTest {

    private static final String SHARED_PREFIX = "#shared";
    private static final String OTHER_USERS_PREFIX = "#users";
    private static final String PERSONAL_PREFIX = "#personal";
    private static final char DELIMITER = '.';
    
    
    private NamespaceProcessor subject;
    private StatusResponseFactory statusResponseStub;
    private ImapSession imapSessionStub;
    private MailboxSession mailboxSessionStub;
    private NamespaceRequest namespaceRequest;
    private MailboxManager mailboxManagerStub;
    private MailboxNameResolver mailboxNameResolverStub;
    private MailboxNameCodec mailboxNameCodecStub;
    private Mockery mockery = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        statusResponseStub = mockery.mock(StatusResponseFactory.class);
        mailboxManagerStub = mockery.mock(MailboxManager.class);
        subject = new NamespaceProcessor(mockery.mock(ImapProcessor.class), mailboxManagerStub, statusResponseStub);
        imapSessionStub = mockery.mock(ImapSession.class);
        mailboxSessionStub = mockery.mock(MailboxSession.class);
        mailboxNameResolverStub = mockery.mock(MailboxNameResolver.class);
        mailboxNameCodecStub = mockery.mock(MailboxNameCodec.class);
     
        namespaceRequest = new NamespaceRequest(ImapCommand.anyStateCommand("Name"), "TAG");
       
    }
    

    
    @Test
    public void testNamespaceResponseShouldContainPersonalAndUserSpaces() throws Exception {
        mockery.checking (new Expectations() {{
            allowing(imapSessionStub).supportMultipleNamespaces(); will(returnValue(true));
            allowing(imapSessionStub).getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY); will(returnValue(mailboxSessionStub));
            allowing(imapSessionStub).getAttribute(EnableProcessor.ENABLED_CAPABILITIES); will(returnValue(null));
            allowing(imapSessionStub).getMailboxNameCodec(); will(returnValue(mailboxNameCodecStub));
            allowing(any(ImapSession.class)).method("setAttribute");

            allowing(mailboxNameCodecStub).getDelimiter(); will(returnValue(DELIMITER));

            allowing(mailboxSessionStub).getMailboxNameResolver();
            will(returnValue(mailboxNameResolverStub));
            
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.personal);
            will(returnValue(Collections.singleton(PERSONAL_PREFIX)));
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.otherUsers);
            will(returnValue(Collections.singleton(OTHER_USERS_PREFIX)));
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.shared);
            will(returnValue(Collections.emptySet()));

            allowing(imapSessionStub).getState();will(returnValue(ImapSessionState.AUTHENTICATED));
            allowing(statusResponseStub).taggedOk(
                    with(any(String.class)), with(any(ImapCommand.class)), 
                    with(any(HumanReadableText.class)), with(any(ResponseCode.class))); will(returnValue(mockery.mock(StatusResponse.class)));
            ignoring(imapSessionStub);
            ignoring(mailboxManagerStub);
            ignoring(statusResponseStub);
        }});
        
        final NamespaceResponse response = buildResponse(null);
        
        final Responder responderMock = expectResponse(response);
        
        subject.doProcess(namespaceRequest, responderMock, imapSessionStub);
    }
    
    @Test
    public void testNamespaceResponseShouldContainSharedSpaces() throws Exception {
        mockery.checking (new Expectations() {{
            allowing(imapSessionStub).supportMultipleNamespaces(); will(returnValue(true));
            allowing(imapSessionStub).getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY); will(returnValue(mailboxSessionStub));
            allowing(imapSessionStub).getAttribute(EnableProcessor.ENABLED_CAPABILITIES); will(returnValue(null));
            allowing(imapSessionStub).getMailboxNameCodec(); will(returnValue(mailboxNameCodecStub));
            allowing(any(ImapSession.class)).method("setAttribute");

            allowing(mailboxNameCodecStub).getDelimiter(); will(returnValue(DELIMITER));
            
            allowing(mailboxSessionStub).getMailboxNameResolver();
            will(returnValue(mailboxNameResolverStub));
            
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.personal);
            will(returnValue(Collections.singleton(PERSONAL_PREFIX)));
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.otherUsers);
            will(returnValue(Collections.singleton(OTHER_USERS_PREFIX)));
            oneOf(mailboxNameResolverStub).listNamespacePrefixes(MailboxNamespaceType.shared);
            will(returnValue(Collections.singleton(SHARED_PREFIX)));

            allowing(imapSessionStub).getState();will(returnValue(ImapSessionState.AUTHENTICATED));
            allowing(statusResponseStub).taggedOk(
                    with(any(String.class)), with(any(ImapCommand.class)), 
                    with(any(HumanReadableText.class)), with(any(ResponseCode.class))); will(returnValue(mockery.mock(StatusResponse.class)));
            ignoring(imapSessionStub);
            ignoring(mailboxManagerStub);
            ignoring(statusResponseStub);
        }});
        
        final List<NamespaceResponse.Namespace> sharedSpaces = new ArrayList<NamespaceResponse.Namespace>();
        sharedSpaces.add(new NamespaceResponse.Namespace(SHARED_PREFIX, DELIMITER));
        final NamespaceResponse response = buildResponse(sharedSpaces);
        
        final Responder responderMock = expectResponse(response);
        
        subject.doProcess(namespaceRequest, responderMock, imapSessionStub);
    }

    private NamespaceResponse buildResponse(final List<NamespaceResponse.Namespace> sharedSpaces) {
       
        final List<NamespaceResponse.Namespace> personalSpaces = new ArrayList<NamespaceResponse.Namespace>();
        personalSpaces.add(new NamespaceResponse.Namespace(PERSONAL_PREFIX, DELIMITER));
        final List<NamespaceResponse.Namespace> otherUsersSpaces = new ArrayList<NamespaceResponse.Namespace>();
        otherUsersSpaces.add(new NamespaceResponse.Namespace(OTHER_USERS_PREFIX, DELIMITER)); 
        
        final NamespaceResponse response = new NamespaceResponse(personalSpaces, otherUsersSpaces, sharedSpaces);
        return response;
    }

    private Responder expectResponse(final NamespaceResponse response) {
        final Responder responderMock = mockery.mock(Responder.class);
        mockery.checking(new Expectations(){{
            oneOf(responderMock).respond(with(equal(response)));
            oneOf(responderMock).respond(with(any(StatusResponse.class)));
        }});
        return responderMock;
    }
}


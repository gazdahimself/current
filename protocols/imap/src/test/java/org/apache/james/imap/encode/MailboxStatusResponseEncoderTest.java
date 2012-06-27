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

package org.apache.james.imap.encode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.encode.base.ByteImapResponseWriter;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.message.response.MailboxStatusResponse;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class MailboxStatusResponseEncoderTest  {
    private static final UnresolvedMailboxName MAILBOX_NAME = new MailboxNameBuilder(2).add(MailboxConstants.INBOX).add("sub").unqualified();

    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;

    private ImapResponseComposer composer;

    private Mockery context = new JUnit4Mockery();
    private MailboxStatusResponseEncoder encoder;
    
    private ImapSession imapSession;

    private ImapEncoder mockNextEncoder;

    private ByteImapResponseWriter writer;
    
    @Before
    public void setUp() throws Exception {
        writer = new ByteImapResponseWriter();
        composer = new ImapResponseComposerImpl(writer);
        
        mockNextEncoder = context.mock(ImapEncoder.class);
        encoder = new MailboxStatusResponseEncoder(mockNextEncoder);
        imapSession = new FakeImapSession(MAILBOX_NAME_CODEC);
    }
    

    @Test
    public void testDoEncode() throws Exception {
        final Long messages = new Long(2);
        final Long recent = new Long(3);
        final Long uidNext = new Long(5);
        final Long uidValidity = new Long(7);
        final Long unseen = new Long(11);

        encoder.encode(new MailboxStatusResponse(messages, recent, uidNext,
                null, uidValidity, unseen, MAILBOX_NAME), composer, imapSession );
        assertEquals("* STATUS \""+ imapSession.getMailboxNameCodec().encode(MAILBOX_NAME) +"\" (MESSAGES 2 RECENT 3 UIDNEXT 5 UIDVALIDITY 7 UNSEEN 11)\r\n", writer.getString());
    }

    @Test
    public void testIsAcceptable() throws Exception {
        assertTrue(encoder.isAcceptable(new MailboxStatusResponse(null, null, null,
                null, null, null, MAILBOX_NAME)));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }
}

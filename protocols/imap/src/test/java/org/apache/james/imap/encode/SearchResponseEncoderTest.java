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

import static org.junit.Assert.*;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.ListResponseEncoder;
import org.apache.james.imap.encode.base.ByteImapResponseWriter;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.message.response.LSubResponse;
import org.apache.james.imap.message.response.ListResponse;
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
public class SearchResponseEncoderTest {
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
    private static final UnresolvedMailboxName MAILBOX_NAME = new MailboxNameBuilder(2).add(MailboxConstants.INBOX).add("sub").unqualified();

    private ListResponseEncoder encoder;

    private ImapEncoder mockNextEncoder;

    private ByteImapResponseWriter writer = new ByteImapResponseWriter();
    private ImapResponseComposer composer = new ImapResponseComposerImpl(writer);

    private Mockery context = new JUnit4Mockery();
    
    private ImapSession imapSession;
    
    @Before
    public void setUp() throws Exception {
        mockNextEncoder = context.mock(ImapEncoder.class);
        encoder = new ListResponseEncoder(mockNextEncoder);
        imapSession = new FakeImapSession(MAILBOX_NAME_CODEC);
    }

    @Test
    public void testIsAcceptable() {
        assertTrue(encoder.isAcceptable(new ListResponse(true, true, true,
                true, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter())));
        assertFalse(encoder.isAcceptable(new LSubResponse(MAILBOX_NAME, true, MAILBOX_NAME_CODEC.getDelimiter())));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    @Test
	public void testName() throws Exception {     
        encoder.encode(new ListResponse(false, false, false, false, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST () \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());
    }

    @Test
	public void testDelimiter() throws Exception {
        encoder.encode(new ListResponse(false, false, false, false, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST () \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());
    }


    @Test
    public void testAllAttributes() throws Exception {
        encoder.encode(new ListResponse(true, true, true, true, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST (\\Noinferiors \\Noselect \\Marked \\Unmarked) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }

    @Test
    public void testNoInferiors() throws Exception {      
        encoder.encode(new ListResponse(true, false, false, false, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST (\\Noinferiors) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());
    }

    @Test
    public void testNoSelect() throws Exception {
        encoder.encode(new ListResponse(false, true, false, false, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST (\\Noselect) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }

    @Test
    public void testMarked() throws Exception {
        encoder.encode(new ListResponse(false, false, true, false, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST (\\Marked) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }

    @Test
    public void testUnmarked() throws Exception {
        encoder.encode(new ListResponse(false, false, false, true, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter()), composer, imapSession);
        assertEquals("* LIST (\\Unmarked) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }
}

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
public class LSubResponseEncoderTest  {
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
    private static final UnresolvedMailboxName MAILBOX_NAME = new MailboxNameBuilder(2).add(MailboxConstants.INBOX).add("sub").unqualified();

    private LSubResponseEncoder encoder;

    private ImapEncoder mockNextEncoder;

    private ByteImapResponseWriter writer = new ByteImapResponseWriter();
    private ImapResponseComposer composer = new ImapResponseComposerImpl(writer);

    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        mockNextEncoder = context.mock(ImapEncoder.class);
        encoder = new LSubResponseEncoder(mockNextEncoder);
    }

    @Test
    public void testIsAcceptable() {
        assertFalse(encoder.isAcceptable(new ListResponse(true, true, true,
                true, false, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter())));
        assertTrue(encoder.isAcceptable(new LSubResponse(MAILBOX_NAME, true, MAILBOX_NAME_CODEC.getDelimiter())));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    @Test
	public void testName() throws Exception {
        encoder.encode(new LSubResponse(MAILBOX_NAME, false, MAILBOX_NAME_CODEC.getDelimiter()), composer, new FakeImapSession());
        assertEquals("* LSUB () \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }

    @Test
	public void testDelimiter() throws Exception {
        encoder.encode(new LSubResponse(MAILBOX_NAME, false, MAILBOX_NAME_CODEC.getDelimiter()), composer, new FakeImapSession());
        assertEquals("* LSUB () \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }

    @Test
    public void testNoSelect() throws Exception {
        encoder.encode(new LSubResponse(MAILBOX_NAME, true, MAILBOX_NAME_CODEC.getDelimiter()), composer, new FakeImapSession());
        assertEquals("* LSUB (\\Noselect) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());


    }
}

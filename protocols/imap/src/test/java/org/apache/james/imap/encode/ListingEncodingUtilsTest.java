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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.james.imap.encode.base.ByteImapResponseWriter;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.junit.Before;
import org.junit.Test;

public class ListingEncodingUtilsTest  {

    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
    private static final UnresolvedMailboxName MAILBOX_NAME = new MailboxNameBuilder(2).add(MailboxConstants.INBOX).add("sub").unqualified();
    private final String typeNameParameters = "A Type Name";
    
    List<String> attributesOutput;
        
    private ByteImapResponseWriter writer = new ByteImapResponseWriter();
    private ImapResponseComposer composer = new ImapResponseComposerImpl(writer);
    
    @Before
    public void setUp() throws Exception {
        attributesOutput = new ArrayList<String>();
      
    }

    @Test
    public void testShouldAddHasChildrenToAttributes() throws Exception {
        // Setup 
        attributesOutput.add("\\HasChildren");
        ListResponse input = new ListResponse(false, false, false, false, true, false, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter());
            
        // Exercise
        ListingEncodingUtils.encodeListingResponse(typeNameParameters, composer, input, MAILBOX_NAME_CODEC);
        Assert.assertEquals("* A Type Name (\\HasChildren) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());
    }
    
    @Test
    public void testShouldAddHasNoChildrenToAttributes() throws Exception {
        // Setup 
        attributesOutput.add("\\HasNoChildren");
        ListResponse input = new ListResponse(false, false, false, false, false, true, MAILBOX_NAME, MAILBOX_NAME_CODEC.getDelimiter());
            
        // Exercise
        ListingEncodingUtils.encodeListingResponse(typeNameParameters, composer, input, MAILBOX_NAME_CODEC);
        Assert.assertEquals("* A Type Name (\\HasNoChildren) \""+MAILBOX_NAME_CODEC.getDelimiter()+"\" \""+ MAILBOX_NAME_CODEC.encode(MAILBOX_NAME) +"\"\r\n", writer.getString());

    }
}

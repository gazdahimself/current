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
package org.apache.james.imap.decode.parser;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.message.request.ListRequest;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.protocols.imap.DecodingException;

/**
 * Parse LIST commands
 */
public class ListCommandParser extends AbstractUidCommandParser {

    public ListCommandParser() {
        super(ImapCommand.authenticatedStateCommand(ImapConstants.LIST_COMMAND_NAME));
    }

    protected ListCommandParser(final ImapCommand command) {
        super(command);
    }

    /**
     * @see
     * org.apache.james.imap.decode.parser.AbstractUidCommandParser#decode(org.apache.james.imap.api.ImapCommand,
     * org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String,
     * boolean, org.apache.james.imap.api.process.ImapSession)
     */
    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids, ImapSession session) throws DecodingException {
        MailboxNameCodec nameCodec = session.getMailboxNameCodec();
        UnresolvedMailboxName referenceName = nameCodec.decode(request.astring());
        UnresolvedMailboxName mailboxPattern = nameCodec.decode(request.astring());
        request.eol();
        final ImapMessage result = createMessage(command, referenceName, mailboxPattern, tag);
        return result;
    }

    protected ImapMessage createMessage(ImapCommand command, final UnresolvedMailboxName referenceName, final UnresolvedMailboxName mailboxPattern, final String tag) {
        final ImapMessage result = new ListRequest(command, referenceName, mailboxPattern, tag);
        return result;
    }
}

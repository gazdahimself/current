/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.name.codec;

/**
 * RFC 3501 non-compliant {@link MailboxNameEscaper} implementation. Applies
 * modified UTF7 de/encoding. When encoding, it handles delimiters in segments
 * as not belonging to the "printable US-ASCII" set.
 * 
 * Why {@link DelimiterAwareUtf7MailboxNameCodec} is RFC 3501 non-compliant: RFC
 * 3501 states that <cite>Modified BASE64 MUST NOT be used to represent any
 * printing US-ASCII character which can represent itself.</cite>
 * 
 * A schematic example: Let delimiter be '.' Then
 * decode("suppliers.amazon&AC4-com") would return an equivalent of new
 * DefaultMailboxName(new String[] {"suppliers", "amazon.com"}). Note that the
 * RFC 3501 compliant {@link OptimisticUtf7mMailboxNameEscaper} would return an
 * equivalent of new DefaultMailboxName(new String[] {"suppliers", "amazon",
 * "com"}). The other direction: DelimiterAwareUtf7MailboxNameCodec.encode(new
 * DefaultMailboxName(new String[] {"suppliers", "amazon.com"})) would result in
 * "suppliers.amazon&AC4-com" while {@link OptimisticUtf7mMailboxNameEscaper} would
 * return "suppliers.amazon.com".
 * 
 * Compliancy of other servers:
 * <ul>
 * <li>Dovecot TODO: dovecot version - handles names like
 * {@link DelimiterAwareUtf7MailboxNameCodec}
 * 
 * <pre>
 * A1 LIST "" "*"
 * * LIST (\HasNoChildren) "/" "Trash"
 * * LIST (\HasNoChildren) "/" "INBOX"
 * A1 OK List completed.
 * A1 CREATE "INBOX/movies/B&AC8-W"
 * A1 OK Create completed.
 * A1 LIST "" "*"
 * * LIST (\HasNoChildren) "/" "Trash"
 * * LIST (\HasChildren) "/" "INBOX"
 * * LIST (\HasNoChildren) "/" "INBOX/movies/B&AC8-W"
 * A1 OK List completed.
 * </pre>
 * 
 * </li>
 * <li>Cyrus v2.3.16 - very strict with modified UTF7: does not accept
 * base64-encoded printables at all:
 * 
 * <pre>
 * A1 LIST "" "*"
 * * LIST (\HasChildren) "/" "INBOX"
 * * LIST (\HasNoChildren) "/" "INBOX/Trash"
 * A1 OK Completed (0.000 secs 5 calls)
 * A1 CREATE "INBOX/movies/B&AC8-W"
 * A1 NO Invalid mailbox name
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * Compliancy of clients.
 * 
 * TODO: What may happen if a client cannot handle this way of escaping?
 * 
 * <ul>
 * <li>Thunderbird: Blindly forwards unescaped delimiters when creating
 * mailboxes, but handles escaped delimiters in mailbox names properly, when it
 * gets them from the server.</li>
 * <li>TODO: other clients</li>
 * </ul>
 * 
 * */
public class PesimisticUtf7mMailboxNameEscaper extends OptimisticUtf7mMailboxNameEscaper {

    public PesimisticUtf7mMailboxNameEscaper(char delimiter) {
        super(delimiter);
    }

    @Override
    protected boolean needsEscaping(char ch) {
        return delimiter == ch || super.needsEscaping(ch);
    }

}

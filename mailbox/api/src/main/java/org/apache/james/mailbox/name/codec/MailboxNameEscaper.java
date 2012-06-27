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

import java.io.IOException;

/**
 * Decides which parts of a mailbox name need to be escaped/unescaped and
 * handles the escaping and unescaping.
 * 
 * Implementations must not store any internal state.
 * 
 */
public interface MailboxNameEscaper {

    void appendDelimiter(Appendable buffer) throws IOException;

    int consumeDelimiter(int position, String encodedMailboxName);

    int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException;

    char getDelimiter();

    boolean isDelimiter(int position, String encodedMailboxName);

    boolean isEscapeSequence(int position, String encodedMailboxName);

    boolean needsEscaping(int position, String mailboxNameSegment);

    int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException;
}
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


public class DoublingMailboxNameEscaper extends OptimisticMailboxNameEscaper {

    public DoublingMailboxNameEscaper(char delimiter) {
        super(delimiter);
    }

    @Override
    public int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException {
        char ch = mailboxNameSegment.charAt(position);
        if (ch != delimiter) {
            throw new IllegalStateException("Nothing to escape at position " + position + " in '" + mailboxNameSegment + "'.");
        }
        buffer.append(ch).append(ch);
        return position + 1;
    }

    @Override
    public boolean isEscapeSequence(int position, String encodedMailboxName) {
        if (position + 1 >= encodedMailboxName.length()) {
            return false;
        }
        char ch = encodedMailboxName.charAt(position);
        return ch == delimiter && encodedMailboxName.charAt(position + 1) == delimiter;
    }

    @Override
    public boolean needsEscaping(int position, String mailboxNameSegment) {
        char ch = mailboxNameSegment.charAt(position);
        return ch == delimiter;
    }

    @Override
    public int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException {
        if (position + 1 >= encodedMailboxName.length()) {
            throw new IllegalStateException("Nothing to unescape at position " + position + " in '" + encodedMailboxName + "'.");
        }
        char ch = encodedMailboxName.charAt(position);
        if (ch != delimiter || encodedMailboxName.charAt(position + 1) != delimiter) {
            throw new IllegalStateException("Nothing to unescape at position " + position + " in '" + encodedMailboxName + "'.");
        }
        buffer.append(ch);
        return position + 2;
    }
}
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
 * Instead of escaping and unescaping it supposes that {@link #delimiter}
 * character is highly improbable or totaly impossible to occur in the
 * mailbox name segments. Well, the naiveness this suppossition depends
 * highly on the chosen delimiter character. E.g. '\f' (form feed 0xC) might be seen as a
 * good choice, but '.' and '/' are bad choices.
 * 
 */
public class OptimisticMailboxNameEscaper implements MailboxNameEscaper {

    protected final char delimiter;

    public OptimisticMailboxNameEscaper(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public void appendDelimiter(Appendable buffer) throws IOException {
        buffer.append(delimiter);
    }

    @Override
    public int consumeDelimiter(int position, String encodedMailboxName) {
        if (encodedMailboxName.charAt(position) != delimiter) {
            throw new IllegalStateException("No delimiter at position " + position + " in '" + encodedMailboxName + "'.");
        }
        return position + 1;
    }

    @Override
    public int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException {
        throw new IllegalStateException("Nothing to escape at position " + position + " in '" + mailboxNameSegment + "'.");
    }

    public char getDelimiter() {
        return delimiter;
    }

    @Override
    public boolean isDelimiter(int position, String encodedMailboxName) {
        return encodedMailboxName.charAt(position) == delimiter;
    }

    @Override
    public boolean isEscapeSequence(int position, String encodedMailboxName) {
        return false;
    }

    @Override
    public boolean needsEscaping(int position, String mailboxNameSegment) {
        return false;
    }

    @Override
    public int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException {
        throw new IllegalStateException("Nothing to unescape at position " + position + " in '" + encodedMailboxName + "'.");
    }

}
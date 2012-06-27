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

import org.apache.james.mailbox.model.MailboxQuery;

/**
 * TODO JcrSearchPatternEscaper.
 */
public class LikeSearchPatternEscaper extends OptimisticMailboxNameEscaper {
    public static final char JCR_UNIVERSAL_WILDCARD = '%';
    public static final char SQL_UNIVERSAL_WILDCARD = '%';
    private final char universalWildcard;
    public LikeSearchPatternEscaper(char delimiter, char universalWildcard) {
        super(delimiter);
        this.universalWildcard = universalWildcard;
    }
    
    @Override
    public int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException {
        char ch = mailboxNameSegment.charAt(position);
        switch (ch) {
        case MailboxQuery.FREEWILDCARD:
        case MailboxQuery.LOCALWILDCARD:
            buffer.append(universalWildcard);
            return position + 1;
        default:
            throw new IllegalStateException("Nothing to escape at position " + position + " in '" + mailboxNameSegment + "'.");
        }
    }

    @Override
    public boolean isEscapeSequence(int position, String encodedMailboxName) {
        char ch = encodedMailboxName.charAt(position);
        switch (ch) {
        case MailboxQuery.FREEWILDCARD:
        case MailboxQuery.LOCALWILDCARD:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean needsEscaping(int position, String mailboxNameSegment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException {
        throw new UnsupportedOperationException();
    }
}

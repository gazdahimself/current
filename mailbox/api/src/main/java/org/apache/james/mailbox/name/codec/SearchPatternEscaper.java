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
import java.util.regex.Pattern;

import org.apache.james.mailbox.model.MailboxQuery;

/**
 * TODO SearchPatternEscaper.
 */
public class SearchPatternEscaper extends OptimisticMailboxNameEscaper {
    public static final SearchPatternEscaper INSTANCE = new SearchPatternEscaper(); 
    private static final String REGEX_FREE = ".*";

    private final String localRegex;

    private SearchPatternEscaper() {
        super(MailboxNameCodec.SAFE_STORE_DELIMITER);
        
        localRegex = "[^"+ Pattern.quote(String.valueOf(delimiter)) +"]*";
        
    }

    @Override
    public int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException {
        char ch = mailboxNameSegment.charAt(position);
        switch (ch) {
        case MailboxQuery.FREEWILDCARD:
            buffer.append(REGEX_FREE);
            return position + 1;
        case MailboxQuery.LOCALWILDCARD:
            buffer.append(localRegex);
            return position + 1;
        default:
            throw new IllegalStateException("Nothing to escape at position " + position + " in '" + mailboxNameSegment + "'.");
        }
    }

    @Override
    public boolean isEscapeSequence(int position, String encodedMailboxName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsEscaping(int position, String mailboxNameSegment) {
        char ch = mailboxNameSegment.charAt(position);
        switch (ch) {
        case MailboxQuery.FREEWILDCARD:
        case MailboxQuery.LOCALWILDCARD:
            return true;
        default:
            return false;
        }
    }

    @Override
    public int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    
}

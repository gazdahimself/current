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
import java.util.List;
import java.util.ListIterator;

import org.apache.james.mailbox.name.AbstractMailboxName;
import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;

/**
 * TODO MaildirMailboxNameCodec.
 */
public class MaildirMailboxNameCodec extends DefaultMailboxNameCodec {

    public MaildirMailboxNameCodec() {
        super(MaildirMailboxNameEscaper.INSTANCE, true);
    }

    /**
     * Strips the initial delimiter and delegates to the same superclass method.
     * @see org.apache.james.mailbox.name.codec.DefaultMailboxNameCodec#decodeSegments(java.lang.String)
     */
    @Override
    protected List<String> decodeSegments(String encodedMailboxName) {
        if (encodedMailboxName.length() > 0 && encodedMailboxName.charAt(0) == getDelimiter()) {
            return super.decodeSegments(encodedMailboxName.substring(1));
        }
        else {
            return super.decodeSegments(encodedMailboxName);
        }
    }

    /**
     * The same as superclass method with the only difference that it prepends the result with delimiter.
     * 
     * @see org.apache.james.mailbox.name.codec.DefaultMailboxNameCodec#encode(AbstractMailboxName)
     */
    @Override
    public String encode(AbstractMailboxName mailboxName) {
        StringBuilder result = new StringBuilder(DefaultUnresolvedMailboxName.estimateSerializedLength(mailboxName));
        ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
        try {
            while (segmentsIt.hasNext()) {
                escaper.appendDelimiter(result);
                String segment = segmentsIt.next();
                int position = 0;
                while (position < segment.length()) {
                    if (escaper.needsEscaping(position, segment)) {
                        position = escaper.escape(position, segment, result);
                    } else {
                        result.append(segment.charAt(position++));
                    }
                }
            }
        } catch (IOException e) {
            /*
             * This should not happen as StringBuilder never throws IOException
             * from append()
             */
            throw new RuntimeException(e);
        }
        return result.toString();
    }

}

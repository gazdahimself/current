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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.james.mailbox.name.AbstractMailboxName;
import org.apache.james.mailbox.name.DefaultMailboxName;
import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;

/**
 * TODO StoreMailboxNameCodec.
 */
public class DefaultMailboxNameCodec implements MailboxNameCodec {

    public DefaultMailboxNameCodec(MailboxNameEscaper escaper, boolean ignoreInitialDelimiter) {
        super();
        this.escaper = escaper;
        this.ignoreInitialDelimiter = ignoreInitialDelimiter;
    }

    public DefaultMailboxNameCodec(MailboxNameEscaper escaper) {
        this(escaper, false);
    }

    protected final MailboxNameEscaper escaper;
    protected final boolean ignoreInitialDelimiter;


    @Override
    public UnresolvedMailboxName decode(String mailboxName) {
        return new DefaultUnresolvedMailboxName(decodeSegments(mailboxName));
    }

    @Override
    public MailboxName decode(String mailboxName, boolean hasRoot) {
        return new DefaultMailboxName(decodeSegments(mailboxName), hasRoot);
    }

    protected List<String> decodeSegments(String encodedMailboxName) {
        int encodedMailboxNameLength = encodedMailboxName.length();
        List<String> result = new ArrayList<String>(encodedMailboxNameLength / 4 + 1);
        int position = 0;
        StringBuilder segmentBuilder = new StringBuilder(16);
        try {
            while (position < encodedMailboxNameLength) {
                if (escaper.isEscapeSequence(position, encodedMailboxName)) {
                    position = escaper.unescape(position, encodedMailboxName, segmentBuilder);
                } else if (escaper.isDelimiter(position, encodedMailboxName)) {
                    if (ignoreInitialDelimiter && position == 0) {
                        position = escaper.consumeDelimiter(position, encodedMailboxName);
                    }
                    else {
                        position = escaper.consumeDelimiter(position, encodedMailboxName);
                        result.add(segmentBuilder.toString());
                        segmentBuilder.setLength(0);
                    }
                } else {
                    char ch = encodedMailboxName.charAt(position++);
                    segmentBuilder.append(ch);
                }
            }
        } catch (IOException e) {
            /*
             * This should not happen as StringBuilder never throws IOException
             * from append()
             */
            throw new RuntimeException(e);
        }
        /* empty last segment ignored silently */
        String lastSegment = segmentBuilder.toString();
        if (lastSegment.length() > 0) {
            result.add(segmentBuilder.toString());
        }
        return result;
    }

    @Override
    public String encode(AbstractMailboxName mailboxName) {
        StringBuilder result = new StringBuilder(DefaultUnresolvedMailboxName.estimateSerializedLength(mailboxName));
        ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
        try {
            while (segmentsIt.hasNext()) {
                if (result.length() > 0) {
                    escaper.appendDelimiter(result);
                }
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

    @Override
    public char getDelimiter() {
        return escaper.getDelimiter();
    }

}

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
 * RFC 3501 compliant {@link MailboxNameEscaper} implementation. Uses
 * modified UTF7 and does not care for any delimiter escaping.
 */
public class OptimisticUtf7mMailboxNameEscaper extends OptimisticMailboxNameEscaper {
    private static final String BASE64M_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+,";
    private static final int CHAR_SIZE = Character.SIZE;

    private static final int INVALID_POSITION = -1;
    private static final int MAX_SEXTET;
    private static final int[] REVERSE_BASE64M_LOOKUP;
    private static final int REVERSE_BASE64M_LOOKUP_SIZE;
    private static final int SEXTET_LENGTH;
    private static final char SHIFT = '&';
    private static final char UNSHIFT = '-';
    static {
        REVERSE_BASE64M_LOOKUP_SIZE = 128;
        REVERSE_BASE64M_LOOKUP = new int[REVERSE_BASE64M_LOOKUP_SIZE];
        for (int i = 0; i < REVERSE_BASE64M_LOOKUP.length; i++) {
            REVERSE_BASE64M_LOOKUP[i] = INVALID_POSITION;
        }
        for (int i = 0; i < BASE64M_ALPHABET.length(); i++) {
            REVERSE_BASE64M_LOOKUP[BASE64M_ALPHABET.charAt(i)] = i;
        }
        SEXTET_LENGTH = 6;
        MAX_SEXTET = (0xffffffff >>> (Integer.SIZE - SEXTET_LENGTH));
    }

    public OptimisticUtf7mMailboxNameEscaper(char delimiter) {
        super(delimiter);
    }

    @Override
    public int escape(int position, String mailboxNameSegment, Appendable buffer) throws IOException {
        char ch = mailboxNameSegment.charAt(position);
        if (!needsEscaping(ch)) {
            throw new IllegalStateException("Nothing to escape at position " + position + " in '" + mailboxNameSegment + "'.");
        }
        position++;
        switch (ch) {
        case SHIFT:
            buffer.append(SHIFT).append(UNSHIFT);
            break;
        default:
            
            /* b64m escape */
            buffer.append(SHIFT);
            int bitCount = 0;
            int bits = 0;
            
            do {
                bitCount += 16;
                while (bitCount >= SEXTET_LENGTH) {
                    bitCount -= SEXTET_LENGTH;
                    bits += ch >> bitCount;
                    bits &= MAX_SEXTET;
                    char encodedChar = BASE64M_ALPHABET.charAt(bits);
                    buffer.append(encodedChar);
                    bits = 0;
                }
                /* store the rest which will be used with the next char */
                bits = (ch << (SEXTET_LENGTH - bitCount)) & MAX_SEXTET;

                if (position >= mailboxNameSegment.length()) {
                    break;
                }
                ch = mailboxNameSegment.charAt(position);
                if (!needsEscaping(ch)) {
                    break;
                }
                position++;
            }
            while (true);
            
            /* append the rest */
            if (bitCount > 0) {
                char encodedChar = BASE64M_ALPHABET.charAt(bits);
                buffer.append(encodedChar);
            }
            
            buffer.append(UNSHIFT);
            
            break;
        }
        return position;
    }

    /**
     * TODO needsEscaping.
     *
     * @param ch
     * @return
     */
    protected boolean needsEscaping(char ch) {
        return ch == SHIFT || ch < 0x20 || ch > 0x7e;
    }

    @Override
    public boolean isEscapeSequence(int position, String encodedMailboxName) {
        char ch = encodedMailboxName.charAt(position);
        return ch == SHIFT;
    }

    /**
     * In modified UTF-7, printable US-ASCII characters, except for "&",
     * represent themselves; that is, characters with octet values 0x20-0x25 and
     * 0x27-0x7e. The character "&" (0x26) is represented by the two-octet
     * sequence "&-".
     * 
     * @see org.apache.james.mailbox.name.codec.OptimisticMailboxNameEscaper#needsEscaping(int,
     *      java.lang.String)
     * 
     * 
     */
    @Override
    public boolean needsEscaping(int position, String mailboxNameSegment) {
        char ch = mailboxNameSegment.charAt(position);
        return needsEscaping(ch);
    }

    @Override
    public int unescape(int position, String encodedMailboxName, Appendable buffer) throws IOException {
        char ch = encodedMailboxName.charAt(position);
        if (ch != SHIFT) {
            throw new IllegalStateException("Nothing to unescape at position " + position + " in '" + encodedMailboxName + "'.");
        }
        position++;
        if (position < encodedMailboxName.length()) {
            ch = encodedMailboxName.charAt(position++);
            switch (ch) {
            case UNSHIFT:
                buffer.append(SHIFT);
                break;
            default:

                /* b64m unescape */
                int decodedBitCount = 0;
                int decodedBits = 0;
                do {
                    if (ch >= REVERSE_BASE64M_LOOKUP_SIZE) {
                        throw new IOException("Unexpected characted '"+ ch +"' in a base64 input.");
                    }
                    int sextet = REVERSE_BASE64M_LOOKUP[ch];
                    if (sextet < 0) {
                        throw new IOException("Unexpected characted '"+ ch +"' in a base64 input.");
                    }
                    decodedBitCount += SEXTET_LENGTH;
                    if (decodedBitCount >= CHAR_SIZE) {
                        /* 16 bits ready - take them */
                        decodedBitCount -= CHAR_SIZE;
                        decodedBits += sextet >>> decodedBitCount;
                        char decodedChar = (char) decodedBits;
                        buffer.append(decodedChar);
                        
                        decodedBits = (sextet << (CHAR_SIZE - decodedBitCount)) & 0xFFFF;
                    }
                    else {
                        decodedBits += sextet << (CHAR_SIZE - decodedBitCount);
                    }
                    
                    if (position >= encodedMailboxName.length()) {
                        throw new IOException("Unexpected end of input in '"+ encodedMailboxName +"'; expected UNSHIFT.");
                    }
                    ch = encodedMailboxName.charAt(position++);

                }
                while (ch != UNSHIFT);
                
                if (ch != UNSHIFT) {
                    throw new IOException("UNSHIFT expected at position "+ (position - 1) +" in '"+ encodedMailboxName +"'.");
                }
                else {
                    /* consume the UNSHIFT */
                    //position++;
                }

                break;
            }
        }
        return position;
    }

}

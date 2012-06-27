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

import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.AbstractMailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;

/**
 * Provides the capabilities of (i) decoding a string representation of a
 * mailbox name into a {@link MailboxName} or
 * {@link UnresolvedMailboxName} and (ii) encoding an
 * {@link UnresolvedMailboxName} into a string representation of the mailbox
 * mailbox name.
 * 
 * Implementations must not store any internal state.
 * 
 */
public interface MailboxNameCodec {
    /**
     * For legacy reasons here. Dot is not a good idea. 
     */
    char LEGACY_DEFAULT_DELIMITER = '.';
    
    char DEFAULT_IMAP_DELIMITER = '/';
    char SAFE_STORE_DELIMITER = '\f';
    char SIEVE_DELIMITER = '/';
    
    MailboxNameCodec DEFAULT_IMAP_NAME_CODEC = new DefaultMailboxNameCodec(new OptimisticUtf7mMailboxNameEscaper(MailboxNameCodec.DEFAULT_IMAP_DELIMITER));
    MailboxNameCodec SAFE_STORE_NAME_CODEC = new DefaultMailboxNameCodec(new OptimisticMailboxNameEscaper(SAFE_STORE_DELIMITER));

    MailboxNameCodec SEARCH_SUBJECT_NAME_CODEC = SAFE_STORE_NAME_CODEC;
    MailboxNameCodec SEARCH_PATTERN_NAME_CODEC = new DefaultMailboxNameCodec(SearchPatternEscaper.INSTANCE);
    
    MailboxNameCodec MAILDIR_NAME_CODEC = new MaildirMailboxNameCodec();
    MailboxNameCodec SIEVE_NAME_CODEC = new DefaultMailboxNameCodec(new OptimisticMailboxNameEscaper(SIEVE_DELIMITER), true);

    /**
     * Special delimiter value meaning "no delimiter".
     */
    char NIL_DELIMITER = Character.UNASSIGNED;


    /**
     * Parses a string representation of a mailbox name into an
     * {@link UnresolvedMailboxName}.
     * 
     * @param mailboxName
     * @return
     */
    UnresolvedMailboxName decode(String mailboxName);

    /**
     * Parses a string representation of a mailbox name into a
     * {@link MailboxName}.
     * 
     * @param mailboxName
     * @param hasRoot
     * @return
     */
    MailboxName decode(String mailboxName, boolean hasRoot);

    /**
     * Serialises the given {@link UnresolvedMailboxName} into its String
     * representation.
     * 
     * @param mailboxName
     * @return
     */
    String encode(AbstractMailboxName mailboxName);

    /**
     * Returns the delimiter which is expected during decoding and which is used
     * during encoding.
     * 
     * @return
     */
    char getDelimiter();
}

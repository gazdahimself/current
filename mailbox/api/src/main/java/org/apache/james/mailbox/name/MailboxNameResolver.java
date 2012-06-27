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

package org.apache.james.mailbox.name;

import java.util.Collection;
import java.util.Comparator;


/**
 * Transforms mailbox names between IMAP requests and store backends. Defines
 * the layout of the mailbox hierarchy on the side of the store and the
 * presentation of mailboxes under individual namespace prefixes to IMAP
 * clients.
 * 
 * {@link MailboxNameResolver} provides a way to list all namespaces presentable
 * to IMAP clients - see {@link #listNamespacePrefixes(MailboxNamespaceType)}.
 * 
 */
public interface MailboxNameResolver {
    public UnresolvedMailboxName unresolve(MailboxName mailboxName, String currentUser);

    /**
     * Transforms the given {@link String} {@code mailboxName} into a
     * {@link MailboxName}. The resulting {@link MailboxName} must be absolute,
     * i.e. it must be independent from any context, esp. from the current user.
     * 
     * 
     * 
     * @param maiboxName
     * @param currentUser
     * @return
     */
    public MailboxName resolve(UnresolvedMailboxName mailboxName, String currentUser);

    /**
     * TODO existsTrivially.
     *
     * @param mailboxName
     * @return
     */
    public boolean existsTrivially(MailboxName mailboxName);
    
    /**
     * TODO getContextualizedComparator.
     *
     * @return
     */
    public Comparator<MailboxName> getContextualizedComparator(String currentUser);
    /**
     * TODO getHome.
     *
     * @param userOrGroup
     * @return
     */
    public MailboxName getInbox(String userOrGroup);
    public MailboxName getInbox(MailboxOwner mailboxOwner);
    
    MailboxOwner getOwner(MailboxName mailboxName);

    boolean isVirtualPrefix(String backendPrefix);
    /**
     * Returns an unmodifiable {@link Collection} of namespace prefixes
     * supported by this {@link MailboxNameResolver}.
     * 
     * @param type
     * @return
     */
    public Collection<String> listNamespacePrefixes(MailboxNamespaceType type);
    /**
     * TODO getOwner.
     *
     * @param userOrGroupName
     * @param isGroup
     * @return
     */
    public MailboxOwner getOwner(String userOrGroupName, boolean isGroup);

    /**
     * TODO hasRoot.
     *
     * @param mailboxName
     * @return
     */
    public boolean hasRoot(UnresolvedMailboxName mailboxName);

    /**
     * TODO getRoot.
     *
     * @param mailboxName
     * @return
     */
    public UnresolvedMailboxName getRoot(UnresolvedMailboxName mailboxName);

}

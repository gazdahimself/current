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

/**
 * Presents a hierarchical mailbox name which may need to be interpreted by a
 * {@link MailboxNameResolver}. The final reference of an
 * {@link UnresolvedMailboxName} may depend on the user name of the current user
 * or other information found in the context of the current request.
 */
public interface UnresolvedMailboxName extends AbstractMailboxName {
    /**
     * Name with no segments. Result of parsing "" mailbox name. To find out if
     * a given {@link UnresolvedMailboxName} or {@link MailboxName} is empty,
     * consider {@link UnresolvedMailboxName#isEmpty()} instead of
     * {@link UnresolvedMailboxName#EMPTY}.equals(myMailboxName).
     */
    public static final UnresolvedMailboxName EMPTY = new DefaultUnresolvedMailboxName();

}

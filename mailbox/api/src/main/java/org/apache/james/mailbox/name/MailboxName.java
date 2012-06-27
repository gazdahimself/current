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

import java.util.Iterator;

/**
 * Note that {@link MailboxName} is deliberately not a subclass of
 * {@link UnresolvedMailboxName} so that it is illegal to use instances of
 * {@link MailboxName} where only {@link UnresolvedMailboxName}s are
 * appropriate.
 */
public interface MailboxName extends AbstractMailboxName, Comparable<MailboxName> {

    /**
     * Name with no segments. Result of parsing "" mailbox name. To find out if
     * a given {@link UnresolvedMailboxName} or {@link MailboxName} is empty,
     * consider {@link UnresolvedMailboxName#isEmpty()} instead of
     * {@link MailboxName#EMPTY}.equals(myMailboxName).
     * 
     */
    public static final MailboxName EMPTY = new DefaultMailboxName();

    /**
     * Creates a new instance of {@link MailboxName} with the given
     * {@code suffix} appended to the last segment of this {@link MailboxName}.
     * 
     * Schemantic example: if this is "folder.subfolder", calling
     * this.appendToLast("*") would result in "folder.subfolder*". Note that
     * this.child("*") would return "folder.subfolder.*"
     * 
     * @param suffix
     * @return
     */
    MailboxName appendToLast(String suffix);

    /**
     * Creates a new instance of {@link MailboxName} through appending the given
     * {@link String} as a bottom most segment to this {@link MailboxName}.
     * 
     * @param segment
     *            single segment. No parsing done with it.
     * @return
     */
    MailboxName child(String segment);

    /**
     * Returns the parent {@link MailboxName}, i.e. the {@link MailboxName}
     * consisting of {@link #getSegmentCount()} - 1 segments, leaving the
     * leftmost (bottom) segment of this {@link MailboxName} out. Returns
     * {@code null} if {@code this.getSegmentCount() == 1}.
     * 
     * @return Returns the parent MailboxName or null.
     */
    MailboxName getParent();

    /**
     * Returns {@code true} if this {@link MailboxName} has a parent
     * {@link MailboxName}; otherwise returns {@code false}. Equivalent to
     * <code>getSegmentCount() > 1</code>
     * 
     * @return {@code true} or {@code false}
     */
    boolean hasParent();

    /**
     * Returns {@code true} if this is to be interpreted as an absolute
     * {@link MailboxName}, i.e. if this {@link MailboxName} is not relative to
     * any other {@link MailboxName}. Otherwise returns {@code false}.
     * 
     * @return {@code true} or {@code false}
     */
    boolean hasRoot();

    /**
     * Returns {@code true} if this is either (a) equal to the given
     * {@code potentialChildMailboxName} or (b) if this is an ancestor of the
     * given {@code potentialChildMailboxName}; {@code false} otherwise.
     * 
     * @param potentialChildMailboxName
     * @return true or false
     */
    boolean isEqualOrAncestorOf(MailboxName potentialChildMailboxName);

    /**
     * Creates a new instance of {@link MailboxName} using the top-most
     * {@code length} segments fond in this {@link MailboxName}.
     * 
     * @param length
     * @return
     * @throws ArrayIndexOutOfBoundsException
     *             iff <code>length < 0</code> or
     *             <code>length > getSegmentCount()</code>.
     */
    MailboxName prefix(int length);

    /**
     * If <code>relativeName.hasRoot()</code> is true, returns
     * <code>relativeName</code>. Otherwise returns a new instance of
     * {@link MailboxName} which is a result of concatenating the segments of
     * this {@link MailboxName} and the segments of the
     * <code>relativeName</code>.
     * 
     * @param relativeName
     * @return
     */
    MailboxName relative(MailboxName relativeName);

    /**
     * Creates a new instance of {@link MailboxName} using the bottom-most
     * {@code length} segments fond in this {@link MailboxName}.
     * 
     * @param length
     * @return
     */
    MailboxName suffix(int length);

    /**
     * Allows for iterating over the union of this {@link MailboxName} and its
     * ancestors in the "top first" order. Schematically, if this
     * {@link MailboxName} is "folder.subfolder.subsubfolder", the iterator
     * returns the {@link MailboxName}s in the following order: "folder",
     * "folder.subfolder", "folder.subfolder.subsubfolder".
     * 
     * @return an iterator
     */
    Iterator<MailboxName> topDownHierarchyLevels();

}

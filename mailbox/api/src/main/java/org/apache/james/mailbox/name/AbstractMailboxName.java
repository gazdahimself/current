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
import java.util.ListIterator;

/**
 * Presents a hierarchical mailbox name. The elements of the hierarchy are
 * called segments.
 * 
 * Implementors must guarantee that the underlying segments are unmodifiable
 * through any of their methods, esp. through the {@link Iterator}s returned by
 * some of their methods.
 * 
 * Implementtations must guarantee that segments are non-null and non-empty.
 * 
 */
public interface AbstractMailboxName {

    public static class ImmutableArrayIterator<E> implements ListIterator<E> {
    
        private int cursorPosition;
    
        private final E[] elements;
    
        private final int end;
    
        private final int start;
    
        public ImmutableArrayIterator(E[] elements, int start, int end) {
            super();
            this.elements = elements;
            this.start = start;
            this.cursorPosition = start;
            this.end = end;
        }
    
        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("The underlying collection is unmodifiable.");
        }
    
        @Override
        public boolean hasNext() {
            return cursorPosition < end;
        }
    
        @Override
        public boolean hasPrevious() {
            return cursorPosition > start;
        }
    
        @Override
        public E next() {
            if (!hasNext()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return elements[cursorPosition++];
        }
    
        @Override
        public int nextIndex() {
            return cursorPosition - start;
        }
    
        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return elements[--cursorPosition];
        }
    
        @Override
        public int previousIndex() {
            return cursorPosition - start - 1;
        }
    
        @Override
        public void remove() {
            throw new UnsupportedOperationException("The underlying collection is unmodifiable.");
        }
    
        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("The underlying collection is unmodifiable.");
        }
    
    }

    /**
     * An array containing a single empty string.
     */
    public static final String[] EMPTY_SEGMENTS = {};

    /**
     * TODO append.
     * 
     * @param relativeName
     * @return
     */
    AbstractMailboxName append(AbstractMailboxName relativeName);

    /**
     * Returns the segment at postion {@code index}. Valid indices are 0 to
     * {@link #getSegmentCount()} - 1.
     * 
     * @param index
     * @return
     */
    String getSegmentAt(int index);

    /**
     * Returns the number of segments in this {@link UnresolvedMailboxName}.
     * 
     * @return the segment count.
     */
    int getSegmentCount();

    /**
     * Equivalent with {@link #getSegmentCount()} == 0.
     * 
     * @return true if this {@link UnresolvedMailboxName} has zero segments,
     *         otherwise false.
     */
    boolean isEmpty();

    /**
     * Allows for iterating over the segments of this
     * {@link UnresolvedMailboxName} in the "top first" order. Schematically,
     * if this {@link UnresolvedMailboxName} is
     * "folder.subfolder.subsubfolder", the iterator returns the segments in the
     * following order: "folder", "subfolder", "subsubfolder".
     * 
     * Note that this method returns a {@link ListIterator} which allows for
     * both next() and previous() cursor movements.
     * 
     * @return a {@link ListIterator}
     */
    ListIterator<String> segmentsIterator();

}

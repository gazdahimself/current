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
import java.util.List;
import java.util.ListIterator;

/**
 * The default {@link MailboxName} implementation.
 * 
 * The segments are stored in a {@link String} array. For the given
 * {@link DefaultMailboxName} only a range of this array is relevant. This range
 * is defined by {@link #start} and {@link #end} attributes.
 * 
 * Multiple instances of {@link DefaultMailboxName} may rely on the same
 * segments String[] instance and they may define different ranges through their
 * {@link #start} and {@link #end} attributes. This makes it possible to
 * construct parent {@link MailboxName}s cheaply.
 * 
 * {@link DefaultMailboxName} is designed not to allow any modification of the
 * underlying {@link #segments} array or of {@link #start} and {@link #end}
 * attribute values.
 * 
 * The non-null and non-empty segments contract of {@link AbstractMailboxName}
 * is explicitly enforced only in {@link #DefaultMailboxName(List, boolean)}
 * which is the only public constructor taking segments. Other constructors 
 * 
 */
public final class DefaultMailboxName implements MailboxName {

    /**
     * A wrapper around an array containing the segments of this
     * {@link MailboxName}. Can be shared between {@link DefaultMailboxName}
     * instances. The reason for its existence is its ability to hold
     * {@link Segments#cachedMailboxOwner}.
     */
    protected static class Segments {
        public static final Segments EMPTY = new Segments(EMPTY_SEGMENTS);
        /**
         * Package-visible because we use it from
         * {@link DefaultMailboxNameResolver#getOwner(MailboxName)}. Do not use
         * it otherwise.
         */
        private MailboxOwner cachedMailboxOwner;
        private final String[] segments;

        public Segments(String[] segments) {
            super();
            this.segments = segments;
        }
    }

    public static class TopDownAncestorsAndThisIterator implements Iterator<MailboxName> {

        private int currentAncestorLength;
        private final DefaultMailboxName mailboxName;

        public TopDownAncestorsAndThisIterator(DefaultMailboxName mailboxName) {
            super();
            this.mailboxName = mailboxName;
            this.currentAncestorLength = 1;
        }

        @Override
        public boolean hasNext() {
            return currentAncestorLength <= mailboxName.getSegmentCount();
        }

        @Override
        public MailboxName next() {
            if (!hasNext()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return new DefaultMailboxName(this.mailboxName.segments, this.mailboxName.start, this.mailboxName.start + (currentAncestorLength++), this.mailboxName.hasRoot());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The underlying collection is unmodifiable.");
        }

    }

    public static final int INVALID_SEGMENT_COUNT = 0;

    /**
     * TODO checkSegments.
     * 
     * @param segments2
     * @return
     */
    protected static String[] checkSegments(List<String> segments) {
        String[] result = new String[segments.size()];
        int i = 0;
        for (String segment : segments) {
            if (segment == null) {
                throw new IllegalArgumentException("Cannot accept a null segment at position " + i + " to create a new " + DefaultMailboxName.class.getName());
            }
            if (segment.length() == 0) {
                throw new IllegalArgumentException("Cannot accept an empty string segment at position " + i + " to create a new " + DefaultMailboxName.class.getName());
            }
            result[i++] = segment;
        }
        return result;
    }

    /**
     * End of the range of the {@link #segments} array relevant for this
     * {@link DefaultMailboxName}. This attribute follows the
     * {@link String#substring(int, int)} convention meaning that {@link #end}
     * is "the ending index, exclusive".
     */
    private final int end;

    private int hashCode = 0;

    private final boolean hasRoot;

    /**
     * A wrapper around an array containing the segments of this
     * {@link MailboxName}. @see {@link #start} and {@link #end}.
     */
    protected final Segments segments;

    /**
     * End of the range of the {@link #segments} array relevant for this
     * {@link DefaultMailboxName}. This attribute follows the
     * {@link String#substring(int, int)} convention meaning that {@link #start}
     * is "the starting index, inclusive".
     */
    protected final int start;

    /**
     * Creates an empty {@link DefaultMailboxName}. Equivalent to new
     * DefaultMailboxName({@value #EMPTY_SEGMENTS}).
     * 
     */
    DefaultMailboxName() {
        super();
        this.segments = Segments.EMPTY;
        this.start = 0;
        this.end = 0;
        this.hasRoot = false;
    }

    /**
     * Creates a new instance of DefaultMailboxName.
     * 
     * @param maiboxName
     * @param inboxSegmentCount
     * @param hasRoot2
     * @param canonical2
     */
    public DefaultMailboxName(AbstractMailboxName maiboxName, boolean hasRoot) {
        if (maiboxName instanceof DefaultUnresolvedMailboxName) {
            DefaultUnresolvedMailboxName duqName = (DefaultUnresolvedMailboxName) maiboxName;
            this.segments = new Segments(duqName.segments);
            this.start = duqName.start;
            this.end = duqName.end;
        } else {
            String[] segs = new String[maiboxName.getSegmentCount()];
            ListIterator<String> segmentsIt = maiboxName.segmentsIterator();
            while (segmentsIt.hasNext()) {
                segs[segmentsIt.nextIndex()] = segmentsIt.next();
            }
            this.segments = new Segments(segs);
            this.start = 0;
            this.end = segs.length;
        }
        this.hasRoot = hasRoot;
    }

    /**
     * Creates a new instance of DefaultMailboxName.
     * 
     * @param segments
     * @param hasRoot
     * @param inboxSegmentCount
     * @param canonical
     */
    public DefaultMailboxName(List<String> segments, boolean hasRoot) {
        this(checkSegments(segments), hasRoot);
    }

    DefaultMailboxName(Segments segments, int start, int end, boolean hasRoot) {
        if (start < 0) {
            throw new IllegalArgumentException("Cannot create a new " + DefaultMailboxName.class.getName() + " with start < 0; found start = " + start + ".");
        }
        if (start >= end) {
            throw new IllegalArgumentException("Cannot create a new " + DefaultMailboxName.class.getName() + " with start >= end; found " + start + " >= " + end + ".");
        }
        this.segments = segments;
        this.start = start;
        this.end = end;
        this.hasRoot = hasRoot;
    }

    /**
     * Package private to be able to avoid the test of unmodifiability.
     * 
     * @param segments
     *            caller grants that segments will not get modified.
     */
    DefaultMailboxName(String[] segments, boolean hasRoot) {
        this.segments = new Segments(segments);
        this.start = 0;
        this.end = segments.length;
        this.hasRoot = hasRoot;
    }

    /**
     * Package private to be able to avoid the test of unmodifiability.
     * 
     * @param segments
     *            caller grants that segments will not get modified.
     * @param start
     * @param end
     * @param inboxSegmentCount
     * @param mailboxOwner
     */
    DefaultMailboxName(String[] segments, int start, int end, boolean hasRoot) {
        this(new Segments(segments), start, end, hasRoot);
    }

    /**
     * @see org.apache.james.mailbox.name.UnresolvedMailboxName#append(org.apache.james.mailbox.name.UnresolvedMailboxName)
     */
    @Override
    public AbstractMailboxName append(AbstractMailboxName relativeName) {
        String[] segments = new String[this.getSegmentCount() + relativeName.getSegmentCount()];
        int segmentsCount = 0;
        for (ListIterator<String> segmentsIt = segmentsIterator(); segmentsIt.hasNext();) {
            String segment = segmentsIt.next();
            if (segment.length() > 0) {
                segments[segmentsCount++] = segment;
            }
        }
        for (ListIterator<String> segmentsIt = relativeName.segmentsIterator(); segmentsIt.hasNext();) {
            String segment = segmentsIt.next();
            if (segment.length() > 0) {
                segments[segmentsCount++] = segment;
            }
        }
        return new DefaultUnresolvedMailboxName(segments, 0, segmentsCount);
    }

    @Override
    public MailboxName appendToLast(String suffix) {
        if (suffix == null) {
            throw new IllegalArgumentException("A non-null suffix needed to appendToLast.");
        } else if (suffix.length() == 0) {
            return this;
        } else {
            int segmentsCount = getSegmentCount();
            String[] segments = new String[segmentsCount];
            System.arraycopy(this.segments.segments, start, segments, 0, segmentsCount);
            segments[segmentsCount - 1] += suffix;
            return new DefaultMailboxName(segments, hasRoot());
        }
    }

    @Override
    public MailboxName child(String segment) {
        if (segment == null || segment.length() == 0) {
            throw new IllegalArgumentException("A non-null and non-empty segment needed to create a child of this "+ DefaultMailboxName.class.getName());
        }
        String[] segments = new String[getSegmentCount() + 1];
        ListIterator<String> segmentsIt = segmentsIterator();
        int i = 0;
        while (segmentsIt.hasNext()) {
            segments[i++] = segmentsIt.next();
        }
        segments[i] = segment;
        return new DefaultMailboxName(segments, hasRoot());
    }

    @Override
    public int compareTo(MailboxName other) {
        ListIterator<String> thisSegmentsIt = this.segmentsIterator();
        ListIterator<String> otherSegmentsIt = other.segmentsIterator();
        while (true) {
            boolean thisHasNext = thisSegmentsIt.hasNext();
            boolean otherHasNext = otherSegmentsIt.hasNext();

            if (thisHasNext && otherHasNext) {
                /* compare segments alphabetically */
                int segCompare = thisSegmentsIt.next().compareTo(otherSegmentsIt.next());
                if (segCompare != 0) {
                    return segCompare;
                } else {
                    /* go deeper in the next iteration */
                }
            } else if (!thisHasNext && !otherHasNext) {
                /* equal */
                return 0;
            } else if (otherHasNext) {
                /* this shorter than other */
                return -1;
            } else {
                /* other shorter than this */
                return 1;
            }
        }
    }

    protected int computeHashCode() {
        final int PRIME = 31;
        int result = 1;
        for (Iterator<String> it = segmentsIterator(); it.hasNext();) {
            String element = it.next();
            result = PRIME * result + (element == null ? 0 : element.hashCode());
        }
        result = PRIME * result + (hasRoot() ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MailboxName) {
            MailboxName other = (MailboxName) o;
            return this.hasRoot() == other.hasRoot() && DefaultUnresolvedMailboxName.equals(this, other);
        } else {
            return false;
        }
    }

    MailboxOwner getCachedOwner() {
        return segments.cachedMailboxOwner;
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#getParent()
     */
    @Override
    public MailboxName getParent() {
        if (getSegmentCount() <= 1) {
            return null;
        } else {
            return new DefaultMailboxName(segments, start, end - 1, hasRoot());
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#getSegmentAt(int)
     */
    @Override
    public String getSegmentAt(int index) {
        int shiftedIndex = index - start;
        if (shiftedIndex < 0 || shiftedIndex >= end) {
            throw new ArrayIndexOutOfBoundsException("Cannot access index " + index + " in " + DefaultMailboxName.class.getName() + " " + toString());
        }
        return segments.segments[shiftedIndex];
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#getSegmentCount()
     */
    @Override
    public int getSegmentCount() {
        return end - start;
    }

    /**
     * Lazy with harmless data race.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = this.hashCode;
        if (result == 0 && getSegmentCount() > 0) {
            result = computeHashCode();
            this.hashCode = result;
        }
        return result;
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#hasParent()
     */
    @Override
    public boolean hasParent() {
        return getSegmentCount() > 1;
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#hasRoot()
     */
    @Override
    public boolean hasRoot() {
        return hasRoot;
    }

    /**
     * @see org.apache.james.mailbox.name.UnresolvedMailboxName#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return getSegmentCount() == 0;
    }

    @Override
    public boolean isEqualOrAncestorOf(MailboxName potentialChildMailboxName) {
        if (this.getSegmentCount() > potentialChildMailboxName.getSegmentCount()) {
            return false;
        } else {
            ListIterator<String> thisSegmentsIt = this.segmentsIterator();
            ListIterator<String> childSegmentsIt = potentialChildMailboxName.segmentsIterator();
            while (thisSegmentsIt.hasNext()) {
                if (!thisSegmentsIt.next().equals(childSegmentsIt.next())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#prefix(int)
     */
    @Override
    public MailboxName prefix(int length) {
        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException("Cannot create a prefix with negative length " + length + ".");
        }
        if (length > getSegmentCount()) {
            throw new ArrayIndexOutOfBoundsException("Cannot create a prefix of length " + length + " out of " + this.toString() + " with length " + getSegmentCount());
        }
        int newEnd = start + length;
        if (length > 0) {
            return new DefaultMailboxName(segments, start, newEnd, hasRoot());
        } else {
            return MailboxName.EMPTY;
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#relative(org.apache.james.mailbox.name.MailboxName)
     */
    @Override
    public MailboxName relative(MailboxName relativeName) {
        if (relativeName.hasRoot()) {
            return relativeName;
        } else {
            return new DefaultMailboxName(append(relativeName), hasRoot());
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#segmentsIterator()
     */
    @Override
    public ListIterator<String> segmentsIterator() {
        return new ImmutableArrayIterator<String>(segments.segments, start, end);
    }

    void setCachedOwner(MailboxOwner owner) {
        segments.cachedMailboxOwner = owner;
    }

    @Override
    public MailboxName suffix(int length) {
        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException("Cannot create a suffix with negative length " + length + ".");
        }
        if (length > getSegmentCount()) {
            throw new ArrayIndexOutOfBoundsException("Cannot create a suffix of length " + length + " out of " + this.toString() + " with length " + getSegmentCount());
        }
        int newStart = end - length;
        boolean newHasRoot = newStart == start ? hasRoot() : false;
        return new DefaultMailboxName(segments, newStart, end, newHasRoot);
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#topDownHierarchyLevels()
     */
    @Override
    public Iterator<MailboxName> topDownHierarchyLevels() {
        return new TopDownAncestorsAndThisIterator(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(DefaultUnresolvedMailboxName.estimateSerializedLength(this));
        for (Iterator<String> it = segmentsIterator(); it.hasNext();) {
            sb.append(it.next()).append('.');
        }
        final int len = sb.length();
        if (len > 0) {
            sb.setLength(len - 1);
        }
        sb.append(hasRoot() ? "[rooted]" : "[rootless]");
        return sb.toString();
    }

}

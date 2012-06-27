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
 * The default {@link UnresolvedMailboxName} implementation.
 * 
 * The segments are stored in a {@link String} array. For the given
 * {@link DefaultUnresolvedMailboxName} only a range of this array is relevant.
 * This range is defined by {@link #start} and {@link #end} attributes.
 * 
 * Multiple instances of {@link DefaultMailboxName} may rely on the
 * same segments String[] instance and they may define different ranges through
 * their {@link #start} and {@link #end} attributes. This makes it possible to
 * construct parent {@link MailboxName}s cheaply.
 * 
 * {@link DefaultMailboxName} is designed not to allow any modification
 * of the underlying {@link #segments} array or of {@link #start} and
 * {@link #end} attribute values.
 */
public class DefaultUnresolvedMailboxName implements UnresolvedMailboxName {
    
    public static int HIGH_SEGMENT_LENGTH_GUESS = 12;  
    public static int LOW_SEGMENT_LENGTH_GUESS = 4;  

    public static int estimateSegmentsCount(String mailboxName) {
        return mailboxName.length() / LOW_SEGMENT_LENGTH_GUESS; 
    }
    public static int estimateSerializedLength(AbstractMailboxName mailboxName) {
        return mailboxName.getSegmentCount() * HIGH_SEGMENT_LENGTH_GUESS; 
    }

    /**
     * TODO segments.
     *
     * @param maiboxName
     * @return
     */
    private static String[] segments(AbstractMailboxName maiboxName) {
        if (maiboxName instanceof DefaultUnresolvedMailboxName) {
            return ((DefaultUnresolvedMailboxName) maiboxName).segments;
        }
        else {
            String[] result = new String[maiboxName.getSegmentCount()];
            ListIterator<String> segmentsIt = maiboxName.segmentsIterator();
            int i = 0;
            while (segmentsIt.hasNext()) {
                result[i++] = segmentsIt.next();
            }
            return result;
        }
    }

    /**
     * End of the range of the {@link #segments} array relevant for this
     * {@link DefaultMailboxName}. This attribute follows the
     * {@link String#substring(int, int)} convention meaning that {@link #end}
     * is "the ending index, exclusive".
     */
    protected final int end;
    private int hashCode = 0;
    /**
     * An array containing the segments of this {@link MailboxName}. @see
     * {@link #start} and {@link #end}.
     */
    protected final String[] segments;
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
    DefaultUnresolvedMailboxName() {
        this(EMPTY_SEGMENTS);
    }

    /**
     * Creates a new instance of DefaultMailboxName.
     * 
     * @param segments
     */
    public DefaultUnresolvedMailboxName(List<String> segments) {
        this(segments.toArray(new String[segments.size()]));
    }

    /**
     * Package private to be able to avoid the test of unmodifiability.
     * 
     * @param segments
     *            caller grants that segments will not get modified.
     */
    DefaultUnresolvedMailboxName(String[] segments) {
        this.segments = segments;
        this.start = 0;
        this.end = segments.length;
    }
    public DefaultUnresolvedMailboxName(String singleSegment) {
        this(new String[] {singleSegment});
    }

    /**
     * Package private to be able to avoid the test of unmodifiability.
     * 
     * @param segments
     *            caller grants that segments will not get modified.
     * @param start
     * @param end
     */
    DefaultUnresolvedMailboxName(String[] segments, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("Cannot create a new " + DefaultMailboxName.class.getName() + " with start < 0; found start = " + start + ".");
        }
        if (start >= end) {
            throw new IllegalArgumentException("Cannot create a new " + DefaultMailboxName.class.getName() + " with start >= end; found " + start + " >= " + end + ".");
        }
        this.segments = segments;
        this.start = start;
        this.end = end;
    }
    /**
     * Creates a new instance of DefaultUnresolvedMailboxName.
     *
     * @param maiboxName
     */
    public DefaultUnresolvedMailboxName(AbstractMailboxName maiboxName) {
        this(segments(maiboxName), maiboxName instanceof DefaultUnresolvedMailboxName ? ((DefaultUnresolvedMailboxName) maiboxName).start : 0, maiboxName instanceof DefaultUnresolvedMailboxName ? ((DefaultUnresolvedMailboxName) maiboxName).end : maiboxName.getSegmentCount());
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

    /**
     * TODO computeHashCode.
     *
     * @return
     */
    protected int computeHashCode() {
        final int PRIME = 31;
        int result = 1;
    
        for (Iterator<String> it = segmentsIterator(); it.hasNext();) {
            String element = it.next();
            result = PRIME * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof UnresolvedMailboxName) {
            return equals(this, (UnresolvedMailboxName) o);
        }
        return false;
    }
    
    public static boolean equals(AbstractMailboxName thisName, AbstractMailboxName other) {
        if (other.getSegmentCount() != thisName.getSegmentCount()) {
            return false;
        } else {
            Iterator<String> thisSegments = thisName.segmentsIterator();
            Iterator<String> otherSegments = other.segmentsIterator();
            while (thisSegments.hasNext() && otherSegments.hasNext()) {
                if (!thisSegments.next().equals(otherSegments.next())) {
                    return false;
                }
            }
            return true;
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
        return segments[shiftedIndex];
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
     * @see org.apache.james.mailbox.name.UnresolvedMailboxName#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return getSegmentCount() == 0;
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxName#segmentsIterator()
     */
    @Override
    public ListIterator<String> segmentsIterator() {
        return new ImmutableArrayIterator<String>(segments, start, end);
    }
    
    /**
     * For debug purposes only. With hard coded '.' delimiter and no escaping.
     */
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
        return sb.toString();
    }
    
}

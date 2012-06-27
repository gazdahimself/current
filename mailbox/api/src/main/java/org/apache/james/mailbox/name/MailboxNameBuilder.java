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
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Use this builder if you know how many segments your MailboxName will have.
 */
public final class MailboxNameBuilder {
    private int counter = 0;
    private String[] segments;

    public MailboxNameBuilder(int size) {
        super();
        segments = new String[size];
        counter = 0;
    }

    public MailboxNameBuilder add(String segment) {
        if (segment == null || segment.length() == 0) {
            throw new IllegalArgumentException("Cannot append an empty or null segment.");
        }
        if (segments == null) {
            throw new IllegalStateException("Segments need to be allocated first before you can append.");
        }
        if (counter >= segments.length) {
            throw new ArrayIndexOutOfBoundsException("Cannot append the " + counter + "th segment: allocated only for " + segments.length);
        }
        segments[counter++] = segment;
        return this;
    }

    /**
     * TODO addAll.
     *
     * @param inbox
     */
    public void addAll(MailboxName mailboxName) {
        ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
        while (segmentsIt.hasNext()) {
            add(segmentsIt.next());
        }
    }
    
    public void addAll(Collection<String> segments) {
        Iterator<String> segmentsIt = segments.iterator();
        while (segmentsIt.hasNext()) {
            add(segmentsIt.next());
        }
    }

    public MailboxName qualified(boolean hasRoot) {
        String[] segs = segments;
        int cnt = counter;
        /*
         * invalidate the segments pointer to be sure that it stays unmodifiable
         * after we return the MailboxName
         */
        segments = null;
        counter = 0;
        return new DefaultMailboxName(segs, 0, cnt, hasRoot);
    }

    public MailboxNameBuilder reallocate(int size) {
        if (segments != null) {
            throw new IllegalStateException("Segments allocated already.");
        }
        segments = new String[size];
        counter = 0;
        return this;
    }

    public UnresolvedMailboxName unqualified() {
        String[] segs = segments;
        int cnt = counter;
        /*
         * invalidate the segments pointer to be sure that it stays unmodifiable
         * after we return the MailboxName
         */
        segments = null;
        counter = 0;
        return new DefaultUnresolvedMailboxName(segs, 0, cnt);
    }
}

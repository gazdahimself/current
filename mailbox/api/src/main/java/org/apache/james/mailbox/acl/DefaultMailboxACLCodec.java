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

package org.apache.james.mailbox.acl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.james.mailbox.acl.MailboxACL.MailboxACLEntryKey;
import org.apache.james.mailbox.acl.MailboxACL.MailboxACLRights;
import org.apache.james.mailbox.exception.UnsupportedRightException;

/**
 * TODO PropertiesMailboxACLCodec.
 */
public class DefaultMailboxACLCodec implements MailboxACLCodec {
    
    private static class DelegatedProperties extends Properties {
        
        private static final long serialVersionUID = 6366250893902915087L;
        private final Map<MailboxACLEntryKey, MailboxACLRights> entries;

        public DelegatedProperties(Map<MailboxACLEntryKey, MailboxACLRights> entries) {
            super();
            this.entries = entries;
        }

        @Override
        public synchronized void clear() {
            entries.clear();
        }

        @Override
        public synchronized Object clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized boolean contains(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized boolean containsKey(Object key) {
            return entries.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return entries.containsValue(value);
        }

        @Override
        public synchronized Enumeration<Object> elements() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized boolean equals(Object o) {
            return entries.equals(o);
        }

        @Override
        public synchronized Object get(Object key) {
            return entries.get(new SimpleMailboxACL.SimpleMailboxACLEntryKey((String)key)).serialize();
        }

        @Override
        public synchronized int hashCode() {
            return entries.hashCode();
        }

        @Override
        public synchronized boolean isEmpty() {
            return entries.isEmpty();
        }

        @Override
        public synchronized Enumeration<Object> keys() {
            return new IteratorWrapper(entries.keySet().iterator());
        }

        @Override
        public Set<Object> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            try {
                return entries.put(new SimpleMailboxACL.SimpleMailboxACLEntryKey((String)key), new SimpleMailboxACL.Rfc4314Rights((String) value));
            } catch (UnsupportedRightException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void rehash() {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized int size() {
            return entries.size();
        }

        @Override
        public synchronized String toString() {
            return entries.toString();
        }

        @Override
        public Collection<Object> values() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static class IteratorWrapper implements Enumeration<Object> {
        private final Iterator<MailboxACLEntryKey> delegate;
        public IteratorWrapper(Iterator<MailboxACLEntryKey> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public boolean hasMoreElements() {
            return delegate.hasNext();
        }

        @Override
        public Object nextElement() {
            return delegate.next().serialize();
        }
        
    }


    private static final int HIGH_ENTRY_LENGTH = 32;
    private static final int LOW_ENTRY_LENGTH = 2;
    
    public static int estimateEntryHashtableSize(String entries) {
        return entries.length() / LOW_ENTRY_LENGTH;
    }

    @Override
    public MailboxACL decode(String acl) {
        if (acl == null || acl.length() == 0) {
            return SimpleMailboxACL.EMPTY;
        }
        Map<MailboxACLEntryKey, MailboxACLRights> entries = new HashMap<MailboxACLEntryKey, MailboxACLRights>(estimateEntryHashtableSize(acl));;
        DelegatedProperties props = new DelegatedProperties(entries);
        try {
            props.load(new StringReader(acl));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new SimpleMailboxACL(Collections.unmodifiableMap(entries), true);
    }

    @Override
    public String encode(MailboxACL acl) {
        if (acl == null) {
            return null;
        }
        else {
            Map<MailboxACLEntryKey, MailboxACLRights> entries = acl.getEntries();
            DelegatedProperties props = new DelegatedProperties(entries);
            StringWriter out = new StringWriter(estimateStringSize(entries));
            try {
                props.store(out, null);
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return out.toString();
        }
    }

    /**
     * TODO estimateStringSize.
     *
     * @param entries
     * @return
     */
    private int estimateStringSize(Map<MailboxACLEntryKey, MailboxACLRights> entries) {
        return entries.size() * HIGH_ENTRY_LENGTH;
    }

}

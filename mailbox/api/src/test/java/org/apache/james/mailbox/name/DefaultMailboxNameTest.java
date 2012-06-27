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

import junit.framework.Assert;

import org.junit.Test;

/**
 * DefaultMailboxNameResolverTest.
 */
public class DefaultMailboxNameTest {

    private static final String SEGMENT_0 = "folder0";
    private static final String SEGMENT_1 = "folder1";
    private static final String SEGMENT_2 = "folder2";

    @Test
    public void testGetParent() {
        String[] segments = new String[] {SEGMENT_0, SEGMENT_1, SEGMENT_2};
        DefaultMailboxName name = new DefaultMailboxName(segments, true);
        MailboxName parent = name.getParent();
        Assert.assertEquals(new DefaultMailboxName(new String[] {SEGMENT_0, SEGMENT_1}, true), parent);
        parent = parent.getParent();
        Assert.assertEquals(new DefaultMailboxName(new String[] {SEGMENT_0}, true), parent);
        parent = parent.getParent();
        Assert.assertNull(parent);
    }
    
    @Test
    public void testGetSegmentAt() {
        String[] segments = new String[] {SEGMENT_0, SEGMENT_1, SEGMENT_2};
        DefaultMailboxName subject = new DefaultMailboxName(segments, true);
        
        int i = 0;
        Assert.assertEquals(segments[i], subject.getSegmentAt(i));
        i++;
        Assert.assertEquals(segments[i], subject.getSegmentAt(i));
        i++;
        Assert.assertEquals(segments[i], subject.getSegmentAt(i));
        i++;
        
        boolean arrayIndexOutOfBoundsExceptionThrown = false;
        try {
            subject.getSegmentAt(i);
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        } catch (ArrayIndexOutOfBoundsException e) {
            arrayIndexOutOfBoundsExceptionThrown = true;
        }
        if (!arrayIndexOutOfBoundsExceptionThrown) {
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        }
        
    }
    
    @Test
    public void testTopDownHierarchyLevels() {
        String[] segments = new String[] {SEGMENT_0, SEGMENT_1, SEGMENT_2};
        DefaultMailboxName subject = new DefaultMailboxName(segments, true);
        
        Iterator<MailboxName> it = subject.topDownHierarchyLevels();
        Assert.assertEquals(new DefaultMailboxName(new String[] {SEGMENT_0}, true), it.next());
        Assert.assertEquals(new DefaultMailboxName(new String[] {SEGMENT_0, SEGMENT_1}, true), it.next());
        Assert.assertEquals(new DefaultMailboxName(new String[] {SEGMENT_0, SEGMENT_1, SEGMENT_2}, true), it.next());
        
        boolean arrayIndexOutOfBoundsExceptionThrown = false;
        try {
            it.next();
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        } catch (ArrayIndexOutOfBoundsException e) {
            arrayIndexOutOfBoundsExceptionThrown = true;
        }
        if (!arrayIndexOutOfBoundsExceptionThrown) {
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        }
        
    }

    
    @Test
    public void testTopDownSegmets() {
        String[] segments = new String[] {SEGMENT_0, SEGMENT_1, SEGMENT_2};
        DefaultMailboxName subject = new DefaultMailboxName(segments, true);
        
        int i = 0;
        Iterator<String> it = subject.segmentsIterator();
        Assert.assertEquals(segments[i++], it.next());
        Assert.assertEquals(segments[i++], it.next());
        Assert.assertEquals(segments[i++], it.next());
        
        boolean arrayIndexOutOfBoundsExceptionThrown = false;
        try {
            it.next();
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        } catch (ArrayIndexOutOfBoundsException e) {
            arrayIndexOutOfBoundsExceptionThrown = true;
        }
        if (!arrayIndexOutOfBoundsExceptionThrown) {
            Assert.fail(ArrayIndexOutOfBoundsException.class.getName() +" expected.");
        }
        
    }

}

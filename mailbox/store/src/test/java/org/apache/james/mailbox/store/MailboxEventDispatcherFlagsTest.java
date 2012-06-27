/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.MailboxACL;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.util.EventCollector;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class MailboxEventDispatcherFlagsTest {

    MailboxEventDispatcher<Long> dispatcher;

    EventCollector collector;

    MessageResult result;
    int sessionId = 10;

    private MailboxSession session = new MockMailboxSession("test", DefaultMailboxNameResolver.INSTANCE) {

        @Override
        public long getSessionId() {
            return sessionId;
        }
        
    };

    private Mockery mockery = new JUnit4Mockery();

    private Mailbox<Long> mailbox = new Mailbox<Long>() {
        
        private MailboxName mailboxName = new MailboxNameBuilder(2).add("#users").add("test").qualified(true);

        @Override
        public Long getMailboxId() {
            return 1L;
        }

        @Override
        public String getUser() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void setUser(String user) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public long getUidValidity() {
            return 0;
        }
        
        @Override
        public MailboxACL getACL() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void setACL(MailboxACL acl) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public MailboxName getMailboxName() {
            return mailboxName;
        }

        @Override
        public void setMailboxName(MailboxName mailboxName) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean isOwnerGroup() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void setOwnerGroup(boolean ownerGroup) {
            throw new UnsupportedOperationException("Not supported");
        }

        
    };
    
    @Before
    public void setUp() throws Exception {
        collector = new EventCollector();

        dispatcher = new MailboxEventDispatcher<Long>(collector);
        result = mockery.mock(MessageResult.class);
        mockery.checking(new Expectations() {{
            allowing(result).getUid();will(returnValue(23L));
        }});
    }


    @Test
    public void testShouldReturnNoChangesWhenSystemFlagsUnchanged() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1,  new Flags(
                Flags.Flag.DELETED), new Flags(Flags.Flag.DELETED))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowAnsweredAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.ANSWERED))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowAnsweredRemoved() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.ANSWERED), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowDeletedAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.DELETED))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DELETED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowDeletedRemoved() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.DELETED), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DELETED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowDraftAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.DRAFT))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DRAFT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowDraftRemoved() {
        dispatcher.flagsUpdated(session,Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.DRAFT), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DRAFT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowFlaggedAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.FLAGGED))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.FLAGGED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowFlaggedRemoved() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.FLAGGED), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.FLAGGED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowRecentAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.RECENT))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowRecentRemoved() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.RECENT), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowSeenAdded() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(),
                new Flags(Flags.Flag.SEEN))));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowSeenRemoved() {
        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, new Flags(
                Flags.Flag.SEEN), new Flags())));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldShowMixedChanges() {
        Flags originals = new Flags();
        originals.add(Flags.Flag.DRAFT);
        originals.add(Flags.Flag.RECENT);
        Flags updated = new Flags();
        updated.add(Flags.Flag.ANSWERED);
        updated.add(Flags.Flag.DRAFT);
        updated.add(Flags.Flag.SEEN);

        dispatcher.flagsUpdated(session, Arrays.asList(result.getUid()), mailbox, Arrays.asList(new UpdatedFlags(result.getUid(), -1, originals, updated)));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator<Flags.Flag> iterator = event.getUpdatedFlags().get(0).systemFlagIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }
}

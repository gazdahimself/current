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
package org.apache.james.mailbox.hbase.mail.model;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.junit.Test;

/**
 * Unit tests for HBaseMailbox class.
 */
public class HBaseMailboxTest {
    

    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final String TEST_USER;
    private static final MailboxOwner TEST_OWNER;
    private static final MailboxName TEST_INBOX;
    static {
        TEST_USER = "ieugen";
        TEST_OWNER = MAILBOX_NAME_RESOLVER.getOwner(TEST_USER, false);
        TEST_INBOX = MAILBOX_NAME_RESOLVER.getInbox(TEST_OWNER);
    }

    /**
     * Test of getter and setter for MailboxId
     */
    @Test
    public void testGetSetMailboxId() {
        System.out.println("getSetMailboxId");
        final HBaseMailbox instance = new HBaseMailbox(TEST_INBOX, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 10);

        UUID expResult = UUID.randomUUID();
        instance.setMailboxId(expResult);
        assertEquals(expResult, instance.getMailboxId());

    }

    /**
     * Test of getter and setter for Namespace, of class HBaseMailbox.
     */
    @Test
    public void testGetSetMailboxName() {
        System.out.println("getSetNamespace");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 124566);
        MailboxName result = instance.getMailboxName();
        assertEquals(mailboxPath, result);

        MailboxName newName = mailboxPath.child("new");
        instance.setMailboxName(newName);
        assertEquals(newName, instance.getMailboxName());

    }

    /**
     * Test of getter and setter for User, of class HBaseMailbox.
     */
    @Test
    public void testGetSetUser() {
        System.out.println("getUser");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 12);
        String result = instance.getUser();
        assertEquals(TEST_OWNER.getName(), result);

        instance.setUser("eric");
        assertEquals("eric", instance.getUser());
    }

    /**
     * Test of getUidValidity method, of class HBaseMailbox.
     */
    @Test
    public void testGetUidValidity() {
        System.out.println("getUidValidity");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 123345);
        long expResult = 123345L;
        long result = instance.getUidValidity();
        assertEquals(expResult, result);
    }

    /**
     * Test of hashCode method, of class HBaseMailbox.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 1234);
        // from the hashCode()
        final int PRIME = 31;
        int result = 1;
        UUID mailboxId = instance.getMailboxId();
        int expResult = PRIME * result + (int) (mailboxId.getMostSignificantBits() ^ (mailboxId.getMostSignificantBits() >>> 32));

        assertEquals(expResult, instance.hashCode());
    }

    /**
     * Test of equals method, of class HBaseMailbox.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 12345);
        final HBaseMailbox instance2 = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 12345);
        instance2.setMailboxId(instance.getMailboxId());
        assertEquals(instance, instance2);
    }

    /**
     * Test of consumeUid method, of class HBaseMailbox.
     */
    @Test
    public void testConsumeUid() {
        System.out.println("consumeUid");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 10);
        long expResult = instance.getLastUid() + 1;
        long result = instance.consumeUid();
        assertEquals(expResult, result);
    }

    /**
     * Test of consumeModSeq method, of class HBaseMailbox.
     */
    @Test
    public void testConsumeModSeq() {
        System.out.println("consumeModSeq");
        final MailboxName mailboxPath = TEST_INBOX;
        final HBaseMailbox instance = new HBaseMailbox(mailboxPath, TEST_OWNER.getName(), TEST_OWNER.isGroup(), 10);
        long expResult = instance.getHighestModSeq() + 1;
        long result = instance.consumeModSeq();
        assertEquals(expResult, result);
    }
}

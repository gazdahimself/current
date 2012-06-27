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
package org.apache.james.mailbox.hbase.mail;

import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOXES;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOXES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOX_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_META_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGE_DATA_BODY_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGE_DATA_HEADERS_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTIONS;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTIONS_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTION_CF;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for HBaseMessageMapper.
 *
 */
public class HBaseMessageMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseMailboxMapperTest.class);
    private static HBaseClusterSingleton cluster;
    private static HBaseUidProvider uidProvider;
    private static HBaseModSeqProvider modSeqProvider;
    private static HBaseMessageMapper mapper;
    private static final List<MailboxName> MBOX_PATHS = new ArrayList<MailboxName>();
    private static final List<Mailbox<UUID>> MBOXES = new ArrayList<Mailbox<UUID>>();
    private static final List<Message<UUID>> MESSAGE_NO = new ArrayList<Message<UUID>>();
    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final int COUNT = 5;
    private static Configuration conf;
    /*
     * we mock a simple message content
     */
    private static final byte[] messageTemplate = Bytes.toBytes(
            "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\n"
            + "From: Fred Foobar <foobar@Blurdybloop.COM>\n"
            + "Subject: Test 02\n"
            + "To: mooch@owatagu.siam.edu\n"
            + "Message-Id: <B27397-0100000@Blurdybloop.COM>\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\n"
            + "\n"
            + "Test\n"
            + "\n.");
    private static SharedInputStream content = new SharedByteArrayInputStream(messageTemplate);
    private static final String USER_PREFIX = "user";
    /**
     * 
     */
    private static final String SUB = "sub";
    
    @BeforeClass
    public static void setUp() throws Exception {
        
        cluster = HBaseClusterSingleton.build();
        ensureTables();
        clearTables();
        conf = cluster.getConf();
        uidProvider = new HBaseUidProvider(conf);
        modSeqProvider = new HBaseModSeqProvider(conf);
        generateTestData();
        final MailboxSession session = new MockMailboxSession("ieugen", MAILBOX_NAME_RESOLVER);
        mapper = new HBaseMessageMapper(session, uidProvider, modSeqProvider, conf);
        
        for (int i = 0; i < MESSAGE_NO.size(); i++) {
            mapper.add(MBOXES.get(1), MESSAGE_NO.get(i));
        }
    }

    /**
     * Test an ordered scenario with count, find, add... methods.
     *
     * @throws Exception
     */
    @Test
    public void testMessageMapperScenario() throws Exception {
        testCountMessagesInMailbox();
        testCountUnseenMessagesInMailbox();
        testFindFirstUnseenMessageUid();
        testFindRecentMessageUidsInMailbox();
        testAdd();
        testGetLastUid();
        testGetHighestModSeq();
    }

    /**
     * Test of countMessagesInMailbox method, of class HBaseMessageMapper.
     */
    private void testCountMessagesInMailbox() throws Exception {
        LOG.info("countMessagesInMailbox");
        long messageCount = mapper.countMessagesInMailbox(MBOXES.get(1));
        assertEquals(MESSAGE_NO.size(), messageCount);
    }

    /**
     * Test of countUnseenMessagesInMailbox method, of class HBaseMessageMapper.
     */
    private void testCountUnseenMessagesInMailbox() throws Exception {
        LOG.info("countUnseenMessagesInMailbox");
        long unseen = mapper.countUnseenMessagesInMailbox(MBOXES.get(1));
        assertEquals(MESSAGE_NO.size() - 1, unseen);
    }

    /**
     * Test of findFirstUnseenMessageUid method, of class HBaseMessageMapper.
     */
    private void testFindFirstUnseenMessageUid() throws Exception {
        LOG.info("findFirstUnseenMessageUid");
        final long uid = mapper.findFirstUnseenMessageUid(MBOXES.get(1));
        assertEquals(1, uid);
    }

    /**
     * Test of findRecentMessageUidsInMailbox method, of class
     * HBaseMessageMapper.
     */
    private void testFindRecentMessageUidsInMailbox() throws Exception {
        LOG.info("findRecentMessageUidsInMailbox");
        List<Long> recentMessages = mapper.findRecentMessageUidsInMailbox(MBOXES.get(1));
        assertEquals(MESSAGE_NO.size() - 1, recentMessages.size());
    }

    /**
     * Test of add method, of class HBaseMessageMapper.
     */
    private void testAdd() throws Exception {
        LOG.info("add");
        // The tables should be deleted every time the tests run.
        long msgCount = mapper.countMessagesInMailbox(MBOXES.get(1));
        LOG.info(msgCount + " " + MESSAGE_NO.size());
        assertEquals(MESSAGE_NO.size(), msgCount);
    }

    /**
     * Test of getLastUid method, of class HBaseMessageMapper.
     */
    private void testGetLastUid() throws Exception {
        LOG.info("getLastUid");
        long lastUid = mapper.getLastUid(MBOXES.get(1));
        assertEquals(MESSAGE_NO.size(), lastUid);
    }

    /**
     * Test of getHighestModSeq method, of class HBaseMessageMapper.
     */
    private void testGetHighestModSeq() throws Exception {
        LOG.info("getHighestModSeq");
        long highestModSeq = mapper.getHighestModSeq(MBOXES.get(1));
        assertEquals(MESSAGE_NO.size(), highestModSeq);
    }

    private static void ensureTables() throws IOException {
        cluster.ensureTable(MAILBOXES_TABLE, new byte[][]{MAILBOX_CF});
        cluster.ensureTable(MESSAGES_TABLE,
                new byte[][]{MESSAGES_META_CF, MESSAGE_DATA_HEADERS_CF, MESSAGE_DATA_BODY_CF});
        cluster.ensureTable(SUBSCRIPTIONS_TABLE, new byte[][]{SUBSCRIPTION_CF});
    }

    private static void clearTables() {
        cluster.clearTable(MAILBOXES);
        cluster.clearTable(MESSAGES);
        cluster.clearTable(SUBSCRIPTIONS);
    }
    
    public static void generateTestData() {
        final Random random = new Random();
        MailboxName mboxPath;
        final PropertyBuilder propBuilder = new PropertyBuilder();

        for (int i = 0; i < COUNT; i++) {
            MailboxOwner owner = MAILBOX_NAME_RESOLVER.getOwner(USER_PREFIX + i, false);
            MailboxName inbox = MAILBOX_NAME_RESOLVER.getInbox(owner);

            if (i % 2 == 0) {
                mboxPath = inbox;
            } else {
                mboxPath = inbox.child(SUB + i);
            }
            MBOX_PATHS.add(mboxPath);
            MBOXES.add(new HBaseMailbox(mboxPath, owner.getName(), owner.isGroup(), random.nextLong()));
            propBuilder.setProperty("gsoc", "prop" + i, "value");
        }
        propBuilder.setMediaType("text");
        propBuilder.setSubType("html");
        propBuilder.setTextualLineCount(2L);

        SimpleMessage<UUID> myMsg;
        final Flags flags = new Flags(Flags.Flag.RECENT);
        final Date today = new Date();

        for (int i = 0; i < COUNT * 2; i++) {
            myMsg = new SimpleMessage<UUID>(today, messageTemplate.length,
                    messageTemplate.length - 20, content, flags, propBuilder,
                    MBOXES.get(1).getMailboxId());
            if (i == COUNT * 2 - 1) {
                flags.add(Flags.Flag.SEEN);
                flags.remove(Flags.Flag.RECENT);
                myMsg.setFlags(flags);
            }
            MESSAGE_NO.add(myMsg);
        }
    }
}

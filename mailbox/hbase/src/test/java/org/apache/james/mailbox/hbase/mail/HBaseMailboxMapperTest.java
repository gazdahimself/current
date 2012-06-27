/**
 * **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one * or more
 * contributor license agreements. See the NOTICE file * distributed with this
 * work for additional information * regarding copyright ownership. The ASF
 * licenses this file * to you under the Apache License, Version 2.0 (the *
 * "License"); you may not use this file except in compliance * with the
 * License. You may obtain a copy of the License at * *
 * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable
 * law or agreed to in writing, * software distributed under the License is
 * distributed on an * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY *
 * KIND, either express or implied. See the License for the * specific language
 * governing permissions and limitations * under the License. *
 * **************************************************************
 */
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
import static org.apache.james.mailbox.hbase.HBaseUtils.mailboxFromResult;
import static org.apache.james.mailbox.hbase.HBaseUtils.mailboxRowKey;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IOUtils;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.hbase.io.ChunkInputStream;
import org.apache.james.mailbox.hbase.io.ChunkOutputStream;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HBaseMailboxMapper unit tests.
 *
 */
public class HBaseMailboxMapperTest {

    /**
     * 
     */
    private static final String SUB = "sub";
    private static final Logger LOG = LoggerFactory.getLogger(HBaseMailboxMapperTest.class);
    private static HBaseClusterSingleton cluster;
    private static Configuration conf;
    private static HBaseMailboxMapper mapper;
    private static List<HBaseMailbox> mailboxList;
    private static List<MailboxName> mailboxNames;
    private static final int USER_COUNT = 2;
    private static final int DOMAIN_COUNT = 3;
    private static final int MAILBOX_COUNT = 3;
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.SAFE_STORE_NAME_CODEC;
    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final String USER_PREFIX = "user";
    private static final String GROUP_PREFIX = "group";
    private static final MailboxName CATCH_ALL_PATTERN = new MailboxNameBuilder(1).add(MailboxQuery.FREEWILDCARD_STRING).qualified(true);
    private static final MailboxName USERS_ROOT_NAME = new MailboxNameBuilder(1).add("#users").qualified(true);
    private static final MailboxName USER0_INBOX = MAILBOX_NAME_RESOLVER.getInbox(USER_PREFIX + 0);
    private static final MailboxName USER0_INBOX_SUB0_SUB1_SUB2 = MAILBOX_NAME_RESOLVER.getInbox(USER_PREFIX + 0).child(SUB + 0).child(SUB + 1).child(SUB + 2);


    @BeforeClass
    public static void setUp() throws Exception {
        cluster = HBaseClusterSingleton.build();
        ensureTables();
        clearTables();
        conf = cluster.getConf();
        fillMailboxList();
        mapper = new HBaseMailboxMapper(conf, MAILBOX_NAME_RESOLVER, MAILBOX_NAME_CODEC);
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

    /**
     * Test of save method, of class HBaseMailboxMapper.
     */
    @Test
    public void testSave() throws Exception {
        
        final HTable mailboxes = new HTable(conf, MAILBOXES_TABLE);
        
        for (HBaseMailbox mlbx : mailboxList) {
            mapper.save(mlbx);
            
    
            final Get get = new Get(mailboxRowKey(mlbx.getMailboxId()));
            // get all columns for the DATA column family
            get.addFamily(MAILBOX_CF);
    
            final Result result = mailboxes.get(get);
            final HBaseMailbox newValue = (HBaseMailbox) mailboxFromResult(result, MAILBOX_NAME_CODEC);
            assertEquals(mlbx, newValue);
            assertEquals(mlbx.getUser(), newValue.getUser());
            assertEquals(mlbx.getMailboxName(), newValue.getMailboxName());
            assertEquals(mlbx.getMailboxId(), newValue.getMailboxId());
            assertEquals(mlbx.getLastUid(), newValue.getLastUid());
            assertEquals(mlbx.getUidValidity(), newValue.getUidValidity());
            assertEquals(mlbx.getHighestModSeq(), newValue.getHighestModSeq());
            assertArrayEquals(mailboxRowKey(mlbx.getMailboxId()), mailboxRowKey(newValue.getMailboxId()));
        }
    
        IOUtils.closeStream(mailboxes);
        
    }

    /**
     * Test of findMailboxByPath method, of class HBaseMailboxMapper.
     */
    @Test
    public void testFindMailboxByPath() throws Exception {
        LOG.info("findMailboxByPath");
        HBaseMailbox mailbox;
        for (MailboxName path : mailboxNames) {
            LOG.info("Searching for " + path);
            mailbox = (HBaseMailbox) mapper.findMailboxByPath(path);
            assertEquals(path, mailbox.getMailboxName());
        }
    }


    /**
     * Test of findMailboxWithPathLike method, of class HBaseMailboxMapper.
     */
    @Test
    public void testFindMailboxWithPathLike() throws Exception {
        LOG.info("findMailboxWithPathLike");
        MailboxName path;
        List<Mailbox<UUID>> result;
        
        path = MAILBOX_NAME_RESOLVER.getInbox(USER_PREFIX + 0).appendToLast(MailboxQuery.FREEWILDCARD_STRING);
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(MAILBOX_COUNT + 1, result.size());
        
        path = USER0_INBOX.child(MailboxQuery.FREEWILDCARD_STRING);
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(MAILBOX_COUNT, result.size());
    
    
        path = MAILBOX_NAME_RESOLVER.getInbox(USER_PREFIX + 0).child(MailboxQuery.LOCALWILDCARD_STRING);
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(1, result.size());
        
        path = CATCH_ALL_PATTERN;
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(mailboxNames.size(), result.size());
    
        
        path = USERS_ROOT_NAME.child(MailboxQuery.FREEWILDCARD_STRING);
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(USER_COUNT * (MAILBOX_COUNT + 1), result.size());
    
        path = USERS_ROOT_NAME.appendToLast(MailboxQuery.FREEWILDCARD_STRING);
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(USER_COUNT * (MAILBOX_COUNT + 1), result.size());
    
    }

    /**
     * Test of hasChildren method, of class HBaseMailboxMapper.
     */
    @Test
    public void testHasChildren() throws Exception {
        MailboxName mailboxName;
        MailboxOwner owner;
        HBaseMailbox mailbox;

        mailboxName = USERS_ROOT_NAME;
        mailbox = new HBaseMailbox(mailboxName, null, false, 12455);
        assertTrue(mapper.hasChildren(mailbox));

        mailboxName = USER0_INBOX;
        owner = MAILBOX_NAME_RESOLVER.getOwner(mailboxName);
        mailbox = new HBaseMailbox(mailboxName, owner.getName(), owner.isGroup(), 12455);
        assertTrue(mapper.hasChildren(mailbox));

        mailboxName = USER0_INBOX_SUB0_SUB1_SUB2;
        owner = MAILBOX_NAME_RESOLVER.getOwner(mailboxName);
        mailbox = new HBaseMailbox(mailboxName, owner.getName(), owner.isGroup(), 12455);
        assertFalse(mapper.hasChildren(mailbox));

    }

    /**
     * Test of list method, of class HBaseMailboxMapper.
     */
    @Test
    public void testList() throws Exception {
        LOG.info("list");
        List<Mailbox<UUID>> result = mapper.list();
        assertEquals(mailboxList.size(), result.size());
    }
    
    /**
     * Test an ordered scenario with list, delete... methods.
     *
     * @throws Exception
     */
    public void testMailboxMapperScenario() throws Exception {
        testSave();
        testDelete();
//        testDeleteAllMemberships(); // Ignore this test
        testDeleteAllMailboxes();
        testChunkStream();
    }

    /**
     * Test of delete method, of class HBaseMailboxMapper.
     */
    private void testDelete() throws Exception {
        LOG.info("delete");
        // delete last 5 mailboxes from mailboxList
        int offset = 5;
        int notFoundCount = 0;

        Iterator<HBaseMailbox> iterator = mailboxList.subList(mailboxList.size() - offset, mailboxList.size()).iterator();

        while (iterator.hasNext()) {
            HBaseMailbox mailbox = iterator.next();
            mapper.delete(mailbox);
            iterator.remove();
            MailboxName path = mailbox.getMailboxName();
            mailboxNames.remove(path);
            LOG.info("Removing mailbox: {}", path);
            try {
                mapper.findMailboxByPath(path);
            } catch (MailboxNotFoundException e) {
                LOG.info("Succesfully removed {}", mailbox);
                notFoundCount++;
            }
        }
        assertEquals(offset, notFoundCount);
        assertEquals(mailboxList.size(), mapper.list().size());
    }


    /**
     * Test of deleteAllMemberships method, of class HBaseMailboxMapper.
     */
    private void testDeleteAllMemberships() {
        LOG.info("deleteAllMemberships");
        fail("Not yet implemented");
    }

    /**
     * Test of deleteAllMailboxes method, of class HBaseMailboxMapper.
     */
    private void testDeleteAllMailboxes() throws MailboxException {
        LOG.info("deleteAllMailboxes");
        mapper.deleteAllMailboxes();
        assertEquals(0, mapper.list().size());
        fillMailboxList();
    }

    private void testChunkStream() throws IOException {
        LOG.info("Checking ChunkOutpuStream and ChunkInputStream");
        final String original = "This is a proper test for the HBase ChunkInputStream and"
                + "ChunkOutputStream. This text must be larger than the chunk size so we write"
                + "and read more then one chunk size. I think that a few more lore ipsum lines"
                + "will be enough."
                + "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor "
                + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis "
                + "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu "
                + "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa"
                + " qui officia deserunt mollit anim id est laborum";
        byte[] data = Bytes.toBytes(original);
        // we make the column size = 10 bytes
        ChunkOutputStream out = new ChunkOutputStream(conf,
                MESSAGES_TABLE, MESSAGE_DATA_BODY_CF, Bytes.toBytes("10"), 10);
        ChunkInputStream in = new ChunkInputStream(conf,
                MESSAGES_TABLE, MESSAGE_DATA_BODY_CF, Bytes.toBytes("10"));
        //create the stream
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(data.length);
        int b;
        while ((b = bin.read()) != -1) {
            out.write(b);
        }
        out.close();
        while ((b = in.read()) != -1) {
            bout.write(b);
        }
        String s = bout.toString();
        assertTrue(original.equals(s));
    }

    private static void fillMailboxList() {
        mailboxList = new ArrayList<HBaseMailbox>();
        mailboxNames = new ArrayList<MailboxName>();
        
        for (int j = 0; j < USER_COUNT; j++) {
            for (boolean isGroup : new boolean[] {false, true}) {
                String prefix = isGroup ? GROUP_PREFIX : USER_PREFIX;
                String user = prefix + j;
                MailboxOwner owner = MAILBOX_NAME_RESOLVER.getOwner(user, isGroup);
                MailboxName mailboxName = MAILBOX_NAME_RESOLVER.getInbox(owner);
                mailboxNames.add(mailboxName);
                mailboxList.add(new HBaseMailbox(mailboxName, user, isGroup,  mailboxList.size()));
                
                for (int k = 0; k < MAILBOX_COUNT; k++) {
                    mailboxName = mailboxName.child(SUB+ k);
                    mailboxNames.add(mailboxName);
                    mailboxList.add(new HBaseMailbox(mailboxName, user, isGroup,  mailboxList.size()));
                }
            }
        }
        
        for (int i = 0; i < DOMAIN_COUNT; i++) {
            String domain = "domain"+i;
            for (int j = 0; j < USER_COUNT; j++) {
                for (boolean isGroup : new boolean[] {false, true}) {
                    String prefix = isGroup ? GROUP_PREFIX : USER_PREFIX;
                    String user = prefix + j + MailboxConstants.AT + domain;
                    MailboxOwner owner = MAILBOX_NAME_RESOLVER.getOwner(user, isGroup);
                    MailboxName mailboxName = MAILBOX_NAME_RESOLVER.getInbox(owner);
                    mailboxNames.add(mailboxName);
                    mailboxList.add(new HBaseMailbox(mailboxName, user, isGroup,  mailboxList.size()));
                    
                    for (int k = 0; k < MAILBOX_COUNT; k++) {
                        mailboxName = mailboxName.child(SUB+ k);
                        mailboxNames.add(mailboxName);
                        mailboxList.add(new HBaseMailbox(mailboxName, user, isGroup,  mailboxList.size()));
                    }
                }
            }
        }
        
        LOG.info("Created test case with {} mailboxes and {} paths",
                mailboxList.size(), mailboxNames.size());
    }

}

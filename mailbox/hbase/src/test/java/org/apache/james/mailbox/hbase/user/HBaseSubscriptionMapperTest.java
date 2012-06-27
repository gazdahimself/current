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
package org.apache.james.mailbox.hbase.user;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.hbase.HBaseMailboxSessionMapperFactory;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.user.model.Subscription;
import org.apache.james.mailbox.store.user.model.impl.SimpleSubscription;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs tests for SubscriptionMapper.
 *
 */
public class HBaseSubscriptionMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseSubscriptionMapperTest.class);
    private static final HBaseClusterSingleton CLUSTER = HBaseClusterSingleton.build();
    private static Configuration conf;
    private static HBaseMailboxSessionMapperFactory mapperFactory;
    private static HBaseSubscriptionMapper mapper;
    private static Map<String, List<SimpleSubscription>> subscriptionMap;
    private static final int USERS = 5;
    private static final int MAILBOX_NO = 5;
    private static final MailboxNameCodec MAILBOX_NAME_CODEC = MailboxNameCodec.SAFE_STORE_NAME_CODEC;
    private static final MailboxNameResolver MAILBOX_NAME_RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    private static final String FAKE_USER_0;
    private static final String FAKE_USER_1;

    static {
        FAKE_USER_0 = "fake0";
        FAKE_USER_1 = "fake1";
    }

    @Before
    public void setUp() throws Exception {
        ensureTables();
        clearTables();
        conf = CLUSTER.getConf();
        mapperFactory = new HBaseMailboxSessionMapperFactory(conf, null, null);
        mapper = new HBaseSubscriptionMapper(conf, MAILBOX_NAME_CODEC);
        fillSubscriptionList();
    }

    private void ensureTables() throws IOException {
        CLUSTER.ensureTable(MAILBOXES_TABLE, new byte[][]{MAILBOX_CF});
        CLUSTER.ensureTable(MESSAGES_TABLE,
                new byte[][]{MESSAGES_META_CF, MESSAGE_DATA_HEADERS_CF, MESSAGE_DATA_BODY_CF});
        CLUSTER.ensureTable(SUBSCRIPTIONS_TABLE, new byte[][]{SUBSCRIPTION_CF});
    }

    private void clearTables() {
        CLUSTER.clearTable(MAILBOXES);
        CLUSTER.clearTable(MESSAGES);
        CLUSTER.clearTable(SUBSCRIPTIONS);
    }

    private static void fillSubscriptionList() throws SubscriptionException {
        LOG.info("Creating subscription list");
        SimpleSubscription subscription;
        String user;
        subscriptionMap = new HashMap<String, List<SimpleSubscription>>();
        for (int i = 0; i < USERS; i++) {
            user = "user" + i;
            final List<SimpleSubscription> mailboxes = new ArrayList<SimpleSubscription>();
            subscriptionMap.put(user, mailboxes);
            MailboxName inbox = MAILBOX_NAME_RESOLVER.getInbox(user);

            for (int j = 0; j < MAILBOX_NO; j++) {
                final MailboxName mailbox;
                if (j == 0) {
                    mailbox = inbox;
                } else {
                    mailbox = inbox.child("BOX" + j);
                }
                if ((i % 2 == 0) && (j > 0)) {
                    continue;
                }
                subscription = new SimpleSubscription(user, mailbox);
                mailboxes.add(subscription);
                mapper.save(subscription);
                LOG.info("Adding subscription " + subscription);
            }
        }
    }

    /**
     * Test of findMailboxSubscriptionForUser method, of class
     * HBseSubscriptionMapper.
     */
    @Test
    public void testFindMailboxSubscriptionForUser() throws Exception {
        LOG.info("findMailboxSubscriptionForUser");

        final SimpleSubscription fake1 = new SimpleSubscription(FAKE_USER_0, MAILBOX_NAME_RESOLVER.getInbox(FAKE_USER_0));
        final SimpleSubscription fake2 = new SimpleSubscription(FAKE_USER_1, MAILBOX_NAME_RESOLVER.getInbox(FAKE_USER_1).child("FAKEBOX"));

        for (Entry<String, List<SimpleSubscription>> entry : subscriptionMap.entrySet()) {
            String user = entry.getKey();
            LOG.info("Searching for all subscriptions for user:{}", user);
            MailboxOwner owner = MAILBOX_NAME_RESOLVER.getOwner(user, false); 
            for (SimpleSubscription subscription : entry.getValue()) {
                final Subscription result = mapper.findMailboxSubscriptionForUser(owner, subscription.getMailbox());
                assertEquals(subscription.getMailbox(), result.getMailbox());
                assertEquals(subscription.getUser(), result.getUser());
            }
        }
        assertNull(mapper.findMailboxSubscriptionForUser(MAILBOX_NAME_RESOLVER.getOwner(fake1.getMailbox()), fake1.getMailbox()));
        assertNull(mapper.findMailboxSubscriptionForUser(MAILBOX_NAME_RESOLVER.getOwner(fake2.getMailbox()), fake2.getMailbox()));
    }

    /**
     * Test of save method, of class HBaseSubscriptionMapper.
     */
    @Test
    public void testSave() throws Exception {
        LOG.info("save");
        final HTable subscriptions = new HTable(mapperFactory.getClusterConfiguration(), SUBSCRIPTIONS_TABLE);

        for (String user : subscriptionMap.keySet()) {
            final Get get = new Get(Bytes.toBytes(user));
            get.addFamily(SUBSCRIPTION_CF);
            final Result result = subscriptions.get(get);
            for (Subscription subscription : subscriptionMap.get(user)) {
                assertTrue(result.containsColumn(SUBSCRIPTION_CF, Bytes.toBytes(MAILBOX_NAME_CODEC.encode(subscription.getMailbox()))));
            }
        }
        subscriptions.close();
    }

    /**
     * Test of findSubscriptionsForUser method, of class
     * HBaseSubscriptionMapper.
     */
    @Test
    public void testFindSubscriptionsForUser() throws Exception {
        LOG.info("findSubscriptionsForUser");
        for (String user : subscriptionMap.keySet()) {
            LOG.info("Searching for all subscriptions for user: " + user);
            final List<Subscription> found = mapper.findSubscriptionsForUser(MAILBOX_NAME_RESOLVER.getOwner(user, false));
            assertEquals(subscriptionMap.get(user).size(), found.size());
            // TODO: patch Subscription to implement equals
            //assertTrue(subscriptionList.get(user).containsAll(foundSubscriptions));
            //assertTrue(foundSubscriptions.containsAll(subscriptionList.get(user)));
            //assertFalse(foundSubscriptions.contains(fake1));
            //assertFalse(foundSubscriptions.contains(fake2));
        }
        //TODO: check what value we should return in case of no subscriptions: null or empty list
        assertEquals(mapper.findSubscriptionsForUser(MAILBOX_NAME_RESOLVER.getOwner(FAKE_USER_0, false)).size(), 0);

    }

    /**
     * Test of delete method, of class HBaseSubscriptionMapper.
     */
    @Test
    public void testDelete() throws Exception {
        LOG.info("delete");
        final HTable subscriptions = new HTable(mapperFactory.getClusterConfiguration(), SUBSCRIPTIONS_TABLE);

        for (String user : subscriptionMap.keySet()) {
            LOG.info("Deleting subscriptions for user: " + user);
            for (SimpleSubscription subscription : subscriptionMap.get(user)) {
                LOG.info("Deleting subscription : " + subscription);
                mapper.delete(subscription);
                final Get get = new Get(Bytes.toBytes(subscription.getUser()));
                final Result result = subscriptions.get(get);
                assertFalse(result.containsColumn(SUBSCRIPTION_CF, Bytes.toBytes(MAILBOX_NAME_CODEC.encode(subscription.getMailbox()))));
            }
        }
        subscriptions.close();
        fillSubscriptionList();
    }
}

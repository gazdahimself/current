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
package org.apache.james.mailbox;

import java.util.Collection;

import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract base class to test {@link SubscriptionManager} implementations
 * 
 *
 */
public abstract class AbstractSubscriptionManagerTest {

    private final static String USER1 = "test";

    public abstract SubscriptionManager createSubscriptionManager();
    
    @Test
    public void testSubscriptionManager() throws Exception {
        SubscriptionManager manager = createSubscriptionManager();
        MailboxSession session = 
                new MockMailboxSession(USER1, DefaultMailboxNameResolver.INSTANCE);
        manager.startProcessingRequest(session);
        
        MailboxName inbox = session.getMailboxNameResolver().getInbox(session.getOwner());
        MailboxName mailbox1 = inbox.child("test1");
        MailboxName mailbox2 = inbox.child("test2");
        
        Assert.assertTrue(manager.subscriptions(session).isEmpty());
        
        manager.subscribe(session, mailbox1);
        Assert.assertEquals(mailbox1, manager.subscriptions(session).iterator().next());
        Assert.assertEquals(1, manager.subscriptions(session).size());
        
        
        manager.subscribe(session, mailbox1);
        Assert.assertEquals(mailbox1, manager.subscriptions(session).iterator().next());
        Assert.assertEquals(1, manager.subscriptions(session).size());
        
        manager.subscribe(session, mailbox2);
        Collection<MailboxName> col = manager.subscriptions(session);
      
        Assert.assertTrue(col.contains(mailbox2));
        Assert.assertTrue(col.contains(mailbox1));
        Assert.assertEquals(2, col.size());
        
        
        manager.unsubscribe(session, mailbox1);
        Assert.assertEquals(mailbox2, manager.subscriptions(session).iterator().next());
        Assert.assertEquals(1, manager.subscriptions(session).size());
        
        manager.unsubscribe(session, mailbox1);
        Assert.assertEquals(mailbox2, manager.subscriptions(session).iterator().next());
        Assert.assertEquals(1, manager.subscriptions(session).size());
        
        
        manager.endProcessingRequest(session);
    }
}

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
package org.apache.james.mailbox.inmemory.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.model.Subscription;

public class InMemorySubscriptionMapper extends NonTransactionalMapper implements SubscriptionMapper {
    
    private static final int INITIAL_SIZE = 64;
    private final Map<String, List<Subscription>> subscriptionsByUser;
    
    public InMemorySubscriptionMapper() {
        subscriptionsByUser = new ConcurrentHashMap<String, List<Subscription>>(INITIAL_SIZE);
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#delete(org.apache.james.mailbox.store.user.model.Subscription)
     */
    @Override
    public synchronized void delete(Subscription subscription) {
        final String user = subscription.getUser();
        final List<Subscription> subscriptions = subscriptionsByUser.get(user);
        if (subscriptions != null) {
            subscriptions.remove(subscription);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    @Override
    public Subscription findMailboxSubscriptionForUser(MailboxOwner owner, MailboxName mailbox) {
        final List<Subscription> subscriptions = subscriptionsByUser.get(owner.getName());
        if (subscriptions != null) {
            for(Subscription subscription : subscriptions) {
                if (subscription.getMailbox().equals(mailbox)) {
                    return subscription;
                }
            }
        }
        return null;
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#findSubscriptionsForUser(java.lang.String)
     */
    @Override
    public List<Subscription> findSubscriptionsForUser(MailboxOwner owner) {
        final List<Subscription> subcriptions = subscriptionsByUser.get(owner.getName());
        if (subcriptions == null) {
            return Collections.emptyList();
        } else {
            /* prevent modifications */
            return Collections.unmodifiableList(subcriptions);
        }
    }

    /**
     * @see org.apache.james.mailbox.store.user.SubscriptionMapper#save(org.apache.james.mailbox.store.user.model.Subscription)
     */
    @Override
    public synchronized void save(Subscription subscription) {
        final String user = subscription.getUser();
        List<Subscription> subscriptions = subscriptionsByUser.get(user);
        if (subscriptions == null) {
            subscriptions  = new ArrayList<Subscription>(StoreMailboxManager.estimateMailboxCountPerUser());
            subscriptionsByUser.put(user, subscriptions);
        }
        subscriptions.add(subscription);
    }
    
    public void deleteAll() {
        subscriptionsByUser.clear();
    }

    /**
     * Do nothing
     */
    @Override
    public void endRequest() {
        // nothing todo
        
    }

}

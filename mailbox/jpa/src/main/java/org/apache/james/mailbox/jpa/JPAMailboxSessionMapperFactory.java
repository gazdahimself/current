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
package org.apache.james.mailbox.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.MailboxACLCodec;
import org.apache.james.mailbox.jpa.mail.JPAMailboxMapper;
import org.apache.james.mailbox.jpa.mail.JPAMessageMapper;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.user.JPASubscriptionMapper;
import org.apache.james.mailbox.jpa.user.model.JPASubscription;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.openjpa.event.AbstractLifecycleListener;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

/**
 * JPA implementation of {@link MailboxSessionMapperFactory}
 * 
 */
public class JPAMailboxSessionMapperFactory extends MailboxSessionMapperFactory<Long> {
    protected class JPAMailboxCodecsInjector extends AbstractLifecycleListener {
        @Override
        public void afterLoad(LifecycleEvent event) {
            JPAMailbox mailbox = (JPAMailbox) event.getSource();
            mailbox.setMailboxNameCodec(JPAMailboxSessionMapperFactory.this.mailboxNameCodec);
            mailbox.setMailboxACLCodec(JPAMailboxSessionMapperFactory.this.mailboxAclCodec);
        }
    }

    protected class JPASubscriptionCodecInjector extends AbstractLifecycleListener {
        @Override
        public void afterLoad(LifecycleEvent event) {
            JPASubscription subscription = (JPASubscription) event.getSource();
            subscription.setMailboxNameCodec(JPAMailboxSessionMapperFactory.this.mailboxNameCodec);
        }
    }

    private final EntityManagerFactory entityManagerFactory;
    private final UidProvider<Long> uidProvider;
    private final ModSeqProvider<Long> modSeqProvider;
    private final MailboxNameCodec mailboxNameCodec;
    private final MailboxACLCodec mailboxAclCodec;

    /**
     * Creates a new {@link JPAMailboxSessionMapperFactory} using
     * {@link MailboxNameCodec#SAFE_STORE_NAME_CODEC} and
     * {@link MailboxACLCodec#DEFAULT}.
     * 
     * @param entityManagerFactory
     * @param uidProvider
     * @param modSeqProvider
     */
    public JPAMailboxSessionMapperFactory(EntityManagerFactory entityManagerFactory, UidProvider<Long> uidProvider, ModSeqProvider<Long> modSeqProvider) {
        this(entityManagerFactory, uidProvider, modSeqProvider, MailboxNameCodec.SAFE_STORE_NAME_CODEC, MailboxACLCodec.DEFAULT);
    }

    /**
     * Use this constructor if you are not content with the default
     * {@link #mailboxNameCodec} and {@link #mailboxAclCodec} set in
     * {@link #JPAMailboxSessionMapperFactory(EntityManagerFactory, UidProvider, ModSeqProvider)}
     * .
     * 
     * @param entityManagerFactory
     * @param uidProvider
     * @param modSeqProvider
     * @param mailboxNameCodec
     * @param mailboxAclCodec
     */
    public JPAMailboxSessionMapperFactory(EntityManagerFactory entityManagerFactory, UidProvider<Long> uidProvider, ModSeqProvider<Long> modSeqProvider, MailboxNameCodec mailboxNameCodec, MailboxACLCodec mailboxAclCodec) {
        this.entityManagerFactory = entityManagerFactory;
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
        this.mailboxNameCodec = mailboxNameCodec;
        this.mailboxAclCodec = mailboxAclCodec;
        ;
        if (entityManagerFactory instanceof OpenJPAEntityManagerFactorySPI) {
            OpenJPAEntityManagerFactorySPI spiManager = (OpenJPAEntityManagerFactorySPI) entityManagerFactory;
            spiManager.addLifecycleListener(new JPASubscriptionCodecInjector(), JPASubscription.class);
            spiManager.addLifecycleListener(new JPAMailboxCodecsInjector(), JPAMailbox.class);
        } else {
            throw new IllegalArgumentException(OpenJPAEntityManagerFactorySPI.class.getName() + " needed instead of " + entityManagerFactory.getClass().getName() + ".");
        }

        createEntityManager().close();
    }

    @Override
    public MailboxMapper<Long> createMailboxMapper(MailboxSession session) {
        return new JPAMailboxMapper(entityManagerFactory, mailboxNameCodec, mailboxAclCodec);
    }

    @Override
    public MessageMapper<Long> createMessageMapper(MailboxSession session) {
        return new JPAMessageMapper(session, uidProvider, modSeqProvider, entityManagerFactory);
    }

    @Override
    public SubscriptionMapper createSubscriptionMapper(MailboxSession session) {
        return new JPASubscriptionMapper(entityManagerFactory, mailboxNameCodec);
    }

    /**
     * Return a new {@link EntityManager} instance
     * 
     * @return manager
     */
    private EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

}

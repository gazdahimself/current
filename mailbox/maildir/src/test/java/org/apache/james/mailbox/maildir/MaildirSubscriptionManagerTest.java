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
package org.apache.james.mailbox.maildir;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.AbstractSubscriptionManagerTest;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.maildir.locator.LocalAndVirtualMailboxLocatorChain;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.junit.Before;

public class MaildirSubscriptionManagerTest extends AbstractSubscriptionManagerTest{
    private static final File MAILDIR_HOME = new File("target/Maildir");
    private static final File MAILDIR_HOME_DOMAINS = new File(MAILDIR_HOME, "domains");
    private static final File MAILDIR_HOME_GROUPS = new File(MAILDIR_HOME, "groups");
    private static final File MAILDIR_HOME_USERS = new File(MAILDIR_HOME, "users");

    @Before
    public void clean() throws IOException {
        FileUtils.deleteDirectory(MAILDIR_HOME);
    }

    @Override
    public SubscriptionManager createSubscriptionManager() {
        LocalAndVirtualMailboxLocatorChain maildirLocator = new LocalAndVirtualMailboxLocatorChain(MAILDIR_HOME_USERS, MAILDIR_HOME_GROUPS, MAILDIR_HOME_DOMAINS);
        MaildirStore store = new MaildirStore(new JVMMailboxPathLocker(), maildirLocator);
        MaildirMailboxSessionMapperFactory factory = new MaildirMailboxSessionMapperFactory(store);
        StoreSubscriptionManager sm = new StoreSubscriptionManager(factory);
        return sm;
    }

}

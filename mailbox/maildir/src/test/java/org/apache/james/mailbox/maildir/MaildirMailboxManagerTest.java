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
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.AbstractMailboxManagerTest;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.maildir.locator.LocalAndVirtualMailboxLocatorChain;
import org.apache.james.mailbox.maildir.locator.LocalSystemMaildirLocator;
import org.apache.james.mailbox.maildir.locator.VirtualMaildirLocator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * MaildirMailboxManagerTest that extends the StoreMailboxManagerTest.
 */
public class MaildirMailboxManagerTest extends AbstractMailboxManagerTest {
    
    private static final File MAILDIR_HOME = new File("target/Maildir");
    private static final File MAILDIR_HOME_USERS = new File(MAILDIR_HOME, "users");
    private static final File MAILDIR_HOME_GROUPS = new File(MAILDIR_HOME, "groups");
    private static final File MAILDIR_HOME_DOMAINS = new File(MAILDIR_HOME, "domains");
    private LocalSystemMaildirLocator maildirLocator;

    /**
     * Setup the mailboxManager.
     * 
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            deleteMaildirTestDirectory();
            maildirLocator = new LocalAndVirtualMailboxLocatorChain(MAILDIR_HOME_USERS, MAILDIR_HOME_GROUPS, MAILDIR_HOME_DOMAINS);
        }
    }
    
    /**
     * Delete Maildir directory after test.
     * 
     * @throws IOException 
     */
    @After
    public void tearDown() throws IOException {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            deleteMaildirTestDirectory();
        }
    }

    /**
     * @see org.apache.james.mailbox.AbstractMailboxManagerTest#testList()
     */
    @Test
    @Override
    public void testList() throws MailboxException, UnsupportedEncodingException {
        
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            maildirLocator = new LocalAndVirtualMailboxLocatorChain(MAILDIR_HOME_USERS, MAILDIR_HOME_GROUPS, MAILDIR_HOME_DOMAINS);

            MaildirStore store = new MaildirStore(new JVMMailboxPathLocker(), maildirLocator);
            MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
            MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
            GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();

            MaildirMailboxManager<Integer> manager = new MaildirMailboxManager<Integer>(mf, null, new JVMMailboxPathLocker(), aclResolver, groupMembershipResolver);
            manager.init();
            setMailboxManager(manager);
            try {
                super.testList();
            } finally {
                try {
                    deleteMaildirTestDirectory();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
            
    }
    
    /**
     * @see org.apache.james.mailbox.AbstractMailboxManagerTest#testBasicOperations()
     */
    @Test
    @Override
    public void testBasicOperations() throws BadCredentialsException, MailboxException, UnsupportedEncodingException {
        
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            
            
            

            MaildirStore store = new MaildirStore(new JVMMailboxPathLocker(), maildirLocator);
            MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
            
            MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
            GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();

            MaildirMailboxManager<Integer> manager = new MaildirMailboxManager<Integer>(mf, null, new JVMMailboxPathLocker(), aclResolver, groupMembershipResolver);
            manager.init();
            setMailboxManager(manager);
            try {
                super.testBasicOperations();
            } finally {
                try {
                    deleteMaildirTestDirectory();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
       
        }

    }

    /**
     * @see org.apache.james.mailbox.AbstractMailboxManagerTest#testCreateSubFolderDirectly()
     */
    @Test
    @Override
    public void testCreateSubFolderDirectly() throws BadCredentialsException, MailboxException { 

        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {

            MaildirStore store = new MaildirStore(new JVMMailboxPathLocker(), maildirLocator);
            MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);
            MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
            GroupMembershipResolver groupMembershipResolver = new SimpleGroupMembershipResolver();

            MaildirMailboxManager<Integer> manager = new MaildirMailboxManager<Integer>(mf, null, new JVMMailboxPathLocker(), aclResolver, groupMembershipResolver);
            manager.init();
            setMailboxManager(manager);
            try {
                super.testCreateSubFolderDirectly();
            } finally {
                try {
                    deleteMaildirTestDirectory();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
       
    
        }

    }

    /**
     * @see org.apache.james.mailbox.MailboxManagerTest#createMailboxManager()
     */
    @Override
    protected void createMailboxManager() {
        // Do nothing, the maildir mailboxManager is created in the test method.
    }
   
    /**
     * Utility method to delete the test Maildir Directory.
     * 
     * @throws IOException
     */
    private void deleteMaildirTestDirectory() throws IOException {
        FileUtils.deleteDirectory(MAILDIR_HOME);
    }
    
}

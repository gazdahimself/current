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
package org.apache.james.mailbox.mock;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.slf4j.LoggerFactory;

/**
 * A mock mailbox manager.
 *
 */
public class MockMailboxManager {
    
    /**
     * The mock mailbox manager constructed based on a provided mailboxmanager.
     */
    private final MailboxManager mockMailboxManager;
    
    private final boolean createVirtualUsers;
    private final boolean createNonVirtualUsers;
    private int mailboxCount = 0;
    
    /**
     * Number of Domains to be created in the Mailbox Manager.
     */
    public static final int DOMAIN_COUNT = 3;
    
    /**
     * Number of Users (with INBOX) to be created in the Mailbox Manager.
     */
    public static final int USER_COUNT = 3;
    
    private static final String USER_PREFIX = "user";
    private static final String DOMAIN_PREFIX = "localhost";
    
    /**
     * Number of Sub Mailboxes (mailbox in INBOX) to be created in the Mailbox Manager.
     */
    public static final int SUB_MAILBOXES_COUNT = 3;
    private static final String SUB_FOLDER_PREFIX = "SUB_FOLDER_";

    /**
     * Number of Sub Sub Mailboxes (mailbox in a mailbox under INBOX) to be created in the Mailbox Manager.
     */
    public static final int SUB_SUB_MAILBOXES_COUNT = 3;
    private static final String SUBSUB_FOLDER_PREFIX = "SUBSUB_FOLDER_";
    
    /**
     * Number of Messages per Mailbox to be created in the Mailbox Manager.
     */
    public static final int MESSAGE_PER_MAILBOX_COUNT = 3;
    
    /**
     * Construct a mock mailboxManager based on a valid mailboxManager.
     * The mailboxManager will be feeded with mailboxes and mails.
     * 
     * @param mailboxManager
     * @throws UnsupportedEncodingException 
     * @throws MailboxException 
     */
    public MockMailboxManager(MailboxManager mailboxManager, boolean createVirtualUsers, boolean createNonVirtualUsers ) throws MailboxException, UnsupportedEncodingException {
        this.mockMailboxManager = mailboxManager;
        this.createVirtualUsers = createVirtualUsers;
        this.createNonVirtualUsers = createNonVirtualUsers;
        feedMockMailboxManager();
    }
    
    /**
     * @return
     */
    public MailboxManager getMockMailboxManager() {
        return mockMailboxManager;
    }
    
    /**
     * Utility method to feed the Mailbox Manager with a number of 
     * mailboxes and messages per mailbox.
     * 
     * @throws MailboxException
     * @throws UnsupportedEncodingException
     */
    private void feedMockMailboxManager() throws MailboxException, UnsupportedEncodingException {
        
        if (createNonVirtualUsers) {

            for (int j=0; j < USER_COUNT; j++) {
                
                String user = USER_PREFIX + j;
                

                MailboxSession mailboxSession = getMockMailboxManager().createSystemSession(user, LoggerFactory.getLogger("mailboxmanager-test"));
                MailboxNameResolver nameResolver = mailboxSession.getMailboxNameResolver();
                MailboxName inboxPath = nameResolver.getInbox(mailboxSession.getOwner());
                createMailbox(mailboxSession, inboxPath);
                
                for (int k=0; k < SUB_MAILBOXES_COUNT; k++) {
                    
                    MailboxName subPath = inboxPath.child(SUB_FOLDER_PREFIX + k);
                    createMailbox(mailboxSession, subPath);
                    
                    for (int l=0; l < SUB_SUB_MAILBOXES_COUNT; l++) {

                        MailboxName subSubPath = subPath.child(SUBSUB_FOLDER_PREFIX + l);
                        createMailbox(mailboxSession, subSubPath);

                    }
                        
                }

                getMockMailboxManager().logout(mailboxSession, true);
        
            }
        }
        
        if (createVirtualUsers) {

            for (int i=0; i < DOMAIN_COUNT; i++) {

                for (int j=0; j < USER_COUNT; j++) {
                    
                    String user = USER_PREFIX + j + "@"+ DOMAIN_PREFIX + i;
                    

                    MailboxSession mailboxSession = getMockMailboxManager().createSystemSession(user, LoggerFactory.getLogger("mailboxmanager-test"));
                    MailboxNameResolver nameResolver = mailboxSession.getMailboxNameResolver();
                    MailboxName inboxPath = nameResolver.getInbox(mailboxSession.getOwner());
                    createMailbox(mailboxSession, inboxPath);
                    
                    for (int k=0; k < SUB_MAILBOXES_COUNT; k++) {
                        
                        MailboxName subPath = inboxPath.child(SUB_FOLDER_PREFIX + k);
                        createMailbox(mailboxSession, subPath);
                        
                        for (int l=0; l < SUB_SUB_MAILBOXES_COUNT; l++) {

                            MailboxName subSubPath = subPath.child(SUBSUB_FOLDER_PREFIX + l);
                            createMailbox(mailboxSession, subSubPath);

                        }
                            
                    }

                    getMockMailboxManager().logout(mailboxSession, true);
            
                }
                
            }            
        }
    }
    
    /**
     * 
     * @param mailboxPath
     * @throws MailboxException
     * @throws UnsupportedEncodingException 
     */
    private void createMailbox(MailboxSession mailboxSession, MailboxName mailboxPath) throws MailboxException, UnsupportedEncodingException {
        MailboxManager mailboxManager = getMockMailboxManager();
        mailboxManager.startProcessingRequest(mailboxSession);
        mailboxManager.createMailbox(mailboxPath, mailboxSession);
        
        MessageManager messageManager = getMockMailboxManager().getMailbox(mailboxPath, mailboxSession);
        for (int j=0; j < MESSAGE_PER_MAILBOX_COUNT; j++) {
            messageManager.appendMessage(new ByteArrayInputStream(MockMail.MAIL_TEXT_PLAIN.getBytes("UTF-8")), 
                    Calendar.getInstance().getTime(), 
                    mailboxSession, 
                    true, 
                    new Flags(Flags.Flag.RECENT));
        }
        getMockMailboxManager().endProcessingRequest(mailboxSession);
        mailboxCount++;
    }

    public int getMailboxCount() {
        return mailboxCount;
    }
    
}

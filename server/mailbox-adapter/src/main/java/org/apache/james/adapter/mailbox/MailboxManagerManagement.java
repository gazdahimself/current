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
package org.apache.james.adapter.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.slf4j.Logger;

/**
 * JMX managmenent for Mailboxes
 */
public class MailboxManagerManagement extends StandardMBean implements MailboxManagerManagementMBean, LogEnabled {

    private MailboxManager mailboxManager;
    private Logger log;

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    public MailboxManagerManagement() throws NotCompliantMBeanException {
        super(MailboxManagerManagementMBean.class);
    }

    /**
     * @see org.apache.james.adapter.mailbox.MailboxManagerManagementMBean#deleteMailboxes(java.lang.String)
     */
    public boolean deleteMailboxes(String username) {
        MailboxSession session = null;
        try {
            session = mailboxManager.createSystemSession(username, log);
            mailboxManager.startProcessingRequest(session);
            List<MailboxMetaData> mList = mailboxManager.search(new MailboxQuery(session.getMailboxNameResolver().getInbox(username)), session);
            for (int i = 0; i < mList.size(); i++) {
                mailboxManager.deleteMailbox(mList.get(i).getMailboxName(), session);
            }
            return true;
        } catch (MailboxException e) {
            log.error("Error while remove mailboxes for user " + username, e);
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
                try {
                    mailboxManager.logout(session, true);
                } catch (MailboxException e) {
                    // ignore here
                }
            }
        }
        return false;
    }

    /**
     * @see org.apache.james.lifecycle.api.LogEnabled#setLog(org.slf4j.Logger)
     */
    public void setLog(Logger log) {
        this.log = log;
    }

    /**
     * @see
     * org.apache.james.adapter.mailbox.MailboxManagerManagementMBean#listMailboxes
     * (java.lang.String)
     */
    public List<String> listMailboxes(String username) {
        MailboxSession session = null;
        List<String> result = null;
        try {
            session = mailboxManager.createSystemSession(username, log);
            mailboxManager.startProcessingRequest(session);
            MailboxNameResolver mailboxNameResolver = session.getMailboxNameResolver();
            MailboxNameCodec mailboxNameCodec = MailboxNameCodec.DEFAULT_IMAP_NAME_CODEC;
            MailboxName home = mailboxNameResolver.getInbox(username);
            List<MailboxMetaData> mds = mailboxManager.search(new MailboxQuery(home.child(MailboxQuery.FREEWILDCARD_STRING)), session);
            result = new ArrayList<String>(mds.size() + 1);
            result.add(mailboxNameCodec.encode(mailboxNameResolver.unresolve(home, username)));
            /* no need to sort the result as mailboxManager.search() returns an already sorted list */
            for (MailboxMetaData md : mds) {
                MailboxName mailboxName = md.getMailboxName();
                result.add(mailboxNameCodec.encode(mailboxNameResolver.unresolve(mailboxName, username)));
            }
        } catch (MailboxException e) {
            log.error("Error list mailboxes for user " + username, e);
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
                try {
                    mailboxManager.logout(session, true);
                } catch (MailboxException e) {
                    // ignore here
                }
            }
        }
        return result != null ? result : Collections.<String>emptyList();
    }
}
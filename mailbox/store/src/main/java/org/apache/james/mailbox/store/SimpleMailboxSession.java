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

package org.apache.james.mailbox.store;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.slf4j.Logger;

/**
 * Describes a mailbox session.
 */
public class SimpleMailboxSession implements MailboxSession, MailboxSession.User {

    private final long sessionId;
    
    private final Logger log;

    private final String userName;
    
    private final String password;
    
    private boolean open = true;

    private final List<Locale> localePreferences;

    private final Map<Object, Object> attributes;
    
    private final SessionType type;
    
    private final MailboxNameResolver mailboxNameResolver;

    private MailboxOwner owner;
    
    public SimpleMailboxSession(final long sessionId, final String userName, final String password,
            final Logger log, final List<Locale> localePreferences, SessionType type, MailboxNameResolver mailboxNameResolver) {
        this.sessionId = sessionId;
        this.log = log;
        this.userName = userName;
        this.password = password;
        this.type = type;
        this.mailboxNameResolver = mailboxNameResolver;
        this.localePreferences = localePreferences;
        this.attributes = new HashMap<Object, Object>();
    }
    
    /**
     * @see org.apache.james.mailbox.MailboxSession#getLog()
     */
    public Logger getLog() {
        return log;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#close()
     */
    public void close() {
        open = false;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#getSessionId()
     */
    public long getSessionId() {
        return sessionId;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#isOpen()
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Renders suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";

        String retValue = "MailboxSession ( " + "sessionId = "
                + this.sessionId + TAB + "open = " + this.open + TAB + " )";

        return retValue;
    }
    
    /**
     * Gets the user executing this session.
     * @return not null
     */
    public User getUser() {
        return this;
    }
    
    /**
     * Gets the name of the user executing this session.
     * 
     * @return not null
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession.User#getLocalePreferences()
     */
    public List<Locale> getLocalePreferences() {
        return localePreferences;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#getAttributes()
     */
    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession.User#getPassword()
     */
    public String getPassword() {
        return password;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#getType()
     */
    public SessionType getType() {
        return type;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#getMailboxNameResolver()
     */
    @Override
    public MailboxNameResolver getMailboxNameResolver() {
        return mailboxNameResolver;
    }

    /**
     * @see org.apache.james.mailbox.MailboxSession#getOwner()
     */
    @Override
    public MailboxOwner getOwner() {
        MailboxOwner result = this.owner;
        if (result == null) {
            result = mailboxNameResolver.getOwner(userName, false);
            this.owner = result;
        }
        return result ;
    }

}

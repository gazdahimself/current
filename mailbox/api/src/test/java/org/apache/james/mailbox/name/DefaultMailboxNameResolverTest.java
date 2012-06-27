/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.name;

import java.util.Arrays;

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.DefaultMailboxName;
import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * DefaultMailboxNameResolverTest.
 * 
 * TODO: more tests:
 * 
 * root-less 
 * other user's 
 * shared
 * 
 * border cases:
 * empty name
 * 
 *  
 * 
 */
public class DefaultMailboxNameResolverTest {

    private static final String SEGMENT_0 = "folder0";
    private static final String SEGMENT_1 = "folder1";
    private static final String DOMAINLESS_USER = "user1";
    private static final String USER_WITH_DOMAIN = "user1@domain1.com";
    private static final String USER_WITH_DOMAIN_USER_PART = "user1";
    private static final String USER_WITH_DOMAIN_DOMAIN_PART = "domain1.com";
    private DefaultMailboxNameResolver subject;

    @Before
    public void setUp() throws Exception {

        subject = new DefaultMailboxNameResolver();

    }
    
    private static UnresolvedMailboxName unqualified(String[] segments) {
        return new DefaultUnresolvedMailboxName(Arrays.asList(segments));
    }
    private static MailboxName qualified(String[] segments, boolean hasRoot) {
        return new DefaultMailboxName(Arrays.asList(segments), hasRoot);
    }

    @Test
    public void testAbsolutizeDomainlessPrivate() {
        MailboxName result = null;
        result = subject.resolve(unqualified(new String[] { MailboxConstants.INBOX, SEGMENT_0, SEGMENT_1}), DOMAINLESS_USER);
        Assert.assertEquals(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_USERS_ROOT, DOMAINLESS_USER, SEGMENT_0, SEGMENT_1 }, true), result);
        
        result = subject.resolve(unqualified(new String[] { MailboxConstants.INBOX, SEGMENT_0}), DOMAINLESS_USER);
        Assert.assertEquals(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_USERS_ROOT, DOMAINLESS_USER, SEGMENT_0 }, true), result);
    }
    
    @Test
    public void testAbsolutizeWithDomainPrivate() {
        MailboxName result = null;
        result = subject.resolve(unqualified(new String[] { MailboxConstants.INBOX, SEGMENT_0, SEGMENT_1}), USER_WITH_DOMAIN);
        Assert.assertEquals(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_VIRTUAL_USERS_ROOT, USER_WITH_DOMAIN_DOMAIN_PART, USER_WITH_DOMAIN_USER_PART, SEGMENT_0, SEGMENT_1 }, true), result);
        
        result = subject.resolve(unqualified(new String[] { MailboxConstants.INBOX, SEGMENT_0}), USER_WITH_DOMAIN);
        Assert.assertEquals(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_VIRTUAL_USERS_ROOT, USER_WITH_DOMAIN_DOMAIN_PART, USER_WITH_DOMAIN_USER_PART, SEGMENT_0 }, true), result);
    }

    @Test
    public void testRelativizeDomainlessPrivate() {
        UnresolvedMailboxName result = null;
        result = subject.unresolve(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_USERS_ROOT, DOMAINLESS_USER, SEGMENT_0, SEGMENT_1 }, true), DOMAINLESS_USER);
        Assert.assertEquals(unqualified(new String[] { MailboxConstants.INBOX, SEGMENT_0, SEGMENT_1 }), result);
    }

    @Test
    public void testRelativizeWithDomainPrivate() {
        UnresolvedMailboxName result = null;
        result = subject.unresolve(qualified(new String[] { DefaultMailboxNameResolver.BACKEND_USERS_ROOT, USER_WITH_DOMAIN_DOMAIN_PART, USER_WITH_DOMAIN_USER_PART, SEGMENT_0, SEGMENT_1 }, true), USER_WITH_DOMAIN);
        Assert.assertEquals(unqualified(new String[] { SEGMENT_0, SEGMENT_1 }), result);
    }

}

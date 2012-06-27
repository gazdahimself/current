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


package org.apache.james.transport.matchers;

import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMatcherConfig;
import org.apache.mailet.base.test.MailUtil;

import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import junit.framework.TestCase;

public class AllTest extends TestCase {

    private FakeMail mockedMail;

    private Matcher matcher;

    public AllTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMatcher() throws MessagingException {
        matcher = new All();
        FakeMatcherConfig mci = new FakeMatcherConfig("All",
                new FakeMailContext());
        matcher.init(mci);
    }

    // test if all recipients was returned
    public void testAllRecipientsReturned() throws MessagingException {
        mockedMail = MailUtil.createMockMail2Recipients(null);
        setupMatcher();

        Collection<MailAddress> matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

}

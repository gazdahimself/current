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

import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class SizeGreaterThanTest extends TestCase {

    private FakeMail mockedMail;

    private Matcher matcher;

    public SizeGreaterThanTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMockedMail(long size) throws ParseException {
        mockedMail = new FakeMail();
        mockedMail.setMessageSize(size);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {new MailAddress("test@email")}));

    }

    private void setupMatcher(String size) throws MessagingException {
        matcher = new SizeGreaterThan();
        FakeMatcherConfig mci = new FakeMatcherConfig("SizeGreaterThan=" + size,
                new FakeMailContext());
        matcher.init(mci);
    }

    
    public void testSizeGreater() throws MessagingException {
    setupMockedMail(2000000);
        setupMatcher("1m");

        Collection<MailAddress> matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients().size());
    }
    
    public void testSizeNotGreater() throws MessagingException {
        setupMockedMail(200000);
        setupMatcher("1m");

        Collection<MailAddress> matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }
    
    public void testThrowExceptionOnInvalidAmount(){
        boolean exception = false;
        try {
            setupMatcher("1mb");
        } catch (MessagingException e) {
            exception = true;
        }
        assertTrue("Exception thrown", exception);
    }
}

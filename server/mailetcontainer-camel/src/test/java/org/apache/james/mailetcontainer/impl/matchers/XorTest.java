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
package org.apache.james.mailetcontainer.impl.matchers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import org.apache.james.transport.matchers.All;
import org.apache.james.transport.matchers.RecipientIs;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMatcherConfig;
import static org.junit.Assert.*;
import org.junit.Test;

public class XorTest {

    private FakeMailContext context;
    private FakeMail mockedMail;
    private CompositeMatcher matcher;

    private void setupMockedMail() throws ParseException {
    mockedMail = new FakeMail();
    mockedMail.setRecipients(Arrays.asList(new MailAddress[]{
            new MailAddress("test@james.apache.org"),
            new MailAddress("test2@james.apache.org")}));

    }

    /**
     * Setup a composite Or matcher and test it
     * @throws MessagingException
     */
    private void setupMatcher() throws MessagingException {
    context = new FakeMailContext();
    matcher = new Xor();
    FakeMatcherConfig mci = new FakeMatcherConfig("Xor", context);
    matcher.init(mci);
    }

    private void setupChild(String match) throws MessagingException {
    Matcher child = null;
    if (match.equals("All")) {
        child = new All();
    } else {
        child = new RecipientIs();
    }
    FakeMatcherConfig sub = new FakeMatcherConfig(match, context);
    child.init(sub);
    matcher.add(child);

    }

    // test if all recipients was returned
    @Test
    public void testIntersectSame() throws MessagingException {
    setupMockedMail();
    setupMatcher();
    setupChild("RecipientIsRegex=test@james.apache.org");
    setupChild("RecipientIsRegex=test@james.apache.org");

    Collection matchedRecipients = matcher.match(mockedMail);

    assertNotNull(matchedRecipients);
    assertEquals(0, matchedRecipients.size());
    }

    @Test
    public void testNoIntersect() throws MessagingException {
    setupMockedMail();
    setupMatcher();
    setupChild("RecipientIsRegex=test@james.apache.org");
    setupChild("RecipientIsRegex=test2@james.apache.org");

    Collection matchedRecipients = matcher.match(mockedMail);

    assertNotNull(matchedRecipients);
    assertEquals(2, matchedRecipients.size());

    Iterator iterator = matchedRecipients.iterator();
    MailAddress address = (MailAddress) iterator.next();
    assertEquals(address, "test@james.apache.org");
    address = (MailAddress) iterator.next();
    assertEquals(address, "test2@james.apache.org");
    }
}

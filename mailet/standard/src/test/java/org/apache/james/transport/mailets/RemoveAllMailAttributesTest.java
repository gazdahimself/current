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


package org.apache.james.transport.mailets;

import junit.framework.TestCase;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MailUtil;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import java.io.UnsupportedEncodingException;

public class RemoveAllMailAttributesTest extends TestCase {

    private Mail mockedMail;

    private Mailet mailet;

    public RemoveAllMailAttributesTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setupMockedMail(MimeMessage m) throws ParseException {
        mockedMail = MailUtil.createMockMail2Recipients(m);
        mockedMail.setAttribute("org.apache.james.test.junit", "true");

    }

    private void setupMailet() throws MessagingException {
        mailet = new RemoveAllMailAttributes();
        FakeMailetConfig mci = new FakeMailetConfig("Test",
                new FakeMailContext());
        mailet.init(mci);
    }

    // test if ToProcessor works
    public void testRemoveAllMailAttributes() throws MessagingException {
        setupMockedMail(null);
        setupMailet();

        // check if the mail has a attribute
        assertTrue(mockedMail.getAttributeNames().hasNext());

        mailet.service(mockedMail);

        // check if all was removed
        assertFalse(mockedMail.getAttributeNames().hasNext());
    }

}

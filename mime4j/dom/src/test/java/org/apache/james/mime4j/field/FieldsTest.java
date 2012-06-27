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

package org.apache.james.mime4j.field;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.MailboxField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.util.ByteSequence;
import org.apache.james.mime4j.util.ContentUtil;
import org.apache.james.mime4j.util.MimeUtil;

public class FieldsTest extends TestCase {

    public void testContentTypeString() throws Exception {
        ContentTypeField field = Fields.contentType("multipart/mixed; "
                + "boundary=\"-=Part.0.37877968dd4f6595.11eccf0271c"
                + ".2dce5678cbc933d5=-\"");
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Type: multipart/mixed;\r\n "
                + "boundary=\"-=Part.0.37877968dd4f6595.11eccf0271c"
                + ".2dce5678cbc933d5=-\"";
        assertEquals(expectedRaw, decode(field));
    }

    public void testContentTypeStringParameters() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("boundary",
                "-=Part.0.37877968dd4f6595.11eccf0271c.2dce5678cbc933d5=-");
        ContentTypeField field = Fields.contentType("multipart/mixed",
                parameters);
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Type: multipart/mixed;\r\n "
                + "boundary=\"-=Part.0.37877968dd4f6595.11eccf0271c"
                + ".2dce5678cbc933d5=-\"";
        assertEquals(expectedRaw, decode(field));
    }

    public void testContentTypeStringParametersWithSpaces() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("param", "value with space chars");
        ContentTypeField field = Fields.contentType("multipart/mixed",
                parameters);
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Type: multipart/mixed; "
                + "param=\"value with space chars\"";
        assertEquals(expectedRaw, decode(field));
    }

    public void testContentTypeStringNullParameters() throws Exception {
        ContentTypeField field = Fields.contentType("text/plain", null);
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Type: text/plain";
        assertEquals(expectedRaw, decode(field));
    }

    public void testInvalidContentType() throws Exception {
        ContentTypeField field = Fields.contentType("multipart/mixed; "
                + "boundary=-=Part.0.37877968dd4f6595.11eccf0271c"
                + ".2dce5678cbc933d5=-");
        assertFalse(field.isValidField());

        assertEquals("multipart/mixed", field.getMimeType());
    }

    public void testContentTransferEncoding() throws Exception {
        ContentTransferEncodingField field = Fields
                .contentTransferEncoding("base64");
        assertTrue(field.isValidField());

        assertEquals("Content-Transfer-Encoding: base64",
                decode(field));
    }

    public void testContentDispositionString() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("inline; "
                + "filename=\"testing 1 2.dat\"; size=12345; "
                + "creation-date=\"Thu, 1 Jan 1970 00:00:00 +0000\"");
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Disposition: inline; filename="
                + "\"testing 1 2.dat\"; size=12345;\r\n creation-date="
                + "\"Thu, 1 Jan 1970 00:00:00 +0000\"";
        assertEquals(expectedRaw, decode(field));
    }

    public void testContentDispositionStringParameters() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("creation-date", MimeUtil.formatDate(new Date(0),
                TimeZone.getTimeZone("GMT")));
        ContentDispositionField field = Fields.contentDisposition("attachment",
                parameters);
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Disposition: attachment; "
                + "creation-date=\"Thu, 1 Jan 1970 00:00:00\r\n +0000\"";
        assertEquals(expectedRaw, decode(field));

        assertEquals(new Date(0), field.getCreationDate());
    }

    public void testContentDispositionStringNullParameters() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("inline",
                (Map<String, String>) null);
        assertTrue(field.isValidField());

        String expectedRaw = "Content-Disposition: inline";
        assertEquals(expectedRaw, decode(field));
    }

    public void testContentDispositionFilename() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("attachment",
                "some file.dat");
        assertTrue(field.isValidField());

        assertEquals("attachment", field.getDispositionType());
        assertEquals("some file.dat", field.getFilename());
    }

    public void testContentDispositionFilenameSize() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("attachment",
                "some file.dat", 300);
        assertTrue(field.isValidField());

        assertEquals("attachment", field.getDispositionType());
        assertEquals("some file.dat", field.getFilename());
        assertEquals(300, field.getSize());
    }

    public void testContentDispositionFilenameSizeDate() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("attachment",
                "some file.dat", 300, new Date(1000), new Date(2000), new Date(
                        3000));
        assertTrue(field.isValidField());

        assertEquals("attachment", field.getDispositionType());
        assertEquals("some file.dat", field.getFilename());
        assertEquals(300, field.getSize());
        assertEquals(new Date(1000), field.getCreationDate());
        assertEquals(new Date(2000), field.getModificationDate());
        assertEquals(new Date(3000), field.getReadDate());
    }

    public void testInvalidContentDisposition() throws Exception {
        ContentDispositionField field = Fields.contentDisposition("inline; "
                + "filename=some file.dat");
        assertFalse(field.isValidField());

        assertEquals("inline", field.getDispositionType());
    }

    public void testDateStringDateTimeZone() throws Exception {
        DateTimeField field = Fields.date("Date", new Date(0), TimeZone
                .getTimeZone("GMT"));
        assertTrue(field.isValidField());

        assertEquals("Date: Thu, 1 Jan 1970 00:00:00 +0000", decode(field
                ));
        assertEquals(new Date(0), field.getDate());

        field = Fields.date("Resent-Date", new Date(0), TimeZone
                .getTimeZone("GMT+1"));
        assertTrue(field.isValidField());

        assertEquals("Resent-Date: Thu, 1 Jan 1970 01:00:00 +0100",
                decode(field));
        assertEquals(new Date(0), field.getDate());
    }

    public void testDateDST() throws Exception {
        long millis = 1216221153000l;
        DateTimeField field = Fields.date("Date", new Date(millis), TimeZone
                .getTimeZone("CET"));
        assertTrue(field.isValidField());

        assertEquals("Date: Wed, 16 Jul 2008 17:12:33 +0200", decode(field
                ));
        assertEquals(new Date(millis), field.getDate());
    }

    public void testMessageId() throws Exception {
        Field messageId = Fields.messageId("acme.org");

        String raw = decode(messageId);
        assertTrue(raw.startsWith("Message-ID: <Mime4j."));
        assertTrue(raw.endsWith("@acme.org>"));
    }

    public void testSubject() throws Exception {
        assertEquals("Subject: ", decode(Fields.subject("")));
        assertEquals("Subject: test", decode(Fields.subject("test")));
        assertEquals("Subject: =?ISO-8859-1?Q?Sm=F8rebr=F8d?=", decode(Fields
                .subject("Sm\370rebr\370d")));

        String seventyEight = "12345678901234567890123456789012345678901234567890123456789012345678";
        assertEquals("Subject:\r\n " + seventyEight, decode(Fields.subject(
                seventyEight)));

        String seventyNine = seventyEight + "9";
        String expected = "Subject: =?US-ASCII?Q?1234567890123456789012345678901234?="
                + "\r\n =?US-ASCII?Q?56789012345678901234567890123456789?=";
        assertEquals(expected, decode(Fields.subject(seventyNine)));
    }

    public void testSender() throws Exception {
        MailboxField field = Fields.sender(AddressBuilder.DEFAULT
                .parseMailbox("JD <john.doe@acme.org>"));
        assertEquals("Sender: JD <john.doe@acme.org>", decode(field));
    }

    public void testFrom() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");

        MailboxListField field = Fields.from(mailbox1);
        assertEquals("From: JD <john.doe@acme.org>", decode(field));

        field = Fields.from(mailbox1, mailbox2);
        assertEquals("From: JD <john.doe@acme.org>, "
                + "Mary Smith <mary@example.net>", decode(field));

        field = Fields.from(Arrays.asList(mailbox1, mailbox2));
        assertEquals("From: JD <john.doe@acme.org>, "
                + "Mary Smith <mary@example.net>", decode(field));
    }

    public void testTo() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("jane.doe@example.org");
        Mailbox mailbox3 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");
        Group group = new Group("The Does", mailbox1, mailbox2);

        AddressListField field = Fields.to(group);
        assertEquals("To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;", decode(field));

        field = Fields.to(group, mailbox3);
        assertEquals("To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));

        field = Fields.to(Arrays.asList(group, mailbox3));
        assertEquals("To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));
    }

    public void testCc() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("jane.doe@example.org");
        Mailbox mailbox3 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");
        Group group = new Group("The Does", mailbox1, mailbox2);

        AddressListField field = Fields.cc(group);
        assertEquals("Cc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;", decode(field));

        field = Fields.cc(group, mailbox3);
        assertEquals("Cc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));

        field = Fields.cc(Arrays.asList(group, mailbox3));
        assertEquals("Cc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));
    }

    public void testBcc() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("jane.doe@example.org");
        Mailbox mailbox3 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");
        Group group = new Group("The Does", mailbox1, mailbox2);

        AddressListField field = Fields.bcc(group);
        assertEquals("Bcc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;", decode(field));

        field = Fields.bcc(group, mailbox3);
        assertEquals("Bcc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));

        field = Fields.bcc(Arrays.asList(group, mailbox3));
        assertEquals("Bcc: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary Smith\r\n <mary@example.net>",
                decode(field));
    }

    public void testReplyTo() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("jane.doe@example.org");
        Mailbox mailbox3 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");
        Group group = new Group("The Does", mailbox1, mailbox2);

        AddressListField field = Fields.replyTo(group);
        assertEquals("Reply-To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;", decode(field));

        field = Fields.replyTo(group, mailbox3);
        assertEquals("Reply-To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary\r\n Smith <mary@example.net>",
                decode(field));

        field = Fields.replyTo(Arrays.asList(group, mailbox3));
        assertEquals("Reply-To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary\r\n Smith <mary@example.net>",
                decode(field));
    }

    public void testMailbox() throws Exception {
        MailboxField field = Fields.mailbox("Resent-Sender", AddressBuilder.DEFAULT
                .parseMailbox("JD <john.doe@acme.org>"));
        assertEquals("Resent-Sender: JD <john.doe@acme.org>", decode(field));
    }

    public void testMailboxList() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");

        MailboxListField field = Fields.mailboxList("Resent-From", Arrays
                .asList(mailbox1, mailbox2));
        assertEquals("Resent-From: JD <john.doe@acme.org>, "
                + "Mary Smith <mary@example.net>", decode(field));
    }

    public void testAddressList() throws Exception {
        Mailbox mailbox1 = AddressBuilder.DEFAULT.parseMailbox("JD <john.doe@acme.org>");
        Mailbox mailbox2 = AddressBuilder.DEFAULT.parseMailbox("jane.doe@example.org");
        Mailbox mailbox3 = AddressBuilder.DEFAULT.parseMailbox("Mary Smith <mary@example.net>");
        Group group = new Group("The Does", mailbox1, mailbox2);

        AddressListField field = Fields.addressList("Resent-To", Arrays.asList(
                group, mailbox3));
        assertEquals("Resent-To: The Does: JD <john.doe@acme.org>, "
                + "jane.doe@example.org;, Mary\r\n Smith <mary@example.net>",
                decode(field));
    }

    public void testInvalidFieldName() throws Exception {
        try {
            Fields.date("invalid field name", new Date());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public static String decode(Field f) throws IOException {
        String s = null;
        ByteSequence raw = f.getRaw();
        if (raw != null) {
            s = ContentUtil.decode(raw);
        }
        if (s == null) {
            StringBuilder buf = new StringBuilder();
            buf.append(f.getName());
            buf.append(": ");
            String body = f.getBody();
            if (body != null) {
                buf.append(body);
            }
            s = MimeUtil.fold(buf.toString(), 0);
        }
        return s;
    }

}

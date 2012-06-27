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

package org.apache.james.mime4j.field.address;

import java.util.List;

import junit.framework.TestCase;

import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.DomainList;
import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.stream.ParserCursor;
import org.apache.james.mime4j.stream.RawFieldParser;
import org.apache.james.mime4j.util.ByteSequence;
import org.apache.james.mime4j.util.ContentUtil;

public class LenientAddressBuilderTest extends TestCase {

    private LenientAddressBuilder parser;

    @Override
    protected void setUp() throws Exception {
        parser = LenientAddressBuilder.DEFAULT;
    }

    public void testParseDomain() throws Exception {
        String s = "machine (comment).  example (dot). com  ; more stuff";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        String domain = parser.parseDomain(raw, cursor, RawFieldParser.INIT_BITSET(';'));
        assertEquals("machine.example.com", domain);
    }

    public void testParseMailboxAddress() throws Exception {
        String s = "<  some  one @ some host . some where . com >";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("some one@somehost.somewhere.com", mailbox.getAddress());
    }

    public void testParseMailboxNullAddress() throws Exception {
        String s = "<>";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("", mailbox.getAddress());
    }
    
    public void testParseMailboxEmptyAddress() throws Exception {
        String s = "<    >";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("", mailbox.getAddress());
    }
    
    public void testParseAddressQuotedLocalPart() throws Exception {
        String s = "<  \"some  one\"   @ some host . some where . com >";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("some  one@somehost.somewhere.com", mailbox.getAddress());
    }

    public void testParseAddressTruncated() throws Exception {
        String s = "<  some  one  ";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("some one", mailbox.getAddress());
    }

    public void testParseAddressTrailingComments() throws Exception {
        String s = "< someone@somehost.somewhere.com  > (garbage) ; ";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("someone@somehost.somewhere.com", mailbox.getAddress());
        assertEquals(';', raw.byteAt(cursor.getPos()));
    }

    public void testParseAddressTrailingGarbage() throws Exception {
        String s = "< someone@somehost.somewhere.com  > garbage) ; ";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("someone@somehost.somewhere.com", mailbox.getAddress());
        assertEquals('g', raw.byteAt(cursor.getPos()));
    }

    public void testParseRoute() throws Exception {
        String s = "  @a, @b, @c :me@home";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        DomainList route = parser.parseRoute(raw, cursor, null);
        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals("a", route.get(0));
        assertEquals("b", route.get(1));
        assertEquals("c", route.get(2));
        assertEquals('m', raw.byteAt(cursor.getPos()));
    }

    public void testParseAddressStartingWithAt() throws Exception {
        String s = "<@somehost.com@somehost.com>";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        Mailbox mailbox = parser.parseMailboxAddress(null, raw, cursor);
        assertEquals("", mailbox.getLocalPart());
        assertEquals(null, mailbox.getDomain());
        DomainList route = mailbox.getRoute();
        assertNotNull(route);
        assertEquals(1, route.size());
        assertEquals("somehost.com@somehost.com", route.get(0));
    }
    
    public void testParseNoRoute() throws Exception {
        String s = "stuff";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        DomainList route = parser.parseRoute(raw, cursor, null);
        assertNull(route);
    }

    public void testParseMailbox() throws Exception {
        Mailbox mailbox1 = parser.parseMailbox("John Doe <jdoe@machine(comment).  example>");
        assertEquals("John Doe", mailbox1.getName());
        assertEquals("jdoe", mailbox1.getLocalPart());
        assertEquals("machine.example", mailbox1.getDomain());

        Mailbox mailbox2 = parser.parseMailbox("Mary Smith \t    \t\t  <mary@example.net>");
        assertEquals("Mary Smith", mailbox2.getName());
        assertEquals("mary", mailbox2.getLocalPart());
        assertEquals("example.net", mailbox2.getDomain());

        Mailbox mailbox3 = parser.parseMailbox("john.doe@acme.org");
        assertNull(mailbox3.getName());
        assertEquals("john.doe@acme.org", mailbox3.getAddress());

        Mailbox mailbox4 = parser.parseMailbox("Mary Smith <mary@example.net>");
        assertEquals("Mary Smith", mailbox4.getName());
        assertEquals("mary@example.net", mailbox4.getAddress());

        // non-ascii should be allowed in quoted strings
        Mailbox mailbox5 = parser.parseMailbox(
                "\"Hans M\374ller\" <hans.mueller@acme.org>");
        assertEquals("Hans M\374ller", mailbox5.getName());
        assertEquals("hans.mueller@acme.org", mailbox5.getAddress());
    }

    public void testParseMailboxEncoded() throws ParseException {
        Mailbox mailbox1 = parser.parseMailbox("=?ISO-8859-1?B?c3R1ZmY=?= <stuff@localhost.localdomain>");
        assertEquals("stuff", mailbox1.getName());
        assertEquals("stuff", mailbox1.getLocalPart());
        assertEquals("localhost.localdomain", mailbox1.getDomain());
    }

    public void testParseMailboxNonASCII() throws Exception {
        Mailbox mailbox1 = parser.parseMailbox(
                "Hans M\374ller <hans.mueller@acme.org>");
        assertEquals("Hans M\374ller", mailbox1.getName());
        assertEquals("hans.mueller@acme.org", mailbox1.getAddress());
    }

    public void testParsePartialQuotes() throws Exception {
        Mailbox mailbox1 = parser.parseMailbox(
                "Hans \"M\374ller\" is a good fella <hans.mueller@acme.org>");
        assertEquals("Hans M\374ller is a good fella", mailbox1.getName());
        assertEquals("hans.mueller@acme.org", mailbox1.getAddress());
    }

    public void testParseMailboxObsoleteSynatax() throws Exception {
        Mailbox mailbox1 = parser.parseMailbox("< (route)(obsolete) " +
                "@host1.domain1 , @host2 . domain2:  foo@bar.org>");
        assertEquals(null, mailbox1.getName());
        assertEquals("foo", mailbox1.getLocalPart());
        assertEquals("bar.org", mailbox1.getDomain());
        DomainList domainList = mailbox1.getRoute();
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals("host1.domain1", domainList.get(0));
        assertEquals("host2.domain2", domainList.get(1));
    }

    public void testParseMailboxEmpty() throws Exception {
        Mailbox mailbox1 = parser.parseMailbox("  ");
        assertNull(mailbox1);
    }

    public void testParseMailboxList() throws Exception {
        String s = "a , b, ,,, c, d,;garbage";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());
        List<Mailbox> mailboxes = parser.parseMailboxes(raw, cursor, RawFieldParser.INIT_BITSET(';'));
        assertEquals(4, mailboxes.size());

        Mailbox mailbox1 = mailboxes.get(0);
        assertEquals("a", mailbox1.getAddress());
        Mailbox mailbox2 = mailboxes.get(1);
        assertEquals("b", mailbox2.getAddress());
        Mailbox mailbox3 = mailboxes.get(2);
        assertEquals("c", mailbox3.getAddress());
        Mailbox mailbox4 = mailboxes.get(3);
        assertEquals("d", mailbox4.getAddress());
        assertEquals(';', raw.byteAt(cursor.getPos()));
    }

    public void testParseMailboxListEmpty() throws Exception {
        String s = "   ";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());
        List<Mailbox> mailboxes = parser.parseMailboxes(raw, cursor, RawFieldParser.INIT_BITSET(';'));
        assertEquals(0, mailboxes.size());
    }

    public void testParseGroup() throws Exception {
        String s = "group: john.doe@acme.org, Mary Smith <mary@example.net>";
        ByteSequence raw = ContentUtil.encode(s);
        ParserCursor cursor = new ParserCursor(0, s.length());
        Group group = parser.parseGroup(raw, cursor);
        assertEquals("group", group.getName());

        MailboxList mailboxes = group.getMailboxes();
        assertEquals(2, mailboxes.size());

        Mailbox mailbox1 = mailboxes.get(0);
        assertNull(mailbox1.getName());
        assertEquals("john.doe@acme.org", mailbox1.getAddress());

        Mailbox mailbox2 = mailboxes.get(1);
        assertEquals("Mary Smith", mailbox2.getName());
        assertEquals("mary@example.net", mailbox2.getAddress());
    }

    public void testParseInvalidGroup() throws Exception {
        Group group = parser.parseGroup("john.doe@acme.org");
        assertEquals("john.doe@acme.org", group.getName());

        MailboxList mailboxes = group.getMailboxes();
        assertEquals(0, mailboxes.size());
    }

    public void testParseAddress() throws Exception {
        Address address = parser.parseAddress("Mary Smith <mary@example.net>");
        assertTrue(address instanceof Mailbox);
        Mailbox mbox = (Mailbox) address;
        assertEquals("Mary Smith", mbox.getName());
        assertEquals("mary@example.net", mbox.getAddress());

        address = parser.parseAddress("group: Mary Smith <mary@example.net>;");
        assertTrue(address instanceof Group);
        Group group = (Group) address;
        assertEquals("group", group.getName());
        MailboxList mailboxes = group.getMailboxes();
        assertEquals(1, mailboxes.size());
        mbox = mailboxes.get(0);
        assertEquals("Mary Smith", mbox.getName());
        assertEquals("mary@example.net", mbox.getAddress());
    }

    public void testParseAddressList() throws Exception {
        AddressList addrList1 = parser.parseAddressList("John Doe <jdoe@machine(comment).  example>");
        assertEquals(1, addrList1.size());
        Mailbox mailbox1 = (Mailbox)addrList1.get(0);
        assertEquals("John Doe", mailbox1.getName());
        assertEquals("jdoe", mailbox1.getLocalPart());
        assertEquals("machine.example", mailbox1.getDomain());

        AddressList addrList2 = parser.parseAddressList("Mary Smith \t    \t\t  <mary@example.net>");
        assertEquals(1, addrList2.size());
        Mailbox mailbox2 = (Mailbox)addrList2.get(0);
        assertEquals("Mary Smith", mailbox2.getName());
        assertEquals("mary", mailbox2.getLocalPart());
        assertEquals("example.net", mailbox2.getDomain());
    }

    public void testEmptyGroup() throws Exception {
        AddressList addrList = parser.parseAddressList("undisclosed-recipients:;");
        assertEquals(1, addrList.size());
        Group group = (Group)addrList.get(0);
        assertEquals(0, group.getMailboxes().size());
        assertEquals("undisclosed-recipients", group.getName());
    }

    public void testMessyGroupAndMailbox() throws Exception {
        AddressList addrList = parser.parseAddressList(
                "Marketing  folks :  Jane Smith < jane @ example . net >," +
                " \" Jack \\\"Jackie\\\" Jones \" < jjones@example.com > (comment(comment)); ,, (comment)  ," +
                " <@example . net,@example(ignore\\)).com:(ignore)john@(ignore)example.net>");
        assertEquals(2, addrList.size());

        Group group = (Group)addrList.get(0);
        assertEquals("Marketing folks", group.getName());
        assertEquals(2, group.getMailboxes().size());

        Mailbox mailbox1 = group.getMailboxes().get(0);
        Mailbox mailbox2 = group.getMailboxes().get(1);

        assertEquals("Jane Smith", mailbox1.getName());
        assertEquals("jane", mailbox1.getLocalPart());
        assertEquals("example.net", mailbox1.getDomain());

        assertEquals(" Jack \"Jackie\" Jones ", mailbox2.getName());
        assertEquals("jjones", mailbox2.getLocalPart());
        assertEquals("example.com", mailbox2.getDomain());

        Mailbox mailbox = (Mailbox)addrList.get(1);
        assertEquals("john", mailbox.getLocalPart());
        assertEquals("example.net", mailbox.getDomain());
        assertEquals(2, mailbox.getRoute().size());
        assertEquals("example.net", mailbox.getRoute().get(0));
        assertEquals("example.com", mailbox.getRoute().get(1));
    }

    public void testEmptyAddressList() throws Exception {
        assertEquals(0, parser.parseAddressList("").size());
        assertEquals(0, parser.parseAddressList("  \t   \t ").size());
        assertEquals(0, parser.parseAddressList("  \t  ,  , , ,,, , \t ").size());
    }

    public void testSimpleForm() throws Exception {
        AddressList addrList = parser.parseAddressList("\"a b c d e f g\" (comment) @example.net");
        assertEquals(1, addrList.size());
        Mailbox mailbox = (Mailbox)addrList.get(0);
        assertEquals("a b c d e f g", mailbox.getLocalPart());
        assertEquals("example.net", mailbox.getDomain());
    }

    public void testFlatten() throws Exception {
        AddressList addrList = parser.parseAddressList("dev : one@example.com, two@example.com; , ,,, marketing:three@example.com ,four@example.com;, five@example.com");
        assertEquals(3, addrList.size());
        assertEquals(5, addrList.flatten().size());
    }

    public void testTortureTest() throws Exception {

        // Source: http://mailformat.dan.info/headers/from.html
        // (Commented out pending confirmation of legality--I think the local-part is illegal.)
        // AddressList.parse("\"Guy Macon\" <guymacon+\" http://www.guymacon.com/ \"00@spamcop.net>");

        // Taken mostly from RFC822.

        // Just make sure these are recognized as legal address lists;
        // there shouldn't be any aspect of the RFC that is tested here
        // but not in the other unit tests.

        parser.parseAddressList("Alfred Neuman <Neuman@BBN-TENEXA>");
        parser.parseAddressList("Neuman@BBN-TENEXA");
        parser.parseAddressList("\"George, Ted\" <Shared@Group.Arpanet>");
        parser.parseAddressList("Wilt . (the Stilt) Chamberlain@NBA.US");

        // NOTE: In RFC822 8.1.5, the following example did not have "Galloping Gourmet"
        // in double-quotes.  I can only assume this was a typo, since 6.2.4 specifically
        // disallows spaces in unquoted local-part.
        parser.parseAddressList("     Gourmets:  Pompous Person <WhoZiWhatZit@Cordon-Bleu>," +
                "                Childs@WGBH.Boston, \"Galloping Gourmet\"@" +
                "                ANT.Down-Under (Australian National Television)," +
                "                Cheapie@Discount-Liquors;," +
                "       Cruisers:  Port@Portugal, Jones@SEA;," +
                "         Another@Somewhere.SomeOrg");

        // NOTE: In RFC822 8.3.3, the following example ended with a lone ">" after
        // Tops-20-Host.  I can only assume this was a typo, since 6.1 clearly shows
        // ">" requires a matching "<".
        parser.parseAddressList("Important folk:" +
                "                   Tom Softwood <Balsa@Tree.Root>," +
                "                   \"Sam Irving\"@Other-Host;," +
                "                 Standard Distribution:" +
                "                   /main/davis/people/standard@Other-Host," +
                "                   \"<Jones>standard.dist.3\"@Tops-20-Host;");

        // The following are from a Usenet post by Dan J. Bernstein:
        // http://groups.google.com/groups?selm=1996Aug1418.21.01.28081%40koobera.math.uic.edu
        parser.parseAddressList("\":sysmail\"@  Some-Group.\t         Some-Org, Muhammed.(I am  the greatest) Ali @(the)Vegas.WBA");
        parser.parseAddressList("me@home.com (comment (nested (deeply\\))))");
        parser.parseAddressList("mailing list: me@home.com, route two <you@work.com>, them@play.com ;");

    }

}

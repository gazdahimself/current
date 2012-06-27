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

package org.apache.james.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.DefaultMailboxNameCodec;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.name.codec.OptimisticMailboxNameEscaper;
import org.junit.Test;

public class MailboxExpressionTest {

    private static final String PART = "mailbox";

    private static final String SECOND_PART = "sub";

    private static final String USER_1 = "user1";
    private static final MailboxName BASE_NAME;

    private static final char DELIMITER = '.';
    
    private static final MailboxNameCodec CODEC = new DefaultMailboxNameCodec(new OptimisticMailboxNameEscaper(DELIMITER));
    private static final MailboxNameResolver RESOLVER = DefaultMailboxNameResolver.INSTANCE;
    
    static {
        BASE_NAME = RESOLVER.getInbox(USER_1);
    }
    

    private MailboxQuery create(String expression) {
        MailboxName qExpression = RESOLVER.resolve(CODEC.decode(expression), USER_1);
        return new MailboxQuery(BASE_NAME, qExpression);
    }
    
    private static MailboxName q(String name) {
        return RESOLVER.resolve(CODEC.decode("INBOX."+name), USER_1);
    }

    @Test
    public void testCombinedNameEmptyPart() throws Exception {
        MailboxQuery expression = create("");
        assertEquals(BASE_NAME, expression.getResolvedExpression());
    }

// MAILBOX-175: MailboxPath.name cannot be null.
//    @Test
//    public void testNullCombinedName() throws Exception {
//        MailboxQuery expression = new MailboxQuery(new MailboxPath(NAMESPACE, null, null), null, DELIMITER);
//        assertNotNull(expression.getResolvedExpression());
//    }

    @Test
    public void testSimpleCombinedName() throws Exception {
        MailboxQuery expression = create(PART);
        assertEquals(BASE_NAME.child(PART), expression.getResolvedExpression());
    }

    @Test
    public void testSimpleExpression() throws Exception {
        MailboxQuery expression = create(PART);
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
    }

    @Test
    public void testEmptyExpression() throws Exception {
        MailboxQuery expression = create("");
        assertTrue(expression.isExpressionMatch(BASE_NAME));
        assertFalse(expression.isExpressionMatch(new MailboxNameBuilder(1).add("whatever").qualified(false)));
        assertFalse(expression.isExpressionMatch(BASE_NAME.child("whatever")));
        assertFalse(expression.isExpressionMatch(BASE_NAME.appendToLast("whatever")));
    }

    @Test
    public void testOnlyLocalWildcard() throws Exception {
        MailboxQuery expression = create("%");
        assertTrue(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(PART + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
    }

    @Test
    public void testOnlyFreeWildcard() throws Exception {
        MailboxQuery expression = create("*");
        assertTrue(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(PART + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
    }

    @Test
    public void testEndsWithLocalWildcard() throws Exception {
        MailboxQuery expression = create(PART + '%');
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(PART + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
    }

    @Test
    public void testStartsWithLocalWildcard() throws Exception {
        MailboxQuery expression = create('%' + PART);
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART + DELIMITER + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
    }

    @Test
    public void testContainsLocalWildcard() throws Exception {
        MailboxQuery expression = create(SECOND_PART + '%' + PART);
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + "w.hat.eve.r" + PART)));
    }

    @Test
    public void testEndsWithFreeWildcard() throws Exception {
        MailboxQuery expression = create(PART + '*');
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(PART + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
    }

    @Test
    public void testStartsWithFreeWildcard() throws Exception {
        MailboxQuery expression = create('*' + PART);
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
    }

    @Test
    public void testContainsFreeWildcard() throws Exception {
        MailboxQuery expression = create(SECOND_PART + '*' + PART);
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "w.hat.eve.r" + PART)));
    }

    @Test
    public void testDoubleFreeWildcard() throws Exception {
        MailboxQuery expression = create(SECOND_PART + "**" + PART);
        assertFalse(expression.isExpressionMatch(q("")));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "w.hat.eve.r" + PART)));
    }

    @Test
    public void testFreeLocalWildcard() throws Exception {
        MailboxQuery expression = create(SECOND_PART + "*%" + PART);
        assertFalse(expression.isExpressionMatch(q("")));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "w.hat.eve.r" + PART)));
    }

    @Test
    public void testLocalFreeWildcard() throws Exception {
        MailboxQuery expression = create(SECOND_PART + "%*" + PART);
        assertFalse(expression.isExpressionMatch(q(SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART)));
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "w.hat.eve.r" + PART)));
    }

    @Test
    public void testMultipleFreeWildcards() throws Exception {
        MailboxQuery expression = create(SECOND_PART + '*' + PART + '*'
                + SECOND_PART + "**");
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART
                + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART + DELIMITER
                + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "tosh.bosh"
                + PART + "tosh.bosh" + SECOND_PART + "boshtosh")));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER
                + PART.substring(1) + DELIMITER + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER
                + PART.substring(1) + DELIMITER + SECOND_PART + PART + DELIMITER
                + SECOND_PART + "toshbosh")));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER
                + PART.substring(1) + DELIMITER + SECOND_PART + PART + DELIMITER
                + SECOND_PART.substring(1))));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + "tosh.bosh"
                + PART + "tosh.bosh" + PART + SECOND_PART + "boshtosh" + PART
                + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART.substring(1)
                + "tosh.bosh" + PART + "tosh.bosh" + SECOND_PART
                + PART.substring(1) + SECOND_PART + "boshtosh" + PART
                + SECOND_PART.substring(1))));
    }

    @Test
    public void testSimpleMixedWildcards() throws Exception {
        MailboxQuery expression = create(SECOND_PART + '%' + PART + '*'
                + SECOND_PART);
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART
                + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART
                + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART + DELIMITER
                + SECOND_PART)));
        
        // Disable this tests as these are wrong. See MAILBOX-65
        //assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART
        //        + SECOND_PART + "Whatever"));
        //assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART
        //        + SECOND_PART + ".Whatever."));
    }

    @Test
    public void testFreeLocalMixedWildcards() throws Exception {
        MailboxQuery expression = create(SECOND_PART + '*' + PART + '%'
                + SECOND_PART);
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART
                + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART
                + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + PART + DELIMITER
                + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + PART + "Whatever"
                + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + PART
                + SECOND_PART + ".Whatever.")));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART
                + SECOND_PART)));
        assertFalse(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART
                + SECOND_PART + DELIMITER + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(SECOND_PART + DELIMITER + PART + DELIMITER
                + SECOND_PART + PART + SECOND_PART)));
    }
    
    @Test
    public void testTwoLocalWildcardsShouldMatchMailboxs() throws Exception {
        MailboxQuery expression = create("%.%");
        assertFalse(expression.isExpressionMatch(q(PART)));
        assertFalse(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART + DELIMITER + SECOND_PART)));
        assertTrue(expression.isExpressionMatch(q(PART + DELIMITER + SECOND_PART)));
    }
    
    @Test
    public void testMailbox65() throws Exception {
        MailboxQuery expression = create("*.test");
        assertTrue(expression.isExpressionMatch(q("blah.test")));
        assertFalse(expression.isExpressionMatch(q("blah.test.go")));

        assertFalse(expression.isExpressionMatch(q("blah.test3")));

    }
}

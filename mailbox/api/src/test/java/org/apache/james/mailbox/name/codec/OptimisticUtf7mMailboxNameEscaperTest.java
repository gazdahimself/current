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

package org.apache.james.mailbox.name.codec;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;
import org.apache.james.mailbox.name.UnresolvedMailboxName;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class OptimisticUtf7mMailboxNameEscaperTest {
    private static final String FOLDER_0 = "folder0";
    private static final String FOLDER_1 = "folder1";

    private OptimisticUtf7mMailboxNameEscaper subject;
    
    private char delimiter;
    private Map<String, UnresolvedMailboxName> bidiTests;
    private MailboxNameCodec codec;
    private static UnresolvedMailboxName mailboxName(String[] segments) {
        return new DefaultUnresolvedMailboxName(Arrays.asList(segments));
    }

    @Before
    public void setUp() throws Exception {

        delimiter = '.';
        subject = new OptimisticUtf7mMailboxNameEscaper(delimiter);
        codec = new DefaultMailboxNameCodec(subject);

        bidiTests = new LinkedHashMap<String, UnresolvedMailboxName>(32);
        bidiTests.put("&AOQ-", mailboxName(new String[] { "ä" }));
        bidiTests.put("&AOQA9g-", mailboxName(new String[] { "äö" }));
        bidiTests.put("&AOQA9gD8-", mailboxName(new String[] { "äöü" }));
        bidiTests.put("&AOQA9gD8AMQ-", mailboxName(new String[] { "äöüÄ" }));
        bidiTests.put("&AOQA9gD8AMQA1g-", mailboxName(new String[] { "äöüÄÖ" }));
        bidiTests.put("&AOQA9gD8AMQA1gDc-", mailboxName(new String[] { "äöüÄÖÜ" }));

        /* i18n escaping */
        bidiTests.put("2012 state leaders" + delimiter + "Ivo Josipovi&AQc-", mailboxName(new String[] { "2012 state leaders", "Ivo Josipović" }));
        bidiTests.put("2012 state leaders" + delimiter + "&ATA-rsen K&APwA5wD8-k", mailboxName(new String[] { "2012 state leaders", "İrsen Küçük" }));
        bidiTests.put("2012 state leaders" + delimiter + "L&AOE-szl&APM- K&APY-v&AOk-r", mailboxName(new String[] { "2012 state leaders", "László Kövér" }));
        bidiTests.put("2012 state leaders" + delimiter + "Andris B&ARM-rzi&AUYBYQ-", mailboxName(new String[] { "2012 state leaders", "Andris Bērziņš" }));
        /* Vladimir Putin */
        bidiTests.put("2012 state leaders" + delimiter + "&BBIEOwQwBDQEOAQ8BDgEQA- &BB8EQwRCBDgEPQ-", mailboxName(new String[] { "2012 state leaders", "Владимир Путин" }));
        /* Slavica Đukić Dejanović */
        bidiTests.put("2012 state leaders" + delimiter + "&BCEEOwQwBDIEOARGBDA- &BAIEQwQ6BDgEWw- &BBQENQRYBDAEPQQ+BDIEOARb-", mailboxName(new String[] { "2012 state leaders", "Славица Ђукић Дејановић" }));
        /* Hamid Karzai */
        bidiTests.put("2012 state leaders" + delimiter + "&Bi0GJwZFBi8- &BqkGMQYyBsw-", mailboxName(new String[] { "2012 state leaders", "حامد کرزی" }));
        /* Hu Jintao */
        bidiTests.put("2012 state leaders" + delimiter + "&gOGVJm2b-", mailboxName(new String[] { "2012 state leaders", "胡锦涛" }));
        /* Mahmoud Ahmadinejad  */
        bidiTests.put("2012 state leaders" + delimiter + "&BkUGLQZFBkgGLw- &BicGLQZFBi8GzCAMBkYGmAYnBi8-", mailboxName(new String[] { "2012 state leaders", "محمود احمدی‌نژاد" }));
        /* Jalal Talabani   */
        bidiTests.put("2012 state leaders" + delimiter + "&BiwGRAYnBkQ- &BjcGJwZEBigGJwZGBkogDg-", mailboxName(new String[] { "2012 state leaders", "جلال طالباني‎" }));
        /* Shimon Peres  */
        bidiTests.put("2012 state leaders" + delimiter + "&BekF3gXiBdUF3w- &BeQF6AXh-", mailboxName(new String[] { "2012 state leaders", "שמעון פרס" }));
        /* Akihito  */
        bidiTests.put("2012 state leaders" + delimiter + "&Zg5OwQ-", mailboxName(new String[] { "2012 state leaders", "明仁" }));
        /* Nursultan Nazarbayev  */
        bidiTests.put("2012 state leaders" + delimiter + "&BB0EsQRABEEEsQQ7BEIEMAQ9- &BB0EMAQ3BDAEQAQxBDAENQQy-", mailboxName(new String[] { "2012 state leaders", "Нұрсұлтан Назарбаев" }));
        /* Lee Myung-bak  */
        bidiTests.put("2012 state leaders" + delimiter + "&x3S6hbwV- &Z05mDlNa-", mailboxName(new String[] { "2012 state leaders", "이명박 李明博" }));
        
        /* simple cases */
        bidiTests.put(FOLDER_0 + delimiter + "Procter &- Gamble", mailboxName(new String[] { FOLDER_0, "Procter & Gamble" }));
        bidiTests.put(FOLDER_0 + delimiter + FOLDER_1, mailboxName(new String[] { FOLDER_0, FOLDER_1 }));
        bidiTests.put("", UnresolvedMailboxName.EMPTY);
        
        //TODO: delimiter at the beginning
        //TODO: delimiter at the end
        //TODO: several subsequent delimiters
        //TODO: null string


    }

    @Test
    public void testBidi() {

        for (Map.Entry<String, UnresolvedMailboxName> pair : bidiTests.entrySet()) {

            final String encodedMailboxName = pair.getKey();
            UnresolvedMailboxName decodedMailboxName = pair.getValue();

            /* encoding test */
            String found = codec.encode(decodedMailboxName);
            Assert.assertEquals(encodedMailboxName, found);

            /* decoding test */
            UnresolvedMailboxName mailboxName = codec.decode(encodedMailboxName);
            Assert.assertEquals(decodedMailboxName, mailboxName);


        }
    }

}

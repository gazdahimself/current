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
package org.apache.jsieve;

import org.apache.jsieve.utils.JUnitUtils;
import org.apache.jsieve.utils.SieveMailAdapter;

import junit.framework.TestCase;

public class LiteralEscapeTest extends TestCase {

    private final String SCRIPT = "require [\"fileinto\", \"reject\"];" +
    "" +
    "# test" +
    "if allof (header :contains \"to\" \"\\\\\") {" +
    "    keep;" +
    "    stop;" +
    "}" +
    "" +
    "# test2" +
    "if anyof (header :contains \"subject\" \"foo\") {" +
    "    keep;" +
    "    stop;" +
    "}"; 
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBackSlash() throws Exception {
        SieveMailAdapter mail = (SieveMailAdapter) JUnitUtils.createMail();
        mail.getMessage().addHeader("to", "tweety@pie");
        JUnitUtils.interpret(mail, SCRIPT);
    }
}

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
package org.apache.james.cli.type;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test class for the CmdType enum.
 */
public class CmdTypeTest {

    /**
     * Test the hasCorrectArguments method.
     */
    @Test
    public void testHasCorrectArguments() {
        CmdType cmd;
        boolean result;

        cmd = CmdType.ADDDOMAIN;

        // Test bogus number
        result = cmd.hasCorrectArguments(-1);
        assertEquals(false, result);

        // Test actual number
        result = cmd.hasCorrectArguments(cmd.getArguments());
        assertEquals(true, result);

        // Test known bad number
        result = cmd.hasCorrectArguments(cmd.getArguments() - 1);
        assertEquals(false, result);
    }

    /**
     * Test the lookup method.
     */
    @Test
    public void testLookup() {
        CmdType result;

        // Test happy path
        result = CmdType.lookup(CmdType.ADDUSER.getCommand());
        assertEquals(CmdType.ADDUSER, result);

        result = CmdType.lookup(CmdType.REMOVEUSER.getCommand());
        assertEquals(CmdType.REMOVEUSER, result);

        result = CmdType.lookup(CmdType.LISTUSERS.getCommand());
        assertEquals(CmdType.LISTUSERS, result);

        result = CmdType.lookup(CmdType.ADDDOMAIN.getCommand());
        assertEquals(CmdType.ADDDOMAIN, result);

        result = CmdType.lookup(CmdType.REMOVEDOMAIN.getCommand());
        assertEquals(CmdType.REMOVEDOMAIN, result);

        result = CmdType.lookup(CmdType.LISTDOMAINS.getCommand());
        assertEquals(CmdType.LISTDOMAINS, result);

        result = CmdType.lookup(CmdType.SETPASSWORD.getCommand());
        assertEquals(CmdType.SETPASSWORD, result);

        // Test known bad value
        result = CmdType.lookup("");
        assertEquals(null, result);

        result = CmdType.lookup("error");
        assertEquals(null, result);

        // Test null value
        result = CmdType.lookup(null);
        assertEquals(null, result);
    }
}
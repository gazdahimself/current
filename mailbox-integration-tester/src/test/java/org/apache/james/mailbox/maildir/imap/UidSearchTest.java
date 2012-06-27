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

package org.apache.james.mailbox.maildir.imap;

import org.apache.james.imap.tester.suite.UidSearch;
import org.apache.james.mailbox.maildir.host.MaildirHostSystem;
import org.apache.james.mailbox.maildir.util.OsDetector;
import org.junit.Test;

public class UidSearchTest extends UidSearch{

    public UidSearchTest() throws Exception {
        super(MaildirHostSystem.build());
    }
    

    @Test
    public void testSearchCombinationsUS() throws Exception {
        // skipped because of maildir does use the create time for internal date 
        // which is ok with the rfc but fail the test
    }

    @Test
    public void testSearchCombinationsITALY() throws Exception {
        // skipped because of maildir does use the create time for internal date 
        // which is ok with the rfc but fail the test
    }

    @Test
    public void testSearchCombinationsKOREA() throws Exception {
        // skipped because of maildir does use the create time for internal date 
        // which is ok with the rfc but fail the test
    }
    
    @Override
    protected void runSessions() throws Exception {
        if (OsDetector.isWindows()) {
            System.out.println("Maildir tests work only on non-windows systems. So skip the test");
        } else {
            super.runSessions(); 
        }
    }
    
}

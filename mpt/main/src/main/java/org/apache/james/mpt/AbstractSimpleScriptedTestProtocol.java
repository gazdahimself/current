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

package org.apache.james.mpt;

import java.io.InputStream;
import java.util.Locale;


/**
 * A Protocol test which reads the test protocol session from a file. The file
 * read is taken as "<test-name>.test", where <test-name> is the value passed
 * into the constructor. Subclasses of this test can set up pre-
 * and post elements for extra elements not defined in the protocol
 * session file.
 */
public abstract class AbstractSimpleScriptedTestProtocol extends
        AbstractProtocolTestFramework {
    private ProtocolSessionBuilder builder = new ProtocolSessionBuilder();

    private static final Locale BASE_DEFAULT_LOCALE = Locale.getDefault();
    
    /**
     * Constructs a scripted test.
     * @param hostSystem not null
     * @param userName user name
     * @param password password for user
     */
    public AbstractSimpleScriptedTestProtocol(HostSystem hostSystem, String userName, String password) {
        super(hostSystem, userName, password);
    }
    
    protected void tearDown() throws Exception {
        Locale.setDefault(BASE_DEFAULT_LOCALE);
        super.tearDown();
    }

    /**
     * Reads test elements from the protocol session file and adds them to the
     * ProtocolSession. Then calls {@link #runSessions()}.
     * 
     * @param locale test under this default locale, not null
     */
    protected void scriptTest(String fileName, Locale locale) throws Exception {
        Locale.setDefault(locale);
        addTestFile(fileName + ".test", runner.getTestElements());
        runSessions();
    }

    /**
     * Finds the protocol session file identified by the test name, and builds
     * protocol elements from it. All elements from the definition file are
     * added to the supplied ProtocolSession.
     * 
     * @param fileName
     *            The name of the file to read
     * @param session
     *            The ProtocolSession to add elements to.
     */
    protected void addTestFile(String fileName, ProtocolInteractor session)
            throws Exception {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream(fileName);
        if (is == null) {
            throw new Exception("Test Resource '" + fileName + "' not found.");
        }

        builder.addProtocolLines(fileName, is, session);
    }
}

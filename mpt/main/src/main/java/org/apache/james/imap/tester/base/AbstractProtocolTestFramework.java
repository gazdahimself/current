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

package org.apache.james.imap.tester.base;

import org.junit.After;
import org.junit.Before;

/**
 * Abstract Protocol Test is the root of all of the James Imap Server test
 * cases. It provides basic functionality for running a protocol session as a
 * JUnit test, and failing if exceptions are thrown. To create a test which
 * reads the entire protocol session from a single protocol definition file, use
 * the {@link AbstractSimpleScriptedTestProtocol}.
 * 
 * @author Darrell DeBoer
 * @author Andrew C. Oliver
 */
public abstract class AbstractProtocolTestFramework {
    /** The Protocol session which is run before the testElements */
    protected ProtocolSession preElements = new ProtocolSession();

    /** The Protocol session which contains the tests elements */
    protected ProtocolSession testElements = new ProtocolSession();

    /** The Protocol session which is run after the testElements. */
    protected ProtocolSession postElements = new ProtocolSession();

    private HostSystem hostSystem;
    
    private final String userName;
    private final String password;

    public AbstractProtocolTestFramework(HostSystem hostSystem, String userName, String password) throws Exception {
        this.hostSystem = hostSystem;
        this.userName = userName;
        this.password = password;
    }

    @Before
    public void setUp() throws Exception {
        hostSystem.beforeTests();
        setUpEnvironment();

    }

    @After
    public void tearDown() throws Exception {
        hostSystem.afterTests();
    }
    protected void continueAfterFailure() {
        preElements.setContinueAfterFailure(true);
        testElements.setContinueAfterFailure(true);
        postElements.setContinueAfterFailure(true);
    }

    /**
     * Runs the pre,test and post protocol sessions against a local copy of the
     * ImapServer. This does not require that James be running, and is useful
     * for rapid development and debugging.
     * 
     * Instead of sending requests to a socket connected to a running instance
     * of James, this method uses the {@link MockImapServer} to simplify
     * testing. One mock instance is required per protocol session/connection.
     * These share the same underlying Mailboxes, because of the way
     * {@link MockImapServer#getImapSession()} works.
     */
    protected void runSessions() throws Exception {
        class SessionContinuation implements HostSystem.Continuation {

            public ProtocolSession session;

            public void doContinue() {
                if (session != null) {
                    session.doContinue();
                }
            }

        }
        SessionContinuation continuation = new SessionContinuation();

        HostSystem.Session[] sessions = new HostSystem.Session[testElements
                .getSessionCount()];

        for (int i = 0; i < sessions.length; i++) {
            sessions[i] = hostSystem.newSession(continuation);
            sessions[i].start();
        }
        try {
            continuation.session = preElements;
            preElements.runSessions(sessions);
            continuation.session = testElements;
            testElements.runSessions(sessions);
            continuation.session = postElements;
            postElements.runSessions(sessions);
        } finally {
            for (int i = 0; i < sessions.length; i++) {
                sessions[i].stop();
            }
        }
    }

    /**
     * Initialises the UsersRepository and ImapHost on first call.
     */
    public void setUpEnvironment() throws Exception {
        hostSystem.reset();
        hostSystem.addUser(userName, password);
    }
    
}

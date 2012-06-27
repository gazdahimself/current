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

/**
 * Host system under test.
 * 
 */
public interface HostSystem {

    /**
     * Resets host system to initial state.
     * 
     * @throws Exception
     */
    public void reset() throws Exception;

    /**
     * Add a user for testing.
     * 
     * @param user
     *            user name
     * @param password
     *            user password
     * @throws Exception
     */
    public boolean addUser(String user, String password) throws Exception;

    /**
     * Creates a new session for functional testing.
     * 
     * @return <code>Session</code>, not null
     * @throws Exception
     */
    public Session newSession(Continuation continuation) throws Exception;

    public interface Session {
        public String readLine() throws Exception;

        public void writeLine(String line) throws Exception;

        /**
         * Opens the session.
         * 
         * @throws Exception
         */
        public void start() throws Exception;

        /**
         * Closes the session.
         * 
         * @throws Exception
         */
        public void stop() throws Exception;
    }

    public interface Continuation {
        public void doContinue();
    }
    
    public void beforeTests() throws Exception;

    
    public void afterTests() throws Exception;
}

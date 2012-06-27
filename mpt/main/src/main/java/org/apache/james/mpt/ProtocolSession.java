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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;


/**
 * A protocol session which can be run against a reader and writer, which checks
 * the server response against the expected values. TODO make ProtocolSession
 * itself be a permissible ProtocolElement, so that we can nest and reuse
 * sessions.
 * 
 * @author Darrell DeBoer <darrell@apache.org>
 * 
 * @version $Revision: 776401 $
 */
public class ProtocolSession implements ProtocolInteractor {
    private boolean continued = false;

    private boolean continuationExpected = false;

    private int maxSessionNumber;

    protected List<ProtocolElement> testElements = new ArrayList<ProtocolElement>();

    private Iterator elementsIterator;

    private Session[] sessions;

    private ProtocolElement nextTest;

    private boolean continueAfterFailure = false;

    public final boolean isContinueAfterFailure() {
        return continueAfterFailure;
    }

    public final void setContinueAfterFailure(boolean continueAfterFailure) {
        this.continueAfterFailure = continueAfterFailure;
    }

    /**
     * Returns the number of sessions required to run this ProtocolSession. If
     * the number of readers and writers provided is less than this number, an
     * exception will occur when running the tests.
     */
    public int getSessionCount() {
        return maxSessionNumber + 1;
    }

    /**
     * Executes the ProtocolSession in real time against the readers and writers
     * supplied, writing client requests and reading server responses in the
     * order that they appear in the test elements. The index of a reader/writer
     * in the array corresponds to the number of the session. If an exception
     * occurs, no more test elements are executed.
     * 
     * @param sessions not null
     */
    public void runSessions(Session[] sessions) throws Exception {
        this.sessions = sessions;
        elementsIterator = testElements.iterator();
        while (elementsIterator.hasNext()) {
            Object obj = elementsIterator.next();
            if (obj instanceof ProtocolElement) {
                ProtocolElement test = (ProtocolElement) obj;
                test.testProtocol(sessions, continueAfterFailure);
            }
        }
    }

    public void doContinue() {
        try {
            if (continuationExpected) {
                continued = true;
                while (elementsIterator.hasNext()) {
                    Object obj = elementsIterator.next();
                    if (obj instanceof ProtocolElement) {
                        nextTest = (ProtocolElement) obj;

                        if (!nextTest.isClient()) {
                            break;
                        }
                        nextTest.testProtocol(sessions, continueAfterFailure);
                    }
                }
                if (!elementsIterator.hasNext()) {
                    nextTest = null;
                }
            } else {
                throw new RuntimeException("Unexpected continuation");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#CL(java.lang.String)
     */
    public void CL(String clientLine) {
        testElements.add(new ClientRequest(clientLine));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SL(java.lang.String, java.lang.String)
     */
    public void SL(String serverLine, String location) {
        testElements.add(new ServerResponse(serverLine, location));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SUB(java.util.List, java.lang.String)
     */
    public void SUB(List<String> serverLines, String location) {
        testElements
                .add(new ServerUnorderedBlockResponse(serverLines, location));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#CL(int, java.lang.String)
     */
    public void CL(int sessionNumber, String clientLine) {
        this.maxSessionNumber = Math.max(this.maxSessionNumber, sessionNumber);
        testElements.add(new ClientRequest(sessionNumber, clientLine));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#CONT(int)
     */
    public void CONT(int sessionNumber) throws Exception {
        this.maxSessionNumber = Math.max(this.maxSessionNumber, sessionNumber);
        testElements.add(new ContinuationElement(sessionNumber));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SL(int, java.lang.String, java.lang.String, java.lang.String)
     */
    public void SL(int sessionNumber, String serverLine, String location,
            String lastClientMessage) {
        this.maxSessionNumber = Math.max(this.maxSessionNumber, sessionNumber);
        testElements.add(new ServerResponse(sessionNumber, serverLine,
                location, lastClientMessage));
    }

    /**
     * @see org.apache.james.mpt.ProtocolInteractor#SUB(int, java.util.List, java.lang.String, java.lang.String)
     */
    public void SUB(int sessionNumber, List<String> serverLines, String location,
            String lastClientMessage) {
        this.maxSessionNumber = Math.max(this.maxSessionNumber, sessionNumber);
        testElements.add(new ServerUnorderedBlockResponse(sessionNumber,
                serverLines, location, lastClientMessage));
    }

    /**
     * A client request, which write the specified message to a Writer.
     */
    private class ClientRequest implements ProtocolElement {
        private int sessionNumber;

        private String message;

        /**
         * Initialises the ClientRequest with the supplied message.
         */
        public ClientRequest(String message) {
            this(-1, message);
        }

        /**
         * Initialises the ClientRequest, with a message and session number.
         * 
         * @param sessionNumber
         * @param message
         */
        public ClientRequest(int sessionNumber, String message) {
            this.sessionNumber = sessionNumber;
            this.message = message;
        }

        /**
         * Writes the request message to the PrintWriters. If the sessionNumber ==
         * -1, the request is written to *all* supplied writers, otherwise, only
         * the writer for this session is writted to.
         * 
         * @throws Exception
         */
        public void testProtocol(Session[] sessions,
                boolean continueAfterFailure) throws Exception {
            if (sessionNumber < 0) {
                for (int i = 0; i < sessions.length; i++) {
                    Session session = sessions[i];
                    writeMessage(session);
                }
            } else {
                Session session = sessions[sessionNumber];
                writeMessage(session);
            }
        }

        private void writeMessage(Session session) throws Exception {
            session.writeLine(message);
        }

        public boolean isClient() {
            return true;
        }

        /**
         * Constructs a <code>String</code> with all attributes
         * in name = value format.
         *
         * @return a <code>String</code> representation 
         * of this object.
         */
        public String toString()
        {
            final String TAB = " ";
            
            String retValue = "ClientRequest ( "
                + "sessionNumber = " + this.sessionNumber + TAB
                + "message = " + this.message + TAB
                + " )";
        
            return retValue;
        }
        
        
    }

    /**
     * Represents a single-line server response, which reads a line from a
     * reader, and compares it with the defined regular expression definition of
     * this line.
     */
    private class ServerResponse implements ProtocolElement {
        private String lastClientMessage;

        private int sessionNumber;

        private String expectedLine;

        protected String location;

        /**
         * Sets up a server response.
         * 
         * @param expectedPattern
         *            A Perl regular expression pattern used to test the line
         *            recieved.
         * @param location
         *            A descriptive value to use in error messages.
         */
        public ServerResponse(String expectedPattern, String location) {
            this(-1, expectedPattern, location, null);
        }

        /**
         * Sets up a server response.
         * 
         * @param sessionNumber
         *            The number of session for a multi-session test
         * @param expectedPattern
         *            A Perl regular expression pattern used to test the line
         *            recieved.
         * @param location
         *            A descriptive value to use in error messages.
         */
        public ServerResponse(int sessionNumber, String expectedPattern,
                String location, String lastClientMessage) {
            this.sessionNumber = sessionNumber;
            this.expectedLine = expectedPattern;
            this.location = location;
            this.lastClientMessage = lastClientMessage;
        }

        /**
         * Reads a line from the supplied reader, and tests that it matches the
         * expected regular expression. If the sessionNumber == -1, then all
         * readers are tested, otherwise, only the reader for this session is
         * tested.
         * 
         * @param out
         *            Is ignored.
         * @param in
         *            The server response is read from here.
         * @throws InvalidServerResponseException
         *             If the actual server response didn't match the regular
         *             expression expected.
         */
        public void testProtocol(Session[] sessions,
                boolean continueAfterFailure) throws Exception {
            if (sessionNumber < 0) {
                for (int i = 0; i < sessions.length; i++) {
                    Session session = sessions[i];
                    checkResponse(session, continueAfterFailure);
                }
            } else {
                Session session = sessions[sessionNumber];
                checkResponse(session, continueAfterFailure);
            }
        }

        protected void checkResponse(Session session,
                boolean continueAfterFailure) throws Exception {
            String testLine = readLine(session);
            if (!match(expectedLine, testLine)) {
                String errMsg = "\nLocation: " + location + "\nLastClientMsg: "
                        + lastClientMessage + "\nExpected: '" + expectedLine
                        + "'\nActual   : '" + testLine + "'";
                if (continueAfterFailure) {
                    System.out.println(errMsg);
                } else {
                    throw new InvalidServerResponseException(errMsg);
                }
            }
        }

        /**
         * A convenience method which returns true if the actual string matches
         * the expected regular expression.
         * 
         * @param expected
         *            The regular expression used for matching.
         * @param actual
         *            The actual message to match.
         * @return <code>true</code> if the actual matches the expected.
         */
        protected boolean match(String expected, String actual) {
            final boolean result = Pattern.matches(expected, actual);
            return result;
        }

        /**
         * Grabs a line from the server and throws an error message if it
         * doesn't work out
         * 
         * @return String of the line from the server
         */
        protected String readLine(Session session) throws Exception {
            try {
                return session.readLine();
            } catch (IOException e) {
                String errMsg = "\nLocation: " + location + "\nExpected: "
                        + expectedLine + "\nReason: Server Timeout.";
                throw new InvalidServerResponseException(errMsg);
            }
        }

        public boolean isClient() {
            return false;
        }

        /**
         * Constructs a <code>String</code> with all attributes
         * in name = value format.
         *
         * @return a <code>String</code> representation 
         * of this object.
         */
        public String toString()
        {
            final String TAB = " ";
            
            String result = "ServerResponse ( "
                + "lastClientMessage = " + this.lastClientMessage + TAB
                + "sessionNumber = " + this.sessionNumber + TAB
                + "expectedLine = " + this.expectedLine + TAB
                + "location = " + this.location + TAB
                + " )";
        
            return result;
        }
        
        
    }

    /**
     * Represents a set of lines which must be recieved from the server, in a
     * non-specified order.
     */
    private class ServerUnorderedBlockResponse extends ServerResponse {
        private List<String> expectedLines = new ArrayList<String>();

        /**
         * Sets up a ServerUnorderedBlockResponse with the list of expected
         * lines.
         * 
         * @param expectedLines
         *            A list containing a reqular expression for each expected
         *            line.
         * @param location
         *            A descriptive location string for error messages.
         */
        public ServerUnorderedBlockResponse(List<String> expectedLines, String location) {
            this(-1, expectedLines, location, null);
        }

        /**
         * Sets up a ServerUnorderedBlockResponse with the list of expected
         * lines.
         * 
         * @param sessionNumber
         *            The number of the session to expect this block, for a
         *            multi-session test.
         * @param expectedLines
         *            A list containing a reqular expression for each expected
         *            line.
         * @param location
         *            A descriptive location string for error messages.
         */
        public ServerUnorderedBlockResponse(int sessionNumber,
                List<String> expectedLines, String location, String lastClientMessage) {
            super(sessionNumber, "<Unordered Block>", location,
                    lastClientMessage);
            this.expectedLines = expectedLines;
        }

        /**
         * Reads lines from the server response and matches them against the
         * list of expected regular expressions. Each regular expression in the
         * expected list must be matched by only one server response line.
         * 
         * @param reader
         *            Server responses are read from here.
         * @throws InvalidServerResponseException
         *             If a line is encountered which doesn't match one of the
         *             expected lines.
         */
        protected void checkResponse(Session session,
                boolean continueAfterFailure) throws Exception {
            List<String> testLines = new ArrayList<String>(expectedLines);
            while (testLines.size() > 0) {
                String actualLine = readLine(session);

                boolean foundMatch = false;
                for (int i = 0; i < testLines.size(); i++) {
                    String expected = (String) testLines.get(i);
                    if (match(expected, actualLine)) {
                        foundMatch = true;
                        testLines.remove(expected);
                        break;
                    }
                }

                if (!foundMatch) {
                    StringBuffer errMsg = new StringBuffer().append(
                            "\nLocation: ").append(location).append(
                            "\nExpected one of: ");
                    Iterator iter = expectedLines.iterator();
                    while (iter.hasNext()) {
                        errMsg.append("\n    ");
                        errMsg.append(iter.next());
                    }
                    errMsg.append("\nActual: ").append(actualLine);
                    if (continueAfterFailure) {
                        System.out.println(errMsg.toString());
                    } else {
                        throw new InvalidServerResponseException(errMsg
                                .toString());
                    }
                }
            }
        }

        /**
         * Constructs a <code>String</code> with all attributes
         * in name = value format.
         *
         * @return a <code>String</code> representation 
         * of this object.
         */
        public String toString()
        {
            final String TAB = " ";
            
            String result = "ServerUnorderedBlockResponse ( "
                + "expectedLines = " + this.expectedLines + TAB
                + " )";
        
            return result;
        }
        
        
    }

    private class ContinuationElement implements ProtocolElement {

        private final int sessionNumber;

        public ContinuationElement(final int sessionNumber) throws Exception {
            this.sessionNumber = sessionNumber < 0 ? 0 : sessionNumber;
        }

        public void testProtocol(Session[] sessions,
                boolean continueAfterFailure) throws Exception {
            Session session = sessions[sessionNumber];
            continuationExpected = true;
            continued = false;
            String testLine = session.readLine();
            if (!"+".equals(testLine) || !continued) {
                final String message = "Expected continuation";
                if (continueAfterFailure) {
                    System.out.print(message);
                } else {
                    throw new InvalidServerResponseException(message);
                }
            }
            continuationExpected = false;
            continued = false;

            if (nextTest != null) {
                nextTest.testProtocol(sessions, continueAfterFailure);
            }
        }

        public boolean isClient() {
            return false;
        }

        /**
         * Constructs a <code>String</code> with all attributes
         * in name = value format.
         *
         * @return a <code>String</code> representation 
         * of this object.
         */
        public String toString()
        {
            final String TAB = " ";
            
            String result = "ContinuationElement ( "
                + "sessionNumber = " + this.sessionNumber + TAB
                + " )";
        
            return result;
        }
        
        
    }

    /**
     * Represents a generic protocol element, which may write requests to the
     * server, read responses from the server, or both. Implementations should
     * test the server response against an expected response, and throw an
     * exception on mismatch.
     */
    private interface ProtocolElement {
        /**
         * Executes the ProtocolElement against the supplied session.
         * 
         * @param continueAfterFailure true when the execution should continue,
         * false otherwise
         * @throws Exception
         */
        void testProtocol(Session[] sessions,
                boolean continueAfterFailure) throws Exception;

        boolean isClient();
    }

    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        String result  = "ProtocolSession ( "
            + "continued = " + this.continued + TAB
            + "continuationExpected = " + this.continuationExpected + TAB
            + "maxSessionNumber = " + this.maxSessionNumber + TAB
            + "testElements = " + this.testElements + TAB
            + "elementsIterator = " + this.elementsIterator + TAB
            + "sessions = " + this.sessions + TAB
            + "nextTest = " + this.nextTest + TAB
            + "continueAfterFailure = " + this.continueAfterFailure + TAB
            + " )";
    
        return result;
    }
    
    
}

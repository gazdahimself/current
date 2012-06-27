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

package org.apache.james.imap.tester.external;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.apache.james.imap.tester.base.HostSystem;

public class ExternalHostSystem implements HostSystem {

    public static final HostSystem createLocalImap() {
        final ExternalHostSystem result = new ExternalHostSystem("localhost",
                143, new SystemLoggingMonitor());
        return result;
    }

    private final InetSocketAddress address;

    private final Monitor monitor;

    public ExternalHostSystem(final String host, final int port,
            final Monitor monitor) {
        super();
        this.address = new InetSocketAddress(host, port);
        this.monitor = monitor;
    }

    public boolean addUser(String user, String password) throws Exception {
        monitor.note("Please ensure user '" + user + "' with password '"
                + password + "' exists.");
        return true;
    }

    public Session newSession(Continuation continuation) throws Exception {
        final SocketChannel channel = SocketChannel.open(address);
        channel.configureBlocking(false);
        final SessionImpl result = new SessionImpl(channel, monitor);
        return result;
    }

    public void reset() throws Exception {
        monitor.note("Please reset system.");
    }

    public interface Monitor {
        void note(String message);
    }

    public static final class SystemLoggingMonitor implements Monitor {

        public void note(String message) {
            System.out.println(message);
        }

    }

    private final static class SessionImpl implements Session {

        private static final byte[] CRLF = { '\r', '\n' };

        private final SocketChannel socket;

        private final Monitor monitor;

        private final ByteBuffer readBuffer;

        private final Charset ascii;

        private final ByteBuffer lineEndBuffer;

        private boolean first = true;

        public SessionImpl(final SocketChannel socket, final Monitor monitor) {
            super();
            this.socket = socket;
            this.monitor = monitor;
            readBuffer = ByteBuffer.allocateDirect(2048);
            ascii = Charset.forName("US-ASCII");
            lineEndBuffer = ByteBuffer.wrap(CRLF);
        }

        public String readLine() throws Exception {
            StringBuffer buffer = new StringBuffer();
            readlineInto(buffer);
            final String result;
            if (first) {
                // fake shabang
                monitor.note("<-" + buffer.toString());
                result = "* OK IMAP4rev1 Server ready";
                first = false;
            } else {
                result = buffer.toString();
                monitor.note("<-" + result);
            }
            return result;
        }

        private void readlineInto(StringBuffer buffer) throws Exception {
            while (socket.read(readBuffer) == 0)
                ;
            readBuffer.flip();
            while (readOneMore(buffer))
                ;
            readBuffer.compact();
        }

        private boolean readOneMore(StringBuffer buffer) throws Exception {
            final boolean result;
            if (readBuffer.hasRemaining()) {
                char next = (char) readBuffer.get();
                if (next == '\n') {
                    result = false;
                } else if (next == '\r') {
                    result = true;
                } else {
                    buffer.append(next);
                    result = true;
                }
            } else {
                readBuffer.clear();
                readlineInto(buffer);
                result = true;
            }
            return result;
        }

        public void start() throws Exception {
            while (!socket.finishConnect()) {
                monitor.note("connecting...");
                Thread.sleep(10);
            }
        }

        public void stop() throws Exception {
            monitor.note("closing");
            socket.close();
        }

        public void writeLine(String line) throws Exception {
            monitor.note("-> " + line);
            ByteBuffer writeBuffer = ascii.encode(line);
            while (writeBuffer.hasRemaining()) {
                socket.write(writeBuffer);
            }
            lineEndBuffer.rewind();
            while (lineEndBuffer.hasRemaining()) {
                socket.write(lineEndBuffer);
            }
        }
    }

    public void afterTests() throws Exception {
        // Do nothing        
    }

    public void beforeTests() throws Exception {
        // Do nothing
    }
}

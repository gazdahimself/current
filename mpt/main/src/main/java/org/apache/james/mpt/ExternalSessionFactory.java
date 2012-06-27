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

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.apache.james.mpt.HostSystem.Continuation;

/**
 * Session factory creates session which connection to a server port.
 */
public class ExternalSessionFactory implements SessionFactory {

    public static final String IMAP_SHABANG = "* OK IMAP4rev1 Server ready";
    protected final InetSocketAddress address;
    protected final Monitor monitor;
    protected final String shabang;

    public ExternalSessionFactory(final String host, final int port, final Monitor monitor, final String shabang) {
        super();
        this.address = new InetSocketAddress(host, port);
        this.monitor = monitor;
        this.shabang = shabang;
    }

    public Session newSession(Continuation continuation) throws Exception {
        monitor.note("Connecting to " + address.getHostName() + ":" + address.getPort());
        final SocketChannel channel = SocketChannel.open(address);
        channel.configureBlocking(false);
        final ExternalSession result = new ExternalSession(channel, monitor, shabang);
        return result;
    }

    public void reset() throws Exception {
        monitor.note("Please reset system.");
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

        String retValue = "ExternalSessionFactory ( "
            + "address = " + this.address + TAB
            + "monitor = " + this.monitor + TAB
            + "shabang = " + this.shabang + TAB
            + " )";

        return retValue;
    }
}
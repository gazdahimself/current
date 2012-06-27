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
package org.apache.james.dnsservice.library;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.mailet.HostAddress;
import org.slf4j.Logger;

/**
 * 
 *
 */
public class MXHostAddressIterator implements Iterator<HostAddress> {

    private Iterator<HostAddress> addresses = null;
    private Iterator<String> hosts;
    private DNSService dns;
    private boolean useSingleIP;
    private Logger logger;
    private int defaultPort;

    public MXHostAddressIterator(Iterator<String> hosts, DNSService dns, boolean useSingleIP, Logger logger) {
        this(hosts, 25, dns, useSingleIP, logger);
    }

    public MXHostAddressIterator(Iterator<String> hosts, int defaultPort, DNSService dns, boolean useSingleIP, Logger logger) {
        this.hosts = hosts;
        this.dns = dns;
        this.useSingleIP = useSingleIP;
        this.logger = logger;
        this.defaultPort = defaultPort;
        
        init();
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
     
        return addresses.hasNext();
    }

    private void init() {
        final List<HostAddress> hAddresses = new ArrayList<HostAddress>();
        while (hosts.hasNext()) {
            String nextHostname = (String) hosts.next();
            final String hostname;
            final String port;

            int idx = nextHostname.indexOf(':');
            if (idx > 0) {
                port = nextHostname.substring(idx + 1);
                hostname = nextHostname.substring(0, idx);
            } else {
                hostname = nextHostname;
                port = defaultPort + "";
            }

            InetAddress[] addrs = null;
            try {
                if (useSingleIP) {
                    addrs = new InetAddress[] { dns.getByName(hostname) };
                } else {
                    addrs = dns.getAllByName(hostname);
                }
                for (int i = 0; i < addrs.length; i++) {
                    hAddresses.add(new org.apache.mailet.HostAddress(hostname, "smtp://" + addrs[i].getHostAddress() + ":" + port));
                }
            } catch (UnknownHostException uhe) {
                // this should never happen, since we just got
                // this host from mxHosts, which should have
                // already done this check.
                StringBuffer logBuffer = new StringBuffer(128).append("Couldn't resolve IP address for discovered host ").append(hostname).append(".");
                logger.error(logBuffer.toString());
            }

        }
        addresses = hAddresses.iterator();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public HostAddress next() {
        return addresses.next();
    }

    /**
     * Not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove not supported by this iterator");
    }

}

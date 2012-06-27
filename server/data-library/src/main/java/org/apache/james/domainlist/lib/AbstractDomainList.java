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

package org.apache.james.domainlist.lib;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.slf4j.Logger;

/**
 * All implementations of the DomainList interface should extends this abstract
 * class
 */
public abstract class AbstractDomainList implements DomainList, LogEnabled, Configurable {
    private DNSService dns;
    private boolean autoDetect = true;
    private boolean autoDetectIP = true;
    private Logger logger;
    private String defaultDomain;

    @Resource(name = "dnsservice")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }

    public void setLog(Logger logger) {
        this.logger = logger;
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * @see org.apache.james.lifecycle.api.Configurable#configure(HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        defaultDomain = config.getString("defaultDomain", "localhost");

        setAutoDetect(config.getBoolean("autodetect", true));
        setAutoDetectIP(config.getBoolean("autodetectIP", true));
    }

    /**
     * @see org.apache.james.domainlist.api.DomainList#getDefaultDomain()
     */
    public String getDefaultDomain() throws DomainListException {
        return defaultDomain;
    }

    /**
     * @see org.apache.james.domainlist.api.DomainList#getDomains()
     */
    public String[] getDomains() throws DomainListException {
        List<String> domains = getDomainListInternal();
        if (domains != null) {

            String hostName = null;
            try {
                hostName = getDNSServer().getHostName(getDNSServer().getLocalHost());
            } catch (UnknownHostException ue) {
                hostName = "localhost";
            }

            getLogger().info("Local host is: " + hostName);

            if (autoDetect == true && (!hostName.equals("localhost"))) {
                domains.add(hostName.toLowerCase(Locale.US));
            }

            if (autoDetectIP == true) {
                domains.addAll(getDomainsIP(domains, dns, getLogger()));
            }

            if (getLogger().isInfoEnabled()) {
                for (Iterator<String> i = domains.iterator(); i.hasNext();) {
                    getLogger().debug("Handling mail for: " + i.next());
                }
            }
            if (domains.isEmpty()) {
                return null;
            } else {
                return domains.toArray(new String[domains.size()]);
            }
        } else {
            return null;
        }
    }

    /**
     * Return a List which holds all ipAddress of the domains in the given List
     * 
     * @param domains
     *            List of domains
     * @return domainIP List of ipaddress for domains
     */
    private static List<String> getDomainsIP(List<String> domains, DNSService dns, Logger log) {
        List<String> domainIP = new ArrayList<String>();
        if (domains.size() > 0) {
            for (int i = 0; i < domains.size(); i++) {
                List<String> domList = getDomainIP(domains.get(i).toString(), dns, log);

                for (int i2 = 0; i2 < domList.size(); i2++) {
                    if (domainIP.contains(domList.get(i2)) == false) {
                        domainIP.add(domList.get(i2));
                    }
                }
            }
        }
        return domainIP;
    }

    /**
     * @see #getDomainsIP(List, DNSService, Logger)
     */
    private static List<String> getDomainIP(String domain, DNSService dns, Logger log) {
        List<String> domainIP = new ArrayList<String>();
        try {
            InetAddress[] addrs = dns.getAllByName(domain);
            for (int j = 0; j < addrs.length; j++) {
                String ip = addrs[j].getHostAddress();
                if (domainIP.contains(ip) == false) {
                    domainIP.add(ip);
                }
            }
        } catch (UnknownHostException e) {
            log.error("Cannot get IP address(es) for " + domain);
        }
        return domainIP;
    }

    /**
     * Set to true to autodetect the hostname of the host on which james is
     * running, and add this to the domain service Default is true
     * 
     * @param autoDetect
     *            set to <code>false</code> for disable
     */
    public synchronized void setAutoDetect(boolean autoDetect) {
        getLogger().info("Set autodetect to: " + autoDetect);
        this.autoDetect = autoDetect;
    }

    /**
     * Set to true to lookup the ipaddresses for each given domain and add these
     * to the domain service Default is true
     * 
     * @param autoDetectIP
     *            set to <code>false</code> for disable
     */
    public synchronized void setAutoDetectIP(boolean autoDetectIP) {
        getLogger().info("Set autodetectIP to: " + autoDetectIP);
        this.autoDetectIP = autoDetectIP;
    }

    /**
     * Return dnsServer
     * 
     * @return dns
     */
    protected DNSService getDNSServer() {
        return dns;
    }

    /**
     * Return domainList
     * 
     * @return List
     */
    protected abstract List<String> getDomainListInternal() throws DomainListException;

}

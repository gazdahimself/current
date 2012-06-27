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

package org.apache.james.smtpserver.fastfail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.protocols.lib.lifecycle.InitializingLifecycleAwareProtocolHandler;

public class DNSRBLHandler extends org.apache.james.protocols.smtp.core.fastfail.DNSRBLHandler implements InitializingLifecycleAwareProtocolHandler {


    private DNSService dns;

    @Resource(name = "dnsservice")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void init(Configuration config) throws ConfigurationException {
        boolean validConfig = false;
        HierarchicalConfiguration handlerConfiguration = (HierarchicalConfiguration)config;
        ArrayList<String> rblserverCollection = new ArrayList<String>();
        List<String> whiteList = handlerConfiguration.getList("rblservers.whitelist");
        if (whiteList != null) {
            for (int i = 0; i < whiteList.size(); i++) {
                String rblServerName = whiteList.get(i);
                rblserverCollection.add(rblServerName);

            }
            if (rblserverCollection != null && rblserverCollection.size() > 0) {
                setWhitelist((String[]) rblserverCollection.toArray(new String[rblserverCollection.size()]));
                rblserverCollection.clear();
                validConfig = true;
            }
        }
        List<String> blackList = handlerConfiguration.getList("rblservers.blacklist");
        if (blackList != null) {

            for (int i = 0; i < blackList.size(); i++) {
                String rblServerName = blackList.get(i);
                rblserverCollection.add(rblServerName);

            }
            if (rblserverCollection != null && rblserverCollection.size() > 0) {
                setBlacklist((String[]) rblserverCollection.toArray(new String[rblserverCollection.size()]));
                rblserverCollection.clear();
                validConfig = true;
            }
        }

        // Throw an ConfiigurationException on invalid config
        if (validConfig == false) {
            throw new ConfigurationException("Please configure whitelist or blacklist");
        }

        setGetDetail(handlerConfiguration.getBoolean("getDetail", false));        
    }

    @Override
    public void destroy() {
        // Do nothing
    }
    @Override
    protected boolean resolve(String ip) {
        try {
            dns.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
    @Override
    protected Collection<String> resolveTXTRecords(String ip) {
        return dns.findTXTRecords(ip);
    }
    
    
}

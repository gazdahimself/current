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

package org.apache.james.domainlist.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.lib.AbstractDomainList;
import org.apache.james.lifecycle.api.Configurable;

/**
 * Mimic the old behavior of JAMES
 */
public class XMLDomainList extends AbstractDomainList implements Configurable {

    private List<String> domainNames = new ArrayList<String>();

    private boolean managementDisabled = false;

    /**
     * @see
     * org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        super.configure(config);
        List<String> serverNameConfs = config.getList("domainnames.domainname");
        for (int i = 0; i < serverNameConfs.size(); i++) {
            try {
                addDomain(serverNameConfs.get(i));
            } catch (DomainListException e) {
                throw new ConfigurationException("Unable to add domain to memory", e);
            }
        }

        managementDisabled = true;

    }

    /**
     * @see org.apache.james.domainlist.lib.AbstractDomainList#getDomainListInternal()
     */
    protected List<String> getDomainListInternal() {

        return new ArrayList<String>(domainNames);
    }

    /**
     * @see org.apache.james.domainlist.api.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domains) throws DomainListException {
        return domainNames.contains(domains);
    }

    /**
     * @see
     * org.apache.james.domainlist.api.DomainList#addDomain(java.lang.String)
     */
    public void addDomain(String domain) throws DomainListException {
        // TODO: Remove later. Temporary fix to get sure no domains can be added
        // to the XMLDomainList
        if (managementDisabled)
            throw new DomainListException("Read-Only DomainList implementation");

        String newDomain = domain.toLowerCase(Locale.US);
        if (containsDomain(newDomain) == false) {
            domainNames.add(newDomain);
        }

    }

    /**
     * @see
     * org.apache.james.domainlist.api.DomainList#removeDomain(java.lang.String)
     */
    public void removeDomain(String domain) throws DomainListException {
        // TODO: Remove later. Temporary fix to get sure no domains can be added
        // to the XMLDomainList
        if (managementDisabled)
            throw new DomainListException("Read-Only DomainList implementation");

        domainNames.remove(domain.toLowerCase(Locale.US));
    }
}

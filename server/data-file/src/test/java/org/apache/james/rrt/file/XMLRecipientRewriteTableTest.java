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
package org.apache.james.rrt.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTableTest;
import org.apache.james.rrt.lib.RecipientRewriteTableUtil;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * Test the XML Virtual User Table implementation.
 */
public class XMLRecipientRewriteTableTest extends AbstractRecipientRewriteTableTest {

    private DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();

    @Before
    @Override
    public void setUp() throws Exception {
        defaultConfiguration.setDelimiterParsingDisabled(true);
        super.setUp();
    }

    @Override
    protected AbstractRecipientRewriteTable getRecipientRewriteTable() throws Exception {
        XMLRecipientRewriteTable localVirtualUserTable = new XMLRecipientRewriteTable();
        localVirtualUserTable.setLog(LoggerFactory.getLogger("MockLog"));
        return localVirtualUserTable;
    }

    /**
     * @throws RecipientRewriteTableException
     * @see org.apache.james.rrt.lib.AbstractRecipientRewriteTableTest#addMapping(java.lang.String,
     *      java.lang.String, java.lang.String, int)
     */
    @Override
    protected boolean addMapping(String user, String domain, String mapping, int type) throws
            RecipientRewriteTableException {

        Collection<String> mappings = virtualUserTable.getUserDomainMappings(user, domain);

        if (mappings == null) {
            mappings = new ArrayList<String>();
        } else {
            removeMappingsFromConfig(user, domain, mappings);
        }

        if (type == ERROR_TYPE) {
            mappings.add(RecipientRewriteTable.ERROR_PREFIX + mapping);
        } else if (type == REGEX_TYPE) {
            mappings.add(RecipientRewriteTable.REGEX_PREFIX + mapping);
        } else if (type == ADDRESS_TYPE) {
            mappings.add(mapping);
        } else if (type == ALIASDOMAIN_TYPE) {
            mappings.add(RecipientRewriteTable.ALIASDOMAIN_PREFIX + mapping);
        }

        if (mappings.size() > 0) {
            defaultConfiguration.addProperty("mapping", user + "@" + domain + "=" + RecipientRewriteTableUtil.
                    CollectionToMapping(mappings));
        }

        try {
            virtualUserTable.configure(defaultConfiguration);
        } catch (Exception e) {
            if (mappings.size() > 0) {
                return false;
            } else {
                return true;
            }
        }

        return true;

    }

    /**
     * @throws RecipientRewriteTableException
     * @see org.apache.james.rrt.lib.AbstractRecipientRewriteTableTest#removeMapping(java.lang.String,
     *      java.lang.String, java.lang.String, int)
     */
    @Override
    protected boolean removeMapping(String user, String domain, String mapping, int type) throws
            RecipientRewriteTableException {

        Collection<String> mappings = virtualUserTable.getUserDomainMappings(user, domain);

        if (mappings == null) {
            return false;
        }

        removeMappingsFromConfig(user, domain, mappings);

        if (type == ERROR_TYPE) {
            mappings.remove(RecipientRewriteTable.ERROR_PREFIX + mapping);
        } else if (type == REGEX_TYPE) {
            mappings.remove(RecipientRewriteTable.REGEX_PREFIX + mapping);
        } else if (type == ADDRESS_TYPE) {
            mappings.remove(mapping);
        } else if (type == ALIASDOMAIN_TYPE) {
            mappings.remove(RecipientRewriteTable.ALIASDOMAIN_PREFIX + mapping);
        }

        if (mappings.size() > 0) {
            defaultConfiguration.addProperty("mapping", user + "@" + domain + "=" + RecipientRewriteTableUtil.
                    CollectionToMapping(mappings));
        }

        try {
            virtualUserTable.configure(defaultConfiguration);
        } catch (Exception e) {
            if (mappings.size() > 0) {
                return false;
            } else {
                return true;
            }
        }

        return true;

    }

    @SuppressWarnings("unchecked")
    private void removeMappingsFromConfig(String user, String domain, Collection<String> mappings) {
        List<String> confs = defaultConfiguration.getList("mapping");
        List<String> stored = new ArrayList<String>();
        for (int i = 0; i < confs.size(); i++) {
            String c = confs.get(i);
            String mapping = user + "@" + domain + "=" + RecipientRewriteTableUtil.CollectionToMapping(mappings);

            if (!c.equalsIgnoreCase(mapping)) {
                stored.add(c);
            }
        }
        // clear old values
        defaultConfiguration.clear();
        // add stored mappings
        for (int i = 0; i < stored.size(); i++) {
            defaultConfiguration.addProperty("mapping", stored.get(i));
        }
    }
}

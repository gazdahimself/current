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
package org.apache.james.container.spring.bean.factory.mailrepositorystore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.container.spring.bean.AbstractBeanFactory;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Provides a registry of mail repositories. A mail repository is uniquely
 * identified by its destinationURL, type and model.
 */
public class MailRepositoryStoreBeanFactory extends AbstractBeanFactory implements MailRepositoryStore, LogEnabled, Configurable {

    /**
     * Map of [destinationURL + type]->Repository
     */
    private Map<String, MailRepository> repositories;

    /**
     * Map of [protocol(destinationURL) + type ]->classname of repository;
     */
    private Map<String, String> classes;

    /**
     * Map of [protocol(destinationURL) + type ]->default config for repository.
     */
    private Map<String, HierarchicalConfiguration> defaultConfigs;

    /**
     * The configuration used by the instance
     */
    private HierarchicalConfiguration configuration;

    /**
     * The Logger
     */
    private Logger logger;

    /**
     * @see
     * org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException {
        this.configuration = configuration;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws Exception {

        getLogger().info("JamesMailStore init...");

        repositories = new ReferenceMap();
        classes = new HashMap<String, String>();
        defaultConfigs = new HashMap<String, HierarchicalConfiguration>();
        List<HierarchicalConfiguration> registeredClasses = configuration.configurationsAt("mailrepositories.mailrepository");
        for (int i = 0; i < registeredClasses.size(); i++) {
            registerRepository(registeredClasses.get(i));
        }

    }

    /**
     * <p>
     * Registers a new mail repository type in the mail store's registry based
     * upon a passed in <code>Configuration</code> object.
     * </p>
     * 
     * <p>
     * This is presumably synchronized to prevent corruption of the internal
     * registry.
     * </p>
     * 
     * @param repConf
     *            the Configuration object used to register the repository
     * @throws ConfigurationException
     *             if an error occurs accessing the Configuration object
     */
    @SuppressWarnings("unchecked")
    public synchronized void registerRepository(HierarchicalConfiguration repConf) throws ConfigurationException {

        String className = repConf.getString("[@class]");

        boolean infoEnabled = getLogger().isInfoEnabled();

        List<String> protocols = repConf.getList("protocols.protocol");

        for (int i = 0; i < protocols.size(); i++) {

            String protocol = protocols.get(i);

            HierarchicalConfiguration defConf = null;

            if (repConf.getKeys("config").hasNext()) {
                // Get the default configuration for these protocol/type
                // combinations.
                defConf = repConf.configurationAt("config");
            }

            String key = protocol;

            if (infoEnabled) {
                StringBuffer infoBuffer = new StringBuffer(128).append("Registering Repository instance of class ").append(className).append(" to handle ").append(protocol).append(" protocol requests for repositories with key ").append(key);
                getLogger().info(infoBuffer.toString());
            }

            if (classes.get(key) != null) {
                throw new ConfigurationException("The combination of protocol and type comprise a unique key for repositories.  This constraint has been violated.  Please check your repository configuration.");
            }

            classes.put(key, className);

            if (defConf != null) {
                defaultConfigs.put(key, defConf);
            }
        }

    }

    /**
     * This method accept a Configuration object as hint and return the
     * corresponding MailRepository. The Configuration must be in the form of:
     * 
     * <pre>
     * &lt;repository destinationURL="[URL of this mail repository]"
     *             type="[repository type ex. OBJECT or STREAM or MAIL etc.]"
     *             model="[repository model ex. PERSISTENT or CACHE etc.]"&gt;
     *   [addition configuration]
     * &lt;/repository&gt;
     * </pre>
     * 
     * @param destination
     *            the destinationURL used to look up the repository
     * @return the selected repository
     * @throws MailRepositoryStoreException
     *             if any error occurs while parsing the Configuration or
     *             retrieving the MailRepository
     */
    public synchronized MailRepository select(String destination) throws MailRepositoryStoreException {

        String protocol = null;

        int idx = destination.indexOf(':');
        if (idx == -1)
            throw new MailRepositoryStoreException("Destination is malformed. Must be a valid URL: " + destination);
        protocol = destination.substring(0, idx);

        String repID = destination;
        MailRepository reply = repositories.get(repID);
        StringBuffer logBuffer = null;
        if (reply != null) {
            if (getLogger().isDebugEnabled()) {
                logBuffer = new StringBuffer(128).append("obtained repository: ").append(repID).append(",").append(reply.getClass());
                getLogger().debug(logBuffer.toString());
            }
            return reply;
        } else {
            String key = protocol;
            String repClass = (String) classes.get(key);
            if (getLogger().isDebugEnabled()) {
                logBuffer = new StringBuffer(128).append("obtained repository: ").append(repClass).append(" to handle: ").append(protocol).append(" with key ").append(key);
                getLogger().debug(logBuffer.toString());
            }

            // If default values have been set, create a new repository
            // configuration element using the default values
            // and the values in the selector.
            // If no default values, just use the selector.
            final CombinedConfiguration config = new CombinedConfiguration();
            HierarchicalConfiguration defConf = defaultConfigs.get(key);
            if (defConf != null) {
                config.addConfiguration(defConf);
            }
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
            builder.addProperty("[@destinationURL]", destination);
            config.addConfiguration(builder);

            try {
                // Use the classloader which is used for bean instance stuff
                Class<MailRepository> clazz = (Class<MailRepository>) getBeanFactory().getBeanClassLoader().loadClass(repClass);
                reply = (MailRepository) getBeanFactory().autowire(clazz, ConfigurableListableBeanFactory.AUTOWIRE_NO, false);

                if (reply instanceof LogEnabled) {
                    ((LogEnabled) reply).setLog(logger);
                }

                if (reply instanceof Configurable) {
                    ((Configurable) reply).configure(config);
                }

                reply = (MailRepository) getBeanFactory().initializeBean(reply, key);

                repositories.put(repID, reply);
                if (getLogger().isInfoEnabled()) {
                    logBuffer = new StringBuffer(128).append("added repository: ").append(repID).append("->").append(repClass);
                    getLogger().info(logBuffer.toString());
                }
                return reply;
            } catch (Exception e) {
                if (getLogger().isWarnEnabled()) {
                    getLogger().warn("Exception while creating repository:" + e.getMessage(), e);
                }
                throw new MailRepositoryStoreException("Cannot find or init repository", e);
            }
        }

    }

    /**
     * @see org.apache.james.mailrepository.api.MailRepositoryStore#getUrls()
     */
    public synchronized List<String> getUrls() {
        return new ArrayList<String>(repositories.keySet());
    }

    public void setLog(Logger logger) {
        this.logger = logger;
    }

    private Logger getLogger() {
        return logger;
    }

}

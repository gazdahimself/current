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
package org.apache.james.container.spring.mailbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.copier.MailboxCopier;
import org.apache.james.mailbox.exception.MailboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link MailboxCopier} support via JMX
 */
public class MailboxCopierManagement implements MailboxCopierManagementMBean, ApplicationContextAware {

    /**
     * The Logger.
     */
    private static Logger log = LoggerFactory.getLogger(MailboxCopierManagement.class.getName());

    private MailboxCopier copier;
    private ApplicationContext context;

    @Resource(name = "mailboxcopier")
    public void setMailboxCopier(MailboxCopier copier) {
        this.copier = copier;
    }

    /**
     * @see
     * org.apache.james.container.spring.mailbox.MailboxCopierManagementMBean
     * #getMailboxManagerBeans()
     */
    public Map<String, String> getMailboxManagerBeans() {

        Map<String, String> bMap = new HashMap<String, String>();

        Map<String, MailboxManager> beans = context.getBeansOfType(MailboxManager.class);

        for (Map.Entry<String, MailboxManager> entry : beans.entrySet()) {
            String name = entry.getValue().getClass().getName();
            bMap.put(entry.getKey(), name);
        }

        return bMap;
    }

    /**
     * @see
     * org.apache.james.container.spring.mailbox.MailboxCopierManagementMBean
     * #copy(java.lang.String, java.lang.String)
     */
    public void copy(String srcBean, String dstBean) throws Exception {
        if (srcBean.equals(dstBean))
            throw new IllegalArgumentException("srcBean and dstBean can not have the same name!");
        try {
            copier.copyMailboxes(context.getBean(srcBean, MailboxManager.class), context.getBean(dstBean, MailboxManager.class));
        } catch (BeansException e) {
            log.error("An exception occured during the copy process", e);
            throw new Exception(e.getMessage());
        } catch (MailboxException e) {
            log.error("An exception occured during the copy process", e);
            throw new Exception(e.getMessage());
        } catch (IOException e) {
            log.error("An exception occured during the copy process", e);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * @see
     * org.springframework.context.ApplicationContextAware#setApplicationContext
     * (org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

}

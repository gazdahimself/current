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
package org.apache.james.queue.activemq;

import javax.jms.ConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.james.queue.jms.JMSMailQueue;
import org.apache.james.queue.jms.JMSMailQueueTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQMailQueueTest extends JMSMailQueueTest {

    @Override
    protected BrokerService createBroker() throws Exception {
        BrokerService broker = super.createBroker();
        // Enable statistics
        broker.setPlugins(new BrokerPlugin[] { new StatisticsBrokerPlugin() });
        broker.setEnableStatistics(true);

        return broker;
    }

    @Override
    protected JMSMailQueue createQueue(ConnectionFactory factory, String queueName) {
        Logger log = LoggerFactory.getLogger("MockLog");
        // slf4j can't set programmatically any log level. It's just a facade
        // log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        ActiveMQMailQueue localQueue = new ActiveMQMailQueue(factory, queueName, useBlobMessages(), log);
        return localQueue;
    }

    protected boolean useBlobMessages() {
        return false;
    }

}

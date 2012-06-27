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
package org.apache.james.mailbox.jcr;


/**
 * Constants for JCR
 *
 */
public interface JCRImapConstants {

    
	/**
	 * Delimiter for Nodes
	 */
	String NODE_DELIMITER = "/";
    char NODE_DELIMITER_CHAR = '/';
    char NS_PREFIX_DELIMITER = ':';
    
    String JCR_ROOT = JCRUtils.fq("jcr", "root");
    
    String NS_PREFIX = "jamesMailbox";

    String MAILBOX_TYPE = JCRUtils.fq(NS_PREFIX, "mailbox");
    String MAILBOX_UID_VALIDITY_PROP = JCRUtils.fq(NS_PREFIX, "mailboxUidValidity");
    String MAILBOX_NAME_PROP = JCRUtils.fq(NS_PREFIX, "mailboxName");
    String MAILBOX_LAST_UID_PROP = JCRUtils.fq(NS_PREFIX, "mailboxLastUid");
    String MAILBOX_OWNER_PROP = JCRUtils.fq(NS_PREFIX, "mailboxOwner");
    String MAILBOX_OWNER_IS_GROUP_PROP = JCRUtils.fq(NS_PREFIX, "mailboxOwnerIsGroup");
    String MAILBOX_HIGHEST_MOD_SEQ_PROP = JCRUtils.fq(NS_PREFIX, "mailboxHighestModSeq");

    String USER_TYPE = JCRUtils.fq(NS_PREFIX, "user");
    String SUBSCRIPTION_USER_PROP = JCRUtils.fq(NS_PREFIX, "user");
    String SUBSCRIPTION_MAILBOXES_PROP =  JCRUtils.fq(NS_PREFIX, "subscriptionMailboxes");
    String MAILBOXES_PATH = "mailboxes";

}

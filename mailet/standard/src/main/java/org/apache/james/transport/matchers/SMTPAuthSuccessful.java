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



package org.apache.james.transport.matchers;

import org.apache.mailet.base.GenericMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import java.util.Collection;

/**
 * <P>Matches mails that are sent by an SMTP authenticated user.</P>
 * <P>If the sender was not authenticated it will not match.</P>
 * <PRE><CODE>
 * &lt;mailet match=&quot;SMTPAuthSuccessful&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * </CODE></PRE>
 *
 * @version CVS $Revision: 1089122 $ $Date: 2011-04-05 18:34:10 +0200 (Tue, 05 Apr 2011) $
 * @since 2.2.0
 */
public class SMTPAuthSuccessful extends GenericMatcher {
    
    /**
     * The mail attribute holding the SMTP AUTH user name, if any.
     */
    private final static String SMTP_AUTH_USER_ATTRIBUTE_NAME = "org.apache.james.SMTPAuthUser";
    
    public Collection<MailAddress> match(Mail mail) {
        String authUser = (String) mail.getAttribute(SMTP_AUTH_USER_ATTRIBUTE_NAME);
        if (authUser != null) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}

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



package org.apache.james.transport.mailets;

import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;

/**
 * <p>This mailet redirects the mail to the named processor</p>
 *
 * <p>Sample configuration:</p>
 * <pre><code>
 * &lt;mailet match="All" class="ToProcessor"&gt;
 *   &lt;processor&gt;spam&lt;/processor&gt;
 *   &lt;notice&gt;Notice attached to the message (optional)&lt;/notice&gt;
 * &lt;/mailet&gt;
 * </code></pre>
 *
 */
public class ToProcessor extends GenericMailet {

    /**
     * Controls certain log messages
     */
    private boolean isDebug = false;

    /**
     * The name of the processor to which this mailet forwards mail
     */
    String processor;

    /**
     * The error message to attach to the forwarded message
     */
    String noticeText = null;

    /**
     * Initialize the mailet
     *
     * @throws MailetException if the processor parameter is missing
     */
    public void init() throws MailetException {
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();
        processor = getInitParameter("processor");
        if (processor == null) {
            throw new MailetException("processor parameter is required");
        }
        noticeText = getInitParameter("notice");
    }

    /**
     * Deliver a mail to the processor.
     *
     * @param mail the mail to process
     *
     * @throws MessagingException in all cases
     */
    public void service(Mail mail) throws MessagingException {
        if (isDebug) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                        .append("Sending mail ")
                        .append(mail)
                        .append(" to ")
                        .append(processor);
            log(logBuffer.toString());
        }
        mail.setState(processor);
        if (noticeText != null) {
            if (mail.getErrorMessage() == null) {
                mail.setErrorMessage(noticeText);
            } else {
                StringBuffer errorMessageBuffer =
                    new StringBuffer(256)
                            .append(mail.getErrorMessage())
                            .append("\r\n")
                            .append(noticeText);
                mail.setErrorMessage(errorMessageBuffer.toString());
            }
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ToProcessor Mailet";
    }
}

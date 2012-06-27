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
package org.apache.jsieve.mailet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Performs the filing of a mail into a specified destination. 
 * <h4>Thread Safety</h4>
 * <p>An instance maybe safe accessed concurrently by multiple threads.</p>
 */
public class FileIntoAction implements MailAction {
    
    private static final char HIERARCHY_DELIMITER = '.';

    public void execute(Action action, Mail mail, ActionContext context) throws MessagingException {
        if (action instanceof ActionFileInto) {
            final ActionFileInto fileIntoAction = (ActionFileInto) action;
            execute(fileIntoAction, mail, context);
        }
    }

    /**
     * <p>
     * Executes the passed ActionFileInto.
     * </p>
     * 
     * <p>
     * This implementation accepts any destination with the root of <code>INBOX</code>.
     * </p>
     * 
     * <p>
     * As the current POP3 server does not support sub-folders, the mail is
     * stored in the INBOX for the recipient of the mail and the full intended
     * destination added as a prefix to the message's subject.
     * </p>
     * 
     * <p>
     * When IMAP support is added to James, it will be possible to support
     * sub-folders of <code>INBOX</code> fully.
     * </p>
     * 
     * @param anAction
     * @param aMail
     * @param context not null
     * @throws MessagingException
     */
    @SuppressWarnings("deprecation")
    public void execute(ActionFileInto anAction, Mail aMail, final ActionContext context) throws MessagingException
    {
        String destinationMailbox = anAction.getDestination();
        MailAddress recipient;
        boolean delivered = false;
        try
        {
            recipient = ActionUtils.getSoleRecipient(aMail);
            MimeMessage localMessage = createMimeMessage(aMail, recipient);
            
            if (!(destinationMailbox.length() > 0 
                    && destinationMailbox.charAt(0) == HIERARCHY_DELIMITER)) {
                destinationMailbox =  HIERARCHY_DELIMITER + destinationMailbox;
            }
            
            final String mailbox = destinationMailbox.replace(HIERARCHY_DELIMITER, '/');
            final String host;
            if (mailbox.charAt(0) == '/') {
                host = "@localhost";
            } else {
                host = "@localhost/";
            }
            final String url = "mailbox://" + recipient.getUser() + host + mailbox;
            //TODO: copying this message so many times seems a waste
            context.post(url, localMessage);
            delivered = true;
        }
        catch (MessagingException ex)
        {
            final Log log = context.getLog();
            if (log.isDebugEnabled()) {
                log.debug("Error while storing mail into. "+destinationMailbox, ex);
            }
            throw ex;
        }
        finally
        {
            // Ensure the mail is always ghosted
            aMail.setState(Mail.GHOST);
        }
        if (delivered)
        {
            final Log log = context.getLog();
            if (log.isDebugEnabled()) {
                log.debug("Filed Message ID: "
                    + aMail.getMessage().getMessageID()
                    + " into destination: \""
                    + destinationMailbox + "\"");
            }
        }
    }
    
    private static MimeMessage createMimeMessage(Mail aMail, MailAddress recipient) throws MessagingException {
        // Adapted from LocalDelivery Mailet
        // Add qmail's de facto standard Delivered-To header
        MimeMessage localMessage = new MimeMessage(aMail.getMessage())
        {
            protected void updateHeaders() throws MessagingException
            {
                if (getMessageID() == null)
                    super.updateHeaders();
                else
                    modified = false;
            }
        };
        localMessage.addHeader("Delivered-To", recipient.toString());

        localMessage.saveChanges();
        return localMessage;
    }
}

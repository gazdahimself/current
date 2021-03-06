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

package org.apache.hupa.server.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

import net.customware.gwt.dispatch.shared.ActionException;

import org.apache.commons.logging.Log;
import org.apache.hupa.server.IMAPStoreCache;
import org.apache.hupa.server.preferences.UserPreferencesStorage;
import org.apache.hupa.server.utils.MessageUtils;
import org.apache.hupa.shared.rpc.ForwardMessage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

/**
 * Handler which handles the forwarding of a message
 * 
 */
public class ForwardMessageHandler extends AbstractSendMessageHandler<ForwardMessage> {

    @Inject
    public ForwardMessageHandler(Log logger, IMAPStoreCache store, Provider<HttpSession> provider, UserPreferencesStorage preferences, @Named("SMTPServerAddress") String address, @Named("SMTPServerPort") int port,
            @Named("SMTPAuth") boolean auth, @Named("SMTPS") boolean useSSL) {
        super(logger, store, provider, preferences, address, port, auth, useSSL);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected List getAttachments(ForwardMessage action) throws MessagingException, ActionException {
        List<?> items = new ArrayList();
        IMAPStore store = cache.get(getUser());

        IMAPFolder folder = (IMAPFolder) store.getFolder(action.getFolder().getFullName());
        if (folder.isOpen() == false) {
            folder.open(Folder.READ_ONLY);
        }
        // Put the original attachments in the list 
        Message msg = folder.getMessageByUID(action.getReplyMessageUid());
        try {
            items = MessageUtils.extractMessageAttachments(logger, msg.getContent());
            logger.debug("Forwarding a message, extracted: " + items.size() + " from original.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Put in the list the attachments uploaded by the user
        items.addAll(super.getAttachments(action));
        return items;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.customware.gwt.dispatch.server.ActionHandler#getActionType()
     */
    public Class<ForwardMessage> getActionType() {
        return ForwardMessage.class;
    }

}

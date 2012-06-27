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

package org.apache.james.lmtpserver;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.lmtpserver.hook.MailboxDeliverToRecipientHandler;
import org.apache.james.protocols.api.handler.CommandDispatcher;
import org.apache.james.protocols.api.handler.CommandHandlerResultLogger;
import org.apache.james.protocols.lib.handler.HandlersPackage;
import org.apache.james.protocols.lmtp.core.LhloCmdHandler;
import org.apache.james.protocols.lmtp.core.WelcomeMessageHandler;
import org.apache.james.protocols.smtp.core.ExpnCmdHandler;
import org.apache.james.protocols.smtp.core.NoopCmdHandler;
import org.apache.james.protocols.smtp.core.PostmasterAbuseRcptHook;
import org.apache.james.protocols.smtp.core.QuitCmdHandler;
import org.apache.james.protocols.smtp.core.ReceivedDataLineFilter;
import org.apache.james.protocols.smtp.core.RsetCmdHandler;
import org.apache.james.protocols.smtp.core.VrfyCmdHandler;
import org.apache.james.protocols.smtp.core.esmtp.MailSizeEsmtpExtension;
import org.apache.james.protocols.smtp.core.log.HookResultLogger;
import org.apache.james.smtpserver.AuthRequiredToRelayRcptHook;
import org.apache.james.smtpserver.JamesDataCmdHandler;
import org.apache.james.smtpserver.JamesMailCmdHandler;
import org.apache.james.smtpserver.JamesRcptCmdHandler;
import org.apache.james.smtpserver.fastfail.ValidRcptHandler;

/**
 * This class represent the base command handlers which are shipped with james.
 */
public class CoreCmdHandlerLoader implements HandlersPackage {

    private final String COMMANDDISPATCHER = CommandDispatcher.class.getName();
    private final String DATACMDHANDLER = JamesDataCmdHandler.class.getName();
    private final String EXPNCMDHANDLER = ExpnCmdHandler.class.getName();
    private final String LHLOCMDHANDLER = LhloCmdHandler.class.getName();
    private final String MAILCMDHANDLER = JamesMailCmdHandler.class.getName();
    private final String NOOPCMDHANDLER = NoopCmdHandler.class.getName();
    private final String QUITCMDHANDLER = QuitCmdHandler.class.getName();
    private final String RCPTCMDHANDLER = JamesRcptCmdHandler.class.getName();
    private final String VALIDRCPTHANDLER = ValidRcptHandler.class.getName();

    private final String RSETCMDHANDLER = RsetCmdHandler.class.getName();
    private final String VRFYCMDHANDLER = VrfyCmdHandler.class.getName();
    private final String MAILSIZEHOOK = MailSizeEsmtpExtension.class.getName();
    private final String WELCOMEMESSAGEHANDLER = WelcomeMessageHandler.class.getName();
    private final String POSTMASTERABUSEHOOK = PostmasterAbuseRcptHook.class.getName();
    private final String AUTHREQUIREDTORELAY = AuthRequiredToRelayRcptHook.class.getName();
    private final String RECEIVEDDATALINEFILTER = ReceivedDataLineFilter.class.getName();
    private final String DATALINEMESSAGEHOOKHANDLER = DataLineLMTPHandler.class.getName();
    private final String DELIVERTORECIPIENTHANDLER = MailboxDeliverToRecipientHandler.class.getName();

    // logging stuff
    private final String COMMANDHANDLERRESULTLOGGER = CommandHandlerResultLogger.class.getName();
    private final String HOOKRESULTLOGGER = HookResultLogger.class.getName();

    private final List<String> commands = new LinkedList<String>();

    public CoreCmdHandlerLoader() {
        // Insert the base commands in the Map
        commands.add(WELCOMEMESSAGEHANDLER);
        commands.add(COMMANDDISPATCHER);
        commands.add(DATACMDHANDLER);
        commands.add(EXPNCMDHANDLER);
        commands.add(LHLOCMDHANDLER);
        commands.add(MAILCMDHANDLER);
        commands.add(NOOPCMDHANDLER);
        commands.add(QUITCMDHANDLER);
        commands.add(RCPTCMDHANDLER);
        commands.add(VALIDRCPTHANDLER);
        commands.add(RSETCMDHANDLER);
        commands.add(VRFYCMDHANDLER);
        commands.add(MAILSIZEHOOK);
        commands.add(AUTHREQUIREDTORELAY);
        commands.add(POSTMASTERABUSEHOOK);
        commands.add(RECEIVEDDATALINEFILTER);
        commands.add(DATALINEMESSAGEHOOKHANDLER);
        commands.add(DELIVERTORECIPIENTHANDLER);
        // Add logging stuff
        commands.add(COMMANDHANDLERRESULTLOGGER);
        commands.add(HOOKRESULTLOGGER);
    }

    /**
     * @see HandlersPackage#getHandlers()
     */
    public List<String> getHandlers() {
        return commands;
    }
}

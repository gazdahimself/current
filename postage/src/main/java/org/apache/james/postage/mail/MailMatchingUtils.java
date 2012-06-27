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
package org.apache.james.postage.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.postage.PostageRunner;
import org.apache.james.postage.PostageRuntimeException;
import org.apache.james.postage.classloading.CachedInstanceFactory;
import org.apache.james.postage.result.MailProcessingRecord;

/**
 * helps matching, analysing and validating result mails and sent test mails
 */
public class MailMatchingUtils {

    private static Log log = LogFactory.getLog(MailMatchingUtils.class);

    /**
     * if this mail was created by postage, whatever run - but by startup check
     */
    public static boolean isPostageStartupCheckMail(MimeMessage message) {
        String headerValue = getMailIdHeader(message);
        return HeaderConstants.JAMES_POSTAGE_STARTUPCHECK_HEADER_ID.equals(headerValue);
    }

    /**
     * if this mail was created by postage, whatever run - but not by startup check
     */
    public static boolean isPostageTestMail(MimeMessage message) {
        return isPostageMail(message) && !isPostageStartupCheckMail(message);
    }

    /**
     * if this mail was created by postage, whatever run - if startup check or live test
     */
    public static boolean isPostageMail(MimeMessage message) {
        return null != getUniqueHeader(message, HeaderConstants.JAMES_POSTAGE_HEADER);
    }

    public static boolean isPostageIdHeaderPresent(MimeMessage message) {
        return null != getMailIdHeader(message);
    }

    public static String getMailIdHeader(MimeMessage message) {
        return getUniqueHeader(message, HeaderConstants.MAIL_ID_HEADER);
    }

    /**
     * if this mail was created by the currently running postage scenario - not by
     * any of those before.
     */
    public static boolean isCurrentRunnerMail(MimeMessage message) {
        String headerValue = getMailIdHeader(message);
        return headerValue != null && headerValue.startsWith(PostageRunner.getMessageIdPrefix());
    }

    public static boolean matchHeader(MimeMessage message, String header, String valueRegex) {
        return Pattern.matches(valueRegex, getUniqueHeader(message, header));
    }

    public static String getUniqueHeader(MimeMessage message, String header) {
        String[] idHeaders;
        try {
            idHeaders = message.getHeader(header);
        } catch (MessagingException e) {
            throw new PostageRuntimeException(e);
        }
        if (idHeaders != null && idHeaders.length > 0) {
            return idHeaders[0]; // there should be exactly one.
        }
        return null;
    }

    public static boolean isMatchCandidate(MimeMessage message) {
        try {
            if (!isPostageIdHeaderPresent(message)) {
                if (isPostageMail(message)) {
                    log.warn(HeaderConstants.MAIL_ID_HEADER + " header is missing from James test mail");
                }
                else log.info("skipping non-postage mail. remains on server. subject was: " + message.getSubject());
                return false;
            }
        } catch (MessagingException e) {
            log.info("failed to get mail subject for logging. remains on server. mails might be corrupt.");
            return false;
        }
        if (MailMatchingUtils.isPostageStartupCheckMail(message)) return false;
        return true;
    }
    
    public static boolean validateMail(MimeMessage message, MailProcessingRecord mailProcessingRecord) {
        String classname = getUniqueHeader(message, HeaderConstants.JAMES_POSTAGE_VALIDATORCLASSNAME_HEADER);
        MailValidator validator = (MailValidator)CachedInstanceFactory.createInstance(classname);
        if (validator == null) return false;
        
        boolean isValid = validator.validate(message, mailProcessingRecord);
        if (isValid) mailProcessingRecord.setValid();
        else log.warn("failed to validate mail");
        
        return isValid;
    }
    
    public static MimeMultipart convertToMimeMultipart(MimeMessage message) {
        try {
            return new MimeMultipart(message.getDataHandler().getDataSource());
        } catch (MessagingException e) {
            throw new RuntimeException("could not convert MimeMessage to MimeMultipart", e);
        }
    }
    
    public static int getMimePartSize(MimeMultipart parts, String mimeType) {
        if (parts != null) {
            try {
                for (int i = 0; i < parts.getCount(); i++) {
                    BodyPart bodyPart = parts.getBodyPart(i);
                    if (bodyPart.getContentType().startsWith(mimeType)) {
                        try {
                            Object content = bodyPart.getContent();
                            if (content instanceof InputStream) {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                IOUtils.copy(((InputStream) content), os);
                                return os.size();
                            } else if (content instanceof String) {
                                return ((String) content).length();
                            } else {
                                throw new IllegalStateException("Unsupported content: "+content.getClass().toString());
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("Unexpected IOException in getContent()");
                        }
                    }
                }
            } catch (MessagingException e) {
                log.info("failed to process body parts.", e);
            }
        }
        return 0;
    }
}

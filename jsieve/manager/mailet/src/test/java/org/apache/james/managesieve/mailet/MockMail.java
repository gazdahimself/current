/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.managesieve.mailet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * <code>MockMail</code>
 */
public class MockMail implements Mail {

    private static final long serialVersionUID = 6535523029509031395L;
    
    MimeMessage _message = null;
    Map<String, Serializable> _attributes = new HashMap<String, Serializable>();

    /**
     * @see org.apache.mailet.Mail#getAttribute(java.lang.String)
     */
    public Serializable getAttribute(String arg0) {
        return _attributes.get(arg0);
    }

    /**
     * @see org.apache.mailet.Mail#getAttributeNames()
     */
    @SuppressWarnings("unchecked")
    public Iterator getAttributeNames() {
        return _attributes.keySet().iterator();
    }

    /**
     * @see org.apache.mailet.Mail#getErrorMessage()
     */
    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getLastUpdated()
     */
    public Date getLastUpdated() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getMessage()
     */
    public MimeMessage getMessage() throws MessagingException {
        return _message;
    }

    /**
     * @see org.apache.mailet.Mail#getMessageSize()
     */
    public long getMessageSize() throws MessagingException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see org.apache.mailet.Mail#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getRecipients()
     */
    @SuppressWarnings("unchecked")
    public Collection getRecipients() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getRemoteAddr()
     */
    public String getRemoteAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getRemoteHost()
     */
    public String getRemoteHost() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#getSender()
     */
    public MailAddress getSender() {
        MailAddress addr = null;
        try {
            addr = new MailAddress((InternetAddress)_message.getSender());
        } catch (AddressException ex) {
        } catch (MessagingException ex) {
        }
        return addr;
    }

    /**
     * @see org.apache.mailet.Mail#getState()
     */
    public String getState() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#hasAttributes()
     */
    public boolean hasAttributes() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see org.apache.mailet.Mail#removeAllAttributes()
     */
    public void removeAllAttributes() {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.apache.mailet.Mail#removeAttribute(java.lang.String)
     */
    public Serializable removeAttribute(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.mailet.Mail#setAttribute(java.lang.String, java.io.Serializable)
     */
    public Serializable setAttribute(String arg0, Serializable arg1) {
        return _attributes.put(arg0, arg1);
    }

    /**
     * @see org.apache.mailet.Mail#setErrorMessage(java.lang.String)
     */
    public void setErrorMessage(String arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.apache.mailet.Mail#setLastUpdated(java.util.Date)
     */
    public void setLastUpdated(Date arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.apache.mailet.Mail#setMessage(javax.mail.internet.MimeMessage)
     */
    public void setMessage(MimeMessage arg0) {
        _message = arg0;
    }

    /**
     * @see org.apache.mailet.Mail#setName(java.lang.String)
     */
    public void setName(String arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.apache.mailet.Mail#setRecipients(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public void setRecipients(Collection arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.apache.mailet.Mail#setState(java.lang.String)
     */
    public void setState(String arg0) {
        // TODO Auto-generated method stub

    }

}

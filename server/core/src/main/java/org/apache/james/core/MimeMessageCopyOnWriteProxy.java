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

package org.apache.james.core;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.james.lifecycle.api.Disposable;
import org.apache.james.lifecycle.api.LifecycleUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;

/**
 * This object wraps a "possibly shared" MimeMessage tracking copies and
 * automatically cloning it (if shared) when a write operation is invoked.
 */
public class MimeMessageCopyOnWriteProxy extends MimeMessage implements Disposable {

    /**
     * Used internally to track the reference count It is important that this is
     * static otherwise it will keep a reference to the parent object.
     */
    protected static class MessageReferenceTracker {

        /**
         * reference counter
         */
        private int referenceCount = 1;

        /**
         * The mime message in memory
         */
        private MimeMessage wrapped = null;

        public MessageReferenceTracker(MimeMessage ref) {
            wrapped = ref;
        }

        protected synchronized void incrementReferenceCount() {
            /*
             * Used to track references while debugging try { throw new
             * Exception("incrementReferenceCount: "+(wrapped != null ?
             * System.identityHashCode(wrapped)+"" :
             * "null")+" ["+referenceCount+"]"); } catch (Exception e) {
             * e.printStackTrace(); }
             */
            referenceCount++;
        }

        protected synchronized void decrementReferenceCount() {
            /*
             * Used to track references while debugging try { throw new
             * Exception("decrementReferenceCount: "+(wrapped != null ?
             * System.identityHashCode(wrapped)+"" :
             * "null")+" ["+referenceCount+"]"); } catch (Exception e) {
             * e.printStackTrace(); }
             */
            referenceCount--;
            if (referenceCount <= 0) {
                LifecycleUtil.dispose(wrapped);
                wrapped = null;
            }
        }

        protected synchronized int getReferenceCount() {
            return referenceCount;
        }

        public synchronized MimeMessage getWrapped() {
            return wrapped;
        }

    }

    protected MessageReferenceTracker refCount;

    /**
     * @param original
     *            MimeMessageWrapper
     * @throws MessagingException
     */
    public MimeMessageCopyOnWriteProxy(MimeMessage original) throws MessagingException {
        this(original, false);
    }

    /**
     * @param original
     *            MimeMessageSource
     * @throws MessagingException
     */
    public MimeMessageCopyOnWriteProxy(MimeMessageSource original) throws MessagingException {
        this(new MimeMessageWrapper(original), true);
    }

    /**
     * Private constructor providing an external reference counter.
     * 
     * @param original
     * @param writeable
     * @throws MessagingException
     */
    private MimeMessageCopyOnWriteProxy(MimeMessage original, boolean writeable) throws MessagingException {
        super(Session.getDefaultInstance(System.getProperties(), null));

        if (original instanceof MimeMessageCopyOnWriteProxy) {
            refCount = ((MimeMessageCopyOnWriteProxy) original).refCount;
        } else {
            refCount = new MessageReferenceTracker(original);
        }

        if (!writeable) {
            refCount.incrementReferenceCount();
        }
    }

    /**
     * Check the number of references over the MimeMessage and clone it if
     * needed before returning the reference
     * 
     * @throws MessagingException
     *             exception
     */
    protected synchronized MimeMessage getWrappedMessageForWriting() throws MessagingException {
        if (refCount.getReferenceCount() > 1) {
            refCount.decrementReferenceCount();
            refCount = new MessageReferenceTracker(new MimeMessageWrapper(refCount.getWrapped()));
        }
        return refCount.getWrapped();
    }

    /**
     * Return wrapped mimeMessage
     * 
     * @return wrapped return the wrapped mimeMessage
     */
    public synchronized MimeMessage getWrappedMessage() {
        return refCount.getWrapped();
    }

    /**
     * @see org.apache.james.lifecycle.api.Disposable#dispose()
     */
    public synchronized void dispose() {
        if (refCount != null) {
            refCount.decrementReferenceCount();
            refCount = null;
        }
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        getWrappedMessage().writeTo(os);
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        getWrappedMessage().writeTo(os, ignoreList);
    }

    /*
     * Various reader methods
     */

    /**
     * @see javax.mail.Message#getFrom()
     */
    public Address[] getFrom() throws MessagingException {
        return getWrappedMessage().getFrom();
    }

    /**
     * @see javax.mail.Message#getRecipients(javax.mail.Message.RecipientType)
     */
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        return getWrappedMessage().getRecipients(type);
    }

    /**
     * @see javax.mail.Message#getAllRecipients()
     */
    public Address[] getAllRecipients() throws MessagingException {
        return getWrappedMessage().getAllRecipients();
    }

    /**
     * @see javax.mail.Message#getReplyTo()
     */
    public Address[] getReplyTo() throws MessagingException {
        return getWrappedMessage().getReplyTo();
    }

    /**
     * @see javax.mail.Message#getSubject()
     */
    public String getSubject() throws MessagingException {
        return getWrappedMessage().getSubject();
    }

    /**
     * @see javax.mail.Message#getSentDate()
     */
    public Date getSentDate() throws MessagingException {
        return getWrappedMessage().getSentDate();
    }

    /**
     * @see javax.mail.Message#getReceivedDate()
     */
    public Date getReceivedDate() throws MessagingException {
        return getWrappedMessage().getReceivedDate();
    }

    /**
     * @see javax.mail.Part#getSize()
     */
    public int getSize() throws MessagingException {
        return getWrappedMessage().getSize();
    }

    /**
     * @see javax.mail.Part#getLineCount()
     */
    public int getLineCount() throws MessagingException {
        return getWrappedMessage().getLineCount();
    }

    /**
     * @see javax.mail.Part#getContentType()
     */
    public String getContentType() throws MessagingException {
        return getWrappedMessage().getContentType();
    }

    /**
     * @see javax.mail.Part#isMimeType(java.lang.String)
     */
    public boolean isMimeType(String mimeType) throws MessagingException {
        return getWrappedMessage().isMimeType(mimeType);
    }

    /**
     * @see javax.mail.Part#getDisposition()
     */
    public String getDisposition() throws MessagingException {
        return getWrappedMessage().getDisposition();
    }

    /**
     * @see javax.mail.internet.MimePart#getEncoding()
     */
    public String getEncoding() throws MessagingException {
        return getWrappedMessage().getEncoding();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentID()
     */
    public String getContentID() throws MessagingException {
        return getWrappedMessage().getContentID();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentMD5()
     */
    public String getContentMD5() throws MessagingException {
        return getWrappedMessage().getContentMD5();
    }

    /**
     * @see javax.mail.Part#getDescription()
     */
    public String getDescription() throws MessagingException {
        return getWrappedMessage().getDescription();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentLanguage()
     */
    public String[] getContentLanguage() throws MessagingException {
        return getWrappedMessage().getContentLanguage();
    }

    /**
     * @see javax.mail.internet.MimeMessage#getMessageID()
     */
    public String getMessageID() throws MessagingException {
        return getWrappedMessage().getMessageID();
    }

    /**
     * @see javax.mail.Part#getFileName()
     */
    public String getFileName() throws MessagingException {
        return getWrappedMessage().getFileName();
    }

    /**
     * @see javax.mail.Part#getInputStream()
     */
    public InputStream getInputStream() throws IOException, MessagingException {
        return getWrappedMessage().getInputStream();
    }

    /**
     * @see javax.mail.Part#getDataHandler()
     */
    public DataHandler getDataHandler() throws MessagingException {
        return getWrappedMessage().getDataHandler();
    }

    /**
     * @see javax.mail.Part#getContent()
     */
    public Object getContent() throws IOException, MessagingException {
        return getWrappedMessage().getContent();
    }

    /**
     * @see javax.mail.Part#getHeader(java.lang.String)
     */
    public String[] getHeader(String name) throws MessagingException {
        return getWrappedMessage().getHeader(name);
    }

    /**
     * @see javax.mail.internet.MimePart#getHeader(java.lang.String,
     *      java.lang.String)
     */
    public String getHeader(String name, String delimiter) throws MessagingException {
        return getWrappedMessage().getHeader(name, delimiter);
    }

    /**
     * @see javax.mail.Part#getAllHeaders()
     */
    public Enumeration getAllHeaders() throws MessagingException {
        return getWrappedMessage().getAllHeaders();
    }

    /**
     * @see javax.mail.Part#getMatchingHeaders(java.lang.String[])
     */
    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
        return getWrappedMessage().getMatchingHeaders(names);
    }

    /**
     * @see javax.mail.Part#getNonMatchingHeaders(java.lang.String[])
     */
    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
        return getWrappedMessage().getNonMatchingHeaders(names);
    }

    /**
     * @see javax.mail.internet.MimePart#getAllHeaderLines()
     */
    public Enumeration getAllHeaderLines() throws MessagingException {
        return getWrappedMessage().getAllHeaderLines();
    }

    /**
     * @see javax.mail.internet.MimePart#getMatchingHeaderLines(java.lang.String[])
     */
    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
        return getWrappedMessage().getMatchingHeaderLines(names);
    }

    /**
     * @see javax.mail.internet.MimePart#getNonMatchingHeaderLines(java.lang.String[])
     */
    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
        return getWrappedMessage().getNonMatchingHeaderLines(names);
    }

    /**
     * @see javax.mail.Message#getFlags()
     */
    public Flags getFlags() throws MessagingException {
        return getWrappedMessage().getFlags();
    }

    /**
     * @see javax.mail.Message#isSet(javax.mail.Flags.Flag)
     */
    public boolean isSet(Flags.Flag flag) throws MessagingException {
        return getWrappedMessage().isSet(flag);
    }

    /**
     * @see javax.mail.internet.MimeMessage#getSender()
     */
    public Address getSender() throws MessagingException {
        return getWrappedMessage().getSender();
    }

    /**
     * @see javax.mail.Message#match(javax.mail.search.SearchTerm)
     */
    public boolean match(SearchTerm arg0) throws MessagingException {
        return getWrappedMessage().match(arg0);
    }

    /**
     * @see javax.mail.internet.MimeMessage#getRawInputStream()
     */
    public InputStream getRawInputStream() throws MessagingException {
        return getWrappedMessage().getRawInputStream();
    }

    /**
     * @see javax.mail.Message#getFolder()
     */
    public Folder getFolder() {
        return getWrappedMessage().getFolder();
    }

    /**
     * @see javax.mail.Message#getMessageNumber()
     */
    public int getMessageNumber() {
        return getWrappedMessage().getMessageNumber();
    }

    /**
     * @see javax.mail.Message#isExpunged()
     */
    public boolean isExpunged() {
        return getWrappedMessage().isExpunged();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0) {
        return getWrappedMessage().equals(arg0);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getWrappedMessage().hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getWrappedMessage().toString();
    }

    /*
     * Various writer methods
     */

    /**
     * @see javax.mail.Message#setFrom(javax.mail.Address)
     */
    public void setFrom(Address address) throws MessagingException {
        getWrappedMessageForWriting().setFrom(address);
    }

    /**
     * @see javax.mail.Message#setFrom()
     */
    public void setFrom() throws MessagingException {
        getWrappedMessageForWriting().setFrom();
    }

    /**
     * @see javax.mail.Message#addFrom(javax.mail.Address[])
     */
    public void addFrom(Address[] addresses) throws MessagingException {
        getWrappedMessageForWriting().addFrom(addresses);
    }

    /**
     * @see javax.mail.Message#setRecipients(javax.mail.Message.RecipientType,
     *      javax.mail.Address[])
     */
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        getWrappedMessageForWriting().setRecipients(type, addresses);
    }

    /**
     * @see javax.mail.Message#addRecipients(javax.mail.Message.RecipientType,
     *      javax.mail.Address[])
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        getWrappedMessageForWriting().addRecipients(type, addresses);
    }

    /**
     * @see javax.mail.Message#setReplyTo(javax.mail.Address[])
     */
    public void setReplyTo(Address[] addresses) throws MessagingException {
        getWrappedMessageForWriting().setReplyTo(addresses);
    }

    /**
     * @see javax.mail.Message#setSubject(java.lang.String)
     */
    public void setSubject(String subject) throws MessagingException {
        getWrappedMessageForWriting().setSubject(subject);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setSubject(java.lang.String,
     *      java.lang.String)
     */
    public void setSubject(String subject, String charset) throws MessagingException {
        getWrappedMessageForWriting().setSubject(subject, charset);
    }

    /**
     * @see javax.mail.Message#setSentDate(java.util.Date)
     */
    public void setSentDate(Date d) throws MessagingException {
        getWrappedMessageForWriting().setSentDate(d);
    }

    /**
     * @see javax.mail.Part#setDisposition(java.lang.String)
     */
    public void setDisposition(String disposition) throws MessagingException {
        getWrappedMessageForWriting().setDisposition(disposition);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setContentID(java.lang.String)
     */
    public void setContentID(String cid) throws MessagingException {
        getWrappedMessageForWriting().setContentID(cid);
    }

    /**
     * @see javax.mail.internet.MimePart#setContentMD5(java.lang.String)
     */
    public void setContentMD5(String md5) throws MessagingException {
        getWrappedMessageForWriting().setContentMD5(md5);
    }

    /**
     * @see javax.mail.Part#setDescription(java.lang.String)
     */
    public void setDescription(String description) throws MessagingException {
        getWrappedMessageForWriting().setDescription(description);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setDescription(java.lang.String,
     *      java.lang.String)
     */
    public void setDescription(String description, String charset) throws MessagingException {
        getWrappedMessageForWriting().setDescription(description, charset);
    }

    /**
     * @see javax.mail.internet.MimePart#setContentLanguage(java.lang.String[])
     */
    public void setContentLanguage(String[] languages) throws MessagingException {
        getWrappedMessageForWriting().setContentLanguage(languages);
    }

    /**
     * @see javax.mail.Part#setFileName(java.lang.String)
     */
    public void setFileName(String filename) throws MessagingException {
        getWrappedMessageForWriting().setFileName(filename);
    }

    /**
     * @see javax.mail.Part#setDataHandler(javax.activation.DataHandler)
     */
    public void setDataHandler(DataHandler dh) throws MessagingException {
        getWrappedMessageForWriting().setDataHandler(dh);
    }

    /**
     * @see javax.mail.Part#setContent(java.lang.Object, java.lang.String)
     */
    public void setContent(Object o, String type) throws MessagingException {
        getWrappedMessageForWriting().setContent(o, type);
    }

    /**
     * @see javax.mail.Part#setText(java.lang.String)
     */
    public void setText(String text) throws MessagingException {
        getWrappedMessageForWriting().setText(text);
    }

    /**
     * @see javax.mail.internet.MimePart#setText(java.lang.String,
     *      java.lang.String)
     */
    public void setText(String text, String charset) throws MessagingException {
        getWrappedMessageForWriting().setText(text, charset);
    }

    /**
     * @see javax.mail.Part#setContent(javax.mail.Multipart)
     */
    public void setContent(Multipart mp) throws MessagingException {
        getWrappedMessageForWriting().setContent(mp);
    }

    /**
     * This does not need a writable message
     * 
     * @see javax.mail.Message#reply(boolean)
     */
    public Message reply(boolean replyToAll) throws MessagingException {
        return getWrappedMessage().reply(replyToAll);
    }

    /**
     * @see javax.mail.Part#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value) throws MessagingException {
        getWrappedMessageForWriting().setHeader(name, value);
    }

    /**
     * @see javax.mail.Part#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value) throws MessagingException {
        getWrappedMessageForWriting().addHeader(name, value);
    }

    /**
     * @see javax.mail.Part#removeHeader(java.lang.String)
     */
    public void removeHeader(String name) throws MessagingException {
        getWrappedMessageForWriting().removeHeader(name);
    }

    /**
     * @see javax.mail.internet.MimePart#addHeaderLine(java.lang.String)
     */
    public void addHeaderLine(String line) throws MessagingException {
        getWrappedMessageForWriting().addHeaderLine(line);
    }

    /**
     * @see javax.mail.Message#setFlags(javax.mail.Flags, boolean)
     */
    public void setFlags(Flags flag, boolean set) throws MessagingException {
        getWrappedMessageForWriting().setFlags(flag, set);
    }

    /**
     * @see javax.mail.Message#saveChanges()
     */
    public void saveChanges() throws MessagingException {
        getWrappedMessageForWriting().saveChanges();
    }

    /*
     * Since JavaMail 1.2
     */

    /**
     * @see javax.mail.internet.MimeMessage#addRecipients(javax.mail.Message.RecipientType,
     *      java.lang.String)
     */
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        getWrappedMessageForWriting().addRecipients(type, addresses);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setRecipients(javax.mail.Message.RecipientType,
     *      java.lang.String)
     */
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        getWrappedMessageForWriting().setRecipients(type, addresses);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setSender(javax.mail.Address)
     */
    public void setSender(Address arg0) throws MessagingException {
        getWrappedMessageForWriting().setSender(arg0);
    }

    /**
     * @see javax.mail.Message#addRecipient(javax.mail.Message.RecipientType,
     *      javax.mail.Address)
     */
    public void addRecipient(RecipientType arg0, Address arg1) throws MessagingException {
        getWrappedMessageForWriting().addRecipient(arg0, arg1);
    }

    /**
     * @see javax.mail.Message#setFlag(javax.mail.Flags.Flag, boolean)
     */
    public void setFlag(Flag arg0, boolean arg1) throws MessagingException {
        getWrappedMessageForWriting().setFlag(arg0, arg1);
    }

    /**
     * @return the message size
     * @throws MessagingException
     */
    public long getMessageSize() throws MessagingException {
        return MimeMessageUtil.getMessageSize(getWrappedMessage());
    }

    /**
     * Since javamail 1.4
     */
    @Override
    public void setText(String text, String charset, String subtype) throws MessagingException {
        getWrappedMessage().setText(text, charset, subtype);
    }

}

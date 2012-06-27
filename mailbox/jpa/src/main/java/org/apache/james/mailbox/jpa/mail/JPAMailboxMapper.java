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

package org.apache.james.mailbox.jpa.mail;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.apache.james.mailbox.acl.MailboxACLCodec;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxExistsException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.jpa.JPATransactionalMapper;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Data access management for mailbox.
 */
public class JPAMailboxMapper extends JPATransactionalMapper implements MailboxMapper<Long> {

    private String lastMailboxName;
    protected final MailboxACLCodec mailboxAclCodec;


    public JPAMailboxMapper(EntityManagerFactory entityManagerFactory, MailboxNameCodec mailboxNameCodec, MailboxACLCodec mailboxAclCodec) {
        super(entityManagerFactory, mailboxNameCodec);
        this.mailboxAclCodec = mailboxAclCodec;
    }

    /**
     * Commit the transaction. If the commit fails due a conflict in a unique key constraint a {@link MailboxExistsException}
     * will get thrown
     */
    @Override
    protected void commit() throws MailboxException {
        try {
            getEntityManager().getTransaction().commit();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException)
                throw new MailboxExistsException(lastMailboxName);
            if (e instanceof RollbackException) {
                Throwable t = e.getCause();
                if (t != null && t instanceof EntityExistsException)
                    throw new MailboxExistsException(lastMailboxName);
            }
            throw new MailboxException("Commit of transaction failed", e);
        }
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#save(Mailbox)
     */
    @Override
    public void save(Mailbox<Long> mailbox) throws MailboxException {
        try {
            this.lastMailboxName = mailbox.getMailboxName().toString();
            getEntityManager().persist(mailbox);
        } catch (PersistenceException e) {
            throw new MailboxException("Save of mailbox " + lastMailboxName +" failed", e);
        } 
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByPath(MailboxPath)
     */
    @Override
    public Mailbox<Long> findMailboxByPath(MailboxName mailboxPath) throws MailboxException, MailboxNotFoundException {
        try {
            return (JPAMailbox) getEntityManager()
                    .createNamedQuery("findMailboxByName")
                    .setParameter("nameParam", mailboxNameCodec.encode(mailboxPath))
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(mailboxPath);
        } catch (PersistenceException e) {
            throw new MailboxException("Search of mailbox " + mailboxPath + " failed", e);
        } 
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#delete(Mailbox)
     */
    @Override
    public void delete(Mailbox<Long> mailbox) throws MailboxException {
        try {
            EntityManager em = getEntityManager();
            em.createNamedQuery("deleteMessages").setParameter("idParam", mailbox.getMailboxId()).executeUpdate();
            em.remove(mailbox);
        } catch (PersistenceException e) {
            throw new MailboxException("Delete of mailbox " + mailbox + " failed", e);
        } 
    }

    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithPathLike(MailboxPath)
     */
    @Override
    public List<Mailbox<Long>> findMailboxWithPathLike(MailboxName path) throws MailboxException {
        try {
            @SuppressWarnings("unchecked")
            List<Mailbox<Long>> mailboxes = getEntityManager()
                    .createNamedQuery("findMailboxWithNameLike")
                    .setParameter("nameParam", mailboxNamePatternCodec.encode(path))
                    .getResultList();
            MailboxQuery mailboxQuery = new MailboxQuery(path);
            List<Mailbox<Long>> result = new ArrayList<Mailbox<Long>>(mailboxes.size());
            for (Mailbox<Long> mailbox : mailboxes) {
                if (mailboxQuery.isExpressionMatch(mailbox.getMailboxName())) {
                    result.add(mailbox);
                }
            }
            return result;
        } catch (PersistenceException e) {
            throw new MailboxException("Search of mailbox " + path + " failed", e);
        }
    }

    public void deleteAllMemberships() throws MailboxException {
        try {
            getEntityManager().createNamedQuery("deleteAllMemberships").executeUpdate();
        } catch (PersistenceException e) {
            throw new MailboxException("Delete of mailboxes failed", e);
        } 
    }
    
    public void deleteAllMailboxes() throws MailboxException {
        try {
            getEntityManager().createNamedQuery("deleteAllMailboxes").executeUpdate();
        } catch (PersistenceException e) {
            throw new MailboxException("Delete of mailboxes failed", e);
        } 
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(Mailbox, char)
     */
    @Override
    public boolean hasChildren(Mailbox<Long> mailbox) throws MailboxException, MailboxNotFoundException {
        try {
            MailboxName queryName = mailbox.getMailboxName().child(MailboxQuery.FREEWILDCARD_STRING);
            @SuppressWarnings("unchecked")
            List<Mailbox<Long>> mailboxes = getEntityManager()
                    .createNamedQuery("findMailboxWithNameLike")
                    .setParameter("nameParam", mailboxNamePatternCodec.encode(queryName))
                    .getResultList();
            MailboxQuery mailboxQuery = new MailboxQuery(queryName);
            for (Mailbox<Long> mbox : mailboxes) {
                if (mailboxQuery.isExpressionMatch(mbox.getMailboxName())) {
                    return true;
                }
            }
            return false;
        } catch (PersistenceException e) {
            throw new MailboxException("Search of mailbox " + mailbox + " failed", e);
        }
    }

	/**
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Mailbox<Long>> list() throws MailboxException{
        try {
            return getEntityManager().createNamedQuery("listMailboxes").getResultList();
        } catch (PersistenceException e) {
            throw new MailboxException("Delete of mailboxes failed", e);
        } 
    }
    
    public MailboxACLCodec getMailboxAclCodec() {
        return mailboxAclCodec;
    }

}

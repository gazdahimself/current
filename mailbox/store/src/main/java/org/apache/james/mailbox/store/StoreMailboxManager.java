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

package org.apache.james.mailbox.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Random;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxPathLocker.LockAwareExecution;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSession.SessionType;
import org.apache.james.mailbox.MailboxSessionIdGenerator;
import org.apache.james.mailbox.RequestAware;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxExistsException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxMetaData.Selectability;
import org.apache.james.mailbox.model.MailboxMetaDataComparator;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;
import org.slf4j.Logger;

/**
 * This base class of an {@link MailboxManager} implementation provides a
 * high-level api for writing your own {@link MailboxManager} implementation. If
 * you plan to write your own {@link MailboxManager} its most times so easiest
 * to extend just this class or use it directly.
 * 
 * If you need a more low-level api just implement {@link MailboxManager}
 * directly
 * 
 * @param <Id>
 */
public class StoreMailboxManager<Id> implements MailboxManager {

    public static final int DEFAULT_FETCH_BATCH_SIZE = 200;
    public static int estimateMailboxCountPerUser() {
        return 8;
    }


    private MailboxEventDispatcher<Id> dispatcher;
    private AbstractDelegatingMailboxListener delegatingListener = null;
    private final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;

    private final Authenticator authenticator;

    private final MailboxACLResolver aclResolver;

    private final GroupMembershipResolver groupMembershipResolver;

    private final static Random RANDOM = new Random();

    private int copyBatchSize = 0;

    private MailboxPathLocker locker;

    private MessageSearchIndex<Id> index;

    private MailboxSessionIdGenerator idGenerator;

    private int fetchBatchSize = DEFAULT_FETCH_BATCH_SIZE;
    
    private final MailboxNameResolver mailboxNameResolver;

    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator, final MailboxPathLocker locker, final MailboxACLResolver aclResolver, final GroupMembershipResolver groupMembershipResolver) {
        this.authenticator = authenticator;
        this.locker = locker;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        this.aclResolver = aclResolver;
        this.groupMembershipResolver = groupMembershipResolver;
        this.mailboxNameResolver = DefaultMailboxNameResolver.INSTANCE;
    }

    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator, final MailboxACLResolver aclResolver, final GroupMembershipResolver groupMembershipResolver) {
        this(mailboxSessionMapperFactory, authenticator, new JVMMailboxPathLocker(), aclResolver, groupMembershipResolver);
    }

    public void setMailboxSessionIdGenerator(MailboxSessionIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public void setCopyBatchSize(int copyBatchSize) {
        this.copyBatchSize = copyBatchSize;
    }

    public void setFetchBatchSize(int fetchBatchSize) {
        this.fetchBatchSize = fetchBatchSize;
    }

    /**
     * Init the {@link MailboxManager}
     * 
     * @throws MailboxException
     */
    @SuppressWarnings("rawtypes")
    public void init() throws MailboxException {
        // The dispatcher need to have the delegating listener added
        dispatcher = new MailboxEventDispatcher<Id>(getDelegationListener());

        if (index == null) {
            index = new SimpleMessageSearchIndex<Id>(mailboxSessionMapperFactory);
        }
        if (index instanceof ListeningMessageSearchIndex) {
            addGlobalListener((ListeningMessageSearchIndex) index, null);
        }

        if (idGenerator == null) {
            idGenerator = new RandomMailboxSessionIdGenerator();
        }
    }

    /**
     * Return the {@link AbstractDelegatingMailboxListener} which is used by
     * this {@link MailboxManager}
     * 
     * @return delegatingListener
     */
    public AbstractDelegatingMailboxListener getDelegationListener() {
        if (delegatingListener == null) {
            delegatingListener = new HashMapDelegatingMailboxListener();
        }
        return delegatingListener;
    }

    /**
     * Return the {@link MessageSearchIndex} used by this {@link MailboxManager}
     * 
     * @return index
     */
    public MessageSearchIndex<Id> getMessageSearchIndex() {
        return index;
    }

    /**
     * Return the {@link MailboxEventDispatcher} used by thei
     * {@link MailboxManager}
     * 
     * @return dispatcher
     */
    public MailboxEventDispatcher<Id> getEventDispatcher() {
        return dispatcher;
    }

    /**
     * Return the {@link MailboxSessionMapperFactory} used by this
     * {@link MailboxManager}
     * 
     * @return mailboxSessionMapperFactory
     */
    public MailboxSessionMapperFactory<Id> getMapperFactory() {
        return mailboxSessionMapperFactory;
    }

    public MailboxPathLocker getLocker() {
        return locker;
    }

    public MailboxACLResolver getAclResolver() {
        return aclResolver;
    }

    public GroupMembershipResolver getGroupMembershipResolver() {
        return groupMembershipResolver;
    }

    /**
     * Set the {@link AbstractDelegatingMailboxListener} to use with this
     * {@link MailboxManager} instance. If none is set here a
     * {@link HashMapDelegatingMailboxListener} instance will be created lazy
     * 
     * @param delegatingListener
     */
    public void setDelegatingMailboxListener(AbstractDelegatingMailboxListener delegatingListener) {
        this.delegatingListener = delegatingListener;
        dispatcher = new MailboxEventDispatcher<Id>(getDelegationListener());
    }

    /**
     * Set the {@link MessageSearchIndex} which should be used by this
     * {@link MailboxManager}. If none is given this implementation will use a
     * {@link SimpleMessageSearchIndex} by default
     * 
     * @param index
     */
    public void setMessageSearchIndex(MessageSearchIndex<Id> index) {
        this.index = index;
    }

    /**
     * Generate an return the next uid validity
     * 
     * @return uidValidity
     */
    protected int randomUidValidity() {
        return Math.abs(RANDOM.nextInt());
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#createSystemSession(java.lang.String,
     *      org.slf4j.Logger)
     */
    @Override
    public MailboxSession createSystemSession(String userName, Logger log) {
        return createSession(userName, null, log, SessionType.System);
    }

    /**
     * Create Session
     * 
     * @param userName
     * @param log
     * @return session
     */
    protected MailboxSession createSession(String userName, String password, Logger log, SessionType type) {
        return new SimpleMailboxSession(randomId(), userName, password, log, new ArrayList<Locale>(), type, mailboxNameResolver);
    }

    /**
     * Generate and return the next id to use
     * 
     * @return id
     */
    protected long randomId() {
        return idGenerator.nextId();
    }

    /**
     * Log in the user with the given userid and password
     * 
     * @param userid
     *            the username
     * @param passwd
     *            the password
     * @return success true if login success false otherwise
     */
    private boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#login(java.lang.String,
     *      java.lang.String, org.slf4j.Logger)
     */
    @Override
    public MailboxSession login(String userid, String passwd, Logger log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, passwd, log, SessionType.User);
        } else {
            throw new BadCredentialsException();
        }
    }

    /**
     * Close the {@link MailboxSession} if not null
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        if (session != null) {
            session.close();
        }
    }

    /**
     * Create a {@link MailboxManager} for the given Mailbox. By default this
     * will return a {@link StoreMessageManager}. If your implementation needs
     * something different, just override this method
     * 
     * @param mailbox
     * @param session
     * @return storeMailbox
     */
    protected StoreMessageManager<Id> createMessageManager(Mailbox<Id> mailbox, MailboxSession session) throws MailboxException {
        return new StoreMessageManager<Id>(getMapperFactory(), getMessageSearchIndex(), getEventDispatcher(), getLocker(), mailbox, getAclResolver(), getGroupMembershipResolver());
    }

    /**
     * Create a Mailbox for the given mailbox path. This will by default return
     * a {@link SimpleMailbox}.
     * 
     * If you need to return something more special just override this method
     * 
     * @param mailboxPath
     * @param session
     * @throws MailboxException
     */
    protected org.apache.james.mailbox.store.mail.model.Mailbox<Id> doCreateMailbox(MailboxName mailboxPath, MailboxOwner owner, MailboxSession session) throws MailboxException {
        return new SimpleMailbox<Id>(mailboxPath, owner.getName(), owner.isGroup(), randomUidValidity());
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#getMailbox(MailboxPath,
     *      MailboxSession)
     */
    @Override
    public org.apache.james.mailbox.MessageManager getMailbox(MailboxName mailboxPath, MailboxSession session) throws MailboxException {
        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        Mailbox<Id> mailboxRow = mapper.findMailboxByPath(mailboxPath);

        if (mailboxRow == null) {
            session.getLog().info("Mailbox '" + mailboxPath + "' not found.");
            throw new MailboxNotFoundException(mailboxPath);

        } else {
            session.getLog().debug("Loaded mailbox " + mailboxPath);

            StoreMessageManager<Id> m = createMessageManager(mailboxRow, session);
            m.setFetchBatchSize(fetchBatchSize);
            return m;
        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#createMailbox(MailboxPath,
     *      MailboxSession)
     */
    @Override
    public void createMailbox(MailboxName mailboxPath, final MailboxSession mailboxSession) throws MailboxException {
        mailboxSession.getLog().debug("createMailbox " + mailboxPath);

        if (mailboxPath.isEmpty()) {
            mailboxSession.getLog().warn("Ignoring mailbox with empty name");
        } else {
            if (mailboxExists(mailboxPath, mailboxSession)) {
                throw new MailboxExistsException(mailboxPath.toString());
            }
            // Create parents first
            // If any creation fails then the mailbox will not be created
            // TODO: transaction
            Iterator<MailboxName> levelsIt = mailboxPath.topDownHierarchyLevels();
            while (levelsIt.hasNext()) {
                final MailboxName ancestor = levelsIt.next();
                locker.executeWithLock(mailboxSession, ancestor, new LockAwareExecution<Void>() {

                    public Void execute() throws MailboxException {
                        if (!mailboxExists(ancestor, mailboxSession)) {
                            MailboxOwner owner = mailboxSession.getMailboxNameResolver().getOwner(ancestor);
                            if (owner == null) {
                                throw new MailboxException("Could not create '"+ ancestor +"' because it does not have an owner.");
                            }
                            final org.apache.james.mailbox.store.mail.model.Mailbox<Id> m = doCreateMailbox(ancestor, owner, mailboxSession);
                            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
                            mapper.execute(new TransactionalMapper.VoidTransaction() {

                                public void runVoid() throws MailboxException {
                                    mapper.save(m);
                                }

                            });

                            // notify listeners
                            dispatcher.mailboxAdded(mailboxSession, m);
                        }
                        return null;

                    }
                }, true);                    
            }

        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#deleteMailbox(MailboxPath,
     *      MailboxSession)
     */
    @Override
    public void deleteMailbox(final MailboxName mailboxPath, final MailboxSession session) throws MailboxException {
        session.getLog().info("deleteMailbox " + mailboxPath);
        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);

        Mailbox<Id> mailbox = mapper.execute(new Mapper.Transaction<Mailbox<Id>>() {

            public Mailbox<Id> run() throws MailboxException {
                final Mailbox<Id> mailbox = mapper.findMailboxByPath(mailboxPath);
                if (mailbox == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }

                // We need to create a copy of the mailbox as maybe we can not
                // refer to the real
                // mailbox once we remove it
                SimpleMailbox<Id> m = new SimpleMailbox<Id>(mailbox);
                mapper.delete(mailbox);
                return m;
            }

        });

        dispatcher.mailboxDeleted(session, mailbox);

    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#renameMailbox(MailboxPath,
     *      MailboxPath, MailboxSession)
     */
    @Override
    public void renameMailbox(final MailboxName from, final MailboxName to, final MailboxSession session) throws MailboxException {
        final Logger log = session.getLog();
        if (log.isDebugEnabled())
            log.debug("renameMailbox " + from + " to " + to);
        if (mailboxExists(to, session)) {
            throw new MailboxExistsException(to.toString());
        }

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        mapper.execute(new Mapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                // TODO put this into a serilizable transaction
                final Mailbox<Id> mailbox = mapper.findMailboxByPath(from);
                if (mailbox == null) {
                    throw new MailboxNotFoundException(from);
                }
                mailbox.setMailboxName(to);
                mapper.save(mailbox);

                dispatcher.mailboxRenamed(session, from, mailbox);

                // rename submailboxes
                final MailboxName childrenPattern = from.child(MailboxQuery.LOCALWILDCARD_STRING);
                locker.executeWithLock(session, from, new LockAwareExecution<Void>() {

                    public Void execute() throws MailboxException {
                        final List<Mailbox<Id>> subMailboxes = mapper.findMailboxWithPathLike(childrenPattern);
                        int fromLength = from.getSegmentCount();
                        int toLength = to.getSegmentCount();

                        for (Mailbox<Id> sub : subMailboxes) {
                            
                            
                            MailboxName oldSubName = sub.getMailboxName();
                            int oldSubLength = oldSubName.getSegmentCount();
                            MailboxNameBuilder mnb = new MailboxNameBuilder(toLength + (oldSubLength  - fromLength));
                            for (ListIterator<String> it = to.segmentsIterator(); it.hasNext();) {
                                mnb.add(it.next());
                            }
                            for (int i = fromLength; i < oldSubLength; i++) {
                                mnb.add(oldSubName.getSegmentAt(i));
                            }
                            MailboxName newSubName = mnb.qualified(true);
                            sub.setMailboxName(newSubName);
                            
                            mapper.save(sub);
                            dispatcher.mailboxRenamed(session, from, sub);

                            if (log.isDebugEnabled()) {
                                log.debug("Rename mailbox sub-mailbox " + oldSubName + " to " + newSubName);
                            }
                        }
                        return null;

                    }
                }, true);

            }

        });

    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#copyMessages(MessageRange,
     *      MailboxPath, MailboxPath, MailboxSession)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<MessageRange> copyMessages(MessageRange set, MailboxName from, MailboxName to, MailboxSession session) throws MailboxException {
        StoreMessageManager<Id> toMailbox = (StoreMessageManager<Id>) getMailbox(to, session);
        StoreMessageManager<Id> fromMailbox = (StoreMessageManager<Id>) getMailbox(from, session);

        if (copyBatchSize > 0) {
            List<MessageRange> copiedRanges = new ArrayList<MessageRange>();
            Iterator<MessageRange> ranges = set.split(copyBatchSize).iterator();
            while (ranges.hasNext()) {
                copiedRanges.addAll(fromMailbox.copyTo(ranges.next(), toMailbox, session));
            }
            return copiedRanges;
        } else {
            return fromMailbox.copyTo(set, toMailbox, session);
        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#search(org.apache.james.mailbox.model.MailboxQuery,
     *      org.apache.james.mailbox.MailboxSession)
     */
    @Override
    public List<MailboxMetaData> search(final MailboxQuery mailboxQuery, MailboxSession session) throws MailboxException {

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        final List<Mailbox<Id>> mailboxes = mapper.findMailboxWithPathLike(mailboxQuery.getResolvedExpression());
        final List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxes.size());
        for (Mailbox<Id> mailbox : mailboxes) {
                MailboxName mailboxName = mailbox.getMailboxName();
                if (mailboxQuery.isExpressionMatch(mailboxName )) {
                    final MailboxMetaData.Children inferiors;
                    if (mapper.hasChildren(mailbox)) {
                        inferiors = MailboxMetaData.Children.HAS_CHILDREN;
                    } else {
                        inferiors = MailboxMetaData.Children.HAS_NO_CHILDREN;
                    }
                    results.add(new SimpleMailboxMetaData(mailboxName, inferiors, Selectability.NONE));
                }
        }
        Collections.sort(results, new MailboxMetaDataComparator(session.getMailboxNameResolver().getContextualizedComparator(session.getUser().getUserName())));
        return results;
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#mailboxExists(MailboxPath,
     *      MailboxSession)
     */
    @Override
    public boolean mailboxExists(MailboxName mailboxPath, MailboxSession session) throws MailboxException {
        if (session.getMailboxNameResolver().existsTrivially(mailboxPath)) {
            return true;
        }
        else {
            try {
                final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
                mapper.findMailboxByPath(mailboxPath);
                return true;
            } catch (MailboxNotFoundException e) {
                return false;
            }
        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#addListener(MailboxPath,
     *      MailboxListener, MailboxSession)
     */
    @Override
    public void addListener(MailboxName path, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addListener(path, listener, session);
    }

    /**
     * End processing of Request for session
     */
    public void endProcessingRequest(MailboxSession session) {
        if (mailboxSessionMapperFactory instanceof RequestAware) {
            ((RequestAware) mailboxSessionMapperFactory).endProcessingRequest(session);
        }
    }

    /**
     * Do nothing. Sub classes should override this if needed
     */
    @Override
    public void startProcessingRequest(MailboxSession session) {
        // do nothing

    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#list(org.apache.james.mailbox.MailboxSession)
     */
    @Override
    public List<MailboxName> list(MailboxSession session) throws MailboxException {
        List<Mailbox<Id>> mailboxes = mailboxSessionMapperFactory.getMailboxMapper(session).list();
        List<MailboxName> mList = new ArrayList<MailboxName>(mailboxes.size());
        for (int i = 0; i < mailboxes.size(); i++) {
            Mailbox<Id> m = mailboxes.get(i);
            mList.add(m.getMailboxName());
        }
        return Collections.unmodifiableList(mList);
    }

    /**
     * @see org.apache.james.mailbox.MailboxListenerSupport#addGlobalListener(org.apache.james.mailbox.MailboxListener,
     *      org.apache.james.mailbox.MailboxSession)
     */
    @Override
    public void addGlobalListener(MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addGlobalListener(listener, session);
    }

    /**
     * @see org.apache.james.mailbox.MailboxListenerSupport#removeListener(org.apache.james.mailbox.model.MailboxPath,
     *      org.apache.james.mailbox.MailboxListener,
     *      org.apache.james.mailbox.MailboxSession)
     */
    @Override
    public void removeListener(MailboxName mailboxPath, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.removeListener(mailboxPath, listener, session);
    }

    @Override
    public void removeGlobalListener(MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.removeGlobalListener(listener, session);

    }

    /**
     * @see org.apache.james.mailbox.MailboxManager#getMailboxNameResolver()
     */
    @Override
    public MailboxNameResolver getMailboxNameResolver() {
        return mailboxNameResolver;
    }
    
    
}

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

package org.apache.james.mailbox.name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxQuery;

/**
 * TODO: the rules.
 */
public class DefaultMailboxNameResolver implements MailboxNameResolver {
    
    public static class ContextualizedComparator implements Comparator<MailboxName> {

        private final MailboxName home;

        public ContextualizedComparator(MailboxName home) {
            super();
            this.home = home;
        }

        @Override
        public int compare(MailboxName n1, MailboxName n2) {
            /* empty first */
            if (n1.isEmpty() && n2.isEmpty()) {
                return 0;
            } else if (n1.isEmpty()) {
                return -1;
            } else if (n2.isEmpty()) {
                return 1;
            } else {
                /* none empty */
                /* users' mailboxes before other (e.g. shared) mailboxes */
                String first1 = n1.getSegmentAt(0);
                String first2 = n2.getSegmentAt(0);
                boolean isUsers1 = BACKEND_USERS_ROOT.equals(first1);
                boolean isUsers2 = BACKEND_USERS_ROOT.equals(first2);

                if (isUsers1 && !isUsers2) {
                    return -1;
                } else if (!isUsers1 && isUsers2) {
                    return 1;
                } else if (isUsers1 && isUsers2) {
                    /* current user before other users */
                    boolean isUnderCurrentUsersHome1 = home.isEqualOrAncestorOf(n1);
                    boolean isUnderCurrentUsersHome2 = home.isEqualOrAncestorOf(n2);

                    if (isUnderCurrentUsersHome1 && !isUnderCurrentUsersHome2) {
                        return -1;
                    } else if (isUnderCurrentUsersHome1 && !isUnderCurrentUsersHome2) {
                        return 1;
                    } else {
                        /*
                         * isUnderCurrentUsersHome1 == isUnderCurrentUsersHome2
                         * two current user's folders or two other user's
                         * folders sort them alphabetically
                         */
                        return n1.compareTo(n2);
                    }
                } else {
                    /*
                     * !isUsers1 && !isUsers2 other (e.g. shared) mailboxes sort
                     * them alphabetically
                     */
                    return n1.compareTo(n2);
                }

            }
        }
    }

    public static class DefaultMailboxOwner implements MailboxOwner {
        protected final boolean group;
        private int hashCode = 0;
        protected final String name;
        private MailboxName inbox;

        private DefaultMailboxOwner() {
            this.name = null;
            this.group = false;
        }

        public DefaultMailboxOwner(String owner, boolean isOwnerGroup) {
            super();
            this.name = owner;
            this.group = isOwnerGroup;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MailboxOwner) {
                MailboxOwner other = (MailboxOwner) o;
                return this.isVirtual() == other.isVirtual() && this.group == other.isGroup() && this.name.equals(other.getName());
            } else {
                return false;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            int result = this.hashCode;
            if (result == 0) {
                final int PRIME = 31;
                result = PRIME + (isVirtual() ? 0 : 1);
                result = PRIME * result + (isVirtual() ? 0 : 1);
                result = PRIME * result + name.hashCode();
                this.hashCode = result;
            }
            return result;
        }

        @Override
        public boolean isGroup() {
            return group;
        }

        @Override
        public boolean isVirtual() {
            return false;
        }

        @Override
        public String toString() {
            return (isVirtual() ? "v" : "") + (isGroup() ? "group:" : "user:") + name;
        }

    }

    public static class DefaultVirtualMailboxOwner extends DefaultMailboxOwner implements VirtualMailboxOwner {
        private final String domainPart;
        private final String userPart;

        public DefaultVirtualMailboxOwner(String userPart, String domainPart, boolean isOwnerGroup) {
            super(userPart + MailboxConstants.AT + domainPart, isOwnerGroup);
            this.domainPart = domainPart;
            this.userPart = userPart;
        }

        @Override
        public String getDomainPart() {
            return domainPart;
        }

        @Override
        public String getUserPart() {
            return userPart;
        }

        @Override
        public boolean isVirtual() {
            return true;
        }
    }

    protected static final String BACKEND_GROUPS_ROOT;

    protected static final String BACKEND_USERS_ROOT;

    protected static final String BACKEND_VIRTUAL_GROUPS_ROOT;
    protected static final String BACKEND_VIRTUAL_USERS_ROOT;
    public static final DefaultMailboxNameResolver INSTANCE;
    protected static final char NAMESPACE_PREFIX_CHAR;

    static final MailboxOwner NULL_OWNER = new DefaultMailboxOwner();
    protected static final String OTHER_USERS_NAMESPACE_PREFIX;
    protected static final Set<String> OTHER_USERS_NAMESPACE_PREFIXES;
    protected static final String OTHER_USERS_VIRTUAL_NAMESPACE_PREFIX;
    protected static final String PERSONAL_NAMESPACE_PREFIX;
    protected static final String SHARED_NAMESPACE_PREFIX;
    protected static final Set<String> SHARED_NAMESPACE_PREFIXES;
    protected static final String SHARED_VIRTUAL_NAMESPACE_PREFIX;
    /*
     * Constant initialisations are in a static block to be able to sort the
     * class members alphabetically.
     */
    static {
        NAMESPACE_PREFIX_CHAR = '#';
        BACKEND_USERS_ROOT = NAMESPACE_PREFIX_CHAR + "users";
        BACKEND_GROUPS_ROOT = NAMESPACE_PREFIX_CHAR + "groups";
        BACKEND_VIRTUAL_USERS_ROOT = NAMESPACE_PREFIX_CHAR + "vusers";
        BACKEND_VIRTUAL_GROUPS_ROOT = NAMESPACE_PREFIX_CHAR + "vgroups";
        PERSONAL_NAMESPACE_PREFIX = "";
        OTHER_USERS_NAMESPACE_PREFIX = NAMESPACE_PREFIX_CHAR + "Users";
        SHARED_NAMESPACE_PREFIX = NAMESPACE_PREFIX_CHAR + "Groups";
        OTHER_USERS_VIRTUAL_NAMESPACE_PREFIX = NAMESPACE_PREFIX_CHAR + "Users";
        SHARED_VIRTUAL_NAMESPACE_PREFIX = NAMESPACE_PREFIX_CHAR + "Groups";

        Set<String> s = new LinkedHashSet<String>();
        s.add(OTHER_USERS_NAMESPACE_PREFIX);
        s.add(OTHER_USERS_VIRTUAL_NAMESPACE_PREFIX);
        OTHER_USERS_NAMESPACE_PREFIXES = Collections.unmodifiableSet(s);

        s = new LinkedHashSet<String>();
        s.add(SHARED_NAMESPACE_PREFIX);
        s.add(SHARED_VIRTUAL_NAMESPACE_PREFIX);
        SHARED_NAMESPACE_PREFIXES = Collections.unmodifiableSet(s);

        INSTANCE = new DefaultMailboxNameResolver();
    }
    private final Set<String> otherUsersNamespaces;

    private final Set<String> personalNamespaces;

    private final Set<String> sharedNamespaces;

    public DefaultMailboxNameResolver() {
        super();
        this.personalNamespaces = createNamespaces(MailboxNamespaceType.personal);
        this.otherUsersNamespaces = createNamespaces(MailboxNamespaceType.otherUsers);
        this.sharedNamespaces = createNamespaces(MailboxNamespaceType.shared);
    }

    /**
     * Adds segments representing a user name to the {@code segments} list. If
     * {@code userName} contains '{@value MailboxConstants#AT}' (a.k.a. {link
     * MailboxConstants#AT}), adds two segments: (i) the domain part and (ii)
     * the user part of the email address. Otherwise adds {@code userName} to
     * the {@code segments} list.
     * 
     * @param userName
     * @param segments
     */
    protected void addUserOrGroupName(String userName, boolean isOwnerGroup, boolean isVirtual, MailboxNameBuilder nameBuilder) {
        int atPos = userName.indexOf(MailboxConstants.AT);
        if (atPos >= 0) {
            /* domain/user quirks */
            nameBuilder.add(isOwnerGroup ? BACKEND_VIRTUAL_GROUPS_ROOT : BACKEND_VIRTUAL_USERS_ROOT);
            String userPart = userName.substring(0, atPos);
            String domainPart = userName.substring(atPos + 1);
            nameBuilder.add(domainPart);
            nameBuilder.add(userPart);
        } else {
            /* domainless user name */
            nameBuilder.add(isOwnerGroup ? BACKEND_GROUPS_ROOT : BACKEND_USERS_ROOT);
            nameBuilder.add(userName);
        }
    }

    /**
     * @see org.apache.james.imap.api.name.MailboxNameResolver#unresolve(org.apache.james.mailbox.name.MailboxName,
     *      org.apache.james.mailbox.MailboxSession)
     */
    public UnresolvedMailboxName unresolve(MailboxName maiboxName, String sessionUser) {
        ListIterator<String> segmentsIt = maiboxName.segmentsIterator();
        String firstSegment = segmentsIt.next();
        if (!hasNamespacePrefix(firstSegment)) {
            throw new IllegalArgumentException("Cannot relativize " + MailboxName.class.getName() + " \"" + maiboxName.toString() + "\" whose first segment does not start with '" + NAMESPACE_PREFIX_CHAR + "' (a.k.a. " + MailboxConstants.class.getName() + ".NAMESPACE_PREFIX_CHAR)");
        }
        if (firstSegment.equals(BACKEND_USERS_ROOT)) {
            if (segmentsIt.hasNext()) {
                String pathUserOrDomain = segmentsIt.next();

                int atPos = sessionUser.indexOf(MailboxConstants.AT);
                if (atPos >= 0 && segmentsIt.hasNext()) {
                    /* domain/user quirks */

                    String pathUser = segmentsIt.next();
                    String userPart = sessionUser.substring(0, atPos);
                    String domainPart = sessionUser.substring(atPos + 1);

                    if (domainPart.equals(pathUserOrDomain) && userPart.equals(pathUser)) {
                        /*
                         * Current user's INBOX - replace pathUserOrDomain and
                         * pathUser with INBOX
                         */
                        MailboxNameBuilder nb = new MailboxNameBuilder(maiboxName.getSegmentCount() - 2);
                        while (segmentsIt.hasNext()) {
                            nb.add(segmentsIt.next());
                        }
                        return nb.unqualified();
                    } else {
                        /*
                         * Not the current user's INBOX - just forward
                         * everything that was read from segmentsIt.
                         */
                        MailboxNameBuilder nb = new MailboxNameBuilder(maiboxName.getSegmentCount());
                        nb.add(getOtherUsersNameSpacePrefix(userPart, domainPart));
                        nb.add(pathUserOrDomain);
                        nb.add(pathUser);
                        while (segmentsIt.hasNext()) {
                            nb.add(segmentsIt.next());
                        }
                        return nb.unqualified();
                    }
                } else {
                    /* domainless user name */
                    if (pathUserOrDomain.equals(sessionUser)) {
                        /*
                         * Current user's INBOX - replace pathUserOrDomain with
                         * INBOX
                         */
                        MailboxNameBuilder nb = new MailboxNameBuilder(maiboxName.getSegmentCount() - 1);
                        nb.add(MailboxConstants.INBOX);
                        while (segmentsIt.hasNext()) {
                            nb.add(segmentsIt.next());
                        }
                        return nb.unqualified();
                    } else {
                        /*
                         * Not the current user's INBOX - just forward
                         * everything that was read from segmentsIt.
                         */
                        MailboxNameBuilder nb = new MailboxNameBuilder(maiboxName.getSegmentCount());
                        nb.add(getOtherUsersNameSpacePrefix(pathUserOrDomain, null));
                        nb.add(pathUserOrDomain);
                        while (segmentsIt.hasNext()) {
                            nb.add(segmentsIt.next());
                        }
                        return nb.unqualified();
                    }
                }
            } else {
                /* USERS_ROOT and nothing else coming after it. */
                return new MailboxNameBuilder(1).add(getOtherUsersNameSpacePrefix(null, null)).unqualified();
            }
        } else {
            /* nothing to relativize - just pass through */
            return new DefaultUnresolvedMailboxName(maiboxName);
        }
    }

    /**
     * Subclasses may wish to override this method.
     * 
     * @param personal
     * @return
     */
    protected Set<String> createNamespaces(MailboxNamespaceType type) {
        switch (type) {
        case personal:
            return Collections.singleton(PERSONAL_NAMESPACE_PREFIX);
        case otherUsers:
            return OTHER_USERS_NAMESPACE_PREFIXES;
        case shared:
            return SHARED_NAMESPACE_PREFIXES;
        default:
            throw new IllegalArgumentException("Unexpected " + MailboxNamespaceType.class.getName() + " value '" + type + "'.");
        }
    }

    /**
     * If there are any segments in the given {@code maiboxName} whose meaning
     * depends on the user name of the current user (e.g. "#personal", "INBOX",
     * etc.), it translates these segments to a form
     * {@link MailboxName} where no such dependency is present. It
     * usually means replacing e.g. "INBOX" with an absolute sequence of
     * segments, e.g. "#users.joe".
     * 
     * @see org.apache.james.imap.api.name.MailboxNameResolver#resolve(java.lang.String,
     *      org.apache.james.mailbox.MailboxSession)
     */
    public MailboxName resolve(final UnresolvedMailboxName mailboxName, final String currentUser) {
        if (mailboxName == null || mailboxName.isEmpty()) {
            return MailboxName.EMPTY;
        } else {

            ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
            String firstSegment = segmentsIt.next();
            boolean startsWithInbox = MailboxConstants.INBOX.equalsIgnoreCase(firstSegment);
            if (personalNamespaces.contains(firstSegment) || startsWithInbox) {
                boolean hasRoot = true;
                boolean virtualUser = isVirtualUser(currentUser);
                MailboxNameBuilder nameBuilder = new MailboxNameBuilder(mailboxName.getSegmentCount() + (virtualUser ? 2 : 1));
                addUserOrGroupName(currentUser, false, virtualUser, nameBuilder);

                if (segmentsIt.hasNext()) {
                    String secondSegment = segmentsIt.next();
                    if (!startsWithInbox && MailboxConstants.INBOX.equalsIgnoreCase(secondSegment)) {
                        /*
                         * #personal/INBOX handled in the same way as INBOX -
                         * just ignore the INBOX after #personal
                         */
                    } else {
                        nameBuilder.add(secondSegment);
                    }
                    /* copy the rest */
                    while (segmentsIt.hasNext()) {
                        nameBuilder.add(segmentsIt.next());
                    }
                }
                return nameBuilder.qualified(hasRoot);
            } else if (otherUsersNamespaces.contains(firstSegment) || sharedNamespaces.contains(firstSegment)) {
                boolean hasRoot = true;
                /* replace firstSegment with BACKEND_USERS_ROOT */
                List<String> result = new ArrayList<String>(mailboxName.getSegmentCount() + 2);
                result.add(toBackendPrefix(firstSegment));
                /* copy the rest */
                while (segmentsIt.hasNext()) {
                    result.add(segmentsIt.next());
                }
                return new DefaultMailboxName(result, hasRoot);
            } else if (!hasNamespacePrefix(firstSegment)) {
                /* no namespace prefix - non-absolute name */
                boolean hasRoot = false;
                return new DefaultMailboxName(mailboxName, hasRoot);
            } else {
                /* illegal prefix */
                StringBuilder sb = new StringBuilder(64);
                sb.append(PERSONAL_NAMESPACE_PREFIX);
                for (String prefix : otherUsersNamespaces) {
                    sb.append(", '").append(prefix).append('\'');
                }
                throw new RuntimeException("Cannot decontextualize " + mailboxName + ": unexpected prefix '" + firstSegment + "'; expected one of {" + sb.toString() + "}");
            }
        }
    }

    @Override
    public boolean existsTrivially(MailboxName mailboxName) {
        if (!mailboxName.hasRoot()) {
            throw new IllegalArgumentException("Cannot check the existence of a rootless mailbox '" + mailboxName + "'");
        }
        if (mailboxName.isEmpty()) {
            /* an empty mailbox name exists trivially */
            return true;
        } else {
            int segmentCount = mailboxName.getSegmentCount();
            if (segmentCount <= 1) {
                /*
                 * the first-level folders which roughly correspond to IMAP
                 * namespace prefixes exist trivially
                 */
                return true;
            } else if (segmentCount == 2 && isVirtualPrefix(mailboxName.getSegmentAt(0))) {
                /*
                 * we return true for #something.domainname as there are no
                 * mailboxes for domains themselves
                 */
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * TODO findOwner.
     * 
     * @param mailboxName
     * @return
     */
    MailboxOwner findOwner(MailboxName mailboxName) {

        if (mailboxName.hasRoot()) {
            ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
            String prefix = segmentsIt.next();
            if (isVirtualPrefix(prefix)) {
                if (segmentsIt.nextIndex() + 1 < mailboxName.getSegmentCount()) {
                    String domain = segmentsIt.next();
                    if (MailboxQuery.containsWidcard(domain)) {
                        return null;
                    } else {
                        String user = segmentsIt.next();
                        if (MailboxQuery.containsWidcard(user)) {
                            return null;
                        } else {
                            DefaultMailboxOwner result = new DefaultVirtualMailboxOwner(user, domain, isGroupPrefix(prefix));
                            /*
                             * Usually, when we look for the owner of a mailbox,
                             * then we will subsequently look for the inbox of the
                             * given owner. So we pre-cache it here.
                             */
                            result.inbox = mailboxName.prefix(getInboxSegmentCount(result.isVirtual()));
                            return result;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                if (segmentsIt.hasNext()) {
                    String user = segmentsIt.next();
                    if (MailboxQuery.containsWidcard(user)) {
                        return null;
                    } else {
                        DefaultMailboxOwner result = new DefaultMailboxOwner(user, isGroupPrefix(prefix));
                        /*
                         * Usually, when we look for the owner of a mailbox,
                         * then we will subsequently look for the inbox of the
                         * given owner. So we pre-cache it here.
                         */
                        result.inbox = mailboxName.prefix(getInboxSegmentCount(result.isVirtual()));
                        return result;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxNameResolver#getContextualizedComparator()
     */
    @Override
    public Comparator<MailboxName> getContextualizedComparator(String currentUser) {
        return new ContextualizedComparator(getInbox(currentUser));
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxNameResolver#getInbox(java.lang.String)
     */
    @Override
    public MailboxName getInbox(String userOrGroup) {
        return buildInbox(userOrGroup, false, isVirtualUser(userOrGroup));
    }

    private MailboxName buildInbox(String userOrGroup, boolean isGroup, boolean isVirtual) {
        MailboxNameBuilder nameBuilder = new MailboxNameBuilder(getInboxSegmentCount(isVirtual));
        addUserOrGroupName(userOrGroup, isGroup, isVirtual, nameBuilder);
        return nameBuilder.qualified(true);
    }

    private int getInboxSegmentCount(boolean isVirtual) {
        return isVirtual ? 3 : 2;
    }

    @Override
    public MailboxName getInbox(MailboxOwner mailboxOwner) {
        if (mailboxOwner == null) {
            return null;
        } else if (mailboxOwner instanceof DefaultMailboxOwner) {
            MailboxName result = ((DefaultMailboxOwner) mailboxOwner).inbox;
            if (result == null) {
                result = buildInbox(mailboxOwner.getName(), mailboxOwner.isGroup(), mailboxOwner.isVirtual());
                ((DefaultMailboxOwner) mailboxOwner).inbox = result;
            }
            return result;
        } else {
            return buildInbox(mailboxOwner.getName(), mailboxOwner.isGroup(), mailboxOwner.isVirtual());
        }
    }

    /**
     * Returns the first {@link MailboxNamespace} available in
     * {@link #otherUsersNamespaces}. This O.K. for this implementation as we
     * have only a single other users' namespace there.
     * 
     * Subclasses with multiple other users' namespaces should consider
     * overriding this method.
     * 
     * @param userPart
     * @param domainPart
     * @return
     */
    protected String getOtherUsersNameSpacePrefix(String userPart, String domainPart) {
        return otherUsersNamespaces.iterator().next();
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxNameResolver#getOwner(org.apache.james.mailbox.name.MailboxName)
     */
    @Override
    public MailboxOwner getOwner(MailboxName mailboxName) {

        if (!mailboxName.hasRoot() || existsTrivially(mailboxName)) {
            return null;
        } else {
            if (mailboxName instanceof DefaultMailboxName) {
                DefaultMailboxName dqmn = (DefaultMailboxName) mailboxName;
                MailboxOwner result = dqmn.getCachedOwner();
                if (result == null) {
                    result = findOwner(mailboxName);
                    if (result == null) {
                        dqmn.setCachedOwner(DefaultMailboxNameResolver.NULL_OWNER);
                    } else {
                        dqmn.setCachedOwner(result);
                    }
                }
                return result;
            } else {
                return findOwner(mailboxName);
            }

        }

    }

    /**
     * returns {@code true} if the given {@code mailboxName} starts with '
     * {@value MailboxConstants#NAMESPACE_PREFIX_CHAR}' (a.k.a
     * {@link MailboxConstants#NAMESPACE_PREFIX_CHAR}) and {@code false}
     * otherwise.
     * 
     * @param mailboxName
     * @return
     */
    protected boolean hasNamespacePrefix(String mailboxName) {
        return mailboxName != null && mailboxName.length() > 0 && mailboxName.charAt(0) == NAMESPACE_PREFIX_CHAR;
    }

    protected boolean isGroupPrefix(String backendPrefix) {
        return backendPrefix.equals(BACKEND_VIRTUAL_GROUPS_ROOT) || backendPrefix.equals(BACKEND_GROUPS_ROOT);
    }

    @Override
    public boolean isVirtualPrefix(String backendPrefix) {
        return backendPrefix.equals(BACKEND_VIRTUAL_USERS_ROOT) || backendPrefix.equals(BACKEND_VIRTUAL_GROUPS_ROOT);
    }

    protected boolean isVirtualUser(String userName) {
        return userName.indexOf(MailboxConstants.AT) >= 0;
    }

    public MailboxOwner getOwner(String userOrGroupName, boolean isGroup) {
        int atPos = userOrGroupName.indexOf(MailboxConstants.AT);
        if (atPos >= 0) {
            return new DefaultVirtualMailboxOwner(userOrGroupName.substring(0, atPos), userOrGroupName.substring(atPos + 1), isGroup);
        } else {
            return new DefaultMailboxOwner(userOrGroupName, isGroup);
        }
    }

    /**
     * @see org.apache.james.mailbox.name.MailboxNameResolver#listNamespacePrefixes(org.apache.james.mailbox.name.MailboxNamespaceType)
     */
    public Collection<String> listNamespacePrefixes(MailboxNamespaceType type) {
        switch (type) {
        case personal:
            return personalNamespaces;
        case otherUsers:
            return otherUsersNamespaces;
        case shared:
            return sharedNamespaces;
        default:
            throw new IllegalArgumentException("Unexpected " + MailboxNamespaceType.class.getName() + " value '" + type + "'.");
        }
    }

    protected String toBackendPrefix(String prefix) {
        return prefix;
    }

    @Override
    public boolean hasRoot(UnresolvedMailboxName mailboxName) {
        if (mailboxName == null || mailboxName.isEmpty()) {
            return false;
        } else {

            ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
            String firstSegment = segmentsIt.next();
            boolean startsWithInbox = MailboxConstants.INBOX.equalsIgnoreCase(firstSegment);
            if (personalNamespaces.contains(firstSegment) || startsWithInbox) {
                return true;
            } else if (otherUsersNamespaces.contains(firstSegment) || sharedNamespaces.contains(firstSegment)) {
                return true;
            } else if (!hasNamespacePrefix(firstSegment)) {
                /* no namespace prefix - non-absolute name */
                return true;
            } else {
                /* illegal prefix */
                return false;
            }
        }
    }

    @Override
    public UnresolvedMailboxName getRoot(UnresolvedMailboxName mailboxName) {
        if (mailboxName == null || mailboxName.isEmpty()) {
            return UnresolvedMailboxName.EMPTY;
        } else {

            ListIterator<String> segmentsIt = mailboxName.segmentsIterator();
            String firstSegment = segmentsIt.next();
            boolean startsWithInbox = MailboxConstants.INBOX.equalsIgnoreCase(firstSegment);
            if (personalNamespaces.contains(firstSegment) || startsWithInbox) {
                return new DefaultUnresolvedMailboxName(firstSegment);
            } else if (otherUsersNamespaces.contains(firstSegment) || sharedNamespaces.contains(firstSegment)) {
                return new DefaultUnresolvedMailboxName(firstSegment);
            } else if (!hasNamespacePrefix(firstSegment)) {
                /* no namespace prefix - non-absolute name */
                return UnresolvedMailboxName.EMPTY;
            } else {
                /* illegal prefix */
                return UnresolvedMailboxName.EMPTY;
            }
        }
    }

}

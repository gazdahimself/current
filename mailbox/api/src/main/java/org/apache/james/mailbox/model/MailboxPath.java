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

package org.apache.james.mailbox.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mailbox.MailboxSession;

/**
 * The path to a mailbox.
 * 
 * The contract:
 * <ul>
 * <li>Null values are forbidden for {@link #namespace} and {@link #name}. Empty
 * string is a legal value for {@link #namespace} and {@link #name}.</li>
 * <li>It is not checked if {@link #name} starts or ends with the path
 * delimiter. Callers must check it when necessary.</li>
 * </ul>
 */
public class MailboxPath {

    public static final MailboxPath EMPTY = new MailboxPath("", null, "");

    private final String namespace;
    private final String user;
    private final String name;

    public MailboxPath(String namespace, String user, String name) {
        if (namespace == null) {
            throw new IllegalArgumentException("Cannot create a " + MailboxPath.class.getName() + " with null namespace.");
        }
        if (name == null) {
            throw new IllegalArgumentException("Cannot create a " + MailboxPath.class.getName() + " with null name.");
        }
        this.namespace = namespace;
        this.user = user;
        this.name = name;
    }

    public MailboxPath(MailboxPath mailboxPath) {
        this(mailboxPath.getNamespace(), mailboxPath.getUser(), mailboxPath.getName());
    }

    /**
     * Creates a new instance of {@link MailboxPath} with the given
     * {@code newName}. Namespace and user are copied from {@code source}.
     * 
     * Warning: Not suitable for creating child pathes! See
     * {@link #MailboxPath(MailboxPath, String, char)} and
     * {@link #parseRelativePath(MailboxSession, String)}.
     * 
     * A replacement for {@code public void setName(String name)} after
     * this.name became final.
     * 
     * 
     * @param source
     * @param newName
     */
    public MailboxPath(MailboxPath mailboxPath, String newName) {
        this(mailboxPath.getNamespace(), mailboxPath.getUser(), newName);
    }

    /**
     * Creates a new instance of {@link MailboxPath} relative to the given
     * {@code referencePath}.
     * 
     * Suitable for creating child paths and relative paths. However if you are
     * not sure that your {@code relativeName} is relative (i.e. not starting
     * with {@link MailboxConstants#NAMESPACE_PREFIX_CHAR}), you should rather
     * use {@link #parseRelativePath(MailboxSession, String)}.
     * 
     * @param referencePath
     * @param relativeName
     * @param delimiter
     * 
     * @throws IllegalArgumentException
     *             via {@link #forceRelativeName(String, String, char)} if the
     *             given {@code relativeName} starts with
     *             {@link MailboxConstants#NAMESPACE_PREFIX_CHAR}.
     */
    public MailboxPath(MailboxPath referencePath, String relativeName, char delimiter) {
        this(referencePath.getNamespace(), referencePath.getUser(), forceRelativeName(referencePath.getName(), relativeName, delimiter));
    }

    /**
     * Get the namespace this mailbox is in
     * 
     * @return The namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the name of the user who owns the mailbox. This can be null e.g. for
     * shared mailboxes.
     * 
     * @return The username
     */
    public String getUser() {
        return user;
    }

    /**
     * Get the name of the mailbox. This is the pure name without user or
     * namespace, so this is what a user would see in his client.
     * 
     * @return The name string
     */
    public String getName() {
        return name;
    }

    /**
     * Return a list of MailboxPath representing the hierarchy levels of this
     * MailboxPath. E.g. INBOX.main.sub would yield
     * 
     * <pre>
     * INBOX
     * INBOX.main
     * INBOX.main.sub
     * </pre>
     * 
     * @param delimiter
     * @return list of hierarchy levels
     */
    public List<MailboxPath> getHierarchyLevels(char delimiter) {
        ArrayList<MailboxPath> levels = new ArrayList<MailboxPath>();
        int index = name.indexOf(delimiter);
        while (index >= 0) {
            final String levelname = name.substring(0, index);
            levels.add(new MailboxPath(namespace, user, levelname));
            index = name.indexOf(delimiter, ++index);
        }
        levels.add(this);
        return levels;
    }

    /**
     * Creates a new {@link MailboxPath} out of {@code childSegmentName} which
     * is supposed to be a single name segment containing no {@code delimiter}s.
     * 
     * @param childSegmentName
     * @param delimiter
     * @return
     */
    public MailboxPath createChildPath(String childSegmentName, char delimiter) {
        return new MailboxPath(namespace, user, name + delimiter + childSegmentName);
    }

    /**
     * Creates a new {@link MailboxPath} using this {@link MailboxPath} as base
     * and {@code relativeName}.
     * 
     * 
     * which is supposed to be a single name segment containing no
     * {@code delimiter}s.
     * 
     * 
     * 
     * @param relativeName
     * @param delimiter
     * @return
     */
    public MailboxPath parseRelativePath(MailboxSession session, String relativeName) {
        if (hasNamespacePrefix(relativeName)) {
            return parse(session, relativeName);
        } else {
            return new MailboxPath(this, relativeName, '.');
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return namespace + ":" + user + ":" + name;
    }

    /**
     * Returns a fully qualified representation of this {@link MailboxPath},
     * which is usually equivalent to {@code namespace + delimiter + name}. Be
     * aware that the fully qualified representation does not contain the
     * {@link #user} attribute value.
     * 
     * @param delimiter
     * @return
     */
    public String toString(char delimiter) {
        int bufferLength = namespace.length() + name.length() + 1;
        StringBuilder result = new StringBuilder(bufferLength);
        result.append(namespace);
        if (name.length() > 0) {
            if (result.length() > 0) {
                result.append(delimiter);
            }
            result.append(name);
        }
        return result.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object mailboxPath) {
        if (this == mailboxPath)
            return true;

        if (!(mailboxPath instanceof MailboxPath))
            return false;
        MailboxPath mp = (MailboxPath) mailboxPath;
        if (namespace == null) {
            if (mp.getNamespace() != null)
                return false;
        } else if (!namespace.equals(mp.getNamespace()))
            return false;
        if (user == null) {
            if (mp.getUser() != null)
                return false;
        } else if (!user.equals(mp.getUser()))
            return false;
        if (name == null) {
            if (mp.getName() != null)
                return false;
        } else if (!name.equals(mp.getName()))
            return false;
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        if (getName() != null)
            result = PRIME * result + getName().hashCode();
        if (getUser() != null)
            result = PRIME * result + getUser().hashCode();
        if (getNamespace() != null)
            result = PRIME * result + getNamespace().hashCode();
        return result;
    }

    /**
     * Return the full name of the {@link MailboxPath}, which is constructed via
     * the {@link #namespace} and {@link #name}
     * 
     * @param delimiter
     * @return fullName
     */
    public String getFullName(char delimiter) {
        return namespace + delimiter + name;
    }

    /**
     * Return a {@link MailboxPath} which represent the INBOX of the given
     * session
     * 
     * @param session
     * @return inbox
     */
    public static MailboxPath inbox(MailboxSession session) {
        return new MailboxPath("", session.getUser().getUserName(), MailboxConstants.INBOX);
    }

    /**
     * Create a {@link MailboxPath} by parsing the given fully qualified string
     * representation of the maibox name. The fully qualified string usually
     * consists of the sequence namespace-delimiter-name, where namespace starts
     * with '{@value MailboxConstants#NAMESPACE_PREFIX_CHAR}' (a.k.a.
     * {@link MailboxConstants#NAMESPACE_PREFIX_CHAR}) and where both namespace
     * and name can be empty strings. So "" is parsed as
     * {@code new MailboxPath("", null, "")}.
     * 
     * Be aware that the fully qualified representation does not contain the
     * {@link #user} attribute value. The user from the given
     * {@link MailboxSession} is supplied to the resulting {@link MailboxPath}
     * only namespace.equals(session.getPersonalSpace()) is true.
     * 
     * @param session
     * @param fullmailboxname
     * @return path
     */
    public static MailboxPath parse(MailboxSession session, String fullmailboxname) {
        if (fullmailboxname == null) {
            throw new IllegalArgumentException("Cannot parse a null mailbox name string.");
        }
        if (fullmailboxname.length() == 0) {
            return EMPTY;
        }

        String namespace;
        String mailbox;
        String username = null;

        /*
         * From http://tools.ietf.org/html/rfc3501#section-5.1.2 : Mailbox
         * Namespace Naming Convention By convention, the first hierarchical
         * element of any mailbox name which begins with "#" identifies the
         * "namespace" of the remainder of the name.
         */
        if (hasNamespacePrefix(fullmailboxname)) {
            char delimiter = '.';
            int i = fullmailboxname.indexOf(delimiter);
            if (i >= 0) {
                namespace = fullmailboxname.substring(0, i);
                mailbox = fullmailboxname.substring(i + 1, fullmailboxname.length());
            }
            else {
                namespace = fullmailboxname;
                mailbox = "";
            }
        } else {
            namespace = "";
            mailbox = fullmailboxname;
        }
        /* IMAP-349: use uppercase for INBOX */
        if (mailbox.equalsIgnoreCase(MailboxConstants.INBOX)) {
            mailbox = MailboxConstants.INBOX;
        }

        if (namespace.equals("")) {
            /*
             * we only use the user as part of the MailboxPath if its a private
             * namespace
             */
            username = session.getUser().getUserName();
        }
        return new MailboxPath(namespace, username, mailbox);

    }

    public static boolean hasNamespacePrefix(String name) {
        return name != null && name.length() > 0 && name.charAt(0) == '#';
    }
    
    /**
     *  
     *
     * @param name
     * @return
     */
    public static String getFirstSegment(String name, char delimiter) {
        int i = name.indexOf(delimiter);
        if (i >= 0) {
            return name.substring(0, i);
        } else {
            return name;
        }
    }

    /**
     * 
     * @param name2
     * @param relativeName
     * @param delimiter
     * @return
     */
    private static String forceRelativeName(String referenceName, String relativeName, char delimiter) {

        if (hasNamespacePrefix(relativeName)) {
            throw new IllegalArgumentException("Relative name (i.e. a name not starting with '" + '#' + "') expected in relativeName parameter; found: \"" + relativeName + "\".");
        }

        int refNameLength = referenceName.length();
        StringBuilder result = new StringBuilder(refNameLength + relativeName.length() + delimiter);
        if (refNameLength > 0) {
            result.append(referenceName);
            if (referenceName.charAt(refNameLength - 1) != delimiter) {
                result.append(delimiter);
            }
        }
        result.append(relativeName);

        return result.toString();
    }

}

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

package org.apache.james.mailbox.maildir.locator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;

/**
 * TODO LocalUsersMaildirNameLocator.
 */
public class LocalSystemMaildirLocator extends AbstractMaildirLocator {

    protected final File groupHomesRoot;
    protected final File userHomesRoot;

    public LocalSystemMaildirLocator(File userHomesRoot, File groupHomesRoot) {
        this(userHomesRoot, groupHomesRoot, null);
    }
    public LocalSystemMaildirLocator(File userHomesRoot, File groupHomesRoot, ChainedMaildirLocator next) {
        super(next);
        
        if (userHomesRoot.equals(groupHomesRoot)) {
            throw new IllegalArgumentException("User home directory root and group home directory root may not be the same directory; found: '" + userHomesRoot + "'.");
        }
        try {
            this.userHomesRoot = userHomesRoot.getCanonicalFile().getAbsoluteFile();
            this.groupHomesRoot = groupHomesRoot.getCanonicalFile().getAbsoluteFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!this.userHomesRoot.exists()) {
            if (!this.userHomesRoot.mkdirs()) {
                throw new RuntimeException("Unable to create users' homes root " + this.userHomesRoot.getPath());
            }
        }

        if (!this.groupHomesRoot.exists()) {
            if (!this.groupHomesRoot.mkdirs()) {
                throw new RuntimeException("Unable to create users' homes root " + this.groupHomesRoot.getPath());
            }
        }

    }
    @Override
    public boolean accepts(File inbox) {
        return isDescendant(userHomesRoot, inbox) || isDescendant(groupHomesRoot, inbox);
    }
    @Override
    public boolean accepts(MailboxNameResolver mailboxNameResolver, MailboxName mailboxName) {
        return mailboxName.getSegmentCount() >= 1 && !mailboxNameResolver.isVirtualPrefix(mailboxName.getSegmentAt(0));
    }
    @Override
    public boolean accepts(MailboxOwner owner) {
        return !owner.isVirtual();
    }
    /**
     * Returns the home directory for the given user or group.
     * 
     * @param userOrGroup
     * @param isGroup
     * @return
     * @throws IOException
     */
    protected File getHome(MailboxOwner owner) {
        return new File(owner.isGroup() ? groupHomesRoot : userHomesRoot, owner.getName());
    }

    protected File getMaildir(File home, boolean isGroup) {
        return new File(home, MaildirFolder.MAILDIR);
    }

    public MailboxOwner getOwner(File maildir) {
        if (isDescendant(userHomesRoot, maildir)) {
            return new DefaultMailboxNameResolver.DefaultMailboxOwner(getRelativePath(userHomesRoot, maildir).getName(), false);
        } else if (isDescendant(groupHomesRoot, maildir)) {
            return new DefaultMailboxNameResolver.DefaultMailboxOwner(getRelativePath(groupHomesRoot, maildir).getName(), false);
        } else {
            return null;
        }
    }

    protected Collection<File> listMaildirs(boolean isGroup) {
        File[] homes = isGroup ? groupHomesRoot.listFiles() : userHomesRoot.listFiles();
        List<File> result = new ArrayList<File>(homes.length);
        for (File home : homes) {
            if (directoryExists(getMaildir(home, isGroup))) {
                File maildir = getMaildir(home, isGroup);
                result.add(maildir);
            }
        }
        return result;
    }
    
    @Override
    public Collection<MailboxOwner> listOwners() {
        File[] homes = userHomesRoot.listFiles();
        ArrayList<MailboxOwner> result = new ArrayList<MailboxOwner>(homes.length * 2);
        boolean isGroup = false;
        for (File home : homes) {
            if (directoryExists(getMaildir(home, isGroup))) {
                result.add(new DefaultMailboxNameResolver.DefaultMailboxOwner(home.getName(), isGroup));
            }
        }
        homes = groupHomesRoot.listFiles();
        result.ensureCapacity(result.size() + homes.length);
        isGroup = true;
        for (File home : homes) {
            if (directoryExists(getMaildir(home, isGroup))) {
                result.add(new DefaultMailboxNameResolver.DefaultMailboxOwner(home.getName(), isGroup));
            }
        }
        
        if (next != null) {
            result.addAll(next.listOwners());
        }
        
        return result;
    }

}

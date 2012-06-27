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

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.name.DefaultMailboxNameResolver;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;

/**
 * TODO VirtualMaildirLocator.
 */
public class VirtualMaildirLocator extends AbstractMaildirLocator {
    
    private static final String GROUPS_DIRECTORY = "groups";
    private static final String USERS_DIRECTORY = "users";


    private final File maildirRoot;

    public VirtualMaildirLocator(File maildirRoot) {
        this(maildirRoot, null);
    }
    
    public VirtualMaildirLocator(File maildirRoot, ChainedMaildirLocator next) {
        super(next);
        try {
            this.maildirRoot = maildirRoot.getCanonicalFile().getAbsoluteFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!this.maildirRoot.exists()) {
            if (!this.maildirRoot.mkdirs()) {
                throw new RuntimeException("Unable to create users' homes root " + this.maildirRoot.getPath());
            }
        }

    }

    @Override
    public boolean accepts(MailboxNameResolver mailboxNameResolver, MailboxName mailboxName) {
        return mailboxName.getSegmentCount() >= 1 && mailboxNameResolver.isVirtualPrefix(mailboxName.getSegmentAt(0));
    }
    
    @Override
    public boolean accepts(MailboxOwner owner) {
        return owner.isVirtual();
    }

    @Override
    protected File getMaildir(File home, boolean isGroup) {
        return home;
    }

    @Override
    public boolean accepts(File inbox) {
        return isDescendant(maildirRoot, inbox);
    }
    
    protected boolean isDomainDirectory(File directory) {
        return true;
    }

    @Override
    public Collection<MailboxOwner> listOwners() {
        File[] domains = maildirRoot.listFiles();
        ArrayList<MailboxOwner> result = new ArrayList<MailboxOwner>(domains.length * 4);
        for (File domain : domains) {
            if (isDomainDirectory(domain)) {
                
                File users = new File(domain, USERS_DIRECTORY);
                if (users.exists()) {
                    File[] homes = users.listFiles();
                    boolean isGroup = false;
                    for (File home : homes) {
                        if (directoryExists(getMaildir(home, isGroup))) {
                            result.add(new DefaultMailboxNameResolver.DefaultVirtualMailboxOwner(home.getName(), domain.getName(), isGroup));
                        }
                    }
                }
                
                
                File groups = new File(domain, GROUPS_DIRECTORY);
                if (groups.exists()) {
                    File[] homes = groups.listFiles();
                    boolean isGroup = true;
                    for (File home : homes) {
                        if (directoryExists(getMaildir(home, isGroup))) {
                            result.add(new DefaultMailboxNameResolver.DefaultVirtualMailboxOwner(home.getName(), domain.getName(), isGroup));
                        }
                    }
                }
            }
        }
        
        
        if (next != null) {
            result.addAll(next.listOwners());
        }
        
        return result;
    }

    @Override
    protected File getHome(MailboxOwner owner) {
        if (!owner.isVirtual()) {
            throw new IllegalArgumentException("Cannot create the home path for a non-virtual user '"+ owner +"'.");
        }
        else {
            String userOrGroup = owner.getName();
            int atPos = userOrGroup.indexOf(MailboxConstants.AT);
            String domain = userOrGroup.substring(atPos + 1);
            String user = userOrGroup.substring(0, atPos);
            StringBuilder sb = new StringBuilder(128)
            .append(maildirRoot.getAbsolutePath())
            .append(File.separatorChar)
            .append(domain)
            .append(File.separatorChar)
            .append(owner.isGroup() ? GROUPS_DIRECTORY : USERS_DIRECTORY)
            .append(File.separatorChar)
            .append(user);
            return new File(sb.toString());
        }
    }

}

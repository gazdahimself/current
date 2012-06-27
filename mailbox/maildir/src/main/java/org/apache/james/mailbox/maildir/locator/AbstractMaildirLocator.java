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
import java.util.ArrayList;
import java.util.List;

import org.apache.james.mailbox.name.DefaultMailboxName;
import org.apache.james.mailbox.name.DefaultUnresolvedMailboxName;
import org.apache.james.mailbox.name.MailboxNameBuilder;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;

/**
 * TODO AbstractMaildirLocator.
 */
public abstract class AbstractMaildirLocator implements ChainedMaildirLocator {
    public static File getRelativePath(File root, File maildir) {
        String maildirPath = maildir.getAbsolutePath();
        int prefixLength = root.getAbsolutePath().length();
        int separatorPosition = maildirPath.indexOf(File.separatorChar, prefixLength + 1);
        if (separatorPosition >= 0) {
            return new File(maildirPath.substring(0, separatorPosition));
        } else {
            return maildir;
        }
    }
    public static boolean isDescendant(File parent, File descendant) {
        String descPath = descendant.getAbsolutePath();
        String parentPath = parent.getAbsolutePath() + File.separatorChar;
        return descPath.startsWith(parentPath);
    }
    protected final MailboxNameCodec mailboxNameCodec;
    protected final ChainedMaildirLocator next;
    public AbstractMaildirLocator(ChainedMaildirLocator next) {
        super();
        this.next = next;

        this.mailboxNameCodec = MailboxNameCodec.MAILDIR_NAME_CODEC;

    }
    
    /**
     * TODO checkHome.
     * 
     * @param result
     * @return
     */
    protected boolean directoryExists(File result) {
        return result.exists() && result.isDirectory();
    }

    public File getInbox(MailboxOwner owner) {
        if (!accepts(owner)) {
            if (next != null) {
                return next.getInbox(owner);
            }
            else {
                throw new IllegalArgumentException(getClass().getName() +" cannot get inbox out for the unsupported "+ owner +".");
            }
        }

        return getMaildir(getHome(owner), owner.isGroup());
    }
    protected abstract File getHome(MailboxOwner owner);

    /**
     * TODO getMaildir.
     *
     * @param home
     * @param isGroup
     * @return
     */
    protected abstract File getMaildir(File home, boolean isGroup);

    @Override
    public boolean isSubMaildir(File inboxPath, File subMaildir) {
        if (!accepts(inboxPath)) {
            if (next != null) {
                return next.isSubMaildir(inboxPath, subMaildir);
            }
            else {
                throw new IllegalArgumentException(getClass().getName() +" cannot perform isSubMaildir() for for the unsupported inbox path '"+ inboxPath +"'.");
            }
        }

        return subMaildir.getParentFile().equals(inboxPath) && subMaildir.getName().charAt(0) == mailboxNameCodec.getDelimiter();
    }
    
    @Override
    public File locate(MailboxNameResolver mailboxNameResolver, MailboxName mailboxName, MailboxOwner owner) {
        if (!accepts(mailboxNameResolver, mailboxName)) {
            if (next != null) {
                return next.locate(mailboxNameResolver, mailboxName, owner);
            }
            else {
                throw new IllegalArgumentException(getClass().getName() +" cannot locate the unsupported mailbox name '"+ mailboxName +"'.");
            }
        }
        File inboxPath = getInbox(owner);
        MailboxName homeMailboxName = mailboxNameResolver.getInbox(owner);
        int suffixLength = mailboxName.getSegmentCount() - homeMailboxName.getSegmentCount();
        if (suffixLength == 0) {
            /* nothing to append */
            return inboxPath;
        } else {
            return new File(inboxPath, mailboxNameCodec.encode(mailboxName.suffix(suffixLength)));
        }
    }
    @Override
    public ChainedMaildirLocator next() {
        return next;
    }
    @Override
    public MailboxName toMailboxName(MailboxName inbox, File inboxPath, MailboxOwner owner, File maildir) {
        if (!accepts(inboxPath)) {
            if (next != null) {
                return next.toMailboxName(inbox, inboxPath, owner, maildir);
            }
            else {
                throw new IllegalArgumentException(getClass().getName() +" cannot perform toMailboxName() for the unsupported inbox path '"+ inboxPath +"'.");
            }
        }
        
        String absMaildir = maildir.getAbsolutePath();
        if (!maildir.getParentFile().equals(inboxPath)) {
            throw new IllegalArgumentException("'" + maildir.getPath() + "' not a direct descendant of '" + inboxPath.getPath() + "'.");
        }

        
        List<String> segments = new ArrayList<String>(DefaultUnresolvedMailboxName.estimateSegmentsCount(absMaildir));

        while (maildir != null && !maildir.equals(inboxPath)) {
            segments.add(maildir.getName());
            maildir = maildir.getParentFile();
        }
        if (segments.size() == 0) {
            return inbox;
        } else {
            MailboxNameBuilder mnb = new MailboxNameBuilder(inbox.getSegmentCount() + segments.size());
            mnb.addAll(inbox);

            for (int i = segments.size() - 1; i >= 0; i--) {
                mnb.add(segments.get(i));
            }

            return new DefaultMailboxName(segments, true);
        }

    }
    
}

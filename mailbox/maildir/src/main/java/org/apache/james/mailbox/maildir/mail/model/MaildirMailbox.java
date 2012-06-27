package org.apache.james.mailbox.maildir.mail.model;

import java.io.IOException;

import org.apache.james.mailbox.acl.MailboxACL;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;

public class MaildirMailbox<Id> extends SimpleMailbox<Id> {

    private final MaildirFolder folder;

    public MaildirMailbox(MaildirMailbox<Id> maildirMailbox) throws IOException {
        this(maildirMailbox.getMailboxName(), maildirMailbox.getUser(), maildirMailbox.isOwnerGroup(), maildirMailbox.getMaildirFolder());
    }
    
    public MaildirMailbox(MailboxName mailboxName, String user, boolean ownerGroup, MaildirFolder folder) throws IOException {
        super(mailboxName, user, ownerGroup, folder.getUidValidity());
        this.folder = folder;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox#getACL()
     */
    @Override
    public MailboxACL getACL() {
        try {
            return folder.getACL();
        } catch (MailboxException e) {
            throw new RuntimeException(e);
        }
    }

    public MaildirFolder getMaildirFolder() {
        return folder;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox#setACL(org.apache.james.mailbox.acl.MailboxACL)
     */
    @Override
    public void setACL(MailboxACL acl) {
        try {
            folder.setACL(acl);
        } catch (MailboxException e) {
            throw new RuntimeException(e);
        }
    }

}

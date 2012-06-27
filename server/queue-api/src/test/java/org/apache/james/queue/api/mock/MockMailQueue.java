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
package org.apache.james.queue.api.mock;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.james.queue.api.MailQueue;
import org.apache.mailet.Mail;

public class MockMailQueue implements MailQueue {

    private final LinkedBlockingQueue<Mail> queue = new LinkedBlockingQueue<Mail>();
    private boolean throwException;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Mail lastMail;

    /**
     * Throw an {@link MailQueueException} on next operation
     */
    public void throwExceptionOnNextOperation() {
        this.throwException = true;
    }

    @Override
    public MailQueueItem deQueue() throws MailQueueException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }
        try {
            final Mail mail = queue.take();
            if (queue.isEmpty()) {
                lastMail = null;
            }
            return new MailQueueItem() {

                @Override
                public Mail getMail() {
                    return mail;
                }

                @Override
                public void done(boolean success) throws MailQueueException {
                    // do nothing here
                }
            };

        } catch (InterruptedException e) {
            throw new MailQueueException("Mock", e);
        }
    }

    @Override
    public void enQueue(final Mail mail, long delay, TimeUnit unit) throws MailQueueException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    queue.put(mail);
                    lastMail = mail;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, delay, unit);
    }

    @Override
    public void enQueue(Mail mail) throws MailQueueException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }
        try {
            queue.put(mail);
            lastMail = mail;
        } catch (InterruptedException e) {
            throw new MailQueueException("Mock", e);
        }
    }

    public Mail getLastMail() {
        return lastMail;
    }

    public void clear() {
        queue.clear();
    }
}

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
package org.apache.james.protocols.pop3.mailbox;

/**
 * Hold meta data for a message
 */
public final class MessageMetaData {

    private final long uid;
    private final long size;

    public MessageMetaData(long uid, long size) {
        this.uid = uid;
        this.size = size;
    }

    /**
     * Return the uid of the message
     * 
     * @return uid
     */
    public long getUid() {
        return uid;
    }

    /**
     * Return the size of a message
     * 
     * @return size
     */
    public long getSize() {
        return size;
    }
}

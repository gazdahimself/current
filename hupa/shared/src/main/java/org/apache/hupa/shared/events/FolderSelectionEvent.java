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

package org.apache.hupa.shared.events;

import org.apache.hupa.shared.data.IMAPFolder;
import org.apache.hupa.shared.data.User;

import com.google.gwt.event.shared.GwtEvent;

public class FolderSelectionEvent extends GwtEvent<FolderSelectionEventHandler>{
    
    private User user;
    private IMAPFolder folder;
    public final static Type<FolderSelectionEventHandler> TYPE = new Type<FolderSelectionEventHandler>();
    public FolderSelectionEvent(User user, IMAPFolder folder) {
        this.user =user;
        this.folder = folder;
    }
    
    public IMAPFolder getFolder() {
        return folder;
    }
    
    public User getUser() {
        return user;
    }

    @Override
    protected void dispatch(FolderSelectionEventHandler handler) {
        handler.onFolderSelectionEvent(this);
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<FolderSelectionEventHandler> getAssociatedType() {
        return TYPE;
    }

}

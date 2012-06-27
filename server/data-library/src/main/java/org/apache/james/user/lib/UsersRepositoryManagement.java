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

package org.apache.james.user.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.UsersRepositoryManagementMBean;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;
import org.apache.james.user.api.model.User;

public class UsersRepositoryManagement extends StandardMBean implements UsersRepositoryManagementMBean {

    /**
     * The administered UsersRepository
     */
    private UsersRepository localUsers;

    @Resource(name = "usersrepository")
    public void setUsersRepository(UsersRepository localUsers) {
        this.localUsers = localUsers;
    }

    public UsersRepositoryManagement() throws NotCompliantMBeanException {
        super(UsersRepositoryManagementMBean.class);
    }

    private JamesUser getJamesUser(String userName) throws UsersRepositoryException {
        User baseuser = localUsers.getUserByName(userName);
        if (baseuser == null)
            throw new IllegalArgumentException("user not found: " + userName);
        if (!(baseuser instanceof JamesUser))
            throw new IllegalArgumentException("user is not of type JamesUser: " + userName);

        return (JamesUser) baseuser;
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#addUser(java.lang.String, java.lang.String)
     */
    public void addUser(String userName, String password) throws Exception {
        try {
            localUsers.addUser(userName, password);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#deleteUser(java.lang.String)
     */
    public void deleteUser(String userName) throws Exception {
        try {
            localUsers.removeUser(userName);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#verifyExists
     * (java.lang.String)
     */
    public boolean verifyExists(String userName) throws Exception {
        try {
            return localUsers.contains(userName);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#countUsers()
     */
    public long countUsers() throws Exception {
        try {
            return localUsers.countUsers();
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#listAllUsers()
     */
    public String[] listAllUsers() throws Exception {
        List<String> userNames = new ArrayList<String>();
        try {
            for (Iterator<String> it = localUsers.list(); it.hasNext();) {
                userNames.add(it.next());
            }
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }
        return (String[]) userNames.toArray(new String[] {});
    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#setPassword
     * (java.lang.String, java.lang.String)
     */
    public void setPassword(String userName, String password) throws Exception {
        try {
            User user = localUsers.getUserByName(userName);
            if (user == null)
                throw new UsersRepositoryException("user not found: " + userName);
            if (user.setPassword(password) == false) {
                throw new UsersRepositoryException("Unable to update password for user " + user);
            }
            localUsers.updateUser(user);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }

    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#unsetAlias(java.lang.String)
     */
    public void unsetAlias(String userName) throws Exception {
        try {
            JamesUser user = getJamesUser(userName);
            if (!user.getAliasing())
                throw new UsersRepositoryException("User " + user + " is no alias");

            user.setAliasing(false);
            localUsers.updateUser(user);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }

    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#getAlias(java.lang.String)
     */
    public String getAlias(String userName) throws Exception {
        try {
            JamesUser user = getJamesUser(userName);
            if (!user.getAliasing())
                return null;
            return user.getAlias();
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }

    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#unsetForwardAddress
     * (java.lang.String)
     */
    public void unsetForwardAddress(String userName) throws Exception {
        try {
            JamesUser user = getJamesUser(userName);
            if (!user.getForwarding())
                throw new UsersRepositoryException("User " + user + " is no forward");

            user.setForwarding(false);
            localUsers.updateUser(user);
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }

    }

    /**
     * @see
     * org.apache.james.user.api.UsersRepositoryManagementMBean#getForwardAddress
     * (java.lang.String)
     */
    public String getForwardAddress(String userName) throws Exception {
        try {
            JamesUser user = getJamesUser(userName);
            if (!user.getForwarding())
                return null;
            return user.getForwardingDestination().toString();
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }

    }

    /**
     * @see org.apache.james.user.api.UsersRepositoryManagementMBean#getVirtualHostingEnabled()
     */
    public boolean getVirtualHostingEnabled() throws Exception {
        try {
            return localUsers.supportVirtualHosting();
        } catch (UsersRepositoryException e) {
            throw new Exception(e.getMessage());

        }
    }

}

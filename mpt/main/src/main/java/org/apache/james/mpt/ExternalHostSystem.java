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

package org.apache.james.mpt;


/**
 * <p>Connects to a host system serving on an open port.</p>
 * <p>
 * This is typically used for functional integration testing of a complete
 * server system (including sockets). Apache James MPT AntLib provides an
 * <a href='http://ant.apache.org' rel='tag'>Ant</a> task suitable for this
 * use case.
 * </p>
 */
public class ExternalHostSystem extends ExternalSessionFactory implements HostSystem {
    
    private final UserAdder userAdder;

    /**
     * Constructs a host system suitable for connection to an open port.
     * @param host host name that will be connected to, not null
     * @param port port on host that will be connected to, not null
     * @param monitor monitors the conduct of the connection
     * @param shabang protocol shabang will be sent to the script test in the place of the
     * first line received from the server. Many protocols pass server specific information
     * in the first line. When not null, this line will be replaced.
     * Or null when the first line should be passed without replacement
     * @param userAdder null when test system has appropriate users already set
     */
    public ExternalHostSystem(final String host, final int port,
            final Monitor monitor, final String shabang, final UserAdder userAdder) {
        super(host, port, monitor, shabang);
        this.userAdder = userAdder;
    }

    public void addUser(String user, String password) throws Exception {
        if (userAdder == null) {
            monitor.note("Please ensure user '" + user + "' with password '"
                + password + "' exists.");
        } else {
            userAdder.addUser(user, password);
        }
    }
}

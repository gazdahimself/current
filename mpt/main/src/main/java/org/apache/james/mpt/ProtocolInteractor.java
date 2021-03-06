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

import java.util.List;

/**
 * Scripts a protocol interaction.
 */
public interface ProtocolInteractor {

    /**
     * adds a new Client request line to the test elements
     */
    public abstract void CL(String clientLine);

    /**
     * adds a new Server Response line to the test elements, with the specified
     * location.
     */
    public abstract void SL(String serverLine, String location);

    /**
     * adds a new Server Unordered Block to the test elements.
     */
    public abstract void SUB(List<String> serverLines, String location);

    /**
     * adds a new Client request line to the test elements
     */
    public abstract void CL(int sessionNumber, String clientLine);

    /**
     * Adds a continuation. To allow one thread to be used for testing.
     */
    public abstract void CONT(int sessionNumber) throws Exception;

    /**
     * adds a new Server Response line to the test elements, with the specified
     * location.
     */
    public abstract void SL(int sessionNumber, String serverLine,
            String location, String lastClientMessage);

    /**
     * adds a new Server Unordered Block to the test elements.
     */
    public abstract void SUB(int sessionNumber, List<String> serverLines,
            String location, String lastClientMessage);

}
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
package org.apache.james.cli.type;

/**
 * Enumeration of valid command types.
 */
public enum CmdType {
    ADDUSER("adduser", 3), 
    REMOVEUSER("removeuser", 2), 
    LISTUSERS("listusers", 1), 
    ADDDOMAIN("adddomain", 2), 
    REMOVEDOMAIN("removedomain", 2), 
    CONTAINSDOMAIN("containsdomain", 2), 
    LISTDOMAINS("listdomains", 1),
    LISTMAPPINGS("listmappings", 1),
    LISTUSERDOMAINMAPPINGS("listuserdomainmappings", 3),
    ADDADDRESSMAPPING("addaddressmapping", 4),
    REMOVEADDRESSMAPPING("removeaddressmapping", 4),
    ADDREGEXMAPPING("addregexmapping", 4),
    REMOVEREGEXMAPPING("removeregexmapping", 4),
    SETPASSWORD("setpassword", 3);
    private String command;
    private int arguments;

    private CmdType(String command, int arguments) {
        this.command = command;
        this.arguments = arguments;
    }

    /**
     * Validate that the number of arguments match the passed value.
     * 
     * @param arguments
     *            The number of argument to compare.
     * @return true if values match, false otherwise.
     */
    public boolean hasCorrectArguments(int arguments) {
        if (this.arguments == arguments)
            return true;

        return false;
    }

    /**
     * Return a CmdType enumeration that matches the passed command.
     * 
     * @param command
     *            The command to use for lookup.
     * @return the CmdType enumeration that matches the passed command, or null
     *         if not found.
     */
    public static CmdType lookup(String command) {
        if (command != null) {
            for (CmdType cmd : values())
                if (cmd.getCommand().equalsIgnoreCase(command))
                    return cmd;
        }
        return null;
    }
    
    /**
     * Return the value of command.
     * 
     * @return the value of command.
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * Return the value of arguments.
     * 
     * @return the value of arguments.
     */
    public int getArguments() {
        return this.arguments;
    }
}

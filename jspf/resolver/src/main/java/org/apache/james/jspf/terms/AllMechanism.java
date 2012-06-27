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


package org.apache.james.jspf.terms;

import org.apache.james.jspf.core.DNSLookupContinuation;
import org.apache.james.jspf.core.SPFSession;
import org.apache.james.jspf.core.exceptions.PermErrorException;

/**
 * This class represent the all mechanism
 * 
 */
public class AllMechanism implements Mechanism, ConfigurationEnabled {

    public static final String REGEX = "[aA][lL][lL]";

    /**
     * @see org.apache.james.jspf.core.SPFChecker#checkSPF(org.apache.james.jspf.core.SPFSession)
     */
    public DNSLookupContinuation checkSPF(SPFSession spfData) throws PermErrorException {
        return null;
    }

    /**
     * @see org.apache.james.jspf.terms.ConfigurationEnabled#config(Configuration)
     */
    public void config(Configuration params) throws PermErrorException {
        // no checks needed
        // the regex only passes with no parameters
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "all";
    }
    
}

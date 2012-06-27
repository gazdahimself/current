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

package org.apache.james.jspf.policies;

import org.apache.james.jspf.core.DNSLookupContinuation;
import org.apache.james.jspf.core.SPF1Constants;
import org.apache.james.jspf.core.SPFSession;
import org.apache.james.jspf.core.SPF1Record;
import org.apache.james.jspf.core.exceptions.NeutralException;
import org.apache.james.jspf.core.exceptions.NoneException;
import org.apache.james.jspf.core.exceptions.PermErrorException;
import org.apache.james.jspf.core.exceptions.TempErrorException;
import org.apache.james.jspf.terms.Modifier;

/**
 * Sets the result to NEUTRAL if no directive is found 
 */
public class NeutralIfNotMatchPolicy implements PolicyPostFilter {
    
    private final class NeutralIfNotMatchModifier implements Modifier {
        
        /**
         * @see org.apache.james.jspf.core.SPFChecker#checkSPF(org.apache.james.jspf.core.SPFSession)
         */
        public DNSLookupContinuation checkSPF(SPFSession spfData) throws PermErrorException, TempErrorException, NeutralException {
            // If no match was found set the result to neutral
            if (spfData.getCurrentResult() == null) {
                spfData.setCurrentResult(SPF1Constants.NEUTRAL);
            }
            return null;
        }

        public String toString() {
            return "defaultresult";
        }

        /**
         * (non-Javadoc)
         * @see org.apache.james.jspf.terms.Modifier#enforceSingleInstance()
         */
		public boolean enforceSingleInstance() {
			return false;
		}
    }

    /**
     * @see org.apache.james.jspf.policies.PolicyPostFilter#getSPFRecord(java.lang.String, org.apache.james.jspf.core.SPF1Record)
     */
    public SPF1Record getSPFRecord(String currentDomain, SPF1Record spfRecord) throws PermErrorException, TempErrorException, NoneException, NeutralException {
        if (spfRecord == null) return null;
        // Set the result to NEUTRAL if at least a directive is present and it didn't match
        // Maybe we should simply append a "?all" at the end, as modifier
        if (spfRecord.getDirectives().size() > 0) {
            spfRecord.getModifiers().add(new NeutralIfNotMatchModifier());
        }
        return spfRecord;
    }
}
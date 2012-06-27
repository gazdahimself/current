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
import org.apache.james.jspf.core.DNSRequest;
import org.apache.james.jspf.core.DNSResponse;
import org.apache.james.jspf.core.SPF1Record;
import org.apache.james.jspf.core.SPF1Utils;
import org.apache.james.jspf.core.SPFCheckerDNSResponseListener;
import org.apache.james.jspf.core.SPFSession;
import org.apache.james.jspf.core.exceptions.NeutralException;
import org.apache.james.jspf.core.exceptions.NoneException;
import org.apache.james.jspf.core.exceptions.PermErrorException;
import org.apache.james.jspf.core.exceptions.TempErrorException;
import org.apache.james.jspf.core.exceptions.TimeoutException;

import java.util.List;

/**
 * Get the raw dns txt or spf entry which contains a spf entry. If a domain
 * publish both, and both are not equals it throws a PermError
 */
public class SPFStrictCheckerRetriever extends SPFRetriever {


    private static final String ATTRIBUTE_SPFSTRICT_CHECK_SPFRECORDS = "SPFStrictCheck.SPFRecords";
    
    private static final class SPFStrictSPFRecordsDNSResponseListener implements SPFCheckerDNSResponseListener {

        /**
         * @see org.apache.james.jspf.core.SPFCheckerDNSResponseListener#onDNSResponse(org.apache.james.jspf.core.DNSResponse, org.apache.james.jspf.core.SPFSession)
         */
        @SuppressWarnings("unchecked")
		public DNSLookupContinuation onDNSResponse(
                DNSResponse response, SPFSession session)
                throws PermErrorException,
                NoneException, TempErrorException,
                NeutralException {
            
            List<String> spfR = (List<String>) session.getAttribute(ATTRIBUTE_SPFSTRICT_CHECK_SPFRECORDS);
            List<String> spfTxtR = null;
            try {
                spfTxtR = response.getResponse();
            } catch (TimeoutException e) {
                throw new TempErrorException("Timeout querying dns");
            }

            String record = calculateSpfRecord(spfR, spfTxtR);
            if (record != null) {
                session.setAttribute(SPF1Utils.ATTRIBUTE_SPF1_RECORD, new SPF1Record(record));
            }

            return null;
            
        }
        
    }
    
    
    private static final class SPFStrictCheckDNSResponseListener implements SPFCheckerDNSResponseListener {

        /**
         * @see org.apache.james.jspf.core.SPFCheckerDNSResponseListener#onDNSResponse(org.apache.james.jspf.core.DNSResponse, org.apache.james.jspf.core.SPFSession)
         */
        public DNSLookupContinuation onDNSResponse(
                DNSResponse response, SPFSession session)
                throws PermErrorException, NoneException,
                TempErrorException, NeutralException {
            try {
                List<String> spfR = response.getResponse();
                
                session.setAttribute(ATTRIBUTE_SPFSTRICT_CHECK_SPFRECORDS, spfR);
                
                String currentDomain = session.getCurrentDomain();
                return new DNSLookupContinuation(new DNSRequest(currentDomain, DNSRequest.TXT), new SPFStrictSPFRecordsDNSResponseListener());
                    
            } catch (TimeoutException e) {
                throw new TempErrorException("Timeout querying dns");
            }
        }
        
        
    }


    /**
     * @see org.apache.james.jspf.policies.SPFRetriever#checkSPF(org.apache.james.jspf.core.SPFSession)
     */
    public DNSLookupContinuation checkSPF(SPFSession spfData)
            throws PermErrorException, TempErrorException, NeutralException,
            NoneException {
        SPF1Record res = (SPF1Record) spfData.getAttribute(SPF1Utils.ATTRIBUTE_SPF1_RECORD);
        if (res == null) {
            String currentDomain = spfData.getCurrentDomain();

            return new DNSLookupContinuation(new DNSRequest(currentDomain, DNSRequest.SPF), new SPFStrictCheckDNSResponseListener());
            
        }
        return null;
    }


    private static String calculateSpfRecord(List<String> spfR, List<String> spfTxtR)
            throws PermErrorException {
        String spfR1 = null;
        String spfR2 = null;
        if (spfR != null) spfR1 = extractSPFRecord(spfR);
        if (spfTxtR != null) spfR2 = extractSPFRecord(spfTxtR);
        
        if (spfR1 != null && spfR2 == null) {
            return spfR1;
        } else if (spfR1 == null && spfR2 != null) {
            return spfR2;
        } else if (spfR1 != null && spfR2 != null) {
            if (spfR1.toLowerCase().equals(spfR2.toLowerCase()) == false) {
                throw new PermErrorException("Published SPF records not equals");
            } else {
                return spfR1;
            }
        } else {
            return null;
        }
    }
}
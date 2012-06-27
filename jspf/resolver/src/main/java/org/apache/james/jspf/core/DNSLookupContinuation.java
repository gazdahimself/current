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

package org.apache.james.jspf.core;



/**
 * This object is used as the return value for spf resolving tasks.
 * Every time a DNS resolution is needed the task should simply return
 * this one including the DNSRequest and a listener to be invoked
 * when the answer will be available.
 */
public class DNSLookupContinuation {
    
    private DNSRequest request;
    private SPFCheckerDNSResponseListener listener;

    public DNSLookupContinuation(DNSRequest request, SPFCheckerDNSResponseListener listener) {
        this.request = request;
        this.listener = listener;
    }

    /**
     * Return the DNSRequest which was used
     * 
     * @return request
     */
    public DNSRequest getRequest() {
        return request;
    }

    /**
     * Return the SPFCheckerDNSResponseListener which should called for the DNSRequest
     * 
     * @return listener
     */
    public SPFCheckerDNSResponseListener getListener() {
        return listener;
    }

    
}

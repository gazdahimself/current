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

package org.apache.jsieve.exception;

/**
 * Class <code>SieveException</code> indicates an exceptional condition
 * encountered within Sieve.
 */
@SuppressWarnings("serial")
public class SieveException extends Exception {

    /**
     * Constructor for SieveException.
     */
    public SieveException() {
        super();
    }

    /**
     * Constructor for SieveException.
     * 
     * @param message
     */
    public SieveException(String message) {
        super(message);
    }

    /**
     * Constructor for SieveException.
     * 
     * @param message
     * @param cause
     */
    public SieveException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for SieveException.
     * 
     * @param cause
     */
    public SieveException(Throwable cause) {
        super(cause);
    }

}

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

package org.apache.james.jspf.executor;


import java.util.List;

/**
 * Implementation of an IRespone Object
 *
 */
public class IResponseImpl implements IResponse {
    private Exception exception = null;
    private List<String> value = null;
    private Object id = null;
    
    public IResponseImpl(Object id, Exception e) {
        this.exception = e;
        this.id = id;
    }
    
    public IResponseImpl(Object id, List<String> result) {
        this.value = result;
        this.id = id;
    }
    
    /**
     * @see org.apache.james.jspf.executor.IResponse#getException()
     */
    public Exception getException() {
        return exception;
    }
    
    /**
     * @see org.apache.james.jspf.executor.IResponse#getId()
     */
    public Object getId() {
        return id;
    }
    
    /**
     * @see org.apache.james.jspf.executor.IResponse#getValue()
     */
    public List<String> getValue() {
        return value;
    }
}
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

package org.apache.hupa.shared.data;

import java.io.Serializable;

/**
 * Attachment of a message
 * 
 *
 */
public class MessageAttachment implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6896197131104882424L;
    private String cType;
    private int size;
    private String name;

    /**
     * Set the name of the attachment
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
        
    }

    /**
     * Return the name of the attachment
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the content-type of the attachment
     * 
     * @param cType
     */
    public void setContentType(String cType) {
        this.cType = cType;
    }
    
    /**
     * Return the content-type of the attachment
     * 
     * @return cType
     */
    public String getContentType() {
        return cType;
    }

    /**
     * Set the content size in bytes
     * 
     * @param size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Return the content size in bytes
     * 
     * @return size
     */
    public int getSize() {
        return size;
    }
}

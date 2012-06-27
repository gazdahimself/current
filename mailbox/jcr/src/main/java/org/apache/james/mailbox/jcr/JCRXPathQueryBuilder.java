/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;

/**
 * TODO JCRXPathQueryBuilder.
 */
public class JCRXPathQueryBuilder {
    
    private final StringBuilder buffer;
    
    public JCRXPathQueryBuilder(int initialBufferCapacity) {
        super();
        this.buffer = new StringBuilder(initialBufferCapacity);
    }

    public JCRXPathQueryBuilder and() {
        buffer.append(" AND ");
        return this;
    }
    
    /**
     * TODO bind.
     *
     * @param workspace
     * @return
     * @throws RepositoryException 
     */
    @SuppressWarnings("deprecation")
    public Query bind(Workspace workspace) throws RepositoryException {
        QueryManager manager = workspace.getQueryManager();
        return manager.createQuery(this.toString(), Query.XPATH);
    }
    public JCRXPathQueryBuilder delimiter() {
        buffer.append(JCRImapConstants.NODE_DELIMITER_CHAR);
        return this;
    }
    public JCRXPathQueryBuilder descendantOrSelf() {
        return delimiter().delimiter();
    }
    
    public JCRXPathQueryBuilder hasMixin(String type) {
        buffer.append("element(*,").append(type).append(")");
        return this;
    }

    public JCRXPathQueryBuilder eq(String attribute, boolean value) {
        buffer.append("[@").append(attribute).append(" = ");
        buffer.append(String.valueOf(value));
        buffer.append("]");
        return this;
    }
    
    public JCRXPathQueryBuilder eq(String attribute, String value) {
        buffer.append("[@").append(attribute).append(" = ");
        literal(value);
        buffer.append("]");
        return this;
    }
    

    /**
     * TODO escapeName.
     *
     * @param nodeName
     * @return 
     */
    public JCRXPathQueryBuilder escapeName(String nodeName) {
        // FIXME prehaps ISO9075.encodePath(nodeName) is a better choice?
        buffer.append(ISO9075.encodePath(nodeName));
        return this;
    }
    
    public JCRXPathQueryBuilder jcrRoot() {
      delimiter();
      buffer.append(JCRImapConstants.JCR_ROOT);
      return this;
  }
    
    /**
     * TODO like.
     *
     * @param mailboxNameProp
     * @param encodedName
     * @return 
     */
    public JCRXPathQueryBuilder like(String attribute, String value) {
        buffer.append("jcr:like(@").append(attribute).append(", ");
        literal(value);
        buffer.append(")");
        return this;
    }

    public JCRXPathQueryBuilder literal(String literal) {
        buffer.append('\'').append(Text.encodeIllegalXMLCharacters(literal)).append('\'');
        return this;
    }

    public JCRXPathQueryBuilder mailboxes() {
        delimiter();
        buffer.append(JCRImapConstants.MAILBOXES_PATH);
        return this;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

}

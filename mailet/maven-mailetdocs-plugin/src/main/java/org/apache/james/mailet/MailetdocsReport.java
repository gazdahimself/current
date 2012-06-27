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

package org.apache.james.mailet;

import java.util.List;

import org.apache.maven.project.MavenProject;


/**
 * <p>Generates catalog and reports on mailets and matchers.</p>
 * <h4>Notes</h4>
 * <ul>
 * <li>Should only used as a report.</li>
 * <li>Mailets are instantiated during report production. </li>
 * </ul>
 * @goal mailetdocs
 * @since 0.1
 * @requiresDependencyResolution compile
 */
public class MailetdocsReport extends AbstractMailetdocsReport {

    /**
     * Builds descriptors for the given project only,
     * without recursion.
     * @param project not null
     */
    protected List<MailetMatcherDescriptor> buildDescriptors(final MavenProject project) {
        logProject(project);
        final List<MailetMatcherDescriptor> descriptors = new DefaultDescriptorsExtractor().extract(project, getLog()).descriptors();
        return descriptors;
    }

    private void logProject(final MavenProject project) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Building descriptors for " + project.getName());
        }
    }
    
}
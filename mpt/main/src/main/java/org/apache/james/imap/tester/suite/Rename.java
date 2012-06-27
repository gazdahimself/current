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

package org.apache.james.imap.tester.suite;

import java.util.Locale;

import org.apache.james.imap.tester.FrameworkForSelectedStateBase;
import org.apache.james.imap.tester.base.HostSystem;
import org.junit.Test;

public abstract class Rename extends FrameworkForSelectedStateBase {

    public Rename(HostSystem system) throws Exception {
        super(system);
    }

    @Test
    public void testRenameUS() throws Exception {
        scriptTest("Rename", Locale.US);
    }
    
    @Test
    public void testRenameKOREA() throws Exception {
        scriptTest("Rename", Locale.KOREA);
    }

    @Test
    public void testRenameITALY() throws Exception {
        scriptTest("Rename", Locale.ITALY);
    }

    @Test
    public void testRenameHierarchyUS() throws Exception {
        scriptTest("RenameHierarchy", Locale.US);
    }

    @Test
    public void testRenameHierarchyKO() throws Exception {
        scriptTest("RenameHierarchy", Locale.KOREA);
    }

    @Test
    public void testRenameHierarchyIT() throws Exception {
        scriptTest("RenameHierarchy", Locale.ITALY);
    }

    @Test
    public void testRenameSelectedUS() throws Exception {
        scriptTest("RenameSelected", Locale.US);
    }

    @Test
    public void testRenameSelectedIT() throws Exception {
        scriptTest("RenameSelected", Locale.ITALY);
    }

    @Test
    public void testRenameSelectedKO() throws Exception {
        scriptTest("RenameSelected", Locale.KOREA);
    }
}

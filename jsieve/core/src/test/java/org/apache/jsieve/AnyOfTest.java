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

package org.apache.jsieve;

import junit.framework.TestCase;

import org.apache.jsieve.commands.ThrowTestException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.parser.generated.ParseException;
import org.apache.jsieve.utils.JUnitUtils;

/**
 * Class AnyOfTest
 */
public class AnyOfTest extends TestCase {

    /**
     * Test for Test 'anyof'
     */
    public void testIfAnyOfFalseTrue() {
        boolean isTestPassed = false;
        String script = "if anyof (false, true) {throwTestException;}";

        try {
            JUnitUtils.interpret(JUnitUtils.createMail(), script);
        } catch (ThrowTestException.TestException e) {
            isTestPassed = true;
        } catch (ParseException e) {
        } catch (SieveException e) {
        }
        assertTrue(isTestPassed);
    }

    /**
     * Test for Test 'anyof'
     */
    public void testIfAnyOfTrueTrue() {
        boolean isTestPassed = false;
        String script = "if anyof (true, true) {throwTestException;}";

        try {
            JUnitUtils.interpret(JUnitUtils.createMail(), script);
        } catch (ThrowTestException.TestException e) {
            isTestPassed = true;
        } catch (ParseException e) {
        } catch (SieveException e) {
        }
        assertTrue(isTestPassed);
    }

    /**
     * Test for Test 'anyof'
     */
    public void testIfAnyOfFalseFalse() {
        boolean isTestPassed = false;
        String script = "if anyof (false, false) {stop;} throwTestException;";

        try {
            JUnitUtils.interpret(JUnitUtils.createMail(), script);
        } catch (ThrowTestException.TestException e) {
            isTestPassed = true;
        } catch (ParseException e) {
        } catch (SieveException e) {
        }
        assertTrue(isTestPassed);
    }

    /**
     * Test for Test 'anyof'
     */
    public void testIfAnyOfTrueFalse() {
        boolean isTestPassed = false;
        String script = "if anyof (true, false) {throwTestException;}";

        try {
            JUnitUtils.interpret(JUnitUtils.createMail(), script);
        } catch (ThrowTestException.TestException e) {
            isTestPassed = true;
        } catch (ParseException e) {
        } catch (SieveException e) {
        }
        assertTrue(isTestPassed);
    }

    /**
     * Test for Test 'anyof' with invalid argument
     */
    public void testInvalidArgument() {
        boolean isTestPassed = false;
        String script = "if anyof 1 {throwTestException;}";

        try {
            JUnitUtils.interpret(JUnitUtils.createMail(), script);
        } catch (SyntaxException e) {
            isTestPassed = true;
        } catch (ParseException e) {
        } catch (SieveException e) {
        }
        assertTrue(isTestPassed);
    }

}

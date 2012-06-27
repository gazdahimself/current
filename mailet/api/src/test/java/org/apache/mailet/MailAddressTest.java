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

package org.apache.mailet;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import junit.framework.TestCase;


/**
 * Tests for Test method for {@link org.apache.mailet.MailAddress}.
 */
public class MailAddressTest extends TestCase {
    private static final String GOOD_LOCAL_PART = "\"quoted@local part\"";
    private static final String GOOD_QUOTED_LOCAL_PART = "\"quoted@local part\"@james.apache.org";
    private static final String BAD_LOCAL_PART_1 = "quoted local-part@james.apache.org";
    private static final String BAD_LOCAL_PART_2 = "quoted@local-part@james.apache.org";
    private static final String BAD_LOCAL_PART_3 = "local-part.@james.apache.org";
    private static final String GOOD_ADDRESS = "server-dev@james.apache.org";
    private static final String GOOD_DOMAIN = "james.apache.org";
    
    private static final String GOOD_DLIT = "server-dev@[127.0.0.1]";
    private static final String BAD_DLIT_1 = "server-dev@[300.0.0.1]";
    private static final String BAD_DLIT_2 = "server-dev@[127.0.1]";

    /**
     * Test method for {@link org.apache.mailet.MailAddress#hashCode()}.
     * @throws AddressException 
     */
    public void testHashCode() throws AddressException {

        MailAddress a = new MailAddress(GOOD_ADDRESS);
        MailAddress b = new MailAddress(GOOD_ADDRESS);
        assertTrue(a.hashCode()+" != "+ b.hashCode(),a.hashCode() == b.hashCode());
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#MailAddress(java.lang.String)}.
     * @throws AddressException 
     */
    public void testMailAddressString() throws AddressException {

        MailAddress a = new MailAddress(GOOD_ADDRESS);
        assertTrue(GOOD_ADDRESS.equals(a.toString()));
        try{
            a = new MailAddress(GOOD_QUOTED_LOCAL_PART);
        }catch (AddressException e){
            assertTrue(e.getMessage(), false);
        }
        try{
            a = new MailAddress(GOOD_DLIT);
        }catch (AddressException e){
            assertTrue(e.getMessage(), false);
        }
        try{
            a = new MailAddress(BAD_LOCAL_PART_1);
            assertFalse(BAD_LOCAL_PART_1,true);
        }catch (AddressException e){
            assertTrue(true);
        }
        
        try{
            a = new MailAddress(BAD_LOCAL_PART_2);
            assertFalse(BAD_LOCAL_PART_2,true);
        }catch (AddressException e){
             assertTrue(true);
        }
        
        try{
            a = new MailAddress(BAD_LOCAL_PART_3);
            assertFalse(BAD_LOCAL_PART_3,true);
        }catch (AddressException e){
            assertTrue(true);
        }
        try{
            a = new MailAddress(BAD_DLIT_1);
            assertFalse(BAD_DLIT_1,true);
        }catch (AddressException e){
            assertTrue(true);
        }
        try{
            a = new MailAddress(BAD_DLIT_2);
            assertFalse(BAD_DLIT_2,true);
        }catch (AddressException e){
            assertTrue(true);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#MailAddress(java.lang.String, java.lang.String)}.
     */
    public void testMailAddressStringString() {

        try{
            new MailAddress("local-part", "domain");
        }catch (AddressException e){
            assertTrue(e.getMessage(), false);
        }
        try{
            MailAddress a = new MailAddress("local-part", "-domain");
            assertFalse(a.toString(),true);
        }catch (AddressException e){
            assertTrue(true);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#MailAddress(javax.mail.internet.InternetAddress)}.
     */
    public void testMailAddressInternetAddress() {

        try{
            new MailAddress(new InternetAddress(GOOD_QUOTED_LOCAL_PART));
        }catch (AddressException e){
            System.out.println("AddressException"+e.getMessage());
            assertTrue(e.getMessage(), false);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#getDomain()}.
     */
    public void testGetDomain() {

        try{
            MailAddress a = new MailAddress(new InternetAddress(GOOD_ADDRESS));
            assertTrue(a.getDomain()+" != "+GOOD_DOMAIN,a.getDomain().equals(GOOD_DOMAIN));
        }catch (AddressException e){
            System.out.println("AddressException"+e.getMessage());
            assertTrue(e.getMessage(), false);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#getLocalPart()}.
     */
    public void testGetLocalPart() {

        try{
            MailAddress a = new MailAddress(new InternetAddress(GOOD_QUOTED_LOCAL_PART));
            assertTrue(GOOD_LOCAL_PART+" != "+a.getLocalPart(),a.getLocalPart().equals(GOOD_LOCAL_PART));
        }catch (AddressException e){
            System.out.println("AddressException"+e.getMessage());
            assertTrue(e.getMessage(), false);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#toString()}.
     */
    public void testToString() {

        try{
            MailAddress a = new MailAddress(new InternetAddress(GOOD_ADDRESS));
            assertTrue(a.toString()+" != "+GOOD_ADDRESS,a.toString().equals(GOOD_ADDRESS));
        }catch (AddressException e){
            System.out.println("AddressException"+e.getMessage());
            assertTrue(e.getMessage(), false);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#toInternetAddress()}.
     */
    public void testToInternetAddress() {

        try{
            InternetAddress b = new InternetAddress(GOOD_ADDRESS);
            MailAddress a = new MailAddress(b);
            assertTrue(a.toInternetAddress().equals(b));
            assertTrue(a.toString()+" != "+GOOD_ADDRESS,a.toString().equals(GOOD_ADDRESS));
        }catch (AddressException e){
            System.out.println("AddressException"+e.getMessage());
            assertTrue(e.getMessage(), false);
        }
    }

    /**
     * Test method for {@link org.apache.mailet.MailAddress#equals(java.lang.Object)}.
     * @throws AddressException 
     */
    public void testEqualsObject() throws AddressException {

        MailAddress a = new MailAddress(GOOD_ADDRESS);
        MailAddress b = new MailAddress(GOOD_ADDRESS);
        
        assertTrue(a.toString()+" != "+b.toString(),a.equals(b));
    }
}

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

import org.apache.james.jspf.core.IPAddr;
import org.apache.james.jspf.core.exceptions.PermErrorException;

import junit.framework.TestCase;

public class IPAddrTest extends TestCase {

    public void testValidIp4Address() throws PermErrorException {
        assertEquals("in-addr", IPAddr.getInAddress("123.212.255.213"));
        assertEquals("in-addr", IPAddr.getInAddress("0.0.0.0"));
        assertEquals("in-addr", IPAddr.getInAddress("255.255.255.255"));
    }

    public void testValidIp4OverIpv6Address() throws PermErrorException {
        assertEquals("ip6", IPAddr.getInAddress("0:0:0:0:0:0:13.1.68.3"));
        assertEquals("ip6", IPAddr
                .getInAddress("0:0:0:0:0:FFFF:129.144.52.38"));
        assertEquals("ip6", IPAddr.getInAddress("::13.1.68.3"));
        assertEquals("ip6", IPAddr.getInAddress("::FFFF:129.144.52.38"));
    }

    public void testValidIp6Address() throws PermErrorException {
        assertEquals("ip6", IPAddr
                .getInAddress("FEDC:BA98:7654:3210:FEDC:BA98:7654:3210"));
        assertEquals("ip6", IPAddr.getInAddress("1080:0:0:0:8:800:200C:417A"));
        assertEquals("ip6", IPAddr.getInAddress("FF01:0:0:0:0:0:0:101"));
        assertEquals("ip6", IPAddr.getInAddress("0:0:0:0:0:0:0:1"));
        assertEquals("ip6", IPAddr.getInAddress("0:0:0:0:0:0:0:0"));
        assertEquals("ip6", IPAddr.getInAddress("1080::8:800:200C:417A"));
        assertEquals("ip6", IPAddr.getInAddress("FF01::101"));
        assertEquals("ip6", IPAddr.getInAddress("::1"));
        assertEquals("ip6", IPAddr.getInAddress("::"));
    }

    public void testInvalidIp6Address() throws PermErrorException {
        try {
            assertEquals("ip6", IPAddr.getInAddress("12AB:0:0:CD3"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr
                    .getInAddress("1080:0:0:0:8::800:200C:417A"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress("FF01:0:0:0:0:0:0:00000"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress("0:0:0:0:0:0:0:0:1"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress("0:0:0:0:0:0:0:O"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress("1080::8:800::200C:417A"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress("FF01:::101"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress(":1:"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("ip6", IPAddr.getInAddress(":"));
            fail();
        } catch (PermErrorException e) {
        }
    }

    public void testInvalidIp4AddressGreatThan255() {
        try {
            assertEquals("in-addr", IPAddr.getInAddress("333.212.255.213"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("in-addr", IPAddr.getInAddress("1.2.3."));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("in-addr", IPAddr.getInAddress("1.2.3.a"));
            fail();
        } catch (PermErrorException e) {
        }
        try {
            assertEquals("in-addr", IPAddr.getInAddress("1.1.1.1111"));
            fail();
        } catch (PermErrorException e) {
        }
    }

}

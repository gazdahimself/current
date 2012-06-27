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


package org.apache.james.jspf;

import org.apache.james.jspf.impl.DNSServiceXBillImpl;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SPFRecord;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

public class DNSServiceXBillImplTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'org.apache.james.jspf.DNSServiceXBillImpl.getLocalDomainNames()'
     */
    public void testGetLocalDomainNames() throws UnknownHostException,
            TextParseException {
        // This write MACHINE-NAME/MACHINE-ADDRESS
        System.out.println(InetAddress.getLocalHost());
        // THIS WRITE localhost/127.0.0.1
        System.out.println(InetAddress.getAllByName(null)[0]);
        // THIS WRITE a fully qualified MACHINE-NAME.MACHINE-DOMAIN
        System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
        // THIS WRITE localhost
        System.out.println(InetAddress.getAllByName(null)[0]
                .getCanonicalHostName());
        Record[] record = new Lookup(Name.root, Type.ANY).run();
        if (record !=null) System.out.println(record[0]);
    }
    
    public void testMultipleStrings() throws Exception {
        Record[] rr = new Record[] { TXTRecord.fromString(Name.fromString("test.local."), Type.TXT, DClass.IN, 0, "\"string \" \"concatenated\"", Name.fromString("local.")) };
        assertEquals("string concatenated", DNSServiceXBillImpl.convertRecordsToList(rr).get(0));

        rr = new Record[] { TXTRecord.fromString(Name.fromString("test.local."), Type.TXT, DClass.IN, 0, "string", Name.fromString("local.")) };
        assertEquals("string", DNSServiceXBillImpl.convertRecordsToList(rr).get(0));

        rr = new Record[] { TXTRecord.fromString(Name.fromString("test.local."), Type.TXT, DClass.IN, 0, "\"quoted string\"", Name.fromString("local.")) };
        assertEquals("quoted string", DNSServiceXBillImpl.convertRecordsToList(rr).get(0));

        rr = new Record[] { SPFRecord.fromString(Name.fromString("test.local."), Type.SPF, DClass.IN, 0, "\"quot\" \"ed st\" \"ring\"", Name.fromString("local.")) };
        assertEquals("quoted string", DNSServiceXBillImpl.convertRecordsToList(rr).get(0));
    }

}

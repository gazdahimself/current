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
package org.apache.james.dnsservice.dnsjava;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

public class DNSJavaServiceTest {

    private TestableDNSServer dnsServer;
    private Cache defaultCache;
    private Resolver defaultResolver;
    private Name[] defaultSearchPaths;

    @Test
    public void testNoMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("dnstest.com."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        Collection<String> records = dnsServer.findMXRecords("nomx.dnstest.com.");
        assertEquals(1, records.size());
        assertEquals("nomx.dnstest.com.", records.iterator().next());
    }

    @Test
    public void testBadMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("dnstest.com."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        Collection<String> records = dnsServer.findMXRecords("badmx.dnstest.com.");
        assertEquals(1, records.size());
        assertEquals("badhost.dnstest.com.", records.iterator().next());
        // Iterator<HostAddress> it =
        // dnsServer.getSMTPHostAddresses("badmx.dnstest.com.");
        // assertFalse(it.hasNext());
    }

    @Test
    public void testINARecords() throws Exception {
        // Zone z = loadZone("pippo.com.");
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("pippo.com."));
        // dnsServer.setLookupper(new ZoneLookupper(z));
        Collection<String> records = dnsServer.findMXRecords("www.pippo.com.");
        assertEquals(1, records.size());
        assertEquals("pippo.com.inbound.mxlogic.net.", records.iterator().next());
    }

    @Test
    public void testMXCatches() throws Exception {
        // Zone z = loadZone("test-zone.com.");
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("test-zone.com."));
        // dnsServer.setLookupper(new ZoneLookupper(z));
        Collection<String> res = dnsServer.findMXRecords("test-zone.com.");
        try {
            res.add(new String());
            fail("MX Collection should not be modifiable");
        } catch (UnsupportedOperationException e) {
        }
        assertEquals(1, res.size());
        assertEquals("mail.test-zone.com.", res.iterator().next());
    }

    /**
     * Test for JAMES-1251
     */
    @Test
    public void testTwoMXSamePrio() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("two-mx.sameprio."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        Collection<String> records = dnsServer.findMXRecords("two-mx.sameprio.");
        assertEquals(2, records.size());
        assertTrue(records.contains("mx1.two-mx.sameprio."));
        assertTrue(records.contains("mx2.two-mx.sameprio."));
    }

    @Test
    public void testThreeMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("three-mx.bar."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        ArrayList<String> records = new ArrayList<String>(dnsServer.findMXRecords("three-mx.bar."));
        assertEquals(3, records.size());
        assertTrue(records.contains("mx1.three-mx.bar."));
        assertTrue(records.contains("mx2.three-mx.bar."));
        assertEquals("mx3.three-mx.bar.", records.get(2));

    }

    /**
     * Test for JAMES-1251
     */
    @Test
    public void testTwoMXDifferentPrio() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("two-mx.differentprio."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        Collection<String> records = dnsServer.findMXRecords("two-mx.differentprio.");
        assertEquals(2, records.size());
        assertTrue(records.contains("mx1.two-mx.differentprio."));
        assertTrue(records.contains("mx2.two-mx.differentprio."));

    }

    /**
     * Test for JAMES-1251
     */
    @Test
    public void testOneMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("one-mx.bar."));
        // a.setSearchPath(new String[] { "searchdomain.com." });
        Collection<String> records = dnsServer.findMXRecords("one-mx.bar.");
        assertEquals(1, records.size());
        assertTrue(records.contains("mx1.one-mx.bar."));

    }
    /*
     * public void testCNAMEasMXrecords() throws Exception { // Zone z =
     * loadZone("brandilyncollins.com."); dnsServer.setResolver(null);
     * dnsServer.setCache(new ZoneCache("brandilyncollins.com.")); //
     * dnsServer.setLookupper(new ZoneLookupper(z)); //Iterator<HostAddress>
     * records = dnsServer.getSMTPHostAddresses("brandilyncollins.com.");
     * //assertEquals(true, records.hasNext()); }
     */

    @Before
    public void setUp() throws Exception {
        dnsServer = new TestableDNSServer();
        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        db.load(new ByteArrayInputStream("<dnsserver><autodiscover>true</autodiscover><authoritative>false</authoritative></dnsserver>".
                getBytes()));
        dnsServer.setLog(LoggerFactory.getLogger("MockLog"));
        dnsServer.configure(db);
        dnsServer.init();

        defaultCache = Lookup.getDefaultCache(DClass.IN);
        defaultResolver = Lookup.getDefaultResolver();
        defaultSearchPaths = Lookup.getDefaultSearchPath();
        Lookup.setDefaultCache(null, DClass.IN);
        Lookup.setDefaultResolver(null);
        Lookup.setDefaultSearchPath(new Name[]{});
    }

    @After
    public void tearDown() throws Exception {
        dnsServer.setCache(null);
        dnsServer = null;
        Lookup.setDefaultCache(defaultCache, DClass.IN);
        Lookup.setDefaultResolver(defaultResolver);
        Lookup.setDefaultSearchPath(defaultSearchPaths);
    }

    private Zone loadZone(String zoneName) throws IOException {
        String zoneFilename = zoneName + "zone";
        URL zoneResource = getClass().getResource(zoneFilename);
        assertNotNull("test resource for zone could not be loaded: " + zoneFilename, zoneResource);
        String zoneFile = zoneResource.getFile();
        Zone zone = new Zone(Name.fromString(zoneName), zoneFile);
        return zone;
    }

    private final class ZoneCache extends Cache {

        Zone z = null;

        public ZoneCache(String string) throws IOException {
            z = loadZone(string);
        }

        @Override
        public SetResponse addMessage(Message arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public synchronized void addNegative(Name arg0, int arg1, SOARecord arg2, int arg3) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public synchronized void addRecord(Record arg0, int arg1, Object arg2) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public synchronized void addRRset(RRset arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public synchronized void clearCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public RRset[] findAnyRecords(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public RRset[] findRecords(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public void flushName(Name arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public void flushSet(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public int getDClass() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public int getMaxCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public int getMaxEntries() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public int getMaxNCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public int getSize() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        protected synchronized SetResponse lookup(Name arg0, int arg1, int arg2) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public SetResponse lookupRecords(Name arg0, int arg1, int arg2) {
            System.out.println("Cache.lookupRecords " + arg0 + "," + arg1 + "," + arg2);
            return z.findRecords(arg0, arg1);
            // return super.lookupRecords(arg0, arg1, arg2);
        }

        @Override
        public void setMaxCache(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public void setMaxEntries(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        @Override
        public void setMaxNCache(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }
    }

    private final class TestableDNSServer extends DNSJavaService {

        public void setResolver(Resolver r) {
            resolver = r;
        }

        public void setCache(Cache c) {
            cache = c;
        }
    }
}

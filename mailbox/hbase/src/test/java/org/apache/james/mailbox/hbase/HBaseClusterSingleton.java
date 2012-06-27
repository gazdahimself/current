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
package org.apache.james.mailbox.hbase;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that will creates a single instance of HBase MiniCluster.
 */
public final class HBaseClusterSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseClusterSingleton.class);
    private static final HBaseTestingUtility htu = new HBaseTestingUtility();
    private static HBaseClusterSingleton cluster = null;
    private MiniHBaseCluster hbaseCluster;
    private Configuration conf;

    /**
     * Builds a MiniCluster instance.
     * @return the {@link HBaseClusterSingleton} instance
     * @throws RuntimeException
     */
    public static synchronized HBaseClusterSingleton build()
            throws RuntimeException {
        LOG.info("Retrieving cluster instance.");
        if (cluster == null) {
            cluster = new HBaseClusterSingleton();
        }
        return cluster;
    }

    private HBaseClusterSingleton() throws RuntimeException {
        htu.getConfiguration().setBoolean("dfs.support.append", true);
        htu.getConfiguration().setInt("zookeeper.session.timeout", 20000);
//        htu.getConfiguration().setInt("hbase.client.retries.number", 2);
        try {
            hbaseCluster = htu.startMiniCluster();
            LOG.info("After cluster start-up.");
            hbaseCluster.waitForActiveAndReadyMaster();
            LOG.info("After active and ready.");
//            ensureTables();
            conf = hbaseCluster.getConfiguration();
        } catch (Exception ex) {
            throw new RuntimeException("Minicluster not starting.");
        } finally {
            if (hbaseCluster != null) {
                // add a shutdown hook for shuting down the minicluster.
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            hbaseCluster.shutdown();
                        } catch (IOException e) {
                            throw new RuntimeException("Exception shuting down cluster.");
                        }
                    }
                }));
            }
        }
    }

    /**
     * Return a configuration for the runnning MiniCluster.
     * @return
     */
    public Configuration getConf() {
        return conf;
    }

    /**
     * Creates a table with the specified column families.
     * @param tableName the table name
     * @param columnFamilies the colum families
     * @throws IOException
     */
    public void ensureTable(String tableName, String... columnFamilies) throws IOException {
        byte[][] cfs = new byte[columnFamilies.length][];
        for (int i = 0; i < columnFamilies.length; i++) {
            cfs[i] = Bytes.toBytes(columnFamilies[i]);
        }
        ensureTable(Bytes.toBytes(tableName), cfs);
    }

    /**
     * Creates a table with the specified column families.
     * @param tableName the table name
     * @param cfs the column families
     * @throws IOException
     */
    public void ensureTable(byte[] tableName, byte[][] cfs) throws IOException {
        HBaseAdmin admin = htu.getHBaseAdmin();
        if (!admin.tableExists(tableName)) {
            htu.createTable(tableName, cfs);
        }
    }

    /**
     * Delete all rows from specified table.
     *
     * @param tableName
     */
    public void clearTable(String tableName) {
        HTable table = null;
        ResultScanner scanner = null;
        try {
            table = new HTable(conf, tableName);
            Scan scan = new Scan();
            scan.setCaching(1000);
            scanner = table.getScanner(scan);
            Result result;
            while ((result = scanner.next()) != null) {
                Delete delete = new Delete(result.getRow());
                table.delete(delete);
            }
        } catch (IOException ex) {
            LOG.info("Exception clearing table {}", tableName);
        } finally {
            IOUtils.closeStream(scanner);
            IOUtils.closeStream(table);
        }
    }
}

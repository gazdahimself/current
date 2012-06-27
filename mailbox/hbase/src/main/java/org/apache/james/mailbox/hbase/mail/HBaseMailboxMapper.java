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
package org.apache.james.mailbox.hbase.mail;

import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOXES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOX_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOX_MESSAGE_COUNT;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOX_NAME;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_META_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGE_INTERNALDATE;
import static org.apache.james.mailbox.hbase.HBaseUtils.mailboxFromResult;
import static org.apache.james.mailbox.hbase.HBaseUtils.mailboxRowKey;
import static org.apache.james.mailbox.hbase.HBaseUtils.toPut;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.WritableByteArrayComparable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IOUtils;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.hbase.HBaseNames;
import org.apache.james.mailbox.hbase.HBaseNonTransactionalMapper;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.name.MailboxNameResolver;
import org.apache.james.mailbox.name.MailboxOwner;
import org.apache.james.mailbox.name.MailboxName;
import org.apache.james.mailbox.name.codec.MailboxNameCodec;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Data access management for mailbox.
 * 
 */
public class HBaseMailboxMapper extends HBaseNonTransactionalMapper implements MailboxMapper<UUID> {

    protected static class PatternComparator extends WritableByteArrayComparable {
        private byte[] value;

        public byte[] getValue() {
            return value;
        }

        private static final Log LOG = LogFactory.getLog(PatternComparator.class);

        private Charset charset = Charset.forName(HConstants.UTF8_ENCODING);

        private Pattern pattern;

        /** Nullary constructor for Writable, do not use */
        public PatternComparator() {
        }

        /**
         * Constructor
         * 
         * @param expr
         *            a valid regular expression
         */
        public PatternComparator(String expr) {
            super(Bytes.toBytes(expr));
            this.pattern = Pattern.compile(expr);
        }

        /**
         * Specifies the {@link Charset} to use to convert the row key to a
         * String.
         * <p>
         * The row key needs to be converted to a String in order to be matched
         * against the regular expression. This method controls which charset is
         * used to do this conversion.
         * <p>
         * If the row key is made of arbitrary bytes, the charset
         * {@code ISO-8859-1} is recommended.
         * 
         * @param charset
         *            The charset to use.
         */
        public void setCharset(final Charset charset) {
            this.charset = charset;
        }

        @Override
        public int compareTo(byte[] value) {
            String str = new String(value, charset);
            if (pattern.matcher(str).matches()) {
                LOG.info(" - match    " + pattern +"\t"+ str);
                return 0;
            }
            else {
                LOG.info(" - no match " + pattern +"\t"+ str);
                return 1;
            }
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            final String expr = in.readUTF();
            this.value = Bytes.toBytes(expr);
            this.pattern = Pattern.compile(expr);
            final String charset = in.readUTF();
            if (charset.length() > 0) {
                try {
                    this.charset = Charset.forName(charset);
                } catch (IllegalCharsetNameException e) {
                    LOG.error("invalid charset", e);
                }
            }
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeUTF(pattern.toString());
            out.writeUTF(charset.name());
        }

    }

    /**
     * Link to the HBase Configuration object and specific mailbox names
     */
    private final Configuration conf;
    private final MailboxNameCodec mailboxNameCodec;
    private final MailboxNameResolver mailboxNameResolver;

    public HBaseMailboxMapper(Configuration conf, MailboxNameResolver mailboxNameResolver, MailboxNameCodec mailboxNameCodec) {
        this.conf = conf;
        this.mailboxNameResolver = mailboxNameResolver;
        this.mailboxNameCodec = mailboxNameCodec;
    }

    @Override
    public Mailbox<UUID> findMailboxByPath(MailboxName mailboxPath) throws MailboxException, MailboxNotFoundException {
        HTable mailboxes = null;
        ResultScanner scanner = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);

            SingleColumnValueFilter nameFilter = new SingleColumnValueFilter(MAILBOX_CF, MAILBOX_NAME, CompareOp.EQUAL, Bytes.toBytes(mailboxNameCodec.encode(mailboxPath)));
            Scan scan = prepareMailboxNameScan(mailboxPath, mailboxes, nameFilter);

            scanner = mailboxes.getScanner(scan);
            Result result = scanner.next();

            if (result == null) {
                throw new MailboxNotFoundException(mailboxPath);
            }
            return mailboxFromResult(result, mailboxNameCodec);
        } catch (IOException e) {
            throw new MailboxException("Search of mailbox " + mailboxPath + " failed", e);
        } finally {
            IOUtils.closeStream(scanner);
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    protected Scan prepareMailboxNameScan(MailboxName mailboxPath, HTable mailboxes, Filter nameFilter) {
        Scan scan = new Scan();
        scan.addFamily(MAILBOX_CF);
        scan.setCaching(mailboxes.getScannerCaching() * 2);
        scan.setMaxVersions(1);

        FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        /*
         * Filters is ORDERED. Passing the parameters in the right order might
         * improve performance: passing the user first means that the other
         * filters will not be tested if the mailbox does not belong to the
         * passed user.
         */
        MailboxOwner owner = mailboxNameResolver.getOwner(mailboxPath);
        if (owner != null) {
            filters.addFilter(new SingleColumnValueFilter(MAILBOX_CF, HBaseNames.MAILBOX_USER, CompareOp.EQUAL, Bytes.toBytes(owner.getName())));
            filters.addFilter(new SingleColumnValueFilter(MAILBOX_CF, HBaseNames.MAILBOX_OWNER_IS_GROUP, CompareOp.EQUAL, Bytes.toBytes(owner.isGroup())));
        }
        filters.addFilter(nameFilter);
        scan.setFilter(filters);
        return scan;
    }

    protected Scan preparePatternScan(MailboxName mailboxPath, HTable mailboxes) {
        String patternName = MailboxNameCodec.SEARCH_PATTERN_NAME_CODEC.encode(mailboxPath);
        PatternComparator pathComparator = new PatternComparator(patternName);
        SingleColumnValueFilter nameFilter = new SingleColumnValueFilter(MAILBOX_CF, HBaseNames.MAILBOX_NAME, CompareOp.EQUAL, pathComparator);
        return prepareMailboxNameScan(mailboxPath, mailboxes, nameFilter);
    }

    @Override
    public List<Mailbox<UUID>> findMailboxWithPathLike(MailboxName mailboxPath) throws MailboxException {
        HTable mailboxes = null;
        ResultScanner scanner = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            Scan scan = preparePatternScan(mailboxPath, mailboxes);
            scanner = mailboxes.getScanner(scan);

            List<Mailbox<UUID>> mailboxList = new ArrayList<Mailbox<UUID>>();

            for (Result result : scanner) {
                mailboxList.add(mailboxFromResult(result, mailboxNameCodec));
            }
            return mailboxList;
        } catch (IOException e) {
            throw new MailboxException("Search of mailbox " + mailboxPath + " failed", e);
        } finally {
            IOUtils.closeStream(scanner);
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    @Override
    public List<Mailbox<UUID>> list() throws MailboxException {
        HTable mailboxes = null;
        ResultScanner scanner = null;
        // TODO: possible performance isssues, we are creating an object from
        // all the rows in HBase mailbox table
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            Scan scan = new Scan();
            scan.addFamily(MAILBOX_CF);
            scan.setCaching(mailboxes.getScannerCaching() * 2);
            scan.setMaxVersions(1);
            scanner = mailboxes.getScanner(scan);
            List<Mailbox<UUID>> mailboxList = new ArrayList<Mailbox<UUID>>();

            Result result;
            while ((result = scanner.next()) != null) {
                Mailbox<UUID> mlbx = mailboxFromResult(result, mailboxNameCodec);
                mailboxList.add(mlbx);
            }
            return mailboxList;
        } catch (IOException ex) {
            throw new MailboxException("HBase IOException in list()", ex);
        } finally {
            scanner.close();
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    @Override
    public void endRequest() {
    }

    @Override
    public void save(Mailbox<UUID> mlbx) throws MailboxException {
        // TODO: maybe switch to checkAndPut for transactions
        HTable mailboxes = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            /*
             * cast to HBaseMailbox to access lastuid and ModSeq
             */
            Put put = toPut((HBaseMailbox) mlbx, mailboxNameCodec);
            mailboxes.put(put);
        } catch (IOException ex) {
            throw new MailboxException("IOExeption", ex);
        } finally {
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    @Override
    public void delete(Mailbox<UUID> mlbx) throws MailboxException {
        // TODO: maybe switch to checkAndDelete
        HTable mailboxes = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            // TODO: delete all maessages from this mailbox
            Delete delete = new Delete(mailboxRowKey(mlbx.getMailboxId()));
            mailboxes.delete(delete);
        } catch (IOException ex) {
            throw new MailboxException("IOException in HBase cluster during delete()", ex);
        } finally {
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    @Override
    public boolean hasChildren(final Mailbox<UUID> mailbox) throws MailboxException, MailboxNotFoundException {
        HTable mailboxes = null;
        ResultScanner scanner = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);

            Scan scan = preparePatternScan(mailbox.getMailboxName().child(MailboxQuery.FREEWILDCARD_STRING), mailboxes);
            scanner = mailboxes.getScanner(scan);
            try {
                if (scanner.next() != null) {
                    return true;
                }
            } catch (IOException e) {
                throw new MailboxNotFoundException("hasChildren() " + mailbox.getMailboxName());
            }
            return false;
        } catch (IOException e) {
            throw new MailboxException("Search of mailbox " + mailbox + " failed", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new MailboxException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }

    public void deleteAllMemberships() {
        HTable messages = null;
        HTable mailboxes = null;
        ResultScanner scanner = null;
        try {
            messages = new HTable(conf, MESSAGES_TABLE);
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            Scan scan = new Scan();
            scan.setMaxVersions(1);
            scan.addColumn(MESSAGES_META_CF, MESSAGE_INTERNALDATE);
            scanner = messages.getScanner(scan);
            Result result;
            List<Delete> deletes = new ArrayList<Delete>();
            while ((result = scanner.next()) != null) {
                deletes.add(new Delete(result.getRow()));
            }
            long totalDeletes = deletes.size();
            messages.delete(deletes);
            if (deletes.size() > 0) {
                // TODO: what shoul we do if not all messages are deleted?
                System.out.println("Just " + deletes.size() + " out of " + totalDeletes + " messages have been deleted");
                // throw new RuntimeException("Just " + deletes.size() +
                // " out of " + totalDeletes + " messages have been deleted");
            }
            List<Put> puts = new ArrayList<Put>();
            scan = new Scan();
            scan.setMaxVersions(1);
            scan.addColumn(MAILBOX_CF, MAILBOX_MESSAGE_COUNT);
            scanner = mailboxes.getScanner(scan);
            Put put = null;
            while ((result = scanner.next()) != null) {
                put = new Put(result.getRow());
                put.add(MAILBOX_CF, MAILBOX_MESSAGE_COUNT, Bytes.toBytes(0L));
                puts.add(new Put());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error deleting MESSAGES table ", e);
        } finally {
            IOUtils.closeStream(scanner);
            if (messages != null) {
                try {
                    messages.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Error closing table " + messages, ex);
                }
            }
        }
    }

    public void deleteAllMailboxes() {
        HTable mailboxes = null;
        ResultScanner scanner = null;
        try {
            mailboxes = new HTable(conf, MAILBOXES_TABLE);
            Scan scan = new Scan();
            scan.setMaxVersions(1);
            scan.addColumn(MAILBOX_CF, MAILBOX_NAME);
            scanner = mailboxes.getScanner(scan);
            Result result;
            List<Delete> deletes = new ArrayList<Delete>();
            while ((result = scanner.next()) != null) {
                deletes.add(new Delete(result.getRow()));
            }
            mailboxes.delete(deletes);
        } catch (IOException ex) {
            throw new RuntimeException("IOException deleting mailboxes", ex);
        } finally {
            IOUtils.closeStream(scanner);
            if (mailboxes != null) {
                try {
                    mailboxes.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Error closing table " + mailboxes, ex);
                }
            }
        }
    }
}

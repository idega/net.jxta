/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *  must not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: SrdiIndex.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
 */
package net.jxta.impl.cm;


import java.io.File;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.ArrayList;

import java.io.IOException;
import java.io.EOFException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;

import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.data.Record;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.filer.BTreeFiler;
import net.jxta.impl.xindice.core.filer.BTreeCallback;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.impl.xindice.core.indexer.NameIndexer;


/**
 *  SrdiIndex
 */
public class SrdiIndex implements Runnable {

    /**
     *  Log4J Logger
     */
    private final static Logger LOG = Logger.getLogger(SrdiIndex.class.getName());

    private long interval = 1000 * 60 * 10;
    private volatile boolean stop = false;
    private Indexer srdiIndexer = null;
    private BTreeFiler cacheDB = null;
    private Thread gcThread = null;
    private Set gcPeerTBL = new HashSet();

    private final String indexName;
    
    /**
     *  Constructor for the SrdiIndex
     *
     * @param  group group
     * @param indexName
     */
    public SrdiIndex(PeerGroup group, String indexName) {
        this.indexName = indexName;

        try {
            String pgdir = null;
            if (group == null) {
                pgdir = "srdi-index";
            } else {
                pgdir = group.getPeerGroupID().getUniqueValue().toString();
            }

            File rootDir = new File(Cm.ROOTDIRBASE, pgdir);

            rootDir = new File(rootDir, "srdi");
            if (!rootDir.exists()) {
                // We need to create the directory
                if (!rootDir.mkdirs()) {
                    throw new RuntimeException("Cm cannot create directory " + rootDir);
                }
            }
            // peerid database
            // Storage
            cacheDB = new BTreeFiler();
            // lazy checkpoint
            cacheDB.setSync(false);
            cacheDB.setLocation(rootDir.getCanonicalPath(), indexName);

            if (!cacheDB.open()) {
                cacheDB.create();
                // now open it
                cacheDB.open();
            }

            // index
            srdiIndexer = new Indexer(false);
            srdiIndexer.setLocation(rootDir.getCanonicalPath(), indexName);
            if (!srdiIndexer.open()) {
                srdiIndexer.create();
                // now open it
                srdiIndexer.open();
            }

            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("[" + ((group == null) ? "none" : group.getPeerGroupName()) + "] : " + "Initialized " + indexName);
            }
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Unable to Initialize databases", de);
            }

            throw new UndeclaredThrowableException(de, "Unable to Initialize databases");
        } catch (Throwable e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Unable to create Cm", e);
            }

            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new UndeclaredThrowableException(e, "Unable to create Cm");
            }
        }
    }

    /**
     *  Construct a SrdiIndex and starts a GC thread which runs every "interval"
     *  milliseconds
     *
     * @param  interval   the interval at which the gc will run in milliseconds
     * @param  group      group context
     * @param  indexName  SrdiIndex name
     */

    public SrdiIndex(PeerGroup group, String indexName, long interval) {
        this(group, indexName);
        this.interval = interval;
        startGC(group, indexName, interval);
    }

    /** Start the GC thread */
    protected synchronized void startGC(PeerGroup group, String indexName, long interval) {
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("[" + ((group == null) ? "none" : group.getPeerGroupName()) + "] : Starting SRDI GC Thread for " + indexName);
        }

        gcThread = new Thread(group.getHomeThreadGroup(), this, "SrdiIndex GC :" + indexName + " every " + interval + "ms");
        gcThread.setDaemon(true);
        gcThread.start();
    }

    /**
     *  Returns the name of this srdi index.
     *
     *  @return index name.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     *  add an index entry
     *
     *@param  primaryKey  primary key
     *@param  attribute   Attribute String to query on
     *@param  value       value of the attribute string
     *@param  expiration  expiration associated with this entry relative time in
     *      milliseconds
     *@param  pid         peerid reference
     */
    public synchronized void add(String primaryKey,
            String attribute,
            String value,
            PeerID pid,
            long expiration) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + indexName + "] Adding " + primaryKey + "/" + attribute + " = '" + value + "' for " + pid);
        }

        try {
            Key key = new Key(primaryKey + attribute + value);
            long expiresin = TimeUtils.toAbsoluteTimeMillis(expiration);

            // update the record if it exists
            synchronized(cacheDB) {
                // FIXME hamada 10/14/04 it is possible a peer re-appears with
                // a different set of indexes since it's been marked for garbage
                // collection.  will address this issue in a subsequent patch
                gcPeerTBL.remove(pid);
                
                Record record = cacheDB.readRecord(key);
                ArrayList old;

                if (record != null) {
                    old = readRecord(record).list;
                } else {
                    old = new ArrayList();
                }
                Entry entry = new Entry(pid, expiresin);

                if (!old.contains(entry)) {
                    old.add(entry);
                } else {
                    // entry exists, replace it (effectively updating expiration)
                    old.remove(old.indexOf(entry));
                    old.add(entry);
                }
                // no sense in keeping expired entries.
                old = removeExpired(old);
                long t0 = System.currentTimeMillis();
                byte[] data = getData(key, old);

                // if (LOG.isEnabledFor(Level.DEBUG)) {
                // LOG.debug("Serialized result in : " + (System.currentTimeMillis() - t0) + "ms.");
                // }
                if (data == null) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Failed to serialize data");
                    }
                    return;
                }
                Value recordValue = new Value(data);
                long pos = cacheDB.writeRecord(key, recordValue);
                Map indexables = getIndexMap(primaryKey + attribute, value);

                srdiIndexer.addToIndex(indexables, pos);
            }
        } catch (IOException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to add SRDI", de);
            }
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to add SRDI", de);
            }
        }
    }


    /**
     *  retrieves a record
     *
     *@param  pkey  primary key
     *@param  skey  secondary key
     *@param  value       value
     *@return             List of Entry objects
     */
    public List getRecord(String pkey, String skey, String value) {
        Record record = null;
        try {
            Key key = new Key(pkey + skey + value);
            synchronized (cacheDB) {
                record = cacheDB.readRecord(key);
            }
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to retrieve SrdiIndex record", de);
            }
        }
        // if record is null, readRecord returns an empty list
        return readRecord(record).list;

    }

    /**
     *  inserts a pkey into a map with a value of value.toUpperCase()
     *
     *@param  primaryKey  primary key
     *@param  value       value
     *@return             The Map
     */

    private Map getIndexMap(String primaryKey, String value) {
        if (primaryKey == null) {
            return null;
        }
        if (value == null) {
            value = "";
        }
        Map map = new HashMap(1);
        map.put(primaryKey, value.toUpperCase());
        return map;
    }

    /**
     *  remove entries pointing to peer id from cache
     *
     *@param  pid   peer id to remove
     */
    public synchronized void remove(PeerID pid) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug(" Adding " + pid + " to peer GC table");
        }
        gcPeerTBL.add(pid);
    }

    /**
     *  Query SrdiIndex
     *
     * @param  attribute  Attribute String to query on
     * @param  value      value of the attribute string
     * @return            an enumeration of canonical paths
     */
    public synchronized Vector query(String primaryKey, String attribute, String value, int threshold) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + indexName + "] Querying for " + threshold + " " + primaryKey + "/" + attribute + " = '" + value + "'");
        }

        // return nothing
        if (primaryKey == null) {
            return new Vector();
        }

        Vector res;

        // a blind query
        if (attribute == null) {
            res = query(primaryKey);
        } else {
            res = new Vector();

            IndexQuery iq = Cm.getIndexQuery(value);

            try {
                srdiIndexer.search(iq, primaryKey + attribute, new SearchCallback(cacheDB, res, threshold, gcPeerTBL));
            } catch (Exception ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while searching in index", ex);
                }
            }
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + indexName + "] Returning " + res.size() + " results for " + primaryKey + "/" + attribute + " = '" + value + "'");
        }

        return res;
    }

    /**
     *  Query SrdiIndex
     *
     *@param  primaryKey  primary key
     *@return             an enumeration of peerids
     */
    public synchronized Vector query(String primaryKey) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + indexName + "] Querying for " + primaryKey);
        }

        Vector res = new Vector();

        try {
            Map map = srdiIndexer.getIndexers();
            Iterator it = map.keySet().iterator();

            while (it != null && it.hasNext()) {
                String indexName = (String) it.next();

                // seperate the index name from attribute
                if (indexName.startsWith(primaryKey)) {
                    NameIndexer idxr = (NameIndexer) map.get(indexName);

                    idxr.query(null, new SearchCallback(cacheDB, res, Integer.MAX_VALUE, gcPeerTBL));
                }
            }
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception while searching in index", ex);
            }
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + indexName + "] Returning " + res.size() + " results for " + primaryKey);
        }

        return res;
    }

    private static final class SearchCallback implements BTreeCallback {
        private BTreeFiler cacheDB = null;
        private int threshold;
        private Vector results;
        private Set table;

        SearchCallback(BTreeFiler cacheDB, Vector results, int threshold, Set table) {
            this.cacheDB = cacheDB;
            this.threshold = threshold;
            this.results = results;
            this.table = table;
        }

        /**
         *  @inheritDoc
         */
        public boolean indexInfo(Value val, long pos) {

            if (results.size() >= threshold) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("SearchCallback.indexInfo reached Threshold :" + threshold);
                }
                return false;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Found " + val.toString());
            }
            Record record = null;

            try {
                record = cacheDB.readRecord(pos);
            } catch (DBException ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while reading indexed", ex);
                }
                return false;
            }
            if (record == null) {
                return true;
            }
            long t0 = System.currentTimeMillis();
            SrdiIndexRecord rec = readRecord(record);
            ArrayList res = rec.list;

            // if (LOG.isEnabledFor(Level.DEBUG)) {
            // LOG.debug("Got result back in : " + (System.currentTimeMillis() - t0) + "ms.");
            // }
            copyIntoVector(results, res, table);
            return true;
        }
    }

    private static final class GcCallback implements BTreeCallback {
        private BTreeFiler cacheDB = null;
        private Indexer idxr = null;
        private List list;
        private Set table;
        GcCallback(BTreeFiler cacheDB, Indexer idxr, List list, Set table) {
            this.cacheDB = cacheDB;
            this.idxr = idxr;
            this.list = list;
            this.table = table;
        }
        /**
         *  @inheritDoc
         */
        public boolean indexInfo(Value val, long pos) {

            Record record = null;
            synchronized(cacheDB) {
                try {
                    record = cacheDB.readRecord(pos);
                } catch (DBException ex) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Exception while reading indexed", ex);
                    }
                    return false;
                }
                if (record == null) {
                    return true;
                }
                SrdiIndexRecord rec = readRecord(record);
                ArrayList res = rec.list;
                boolean changed = false;

                for (int i = 0; i < res.size(); i++) {
                    Entry entry = (Entry) res.get(i);

                    if (isExpired(entry.expiration) || table.contains(entry.peerid)) {
                        res.remove(i);
                        changed = true;
                    }
                }
                if (changed) {
                    if (res.size() == 0) {
                        try {
                            cacheDB.deleteRecord(rec.key);
                            list.add(new Long(pos));
                        } catch (DBException e) {
                            if (LOG.isEnabledFor(Level.WARN)) {
                                LOG.warn("Exception while deleting empty record", e);
                            }
                        }

                    } else {
                        // write it back
                        byte[] data = getData(rec.key, res);
                        Value recordValue = new Value(data);

                        try {
                            cacheDB.writeRecord(pos, recordValue);
                        } catch (DBException ex) {
                            if (LOG.isEnabledFor(Level.WARN)) {
                                LOG.warn("Exception while writing back record", ex);
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    /**
     *  copies the content of ArrayList into a vector expired entries are not
     * copied
     *
     *@param  to    Vector to copy into
     *@param  from  ArrayList to copy from
     */

    private static void copyIntoVector(Vector to, ArrayList from, Set table) {
        for (int i = 0; i < from.size(); i++) {
            Entry entry = (Entry) from.get(i);
            boolean expired = isExpired(entry.expiration);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Entry peerid : " + entry.peerid + " Expires at : " + entry.expiration);
                LOG.debug("Entry expired " + expired);
            }
            if (!to.contains(entry.peerid) && !expired) {
                if (!table.contains(entry.peerid)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("adding Entry :" + entry.peerid + " to list");
                }
                to.add(entry.peerid);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Skipping gc marked entry :" + entry.peerid);
                    }
                }
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Skipping expired Entry :" + entry.peerid);
                }
            }
        }
    }

    /**
     *  Converts a List of {@link Entry} into a byte[]
     *
     *@param  key   record key
     *@param  list  ArrayList to convert
     *@return       byte []
     */
    private static byte[] getData(Key key, List list) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(key.toString());
            dos.writeInt(list.size());
            Iterator eachEntry = list.iterator();
            while (eachEntry.hasNext()) {
                Entry anEntry = (Entry) eachEntry.next();
                dos.writeUTF(anEntry.peerid.toString());
                dos.writeLong(anEntry.expiration);
            }
            dos.close();
            return bos.toByteArray();
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Exception while reading Entry", ie);
            }
        }
        return null;
    }

    /**
     *  Reads the content of a record into ArrayList
     *
     *@param  record  Btree Record
     *@return         ArrayList of entries
     */
    public static SrdiIndexRecord readRecord(Record record) {
        ArrayList result = new ArrayList();
        Key key = null;
        if (record == null) {
            return new SrdiIndexRecord(null, result);
        }
        if (record.getValue().getLength() <= 0) {
            return new SrdiIndexRecord(null, result);
        }
        InputStream is = record.getValue().getInputStream();

        try {
            DataInputStream ois = new  DataInputStream(is);
            key = new Key(ois.readUTF());
            int size = ois.readInt();

            for (int i = 0; i < size; i++) {
                try {
                    String idstr = ois.readUTF();
                    PeerID pid = (PeerID) IDFactory.fromURI(new URI(idstr));
                    long exp = ois.readLong();
                    Entry entry = new Entry(pid, exp);
                    result.add(entry);
                } catch (URISyntaxException badID) {
                    continue;
                }
            }
            ois.close();
        } catch (EOFException eofe) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Empty record", eofe);
            }
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception while reading Entry", ie);
            }
        }
        return new SrdiIndexRecord(key, result);
    }

    /**
     * Empties the index completely.
     * The entries are abandonned to the GC.
     */
    public synchronized void clear() {
        // FIXME changing the behavior a bit
        // instead of dropping all srdi entries, we let them expire
        // if that is not a desired behavior the indexer could be dropped
        // simply close it, and remove all index db created
        try {
            srdiIndexer.close();
            cacheDB.close();
        } catch (Exception e) {
            // bad bits we are done
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to close index", e);
            }
        }
    }

    /**
     *  Garbage Collect expired entries
     */
    public synchronized void garbageCollect() {
        try {
            Map map = srdiIndexer.getIndexers();
            Iterator it = map.keySet().iterator();
            List list = new ArrayList();

            while (it.hasNext()) {
                String indexName = (String) it.next();
                NameIndexer idxr = (NameIndexer) map.get(indexName);
                idxr.query(null, new GcCallback(cacheDB, srdiIndexer, list, gcPeerTBL));
                srdiIndexer.purge(list);
            }
            gcPeerTBL.clear();
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failure during SRDI Garbage Collect", ex);
            }
        }
    }

    /**
     *  Remove expired entries from an ArrayList
     *
     *@param  list  The ArrayLsit
     *@return       ArrayList without any expired entries
     */
    private static ArrayList removeExpired(ArrayList list) {

        for (int i = 0; i < list.size(); i++) {
            Entry entry = (Entry) list.get(i);
            boolean expired = isExpired(entry.expiration);

            if (expired) {
                list.remove(i);
                i--;
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Entry peerid :" + entry.peerid + " Expires at :" + entry.expiration);
                    LOG.debug("Entry expired " + expired);
                }
            }
        }
        return list;
    }

    private static boolean isExpired(long expiration) {
        return (expiration < System.currentTimeMillis());
    }

    /**
     *  stop the current running thread
     */
    public synchronized void stop() {
        stop = true;
        // wakeup and die
        try {
            Thread temp = gcThread;

            if (temp != null) {
                synchronized (temp) {
                    temp.notify();
                }
            }
        } catch (Exception ignored) {// ignored
        }

        // Stop the database

        try {
            srdiIndexer.close();
            cacheDB.close();
            gcPeerTBL.clear();
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Unable to stop the Srdi Indexer", ex);
            }
        }
    }

    /**
     *  {@inheritDoc}
     *
     *  <p/>Periodic thread for GC
     */
    public void run() {
        try {
            while (!stop) {
                try {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Waiting for " + interval + "ms before garbage collection");
                    }

                    synchronized (gcThread) {
                        gcThread.wait(interval);
                    }
                } catch (InterruptedException woken) {
                    // The only reason we are woken is to stop.
                    Thread.interrupted();
                    continue;
                }

                if (stop) {
                    break;
                }

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Garbage collection started");
                }

                garbageCollect();

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Garbage collection completed");
                }
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        }
        finally {
            synchronized (this) {
                gcThread = null;
            }
        }
    }

    /**
     *  Flushes the Srdi directory for a specified group
     *  this method should only be called before initialization of a given group
     *  calling this method on a running group would have undefined results
     *
     *@param  group group context
     */
    public static void clearSrdi(PeerGroup group) {

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Clearing SRDI for " + group.getPeerGroupName());
        }

        try {
            String pgdir = null;

            if (group == null) {
                pgdir = "srdi-index";
            } else {
                pgdir = group.getPeerGroupID().getUniqueValue().toString();
            }
            File rootDir = new File(Cm.ROOTDIRBASE, pgdir);

            rootDir = new File(rootDir, "srdi");
            if (rootDir.exists()) {
                // remove it along with it's content
                String[] list = rootDir.list();

                for (int i = 0; i < list.length; i++) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Removing : " + list[i]);
                    }
                    File file = new File(rootDir, list[i]);

                    if (!file.delete()) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Unable to delete the file");
                        }
                    }
                }
                rootDir.delete();
            }
        } catch (Throwable t) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Unable to clear Srdi", t);
            }
        }
    }

    /**
     *  An entry in the index tables.
     */
    public final static class Entry {

        public PeerID peerid;
        public long expiration;

        /**
         *  Peer Pointer reference
         *
         *@param  peerid      PeerID for this entry
         *@param  expiration  the expiration for this entry
         */
        public Entry(PeerID peerid, long expiration) {
            this.peerid = peerid;
            this.expiration = expiration;
        }

        /**
        *  {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof Entry) {
                return (peerid.equals(((Entry) obj).peerid));
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return peerid.hashCode();
        }
    }
    
    /**
     *  an SrdiIndexRecord wrapper
     */
    public final static class SrdiIndexRecord {

        public Key key;
        public ArrayList list;

        /**
         *   SrdiIndex record
         *
         *@param  key   record key
         *@param  list  record entries
         */
        public SrdiIndexRecord(Key key, ArrayList list) {
            this.key = key;
            this.list = list;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (obj instanceof SrdiIndexRecord) {
                return (key.equals(((SrdiIndexRecord) obj).key));
            }
            return false;
        }

        /**
         *{@inheritDoc}
         */
        public int hashCode() {
            return key.hashCode();
        }
    }
}


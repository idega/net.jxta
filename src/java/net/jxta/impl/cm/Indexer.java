/*
 *  $Id: Indexer.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
 *
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
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: Indexer.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
 */

package net.jxta.impl.cm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.data.Record;
import net.jxta.impl.xindice.core.filer.BTreeException;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.filer.BTreeFiler;
import net.jxta.impl.xindice.core.filer.BTreeCallback;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.impl.xindice.core.indexer.NameIndexer;

public final class Indexer {

    private String dir = null;
    private String file = null;
    private HashMap indices = null;
    private final static String listFileName = "offsets";
    private BTreeFiler listDB = null;
    private boolean sync = true;

    /**
     * The Log4J debugging category.
     */
    private final static Logger LOG = Logger.getLogger(Indexer.class.getName());

    /*
     *      Indexer manages indexes to various advertisement types,
     *      and maintains a listDB which holds records that hold references
     *      to records in advertisments.tbl
     *
     *       -------          -------               /    ------- 
     *      | index | ---->> | listDB |   ------->>  -   | advDB |
     *       -------          -------               \    ------- 
     *
     */
    public Indexer() {
        indices = new HashMap();
    }

    /**
     * Creates an indexer
     * @param sync passed through to xindice to determine a lazy checkpoint or not
     *        false == lazy checkpoint
     */
    public Indexer(boolean sync) {
        indices = new HashMap();
        this.sync = sync;
    }
    public void setLocation(String dir, String file) {
        this.dir = dir;
        this.file = file;

        // upon restart, load existing indices

        File directory = new File(dir);
        File[] indexFiles = directory.listFiles(new FilenameFilter() {
                                                    public boolean accept(File parentDir, String fileName) {
                                                        return fileName.endsWith(".idx");
                                                    }
                                                }
                                               );
        for (int i=0; i < indexFiles.length; i++) {
            String indexFileName = indexFiles[i].getName();
            int dash = indexFileName.lastIndexOf("-");
            int dot = indexFileName.lastIndexOf(".idx");
            if (dot > 0 && dash > 0) {
                String name = indexFileName.substring(dash + 1, dot).trim();
                if (indices.get(name) == null) {
                    try {
                        NameIndexer indexer = new NameIndexer();
                        // location should be the same as in
                        // addToIndex below
                        indexer.setLocation(dir, file + "-" + name);
                        indexer.setSync(sync);
                        if (!indexer.open()) {
                            indexer.create();
                            indexer.open();
                        }
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Adding :" + indexFileName + " under " + name);
                        }
                        indices.put(name, indexer);
                    } catch (DBException ignore) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("Failed to create Index " + name , ignore);
                        }
                    }
                }
            }
        }
        try {
            // record pointers
            listDB = new BTreeFiler();
            listDB.setSync(sync);
            listDB.setLocation(directory.getCanonicalPath(), file + "-" + listFileName);
            if (!listDB.open()) {
                listDB.create();
                // now open it
                listDB.open();
            }
        } catch (DBException dbe) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Failed during listDB Creation", dbe);
            }
        }
        catch (IOException ie) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Failed during listDB Creation", ie);
            }
        }
    }

    public boolean open() throws DBException {
        return true;
    }

    public boolean create() throws DBException {
        return true;
    }

    public synchronized boolean close()
    throws DBException {

        if (indices != null) {
            Iterator i = indices.values().iterator();
            Iterator names = indices.keySet().iterator();
            while (i.hasNext()) {
                NameIndexer index = (NameIndexer) i.next();
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Closing Indexer :" + names.next());
                }
                index.close();
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Clearing indices HashMap");
            }
            indices.clear();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Closing listDB");
            }
            listDB.close();
        }
        return true;
    }

    /**
     * returns an iteration of index fields (attributes)
     */
    public Map getIndexers() {
        return Collections.unmodifiableMap(indices);
    }

    /**
     * returns listDB
     */
    public BTreeFiler getListDB() {
        return listDB;
    }

    private static final class EndsWithCallback implements BTreeCallback {

        private int op = IndexQuery.ANY;
        private BTreeCallback callback = null;
        private Value pattern = null;

        EndsWithCallback(int op, BTreeCallback callback, Value pattern) {
            this.op = op;
            this.callback = callback;
            this.pattern = pattern;
        }

        /**
         *  {@inheritDoc}
         **/
        public boolean indexInfo(Value val, long pos) {

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("value :" + val + " pattern :" + pattern );
            }

            switch (op) {
            case IndexQuery.EW:
                if (val.endsWith(pattern)) {
                    return callback.indexInfo(val, pos);
                }
                break;

            case IndexQuery.NEW:
                if (!val.endsWith(pattern)) {
                    return callback.indexInfo(val, pos);
                }
                break;

            case IndexQuery.BWX:
                if (val.contains(pattern)) {
                    return callback.indexInfo(val, pos);
                }
                break;

            default:
                break;

            }
            return true;
        }
    }

    public void search(IndexQuery query, String name, BTreeCallback callback)
    throws IOException, BTreeException {

        BTreeCallback cb = new SearchCallback(listDB, callback);
        if (query != null) {
            int op = query.getOperator();
            if (op == IndexQuery.EW ||
                op == IndexQuery.NEW ||
                op == IndexQuery.BWX) {

                query = new IndexQuery(IndexQuery.ANY, query.getValues());
                cb = new EndsWithCallback(op,
                                          new SearchCallback(listDB, callback),
                                          query.getValue(0));
            }
        }

        if (name == null) {
            if (indices != null) {
                Iterator i = indices.values().iterator();
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Searching all indexes");
                }
                while (i.hasNext()) {
                    NameIndexer index = (NameIndexer) i.next();
                    index.query(query, new SearchCallback(listDB, callback));
                }
                return;
            }
        } else {
            NameIndexer indexer = (NameIndexer) indices.get(name);
            if (indexer == null) {
                return;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Searching Index : "+ name);
            }
            indexer.query(query, cb);
        }
    }

    public void addToIndex(Map indexables, long pos)
    throws IOException, DBException {

        if (indexables == null) {
            return;
        }

        Iterator ni = indexables.keySet().iterator();
        while (ni.hasNext()) {
            String name = (String) ni.next();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("looking up NameIndexer : " + name);
            }
            NameIndexer indexer = (NameIndexer) indices.get(name);
            if (indexer == null) {
                indexer = new NameIndexer();
                // location should be the same as in setLocation above
                indexer.setLocation(dir, file + "-" + name);
                indexer.setSync(sync);
                if (!indexer.open()) {
                    indexer.create();
                    indexer.open();
                }
                indices.put(name, indexer);
            }

            //we need to make sure that the db key is unique from the
            //the index key to avoid value collision
            Key dbKey = new Key(name + (String) indexables.get(name));
            Key indexKey = new Key((String) indexables.get(name));
            long listPos = writeRecord(listDB, dbKey, pos);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Adding a reference at position :" + listPos +
                          " to "+ name + " index, Key: " + (String) indexables.get(name));
            }
            indexer.add(indexKey, listPos);
        }
    }

    public void removeFromIndex(Map indexables, long pos)
    throws DBException {

        Iterator ni;
        if (indexables == null) {
            ni = indices.keySet().iterator();
        } else {
            ni = indexables.keySet().iterator();
        }

        Long lpos = new Long(pos);
        while (ni.hasNext()) {
            String name = (String) ni.next();
            NameIndexer indexer = (NameIndexer) indices.get(name);
            if (indexer != null) {
                //we need to make sure that the db key is unique from the
                //the index key to avoid value collision
                Key dbKey = new Key(name + (String) indexables.get(name));
                Key indexKey = new Key((String) indexables.get(name));
                synchronized (listDB) {
                    Record record = listDB.readRecord(dbKey);
                    Set offsets = readRecord(record);
                    if (!offsets.isEmpty()) {
                        if (offsets.contains(lpos)) {
                            offsets.remove(lpos);
                            Value recordValue = new Value(toByteArray(offsets));
                            listDB.writeRecord(dbKey, recordValue);
                        }
                        if (offsets.isEmpty()) {
                            // only we can proceed to remove the entry from the index
                            listDB.deleteRecord(dbKey);
                            indexer.remove(indexKey);
                        }
                    } else {
                        // empty record purge it
                        listDB.deleteRecord(dbKey);
                    }
                }
            }
        }
    }

    /**
     * purge all index entries pointing to a certain record.
     *
     * @param list List of Long position(s) at which the record to be purged is
     * located in the main database.
     */
    public void purge(List list)
    throws IOException, BTreeException {

        IndexQuery iq = new IndexQuery(IndexQuery.ANY, "");
        Set keys = indices.keySet();
        Object[] objKeys = keys.toArray();
        for(int i=0; i<objKeys.length; i++ ){
            NameIndexer index = (NameIndexer) indices.get(objKeys[i]);
            PurgeCallback pc = new PurgeCallback(listDB, index, (String)objKeys[i], list);
            index.query(iq, pc);
        }
    }

    /**
     * purge all index entries pointing to a certain record.
     *
     * @param pos the position at which the record to be purged is
     * located in the main database.
     */
    public void purge(long pos)
    throws IOException, BTreeException {

        IndexQuery iq = new IndexQuery(IndexQuery.ANY, "");
        Set keys = indices.keySet();
        Object[] objKeys = keys.toArray();
        for(int i=0; i<objKeys.length; i++ ){
            NameIndexer index = (NameIndexer) indices.get(objKeys[i]);
            PurgeCallback pc = new PurgeCallback(listDB, 
                                                 index,
                                                 (String)objKeys[i], 
                                                 Collections.singletonList(new Long(pos)));
            index.query(iq, pc);
        }
    }

    private static final class PurgeCallback implements BTreeCallback {

        private NameIndexer indexer = null;
        private List list;
        private BTreeFiler listDB = null;
        private String indexKey = null;
        
        PurgeCallback(BTreeFiler listDB, NameIndexer indexer, String indexKey, List list) {
            this.listDB = listDB;
            this.indexer = indexer;
            this.indexKey = indexKey;
            this.list = list;
        }

        /**
         *  {@inheritDoc}
         **/
        public boolean indexInfo(Value val, long pos) {
            // Read record to determine whether there's a refrence to pos
            try {
                boolean changed;
                synchronized (listDB) {
                    Record record = listDB.readRecord(pos);
                    Set offsets = readRecord(record);
                    changed = offsets.removeAll(list);
                    if (changed) {
                        if (!offsets.isEmpty()) {
                            Value recordValue = new Value(toByteArray(offsets));
                            listDB.writeRecord(pos, recordValue);
                        } else {
                            listDB.deleteRecord(new Key(indexKey + val));
                            indexer.remove(new Key(val));
                        }
                    } else {
                        // not a match continue callback
                        return true;
                    }
                }
            } catch (DBException ignore) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("An exception occured", ignore);
                }
            }
            return true;
        }
    }

    private static byte[] toByteArray(Set offsets) {
        try {
            int size = offsets.size();
            ByteArrayOutputStream bos= new ByteArrayOutputStream((size * 8 ) + 4);
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(size);
            Iterator oi = offsets.iterator();
            while (oi.hasNext()) {
                Long lpos = (Long) oi.next();
                dos.writeLong(lpos.longValue());
            }
            dos.close();
            return bos.toByteArray();
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception during array to byte array conversion", ie);
            }
        }
        return null;
    }

    public static Set readRecord(Record record) {
        Set result = new TreeSet();
        if (record == null) {
            return result;
        }
        InputStream is = record.getValue().getInputStream();
        try {
            DataInputStream ois = new DataInputStream(is);
            int size = ois.readInt();
            for (int i=0; i < size; i++) {
                result.add(new Long(ois.readLong()));
            }
            ois.close();
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception while reading Entry", ie);
            }
        }
        return result;
    }

    private static long writeRecord(BTreeFiler listDB, Key key, long pos)
    throws DBException, IOException {

        synchronized (listDB) {
            Long lpos = new Long(pos);
            Record record = listDB.readRecord(key);
            Set offsets = readRecord(record);
            if (LOG.isEnabledFor(Level.DEBUG) && offsets != null) {
                LOG.debug("list.contains " + pos + " : " + offsets.contains(lpos));
            }

            if (!offsets.contains(lpos)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Adding a reference to record at :" + lpos);
                    LOG.debug("Writing :" + offsets.size() + " references");
                }
                offsets.add(lpos);
            }
            Value recordValue = new Value(toByteArray(offsets));
            return listDB.writeRecord(key, recordValue);
        }
    }

    public static final class SearchCallback implements BTreeCallback {

        private BTreeCallback callback = null;
        private BTreeFiler listDB = null;

        public SearchCallback (BTreeFiler listDB, BTreeCallback callback) {
            this.listDB = listDB;
            this.callback = callback;
        }

        /**
         *  {@inheritDoc}
         **/
        public boolean indexInfo(Value val, long pos) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Found " + val.toString() + " at " + pos);
            }
            Record record = null;
            Set offsets = null;
            boolean result = true;
            try {
                synchronized (listDB) {
                    record = listDB.readRecord(pos);
                    offsets = readRecord(record);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Found " + offsets.size() + " entries");
                    }
                }

                Iterator oi = offsets.iterator();
                while (oi.hasNext()) {
                    Long lpos = (Long) oi.next();
                    result &= callback.indexInfo(val, lpos.longValue());
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Callback result : "+result);
                    }
                }
            } catch (DBException ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while reading indexed", ex);
                }
                return false;
            }
            return result;
        }
    }
}

/*
 *  $Id: Cm.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
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
 *  $Id: Cm.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
 */
package net.jxta.impl.cm;


import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Vector;
import java.util.ResourceBundle;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.protocol.SrdiMessage;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

import net.jxta.impl.config.Config;
import net.jxta.impl.util.JxtaHash;
import net.jxta.impl.util.TimeUtils;

import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Record;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.filer.BTreeCallback;
import net.jxta.impl.xindice.core.filer.BTreeFiler;
import net.jxta.impl.xindice.core.filer.BTreeException;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.impl.xindice.core.indexer.NameIndexer;


/**
 * This class implements a limited document caching mechanism
 * intended to provide cache for services that have a need for cache
 * to search and exchange jxta documents.
 *
 * Only Core Services are intended to use this mechanism.
 */
public final class Cm implements Runnable {

    /**
     * Log4J Logger
     */
    private final static Logger LOG = Logger.getLogger(Cm.class.getName());

    /**
     * the name we will use for the base directory
     */
    public final static File ROOTDIRBASE = new File(Config.JXTA_HOME + "cm");

    /**
     *  adv types
     */
    private static final String[] DIRNAME = { "Peers", "Groups", "Adv", "Raw" };

    // garbage collect once an hour
    public static final long DEFAULT_GC_MAX_INTERVAL = 1 * TimeUtils.ANHOUR;

    /*
     *  expiration db
     */
    private BTreeFiler cacheDB = null;
    private Indexer indexer = null;
    private final static String databaseFileName = "advertisements";

    private boolean stop = false;

    private boolean trackDeltas = false;
    private final Map deltaMap = new HashMap(3);

    /**
     * file descriptor for the root of the cm
     */
    protected File rootDir;

    private ThreadGroup threads = null;
    private Thread gcThread = null;
    private long gcTime = 0;
    private long gcMinInterval = 1000L * 60L;
    private long gcMaxInterval = DEFAULT_GC_MAX_INTERVAL;
    private int maxInconvenienceLevel = 1000;
    private volatile int inconvenienceLevel = 0;

    /**
     * Constructor for cm
     *
     * @param  areaName        the name of the cm sub-dir to create
     * @param  enableOptimize  whether to enable indexing
     * 
     * NOTE: Default garbage interval once an hour
     */
    public Cm(String areaName, boolean enableOptimize) {
        // Default garbage collect once an hour
        this(Thread.currentThread().getThreadGroup(), areaName, enableOptimize, DEFAULT_GC_MAX_INTERVAL, false);
    }

    /**
     * Constructor for cm
     *
     * @param  areaName        the name of the cm sub-dir to create
     * @param  enableOptimize  whether to enable indexing
     * @param  gcinterval      garbage collect max interval
     * @param  trackDeltas     when true deltas are tracked 
     */
    public Cm(ThreadGroup threads,
            String areaName,
            boolean enableOptimize,
            long gcinterval,
            boolean trackDeltas) {

        this.threads = threads;

        this.trackDeltas = trackDeltas;
        this.gcMaxInterval = gcinterval;
        this.gcTime = System.currentTimeMillis() + gcMaxInterval;

        try {
            rootDir = new File(ROOTDIRBASE, areaName);
            rootDir = new File(rootDir.getAbsolutePath());
            if (!rootDir.exists()) {
                // We need to create the directory
                if (!rootDir.mkdirs()) {
                    throw new RuntimeException("Cm cannot create directory " + rootDir);
                }
            }

            /*
             * to avoid inconsistent database state, it is highly recommended that
             * checkpoint is true by default, which causes fd.sync() on every write
             * operation.  In transitory caches such as SrdiCache it makes perfect sense
             */
            boolean chkPoint = true;
            ResourceBundle jxtaRsrcs = ResourceBundle.getBundle("net.jxta.user");
            String checkpointStr = jxtaRsrcs.getString("impl.cm.defferedcheckpoint");

            if (checkpointStr != null) {
                chkPoint = (checkpointStr.equalsIgnoreCase("true")) ? false : true;
            }

            // Storage
            cacheDB = new BTreeFiler();
            // no deffered checkpoint
            cacheDB.setSync(chkPoint);
            cacheDB.setLocation(rootDir.getAbsolutePath(), databaseFileName);

            if (!cacheDB.open()) {
                cacheDB.create();
                // now open it
                cacheDB.open();
            }

            // Index
            indexer = new Indexer(chkPoint);
            indexer.setLocation(rootDir.getAbsolutePath(), databaseFileName);

            if (!indexer.open()) {
                indexer.create();
                // now open it
                indexer.open();
            }

            if (System.getProperty("net.jxta.impl.cm.index.rebuild") != null) {
                rebuildIndex();
            }
            gcThread = new Thread(threads, this, "CM GC Thread interval : " + gcMinInterval);
            gcThread.setDaemon(true);
            gcThread.start();

            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Instantiated Cm for: " + rootDir.getAbsolutePath());
            }

        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Unable to Initialize databases", de);
            }
            throw new UndeclaredThrowableException(de, "Unable to Initialize databases");
        } catch (Throwable e) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Unable to create Cm", e);
            }

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof Error) {
                throw (Error) e;
            } else {
                throw new UndeclaredThrowableException(e, "Unable to create Cm");
            }
        }
    }

    public String toString() {
        return "CM for " + rootDir.getAbsolutePath() + "[" + super.toString() + "]";
    }

    private static String getDirName(Advertisement adv) {
        if (adv instanceof PeerAdvertisement) {
            return DIRNAME[DiscoveryService.PEER];
        } else if (adv instanceof PeerGroupAdvertisement) {
            return DIRNAME[DiscoveryService.GROUP];
        }

        return DIRNAME[DiscoveryService.ADV];
    }

    /**
     * Generates a random file name using doc hashcode
     *
     * @param  doc  to hash to generate a unique name
     * @return      String a random file name
     */
    public static String createTmpName(StructuredTextDocument doc) {
        try {
            StringWriter out = new StringWriter();

            doc.sendToWriter(out);
            out.close();
            JxtaHash digester = new JxtaHash(out.toString());
            BigInteger hash = digester.getDigestInteger();

            if (hash.compareTo(BigInteger.ZERO) < 0) {
                hash = hash.negate();
            }
            String strHash = "cm" + hash.toString(16);

            return strHash;
        } catch (IOException ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception creating tmp name: ", ex);
            }
            
            throw new IllegalStateException("Could not generate name from document");
        }
    }

    /**
     * Gets the list of all the files into the given folder
     *
     * @param  dn  contains the name of the folder
     *
     * @return Vector Strings containing the name of the
     * files
     */
    public Vector getRecords(String dn, int threshold,
            Vector values, Vector expirations) {

        return getRecords(dn, threshold, values, expirations, false);
    }

    public synchronized Vector getRecords(String dn, int threshold,
            Vector values, Vector expirations,
            boolean purge) {

        Vector res = new Vector();

        if (dn == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("null directory name");
            }
            return res;
        } else {
            IndexQuery iq = new IndexQuery(IndexQuery.SW, new Value(dn));

            try {
                cacheDB.query(iq, new SearchCallback(cacheDB, indexer, res, expirations, threshold, purge));
            } catch (DBException dbe) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Exception during getRecords(): ", dbe);
                }
            } catch (IOException ie) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Exception during getRecords(): ", ie);
                }
            }
            return res;
        }
    }

    public void garbageCollect() {
        // calling getRecords is good enough since it removes
        // expired entries
        Map map = indexer.getIndexers();
        Iterator it = map.keySet().iterator();
        long t0 = 0;

        while (it != null && it.hasNext()) {
            t0 = System.currentTimeMillis();
            String indexName = (String) it.next();

            getRecords(indexName, Integer.MAX_VALUE, null, null, true);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Cm garbageCollect :" + indexName + " in :" + (System.currentTimeMillis() - t0));
            }
        }
    }

    /**
     *  Returns the relative time in milliseconds at which the file
     *  will expire.
     *
     * @param  dn  contains the name of the folder
     * @param  fn  contains the name of the file
     *
     * @return the absolute time in milliseconds at which this
     * document will expire. -1 is returned if the file is not
     * recognized or already expired.
     */
    public synchronized long getLifetime(String dn, String fn) {
        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);

            if (record == null) {
                return -1;
            }
            Long life = (Long) record.getMetaData(Record.LIFETIME);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Lifetime for :" + fn + "  " + life.toString());
            }
            if (life.longValue() < System.currentTimeMillis()) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Removing expired record :" + fn);
                }
                try {
                    remove(dn, fn);
                } catch (IOException e) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Failed to remove record", e);
                    }
                }
            }
            return TimeUtils.toRelativeTimeMillis(life.longValue());
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to remove " + dn + "/" + fn, de);
            }
            return -1;
        }
    }

    /**
     *  Returns the maximum duration in milliseconds for which this
     *  document should cached by those other than the publisher. This
     *  value is either the cache lifetime or the remaining lifetime
     *  of the document, whichever is less.
     *
     * @param  dn  contains the name of the folder
     * @param  fn  contains the name of the file
     * @return     number of milliseconds until the file expires or -1 if the
     * file is not recognized or already expired.
     */
    public synchronized long getExpirationtime(String dn, String fn) {
        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);
            long expiration = calcExpiration(record);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Expiration for :" + fn + "  " + expiration);
            }
            if (expiration < 0) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Removing expired record :" + fn);
                }
                try {
                    remove(dn, fn);
                } catch (IOException e) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Failed to remove record", e);
                    }
                }
            }
            return expiration;
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to get " + dn + "/" + fn, de);
            }
            return -1;
        }
    }

    /**
     *  Figures out expiration
     *
     * @param  record  record
     * @return         expiration in ms
     */
    private static long calcExpiration(Record record) {
        if (record == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Record is null returning expiration of -1");
            }
            return -1;
        }
        Long exp = (Long) record.getMetaData(Record.EXPIRATION);
        Long life = (Long) record.getMetaData(Record.LIFETIME);
        long expiresin = life.longValue() - System.currentTimeMillis();

        if (expiresin <= 0) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Record expired" + " lifetime   : " + life.longValue() + " expiration: " + exp.longValue() + " expires in: " + expiresin);
                LOG.debug("Record expires on :" + new Date(life.longValue()));
            }
            return -1;
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Record lifetime: " + life.longValue() + " expiration: " + exp.longValue() + " expires in: " + expiresin);
                LOG.debug("Record expires on :" + new Date(life.longValue()));
            }
            return Math.min(expiresin, exp.longValue());
        }
    }

    /**
     * Returns the inputStream of a specified file, in a specified dir
     *
     * @param  dn               directory name
     * @param  fn               file name
     * @return                  The inputStream value
     * @exception  IOException  if an I/O error occurs
     */
    public InputStream getInputStream(String dn,
            String fn) throws IOException {

        Key key = new Key(dn + "/" + fn);

        try {
            Record record = cacheDB.readRecord(key);

            if (record == null) {
                return null;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Restored record for " + key);
            }
            Value val = record.getValue();

            if (val != null) {
                return val.getInputStream();
            } else {
                return null;
            }
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to restore record for " + key, de);
            }

            IOException failure = new IOException("Failed to restore record for " + key);

            failure.initCause(de);

            throw failure;
        }
    }

    /**
     * Remove a file
     *
     * @param  dn            directory name
     * @param  fn            file name
     * @throws  IOException  if an I/O error occurs
     */
    public synchronized void remove(String dn,
            String fn)
        throws IOException {

        try {
            if (fn == null) {
                return;
            }
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);
            long removePos = cacheDB.findValue(key);

            cacheDB.deleteRecord(key);
            if (record != null) {
                try {
                    InputStream is = record.getValue().getInputStream();
                    Advertisement adv = AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
                    Map indexables = getIndexfields(adv.getIndexFields(), (StructuredDocument) adv.getDocument(MimeMediaType.XMLUTF8));

                    indexer.removeFromIndex(addKey(dn, indexables), removePos);
                    // add it to deltas to expire it in srdi
                    addDelta(dn, indexables, 0);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removed " + record);
                    }
                } catch (Exception e) {
                    // bad bits we are done
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("failed to remove " + dn + "/" + fn, e);
                    }
                }
            }
        } catch (DBException de) {
            // entry does not exist
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("failed to remove " + dn + "/" + fn);
            }
        }
    }

    /**
     * Restore a saved StructuredDocument.
     *
     * @param  dn            directory name
     * @param  fn            file name
     * @return               StructuredDocument containing the file
     * @throws  IOException  if an I/O error occurs
     * was not possible.
     */
    public StructuredDocument restore(String dn, String fn) throws IOException {

        InputStream is = getInputStream(dn, fn);

        return StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);
    }

    /**
     *  Restore an advetisement into a byte array.
     *
     * @param  dn            directory name
     * @param  fn            file name
     * @return               byte [] containing the file
     * @throws  IOException  if an I/O error occurs
     */
    public synchronized byte[] restoreBytes(String dn,
            String fn) throws IOException {

        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);

            if (record == null) {
                return null;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("restored " + record);
            }
            Value val = record.getValue();

            if (val != null) {
                return val.getData();
            } else {
                return null;
            }
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to restore " + dn + "/" + fn, de);
            }

            IOException failure = new IOException("failed to restore " + dn + "/" + fn);

            failure.initCause(de);

            throw failure;
        }
    }

    /**
     * Stores a StructuredDocument in specified dir, and file name
     *
     * @param  dn               directory name
     * @param  fn               file name
     * @param  adv              Advertisement to store
     * @exception  IOException  if an I/O error occurs
     */
    public void save(String dn,
            String fn,
            Advertisement adv) throws IOException {
        save(dn, fn, adv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.NO_EXPIRATION);
    }

    /**
     * Stores a StructuredDocument in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param  dn               directory name
     * @param  fn               file name
     * @param  adv              Advertisement to save
     * @param  lifetime         Document (local) lifetime in relative ms
     * @param  expiration       Document (global) expiration time in relative ms
     * @exception  IOException  Thrown if there is a problem saving the document.
     */
    public synchronized void save(String dn,
            String fn,
            Advertisement adv,
            long lifetime,
            long expiration)
        throws IOException {

        try {
            if (expiration < 0 || lifetime <= 0) {
                throw new IllegalArgumentException("Bad expiration or lifetime.");
            }
            StructuredDocument doc;

            try {
                doc = (StructuredDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    IOException failure = new IOException("Advertisement couldn't be saved");

                    failure.initCause(e);

                    throw failure;
                }
            }

            Key key = new Key(dn + "/" + fn);
            // save the new version
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            doc.sendToStream(baos);
            baos.close();
            Value value = new Value(baos.toByteArray());

            baos = null;
            Long oldLife = null;
            Record record = cacheDB.readRecord(key);

            if (record != null) {
                // grab the old lifetime
                oldLife = (Long) record.getMetaData(Record.LIFETIME);
            }

            long absoluteLifetime = TimeUtils.toAbsoluteTimeMillis(lifetime);

            if (oldLife != null) {
                if (absoluteLifetime < oldLife.longValue()) {
                    // make sure we don't override the original value
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug(
                                "Overriding attempt to decrease adv lifetime from : " + new Date(oldLife.longValue()) + " to :"
                                + new Date(absoluteLifetime));
                    }
                    absoluteLifetime = oldLife.longValue();
                }
            }
            // make sure expiration does not exceed lifetime
            if (expiration > lifetime) {
                expiration = lifetime;
            }
            long pos = cacheDB.writeRecord(key, value, absoluteLifetime, expiration);
            Map indexables = getIndexfields(adv.getIndexFields(), doc);
            Map keyedIdx = addKey(dn, indexables);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Indexing " + keyedIdx + " at " + pos);
            }
            indexer.addToIndex(keyedIdx, pos);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                // too noisy
                // LOG.debug("Wrote " + key + " = " + value);
                LOG.debug("Stored " + indexables + " at " + pos);
            }

            if( expiration > 0 ) {
                // Update for SRDI with our caches lifetime only if we are prepared to share the advertisement with others.
                addDelta(dn, indexables, TimeUtils.toRelativeTimeMillis(absoluteLifetime));
            }

        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration, de);
            }
            
            IOException failure = new IOException("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration);

            failure.initCause(de);

            throw failure;
        }
    }

    /**
     * Store some bytes in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param  dn               directory name
     * @param  fn               file name
     * @param  data             byte array to save
     * @param  lifetime         Document (local) lifetime in relative ms
     * @param  expiration       Document (global) expiration time in relative ms
     * @exception  IOException  Thrown if there is a problem saving the document.
     */
    public synchronized void save(String dn,
            String fn,
            byte[] data,
            long lifetime,
            long expiration)
        throws IOException {

        try {
            if (expiration < 0 || lifetime <= 0) {
                throw new IllegalArgumentException("Bad expiration or lifetime.");
            }

            Key key = new Key(dn + "/" + fn);
            Value value = new Value(data);
            Long oldLife = null;
            Record record = cacheDB.readRecord(key);

            if (record != null) {
                // grab the old lifetime
                oldLife = (Long) record.getMetaData(Record.LIFETIME);
            }
            
            // save the new version

            long absoluteLifetime = TimeUtils.toAbsoluteTimeMillis(lifetime);

            if (oldLife != null) {
                if (absoluteLifetime < oldLife.longValue()) {
                    // make sure we don't override the original value
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug(
                                "Overriding attempt to decrease adv lifetime from : " + new Date(oldLife.longValue()) + " to :"
                                + new Date(absoluteLifetime));
                    }
                    absoluteLifetime = oldLife.longValue();
                }
            }
            
            // make sure expiration does not exceed lifetime
            if (expiration > lifetime) {
                expiration = lifetime;
            }

            cacheDB.writeRecord(key, value, absoluteLifetime, expiration);
        } catch (DBException de) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration, de);
            }
            
            IOException failure = new IOException("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration);

            failure.initCause(de);

            throw failure;
        }
    }

    private static Map getIndexfields(String[] fields, StructuredDocument doc) {
        Map map = new HashMap();

        if (doc == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Null document");
            }
            return map;
        }
        if (fields == null) {
            return map;
        }
        for (int i = 0; i < fields.length; i++) {
            Enumeration en = doc.getChildren(fields[i]);

            while (en.hasMoreElements()) {
                String val = (String) ((Element) en.nextElement()).getValue();

                if (val != null) {
                    map.put(fields[i], val.toUpperCase());
                }
            }
        }
        return map;
    }

    /* adds a primary index 'dn' to indexables */
    private static Map addKey(String dn, Map map) {
        if (map == null) {
            return null;
        }
        Map tmp = new HashMap();

        if (map.size() > 0) {
            Iterator it = map.keySet().iterator();

            while (it != null && it.hasNext()) {
                String name = (String) it.next();

                tmp.put(dn + name, map.get(name));
            }
        }
        return tmp;
    }

    private static final class EntriesCallback implements BTreeCallback {

        private BTreeFiler cacheDB = null;
        private int threshold;
        private Vector results;
        private String key;

        EntriesCallback(BTreeFiler cacheDB, Vector results, String key, int threshold) {
            this.cacheDB = cacheDB;
            this.results = results;
            this.key = key;
            this.threshold = threshold;
        }

        /**
         *  {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            if (results.size() >= threshold) {
                return false;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Found " + val.toString() + " at " + pos);
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
            long exp = calcExpiration(record);

            if (exp <= 0) {
                // skip expired and private entries
                return true;
            }
            Long life = (Long) record.getMetaData(Record.LIFETIME);
            SrdiMessage.Entry entry = new SrdiMessage.Entry(key, val.toString(), (life.longValue() - System.currentTimeMillis()));

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug(" key [" + entry.key + "] value [" + entry.value + "] exp [" + entry.expiration + "]");
            }
            results.addElement(entry);
            return true;
        }
    }


    private final class SearchCallback implements BTreeCallback {

        private BTreeFiler cacheDB = null;
        private Indexer indexer = null;
        private int threshold;
        private Vector results;
        private Vector expirations;
        private boolean purge;

        SearchCallback(BTreeFiler cacheDB, Indexer indexer, Vector results, Vector expirations, int threshold) {
            this(cacheDB, indexer, results, expirations, threshold, false);
        }

        SearchCallback(BTreeFiler cacheDB, Indexer indexer, Vector results, Vector expirations, int threshold, boolean purge) {
            this.cacheDB = cacheDB;
            this.indexer = indexer;
            this.results = results;
            this.threshold = threshold;
            this.expirations = expirations;
            this.purge = purge;
        }

        /**
         *  {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            if (results.size() >= threshold) {
                return false;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Found " + val.toString() + " at " + pos);
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

            /* too noisy
             if (LOG.isEnabledFor(Level.DEBUG)) {
             LOG.debug("Search callback record " + record.toString());
             }
             */
            long exp = calcExpiration(record);

            if (exp < 0) {
                if (purge) {
                    try {
                        indexer.purge(pos);
                        cacheDB.deleteRecord(record.getKey());
                    } catch (DBException ex) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Exception while reading indexed", ex);
                        }
                    } catch (IOException ie) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Exception while reading indexed", ie);
                        }
                    }
                } else {
                    ++inconvenienceLevel;
                }
                return true;
            }

            if (expirations != null) {
                expirations.addElement(new Long(exp));
            }
            results.addElement(record.getValue().getInputStream());
            return true;
        }
    }


    private static final class removeCallback implements BTreeCallback {

        private BTreeFiler cacheDB = null;
        private Indexer indexer = null;

        removeCallback(BTreeFiler cacheDB, Indexer indexer) {
            this.cacheDB = cacheDB;
            this.indexer = indexer;
        }

        /**
         *  {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            Record record = null;

            try {
                record = cacheDB.readRecord(pos);
            } catch (DBException ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while reading record", ex);
                }
                return false;
            }
            if (record == null) {
                return true;
            }
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Removing Record at position :" + pos);
                }
                indexer.purge(pos);
                cacheDB.deleteRecord(record.getKey());
            } catch (DBException ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while reading indexed", ex);
                }
            } catch (IOException ie) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception while reading indexed", ie);
                }
            }
            return true;
        }
    }

    protected static IndexQuery getIndexQuery(String value) {

        int operator = IndexQuery.ANY;

        if (value == null) {
            return null;
        } else if (value.length() == 0 || "*".equals(value)) {
            return null;
        } else if (value.indexOf("*") < 0) {
            operator = IndexQuery.EQ;
        } else if (value.charAt(0) == '*' && value.charAt(value.length() - 1) != '*') {
            operator = IndexQuery.EW;
            value = value.substring(1, value.length());
        } else if (value.charAt(value.length() - 1) == '*' && value.charAt(0) != '*') {
            operator = IndexQuery.SW;
            value = value.substring(0, value.length() - 1);
        } else {
            operator = IndexQuery.BWX;
            value = value.substring(1, value.length() - 1);
        }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Index query operator :" + operator);
        }
        return new IndexQuery(operator, new Value(value.toUpperCase()));
    }

    /**
     * Search and recovers documents that contains at least
     * a macthing pair of tag/value.
     *
     * @param  dn         contains the name of the folder on which to 
     *                    perform the search
     * @param  value      contains the value to search on.
     * @param  attribute  attribute to search on
     * @param  threshold  threshold
     * @return            Enumeration containing of all the documents names
     */
    public synchronized Vector search(String dn, String attribute,
            String value, int threshold,
            Vector expirations) {

        Vector res = new Vector();
        IndexQuery iq = getIndexQuery(value);

        try {
            indexer.search(iq, dn + attribute, new SearchCallback(cacheDB, indexer, res, expirations, threshold));
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Exception while searching in index", ex);
            }
        }
        return res;
    }

    /**
     * returns all entries that are cached
     *
     * @param  dn  the relative dir name
     * @return     SrdiMessage.Entries
     */
    public synchronized Vector getEntries(String dn, boolean clearDeltas) {
        Vector res = new Vector();

        try {
            Map map = indexer.getIndexers();
            BTreeFiler listDB = indexer.getListDB();
            Iterator it = map.keySet().iterator();

            while (it != null && it.hasNext()) {
                String indexName = (String) it.next();

                // seperate the index name from attribute
                if (indexName.startsWith(dn)) {
                    String attr = indexName.substring((dn).length());
                    NameIndexer idxr = (NameIndexer) map.get(indexName);

                    idxr.query(null, new Indexer.SearchCallback(listDB, new EntriesCallback(cacheDB, res, attr, Integer.MAX_VALUE)));
                }
            }
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Exception while searching in index", ex);
            }
        }

        if (clearDeltas) {
            clearDeltas(dn);
        }

        return res;
    }

    /**
     * returns all entries that are added since this method was last called
     *
     * @param  dn  the relative dir name
     * @return     SrdiMessage.Entries
     */
    public synchronized Vector getDeltas(String dn) {
        Vector result = new Vector();

        List deltas = (List) deltaMap.get(dn);

        if (deltas != null) {
            result.addAll(deltas);
            deltas.clear();
        }

        return result;
    }

    private synchronized void clearDeltas(String dn) {

        List deltas = (List) deltaMap.get(dn);

        if (deltas == null) {
            return;
        }
        deltas.clear();
    }

    private synchronized void addDelta(String dn, Map indexables, long exp) {

        if (trackDeltas) {
            Iterator eachIndex = indexables.entrySet().iterator();

            if (eachIndex.hasNext()) {
                List deltas = (List) deltaMap.get(dn);

                if (deltas == null) {
                    deltas = new ArrayList();
                    deltaMap.put(dn, deltas);
                }

                while (eachIndex.hasNext()) {
                    Map.Entry anEntry = (Map.Entry) eachIndex.next();
                    String attr = (String) anEntry.getKey();
                    String value = (String) anEntry.getValue();
                    SrdiMessage.Entry entry = new SrdiMessage.Entry(attr, value, exp);

                    deltas.add(entry);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Added entry  :" + entry + " to deltas");
                    }
                }
            }
        }
    }

    public synchronized void setTrackDeltas(boolean trackDeltas) {

        this.trackDeltas = trackDeltas;

        if (!trackDeltas) {
            deltaMap.clear();
        }
    }

    /**
     * stop the cm
     */
    public synchronized void stop() {
        try {
            cacheDB.close();
            indexer.close();
            stop = true;
            notify();
        } catch (DBException ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Unable to close advertisments.tbl", ex);
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void run() {
        try {
            while (!stop) {
                try {

                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("waiting " + gcMinInterval + "ms before garbage collection");
                    }

                    wait(gcMinInterval);
                } catch (InterruptedException woken) {
                    Thread.interrupted();

                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Thread interrupted", woken);
                    }
                }

                if (stop) {
                    // if asked to stop, exit
                    break;
                }

                if ((inconvenienceLevel > maxInconvenienceLevel) || (System.currentTimeMillis() > gcTime)) {

                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Garbage collection started");
                    }

                    garbageCollect();
                    inconvenienceLevel = 0;
                    gcTime = System.currentTimeMillis() + gcMaxInterval;
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Garbage collection completed");
                    }
                }
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        }
        finally {
            gcThread = null;
        }
    }

    private synchronized void rebuildIndex()
        throws BTreeException, DBException, IOException {

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Rebuilding indices");
        }

        String pattern = "*";
        IndexQuery any = new IndexQuery(IndexQuery.ANY, pattern);

        cacheDB.query(any, new RebuildIndexCallback(cacheDB, indexer));
    }

    private static final class RebuildIndexCallback implements BTreeCallback {

        private BTreeFiler database = null;
        private Indexer index = null;

        RebuildIndexCallback(BTreeFiler database, Indexer index) {
            this.database = database;
            this.index = index;
        }

        /**
         *  {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            try {
                Record record = database.readRecord(pos);

                if (record == null) {
                    return true;
                }

                InputStream is = record.getValue().getInputStream();
                Advertisement adv = AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
                Map indexables = getIndexfields(adv.getIndexFields(), (StructuredDocument) adv.getDocument(MimeMediaType.XMLUTF8));

                String dn = getDirName(adv);
                Map keyedIdx = addKey(dn, indexables);

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Restoring index " + keyedIdx + " at " + pos);
                }
                index.addToIndex(keyedIdx, pos);
            } catch (Exception ex) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception rebuilding index  at " + pos, ex);
                }
                return true;
            }

            return true;
        }
    }
}

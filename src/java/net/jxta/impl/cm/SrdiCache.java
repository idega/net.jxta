/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
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
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *  not be used to endorse or promote products derived from this
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
 *  $Id: SrdiCache.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
 */
package net.jxta.impl.cm;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import net.jxta.impl.util.Dlink;
import net.jxta.impl.util.Dlist;
import net.jxta.impl.util.TimeUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 *  Description of the Class
 */
public class SrdiCache implements Runnable {
	private final static Logger LOG = Logger.getLogger(SrdiCache.class.getName());

	private Hashtable caches = new Hashtable();
	private long maxSize;
	private long size;
	private long interval;
	private Dlist lru;
	private boolean stop = false;


	/**
	 *  Constructor for the SrdiCache object
	 *
	 * @param  maxSize  maximum number of entries in the cache
	 */
	public SrdiCache(long maxSize) {
		this.maxSize = maxSize;
		this.size = 0;
		this.lru = new Dlist();
		if (LOG.isEnabledFor(Level.DEBUG))
			LOG.debug("SrdiCache initialized with maxSize : "+maxSize);
	}

	/**
	 *  Construct a srdiObject and starts a GC thread which runs
	 *  every "interval" milliseconds
	 *
	 * @param  maxSize  maximum number of entries in the cache
	 * @param  interval  the interval at which the gc will run in milliseconds
	 */

	public SrdiCache(long maxSize, long interval) {
		this.maxSize = maxSize;
		this.size = 0;
		this.interval = interval;
		this.lru = new Dlist();
		if (LOG.isEnabledFor(Level.DEBUG))
			LOG.debug("SrdiCache initialized with maxSize : "+maxSize);
		new Thread(this, "SrdiCache Garbage collection Thread").start();
	}

	/**
	 *  returns the maxSize of the SrdiCache object
	 *
	 * @return    The maxSize value
	 */
	public long getMaxSize() {
		return maxSize;
	}


	/**
	 *  Sets the maxSize of the SrdiCache object
	 *  only in the condition where maxSize is > size of the cache
	 *  in other words we can't shrink it past the size
	 *
	 * @param  maxSize  new size
	 * @return          new maxSize, if maxSize is less < size, this.maxSize is not 
	 *                  modified.
	 */
	public synchronized long setMaxSize(long maxSize) {
		if (maxSize > this.size) {
			this.maxSize = maxSize;
		}
		if (LOG.isEnabledFor(Level.DEBUG))
			LOG.debug("SrdiCache maxSize : "+this.maxSize);
		return this.maxSize;
	}


	/**
	 *  Gets the size attribute of the SrdiCache object
	 *
	 * @return    The number of entries in cache
	 */
	public long getSize() {
		if (LOG.isEnabledFor(Level.DEBUG))
			LOG.debug("SrdiCache size : "+size);
		return size;
	}
        /**
	 * return all primary keys
	 * @return Enumeration of primary keys
	 */
	public Enumeration getPrimaryKeys( ) {
	    return caches.keys();
	}

        /**
	 * return all primary keys
	 * @return Enumeration of secondary keys
	 */

	public Enumeration getSecondaryKeys(String primaryKey) {
	    Hashtable tbl = (Hashtable) caches.get(primaryKey);
	    if (tbl == null) {
		return null;
	    } else  return tbl.keys();
	}
	
        /**
	 * return all entries of primary, and secondary keys
	 * @return Enumeration of Entries
	 */
	public Enumeration getEntries(String primaryKey, String secondaryKey) {
	    Hashtable tbl = (Hashtable) caches.get(primaryKey);
	    if (tbl == null) {
		return null;
	    } else  return ((Vector) tbl.get(secondaryKey)).elements();
	}

	/**
	 *  add a cache entry
	 *
	 * @param  attribute   Attribute String to query on
	 * @param  value       value of the attribute string
	 * @param  path        in a specific path, if null specified search in all
	 *      paths
	 * @param  expiration  expiration associated with this entry
	 *                     relative time in milliseconds	 
	 */
	public synchronized void add(String primaryKey, String attribute, String value, Object path, long expiration, boolean sticky) {

                if (value != null) {
                    value = value.toUpperCase();
                }
		Hashtable ptbl = (Hashtable) caches.get(primaryKey);
		if (ptbl == null) {
			// create a new table for this primaryKey
			ptbl = new Hashtable();
			caches.put(primaryKey, ptbl);
		}
		
		Hashtable secondaryTbl = (Hashtable) ptbl.get(attribute);
		if (secondaryTbl == null) {
			// create a new table for this attribute
			secondaryTbl = new Hashtable();
			ptbl.put(attribute, secondaryTbl);
		}
		if (size == maxSize) {
			// purge some
			purge(0);
		}
		SrdiEntry entry = new SrdiEntry(path, expiration);
		if (!sticky) {
		    // add it to the list
		    lru.putLast(entry);
		}
                if(value != null) {
                    if (!secondaryTbl.containsKey(value)) {
			Vector vec = new Vector();
			vec.add(entry);
			secondaryTbl.put(value, vec);
                    } else {
			Vector vec = (Vector) secondaryTbl.get(value);
			if (!vec.contains(entry)) {
				vec.add(entry);
			}
                    }
                }
		size++;
	}


	/**
	 *  Determines whether this object is caching a particular key
	 *
	 * @param  primaryKey	primary key to search for
	 * @param  secondaryKey	secondardy key to search for.
	 * @return      true if the object is caching key
	 */
	public boolean containsKey(String primaryKey, String secondaryKey) {
	    	Hashtable ptbl = (Hashtable) caches.get(primaryKey);
		if (ptbl != null) {
			return caches.containsKey(secondaryKey);
		} else return false;
	}


	/**
	 *  remove a file entry from cache
	 *
	 * @param  path  relative path
	 */
	public synchronized void remove(Object path) {

		Enumeration pkeys = caches.elements();
		while (pkeys.hasMoreElements()) {
		 Enumeration caTbl= ((Hashtable)pkeys.nextElement()).elements();
		while (caTbl.hasMoreElements()) {
			Hashtable tbl = (Hashtable) caTbl.nextElement();
			// remove all entries that contain value of "path"
			Iterator values = tbl.values().iterator();
			while (values.hasNext()) {
				Vector vec = (Vector) values.next();
				for (int i = 0; i < vec.size(); i++) {
					SrdiEntry entry = (SrdiEntry) vec.elementAt(i);
					if (entry.getPath().equals(path)) {
						vec.remove(i);
						// shift to the left
						i--;
						size--;
					}
				}
			}
		}
		}
	}

	/**
	 *  Query the cache
	 *
	 * @param  attribute  Attribute String to query on
	 * @param  value      value of the attribute string
	 * @return            an enumeration of canonical paths
	 */
	public synchronized Enumeration query(String primaryKey, String attribute, String value) {

		boolean endswith = false;
		boolean startswith = false;
		boolean allvalues = false;

		if (primaryKey == null || primaryKey.length() == 0) {
			throw new IllegalArgumentException("primaryKey is mandatory");
		}

		if (value == null || value.length() == 0 ||
                    attribute == null || attribute.length() == 0) {
			allvalues = true;
		} else {
                        value = value.toUpperCase();
			if (value.charAt(0) == '*') {
				endswith = true;
				value = value.substring(1, value.length());
			}
			if (value.length() == 0) {
				allvalues = true;
			} else if (value.charAt(value.length() - 1) == '*') {
				startswith = true;
				value = value.substring(0, value.indexOf("*"));
			}
		}
		
                Hashtable ptbl = (Hashtable) caches.get(primaryKey);
		if (ptbl == null) {
		    return new Vector().elements();
                } else if (attribute == null){
                    Vector result = new Vector();
                    Enumeration each = ptbl.elements();
                    if (each.hasMoreElements()) {
                        Hashtable tb = (Hashtable)each.nextElement();
                        Enumeration vecs = tb.elements();
                        if (vecs.hasMoreElements()) {
                            Vector tmp = (Vector)vecs.nextElement();
                            addTo(result,getPaths(tmp));
                        }
                    }
                    return result.elements();
                }
		Hashtable tbl = (Hashtable) ptbl.get(attribute);
		if (tbl == null)
		    return new Vector().elements();
		
		if (allvalues) {
		        Enumeration values = tbl.elements();
			Vector result = new Vector();
			Vector tmp;
			while (values.hasMoreElements()) {
			   tmp = (Vector) values.nextElement();
			   addTo(result,getPaths(tmp));
			}

			return result.elements();
			
		} else if (!endswith && !startswith) {

			Vector res = (Vector) tbl.get(value);

			if (res != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(attribute + " Found " + res.size());
				}
				return getPaths(res);
			}
			// other cases
		} else {
			Vector result = new Vector();
			Enumeration keys = tbl.keys();
			while (keys.hasMoreElements()) {
				String val = (String) keys.nextElement();
				if (startswith && !endswith) {
					if (val.startsWith(value)) {
					    addTo(result, ((Vector) tbl.get(val)).elements());
					}
				} else if (endswith && !startswith) {
					if (val.endsWith(value)) {
					    addTo(result, ((Vector) tbl.get(val)).elements());
					}
				} else if (startswith && endswith) {
					if (val.indexOf(value) >= 0) {
					    addTo(result,((Vector) tbl.get(val)).elements());
					}
				}
			}
			return getPaths(result);
		}

		// empty vector
		return new Vector().elements();
	}
        
	/* returns vector of peerid strings 
	 *
	 */
        private Enumeration getPaths (Vector entries) {
	    Vector result = new Vector();
	    for (int i=0; i<entries.size(); i++ ) {
		SrdiEntry entry = (SrdiEntry) entries.elementAt(i);
		if(! isExpired(entry) ) {
		    result.addElement(entry.path);
		}
	    }
	    return result.elements();
	}

	/**
	 *  Adds content of one vector to another
	 *
	 * @param  to    vector where elements to be copied into
	 * @param  from  vector where elements to be copied from
	 */
	private void addTo(Vector to, Enumeration from) {

		while (from.hasMoreElements()) {
		    Object obj = from.nextElement();
                    if(!to.contains(obj)) {
			to.add(obj);
		    }
		}
	}

	/**
	 * Purges some of the cache.
	 * The entries are cleaned-up properly.
	 *
	 * @param  fraction  Description of the Parameter
	 */
	public void purge(int fraction) {
		if (size == 0) {
			return;
		}

		if (fraction == 0) {
			fraction = 1;
		}
		long nbToPurge = size / fraction;
		if (nbToPurge == 0) {
			nbToPurge = 1;
		}

		while (nbToPurge-- > 0) {
			SrdiEntry entry = (SrdiEntry) lru.next();
			if (LOG.isEnabledFor(Level.DEBUG))
				LOG.debug("SrdiCache Purging : "+entry.getPath());
			remove(entry.getPath());
			entry.unlink();
			size--;
		}
	}


	/**
	 * Empties the cache completely.
	 * The entries are abandonned to the GC.
	 */
	public void clear() {
		lru.clear();
		caches.clear();
        }
	
        public void garbageCollect() {
	        //primary key table
		Enumeration primaryKeyTbl = caches.elements();
		if (LOG.isEnabledFor(Level.DEBUG))
                          LOG.debug("SrdiCache garbage collect");
		while (primaryKeyTbl.hasMoreElements()) {
                        // secondary key table
			Hashtable attributes = (Hashtable) primaryKeyTbl.nextElement();
                        Iterator secondaryKeyTbl = attributes.values().iterator();
                        while (secondaryKeyTbl.hasNext()) {
                            Hashtable secondaryEntries = (Hashtable) secondaryKeyTbl.next();
			    // remove all expired entries
			    Iterator values = secondaryEntries.values().iterator();
			    while (values.hasNext()) {
				Vector entries = ((Vector) values.next());
				for (int i=0; i<entries.size(); i++) {
				    if (isExpired((SrdiEntry)entries.elementAt(i))) {
					entries.removeElementAt(i);
				    }
				}
			}
                        }
		}
	    
	}
	
        public void removeKey(String primaryKey, String secondaryKey) {
	    
		Hashtable caTbl = (Hashtable) caches.get(primaryKey);
		if (LOG.isEnabledFor(Level.DEBUG))
                          LOG.debug("SrdiCache removing entries of pkey ["+primaryKey+"] skey["+secondaryKey +"]" );
		caTbl.remove(secondaryKey);
		}

        private boolean isExpired(SrdiEntry entry) {
	    return (entry.expiration < System.currentTimeMillis());
	}

	/**
	 *  Description of the Class
	 */
	class SrdiEntry extends Dlink {

		private Object path;
		private long expiration;


		// The application interface.
		/**
		 * Constructor for the SrdiEntry object
		 * relative time is converted to absolute time
		 *
		 * @param  path        path 
		 * @param  expiration  expiration associated with this entry
		 *                     relative time in milliseconds
		 */
		public SrdiEntry(Object path, long expiration) {
			this.path = path;
			this.expiration = TimeUtils.toAbsoluteTimeMillis( expiration );
		}


		/**
		 *  Gets the path attribute of the SrdiEntry object
		 *
		 * @return    The path value
		 */
		public Object getPath() {
			return path;
		}


		/**
		 *  Gets the expiration attribute of the SrdiEntry object
		 *
		 * @return    The expiration value
		 */
		public long getExpiration() {
			return expiration;
		}
	}
	/**
	 *  stop the current running thread
	 */
	public synchronized void stop() {
		stop = true;
		// wakeup and die
		notify();
	}
	public synchronized void run() {
		while (!stop) {
			try {
				if (LOG.isEnabledFor(Level.DEBUG)) {
					LOG.debug("waiting for " + interval + " before garbage collection");
				}
				wait(interval);
                                if (stop) {
                                    //if asked to stop, return
                                    return;
                                }
			} catch (InterruptedException e) {
			}
			if (LOG.isEnabledFor(Level.DEBUG)) {
				LOG.debug("Garbage collection started");
			}
			garbageCollect();
			if (LOG.isEnabledFor(Level.DEBUG)) {
				LOG.debug("Garbage collection completed");
			}
		}
	    
	}

}


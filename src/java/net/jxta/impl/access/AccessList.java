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
 *  $Id: AccessList.java,v 1.1 2007/01/16 11:01:51 thomas Exp $
 */
package net.jxta.impl.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 *  PeerView ACL is described as follows <p/>
 */
public class AccessList {
    private final static String peerTag = "peer";
    private final static String nameTag = "name";
    private final static String descriptionTag = "description";
    private final static String accessTag = "access";
    private final static String grantAllTag = "grantAll";
    private final static String deny = "deny";
    private final static String grant = "grant";
    private final static String idTag = "ID";
    protected Map accessMap = new HashMap();
    String description = null;
    boolean grantAll = false;
    /**
     *  Log4J Logger
     **/
    private static final transient Logger LOG = Logger.getLogger(AccessList.class.getName());
    /**
     *  Default Constructor
     */
    public AccessList() { }
    /**
     *  Intialize access list from an InputStream
     */
    public AccessList(InputStream stream) throws IOException {
        init(stream);
    }
    
    /**
     *  Intialize access list from a file
     *  @param fromFile file to init from
     *  @throws IOException if an io error occurs
     */
    public void init(File fromFile) throws IOException {
        InputStream is = new FileInputStream(fromFile);
        init(is);
        is.close();
    }

    public void refresh(File file) {
        if (file.exists()) {
            try {
                InputStream is = new FileInputStream(file);
                refresh(is);
                is.close();
            } catch (IOException io) {
                //bad input
            }
        }
    }
    
    public void refresh(InputStream stream) throws IOException {
        AccessList tmp = new AccessList(stream);
        refresh(tmp);
    }

    
    private void init(InputStream stream) throws IOException {
        StructuredTextDocument doc = (StructuredTextDocument)
                                     StructuredDocumentFactory.newStructuredDocument(
                                         MimeMediaType.XMLUTF8, stream);
        initialize(doc);
    }

    /**
     *  Constructor for the HealthMessage object
     *
     *@param  srcID
     *@param  entries
     */
    public AccessList(Map map) {
        this.accessMap = map;
    }

    /**
     *  Construct from a StructuredDocument
     *
     *@param  root  root element
     */
    public AccessList(Element root) {
        TextElement doc = (TextElement) root;

        if (!getAdvertisementType().equals(doc.getName())) {
            throw new IllegalArgumentException("Could not construct : " +
                                               getClass().getName() + "from doc containing a " + doc.getName());
        }
        initialize(doc);
    }

    /**
     *  gets the description
     *
     *@return  the document description
     */
    public String getDescrption() {
        return description;
    }

    /**
     *  gets the description
     *
     *@return  the document description
     */
    public boolean getGrantAll() {
        return grantAll;
    }
    /**
     *  gets the description
     *
     *@return  the document description
     */
    public void setGrantAll(boolean grantAll) {
        this.grantAll = grantAll;
    }

    /**
     *  sets description
     *
     *@param  description  The new description
     */
    public void setDescrption(String description) {
        this.description = description;
    }

    /**
     *  sets the entries list
     *
     *@param  list  The new entries value
     */
    public void setEntries(Map map) {
        this.accessMap =map;
    }

    /**
     *  sets the entries list
     *
     *@param  list  The new entries value
     */
    private void refresh(AccessList acl) {
        this.accessMap.putAll(acl.accessMap);
    }

    public void add(Entry entry) {
        if (!accessMap.containsKey(entry.id)) {
            accessMap.put(entry.id, entry);
        }
    }

    public void remove(Entry entry) {
        if (accessMap.containsKey(entry.id)) {
            accessMap.remove(entry.id);
        }
    }

    public boolean isAllowed(ID id) {
        if (grantAll) {
            return true;
        } else if (accessMap.containsKey(id)) {
            Entry entry = (Entry) accessMap.get(id);
            return entry.access;
        } else {
            return false;
        }
    }

    /**
     *  gets the entries list
     *
     *@return     The entries value
     *@returns    List The List containing Entries
     */
    public Map getAccessMap() {
        return accessMap;
    }
    
    /**
     *  {@inheritDoc}
     *
     *@param  asMimeType  mime type encoding
     *@return             The document value
     */
    public Document getDocument(MimeMediaType asMimeType) {
        StructuredDocument adv = (StructuredTextDocument)
                                 StructuredDocumentFactory.newStructuredDocument(asMimeType, getAdvertisementType());
        if (adv instanceof Attributable) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }
        Element e;
        e = adv.createElement(grantAllTag, Boolean.valueOf(grantAll).toString());
         adv.appendChild(e);
        if (description != null) {
            e = adv.createElement(descriptionTag, description);
            adv.appendChild(e);
        }
        Iterator it = accessMap.values().iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (entry.id == null && entry.name == null) {
                //skip bad entries
                continue;
            }
            e = adv.createElement(peerTag, entry.id.toString());
            adv.appendChild(e);
            ((Attributable) e).addAttribute(nameTag, entry.name);
            if (entry.access) {
                ((Attributable) e).addAttribute(accessTag, grant);
            } else {
                ((Attributable) e).addAttribute(accessTag, deny);
            }
        }
        return adv;
    }

    /**
     *  Process an individual element from the document.
     *
     *@param  doc
     */
    protected void initialize(TextElement doc) {

        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            TextElement elem = (TextElement) elements.nextElement();
            if (elem.getName().equals(grantAllTag)) {
                grantAll = Boolean.getBoolean(elem.getTextValue());
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Grant all access = [ "+grantAll+" ]");
                }

            }
            if (elem.getName().equals(descriptionTag)) {
                description = elem.getTextValue();
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Loading [ "+description+" ] access list :");
                }

            }
            if (elem.getName().equals(peerTag)) {
                String name = "NA";
                Attribute nameAttr = ((Attributable) elem).getAttribute(nameTag);
                if (nameAttr != null) {
                    name = nameAttr.getValue();
                }
                String access = "grant";
                Attribute accessAttr = ((Attributable) elem).getAttribute(accessTag);
                if (accessAttr != null) {
                    access = accessAttr.getValue();
                }
                ID pid = ID.nullID;
                try {
                    URI id = new URI(elem.getTextValue());
                    pid = IDFactory.fromURI(id);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("unknown ID format in advertisement: " + elem.getTextValue());
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Id is not a known id type: " + elem.getTextValue());
                }
                boolean acl = access.toUpperCase().equals(grant.toUpperCase());
                Entry entry = new Entry(pid, name, acl);
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("Adding entry to access list :"+entry.toString());
                    }
                    accessMap.put(entry.id, entry);
            }
        }
    }

    /**
     *  returns the document string representation of this object
     *
     *@return    String representation of the of this message type
     */
    public String toString() {

        try {
            StructuredTextDocument doc =
                (StructuredTextDocument) getDocument(MimeMediaType.XMLUTF8);
            return doc.toString();
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            } else
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new UndeclaredThrowableException(e);
                }
        }
    }


    /**
     *  All messages have a type (in xml this is &#0033;doctype) which
     *  identifies the message
     *
     *@return    String "jxta:XACL"
     */
    public static String getAdvertisementType() {
        return "jxta:XACL";
    }


    /**
     *  Entries class
     */
    public final static class Entry {
        /**
         *  Entry ID entry id
         */
        public ID id;
        /**
         *  Entry name
         */
        public String name;
        /**
         *  Entry name
         */
        public boolean access;

        /**
         *  Creates a Entry with id and name
         *
         *@param  id     id
         *@param  name  node name
         */

        public Entry(ID id, String name, boolean access) {
            this.id = id;
            this.name = name;
            this.access = access;
        }

        public String toString() {
            return "[" + name + "  access = "+ access + " : "+ id.toString()+ "]";
        }
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            return id.equals( ((Entry)obj).id);
        }
        public int hashCode() {
            return id.hashCode();
        }
    }
}


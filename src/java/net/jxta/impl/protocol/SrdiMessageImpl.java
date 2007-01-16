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
 *  $Id: SrdiMessageImpl.java,v 1.1 2007/01/16 11:01:40 thomas Exp $
 */
package net.jxta.impl.protocol;


import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;

import java.io.IOException;
import java.net.URISyntaxException;

import net.jxta.document.*;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.protocol.SrdiMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;


/**
 * SrdiMessageImpl provides the SRDI message binding
 */
public class SrdiMessageImpl extends SrdiMessage {

    /**
     * The Log4J debugging category.
     */
    private final static Logger LOG = Logger.getLogger(SrdiMessageImpl.class.getName());

    /**
     *  PeerID element name
     */
    public final static String pidTag = "PID";

    /**
     *  ttl element name
     */
    public final static String ttlTag = "ttl";

    /**
     *  Entry element name
     */
    public final static String entryTag = "Entry";

    /**
     *  Primary Key element name
     */
    public final static String pKeyTag = "PKey";

    /**
     *  Secondary Key element name
     */
    public final static String sKeyTag = "SKey";

    /**
     *  Value element name
     */
    public final static String valTag = "Value";

    /**
     *  Expiration element name
     */
    public final static String expirationTag = "Expiration";

    /**
     * Construct an empty doc
     */
    public SrdiMessageImpl() {
        setTTL(0);
    }

    /**
     * Construct a doc from InputStream
     *
     *  @deprecated It's better to generate the document yourself. This method
     *  cannot deduce the mime type of the content.
     *
     * @param  stream           the underlying input stream.
     * @exception  IOException  if an I/O error occurs.
     */
    public SrdiMessageImpl(InputStream stream) throws IOException {

        // We are asked to assume that the message from which this response
        // is constructed is an XML document.
        XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, stream);

        readIt(doc);
    }

    /**
     * Construct from a StructuredDocument
     *
     * @param  root  the underlying document
     */
    public SrdiMessageImpl(Element root) {
        if (!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports XLMElement");
        }
        
        XMLElement doc = (XMLElement) root;
        
        String doctype = doc.getName();
        
        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }
        
        if (!doctype.equals(getMessageType()) && !getMessageType().equals(typedoctype)) {
            throw new IllegalArgumentException("Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        
        readIt(doc);
    }

    /**
     * Construct a msg from entries
     *
     * @param  peerid       PeerID associated with this message
     * @param  ttl          TTL
     * @param  pKey         primary key
     * @param  entries      the entries for this message
     */
    public SrdiMessageImpl(PeerID peerid,
            int ttl,
            String pKey,
            List entries) {

        setPeerID(peerid);
        setTTL(ttl);
        setPrimaryKey(pKey);
        setEntries(entries);
    }

    /**
     * Construct a msg consisting of a single entry
     *
     * @param  peerid       PeerID associated with this message
     * @param  ttl          TTL
     * @param  pKey         primary key
     * @param  key          the secondary key
     * @param  value        value for the key
     * @param  expiration   expirations for this entry
     */
    public SrdiMessageImpl(PeerID peerid,
            int ttl,
            String pKey,
            String key,
            String value,
            long expiration) {

        setPeerID(peerid);
        setTTL(ttl);
        setPrimaryKey(pKey);
        addEntry(key, value, expiration);
    }

    /**
     * Construct a doc from vectors of strings
     *
     * @param  peerid       PeerID associated with this message
     * @param  ttl          TTL
     * @param  pKey         primary key
     * @param  entries      the entries for this message
     */
    public SrdiMessageImpl(String peerid,
            int ttl,
            String pKey,
            List entries) {

        PeerID pid;

        try {
            pid = (PeerID) IDFactory.fromURI(new URI(peerid));
        } catch (URISyntaxException badID) {
            throw new IllegalArgumentException("Invalid PeerID ID in message");
        }

        setPeerID(pid);
        setTTL(ttl);
        setPrimaryKey(pKey);
        setEntries(entries);
    }

    /**
     * @param  doc
     */
    public void readIt(XMLElement doc) {

        String key = null;
        String value = null;
        long expiration = 0;

        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();

            if (elem.getName().equals(pidTag)) {
                try {
                    URI pID = new URI(elem.getTextValue());

                    setPeerID((PeerID) IDFactory.fromURI(pID));
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Invalid PeerID ID in message");
                }
                continue;
            }
            if (elem.getName().equals(pKeyTag)) {
                setPrimaryKey(elem.getTextValue());
            }
            if (elem.getName().equals(ttlTag)) {
                setTTL(Integer.parseInt(elem.getTextValue()));
            }

            if (elem.getName().equals(entryTag)) {
                Attribute keyEl = ((Attributable) elem).getAttribute(sKeyTag);

                if (keyEl == null) {
                    key = "NA";
                } else {
                    key = keyEl.getValue();
                }

                value = elem.getTextValue();
                if (null != value) {
                  Attribute expAttr = elem.getAttribute(expirationTag);

                  if (expAttr != null) {
                      String expstr = expAttr.getValue();

                      expiration = Long.parseLong(expstr);
                  } else {
                      expiration = -1;
                  }
                  
                  SrdiMessage.Entry entry = new SrdiMessage.Entry(key, value, expiration);

                  addEntry(entry);
                } else {
                  if (LOG.isEnabledFor(Level.DEBUG)) {
                     LOG.debug("SrdiMessage Entry with a Null value");
                  }
                
                }
            }
        }
    }

    /**
     * return a Document representation of this object
     *
     * @param  encodeAs
     * @return           document represtation of this object
     */
    public Document getDocument(MimeMediaType encodeAs) {

        StructuredTextDocument adv = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument(encodeAs, getMessageType());

        if (adv instanceof Attributable) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }

        Element e;
        Iterator eachEntry = getEntries().iterator();
        PeerID peerid = getPeerID();

        if (peerid != null) {
            e = adv.createElement(pidTag, peerid.toString());
            adv.appendChild(e);
        }
        if (getPrimaryKey() != null) {
            e = adv.createElement(pKeyTag, getPrimaryKey());
            adv.appendChild(e);
        }
        if (getTTL() > 0) {
            e = adv.createElement(ttlTag, Integer.toString(getTTL()));
            adv.appendChild(e);
        }

        while (eachEntry.hasNext()) {
            SrdiMessage.Entry entry = (SrdiMessage.Entry) eachEntry.next();

            if (entry.key == null && entry.value == null) {
                // skip bad entries
                continue;
            }
            e = adv.createElement(entryTag, entry.value);
            adv.appendChild(e);
            ((Attributable) e).addAttribute(expirationTag, Long.toString(entry.expiration));
            ((Attributable) e).addAttribute(sKeyTag, entry.key);
        }

        return adv;
    }

    /**
     * returns the document string representation of this object
     *
     * @return    String representation of the of this message type
     */
    public String toString() {
        
        StructuredTextDocument doc = (StructuredTextDocument) getDocument(MimeMediaType.XMLUTF8);
            
        return doc.toString();
    }
}


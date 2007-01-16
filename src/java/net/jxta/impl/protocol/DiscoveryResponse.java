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
 *  $Id: DiscoveryResponse.java,v 1.1 2007/01/16 11:01:40 thomas Exp $
 */
package net.jxta.impl.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.document.XMLDocument;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerAdvertisement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *  DiscoveryResponse.
 *
 *  <p/>This message is part of the standard JXTA Peer Discovery Protocol (PDP).
 *
 *  <pre>
 * &lt;xs:element name="DiscoveryResponse" type="jxta:DiscoveryResponse"/>
 *
 * &lt;xs:complexType name="DiscoveryResponse">
 *   &lt;xs:sequence>
 *     &lt;xs:element name="Type" type="jxta:DiscoveryQueryType"/>
 *     &lt;xs:element name="Count" type="xs:unsignedInt" minOccurs="0"/>
 *     &lt;xs:element name="Attr" type="xs:string" minOccurs="0"/>
 *     &lt;xs:element name="Value" type="xs:string" minOccurs="0"/>
 *     &lt;!-- The following should refer to a peer adv, but is instead a whole doc for historical reasons -->
 *     &lt;xs:element name="PeerAdv" minOccurs="0">
 *     &lt;xs:complexType>
 *       &lt;xs:simpleContent>
 *         &lt;xs:extension base="xs:string">
 *           &lt;xs:attribute name="Expiration" type="xs:unsignedLong"/>
 *         &lt;/xs:extension>
 *       &lt;/xs:simpleContent>
 *     &lt;/xs:complexType>
 *     &lt;/xs:element>
 *     &lt;xs:element name="Response" maxOccurs="unbounded">
 *     &lt;xs:complexType>
 *       &lt;xs:simpleContent>
 *         &lt;xs:extension base="xs:string">
 *           &lt;xs:attribute name="Expiration" type="xs:unsignedLong"/>
 *         &lt;/xs:extension>
 *       &lt;/xs:simpleContent>
 *     &lt;/xs:complexType>
 *     &lt;/xs:element>
 *   &lt;/xs:sequence>
 * &lt;/xs:complexType>
 * </pre>
 *
 *@see    net.jxta.discovery.DiscoveryService
 *@see    net.jxta.impl.discovery.DiscoveryServiceImpl
 *@see    <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-pdp" 
 *        target="_blank">JXTA Protocols Specification : Peer Discovery Protocol</a>
 */
public class DiscoveryResponse extends DiscoveryResponseMsg {

    private final static transient Logger LOG = Logger.getLogger(DiscoveryResponse.class.getName());

    private final static String countTag = "Count";
    private final static String expirationTag = "Expiration";
    private final static String peerAdvTag = "PeerAdv";
    private final static String queryAttrTag = "Attr";
    private final static String queryValueTag = "Value";
    private final static String responsesTag = "Response";
    private final static String typeTag = "Type";

    /**
     *  Constructor for new instances.
     */
    public DiscoveryResponse() {
        super();
    }

    /**
     *  Construct from a StructuredDocument
     *
     *@param  root  Description of the Parameter
     */
    public DiscoveryResponse(Element root) {

        if (!TextElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports TextElement");
        }
        TextElement doc = (TextElement) root;
        String docName = doc.getName();
        if (!getAdvertisementType().equals(docName)) {
            throw new IllegalArgumentException("Could not construct : " +
                                               getClass().getName() + " from doc containing a " +
                                               docName);
        }
        readIt(doc);
    }


    /**
     * {@inheritDoc}
     */
    public Enumeration getAdvertisements() {
        if (null == advertisements) {
            parseAdvertisements();
        }

        if ((0 == advertisements.size()) && (getPeerAdvertisement() != null) && (type == DiscoveryService.PEER)) {
            // this takes care of the case where the only response
            // is the peerAdv
            advertisements = Collections.singletonList(peerAdvertisement);
        }
        return Collections.enumeration(advertisements);
    }

    /**
     *  set the responses to the query
     *
     *@param  advs  the responses for this query
     */
    protected void setAdvertisements(List advs) {
        this.advertisements = new ArrayList(advs);
    }

    /**
     * {@inheritDoc}
     */
    public Document getDocument(MimeMediaType asMimeType) {

        StructuredTextDocument adv = (StructuredTextDocument)
                                     StructuredDocumentFactory.newStructuredDocument(asMimeType,
                                             getAdvertisementType());

        if (adv instanceof XMLDocument) {
            ((XMLDocument) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }

        Enumeration advs = getResponses();
        Enumeration exps = getExpirations();

        Element e;
        e = adv.createElement(countTag, Integer.toString(responses.size()));
        adv.appendChild(e);
        e = adv.createElement(typeTag, Integer.toString(type));
        adv.appendChild(e);

        PeerAdvertisement myPeerAdv = getPeerAdvertisement();

        if(null != myPeerAdv) {
            e = adv.createElement(peerAdvTag, myPeerAdv.toString());
            adv.appendChild(e);
        }

        if ((attr != null) && (attr.length() > 0)) {
            e = adv.createElement(queryAttrTag, getQueryAttr());
            adv.appendChild(e);
            if ((value != null) && (value.length() > 0)) {
                e = adv.createElement(queryValueTag, value);
                adv.appendChild(e);
            }
        }

        try {
            while (advs.hasMoreElements()) {
                Long l = (Long) exps.nextElement();
                Object response = advs.nextElement();

                if (response instanceof InputStream) {
                    e = adv.createElement(responsesTag, streamToString((InputStream) response));
                } else {
                    e = adv.createElement(responsesTag, response.toString());
                }
                adv.appendChild(e);
                if (adv instanceof Attributable) {
                    ((Attributable) e).addAttribute(expirationTag, l.toString());
                }
            }
        } catch (Exception failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Got an Exception during doc creation", failed);
            }
            IllegalStateException failure = new IllegalStateException("Got an Exception during doc creation");
            failure.initCause(failed);
            throw failure;
        }
        return adv;
    }

    /**
     *  Description of the Method
     */
    private void parseAdvertisements() {

        List advertisements = new ArrayList();

        Enumeration eachResponse = getResponses();

        while (eachResponse.hasMoreElements()) {
            Object response = eachResponse.nextElement();

            if (response instanceof String) {
                String str = (String) response;
                try {
                    Advertisement adv = (Advertisement)
                                        AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, new StringReader(str));
                    advertisements.add(adv);
                } catch (Exception e) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("No advertisements in response element", e);
                    }
                }
            } else
                if (response instanceof InputStream) {
                    InputStream is = (InputStream) response;
                    try {
                        Advertisement adv = (Advertisement)
                                            AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
                        advertisements.add(adv);
                    } catch (Exception e) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Can not parse Response", e);
                        }
                    }
                } else {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Can not parse Response of type " + response.getClass().getName());
                    }
                }
        }
        setAdvertisements(advertisements);
    }

    /**
     *  Parses a document into this object
     *
     *@param  doc  Document
     */
    private void readIt(TextElement doc) {
        Vector res = new Vector();
        Vector exps = new Vector();

        try {
            Enumeration elements = doc.getChildren();
            while (elements.hasMoreElements()) {
                TextElement elem = (TextElement) elements.nextElement();
                if (elem.getName().equals(typeTag)) {
                    type = Integer.parseInt(elem.getTextValue());
                    continue;
                }

                if (elem.getName().equals(peerAdvTag)) {
                    String peerString = elem.getTextValue();

                    if(null == peerString) {
                        continue;
                    }

                    peerString = peerString.trim();
                    if(peerString.length() > 0) {
                        setPeerAdvertisement((PeerAdvertisement)
                                              AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, new StringReader(elem.getTextValue())));
                    }
                    continue;
                }

                if (elem.getName().equals(queryAttrTag)) {
                    setQueryAttr(elem.getTextValue());
                    continue;
                }

                if (elem.getName().equals(queryValueTag)) {
                    setQueryValue(elem.getTextValue());
                    continue;
                }

                if (elem.getName().equals(responsesTag)) {
                    // get the response
                    String aResponse = elem.getTextValue();

                    if (null == aResponse) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Discarding an empty response tag");
                        }
                        continue;
                    }
                    res.add(aResponse);

                    long exp;
                    // get expiration associated with this response
                    if(elem instanceof Attributable) {
                        Attribute attr = ((Attributable) elem).getAttribute(expirationTag);

                        if (null != attr) {
                            exp = Long.parseLong(attr.getValue());
                        } else {
                            // if there are no attribute use DEFAULT_EXPIRATION
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Received an old-style DiscoveryResponse.\n " +
                                          "You received a response from a peer that does \n" +
                                          "not support advertisement aging. \n" +
                                          "Setting expiration to DiscoveryService.DEFAULT_EXPIRATION ");
                            }
                            exp = DiscoveryService.DEFAULT_EXPIRATION;
                        }
                    } else {
                        exp = DiscoveryService.DEFAULT_EXPIRATION;
                    }

                    exps.add(new Long(exp));
                }
            }
        } catch (Exception failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Got an Exception during Parse ", failed);
            }
            IllegalArgumentException failure = new IllegalArgumentException("Got an Exception during parse");
            failure.initCause(failed);
            throw failure;
        }
        setResponses(res);
        setExpirations(exps);
    }


    /**
     *  Reads in a stream into a string
     *
     *@param  is  inputstream
     *@return     string representation of a stream
     */
    private String streamToString(InputStream is) {
        StringBuffer stw = new StringBuffer();
        Reader reader = null;
        try {
            reader = new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("InputStreamReader creation error", uee);
            }
        }
        char[] buf = new char[512];

        try {
            do {
                int c = reader.read(buf);
                if (c == -1) {
                    break;
                }
                stw.append(buf, 0, c);
            } while (true);
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Got an Exception during stream conversion", ie);
            }
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {}
        }

        return stw.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {

        try {
            StructuredTextDocument doc = (StructuredTextDocument) getDocument(MimeMediaType.XMLUTF8);
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
}

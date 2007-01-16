/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: PeerGroupAdv.java,v 1.1 2007/01/16 11:01:41 thomas Exp $
 */

package net.jxta.impl.protocol;


import java.util.Enumeration;
import java.net.URI;
import java.util.Hashtable;

import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.id.IDFactory;
import net.jxta.document.*;


public class PeerGroupAdv extends PeerGroupAdvertisement {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(PeerGroupAdv.class.getName());
    
    private static final String nameTag = "Name";
    private static final String gidTag = "GID";
    private static final String descTag = "Desc";
    private static final String msidTag = "MSID";
    private static final String svcTag = "Svc";
    private static final String mcidTag = "MCID";
    private static final String paramTag = "Parm";
    private static final String[] fields = { nameTag, gidTag, descTag};
    
    public static class Instantiator implements AdvertisementFactory.Instantiator {
        
        /**
         *   {@inheritDoc}
         **/
        
        public String getAdvertisementType() {
            return PeerGroupAdv.getAdvertisementType();
        }
        
        /**
         *   {@inheritDoc}
         **/
        public Advertisement newInstance() {
            return new PeerGroupAdv();
        }
        
        /**
         *   {@inheritDoc}
         **/
        public Advertisement newInstance(net.jxta.document.Element root) {
            return new PeerGroupAdv(root);
        }
    }


    ;
    
    /**
     *  Use the Instantiator method to construct Peer Group Advs.
     **/
    private PeerGroupAdv() {}
    
    /**
     *  Use the Instantiator method to construct Peer Group Advs.
     **/
    private PeerGroupAdv(Element root) {
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
        
        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException("Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();
            
            if (!handleElement(elem)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Unhandled Element: " + elem.toString());
                }
            }
        }
        
        // Sanity Check!!!
        if (null == getPeerGroupID()) {
            throw new IllegalArgumentException("Peer Group Advertisement did not contain a peer group id.");
        }

        if (null == getModuleSpecID()) {
            throw new IllegalArgumentException("Peer Group Advertisement did not contain a module spec id.");
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected boolean handleElement(Element raw) {
        
        if (super.handleElement(raw)) {
            return true;
        }
        
        XMLElement elem = (XMLElement) raw;
        
        if (elem.getName().equals(nameTag)) {
            setName(elem.getTextValue());
            return true;
        }
        
        if (elem.getName().equals(descTag)) {
            setDesc(elem);
            return true;
        }
        
        if (elem.getName().equals(gidTag)) {
            try {
                URI grID = new URI(elem.getTextValue());

                setPeerGroupID((PeerGroupID) IDFactory.fromURI(grID));
            } catch (URISyntaxException badID) {
                throw new IllegalArgumentException("Bad peer group ID in advertisement: " + elem.getTextValue());
            } catch (ClassCastException badID) {
                throw new IllegalArgumentException("Id is not a group id: " + elem.getTextValue());
            }
            return true;
        }
        
        if (elem.getName().equals(msidTag)) {
            try {
                URI specID = new URI(elem.getTextValue());

                setModuleSpecID((ModuleSpecID) IDFactory.fromURI(specID));
            } catch (URISyntaxException badID) {
                throw new IllegalArgumentException("Bad msid in advertisement: " + elem.getTextValue());
            } catch (ClassCastException badID) {
                throw new IllegalArgumentException("Id is not a module spec id: " + elem.getTextValue());
            }
            return true;
        }
        
        if (elem.getName().equals(svcTag)) {
            Enumeration elems = elem.getChildren();
            String classID = null;
            Element param = null;

            while (elems.hasMoreElements()) {
                TextElement e = (TextElement) elems.nextElement();

                if (e.getName().equals(mcidTag)) {
                    classID = e.getTextValue();
                    continue;
                }
                if (e.getName().equals(paramTag)) {
                    param = e;
                    continue;
                }
            }
            if (classID != null && param != null) {
                // Add this param to the table. putServiceParam()
                // clones param into a standalone document automatically.
                // (classID gets cloned too).
                try {
                    putServiceParam(IDFactory.fromURI(new URI(classID)), param);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad mcid in advertisement: " + elem.getTextValue());
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Id is not a module class id: " + elem.getTextValue());
                }
            }
            return true;
        }
        
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Document getDocument(MimeMediaType encodeAs) {
        if (null == getPeerGroupID()) {
            throw new IllegalStateException("Peer Group Advertisement did not contain a peer group id.");
        }

        if (null == getModuleSpecID()) {
            throw new IllegalStateException("Peer Group Advertisement did not contain a module spec id.");
        }

        StructuredDocument adv = (StructuredDocument) super.getDocument(encodeAs);
        
        Element e;

        e = adv.createElement(gidTag, getPeerGroupID().toString());
        adv.appendChild(e);
        
        e = adv.createElement(msidTag, getModuleSpecID().toString());
        adv.appendChild(e);
        
        // name is optional
        if (null != getName()) {
            e = adv.createElement(nameTag, getName());
            adv.appendChild(e);
        }
        
        // desc is optional
        StructuredDocument desc = getDesc();

        if (desc != null) {
            StructuredDocumentUtils.copyElements(adv, adv, desc);
        }
        
        // FIXME: this is inefficient - we force our base class to make
        // a deep clone of the table.
        Hashtable serviceParams = getServiceParams();
        Enumeration classIds = serviceParams.keys();

        while (classIds.hasMoreElements()) {
            ModuleClassID classId = (ModuleClassID) classIds.nextElement();
            
            Element s = adv.createElement(svcTag);

            adv.appendChild(s);
            
            e = adv.createElement(mcidTag, classId.toString());
            s.appendChild(e);
            
            e = (Element) serviceParams.get(classId);
            StructuredDocumentUtils.copyElements(adv, s, e, paramTag);
            
        }
        return adv;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String[] getIndexFields() {
        return fields;
    }
}

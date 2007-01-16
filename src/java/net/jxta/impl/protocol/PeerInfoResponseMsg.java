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
 * $Id: PeerInfoResponseMsg.java,v 1.1 2007/01/16 11:01:40 thomas Exp $
 */

package net.jxta.impl.protocol;

import java.net.URISyntaxException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Hashtable;

import net.jxta.document.*;
import net.jxta.peer.PeerID;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.protocol.PeerInfoResponseMessage;

/**
 * This class implements {@link net.jxta.protocol.PeerInfoResponseMessage}.
 *
 * <p>This message is part of the Peer PeerInfoService protocol
 *
 * <pre>
 * &lt;xs:element name="<classname>PeerInfoResponse</classname>" type="jxta:PeerInfoResponse"/>
 *
 * &lt;xs:complexType name="PeerInfoResponse">
 *     &lt;xs:element name="sourcePid" type="xs:anyURI"/>
 *     &lt;xs:element name="targetPid" type="xs:anyURI"/>
 *     &lt;xs:element name="uptime" type="xs:unsignedLong" minOccurs="0"/>
 *     &lt;xs:element name="timestamp" type="xs:unsignedLong" minOccurs="0"/>
 *     &lt;xs:element name="response" type="xs:anyType" minOccurs="0"/>
 * &lt;/xs:complexType>
 * </pre>
 *
 * @since JXTA 1.0
 */
public class PeerInfoResponseMsg extends PeerInfoResponseMessage {
    
    public PeerInfoResponseMsg() {
        super();
    }
    
    /**
     *  @deprecated Please use the Advertisement factory and accessors
     **/
    public PeerInfoResponseMsg(PeerID spid, PeerID tpid, long uptime, long timestamp) {
        super();
        setSourcePid(spid);
        setTargetPid(tpid);
        setUptime(uptime);
        setTimestamp(timestamp);

    }
    
    public PeerInfoResponseMsg(Element root) {
        initialize( root );
    }
    
    public void initialize(Element root) {
        
        if( !TextElement.class.isInstance( root ) )
            throw new IllegalArgumentException( getClass().getName() + " only supports TextElement" );
        
        setSourcePid(null);
        setTargetPid(null);
        setUptime(0);
        setTimestamp(0);
        
        TextElement doc = (TextElement) root;
        
        Enumeration elements;
        elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            TextElement element = (TextElement) elements.nextElement();
            String elementName = element.getName();
            if (elementName.equals("sourcePid")) {
                try {
		    		URI peerid =  new URI( element.getTextValue() );
		    		ID id = IDFactory.fromURI( peerid );
		    		if ( !(id instanceof PeerID))
		    			throw new IllegalArgumentException( "Bad ID in advertisement, not a PeerID" );
                    setSourcePid( (PeerID) id);
                } catch ( URISyntaxException badID ) {
                    throw new IllegalArgumentException( "Bad peerid ID in advertisement" );
                }
                continue;
            }
            
            if (elementName.equals("targetPid")) {
                try {
		    URI peerid =  new URI( element.getTextValue() );
		    PeerID id = (PeerID) IDFactory.fromURI( peerid );
                    setTargetPid( id);
                 } catch ( URISyntaxException badID ) {
                    throw new IllegalArgumentException( "Bad peerid ID in advertisement" );
                } catch ( ClassCastException badID ){
 		    throw new IllegalArgumentException( "Bad ID in advertisement, not a PeerID" );                   
                }
            }
            
            if (elementName.equals("uptime")) {
                setUptime(Long.parseLong(element.getTextValue()));
                continue;
            }
            
            if (elementName.equals("timestamp")) {
                setTimestamp(Long.parseLong(element.getTextValue()));
                continue;
            }
            
            if (elementName.equals("response")) {
                Enumeration elems = element.getChildren();
                if( elems.hasMoreElements() )
                    setResponse(StructuredDocumentUtils.copyAsDocument(
                    (Element) elems.nextElement() ) );
                continue;
            }
            
            
        }
    }
    
    public Document getDocument(MimeMediaType encodeAs) {
        
        StructuredTextDocument doc = (StructuredTextDocument)
        StructuredDocumentFactory.newStructuredDocument
        (encodeAs, getMessageType());
        TextElement e = null;
        
        if( doc instanceof Attributable ) {
            ((Attributable)doc).addAttribute( "xmlns:jxta", "http://jxta.org" );
        }
        
        e = doc.createElement("sourcePid", getSourcePid().toString() );
        doc.appendChild(e);
        
        e = doc.createElement("targetPid", getTargetPid().toString() );
        doc.appendChild(e);
        
        Element response = getResponse();
        if( null != response ) {
            e = doc.createElement("response" );
            doc.appendChild(e);
            
            StructuredDocumentUtils.copyElements( doc, e, response );
        }
        
        e = doc.createElement("uptime", String.valueOf(getUptime()));
        doc.appendChild(e);
        
        e = doc.createElement("timestamp",
        String.valueOf(getTimestamp()));
        doc.appendChild(e);
               
        return doc;
    }
    
}


/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
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
 * $Id: RouteResponse.java,v 1.1 2007/01/16 11:01:39 thomas Exp $
 */

package net.jxta.impl.protocol;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Enumeration;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLElement;

import net.jxta.protocol.RouteAdvertisement;
import net.jxta.protocol.RouteResponseMsg;

/**
 * Used by the Endpoint Routing protocol in response to Route Query Messages. 
 * The Route Response Message contains a route advertisement for the destination 
 * peer. 
 *
 * <p/><pre>
 * &lt;xs:complexType name="ERR">
 *   &lt;xs:sequence>
 *     &lt;xs:element name="Dst">
 *       &lt;xs:complexType>
 *         &lt;xs:sequence>
 *           &lt;xs:element ref="jxta:RA" />
 *         &lt;/xs:sequence>
 *       &lt;/xs:complexType>
 *     &lt;/xs:element>
 *     &lt;xs:element name="Src">
 *       &lt;xs:complexType>
 *         &lt;xs:sequence>
 *           &lt;xs:element ref="jxta:RA" />
 *         &lt;/xs:sequence>
 *       &lt;/xs:complexType>
 *     &lt;/xs:element>
 *   &lt;/xs:sequence>
 * &lt;/xs:complexType>
 * </pre>
 *
 * @see    net.jxta.impl.endpoint.router.EndpointRouter
 * @see    <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-erp" 
 *         target="_blank">JXTA Protocols Specification : Endpoint Routing Protocol</a>
 */
public class RouteResponse extends RouteResponseMsg {
    
    private static final String destRouteTag = "Dst";
    private static final String srcRouteTag = "Src";
    
    /**
     * Construct a new Route Response Message
     **/
    public RouteResponse() {
    }
    
    /**
     * Construct from a StructuredDocument
     */
    public RouteResponse( Element root ) {
        
        if( !XMLElement.class.isInstance( root ) )
            throw new IllegalArgumentException( getClass().getName() + " only supports XMLElement" );
        
        XMLElement doc = (XMLElement) root;

        String doctype = doc.getName();
        
        String typedoctype = "";
        Attribute itsType = doc.getAttribute( "type" );
        if( null != itsType )
            typedoctype = itsType.getValue();
        
        if( !doctype.equals(getAdvertisementType()) &&
        !(doctype.equals(super.getAdvertisementType()) && getAdvertisementType().equals(typedoctype)) )
            throw new IllegalArgumentException( "Could not construct : "
            + getClass().getName() + "from doc containing a " + doc.getName() );
        
        readIt( doc );
    }
    
    private void readIt( XMLElement doc ) {
        
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();
            
            if( elem.getName().equals(destRouteTag)) {
                for( Enumeration eachXpt = elem.getChildren();
                eachXpt.hasMoreElements(); ) {
                    XMLElement aXpt = (XMLElement) eachXpt.nextElement();
                    
                    RouteAdvertisement route = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(aXpt);
                    setDestRoute(route);
                }
                continue;
            }
            
            if (elem.getName().equals(srcRouteTag)) {
                for( Enumeration eachXpt = elem.getChildren();
                eachXpt.hasMoreElements(); ) {
                    XMLElement aXpt = (XMLElement) eachXpt.nextElement();
                    
                    RouteAdvertisement route = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(aXpt);
                    setSrcRoute(route);
                }
                continue;
            }
        }
    }
    
    /**
     *  return a Document represetation of this object
     */
    public Document getDocument(MimeMediaType asMimeType) {
        
        StructuredDocument adv = (StructuredTextDocument)
        StructuredDocumentFactory.newStructuredDocument( asMimeType,
        getAdvertisementType() );
        
        if( adv instanceof Attributable ) {
            ((Attributable)adv).addAttribute( "xmlns:jxta", "http://jxta.org" );
        }
        
        Element e;
        
        RouteAdvertisement route = getDestRoute();
        if (route != null) {
            e = adv.createElement(destRouteTag);
            adv.appendChild(e);
            StructuredTextDocument xptDoc = (StructuredTextDocument)
            route.getDocument( asMimeType );
            StructuredDocumentUtils.copyElements( adv, e, xptDoc );
        }
        
        route = getSrcRoute();
        if (route != null) {
            e = adv.createElement(srcRouteTag);
            adv.appendChild(e);
            StructuredTextDocument xptDoc = (StructuredTextDocument)
            route.getDocument( asMimeType );
            StructuredDocumentUtils.copyElements( adv, e, xptDoc );
        }
        return adv;
    }
    
    /**
     * return a string representaion of this RouteResponse doc
     *
     */
    public String toString() {
        
        try {
            StructuredTextDocument doc =
            (StructuredTextDocument) getDocument( MimeMediaType.XMLUTF8 );
            
            return doc.toString();
        } catch( Throwable e ) {
            if( e instanceof Error ) {
                throw (Error) e;
            } else if( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else {
                throw new UndeclaredThrowableException( e );
            }
        }
    }
}

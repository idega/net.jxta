/*
 * $Id: LimitedRangeRdvMsg.java,v 1.1 2007/01/16 11:01:41 thomas Exp $
 ********************
 *
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 ********************
 */

package net.jxta.impl.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import net.jxta.document.*;
import net.jxta.id.ID;
import net.jxta.protocol.LimitedRangeRdvMessage;


/**
 * This class implements the LimitedRangeRdvMessage abstract class
 *
 * <pre>
 *      &lt;?xml version="1.0" standalone='yes'?&gt;
 *       &lt;jxta:LimitedRangeRdvMessage&gt;
 *        &lt;TTL&gt;int&lt;/TTL&gt;
 *        &lt;Dir&gt;int&lt;/Dir&t;
 *        &lt;SrcSvcName&gt;string&lt;/SrcSvcName&gt;
 *        &lt;SrcSvcParams&gt;string&lt;/SrcSvcParams&gt;
 *        &lt;SrcPeerID&gt;string&lt;/SrcPeerID&gt;
 *        &lt;SrcRoute&gt;string&lt;/SrcRoute&gt;
 *       &lt;/jxta:LimitedRangeRdvMessage&gt;
 * </pre>
 *
 */
public class LimitedRangeRdvMsg extends LimitedRangeRdvMessage {
    
    /**
     * Construct from a StructuredDocument
     *
     */
    
    public LimitedRangeRdvMsg() {
    }
        
    public LimitedRangeRdvMsg(Element root) {
        initialize( root );
    }
    
    /**
     * Do not know what ID to return. We should come up with a special
     * purpose one.
     */

    public ID getID() {
        return ID.nullID;
    }

    protected void initialize(Element root) {
        
        if( !TextElement.class.isInstance( root ) )
            throw new IllegalArgumentException( getClass().getName() + " only supports TextElement" );
        
        TextElement doc = (TextElement) root;
        
        if( ! doc.getName().equals( getAdvertisementType() ))
            throw new IllegalArgumentException( "Could not construct : "
            + getClass().getName()
            + " from doc containing a '"
            + doc.getName() + "'. Should be : " + getAdvertisementType() );
     
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            TextElement elem = (TextElement) elements.nextElement();


            if( elem.getName().equals(LimitedRangeRdvMessage.TTLTag)) {
                setTTL( Integer.parseInt( elem.getTextValue() ) );
                continue;
            }

            if( elem.getName().equals(LimitedRangeRdvMessage.DirTag)) {
                setDirection( Integer.parseInt( elem.getTextValue() ) );
                continue;
            }


            if (elem.getName().equals(LimitedRangeRdvMessage.SrcRouteAdvTag)) {
                setSrcRouteAdv (elem.getTextValue());
                continue;
            }

            if (elem.getName().equals(LimitedRangeRdvMessage.SrcPeerIDTag)) {
                setSrcPeerID (elem.getTextValue());
                continue;
            }

            if (elem.getName().equals(LimitedRangeRdvMessage.SrcSvcNameTag)) {
                setSrcSvcName (elem.getTextValue());
                continue;
            }

            if (elem.getName().equals(LimitedRangeRdvMessage.SrcSvcParamsTag)) {
                setSrcSvcParams (elem.getTextValue());
                continue;
            }

        }
    }
    
    /**
     *  @inheritDoc
     **/
    public Document getDocument( MimeMediaType mediaType ) {
        
        StructuredTextDocument adv = (StructuredTextDocument)
        StructuredDocumentFactory.newStructuredDocument(
        mediaType, getAdvertisementType() );
        
        if( adv instanceof Attributable ) {
            ((Attributable)adv).addAttribute( "xmlns:jxta", "http://jxta.org" );
        }
        
        TextElement e = null;

	e = adv.createElement(LimitedRangeRdvMessage.TTLTag, Integer.toString(getTTL()));
	adv.appendChild(e);

	e = adv.createElement(LimitedRangeRdvMessage.DirTag, Integer.toString(getDirection()));
	adv.appendChild(e);

        if (getSrcPeerID() != null) {
            e = adv.createElement(LimitedRangeRdvMessage.SrcPeerIDTag, getSrcPeerID());
            adv.appendChild(e);
        }

        if (getSrcSvcName() != null) {
            e = adv.createElement(LimitedRangeRdvMessage.SrcSvcNameTag, getSrcSvcName());
            adv.appendChild(e);
        }

        if (getSrcSvcParams() != null) {
            e = adv.createElement(LimitedRangeRdvMessage.SrcSvcParamsTag, getSrcSvcParams());
            adv.appendChild(e);
        }

        if (getSrcRouteAdv() != null) {
            e = adv.createElement(LimitedRangeRdvMessage.SrcRouteAdvTag, getSrcRouteAdv());
            adv.appendChild(e);
        }

        return adv;
    }
}




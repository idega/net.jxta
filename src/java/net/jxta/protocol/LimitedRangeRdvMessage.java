/*
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights
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
 * $Id: LimitedRangeRdvMessage.java,v 1.1 2007/01/16 11:01:34 thomas Exp $
 */

package net.jxta.protocol;

import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;

/**
 * This class defines the Limited Range Rendezvous message.
 *
 * @since JXTA 2.0
 */

public abstract class LimitedRangeRdvMessage {

    public static final String Name = "LimitedRangeRdvMessage";

    public static final String TTLTag = "TTL";
    public static final String DirTag = "Dir";
    public static final String SrcSvcNameTag = "SrcSvcName";
    public static final String SrcSvcParamsTag = "SrcSvcParams";
    public static final String SrcPeerIDTag = "SrcPeerID";
    public static final String SrcRouteAdvTag = "SrcRouteAdv";


    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int BOTH = 3;

    private int ttl=0;
    private int dir=0;
    private String srcSvcName=null;
    private String srcSvcParams=null;
    private String srcPeerID =null;
    private String srcRouteAdv=null;

  /**
   * All messages have a type (in xml this is !doctype)
   * which identifies the message
   *    
   * @return String "jxta:LimitedRangeRdvMessage"
   */

    public static String getAdvertisementType() {
        return "jxta:LimitedRangeRdvMessage" ;
    }

  /**
   * Get the TTL
   *    
   * @return int Time To Live
   */
    public int getTTL() {
	return ttl;
    }

  /**
   * set the TTL
   *    
   * @param ttl TTL
   */
    public void setTTL(int ttl) {
      this.ttl = ttl;
    }


  /**
   * Get the direction the message must take
   *    
   * @return int  UP, DOWN or BOTH
   */

    public int getDirection() {
      return dir;
    }

  /**
   * set the Direction of the message
   *    
   * @param dir direction
   */
    public void setDirection(int dir) {
      this.dir = dir;
    }

  /**
   * Get the Source Service Name (listening for the response)
   *    
   * @return String Source Service Name
   */
    public String getSrcSvcName () {
      return srcSvcName;
    }

  /**
   * set the Source Service Name (listening for the response)
   *    
   * @param srcSvcName Source Service Name
   */
    public void setSrcSvcName (String srcSvcName) {
      this.srcSvcName = srcSvcName;
    }

  /**
   * Get the Source Service Param (listening for the response)
   *    
   * @return String Source Service Param
   */
    public String getSrcSvcParams () {
      return srcSvcParams;
    }

  /**
   * set the Source Service Params (listening for the response)
   *    
   * @param srcSvcParams Source Service Params
   */
    public void setSrcSvcParams (String srcSvcParams) {
      this.srcSvcParams = srcSvcParams;
    }

  /**
   * Get the Source PeerID
   *    
   * @return String Source PeerID
   */
    public String getSrcPeerID () {
      return srcPeerID;
    }

  /**
   * set the Source PeerID
   *    
   * @param srcPeerID Source PeerID
   */
    public void setSrcPeerID (String srcPeerID) {
      this.srcPeerID = srcPeerID;
    }

  /**
   * Get the Source RouteAdvertisement (listening for the response)
   *    
   * @return String Source RouteAdvertisement
   */
    public String getSrcRouteAdv () {
      return srcRouteAdv;
    }

  /**
   * set the Source Route Advertisement (listening for the response)
   *    
   * @param srcRouteAdv Source Route Advertisement
   */
    public void setSrcRouteAdv (String srcRouteAdv) {
      this.srcRouteAdv = srcRouteAdv;
    }


  /**
   * Write advertisement into a document. asMimeType is a mime media-type
   * specification and provides the form of the document which is being
   * requested. Two standard document forms are defined. "text/text" encodes
   * the document in a form nice for printing out, and "text/xml" which
   * provides an XML representation.
   *
   * @param asMimeType mime-type format requested
   * @return Document representation of the document as an advertisement
   */
    public abstract Document getDocument ( MimeMediaType asMimeType );

}

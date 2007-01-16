/*
 *  Copyright(c) 2001 Sun Microsystems, Inc.  All rights
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
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES(INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
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
 *  $Id: DiscoveryResponseMsg.java,v 1.1 2007/01/16 11:01:33 thomas Exp $
 */
package net.jxta.protocol;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import java.io.IOException;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;

/**
 *  This class defines the DiscoveryService message "Response". <p/>
 *
 *  The default behavior of this abstract class is simply a place holder for the
 *  generic resolver query fields. This message is the response to the
 *  DiscoveryQueryMsg.
 *
 *@see    net.jxta.discovery.DiscoveryService
 *@see    net.jxta.protocol.DiscoveryQueryMsg
 */
public abstract class DiscoveryResponseMsg {

    /**
     *  attribute used by the query
     */
    protected String attr = null;

    /**
     *  Responding peer's advertisement
     */
    protected PeerAdvertisement peerAdvertisement = null;

    /**
     *  <p/><ul>
     *      <li>values are {@link java.lang.String} or {@link java.io.InputStream}</li>
     *  </ul>
     */
    protected List responses = Collections.EMPTY_LIST;

    /**
     *  <p/><ul>
     *      <li>values are {@link net.jxta.document.Advertisement}</li>
     *  </ul>
     */
    protected List advertisements = null;

    /**
     *  Expirations
     */
    protected List expirations = Collections.EMPTY_LIST;

    /**
     *  Advertisement type used by the query
     *
     *  <p/>FIXME 20040514 bondolo@jxta.org not a great default...
     */
    protected int type = DiscoveryService.PEER;

    /**
     *  Value used by the query
     */
    protected String value = null;

    /**
     *  All messages have a type(in xml this is !doctype) which identifies the
     *  message
     *
     * @return    String "jxta:ResolverResponse"
     */
    public static String getAdvertisementType() {
        return "jxta:DiscoveryResponse";
    }

    /**
     *  returns the response advertisement objects
     *
     *@return    Enumeration of Advertisements responses
     */
    public abstract Enumeration getAdvertisements();

    /**
     *  Get the response type
     *
     *@return int type of discovery message PEER, GROUP or ADV discovery type
     *      response
     */
    public int getDiscoveryType() {
        return type;
    }

    /**
     *  set the Response type whether it's peer, or group discovery
     *
     *@param  type  int representing the type
     */
    public void setDiscoveryType(int type) {
        this.type = type;
    }

    /**
     *  Write advertisement into a document. asMimeType is a mime media-type
     *  specification and provides the form of the document which is being
     *  requested. Two standard document forms are defined. "text/text" encodes
     *  the document in a form nice for printing out and "text/xml" which
     *  provides an XML format.
     *
     *@param  asMimeType  mime-type requested
     *@return             Document document that represents the advertisement
     */

    public abstract Document getDocument(MimeMediaType asMimeType);

    /**
     *  returns the responding peer's advertisement
     *
     *@return the Peer's advertisement
     */
    public PeerAdvertisement getPeerAdvertisement() {
        return peerAdvertisement;
    }

    /**
     *  Sets the responding peer's advertisement
     *
     *@param newAdv the responding Peer's advertisement
     */
    public void setPeerAdvertisement(PeerAdvertisement newAdv) {
        peerAdvertisement = newAdv;
    }

    /**
     *  returns the attributes used by the query
     *
     *@return    String attribute of the query
     */
    public String getQueryAttr() {
        return attr;
    }

    /**
     *  returns the value used by the query
     *
     *@return    String value used by the query
     */
    public String getQueryValue() {
        return value;
    }

    /**
     *  Get the response count
     *
     *@return    int count
     */
    public int getResponseCount() {
        if((0 == responses.size()) &&(peerAdvertisement != null) &&(type == DiscoveryService.PEER)) {
            return 1;
        } else {
            return responses.size();
        }
    }

    /**
     *  Gets the expirations attribute of the DiscoveryResponseMsg object
     *
     *@return    The expirations value
     */
    public Enumeration getExpirations() {
        if((0 == expirations.size()) &&(peerAdvertisement != null) &&(type == DiscoveryService.PEER)) {
            // this takes care of the case where the only response is the peerAdv
            expirations = Collections.singletonList(new Long(DiscoveryService.DEFAULT_EXPIRATION));
        }

        return Collections.enumeration(expirations);
    }

    /**
     *  set the expirations for this query
     *
     *@param  expirations  the expirations for this query
     */
    public void setExpirations(Vector expirations) {
        this.expirations = new ArrayList(expirations);
    }

    /**
     *  returns the response(s)
     *
     *@return    Enumeration of String responses
     */
    public Enumeration getResponses() {
        if((0 == responses.size()) &&(peerAdvertisement != null) &&(type == DiscoveryService.PEER)) {
            // this takes care of the case where the only response is the peerAdv
            responses = Collections.singletonList(peerAdvertisement.toString());
        }

        return Collections.enumeration(responses);
    }

    /**
     *  set the responses to the query
     *
     *@param  responses
     */
    public void setResponses(Vector responses) {
        this.responses = new ArrayList(responses);
    }

    /**
     *  set the attribute used by the query
     *
     *@param  attr
     */
    public void setQueryAttr(String attr) {
        this.attr = attr;
    }


    /**
     *  set the value used by the query
     *
     *@param  value
     */
    public void setQueryValue(String value) {
        this.value = value;
    }
}

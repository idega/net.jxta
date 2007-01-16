/*
 *
 * $Id: PeerGroupBinaryID.java,v 1.1 2007/01/16 11:02:06 thomas Exp $
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 */
package net.jxta.impl.id.binaryID;

import java.net.URL;

/**
 * This class implements a PeerGroup ID. Each peer group is assigned a unique
 * peer id.BinaryID id are used to implement peer group id. Because this id is
 * built with BinaryID, pulling the parent group requires a little work. The
 * parent group is the first id, with the second following, seperated by a
 * dash '-' character.<p>
 *
 * @author Daniel Brookshier <a HREF="mailto:turbogeek@cluck.com">turbogeek@cluck.com</a>
 *
 * @see net.jxta.id.ID
 * @see net.jxta.id.IDFactory
 * @see net.jxta.impl.id.BinaryID.BinaryID
 * @see net.jxta.impl.id.BinaryID.BinaryIDFactory
 * @see net.jxta.peergroup.PeerGroupID
 */
public final class PeerGroupBinaryID extends net.jxta.peergroup.PeerGroupID {
    /** LOG object for this class. */
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PeerGroupBinaryID.class.getName()); 
    
    /** This is the id string used in the XML of the id. The format is TX0..Xn where T is the type and X0 through Xn are the base64 encoded id.*/
    protected String id;
    
    /**
     * Constructor for creating a new PeerGroupID with a unique ID and a parent.<p>
     *
     * Note that only the ID for the parent is obtained and not the
     * parent and the grandparent.
     *
     * @param parent Parent peer group.
     * @param data data byte array to be used as the id.
     * @param lengthIncluded If true, the first byte in the data array is the length of the remaining bytes.
     */
    public PeerGroupBinaryID(net.jxta.peergroup.PeerGroupID parent, byte[] data, boolean lengthIncluded) {
        this();
        
        String parentStr = IDFormat.childGroup(parent); 
        if (parentStr != null){
            id = BinaryIDFactory.newBinaryID(BinaryID.flagPeerGroupID, data,lengthIncluded).getID() +"."+parentStr.replace('-','.');
        }else{
            id = BinaryIDFactory.newBinaryID(BinaryID.flagPeerGroupID, data,lengthIncluded).getID();
        }
    }
    /**
     * Creates a ID from a string. Note that the ID is not currently validated.
     * @param id Value of ID.
     */
    
    protected PeerGroupBinaryID(String id){
        super();
        this.id = id;
    }
    /**
     * Constructor for creating a new PeerGroupID with a unique ID and a parent.
     *
     * @param data DOCUMENT ME!
     * @param lengthIncluded DOCUMENT ME!
     */
    public PeerGroupBinaryID(byte[] data, boolean lengthIncluded) {
        this();
        id = BinaryIDFactory.newBinaryID(BinaryID.flagPeerGroupID, data, lengthIncluded).getID();
    }
    
    /**
     * Constructor for creating a new PeerGroupID. Note that this creates an
     * invalid ID but is required for serialization.
     */
    public PeerGroupBinaryID() {
        super();
    }
    /**
     * Constructor. Intializes contents from provided ID. This PeerGroupID has
     * no parent.
     *
     * @param id    the ID data
     */
    public PeerGroupBinaryID(BinaryID id) {
        super();
        this.id = id.getID();
    }
    
 
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object target) {
        boolean result = false;
        if (this == target) {
            result = true;
        } else if ((id != null) && target instanceof PeerGroupBinaryID &&  getUniqueValue().equals(((PeerGroupBinaryID) target).getUniqueValue())) {
            result = true;
        } else if(target instanceof String && getURL( ).toString().equals(target)) {
            result = true;
        } else if (id == null && target.equals(net.jxta.id.ID.nullID) ){
            result = true;
        }
        //LOG.error("result:"+result+" type:"+target.getClass().getName()+" getUniqueValue()="+getUniqueValue(),new RuntimeException());
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getUniqueValue().hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getUniqueValue() {
        return getIDFormat() + "-" + id;
    }
    
    /**
     * {@inheritDoc}
     */
    public URL getURL() {
        return IDFormat.getURL((String) getUniqueValue());
    }
    
    /**
     * {@inheritDoc}
     */
    public net.jxta.id.ID getPeerGroupID() {
        // convert to the generic world PGID as necessary
        return IDFormat.translateToWellKnown(this);
    }
    /**
     * {@inheritDoc}
     */
    public  net.jxta.peergroup.PeerGroupID getParentPeerGroupID() {
        net.jxta.peergroup.PeerGroupID result = null;
        try{
            if (id == null){
                result = (net.jxta.peergroup.PeerGroupID)net.jxta.id.ID.nullID;
            }
            String idd = id;
            int parentStart = idd.indexOf('.');
            if (parentStart != -1) {
                idd = idd.substring(parentStart + 1);
            } else {
                result = null;
            }
            URL url = new URL("urn:jxta:"+idd.replace('.','-'));
            net.jxta.peergroup.PeerGroupID peerGroupID = (net.jxta.peergroup.PeerGroupID)net.jxta.id.IDFactory.fromURL(url);
            result =  (net.jxta.peergroup.PeerGroupID)IDFormat.translateToWellKnown(peerGroupID);
        }catch(Exception e ){
            if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
                LOG.warn("cannot convert sub group. ID value = "+id);
            }
            result = null;
            
        }
        //LOG.error("getParentPeerGroupID():"+result);
        return result;
    }
    /**
     * returns the coded ID without the binaryid tag.
     *
     * @return The coded ID without the binaryid tag.
     */
    protected String getID() {
        return id;
    }
}
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: PeerGroupID.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
 */
package net.jxta.peergroup;

import java.net.URI;
import java.net.URL;

import java.net.MalformedURLException;

import net.jxta.id.ID;
import net.jxta.id.IDFactory;

/**
 *  This class implements a PeerGroup ID. Each peer group is assigned a
 *  unique id.
 *
 *  @see         net.jxta.id.ID
 *  @see         net.jxta.id.IDFactory
 *  @see         net.jxta.peer.PeerID
 *
 * @since JXTA 1.0
 */
public abstract class PeerGroupID extends ID {
    
    /**
     * The well known Unique Identifier of the world peergroup.
     * This is a singleton within the scope of a VM.
     **/
    public final static PeerGroupID worldPeerGroupID = new WorldPeerGroupID();
    
    /**
     * The well known Unique Identifier of the net peergroup.
     * This is a singleton within the scope of this VM.
     **/
    public final static PeerGroupID defaultNetPeerGroupID = new NetPeerGroupID();
    
    /**
     *  Returns the parent peer group id of this peer group id, if any.
     *
     *  @return the id of the parent peergroup or null if this group has no
     *  parent group.
     **/
    public abstract PeerGroupID getParentPeerGroupID();
}

final class WorldPeerGroupID extends PeerGroupID {
    
    /**
     * The name associated with this ID Format.
     */
    final static  String JXTAFormat = "jxta";
    
    private static final String UNIQUEVALUE = "WorldGroup";
    
    /**
     *  WorldPeerGroupID is not intended to be constructed. You should use the 
     *  {@link PeerGroupID.worldPeerGroupID} constant instead.
     **/
    WorldPeerGroupID() {
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        return (this == target);   // worldPeerGroupID is only itself.
    }
    
    /**
     * deserialization has to point back to the singleton in this VM
     */
    private Object readResolve() {
        return PeerGroupID.worldPeerGroupID;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return JXTAFormat;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Object getUniqueValue() {
        return getIDFormat() + "-" + UNIQUEVALUE;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroupID getParentPeerGroupID() {
        return null;
    }
}

final class NetPeerGroupID extends PeerGroupID {
    /**
     * The name associated with this ID Format.
     */
    final static  String JXTAFormat = "jxta";
    
    private static final String UNIQUEVALUE = "NetGroup";
    
    /**
     *  NetPeerGroupID is not intended to be constructed. You should use the 
     *  {@link PeerGroupID.defaultNetPeerGroupID} constant instead.
     **/
    NetPeerGroupID() {
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        return (this == target);   // netPeerGroupID is only itself.
    }
    
    /**
     * deserialization has to point back to the singleton in this VM
     */
    private Object readResolve() {
        return PeerGroupID.defaultNetPeerGroupID;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return JXTAFormat;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Object getUniqueValue() {
        return getIDFormat() + "-" + UNIQUEVALUE;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroupID getParentPeerGroupID() {
        return PeerGroupID.worldPeerGroupID;
    }
}

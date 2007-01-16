/*
 *
 * $Id: CodatID.java,v 1.1 2007/01/16 11:02:00 thomas Exp $
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

package net.jxta.impl.id.CBID;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;

import java.io.IOException;
import java.security.DigestException;
import java.security.ProviderException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.document.Document;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;

/**
 *  An implementation of the {@link net.jxta.codat.CodatID} ID Type.
 **/
public class CodatID extends net.jxta.impl.id.UUID.CodatID {
    
    /**
     *  Log4J Logger
     **/
    private static final transient Logger LOG = Logger.getLogger( CodatID.class.getName() );
    
    /**
     * Internal constructor
     **/
    protected CodatID() {
        super();
    }
    
    /**
     * Intializes contents from provided bytes.
     *
     * @param id    the ID data
     **/
    protected CodatID( IDBytes id ) {
        super( id );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID)}.
     **/
    public CodatID( PeerGroupID groupID ) {
        super( groupID.getUUID(), UUIDFactory.newUUID() );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,byte[])}.
     **/
    public CodatID( PeerGroupID groupID, byte [] seed ) {
        this( );
        
        UUID groupCBID = groupID.getUUID();
        
        id.longIntoBytes(
        CodatID.groupIdOffset, groupCBID.getMostSignificantBits() );
        id.longIntoBytes(
        CodatID.groupIdOffset + 8, groupCBID.getLeastSignificantBits() );
        
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance( "SHA-1" );
        } catch( NoSuchAlgorithmException caught ) {
            digester = null;
        }
        
        if (digester == null) {
            throw new ProviderException("SHA1 digest algorithm not found");
        }
        
        byte[] digest = digester.digest( seed );
        
        //we keep only the 128 most significant bits
        byte[] buf16 = new byte[16];
        
        System.arraycopy( digest, 0, buf16, 0, 16 );
        
        UUID peerCBID = UUIDFactory.newUUID( buf16 );
        
        id.longIntoBytes(CodatID.idOffset, peerCBID.getMostSignificantBits());
        id.longIntoBytes(CodatID.idOffset + 8, peerCBID.getLeastSignificantBits());
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,InputStream)}.
     **/
    public CodatID( PeerGroupID groupID, InputStream in ) throws IOException {
        super( groupID, in );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,InputStream)}.
     **/
    public CodatID( PeerGroupID groupID, byte [] seed, InputStream in ) throws IOException {
        this( groupID, seed );
        
        setHash( in );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.id.ID getPeerGroupID() {
        UUID groupCBID = new UUID(
            id.bytesIntoLong( CodatID.groupIdOffset ),
            id.bytesIntoLong( CodatID.groupIdOffset + 8 ) );
        
        PeerGroupID groupID = new PeerGroupID( groupCBID );
        
        // convert to the generic world PGID as necessary
        return IDFormat.translateToWellKnown( groupID );
    }
    
    /**
     *  Returns the UUID associated with this CodatID.
     *  
     *  @return The UUID associated with this CodatID.
     **/
    public UUID getUUID() {
        return new UUID( id.bytesIntoLong( CodatID.idOffset ), 
        id.bytesIntoLong( CodatID.idOffset + 8 ) );
    }
}

/************************************************************************
 *
 * $Id: WelcomeMessage.java,v 1.1 2007/01/16 11:02:03 thomas Exp $
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
 *********************************************************************************/
package net.jxta.impl.endpoint.msgframing;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.StringTokenizer;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

/**
 *  Contains a JXTA connection Welcome Message. The Welcome Message is sent by
 *  both participant peers as the first interchange on newly opened connections.
 *
 *  <p/>The Welcome Message contains the following information:
 *  <ul>
 *      <li>The address to which the local peer believes it is connected.</li>
 *      <li>The local peer's return address, the source address.</li>
 *      <li>The local peer's peer id.</li>
 *      <li>A flag which controls propagation behaviour for this conneciton.</li>
 *  </ul>
 *
 *@see    <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#trans-tcpipt" 
 *        target="_blank">JXTA Protocols Specification : TCP/IP Message Transport</a>
 */
public class WelcomeMessage {
    
    /**
     *  The Welcome Message Signature/Preamble
     **/
    private final static String GREETING = "JXTAHELLO";
    
    /**
     *  A space for separating elements of the welcome message.
     **/
    private final static String SPACE = " ";
    
    /**
     *  The current welcome message version. This is the only version we will emit.
     **/
    private final static String CURRENTVERSION = "1.1";
    
    /**
     *  The destination address that we believe we are connecting to.
     **/
    private final EndpointAddress destinationAddress;

    /**
     *  Our return address, the purported source address of this connection.
     **/
    private final EndpointAddress publicAddress;
    
    /**
     *  Our peerid, the logical return address.
     **/
    private final ID peerID;
    
    /**
    *   This connection does not wish to receive any propagation/broadcast/notifications.
    **/
    private boolean noPropagate = false;
     
    /**
    *   The welcome message version we are supporting
    **/
    private final String versionString;
      
    /**
     *  The welcome message as a text string.
     **/
    private final String welcomeString;
    
    /**
     *  The welcome message as UTF-8 byte stream.
     **/
    private byte [] welcomeBytes;
    
    /** 
     *  Creates a new instance of WelcomeMessage for our Welcome Message.
     *
     *  @param destAddr The destination address that we believe we are connecting to.
     *  @param publicaddress Our return address, the purported source address of this connection.
     *  @param peerid Our peerid, the logical return address.
     *  @param dontPropagate If <tt>true</tt> this connection does not wish to receive any propagation/broadcast/notifications.
     **/
    public WelcomeMessage( EndpointAddress destAddr, EndpointAddress publicaddress, ID peerid, boolean dontPropagate ) {
        destinationAddress = destAddr;
        publicAddress = publicaddress;
        peerID = peerid;
        noPropagate = dontPropagate;
        versionString = CURRENTVERSION;
        
        welcomeString = GREETING +
            SPACE +
            destAddr.toString() +
            SPACE +
            publicAddress.toString() +
            SPACE +
            peerID.toString() +
            SPACE +
            (noPropagate ? "1" : "0") +
            SPACE +
            versionString;
        
        try {
            welcomeBytes  = welcomeString.getBytes( "UTF-8" );
        } catch ( UnsupportedEncodingException never ) {
            // all implementations must support utf-8
            ;
        }
    }
    
    /** 
     *  Creates a new instance of WelcomeMessage for another peer's Welcome Message
     *
     *  @param in The InputStream to read the welcome message from.
     *  @throws IOException If there is a problem reading the welcome header.
     **/
    public WelcomeMessage( InputStream in ) throws IOException {
        welcomeBytes = new byte [4096];
        int readAt = 0;
        boolean sawCR = false;
        boolean sawCRLF = false;
        
        // read the welcome message
        do {
            int c = in.read();
            
            switch( c ) {
                case -1 : 
                    throw new EOFException( "Stream terminated before end of welcome message" );
                    
                case '\r' :
                if( sawCR ) {
                    welcomeBytes[readAt++] = (byte) 0x0D;
                }
                
                sawCR = true;
                break;
                
                case '\n' :
                    if( sawCR ) {
                        sawCRLF = true;
                    } else {
                        welcomeBytes[readAt++] = (byte) 0x0A;
                    }
                    break;
                    
                default :
                    welcomeBytes[readAt++] = (byte) c;
                    sawCR = false;
            }
            
            if( readAt == welcomeBytes.length ) {
                throw new IOException( "Invalid welcome message, too long" );
            }
            
        } while( !sawCRLF );
        
        byte [] truncatedBytes = new byte [readAt];
        
        System.arraycopy( welcomeBytes, 0, truncatedBytes, 0, readAt );
        
        welcomeBytes = truncatedBytes;
        
        welcomeString = new String( welcomeBytes, "UTF-8" );
        
        // we have the message, parse it.
        StringTokenizer thePieces = new StringTokenizer( welcomeString, " " );
        
        if( !thePieces.hasMoreTokens() || !GREETING.equals(thePieces.nextToken()) ) {
            throw new IOException( "Invalid welcome message, did not start with greeting" );
        }
        
        if( !thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, nothing after greeting" );
        }
            
        destinationAddress = new EndpointAddress( thePieces.nextToken() );
        
        if( !thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, nothing after destination address" );
        }
            
        publicAddress = new EndpointAddress( thePieces.nextToken() );
        
        if( !thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, nothing after public address" );
        }
        
        try {
        URI peerURL =  new URI( thePieces.nextToken() );
        
        peerID = IDFactory.fromURI( peerURL );
        } catch( URISyntaxException badURI ) {
            IOException failed = new IOException( "Invalid welcome message, bad peer id" );
            
            failed.initCause( badURI );
            
            throw failed;
        }
        
        if( !thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, nothing after peer id" );
        }
        
        String noPropagateStr = thePieces.nextToken();
        
        if( noPropagateStr.equals( "1" ) ) {
            noPropagate = true;
        } else {
            if( noPropagateStr.equals( "0" ) ) {
                noPropagate = false;
            } else {
                throw new IOException( "Invalid welcome message, illegal value for propagate flag" );
            }
        }

        if( !thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, version missing" );
        }

        versionString  = thePieces.nextToken();
        
        if( thePieces.hasMoreTokens() ) {
            throw new IOException( "Invalid welcome message, text after version string" );
        }
    }
    
    /**
     *  Write the welcome message to the provided stream.
     *
     *  @param theStream The OutputStream to which to write the welcome message.
     *  @throws IOException If there is a problem writing the welcome message.
     **/
    public void sendToStream( OutputStream theStream ) throws IOException {
        theStream.write( welcomeBytes );
        theStream.write( '\r' );
        theStream.write( '\n' );
    }

    /**
     *  Return the peerid associated with the Welcome Message.
     *
     *  @return The peer ID from the Welcome Message.
     **/
    public ID getPeerID() {
        return peerID;
    }
    
    /**
     *  Return the source address associated with the Welcome Message.
     *
     *  @return The source address from the Welcome Message.
     **/
    public EndpointAddress getPublicAddress() {
        return publicAddress;
    }
    
    /**
     *  Return the destination address associated with the Welcome Message.
     *
     *  @return The destination address from the Welcome Message.
     **/
    public EndpointAddress getDestinationAddress() {
        return destinationAddress;
    }
    
    /**
     *  Return the the propagation preference from the Welcome Message.
     *
     *  @return <tt>true</tt> if <strong>no</strong> propagation is desired 
     *  otherwise <tt>false</tt>
     **/
    public boolean dontPropagate() {
        return noPropagate;
    }
    
    /**
     *  Return the version associated with the Welcome Message.
     *
     *  @return The version from the Welcome Message.
     **/
    public String getWelcomeVersion() {
        return versionString;
    }

    /**
     *  Return a String containing the Welcome Message.
     *
     *  @return a String containing the Welcome Message.
     **/
    public String getWelcomeString() {
        return welcomeString;
    }
}

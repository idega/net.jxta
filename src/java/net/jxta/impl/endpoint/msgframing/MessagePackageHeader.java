/************************************************************************
 *
 * $Id: MessagePackageHeader.java,v 1.1 2007/01/16 11:02:03 thomas Exp $
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;

/**
 *  Header Package for Messages. Analagous to HTTP Headers.
 *  
 **/
public class MessagePackageHeader {
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger( MessagePackageHeader.class.getName() );
    
    /**
     * Used for storing headers.
     **/
    public static class Header {
        
        String  name;
        byte [] value;
        
        public Header( String name, byte [] value ) {
            this.name = name;
            this.value = value;
        }
        
        public String getName()  { return name; }
        
        public byte [] getValue() { return value; }
    }
    
    List headers = new ArrayList();
    
    /**
     * Creates a new instance of MessagePackage. Used for outgoing messages.
     **/
    public MessagePackageHeader( ) {
        
    }
    
    /**
     * Creates a new instance of MessagePackage. Used for incoming messages.
     *
     *  @param in the stream from which the headers will be read.
     **/
    public MessagePackageHeader( InputStream in ) throws IOException {
        boolean sawEmpty = false;
        boolean sawLength = false;
        boolean sawType = false;
        DataInput di = new DataInputStream( in );
        
        // XXX 20021014 bondolo@jxta.org A framing signature would help here.
        
        do {
            byte headerNameLength = di.readByte();
            
            if( 0 == headerNameLength )
                sawEmpty = true;
            else {
                byte [] headerNameBytes = new byte [headerNameLength];
                
                di.readFully( headerNameBytes );
                
                String headerNameString = new String( headerNameBytes, "UTF-8" );
                
                if( headerNameString.equalsIgnoreCase( "content-length" ) ) {
                    if( sawLength )
                        throw new IOException( "Duplicate content-length header" );
                    
                    sawLength = true;
                }
                
                if( headerNameString.equalsIgnoreCase( "content-type" ) ) {
                    if( sawType )
                        throw new IOException( "Duplicate content-type header" );
                    
                    sawType = true;
                }
                
                int headerValueLength = di.readUnsignedShort();
                
                byte [] headerValueBytes = new byte [headerValueLength];
                
                di.readFully( headerValueBytes );
                
                headers.add( new Header( headerNameString, headerValueBytes ) );
                
            }
        } while( !sawEmpty );
        
        if( !sawLength ) {
            if (LOG.isEnabledFor(Level.WARN))
                LOG.warn( "Content Length header was missing" );
            throw new IOException( "Content Length header was missing" );
        }
        
        if( !sawType ) {
            if (LOG.isEnabledFor(Level.WARN))
                LOG.warn( "Content Type header was missing" );
            throw new IOException( "Content Type header was missing" );
        }
    }
    
    /**
     *  Add a header.
     *
     *  @param name the header name
     *  @param value the value for the header
     **/
    public void addHeader( String name, byte [] value ) {
        /*
        if (LOG.isEnabledFor(Level.DEBUG))
          LOG.debug("Add header :" + name + "(" + name.length() +") with " + value.length + " bytes of value" );
        */
        
        headers.add( new Header( name, value ) );
    }
    
    /**
     *  Replace a header. Replaces all existing headers with the same name
     *
     *  @param name the header name
     *  @param value the value for the header
     **/
    public void replaceHeader( String name, byte [] value ) {
        Iterator eachHeader = getHeaders();
        while( eachHeader.hasNext() ) {
            Header aHeader = (Header) eachHeader.next();
            
            if( aHeader.getName().equalsIgnoreCase( name ) )
                headers.remove( aHeader );
        }
        
        headers.add( new Header( name, value ) );
    }
    
    /**
     *  Gets all of the headers
     **/
    public Iterator getHeaders() {
        return headers.iterator();
    }
    
    /**
     *  Gets all of the headers matching the specified name
     *
     *  @param name the name of the header we are seeking.
     **/
    public Iterator getHeader( String name ) {
        Iterator eachHeader = getHeaders();
        List matchingHeaders = new ArrayList();
        
        while( eachHeader.hasNext() ) {
            Header aHeader = (Header) eachHeader.next();
            
            if( name.equals(aHeader.getName()) )
                matchingHeaders.add( aHeader );
        }
        
        return matchingHeaders.iterator();
    }
    
    /**
     *  Write the headers to a stream
     *
     *  @param out  the stream to send the headers to.
     **/
    public void sendToStream( OutputStream out ) throws IOException {
        Iterator eachHeader = getHeaders();
        DataOutput dos = new DataOutputStream( out );
        
        // XXX 20021014 bondolo@jxta.org A framing signature would help here

        while( eachHeader.hasNext() ) {
            Header aHeader = (Header) eachHeader.next();
            
            byte [] nameBytes = aHeader.getName().getBytes( "UTF-8" );
            byte [] value = aHeader.getValue();
            
            dos.write( nameBytes.length );
            dos.write( nameBytes );
            dos.writeShort( value.length );
            dos.write( value );
        }
        
        // write empty header
        dos.write( 0 );
    }
    
    /**
     *  Convenience Method for Content Length header
     *
     *  @param length length of the message.
     **/
    public void setContentLengthHeader( long length ) {
        byte [] lengthAsBytes = new byte [8];
        long lengthAsLong = length;
        for( int eachByte = 0; eachByte < 8; eachByte ++ )
            lengthAsBytes[eachByte] = (byte) (lengthAsLong >> ((7 - eachByte) * 8L));
        
        replaceHeader( "content-length", lengthAsBytes );
    }
    
    /**
     *  Convenience Method for Content Length header
     *
     *  @return length from the header.
     **/
    public long getContentLengthHeader( ) {
        Header header = (Header) getHeader( "content-length" ).next();
        byte [] lengthAsBytes = header.getValue();
        
        long lengthAsLong = 0L;
        
        for( int eachByte = 0; eachByte < 8; eachByte ++ )
            lengthAsLong |= ((long)(lengthAsBytes[eachByte] & 0xff)) << ((7 - eachByte) * 8L);
        
        return lengthAsLong;
    }
    
    /**
     *  Convenience Method for Content Type header
     *
     *  @param type type of the message.
     **/
    public void setContentTypeHeader( MimeMediaType type ) {
        try {
            replaceHeader( "content-type", type.toString().getBytes( "UTF-8" ) );
        } catch ( UnsupportedEncodingException never ) {
            // utf-8 is a required encoding.
            throw new IllegalStateException( "UTF-8 encoding support missing!" );
        }
    }
    
    /**
     *  Convenience Method for Content Type header
     *
     *  @return type from the header.
     **/
    public MimeMediaType getContentTypeHeader( ) {
        Header header = (Header) getHeader( "content-type" ).next();
        try {
            return new MimeMediaType( new String( header.getValue(), "UTF-8" ) ).intern();
        } catch ( UnsupportedEncodingException never ) {
            // utf-8 is a required encoding.
            throw new IllegalStateException( "UTF-8 encoding support missing!" );
        }
    }    
}

/************************************************************************
 *
 * $Id: StringMessageElement.java,v 1.1 2007/01/16 11:01:26 thomas Exp $
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

package net.jxta.endpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import java.lang.ref.SoftReference;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;

/**
 *  A Message Element using character strings for the element data.
 *
 **/
public class StringMessageElement extends TextMessageElement {
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger( StringMessageElement.class.getName() );
    
    private static final MimeMediaType DEFAULT_TEXT_ENCODING;
    
    static {
        // There doesn't seem to be a way to do this directly. The goal is to get
        // the canonical name of the encoding being used. Since ByteArrayInputStream
        // does not copy the data, this operation is relatively cheap if a little
        // circuitous.
        InputStreamReader getEncoding = new InputStreamReader( new ByteArrayInputStream( new byte [0] ) );
        
        String encoding = getEncoding.getEncoding();
        
          /**
            bondolo 20040904 For J2SE 5.0 :
         
            String encoding = java.nio.charset.Charset.defaultCharset().name();
        
         **/
      DEFAULT_TEXT_ENCODING = new MimeMediaType( MimeMediaType.TEXT_DEFAULTENCODING, "charset=\"" + encoding + "\"", true ).intern();
    }
    
    /**
     *  The data for this Message Element.
     **/
    protected String data;
    
    /**
     *  Returns an appropriate mime type for the given encoding name. The
     *  mimetype will contain the canonical name of the encoding.
     *
     *  @param encoding name of the desired encoding.
     *  @return the mime type.
     **/
    private static MimeMediaType makeMimeType( String encoding ) throws UnsupportedEncodingException {
         InputStreamReader getEncoding = new InputStreamReader( new ByteArrayInputStream( new byte [0] ), encoding );
        
        String canonicalName = getEncoding.getEncoding();
       
        return new MimeMediaType( MimeMediaType.TEXT_DEFAULTENCODING, "charset=\"" + canonicalName + "\"", true ).intern();
    }
    
    /**
     *  Create a new Message Element from the provided String. The String will
     *  be encoded for transmission using UTF-8.
     *
     *  @param name Name of the Element. May be the empty string ("") or null if
     *  the Element is not named.
     *  @param value A String containing the contents of this element.
     *  @param sig Message digest/digital signature element. If no signature is
     *  to be specified, pass <code>null</code>.
     *
     *  @throws IllegalArgumentException if <code>value</code> is
     *  <code>null</code>.
     **/
    public StringMessageElement(String name, String value, MessageElement sig) {
        super( name, MimeMediaType.TEXTUTF8, sig );
        
        if( null == value ) {
            throw new IllegalArgumentException( "value must be non-null" );
        }
        
        data = value;
    }
    
    /**
     *  Create a new Message Element from the provided String. The string will
     *  be encoded for transmission using specified character encoding.
     *
     *  @param name Name of the MessageElement. May be the empty string ("") or
     *  <code>null</code> if the MessageElement is not named.
     *  @param value A String containing the contents of this element.
     *  @param encoding Name of the character encoding to use. If
     *  <code>null</code> then the system default charcter encoding will be
     *  used. (Using the system default charcter encoding should be used with
     *  extreme caution).
     *  @param sig Message digest/digital signature element. If no signature is
     *  to be specified, pass <code>null</code>.
     *
     *  @throws IllegalArgumentException if <code>value</code> is
     *  <code>null</code>.
     *  @throws UnsupportedEncodingException if the requested encoding is not
     *  supported.
     **/
    public StringMessageElement( String name, String value, String encoding, MessageElement sig ) throws UnsupportedEncodingException {
        super( name, (null == encoding) ? DEFAULT_TEXT_ENCODING : makeMimeType(encoding), sig );
        
        if( null == value ) {
            throw new IllegalArgumentException( "value must be non-null" );
        }
        
        data = value;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        if( this == target ) {
            return true;
        }
        
        if( target instanceof MessageElement ) {
            if( !super.equals(target) )
                return false;
            
            if( target instanceof StringMessageElement ) {
                StringMessageElement likeMe = (StringMessageElement) target;
                
                return data.equals( likeMe.data ); // same chars?
            } else if ( target instanceof TextMessageElement) {
                // have to do a slow char by char comparison. Still better than the stream since it saves encoding.
                // XXX 20020615 bondolo@jxta.org the performance of this could be much improved.
                
                TextMessageElement likeMe = (TextMessageElement) target;
                
                try {
                    Reader myReader = getReader();
                    Reader itsReader = likeMe.getReader();
                    
                    int mine;
                    int its;
                    do {
                        mine = myReader.read();
                        its = itsReader.read();
                        
                        if( mine != its )
                            return false;       // content didn't match
                        
                    } while( (-1 != mine) && (-1 != its) );
                    
                    return ( (-1 == mine) && (-1 == its) ); // end at the same time?
                } catch( IOException fatal ) {
                    IllegalStateException failure = new IllegalStateException( "MessageElements could not be compared." );
                    failure.initCause( fatal );
                    throw failure;
                }
            }
            else {
                // have to do a slow stream comparison.
                // XXX 20020615 bondolo@jxta.org the performance of this could be much improved.
                
                MessageElement likeMe = (MessageElement) target;
                
                try {
                    InputStream myStream = getStream();
                    InputStream itsStream = likeMe.getStream();
                    
                    int mine;
                    int its;
                    do {
                        mine = myStream.read();
                        its = itsStream.read();
                        
                        if( mine != its )
                            return false;       // content didn't match
                        
                    } while( (-1 != mine) && (-1 != its) );
                    
                    return ( (-1 == mine) && (-1 == its) ); // end at the same time?
                } catch( IOException fatal ) {
                    IllegalStateException failure = new IllegalStateException( "MessageElements could not be compared." );
                    failure.initCause( fatal );
                    throw failure;
                }
            }
        }
        
        return false; // not a new message element
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode() {
        int result = super.hashCode() * 6037 + // a prime
        data.hashCode();
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String toString( ) {
        return data;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized byte[] getBytes( boolean copy ) {
        byte [] cachedBytes = null;
        
        if( null != cachedGetBytes ) {
            cachedBytes = (byte []) cachedGetBytes.get();
        }
        
        if( null == cachedBytes ) {
            if (LOG.isEnabledFor(Level.DEBUG)){
                LOG.debug( "Creating getBytes of " + getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this))  );
            }
            
            String charset = type.getParameter( "charset" );
            
            try {
                cachedBytes = data.getBytes( charset );
            } catch( UnsupportedEncodingException caught ) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn( "MessageElement Data could not be generated", caught );
                }
                IllegalStateException failure = new IllegalStateException( "MessageElement Data could not be generated" );
                failure.initCause( caught );
                throw failure;
            }
            
            cachedGetBytes = new SoftReference( cachedBytes );
        }
        
        if( !copy ) {
            return cachedBytes;
        }
        
        byte [] bytesCopy = (byte[]) cachedBytes.clone();
        
        return bytesCopy;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public long getCharLength() {
        return data.length();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized char[] getChars( boolean copy ) {
        char [] cachedChars = null;
        
        if( null != cachedGetChars ) {
            cachedChars = (char []) cachedGetChars.get();
        }
        
        if( null == cachedChars ) {
            if (LOG.isEnabledFor(Level.DEBUG)){
                LOG.debug( "creating cachedGetChars of " + getClass().getName() + '@' + Integer.toHexString(hashCode())  );
            }
            
            cachedChars = new char [ data.length() ];
            
            data.getChars( 0, data.length(), cachedChars, 0 );
            
            // if this is supposed to be a shared buffer then we can cache it.
            
            cachedGetChars = new SoftReference( cachedChars );
        }
        
        if( !copy ) {
            return cachedChars;
        }
        
        char [] copyChars = (char[]) cachedChars.clone();
        
        return copyChars;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputStream getStream() throws IOException {
        byte cachedBytes[] = null;
        
        synchronized( this ) {
            if (null != cachedGetBytes)  {
                cachedBytes = (byte []) cachedGetBytes.get();
            }
        }
        
        if( null != cachedBytes ) {
            return new ByteArrayInputStream( cachedBytes );
        } else {
            String charset = type.getParameter( "charset" );
            
            return new CharSequenceInputStream( data, charset );
        }
    }
    
    /**
     *  {@inheritDoc}
     *
     *  @return InputStream of the stream containing element data.
     *  @throws IOException when there is a problem getting a reader.
     **/
    public Reader getReader() throws IOException {
        
        return new StringReader( data );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToStream( OutputStream sendTo ) throws IOException {

        sendTo.write( getBytes(false) );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToWriter( Writer sendTo ) throws IOException {
        sendTo.write( data );
    }
    
    /**
     *
     **/
    private static class CharSequenceInputStream extends InputStream {
        
        private final CharBuffer charData;
        
        private final CharsetEncoder conversion;
        
        private boolean marked = false;
        private byte mark_multiByteChar[];
        private int mark_position;
        
        private byte multiByteChar[];
        private int position;
        
        /**
         *
         **/
        public CharSequenceInputStream( CharSequence s, String encoding ) {
            charData = CharBuffer.wrap(s);
            
            Charset encodingCharset = Charset.forName( encoding );
            
            conversion = encodingCharset.newEncoder();
            conversion.onMalformedInput( CodingErrorAction.REPLACE );
            conversion.onUnmappableCharacter( CodingErrorAction.REPLACE );
            
            int maxBytes = new Float( conversion.maxBytesPerChar() ).intValue();
            
            multiByteChar = new byte[maxBytes];
            position = multiByteChar.length;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public void mark( int ignored ) {
            charData.mark();
            mark_multiByteChar = (byte[]) multiByteChar.clone();
            mark_position = position;
            marked = true;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public boolean markSupported( ) {
            return true;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public void reset() throws IOException {
            
            if( !marked ) {
                throw new IOException( "reset() called before mark()" );
            }
            
            charData.reset();
            multiByteChar = (byte[]) mark_multiByteChar.clone();
            position = mark_position;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public int read() throws IOException{
            // prefill the buffer
            while( multiByteChar.length == position ) {
                int readsome = read( multiByteChar, 0, multiByteChar.length );
                
                if( -1 == readsome ) {
                    return -1;
                }
                
                position = multiByteChar.length - readsome;
                
                if( (0 != position) && (0 != readsome) ) {
                    System.arraycopy( multiByteChar, 0, multiByteChar, position, readsome );
                }
            }
            
            return( multiByteChar[position++] & 0xFF );
        }
        
        /**
         *  {@inheritDoc}
         **/
        public int read( byte[] buffer ) throws IOException {
            return read( buffer, 0, buffer.length );
        }
        
        /**
         *  {@inheritDoc}
         **/
        public int read( byte[] buffer, int offset, int length ) throws IOException {
            // handle partial characters;
            if( multiByteChar.length != position ) {
                int copying = Math.min( length, multiByteChar.length - position );
                System.arraycopy( multiByteChar, position, buffer, offset, copying );
                position += copying;
                return copying;
            }
            
            ByteBuffer bb = ByteBuffer.wrap( buffer, offset, length );
            
            int before = bb.remaining();
            
            CoderResult result = conversion.encode( charData, bb, true );
            
            int readin = before - bb.remaining();
            
            if( CoderResult.UNDERFLOW == result ) {
                if( 0 == readin ) {
                    return -1;
                } else {
                    return readin;
                }
            }
            
            if( CoderResult.OVERFLOW == result ) {
                return readin;
            }
            
            result.throwException();
            
            // NEVERREACHED
            return 0;
        }
    }
}

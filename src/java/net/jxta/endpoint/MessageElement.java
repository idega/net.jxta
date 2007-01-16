/*
 *
 * $Id: MessageElement.java,v 1.1 2007/01/16 11:01:27 thomas Exp $
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

package net.jxta.endpoint;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.util.CountingOutputStream;
import net.jxta.util.DevNullOutputStream;

// imported for implementation of {@link #getUniqueName()} and {@link #getSequentialName()}
import net.jxta.impl.id.UUID.UUIDFactory;

/**
 * JXTA Message Elements are used to add data to a Message. Message Elements are
 * immutable objects. A Message Element may be shared amongst as many messages
 * as is desired. A Message Element is composed of four components:
 *
 *  <ul>
 *  <li>An optional name. This may be any {@link java.lang.String}. Unnamed
 * elements are assumed to have the name "" (the empty string).</li>
 *  <li>An optional {@link net.jxta.document.MimeMediaType}. If not specified
 * the Mime Media Type is assumed to be "Application/Octet-Stream".</li>
 *  <li>Data. Sub-classes of MessageElement allow you to create elements based
 * on a variety of data formats.</li>
 *  <li>An optional signature. This is a Message Element that is associated to
 *  this element and may contain a cryptographic signature/hash of this message
 *  element.
 *  </li>
 *
 *  <p/>The data contained with in a MessageElement is accessible in four ways:
 *
 *  <ul>
 *  <li>As an {@link java.io.InputStream}</li>
 *  <li>Sending the data a {@link java.io.OutputStream}</li>
 *  <li>As a {@link java.lang.String}.</li>
 *  <li>As a byte array.</li>
 *  </ul>
 *
 * @see net.jxta.endpoint.Message
 */
public abstract class MessageElement implements Document {
    
    /**
     *  Log4J logger
     **/
    private static transient final Logger LOG = Logger.getLogger( MessageElement.class.getName());
    
    /**
     *  The name of this element. May be the empty string ("") if the element is
     *  unnamed.
     **/
    protected final String name;
    
    /**
     *  The type of this element.
     **/
    protected final MimeMediaType type;
    
    /**
     *  The optional element which digitially signs or digests this element.
     *  If null then the element is has no signature element.
     **/
    protected final MessageElement sig;
    
    /**
     *  message properties hashmap
     **/
    protected Map properties = new HashMap();
    
    /**
     *  cached result of {@link #getByteLength()} operation.
     **/
    protected transient long cachedGetByteLength = -1;
    
    /**
     *  cached result of {@link #getBytes(boolean)} operation.
     **/
    protected transient SoftReference cachedGetBytes = null;
    
    /**
     *  cached result of {@link #toString()} operation.
     **/
    protected transient SoftReference cachedToString = null;
    
    /**
     * Returns a pseudo-random unique string which can be used as an element
     * name.
     *
     * @return String containing a pseudo-random value
     */
    public static String getUniqueName() {
        return UUIDFactory.newUUID().toString();
    }
    
    /**
     * Returns a string containing a pseudo-random unique string. The result of
     * <code>String.compare()</code> will be consistent with the order in which
     * results were returned from this function.
     *
     * <p/>Security Consideration : Be aware that the pseudo random portion of
     * the names generated by this string are shared amongst all peer groups
     * running in the same classloader. You may be at a risk for loss of
     * annonimity if you use the element names produced in more than one peer
     * group.
     *
     * @return String containing a pseudo-random value. The result of
     * <code>String.compare()</code> will be consistent with the order in
     * which results were returned from this function.
     *
     */
    public static String getSequentialName() {
        return UUIDFactory.newSeqUUID().toString();
    }
    
    /**
     *  Internal constructor for initializaing everything but the data.
     *
     *   @param name Name of the Element. May be the empty string ("") if
     *    the Element is not named.
     *   @param type Type of the Element. null is equivalent to specifying
     *    the type "Application/Octet-stream"
     *   @param sig optional message digest/digital signature elemnent. If
     *    no signature is to be specified, pass null.
     **/
    protected MessageElement( String name, MimeMediaType type, MessageElement sig ) {
        this.name = ( null != name ) ? name : "";
        
        this.type = ( null != type ) ? type : MimeMediaType.AOS;
        
        if( (null != sig) && (null != sig.sig) ) {
            throw new IllegalArgumentException( "Invalid Signature Element. Signatures may not have signatures." );
        }
        
        this.sig = sig;
    }
    
    /**
     *  {@inheritDoc}
     *
     *  @deprecated Since Message Elements are immutable this method does
     *  nothing useful.
     **/
    public final Object clone( ) {
        return this;
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>Elements are considered equal if they have the same name, type and
     *  signatures. Element data is not considered by this implementation as
     *  it is mostly intended for subclass use.
     *
     **/
    public boolean equals( Object target ) {
        if( this == target ){
            return true;  // same object
        }
        
        if( target instanceof MessageElement ) {
            MessageElement likeMe = (MessageElement) target;
            
            // sig is nullable so test seperatly.
            boolean sigequals = (null != sig) ? sig.equals( likeMe.sig ) : (null==likeMe.sig);
            
            return sigequals &&
            name.equals( likeMe.name ) &&
            type.equals( likeMe.type );
        }
        
        return false; // not a MessageElement
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode( ) {
        int sigHash = ( (null != getSignature()) && (this != getSignature())) ?
        getSignature().hashCode() : 1;
        
        int result =  sigHash * 2467 + // a prime
        getElementName().hashCode() * 3943 + // also a prime
        getMimeType().hashCode();
        
        return (0 != result) ? result : 1;
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>Returns a String representation of the element data. The
     *  <code>'charset'</code> parameter of the message element's mimetype, if
     *  any, is used to determine encoding. If the charset specified is
     *  unsupported then the default enconding will be used.
     *
     *  <p/>synchronized for caching purposes.
     **/
    public synchronized String toString( ) {
        String result = null;
        
        if( null != cachedToString ) {
            result = (String) cachedToString.get();
            
            if (null != result)
                return result;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)){
            LOG.debug( "creating toString of " + getClass().getName() + '@' + Integer.toHexString(hashCode()) );
        }
        
        String charset = type.getParameter( "charset" );
        
        StringBuffer theString = new StringBuffer();
        
        Reader asString = null;
        
        try {
            if( null == charset )
                asString = new InputStreamReader( getStream() );
            else {
                try {
                    asString = new InputStreamReader( getStream(), charset );
                } catch( UnsupportedEncodingException caught ) {
                    throw new IllegalStateException( "Unsupported charset : " + charset );
                }
            }
            
            char [] characters = new char [256];
            do {
                int res = asString.read( characters );
                
                if( res < 0 ){
                    break;
                }
                
                theString.append( characters, 0, res );
            } while( true );
            
            
            result = theString.toString();
            
            cachedToString = new SoftReference( result );
            return result;
        } catch ( IOException caught ) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error( "Could not generate string for element. ", caught );
            }
            
            throw new IllegalStateException( "Could not generate string for element. " + caught );
        }
    }
    
    /**
     *  Returns the name of the MessageElement. Unnamed elements will return
     *  the empty string ("");
     *
     *  @return String containing the name of the MessageElement.
     **/
    public String getElementName() {
        return name;
    }
    
    /**
     * {@inheritDoc}
     *
     *  <p/>Will return "Application/Octet-Stream" if no type was originally
     *   specified.
     **/
    public MimeMediaType getMimeType() {
        return type;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p/>We use the "unknown" extension and leave it to sub-classes to
     * extend this. If we had a mailcap facility we could do better
     * classification based on mimetype.
     *
     */
    public String getFileExtension() {
        return "???";
    }
    
    /**
     *   Returns the size of the element data in bytes.
     *
     *   @return long containing the size of the element data.
     **/
    public synchronized long getByteLength() {
        if( cachedGetByteLength >= 0 )
            return cachedGetByteLength;
        
        CountingOutputStream countBytes = new CountingOutputStream( new DevNullOutputStream() );
        
        try {
            sendToStream( countBytes );
            cachedGetByteLength = countBytes.getBytesWritten();
            return cachedGetByteLength;
        } catch( IOException caught ) {
            throw new IllegalStateException( "Could not get length of element : " + caught.toString() );
        }
    }
    
    /**
     *  Returns a byte array which contains the element data. The byte array
     *  returned <b>may be shared amongst all copies of the element</b>,
     *  do not modify it. The <code>copy</code> parameter allows you to request a
     *  private, modifiable copy of the element data.
     *
     *  <p/>This implementation builds the byte array from the stream.
     *
     * @return byte[] Contents of message element.
     * @param copy If true then the result can be modified without damaging the state of this
     * MessageElement. If false, then the result may be a shared copy of the data and
     * should be considered read-only.
     **/
    public synchronized byte[] getBytes( boolean copy ) {
        byte [] result = null;
        
        if( null != cachedGetBytes ) {
            result = (byte []) cachedGetBytes.get();
            
            if (null != result)
                if ( copy ) {
                    byte [] theCopy = new byte[ result.length ];
                    
                    System.arraycopy( theCopy, 0, result, 0, result.length );
                } else {
                    return result;
                }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)){
            LOG.debug( "creating getBytes of " + getClass().getName() + '@' + Integer.toHexString(hashCode())  );
        }
        
        long len = getByteLength();
        
        if( len > Integer.MAX_VALUE ) {
            if (LOG.isEnabledFor(Level.ERROR))
                LOG.error( "MessageElement is too large to be stored in a byte array." );
            
            throw new IllegalStateException( "MessageElement is too large to be stored in a byte array." );
        }
        
        result = new byte [ (int) len ];
        
        try {
            DataInput di = new DataInputStream( getStream() );
            
            di.readFully( result );
        } catch ( IOException caught ) {
            if (LOG.isEnabledFor(Level.ERROR)){
                LOG.error( "ailed to get bytes of Message Element. ", caught );
            }
            throw new IllegalStateException( "Failed to get bytes of Message Element. " + caught );
        }
        
        // if this is supposed to be a shared buffer then we can cache it.
        if( !copy ) {
            cachedGetBytes = new SoftReference( result );
        }
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     *
     * <p/>This version probably has sub-optimal performance. Sub-classes
     * should override this implementation.
     **/
    public void sendToStream( OutputStream sendTo ) throws IOException {
        copyInputStreamToOutputStream( getStream(), sendTo );
    }
    
    /**
     *  Returns the element containing the digest/digital signature for
     *  this element
     *
     *  @return Element containing the digital signature.
     **/
    public MessageElement getSignature() {
        
        return sig;
    }
    
    /**
     *  Associate a transient property with this element. If there was a previous
     *  value for the key provided then it is returned.
     *
     *  <p/>Element properties are useful for managing the state of element
     *  during processing. Element properties are not transmited with the
     *  message element when the message element is sent as part of a message.
     *
     *  <p/>The setting of particular keys may be controlled by a Java Security
     *  Manager. Keys of type 'java.lang.Class' are checked against the caller of
     *  this method. Only callers which are instances of the key class may modify
     *  the property. This check is not possible through reflection. All other
     *  types of keys are unchecked.
     *
     *  @param key  the property key
     *  @param value  the value for the property
     *  @return previous value for the property or null if no previous
     **/
    public synchronized Object setElementProperty( Object key, Object value ) {
        
    /*
    if( key instanceof java.lang.Class ) {
      Class keyClass = (Class) key;
      SecurityManager secure =  new SecurityManager() {
        public boolean checkCallerOfClass( Class toCheck ) {
          Class [] context = getClassContext();
     
          return toCheck.isAssignableFrom( context[2] );
        }
      };
     
      if( !secure.checkCallerOfClass( keyClass ) ) {
        throw new SecurityException( "You can't set that key from this context." );
      }
    }
     */
        
        return properties.put( key, value );
    }
    
    /**
     *  Retrieves a transient property from the set for this element.
     *
     *  <p/>Element properties are useful for managing the state of element
     *  during processing. Element properties are not transmited with the
     *  message element when the message element is sent as part of a message.
     *
     *  @param key  the property key.
     *  @return value for the property or null if there is no property for this
     *  key.
     **/
    public Object getElementProperty( Object key ) {
        
        return properties.get( key );
    }
    
    /**
     * Copies an input stream to an output stream with buffering.
     *
     * @param source The stream to copy from.
     * @param sink The stream to send the data to.
     * @throws IOException if there is a problem copying the data
     */
    protected static void copyInputStreamToOutputStream( InputStream source, OutputStream sink ) throws IOException {
        int c;
        byte[] buf = new byte[4096];
        
        do {
            c=source.read(buf);
            
            if( -1 == c ) {
                break;
            }
            
            sink.write(buf,0,c);
        } while( true );
    }
}


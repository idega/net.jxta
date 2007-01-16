/************************************************************************
 *
 * $Id: IDBytes.java,v 1.1 2007/01/16 11:02:08 thomas Exp $
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

package net.jxta.impl.id.UUID;

import java.io.Serializable;
import java.util.Arrays;

import java.util.zip.Checksum;
import java.util.zip.CRC32; // used in hashCode

/**
 * Maintains the internal representation of a 'uuid' JXTA ID.
 *
 * @see net.jxta.id.IDFactory
 * @see net.jxta.impl.id.UUID.IDFormat
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#refimpls-ids-jiuft" target="_blank">JXTA Protocols Specification : UUID ID Format</a>
 **/
public final class IDBytes implements Serializable {
    
    /**
     *  The bytes.
     **/
    public byte [] bytes = null;
    
    /**
     *  if true then we have calculated the hash value for this object.
     **/
    protected transient volatile boolean hashIsCached = false;
    
    /**
     *  The cached hash value for this object
     **/
    protected transient int cachedHash = 0;
    
    /**
     *  Constructs a new byte representation. This constructor initializes only
     *  the flag fields of the ID.
     *
     **/
    public IDBytes() {
        this.bytes = new byte[ IDFormat.IdByteArraySize ];
        this.bytes[IDFormat.flagsOffset + IDFormat.flagsIdTypeOffset] = 0;
    }
    
    /**
     * Compares two IDs for equality.
     *
     * @param target  the ID to be compared against.
     * @return	boolean true if IDs are equal, false otherwise.
     **/
    public boolean equals( Object target ) {
        if (this == target) {
            return true;
        }
        
        if (target instanceof IDBytes) {
            return Arrays.equals( bytes, ((IDBytes)target).bytes );
        } else {
            return false;
        }
    }
    
    /**
     * Public member calculates a hash code for this ID. Used by Hashmaps.
     *
     * @return int Containing the hashcode of this ID.
     **/
    public int hashCode() {
        if( !hashIsCached ) {
            synchronized( this ) {
                // check again since it may have been updated while we were waiting for lock
                if( !hashIsCached ) {
                    Checksum crc = new CRC32();
                    
                    crc.update( bytes, 0, bytes.length );
                    
                    cachedHash = (int) crc.getValue();
                    hashIsCached = true;
                }
            }
        }
        
        return cachedHash;
    }
    
    /**
     * Returns a string representation of the ID bytes. The bytes are encoded
     * in hex ASCII format with two characters per byte. The pad bytes between
     * the primary id portion and the flags field are ommitted.
     *
     * @return	String containting the URI
     **/
    public String toString( ) {
        return getUniqueValue().toString();
    }
    
    /**
     *  Private replacement for toHexString since we need the leading 0 digits.
     *  Returns a char array containing byte value encoded as 2 hex chars.
     *
     *  @param  theByte a byte containing the value to be encoded.
     *  @return	char[] containing byte value encoded as 2 hex characters.
     **/
    private static char[] toHexDigits( byte theByte ) {
        final char [] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
                char result[] = new char[2];
                
                result[0] = HEXDIGITS[(theByte >>> 4) & 15];
                result[1] = HEXDIGITS[theByte & 15];
                
                return result;
    }
    
    /**
     *  Return an object containing the unique value of the ID. This object must
     *  provide implementations of toString() and hashCode() that are canonical
     *  and conisistent from run-to-run given the same input values. Beyond
     *  this nothing should be assumed about the nature of this object. For some
     *  implementations the object returned may be the same as provided.
     *
     *  @return	Object which can provide canonical representations of the ID.
     **/
    public Object getUniqueValue() {
        StringBuffer    encoded = new StringBuffer(144);
        int             lastIndex;
        
        // find the last non-zero index.
        for( lastIndex = IDFormat.flagsOffset - 1; lastIndex > 0; lastIndex-- ) {
            if( 0 != bytes[lastIndex] ) {
                break;
            }
        }
        
        // build the string.
        for( int eachByte = 0; eachByte <= lastIndex; eachByte++ ) {
            char asHex [] = toHexDigits(bytes[eachByte]);
            encoded.append( asHex );
        }
        
        // append the last two chars.
        
        char asHex [] = toHexDigits(bytes[IDFormat.flagsOffset + IDFormat.flagsIdTypeOffset]);
        encoded.append( asHex );
        
        return encoded.toString();
    }
    
    /**
     *  Insert a long value into the byte array. The long is stored in
     *  big-endian order into the byte array begining at the specified index.
     *
     *  @param offset location within the byte array to insert.
     *  @param value value to be inserted.
     **/
    public void longIntoBytes( int offset, long value ) {
        if( (offset < 0) || ((offset + 8) > IDFormat.IdByteArraySize) ) {
            throw new IndexOutOfBoundsException( "Bad offset" );
        }
        
        for( int eachByte = 0; eachByte < 8; eachByte ++ ) {
            bytes[eachByte + offset] = (byte) (value >> ((7 - eachByte) * 8L));
        }
        
        hashIsCached = false;
    }
    
    /**
     *  Return the long value of a portion of the byte array. The long is
     *  retrieved in big-endian order from the byte array at the specified
     *  offset.
     *
     *  @param offset location within the byte array to extract.
     *  @return long value extracted from the byte array.
     **/
    public long bytesIntoLong( int offset ) {
        if( (offset < 0) || ((offset + 8) > IDFormat.IdByteArraySize) ) {
            throw new IndexOutOfBoundsException( "Bad offset" );
        }
        
        long result = 0L;
        
        for( int eachByte = 0; eachByte < 8; eachByte ++ ) {
            result |= ((long)(bytes[eachByte + offset] & 0xff)) << ((7 - eachByte) * 8L);
        }
        
        return result;
    }
}

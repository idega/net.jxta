/*
 * $Id: BinaryDocument.java,v 1.1 2007/01/16 11:01:29 thomas Exp $
 ********************
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 ********************
 */

package net.jxta.impl.document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import net.jxta.document.MimeMediaType;
import net.jxta.document.Document;

/**
 * This class is an implementation of the Document interface. It is perhaps the
 * simplest implementation of the Document interface possible.
 **/
public class BinaryDocument implements Document {
    
    /**
     * Our Mime Media Type
     */
    private static final MimeMediaType [] myTypes = {
        MimeMediaType.AOS
    };
    
    /** 
     * Storage for our bytes.
     */
    private final byte [] ourBytes;
    
    /**
     *  Returns the MIME Media types supported by this this Document per
     *  {@link <a href="http://www.ietf.org/rfc/rfc2046.txt">IETF RFC 2046 <i>MIME : Media Types</i></a>}.
     *
     *  Jxta does not currently support the 'Multipart' or 'Message' media types.
     *
     *  @return An array of MimeMediaType objects containing the MIME Media Type
     *  for this Document.
     */
    public static MimeMediaType [] getSupportedMimeTypes() {
        return( (MimeMediaType []) myTypes.clone() );
    }
    
    /** Creates new BinaryDocument
     *
     * @param someBytes Contains a byte array which will serve as our data.
     */
    public BinaryDocument( byte [] someBytes ) {
        ourBytes = (byte []) someBytes.clone();
    }
    
    /**
     * Returns the MIME Media type of this Document per
     * {@link <a href=http://www.ietf.org/rfc/rfc2046.txt">IETF RFC 2046 <i>MIME : Media Types</i></a>}.
     *
     * Jxta does not currently support the 'Multipart' or 'Message' media types.
     *
     * @return A MimeMediaType object containing the MIME Media Type for this Document.
     */
    public MimeMediaType getMimeType() {
        return( myTypes[0] );
    }
    
    /**
     *  Returns the file extension type used by this Document. This value
     *  is chosen based upon the MIME Media Type for this Document.
     *
     * @return a string containing an appropriate file extension
     **/
    public String getFileExtension() {
        return "bin";
    }
    
    /**
     * Returns a stream of bytes which represent the content of this Document.
     *
     * @return An {@link InputStream} containting the bytes of this Document.
     */
    public InputStream getStream() throws IOException {
        return( new ByteArrayInputStream( ourBytes ) );
    }
    
    /**
     * Returns a stream of bytes which represent the content of this Document.
     *
     * @return An {@link InputStream} containting the bytes of this Document.
     */
    public void sendToStream( OutputStream stream ) throws IOException {
        stream.write( ourBytes );
    }
}

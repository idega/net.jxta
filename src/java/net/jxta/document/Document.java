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
 * $Id: Document.java,v 1.1 2007/01/16 11:01:46 thomas Exp $
 */

package net.jxta.document;

import java.io.InputStream;
import java.io.OutputStream;

import java.io.IOException;

/**
 * A simple typed container for data. A <code>Document</code> is presented as a
 * byte stream with an associated type. The data type is specified using a
 * MIME Media Type (as defined by
 * {@link <a href="http://www.ietf.org/rfc/rfc2046.txt" target="_blank">IETF RFC 2046 <i>MIME : Media Types</i></a>}).
 *
 * @see         net.jxta.document.MimeMediaType
 * @see         net.jxta.document.StructuredDocument
 * @see         net.jxta.document.StructuredDocumentFactory
 **/
public interface Document {
    
    /**
     * Returns the MIME Media type of this <code>Document</code> per
     * {@link <a href="http://www.ietf.org/rfc/rfc2046.txt" target="_blank">IETF RFC 2046 <i>MIME : Media Types</i></a>}.
     *
     * <p/>JXTA does not currently support the '<code>Multipart</code>' or
     * '<code>Message</code>' media types.
     *
     * @return A MimeMediaType object containing the MIME Media Type for this
     * <code>Document</code>.
     **/
    MimeMediaType getMimeType();
    
    /**
     * Returns the file extension type used by this <code>Document</code>. This
     * value is usually chosen based upon the MIME Media Type.
     *
     * @return A String containing an appropriate file extension for this
     * <code>Document</code>.
     **/
    String getFileExtension();
    
    /**
     * Returns the stream of bytes which represents the content of this
     * <code>Document</code>.
     *
     * @return An {@link java.io.InputStream} containing the bytes
     * of this <code>Document</code>.
     * @exception  IOException if an I/O error occurs.
     **/
    InputStream getStream() throws IOException;
    
    /**
     *  Send the contents of this <code>Document</code> to the specified stream.
     *
     *  @param stream   The OutputStream to which the <code>Document</code> will
     * be written.
     *  @exception  IOException if an I/O error occurs.
     **/
    void sendToStream( OutputStream stream ) throws IOException;
}

/*
 * $Id: PlainTextDocument.java,v 1.1 2007/01/16 11:01:29 thomas Exp $
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Vector;

import java.io.IOException;
import java.security.ProviderException;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
/**
 * This class is an implementation of the StructuredDocument interface using
 * simple text
 */
public class PlainTextDocument extends PlainTextElement implements TextDocumentCommon {
    
    private final static class Instantiator implements StructuredDocumentFactory.TextInstantiator {
        
        /**
         *  The MIME Media Types which this <CODE>StructuredDocument</CODE> is
         *  capable of emitting.
         */
        private static final MimeMediaType [] myTypes = {
            MimeMediaType.TEXT_DEFAULTENCODING
        };
        
        // these are the file extensions which are likely to contain files of
        // the type i like.
        private static final ExtensionMapping [] myExtensions = {
            new ExtensionMapping( "txt", myTypes[0] ),
            new ExtensionMapping( "text", myTypes[0] ),
            new ExtensionMapping( "txt", null )
        };
        
        /** Creates new LiteXMLDocumentInstantiator */
        public Instantiator() {
        }
        
        /**
         * Returns the MIME Media types supported by this this Document per
         * {@link <a href="http://www.ietf.org/rfc/rfc2046.txt">IETF RFC 2046 <i>MIME : Media Types</i></a>}.
         *
         * Jxta does not currently support the 'Multipart' or 'Message' media types.
         *
         * @return An array of MimeMediaType objects containing the MIME Media Type
         * for this Document.
         *
         */
        public MimeMediaType[] getSupportedMimeTypes() {
            return( myTypes );
        }
        
        /**
         * Returns the file extension types supported by this this Document.
         *
         * @return An array of string objects containing file extensions
         *
         */
        public ExtensionMapping [] getSupportedFileExtensions() {
            return( myExtensions );
        }
        
        /**
         * Create a new structured document of the type specified by doctype.
         *
         * @param  mimeType    The mimetype to be associated with this instance.
         *     the base type must be one of the types returned by <tt>getSupportedMimeTypes</tt>.
         *     Some implementations may accept parameters in the params section of the mimetype.
         * @param  doctype     Type for the base node of the document.
         * @return StructuredDocument instance.
         */
        public StructuredDocument newInstance(MimeMediaType mimeType, String doctype) {
            return new PlainTextDocument( mimeType, doctype );
        }
        
        /**
         * Create a new structured document of the type specified by doctype.
         *
         * @param  mimeType    The mimetype to be associated with this instance.
         *     the base type must be one of the types returned by <tt>getSupportedMimeTypes</tt>.
         *     Some implementations may accept parameters in the params section of the mimetype.
         * @param  doctype     Type for the base node of the document.
         * @param  value     value for the base node of the document.
         * @return StructuredDocument instance.
         */
        public StructuredDocument newInstance(MimeMediaType mimeType, String doctype, String value) {
            return new PlainTextDocument( mimeType, doctype, value );
        }
        
        /**
         * Create a structured document from a stream containing an appropriately serialized
         * instance of the same document.
         *
         * @param  mimeType    The mimetype to be associated with this instance.
         *     the base type must be one of the types returned by <tt>getSupportedMimeTypes</tt>.
         *     Some implementations may accept parameters in the params section of the mimetype.
         * @param  source     Inputstream from which to read the instance.
         * @return StructuredDocument instance.
         * @exception  IOException occurs when there is a problem with the source input stream.
         */
        public StructuredDocument newInstance(MimeMediaType mimeType, InputStream source) throws IOException {
            throw new ProviderException( "PlainTextDocument does not support input" );
        }
        
        /**  Create a structured document from a Reader containing an appropriately serialized
         *  instance of the same document.
         *
         *  @param  mimeType    The mimetype to be associated with this instance.
         *      the base type must be one of the types returned by
         * <tt>getSupportedMimeTypes</tt>. Some implementations may accept
         * parameters in the params section of the mimetype.
         *  @param  source     Reader from which to read the instance.
         *  @return {@link StructuredDocument} instance.
         *  @exception  IOException occurs when there is a problem with the source input stream.
         */
        public StructuredDocument newInstance(MimeMediaType mimeType, Reader source) throws IOException {
            throw new ProviderException( "PlainTextDocument does not support input" );
        }
        
    };
    
    public static final StructuredDocumentFactory.TextInstantiator INSTANTIATOR = new Instantiator();
    
    private MimeMediaType mimeType = null;
    
    /** Creates new PlainTextDocument */
    public PlainTextDocument( final MimeMediaType mimeType, String type ) {
        super( null, type );
        doc = this;
        parent = this;
        
        this.mimeType = mimeType;
    }
    
    /** Creates new PlainTextDocument with a value for the root element */
    public PlainTextDocument( final MimeMediaType mimeType, final String type, final String value ) {
        super( null, type, value );
        doc = this;
        parent = this;
        
        this.mimeType = mimeType;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String toString( ) {
        StringWriter  stringOut = new StringWriter();
        
       
        try {
            printNice( stringOut, 0, true );
            stringOut.close();
        } catch ( IOException caught ) {
            return null;
        }
        
        return stringOut.toString();
    }
    
    /**
     * get Type
     **/
    public MimeMediaType getMimeType() {
        return mimeType;
    }
    
    
    /**
     *  {@inheritDoc}
     **/
    public String getFileExtension() {
        return TextDocumentCommon.Utils.getExtensionForMime(
        INSTANTIATOR.getSupportedFileExtensions(), getMimeType() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.document.Element createElement( Object key ) {
        return createElement( key, null );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.document.Element createElement( Object key, Object val ) {
        if( !String.class.isAssignableFrom( key.getClass() ) )
            throw new ClassCastException( key.getClass().getName() + " not supported by createElement." );
        
        if( (null != val) && !String.class.isAssignableFrom( val.getClass() ) )
            throw new ClassCastException( val.getClass().getName() + " not supported by createElement." );
        
        return (net.jxta.document.Element) new PlainTextElement( this, (String) key, (String) val );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public TextElement createElement( String name ) {
        return (TextElement) new PlainTextElement( this, name );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public TextElement createElement(String name, String val) {
        return (TextElement) new PlainTextElement( this, name, val );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputStream getStream() throws IOException {
        // XXX  bondolo@jxta.org 20010307    Should be using a pipe
        String charset = mimeType.getParameter("charset");
        
        if (charset == null) {
            return new ByteArrayInputStream( toString().getBytes() );
        } else {
            return new ByteArrayInputStream( toString().getBytes( charset ) );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToStream( OutputStream stream ) throws IOException {
        String charset = mimeType.getParameter("charset");
        
        Writer osw;
        if (charset == null) {
            osw = new OutputStreamWriter( stream );
        } else {
            osw = new OutputStreamWriter( stream, charset );
        }
        
        Writer out = new BufferedWriter( osw );
        
        sendToWriter( out );
        out.flush();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Reader getReader() {
        // XXX  bondolo@jxta.org 20010307    Should be using a pipe
        
        return new StringReader( toString() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendToWriter( Writer stream ) throws IOException {
        printNice( stream, 0, true );
    }
}

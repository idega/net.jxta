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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: StructuredDocumentFactory.java,v 1.1 2007/01/16 11:01:46 thomas Exp $
 */
package net.jxta.document;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import java.io.IOException;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.util.ClassFactory;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextMessageElement;

/**
 * A factory for constructing instances of {@link StructuredDocument}.
 * Behind the scenes, it also provides for the registration of the mime-types
 * and constructors needed to accomplish the construction. All supported
 * mime-types will need to register their implementation in this factory.
 *
 * <p>The configuration is done via the property
 * <tt>net.jxta.impl.config.StructuredDocumentInstanceTypes</tt>
 *
 * @see         net.jxta.document.Document
 * @see         net.jxta.document.StructuredTextDocument
 * @see         net.jxta.document.StructuredDocument
 * @see         net.jxta.document.MimeMediaType
 **/
public final class StructuredDocumentFactory extends ClassFactory {
    /**
     *  Log4J categorgy
     **/
    private static final Logger LOG = Logger.getLogger( StructuredDocumentFactory.class.getName());
    
    /**
     *  Interface for instantiators of StructuredDocuments
     **/
    public interface Instantiator {
        
        /**
         *  For mapping between extensions and mime types.
         **/
        class ExtensionMapping extends Object {
            /** The extension **/
            String          extension;
            
            /** mimetype it maps to **/
            MimeMediaType   mimetype;
            
            /**
             *  disabled constructor
             **/
            private ExtensionMapping() {}
            
            /**
             *  default constructor
             **/
            public ExtensionMapping( String extension, MimeMediaType mimetype ) {
                this.extension = extension;
                this.mimetype = mimetype;
            }
            
            public boolean equals( Object target ) {
                if( this == target ) {
                    return true;
                }
                
                if( target instanceof ExtensionMapping ) {
                    ExtensionMapping asMapping = (ExtensionMapping) target;
                    return ( extension.equals( asMapping.extension ) &&
                    mimetype.equals( asMapping.mimetype ) );
                } else
                    return false;
            }
            
            public int hashCode() {
                return extension.hashCode() ^ mimetype.hashCode();
            }
            
            public String toString() {
                return extension + " -> " + mimetype.toString();
            }
            
            /**
             *  returns the extension which is part of this mapping.
             *  @return the extension which is part of this mapping.
             **/
            public String getExtension() { return extension; }
            
            /**
             *  returns the MIME Media Type which is part of this mapping.
             *  @return the MIME Media Type which is part of this mapping.
             **/
            public MimeMediaType getMimeMediaType() { return mimetype; }
        }
        
        /**
         * Returns the MIME Media types supported by this this Document per
         * {@link <a href="http://www.ietf.org/rfc/rfc2046.txt">IETF RFC 2046 <i>MIME : Media Types</i></a>}.
         *
         * <p/>JXTA does not currently support the 'Multipart' or 'Message'
         * media types.
         *
         * @return An array of MimeMediaType objects containing the MIME Media Type
         *  for this Document.
         *
         **/
        MimeMediaType [] getSupportedMimeTypes();
        
        /**
         * Returns the mapping of file extension and mime-types for this type
         * of document. The default extension is mapped to the 'null' mime-type
         * and should only be used if no other mapping matches.
         *
         * @return An array of objects containing file extensions
         *
         */
        ExtensionMapping [] getSupportedFileExtensions();
        
        /**
         * Create a new structured document of the type specified by doctype.
         *
         * @param  mimeType    The mimetype to be associated with this instance.
         *  the base type must be one of the types returned by
         *  <tt>getSupportedMimeTypes</tt>. Some implementations may accept
         *  parameters in the params section of the mimetype.
         * @param  doctype     Type for the base node of the document.
         * @return StructuredDocument instance.
         **/
        StructuredDocument newInstance( MimeMediaType mimeType, String doctype );
        
        /**
         * Create a new structured document of the type specified by doctype.
         *
         * @param  mimeType    The mimetype to be associated with this instance.
         *  The base type must be one of the types returned by
         *  <tt>getSupportedMimeTypes</tt>. Some implementations may accept
         *  parameters in the params section of the mimetype.
         * @param  doctype     Type for the base node of the document.
         * @param  value     value for the base node of the document.
         * @return {@link StructuredDocument} instance.
         **/
        StructuredDocument newInstance( MimeMediaType mimeType, String doctype, String value );
        
        /**
         *  Create a structured document from a stream containing an appropriately serialized
         *  instance of the same document.
         *
         *  @param  mimeType    The mimetype to be associated with this instance.
         *      the base type must be one of the types returned by
         * <tt>getSupportedMimeTypes</tt>. Some implementations may accept
         * parameters in the params section of the mimetype.
         *  @param  source     Inputstream from which to read the instance.
         *  @return {@link StructuredDocument} instance.
         *  @exception  IOException occurs when there is a problem with the source input stream.
         **/
        StructuredDocument newInstance( MimeMediaType mimeType, InputStream source )
        throws IOException;
    }
    
    /**
     *  Interface for instantiators of StructuredTextDocuments
     **/
    public interface TextInstantiator extends Instantiator {
      
        /**
         *  Create a structured document from a Reader containing an appropriately serialized
         *  instance of the same document.
         *
         *  @param  mimeType    The mimetype to be associated with this instance.
         *      the base type must be one of the types returned by
         * <tt>getSupportedMimeTypes</tt>. Some implementations may accept
         * parameters in the params section of the mimetype.
         *  @param  source     Reader from which to read the instance.
         *  @return {@link StructuredDocument} instance.
         *  @exception  IOException occurs when there is a problem with the source input stream.
         **/
        StructuredDocument newInstance( MimeMediaType mimeType, Reader source )
        throws IOException;
    }

      /**
     *  This class is a singleton. This is the instance that backs the
     *  static methods.
     **/
    private static StructuredDocumentFactory factory =
    new StructuredDocumentFactory();
    
    /**
     *  This is the map of mime-types and instantiators used by
     *  <CODE>newStructuredDocument</CODE>.
     **/
    private final Map  encodings = new Hashtable();
    
    /**
     *  This is the map of extensions to mime-types used by
     *  {@link #getMimeTypeForFileExtension(String) }
     **/
    private final Map  extToMime = new HashMap();
    
    /**
     *  This is the map of mime-types to extensions used by
     *  {@link #getFileExtensionForMimeType(MimeMediaType mimetype) }
     **/
    private final Map  mimeToExt = new HashMap();
    
    /**
     *  If true then the pre-defined set of StructuredDocument sub-classes has
     *  been registered from the property containing them.
     **/
    private boolean loadedProperty = false;
    
    /**
     *  Private constructor. This class is not meant to be instantiated except
     *  by itself.
     *
     **/
    private StructuredDocumentFactory() {}
    
    /**
     *  Registers the pre-defined set of StructuredDocument sub-classes so that
     *  this factory can construct them.
     *
     *  @return boolean true if at least one of the StructuredDocument sub-classes could
     *  be registered otherwise false.
     **/
    private boolean doLoadProperty() {
        try {
            return registerFromResources( "net.jxta.impl.config",
            "StructuredDocumentInstanceTypes" );
        }
        catch ( MissingResourceException notFound ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Could not find net.jxta.impl.config properties file!" );
            }
            return false;
        }
    }
    
    /**
     *  Used by ClassFactory methods to get the mapping of Mime Types to
     *  constructors.
     *
     *  @return Hastable the hashtable containing the mappings.
     **/
    protected Map getAssocTable() {
        return encodings;
    }
    
    /**
     * Used by ClassFactory methods to ensure that all keys used with the
     * mapping are of the correct type.
     *
     *  @return Class object of the key type.
     **/
    protected Class getClassForKey() {
        return java.lang.String.class;
    }
    
    /**
     *  Used by ClassFactory methods to ensure that all of the instantiators
     *  which are registered with this factory have the correct interface.
     *
     *  @return Class object of the "Factory" type.
     **/
    protected Class getClassOfInstantiators() {
        // our key is the doctype names.
        return Instantiator.class;
    }
    
    /**
     *  Register a class with the factory from its class name. We override the
     *  standard implementation to get the mime type from the class and
     *  use that as the key to register the class with the factory.
     *
     *  @param className The class name which will be regiestered.
     *  @return boolean true if the class was registered otherwise false.
     **/
    protected boolean registerAssoc( String className ) {
        boolean registeredSomething = false;
        
        //LOG.debug( "Registering : " + className );
        
        try {
            Class docClass = Class.forName( className );
            
            Instantiator instantiator = (Instantiator)
            (docClass.getField("INSTANTIATOR").get(null));
            
            MimeMediaType [] mimeTypes = instantiator.getSupportedMimeTypes();
            
            for( int eachType = 0; eachType < mimeTypes.length; eachType++ ) {
                //LOG.debug( "   Registering Type : " + mimeTypes[eachType].getMimeMediaType() );
                
                registeredSomething |= registerInstantiator( 
                    mimeTypes[eachType],
                    instantiator );
            }
        }
        catch( Exception all ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Failed to register '" + className + "'", all );
            }
        }
        
        return registeredSomething;
    }
    
    /**
     *  Returns the prefered extension for a given mime-type. If there is no
     *  mapping or no prefered extension for this mimetype then null is
     *  returned.
     *
     *  @param mimetype the MimeMediaType we wish to know the file extension for.
     *  @return String containing the extension or null for mime-types with no
     *  known association.
     **/
    public static String getFileExtensionForMimeType( MimeMediaType mimetype ) {
        return (String) factory.mimeToExt.get( mimetype.getMimeMediaType() );
    }
    
    /**
     *  Returns the prefered mime-type for a given file extension. If there is
     * no mapping then null is returned.
     *
     *  @param extension The extension we wish to know the mime-type for.
     *  @return MimeMediaType associated with this file extension.
     **/
    public static MimeMediaType getMimeTypeForFileExtension( String extension ) {
        MimeMediaType result = null;
        String fromMap = (String) factory.extToMime.get( extension );
        
        if( null != fromMap ) {
            result = new MimeMediaType( fromMap );
        }
        
        return result; 
    }
    
    /**
     * Register an instantiator object a mime-type of documents to be
     * constructed.
     *
     * @param mimetype   the mime-type associated.
     * @param instantiator the instantiator that wants to be registered..
     * @return boolean true   if the instantiator for this mime-type is now
     * registered. If there was already an instantiator this mime-type then
     * false will be returned.
     * @throws SecurityException   there were permission problems registering
     *  the instantiator.
     **/
    public static boolean registerInstantiator(
    MimeMediaType mimetype,
    Instantiator instantiator ) {
        boolean registered = factory.registerAssoc( mimetype.getMimeMediaType(),
        instantiator );
        
        if( registered ) {
            Instantiator.ExtensionMapping [] extensions =
            instantiator.getSupportedFileExtensions();
            
            for( int eachExt = 0; eachExt < extensions.length; eachExt++ ) {
                if( null != extensions[eachExt].getMimeMediaType() ) {
                    factory.extToMime.put(
                    extensions[eachExt].getExtension(),
                    extensions[eachExt].getMimeMediaType().getMimeMediaType() );
                    
                    factory.mimeToExt.put(
                    extensions[eachExt].getMimeMediaType().getMimeMediaType(),
                    extensions[eachExt].getExtension() );
                }
            }
        }
        
        return registered;
    }
    
    /**
     * Constructs an instance of {@link StructuredDocument} matching
     * the mime-type specified by the <CODE>mimetype</CODE> parameter. The
     * <CODE>doctype</CODE> parameter identifies the base type of the
     * {@link StructuredDocument}.
     *
     * @param mimetype Specifies the mime media type to be associated with
     *  the {@link StructuredDocument} to be created.
     * @param doctype Specifies the root type of the {@link StructuredDocument}
     *  to be created.
     * @return StructuredDocument The instance of {@link StructuredDocument}
     *  or null if it could not be created.
     * @throws NoSuchElementException invalid mime-media-type
     **/
    public static StructuredDocument newStructuredDocument( MimeMediaType mimetype,
    String doctype ) {
        if( !factory.loadedProperty ) {
            factory.loadedProperty = factory.doLoadProperty();
        }
        
        Instantiator instantiator =
        (Instantiator) factory.getInstantiator( mimetype.getMimeMediaType() );
        
        return instantiator.newInstance( mimetype, doctype );
    }
    
    /**
     * Constructs an instance of {@link StructuredDocument} matching
     * the mime-type specified by the <CODE>mimetype</CODE> parameter. The
     * <CODE>doctype</CODE> parameter identifies the base type of the
     * {@link StructuredDocument}. Value supplies a value for the root
     * element.
     *
     * @param mimetype Specifies the mime media type to be associated with
     *  the {@link StructuredDocument} to be created.
     * @param doctype Specifies the root type of the {@link StructuredDocument}
     *  to be created.
     * @param value Specifies a value for the root element.
     * @return StructuredDocument The instance of {@link StructuredDocument}
     *  or null if it could not be created.
     * @throws NoSuchElementException if the mime-type has not been registerd.
     **/
    public static StructuredDocument newStructuredDocument( MimeMediaType mimetype,
    String doctype,
    String value ) {
        if( !factory.loadedProperty ) {
            factory.loadedProperty = factory.doLoadProperty();
        }
        
        Instantiator instantiator =
        (Instantiator) factory.getInstantiator( mimetype.getMimeMediaType() );
        
        return instantiator.newInstance( mimetype, doctype, value );
    }
    
    /**
     * Constructs an instance of {@link StructuredDocument} matching
     * the mime-type specified by the <CODE>mimetype</CODE> parameter. The
     * <CODE>doctype</CODE> parameter identifies the base type of the
     * {@link StructuredDocument}.
     *
     * @param mimetype Specifies the mime media type to be associated with the
     *  {@link StructuredDocument} to be created.
     * @param stream Contains an InputStream from which the document will be
     *  constructed.
     * @return StructuredDocument The instance of {@link StructuredDocument}
     *  or null if it could not be created.
     * @throws IOException If there is a problem reading from the stream.
     * @throws NoSuchElementException if the mime-type has not been registerd.
     **/
    public static StructuredDocument newStructuredDocument( MimeMediaType mimetype,
    InputStream stream )
    throws IOException {
        if( !factory.loadedProperty ) {
            factory.loadedProperty = factory.doLoadProperty();
        }
        
        Instantiator instantiator =
        (Instantiator) factory.getInstantiator( mimetype.getMimeMediaType() );
        
        return instantiator.newInstance( mimetype, stream );
    }
    
     /**
     * Constructs an instance of {@link StructuredDocument} matching
     * the mime-type specified by the <CODE>mimetype</CODE> parameter. The
     * <CODE>doctype</CODE> parameter identifies the base type of the
     * {@link StructuredDocument}.
     *
     * @param mimetype Specifies the mime media type to be associated with the
     *  {@link StructuredDocument} to be created.
     * @param reader A Reader from which the document will be constructed.
     * @return StructuredDocument The instance of {@link StructuredDocument}
     *  or null if it could not be created.
     * @throws IOException If there is a problem reading from the stream.
     * @throws NoSuchElementException if the mime-type has not been registerd.
     * @throws UnsupportedOperationException if the mime-type provided is not
      * a text oriented mimetype.
     **/
    public static StructuredDocument newStructuredDocument( MimeMediaType mimetype,
    Reader reader )
    throws IOException {
        if( !factory.loadedProperty ) {
            factory.loadedProperty = factory.doLoadProperty();
        }
        
        Instantiator instantiator =
        (Instantiator) factory.getInstantiator( mimetype.getMimeMediaType() );
        
        if( !(instantiator instanceof TextInstantiator) ) {
          // XXX 20020502 bondolo@jxta.org we could probably do something 
          // really inefficient that would allow it to work, but better not to.
          // if ReaderInputStream existed, it would be easy to do.
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Document Class '" + instantiator.getClass().getName() + "' associated with '" + mimetype + "' is not a text oriented document" );
            }
            
          throw new UnsupportedOperationException( "Document Class '" + instantiator.getClass().getName() + "' associated with '" + mimetype + "' is not a text oriented document" );
          }
        
        return ((TextInstantiator)instantiator).newInstance( mimetype, reader );
    }
    
     /**
     * Constructs an instance of {@link StructuredDocument} based upon the
     * content of the provided message element.
     *
     * @param element The message element from which to create the document.
     * @return StructuredDocument The instance of {@link StructuredDocument}
     *  or null if it could not be created.
     * @throws IOException If there is a problem reading from the stream.
     * @throws NoSuchElementException if the mime-type has not been registerd.
     **/
    public static StructuredDocument newStructuredDocument( MessageElement element ) 
    throws IOException {
        if( !factory.loadedProperty ) {
            factory.loadedProperty = factory.doLoadProperty();
        }
        
        Instantiator instantiator =
        (Instantiator) factory.getInstantiator( element.getMimeType().getMimeMediaType() );
        
        if( (instantiator instanceof TextInstantiator) && (element instanceof TextMessageElement) ) {
            return ((TextInstantiator)instantiator).newInstance( element.getMimeType(), ((TextMessageElement)element).getReader() );
        } else {
            return instantiator.newInstance( element.getMimeType(), element.getStream() );
        }
    }
}

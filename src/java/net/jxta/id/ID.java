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
 * $Id: ID.java,v 1.1 2007/01/16 11:01:57 thomas Exp $
 */

package net.jxta.id;

import java.net.URI;
import java.net.URL;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 *  IDs are used to uniquely identify peers, peer groups, pipes and other
 *  types of objects manipulated by the JXTA APIs.
 *
 *  @see net.jxta.id.IDFactory
 *  @see net.jxta.codat.CodatID
 *  @see net.jxta.peer.PeerID
 *  @see net.jxta.peergroup.PeerGroupID
 *  @see net.jxta.pipe.PipeID
 *  @see net.jxta.platform.ModuleClassID
 *  @see net.jxta.platform.ModuleSpecID
 *  @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#ids" target='_blank'>JXTA Protocols Specification : IDs</a>
 **/
public abstract class ID implements java.io.Serializable {
    
    /**
     * This defines the URI scheme that we will be using to present JXTA IDs.
     * JXTA IDs are encoded for presentation into URIs (see
     * {@link <a href="http://www.ietf.org/rfc/rfc2396.txt">IETF RFC 2396 Uniform Resource Identifiers (URI) : Generic Syntax</a>}
     * ) as URNs (see
     * {@link <a href="http://www.ietf.org/rfc/rfc2141.txt">IETF RFC 2141 Uniform Resource Names (URN) Syntax</a>}
     * ).
     **/
    public static final String URIEncodingName = "urn";
    
    /**
     *  This defines the URN Namespace that we will be using to present JXTA IDs.
     *  The namespace allows URN resolvers to determine which sub-resolver to use
     *  to resolve URN references. All JXTA IDs are presented in this namespace.
     **/
    public static final String URNNamespace = "jxta";
    
    /**
     *	The null ID. The NullID is often used as a placeholder in fields which
     *  are uninitialized.
     *
     *  <p/>This is a singleton within the scope of a VM.
     **/
    public static final ID nullID = new NullID();
    
    /**
     *
     * Creates an ID by parsing the given URI.
     *
     * <p>This convenience factory method works as if by invoking the
     * {@link IDFactory#fromURI(URI)} method; any {@link URISyntaxException}
     * thrown is caught and wrapped in a new {@link IllegalArgumentException}
     * object, which is then thrown.  </p>
     *
     * <p> This method is provided for use in situations where it is known that
     * the given string is a legal ID, for example for ID constants declared
     * within in a program, and so it would be considered a programming error
     * for the URI not to parse as such.  The {@link IDFactory}, which throws
     * {@link URISyntaxException} directly, should be used situations where a
     * ID is being constructed from user input or from some other source that
     * may be prone to errors.  </p>
     *
     * @param  fromURI   The URI to be parsed into an ID
     * @return The new ID
     *
     * @throws  NullPointerException
     *          If <tt>fromURI</tt> is <tt>null</tt>
     *
     * @throws  IllegalArgumentException
     *          If the given URI is not a valid ID.
     **/
    public static ID create( URI fromURI ) {
        try {
            return IDFactory.fromURI( fromURI );
        } catch (URISyntaxException badid) {
            IllegalArgumentException failure = new IllegalArgumentException();
            failure.initCause( badid );
            throw failure;
        }
    }
    
    /**
     *  Constructor for IDs. IDs are constructed using the {@link IDFactory} or
     *  {@link #create(URI)}.
     *
     **/
    protected ID() {
    }
    
    /**
     *  Returns a string representation of the ID. This representation should be
     *  used primarily for debugging purposes. For most other situations IDs
     *  should be externalized as Java URI Objects via {@link #toURI()}.
     *
     *  <p/>The default implementation is the <code>toString()</code> of the ID
     *  represented as a URI.
     *
     *  @return	String containing the URI
     *
     **/
    public String toString( ) {
        return toURI().toString();
    }
    
    /**
     *  Returns a string identifier which indicates which ID format is
     *  used by this ID instance.
     *
     *  @return	a string identifier which indicates which ID format is
     *  used by this ID instance.
     **/
    public abstract String getIDFormat();
    
    /**
     *  Returns an object containing the unique value of the ID. This object
     *  must provide implementations of toString(), equals() and hashCode() that
     *  are canonical and consistent from run-to-run given the same input values.
     *  Beyond this nothing should be assumed about the nature of this object.
     *  For some implementations the object returned may be <code>this</code>.
     *
     *  @return	Object which can provide canonical representations of the ID.
     **/
    public abstract Object getUniqueValue();
    
    /**
     *  Returns a URL representation of the ID. The
     *  {@link net.jxta.id.IDFactory JXTA ID Factory} can be used to construct
     *  ID Objects from URLs containing JXTA IDs.
     *
     *  @deprecated URIs are now the prefered way of manipulating IDs
     *
     *  @see net.jxta.id.IDFactory#fromURL( java.net.URL )
     *
     *  @return	URL Object containing the URI
     **/
    public URL getURL( ) {
        try {
           return IDFactory.jxtaURL( URIEncodingName, "", URNNamespace + ":" + (String) getUniqueValue() );
        } catch( MalformedURLException caught ) {
            IllegalStateException failure = new IllegalStateException( "Environment incorrectly intialized." );
            failure.initCause( caught );
            throw failure;
        }
    }
    
    /**
     *  Returns a URI representation of the ID. {@link java.net.URI URIs} are
     *  the prefered way of externalizing and presenting JXTA IDs. The
     *  {@link net.jxta.id.IDFactory JXTA ID Factory} can be used to construct
     *  ID Objects from URIs containing JXTA IDs.
     *
     *  @see net.jxta.id.IDFactory#fromURI( java.net.URI )
     *
     *  @return	URI Object containing the URI
     **/
    public URI toURI( ) {
        return URI.create( URIEncodingName + ":" + URNNamespace + ":" + (String) getUniqueValue()  );
    }
}

/**
 * The NullID is often used as a placeholder in fields which are uninitialized.
 */
final class NullID extends ID {
    final static String JXTAFormat = "jxta";
    
    final static String UNIQUEVALUE = "Null";
    
    /**
     *  NullID is not intended to be constructed. You should use the
     *  {@link ID.nullID} constant instead.
     **/
    NullID() {
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        return (this == target);   // null is only itself.
    }
    
    /**
     * deserialization has to point back to the singleton in this VM.
     */
    private Object readResolve() {
        return ID.nullID;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return JXTAFormat;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Object getUniqueValue() {
        return getIDFormat() + "-" + UNIQUEVALUE;
    }
}

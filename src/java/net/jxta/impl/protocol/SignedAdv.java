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
 * $Id: SignedAdv.java,v 1.1 2007/01/16 11:01:39 thomas Exp $
 */

package net.jxta.impl.protocol;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.Enumeration;
import java.security.Signature;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Attribute;
import net.jxta.document.Attributable;
import net.jxta.document.AdvertisementFactory;
import net.jxta.credential.Credential;
import net.jxta.document.Advertisement;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.id.ID;
import net.jxta.protocol.SignedAdvertisement;

import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.util.BASE64OutputStream;
import net.jxta.impl.util.BASE64InputStream;

/**
 * A container for signed Advertisements
 **/
public class SignedAdv extends SignedAdvertisement {

    /**
     *  Instantiator for PlatformConfig
     **/
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        /**
         * Returns the identifying type of this Advertisement.
         *
         * @return String the type of advertisement
         *
         * @since JXTA 1.0
         **/
        public String getAdvertisementType( ) {
            return ADV_TYPE;
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the type
         * specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @since JXTA 1.0
         *
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         * could not be created.
         *
         **/
        public Advertisement newInstance( ) {
            return new SignedAdv();
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the type
         * specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @since JXTA 1.0
         *
         * @param root Specifies a portion of a StructuredDocument which will be
         * converted into an Advertisement.
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         * could not be created.
         *
         **/
        public Advertisement newInstance(Element root) {
            return new SignedAdv( root );
        }
    };

    /**
     *  Log4J Logger
     **/
    private static final transient Logger LOG = Logger.getLogger(SignedAdv.class.getName());

    private static final String ADV_TYPE =  "jxta:SA";

    private static final String [] INDEX_FIELDS = { };

    private byte [] signature = null;

    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public static String getAdvertisementType() {
        return ADV_TYPE;
    }

    /**
     *  Use the Instantiator through the factory
     **/
    SignedAdv() {}

    /**
     *  Use the Instantiator through the factory
     **/
    SignedAdv( Element root ) {
        if( !XMLElement.class.isInstance( root ) )
            throw new IllegalArgumentException( getClass().getName() + " only supports XLMElement" );

        XMLElement doc = (XMLElement) root;

        String doctype = doc.getName();

        String typedoctype = "";
        Attribute itsType = doc.getAttribute( "type" );
        if( null != itsType )
            typedoctype = itsType.getValue();

        if( !doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype) ) {
            throw new IllegalArgumentException( "Could not construct : "
                                                + getClass().getName() + "from doc containing a " + doc.getName() );
        }

        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            Element elem = (Element) elements.nextElement();

            if( !handleElement( elem ) ) {
                if ( LOG.isEnabledFor(Level.DEBUG) )
                    LOG.debug( "Unhandled Element: " + elem.toString() );
            }
        }

        // Sanity Check!!!

    }

    /**
    *  {@inheritDoc}
     **/
    public String [] getIndexFields() {
        return INDEX_FIELDS;
    }

    /**
    *  {@inheritDoc}
     **/
    public net.jxta.id.ID getID() {
        // FIXME bondolo Needs real implementation.
        return ID.nullID;
    }

    /**
    *  {@inheritDoc}
     **/
    protected boolean handleElement( Element raw ) {

        if ( super.handleElement( raw ) )
            return true;

        XMLElement elem = (XMLElement) raw;

        if( "Credential".equals( elem.getName() ) ) {
            signer = new PSECredential( elem );

            return true;
        } else if( "Signature".equals( elem.getName() ) ) {
            try {
                Reader signatureB64 = new StringReader( elem.getTextValue() );
                InputStream bis = new BASE64InputStream( signatureB64 );
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                do {
                    int c = bis.read();

                    if( -1 == c ) {
                        break;
                    }
                    bos.write( c );
                } while( true );

                bis.close();
                bos.close();
                signature = bos.toByteArray();

                return true;
            } catch( IOException failed ) {
                IllegalArgumentException failure = new IllegalArgumentException( "Could not process Signature" );
                failure.initCause( failed );

                throw failure;
            }
        } else if( "Advertisement".equals( elem.getName() ) ) {
            try {
                Reader advertisementB64 = new StringReader( elem.getTextValue() );
                InputStream bis = new BASE64InputStream( advertisementB64 );
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                do {
                    int c = bis.read();

                    if( -1 == c ) {
                        break;
                    }
                    bos.write( c );
                } while( true );

                byte advbytes[] = bos.toByteArray();

                Signature verifier = ((PSECredential)signer).getSignatureVerifier( "SHA1WITHRSA" );

                verifier.update( advbytes );

                boolean matched = verifier.verify( signature );

                if( !matched ) {
                    throw new IllegalArgumentException( "Advertisement could not be verified" );
                }

                advertisementB64 = new StringReader( elem.getTextValue() );
                bis = new BASE64InputStream( advertisementB64 );

                adv = AdvertisementFactory.newAdvertisement( elem.getRoot().getMimeType(), bis );

                return true;
            } catch( IOException failed ) {
                IllegalArgumentException failure = new IllegalArgumentException( "Could not process Advertisement" );
                failure.initCause( failed );

                throw failure;
            } catch( java.security.NoSuchAlgorithmException failed ) {
                IllegalArgumentException failure = new IllegalArgumentException( "Could not process Advertisement" );
                failure.initCause( failed );

                throw failure;
            } catch( java.security.SignatureException failed ) {
                IllegalArgumentException failure = new IllegalArgumentException( "Could not process Advertisement" );
                failure.initCause( failed );

                throw failure;
            }
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     **/
    public Document getDocument( MimeMediaType encodeAs ) {

        if( null == adv ) {
            throw new IllegalStateException( "Advertisement not initialized" );
        }

        if( null == signer ) {
            throw new IllegalStateException( "Signer Credential not initialized" );
        }

        if( !(signer instanceof PSECredential) ) {
            throw new IllegalStateException( "Signer Credential not initialized" );
        }

        StructuredDocument doc = (StructuredDocument) super.getDocument( encodeAs );

        StructuredDocument advDoc = (StructuredDocument) adv.getDocument( encodeAs );

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            advDoc.sendToStream( bos );
            bos.close();

            byte advData [] = bos.toByteArray();

            PSECredential psecred = (PSECredential) signer;

            Signature advSigner = psecred.getSigner( "SHA1WITHRSA" );

            advSigner.update( advData );

            byte signature[] = advSigner.sign();

            StringWriter signatureB64 = new StringWriter();
            StringWriter advertisementB64 = new StringWriter();

            OutputStream signatureOut = new BASE64OutputStream( signatureB64 );
            signatureOut.write( signature );
            signatureOut.close();

            OutputStream advertisementOut = new BASE64OutputStream( advertisementB64, 72 );
            advertisementOut.write( advData );
            advertisementOut.close();

            StructuredDocument creddoc = (StructuredDocument) signer.getDocument( encodeAs );
            StructuredDocumentUtils.copyElements( doc, doc, creddoc, "Credential" );

            Element elem = doc.createElement( "Signature", signatureB64.toString() );
            doc.appendChild( elem );

            elem = doc.createElement( "Advertisement", advertisementB64.toString() );
            doc.appendChild( elem );
            if( doc instanceof Attributable ) {
                ((Attributable)elem).addAttribute( "type", adv.getAdvType() );
            }
        } catch( Exception failed ) {
            if( failed instanceof RuntimeException ) {
                throw (RuntimeException) failed;
            } else {
                throw new UndeclaredThrowableException( failed, "Failure building document" );
            }
        }

        return doc;
    }
}

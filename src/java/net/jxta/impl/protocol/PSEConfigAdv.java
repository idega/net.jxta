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
 * $Id: PSEConfigAdv.java,v 1.1 2007/01/16 11:01:41 thomas Exp $
 */

package net.jxta.impl.protocol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;
import java.security.AlgorithmParameters;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import javax.crypto.EncryptedPrivateKeyInfo;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.KeySpec;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.UnknownServiceException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.document.XMLElement;
import net.jxta.document.AdvertisementFactory.Instantiator;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleClassID;
import net.jxta.protocol.ConfigParams;

/**
 * Contains parameters for configuration of the PSE Membership service.
 *
 * <p/>Note: This implementation contemplates multiple root certs in its
 * schema, but the API has not yet been extended to include this functionality.
 **/
public final class PSEConfigAdv extends ExtendableAdvertisement {
    
    /**
     *  Our DOCTYPE
     **/
    private static final String advType =  "jxta:PSEConfig";
    
    /**
     *  Instantiator for PlatformConfig
     **/
    public static class Instantiator implements AdvertisementFactory.Instantiator {
        
        /**
         * {@inheritDoc}
         **/
        public String getAdvertisementType( ) {
            return advType;
        }
        
        /**
         * {@inheritDoc}
         **/
        public Advertisement newInstance( ) {
            return new PSEConfigAdv();
        }
        
        /**
         * {@inheritDoc}
         **/
        public Advertisement newInstance(Element root) {
            return new PSEConfigAdv( root );
        }
    };
    
    private static final Logger LOG = Logger.getLogger(PSEConfigAdv.class.getName());
    
    private static final String ROOT_CERT_TAG = "RootCert" ;
    private static final String CERT_TAG = "Certificate" ;
    private static final String ENCRYPTED_PRIVATE_KEY_TAG = "EncryptedPrivateKey";
    private static final String KEY_STORE_TYPE_ATTR = "KeyStoreType" ;
    private static final String KEY_STORE_PROVIDER_ATTR = "KeyStoreProvider";
    private static final String KEY_STORE_LOCATION_TAG = "KeyStoreLocation";
    
    private static final String [] fields = { };
    
    private final List certs = new ArrayList();
    
    private EncryptedPrivateKeyInfo encryptedPrivateKey = null;
    
    private String privAlgorithm = null;
    
    private String keyStoreType = null;
    
    private String keyStoreProvider = null;
    
    private URI keyStoreLocation = null;
    
    /**
     *  Use the Instantiator through the factory
     **/
    private PSEConfigAdv() {
    }
    
    /**
     *  Use the Instantiator through the factory
     **/
    private PSEConfigAdv(Element root) {
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
        
        Enumeration eachAttr = doc.getAttributes();
        
        while (eachAttr.hasMoreElements()) {
            Attribute anAttr = (Attribute) eachAttr.nextElement();
            
            if( KEY_STORE_TYPE_ATTR.equals(anAttr.getName()) ) {
                keyStoreType = anAttr.getValue().trim();
            } else if( KEY_STORE_PROVIDER_ATTR.equals(anAttr.getName()) ) {
                keyStoreProvider = anAttr.getValue().trim();
            } else if ("type".equals(anAttr.getName())) {
                ;
            } else if ("xmlns:jxta".equals(anAttr.getName())) {
                ;
            } else {
                if ( LOG.isEnabledFor(Level.WARN) ) {
                    LOG.warn( "Unhandled Attribute: " + anAttr.getName() );
                }
            }
        }
        
        certs.clear();
        
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();
            
            if( !handleElement( elem ) ) {
                if ( LOG.isEnabledFor(Level.DEBUG) )
                    LOG.debug( "Unhandled Element: " + elem.toString() );
            }
        }
        
        // Sanity Check!!!
    }
    
    /**
     * Make a safe clone of this PSEConfigAdv.
     *
     * @return Object A copy of this PSEConfigAdv
     */
    public Object clone() {
        
        PSEConfigAdv result = new PSEConfigAdv();
        
        result.encryptedPrivateKey = encryptedPrivateKey;
        result.privAlgorithm = privAlgorithm;
        result.setCertificateChain( getCertificateChain() );
        
        return result;
    }
    
    /**
     * {@inheritDoc}
     **/
    public static String getAdvertisementType() {
        return advType ;
    }
    
    /**
     * {@inheritDoc}
     **/
    public String getAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public ID getID() {
        InputStream data = new ByteArrayInputStream( getCert().getBytes() );
        
        try {
            return IDFactory.newCodatID( PeerGroupID.worldPeerGroupID, new byte [16], data );
        } catch ( IOException failed ) {
            throw new UndeclaredThrowableException( failed, "Could not generate id" );
        }
    }
    
    /**
     *  Returns the Root Certificate for this peer.
     *
     *  @return the root certificate for this peer.
     **/
    public X509Certificate getCertificate() {
        if( certs.isEmpty() ) { 
            return null;
        } else {
            return (X509Certificate) certs.get(0);
        }
    }
    
    /**
     *  Returns the Root Certificate for this peer.
     *
     *  @return the root certificate for this peer.
     **/
    public X509Certificate[] getCertificateChain() {
        return (X509Certificate[]) certs.toArray( new X509Certificate[certs.size()] );
    }
    
    /**
     *  Returns the Root Ceritficate for this peer encoded as a BASE64 String.
     *
     *  @return the Root Certificate for this peer as a BASE64 String.
     **/
    public String getCert() {
        X509Certificate rootCert = getCertificate();
        
        if( null != rootCert ) {
            try {
                return PSEUtils.base64Encode( getCertificate().getEncoded() );
            } catch( Throwable failed ) {
                throw new IllegalStateException( "Failed to process root cert" );
            }
        } else {
            return null;
        }
    }
    
    /**
     *  Returns the Root Ceritficate for this peer encoded as a BASE64 String.
     *
     *  @return the Root Certificate for this peer as a BASE64 String.
     **/
    public void setCert( String newCert ) {
        try {
            byte [] cert_der = PSEUtils.base64Decode( new StringReader(newCert) );
            
            CertificateFactory cf = CertificateFactory.getInstance( "X509" );
            
            setCertificate( (X509Certificate) cf.generateCertificate( new ByteArrayInputStream(cert_der) ) );
        } catch( Throwable failed ) {
            throw new IllegalArgumentException( "Failed to process cert" );
        }
    }
    
    /**
     *  Sets the Root Certificate for this peer. If null then the Private Key
     *  is also cleared.
     *
     *  @param newCert The root certificate to be associated with this peer or
     *  <code>null</code> if clearing the advertisement.
     **/
    public void setCertificate( X509Certificate newCert ) {
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "setCert : " + newCert );
        }
        
        certs.clear();
        
        if( null == newCert ) {
            encryptedPrivateKey = null;
        } else {
            certs.add( newCert );
        }
    }
    
    /**
     *  Sets the Root Certificate chain for this peer. If null then the Private 
     *  Key is also cleared.
     *
     *  @param newCert The root certificate to be associated with this peer or
     *  <code>null</code> if clearing the advertisement.
     **/
    public void setCertificateChain( X509Certificate[] newCerts ) {
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "setCert : " + newCerts );
        }
        
        certs.clear();
        
        if( null == newCerts ) {
            encryptedPrivateKey = null;
        } else {
            certs.addAll( Arrays.asList( newCerts ) );
        }
    }
    
    /**
     *  Get the private key from this advertisement. The private key is
     *  retrieved from the advertisement using the provided password.
     *
     *  @param password the password to use in attempting to decrypt the private
     *  key.
     *  @return the decrypted private key.
     **/
    public PrivateKey getPrivateKey( char [] password ) {
        
        return PSEUtils.pkcs5_Decrypt_pbePrivateKey( password, privAlgorithm, encryptedPrivateKey );
    }
    
    /**
     *  Get the encrypted private key from this advertisement.
     *
     *  @return the encrypted private key.
     **/
    public EncryptedPrivateKeyInfo getEncryptedPrivateKey( ) {
        
        return encryptedPrivateKey;
    }
    
    /**
     *  Get the encrypted private key algorithm from this advertisement.
     *
     *  @return the decrypted private key algorithm.
     **/
    public String getEncryptedPrivateKeyAlgo( ) {
        
        return privAlgorithm;
    }
    
    /**
     *  Get the encrypted private key from this advertisement.
     *
     *  @return the encoded encrypted private key, a BASE64 String of a DER
     *  encoded PKCS8  EncrpytePrivateKeyInfo.
     **/
    public String getEncryptedPrivKey() {
        try {
            return PSEUtils.base64Encode( encryptedPrivateKey.getEncoded() );
        } catch( Throwable failed ) {
            if ( LOG.isEnabledFor(Level.ERROR ) ) {
                LOG.error( "Failed to process private key", failed );
            }
            
            throw new IllegalStateException( "Failed to process private key" );
        }
    }
    
    public String getKeyStoreType() {
        return keyStoreType;
    }
    
    public void setKeyStoreType( String type ) {
        keyStoreType = type;
    }
    
    public String getKeyStoreProvider() {
        return keyStoreProvider;
    }
    
    public void setKeyStoreProvider( String provider ) {
        keyStoreProvider = provider;
    }
    
    public URI getKeyStoreLocation() {
        return keyStoreLocation;
    }
    
    public void setKeyStoreLocation( URI location ) {
        keyStoreLocation = location;
    }
    
    /**
     *  Set the encrypted private key for this advertisement. The private key
     *  is provided as a BASE64 String of a DER encoded PKCS8
     *  EncrpytePrivateKeyInfo.
     *
     *  @param newPriv a BASE64 String of a DER encoded PKCS8
     *  EncrpytePrivateKeyInfo.
     *  @param algorithm The public key algorithm used by this private key.
     *  Currently only "RSA" is supported.
     **/
    public void setEncryptedPrivateKey( String newPriv, String algorithm ) {
        try {
            byte [] key_der = PSEUtils.base64Decode( new StringReader(newPriv) );
            
            EncryptedPrivateKeyInfo newEncryptedPriv = new EncryptedPrivateKeyInfo( key_der );
            
            setEncryptedPrivateKey( newEncryptedPriv, algorithm );
        } catch( Throwable failed ) {
            if ( LOG.isEnabledFor(Level.ERROR ) ) {
                LOG.error( "Failed to process private key", failed );
            }
            
            throw new IllegalArgumentException( "Failed to process private key :" + failed.toString() );
        }
    }
    
    /**
     *  Set the encrypted private key for this advertisement.
     *
     *  @param newPriv The encrypted private key.
     *  @param algorithm The public key algorithm used by this private key.
     *  Currently only "RSA" is supported.
     **/
    public void setEncryptedPrivateKey( EncryptedPrivateKeyInfo newPriv, String algorithm ) {
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "setPrivateKey : " + newPriv );
        }
        
        encryptedPrivateKey = newPriv;
        privAlgorithm = algorithm;
    }
    
    /**
     *  Set the encrypted private key for this advertisement.
     *
     *  @param password The password to be used in encrypting the private key
     *  @param newPriv  The private key to be stored in encrypted form.
     **/
    public void setPrivateKey( PrivateKey newPriv, char [] password ) {
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "setPrivateKey : " + newPriv );
        }
        
        EncryptedPrivateKeyInfo encypted = PSEUtils.pkcs5_Encrypt_pbePrivateKey( password, newPriv, 500 );
        
        setEncryptedPrivateKey( encypted, newPriv.getAlgorithm() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected boolean handleElement( Element raw ) {
        
        if ( super.handleElement( raw ) )
            return true;
        
        XMLElement elem = (XMLElement) raw;
        
        if ( ROOT_CERT_TAG.equals(elem.getName()) ) {
            
            Enumeration elements = elem.getChildren();
            
            while (elements.hasMoreElements()) {
                XMLElement eachcertelem = (XMLElement) elements.nextElement();
                
                if ( CERT_TAG.equals(eachcertelem.getName()) ) {
                    // XXX bondolo 20040415 backwards compatibility
                    eachcertelem.addAttribute( "type", net.jxta.impl.protocol.Certificate.getMessageType() );

                    net.jxta.impl.protocol.Certificate certChain = new net.jxta.impl.protocol.Certificate( eachcertelem );
                    
                    setCertificateChain( certChain.getCertificates() );
                    
                    continue;
                }
                
                if ( ENCRYPTED_PRIVATE_KEY_TAG.equals(eachcertelem.getName()) ) {
                    String value = eachcertelem.getTextValue();
                    if( null == value ) {
                        throw new IllegalArgumentException( "Empty Private Key element" );
                    }
                    
                    value = value.trim();
                    
                    Attribute algo = eachcertelem.getAttribute( "algorithm" );
                    
                    if( null == algo ) {
                        throw new IllegalArgumentException( "Private Key element must include algorithm attribute" );
                    }
                    
                    setEncryptedPrivateKey( value, algo.getValue() );
                    continue;
                }
                
                if ( LOG.isEnabledFor(Level.DEBUG) )
                    LOG.debug( "Unhandled Element: " + eachcertelem.getName() );
                
            }
            
            return true;
        }
        
        if ( KEY_STORE_LOCATION_TAG.equals(elem.getName()) ) {
            try {
                keyStoreLocation = new URI( elem.getTextValue() );
            } catch ( URISyntaxException badURI ) {
                IllegalArgumentException iae = new IllegalArgumentException( "Bad key store location URI" );
                iae.initCause( badURI );
                
                throw iae;
            }
        }
        
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Document getDocument( MimeMediaType encodeAs ) {
        StructuredDocument adv = (StructuredDocument) super.getDocument( encodeAs );
        
        if( adv instanceof Attributable ) {
            Attributable attrDoc = (Attributable) adv;
            
            if( null != keyStoreType ) {
                attrDoc.addAttribute( KEY_STORE_TYPE_ATTR, keyStoreType );
                
                if( null != keyStoreProvider ) {
                    attrDoc.addAttribute( KEY_STORE_PROVIDER_ATTR, keyStoreProvider );
                }
            }
        }
        
        if( null != keyStoreLocation ) {
            Element keyStoreLocationURI = adv.createElement( KEY_STORE_LOCATION_TAG, keyStoreLocation.toString() );
            adv.appendChild(keyStoreLocationURI);
        }
        
        String encodedRoot = getCert();
        String encodedPrivateKey = getEncryptedPrivKey();

        if( (null != encodedRoot) && (null != encodedPrivateKey) ) {
            Element rootcert = adv.createElement( ROOT_CERT_TAG, null );
            adv.appendChild(rootcert);

            // FIXME bondolo 20040501 needs to write certificate chain.

            Element cert = adv.createElement( CERT_TAG, encodedRoot );
            rootcert.appendChild(cert);


            Element privatekey = adv.createElement( ENCRYPTED_PRIVATE_KEY_TAG, encodedPrivateKey );
            rootcert.appendChild(privatekey);

            if( privatekey instanceof Attributable ) {
                ((Attributable)privatekey).addAttribute( "algorithm", privAlgorithm );
            }
        }
        
        return adv;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String [] getIndexFields() {
        return fields;
    }
}

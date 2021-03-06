/************************************************************************
 *
 * $Id: PSEKeyStoreManagerFactory.java,v 1.1 2007/01/16 11:01:48 thomas Exp $
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

package net.jxta.impl.membership.pse;

import java.util.NoSuchElementException;
import java.net.URI;
import java.io.File;

import java.security.KeyStoreException;
import java.security.NoSuchProviderException;

import net.jxta.peergroup.PeerGroup;
import net.jxta.document.Advertisement;
import net.jxta.id.ID;
import net.jxta.document.Element;
import net.jxta.protocol.ConfigParams;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLElement;

import net.jxta.impl.cm.Cm;
import net.jxta.impl.config.Config;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.PSEConfigAdv;

/**
 *  Encapsulates the behaviour for creating KeyStoreManager Objects.
 **/
public abstract class PSEKeyStoreManagerFactory {
    
    /**
    *   The default KeyStoreManagerGenerator
    **/
    private static PSEKeyStoreManagerFactory defaultGenerator = null;
    
    /**
    *   Sets the default KeyStoreManagerGenerator. 
    *
    *   @param newDefault The new default KeyStoreManagerGenerator.
    **/
    public static void setDefault(PSEKeyStoreManagerFactory newDefault ) {
        synchronized( PSEKeyStoreManagerFactory.class ) {
            defaultGenerator = newDefault;
            }
    }
    
    /**
    *   Returns the default KeyStoreManagerGenerator.
    *
    *   @return The current default KeyStoreManagerGenerator.
    **/
    public static PSEKeyStoreManagerFactory getDefault() {
        synchronized( PSEKeyStoreManagerFactory.class ) {
            if (defaultGenerator == null) {
                defaultGenerator = new PSEKeyStoreManagerFactoryDefault();
            }
            
            return defaultGenerator;
        }
    }
    
    /**
    *   Creates a new KeyStoreManager instance based upon the context and configuration.
    *
    *   @param service  The service that this keystore manager will be working for.
    *   @param config   The configuration parameters.
    **/
    public abstract KeyStoreManager getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException;

    
    /**
    *   Provides the default behaviour for generating KeyStore managers for PSE Membership Service Instances.    
    **/
    private static class PSEKeyStoreManagerFactoryDefault extends PSEKeyStoreManagerFactory {

        /**
        *   {@inheritDoc}
        *   
        *   <p/>If no location is specified then use the CMKeyStoreManager otherwise use the URIKeyStoreManager.
        **/
        public KeyStoreManager getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {

            URI location = config.getKeyStoreLocation();
            KeyStoreManager store_manager;

            try {
                if( null == location ) {
                    store_manager = new CMKeyStoreManager(config.getKeyStoreType(), config.getKeyStoreProvider(), service.getGroup(), service.getAssignedID());
                } else {

                    if( !location.isAbsolute() ) {
                        
                        // Resolve the absolute location of the keystore relative to our prefs directory. This will make a file:// URI.
                        File pseHome = new File( Config.JXTA_HOME );
                        location = location.resolve( pseHome.toURI() );
                    }

                    store_manager = new URIKeyStoreManager(config.getKeyStoreType(), config.getKeyStoreProvider(), location);
                }
                
                return store_manager;                
            } catch( NoSuchProviderException not_available ) {
                throw new PeerGroupException( "Requested KeyStore provider not available", not_available );
            } catch( KeyStoreException bad ) {
                throw new PeerGroupException( "KeyStore failure initializing KeyStoreManager", bad );
            }
        }
    }  
}

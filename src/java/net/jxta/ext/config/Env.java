/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *  must not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: Env.java,v 1.1 2007/01/16 11:01:36 thomas Exp $
 */
package net.jxta.ext.config;

import net.jxta.impl.endpoint.IPUtils;

import java.io.File;
import java.net.InetAddress;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Container for systems properties.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class Env {

    /**
     * JXTA persistence directory.
     */
    
    public final static File JXTA_HOME;
    
    /**
     * JXTA {@link net.jxta.impl.protocol.PlatformConfig} file: {@value}
     *
     * @see    net.jxta.impl.peergroup.PlatformConfigurator PlatformConfigurator
     */
    
    public final static String PLATFORM_CONFIG = "PlatformConfig";
    
    /**
     * {@link net.jxta.ext.config.Profile} configuration file: {@value}
     *
     * @see    net.jxta.ext.config.Profile Profile
     */
    
    public final static String PROFILE = "profile.xml";

    /**
     * JXTA configuration properties.
     */
    
    public final static String CONFIG_PROPERTIES = "config.properties";

    /**
     * Root metwork {@link net.jxta.id.ID}: {@value}
     */
    
    public final static String NETWORK_ID_KEY = "NetPeerGroupID";
    
    /**
     * Root network group name: {@value}
     */
    
    public final static String NETWORK_NAME_KEY = "NetPeerGroupName";
    
    /**
     * Root network description: {@value}
     */
    
    public final static String NETWORK_DESCRIPTION_KEY = "NetPeerGroupDesc";

    /**
     * Reconfiguration semaphore: {@value}
     */
    
    public final static String RECONFIGURE = "reconf";
    
    /**
     * JXTA principal {@link java.lang.System} property key: {@value}
     */
    
    public final static String PRINCIPAL = "net.jxta.tls.principal";
    
    /**
     * JXTA password {@link java.lang.System} property key: {@value}
     */
    
    public final static String PASSWORD = "net.jxta.tls.password";
    
    /**
     * JXTA HTTP proxy {@link java.lang.System} property key: {@value}
     */
    
    public final static String PROXY = "jxta.proxy";
    
    /**
     * HTTP proxy user {@link java.lang.System} key: {@value}
     */
    
    public final static String PROXY_USER = "jxta.proxy.user";
    
    /**
     * HTTP proxy passwod {@link java.lang.System} key: {@value}
     */
    
    public final static String PROXY_PASSWORD = "jxta.proxy.password";
    
    /**
     * HTTP proxy host {@link java.lang.System} key: {@value}
     */
    
    public final static String PROXY_HOST = "http.proxyHost";
    
    /**
     * HTTP proxy port {@link java.lang.System} key: {@value}
     */
    
    public final static String PROXY_PORT = "http.proxyPort";
    
    /**
     * Any/All local addresses.
     */
    
    public final static InetAddress ALL_ADDRESSES = IPUtils.ANYADDRESS;

    /**
     * {@link java.util.List} of non-routable addresses.
     */
    
    public final static List NON_ROUTABLE_ADDRESSES;

    private final static String JXTA = "JXTA_HOME";
    private final static Logger LOG = Logger.getLogger(Env.class.getName());

    static {
        String s = System.getProperty(Env.JXTA);
        File f = null;

        if (s == null) {
            try {
                f = Conversion.toFile(Default.HOME_ADDRESS);
            } catch (ConversionException ce) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("jxta home conversion", ce);
                }
            }
        }

        JXTA_HOME = f != null ? f : new File(s);

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("JXTA HOME: " + JXTA_HOME.toString());
        }

        NON_ROUTABLE_ADDRESSES = new ArrayList();

        NON_ROUTABLE_ADDRESSES.add("192.168.");
        NON_ROUTABLE_ADDRESSES.add("10.");
    }
}

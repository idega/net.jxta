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
 *  $Id: Default.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Default utilities.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

class Default {

    private final static int MILLISECONDS_PER_SECOND = 1000;

    public static URI HOME_ADDRESS;
    public final static String HOME = "file://${user.home}/.jxta";
    public final static String PEER_DESCRIPTOR = "unspecified";
    public final static String PEER_NAME = null;
    public final static String ANONYMOUS = "Anonymous";
    public final static boolean SECURITY_IS_ENABLED = true;
    public final static String PRINCIPAL = null;
    public final static String PASSWORD = null;
    public final static String PEER_DESCRIPTION = "Created by: " +
        Configurator.class.getName();
    public final static Trace TRACE = Trace.DEFAULT;
    public final static String ROOT_CERTIFICATE = null;
    public static URI RENDEZVOUS_BOOTSTRAP_ADDRESS;
    public static boolean RENDEZVOUS_DISCOVERY_IS_ENABLED = true;
    public static URI RELAYS_BOOTSTRAP_ADDRESS;
    public static boolean RELAYS_DISCOVERY_IS_ENABLED = true;
    public static URI REFLECTION_BOOTSTRAP_ADDRESS;
    public static URI ANY_ADDRESS;
    public static URI ANY_TCP_ADDRESS;
    public static URI ANY_HTTP_ADDRESS;
    public static URI ANY_UDP_ADDRESS;
    public static List BOOTSTRAP_ADDRESSES;
    public final static boolean RENDEZVOUS_SERVICE_IS_ENABLED = false;
    public final static boolean RENDEZVOUS_SERVICE_AUTO_START_IS_ENABLED = false;
    public final static long RENDEZVOUS_SERVICE_AUTO_START = 0; //30 * MILLISECONDS_PER_SECOND;
    public final static boolean RELAY_SERVICE_IS_ENABLED = false;
    public final static int RELAY_SERVICE_QUEUE_SIZE = 20;
    public final static boolean RELAY_SERVICE_INCOMING_IS_ENABLED = false;
    public final static boolean RELAY_SERVICE_OUTGOING_IS_ENABLED = true;
    public final static int INCOMING_MAXIMUM = 100;
    public final static int OUTGOING_MAXIMUM = 1;
    public final static long INCOMING_LEASE = 2 * 60 * 60 *
        MILLISECONDS_PER_SECOND;
    public final static long OUTGOING_LEASE = INCOMING_LEASE;
    public final static int QUEUE_SIZE = 100;
    public final static boolean TCP_IS_ENABLED = true;
    public final static boolean TCP_INCOMING_IS_ENABLED = true;
    public final static boolean TCP_OUTGOING_IS_ENABLED = true;
    public final static int TCP_PORT = 9701;
    public static URI TCP_ADDRESS;
    public final static boolean TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED = false;
    public final static boolean TCP_PROXY_IS_ENABLED = false;
    public final static int TCP_PROXY_PORT = 8081;
    public final static int PORT_RANGE = 100;
    public final static boolean MULTICAST_IS_ENABLED = false;
    public static URI MULTICAST_ADDRESS;
    public final static String MULTICAST_IP = "224.0.1.85";
    public final static int MULTICAST_PORT = 1234;
    public final static int MULTICAST_SIZE = 16384;
    public final static boolean HTTP_IS_ENABLED = true;
    public final static boolean HTTP_INCOMING_IS_ENABLED = false;
    public final static boolean HTTP_OUTGOING_IS_ENABLED = true;
    public final static int HTTP_PORT = 9700;
    public static URI HTTP_ADDRESS;
    public final static boolean HTTP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED = false;
    public final static boolean HTTP_PROXY_IS_ENABLED = false;
    public final static int ENDPOINT_SERVICE_QUEUE_SIZE = 20;
    public final static boolean PROXY_SERVICE_IS_ENABLED = false;
    public final static int MINIMUM_PORT = 1;
    public final static int MINIMUM_DYNAMIC_PORT = 0;
    public final static int MAXIMUM_PORT = 65535;
    public final static int INVALID_PORT = -1;
    public final static int MAXIMUM_MULTICAST_SIZE = 1048575;
    public final static int MINIMUM_MULTICAST_SIZE = 0;
    public final static String CONFIG_MODE = "auto";
    public final static int HTTP_PROXY_PORT = 8080;

    private final static String RDV_BOOTSTRAP =
        "http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?2";
    private final static String RLY_BOOTSTRAP =
        "http://rdv.jxtahosts.net/cgi-bin/relays.cgi?2";
    private final static String REFLECTION_BOOTSTRAP =
        "http://rdv.jxtahosts.net/cgi-bin/reflection.cgi";
    private final static String COLON = ":";
    private final static String MULTICAST = Protocol.UDP_URI +
                                            Default.MULTICAST_IP + COLON +
                                            Default.MULTICAST_PORT;

    private final static Logger LOG = Logger.getLogger(Default.class.getName());

    static {
        try {
            HOME_ADDRESS = Conversion.toURI(Util.expand(HOME));
        } catch (ConversionException ce) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid encoding", ce);
            }
        }

        RENDEZVOUS_BOOTSTRAP_ADDRESS = toURI(RDV_BOOTSTRAP);
        RELAYS_BOOTSTRAP_ADDRESS = toURI(RLY_BOOTSTRAP);
        REFLECTION_BOOTSTRAP_ADDRESS = toURI(REFLECTION_BOOTSTRAP);
        ANY_ADDRESS = toURI(Protocol.SCHEMELESS_URI_DELIMITER + COLON);
        ANY_TCP_ADDRESS = toURI(Protocol.TCP_URI + COLON);
        ANY_HTTP_ADDRESS = toURI(Protocol.HTTP_URI + COLON);
        ANY_UDP_ADDRESS = toURI(Protocol.UDP_URI + COLON);
        TCP_ADDRESS = toURI(Protocol.TCP_URI +
                            Env.ALL_ADDRESSES.getHostAddress() + COLON +
                            TCP_PORT);
        HTTP_ADDRESS = toURI(Protocol.HTTP_URI +
                            Env.ALL_ADDRESSES.getHostAddress() + COLON +
			    HTTP_PORT);
        MULTICAST_ADDRESS = toURI(MULTICAST);

        BOOTSTRAP_ADDRESSES = new ArrayList();

        BOOTSTRAP_ADDRESSES.add(ANY_ADDRESS);
        BOOTSTRAP_ADDRESSES.add(ANY_TCP_ADDRESS);
        BOOTSTRAP_ADDRESSES.add(ANY_HTTP_ADDRESS);
    }

    private static URI toURI(String s) {
        URI u = null;

        try {
            u = new URI(s);
        } catch (URISyntaxException use) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid uri", use);
            }
        }

        return u;
    }

    private Default() { }
}

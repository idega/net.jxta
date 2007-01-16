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
 *  $Id: Util.java,v 1.1 2007/01/16 11:01:36 thomas Exp $
 */
package net.jxta.ext.config;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Utilities.
 *
 * @author    james todd [gonzo at jxta dot org]
 */

public class Util {

    private final static String MACRO_PREFIX = "${";
    private final static String MACRO_POSTFIX = "}";
    private final static String MACRO_ESCAPE = "\\";
    private final static String NEW_LINE = "\n";
    private final static String EMPTY_STRING = "";
    private final static Logger LOG = Logger.getLogger(Util.class.getName());

    /**
     * Expands a {@link net.jxta.ext.config.Resource} macro expression.
     */
    
    public static String expand(String s) {
        StringBuffer sb = null;

        if (s != null) {
            sb = new StringBuffer(s);
            int i = -1;
            int j = -1;

            while ((i = sb.indexOf(MACRO_PREFIX)) > -1 &&
                   (i - MACRO_ESCAPE.length() < 0 ||
                    (i - MACRO_ESCAPE.length() >= 0 &&
                     !sb.substring(i - MACRO_ESCAPE.length(), i).equals(MACRO_ESCAPE))) &&
                   (j = sb.indexOf(MACRO_POSTFIX, i + MACRO_PREFIX.length())) > -1) {
                String p = expand(sb.substring(i + MACRO_PREFIX.length(), j));

                sb.replace(i, j + MACRO_POSTFIX.length(), System.getProperty(p, ""));
            }
        }

        return sb != null ? sb.toString() : null;
    }

    /**
     * Accessor for the local address.
     *
     * @return    the local address
     */
    
    public static String getLocalHost() {
        String s = null;

        try {
            s = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException uhe) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("can't resolve local host", uhe);
            }
        }

        return s;
    }

    /**
     * {@link java.net.URI} address validator.
     *
     * @param   address     address
     * @return              validated address
     */
    
    public static String validateAddress(URI address) {
        return validateAddress(address, (boolean)true);
    }

    /**
     * {@link java.net.URI} address minimum port specification validator.
     *
     * @param   address         address
     * @param   minimumPort     minimum port specification
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, int minimumPort) {
        return validateAddress(address, (String)null, minimumPort);
    }

    /**
     * {@link java.net.URI} address non-specified host validator.
     *
     * @param   address         address
     * @param   requireHost     require host check
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, boolean requireHost) {
        return validateAddress(address, (String)null, requireHost);
    }

    /**
     * {@link java.net.URI} address scheme validator.
     *
     * @param   address         address
     * @param   scheme          address scheme check
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, String scheme) {
        return validateAddress(address, scheme, (boolean)false);
    }

    /**
     * {@link java.net.URI} address scheme and host validator.
     *
     * @param   address         addresss
     * @param   scheme          address scheme check
     * @param   requireHost     require host check
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, String scheme, boolean requireHost) {
        return validateAddress(address, scheme, requireHost, Default.MINIMUM_DYNAMIC_PORT);
    }

    /**
     * {@link java.net.URI} address scheme and minimum port validator.
     *
     * @param   address         addresss
     * @param   scheme          address scheme check
     * @param   minimumPort     minimum port check
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, String scheme, int minimumPort) {
        return validateAddress(address, scheme, (boolean)false, minimumPort);
    }

    /**
     * {@link java.net.URI} address scheme, host and minium port validator.
     *
     * @param   address         addresss
     * @param   scheme          address scheme check
     * @param   requireHost     required host check
     * @param   minimumPort     minimum port check
     * @return                  validated address
     */
    
    public static String validateAddress(URI address, String scheme, boolean requireHost,
                                         int minimumPort) {
        StringBuffer b = new StringBuffer();

        if (address != null) {
            if (scheme != null &&
                !address.getScheme().equalsIgnoreCase(scheme)) {
                b.append((b.length() > 0 ? NEW_LINE : EMPTY_STRING) + "invalid scheme: " +
                         scheme);
            }

            if (requireHost) {
                String h = address.getHost();

                if (h == null ||
                    h.trim().length() == 0) {
                    b.append((b.length() > 0 ? NEW_LINE : EMPTY_STRING) + "invalid host: " + h);
                }

                // xxx: don't validate the adress for now.
                //         it would be nice to validate fqn addresses (eg www.jxta.org) and ip addresses
                //         (eg 123.123.123.123) alike without requiring resolution.
                /*
                if (h != null &&
                    Protocol.isValid(address.getScheme())) {
                    InetAddress ia = null;

                    try {
                        ia = InetAddress.getByName(h);
                    } catch (UnknownHostException uhe) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid address", uhe);
                        }
                    }

                    if (ia == null) {
                        b.append((b.length() > 0 ? NEW_LINE : EMPTY_STRING) + "invalid host: " + h);
                    }
            }
                */

                if (address.getPort() < minimumPort ||
                    address.getPort() > Default.MAXIMUM_PORT) {
                    b.append((b.length() > 0 ? NEW_LINE : EMPTY_STRING) + "invalid port: " + address.getPort());
                }
            }
        } else {
            b.append((b.length() > 0 ? NEW_LINE : EMPTY_STRING) + "null address");
        }

        return b.toString();
    }

    /**
     * {@link net.jxta.ext.config.Address} normalizer.
     *
     * @param   address     address
     * @return              normalized {@link java.net.URI}
     */
    
    public static URI normalize(Address address) {
        return normalize(address, true);
    }

    /**
     * {@link net.jxta.ext.config.Address} normalizer including port availability.
     *
     * @param   address     address
     * @param   portScan    port availability check
     * @return              normalized {@link java.net.URI}
     */

    public static URI normalize(Address address, boolean portScan) {
        URI u = address != null ? address.getAddress() : null;

        return normalize(address, portScan,
                         model(u != null ? u.getScheme() : null));
    }

    /**
     * {@link net.jxta.ext.config.Address} normalizer including port availability and {@link java.net.URI}
     * template.
     *
     * @param   address     address
     * @param   portScan    port availability check
     * @param   model       template {@link java.net.URI}
     * @return              normalized {@link java.net.URI}
     */
    
    public static URI normalize(Address address, boolean portScan, URI model) {
        URI u = address != null ? address.getAddress() : null;

        if (u != null) {
            String s = u.getScheme();
            String h = u.getHost();
            int p = u.getPort();

            if (model != null) {
                if (s == null ||
                    s.trim().length() == 0) {
                    s = model.getScheme();
                }

                if (h == null ||
                    h.trim().length() == 0) {
                    h = model.getHost();
                }

                if (p == Default.INVALID_PORT) {
                    p = model.getPort();
                }
            }

            try {
                address.setAddress(new URI(u.getScheme(), u.getUserInfo(),
                                           h, p, u.getPath(), u.getQuery(),
                                           u.getFragment()));

                u = new URI(u.getScheme(), u.getUserInfo(), h,
                            portScan ? getNextAvailablePort(address, p) : p,
                            u.getPath(), u.getQuery(), u.getFragment());
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid transformation", use);
                }
            }
        }

        return u;
    }

    /**
     * {@link java.net.URI} factory.
     *
     * @param   scheme  scheme specification
     * @param   host    host specification
     * @param   port    port specification
     * @return          newly constructed {@link java.net.URI}
     */
    
    public static URI toURI(String scheme, String host, int port) {
        URI u = null;

        try {
            u = new URI(scheme, EMPTY_STRING,
                        host == null || host.trim().length() == 0 ? getLocalHost() : host,
                        port, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        } catch (URISyntaxException use) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid transformation", use);
            }
        }

        return u;
    }

    /**
     * {@link java.net.URI} factory.
     *
     * @param   scheme  scheme specification
     * @return          newly constructed {@link java.net.URI}
     */
    
    public static URI model(String scheme) {
        return model(scheme, Default.INVALID_PORT);
    }
    
    /**
     * {@link java.net.URI} factory.
     *
     * @param   scheme  scheme specification
     * @param   port    port specification
     * @return          newly constructed {@link java.net.URI}
     */

    public static URI model(String scheme, int port) {
        URI u = null;

        if (Protocol.TCP.equalsIgnoreCase(scheme)) {
            u = toURI(Default.ANY_TCP_ADDRESS.getScheme(), null,
                      port != Default.INVALID_PORT ?
                      port : Default.TCP_PORT);
        } else if (Protocol.HTTP.equalsIgnoreCase(scheme)) {
            u = toURI(Default.ANY_HTTP_ADDRESS.getScheme(), null,
                      port != Default.INVALID_PORT ?
                      port : Default.HTTP_PORT);
        } else if (Protocol.UDP.equalsIgnoreCase(scheme)) {
            u = toURI(Default.ANY_UDP_ADDRESS.getScheme(),
                      Default.MULTICAST_ADDRESS.getHost(),
                      port != Default.INVALID_PORT ?
                      port : Default.MULTICAST_PORT);
        }

        return u;
    }

    /**
     * Checks the specified port for availability.
     *
     * @param   address     specified address
     * @param   port        specified port
     * @return              port availablity results
     */
    
    public static boolean isPortAvailable(InetAddress address, int port) {
        ServerSocket ss = getServerSocket(address, port);
        boolean isAvailable = ss != null;

        if (ss != null) {
            try {
                ss.close();
            } catch (IOException ioe) {}
        }

        ss = null;

        return isAvailable;
    }

    /**
     * {@link java.net.ServerSocket} factory.
     *
     * @param   address     specified address
     * @param   port        specified port
     * @return              newly created {@link java.net.ServerSocket}
     */
    
    public static ServerSocket getServerSocket(InetAddress address, int port) {
        ServerSocket ss = null;

        try {
            ss = new ServerSocket(port, 0,
                                  address != null &&
                                  ! address.getHostAddress().equals(Env.ALL_ADDRESSES.getHostAddress()) ?
                                  address : null);
        } catch (BindException be) {}
        catch (IOException ioe) {}

        return ss;
    }

    /**
     * Accessor for user-agent proxy.
     *
     * @return          user-agent proxy
     */
    
    public static String getProxyFromUserAgent() {
        String p = null;

        return p;
    }

    /**
     * Address non-routable validator.
     *
     * @param   u       specified address
     * @return          address non-routability indicator
     */
    
    public static boolean isNonRoutable(URI u) {
        boolean isNonRoutable = false;
        String a = u != null ? u.getHost() : null;

        if (a != null) {
            for (Iterator r = Env.NON_ROUTABLE_ADDRESSES.iterator();
                 ! isNonRoutable && r.hasNext(); ) {
                if (a.startsWith((String)r.next())) {
                    isNonRoutable = true;
                }
            }
        }

        return isNonRoutable;
    }

    /**
     * Address multicast validator.
     *
     * @param   u       specified address
     * @return          address multicast indicator
     */
    
    public static boolean isMulticast(URI u) {
        String h = u != null ? u.getHost() : null;
        InetAddress ia =  null;

        if (h != null) {
            try {
                ia = InetAddress.getByAddress(h, inetAddressToBytes(h));
            } catch (UnknownHostException uhe) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid address", uhe);
                }
            }
        }

        return ia != null ? ia.isMulticastAddress() : false;
    }

    /**
     * Address to byte array converter.
     *
     * @param   ipAddr      specified address
     * @return              byte array address equivalent
     */
    
    public static byte[] inetAddressToBytes(String ipAddr) {
        byte[] bytes = new byte[4];
        int bit = 0;

        for(int i = 0; i < ipAddr.length(); i++) {
            char c = ipAddr.charAt(i);

            switch(c) {
            case '.':
                bit++;
                break;
            default:
                bytes[bit] = (byte)(bytes[bit]*10 + Character.digit(c, 10));
                break;
            }
        }

        return bytes;
    }
 
    private static int getNextAvailablePort(Address a, int defaultPort) {
        URI u = a != null ? a.getAddress() : null;
        String h = u != null ? u.getHost() : null;
        int p = u != null ? u.getPort() : Default.INVALID_PORT;
        int port = p != Default.INVALID_PORT ? p : defaultPort;
        int pr = a != null ? a.getPortRange() : Default.PORT_RANGE;
        InetAddress ia = null;

        if (h != null) {
            try {
                ia = InetAddress.getByAddress(h, inetAddressToBytes(h));
            } catch (UnknownHostException uhe) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid address", uhe);
                }
            }

            if (ia != null &&
                p != Default.INVALID_PORT) {
                for (int i = p; i <= p + pr; i++) {
                    if (isPortAvailable(ia, i)) {
                        port = i;

                        break;
                    }
                }
            }
        }

        return port;
    }
}

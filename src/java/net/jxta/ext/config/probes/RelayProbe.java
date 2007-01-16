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
 *  $Id: RelayProbe.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
 */
package net.jxta.ext.config.probes;

import net.jxta.ext.config.Address;
import net.jxta.ext.config.Env;
import net.jxta.ext.config.PublicAddress;
import net.jxta.ext.config.Transport;
import net.jxta.ext.config.Util;
import net.jxta.ext.http.Dispatcher;
import net.jxta.ext.http.Message;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A systems connectivity test.
 *
 * @author      james todd [gonzo at jxta.dot org]
 */

public class RelayProbe {

    private final static long MAX_WAIT = 7 * 1000;
    private final static String ORIGINATOR = "originator";
    private final static String DELIMITER = ":";
    private final static Logger LOG = Logger.getLogger(RelayProbe.class.getName());

    private List transports = null;
    private List reflection = null;
    private String datagram = null;

    /**
     * Constructor which specifies a {@link java.util.List} of
     * {@link net.jxta.ext.service.reflection.ReflectionServlet Reflection Service} providers
     * that are to be validated against the provided {@link net.jxta.ext.config.Transport}.
     *
     * @param   reflection      {@link java.util.List} of reflection test providers
     * @param   transports      {@link java.util.List} of test addresses
     */
    
    public RelayProbe(List reflection, List transports) {
        if (reflection != null) {
            Collections.shuffle(reflection);
        }

        this.reflection = reflection;
        this.transports = transports;
    }

    /**
     * Performs the connectivity test.
     *
     * @return          connectivity test results
     */
    
    public boolean probe() {
        boolean reachible = false;

        if (this.reflection == null ||
            this.reflection.size() == 0 ||
            this.transports == null ||
            this.transports.size() == 0) {
            return reachible;
        }

        List inbound = getIncoming();

        if (inbound == null ||
            inbound.size() == 0) {
            return reachible;
        }

        List sockets = new ArrayList();

        for (Iterator r = this.reflection.iterator();
             ! reachible && r.hasNext(); ) {
            URL reflection = null;

            try {
                reflection = ((URI)r.next()).toURL();
            } catch (MalformedURLException mue) {}

            if (reflection != null) {
                sockets = bindSockets(inbound);

                if (sockets != null &&
                    sockets.size() > 0 &&
                    this.datagram != null &&
                    this.datagram.length() > 0) {
                    for (Iterator s = sockets.iterator(); s.hasNext(); ) {
                        ((Thread)s.next()).start();
                    }

                    String response = dispatch(reflection, this.datagram, MAX_WAIT * sockets.size());

                    for (Iterator s = sockets.iterator(); s.hasNext(); ) {
                        ((Thread)s.next()).interrupt();
                    }

                    reachible = isReachible(response);
                }

                sockets.clear();
            }
        }

        return reachible;
    }

    private List getIncoming() {
        List inbound = new ArrayList();

        for (Iterator ti = this.transports.iterator(); ti.hasNext(); ) {
            Transport t = (Transport)ti.next();

            if (t.isIncoming()) {
                boolean publicOnly = false;

                for (Iterator pi = t.getPublicAddresses().iterator(); pi.hasNext(); ) {
                    PublicAddress pa = (PublicAddress)pi.next();

                    publicOnly |= pa.isExclusive();

                    if (! Util.isMulticast(pa.getAddress()) &&
                        ! Env.ALL_ADDRESSES.getHostAddress().equals(pa.getAddress().getHost())) {
                        inbound.add(pa);
                    }
                }

                if (! publicOnly) {
                    for (Iterator ai = t.getAddresses().iterator(); ai.hasNext(); ) {
                        Address a = (Address)ai.next();

                        if (a.getAddress() != null &&
                            ! Util.isMulticast(a.getAddress()) &&
                            ! Env.ALL_ADDRESSES.getHostAddress().equals(a.getAddress().getHost())) {
                            inbound.add(a);
                        }
                    }
                }
            }
        }

        return inbound;
    }

    private List bindSockets(List inbound) {
        List pings = new ArrayList();
        StringBuffer sb = new StringBuffer();

        for (Iterator i = inbound.iterator(); i.hasNext(); ) {
            Address a = (Address)i.next();
            URI u = a != null ? a.getAddress() : null;
            String h = u != null ? u.getHost() : null;
            int port = u != null ? u.getPort() : -1;

            if (h != null &&
                port > -1) {
                InetAddress ia = null;

                try {
                    ia = InetAddress.getByAddress(h, Util.inetAddressToBytes(h));
                } catch (UnknownHostException uhe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid address", uhe);
                    }
                }

                // xxx: should latch to socket during normalization process
                if (ia != null) {
                    ServerSocket ss = Util.getServerSocket(ia, port);

                    if (ss != null) {
                        pings.add(new PingPong(ss));
                        sb.append(u + (i.hasNext() ? "\n" : ""));
                    }
                }
            }
        }

        this.datagram = sb.toString().trim();

        return pings;
    }

    private String dispatch(URL reflection, String msg, long wait) {
        Message r = null;

        try {
            r = new Dispatcher(reflection,
                               new Message(msg.toString().trim()), wait).dispatch();
        } catch (IOException ioe) {}

        return r != null && r.getBody() != null ?
               r.getBody().trim() : null;
    }

    private boolean isReachible(String response) {
        boolean reachible = false;

        if (response != null &&
            response.length() > 0) {
            BufferedReader br = new BufferedReader(new StringReader(response));
            String ln = null;
            int i = -1;

            try {
                while ((ln = br.readLine()) != null &&
                       ! reachible) {
                    if (! ln.toLowerCase().startsWith(ORIGINATOR)) {
                        i = ln.lastIndexOf(DELIMITER);

                        if (i > -1 &&
                            i + DELIMITER.length() < ln.length()) {
                            reachible = Boolean.valueOf(ln.substring(i +
                                                        DELIMITER.length())).booleanValue();
                        }
                    }
                }
            } catch (IOException ioe) {}
        }

        return reachible;
    }
}

/**
 * Trivial connectivity test protocol.
 */

class PingPong
    extends Thread {

    private static final String PING = "ping";
    private static final String PONG = "pong";
    private static final int MAX = 25;

    private ServerSocket server = null;

    /**
     * Constructor which manages the specified {@link java.net.ServerSocket}.
     *
     * @param   server      {@link java.net.ServetSocket} server
     */
    
    public PingPong(ServerSocket server) {
        this.server = server;

        setDaemon(true);
        setName(getClass().getName());
    }

    /**
     * Peforms the connectivity test.
     */
    
    public void run() {
        try {
            this.server.setSoTimeout(50);
        } catch (SocketException se) {}

        // xxx: handle sequentially
        while (! isInterrupted()) {
            try {
                handle(this.server.accept());
            } catch (IOException ioe) {}
        }

        if (this.server != null) {
            try {
                this.server.close();
            } catch (IOException ioe) {}

            this.server = null;
        }
    }

    private void handle(Socket s)
    throws IOException {
        InputStream is = s.getInputStream();
        StringBuffer sb = new StringBuffer();
        int c = -1;

        while ((c = is.read()) != -1) {
            sb.append((char)c);

            if (sb.toString().toLowerCase().indexOf(PING) > -1) {
                OutputStream os = s.getOutputStream();

                os.write(PONG.getBytes());
                os.flush();
                os.close();

                break;
            }

            if (sb.length() > MAX) {
                break;
            }
        }
    }
}

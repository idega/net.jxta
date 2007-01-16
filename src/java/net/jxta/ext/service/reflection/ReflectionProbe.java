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
*  $Id: ReflectionProbe.java,v 1.1 2007/01/16 11:02:07 thomas Exp $
*/

package net.jxta.ext.service.reflection;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A reflection test implementation.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class ReflectionProbe {

    /**
     * PING protocol: {@value}
     */
    
    public static final String PING = "ping";
    
    /**
     * PONG protocol: {@value}
     */
    
    public static final String PONG = "pong";

    /**
     * Maximum wait interval: {@value}
     */
    
    static final int MAXTIME = 7 * 1000;
    
    /**
     * Wait interval: {@value}
     */
    
    static final int INTERVAL = 50;

    private boolean isDone = false;

    /**
     * Default constructor.
     */
    
    public ReflectionProbe() {}

    /**
     * Execute test.
     *
     * @param       t       {@link java.util.List}  test addresses
     * @return              {@link java.util.List} of test results
     */
    
    public List probe(List t) {
        List r = new ArrayList();
        Probe probe = null;

        for (Iterator i = t != null ?
            t.iterator() : Collections.EMPTY_LIST.iterator();
            i.hasNext() && r.size() == 0; ) {
            probe = new Probe((URI)i.next());

            probe.start();

            long then = System.currentTimeMillis() + MAXTIME;

            while (! probe.isDone() &&
                System.currentTimeMillis() < then) {
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException ie) {}
            }

            probe.stop();

            if (probe.isValid()) {
                r.add(probe.getURI());
            }
        }

        return r;
    }
}

/**
 * Test handler.
 */

class Probe
    implements Runnable {

    private static final char DOT = '.';

    private URI u = null;
    private boolean isValid = false;
    private boolean isDone = false;
    private Socket s = null;
    private InputStream is = null;
    private OutputStream os = null;
    private Thread t = null;

    /**
     * Constructor that specifies a destination {@link java.net.URI}.
     *
     * @param       u       test {@link java.net.URI}
     */
    
    public Probe(URI u) {
        this.u = u;
    }

    /**
     * Accessor to the {@link java.net.URI} test address.
     *
     * @return          {@link java.net.URI} test address
     */
    
    public URI getURI() {
        return u;
    }

    /**
     * Accessor to validity attribute.
     *
     * @return          validity attribute
     */
    
    public boolean isValid() {
        return this.isValid;
    }

    /**
     * Accessor to the test completion state.
     *
     * @return              test completion state
     */
    
    public boolean isDone() {
        return this.isDone;
    }

    /**
     * Starts the test.
     */
    
    public void start() {
        this.t = new Thread(this, Probe.class.getName());

        this.t.setDaemon(true);
        this.t.start();
    }

    /**
     * Stops the test.
     */
    
    public void stop() {
        if (this.t != null) {
            this.t.interrupt();
        }

        this.isDone = true;

        cleanup();
    }

    /**
     * Executes the test.
     */
    
    public void run() {
        connect(getAddress(this.u.getHost(), this.u.getPort()));
        probe();
        stop();
    }

    private SocketAddress getAddress(String s, int p) {
        SocketAddress sa = null;

        try {
            sa = new InetSocketAddress(InetAddress.getByAddress(toBytes(s)), p);
        } catch (UnknownHostException uhe) {}

        return sa;
    }

    private void connect(SocketAddress sa) {
        if (sa != null) {
            this.s = new Socket();

            try {
                this.s.connect(sa, ReflectionProbe.MAXTIME);
            } catch (IOException ioe) {}
        }

        if (this.s != null &&
            this.s.isConnected()) {
            try {
                this.is = s.getInputStream();
            } catch (IOException ioe) {}

            try {
                this.os = s.getOutputStream();
            } catch (IOException ioe) {}
        }
        else {
            stop();
        }
    }

    private byte[] toBytes(String s) {
        byte[] b = null;
        byte[] a = s != null ? s.trim().getBytes() : null;

        if (a != null &&
            a.length > 0) {
            b = new byte[4];

            int j = 0;
            char c;

            for (int i = 0; i < a.length; i++ ) {
                c = (char)a[i];

                if (c != DOT) {
                    b[j] = (byte)(b[j] * 10 + Character.digit(c, 10));
                } else {
                    j++;
                }
            }
        }

        return b;
    }

    private void probe() {
        if (! this.isDone &&
            this.is != null &&
            this.os != null) {
            try {
                this.os.write(ReflectionProbe.PING.getBytes());
                this.os.flush();
            } catch (IOException ioe) {}

            StringBuffer sb = new StringBuffer();

            try {
                int c;
                boolean done = false;

                while (! done &&
                       (c = this.is.read()) != -1) {
                    sb.append(Character.toLowerCase((char)c));

                    if (sb.indexOf(ReflectionProbe.PONG) > -1) {
                        done = true;
                        this.isValid = true;
                    }
                }
            } catch (IOException ioe) {}
        }
    }

    private void cleanup() {
        if (this.is != null) {
            try {
                this.is.close();
            } catch (IOException ioe) {}
        }

        this.is = null;

        if (this.os != null) {
            try {
                this.os.close();
            } catch (IOException ioe) {}
        }

        this.os = null;

        if (this.s != null) {
            try {
                this.s.close();
            } catch (IOException ioe) {}
        }

        this.s = null;
    }
}
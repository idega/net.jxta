/*
 *
 * $Id: HttpMessageReceiver.java,v 1.1 2007/01/16 11:02:06 thomas Exp $
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
 */

package net.jxta.impl.endpoint.servlethttp;


import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.MultiException;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.config.Config;
import net.jxta.impl.util.TimeUtils;


/**
 * Simple Message Receiver for server side.
 **/
public class HttpMessageReceiver implements MessageReceiver {

    /**
     *    Log4j logger
     **/
    private static final Logger LOG = Logger.getLogger(HttpMessageReceiver.class.getName());

    /**
     * the relative URI of where the message receiver servlet will be
     * mounted
     */
    private final static String MSG_RECEIVER_RELATIVE_URI = "/*";

    /**
     * The ServletHttpTransport that created this MessageReceiver
     **/
    private final ServletHttpTransport servletHttpTransport;

    /**
     * The public address is of the form jxta://peerId since the
     **/
    private final List publicAddresses;

    /**
     * the min threads that the http server will use for handling requests
     **/
    private static int minThreads = 10;
    
    /**
     * the max threads that the http server will use for handling requests
     **/
    private static int maxThreads = 100;
    
    /**
     * how long a thread can remain idle until the worker thread is let go
     **/
    private static long maxThreadIdleTime = 10 * TimeUtils.ASECOND;
    
    /**
     * how long an http request has to finish transferring before the http
     * server discards the request.
     *
     * <p/>Jetty does not kill existing connections when stopping, so this
     * is also the time it takes for everything to stop if there's
     * a thread wainting on an idle connection.
     */
    private static long maxReqReadTime = 30 * TimeUtils.ASECOND;
    
    /**
     * the Jetty HTTP Server instance
     **/
    private final HttpServer server;
    private final ServletHandler handler;
    private final SocketListener listener;

    /**
     * The listener to invoke when making an incoming messenger.
     **/
    private final MessengerEventListener messengerEventListener;

    public HttpMessageReceiver(ServletHttpTransport servletHttpTransport,
            List publicAddresses,
            InetAddress useInterface, int port) throws PeerGroupException {
        this.servletHttpTransport = servletHttpTransport;
        this.publicAddresses = publicAddresses;

        // read settings from the properties file
        Properties prop = getJxtaProperties();
        
        initFromProperties(prop);
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info(
                    "Min threads=" + minThreads + "\tMax threads=" + maxThreads + "\n\tMax thread idle time=" + maxThreadIdleTime
                    + "ms\tMax request read time=" + maxReqReadTime + "ms");
        }
        
        // Disabled Jetty Log
        Log.instance().disableLog();

        // Initialize the Jetty HttpServer
        InetAddrPort addrPort = new InetAddrPort(useInterface, port);

        listener = new SocketListener(addrPort);

        listener.setMinThreads(minThreads);
        listener.setMaxThreads(maxThreads);
        listener.setMaxIdleTimeMs((int) maxThreadIdleTime);

        // Create the Jetty http server and add the listener
        server = new HttpServer();
        server.addListener(listener);

        // Create a context for the handlers at the root, then added a servlet
        // handler for the specified servlet class and add it to the context
        HttpContext handlerContext = server.getContext("/");

        handler = new ServletHandler();

        handler.setUsingCookies(false);
        handler.initialize(handlerContext);

        // Use peer group class loader (useful for HttpMessageServlet)
        handlerContext.setClassLoader(servletHttpTransport.getEndpointService().getGroup().getLoader());
        handlerContext.addHandler(handler);

        // Set up support for downloading midlets.
        if (System.getProperty("net.jxta.http.allowdownload") != null) {
            HttpContext context = server.addContext("/midlets/*");

            context.setResourceBase("./midlets/");
            // context.setDirAllowed(false);
            // context.setServingResources(true);

            // String methods[] = {"GET"};
            ResourceHandler resHandler = new ResourceHandler();

            // resHandler.setAllowedMethods(methods);
            context.addHandler(resHandler);
        }

        handler.addServlet(MSG_RECEIVER_RELATIVE_URI, HttpMessageServlet.class.getName());

        // XXX bondolo 20040628 shouldn't we wait until after server startup?
        messengerEventListener = servletHttpTransport.getEndpointService().addMessageTransport(this);
        if (messengerEventListener == null) {
            throw new PeerGroupException("Transport registration refused");
        }
    }

    synchronized void startServer() throws IOException {
        try {
            server.start();
            handler.getServletContext().setAttribute("HttpMessageReceiver", this);
        } catch (MultiException e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Could not start server", e);
            }

            IOException failure = new IOException("Could not start server");

            failure.initCause(e);
            throw failure;
        }
    }

    synchronized void stopServer() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Interrupted during stop()", e);
            }
        }
    }

    boolean messengerReadyEvent(HttpServletMessenger newMessenger, EndpointAddress connAddr) {
        return messengerEventListener.messengerReady(new MessengerEvent(this, newMessenger, connAddr));
    }

    /**
     * {@inheritDoc}
     **/
    public Iterator getPublicAddresses() {

        if (publicAddresses == null) {
            return Collections.EMPTY_LIST.iterator();
        }

        return Collections.unmodifiableList(publicAddresses).iterator();
    }

    /**
     * {@inheritDoc}
     **/
    public String getProtocolName() {
        return servletHttpTransport.protocolName;
    }

    /**
     * {@inheritDoc}
     **/
    public EndpointService getEndpointService() {
        return servletHttpTransport.getEndpointService();
    }
    
    /**
     * {@inheritDoc}
     **/
    public Object transportControl(Object operation, Object Value) {
        return null;
    }

    ServletHttpTransport getServletHttpTransport() {
        return servletHttpTransport;
    }

    /**
     * Returns a Properties instance for jxta.properties if the file exists;
     * otherwise, returns null.
     **/
    private static Properties getJxtaProperties() {
        Properties prop = new Properties();
        InputStream in = null;
        
        try {
            in = new FileInputStream(Config.JXTA_HOME + "jxta.properties");
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Read jxta.properties from " + Config.JXTA_HOME);
            }
        } catch (FileNotFoundException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("no jxta.properties in " + Config.JXTA_HOME);
            }
        }
        
        if (in != null) {
            try {
                prop.load(in);
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Error reading jxta.properties", e);
                }
            }
            finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                    ;
                }
                in = null;
            }
        } else {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("jxta.properties cannot be found");
            }
        }
        
        return prop;
    }
    
    /**
     * Reads the properties from the jxta.properties file
     **/
    private void initFromProperties(Properties prop) {
        
        if (prop != null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Using jxta.properties to configure HTTP server");
            }
            
            String minThreadsStr = prop.getProperty("HttpServer.MinThreads");
            String maxThreadsStr = prop.getProperty("HttpServer.MaxThreads");
            String maxReqReadTimeStr = prop.getProperty("HttpServer.MaxRequestReadTime");
            String maxThreadIdleTimeStr = prop.getProperty("HttpServer.MaxThreadIdleTime");
            
            try {
                if (minThreadsStr != null) {
                    minThreads = Integer.parseInt(minThreadsStr);
                }
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid HttpServer.MinThreads value; using default");
                }
            }
            
            try {
                if (maxThreadsStr != null) {
                    maxThreads = Integer.parseInt(maxThreadsStr);
                }
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid HttpServer.MaxThreads value; using default");
                }
            }
            
            try {
                if (maxReqReadTimeStr != null) {
                    maxReqReadTime = Integer.parseInt(maxReqReadTimeStr);
                }
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid HttpServer.MaxReqReadTime value; using default");
                }
            }
            
            try {
                if (maxThreadIdleTimeStr != null) {
                    maxThreadIdleTime = Integer.parseInt(maxThreadIdleTimeStr);
                }
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid HttpServer.MaxThreadIdleTime value; using default");
                }
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("jxta.properties not found: using default values");
            }
        }
    }
    
}

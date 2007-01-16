/*
 *
 * $Id: HttpMessageServlet.java,v 1.1 2007/01/16 11:02:06 thomas Exp $
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


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.meter.*;

import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.transportMeter.*;
import net.jxta.impl.util.TimeUtils;


/**
 * This is a simple servlet that accepts POSTed Jxta messages over HTTP
 * and hands them up to EndpointService. It also supports a ping operation. When
 * the URI is /ping, it simply responds with a 200.
 */
public class HttpMessageServlet extends HttpServlet {
    
    /**
     * Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(HttpMessageServlet.class.getName());
    
    private HttpMessageReceiver owner = null;
    
    /** The endpoint that the servlet is receiving messages for **/
    private EndpointService endpoint = null;
    private EndpointAddress localAddress = null;
    private byte[] pingResponseBytes;
    private ServletHttpTransport servletHttpTransport = null;
    
    // We set that to true to let threads know that this servlet
    // has been (is being) destroyed.
    private boolean destroyed = false;
    
    /**
     * Stores the endpoint from the ServletContext in to the data member
     * for easy access.
     **/
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        try {
            owner = (HttpMessageReceiver) getServletContext().getAttribute("HttpMessageReceiver");
            if (owner == null) {
                throw new ServletException("Servlet Context did not contain 'HttpMessageReceiver'");
            }
        } catch (ClassCastException e) {
            throw new ServletException("'HttpMessageReceiver' attribute was not of the proper type in the Servlet Context");
        }
        servletHttpTransport = owner.getServletHttpTransport();
        endpoint = owner.getEndpointService();
        
        String peerId = endpoint.getGroup().getPeerID().getUniqueValue().toString();

        localAddress = new EndpointAddress("jxta", peerId, null, null);
        
        try {
            pingResponseBytes = peerId.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException never) {
            // UTF-8 is always available.
            ;
        }
    }
    
    /**
     * Handle the ping by sending back a 200.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("GET " + req.getQueryString() + " thread = " + Thread.currentThread());
        }
        
        processRequest(req, res);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("GET done for thread = " + Thread.currentThread());
        }
    }
    
    /**
     * Handle posted messages. We first validate the message, which is where
     * we check the content-length of the message. If that passes
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("POST " + req.getQueryString() + " thread = " + Thread.currentThread());
        }
        
        processRequest(req, res);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("POST done for thread = " + Thread.currentThread());
        }
    }
    
    // Unlike what jetty's doc says, destroy is actually called when the
    // the server is stopped, even if there are server threads still
    // running this servlet. Good, that's what we need.
    
    public void destroy() {
        synchronized (this) {
            
            // All we need to do is wakeup the threads that are
            // waiting. (In truth we'll miss those that are waiting
            // on a messenger, but that'll do for now, because we do that
            // only when shutting down the group and then the realy will
            // be shutdown as well, which will take care of the messengers.
            destroyed = true;
            notifyAll();
        }
    }
    
    /**
     * Handles all request
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            final char nl = '\n';
            StringBuffer b = new StringBuffer();
            
            b.append("HTTP request:" + nl);
            b.append("  AUTH_TYPE: " + req.getAuthType() + nl);
            b.append("  CONTEXT_PATH: " + req.getContextPath() + nl);
            
            Cookie[] cookies = req.getCookies();
            
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    b.append("  COOKIE[" + i + "]:" + nl);
                    b.append("    comment: " + cookies[i].getComment() + nl);
                    b.append("    domain: " + cookies[i].getDomain() + nl);
                    b.append("    max age: " + cookies[i].getMaxAge() + nl);
                    b.append("    name: " + cookies[i].getName() + nl);
                    b.append("    path: " + cookies[i].getPath() + nl);
                    b.append("    secure: " + cookies[i].getSecure() + nl);
                    b.append("    value: " + cookies[i].getValue() + nl);
                    b.append("    version: " + cookies[i].getVersion() + nl);
                }
            }
            
            for (Enumeration headers = req.getHeaderNames(); headers.hasMoreElements();) {
                String header = (String) headers.nextElement();
                
                b.append("  HEADER[" + header + "]: " + req.getHeader(header) + nl);
            }
            
            b.append("  METHOD: " + req.getMethod() + nl);
            b.append("  PATH_INFO: " + req.getPathInfo() + nl);
            b.append("  PATH_TRANSLATED: " + req.getPathTranslated() + nl);
            b.append("  QUERY_STRING: " + req.getQueryString() + nl);
            b.append("  REMOTE_USER: " + req.getRemoteUser() + nl);
            b.append("  REQUESTED_SESSION_ID: " + req.getRequestedSessionId() + nl);
            b.append("  REQUEST_URI: " + req.getRequestURI() + nl);
            b.append("  SERVLET_PATH: " + req.getServletPath() + nl);
            b.append("  REMOTE_USER: " + req.getRemoteUser() + nl);
            b.append("  isSessionIdFromCookie: " + req.isRequestedSessionIdFromCookie() + nl);
            b.append("  isSessionIdFromURL: " + req.isRequestedSessionIdFromURL() + nl);
            b.append("  isSessionIdValid: " + req.isRequestedSessionIdValid() + nl);
            
            for (Enumeration attributes = req.getAttributeNames(); attributes.hasMoreElements();) {
                String attribute = (String) attributes.nextElement();
                
                b.append("  ATTRIBUTE[" + attribute + "]: " + req.getAttribute(attribute) + nl);
            }
            
            b.append("  ENCODING: " + req.getCharacterEncoding() + nl);
            b.append("  CONTENT_LENGTH: " + req.getContentLength() + nl);
            b.append("  CONTENT_TYPE: " + req.getContentType() + nl);
            b.append("  LOCALE: " + req.getLocale().toString() + nl);
            
            for (Enumeration parameters = req.getParameterNames(); parameters.hasMoreElements();) {
                String parameter = (String) parameters.nextElement();
                
                b.append("  PARAMETER[" + parameter + "]: " + req.getParameter(parameter) + nl);
            }
            
            b.append("  PROTOCOL: " + req.getProtocol() + nl);
            b.append("  REMOTE_ADDR: " + req.getRemoteAddr() + nl);
            b.append("  REMOTE_HOST: " + req.getRemoteHost() + nl);
            b.append("  SCHEME: " + req.getScheme() + nl);
            b.append("  SERVER_NAME: " + req.getServerName() + nl);
            b.append("  SERVER_PORT: " + req.getServerPort() + nl);
            b.append("  isSecure: " + req.isSecure());
            
            LOG.debug(b);
        }
        
        long requestStartTime = 0;
        long connectionEstablishedTime = 0;
        long lastReadWriteTime = 0;
        int requestSize = 0;
        TransportBindingMeter transportBindingMeter = null;
        
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            requestStartTime = System.currentTimeMillis();
            
            int contentLength = req.getContentLength();

            requestSize += (contentLength != -1) ? contentLength : 0;
        }
        
        // check if a peerId was given
        String requestorPeerId = getRequestorPeerId(req);
        
        // get the query string
        String queryString = req.getQueryString();
        String requestTimeoutString = null;
        String lazyCloseTimeoutString = null;
        String reqDestAddrString = null;
        
        if (queryString != null) {
            // the query string is of the format requestTimeout,lazyCloseTimeout
            // the times given are in milliseconds
            int commaIndex = queryString.indexOf(',');
            
            if (commaIndex == -1) {
                // there is no lazy close timeout
                requestTimeoutString = queryString;
                lazyCloseTimeoutString = null;
            } else {
                requestTimeoutString = queryString.substring(0, commaIndex);
                lazyCloseTimeoutString = queryString.substring(commaIndex + 1);
                commaIndex = lazyCloseTimeoutString.indexOf(',');
                if (commaIndex != -1) {
                    reqDestAddrString = lazyCloseTimeoutString.substring(commaIndex + 1);
                    lazyCloseTimeoutString = lazyCloseTimeoutString.substring(0, commaIndex);
                }
            }
        }
        
        // Protect agains clients that will try top have us keep
        // backchannel connections for ever. If they re-establish
        // all the time it's fine, but until we have a more sophisticated
        // mechanism, we want to make sure we quit timely if the client's gone.
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug(
                    "req.getQueryString() = " + queryString + "\trequestTimeoutString = " + requestTimeoutString + "\tlazyCloseTimeoutString = "
                    + lazyCloseTimeoutString + "\treqDestAddrString = " + reqDestAddrString);
        }
        
        // get the timeout if there is one
        long timeout = getRequestTimeout(requestTimeoutString);
        long lazyCloseTimeout = getLazyCloseTimeout(lazyCloseTimeoutString);
        
        // check for incoming message
        boolean hasMessageContent = hasMessageContent(req);
        
        // check if this is a ping request, no requestor peerId or incoming message
        if (null == requestorPeerId && !hasMessageContent) {
            // this is only a ping request
            pingResponse(res);
            
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                long connectionTime = System.currentTimeMillis() - requestStartTime;
                EndpointAddress sourceAddress = new EndpointAddress("http", req.getRemoteHost(), null, null); //

                transportBindingMeter = servletHttpTransport.getTransportBindingMeter(requestorPeerId, sourceAddress);
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionEstablished(false, connectionTime);
                    transportBindingMeter.dataReceived(false, requestSize);
                    transportBindingMeter.dataSent(false, 0);
                    transportBindingMeter.pingReceived();
                    transportBindingMeter.connectionClosed(false, connectionTime);
                }
            }
            
            return;
        }
        
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            lastReadWriteTime = connectionEstablishedTime = System.currentTimeMillis();
            long connectTime = connectionEstablishedTime - requestStartTime;
            EndpointAddress sourceAddress = new EndpointAddress("http", req.getRemoteHost(), null, null); //

            transportBindingMeter = servletHttpTransport.getTransportBindingMeter(requestorPeerId, sourceAddress);
            if (transportBindingMeter != null) {
                transportBindingMeter.connectionEstablished(false, connectTime);
                transportBindingMeter.dataReceived(false, requestSize);
            }
        }
        
        // check if the request included polling (valid requestor peerId and timeout not -1)
        HttpServletMessenger messenger = null;

        if (null != requestorPeerId && -1 != timeout && null != reqDestAddrString) {
            // create the back channel messenger
            EndpointAddress incomingAddr = new EndpointAddress("jxta", requestorPeerId, null, null);
            
            EndpointAddress reqDestAddr = new EndpointAddress(reqDestAddrString);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Creating back channel messenger for " + incomingAddr + " ( " + reqDestAddr + ")");
            }
            
            messenger = new HttpServletMessenger(owner.getEndpointService().getGroup().getPeerGroupID(), localAddress, incomingAddr);
            boolean taken = owner.messengerReadyEvent(messenger, reqDestAddr);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Incoming messenger to: " + incomingAddr + " taken=" + taken);
            }
            
            if (!taken) {
                messenger.close();
                messenger = null;
            }
        }
        
        // get the incoming message is there is one
        if (hasMessageContent) {
            Message incomingMessage = null;
        
            // read the stream
            InputStream in = req.getInputStream();
            
            // construct the message. Send BAD_REQUEST if the message construction
            // fails
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Reading message from request");
                }
                
                // FIXME 20040927 bondolo Should get message mimetype from http header.
                // FIXME 20040927 bondolo Should get message encoding from http header.
                incomingMessage = WireFormatMessageFactory.fromWire(in, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE, (MimeMediaType) null);
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    lastReadWriteTime = System.currentTimeMillis();
                    long receiveTime = lastReadWriteTime - connectionEstablishedTime;

                    transportBindingMeter.messageReceived(false, incomingMessage, receiveTime, 0); // size=0 since it was already incorporated in the request size
                }
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Malformed JXTA message, responding with BAD_REQUEST", e);
                }
                
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Message was not a valid JXTA message");
                
                // close the messenger if there was one
                if (null != messenger) {
                    messenger.close();
                }
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.connectionDropped(false, System.currentTimeMillis() - requestStartTime);
                }
                
                // stop processing this request
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Ending request without returning a message");
                }
                return;
            }
        
            // check if there was an incoming message
            if (null != incomingMessage) {
                // post the incoming message to the endpoint demux
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Demuxing " + incomingMessage);
                }
            
                try {
                    endpoint.demux(incomingMessage);
                } catch (Throwable e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Failure demuxing an incoming message", e);
                    }
                }
            }
        
        }
        
        // valid request, send back OK response
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending OK in response to request");
        }
        
        // We may later decide that contentLength should not be set after all.
        // if we use chunking. Otherwise we must set it; specially to zero, so that
        // jetty does not forcefully close the connection after each message in order
        // to complete the transaction http-1.0-style.
        
        boolean mustSetContentLength = true;
        
        res.setStatus(HttpServletResponse.SC_OK);
        
        // if there is a messenger, start polling for messages to send on the back channel
        if (LOG.isEnabledFor(Level.DEBUG)) {
            if (messenger != null) {
                LOG.debug("messenger.isClosed() = " + messenger.isClosed());
            }
        }
        
        // Check if the back channel is to be used for sending messages.
        // We use a do construct to facilitate break out of the block
        // down to common code.
        if (timeout != -1) {
            do {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Wait for message from the messenger timeout = " + timeout);
                }
                
                Message outMsg = null;
                
                if (messenger == null) {
                    try {
                        long left;
                        long quitAt = System.currentTimeMillis() + timeout;
                        
                        synchronized (this) {
                            while ((left = quitAt - System.currentTimeMillis()) > 0 && !destroyed) {
                                wait(left);
                            }
                        }
                    } catch (InterruptedException ie) {
                        // Ok. Leave early, then.
                        Thread.interrupted();
                    }
                    
                    continue;
                } else {
                    // send a message if there is one
                    try {
                        outMsg = messenger.waitForMessage(timeout);
                    } catch (InterruptedException ie) {
                        // Ok. Leave early, then.
                        Thread.interrupted();
                        continue;
                    }
                }
                
                if (outMsg == null) {
                    // close the messenger
                    if (messenger != null) {
                        messenger.close();
                    }
                    
                    // done processing the request
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Terminating request with no message to send.");
                    }
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        transportBindingMeter.connectionClosed(false, System.currentTimeMillis() - requestStartTime);
                    }
                    
                    // We know we did not respond anything. Do not set content-length
                    // In general it's better if jetty closes the connection here, because
                    // it could have been an unused back-channel and the client has to open
                    // a new one next time, thus making sure we get to see a different URL
                    // (if applicable). Jdk should do that anyway, but ... ).
                    return;
                }
                
                // send the message
                WireFormatMessage serialed = WireFormatMessageFactory.toWire(outMsg, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE, (MimeMediaType[]) null);
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sending " + outMsg + " on back channel to " + req.getRemoteHost());
                }
                
                // if only one message is being returned, set the content length, otherwise try to use chunked encoding
                if (lazyCloseTimeout == -1) {
                    res.setContentLength((int) serialed.getByteLength());
                }
                
                // Either way, we've done what had to be done.
                mustSetContentLength = false;
                
                // get the output stream for the response
                OutputStream out = res.getOutputStream();
                
                // send the message
                try {
                    serialed.sendToStream(out);
                    out.flush();
                    
                    messenger.messageSent(true);
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Successfully sent " + outMsg + " on back channel to " + req.getRemoteHost());
                    }
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        lastReadWriteTime = System.currentTimeMillis();
                        long sendTime = lastReadWriteTime - connectionEstablishedTime;
                        long bytesSent = serialed.getByteLength();

                        transportBindingMeter.messageSent(false, outMsg, sendTime, bytesSent);
                    }
                } catch (IOException ex) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Failed sending Message on back channel to " + req.getRemoteHost());
                    }
                    
                    messenger.messageSent(false);
                    
                    // make sure the response is pushed out
                    res.flushBuffer();
                    
                    // close the messenger
                    messenger.close();
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        transportBindingMeter.connectionDropped(false, System.currentTimeMillis() - requestStartTime);
                    }
                    
                    throw ex;
                }
                
                // check for lazy close option
                if (lazyCloseTimeout == -1) {
                    messenger.close();
                    break;
                }
                
                long quitAt = System.currentTimeMillis() + lazyCloseTimeout;

                while (!messenger.isClosed()) {
                    long tempTimeout = quitAt - System.currentTimeMillis();
                    
                    if (tempTimeout <= 0) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("lazy close timed out");
                        }
                        
                        break;
                    }
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Wait for more messages from the messenger. timeout = " + tempTimeout);
                    }
                    
                    // send a message if there is one
                    try {
                        outMsg = messenger.waitForMessage(tempTimeout);
                    } catch (InterruptedException ie) {
                        // Ok. Leave early, then.
                        Thread.interrupted();
                        continue;
                    }
                    
                    // check if we got a message
                    if (outMsg == null) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("No additional messages to send in response to " + req.getRemoteHost());
                        }
                        
                        break;
                    }
                    
                    // send the message
                    serialed = WireFormatMessageFactory.toWire(outMsg, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE, (MimeMediaType[]) null);
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Sending additional message " + outMsg + " on back channel, length = " + serialed.getByteLength());
                    }
                    
                    // send the message
                    try {
                        serialed.sendToStream(out);
                        out.flush();
                        
                        messenger.messageSent(true);
                        
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Successfully sent " + outMsg + " on back channel");
                        }
                        
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            lastReadWriteTime = System.currentTimeMillis();
                            long sendTime = lastReadWriteTime - connectionEstablishedTime;
                            long bytesSent = serialed.getByteLength();

                            transportBindingMeter.messageSent(false, outMsg, sendTime, bytesSent);
                        }
                    } catch (IOException ex) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Failed sending " + outMsg + " on back channel to " + req.getRemoteHost(), ex);
                        }
                        
                        messenger.messageSent(false);
                        
                        // make sure the response is pushed out
                        // res.flushBuffer();
                        
                        // close the messenger
                        messenger.close();
                        
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionDropped(false, System.currentTimeMillis() - requestStartTime);
                        }
                        
                        throw ex;
                    }
                }
                
                // close the messenger
                messenger.close();
            } while (false);
        } // see comment in matching do
        
        // If contentLength was never set and we have not decided *not* to set it, then we
        // must set it to 0 (that's the truth in that case). This allows Jetty to keep to
        // keep the connection open unless what's on the other side is a 1.0 proxy.
        if (mustSetContentLength) {
            res.setContentLength(0);
        }
        
        // done processing the request
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Finished processing the request from " + req.getRemoteHost());
        }
        
        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
            transportBindingMeter.connectionClosed(false, System.currentTimeMillis() - requestStartTime);
        }
    }
    
    /**
     * Returns the peerId of the peer making the request, if given
     */
    private static String getRequestorPeerId(HttpServletRequest req) {
        // get the potential PeerId from the PathInfo
        String requestorPeerId = req.getPathInfo();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("req.getPathInfo() = " + requestorPeerId);
        }
        
        if (null != requestorPeerId) {
            int begin = 0;
            int end = requestorPeerId.length();
            
            // check for all leading "/"
            while (begin < end && requestorPeerId.charAt(begin) == '/') {
                begin++;
            }
            
            // check for all trailing "/"
            while (end - begin > 0 && requestorPeerId.charAt(end - 1) == '/') {
                end--;
            }
            
            if (begin == end) {
                // nothing left of the string
                requestorPeerId = null;
            } else {
                // get the new substring
                requestorPeerId = requestorPeerId.substring(begin, end);
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("requestorPeerId = " + requestorPeerId);
        }
        
        return requestorPeerId;
    }
    
    /**
     * Returns the request timeout or -1 if a request timeout is not given
     */
    private static long getRequestTimeout(String requestTimeoutString) {
        // the default timeout is -1, which means do not return a message
        long timeout = -1;
        
        if (null != requestTimeoutString) {
            try {
                timeout = Long.parseLong(requestTimeoutString);
                
                // Protect agains clients that will try to have us keep
                // backchannel connections for ever. If they re-establish
                // all the time it's fine, but until we have a more
                // sophisticated mechanism, we want to make sure we quit
                // timely if the client's gone.
                // 0 is supposed to mean forever but we do not comply
                // either.
                if (timeout > (2 * TimeUtils.AMINUTE) || timeout == 0) {
                    timeout = (2 * TimeUtils.AMINUTE);
                }
                
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("The requestTimeout does not contain a decimal number " + requestTimeoutString);
                }
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("requestTimeout = " + timeout);
        }
        
        return timeout;
    }
    
    /**
     * Returns the lazy close timeout or -1 if a lazy close timeout is not given
     */
    private static long getLazyCloseTimeout(String lazyCloseTimeoutString) {
        // the default timeout is -1, which means do not wait for additional messages
        long timeout = -1;
        
        if (null != lazyCloseTimeoutString) {
            try {
                timeout = Long.parseLong(lazyCloseTimeoutString);
                
                // Protect agains clients that will try top have us keep
                // backchannel connections for ever. If they re-establish
                // all the time it's fine, but until we have a more
                // sophisticated mechanism, we want to make sure we quit
                // timely if the client's gone.
                // 0 is supposed to mean forever but we do not comply
                // either.
                if (timeout > (2 * TimeUtils.AMINUTE) || timeout == 0) {
                    timeout = (2 * TimeUtils.AMINUTE);
                }
                
            } catch (NumberFormatException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("the lazyCloseTimeoutString does not contain a decimal number " + lazyCloseTimeoutString);
                }
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("lazyCloseTimeout = " + timeout);
        }
        
        return timeout;
    }
    
    /**
     * Checks if the request includes a message as content
     */
    private static boolean hasMessageContent(HttpServletRequest req) {
        boolean hasContent = false;
        
        int contentLength = req.getContentLength();
        
        // if the content length is not zero, there is an incoming message
        // Either the message length is given or it is a chunked message
        if (contentLength > 0) {
            hasContent = true;
        } else if (contentLength == -1) {
            // check if the transfer encoding is chunked
            String transferEncoding = req.getHeader("Transfer-Encoding");

            hasContent = "chunked".equals(transferEncoding);
        }
        
        return hasContent;
    }
    
    /**
     * Returns a response to a ping request.  The response is the peerId of this peer
     */
    private void pingResponse(HttpServletResponse res) throws ServletException, IOException {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Responding with 200 and peerID to request");
        }
        
        res.setStatus(HttpServletResponse.SC_OK);
        
        res.setContentLength(pingResponseBytes.length);
        
        OutputStream out = res.getOutputStream();

        out.write(pingResponseBytes);
        out.flush();
        out.close();
    }
}

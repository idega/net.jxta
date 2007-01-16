/*
 *
 * $Id: HttpClientMessenger.java,v 1.1 2007/01/16 11:02:06 thomas Exp $
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
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.ConnectException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peer.PeerID;

import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;


/**
 * Simple messenger that simply posts a message to a URL.
 *
 * <p/>URL/HttpURLConnection is used, so (depending on your JDK) you will get
 * reasonably good persistent connection management.
 */
public class HttpClientMessenger extends BlockingMessenger {
    
    /**
     *    log4j logger
     **/
    private final static Logger LOG = Logger.getLogger(HttpClientMessenger.class.getName());
    
    /**
     * query string is of the format ?{request timeout},{lazy close timeout}
     * the timeout's are expressed in seconds.
     * -1 means do not wait at all, 0 means wait forever
     * default value is wait 2 minutes (120*1000 millis) for a message.
     * Lazy close timeout is set to the same. Try -1 if multiple msgs is trbl.
     **/
    private final static String DEFAULT_RECEIVER_QUERY_STRING = "?120000,120000";
    
    private final URL senderURL;

    /**
     * The ServletHttpTransport that created this object
     **/
    private final ServletHttpTransport servletHttpTransport;
    
    private final MessageElement srcAddressElement;
    
    private final EndpointAddress logicalDest;
    
    private TransportBindingMeter transportBindingMeter;
    
    private transient long lastUsed = System.currentTimeMillis();
    
    private BackChannelListener listener = null;
    
    /**
     * Warn only once about obsolete proxies.
     **/
    private static boolean neverWarned = true;
    
    /**
     *  Constructs the messenger.
     *
     *  @param servletHttpTransport The transport this messenger will work for.
     *  @param peerId The peerId of this peer.
     *  @param destAddr The destination address.
     **/
    public HttpClientMessenger(ServletHttpTransport servletHttpTransport,
            PeerID peerId,
            EndpointAddress destAddr) throws IOException {
        
        // We do use self destruction.
        super(servletHttpTransport.getEndpointService().getGroup().getPeerGroupID(), destAddr, true);
        
        this.servletHttpTransport = servletHttpTransport;
        
        // XXX bondolo 20041013 I removed some special non-standard IPv6 address handling here. I want to see if it breaks.
        
        String protoAddr = destAddr.getProtocolAddress();
        
        String host;
        int port;
        int lastColon = protoAddr.lastIndexOf(':');
            
        if ((-1 == lastColon) || (lastColon < protoAddr.lastIndexOf(']')) || ((lastColon + 1) == protoAddr.length())) {
            // There's no port or it's an IPv6 addr with no port or the colon is the last character.
            host = protoAddr;
            port = 80;
        } else {
            host = protoAddr.substring(0, lastColon);
            port = Integer.parseInt(protoAddr.substring(lastColon + 1));
        }
        
        URL receiverURL = new URL("http", host, port, "/" + peerId.getUniqueValue().toString() + DEFAULT_RECEIVER_QUERY_STRING + "," + destAddr);
        
        senderURL = new URL("http", host, port, "/");
        
        EndpointAddress srcAddr = new EndpointAddress("jxta", peerId.getUniqueValue().toString(), null, null);

        this.srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, srcAddr.toString(), null);
        
        logicalDest = retreiveLogicalDestinationAddress();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Connected to " + logicalDest);
        }
        
        // start receiving messages from the other peer
        listener = new BackChannelListener(receiverURL, servletHttpTransport.getEndpointService(), transportBindingMeter, this);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Created messenger for " + destAddr);
        }
    }
    
    /*
     * The cost of just having a finalize routine is high. The finalizer is
     * a bottleneck and can delay garbage collection all the way to heap
     * exhaustion. Leave this comment as a reminder to future maintainers.
     * Below is the reason why finalize is not needed here.
     *
     * These messengers never go to the application layer. Endpoint code does
     * call close when necessary.
     
     protected void finalize() {
     }
     
     */
    
    // We give this package access to the otherwise protected shutdown method.
    void doshutdown() {
        shutdown();
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized void closeImpl() {
        
        if (isClosed()) {
            return;
        }
        
        super.close();
        
        if (logicalDest == null || listener == null) {
            // This messenger constructor has failed early and now the
            // roadkill is being GC'ed. There's nothing to clean.
            // (Exceptions in ctors are always a bit of a problem, fortunately
            // java sets data members to a predictible value before
            // construction).
            // Note: since close is no-longer invoked by finalize, this
            // should not happen.
            
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Close messenger to " + logicalDest.toString());
        }
        
        BackChannelListener back = listener;

        listener = null;
        
        if (null != back) {
            back.stopReceiving();
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean sendMessageBImpl(Message message, String service, String serviceParam) throws IOException {
        
        if (isClosed()) {
            IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug(failure, failure);
            }
            
            throw failure;
        }
        
        // Set the message with the appropriate src and dest address
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement);
        
        EndpointAddress destAddressToUse = getDestAddressToUse(service, serviceParam);
        
        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(),
                (MessageElement) null);
        
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        
        try {
            return doSend(message);
        } catch (IOException e) {
            // close this messenger
            close();
            // rethrow the exception
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public EndpointAddress getLogicalDestinationImpl() {
        return logicalDest;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean isIdleImpl() {
        return isClosed() || (System.currentTimeMillis() - lastUsed > 15 * TimeUtils.AMINUTE);
    }
    
    /**
     *  Connects to the http server and retreives the Logical Destination Address
     **/
    private final EndpointAddress retreiveLogicalDestinationAddress() throws IOException {
        long beginConnectTime = 0;
        long connectTime = 0;
        
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            beginConnectTime = System.currentTimeMillis();
        }
        
        // open a connection to the other end
        HttpURLConnection urlConn = (HttpURLConnection) senderURL.openConnection();
        
        urlConn.setDoOutput(true);
        urlConn.setDoInput(true);
        urlConn.setRequestMethod("GET");
        urlConn.setAllowUserInteraction(false);
        urlConn.setUseCaches(false);
        
        // this is where the connection is actually made, if not already connected.
        // If we can't connect, assume it is dead
        int code = urlConn.getResponseCode();
            
        if (code != HttpURLConnection.HTTP_OK) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = servletHttpTransport.getTransportBindingMeter(null, getDestinationAddress());
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(true, System.currentTimeMillis() - beginConnectTime);
                }
            }
                
            throw new SocketException("Message not accepted: HTTP status " + "code=" + code + " reason=" + urlConn.getResponseMessage());
        }
        
        // check for a returned peerId
        int msglength = urlConn.getContentLength();

        try {
            if (msglength > 0) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Message body (" + msglength + ") starts");
                }
                
                InputStream inputStream = urlConn.getInputStream();
                
                // read the peerId
                byte[] peerIdBytes = new byte[msglength];
                int bytesRead = 0;

                while (bytesRead < msglength) {
                    int thisRead = inputStream.read(peerIdBytes, bytesRead, msglength - bytesRead);
                    
                    if (-1 == thisRead) {
                        break;
                    }
                    
                    bytesRead += thisRead;
                }
                
                if (bytesRead < msglength) {
                    throw new SocketException("Content ended before promised Content length");
                }
                
                String peerIdString = new String(peerIdBytes);
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                    connectTime = System.currentTimeMillis();
                    transportBindingMeter = servletHttpTransport.getTransportBindingMeter(peerIdString, getDestinationAddress());
                    if (transportBindingMeter != null) {
                        transportBindingMeter.connectionEstablished(true, connectTime - beginConnectTime);
                        transportBindingMeter.ping(connectTime);
                        transportBindingMeter.connectionClosed(true, connectTime - beginConnectTime);
                    }
                }
                
                return new EndpointAddress("jxta", peerIdString, null, null);
            }
        } catch (IOException e) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                connectTime = System.currentTimeMillis();
                transportBindingMeter = servletHttpTransport.getTransportBindingMeter(null, getDestinationAddress());
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(true, connectTime - beginConnectTime);
                }
            }
            
            throw e;
        }
        
        throw new IOException("Could not get destination logical address");
    }
    
    /**
     *  Connects to the http server and POSTs the message
     **/
    private boolean doSend(Message msg) throws IOException {
        long beginConnectTime = 0;
        long connectTime = 0;
        
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            beginConnectTime = System.currentTimeMillis();
        }
        
        WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE, (MimeMediaType[]) null);
        
        MimeMediaType encoding = serialed.getContentEncoding();
        
        for (int n = 0; n < 2; n++) {
            // open a connection to the other end
            URL tempURL = new URL(senderURL, "");
            HttpURLConnection urlConn = (HttpURLConnection) tempURL.openConnection();
            
            try {
                urlConn.setDoOutput(true);
                urlConn.setDoInput(true);
                urlConn.setRequestMethod("POST");
                urlConn.setAllowUserInteraction(false);
                urlConn.setUseCaches(false);
                
                if (null != encoding) {
                    urlConn.setRequestProperty("content-encoding", serialed.getContentEncoding().toString());
                }
                urlConn.setRequestProperty("content-length", Integer.toString((int) serialed.getByteLength()));
                urlConn.setRequestProperty("content-type", serialed.getMimeType().toString());
                
                // send the message
                OutputStream out = urlConn.getOutputStream();
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    connectTime = System.currentTimeMillis();
                    transportBindingMeter.connectionEstablished(true, connectTime - beginConnectTime);
                }
                
                serialed.sendToStream(out);
                out.flush();
                
                int responseCode;
                
                try {
                    responseCode = urlConn.getResponseCode();
                } catch (IOException ioe) {
                    // Could not connect. This seems to happen a lot with a loaded Http 1.0
                    // proxy. Apparently, HttpUrlConnection can be fooled by the proxy
                    // in believing that the connection is still open and thus breaks
                    // when attempting to make a second transaction. We should not have to but it
                    // seems that it befalls us to retry.
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Http 1.0 proxy seems in use");
                    }
                    
                    // maybe a retry will help.
                    continue;
                }
                
                // NOTE: If the proxy closed the connection 1.0 style without returning
                // a status line, we do not get an exception: we get a -1 response code.
                // Apparently, proxies no-longer do that anymore. Just in case, we issue a
                // warning and treat it as OK.
                if (responseCode == -1) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        if (neverWarned) {
                            LOG.warn("Obsolete http proxy does not issue HTTP_OK response. Assuming OK");
                            neverWarned = false;
                        }
                    }
                    responseCode = HttpURLConnection.HTTP_OK;
                }
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        transportBindingMeter.dataSent(true, serialed.getByteLength());
                        transportBindingMeter.connectionDropped(true, System.currentTimeMillis() - beginConnectTime);
                    }
                    
                    throw new IOException("Message not accepted: HTTP status " + "code=" + responseCode + " reason=" + urlConn.getResponseMessage());
                }
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    long messageSentTime = System.currentTimeMillis();

                    transportBindingMeter.messageSent(true, msg, messageSentTime - connectTime, serialed.getByteLength());
                    transportBindingMeter.connectionClosed(true, messageSentTime - beginConnectTime);
                }
                
                // Only if it worked, update lastused.
                lastUsed = System.currentTimeMillis();
                return true;
            }
            finally {
                // This does prevent the creation of an infinite number of connections
                // if we happen to be going through a 1.0-only proxy or connect to a server
                // that still does not set content length to zero for the response. With this, at
                // least we close them (they eventualy close anyway because the other side closes
                // them but it takes too much time). If content-length is set, then jdk ignores
                // the disconnect AND reuses the connection, which is what we want.
                
                urlConn.disconnect();
            }
        }
        
        return false;
    }
    
    /**
     *  Handles the http back channel and receives messages.
     **/
    private static class BackChannelListener implements Runnable {
        private final URL backChannelURL;
        private final EndpointService endpointService;
        private final TransportBindingMeter transportBindingMeter;
        
        private volatile boolean isStopped = false;
        
        private Thread backChannelThread;
        private HttpClientMessenger frontChannel;
        
        BackChannelListener(URL backChannelURL, EndpointService endpointService, TransportBindingMeter transportBindingMeter, HttpClientMessenger frontChannel) {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("new BackChannelListener for " + backChannelURL);
            }
            
            this.backChannelURL = backChannelURL;
            this.endpointService = endpointService;
            this.transportBindingMeter = transportBindingMeter;
            this.frontChannel = frontChannel;
            
            backChannelThread = new Thread(this, "HttpClientMessenger backChannel to " + backChannelURL);
            backChannelThread.setDaemon(true);
            backChannelThread.start();
        }
        
        // DO NOT SYNCHRONIZE THIS METHOD ON "this"
        // It needs to synchronize on backChannelListener => deadlock.
        
        protected void stopReceiving() {
            if (isStopped) {
                return;
            }
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("stopReceiving for " + backChannelURL);
            }
            
            isStopped = true;
            
            // Here, we are forced to abandon this object open. Because we could
            // get blocked forever trying to close it. It will rot away after
            // the current read returns. The best we can do is interrupt the
            // thread; unlikely to have an effect per the current.
            // HttpURLConnection implementation.
            backChannelThread.interrupt();
            
            HttpClientMessenger front = frontChannel;

            frontChannel = null;
            if (front != null) {
                front.close();
            }
        }
        
        protected boolean isStopReceiving() {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("isStopReceiving for " + backChannelURL + " is " + isStopped);
            }
            
            return isStopped;
        }
        
        /**
         *  Connects to the http server and waits for messages to be sent and processes them
         **/
        public void run() {
            try {
                long beginConnectTime = 0;
                long connectTime = 0;
                HttpURLConnection conn = null;
                
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Start receiving messages from " + backChannelURL);
                }
                
                InputStream inputStream = null;
                
                // get messages until the messenger is closed
                while (!isStopReceiving()) {
                    if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                        beginConnectTime = System.currentTimeMillis();
                    }
                    
                    // start receiving messages
                    try {
                        long messageReceivedTime = connectTime;
                        
                        while (!isStopReceiving()) {
                            // Reusing the stream even though we don't check to see if the channel was closed.
                            if (conn != null) {
                                try {
                                    // Always connect (no cost if connected).
                                    conn.connect();
                                } catch (IOException ioe) {
                                    if (LOG.isEnabledFor(Level.WARN)) {
                                        LOG.warn("Unable to reconnect to " + backChannelURL, ioe);
                                    }
                                }
                                
                                int responseCode;
                                
                                try {
                                    if (LOG.isEnabledFor(Level.DEBUG)) {
                                        LOG.debug("Waiting for response code from " + backChannelURL);
                                    }
                                    
                                    responseCode = conn.getResponseCode();
                                    
                                    if (responseCode != HttpURLConnection.HTTP_OK) {
                                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                            transportBindingMeter.connectionFailed(true, System.currentTimeMillis() - beginConnectTime);
                                        }
                                        
                                        throw new IOException("HTTP Failure: " + conn.getResponseCode() + " : " + conn.getResponseMessage());
                                    }
                                } catch (RuntimeException wrapper) { // Deal with URLConnection artefact. (Treat it like EOF).
                                    // This should no-longer happen because we no-longer break connections and swicth threads.
                                    conn = null;
                                    continue;
                                } catch (ConnectException ce) { // Catch a missing server exception
                                    stopReceiving();
                                    continue;
                                }
                                
                                if (LOG.isEnabledFor(Level.DEBUG)) {
                                    LOG.debug(
                                            "Response " + responseCode + " for Connection : " + backChannelURL + "\n\tContent-Type : "
                                            + conn.getHeaderField("Content-Type") + "\tContent-Length : " + conn.getHeaderField("Content-Length")
                                            + "\tTransfer-Encoding : " + conn.getHeaderField("Transfer-Encoding"));
                                }
                                
                                inputStream = conn.getInputStream();
                            }
                            
                            /*
                             * From HttpURLConnection JavaDoc:
                             * Each HttpURLConnection instance is used to make a single
                             * request but the underlying network connection to the HTTP
                             * server may be transparently shared by other instances.
                             * Calling the close() methods on the InputStream or OutputStream
                             * of an HttpURLConnection after a request may free network resources
                             * associated with this instance but has no effect on any shared
                             * persistent connection. Calling the disconnect() method may close the
                             * underlying socket if a persistent connection is otherwise idle at that
                             * time.
                             */
                            // If we have no connection or the old connection was
                            // exhausted, create a new connection and block for
                            // data.
                            
                            if (conn == null) {
                                if (LOG.isEnabledFor(Level.DEBUG)) {
                                    LOG.debug("Opening new connection to " + backChannelURL);
                                }
                                
                                conn = (HttpURLConnection) backChannelURL.openConnection(); // Incomming data channel
                                
                                conn.setDoOutput(false);
                                conn.setDoInput(true);
                                conn.setRequestMethod("GET");
                                conn.setAllowUserInteraction(false);
                                conn.setUseCaches(false);
                                
                                // Loop back and try again to get a stream.
                                continue;
                            }
                            
                            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                connectTime = System.currentTimeMillis();
                                transportBindingMeter.connectionEstablished(true, beginConnectTime - connectTime);
                                transportBindingMeter.dataReceived(true, conn.getContentLength());
                            }
                            
                            // read the message!
                            Message incomingMsg = null;

                            try {
                                // FIXME 20040907 bondolo Should get message mimetype from http header.
                                // FIXME 20040907 bondolo Should get message encoding from http header.
                                incomingMsg = WireFormatMessageFactory.fromWire(inputStream, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE,
                                        (MimeMediaType) null);
                            } catch (EOFException e) {
                                // Connection ran out of messages. let it go.
                                conn = null;
                                
                                inputStream.close();
                                continue;
                            }
                            
                            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                long now = System.currentTimeMillis();

                                transportBindingMeter.messageReceived(true, incomingMsg, incomingMsg.getByteLength(), now - messageReceivedTime);
                                messageReceivedTime = now;
                            }
                            
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Received " + incomingMsg + " from " + backChannelURL);
                            }
                            
                            try {
                                endpointService.demux(incomingMsg);
                            } catch (Throwable e) {
                                if (LOG.isEnabledFor(Level.WARN)) {
                                    LOG.warn("Failure demuxing an incoming message", e);
                                }
                                
                                throw e;
                            }
                        }
                        
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionClosed(true, System.currentTimeMillis() - beginConnectTime);
                        }
                    } catch (IOException e) {
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionDropped(true, System.currentTimeMillis() - beginConnectTime);
                        }
                        
                        // If we managed to get down here, it is really an error.
                        // However, being disconnected from the server, for
                        // whatever reason is a common place event. No need
                        // to clutter the screen with scary messages.
                        // When the message layer believes it's serious, it
                        // prints the scary message already.
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Failed to read message from " + backChannelURL, e);
                        }
                        // Time to call this connection dead.
                        stopReceiving();
                        break;
                    }
                }
            } catch (Throwable argh) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Backchannel listener exiting because of uncaught exception", argh);
                }
                stopReceiving();
            } finally {
                backChannelThread = null;
            }
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Stop receiving messages from " + backChannelURL);
            }
        }
    }
}

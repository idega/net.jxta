/*
 *
 * $Id: TcpConnection.java,v 1.1 2007/01/16 11:01:51 thomas Exp $
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

package net.jxta.impl.endpoint.tcp;


import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.util.LimitInputStream;
import net.jxta.util.WatchedInputStream;
import net.jxta.util.WatchedOutputStream;

import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;

/**
 * Low-level TcpMessenger
 *
 */
class TcpConnection implements Runnable {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(TcpConnection.class.getName());
    
    private static final MimeMediaType appMsg = new MimeMediaType("application/x-jxta-msg").intern();
    
    private final TcpTransport proto;
    
    private EndpointAddress dstAddress = null;
    private EndpointAddress fullDstAddress = null;
    private transient InetAddress inetAddress = null;
    private transient int port = 0;
    
    private transient volatile boolean closed = false;
    private transient Thread recvThread = null;
    
    private transient WelcomeMessage myWelcome = null;
    private transient WelcomeMessage itsWelcome = null;
    
    private final transient long firstUsed = TimeUtils.timeNow();
    private transient long lastUsed = TimeUtils.timeNow();
    private transient Socket sharedSocket = null;
    private transient WatchedOutputStream woutputStream = null;
    private transient WatchedInputStream winputStream = null;
    private transient OutputStream outputStream = null;
    private transient InputStream inputStream = null;

    private TransportBindingMeter transportBindingMeter;
    private boolean initiator;
    private long connectionBegunTime;
    private boolean closingDueToFailure = false;
    
    /**
     *  only one outgoing message at a time per connection.
     **/
    private final transient Object writeLock;
    
    /**
     *  Creates a new TcpConnection for the specified destination address.
     *
     *  @param destaddr the destination address of this connection.
     *  @param p    the transport which this connection is part of.
     *  @throws IOException for failures in creating the connection.
     **/
    TcpConnection(EndpointAddress destaddr, TcpTransport p) throws IOException {
        initiator = true;
        
        proto = p;
        
        this.fullDstAddress = destaddr;
        this.dstAddress = new EndpointAddress(destaddr, null, null);
        
        String protoAddr = destaddr.getProtocolAddress();
        int portIndex = protoAddr.lastIndexOf(":");

        if (portIndex == -1) {
            throw new IllegalArgumentException("Invalid Protocol Address (port # missing) ");
        }
        
        String portString = protoAddr.substring(portIndex + 1);
        try {
            port = Integer.valueOf(portString).intValue();
        } catch (NumberFormatException caught) {
            throw new IllegalArgumentException("Invalid Protocol Address (port # invalid): " + portString );
        }
        
        // Check for bad port number.
        if ((port <= 0) || (port > 65535)) {
            throw new IllegalArgumentException("Invalid port number in Protocol Address : " + port);
        }
        
        String hostString = protoAddr.substring(0, portIndex);
        inetAddress = InetAddress.getByName(hostString);

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("New TCP Connection to : " + dstAddress + " / "+ inetAddress.getHostAddress() + ":" + port );
        }
        
        writeLock = new String( "TCP write lock for " + inetAddress.getHostAddress() + ":" + port );
        
        // See if we're attempting to use the loopback address.
        // And if so, is the peer configured for the loopback network only?
        // (otherwise the connection is not permitted). Btw, the otherway around 
        // is just as wrong, so we check both at once and pretend it cannot work,
        // even if it might have.
        // FIXME 20041130 This is not an appropriate check if the other peer is
        // running on the same machine and the InetAddress.getByName returns the
        // loopback address.
        if (inetAddress.isLoopbackAddress() != proto.usingInterface.isLoopbackAddress()) {
            throw new IOException("Network unreachable");
        }
        
        try {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                connectionBegunTime = System.currentTimeMillis();
            }
            
            /*
             * DVT KLUDGE TO MAKE THE ROUTER'S LIFE MISERABLE.
             * to be removed after new ad-hoc routing dvt.
             */
            
            int rp = proto.getRestrictionPort();
            
            if (rp != -1 && (port < rp - 1 || port > rp + 1)) {
                throw new IOException("Simulated separate networks killed outgoing cnx.");
            }

            sharedSocket = IPUtils.connectToFrom(inetAddress, port, proto.usingInterface, 0, proto.connectionTimeOut);
            
            startSocket();
            
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = proto.getUnicastTransportBindingMeter((PeerID) getDestinationPeerID(), dstAddress);

                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionEstablished(initiator, System.currentTimeMillis() - connectionBegunTime);
                }
                // Fix-Me: We need to add the bytes from the Welcome Messages to the transportBindingMeter, iam@jxta.org
            }
        } catch (IOException e) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = proto.getUnicastTransportBindingMeter(null, dstAddress);

                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(initiator, System.currentTimeMillis() - connectionBegunTime);
                }
                // Fix-Me: We need to add the bytes from the Welcome Messages to the transportBindingMeter, iam@jxta.org
            }

            // If we failed for any reason, make sure the socket is closed.
            // We're the only one to know about it.
            if (sharedSocket != null) {
                sharedSocket.close();
            }
            throw e;
        }
    }
    
    /**
     *  Creates a new connection from an incoming socket
     *
     *  @param incSocket    the incoming socket.
     *  @param TcpTransport the transport we are working for.
     *  @throws IOException for failures in creating the connection.
     **/
    TcpConnection(Socket incSocket, TcpTransport p) throws IOException {
        proto = p;
        try {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Connection from " + incSocket.getInetAddress().getHostAddress() + ":" + incSocket.getPort());
            }

            initiator = false;

            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                connectionBegunTime = System.currentTimeMillis();
            }

            inetAddress = incSocket.getInetAddress();
            port = incSocket.getPort();
            
            writeLock = new String( "TCP write lock for " + inetAddress.getHostAddress() + ":" + port );

            // Temporarily, our address for inclusion in the welcome message response.
            dstAddress = new EndpointAddress(proto.getProtocolName(), inetAddress.getHostAddress() + ":" + port, null, null);
            fullDstAddress = dstAddress;
            
            sharedSocket = incSocket;
            startSocket();
            
            // The correct value for dstAddr: that of the other party.
            dstAddress = itsWelcome.getPublicAddress();
            fullDstAddress = dstAddress;

            // Reset the thread name now that we have a meaningfull
            // destination address and remote welcome msg.
            setThreadName();
            
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = proto.getUnicastTransportBindingMeter((PeerID) getDestinationPeerID(), dstAddress);

                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionEstablished(initiator, System.currentTimeMillis() - connectionBegunTime);
                }
            }
            
        } catch (IOException e) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = proto.getUnicastTransportBindingMeter(null, dstAddress);
                
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(initiator, System.currentTimeMillis() - connectionBegunTime);
                }
            }
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean equals(Object target) {
        if (this == target) {
            return true;
        }
        
        if (null == target) {
            return false;
        }
        
        if (target instanceof TcpConnection) {
            TcpConnection likeMe = (TcpConnection) target;
            
            return getDestinationAddress().equals(likeMe.getDestinationAddress()) && getDestinationPeerID().equals(likeMe.getDestinationPeerID());
        }
        
        return false;
    }
    
    /**
     * {@inheritDoc}
     **/
    protected void finalize() {
        closingDueToFailure = false;
        close();
    }
    
    /**
     * {@inheritDoc}
     **/
    public int hashCode() {
        return  getDestinationPeerID().hashCode() + getDestinationAddress().hashCode();
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>Implementation for debugging.
     **/
    public String toString() {
        return super.toString() + ":" + ((null != itsWelcome) ? itsWelcome.getPeerID().toString() : "unknown") + " on address "
                + ((null != dstAddress) ? dstAddress.toString() : "unknown");
    }
    
    private synchronized void setThreadName() {
        
        Thread temp = recvThread;
        
        if (temp != null) {
            try {
                temp.setName("TCP receive : " + itsWelcome.getPeerID() + " on address " + dstAddress);
            } catch (Exception ez1) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Cannot change thread name", ez1);
                }
            }
        }
    }
    
    public EndpointAddress getDestinationAddress() {
        return (EndpointAddress) dstAddress.clone();
    }
    
    public EndpointAddress getConnectionAddress() {
        // Somewhat confusing but destinationAddress is the name of that thing
        // for the welcome message.
        return itsWelcome.getDestinationAddress();
    }
    
    public ID getDestinationPeerID() {
        return itsWelcome.getPeerID();
    }
    
    private void startSocket() throws IOException {
        
        sharedSocket.setKeepAlive(true);
        int useBufferSize = Math.max(TcpTransport.ChunkSize, sharedSocket.getSendBufferSize());

        sharedSocket.setSendBufferSize(useBufferSize);

        useBufferSize = Math.max(TcpTransport.RecvBufferSize, sharedSocket.getReceiveBufferSize());
        sharedSocket.setReceiveBufferSize(useBufferSize);
        
        sharedSocket.setSoLinger(true, TcpTransport.LingerDelay);
        sharedSocket.setTcpNoDelay(true);
        
        woutputStream = new WatchedOutputStream(sharedSocket.getOutputStream(), TcpTransport.ChunkSize);
        woutputStream.setWatchList(proto.ShortCycle);

        winputStream = new WatchedInputStream(sharedSocket.getInputStream(), TcpTransport.ChunkSize);
        winputStream.setWatchList(proto.LongCycle);

        if ((winputStream == null) || (woutputStream == null)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("   failed getting streams.");
            }
            throw new IOException("Could not get streams");
        }
        outputStream = new BufferedOutputStream(woutputStream, TcpTransport.SendBufferSize);
        inputStream = winputStream;

        myWelcome = new WelcomeMessage(fullDstAddress, proto.getPublicAddress(), proto.group.getPeerID(), false);
        
        myWelcome.sendToStream(outputStream);
        outputStream.flush();
        
        // The response should arrive shortly or we bail out.
        inputActive(true);
        
        itsWelcome = new WelcomeMessage(inputStream);
        
        // Ok, we can wait for messages now.
        inputActive(false);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("startSocket : Hello from " + itsWelcome.getPublicAddress() + " [" + itsWelcome.getPeerID() + "]");
        }
        
        recvThread = new Thread(proto.myThreadGroup, this);
        setThreadName();
        recvThread.setDaemon(true);
    }
    
    protected void start() {
        recvThread.start();
    }

    /**
     * Send message to the remote peer.
     *
     *  @param msg  the message to send.
     *  @return If <tt>true</tt> the message was sent successfully otherwise <tt>false</tt>.
     **/
    public boolean sendMessage(Message msg) throws IOException {
        
        // socket is a stream, only one writer at a time...
        synchronized (writeLock) {
            if (closed) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Connection was closed to : " + dstAddress);
                }
                
                throw new IOException("Connection was closed to : " + dstAddress);
            }
            
            boolean success = false;
            long sendBeginTime = 0;
            long size = 0;
            
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                sendBeginTime = System.currentTimeMillis();
            }
            
            try {
                // 20020730 bondolo@jxta.org Do something with content-coding here
                // serialize the message.
                WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, appMsg, (MimeMediaType[]) null);

                // Build the protocol header
                // Allocate a buffer to contain the message and the header
                
                MessagePackageHeader header = new MessagePackageHeader();

                header.setContentTypeHeader(serialed.getMimeType());
                
                size = serialed.getByteLength();
                header.setContentLengthHeader(size);

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sending " + msg + " (" + serialed.getByteLength() + ") to " + dstAddress + " via " + inetAddress.getHostAddress() + ":" + port);
                }

                header.sendToStream(outputStream);
                serialed.sendToStream(outputStream);
                outputStream.flush();
                
                // all done!
                success = true;
                setLastUsed(System.currentTimeMillis());
                
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.messageSent(initiator, msg, System.currentTimeMillis() - sendBeginTime, size);
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sent " + msg + " successfully via " + inetAddress.getHostAddress() + ":" + port );
                }
                
                return true;
            } catch (Exception failed) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.sendFailure(initiator, msg, System.currentTimeMillis() - sendBeginTime, size);
                }
                                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Message send failed for " + inetAddress.getHostAddress() + ":" + port, failed);
                }
                
                closingDueToFailure = true;
                close();
                
                IOException failure = new IOException("Failed sending " + msg + " to : " + inetAddress.getHostAddress() + ":" + port );
                failure.initCause(failed);

                throw failure;
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     *
     * This is the background Thread. While the connection is active, takes
     * messages from the queue and send it.
     **/
    public void run() {
        long receiveBeginTime = 0;
        long size = 0;
        
        try {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Starting receiver for " + inetAddress.getHostAddress() + ":" + port);
            }
            
            try {
                while (isConnected()) {
                    if (closed) {
                        break;
                    }
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Message receive starts for " + inetAddress.getHostAddress() + ":" + port);
                    }
                    // We can stay blocked here for a long time, it's ok.
                    MessagePackageHeader header = new MessagePackageHeader(inputStream);
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                        receiveBeginTime = System.currentTimeMillis();
                    }
                    
                    MimeMediaType msgMime = header.getContentTypeHeader();
                    
                    long msglength = header.getContentLengthHeader();
                    
                    // FIXME 20020730 bondolo@jxta.org Do something with content-coding here.
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("tcp receive - message body (" + msglength + ") starts for " + inetAddress.getHostAddress() + ":" + port);
                    }
                    
                    // read the message!
                    // We have received the header, so, the rest had better
                    // come. Turn the short timeout on.
                    inputActive(true);
                    
                    Message msg = null;

                    try {
                        msg = WireFormatMessageFactory.fromWire(new LimitInputStream(inputStream, msglength, true), msgMime, (MimeMediaType) null);
                    } catch (IOException failed) {
                        if (LOG.isEnabledFor(Level.INFO)) {
                            LOG.info("tcp receive - failed reading msg from " + inetAddress.getHostAddress() + ":" + port);
                            // LOG.error( sharedSocket.toString() +
                            // "\tbound " + sharedSocket.isBound() +
                            // "\tclosed " + sharedSocket.isClosed() +
                            // "\tconntected " + sharedSocket.isConnected() +
                            // "\tisInputShutDown " + sharedSocket.isInputShutdown() );
                        }
                        
                        throw failed;
                    } finally {
                        // We can relax again.
                        inputActive(false);
                    }
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Handing " + msg + " from " + inetAddress.getHostAddress() + ":" + port + " to EndpointService");
                    }
                    
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        transportBindingMeter.messageReceived(initiator, msg, System.currentTimeMillis() - receiveBeginTime, msglength);
                    }

                    // Demux the message for the upper layers.
                    proto.endpoint.demux(msg);
                    
                    setLastUsed(System.currentTimeMillis());
                }
            } catch (InterruptedIOException woken) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.receiveFailure(initiator, System.currentTimeMillis() - receiveBeginTime, size);
                }
                
                // We have to treat this as fatal since we don't know where
                // in the framing the input stream was at. This should have
                // been handled below.
                
                closingDueToFailure = true;
                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn(
                            "tcp receive - Error : read() timeout after " + woken.bytesTransferred + " on connection " + inetAddress.getHostAddress()
                            + ":" + port);
                }
            } catch (EOFException finished) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.receiveFailure(initiator, System.currentTimeMillis() - receiveBeginTime, size);
                }
                
                // The other side has closed the connection
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("tcp receive - Connection was closed by " + inetAddress.getHostAddress() + ":" + port);
                }
            } catch (SocketException finished) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.receiveFailure(initiator, System.currentTimeMillis() - receiveBeginTime, size);
                }
                
                closingDueToFailure = true;
                
                // The other side has closed the connection
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("tcp receive - Connection was closed by " + inetAddress.getHostAddress() + ":" + port);
                }
            } catch (Throwable e) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    transportBindingMeter.receiveFailure(initiator, System.currentTimeMillis() - receiveBeginTime, size);
                }
                
                closingDueToFailure = true;
                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("tcp receive - Error on connection " + inetAddress.getHostAddress() + ":" + port, e);
                }
            } finally {
                if (!closed) {
                    // We need to close the connection down.
                    close();
                }
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        } finally {
            recvThread = null;
        }
    }
    
    private void closeIOs() {
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (Exception ez1) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("could not close inputStream ", ez1);
                }
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
                outputStream = null;
            } catch (Exception ez1) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Error : could not close outputStream ", ez1);
                }
            }
        }
        if (sharedSocket != null) {
            try {
                sharedSocket.close();
                sharedSocket = null;
            } catch (Exception ez1) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Error : could not close socket ", ez1);
                }
            }
        }
    }
    
    /**
     *  Soft close of the connection. Messages can no longer be sent, but any
     *  in the queue will be flushed.
     **/
    public synchronized void close() {
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info( (closingDueToFailure ? "Failure" : "Normal") + 
                    " close (open " + TimeUtils.toRelativeTimeMillis( TimeUtils.timeNow(), firstUsed ) +
                    "ms) of socket to : " + dstAddress + " / " + inetAddress.getHostAddress() + ":"
                    + port);
            if (LOG.isEnabledFor(Level.DEBUG) && closingDueToFailure) {
                LOG.debug("stack trace", new Throwable("stack trace"));
            }
        }
        
        if (!closed) {
            setLastUsed(0); // we idle now. Way idle.
            closeIOs();
            closed = true;
            Thread temp = recvThread;
            if (temp != null) {
                temp.interrupt();
            }
         
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                if (closingDueToFailure) {
                    transportBindingMeter.connectionDropped(initiator, System.currentTimeMillis() - connectionBegunTime);
                } else {
                    transportBindingMeter.connectionClosed(initiator, System.currentTimeMillis() - connectionBegunTime);
                }
            }
        
            // socket closing happens in the shutdown of recvThread
        }
    }
    
    /**
     *  return the current connection status.
     *
     *  @param true if there is an active connection to the remote peer,
     *  otherwise false.
     *
     **/
    public boolean isConnected() {
        return ((recvThread != null) && (!closed));
    }
    
    /**
     *  Return the absolute time in milliseconds at which this Connection was last used.
     *
     *  @return absolute time in milliseconds.
     **/
    public long getLastUsed() {
        return lastUsed;
    }
    
    /**
     *  Set the last used time for this connection in absolute milliseconds.
     *
     *  @param time absolute time in milliseconds.
     **/
    private void setLastUsed(long time) {
        lastUsed = time;
    }
    
    TransportBindingMeter getTransportBindingMeter() {
        return transportBindingMeter;
    }
    
    /**
     * This is called with "true" when the invoker is about to read some
     * input and is not willing to wait for it to come.
     * This is called with "false" when the invoker is about to wait for
     * a long time for input to become available with a potentialy very long
     * blocking read.
     */
    private void inputActive(boolean active) {
        if (active) {
            winputStream.setWatchList(proto.ShortCycle);
        } else {
            winputStream.setWatchList(proto.LongCycle);
        }
    }
}

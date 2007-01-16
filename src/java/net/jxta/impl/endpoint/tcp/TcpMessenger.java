/*
 *
 * $Id: TcpMessenger.java,v 1.1 2007/01/16 11:01:51 thomas Exp $
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


import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.util.TimeUtils;


/**
 * Implements a messenger which sends messages via raw TCP sockets.
 *
 * <p/>FIXME jice@jxta.org 20021007:
 * Although in theory not too clean, we could merge connection and messenger.
 * there is a one-to-one mapping between them except that for incoming
 * connections we sometimes throw the messenger away. Merging them
 * would add only a message element and an endpoint address to the connection
 * object and would simplify close() isclosed() and GC's life quite a bit.
 *
 * (Look Ma, no synch ! All synchronized() have been removed. With the help
 * of a volatile reference to the TcpConnection, this is no longer necessary
 * this optimizes at least one critical function: isClosed().
 *
 **/
class TcpMessenger extends BlockingMessenger {

    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(TcpMessenger.class.getName());
    
    /**
     *  The source address of messages sent on this messenger.
     **/
    private final EndpointAddress srcAddress;
    
    private final MessageElement srcAddressElement;

    /**
     *  Cache of the logical destination of this messenger. (It helps if it works even after close)
     **/
    private final EndpointAddress logicalDestAddress;
    
    /**
     *  The message transport we are working for.
     **/
    private final TcpTransport proto;
    
    /**
     *  The connection
     **/
    volatile TcpConnection conn;
    
    /**
     * If this is an incoming connection we must not close it when this messenger disapears.
     * It has many reasons to disappear while the connection must keep receiving messages.
     * This is causing some problems for incoming messengers that are managed
     * by some entity, such as the router or the relay. These two do call close
     * explicitly when they discard a messenger, and their intent is truely
     * to nuke the connection. So basically we need to distinguish between
     * incoming messengers that are abandonned without closing (for these we
     * must protect the input side because that's the only reason for the
     * connection being there) and incoming messengers that are explicitly
     * closed (in which case we must let the entire connection be closed).
     */
    boolean incoming = false;

    /**
     *  Create a new TcpNonBlockingMessenger for the specified address.
     *
     *  @param destaddr the destination of the messenger
     *  @param p    the tcp MessageSender we are working for.
     **/
    TcpMessenger(EndpointAddress destaddr, TcpConnection conn, TcpTransport p)
        throws IOException {

        // We need self destruction: tcp messengers are expenssive to make and they refer to
        // a connection that must eventually be closed.
        super(p.group.getPeerGroupID(), destaddr, true);
        
        if (null == conn) {
            throw new IOException("Could not get connection for address " + dstAddress);
        }

        if (!conn.isConnected()) {
            throw new IOException("Connection was closed to " + dstAddress);
        }
        
        this.conn = conn;
        
        this.srcAddress = p.getPublicAddress(); // already a clone
        
        srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, srcAddress.toString(), (MessageElement) null);
        
        this.proto = p;
        this.incoming = true;
        logicalDestAddress = new EndpointAddress("jxta", conn.getDestinationPeerID().getUniqueValue().toString(), null, null);
    }
    
    /**
     *  Create a new TcpNonBlockingMessenger for the specified address.
     *
     *  @param destaddr the destination of the messenger
     *  @param p    the tcp MessageSender we are working for.
     **/
    TcpMessenger(EndpointAddress destaddr, TcpTransport p) throws IOException {

        /**
         *  Create a connection. Not needed immediately, but this gets things going
         **/
        this(destaddr, new TcpConnection(destaddr, p), p);
        this.incoming = false;
    }

    /*
     * The cost of just having a finalize routine is high. The finalizer is
     * a bottleneck and can delay garbage collection all the way to heap
     * exhaustion. Leave this comment as a reminder to future maintainers.
     * Below is the reason why finalize is not needed here.
     *
     * These messengers are never given to application layers. Endpoint code
     * always calls close when needed.
     * There used to be an incoming special case in order to *prevent* closure
     * because the inherited finalize used to call close. This is no-longer
     * the case. For the outgoing case, we do not need to call close
     * for the reason explained above.

     public void finalize () {
     }

     */

    /**
     * Starts the underlying connection receive thread if any.
     */
    protected void start() {
        if (conn != null) {
            conn.start();
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void closeImpl() {

        TcpConnection toClose = conn;

        if (toClose == null) {
            return;
        }

        conn = null;

        // Now everyone knows its closed and the connection
        // can no-longer be obtained. So, we can go about our
        // business of closing it.
        // It can happen that a redundant close() is done, since
        // two threads could grab conn before one nullifies it but it
        // does not matter. close() is idempotent.

        super.close();

        toClose.close();
    }

    // FIXME - jice@jxta.org 20040413: Warning. this is overloading the standard isClosed(). Things were arranged so that it
    // should still work, but it's a stretch. Transports should get a deeper retrofit eventually.
    public boolean isClosed() {

        TcpConnection holdIt = conn;

        if (holdIt == null) {
            return true;
        }
        if (holdIt.isConnected()) {
            return false;
        }

        // Ah, this connection is broken. So, we weren't closed, but now
        // we are. That could happen redundantly since two threads could
        // find that holdIt.isConnected() is false before one of them
        // first zeroes conn. But it does not matter. super.close() is
        // idempotent (and does pretty much nothing in our case, anyway).

        super.close();
        conn = null;
        return true;
    }

    TransportBindingMeter getTransportBindingMeter() {
        if (conn != null) {
            return conn.getTransportBindingMeter();
        } else { 
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Since we probe the connection status, we'll keep a messenger as long
     * as the connection is active, even if only on the incoming side.
     * So we're being a bit nice to the other side. Anyway, incoming
     * connections do not go away when the messenger does. There's a receive
     * timeout for that.
     */
    public boolean isIdleImpl() {
        TcpConnection holdIt = conn;

        return (holdIt == null) || (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), holdIt.getLastUsed()) > 15 * TimeUtils.AMINUTE);
    }

    /**
     * {@inheritDoc}
     *
     **/
    public EndpointAddress getLogicalDestinationImpl() {
        EndpointAddress holdIt = logicalDestAddress;

        return holdIt == null ? null : (EndpointAddress) holdIt.clone();
    }
    
    /** Sends a message to the destination
     *
     *  @param msg      the message to send.
     *  @param destService  Optionally replaces the service in the destination
     *  address. If null then the destination address's default service
     *  will be used.
     *  @param destServiceParam  Optionally replaces the service param in the
     *  destination address. If null then the destination address's default service
     *  parameter will be used.
     *  @return If <tt>true</tt> the message was sent successfully otherwise <tt>false</tt>.
     **/
    public boolean sendMessageBImpl(Message message, String service, String serviceParam)
        throws IOException {

        // We're not synchronized. conn could become null on us.
        // Causality between close and send is unimportant we just need
        // to prevent an NPE if they happen in parallel.
        // So, get a private reference just long enough to send.

        TcpConnection myConn = conn;

        if (isClosed()) {
            IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info(failure);
            }
            
            throw failure;
        }
        
        // Set the message with the appropriate src and dest address
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement);
        
        EndpointAddress destAddressToUse = getDestAddressToUse(service, serviceParam);
        
        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(),
                (MessageElement) null);
        
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        
        // send it
        try {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Sending " + message + " to " + destAddressToUse + " on connection " + conn.getDestinationAddress());
            }
        
            return myConn.sendMessage(message);
        } catch (IOException caught) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Message send failed for " + message, caught);
            }

            close();

            throw caught;
        }
    }
}

/*
 *
 * $Id: LoopbackMessenger.java,v 1.1 2007/01/16 11:01:54 thomas Exp $
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

package net.jxta.impl.endpoint;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

/**
 * This class implements local delivery of messages ( for example when the
 * InputPipe and the OutputPipe are located on the same peer)
 *
 * <p/>The reason this class is useful is that it may not always be possible to
 * connect to oneself without actually going to a relay. If your peer is an
 * http client, it is not able to connect to self through the normal http
 * transport.
 *
 * <p/>Since transports cannot be relied on to perform a loopback, some layer
 * above has to figure out that a message is looping back.
 * Since peerid loopback does not explicitly request to go through a real
 * transport, and since peerid addressing is the job of the router, it is
 * the router that performs loopback.
 *
 * <p/>The router could probably perform the lookback by delivering the message
 * to its own input queue, that would take a special transport instead of a
 * special messenger, which is the same kind of deal but would imply some
 * incoming message processing by the router for every message. In
 * contrast, the loopback messenger is setup once and the router will never
 * sees the messages. That's a good optimization.
 *
 * <p/>Alternatively, the endpoint service itself could figure out the
 * loopback, but since the API wants to give a messenger to the requestor
 * rather than just sending a message, the endpoint would have to setup a
 * loopback messenger anyway. So it is pretty much the same.
 *
 * <p/>Anyone with a better way, speak up.
 *
 * J-C
 */

public class LoopbackMessenger extends BlockingMessenger {

    private static final Logger LOG = Logger.getLogger(LoopbackMessenger.class.getName());
    
    /**
     *  The source address of messages sent on this messenger.
     **/
    private EndpointAddress srcAddress = null;
    
    private MessageElement srcAddressElement = null;
    
    
    private EndpointAddress logicalDestination = null;
    
    /**
     *  The endpoint we are working for, ie. that we will loop back to.
     **/
    EndpointService endpoint = null;
    
    /**
     *  Create a new loopback messenger.
     *
     *  @param ep   where messages go
     *  @param src  who messages should be addressed from
     *  @param dest who messages should be addressed to
     *
     **/
    public LoopbackMessenger(EndpointService ep,
    EndpointAddress src,
    EndpointAddress dest,
    EndpointAddress logicalDest ) {
        super( ep.getGroup().getPeerGroupID(), dest, false );
        logicalDestination = (EndpointAddress) logicalDest.clone();
        
        endpoint = ep;

        srcAddress = (EndpointAddress) src.clone();

        srcAddressElement = new StringMessageElement(
        EndpointServiceImpl.MESSAGE_SOURCE_NAME,
        srcAddress.toString(),
        (MessageElement) null );
    }
    
    /**
     * {@inheritDoc}
     *
     **/
    public EndpointAddress getLogicalDestinationImpl() {
        return (EndpointAddress) logicalDestination.clone();
    }
    

    /**
     * {@inheritDoc}
     *
     **/
    public boolean isIdleImpl() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     **/
    public void closeImpl() {
    }

    /**  Sends a message to the destination
     *
     *  @param message      the message to send.
     *  @param service  Optionally replaces the service in the destination
     *  address. If null then the destination address's default service
     *  will be used.
     *  @param serviceParam  Optionally replaces the service param in the
     *  destination address. If null then the destination address's default service
     *  parameter will be used.
     **/
    public boolean sendMessageBImpl(Message message, String service, String serviceParam)
	throws IOException {

        if ( isClosed() ) {
            IOException failure = new IOException( "Messenger was closed, it cannot be used to send messages." );
            
            if (LOG.isEnabledFor(Level.WARN))
                LOG.warn( failure, failure );
            
	    throw failure;
        }
        
        // Set the message with the appropriate src and dest address
        message.replaceMessageElement(
        EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement );
        
        MessageElement dstAddressElement = new StringMessageElement(
        EndpointServiceImpl.MESSAGE_DESTINATION_NAME,
        getDestAddressToUse( service, serviceParam ).toString(),
        (MessageElement) null );

        message.replaceMessageElement(
        EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement );
        
        // queue it up.
        endpoint.demux( message );
        
        return true;
    }
}

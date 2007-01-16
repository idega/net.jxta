/************************************************************************
 *
 * $Id: MessageSender.java,v 1.1 2007/01/16 11:01:27 thomas Exp $
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

package net.jxta.endpoint;

import java.io.IOException;

/**
 *  A MessageSender is a MessageTransport that is able to send messages to
 * remote peers using some protocol. MessageSenders provide facilities for:
 *
 *  <p/><ul>
 *  <li>Determining point-to-point connectivity (ping)</li>
 *  <li>Sending point-to-point (unicast) messages</li>
 *  <li>Sending propagated (multicast) messages</li>
 *  </ul>
 *
 *  <p/>MessageSenders additionally describe themselves in terms of their
 *  abilities.
 *  <dl>
 *  <dt>{@link #isConnectionOriented()}</dt><dd>Indicates that the
 *  MessageTransport can provide effcient transport of a series of messages to
 *  the same destination</dd>
 *  <dt>{link@ #allowRouting()}</dt><dd>Indicates that the Message Transport 
 *  can be used in the routing of messages to destinations which are not 
 *  directly reachable via this transport.</dd>
 *  </dl>
 *
 *  @see net.jxta.endpoint.MessageTransport
 *  @see net.jxta.endpoint.Messenger
 *  @see net.jxta.endpoint.EndpointService
 *  @see net.jxta.endpoint.MessageReceiver
 *  @see net.jxta.endpoint.Message
 */
public interface MessageSender extends MessageTransport {
    
    /**
     *  Returns the {@link EndpointAddress} which will be used as the source
     *  address for all messages sent by this message sender. This is the
     *  "prefered" address to which replies may be sent. This address is not
     *  necessarily the best or only address by which the peer may be reached.
     *
     *  <p/>The public address may also be for a different message transport.
     *
     *  @return an EndpointAddress containing the public address for this
     *  message receiver.
     */
    public EndpointAddress getPublicAddress();
    
    /**
     * Returns true if the endpoint protocol can establish
     * connection to the remote host (like TCP).
     *
     * @return <tt>true</tt> if the protocol is connection oriented.
     */
    public boolean isConnectionOriented();
    
    /**
     * Returns true if the endpoint protocol can be used by the EndpointRouter.
     *
     * <p/>More specifically, this protocol will be used to route messages who's
     * final destination is <b>not</b> one of the endpoint addresses available
     * from getReachableEndpointAddresses.
     *
     * @return <tt>true</tt> if the protocol can be used by the EndpointRouter
     */
    public boolean allowsRouting();
    
    /**
     *  Creates an {@link Messenger} for sending messages to the
     *  specified destination {@link EndpointAddress}.
     *
     * @param dest EndpointAddress of the destination
     * @param hint A hint for the transport to use when creating the messenger.
     * @return a Messenger. null is returned if the EndpointAddress is
     * not reachable.
     */
    public Messenger getMessenger( EndpointAddress dest, Object hint );
    
    /**
     * Propagates a Message on this Message Transport.
     *
     * <p/><strong>WARNING</strong>: The message object should not be reused or
     * modified after the call is made. Concurrent modifications will produce
     * unexpected result.
     *
     * @param msg The Message to be propagated.
     * @param serviceName Contains the name of the destination service, if any.
     * This will be integrated into the destination address.
     * @param serviceParams Contains the parameters associated with the service,
     * if any. This will be integrated into the destination address.
     * @param prunePeer A peer which should not receive the propagated message
     * or null for all peers. This is sometimes used in flooding type algorithms
     * to avoid sending to the peer that this peer received the message from.
     * @throws IOException is thrown when the message could not be propagated.
     */
    public void propagate( Message msg,
    String serviceName,
    String serviceParams,
    String prunePeer) throws IOException;
    
    /**
     * Returns true if propagation is supported, and enabled.
     *
     * @return <code>true</code> if propagation is supported and enabled
     * otherwise <code>false</code>.
     */
    public boolean isPropagateEnabled();
    
    /**
     * Returns true if propagation is supported.
     *
     * @return <code>true</code> if propagation is supported otherwise
     * <code>false</code>.
     */
    public boolean isPropagationSupported();
    
    /**
     * Returns true if the target address is reachable via this Message
     * Transport otherwise returns false.
     *
     * @return <code>true</code> if the specified endpoint address is reachable
     * otherwise <code>false</code>.
     */
    public boolean ping(EndpointAddress addr);
}

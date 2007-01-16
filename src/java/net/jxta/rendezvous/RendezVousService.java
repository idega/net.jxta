/*
 *
 * $Id: RendezVousService.java,v 1.1 2007/01/16 11:01:28 thomas Exp $
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
 * =========================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.rendezvous;

import java.util.Vector;
import java.util.Enumeration;

import java.io.IOException;

import net.jxta.id.ID;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.EndpointAddress;

import net.jxta.protocol.PeerAdvertisement;
import net.jxta.service.Service;

/**
 * The RendezVous Service provides propagation of messages within a JXTA 
 * PeerGroup.
 *
 * <p/>While the internal protcol of diffusion is left to the implementation of
 * the service, the JXTA RendezVous Service defines a subscription mechanism
 * allowing JXTA peers to receive propagated messages (clients of the service)
 * or become a repeater of the service (rendezvous peers).
 *
 * <p/>The Standard Reference Implementation requires that at least one peer in
 * a PeerGroup act as a Rendezvous. Rendezvous peers can dynamically join or 
 * leave the PeerGroup over time.
 *
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target='_blank'>JXTA Protocols Specification : Rendezvous</a>
 **/
public interface RendezVousService extends Service {
    
    /**
     *  Perform <code>propagate()</code> or <code>walk()</code> using the most
     *  appropriate TTL value for the implementation and configuration. The
     *  message will almost certainly be sent with a TTL value much less than
     *  this value.
     **/
    public static final int DEFAULT_TTL = Integer.MAX_VALUE;
    
    /**
     * Add a peer as a new RendezVousService point.
     *
     * <p/>If/When the RendezVousService accepts the connection, the RendezVous
     * service will invoke the RendezVousMonitor.
     *
     * @param adv the advertisement of the RendezVousService peer
     * @throws IOException When the specified peer is unreachable
     **/
    public void connectToRendezVous(PeerAdvertisement adv) throws IOException;
    
    /**
     * Attempt connection to the specified peer as a new RendezVous point.
     *
     * <p/>If/When the RendezVous accepts the connection, the RendezVous
     * service will invoke the RendezVousMonitor.
     *
     * @param addr EndpointAddress of the rendezvous peer
     * @throws IOException When the specified peer is unreachable
     **/
    public void connectToRendezVous(EndpointAddress addr) throws IOException;
        
    /**
     * Disconnect from the specified rendezvous.
     *
     * @param peerID the PeerId of the RendezVous to disconnect from.
     **/
    public void disconnectFromRendezVous(ID peerID);
    
    
    /**
     * Returns an Enumeration of the PeerID all the RendezVous on which this
     * Peer is currentely connected.
     *
     * @return Enumeration enumeration of RendezVous.
     **/
    public Enumeration getConnectedRendezVous();
    
    /**
     * Returns an Enumeration of the PeerID all the RendezVous on which this
     * Peer failed to connect to.
     *
     * @return Enumeration of the PeerID all the RendezVous on which this
     * Peer failed to connect to.
     **/
    public Enumeration getDisconnectedRendezVous();

    /**
     * Start the local peer as a RendezVous peer.
     **/
    public void startRendezVous();
    
    /**
     * Stop the RendezVous function on the local Peer. All connected Peers are
     * disconnected.
     **/
    public void stopRendezVous();
    
    /**
     * Returns an Enumeration of PeerID of the peers that are currently
     * connected.
     *
     * @return Enumeration of PeerID of the peers that are currently
     * connected.
     *
     **/
    public Enumeration getConnectedPeers();
    
    /**
     * Returns a Vector of PeerID of the peers that are currentely
     * connected.
     *
     * @return Vector of peers connected to that rendezvous
     **/
    public Vector getConnectedPeerIDs();
    
    /**
     *  Registers the given listener under the given name to receive messages 
     *  propagated by the rendezvous service. The given listener will be added
     *  only if no other listener is already registered with these names.
     *
     *  <p/>For historical reasons the messages that will be received are those
     *  address to the ServiceName and ServiceParam pair such that this 
     *  listener's name is equal to their concatenation. For example, if a 
     *  message is propagated or walked to a service named "Cheese" with a 
     *  service parameter of "Burger", it will be received by a propagate 
     *  listener of name "CheeseBurger".
     *
     * @deprecated The naming convention is contrary to the more recent usage 
     * of specifying listeners with separate serviceName and serviceParam. 
     * Prefer {@link #addPropagateListener(String, String, EndpointListener)}.
     *
     * @param name The name of the listener.
     * @param listener An EndpointListener to process the message.
     * @return true if listener was registered, otherwise false.
     **/
    public boolean addPropagateListener(String name, EndpointListener listener) throws IOException;
    
    /**
     * Registers the provided listener under the given serviceName and 
     * serviceParam to receive messages propagated by the Rendezvous service. 
     * The listener will be added only if no other listener is already  
     * registered with these names.
     *
     * @param serviceName The serviceName of the listener.
     * @param serviceParam The serviceParam of the listener.
     * @param listener An EndpointListener to process the message.
     * @return true if listener was registered, otherwise false.
     **/
    public boolean addPropagateListener(String serviceName, String serviceParam, EndpointListener listener);
    
    /**
     * Removes a Listener previously added with addPropagateListener. If the 
     * given listener is not the one currently registered, nothing is removed.
     *
     * @deprecated The naming convention is contrary to the more recent usage 
     * of specifying listeners with separate serviceName and serviceParam. 
     * Prefer {@link #addPropagateListener(String, String, EndpointListener)}.
     *
     * @param name The name of the listener.
     * @param listener An EndpointListener to process the message.
     * @return the listener removed, null if the listener was not removed.
     **/
    public EndpointListener removePropagateListener(String name, EndpointListener listener);
    
    /**
     * Removes a Listener previously added with addPropagateListener.
     * If the given listener is not the one currently registered, nothing is removed.
     *
     * @param serviceName The serviceName of the listener.
     * @param serviceParam The serviceParam of the listener.
     * @param listener An EndpointListener to process the message.
     * @return the listener removed, <tt>null</tt> if the listener was not registered.
     **/
    public EndpointListener removePropagateListener(String serviceName, String serviceParam, EndpointListener listener);
    
    /**
     * Add a listener for RendezVousEvents.
     *
     * @param listener An RendezvousListener to process the event.
     **/
    public void addListener( RendezvousListener listener );
    
    /**
     * Removes a Listener previously added with addListener.
     *
     * @param listener the RendezvousListener listener remove
     **/
    public boolean removeListener( RendezvousListener listener );
    
    /**
     * Propagates a message to the local network and to as many members of
     * the peer group as possible.
     *
     * <p/>This method sends the message to all peers, rendezvous peers and
     * edge peer. This method of propation is very expensive and should
     * be used very cautiously. When rendezvous peers are used in order to
     * cache index of data, it is more efficient to use the walk() method.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * @param msg is the message to propagate.
     * @param serviceName is the name of the service.
     * @param serviceParam is the parameter of the service.
     * @param ttl The requested TTL for the message.
     **/
    public void propagate( Message msg, String serviceName, String serviceParam, int ttl ) throws IOException;
    
    
    /**
     * Propagates a message to the specified peers.
     *
     * @param destPeerIds An enumeration of PeerIDs of the peers that are the
     * intended recipients of the propgated message.
     * @param msg The message to propagate.
     * @param serviceName The name of the service.
     * @param serviceParam The parameter of the service.
     * @param ttl The requested TTL for the message.
     **/
    public void propagate( Enumeration destPeerIds, Message msg, String serviceName, String serviceParam, int ttl ) throws IOException;
    
    
    /**
     * Propagates a message to members of the peer group reachable via the
     * local network. Typically this is accomplished by broadcasting or
     * multicasting.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * @deprecated The prunePeer parameter is ignored and as such this varient
     * is not needed. 
     *
     * @param msg is the message to propagate.
     * @param serviceName is the name of the service.
     * @param serviceParam is the parameter of the service.
     * @param prunePeer is a peer to prune in the propagation.
     * @param ttl The requested TTL for the message.
     **/
    public void propagateToNeighbors(Message msg, String serviceName, String serviceParam, int ttl, String prunePeer) throws IOException;
    
    /**
     * Propagates a message to members of the peer group reachable via the
     * local network. Typically this is accomplished by broadcasting or
     * multicasting.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * @param msg is the message to propagate.
     * @param serviceName is the name of the service.
     * @param serviceParam is the parameter of the service.
     * @param ttl The requested TTL for the message.
     **/
    public void propagateToNeighbors(Message msg, String serviceName, String serviceParam, int ttl) throws IOException;
    
    /**
     * Propagates a message to as many members of the peer group as possible.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * @deprecated The prunePeer parameter is ignored and as such this varient
     * is not needed. 
     *
     * @param msg is the message to propagate.
     * @param serviceName is the name of the service
     * @param serviceParam is the parameter of the service
     * @param ttl The requested TTL for the message.
     * @param prunePeer is a peer to prune in the propagation.
     **/
    public void propagateInGroup(Message msg, String serviceName, String serviceParam, int ttl, String prunePeer) throws IOException;
    
    /**
     * Propagates a message to as many members of the peer group as possible.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * @param msg is the message to propagate.
     * @param serviceName is the name of the service
     * @param serviceParam is the parameter of the service
     * @param ttl The requested TTL for the message.
     **/
    public void propagateInGroup(Message msg, String serviceName, String serviceParam, int ttl) throws IOException;
    
    /**
     * Return true if connected to a rendezvous.
     *
     * @return    true if connected to a rendezvous, false otherwise
     **/
    public boolean isConnectedToRendezVous();
    
    /**
     * Returns the role status of this peer. 
     *
     * @return <tt>true</tt> if this Peer is acting as a "rendezvous" per the
     * implementation definition.
     **/
    public boolean isRendezVous();
    
    /**
     *  Returns the current status of this peer within the current group.
     *
     *  @return the current status.
     **/
    public RendezVousStatus getRendezVousStatus();
        
    /**
     *  Enable or disable the automatic switching between an Edge Peer
     *  and a Rendezvous Peer.
     *
     *  @param auto    true will activate automatic switching
     *  @return        the previous value of this mode
     **/
    public boolean setAutoStart(boolean auto);
    
    /**
     *  Enable or disable the automatic switching between an Edge Peer
     *  and a Rendezvous Peer.
     *
     *  @param auto    <tt>true</tt> will activate automatic switching
     *  @param period  The period of auto-checking
     *  @return        The previous value of this mode.
     **/
    public boolean setAutoStart( boolean auto, long period );
    
    /**
     * Walk a message through the rendezvous peers of the network: only
     * rendezvous peers will receive the message.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * <p/><b>Note</b>: The original msg is not modified and may be reused upon return.
     *
     * @param msg is the message to walk.
     * @param serviceName is the name of the service
     * @param serviceParam is the parameter of the service
     * @param ttl is the maximum TTL of the message.
     * @throws IOException when walking the message is impossible (network failure)
     **/
    public void walk( Message msg, String serviceName, String serviceParam, int ttl ) throws IOException;
    
    /**
     * <p/>Walk a message through the rendezvous peers of the network: only
     * rendezvous peers will receive the message.
     *
     * <p/>Only a single HOP at a time is performed. Messages are always
     * delivered to the destination handler on arrival. This handler
     * is responsible for repropagating further, if deemed appropropriate.
     *
     * <p/>Loop and TTL control are performed automatically.
     *
     * <p/>Messages can be propagated via this method for the first time or
     * can be re-propagated by re-using a message that came in via propagation.
     * In the later case, the TTL and loop detection parameters CANNOT be
     * re-initialized. If one wants to "re-propagate" a message with a new TTL
     * and blank gateways list one must generate a completely new message.
     * This limits the risk of accidental propagation storms, although they
     * can always be engineered deliberately.
     *
     * <p/><b>Note</b>: The original msg is not modified and may be reused upon return.
     *
     * @param destPeerIDs is a Vector of PeerIDs of the peers which are receiving
     * first the walker. Note that each entry in the Vector will create its own
     * walker.
     * @param msg is the message to walk.
     * @param serviceName is the name of the service
     * @param serviceParam is the parameter of the service
     * @param ttl is the maximum TTL of the message.
     * @throws IOException when walking the message is impossible (network failure)
     **/
    public void walk(Vector destPeerIDs, Message msg, String serviceName, String serviceParam, int ttl) throws IOException;
    
    /**
     * Returns a vector of RdvAdvertisement of the local view of rendezvous peers.
     *
     *  <p/><ul>
     *      <li>Values are {@link net.jxta.protocol.RdvAdvertisement}.</li>
     *  </ul>
     *
     * @return Vector containing RdvAdvertisement of the local view of RDV peers.
     **/
    public Vector getLocalWalkView();
    
    /**
     * Set a new deadline for the rendezvous to be proven alive.
     * As a result a lease reponse must be sought and obtained within the
     * specified delay or the rdv is considered disconnected.
     * So, if a lease scheme is used, this can be implemented simply by
     * setting the new lease deadLine accordingly.
     *
     * <p/>A timeout of 0 or less triggers immediate disconnection.
     *
     * @param  peer    The peer to be challenged
     * @param  timeout The delay
     **/
    public void challengeRendezVous(ID peer, long timeout);
}

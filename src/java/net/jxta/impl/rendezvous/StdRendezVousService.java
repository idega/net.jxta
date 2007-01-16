/*
 *
 * $Id: StdRendezVousService.java,v 1.1 2007/01/16 11:02:02 thomas Exp $
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

package net.jxta.impl.rendezvous;


import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.TimerThreadNamer;


/**
 * Base class for providers which implement the JXTA Standard Rendezvous
 * Protocol.
 *
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 **/
public abstract class StdRendezVousService extends RendezVousServiceProvider {
    
    /**
     *  Log4J Category
     **/
    private final static Logger LOG = Logger.getLogger(StdRendezVousService.class.getName());
    
    public final static String ConnectRequest = "Connect";
    public final static String DisconnectRequest = "Disconnect";
    public final static String ConnectedPeerReply = "ConnectedPeer";
    public final static String ConnectedLeaseReply = "ConnectedLease";
    public final static String ConnectedRdvAdvReply = "RdvAdvReply";
    public final static String RdvAdvReply = "RdvAdv";
    
    /**
     * Default Maximum TTL.
     **/
    protected static final int DEFAULT_MAX_TTL = 200;
    
    protected final String              pName;
    protected final String              pParam;
    
    /**
     *  The registered handler for messages using the Standard Rendezvous
     *  Protocol.
     *
     *  @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol
     **/
    private StdRdvProtocolListener      handler;
    
    protected final Timer               timer;
    
    /**
     *  Interface for listeners to : &lt;assignedID>/<group-unique>
     **/
    protected interface StdRdvProtocolListener extends EndpointListener {}
    
    /**
     * Constructor
     **/
    protected StdRendezVousService(PeerGroup group, RendezVousServiceImpl rdvService) {
        
        super(group, rdvService);
        
        MAX_TTL = DEFAULT_MAX_TTL;
        
        pName = rdvService.getAssignedID().toString();
        pParam = group.getPeerGroupID().getUniqueValue().toString();
        
        timer = new Timer(true);
        timer.schedule(new TimerThreadNamer("StdRendezVousService Timer for " + group.getPeerGroupID()), 0);
    }
    
    /**
     * @inheritDoc
     **/
    protected int startApp(String[] argv, StdRdvProtocolListener handler) {
        
        this.handler = handler;
        
        rdvService.endpoint.addIncomingMessageListener(handler, pName, null);
        
        return super.startApp(argv);
    }
    
    /**
     * @inheritDoc
     **/
    protected void stopApp() {
        EndpointListener shouldbehandler = rdvService.endpoint.removeIncomingMessageListener(pName, null);
        
        if (handler != shouldbehandler) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Unregistered listener was not as expected." + handler + " != " + shouldbehandler);
            }
        }
        
        timer.cancel();
        
        super.stopApp();
    }
    
    /**
     * Receive and publish a Rendezvous Peer Advertisement.
     *
     * @param  msg  Message containing the Rendezvous Peer Advertisement
     **/
    protected void processRdvAdvReply(Message msg) {
        
        try {
            MessageElement elem = msg.getMessageElement("jxta", RdvAdvReply);
            
            if (null != elem) {
                XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( elem );
                
                PeerAdvertisement adv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(doc);
                
                DiscoveryService discovery = group.getDiscoveryService();
                
                if (null != discovery) {
                    discovery.publish(adv, DiscoveryService.DEFAULT_EXPIRATION, DiscoveryService.DEFAULT_EXPIRATION);
                }
            }
        } catch (Exception failed) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Publish Rdv Adv failed", failed);
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void processReceivedMessage(Message message, RendezVousPropagateMessage propHdr, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        
        if (srcAddr.getProtocolName().equalsIgnoreCase("jxta")) {
            String idstr = ID.URIEncodingName + ":" + ID.URNNamespace + ":" + srcAddr.getProtocolAddress();
            
            PeerID peerid = null;
            
            try {
                peerid = (PeerID) IDFactory.fromURI(new URI(idstr));
            } catch (URISyntaxException badID) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Bad ID in message", badID);
                }
                return;
            } catch (ClassCastException badID) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("ID is not a peer id", badID);
                }
                return;
            }
            
            if (!group.getPeerID().equals(peerid)) {
                PeerConnection pConn = getPeerConnection(peerid);
                
                if (null == pConn) {
                    PeerViewElement pve = rdvService.rpv.getPeerViewElement(peerid);
                    
                    if (null == pve) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Received " + message + " (" + propHdr.getMsgId() + ") from unrecognized peer : " + peerid);
                        }
                        
                        propHdr.setTTL(Math.min(propHdr.getTTL(), 3)); // will be reduced during repropagate stage.
                        
                        // FIXME 20040503 bondolo need to add tombstones so that we don't end up spamming disconnects.
                        if (rdvService.isRendezVous() || (getPeerConnections().length > 0)) {
                            // edge peers with no rdv should not send disconnect.
                            sendDisconnect(peerid, null);
                        }
                    } else {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Received " + message + " (" + propHdr.getMsgId() + ") from " + pve);
                        }
                    }
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Received " + message + " (" + propHdr.getMsgId() + ") from " + pConn);
                    }
                }
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Received " + message + " (" + propHdr.getMsgId() + ") from loopback.");
                }
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Received " + message + " (" + propHdr.getMsgId() + ") from network -- repropagating with TTL 2");
            }
            
            propHdr.setTTL(Math.min(propHdr.getTTL(), 3)); // will be reduced during repropagate stage.
        }
        
        super.processReceivedMessage(message, propHdr, srcAddr, dstAddr);
    }
    
    /**
     *  @inheritDoc
     **/
    public void propagate(Enumeration destPeerIDs, Message msg, String serviceName, String serviceParam, int ttl) {
        
        ttl = Math.min(ttl, MAX_TTL);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            int numPeers = 0;
            
            try {
                while (destPeerIDs.hasMoreElements()) {
                    ID dest = (ID) destPeerIDs.nextElement();
                    
                    try {
                        PeerConnection pConn = (PeerConnection) getPeerConnection(dest);
                        
                        if (null == pConn) {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Sending " + msg + " (" + propHdr.getMsgId() + ") to " + dest);
                            }
                            
                            EndpointAddress addr = mkAddress((PeerID) dest, PropSName, PropPName);
                            
                            Messenger messenger = rdvService.endpoint.getMessengerImmediate(addr, null);
                            
                            if (null != messenger) {
                                try {
                                    messenger.sendMessage(msg);
                                } catch (IOException ignored) {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        } else {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Sending " + msg + " (" + propHdr.getMsgId() + ") to " + pConn);
                            }
                            
                            if (pConn.isConnected()) {
                                pConn.sendMessage((Message) msg.clone(), PropSName, PropPName);
                            } else {
                                continue;
                            }
                        }
                        numPeers++;
                    } catch (Exception failed) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Failed to send " + msg + " (" + propHdr.getMsgId() + ") to " + dest);
                        }
                    }
                }
            }
            finally {
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.propagateToPeers(numPeers);
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Propagated " + msg + " (" + propHdr.getMsgId() + ") to " + numPeers + " peers.");
                }
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Declined to send " + msg + " ( no propHdr )");
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagateToNeighbors(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(MAX_TTL, ttl);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            try {
                sendToNetwork(msg, propHdr);
                
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.propagateToNeighbors();
                }
            } catch (IOException failed) {
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.propagateToNeighborsFailed();
                }
                
                throw failed;
            }
        }
    }
    
    /**
     * Returns the peer connection or null if not present.
     *
     * @return PeerConnection the peer connection or null if not present.
     **/
    public abstract PeerConnection getPeerConnection(ID id);
    
    /**
     * Returns a list of the current peer connections.
     *
     * @return PeerConnection the peer connection or null if not present.
     **/
    protected abstract PeerConnection[] getPeerConnections();
    
    /**
     * Sends to all connected peers.
     *
     * <p/>Note: The original msg is not modified and may be reused upon return.
     *
     * @param  msg           is the message to propagate.
     * @param  serviceName   is the name of the service
     * @param  serviceParam  is the parameter of the service
     */
    protected int sendToEachConnection(Message msg, RendezVousPropagateMessage propHdr) {
        
        int sentToPeers = 0;
        
        List peers = Arrays.asList(getPeerConnections());
        Iterator eachClient = peers.iterator();
        
        while (eachClient.hasNext()) {
            PeerConnection pConn = (PeerConnection) eachClient.next();
            
            // Check if this rendezvous has already processed this propagated message.
            if (!pConn.isConnected()) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Skipping " + pConn + " for " + msg + "(" + propHdr.getMsgId() + ") -- disconnected.");
                }
                // next!
                continue;
            }
            
            if (propHdr.isVisited(pConn.getPeerID().toURI())) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Skipping " + pConn + " for " + msg + "(" + propHdr.getMsgId() + ") -- already visited.");
                }
                // next!
                continue;
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Sending " + msg + "(" + propHdr.getMsgId() + ") to " + pConn);
            }
            
            if( pConn.sendMessage((Message) msg.clone(), PropSName, PropPName) ) {;
            sentToPeers++;
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sent " + msg + "(" + propHdr.getMsgId() + ") to " + sentToPeers + " of " + peers.size() + " peers.");
        }
        
        return sentToPeers;
    }
    
    /**
     *  Sends a disconnect message to the specified peer.
     *
     *  @param id the peer which we will disconnect from.
     *  @param Messenger the messenger to use in sending the disconnect.
     **/
    protected void sendDisconnect( PeerID peerid, PeerAdvertisement padv ) {
        
        Message msg = new Message();
        
        // The request simply includes the local peer advertisement.
        try {
            msg.replaceMessageElement("jxta", new TextDocumentMessageElement(DisconnectRequest, getPeerAdvertisementDoc(), null));
            
            EndpointAddress addr = mkAddress(peerid, null, null);
            
            RouteAdvertisement hint = null;
            
            if (null != padv) {
                hint = RendezVousServiceImpl.extractRouteAdv(padv);
            }
            
            Messenger messenger = rdvService.endpoint.getMessenger(addr, null);
            
            if (null == messenger) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not get messenger for " + peerid);
                    
                }
                return;
            }
            
            messenger.sendMessage(msg, pName, pParam);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("sendDisconnect failed", e);
            }
        }
    }
    
    /**
     *  Sends a disconnect message to the specified peer.
     *
     *  @param id the peer which we will disconnect from.
     *  @param Messenger the messenger to use in sending the disconnect.
     **/
    protected void sendDisconnect( PeerConnection pConn ) {
        
        Message msg = new Message();
        
        // The request simply includes the local peer advertisement.
        try {
            msg.replaceMessageElement("jxta", new TextDocumentMessageElement(DisconnectRequest, getPeerAdvertisementDoc(), null));
            
            pConn.sendMessage(msg, pName, pParam);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("sendDisconnect failed", e);
            }
        }
    }
}

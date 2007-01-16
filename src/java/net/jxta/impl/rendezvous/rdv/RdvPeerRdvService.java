/*
 *  $Id: RdvPeerRdvService.java,v 1.1 2007/01/16 11:02:12 thomas Exp $
 *
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  =========================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */
package net.jxta.impl.rendezvous.rdv;


import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.TimerTask;
import java.util.List;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;

import net.jxta.rendezvous.RendezvousEvent;

import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.PeerConnection;
import net.jxta.impl.rendezvous.RdvGreeter;
import net.jxta.impl.rendezvous.RdvWalk;
import net.jxta.impl.rendezvous.RdvWalker;
import net.jxta.impl.rendezvous.RendezVousPropagateMessage;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.StdRendezVousService;
import net.jxta.impl.rendezvous.limited.LimitedRangeWalk;
import net.jxta.impl.rendezvous.rendezvousMeter.ClientConnectionMeter;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;


/**
 * A JXTA {@link net.jxta.rendezvous.RendezvousService} implementation which
 * implements the rendezvous server portion of the standard JXTA Rendezvous
 * Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezvousService
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 **/
public class RdvPeerRdvService extends StdRendezVousService {
    
    /**
     *  Log4J Logger
     **/
    private final static Logger LOG = Logger.getLogger(RdvPeerRdvService.class.getName());
    
    public static final String RDV_SVC_NAME = "RdvWalkSvcName";
    public static final String RDV_SVC_PARAM = "RdvWalkSvcParam";
    
    public final static long   GC_INTERVAL = 2 * TimeUtils.AMINUTE;
    
    private final Map clients = Collections.synchronizedMap(new HashMap());
    
    private long leaseDuration = 20L * TimeUtils.AMINUTE;
    private long maxNbOfClients = 200;
    
    private RdvWalk walk = null;
    private RdvGreeter greeter = null;
    private RdvWalker walker = null;
    private WalkListener walkListener = null;
    
    /**
     * Constructor for the RdvPeerRdvService object
     */
    public RdvPeerRdvService(PeerGroup g, RendezVousServiceImpl rdvService) {
        
        super(g, rdvService);
        
        ConfigParams confAdv = (ConfigParams) g.getConfigAdvertisement();
        
        // Get the config. If we do not have a config, we're done; we just keep
        // the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            Advertisement adv = null;
            
            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(rdvService.getAssignedID());
                
                if (null != configDoc) {
                    // XXX 20041027 backwards compatibility
                    configDoc.addAttribute( "type", RdvConfigAdv.getAdvertisementType() );

                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (java.util.NoSuchElementException failed) {
                ;
            }
            
            if (adv instanceof RdvConfigAdv) {
                RdvConfigAdv rdvConfigAdv = (RdvConfigAdv) adv;
                
                if (rdvConfigAdv.getMaxTTL() > 0) {
                    MAX_TTL = rdvConfigAdv.getMaxTTL();
                }
                
                if (rdvConfigAdv.getMaxClients() > 0) {
                    maxNbOfClients = rdvConfigAdv.getMaxClients();
                }
                
                if (rdvConfigAdv.getLeaseDuration() > 0) {
                    leaseDuration = rdvConfigAdv.getLeaseDuration();
                }
            }
        }
        
        // Update the peeradv with that information:
        try {
            XMLDocument params = (XMLDocument)
            StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            Element e = params.createElement("Rdv", Boolean.TRUE.toString());
            
            params.appendChild(e);
            group.getPeerAdvertisement().putServiceParam(rdvService.getAssignedID(), params);
        } catch (Exception ohwell) {
            // don't worry about it for now. It'll still work.
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed adding service params", ohwell);
            }
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("RendezVous Service is initialized for " + group.getPeerGroupID() + " as a Rendezvous peer.");
        }
    }
    
    /**
     *  Listener for
     *
     *  &lt;assignedID>/&lt;group-unique>
     **/
    private class StdRdvRdvProtocolListener implements StdRendezVousService.StdRdvProtocolListener {
        
        /**
         *  {@inheritDoc}
         **/
        public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("[" + group.getPeerGroupID() + "] processing " + msg);
            }
            
            if (msg.getMessageElement("jxta", ConnectRequest) != null) {
                processLeaseRequest(msg);
            }
            
            if (msg.getMessageElement("jxta", DisconnectRequest) != null) {
                processDisconnectRequest(msg);
            }
            
            if (msg.getMessageElement("jxta", RdvAdvReply) != null) {
                processRdvAdvReply(msg);
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected int startApp(String[] argv) {
        
        super.startApp(argv, new StdRdvRdvProtocolListener());
        
        // The other services may not be fully functional but they're there
        // so we can start our subsystems.
        // As for us, it does not matter if our methods are called between init
        // and startApp().
        
        // Start the Greeter / Walker protcol
        walkInit();
        
        timer.scheduleAtFixedRate(new GCTask(), GC_INTERVAL, GC_INTERVAL);
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.startRendezvous();
        }
        
        rdvService.generateEvent(RendezvousEvent.BECAMERDV, group.getPeerID());
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("RdvPeerRdvService is started");
        }
        
        return 0;
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected synchronized void stopApp() {
        
        if (closed) {
            return;
        }
        
        closed = true;
        
        if (walkListener != null) {
            greeter.setEndpointListener(null);
            walkListener = null;
            greeter.stop();
            greeter = null;
        }
        
        if (walker != null) {
            walker.stop();
            walker = null;
        }
        
        if (walk != null) {
            walk.stop();
            walk = null;
        }
        
        // Tell all our clientss that we are going down
        disconnectAllClients();
        
        clients.clear();
        
        super.stopApp();
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.stopRendezvous();
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void challengeRendezVous(ID peer, long delay) {
        throw new UnsupportedOperationException("Not supported by rendezvous");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void disconnectFromRendezVous(ID peerId) {
        
        throw new UnsupportedOperationException("Not supported by rendezvous");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isConnectedToRendezVous() {
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void setChoiceDelay(long delay) {
        // No effect on rendezvous
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Vector getConnectedPeerIDs() {
        
        Vector result = new Vector();
        List allClients = Arrays.asList(clients.values().toArray());
        
        Iterator eachClient = allClients.iterator();
        
        while (eachClient.hasNext()) {
            PeerConnection aConnection = (PeerConnection) eachClient.next();
            
            if (aConnection.isConnected()) {
                result.add(aConnection.getPeerID());
            }
        }
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getConnectedPeers() {
        
        return Collections.enumeration(getConnectedPeerIDs());
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getConnectedRendezVous() {
        // This is a rdv peer. Cannot connect to other rdvs.
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getDisconnectedRendezVous() {
        // This is a rdv peer. Cannot connect to other rdvs.
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagate(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(ttl, MAX_TTL);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            walk((Message) msg.clone(), PropSName, PropPName, ttl);
            //hamada: this is a very expensive operation and therefore not a supported operation
            //sendToEachConnection(msg, propHdr);
            sendToNetwork(msg, propHdr);
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagateInGroup(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(ttl, MAX_TTL);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            walk((Message) msg.clone(), PropSName, PropPName, ttl);
            sendToEachConnection(msg, propHdr);
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     * @deprecated this operation is a privileged operation and expensive 
     * operation to be supported, use walk instead
     **/
    protected void repropagate(Message msg, RendezVousPropagateMessage propHdr, String serviceName, String serviceParam) {
        /*
        try {
            RendezVousPropagateMessage newPropHdr = updatePropHeader(msg, propHdr, serviceName, serviceParam, MAX_TTL);
            
            if (null != newPropHdr) {
                walk((Message) msg.clone(), PropSName, PropPName, MAX_TTL);
                sendToEachConnection(msg, newPropHdr);
                sendToNetwork(msg, newPropHdr);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Null propagate header, declining to repropagate " + msg);
                }
            }
        } catch (Exception ez1) {
            // Not much we can do
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Could not repropagate " + msg + " (" + propHdr.getMsgId() + ")", ez1);
            }
        }
        */
    }
    
    /**
     *  @inheritDoc
     **/
    public PeerConnection getPeerConnection(ID peer) {
        return (PeerConnection) clients.get(peer);
    }
    
    /**
     *  @inheritDoc
     **/
    protected PeerConnection[] getPeerConnections() {
        return (PeerConnection[]) clients.values().toArray(new PeerConnection[0]);
    }
    
    /**
     * Add a client to our collection of clients.
     *
     * @param  padv   The advertisement of the peer to be added.
     * @param  lease  The lease duration in relative milliseconds.
     **/
    private ClientConnection addClient(PeerAdvertisement padv, long lease) {
        ClientConnectionMeter clientConnectionMeter = null;
        
        int eventType;
        ClientConnection pConn;
        
        synchronized (clients) {
            pConn = (ClientConnection) clients.get(padv.getPeerID());
            
            // Check if the peer is already registered.
            if (null != pConn) {
                eventType = RendezvousEvent.CLIENTRECONNECT;
            } else {
                eventType = RendezvousEvent.CLIENTCONNECT;
                pConn = new ClientConnection(group, rdvService, padv.getPeerID());
                clients.put(padv.getPeerID(), pConn);
            }
        }
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            clientConnectionMeter = rendezvousServiceMonitor.getClientConnectionMeter(padv.getPeerID());
        }
        
        if (RendezvousEvent.CLIENTCONNECT == eventType) {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (clientConnectionMeter != null)) {
                clientConnectionMeter.clientConnectionEstablished(lease);
            }
        } else {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (clientConnectionMeter != null)) {
                clientConnectionMeter.clientLeaseRenewed(lease);
            }
        }
        
        rdvService.generateEvent(eventType, padv.getPeerID());
        
        pConn.connect(padv, lease);
        
        return pConn;
    }
    
    /**
     *  Removes the specified client from the clients collections.
     *
     *  @param  pConn   The connection object to remove.
     *  @param  requested   If <code>true</code> then the disconnection was
     *  requested by the remote peer.
     *  @return the ClientConnection object of the client or <code>null</code>
     *  if the client was not known.
     */
    private ClientConnection removeClient(PeerConnection pConn, boolean requested) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Disconnecting client " + pConn);
        }

        if (pConn.isConnected()) {
            pConn.setConnected(false);
            sendDisconnect( pConn );
        }
        
        rdvService.generateEvent( requested ? RendezvousEvent.CLIENTDISCONNECT : RendezvousEvent.CLIENTFAILED, pConn.getPeerID());      
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            ClientConnectionMeter clientConnectionMeter = rendezvousServiceMonitor.getClientConnectionMeter((PeerID) pConn.getPeerID());
            
            clientConnectionMeter.clientConnectionDisconnected(requested);
        }
        
        return (ClientConnection) clients.remove(pConn.getPeerID());
    }
    
    private void disconnectAllClients() {
        Iterator eachConnected = Arrays.asList(clients.values().toArray()).iterator();
        
        while (eachConnected.hasNext()) {
            ClientConnection pConn = (ClientConnection) eachConnected.next();
            
            try {
                removeClient(pConn, false);
            } catch (Exception ez1) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("disconnectClient failed for" + pConn, ez1);
                }
                continue;
            }
        }
    }
    
    /**
     * Handle a disconnection request
     *
     * @param  msg  Message containting the disconnection request.
     */
    private void processDisconnectRequest(Message msg) {
        
        PeerAdvertisement adv = null;
        
        try {
            MessageElement elem = msg.getMessageElement("jxta", DisconnectRequest);
            
            adv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(elem.getMimeType(), elem.getStream());
        } catch (Exception e) {
            return;
        }
        
        ClientConnection pConn = (ClientConnection) clients.get(adv.getPeerID());
        
        if (null != pConn) {
            removeClient(pConn, true);
        }
    }
    
    /**
     *  Handles a lease request message
     *
     *  @param  msg  Message containing the lease request
     */
    private void processLeaseRequest(Message msg) {
        
        PeerAdvertisement padv;
        
        try {
            MessageElement elem = msg.getMessageElement("jxta", ConnectRequest);
            
            padv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(elem.getMimeType(), elem.getStream());
            msg.removeMessageElement(elem);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Cannot retrieve advertisment from lease request", e);
            }
            return;
        }
        
        // Publish the client's peer advertisement
        try {
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (null != discovery) {
                // This is not our own peer adv so we must not share it or keep it that long.
                discovery.publish(padv, leaseDuration * 2, 0 );
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Client peer advertisement publish failed", e);
            }
        }
        
        long lease;
        
        ClientConnection pConn = (ClientConnection) clients.get(padv.getPeerID());
        
        if (null != pConn) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Renewing client lease to " + pConn );
            }
            
            lease = leaseDuration;
        } else {
            if( clients.size() < maxNbOfClients ) {
                lease = leaseDuration;
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Offering new client lease to " + padv.getName() + " [" + padv.getPeerID() + "]");
                }
            } else {
                lease = 0;
                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Max clients exceeded, declining lease request from: " + padv.getName() + " [" + padv.getPeerID() + "]");
                }
            }
        }
        
        if (lease > 0) {
            pConn = addClient(padv, lease);
            
            // FIXME 20041015 bondolo We're supposed to send a lease 0 if we can't accept new clients.
            sendLease(pConn, leaseDuration);
        }
    }
    
    /**
     *  Sends a Connected lease reply message to the specified peer
     *
     * @param  pConn  The client peer.
     * @param  lease  lease duration.
     * @return        Description of the Returned Value
     */
    private boolean sendLease(ClientConnection pConn, long lease) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending lease (" + lease + ") to " + pConn.getPeerName());
        }
        
        Message msg = new Message();
        
        msg.addMessageElement("jxta", new TextDocumentMessageElement(ConnectedRdvAdvReply, getPeerAdvertisementDoc(), null));
        
        msg.addMessageElement("jxta", new StringMessageElement(ConnectedPeerReply, group.getPeerID().toString(), null));
        
        msg.addMessageElement("jxta", new StringMessageElement(ConnectedLeaseReply, Long.toString(lease), null));
        
        return pConn.sendMessage(msg, pName, pParam);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void walk(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Walk initiated for " + msg + " [" + serviceName + "/" + serviceParam + "]");
        }
        
        if (walker == null) {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walkFailed();
            }
            
            // The walker is not yet initialized. Fail.
            IOException failure = new IOException("Cannot walk message : no walker");
            
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot walk message : no walker", failure);
            }
            throw failure;
        }
        
        msg.replaceMessageElement("jxta", new StringMessageElement(RDV_SVC_NAME, serviceName, null));
        
        msg.replaceMessageElement("jxta", new StringMessageElement(RDV_SVC_PARAM, serviceParam, null));
        
        try {
            walker.sendMessage( null, msg, pName, pParam, ttl, null );
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walk();
            }
        } catch (IOException failure) {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walkFailed();
            }
            
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot send message with Walker", failure);
            }
            IOException failed = new IOException("Cannot send message with Walker");
            
            failed.initCause(failure);
            throw failed;
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void walk(Vector destPeerIDs, Message msg, String serviceName, String serviceParam, int defaultTTL) throws IOException {
        
        if ((destPeerIDs == null) || (destPeerIDs.size() == 0)) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("No destination");
            }
            throw new IOException("no destination");
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("walk initiated for :" + "\n\tsvc name:" + serviceName + "\tsvc params:" + serviceParam);
        }
        
        if (walker == null) {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walkFailed();
            }
            
            // The walker is not yet initialized. Fail.
            IOException failure = new IOException("Cannot walk message : no walker");
            
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot walk message : no walker", failure);
            }
            throw failure;
        }
        
        msg.replaceMessageElement("jxta", new StringMessageElement(RDV_SVC_NAME, serviceName, null));
        
        msg.replaceMessageElement("jxta", new StringMessageElement(RDV_SVC_PARAM, serviceParam, null));
        
        PeerID dest = null;
        
        for (int i = 0; i < destPeerIDs.size(); ++i) {
            try {
                dest = (PeerID) destPeerIDs.elementAt(i);
                
                Message tmpMsg = (Message) msg.clone();
                
                walker.sendMessage( dest, tmpMsg, pName, pParam, defaultTTL, null );
            } catch (Exception failed) {
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.walkToPeersFailed();
                }
                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Cannot send message with Walker to: " + dest, failed);
                }
                
                IOException failure = new IOException("Cannot send message with Walker to: " + dest );
                failure.initCause( failed );
            }
        }
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.walkToPeers(destPeerIDs.size());
        }
    }
    
    private void walkInit() {
        
        // Create a LimitedRange Walk
        walk = new LimitedRangeWalk(group, pName, pParam, rdvService.rpv);
        
        // Get a Greeter
        greeter = walk.getGreeter();
        
        if (greeter == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot get Greeter");
            }
            return;
        }
        
        // Start the Greeter
        greeter.start();
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Greeter listening on " + pName + "/" + pParam);
        }
        
        // We need to use a Walker in order to propagate the request
        // when when have no answer.
        
        walker = walk.getWalker();
        
        if (walker == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot get Walker");
            }
            return;
        }
        
        // Set our Endpoint Listener
        walkListener = new WalkListener();
        greeter.setEndpointListener(walkListener);
    }
    
    /**
     *  Periodic cleanup task
     **/
    private class GCTask extends TimerTask {
        
        /**
         *  {@inheritDoc
         **/
        public void run() {
                        
            try {
                long gcStart = TimeUtils.timeNow();
                int gcedClients = 0;

                List allClients = Arrays.asList(clients.values().toArray());
                Iterator eachClient = allClients.iterator();
                
                while (eachClient.hasNext()) {
                    ClientConnection pConn = (ClientConnection) eachClient.next();
                    
                    try {
                        long now = TimeUtils.timeNow();
                        
                        if (!pConn.isConnected() || (pConn.getLeaseEnd() < now)) {
                            // This client has dropped out or the lease is over.
                            // remove it.
                            
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("GC CLIENT: dropping " + pConn);
                            }
                            
                            pConn.setConnected(false);
                            removeClient(pConn, false);
                            gcedClients++;
                        }
                    } catch (Exception e) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("GCTask failed for " + pConn, e);
                        }
                    }
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Client GC " + gcedClients + " of " + allClients.size() + " clients completed in " + TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), gcStart) + "ms." );
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            }
        }
    }    
    
    /**
     *  @inheritDoc
     **/
    private class WalkListener implements EndpointListener {
        
        /**
         *  {@inheritDoc}
         **/
        public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {
            
            MessageElement serviceME = msg.getMessageElement("jxta", RDV_SVC_NAME);
            
            if (null == serviceME) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Discarding " + msg + " because its missing service name element");
                }
                return;
            }
            
            msg.removeMessageElement(serviceME);
            String sName = serviceME.toString();
            
            MessageElement paramME = msg.getMessageElement("jxta", RDV_SVC_PARAM);
            
            String sParam;
            
            if (null == paramME) {
                sParam = null;
            } else {
                msg.removeMessageElement(paramME);
                sParam = paramME.toString();
            }
            
            EndpointAddress realDest = new EndpointAddress(dstAddr, sName, sParam);
            
            EndpointListener listener = rdvService.getListener(sName + sParam);
            
            if (listener != null) {
                // We have a local listener for this message.
                // Deliver it.
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Calling local listener for [" + sName + "/" + sParam + "] with " + msg);
                }
                
                try {
                    listener.processIncomingMessage(msg, srcAddr, realDest);
                } catch (Throwable ignored) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Uncaught Throwable during callback of (" + listener + ") to " + sName + "/" + sParam, ignored);
                    }
                }
                
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.receivedMessageProcessedLocally();
                }
            }
        }
    }
}

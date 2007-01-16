/*
 *  $Id: EdgePeerRdvService.java,v 1.1 2007/01/16 11:02:09 thomas Exp $
 *
 *  Copyright (c) 2001-2004 Sun Microsystems, Inc.  All rights reserved.
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
package net.jxta.impl.rendezvous.edge;


import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.TimerTask;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;

import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.PeerConnection;
import net.jxta.impl.rendezvous.RendezVousPropagateMessage;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.StdRendezVousService;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousConnectionMeter;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.rendezvous.rpv.PeerViewListener;
import net.jxta.impl.rendezvous.rpv.PeerViewEvent;
import net.jxta.impl.util.TimeUtils;


/**
 * A JXTA {@link net.jxta.rendezvous.RendezvousService} implementation which
 * implements the client portion of the standard JXTA Rendezvous Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezvousService
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 **/
public class EdgePeerRdvService extends StdRendezVousService implements PeerViewListener {
    
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger(EdgePeerRdvService.class.getName());
    
    // period.
    private final static long MONITOR_INTERVAL = 20 * TimeUtils.ASECOND;
    private final static long ADDEVENT_DELAY = 3 * TimeUtils.ASECOND;
    private final static long CHALLENGE_TIMEOUT = 90 * TimeUtils.ASECOND;
    
    /**
     *  Number of rendezvous we will try to connect to.
     **/
    private final int MAX_RDV_CONNECTIONS = 1;
    
    private boolean useOnlySeeds = false;
    private long LEASE_MARGIN = 5 * TimeUtils.AMINUTE;
    private long maxChoiceDelay = ADDEVENT_DELAY;
    
    /**
     * This the time in absolute milliseconds at which the monitor is scheduled
     * to start.The monitor will not be scheduled at all until there is at
     * least one item in the peerview. The more items in the peerview,
     * the earlier we start. Once there are at least rdvConfig.minHappyPeerView items
     * it guaranteed that we start immediately because the start date
     * is in the past.
     **/
    private long monitorStartAt = -1;
    
    /**
     * Once choice delay has reached zero, any ADD event could trigger
     * a attempt at connecting to one of the rdvs. If these events come
     * in bursts while we're not yet connected, we might end-up doing
     * many parallel attempts, which is a waste of bandwidth. Instead
     * we refrain from doing more than one attempt every ADDEVENT_DELAY
     **/
    private long monitorNotBefore = -1;
    
    /**
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.peer.ID}.</li>
     *      <li>Values are {@link net.jxta.impl.rendezvous.RdvConnection}.</li>
     *  </ul>
     **/
    private final Map rendezVous = Collections.synchronizedMap(new HashMap());
    
    /**
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.peer.PeerID}.</li>
     *      <li>Values are {@link java.lang.Long} containing the time at which
     * the rendezvous disconnected.</li>
     *  </ul>
     **/
    private final Set disconnectedRendezVous = Collections.synchronizedSet(new HashSet());
    
    /**
     * Constructor
     *
     * @param  g     Description of Parameter
     * @param  sadv  Description of Parameter
     **/
    public EdgePeerRdvService(PeerGroup group, RendezVousServiceImpl rdvService) {
        
        super(group, rdvService);
        
        ConfigParams confAdv = group.getConfigAdvertisement();
        
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
                
                if (-1 != rdvConfigAdv.getMaxTTL()) {
                    MAX_TTL = rdvConfigAdv.getMaxTTL();
                }
                
                useOnlySeeds = rdvConfigAdv.getUseOnlySeeds();
                
                if (0 != rdvConfigAdv.getLeaseMargin()) {
                    LEASE_MARGIN = rdvConfigAdv.getLeaseMargin();
                }
                
                if( rdvConfigAdv.getMinHappyPeerView() > 0) {
                    maxChoiceDelay = rdvConfigAdv.getMinHappyPeerView() * ADDEVENT_DELAY;
                } else {
                    maxChoiceDelay = ADDEVENT_DELAY;
                }
            }
        }
        
        // If edge peers ever use a walker, here is a good point to instantiate 
        // one with:
        // For protocol bw compatibility, edge peers use the legacy
        // rdv protocol rather than the walker.
        // walker = new LimitedRangeWalk(group, pName, pParam, rdvService.rpv);
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("RendezVous Service is initialized for " + group.getPeerGroupID() + " as an Edge peer.");
        }
    }
    
    /**
     *  Listener for
     *
     *  &lt;assignedID>
     **/
    private class StdRdvEdgeProtocolListener implements StdRendezVousService.StdRdvProtocolListener {
        
        /**
         *  {@inheritDoc}
         **/
        public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("[" + group.getPeerGroupID() + "] processing " + msg);
            }
            
            if (msg.getMessageElement("jxta", RdvAdvReply) != null) {
                processRdvAdvReply(msg);
            }
            
            if ((msg.getMessageElement("jxta", ConnectedPeerReply) != null) || (msg.getMessageElement("jxta", ConnectedRdvAdvReply) != null)) {
                processConnectedReply(msg);
            }
            
            if (msg.getMessageElement("jxta", DisconnectRequest) != null) {
                processDisconnectRequest(msg);
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected int startApp(String[] arg) {
        
        super.startApp(arg, new StdRdvEdgeProtocolListener());
        
        // The other services may not be fully functional but they're there
        // so we can start our subsystems.
        // As for us, it does not matter if our methods are called between init
        // and startApp().
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.startEdge();
        }
        
        rdvService.generateEvent(RendezvousEvent.BECAMEEDGE, group.getPeerID());
        
        long choiceDelay;
        
        // When quickStart is true, start-up is fast. This is typically
        // used when useOnlySeeds is true, on the grounds
        // that the choice is normaly very limited and that all candidates
        // are queried early on, and at roughly the same time. So, the
        // first one that responds will do just as well.
        // Else, we do compute a dead line that will allow for the
        // peerview to fill up a bit.
        
        if (useOnlySeeds) {
            choiceDelay = 0;
        } else {
            // If there are already peers in the peer view, then it is
            // worth scheduling the monitor; else, wait for an ADD event.
            // Else we'll schedule it according to how many peers we
            // already have in the peerview. If there are enough we could
            // go immediately.
            
            List rpv = rdvService.getLocalWalkView();
            
            int rpvSize = rpv.size();
            
            choiceDelay = Math.max(0, maxChoiceDelay - (rpvSize * ADDEVENT_DELAY));
        }
        
        monitorStartAt = TimeUtils.toAbsoluteTimeMillis(choiceDelay);
        
        timer.schedule(new MonitorTask(), choiceDelay, MONITOR_INTERVAL);
        
        rdvService.rpv.addListener(this);
        
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
        
        disconnectFromAllRendezVous();
        
        // If edge peers are ever converted to use a walker,
        // remember to stop the walker here, with: walker.stop();
        
        rdvService.rpv.removeListener(this);
        
        super.stopApp();
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.stopEdge();
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Vector getConnectedPeerIDs() {
        
        return new Vector();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getConnectedPeers() {
        
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isConnectedToRendezVous() {
        return !rendezVous.isEmpty();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getConnectedRendezVous() {
        
        return Collections.enumeration(Arrays.asList(rendezVous.keySet().toArray()));
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getDisconnectedRendezVous() {
        List result = Arrays.asList(disconnectedRendezVous.toArray());
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug(result.size() + " rendezvous disconnections.");
        }
        
        return Collections.enumeration(result);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void challengeRendezVous(ID peerid, long delay) {
        
        // If immediate failure is requested, just do it.
        // <code>disconnectFromRendezVous()</code> will at least get the peer
        // removed from the peerView, even if it is not currently a rendezvous
        // of ours. That permits to purge from the peerview rdvs that we try
        // and fail to connect to, faster than the background keep alive done
        // by PeerView itself.
        if (delay <= 0) {
            removeRdv( peerid, false );
            return;
        }
        
        RdvConnection pConn = (RdvConnection) rendezVous.get(peerid);
        
        if (null != pConn) {
            long adjusted_delay = Math.max(0, Math.min(TimeUtils.toRelativeTimeMillis(pConn.getLeaseEnd()), delay));
            
            pConn.setLease(adjusted_delay, adjusted_delay);
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void disconnectFromRendezVous(ID peerId) {
        
        removeRdv((PeerID) peerId, false);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagate(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(ttl, MAX_TTL);
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            sendToEachConnection(msg, propHdr);
            sendToNetwork(msg, propHdr);
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Declining to propagate " + msg + " (No prop header)");
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
            sendToEachConnection(msg, propHdr);
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Declining to propagate " + msg + " (No prop header)");
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected void repropagate(Message msg, RendezVousPropagateMessage propHdr, String serviceName, String serviceParam) {
        
        try {
            propHdr = updatePropHeader(msg, propHdr, serviceName, serviceParam, MAX_TTL);
            
            if (null != propHdr) {
                // Note (hamada): This is an unnecessary operation, and serves 
                // no purpose other than the additional loads it imposes on the
                // rendezvous.  Local subnet network operations should be (and are)
                // sufficient to achieve the goal.
                //sendToEachConnection(msg, propHdr);
                sendToNetwork(msg, propHdr);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Declining to repropagate " + msg + " (No prop header)");
                }
            }
        } catch (Exception ez1) {
            // Not much we can do
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not repropagate " + msg, ez1);
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void walk(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        propagateInGroup(msg, serviceName, serviceParam, ttl);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void walk(Vector destPeerIDs, Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        propagate(destPeerIDs.elements(), msg, serviceName, serviceParam, ttl);
    }
    
    /**
     *  @inheritDoc
     **/
    public PeerConnection getPeerConnection(ID peer) {
        return (PeerConnection) rendezVous.get(peer);
    }
    
    /**
     *  @inheritDoc
     **/
    protected PeerConnection[] getPeerConnections() {
        return (PeerConnection[]) rendezVous.values().toArray(new PeerConnection[0]);
    }
    
    /**
     *  Attempt to connect to a rendezvous we have not previously connected to.
     *
     *  @param radv Rendezvous advertisement for the Rdv we want to connect to.
     **/
    private void newLeaseRequest(RdvAdvertisement radv) throws IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending new lease request to " + radv.getPeerID() );
        }
        
        RendezvousConnectionMeter rendezvousConnectionMeter = null;
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter( radv.getPeerID().toString() );
        }
        
        EndpointAddress addr = mkAddress(radv.getPeerID(), null, null);
        
        RouteAdvertisement hint = radv.getRouteAdv();
        
        Messenger messenger = rdvService.endpoint.getMessenger(addr, hint);
        
        if (null == messenger) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not get messenger for " + addr );
            }
            
            throw new IOException("Could not connect to " + addr  );
        }
        
        Message msg = new Message();
        
        // The request simply includes the local peer advertisement.
        msg.replaceMessageElement("jxta", new TextDocumentMessageElement(ConnectRequest, getPeerAdvertisementDoc(), null));
        
        messenger.sendMessage(msg, pName, pParam);
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousConnectionMeter != null)) {
            rendezvousConnectionMeter.beginConnection();
        }
    }
    
    private void disconnectFromAllRendezVous() {
        Iterator eachRendezvous = Arrays.asList(rendezVous.values().toArray()).iterator();
        
        while (eachRendezvous.hasNext()) {
            try {
                RdvConnection pConn = (RdvConnection) eachRendezvous.next();
                
                disconnectFromRendezVous(pConn.getPeerID());
            } catch (Exception failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("disconnectFromRendezVous failed ", failed);
                }
            }
        }
    }
    
    /**
     *  Handle a disconnection request from a remote peer.
     *
     * @param  msg  Description of Parameter
     */
    private void processDisconnectRequest(Message msg) {
        
        try {
            MessageElement elem = msg.getMessageElement("jxta", DisconnectRequest);
            
            if (null != elem) {
                InputStream is = elem.getStream();
                PeerAdvertisement adv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(elem.getMimeType(), is);
                
                RdvConnection rdvConnection = (RdvConnection) rendezVous.get(adv.getPeerID());
                
                if (null != rdvConnection) {
                    rdvConnection.setConnected(false);
                    removeRdv(adv.getPeerID(), true);
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Ignoring disconnect request from " + adv.getPeerID());
                    }
                }
            }
        } catch (Exception failure) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failure processing disconnect request", failure);
            }
        }
    }
    
    /**
     * Add a rendezvous to our collection of rendezvous peers.
     *
     * @param  padv   PeerAdvertisement for the rendezvous peer.
     * @param  lease  The duration of the lease in relative milliseconds.
     */
    private void addRdv(PeerAdvertisement padv, long lease) {
        
        int eventType;
        
        RdvConnection rdvConnection;
        
        synchronized (rendezVous) {
            rdvConnection = (RdvConnection) rendezVous.get(padv.getPeerID());
            
            if (null == rdvConnection) {
                rdvConnection = new RdvConnection(group, rdvService, padv.getPeerID());
                rendezVous.put(padv.getPeerID(), rdvConnection);
                disconnectedRendezVous.remove(padv.getPeerID());
                eventType = RendezvousEvent.RDVCONNECT;
            } else {
                eventType = RendezvousEvent.RDVRECONNECT;
            }
        }
        
        // Check if the peer is already registered.
        if (RendezvousEvent.RDVRECONNECT == eventType) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Renewed RDV lease from " + rdvConnection);
            }
            
            // Already connected, just upgrade the lease
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
                RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter(padv.getPeerID());
                
                rendezvousConnectionMeter.leaseRenewed(lease);
            }
        } else {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("New RDV lease from " + rdvConnection);
            }
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
                RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter(padv.getPeerID());
                
                rendezvousConnectionMeter.connectionEstablished(lease);
            }
        }
        
        rdvConnection.connect(padv, lease, Math.min(LEASE_MARGIN, (lease / 2)));
        
        rdvService.generateEvent(eventType, padv.getPeerID());
    }
    
    /**
     * Remove the specified rendezvous from our collection of rendezvous.
     *
     * @param  rdvid  the id of the rendezvous to remove.
     **/
    private void removeRdv(ID rdvid, boolean requested) {
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Disconnect from RDV " + rdvid );
        }
        
        PeerConnection rdvConnection;
        
        synchronized (this) {
            rdvConnection = (PeerConnection) rendezVous.remove(rdvid);
            
            // let's add it to the list of disconnected rendezvous
            if( null != rdvConnection ) {
                disconnectedRendezVous.add(rdvid);
            }
        }
        
        if (null != rdvConnection) {
            if (rdvConnection.isConnected()) {
                rdvConnection.setConnected(false);
                sendDisconnect( rdvConnection );
            }
        }
        
        /*
         *  Remove the rendezvous we are disconnecting from the peerview as well.
         *  This prevents us from immediately reconnecting to it.
         */
        rdvService.rpv.notifyFailure((PeerID) rdvid, false);
        
        rdvService.generateEvent(requested ? RendezvousEvent.RDVDISCONNECT : RendezvousEvent.RDVFAILED, rdvid);
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter((PeerID) rdvid);
            
            rendezvousConnectionMeter.connectionDisconnected();
        }
    }
    
    private void sendLeaseRequest(RdvConnection pConn) throws IOException {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending Lease request to " + pConn);
        }
        
        RendezvousConnectionMeter rendezvousConnectionMeter = null;
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter( pConn.getPeerID().toString() );
        }
        
        Message msg = new Message();
        
        // The request simply includes the local peer advertisement.
        msg.replaceMessageElement("jxta", new TextDocumentMessageElement(ConnectRequest, getPeerAdvertisementDoc(), null));
        
        pConn.sendMessage(msg, pName, pParam);
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousConnectionMeter != null)) {
            rendezvousConnectionMeter.beginConnection();
        }
    }
    
    /**
     * Description of the Method
     *
     * @param  msg  Description of Parameter
     **/
    private void processConnectedReply(Message msg) {
        
        // get the Peer Advertisement of the RDV.
        MessageElement elem = msg.getMessageElement("jxta", ConnectedRdvAdvReply);
        
        if (null == elem) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("missing rendezvous peer advertisement");
            }
            return;
        }
                
        long lease;
        
        try {
            MessageElement el = msg.getMessageElement("jxta", ConnectedLeaseReply);
            
            if (el == null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("missing lease");
                }
                return;
            }
            lease = Long.parseLong(el.toString());
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Parse lease failed with ", e);
            }
            return;
        }
        
        ID pId;
        MessageElement el = msg.getMessageElement("jxta", ConnectedPeerReply);
        
        if (el == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("missing rdv peer");
            }
            return;
        }
        
        try {
            pId = IDFactory.fromURI(new URI(el.toString()));
        } catch (URISyntaxException badID) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Bad RDV peer ID");
            }
            return;
        }
        
        if (lease <= 0) {
            removeRdv(pId, false);
        } else {
            if (rendezVous.containsKey(pId)
            || ((rendezVous.size() < MAX_RDV_CONNECTIONS) && (rdvService.rpv.getPeerViewElement(pId) != null))) {
                 InputStream is = null;

                 PeerAdvertisement padv = null;

                 try {
                    is = elem.getStream();

                    padv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(elem.getMimeType(), is);

                    // This is not our own peer adv so we must not keep it longer than 
                    // its expiration time.
                    DiscoveryService discovery = group.getDiscoveryService();

                    if (null != discovery) {
                        // This is not our own peer adv so we must not share it or keep it that long.
                        discovery.publish(padv, lease * 2, 0);
                    }
                } catch (Exception e) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("failed to publish Rendezvous Advertisement", e);
                    }
                } finally {
                    if (null != is) {
                        try {
                            is.close();
                        } catch (IOException ignored) {
                            ;
                        }
                    }
                    is = null;
                }

                if (null == padv) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("missing rendezvous peer advertisement");
                    }
                    return;
                }

                String rdvName = padv.getName();

                if (null == padv.getName()) {
                    rdvName = pId.toString();
                }

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("RDV Connect Response : peer=" + rdvName + " lease=" + lease + "ms");
                }
        
                addRdv(padv, lease);
            } else {
                LOG.debug("Ignoring lease offer from " + pId);
                // XXX bondolo 20040423 perhaps we should send a disconnect here.
            }
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public void setChoiceDelay(long delay) {
        monitorStartAt = TimeUtils.toAbsoluteTimeMillis(delay);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void peerViewEvent(PeerViewEvent event) {
        
        int theEventType = event.getType();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + group.getPeerGroupName() + "] Processing " + event);
        }
        
        switch (theEventType) {
            
            case PeerViewEvent.ADD:
                synchronized (this) {
                    try {
                        // There is a new rdv in the peerview. If we are not
                        // connected, it is worth a try, right away.
                        
                        // Use the single timer thread rather than doing it
                        // from this thread which belongs to the invoker.
                        // This removes risks of dealocks and other calamities.
                        // All we have to do is to change the schedule.
                        
                        if (!rendezVous.isEmpty()) {
                            break;
                        }
                        
                        // We do not act upon every single add event. If they
                        // come in storms as they do during boot, it would
                        // make us launch many immediate attempts in parallel,
                        // which causes useless traffic.  As long as
                        // choiceDelay is not exhausted we just reschedule
                        // accordingly. Once choiceDelay is exhausted, we
                        // schedule for immediate execution, but only if we
                        // haven't done so in the last ADDEVENT_DELAY.
                        
                        long choiceDelay;
                        
                        if (monitorStartAt == -1) {
                            // The startDate had never been decided. Initialize it now.
                            choiceDelay = maxChoiceDelay;
                            monitorStartAt = TimeUtils.toAbsoluteTimeMillis(choiceDelay);
                        } else {
                            choiceDelay = TimeUtils.toRelativeTimeMillis(monitorStartAt);
                        }
                        
                        if (choiceDelay <= 0) {
                            if (TimeUtils.toRelativeTimeMillis(monitorNotBefore) > 0) {
                                break;
                            }
                            monitorNotBefore = TimeUtils.toAbsoluteTimeMillis(ADDEVENT_DELAY);
                            choiceDelay = 0;
                        } else {
                            monitorStartAt -= ADDEVENT_DELAY;
                        }
                        
                        // Either way, we're allowed to (re) schedule; possibly immediately.
                        
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Scheduling rdv monitor in " + choiceDelay + "ms.");
                        }
                        
                        timer.schedule(new MonitorTask(), choiceDelay);
                    } catch (Exception anything) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Event could not be processed", anything);
                        }
                        // Don't do it, then. The likely cause is that this
                        // monitor is being closed.
                    }
                }
                break;
                
            case PeerViewEvent.REMOVE:
            case PeerViewEvent.FAIL:
                PeerViewElement pve = event.getPeerViewElement();
                
                ID failedPVE = pve.getRdvAdvertisement().getPeerID();
                
                RdvConnection pConn = (RdvConnection) rendezVous.get(failedPVE);
                
                if (null != pConn) {
                    pConn.setConnected(false);
                    removeRdv(pConn.getPeerID(), false);
                }
                break;
                
            default:
                break;
        }
    }
    
    /**
     *  Connects to a random rendezvous from the peer view.
     **/
    private void connectToRandomRdv() {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Periodic rendezvous connect attempt for " + group.getPeerGroupID());
        }
        
        List currentView = new ArrayList(Arrays.asList(rdvService.rpv.getView().toArray()));
        
        Collections.shuffle(currentView);
        
        while (!currentView.isEmpty()) {
            PeerViewElement pve = (PeerViewElement) currentView.remove(0);
            
            RdvAdvertisement radv = pve.getRdvAdvertisement();
            
            if (null == radv) {
                continue;
            }
            
            if( null != getPeerConnection( radv.getPeerID() ) ) {
                continue;
            }
            
            try {
                newLeaseRequest(radv);
                break;
            } catch (IOException ez) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("rdv connection failed.", ez);
                }
            }
        }
    }
    
    /**
     *  A timer task for monitoring our active rendezvous connections
     *
     *  <p/>Checks leases, challenges when peer adv has changed, initiates
     *  lease renewals, starts new lease requests.
     **/
    private class MonitorTask extends TimerTask {
        
        /**
         *  @inheritDoc
         **/
        public void run() {
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("[" + group.getPeerGroupID() + "] Periodic rendezvous check");
                }
                
                Iterator eachRendezvous = Arrays.asList(rendezVous.values().toArray()).iterator();
                
                while (eachRendezvous.hasNext()) {
                    RdvConnection pConn = (RdvConnection) eachRendezvous.next();
                    
                    try {
                        if ( !pConn.isConnected() ) {
                            if (LOG.isEnabledFor(Level.INFO)) {
                                LOG.debug("[" + group.getPeerGroupID() + "] Lease expired. Disconnected from " + pConn);
                            }
                            removeRdv(pConn.getPeerID(), false);
                            continue;
                        }
                        
                        if (pConn.peerAdvertisementHasChanged()) {
                            // Pretend that our lease is expiring, so that we do not rest
                            // until we have proven that we still have an rdv.
                            
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("[" + group.getPeerGroupID() + "] Local PeerAdvertisement changed. Challenging " + pConn);
                            }
                            
                            challengeRendezVous(pConn.getPeerID(), CHALLENGE_TIMEOUT);
                            continue;
                        }
                        
                        if (TimeUtils.toRelativeTimeMillis(pConn.getRenewal()) <= 0) {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("[" + group.getPeerGroupID() + "] Attempting lease renewal for " + pConn);
                            }
                            
                            sendLeaseRequest(pConn);
                        }
                    } catch (Exception e) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn( "[" + group.getPeerGroupID() + "] Failure while checking " + pConn, e);
                        }
                    }
                }
                
                // Not enough Rdvs? Try finding more.
                if (rendezVous.size() < MAX_RDV_CONNECTIONS) {
                    connectToRandomRdv();
                }
            } catch (Throwable t) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Uncaught throwable in thread :" + Thread.currentThread().getName(), t);
                }
            }
        }
    }
}

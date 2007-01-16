/*
 *  $Id: AdhocPeerRdvService.java,v 1.1 2007/01/16 11:02:10 thomas Exp $
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
package net.jxta.impl.rendezvous.adhoc;


import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;

import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.RendezVousPropagateMessage;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.RendezVousServiceProvider;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;


/**
 * A JXTA {@link net.jxta.rendezvous.RendezvousService} implementation which
 * implements the ad hoc portion of the standard JXTA Rendezvous Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezvousService
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 **/
public class AdhocPeerRdvService extends RendezVousServiceProvider {
    
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger(AdhocPeerRdvService.class.getName());
    
    /**
     * Default Maximum TTL. This is minimum needed to bridge networks.
     **/
    private static final int DEFAULT_MAX_TTL = 2;
    
    /**
     * Constructor
     *
     * @param  g     Description of Parameter
     * @param  sadv  Description of Parameter
     **/
    public AdhocPeerRdvService(PeerGroup g, RendezVousServiceImpl rdvService) {
        
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
                
                MAX_TTL = (-1 != rdvConfigAdv.getMaxTTL()) ? rdvConfigAdv.getMaxTTL() : DEFAULT_MAX_TTL;
            } else {
                MAX_TTL = DEFAULT_MAX_TTL;
            }
        } else {
            MAX_TTL = DEFAULT_MAX_TTL;
        }

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("RendezVous Service is initialized for " + g.getPeerGroupID() + " as an ad hoc peer. ");
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected int startApp(String[] arg) {
        
        super.startApp(arg);
        
        // The other services may not be fully functional but they're there
        // so we can start our subsystems.
        // As for us, it does not matter if our methods are called between init
        // and startApp().
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.startEdge();
        }
        
        // we are nominally an edge peer
        rdvService.generateEvent(RendezvousEvent.BECAMEEDGE, group.getPeerID());
        
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
        
        super.stopApp();
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.stopEdge();
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Vector getConnectedPeerIDs() {
        
        return new Vector(0);
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
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getConnectedRendezVous() {
        
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getDisconnectedRendezVous() {
        
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void connectToRendezVous(PeerAdvertisement adv) throws IOException {
        
        throw new UnsupportedOperationException("Not supported by ad hoc");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void connectToRendezVous(EndpointAddress addr) throws IOException {
        
        throw new UnsupportedOperationException("Not supported by ad hoc");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void challengeRendezVous(ID peer, long delay) {
        
        throw new UnsupportedOperationException("Not supported by ad hoc");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void disconnectFromRendezVous(ID peerId) {
        
        throw new UnsupportedOperationException("Not supported by ad hoc");
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void setChoiceDelay(long delay) {
    }

    /**
     *  {@inheritDoc}
     **/
    public void propagate(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(ttl, MAX_TTL);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
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
            sendToNetwork(msg, propHdr);
            
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagate(Enumeration destPeerIDs, Message msg, String serviceName, String serviceParam, int ttl) {
        
        ttl = Math.min(ttl, MAX_TTL);
        
        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, ttl);
        
        if (null != propHdr) {
            int numPeers = 0;
            
            try {
                while (destPeerIDs.hasMoreElements()) {
                    ID dest = (ID) destPeerIDs.nextElement();
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Sending " + msg + " to client " + dest);
                    }
                    
                    EndpointAddress addr = mkAddress((PeerID) dest, PropSName, PropPName);
                    
                    Messenger messenger = rdvService.endpoint.getMessenger(addr);
                    
                    if (null != messenger) {
                        try {
                            messenger.sendMessage(msg);
                            numPeers++;
                        } catch (IOException failed) {
                            continue;
                        }
                    }
                }
            }
            finally {
                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.propagateToPeers(numPeers);
                }
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void propagateToNeighbors(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        ttl = Math.min(ttl, MAX_TTL);
        
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
     *  {@inheritDoc}
     **/
    public void walk(Message msg, String serviceName, String serviceParam, int ttl) throws IOException {
        
        propagate(msg, serviceName, serviceParam, ttl);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void walk(Vector destPeerIDs,
            Message msg,
            String serviceName,
            String serviceParam,
            int ttl)
        throws IOException {
        
        propagate(destPeerIDs.elements(), msg, serviceName, serviceParam, ttl);
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected void repropagate(Message msg, RendezVousPropagateMessage propHdr, String serviceName, String serviceParam) {
        
        try {
            propHdr = updatePropHeader(msg, propHdr, serviceName, serviceParam, MAX_TTL);
            
            if (null != propHdr) {
                sendToNetwork(msg, propHdr);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Declining to repropagate " + msg + " (" + propHdr.getMsgId() + ")");
                }
            }
        } catch (Exception ez1) {
            // Not much we can do
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not repropagate " + msg + " (" + propHdr.getMsgId() + ")", ez1);
            }
        }
    }
}

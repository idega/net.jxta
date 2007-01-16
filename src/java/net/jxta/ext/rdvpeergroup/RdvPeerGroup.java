/*
 * Copyright (c) 2003 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: RdvPeerGroup.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
 */

package net.jxta.ext.rdvpeergroup;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

import java.io.IOException;

import net.jxta.document.Advertisement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.LightWeightPeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.rendezvous.RendezvousEvent;

import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.rendezvous.rpv.PeerViewEvent;
import net.jxta.impl.rendezvous.rpv.PeerViewListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * RdvPeerGroup is an LightWeighttPeerGroup that uses all 
 * the parent PeerGroup's services except for the RendezVous
 * Service which is instantiaded by RdvPeerGroup.
 *
 * RdvPeerGroup allows a set of peers to run as
 * rdv peers (control peers)  within the RdvPeerGroup, other peers to
 * be edge peers (primary peers), and other peers to simply keep track
 * of running rdv peers (guest peers).
 **/

public class RdvPeerGroup extends LightWeightPeerGroup 
                          implements PeerViewListener, RendezvousListener {

	private static final Logger LOG = Logger.getLogger(RdvPeerGroup.class.getName());

    private RendezVousServiceImpl rdvService = null;
    private PeerGroup parentPeerGroup = null;
    private boolean isPrimaryPeer = false;
    private boolean isControlPeer = false;
    private boolean isGuestPeer = false;
    private boolean isPPGJoined = false;
    private PeerView rpv = null;
    private static String serviceName = "RdvPeerGroupRPV";
    private RendezvousListener listener = null;

    /**
     * Constructor
     *
     * All classes that extend RdvPeerGroup must invoke this
     * constructor.
     *
     * Note that this constructor does not join the PeerGroup.
     *
     * @param adv PeerGroupAdvertisement of the RdvPeerGroup
     * @param parentPeerGroup PeerGroup object of the parent
     * PeerGroup.
     **/
    public RdvPeerGroup (PeerGroupAdvertisement adv,
                         PeerGroup parentPeerGroup) {
        super (adv);
        this.parentPeerGroup = parentPeerGroup;
    }


    /**
     * Joins the RdvPeerGroup as a primary peer (edge peer).
     *
     * @param listener optional listener allowing the application
     * to be asynchronously notified of the state of the connection.
     * @return boolean true is returned when joining was authorized. False
     * otherwise.
     **/
    public boolean joinAsPrimaryPeer (RendezvousListener listener) throws IOException {

        if (isPrimaryPeer) {
            return true;
        }

        if (isControlPeer) {
            return false;
        }

        this.listener = listener;

        isPrimaryPeer = true;

        if (! isPPGJoined) {
            join (false);
        }
        return true;
    }

    /**
     * Joins the RdvPeerGroup as a control peer (rdv peer).
     *
     * @param listener optional listener allowing the application
     * to be asynchronously notified of the state of the connection.
     * @return boolean true is returned when joining was authorized. False
     * otherwise.
     **/
    public boolean joinAsControlPeer (RendezvousListener listener) throws IOException {

        if (isControlPeer) {
            return true;
        }

        if (isPrimaryPeer) {
            return false;
        }

        this.listener = listener;

        isControlPeer = true;

        if (! isPPGJoined) {
            join (true);
        }
        return true;
    }


    /**
     * Joins the RdvPeerGroup as a guest peer (PeerView only).
     *
     * @param listener optional listener allowing the application
     * to be asynchronously notified of the state of the connection.
     * @return boolean true is returned when joining was authorized. False
     * otherwise.
     **/
    public boolean joinAsGuestPeer (RendezvousListener listener) throws IOException {

        if (isGuestPeer) {
            return true;
        }

        if (isControlPeer) {
            return false;
        }

        if (isPrimaryPeer) {
            return false;
        }

        super.init (parentPeerGroup, null, null);

        this.listener = listener;

        startPeerView();

        isGuestPeer = true;

        return true;
    }


    /**
     * Leaves (quit) the RdvPeerGroup
     **/
    public void leave () {

        if (isPPGJoined) {
            // remove listener.
            getRendezVousService().removeListener (this);
            stopApp ();
            isPPGJoined = false;
        }
        stopPeerView();
    }

    /**
     * Get an Enumeration of the PeerIDs of the 
     * control peers of this RdvPeerGroup. Note that
     * the result may be null if the RdvPeerGroup is not
     * yet connected.
     *
     * @return Enumeration of PeerID of the control peers.
     **/
    public Enumeration getControlPeers () {

        Vector peers = null;

        peers = getRpvView ();
        if (peers.size() != 0) {
            return peers.elements();
        }

        if (isPPGJoined) {
            peers = getRendezVousService().getLocalWalkView();
        } else {
            peers = new Vector ();
        }
        return peers.elements();
    }

    private void join (boolean asControlPeer) throws IOException {

        if (isPPGJoined) {
            throw new IOException ("Already joined");
        }

        stopPeerView();

        init (parentPeerGroup, null, null);

        // start the PeerGroup 
        startApp (null);

        // Attach a listener.
        getRendezVousService().addListener (this);

        if (asControlPeer) {
            getRendezVousService().startRendezVous ();
        } else {
            getRendezVousService().stopRendezVous();
        }

        isPPGJoined = true;
    }

    private void startPeerView () {

        if (rpv != null) {
            return;
        }

        if (! isPPGJoined) {
            rpv = new PeerView (this,
                                parentPeerGroup,
                                null, 
                                serviceName);
        }
        rpv.addListener (this);
    }

    private void stopPeerView () {

        if (rpv != null) {
            rpv.removeListener (this);
            rpv.stop();
            rpv = null;
        }
    }

    private Vector getRpvView () {

        Vector tmp = new Vector();

        if (rpv == null) {
            return tmp;
        }

        Iterator peers = rpv.getView().iterator();

        while (peers.hasNext()) {
            try {

                PeerViewElement peer = (PeerViewElement) peers.next();
                RdvAdvertisement adv = peer.getRdvAdvertisement();
                tmp.add (adv);
            } catch (Exception ez) {
                // Should not happen
                if ( LOG.isEnabledFor(Level.DEBUG) ) {
                    LOG.debug("invalid RdvAdvertisement");
                }
                return tmp;
            }
        }
	    return tmp;
    }

    /**
     * Called when an event occurs for the Rendezvous service
     *
     * @param event the rendezvous event
     */
    public void rendezvousEvent( RendezvousEvent event ){

        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug("Got RendezVousService event type= " + event.getType());
        }
        
        generateEvent (event);
    }

    /**
     * peerViewEvent the peerview event
     * @param event   peer view event
     */
    public void peerViewEvent ( PeerViewEvent event ) {

        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug("Got PeerView event");
        }

        PeerViewElement pve = event.getPeerViewElement();

        if (pve == null) {
            // Should not happen
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Invalid PeerViewEvent: no PeerViewElement");
            }
            throw new RuntimeException ("Invalid PeerViewEvent !");
        }

        RdvAdvertisement radv = pve.getRdvAdvertisement();

        if (radv == null) {
            // Should not happen
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Invalid PeerViewEvent: no RdvAdvertisement");
            }
            throw new RuntimeException ("Invalid PeerViewEvent !");
        }

        ID pid = radv.getPeerID();

        if (pid == null) {
            // Should not happen
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Invalid PeerViewEvent: no rdv PeerID");
            }
            throw new RuntimeException ("Invalid PeerViewEvent !");
        }


        switch (event.getType()) {

            case PeerViewEvent.ADD: {
                generateEvent (RendezvousEvent.RDVCONNECT, pid);
                break;
            }

            case PeerViewEvent.FAIL: {
                generateEvent (RendezvousEvent.RDVDISCONNECT, pid);
                break;
            }
            case PeerViewEvent.REMOVE: {
                generateEvent (RendezvousEvent.RDVFAILED, pid);
                break;
            }
            default: {
                // Should not happen
                if ( LOG.isEnabledFor(Level.DEBUG) ) {
                    LOG.debug("PeerViewEvent invalid type = " + event.getType());
                }
                break;
            }
        }
    }

    private void generateEvent(int type, ID peer) {


        RendezvousEvent event = new RendezvousEvent(this, type, peer);
        
        generateEvent (event);
    }


    private void generateEvent (RendezvousEvent event) {

        RendezvousListener l = null;

        synchronized (this) {
            l = listener;
        }

        
        if (l == null) {
            // Nothing to do
            return;
        }

        try {
            l.rendezvousEvent(event);
        } catch (Throwable ignored) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Uncaught Throwable in listener : " + "(" + l.getClass().getName() + ")", ignored);
            }
        }
    }

    

    /*****************************************************************
     * LightWeightPeerGroup API
     *****************************************************************/

    /**
    *	{@inheritDoc}
    **/
    public void init(PeerGroup group,
                     ID assignedID,
                     Advertisement implAdv) {

        super.init (group, assignedID, implAdv);

        // Create a RendezVousService
        rdvService = new RendezVousServiceImpl ();


        // Initialialize the RendezVousService.
        rdvService.init (this,
                         PeerGroup.rendezvousClassID,
                         null);
    }


    /**
    *	{@inheritDoc}
    **/
    public int startApp(String[] args) {

        if (rdvService != null) {
            return rdvService.startApp (args);
        } else {
            return START_AGAIN_PROGRESS;
        }
    }
    
    /**
    *	{@inheritDoc}
    **/
   public void stopApp() {

        if (rdvService != null) {
            rdvService.stopApp();
        }
    }

    /**
    *	{@inheritDoc}
    **/
    public boolean isRendezvous() { 

        if (rdvService == null) {
            return false;
        } else {
            return rdvService.isRendezVous();
        }
    }
    
    
    public RendezVousService getRendezVousService() { 
        return rdvService;
    }    

    public void unref() { 
        return;
    }
}

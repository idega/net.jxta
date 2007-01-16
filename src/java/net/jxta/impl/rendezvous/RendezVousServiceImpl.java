/*
 *  $Id: RendezVousServiceImpl.java,v 1.1 2007/01/16 11:02:02 thomas Exp $
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
package net.jxta.impl.rendezvous;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.id.ID;
import net.jxta.meter.MonitorResources;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezVousStatus;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.service.Service;

import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import net.jxta.impl.meter.MonitorManager;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.adhoc.AdhocPeerRdvService;
import net.jxta.impl.rendezvous.edge.EdgePeerRdvService;
import net.jxta.impl.rendezvous.rdv.RdvPeerRdvService;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousServiceMonitor;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.TimerThreadNamer;


/**
 * A JXTA {@link net.jxta.rendezvous.RendezvousService} implementation which
 * implements the standard JXTA Rendezvous Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezvousService
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 */
public final class RendezVousServiceImpl implements RendezVousService {

    /**
     *  Log4J Logger
     */
    private final static transient Logger LOG = Logger.getLogger(RendezVousServiceImpl.class.getName());

    private final static long rdv_watchdog_interval_default = 5 * TimeUtils.AMINUTE; // 5 Minutes

    private static final double DEMOTION_FACTOR = 0.05;
    private static final long DEMOTION_MIN_PEERVIEW_COUNT = 5;
    private static final long DEMOTION_MIN_CLIENT_COUNT = 3;
    protected static final int MAX_MSGIDS = 1000;

    private final static Random random = new Random();

    private PeerGroup group = null;
    private ID assignedID = null;
    private ModuleImplAdvertisement implAdvertisement = null;

    private PeerGroup advGroup = null;
    public EndpointService endpoint = null;

    private RendezvousServiceMonitor rendezvousServiceMonitor;

    private final Timer timer = new Timer(true);
    private RdvWatchdogTask autoRdvTask = null;

    private long rdv_watchdog_interval = 5 * TimeUtils.AMINUTE; // 5 Minutes

    private final Set eventListeners = Collections.synchronizedSet(new HashSet());
    private final Map propListeners = new HashMap();

    /**
     *  The peer view for this peer group.
     */
    public volatile PeerView rpv = null;

    private final List msgIds = new ArrayList(MAX_MSGIDS);
    private int messagesReceived;

    private RdvConfigAdv.RendezVousConfiguration config = RdvConfigAdv.RendezVousConfiguration.EDGE;
    private boolean autoRendezvous = false;

    private String[] savedArgs = null;

    /**
     *  Object to lock on while changing rdv states.
     */
    private final Object rdvProviderSwitchLock = new String("Provider switch lock");

    /**
     *  If <code>true</code> then a rdv provider change is in progress.
     */
    private boolean rdvProviderSwitchStatus = false;

    private RendezVousServiceProvider provider = null;

    /**
     * Constructor for the RendezVousServiceImpl object
     */
    public RendezVousServiceImpl() {}

    public final static RouteAdvertisement extractRouteAdv(PeerAdvertisement adv) {

        try {
            // Get its EndpointService advertisement
            XMLElement endpParam = (XMLElement) adv.getServiceParam(PeerGroup.endpointClassID);

            if (endpParam == null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No Endpoint Params");
                }
                return null;
            }

            // get the Route Advertisement element
            Enumeration paramChilds = endpParam.getChildren(RouteAdvertisement.getAdvertisementType());
            XMLElement param;

            if (paramChilds.hasMoreElements()) {
                param = (XMLElement) paramChilds.nextElement();
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No Route Adv in Peer Adv");
                }
                return null;
            }

            // build the new route
            RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement((XMLElement) param);

            route.setDestPeerID(adv.getPeerID());

            return route;
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to extract radv", e);
            }
        }

        return null;
    }

    /**
     *  {@inheritDoc}
     */
    protected void finalize() throws Throwable {
        stopApp();
        super.finalize();
    }

    /**
     *  {@inheritDoc}
     */
    public Service getInterface() {
        return new RendezVousServiceInterface(this);
    }

    /**
     *  {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    public ID getAssignedID() {
        return assignedID;
    }

    /**
     *  {@inheritDoc}
     *
     *  <p/><b>Note</b>: it is permissible to pass null as the impl parameter
     *  when this instance is not being loaded via the module framework.
     */
    public synchronized void init(PeerGroup g, ID assignedID, Advertisement impl) {

        this.group = g;
        this.assignedID = assignedID;
        this.implAdvertisement = (ModuleImplAdvertisement) impl;

        timer.schedule(new TimerThreadNamer("RendezVousServiceImpl Timer for " + group.getPeerGroupID()), 0);

        advGroup = g.getParentGroup();

        if ((null == advGroup) || PeerGroupID.worldPeerGroupID.equals(advGroup.getPeerGroupID())) {
            // For historical reasons, we publish in our own group rather than
            // the parent if our parent is the world group.
            advGroup = group;
        }

        ConfigParams confAdv = (ConfigParams) g.getConfigAdvertisement();

        // Get the config. If we do not have a config, we're done; we just keep
        // the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            Advertisement adv = null;

            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(getAssignedID());

                if (null != configDoc) {
                    // XXX 20041027 backwards compatibility
                    configDoc.addAttribute( "type", RdvConfigAdv.getAdvertisementType() );

                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (NoSuchElementException failed) {
                ;
            }

            if (adv instanceof RdvConfigAdv) {
                RdvConfigAdv rdvConfigAdv = (RdvConfigAdv) adv;

                config = rdvConfigAdv.getConfiguration();

                autoRendezvous = rdvConfigAdv.getAutoRendezvousCheckInterval() > 0;

                rdv_watchdog_interval = rdvConfigAdv.getAutoRendezvousCheckInterval();
            }
        }

        if (PeerGroupID.worldPeerGroupID.equals(group.getPeerGroupID())) {
            config = RdvConfigAdv.RendezVousConfiguration.AD_HOC;
        }

        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring RendezVous Service : " + assignedID);

            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: " + implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : " + implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : " + implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : " + implAdvertisement.getCode());
            }

            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : " + group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : " + group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : " + group.getPeerID());

            configInfo.append("\n\tConfiguration :");
            if (null != advGroup) {
                configInfo.append("\n\t\tAdvertising group : " + advGroup.getPeerGroupName() + " [" + advGroup.getPeerGroupID() + "]");
            } else {
                configInfo.append("\n\t\tAdvertising group : (none)");
            }
            configInfo.append("\n\t\tRendezVous : " + config );
            configInfo.append("\n\t\tAuto RendezVous : " + autoRendezvous);
            configInfo.append("\n\t\tAuto-RendezVous Reconfig Interval : " + rdv_watchdog_interval);

            LOG.info(configInfo);
        }

        synchronized (rdvProviderSwitchLock) {
            rdvProviderSwitchStatus = true;
        }
    }

    /**
     *  {@inheritDoc}
     */
    public int startApp(String[] arg) {
        endpoint = group.getEndpointService();

        if (null == endpoint) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Stalled until there is an endpoint service");
            }

            return START_AGAIN_STALLED;
        }

        Service needed = group.getMembershipService();

        if (null == needed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Stalled until there is a membership service");
            }

            return START_AGAIN_STALLED;
        }

        // Create the PeerView instance
        if (RdvConfigAdv.RendezVousConfiguration.AD_HOC != config) {
            rpv = new PeerView(group, advGroup, this, getAssignedID().toString() + group.getPeerGroupID().getUniqueValue().toString());

            rpv.start();
        }

        synchronized (rdvProviderSwitchLock) {
            if (RdvConfigAdv.RendezVousConfiguration.AD_HOC == config) {
                provider = new AdhocPeerRdvService(group, this);
            } else if (RdvConfigAdv.RendezVousConfiguration.EDGE == config) {
                provider = new EdgePeerRdvService(group, this);
            } else if (RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS == config) {
                provider = new RdvPeerRdvService(group, this);
            } else {
                throw new IllegalStateException("Unrecognized rendezvous configuration");
            }

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING) {
                rendezvousServiceMonitor = (RendezvousServiceMonitor) MonitorManager.getServiceMonitor(group,
                                           MonitorResources.rendezvousServiceMonitorClassID);
                provider.setRendezvousServiceMonitor(rendezvousServiceMonitor);
            }

            provider.startApp(null);

            rdvProviderSwitchStatus = false;
        }

        if (autoRendezvous && !PeerGroupID.worldPeerGroupID.equals(group.getPeerGroupID())) {
            startWatchDogTimer();
        }

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Rendezvous Serivce started");
        }

        return 0;
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void stopApp() {

        synchronized (rdvProviderSwitchLock) {
            // We won't ever release this lock. We are shutting down. There is
            // no reason to switch after stopping is begun.
            rdvProviderSwitchStatus = true;

            if (provider != null) {
                provider.stopApp();
                provider = null;
            }
        }

        if (rpv != null) {
            rpv.stop();
            rpv = null;
        }

        Iterator eachListener = propListeners.keySet().iterator();

        while (eachListener.hasNext()) {
            String aListener = (String) eachListener.next();

            try {
                endpoint.removeIncomingMessageListener(aListener, null);
            } catch (Exception ignored) {
                ;
            }

            eachListener.remove();
        }

        propListeners.clear();

        timer.cancel();

        msgIds.clear();
        eventListeners.clear();

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Rendezvous Serivce stopped");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRendezVous() {

        RendezVousStatus currentStatus = getRendezVousStatus();

        return (RendezVousStatus.AUTO_RENDEZVOUS == currentStatus) || (RendezVousStatus.RENDEZVOUS == currentStatus);
    }

    /**
     * @inheritDoc
     */
    public RendezVousStatus getRendezVousStatus() {
        RendezVousServiceProvider currentProvider = provider;

        if (null == currentProvider) {
            return RendezVousStatus.NONE;
        } else {
            if (currentProvider instanceof AdhocPeerRdvService) {
                return RendezVousStatus.ADHOC;
            } else if (currentProvider instanceof EdgePeerRdvService) {
                return autoRendezvous ? RendezVousStatus.AUTO_EDGE : RendezVousStatus.EDGE;
            } else if (currentProvider instanceof RdvPeerRdvService) {
                return autoRendezvous ? RendezVousStatus.AUTO_RENDEZVOUS : RendezVousStatus.RENDEZVOUS;
            } else {
                return RendezVousStatus.UNKNOWN;
            }
        }
    }

    /**
     *  (@inheritDoc}
     */
    public boolean setAutoStart(boolean auto) {
        return setAutoStart(auto, rdv_watchdog_interval_default);
    }

    /**
     *  (@inheritDoc}
     */
    public synchronized boolean setAutoStart(boolean auto, long period) {
        rdv_watchdog_interval = period;
        boolean old = autoRendezvous;

        autoRendezvous = auto;

        if (auto && !old) {
            startWatchDogTimer();
        } else
            if (old && !auto) {
                stopWatchDogTimer();
            }
        return old;
    }

    /**
     * Force the peerview to use the given peer as a seed peer and force the edge rendezvous
     * provider (if we're edge) to chose a rendezvous as soon as there is one (likely but not
     * necessarily the one given).
     *
     * @param addr The addres of the seed peer (raw or peer id based)
     * @param radv An optional route advertisement, which may be null.
     * @throws IOException if it failed immediately.
     */
    private void connectToRendezVous(EndpointAddress addr, RouteAdvertisement routeHint) throws IOException {

        PeerView currView = rpv;
        if (null == currView) {
            throw new IOException("No PeerView");
        }

        // In order to mimic the past behaviour as closely as possible we add that peer to the seed list automatically and we
        // change the provider choice delay (edge peer only), so that it choses a rendezvous as soon as the suggested one is added
        // to the peerview. However, another seed rendezvous might beat it to the finish line (assuming there are other seeds).

        currView.addSeed(addr.toURI());
        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            currentProvider.setChoiceDelay(0);
        }

        if (! currView.probeAddress(addr, routeHint)) {
            throw new IOException("Could not probe:" + addr);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void connectToRendezVous(PeerAdvertisement adv) throws IOException {

        EndpointAddress addr = new EndpointAddress("jxta", adv.getPeerID().getUniqueValue().toString(), null, null);

        connectToRendezVous(addr, extractRouteAdv(adv));
    }

    /**
     *  {@inheritDoc}
     */
    public void connectToRendezVous(EndpointAddress addr) throws IOException {
        connectToRendezVous(addr, null);
    }

    /**
     *  {@inheritDoc}
     */
    public void challengeRendezVous(ID peer, long delay) {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            currentProvider.challengeRendezVous(peer, delay);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void disconnectFromRendezVous(ID peerId) {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            currentProvider.disconnectFromRendezVous(peerId);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public Enumeration getConnectedRendezVous() {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            return currentProvider.getConnectedRendezVous();
        }

        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     *  {@inheritDoc}
     */
    public Enumeration getDisconnectedRendezVous() {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            return currentProvider.getDisconnectedRendezVous();
        }

        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     *  {@inheritDoc}
     */
    public Enumeration getConnectedPeers() {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            return currentProvider.getConnectedPeers();
        }

        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     *  {@inheritDoc}
     */
    public Vector getConnectedPeerIDs() {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            return currentProvider.getConnectedPeerIDs();
        }

        return new Vector();
    }

    /**
     * Gets the rendezvousConnected attribute of the RendezVousServiceImpl object
     *
     * @return    true if connected to a rendezvous, false otherwise
     */
    public boolean isConnectedToRendezVous() {

        RendezVousServiceProvider currentProvider = provider;

        if (currentProvider != null) {
            return currentProvider.isConnectedToRendezVous();
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     */
    public void startRendezVous() {
        try {
            if (isRendezVous() || PeerGroupID.worldPeerGroupID.equals(group.getPeerGroupID())) {
                return;
            }

            synchronized (rdvProviderSwitchLock) {
                if (rdvProviderSwitchStatus) {
                    IOException failed = new IOException("Currently switching rendezvous configuration. try again later.");

                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Failed to start rendezvous", failed);
                    }
                    throw failed;
                }

                rdvProviderSwitchStatus = true;

                // We are at this moment an Edge Peer. First, the current implementation
                // must be stopped.
                if (provider != null) {
                    provider.stopApp();
                    provider = null;
                }

                config = RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS;

                // Now, a new instance of RdvPeerRdvService must be created and initialized.
                provider = new RdvPeerRdvService(group, this);

                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING) {
                    provider.setRendezvousServiceMonitor(rendezvousServiceMonitor);
                }

                provider.startApp(savedArgs);

                rdvProviderSwitchStatus = false;
            }
        } catch (IOException failure) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to start rendezvous", failure);
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void stopRendezVous() {

        if (!isRendezVous()) {
            return;
        }

        synchronized (rdvProviderSwitchLock) {
            if (rdvProviderSwitchStatus) {
                IOException failed = new IOException("Currently switching rendezvous configuration. try again later.");

                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed to stop rendezvous", failed);
                }
            }

            rdvProviderSwitchStatus = true;

            // If the service was already started, then it needs to be stopped,
            // and a new instance of an EdgePeerRdvService must be created and initialized and
            // started.

            if (provider != null) {
                provider.stopApp();
                provider = null;
            }

            config = RdvConfigAdv.RendezVousConfiguration.EDGE;

            provider = new EdgePeerRdvService(group, this);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING) {
                provider.setRendezvousServiceMonitor(rendezvousServiceMonitor);
            }

            provider.startApp(savedArgs);

            rdvProviderSwitchStatus = false;
        }
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized boolean addPropagateListener(String name, EndpointListener listener) {

        // FIXME: jice@jxta.org - 20040726 - The naming of PropagateListener is inconsistent with that of EndpointListener. It is
        // not a major issue but is ugly since messages are always addressed with the EndpointListener convention. The only way to
        // fix it is to deprecate addPropagateListener in favor of a two argument version and wait for applications to adapt. Only
        // once that transition is over, will we be able to know where the separator has to be.

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Adding listener (" + listener + ") for name= " + name);
        }

        // Take the optimistic stance. Since we're synchronized, replace the current one, and if we find there was one and it's
        // not the same, put things back as they were.

        EndpointListener current = (EndpointListener) propListeners.put(name, listener);
        if ((current != null) && (current != listener)) {
            propListeners.put(name, current);
            return false;
        }
        return true;
    }

    /**
     *  {@inheritDoc}
     */
    public boolean addPropagateListener(String serviceName, String serviceParam, EndpointListener listener) {
        // Until the old API is killed, the new API behaves like the old one (so that name
        // collisions are still detected if both APIs are in use).
        return addPropagateListener(serviceName + serviceParam, listener);
    }

    public synchronized EndpointListener getListener(String str) {
        return (EndpointListener) propListeners.get(str);
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized EndpointListener removePropagateListener(String name, EndpointListener listener) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Removing listener (" + listener + ") for name= " + name);
        }

        // Take the optimistic stance. Since we're synchronized, remove it, and if we find the invoker is cheating. Put it back.

        EndpointListener current = (EndpointListener) propListeners.remove(name);
        if ((current != null) && (current != listener)) {
            propListeners.put(name, current);
            return null;
        }

        return current;
    }

    /**
     *  {@inheritDoc}
     */
    public EndpointListener removePropagateListener(String serviceName, String serviceParam,
            EndpointListener listener) {

        // Until the old API is killed, the new API behaves like the old one (so that name
        // collisions are still detected if both APIs are in use).
        return removePropagateListener(serviceName + serviceParam, listener);
    }

    /**
     *  {@inheritDoc}
     */
    public void propagate(Message msg,
                          String serviceName,
                          String serviceParam,
                          int defaultTTL)
    throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.propagate(msg, serviceName, serviceParam, defaultTTL);
    }

    /**
     *  {@inheritDoc}
     */
    public void propagate(Enumeration destPeerIDs, Message msg, String serviceName, String serviceParam, int defaultTTL) throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.propagate(destPeerIDs, msg, serviceName, serviceParam, defaultTTL);
    }

    /**
     *  {@inheritDoc}
     */
    public void walk(Message msg,
                     String serviceName,
                     String serviceParam,
                     int defaultTTL)
    throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.walk(msg, serviceName, serviceParam, defaultTTL);
    }

    /**
     *  {@inheritDoc}
     */
    public void walk(Vector destPeerIDs,
                     Message msg,
                     String serviceName,
                     String serviceParam,
                     int defaultTTL)
    throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.walk(destPeerIDs, msg, serviceName, serviceParam, defaultTTL);
    }

    /**
     *  {@inheritDoc}
     */
    public Vector getLocalWalkView() {

        Vector tmp = new Vector();

        PeerView currView = rpv;
        if (null == currView) {
            return tmp;
        }

        Iterator eachPVE = Arrays.asList(currView.getView().toArray()).iterator();

        while (eachPVE.hasNext()) {
            PeerViewElement peer = (PeerViewElement) eachPVE.next();
            RdvAdvertisement adv = peer.getRdvAdvertisement();
            tmp.add(adv);
        }
        return tmp;
    }

    /**
     *  {@inheritDoc}
     */
    public void propagateToNeighbors(Message msg,
                                     String serviceName,
                                     String serviceParam,
                                     int ttl,
                                     String prunePeer) throws IOException {

        propagateToNeighbors(msg, serviceName, serviceParam, ttl);
    }

    /**
     *  {@inheritDoc}
     */
    public void propagateToNeighbors(Message msg,
                                     String serviceName,
                                     String serviceParam,
                                     int ttl) throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.propagateToNeighbors(msg, serviceName, serviceParam, ttl);
    }
    /**
     *  {@inheritDoc}
     */
    public void propagateInGroup(Message msg,
                                 String serviceName,
                                 String serviceParam,
                                 int ttl,
                                 String prunePeer) throws IOException {

        propagateInGroup(msg, serviceName, serviceParam, ttl);
    }

    /**
     *  {@inheritDoc}
     */
    public void propagateInGroup(Message msg,
                                 String serviceName,
                                 String serviceParam,
                                 int ttl) throws IOException {

        RendezVousServiceProvider currentProvider = provider;
        if (null == currentProvider) {
            throw new IOException("No RDV provider");
        }
        currentProvider.propagateInGroup(msg, serviceName, serviceParam, ttl);
    }

    /**
     *  {@inheritDoc}
     */
    public final void addListener(RendezvousListener listener) {

        eventListeners.add(listener);
    }

    /**
     *  {@inheritDoc}
     */
    public final boolean removeListener(RendezvousListener listener) {

        return eventListeners.remove(listener);
    }

    /**
     *  Creates a rendezvous event and sends it to all registered listeners.
     */
    public final void generateEvent(int type, ID regarding) {

        Iterator eachListener = Arrays.asList(eventListeners.toArray()).iterator();

        RendezvousEvent event = new RendezvousEvent(getInterface(), type, regarding);

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Calling listeners for " + event);
        }

        while (eachListener.hasNext()) {
            RendezvousListener aListener = (RendezvousListener) eachListener.next();

            try {
                aListener.rendezvousEvent(event);
            } catch (Throwable ignored) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Uncaught Throwable in listener (" + aListener + ")", ignored);
                }
            }
        }
    }

    private synchronized void startWatchDogTimer() {
        stopWatchDogTimer();

        autoRdvTask = new RdvWatchdogTask();

        // Now that we have an Auto-switch flag we only use the higher timeout
        // if auto-switch is off .
        // Set a watchdog, so the peer will become rendezvous if, after rdv_watchdog_interval it
        // still has not connected to any rendezvous peer.
        timer.schedule( autoRdvTask, rdv_watchdog_interval, rdv_watchdog_interval);
    }

    private synchronized void stopWatchDogTimer() {
        RdvWatchdogTask tw = autoRdvTask;

        if (tw != null) {
            autoRdvTask.cancel();
            autoRdvTask = null;
        }
    }

    /**
     * Edge Peer mode connection watchdog.
     */
    private class RdvWatchdogTask extends TimerTask {

        /**
         *  {@inheritDoc}
         */
        public synchronized void run() {
            try {
                if (!isRendezVous()) {
                    Enumeration rdvs = getConnectedRendezVous();

                    if (!rdvs.hasMoreElements()) {
                        // This peer has not been able to connect to any rendezvous peer.
                        // become one.

                        // become a rendezvous peer.
                        startRendezVous();
                    }
                } else {
                    // Perhaps we can demote ourselves back to an edge

                    int numberOfClients = getConnectedPeerIDs().size();
                    int peerViewSize = getLocalWalkView().size();

                    boolean isManyElementsInPeerView = (peerViewSize > DEMOTION_MIN_PEERVIEW_COUNT);
                    boolean isFewClients = (numberOfClients < DEMOTION_MIN_CLIENT_COUNT);

                    if (isManyElementsInPeerView) {
                        if (numberOfClients == 0) {
                            // Demote ourselves if there are no clients and
                            // there are more than the minimum rendezvous around
                            stopRendezVous();
                        } else if (isFewClients && (RendezVousServiceImpl.random.nextDouble() < DEMOTION_FACTOR)) {
                            // Randomly Demote ourselves if there are few clients and
                            // there are many rendezvous
                            stopRendezVous();
                        }
                    }
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable in Timer : " + Thread.currentThread().getName(), all);
                }
            }
        }
    }

    public boolean isMsgIdRecorded(UUID id) {

        boolean found;

        synchronized (msgIds) {
            found = msgIds.contains(id);
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug(id + " = " + found);
        }

        return found;
    }

    /**
     *  Checks if a message id has been recorded
     *
     *  @param id message to record.
     *  @result true if message was added otherwise (duplicate) false.
     */
    public boolean addMsgId(UUID id) {

        synchronized (msgIds) {
            if (isMsgIdRecorded(id)) {
                // Already there. Nothing to do
                return false;
            }

            if (msgIds.size() < MAX_MSGIDS) {
                msgIds.add(id);
            } else {
                msgIds.set((messagesReceived % MAX_MSGIDS), id);
            }

            messagesReceived++;
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Added Message ID : " + id);
        }

        return true;
    }

    public UUID createMsgId() {
        return UUIDFactory.newSeqUUID();
    }

    /**
     *  Get the current provider. This is for debugging purposes only.
     *
     *  @deprecated This is private for debugging and diagnostics only.
     */
    net.jxta.impl.rendezvous.RendezVousServiceProvider getRendezvousProvider() {
        return provider;
    }
}

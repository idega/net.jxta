/*
 * Copyright (c) 2002-2004 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and
 *    "Project JXTA" must not be used to endorse or promote products
 *    derived from this software without prior written permission.
 *    For written permission, please contact Project JXTA at
 *    http://www.jxta.org.
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
 * This license is based on the BSD license adopted by the Apache
 * Foundation.
 *
 * $Id: PeerView.java,v 1.1 2007/01/16 11:01:53 thomas Exp $
 */

package net.jxta.impl.rendezvous.rpv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

import net.jxta.impl.access.AccessList;
import net.jxta.impl.config.Config;
import net.jxta.impl.endpoint.relay.RelayClient;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.TimerThreadNamer;


/**
 * This class models a Rendezvous Peer View (RPV):
 * ordered collection of all other Rendezvous Peers visible to
 * this Peer.
 *
 * <p/>Presently this class implements a random "diffusion" algorithm
 * where each Peer periodically selects a randomly selected peer
 * advertisement from its view and sends it over to a randomly
 * selected peer from its view. Over time, this causes every peer to
 * learn about every other peer, resulting in a "consistent" peer
 * view.
 *
 * <p/>This diffusion process is bootstrapped by every peer sending their
 * own peer advertisements to some well-known, stable, "seed" peers on
 * startup.
 **/
public final class PeerView implements EndpointListener, RendezvousListener {
    
    /**
     *  Log4J Logger
     **/
    private static final transient Logger LOG = Logger.getLogger(PeerView.class.getName());
    
    /**
     *  Our service name
     **/
    static final String SERVICE_NAME = "PeerView";
    
    /**
     *  Namespace used for rdv message elements.
     **/
    static final String MESSAGE_NAMESPACE = "jxta";
    
    /**
     * Element name of outgoing messages. Note that the element contains a
     * RdvAvertisement and <emphasis>not</emphasis> a Peer Advertisement.
     **/
    static final String MESSAGE_ELEMENT_NAME = "PeerView.PeerAdv";
    
    /**
     * Element name of responses. Note that the element contains a
     * RdvAvertisement and <emphasis>not</emphasis> a Peer Advertisement.
     */
    static final String RESPONSE_ELEMENT_NAME = "PeerView.PeerAdv.Response";
    
    /**
     * Message element name for PeerView "Cached" Message Element
     */
    static final String CACHED_RADV_ELEMENT_NAME = "PeerView.Cached";
    
    /**
     * Optional message element that specifies by its presence in a peerview
     * message that the referenced peer is not the provider of the
     * RdvAdvertisement and the advertisement is a "hint" or referal from the
     * responding peer.
     *
     * In practice, when sending its own RdvAdvertisement, a peer does not
     * include this element, but when sending another peer's RdvAdvertisement,
     * this element is included.
     */
    static final MessageElement CACHED_RADV_ELEMENT = new StringMessageElement(CACHED_RADV_ELEMENT_NAME, Boolean.TRUE.toString(), null);
    
    /**
     * Message element name that specifies the route advertisement of the
     * source of the message.
     */
    static final String SRCROUTEADV_ELEMENT_NAME = "PeerView.SrcRouteAdv";
    
    /**
     * Message element name for PeerView "Edge" Message Element
     */
    static final String EDGE_ELEMENT_NAME = "PeerView.EdgePeer";
    
    /**
     * Optional message element that specifies by its presence in a peerview
     * message that the referenced peer is an edge peer and not a member of the
     * peerview.
     */
    static final MessageElement EDGE_ELEMENT = new StringMessageElement(EDGE_ELEMENT_NAME, Boolean.TRUE.toString(), null);
    
    /**
     * Message element name for PeerView "Failure" Message Element
     */
    static final String FAILURE_ELEMENT_NAME = "PeerView.Failure";
    
    /**
     * Optional message element that specifies by its presence in a peerview
     * message that the referenced peer has either failed or is quitting. If the
     * "cached" element is also set then the error is being reported by a third
     * party.
     */
    static final MessageElement FAILURE_ELEMENT = new StringMessageElement(FAILURE_ELEMENT_NAME, Boolean.TRUE.toString(), null);
    
    /**
     * This is the interval between adv exchange in seconds. This is
     * the main tunable runtime parameter for the diffusion
     * process. An interval that is too low will improve view
     * consistency at the expense of gratuitous network traffic. On
     * the other hand, an interval that is too high will cause the
     * view to become inconsistent. It is desirable to err on the side
     * of extra traffic.
     */
    private static final long DEFAULT_SEEDING_PERIOD = 5 * TimeUtils.ASECOND;
    
    private static final long WATCHDOG_PERIOD = 30 * TimeUtils.ASECOND;
    private static final long ACL_REFRESH_PERIOD = 30 * TimeUtils.AMINUTE;
    private static final long SEEDING_URI_REFRESH_PERIOD = 60 * TimeUtils.AMINUTE;
    private static final long WATCHDOG_GRACE_DELAY = 5 * TimeUtils.AMINUTE;
    
    private static final long DEFAULT_BOOTSTRAP_KICK_INTERVAL = 3 * TimeUtils.ASECOND;
    
    private static final int MIN_BOOTLEVEL = 0;
    private static final int BOOTLEVEL_INCREMENT = 1;
    private static final int MAX_EDGE_PEER_BOOTLEVEL = 8;
    private static final int MAX_RDV_PEER_BOOTLEVEL = 6;
    
    /**
     * Default lifetimes for RdvAdvertisement that are published.
     **/
    private static final long DEFAULT_RADV_LIFETIME = 1 * TimeUtils.ADAY;
    private static final long DEFAULT_RADV_EXPIRATION = 1 * TimeUtils.ANHOUR;
    
    /**
     * DEFAULT_SEEDING_RDVPEERS
     *
     * This value is the maximum number of rendezvous peers that will be
     * send our own advertisement at boot time.
     **/
    private static final int DEFAULT_SEEDING_RDVPEERS = 5;
    
    private final PeerGroup group;
    private final PeerGroup advertisingGroup;
    private final RendezVousServiceImpl rdvService;
    private final EndpointService endpoint;
    
    /**
     *  The name of this PeerView.
     *
     *  <p>FIXME 20040623 bondolo This should be a CodatID.
     **/
    private final String name;
    
    /**
     * Delay in relative milliseconds to apply before contacting seeding rdvs.
     * 0 is supposed to be reserved by RdvConfig to mean "use the default".
     * However, it is in fact a valid value and also the one we want for the default.
     * The only problem with that is that it is not possible to configure this value
     * explicitly, should it one day not be the default. The issue is actually in RdvConfig.
     **/
    private long seedingRdvConnDelay = 0;
    
    /**
     *  Whether relays should be probed for rendezvous advertisements.
     **/
    private boolean probeRelays = true;
    
    /**
     *  If the peerview is smaller than this we will try harder to find
     *  additional peerview members.
     **/
    private int minHappyPeerView = 4;
    
    /**
     * Whether we are restricted to using seed rdvs only.
     **/
    private boolean useOnlySeeds = false;
    
    /**
     * These are seed peers which were specified as part of the configuration
     * data or programmatically. These seeds are never deleted.
     *
     *  <ul>
     *      <li>Values are {@link java.net.URI}.</li>
     *  </ul>
     */
    private final Set permanentSeedHosts = Collections.synchronizedSet( new HashSet() );
    
    /**
     * The active list of seed peers. Formed by the union of the permanent
     * seed hosts and the lists of seed peers downloaded from the seeding
     * URIs.
     *
     *  <ul>
     *      <li>Values are {@link java.net.URI}.</li>
     *  </ul>
     */
    private final Set activeSeedHosts = Collections.synchronizedSet( new HashSet() );
    
    /**
     * These URIs specify location of seed peer lists. The URIs will be resolved
     * via URLConnection and are assumed to refer to plain text lists of URIs.
     *
     *  <ul>
     *      <li>Values are {@link java.net.URI}.</li>
     *  </ul>
     */
    private final Set seedingURIs = Collections.synchronizedSet( new HashSet() );
    
    private long nextSeedingURIrefreshTime = 0;
    
    /**
     * A single timer is used to periodically kick each PeerView
     * into activity. For the Random PeerView, this activity consists
     * of selecting a PeerViewElement at random from its view and
     * sending it across to a randomly-selected peer from its view.
     *
     * <p/>FIXME 20021121 lomax
     *
     * <p/>The original idea of using a common timer in order to save threads IS a
     * very good idea. However, limitations, and actually, bugs, in java.util.Timer
     * creates the following problems when using a static Timer:
     *
     *  <ul>
     *  <li>Memory Leak: Cancelling a TimerTask does not remove it from the
     *     execution queue of the Timer until the Timer is cancelled or the
     *     TimerTask is fired. Since most of the TimerTasks are inner classes
     *     this can mean that the PeerView is held around for a long time.</li>
     *
     *  <li>java.util.Timer is not only not real-time (which is more or less fine
     *     for the PeerView, but it sequentially invokes tasks (only one Thread
     *     per Timer). As a result, tasks that takes a long time to run delays
     *     other tasks.</li>
     *  </ul>
     *
     * <p/>The PeerView would function better with a better Timer, but JDK does
     * not provide a standard timer that would fullfill the needs of the
     * PeerView. Maybe we should implement a JXTA Timer, since lots of the JXTA
     * services, by being very asynchronous, rely on the same kind of timer
     * semantics as the PeerView. Until then, creating a Timer per instance of
     * the PeerView (i.e. per instance of joined PeerGroup) is the best
     * workaround.
     **/
    private final Timer timer;
    
    /**
     * A random number generator.
     */
    private final static Random random = new Random();
    
    /**
     * List of scheduled tasks
     **/
    private final Set scheduledTasks = Collections.synchronizedSet(new HashSet());
    
    /**
     *  Describes the frequency and amount of effort we will spend updating
     *  the peerview.
     **/
    private int bootLevel = MIN_BOOTLEVEL;
    
    /**
     *  Earliest absolute time in milliseconds at which we will allow a reseed
     *  to take place.
     **/
    private long earliestReseed = 0L;
    
    private final String uniqueGroupId;
    
    /**
     *  Listeners for PeerView Events.
     *
     *  <ul>
     *      <li>Values are {@link PeerViewListener}.
     *  </ul>
     **/
    private final Set rpvListeners = Collections.synchronizedSet(new HashSet());
    
    /**
     *  Used for querying for pves.
     **/
    private InputPipe wirePipeInputPipe = null;
    
    /**
     *  Used for querying for pves.
     **/
    private OutputPipe wirePipeOutputPipe = null;
    
    /**
     *  Used for notifications about pve failures.
     **/
    private InputPipe localGroupWirePipeInputPipe = null;
    
    /**
     *  Used for notifications about pve failures.
     **/
    private OutputPipe localGroupWirePipeOutputPipe = null;
    
    /**
     *  A task which monitors the up and down peers in the peerview.
     **/
    private WatchdogTask watchdogTask = null;
    
    /**
     * This is the accumulated view by an instance of this class.
     *
     * <p/>Values are {@see net.jxta.impl.rendezvous.rpv.PeerViewElement}
     */
    private final SortedSet localView = Collections.synchronizedSortedSet(new TreeSet());
    
    /**
     *  PVE for ourself.
     *
     *  FIXME bondolo 20041015 This should be part of the local view.
     **/
    private final PeerViewElement self;
    private PeerViewElement upPeer = null;
    private PeerViewElement downPeer = null;
    
    private final PeerViewStrategy replyStrategy;
    
    private final PeerViewStrategy kickRecipientStrategy;
    
    private final PeerViewStrategy kickAdvertisementStrategy;
    
    private final PeerViewStrategy refreshRecipientStrategy;
    
    protected final AccessList acl = new AccessList();
    protected  File aclFile;
    protected long aclFileLastModified = 0;
    protected long nextACLrefreshTime =0;
    
    // PeerAdv tracking.
    private PeerAdvertisement lastPeerAdv = null;
    private int lastModCount = -1;
    
    private final PipeAdvertisement localGroupWirePipeAdv;
    private final PipeAdvertisement advGroupPropPipeAdv;
    
    /**
     *  If <code>true</code> then this Peer View instance is closed and is
     *  shutting down.
     **/
    private volatile boolean closed = false;
    
    /**
     *  Get an instance of PeerView for the specified PeerGroup and Service.
     *
     *  @param group Peer Group in which this Peer View instance operates.
     *  @param advertisingGroup Peer Group in which this Peer View instance will
     *  advertise and broadcast its existance.
     *  @param rdvService The rdvService we are to use.
     *  @param name The identifying name for this Peer View instance.
     **/
    public PeerView(PeerGroup group, PeerGroup advertisingGroup, RendezVousServiceImpl rdvService, String name ) {
        
        this.group = group;
        this.advertisingGroup = advertisingGroup;
        this.rdvService = rdvService;
        this.name = name;
        
        this.endpoint = group.getEndpointService();
        aclFile = new File(Config.JXTA_HOME + "rendezvousACL.xml");
        aclFileLastModified = aclFile.lastModified();
        
        try {
            acl.init(aclFile);
            this.nextACLrefreshTime = System.currentTimeMillis() + ACL_REFRESH_PERIOD;
        } catch (IOException io) {
            acl.setGrantAll(true);
            this.nextACLrefreshTime = Long.MAX_VALUE;
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("PeerView Access Control permanently granting all permissions");
            }
        }
        
        this.uniqueGroupId = group.getPeerGroupID().getUniqueValue().toString();
        
        timer = new Timer(true);
        timer.schedule(new TimerThreadNamer("PeerView Timer for " + group.getPeerGroupID()), 0);
        
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
                
                permanentSeedHosts.addAll(Arrays.asList(rdvConfigAdv.getSeedRendezvous()));
                
                useOnlySeeds = rdvConfigAdv.getUseOnlySeeds();
                
                seedingURIs.addAll(Arrays.asList(rdvConfigAdv.getSeedingURIs()));
                
                if (rdvConfigAdv.getSeedRendezvousConnectDelay() > 0) {
                    seedingRdvConnDelay = rdvConfigAdv.getSeedRendezvousConnectDelay();
                }
                
                probeRelays = rdvConfigAdv.getProbeRelays();
                
                if (rdvConfigAdv.getMinHappyPeerView() > 0) {
                    minHappyPeerView = rdvConfigAdv.getMinHappyPeerView();
                }
            }
        }
        
        lastPeerAdv = group.getPeerAdvertisement();
        lastModCount = lastPeerAdv.getModCount();
        
        // create a new local RdvAdvertisement and set it to self.
        RdvAdvertisement radv = createRdvAdvertisement(lastPeerAdv, name);
        
        self = new PeerViewElement(endpoint, radv);
        
        // if ( rdvService.isRendezVous() ) {
        // addPeerViewElement( self );
        // }
        
        // setup endpoint listener
        endpoint.addIncomingMessageListener(this, SERVICE_NAME, uniqueGroupId);
        
        // add rendezvous listener
        rdvService.addListener(this);
        
        // initialize strategies
        replyStrategy = new PeerViewRandomWithReplaceStrategy(localView);
        
        kickRecipientStrategy = new PeerViewRandomStrategy(localView);
        
        kickAdvertisementStrategy = new PeerViewRandomWithReplaceStrategy(localView);
        
        refreshRecipientStrategy = new PeerViewSequentialStrategy(localView);
        
        localGroupWirePipeAdv = makeWirePipeAdvertisement(group);
        
        if (null != advertisingGroup) {
            advGroupPropPipeAdv = makeWirePipeAdvertisement(advertisingGroup);
        } else {
            advGroupPropPipeAdv = null;
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("PeerView created for group \"" + group.getPeerGroupName() + "\" [" + group.getPeerGroupID() + "] name \"" + name + "\"");
        }
    }
    
    /**
     * {@inheritDoc}
     *
     *  Listener for "PeerView"/<peergroup-unique-id> and propagate pipes.
     **/
    public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        
        // IsEdgePeer is confusing because the is* predicates are used
        // to refer to the properties of the message we're processing.
        boolean localIsEdge = !rdvService.isRendezVous();
        
        // check what kind of message this is (response or not).
        boolean isResponse = false;
        MessageElement me = msg.getMessageElement(MESSAGE_NAMESPACE, MESSAGE_ELEMENT_NAME);
        
        if (me == null) {
            me = msg.getMessageElement(MESSAGE_NAMESPACE, RESPONSE_ELEMENT_NAME);
            if (me == null) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Discarding damaged " + msg + ".");
                }
                return;
            } else {
                isResponse = true;
            }
        }
        
        Advertisement adv;
        
        try {
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(me);
            adv = AdvertisementFactory.newAdvertisement(asDoc);
        } catch (RuntimeException failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed building rdv advertisement from message element", failed);
            }
            return;
        } catch (IOException failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed building rdv advertisement from message element", failed);
            }
            return;
        }
        
        if (!(adv instanceof RdvAdvertisement)) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Response does not contain radv (" + adv.getAdvertisementType() + ")");
            }
            return;
        }
        
        RdvAdvertisement radv = (RdvAdvertisement) adv;
        // See if we can find a src route adv in the message.s
        me = msg.getMessageElement(MESSAGE_NAMESPACE, SRCROUTEADV_ELEMENT_NAME);
        if (me != null) {
            try {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(me);
                Advertisement routeAdv = AdvertisementFactory.newAdvertisement(asDoc);
                
                if (!(routeAdv instanceof RouteAdvertisement)) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Advertisement is not a RouteAdvertisement");
                    }
                } else {
                    RouteAdvertisement rdvRouteAdv = (RouteAdvertisement) radv.getRouteAdv().clone();
                    
                    // XXX we stich them together even if in the end it gets optimized away
                    RouteAdvertisement.stichRoute(rdvRouteAdv, (RouteAdvertisement) routeAdv);
                    radv.setRouteAdv(rdvRouteAdv);
                }
            } catch (RuntimeException failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed building route adv from message element", failed);
                }
            } catch (IOException failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed building route adv from message element", failed);
                }
            }
        }
        me = null;
        
        // Is this a message about ourself?
        if (group.getPeerID().equals(radv.getPeerID())) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Received a PeerView message about self. Discard.");
            }
            
            return;
        }
        
        // Collect the various flags.
        
        boolean isFailure = (msg.getMessageElement(MESSAGE_NAMESPACE, FAILURE_ELEMENT_NAME) != null);
        boolean isCached = (msg.getMessageElement(MESSAGE_NAMESPACE, CACHED_RADV_ELEMENT_NAME) != null);
        boolean isFromEdge = (msg.getMessageElement(MESSAGE_NAMESPACE, EDGE_ELEMENT_NAME) != null);
        boolean isTrusted = acl.isAllowed(radv.getPeerID());
        if (!localIsEdge && !isFromEdge && !isTrusted) {
            if (LOG.isEnabledFor(Level.WARN)) {
                 LOG.warn("Rejecting peerview entry for " + radv.getPeerID());
            }
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            String srcPeer = srcAddr.toString();
            
            if( "jxta".equals(srcAddr.getProtocolName())) {
                try {
                    String idstr = ID.URIEncodingName + ":" + ID.URNNamespace + ":" + srcAddr.getProtocolAddress();
                    
                    ID asID = IDFactory.fromURI( new URI(idstr) );
                    
                    PeerViewElement pve = getPeerViewElement( asID );
                    if( null != pve ) {
                        srcPeer = "\"" + pve.getRdvAdvertisement().getName() + "\"";
                    }
                } catch( URISyntaxException failed ) {
                }
            }
            
            LOG.debug(
                    "[" + group.getPeerGroupID() + "] Received a" + (isCached ? " cached" : "") + (isResponse ? " response" : "")
                    + (isFailure ? " failure" : "") + " message" + (isFromEdge ? " from edge" : "") + " regarding \"" + radv.getName() + "\" from "
                    + srcPeer);
        }
        
        // if this is a notification failure. All we have to do is locally
        // process the failure
        if (isFailure) {
            notifyFailure(radv.getPeerID(), false);
            return;
        }
        
        if (!isFromEdge && !isCached && isTrusted) {
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (discovery != null) {
                try {
                    discovery.publish(radv, DEFAULT_RADV_LIFETIME, DEFAULT_RADV_EXPIRATION);
                } catch (IOException ex) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not publish " + radv.getName(), ex);
                    }
                }
            }
        }
        
        // Figure out if we know that peer already. If we do, reuse the pve
        // that we have.
        boolean isNewbie = false;
        boolean added = false;
        PeerViewElement pve;
        int viewSize = 0;
        
        synchronized (localView) {
            PeerViewElement newbie = new PeerViewElement(endpoint, radv);
            pve = getPeerViewElement(newbie);
            
            if (null == pve) {
                pve = newbie;
                isNewbie = true;
            }
            
            if (!isFromEdge && !isCached && isTrusted) {
                if (!useOnlySeeds || isSeedRdv(radv)) {
                    if (isNewbie) {
                        added = addPeerViewElement(pve);
                    } else {
                        pve.setRdvAdvertisement(radv);
                    }
                }
            }
            
            viewSize = localView.size();
        }
        
        if( !isNewbie && isFromEdge ) {
            // The message stated that it is from an edge we believed was a peerview member.
            // Best thing to do is tell everyone that it's no longer in peerview.
            notifyFailure(pve, true);
            // we continue processing because it's not the other peer's fault we had the wrong idea.
        }
        
        // Do the rest of the add related tasks out of synch.
        // We must not nest any possibly synchronized ops in
        // the LocalView lock; it's the lowest level.
        
        if (added) {
            // Notify local listeners
            generateEvent(PeerViewEvent.ADD, pve);
        }
        
        /*
         * Now, see what if any message we have to send as a result.
         * There are four kind of messages we can send:
         *
         * - A response with ourselves, if we're being probed and we're
         * a rdv.
         *
         * - A probe to the peer whose adv we received, because we want
         * confirmation that it's alive.
         *
         * - A response with a random adv from our cache if we're being probed
         *
         * We may send more than one message.
         */
        
        // Type 1: respond with self.
        // We need to do that whenever we're being probed and we're an rdv,
        // and the adv we got is that of the sender (!cached - otherwise we
        // can't respond to the sender, it's a kick message that tells us about
        // another peer, which is handled by Type 2 below).
        // This could happen along with Type 2 below.
        if (!isCached && !localIsEdge && !isResponse) {
            boolean status = send(pve, self, true, false);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Type 1 (Respond with self) : Sent to " + pve + " result=" + status);
            }
        }
        
        // Type 2: probe it.
        // We need to probe if we do not have it in our view and it is
        // a cached adv (from a third party, so non-authoritative) which
        // can be found either in a response or in a kick message.
        // OR, if it is a probe from a peer that we do not know (in which
        // case we will probe here if it pretends to be an rdv, and also
        // respond (see Type 1, above) - only if it is NOT a response).
        // If isNewbie && added, isCached cannot be true, so we do not
        // need to check for added; (isCached && isNewbie) is enough.
        // If isNewbie && added, response cannot be false, so there is
        // no need to check for added; (isNewbie && ! reponse) is enough.
        // Whatchout: do not always probe cached things we got in a response
        // because we'd likely get another response with another cached
        // adv, thus cascading through all rdvs.
        // What we do is to use the information only if our view is way small.
        // in order to garantee connectivity for the future.
        // If useOnlySeeds, we're not allowed to use other than our
        // seed rdvs, so do not probe anything we learn from 3rd party. 
        if (!useOnlySeeds && isNewbie ) {                
            if ( (isCached && !isResponse) || (!isCached && !isResponse && !isFromEdge) ) {            
                boolean status = send(pve, self, false, false);
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Type 2 (Probe PVE) : Probed " + pve + " result=" + status);
                }
            }
        }
        
        // Type 3: respond with random cached adv because being probed (likely
        // by an edge, but we don't care, even another rdv could benefit from it).
        // This could happen along with Type 2 above although it is rather
        // unlikely.
        // Respond with a strategized adv from our view.
        //
        // This always happens along with Type 1 and sometimes along with
        // Type 2 in the same time: we could send three messages. That would
        // be if we receive a probe from another rdv that we we do not know
        // yet and we're an rdv ourselves. So, we'll respond with ourselves,
        // we will probe the sender since it pretends to be an rdv, and we
        // also will send a chosen rdv from our view and send it (done here).
        // Note, it could mean a cascade of probes to all rdvs: the recipient
        // of our cached adv would then probe it, thus receiving another
        // cached adv, which it would probe, etc.
        // This phenomenon is prevented in Type 2 by probing cached peers
        // such responses only if the view is small enough.
        // NOTE: rdv peers do not need this all that much and we could
        // avoid it for them, but it should not cause much problems so we
        // might as well leave it for  now.
        
        if (!isCached && !isResponse) {
            // Someone is looking for rdvs. try to help.
            
            PeerViewElement sendpve = replyStrategy.next();
            
            if ((sendpve != null) && !pve.equals(sendpve) && !self.equals(sendpve)) {
                boolean status = send(pve, sendpve, true, false);
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Type 3 (Respond with random pve) : Sent " + sendpve + " to " + pve + " result=" + status);
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public void rendezvousEvent(RendezvousEvent event) {
        
        if (closed) {
            return;
        }
        
        boolean notifyFailure = false;
        
        synchronized (this) {
            
            int theEventType = event.getType();
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("[" + group.getPeerGroupName() + "] Processing  " + event);
            }
            
            refreshSelf();
            
            if ((RendezvousEvent.BECAMERDV == theEventType) || (RendezvousEvent.BECAMEEDGE == theEventType)) {
                // kill any existing watchdog task
                if (null != watchdogTask) {
                    removeTask(watchdogTask);
                    watchdogTask.cancel();
                    watchdogTask = null;
                }
            }
            
            switch (theEventType) {
                case RendezvousEvent.RDVCONNECT:
                case RendezvousEvent.RDVRECONNECT:
                case RendezvousEvent.CLIENTCONNECT:
                case RendezvousEvent.CLIENTRECONNECT:
                case RendezvousEvent.RDVFAILED:
                case RendezvousEvent.RDVDISCONNECT:
                case RendezvousEvent.CLIENTFAILED:
                case RendezvousEvent.CLIENTDISCONNECT:
                    break;
                    
                case RendezvousEvent.BECAMERDV:
                    openWirePipes();
                    watchdogTask = new WatchdogTask();
                    addTask(watchdogTask, WATCHDOG_PERIOD, WATCHDOG_PERIOD);
                    rescheduleKick(true);
                    break;
                    
                case RendezvousEvent.BECAMEEDGE:
                    openWirePipes();
                    if (!localView.isEmpty()) {
                        // FIXME bondolo 20040229 since we likely don't have a
                        // rendezvous connection, it is kind of silly to be sending
                        // this now. probably should wait until we get a rendezvous
                        // connection.
                        notifyFailure = true;
                    }
                    rescheduleKick(true);
                    break;
                    
                default:
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("[" + group.getPeerGroupName() + "] Unexpected RDV event : " + event);
                    }
                    break;
            }
        }
        
        // we can't do the notification under synchronization.
        if (notifyFailure) {
            notifyFailure(self, true);
        }
    }
    
    public void start() {
        // do nothing for now... all the good stuff happens as a result of
        // rendezvous events.
    }
    
    public void stop() {
        
        synchronized (this) {
            // Only one thread gets to perform the shutdown.
            if (closed) {
                return;
            }
            closed = true;
        }
        
        // notify other rendezvous peers that we are going down (only
        // if this peer is a rendezvous)
        if (rdvService.isRendezVous()) {
            notifyFailure(self, true);
        }
        
        // From now on we can nullify everything we want. Other threads check
        // the closed flag where it matters.
        synchronized (this) {
            if (watchdogTask != null) {
                removeTask(watchdogTask);
                watchdogTask.cancel();
                watchdogTask = null;
            }
            
            // Remove message listener.
            endpoint.removeIncomingMessageListener(SERVICE_NAME, uniqueGroupId);
            
            // Remove rendezvous listener.
            rdvService.removeListener(this);
            
            // Remove all our pending scheduled tasks
            // Carefull with the indices while removing: do it backwards, it's
            // cheaper and simpler.
            
            synchronized (scheduledTasks) {
                Iterator eachTask = scheduledTasks.iterator();
                
                while (eachTask.hasNext()) {
                    try {
                        TimerTask task = (TimerTask) eachTask.next();
                        
                        task.cancel();
                        eachTask.remove();
                    } catch (Exception ez1) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Cannot cancel task: ", ez1);
                        }
                        continue;
                    }
                }
            }
            
            // Make sure that we close our WirePipes
            closeWirePipes();
            
            // Let go of the up and down peers.
            downPeer = null;
            upPeer = null;
            localView.clear();
            
            timer.cancel();
            
            rpvListeners.clear();
        }
    }
    
    protected void addTask(TimerTask task, long delay, long interval) {
        
        synchronized (scheduledTasks) {
            if (scheduledTasks.contains(task)) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Task list already contains specified task.");
                }
            }
            
            scheduledTasks.add(task);
        }
        
        if (interval >= 1) {
            timer.schedule(task, delay, interval);
        } else {
            timer.schedule(task, delay);
        }
    }
    
    protected void removeTask(TimerTask task) {
        scheduledTasks.remove(task);
    }
    
    /**
     * Adds the specified URI to the list of seeds. Even if useOnlySeeds is in
     * effect, this seed may now be used, as if it was part of the initial
     * configuration.
     *
     * @param seed the URI of the seed rendezvous.
     **/
    public void addSeed(URI seed) {
        permanentSeedHosts.add(seed);
    }
    
    /**
     * Probe the specified peer immediately.
     *
     * <p/> Note: If "useOnlySeeds" is in effect and the peer is not a seed, any response to this probe will be ignored.
     **/
    public boolean probeAddress(EndpointAddress address, Object hint) {
        
        PeerViewElement holdIt = null;
        
        synchronized(localView) {
            holdIt = self;
        }
        
        return send(address, hint, holdIt, false, false);
    }
    
    /**
     * Send our own advertisement to all of the seed rendezvous.
     */
    public void seed() {
        long reseedRemaining = earliestReseed - TimeUtils.timeNow();
        
        if (reseedRemaining > 0) {
            // Too early; the previous round is not even done.
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Still Seeding for " + reseedRemaining + "ms.");
            }
            return;
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("New Seeding...");
        }
        
        // Schedule sending propagated query to our local network neighbors.
        timedSend(self, (EndpointAddress) null, DEFAULT_SEEDING_PERIOD * 2);
        
        // We start trying configured seed peers only after some time, so we
        // make sure that the topology will not tend to be centralized.
        if( (TimeUtils.timeNow() > nextSeedingURIrefreshTime) && (localView.size() < minHappyPeerView) ) {
            nextSeedingURIrefreshTime = TimeUtils.toAbsoluteTimeMillis(SEEDING_URI_REFRESH_PERIOD);
            List seedRdvs = new ArrayList(permanentSeedHosts);
            
            if (!seedingURIs.isEmpty() ) {
                boolean allLoadsFailed = true;
                
                Iterator allSeedingURIs = seedingURIs.iterator();
                while(allSeedingURIs.hasNext()) {
                    URI aURI = (URI) allSeedingURIs.next();
                    try {
                        seedRdvs.addAll(Arrays.asList(loadSeeds(aURI)));
                        allLoadsFailed = false;
                    } catch( IOException failed ) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Failed loading seeding list from : " + aURI );
                        }
                    }
                }
               
                if( allLoadsFailed ) {
                    // Allow for an early reload if we couldn't contact any of
                    // the seeding URIS.
                    nextSeedingURIrefreshTime = TimeUtils.toAbsoluteTimeMillis(SEEDING_URI_REFRESH_PERIOD / 5);
                }
            }
            
            synchronized( activeSeedHosts ) {
                activeSeedHosts.clear();
                Collections.shuffle(seedRdvs);
                activeSeedHosts.addAll( seedRdvs );
            }
        }
        
        long iterations = 0;
        
        if(localView.size() < minHappyPeerView) {
            // We only do these things if we don't have a "happy" Peer View.
            // If the Peer View is already "happy" then we will use only
            // Peer View referrals for learning of new entires.
                      
            List seedRdvs = new ArrayList(activeSeedHosts);
            
            while ( !seedRdvs.isEmpty() ) {
                if (sendRandomByAddr(seedRdvs, DEFAULT_SEEDING_RDVPEERS, seedingRdvConnDelay + DEFAULT_SEEDING_PERIOD * iterations)) {
                    ++iterations;
                }
            }
            
            if ( !useOnlySeeds ) {
                // If use only seeds, we're not allowed to put in the peerview
                // anything but our seed rdvs. So, we've done everything that
                // was required.
                
                // Schedule sending propagated query to our advertising group
                if (advertisingGroup != null) {
                    // send it, but not immediately.
                    scheduleAdvertisingGroupQuery(DEFAULT_SEEDING_PERIOD * 2);
                }
                
                // send own advertisement to a random set of rendezvous
                List rdvs = discoverRdvAdverisements();
                
                Collections.shuffle(rdvs);
                
                while ( !rdvs.isEmpty() ) {
                    if (sendRandomByAdv(rdvs, DEFAULT_SEEDING_RDVPEERS, DEFAULT_SEEDING_PERIOD * iterations)) {
                        ++iterations;
                    }
                }
                
                if (probeRelays) {
                    List relays = getRelayPeers();
                    
                    Collections.shuffle(relays);
                    
                    while ( !relays.isEmpty() ) {
                        if (sendRandomByAddr(relays, DEFAULT_SEEDING_RDVPEERS, DEFAULT_SEEDING_PERIOD * iterations)) {
                            ++iterations;
                        }
                    }
                }
            }
        }
        
        earliestReseed = TimeUtils.toAbsoluteTimeMillis(seedingRdvConnDelay + (DEFAULT_SEEDING_PERIOD * iterations));
    }
    
    /**
     * Evaluates if the given pve corresponds to one of our seed rdvs. This is
     * to support the useOnlySeeds flag. The test is not completely foolproof
     * since our list of seed rdvs is just transport addresses. We could be
     * given a pve that exhibits an address that corresponds to one of our seeds
     * but is fake. And we might later succeed in connecting to that rdv via one
     * the other, real addresses. As a result, useOnlySeeds is *not* a security
     * feature, just a convenience for certain kind of deployments. Seed
     * rdvs should include certificates for such a restriction to be a security
     * feature.
     */
    private boolean isSeedRdv(RdvAdvertisement rdvAdv) {
        
        RouteAdvertisement radv = rdvAdv.getRouteAdv();
        
        if (radv == null) {
            return false;
        }
        
        AccessPointAdvertisement apAdv = radv.getDest();
        
        if (apAdv == null) {
            return false;
        }
        
        // The accessPointAdv returns a live (!) copy of the endpoint addresses.
        List addrList = new ArrayList(apAdv.getVectorEndpointAddresses());
        
        if (addrList.isEmpty()) {
            return false;
        }
        
        ListIterator eachAddr = addrList.listIterator();
        
        // convert each string to a URI
        while (eachAddr.hasNext()) {
            String anAddr = (String) eachAddr.next();
            
            try {
                // Convert to URI to compare with seedHosts
                eachAddr.set( new URI(anAddr));
            } catch (URISyntaxException badURI) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Skipping bad URI : " + anAddr, badURI);
                }
            }
        }
        
        // seedList must be treated as read-only.
        // We do what we want with addrVect
        
        addrList.retainAll(activeSeedHosts);
        
        // What's left is the intersection of seedHosts and the set of
        // endpoint addresses in the given pve. If it is non-empty, then we
        // accept the pve as that of a seed host.
        return (!addrList.isEmpty());
    }
    
    /**
     * Make sure that the PeerView properly changes behavior, when switching
     * from edge mode to rdv mode, and vice-versa.
     * Since openWirePipes() requires some other services such as the Pipe
     * Service, and since updateStatus is invoked this work must happen in
     * background, giving a chance to other services to be started.
     **/
    private class OpenPipesTask extends TimerTask {
        
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (closed) {
                    return;
                }
                
                openWirePipes();
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread: " + Thread.currentThread().getName(), all);
                }
            } finally {
                removeTask(this);
            }
        }
    }
    
    private void scheduleOpenPipes(long delay) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Scheduling open pipes attempt in " + delay + "ms.");
        }
        
        addTask(new OpenPipesTask(), delay, -1);
    }
    
    /**
     * Send a PeerView Message to the specified peer.
     *
     * @param response indicates whether this is a response. Otherwise
     * we may create a distributed loop where peers keep perpetually
     * responding to each-other.
     * @param failure Construct the message as a failure notification.
     **/
    private boolean send(PeerViewElement dest, PeerViewElement pve, boolean response, boolean failure) {
        
        Message msg = makeMessage(pve, response, failure);
        
        boolean result = dest.sendMessage(msg, SERVICE_NAME, uniqueGroupId);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending " + msg + " to " + dest + " success = " + result);
        }
        
        return result;
    }
    
    /**
     * Send a PeerView Message to the specified peer.
     *
     * @param response indicates whether this is a response. Otherwise
     * we may create a distributed loop where peers keep perpetually
     * responding to each-other.
     * @param failure Construct the message as a failure notification.
     **/
    private boolean send(EndpointAddress dest, Object hint,
            PeerViewElement pve, boolean response, boolean failure) {
        
        Message msg = makeMessage(pve, response, failure);
        
        if (null != dest) {
            EndpointAddress realAddr = new EndpointAddress(dest, SERVICE_NAME, uniqueGroupId);
            
            Messenger messenger = rdvService.endpoint.getMessengerImmediate(realAddr, hint);
            
            if (null != messenger) {
                try {
                    boolean result = messenger.sendMessage(msg);
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Sending " + msg + " to " + dest + " success = " + result);
                    }
                    
                    return result;
                } catch (IOException failed) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not send " + msg + " to " + dest, failed);
                    }
                    return false;
                }
            } else {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not get messenger for " + dest);
                }
                
                return false;
            }
        } else {
            // Else, propagate the message.
            try {
                endpoint.propagate(msg, SERVICE_NAME, uniqueGroupId);
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sent " + msg + " via propagate");
                }
                return true;
            } catch (IOException ez) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    // Pretty strange. This has little basis for failure...
                    LOG.warn("Could not propagate " + msg, ez);
                }
                return false;
            }
        }
    }
    
    /**
     * Send a PeerView Message to the specified peer.
     *
     * @param response indicates whether this is a response. Otherwise
     * we may create a distributed loop where peers keep perpetually
     * responding to each-other.
     * @param failure Construct the message as a failure notification.
     **/
    private boolean send(OutputPipe dest, PeerViewElement pve, boolean response, boolean failure) {
        
        Message msg = makeMessage(pve, response, failure);
        
        try {
            return dest.send(msg);
        } catch (IOException ez) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not send " + msg, ez);
            }
            return false;
        }
    }
    
    /**
     *  Make a PeerView Message
     **/
    private Message makeMessage(PeerViewElement content, boolean response, boolean failure) {
        
        Message msg = new Message();
        
        // edge peers add an identifying element, RDV peers do not
        if (!rdvService.isRendezVous()) {
            msg.addMessageElement(MESSAGE_NAMESPACE, EDGE_ELEMENT);
        }
        
        if (failure) {
            // This is a failure notification.
            msg.addMessageElement(MESSAGE_NAMESPACE, FAILURE_ELEMENT);
        }
        
        refreshSelf();
        
        RdvAdvertisement radv = content.getRdvAdvertisement();
        
        XMLDocument doc = (XMLDocument) radv.getDocument(MimeMediaType.XMLUTF8);
        String msgName = response ? RESPONSE_ELEMENT_NAME : MESSAGE_ELEMENT_NAME;
        
        MessageElement msge = new TextDocumentMessageElement(msgName, doc, null);
        
        msg.addMessageElement(MESSAGE_NAMESPACE, msge);
        
        if (!content.equals(self)) {
            // This is a cached RdvAdvertisement
            msg.addMessageElement(MESSAGE_NAMESPACE, CACHED_RADV_ELEMENT);
            
            // This message contains an RdvAdvertisement which is not ourself. In that
            // case, it is wise to also send the local route advertisement (as the optional
            // SrcRdvAdv) so the destination might have a better change to access the "content"
            // RendezvousAdv (this peer will then act as a hop).
            
            RouteAdvertisement localra = RendezVousServiceImpl.extractRouteAdv(lastPeerAdv);
            
            if (localra != null) {
                try {
                    XMLDocument radoc = (XMLDocument) localra.getDocument(MimeMediaType.XMLUTF8);
                    
                    msge = new TextDocumentMessageElement(SRCROUTEADV_ELEMENT_NAME, radoc, null);
                    msg.addMessageElement(MESSAGE_NAMESPACE, msge);
                } catch (Exception ez1) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not create optional src route adv for " + content, ez1);
                    }
                }
            }
        }
        
        return msg;
    }
    
    /**
     * Invoked by anyone in order to inform the PeerView of a failure
     * of one of the member peers.
     *
     *  @param pid ID of the peer which failed.
     *  @param propagateFailure If <tt>true</tt>then broadcast the failure to 
     *  other peers otherwise only update the local peerview.
     **/
    public void notifyFailure(PeerID pid, boolean propagateFailure) {
        
        PeerViewElement pve = getPeerViewElement(pid);
        
        if (null != pve) {
            notifyFailure(pve, propagateFailure);
        }
    }
    
    /**
     *  Invoked when a peerview member peer becomes unreachable.
     *
     *  @param pid ID of the peer which failed.
     *  @param propagateFailure If <tt>true</tt>then broadcast the failure to 
     *  other peers otherwise only update the local peerview.
     **/
    void notifyFailure(PeerViewElement pve, boolean propagateFailure) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Notifying failure of " + pve);
        }
        
        try {
            boolean removedFromPeerView = removePeerViewElement(pve);
            
            // only propagate if we actually knew of the peer
            propagateFailure &= (removedFromPeerView || (self == pve));
            
            // Notify local listeners
            if (removedFromPeerView) {
                generateEvent(PeerViewEvent.FAIL, pve);
            }
            
            boolean emptyPeerView = localView.isEmpty();
            
            // If the local view has become empty, reset the kicker into
            // a seeding mode.
            if (emptyPeerView && removedFromPeerView) {
                rescheduleKick(true);
            }
            
            if (propagateFailure) {
                // Notify other rendezvous peers that there has been a failure.
                OutputPipe op = localGroupWirePipeOutputPipe;
                
                if (null != op) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Propagating failure of " + pve);
                    }
                    
                    send(op, pve, true, true);
                }
            }
        } catch (Exception ez) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failure while generating noficiation of failure of PeerView : " + pve, ez);
            }
        }
    }
    
    /**
     * Invoked by the Timer thread to cause each PeerView to initiate
     * a Peer Advertisement exchange.
     */
    private void kick() {
        
        try {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Begun kick() in " + group.getPeerGroupID());
            }
            
            // Use seed strategy. (it has its own throttling and resource limiting).
            seed();
            
            // refresh ourself to a peer in our view
            PeerViewElement refreshee = refreshRecipientStrategy.next();
            
            if ((refreshee != null) && (self != refreshee)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Refresh " + refreshee);
                }
                send(refreshee, self, false, false);
            }
            
            if (!rdvService.isRendezVous()) {
                return;
            }
            
            // now share an adv from our local view to another peer from our
            // local view.
            
            PeerViewElement recipient = kickRecipientStrategy.next();
            
            if (recipient == null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No recipient to send adv ");
                }
                return;
            }
            
            PeerViewElement rpve = kickAdvertisementStrategy.next();
            
            if (rpve == null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No adv to send");
                }
                return;
            }
            
            if (rpve.equals(recipient) || self.equals(recipient)) {
                // give up: no point in sending a peer its own adv
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("adv to send is same as recipient: Nothing to do.");
                }
                return;
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Sending adv " + rpve + " to " + recipient);
            }
            
            send(recipient, rpve, true, false);
        } finally {
            rescheduleKick(false);
        }
    }
    
    /**
     *  Choose a boot level appropriate for the current configuration and state.
     *
     *  @return the new boot level.
     **/
    private int adjustBootLevel() {
        
        boolean areWeHappy = localView.size() >= minHappyPeerView;
        
        // increment boot level faster if we have a reasonable peerview.
        int increment = areWeHappy ? BOOTLEVEL_INCREMENT : BOOTLEVEL_INCREMENT * 2;
        
        int maxbootlevel = rdvService.isRendezVous() ? MAX_RDV_PEER_BOOTLEVEL : MAX_EDGE_PEER_BOOTLEVEL;
        
        // if we don't have a reasonable peerview, we continue to try harder.
        maxbootlevel -= (areWeHappy ? 0 : BOOTLEVEL_INCREMENT);
        
        bootLevel = Math.min(maxbootlevel, bootLevel + increment);
        
        return bootLevel;
    }
    
    private synchronized void rescheduleKick(boolean now) {
        
        if (closed) {
            return;
        }
        
        // Set the next iteration
        try {
            if (now) {
                bootLevel = MIN_BOOTLEVEL;
            } else {
                adjustBootLevel();
            }
            
            long tilNextKick = DEFAULT_BOOTSTRAP_KICK_INTERVAL * ((1L << bootLevel) - 1);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug(
                        "Scheduling kick in " + (tilNextKick / TimeUtils.ASECOND) + " seconds at bootLevel " + bootLevel + " in group "
                        + group.getPeerGroupID());
            }
            
            KickerTask task = new KickerTask();
            
            addTask(task, tilNextKick, -1);
        } catch (Exception ez1) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot set timer. RPV will not work.", ez1);
            }
        }
    }
    
    /**
     * Send our own advertisement to a randomly choosen set of potential
     * rendezvous peers.
     *
     * @param rdvs A list of RdvAdvertisement for the remote peers.
     * @param maxNb is the maximum number of peers to send the advertisement.
     * @return boolean true if there was at least one peer to send the
     * advertisement to. Returns false otherwise.
     **/
    private boolean sendRandomByAdv(List rdvs, int maxNb, long delay) {
        
        if (rdvs.isEmpty()) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No RDV peers to send queries");
            }
            return false;
        }
        
        int counter = Math.min(maxNb, rdvs.size());
        
        while (counter-- > 0) {
            RdvAdvertisement radv = (RdvAdvertisement) rdvs.remove(0);
            PeerViewElement pve = new PeerViewElement(endpoint, radv);
            
            timedSend(self, pve, delay);
        }
        
        return true;
    }
    
    /**
     * Send our own advertisement to a randomly choosen set of potential
     * rendezvous peers.
     *
     * @param rdvs A list of URI or EndpointAddress for the remote peers.
     * @param maxNb is the maximum number of peers to send the advertisement.
     * @return boolean true if there was at least one peer to send the
     * advertisement to. Returns false otherwise.
     **/
    private boolean sendRandomByAddr(List dests, int maxNb, long delay) {
        
        if (dests.isEmpty()) {
            return false;
        }
        
        int counter = Math.min(maxNb, dests.size());
        
        while (counter-- > 0) {
            Object addr = dests.remove(0);
            
            EndpointAddress dest;
            if( addr instanceof URI ) {
                try {
                dest = new EndpointAddress((URI) addr);
                } catch( IllegalArgumentException badAddr ) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("bad URI in seed list : " + addr, badAddr );
                    }
                    continue;
                }
            } else {
                dest = (EndpointAddress) addr;
            }
            
            timedSend(self, dest, delay);
        }
        
        return true;
    }
    
    /**
     * Get a List of RdvAdvertisements locally found in Discovery
     * of peers that w ere known to act as Rendezvous.
     *
     * @return List a vector of RdvAdvertisement.
     **/
    
    private List discoverRdvAdverisements() {
        
        List v = new ArrayList();
        
        DiscoveryService discovery = group.getDiscoveryService();
        
        if (discovery == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Discovery is not yet enabled.");
            }
            
            /* This may happen when a group is just joined. */
            return v;
        }
        
        Enumeration rdvs;
        
        try {
            rdvs = discovery.getLocalAdvertisements(DiscoveryService.ADV, RdvAdvertisement.ServiceNameTag, name );
            
            // start an async query for next time around.
            discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, RdvAdvertisement.ServiceNameTag, name, Integer.MAX_VALUE);
        } catch (IOException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed getting RdvAdvertisements from local discovery", e);
            }
            return v;
        }
        
        while (rdvs.hasMoreElements()) {
            Advertisement adv = (Advertisement) rdvs.nextElement();
            
            if (adv instanceof RdvAdvertisement) {
                RdvAdvertisement rdv = (RdvAdvertisement) adv;
                
                if (rdv.getGroupID().equals(group.getPeerGroupID()) && !group.getPeerID().equals(rdv.getPeerID())) {
                    v.add(rdv);
                }
            }
        }
        
        return v;
    }
    
    /**
     *  Refresh the local copy of the peer advertisement and the rendezvous
     *  advertisement.
     **/
    private void refreshSelf() {
        
        RdvAdvertisement radv = null;
        
        synchronized (this) {
            PeerAdvertisement newPadv = group.getPeerAdvertisement();
            int newModCount = newPadv.getModCount();
            
            if ((lastPeerAdv != newPadv) || (lastModCount != newModCount)) {
                lastPeerAdv = newPadv;
                lastModCount = newModCount;
                
                // create a new local RdvAdvertisement and set it to self.
                radv = createRdvAdvertisement(lastPeerAdv, name);
                
                if (radv != null) {
                    self.setRdvAdvertisement(radv);
                }
            }
        }
        
        // republish our own RdvAdvertisement if it's been updated.
        if (radv != null) {
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (null != discovery) {
                try {
                    if (rdvService.isRendezVous()) {
                        discovery.publish(radv, DEFAULT_RADV_LIFETIME, DEFAULT_RADV_EXPIRATION);
                    } else {
                        // in case we were previously a rendezvous.
                        discovery.flushAdvertisement(radv);
                    }
                } catch (IOException ignored) {
                    ;
                }
            }
        }
    }
    
    private static RdvAdvertisement createRdvAdvertisement(PeerAdvertisement padv, String name) {
        
        try {
            // FIX ME: 10/19/2002 lomax@jxta.org. We need to properly set up the service ID. Unfortunately
            // this current implementation of the PeerView takes a String as a service name and not its ID.
            // Since currently, there is only PeerView per group (all peerviews share the same "service", this
            // is not a problem, but that will have to be fixed eventually.
            
            // create a new RdvAdvertisement
            RdvAdvertisement rdv = (RdvAdvertisement) AdvertisementFactory.newAdvertisement(RdvAdvertisement.getAdvertisementType());
            
            rdv.setPeerID(padv.getPeerID());
            rdv.setGroupID(padv.getPeerGroupID());
            rdv.setServiceName(name);
            rdv.setName(padv.getName());
            
            RouteAdvertisement ra = RendezVousServiceImpl.extractRouteAdv(padv);
            
            // Insert it into the RdvAdvertisement.
            rdv.setRouteAdv(ra);
            
            return rdv;
        } catch (Exception ez) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot create Local RdvAdvertisement: ", ez);
            }
            return null;
        }
    }
    
    /**
     * Add a listener for PeerViewEvent
     *
     * @param  listener  An PeerViewListener to process the event.
     **/
    public boolean addListener(PeerViewListener listener) {
        boolean added = rpvListeners.add(listener);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Registered PeerViewEvent Listener (" + listener.getClass().getName() + ")");
        }
        
        return added;
    }
    
    /**
     * Removes a PeerViewEvent Listener previously added with addListener.
     *
     * @param  listener  the PeerViewListener listener remove
     * @return           whether successful or not
     **/
    public boolean removeListener(PeerViewListener listener) {
        boolean removed = rpvListeners.remove(listener);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Removed PeerViewEvent Listener (" + listener.getClass().getName() + ")");
        }
        
        return removed;
    }
    
    /**
     *  Generate a PeerView Event and notify all listeners.
     *
     *  @param type the Event Type.
     *  @param element  The peer having the event.
     **/
    private void generateEvent(int type, PeerViewElement element) {
        
        PeerViewEvent newevent = new PeerViewEvent(this, type, element);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Calling listeners for " + newevent + " in group " + group.getPeerGroupID());
        }
        
        Iterator eachListener = Arrays.asList(rpvListeners.toArray()).iterator();
        
        while (eachListener.hasNext()) {
            PeerViewListener pvl = (PeerViewListener) eachListener.next();
            
            try {
                pvl.peerViewEvent(newevent);
            } catch (Throwable ignored) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable in PeerViewEvent listener : (" + pvl.getClass().getName() + ")", ignored);
                }
            }
        }
    }
    
    private PipeAdvertisement makeWirePipeAdvertisement(PeerGroup destGroup) {
        
        PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        
        // Create a pipe advertisement for this group.
        // Generate a well known but unique ID.
        // FIXME bondolo 20040507 The ID created is really poor, it has only
        // 2 unique bytes on average. it would be much better to hash something
        // also, since the the definition of how to use the seed bytes is not
        // fixed, it's not reliable.
        PipeID pipeId = IDFactory.newPipeID(destGroup.getPeerGroupID(),
                (SERVICE_NAME + group.getPeerGroupID().getUniqueValue().toString() + name).getBytes());
        
        adv.setPipeID(pipeId);
        adv.setType(PipeService.PropagateType);
        adv.setName(SERVICE_NAME + " pipe for " + group.getPeerGroupID());
        
        return adv;
    }
    
    private synchronized void openWirePipes() {
        
        PipeService pipes = (PipeService) group.getPipeService();
        
        if (null == pipes) {
            scheduleOpenPipes(TimeUtils.ASECOND); // Try again in one second.
            return;
        }
        
        try {
            if (rdvService.isRendezVous()) {
                // First, listen to in our own PeerGroup
                if (null == localGroupWirePipeInputPipe) {
                    localGroupWirePipeInputPipe = pipes.createInputPipe(localGroupWirePipeAdv, new WirePipeListener());
                }
                
                if (null == localGroupWirePipeOutputPipe) {
                    // Creates the OutputPipe - note that timeout is irrelevant for
                    // propagated pipe.
                    
                    localGroupWirePipeOutputPipe = pipes.createOutputPipe(localGroupWirePipeAdv, 1 * TimeUtils.ASECOND);
                }
                
                if (localGroupWirePipeOutputPipe == null) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Cannot get OutputPipe for current group");
                    }
                }
            } else {
                if (null != localGroupWirePipeInputPipe) {
                    localGroupWirePipeInputPipe.close();
                    localGroupWirePipeInputPipe = null;
                }
                
                if (null != localGroupWirePipeOutputPipe) {
                    localGroupWirePipeOutputPipe.close();
                    localGroupWirePipeOutputPipe = null;
                }
            }
        } catch (Exception failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not open pipes in local group. Trying again in 1 second.", failed);
            }
            
            scheduleOpenPipes(TimeUtils.ASECOND); // Try again in one second.
            return;
        }
        
        if (advertisingGroup != null) {
            
            try {
                pipes = advertisingGroup.getPipeService();
                
                if (null == pipes) {
                    scheduleOpenPipes(TimeUtils.ASECOND); // Try again in one second.
                    return;
                }
                
                if (rdvService.isRendezVous()) {
                    if (null == wirePipeInputPipe) {
                        wirePipeInputPipe = pipes.createInputPipe(advGroupPropPipeAdv, new WirePipeListener());
                    }
                } else {
                    if (wirePipeInputPipe != null) {
                        wirePipeInputPipe.close();
                        wirePipeInputPipe = null;
                    }
                }
                
                if (null == wirePipeOutputPipe) {
                    wirePipeOutputPipe = pipes.createOutputPipe(advGroupPropPipeAdv, 1 * TimeUtils.ASECOND);
                }
                
                if (wirePipeOutputPipe == null) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Cannot get OutputPipe for current group");
                    }
                }
                
            } catch (Exception failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not open pipes in local group. Trying again in 1 second.", failed);
                }
                
                scheduleOpenPipes(TimeUtils.ASECOND); // Try again in one second.
                return;
            }
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Propagate Pipes opened.");
        }
    }
    
    private synchronized void closeWirePipes() {
        
        if (localGroupWirePipeInputPipe != null) {
            localGroupWirePipeInputPipe.close();
            localGroupWirePipeInputPipe = null;
        }
        
        if (localGroupWirePipeOutputPipe != null) {
            localGroupWirePipeOutputPipe.close();
            localGroupWirePipeOutputPipe = null;
        }
        
        if (wirePipeInputPipe != null) {
            wirePipeInputPipe.close();
            wirePipeInputPipe = null;
        }
        
        if (wirePipeOutputPipe != null) {
            wirePipeOutputPipe.close();
            wirePipeOutputPipe = null;
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Propagate Pipes closed.");
        }
    }
    
    /**
     *  Adapter class for receiving wire pipe messages
     **/
    private class WirePipeListener implements PipeMsgListener {
        
        /**
         *  {@inheritDoc}
         **/
        public void pipeMsgEvent(PipeMsgEvent event) {
            
            Message msg = event.getMessage();
            
            boolean failure = (null != msg.getMessageElement(MESSAGE_NAMESPACE, FAILURE_ELEMENT_NAME));
            boolean response = (null != msg.getMessageElement(MESSAGE_NAMESPACE, RESPONSE_ELEMENT_NAME));
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug(
                        "Received a PeerView " + (failure ? "failure " : "") + (response ? "response " : "") + "message [" + msg
                        + "] on propagated pipe " + event.getPipeID());
            }
            
            if (!failure && !response) {
                
                // If this is not a failure message then decide if we will respond.
                //
                // We play a game that is tuned by the view size so that the expectation of number of responses is equal to
                // minHappyPeerView. The game is to draw a number between 0 and the pv size.  If the result is < minHappyPeerView,
                // then we win (respond) else we lose (stay silent). The probability of winning is HAPPY_SIZE/viewsize. If each of
                // the viewsize peers plays the same game, on average HAPPY_SIZE of them win (with a significant variance, but
                // that is good enough). If viewsize is <= HAPPY_SIZE, then all respond.  This is approximate, of course, since
                // the view size is not always consistent among peers.
                
                int viewsize = PeerView.this.localView.size();
                
                if (viewsize > minHappyPeerView) {
                    int randinview = random.nextInt(viewsize);
                    
                    if (randinview >= minHappyPeerView) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Ignoring " + msg + " from pipe " + event.getPipeID());
                        }
                        // We "lose".
                        return;
                    }
                } // Else, we always win; don't bother playing.
            }
            
            // Fabricate dummy src and dst addrs so that we can call processIncoming. These are
            // only used for traces. The merit of using the pipeID is that it is recognizable
            // in these traces.
            EndpointAddress src = new EndpointAddress(event.getPipeID(), SERVICE_NAME, null);
            EndpointAddress dest = new EndpointAddress(event.getPipeID(), SERVICE_NAME, null);
            
            try {
                // call the peerview.
                PeerView.this.processIncomingMessage(msg, src, dest);
            } catch (Throwable ez) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed processing " + msg + " from pipe " + event.getPipeID(), ez);
                }
            }
        }
    }
    
    private synchronized void scheduleAdvertisingGroupQuery(long delay) {
        
        if (closed) {
            return;
        }
        
        TimerTask task = new AdvertisingGroupQueryTask();
        
        addTask(task, delay, -1);
    }
    
    /**
     * Class implementing the query request on the AdvertisingGroup
     **/
    private final class AdvertisingGroupQueryTask extends TimerTask {
        
        /**
         *  {@inheritDoc}
         **/
        public boolean cancel() {
            
            boolean res = super.cancel();
            
            return res;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (closed) {
                    return;
                }
                
                OutputPipe op = wirePipeOutputPipe;
                
                if (null != op) {
                    Message msg = makeMessage(self, false, false);
                    
                    op.send(msg);
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            } finally {
                removeTask(this);
            }
        }
    }
    
    /**
     * Get a copy of the PeerView for this group.
     *
     * @return A SortedSet which is the current local view of the peerview
     **/
    public SortedSet getView() {
        synchronized (localView) {
            return new TreeSet(localView);
        }
    }
    
    /**
     * Get a list of the current seeds for this peerview.
     *
     * @deprecated This is a private accessor for debugging and diagnostics.
     * Unless you are the JXTA Shell 'rdvcontrol' command then it's probably
     * not a good idea to use this.
     *
     * @return A Collection of {@link java.net.URI}.
     **/
    public Collection getActiveSeeds() {
        
        return Arrays.asList( activeSeedHosts.toArray() );
    }
    
    /**
     * Get a list of the current seeds for this peerview.
     *
     * @deprecated This is a private accessor for debugging and diagnostics.
     * Unless you are the JXTA Shell 'rdvcontrol' command then it's probably
     * not a good idea to use this.
     *
     * @return A Collection of {@link java.net.URI}.
     **/
    public Collection getPermanentSeeds() {
        
        return Arrays.asList( permanentSeedHosts.toArray() );
    }
    
    /**
     * Get a list of the current seeds for this peerview.
     *
     * @deprecated This is a private accessor for debugging and diagnostics.
     * Unless you are the JXTA Shell 'rdvcontrol' command then it's probably
     * not a good idea to use this.
     *
     * @return A Collection of {@link java.net.URI}.
     **/
    public Collection getSeedingHosts() {
        
        return Arrays.asList( seedingURIs.toArray() );
    }
    
    /**
     *  Add the provided element to the local peerview.
     *
     *  @param pve the <code>PeerViewElement</code> to add.
     *  @return <code>true</true> if the element was not present and added
     *  otherwise <code>false</code>.
     **/
    private boolean addPeerViewElement(PeerViewElement pve) {
        boolean added;
        
        if (null == pve.getRdvAdvertisement()) {
            throw new IllegalStateException("Cannot add a seed pve to local view");
        }
        
        synchronized (localView) {
            added = localView.add(pve);
            
            if (added) {
                // Refresh, if necessary, our up and down peers.
                updateUpAndDownPeers();
            }
        }
        
        if (added) {
            pve.setPeerView(this);
        }
        
        return added;
    }
    
    /**
     *  Remove the provided element from the local peerview.
     *
     *  @param pve the <code>PeerViewElement</code> to remove.
     *  @return <code>true</true> if the element was present and removed
     *  otherwise <code>false</code>.
     **/
    private boolean removePeerViewElement(PeerViewElement pve) {
        boolean removed;
        
        synchronized (localView) {
            removed = localView.remove(pve);
            
            if (removed) {
                // Refresh, if necessary, our up and down peers.
                updateUpAndDownPeers();
            }
        }
        
        if (removed) {
            pve.setPeerView(null);
        }
        
        return removed;
    }
    
    /**
     * Return from the local view, the PeerViewElement that is equal to the
     * given PeerViewDestination, if one exists or <code>null</code> if it is
     * not present. Identity is defined by {@link PeerViewDestination#equals()}
     * which only looks at the destination address. Thus a PeerViewDestination
     * is enough. A full PeerViewElement may be passed as well.  This method
     * does not require external synchronization.
     *
     * @param wanted PeerViewDestination matching the desired one.
     * @return the matching PeerViewElement or <code>null</code> if it could not
     * be found.
     **/
    public PeerViewElement getPeerViewElement(PeerViewDestination wanted) {
        
        try {
            PeerViewElement found = (PeerViewElement) localView.tailSet(wanted).first();
            
            if (wanted.equals(found)) {
                return found;
            }
        } catch (NoSuchElementException nse) {
            // This can happen if the tailset is empty. We could test for it,
            // but it could still become empty after the test, since it reflects
            // concurrent changes to localView. Not worth synchronizing for that
            // rare occurence. The end-result is still correct.
        }
        
        return null;
    }
    
    /**
     * Get from the local view, the PeerViewElement for the given PeerID, if one
     * exists. Null otherwise. This method does not require external
     * synchronization.
     *
     * @param pid the PeerID of the desired element.
     * @return the matching PeerViewElement null if it could not be found.
     **/
    public PeerViewElement getPeerViewElement(ID pid) {
        
        return getPeerViewElement(new PeerViewDestination(pid));
    }
    
    /**
     * Get the down peer from the local peer.
     *
     * @return the down PeerViewElement or null if there is no such peer.
     **/
    public PeerViewElement getDownPeer() {
        return downPeer;
    }
    
    /**
     * Get the local peer.
     *
     * @return the local PeerViewElement
     **/
    public PeerViewElement getSelf() {
        return self;
    }
    
    /**
     * Get the up peer from the local peer.
     *
     * @return the up PeerViewElement or null if there is no such peer.
     **/
    public PeerViewElement getUpPeer() {
        return upPeer;
    }
    
    /**
     *  update Up and Down Peers
     **/
    private void updateUpAndDownPeers() {
        
        synchronized (localView) {
            final PeerViewElement oldDown = downPeer;
            final PeerViewElement oldUp = upPeer;
            
            SortedSet headSet = localView.headSet(self);
            
            if (headSet.size() > 0) {
                downPeer = (PeerViewElement) headSet.last();
            } else {
                downPeer = null;
            }
            
            SortedSet tailSet = localView.tailSet(self);
            
            if (tailSet.size() > 0) {
                if (self.equals(tailSet.first())) {
                    Iterator eachTail = tailSet.iterator();
                    
                    eachTail.next(); // self
                    
                    if (eachTail.hasNext()) {
                        upPeer = (PeerViewElement) eachTail.next();
                    } else {
                        upPeer = null;
                    }
                } else {
                    upPeer = (PeerViewElement) tailSet.first();
                }
            } else {
                upPeer = null;
            }
            
            if ((oldDown != downPeer) && (downPeer != null)) {
                downPeer.setLastUpdateTime(TimeUtils.timeNow());
            }
            
            if ((oldUp != upPeer) && (upPeer != null)) {
                upPeer.setLastUpdateTime(TimeUtils.timeNow());
            }
        }
    }
    
    /**
     * A task that checks on upPeer and downPeer.
     **/
    private final class WatchdogTask extends TimerTask {
        
        WatchdogTask() {
        }
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (closed) {
                    return;
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Watchdog task executing for group " + PeerView.this.group.getPeerGroupID());
                }
                
                // check for acl updates
                if (TimeUtils.timeNow() > PeerView.this.nextACLrefreshTime) {
                    PeerView.this.nextACLrefreshTime = TimeUtils.toAbsoluteTimeMillis( ACL_REFRESH_PERIOD );
                    if (aclFile.lastModified() > aclFileLastModified) {
                        PeerView.this.aclFileLastModified = aclFile.lastModified();
                        acl.refresh(aclFile);
                    }
                }
                
                PeerViewElement up = PeerView.this.getUpPeer();
                
                if (up != null) {
                    if (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), up.getLastUpdateTime()) > WATCHDOG_GRACE_DELAY) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("UP peer has gone MIA : " + up);
                        }
                        
                        notifyFailure(up, true);
                        
                    } else {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Checking on UP peer : " + up);
                        }
                        
                        PeerView.this.send(up, PeerView.this.getSelf(), false, false);
                    }
                }
                
                PeerViewElement down = PeerView.this.getDownPeer();
                
                if (down != null) {
                    if (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), down.getLastUpdateTime()) > WATCHDOG_GRACE_DELAY) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("DOWN peer has gone MIA : " + down);
                        }
                        
                        notifyFailure(down, true);
                        
                    } else {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Checking on DOWN peer : " + down);
                        }
                        
                        PeerView.this.send(down, PeerView.this.getSelf(), false, false);
                    }
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            }
        }
    }
    
    /**
     *  @return List containing {@link net.jxta.endpoint.EndpointAddress}.
     **/
    private List getRelayPeers() {
        
        List res = new ArrayList();
        
        try {
            EndpointService ep = group.getEndpointService();
            
            Iterator it = ep.getAllMessageTransports();
            
            while (it.hasNext()) {
                MessageTransport mt = (MessageTransport) it.next();
                
                if( !mt.getEndpointService().getGroup().getPeerGroupID().equals(group.getPeerGroupID()) ) {
                    continue;
                }
                
                if (mt instanceof RelayClient) {
                    RelayClient er = (RelayClient) mt;
                    List v = er.getActiveRelays(group);
                    
                    if (v == null) {
                        continue;
                    }
                    
                    Iterator eachRelay = v.iterator();
                    
                    while (eachRelay.hasNext()) {
                        AccessPointAdvertisement anAPA = (AccessPointAdvertisement) eachRelay.next();
                        
                        res.add(new EndpointAddress("jxta", anAPA.getPeerID().getUniqueValue().toString(), null, null));
                    }
                }
            }
        } catch (Exception ez1) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Unexpected error getting relays", ez1);
            }
        }
        
        return res;
    }
    
    
    static URI[] loadSeeds( URI seedingURI ) throws IOException {
        URL seedingURL = seedingURI.toURL();
        
        URLConnection connection = seedingURL.openConnection();
        connection.setDoInput(true);
        InputStream is = connection.getInputStream();
        BufferedReader seeds = new BufferedReader( new InputStreamReader( is ) );
        
        List seedURIs = new ArrayList();
        while( true ) {
            String aSeed = seeds.readLine();
            
            if( null == aSeed ) {
                break;
            }
            
            aSeed = aSeed.trim();
            
            if( 0 == aSeed.length() ) {
                continue;
            }
            
            try {
                URI seedURI = new URI(aSeed);
                
                if( seedURI.isAbsolute() ) {
                    seedURIs.add( seedURI );
                } else {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("bad URI in seeding list : " + aSeed );
                    }
                }
            } catch(URISyntaxException badURI) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("bad URI in seeding list : " + aSeed, badURI );
                }
            }
        }
        
        is.close();
        
        return (URI[]) seedURIs.toArray( new URI[seedURIs.size()]);
    }
    
    /**
     * Class implementing the kicker
     **/
    private final class KickerTask extends TimerTask {
        
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (closed) {
                    return;
                }
                
                PeerView.this.kick();
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread : " + Thread.currentThread().getName(), all);
                }
            } finally {
                removeTask(this);
            }
        }
    }
    
    private synchronized void timedSend(PeerViewElement pve, PeerViewElement dest, long delay) {
        
        if (closed) {
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Timed send of " + pve + " to " + dest + " in " + (delay / TimeUtils.ASECOND) + " seconds");
        }
        
        TimedSendTask task = new TimedSendTask(dest, pve);
        
        addTask(task, delay, -1);
    }
    
    private synchronized void timedSend(PeerViewElement pve, EndpointAddress destAddr, long delay) {
        
        if (closed) {
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Timed send of " + pve + " to " + destAddr + " in " + (delay / TimeUtils.ASECOND) + " seconds");
        }
        
        TimedSendTask task = new TimedSendTask(destAddr, pve);
        
        addTask(task, delay, -1);
    }
    
    /**
     * Class implementing the TimerTask that tries to send an advertisement
     * to a remote peer, in background.
     **/
    private final class TimedSendTask extends TimerTask {
        
        private final PeerViewElement pve;
        private PeerViewElement destpve = null;
        private EndpointAddress destaddr = null;
        
        public TimedSendTask(EndpointAddress dest, PeerViewElement pve) {
            this.destaddr = dest;
            this.pve = pve;
        }
        
        public TimedSendTask(PeerViewElement destpve, PeerViewElement pve) {
            this.destpve = destpve;
            this.pve = pve;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public synchronized boolean cancel() {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Canceling TimedSendTask for : " + pve);
            }
            
            return super.cancel();
        }
        
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (closed) {
                    return;
                }
                
                boolean status;
                
                if (null != destpve) {
                    status = PeerView.this.send(destpve, pve, false, false);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("pv.send to " + destpve + " status=" + status);
                    }
                } else {
                    status = PeerView.this.send(destaddr, null, pve, false, false);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("pv.send to " + destaddr + " status=" + status);
                    }
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread : " + Thread.currentThread().getName(), all);
                }
            } finally {
                removeTask(this);
            }
        }
    }
}

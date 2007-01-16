/*
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
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *  not be used to endorse or promote products derived from this
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
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: DiscoveryServiceImpl.java,v 1.1 2007/01/16 11:01:50 thomas Exp $
 */
package net.jxta.impl.discovery;


import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.OutgoingMessageEvent;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.DiscoveryQueryMsg;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.ResolverQueryMsg;
import net.jxta.protocol.ResolverResponseMsg;
import net.jxta.protocol.ResolverSrdiMsg;
import net.jxta.protocol.SrdiMessage;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.resolver.QueryHandler;
import net.jxta.impl.resolver.InternalQueryHandler;
import net.jxta.resolver.ResolverService;
import net.jxta.resolver.SrdiHandler;
import net.jxta.service.Service;
import net.jxta.endpoint.EndpointAddress;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.cm.Cm;
import net.jxta.impl.cm.Srdi;
import net.jxta.impl.cm.SrdiIndex;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.DiscoveryConfigAdv;
import net.jxta.impl.protocol.DiscoveryQuery;
import net.jxta.impl.protocol.DiscoveryResponse;
import net.jxta.impl.protocol.ResolverQuery;
import net.jxta.impl.protocol.ResolverResponse;
import net.jxta.impl.protocol.SrdiMessageImpl;
import net.jxta.impl.util.TimeUtils;


/**
 * This Discovery Service implementation provides a mechanism to discover peers
 * within the horizon of the resolver service. The horizon is normally
 * restricted to the group's boundaries but this is not an absolute requirement.
 * Use of the Resolver service is not an absolute requirement either for a
 * discovery service, but this is what this is part of the platform and
 * default net peer group protocol set, which this code implements.
 *
 * <p/>This implementation uses the standard JXTA Peer Discovery Protocol
 * (PDP).
 *
 * <p/>The DiscoveryService service also provides a way to obtain information
 * from a specified peer and request other peer advertisements, this
 * method is particularly useful in the case of a portal where new
 * relationships may be established starting from a predetermined peer
 * (perhaps described in address book, or through an invitation)
 *
 * @see net.jxta.discovery.DiscoveryService
 * @see net.jxta.protocol.DiscoveryQueryMsg
 * @see net.jxta.impl.protocol.DiscoveryQuery
 * @see net.jxta.protocol.DiscoveryResponseMsg
 * @see net.jxta.impl.protocol.DiscoveryResponse
 * @see net.jxta.resolver.ResolverService
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-pdp" target="_blank">JXTA Protocols Specification : Peer Discovery Protocol</a>
 */
public class DiscoveryServiceImpl implements DiscoveryService,
            InternalQueryHandler,
            RendezvousListener,
            SrdiHandler,
    Srdi.SrdiInterface {

    /**
     *  Log4J Logger
     */
    private final static Logger LOG = Logger.getLogger(DiscoveryServiceImpl.class.getName());

    /**
     *  The current query ID. The next query will be issued with this id.
     */
    private static int qid = 0;

    /**
     * The cache manager we're going to use to cache jxta advertisements
     */
    protected Cm cm;

    /**
     *  adv types
     */
    protected final static String[] dirname = { "Peers", "Groups", "Adv"};

    /**
     *  The maximum number of responses we will return for ANY query.
     */
    private final int MAX_RESPONSES = 50;

    private PeerGroup group = null;

    /**
     *  assignedID as a String.
     */
    private String handlerName = null;
    private ModuleImplAdvertisement implAdvertisement = null;
    private ResolverService resolver;
    private RendezVousService rendezvous;
    private MembershipService membership = null;

    private String localPeerId = null;

    private boolean started = false;

    /**
     *  The table of discovery listeners.
     *
     *  <p/><ul>
     *      <li>Values are <@link net.jxta.discovery.DiscoveryListener}</li>
     *  </ul>
     */
    private Set listeners = new HashSet();

    /**
     *  The table of discovery query listeners.
     *
     *  <p/><ul>
     *      <li>Keys are the query ID as an {@link java.lang.Integer}</li>
     *      <li>Values are <@link net.jxta.discovery.DiscoveryListener}</li>
     *  </ul>
     */
    private Hashtable listenerTable = new Hashtable();

    private final Object checkPeerAdvLock = new String("Check/Update PeerAdvertisement Lock");
    private PeerAdvertisement lastPeerAdv = null;
    private int lastModCount = -1;

    private boolean isRdv = false;
    private boolean alwaysUseReplicaPeer = false;
    private Credential credential = null;
    private StructuredDocument credentialDoc = null;
    private SrdiIndex srdiIndex = null;
    private Srdi srdi = null;
    private Thread srdiThread = null;
    private boolean localonly = false;

    private long initialDelay = 60 * TimeUtils.ASECOND;
    private long runInterval = 30 * TimeUtils.ASECOND;

    private final static String srdiIndexerFileName = "discoverySrdi";

    /**
     *  the discovery interface object
     */
    private DiscoveryService discoveryInterface = null;

    /**
     *  {@inheritDoc}
     */
    public synchronized Service getInterface() {
        if (discoveryInterface == null) {
            discoveryInterface = new DiscoveryServiceInterface(this);
        }
        return discoveryInterface;
    }

    /**
     *  {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    /**
     *  {@inheritDoc}
     */
    public int getRemoteAdvertisements(String peer,
                                       int type,
                                       String attribute,
                                       String value,
                                       int threshold) {

        return getRemoteAdvertisements(peer, type, attribute, value, threshold, null);
    }

    /**
     *  {@inheritDoc}
     */
    public int getRemoteAdvertisements(String peer,
                                       int type,
                                       String attribute,
                                       String value,
                                       int threshold,
                                       DiscoveryListener listener) {

        int myQueryID = nextQid();

        if (localonly) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("localonly, no network operations performed");
            }
            return myQueryID;
        }

        if (resolver == null) {
            // warn about calling the service before it started
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("resolver has not started yet, query discarded.");
            }
            return myQueryID;
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            StringBuffer query = new StringBuffer("Sending query#" + myQueryID + " for " + threshold + " " + dirname[type] + " advs");

            if (attribute != null) {
                query.append("\n\tattr = " + attribute);

                if (value != null) {
                    query.append("\tvalue = " + value);
                }
            }

            LOG.debug(query);
        }

        long t0 = System.currentTimeMillis();
        DiscoveryQueryMsg dquery = new DiscoveryQuery();

        dquery.setDiscoveryType(type);
        dquery.setAttr(attribute);
        dquery.setValue(value);
        dquery.setThreshold(threshold);

        if (listener != null) {
            listenerTable.put(new Integer(myQueryID), listener);
        }

        ResolverQuery query = new ResolverQuery(handlerName, credentialDoc, localPeerId, dquery.toString(), myQueryID);

        // check srdi
        if (peer == null && srdiIndex != null) {
            Vector res = srdiIndex.query(dirname[type], attribute, value, threshold);

            if (res.size() > 0) {
                srdi.forwardQuery(res, query, threshold);
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Srdi forward a query #" + myQueryID + " in " + (System.currentTimeMillis() - t0) + "ms.");
                }
                return myQueryID;
                // nothing in srdi, get a starting point in rpv
            }
            else
                if (group.isRendezvous() && attribute != null && value != null) {
                    PeerID destPeer = srdi.getReplicaPeer(dirname[type] + attribute + value);

                    if (destPeer != null) {
                        if (!destPeer.equals(group.getPeerID())) {
                            // forward query increments the hopcount to indicate getReplica
                            // has been invoked once
                            srdi.forwardQuery(destPeer, query);
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Srdi forward query #" + myQueryID + " to " + destPeer + " in " + (System.currentTimeMillis() - t0) + "ms.");
                            }
                            return myQueryID;
                        }
                    }
                }
        }

        // no srdi, not a rendezvous, start the walk
        resolver.sendQuery(peer, query);
        if (LOG.isEnabledFor(Level.DEBUG)) {
            if (peer == null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sent a query #" + myQueryID + " in " + (System.currentTimeMillis() - t0) + "ms.");
                }
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sent a query #" + myQueryID + " to " + peer + " in " + (System.currentTimeMillis() - t0) + "ms.");
                }
            }
        }

        return myQueryID;
    }

    /**
     *  {@inheritDoc}
     */
    public Enumeration getLocalAdvertisements(int type,
                                              String attribute,
                                              String value) throws IOException {

        if ((type > 2) || (type < 0)) {
            throw new IllegalArgumentException("Unknown Advertisement type");
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            StringBuffer query = new StringBuffer("Searching for " + dirname[type] + " advs");

            if (attribute != null) {
                query.append("\n\tattr = " + attribute);
            }

            if (value != null) {
                query.append("\tvalue = " + value);
            }
            LOG.debug(query);
        }

        return search(type, attribute, value, Integer.MAX_VALUE, false, null).elements();
    }

    /**
     *  {@inheritDoc}
     */
    public void init(PeerGroup pg, ID assignedID, Advertisement impl) throws PeerGroupException {

        group = pg;
        handlerName = assignedID.toString();
        implAdvertisement = (ModuleImplAdvertisement) impl;
        localPeerId = group.getPeerID().toString();

        try {
            ResourceBundle jxtaRsrcs = ResourceBundle.getBundle("net.jxta.user");
            String offStr = jxtaRsrcs.getString("impl.discovery.localonly");

            if (offStr != null) {
                localonly = offStr.equalsIgnoreCase("true");
                if (LOG.isEnabledFor(Level.WARN) && localonly) {
                    LOG.warn("configuration via properties is being phased out. Consider using parameters in the group advertisement or PlatformConfig advertisement instead.");
                }
            }
        } catch (MissingResourceException re) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("localonly not defined, initializing to false");
            }
        }

        ConfigParams confAdv = (ConfigParams) pg.getConfigAdvertisement();

        // Get the config. If we do not have a config, we're done; we just keep
        // the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            Advertisement adv = null;

            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(assignedID);

                if (null != configDoc) {
                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (NoSuchElementException failed) {
                ;
            }

            if (adv instanceof DiscoveryConfigAdv) {
                DiscoveryConfigAdv discoConfigAdv = (DiscoveryConfigAdv) adv;

                alwaysUseReplicaPeer = discoConfigAdv.getForwardAlwaysReplica();

                localonly |= discoConfigAdv.getLocalOnly();

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    if (localonly) {
                        LOG.debug("localonly set to true via service parameters");
                    }
                    if (alwaysUseReplicaPeer) {
                        LOG.debug("alwaysUseReplicaPeer set to true via service parameters");
                    }
                }
            }
        }

        cm = ((StdPeerGroup) group).getCacheManager();
        cm.setTrackDeltas(!localonly);

        // Initialize the peer adv tracking.
        checkUpdatePeerAdv();

        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Discovery Service : " + assignedID);

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
            configInfo.append("\n\t\tLocal Only : " + localonly);
            configInfo.append("\n\t\tAlways Use ReplicaPeer : " + alwaysUseReplicaPeer);

            LOG.info(configInfo);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public int startApp(String[] arg) {

        // Now we know that the resolver is going to be there.
        // The cm needs the resolver. The code is arranged so that
        // until the resolver and the cm are created, we just pretend
        // to be working. We have no requirement to be operational before
        // startApp() is called, but we must tolerate our public methods
        // being invoked. The reason for it is that services are registered
        // upon return from init() so that other services startApp() methods
        // can find them. (all startApp()s are called after all init()s - with
        // a few exceptions).

        resolver = group.getResolverService();

        if (null == resolver) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Stalled until there is a resolver service");
            }

            return Module.START_AGAIN_STALLED;
        }

        membership = group.getMembershipService();

        if (null == membership) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Stalled until there is a membership service");
            }

            return Module.START_AGAIN_STALLED;
        }

        rendezvous = group.getRendezVousService();

        if (null == rendezvous) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Stalled until there is a rendezvous service");
            }

            return Module.START_AGAIN_STALLED;
        }

        // local only discovery
        if (!localonly) {
            resolver.registerHandler(handlerName, this);
        }

        try {
            credential = (Credential) membership.getDefaultCredential();

            if (null != credential) {
                credentialDoc = credential.getDocument(MimeMediaType.XMLUTF8);
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to get credential", e);
            }
        }

        if (rendezvous.isRendezVous()) {
            beRendezvous();
        } else {
            beEdge();
        }

        rendezvous.addListener(this);

        started = true;

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Discovery service started");
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * dettach from the resolver
     */
    public void stopApp() {

        boolean failed = false;

        rendezvous.removeListener(this);

        if (resolver.unregisterHandler(handlerName) == null) {
            failed = true;
        }

        if (rendezvous.isRendezVous()) {
            if (resolver.unregisterSrdiHandler(handlerName) == null) {
                failed = true;
            }
        }

        if (LOG.isEnabledFor(Level.DEBUG) && failed) {
            LOG.debug("failed to unregister discovery from resolver.");
        }

        // stop the DiscoverySrdiThread
        if (srdiThread != null) {
            srdi.stop();
            srdi = null;
        }

        // Reset values in order to avoid cross-reference problems with GC
        resolver = null;
        group = null;
        membership = null;
        srdiIndex = null;
        srdiThread = null;
        rendezvous = null;

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Discovery service stopped");
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void flushAdvertisements(String id, int type)  throws IOException {

        if ((type <= ADV) && (id != null)) {
            ID advID = ID.create(URI.create(id));
            String advName = advID.getUniqueValue().toString();

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("flushing adv " + advName + " of type " + dirname[type]);
            }
            cm.remove(dirname[type], advName);
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("flushing advertisements of type " + dirname[type]);
            }
            cm.remove(dirname[type], null);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void flushAdvertisement(Advertisement adv)  throws IOException {

        int type = 0;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else
            if (adv instanceof PeerGroupAdvertisement) {
                type = GROUP;
            } else {
                type = ADV;
            }

        ID id = adv.getID();
        String advName = null;

        if (id != null && !id.equals(ID.nullID)) {
            advName = id.getUniqueValue().toString();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Flushing adv " + advName + " of type " + dirname[type]);
            }
        } else {
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (Exception everything) {
                IOException failure = new IOException("Failure removing Advertisement");

                failure.initCause(everything);
                throw failure;
            }
            advName = Cm.createTmpName(doc);
        }

        if (advName != null) {
            cm.remove(dirname[type], advName);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void publish(Advertisement adv) throws IOException {

        publish(adv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
    }

    /**
     *  {@inheritDoc}
     */
    public void publish(Advertisement adv,
                        long lifetime,
                        long expiration) throws IOException {

        ID advID = null;
        String advName = null;
        int type = -1;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else
            if (adv instanceof PeerGroupAdvertisement) {
                type = GROUP;
            } else {
                type = ADV;
            }

        switch (type) {
        case PEER:
            if (adv instanceof PeerAdvertisement) {
                break;
            }
            throw new IOException("Not a peer advertisement");

        case GROUP:
            if (adv instanceof PeerGroupAdvertisement) {
                break;
            }
            throw new IOException("Not a peergroup advertisement");

        case ADV:
            break;

        default:
            throw new IOException("Unknown advertisement type");
        }

        advID = adv.getID();

        // if we dont have a unique id for the adv, use the hash method
        if ((null == advID) || advID.equals(ID.nullID)) {
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (Exception everything) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed to generated document from advertisement", everything);
                }
                IOException failure = new IOException("Failed to generate document from advertisement");

                failure.initCause(everything);
                throw failure;
            }
            try {
                advName = Cm.createTmpName(doc);
            } catch (IllegalStateException ise) {
                IOException failure = new IOException("Failed to generate tempname from advertisement");

                failure.initCause(ise);
                throw failure;
            }
        } else {
            advName = advID.getUniqueValue().toString();
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug(
                "Publishing a " + adv.getAdvType() + " as " + dirname[type] + " / " + advName + "\n\texpiration : " + expiration + "\tlifetime :"
                + lifetime);
        }

        // save it
        cm.save(dirname[type], advName, adv, lifetime, expiration);
    }

    /**
     *  {@inheritDoc}
     */
    public void remotePublish(Advertisement adv) {

        remotePublish(null, adv, DiscoveryService.DEFAULT_EXPIRATION);
    }

    /**
     *  {@inheritDoc}
     */
    public void remotePublish(Advertisement adv, long timeout) {

        remotePublish(null, adv, timeout);
    }

    /**
     *  {@inheritDoc}
     */
    public void remotePublish(String peerid, Advertisement adv) {
        remotePublish(peerid, adv, DiscoveryService.DEFAULT_EXPIRATION);
    }

    /**
     *  {@inheritDoc}
     */
    public void processResponse(ResolverResponseMsg response) {
        processResponse(response, null);
    }

    /**
     *  {@inheritDoc}
     */
    public void processResponse(ResolverResponseMsg response, EndpointAddress srcAddress) {

        long t0 = System.currentTimeMillis();
        DiscoveryResponse res;

        try {
            StructuredTextDocument asDoc = (StructuredTextDocument)
                                           StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(response.getResponse()));

            res = new DiscoveryResponse(asDoc);
        } catch (Exception e) {
            // we don't understand this msg, let's skip it
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to Read Deiscovery Response", e);
            }
            return;
        } 
        /*
         PeerAdvertisement padv = res.getPeerAdvertisement();
         if (padv == null)
         return;
         
         if (LOG.isEnabledFor(Level.DEBUG)) {
         LOG.debug("Got a " + dirname[res.getDiscoveryType()] +
         " from "+padv.getName()+ " response : " +
         res.getQueryAttr() + " = " + res.getQueryValue());
         }
         
         try {
         // The sender does not put an expiration on that one, but
         // we do not want to keep it around for more than the
         // default duration. It may get updated or become invalid.
         publish(padv, PEER, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
         } catch (Exception e) {
         if (LOG.isEnabledFor(Level.DEBUG)) {
         LOG.debug(e, e);
         }
         return;
         }
         */
        Advertisement adv;

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Processing responses for query #" + response.getQueryId());
        }
        Enumeration en = res.getAdvertisements();
        Enumeration exps = res.getExpirations();

        long exp;

        if (en != null) {
            while (en.hasMoreElements()) {
                adv = (Advertisement) en.nextElement();
                exp = ((Long) exps.nextElement()).longValue();

                if (exp > 0 && adv != null) {
                    try {
                        publish(adv, exp, exp);
                    } catch (Exception e) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Error publishing Advertisement", e);
                        }
                    }
                }
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Received empty responses");
            }
        }

        DiscoveryEvent newevent = new DiscoveryEvent(srcAddress, res, response.getQueryId());

        DiscoveryListener dl = (DiscoveryListener)
                               listenerTable.get(new Integer(response.getQueryId()));

        if (dl != null) {
            try {
                dl.discoveryEvent(new DiscoveryEvent(srcAddress, res, response.getQueryId()));
            } catch (Throwable all) {
                LOG.fatal("Uncaught Throwable in listener :" + Thread.currentThread().getName(), all);
            }
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("processed a response for query #" + response.getQueryId() + " in :" + (System.currentTimeMillis() - t0));
        }

        // are there any registered discovery listeners,
        // generate the event and callback.
        t0 = System.currentTimeMillis();

        DiscoveryListener[] allListeners = (DiscoveryListener[]) listeners.toArray(new DiscoveryListener[0]);

        for (int eachListener = 0; eachListener < allListeners.length; eachListener++) {
            try {
                allListeners[eachListener].discoveryEvent(newevent);
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn(
                        "Uncaught Throwable in listener (" + allListeners[eachListener].getClass().getName() + ") :"
                        + Thread.currentThread().getName(),
                        all);
                }
            }
        }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Called all listenters to query #" + response.getQueryId() + " in :" + (System.currentTimeMillis() - t0));
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    public int processQuery(ResolverQueryMsg query) {
        
        return processQuery(query, null);
    }
    
    /**
     *  {@inheritDoc}
     */
    public int processQuery(ResolverQueryMsg query, EndpointAddress srcAddress) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            if (srcAddress != null) {
                LOG.debug("Processing query #" + query.getQueryId()+" from:"+srcAddress.toString());
            } else  {
                LOG.debug("Processing query #" + query.getQueryId()+" from: unknown");
            }
        }

        Vector results = null;
        Vector expirations = new Vector();
        DiscoveryQuery dq;
        long t0 = System.currentTimeMillis();

        try {
            StructuredTextDocument asDoc = (StructuredTextDocument)
                                           StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(query.getQuery()));

            dq = new DiscoveryQuery(asDoc);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Malformed query : ", e);
            }
            return ResolverService.OK;
        }

        if ((dq.getThreshold() < 0) || (dq.getDiscoveryType() < PEER) || (dq.getDiscoveryType() > ADV)) {

            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Malformed query");
            }
            return ResolverService.OK;
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Got a " + dirname[dq.getDiscoveryType()] + " query #" + query.getQueryId() + " query :" + dq.getAttr() + " = " + dq.getValue());
        }

        /*
         // Get the Peer Adv from the query and publish it.
         PeerAdvertisement padv = dq.getPeerAdvertisement();
         try {
         if (!(padv.getPeerID().toString()).equals(localPeerId)) {
         // publish others only. Since this one comes from outside,
         // we must not keep it beyond its expiration time.
         // FIXME: [jice@jxta.org 20011112] In theory there should
         // be an expiration time associated with it in the msg, like
         // all other items.
         publish(padv, PEER, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
         }
         } catch (Exception e) {
         if (LOG.isEnabledFor(Level.DEBUG)) {
         LOG.debug("Bad Peer Adv in Discovery Query", e);
         }
         }
         */

        /*
         *  threshold==0 and type==PEER is a special case. In this case we are
         *  responding for the purpose of providing our own adv only.
         */
        int thresh = Math.min(dq.getThreshold(), MAX_RESPONSES);

        /*
         *  threshold==0 and type==PEER is a special case. In this case we are
         *  responding for the purpose of providing our own adv only.
         */
        if ((dq.getDiscoveryType() == PEER) && (0 == dq.getThreshold())) {
            results = new Vector();
            results.add(group.getPeerAdvertisement().toString());
            expirations.add(new Long(DiscoveryService.DEFAULT_EXPIRATION));
            respond(query, dq, results, expirations);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Responding to query #" + query.getQueryId() + " in :" + (System.currentTimeMillis() - t0));
            }
            return ResolverService.OK;
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("start local search query" + dq.getAttr() + " " + dq.getValue());
            }
            results = search(dq.getDiscoveryType(), dq.getAttr(), dq.getValue(), thresh, true, expirations);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("start local search pruned " + results.size());
            }
        }


        // We only share the advs with > 0 expiration time.
        Iterator eachExpiration = expirations.iterator();
        Iterator eachAdv = results.iterator();
        
        while( eachExpiration.hasNext() ) {
            eachAdv.next();
            
            if( ((Long) eachExpiration.next()).longValue() <= 0 ) {
                eachAdv.remove();
                eachExpiration.remove();
            }
        }
        
        if (!results.isEmpty()) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Responding to " + dirname[dq.getDiscoveryType()] + " Query : " + dq.getAttr() + " = " + dq.getValue());
            }
            respond(query, dq, results, expirations);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Responded to query #" + query.getQueryId() + " in :" + (System.currentTimeMillis() - t0));
            }
            return ResolverService.OK;
        } else {
            // If this peer is a rendezvous, simply let the resolver
            // re-propagate the query.
            // If this peer is not a rendez, just discard the query.
            if (!group.isRendezvous()) {
                return ResolverService.OK;
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Querying SrdiIndex query #" + query.getQueryId());
            }
            Vector res = srdiIndex.query(dirname[dq.getDiscoveryType()], dq.getAttr(), dq.getValue(), thresh);

            if (res.size() > 0) {
                srdi.forwardQuery(res, query, thresh);
                return ResolverService.OK;
            } else if (query.getHopCount() == 0) {
                    PeerID destPeer = srdi.getReplicaPeer(dirname[dq.getDiscoveryType()] + dq.getAttr() + dq.getValue());
                    // destPeer can be null in a small rpv (<3)
                    if (destPeer != null) {
                        if (!destPeer.equals(group.getPeerID())) {
                            srdi.forwardQuery(destPeer, query);
                            return ResolverService.OK;
                        } else {
                            // start the walk since this peer is this the starting peer
                            query.incrementHopCount();
                        }
                    }
                }
        }
        return ResolverService.Repropagate;
    }

    private void respond(ResolverQueryMsg query,
                         DiscoveryQuery dq,
                         Vector results,
                         Vector expirations) {
        if (localonly) {
            return;
        }

        ResolverResponseMsg response;
        DiscoveryResponse dresponse = new DiscoveryResponse();

        // peer adv is optional, skip
        dresponse.setDiscoveryType(dq.getDiscoveryType());
        dresponse.setQueryAttr(dq.getAttr());
        dresponse.setQueryValue(dq.getValue());
        dresponse.setResponses(results);
        dresponse.setExpirations(expirations);

        // create a response from the query
        response = query.makeResponse();
        response.setCredential(credentialDoc);
        response.setResponse(dresponse.toString());

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Responding to " + query.getSrc());
        }

        resolver.sendResponse(query.getSrc(), response);
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void addDiscoveryListener(DiscoveryListener listener) {

        listeners.add(listener);
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized boolean removeDiscoveryListener(DiscoveryListener listener) {

        Iterator e = listenerTable.keySet().iterator();

        while (e.hasNext()) {
            Object key = e.next();

            if (listenerTable.get(key) == listener) {
                e.remove();
            }
        }

        return (listeners.remove(listener));
    }

    /**
     *  {@inheritDoc}
     */
    public void remotePublish(String peerid, Advertisement adv, long timeout) {

        if (localonly) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("localonly, no network operations performed");
            }
            return;
        }
        int type = -1;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else
            if (adv instanceof PeerGroupAdvertisement) {
                type = GROUP;
            } else {
                type = ADV;
            }

        // In case this is invoked before startApp().
        if (resolver == null) {
            return;
        }

        Vector advert = new Vector(1);
        Vector expirations = new Vector(1);

        advert.add(adv.toString());
        expirations.add(new Long(timeout));

        DiscoveryResponse dresponse = new DiscoveryResponse();

        dresponse.setDiscoveryType(type);
        dresponse.setResponses(advert);
        dresponse.setExpirations(expirations);

        ResolverResponse pushRes = new ResolverResponse(handlerName, credentialDoc, 0, dresponse.toString());

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Remote publishing ");
        }

        resolver.sendResponse(peerid, pushRes);
    }

    /**
     *  Search for a doc, that matches attr, and value
     * bytes is set to true if the caller wants wire format of the
     * advertisement, or set to false if caller wants Advertisement
     * objects.
     *
     * @param  type         Discovery type PEER, GROUP, ADV
     * @param  threshold    the upper limit of responses from one peer
     * @param  bytes        flag to indicate how the results are returned-- advs, or bytes
     * @param  expirations  vector containing the expirations associated with is returned
     * @param  attr         attribute name to narrow disocvery to Valid values for
     *      this parameter are null (don't care), or exact element name in the
     *      advertisement of interest (e.g. "Name")
     * @param  value        Value
     * @return              vector of results either as docs, or Strings
     */
    private Vector search(int type,
                          String attr,
                          String value,
                          int threshold,
                          boolean bytes,
                          Vector expirations) {

        if( type == PEER ) {
            checkUpdatePeerAdv();
        }

        Vector results;

        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be greater than zero");
        }

        if (expirations != null) {
            expirations.clear();
        }

        if (attr != null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Searching for " + threshold + " entries of type : " + dirname[type]);
            }
            // a discovery query with a specific search criteria.
            results = cm.search(dirname[type], attr, value, threshold, expirations);
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Getting " + threshold + " entries of type : " + dirname[type]);
            }
            // Returning any entry that exists
            results = cm.getRecords(dirname[type], threshold, null, expirations);
        }

        if (results.size() == 0 || bytes) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Returning " + results.size() + " results");
            }

            // nothing more to do;
            return results;
        }

        Vector advertisements = new Vector();

        for (int i = 0; i < results.size(); i++) {
            InputStream bis = null;

            try {
                bis = (InputStream) results.elementAt(i);
                Advertisement adv = AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, bis);

                advertisements.addElement(adv);
            } catch (Exception e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed building advertisment", e);
                }
            } finally {
                if (null != bis) {
                    try {
                        bis.close();
                    } catch (IOException ignored) {
                        ;
                    }
                }
                bis = null;
            }
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Returning " + advertisements.size() + " advertisements");
        }

        return advertisements;
    }

    /**
     * {@inheritDoc}
     */

    public long getAdvExpirationTime(ID id, int type) {
        String advName = null;

        if (id != null && !id.equals(ID.nullID)) {
            advName = id.getUniqueValue().toString();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Getting expiration time of " + advName + " of type " + dirname[type]);
            }
        } else {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("invalid attempt to get advertisement expiration time of NullID");
            }
            return -1;
        }

        return cm.getExpirationtime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    public long getAdvLifeTime(ID id, int type) {

        if (id == null || id.equals(ID.nullID)) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("invalid attempt to get advertisement lifetime of a NullID");
            }
            return -1;
        }
        
        String advName = id.getUniqueValue().toString();
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Getting lifetime of " + advName + " of type " + dirname[type]);
        }

        return cm.getLifetime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    public long getAdvExpirationTime(Advertisement adv) {
        int type = 0;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else
            if (adv instanceof PeerGroupAdvertisement) {
                type = GROUP;
            } else {
                type = ADV;
            }

        String advName = null;
        ID id = adv.getID();

        if (id != null && !id.equals(ID.nullID)) {
            advName = id.getUniqueValue().toString();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("attempting to getAdvExpirationTime on " + advName + " of type " + dirname[type]);
            }
        } else {
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (Exception everything) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed to get document", everything);
                }
                return -1;
            }
            advName = Cm.createTmpName(doc);
        }
        
        return cm.getExpirationtime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    public long getAdvLifeTime(Advertisement adv) {

        int type = 0;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else
            if (adv instanceof PeerGroupAdvertisement) {
                type = GROUP;
            } else {
                type = ADV;
            }

        ID id = adv.getID();
        String advName = null;

        if (id != null && !id.equals(ID.nullID)) {
            advName = id.getUniqueValue().toString();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("attempting to getAdvLifeTime " + advName + " of type " + dirname[type]);
            }
        } else {
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (Exception everything) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed to get document", everything);
                }
                return -1;
            }
            advName = Cm.createTmpName(doc);
        }
        return cm.getLifetime(dirname[type], advName);
    }

    /**
     *  {@inheritDoc}
     */
    public boolean processSrdi(ResolverSrdiMsg message) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + group.getPeerGroupID() + "] Received an SRDI messsage");
        }

        SrdiMessage srdiMsg;

        try {
            StructuredTextDocument asDoc = (StructuredTextDocument)
                                           StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(message.getPayload()));

            srdiMsg = new SrdiMessageImpl(asDoc);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed parsing srdi message", e);
            }
            return false;
        }

        PeerID pid = srdiMsg.getPeerID();

        Iterator eachEntry = srdiMsg.getEntries().iterator();

        while (eachEntry.hasNext()) {
            SrdiMessage.Entry entry = (SrdiMessage.Entry) eachEntry.next();

            if (entry.expiration > 0) {
                srdiIndex.add(srdiMsg.getPrimaryKey(), entry.key, entry.value, pid, entry.expiration);
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug(
                        "Primary Key [" + srdiMsg.getPrimaryKey() + "] key [" + entry.key + "] value [" + entry.value + "] exp ["
                        + entry.expiration + "]");
                }
            }
        }
        srdi.replicateEntries(srdiMsg);
        return true;
    }

    /**
     *  {@inheritDoc}
     */
    public void messageSendFailed(PeerID peerid, OutgoingMessageEvent e) {
        if (srdiIndex != null) {
            srdiIndex.remove(peerid);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void pushEntries(boolean all) {

        pushSrdi(null, PEER, all);
        pushSrdi(null, GROUP, all);
        pushSrdi(null, ADV, all);
    }

    /**
     * push srdi entries
     *
     *@param all if true push all entries, otherwise just deltas
     */
    protected void pushSrdi(ID peer, int type, boolean all) {

        List entries;

        if (all) {
            entries = cm.getEntries(dirname[type], true);
        } else {
            entries = cm.getDeltas(dirname[type]);
        }

        if (!entries.isEmpty()) {
            SrdiMessage srdiMsg;

            try {
                srdiMsg = new SrdiMessageImpl(group.getPeerID(), 1, // ttl of 1, ensure it is replicated
                                              dirname[type], entries);

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Pushing " + entries.size() + (all ? " entries" : " deltas") + " of type " + dirname[type]);
                }
                srdi.pushSrdi(peer, srdiMsg);
            } catch (Exception e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Exception pushing SRDI Entries", e);
                }
            }
        } else {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No" + (all ? " entries" : " deltas") + " of type " + dirname[type] + " to push");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void rendezvousEvent(RendezvousEvent event) {

        int theEventType = event.getType();

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("[" + group.getPeerGroupName() + "] Processing " + event);
        }

        switch (theEventType) {

        case RendezvousEvent.RDVCONNECT:
        case RendezvousEvent.RDVRECONNECT:
            // start tracking deltas
            cm.setTrackDeltas(true);
            break;

        case RendezvousEvent.CLIENTCONNECT:
        case RendezvousEvent.CLIENTRECONNECT:
            break;

        case RendezvousEvent.RDVFAILED:
        case RendezvousEvent.RDVDISCONNECT:
            // stop tracking deltas until we connect again
            cm.setTrackDeltas(false);
            break;

        case RendezvousEvent.CLIENTFAILED:
        case RendezvousEvent.CLIENTDISCONNECT:
            break;

        case RendezvousEvent.BECAMERDV:
            beRendezvous();
            break;

        case RendezvousEvent.BECAMEEDGE:
            beEdge();
            break;

        default:
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("[" + group.getPeerGroupName() + "] Unexpected RDV event : " + event);
            }
            break;
        }
    }

    /**
     *  this used internally to insure we use a locally (and the current session)
     *  unique query id
     *
     * @return    next query id to use
     */
    private synchronized static int nextQid() {
        return qid++;
    }

    /**
     *  Checks to see if the local peer advertisement has been updated and if
     *  it has then republish it to the CM.
     **/
    private void checkUpdatePeerAdv() {
        PeerAdvertisement newPadv = group.getPeerAdvertisement();
        int newModCount = newPadv.getModCount();

        boolean updated = false;
        synchronized(checkPeerAdvLock) {
            if ((lastPeerAdv != newPadv) || (lastModCount < newModCount) ) {
                lastPeerAdv = newPadv;
                lastModCount = newModCount;
                updated = true;
            }

            if( updated ) {
                // Publish the local Peer Advertisement
                try {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("publishing local advertisement");
                    }

                    // This is our own; we can publish it for a long time in our cache
                    publish(newPadv, INFINITE_LIFETIME, DEFAULT_EXPIRATION);
                } catch (Exception ignoring) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not publish local peer advertisement: ", ignoring);
                    }
                }
            }
        }
    }

    /**
     * Change the behavior to be an rendezvous Peer Discovery Service.
     * If the Service was acting as an Edge peer, cleanup.
     */
    private synchronized void beRendezvous() {

        if (isRdv && (srdi != null || srdiIndex != null)) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Already a rendezvous -- No Switch is needed");
            }
            return;
        }

        isRdv = true;

        // rdv peers do not need to track deltas
        cm.setTrackDeltas(false);

        if (srdiIndex == null) {
            srdiIndex = new SrdiIndex(group, srdiIndexerFileName);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("srdiIndex created");
            }

        }

        // Kill SRDI, create a new one.
        if (srdi != null) {
            srdi.stop();
            if (srdiThread != null) {
                srdiThread = null;
            }
            srdi = null;
        }

        if (!localonly) {
            srdi = new Srdi(group, handlerName, this, srdiIndex, initialDelay, runInterval);
            resolver.registerSrdiHandler(handlerName, this);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("srdi created, and registered as an srdi handler ");
            }
        }

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Switched to Rendezvous peer role.");
        }
    }

    /**
     * Change the behavior to be an Edge Peer Discovery Service.
     * If the Service was acting as a Rendezvous, cleanup.
     */
    private synchronized void beEdge() {

        // make sure we have been here before
        if (!isRdv && srdiThread != null) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Already an Edge peer -- No Switch is needed.");
            }
            return;
        }

        isRdv = false;
        if (rendezvous.getConnectedRendezVous().hasMoreElements()) {
            // if we have a rendezvous connection track deltas, otherwise wait
            // for a connect event to set this option
            cm.setTrackDeltas(true);
        }
        if (srdiIndex != null) {
            srdiIndex.stop();
            srdiIndex = null;
            resolver.unregisterSrdiHandler(handlerName);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("stopped cache and unregistered from resolver");
            }
        }

        // Kill SRDI
        if (srdi != null) {
            srdi.stop();
            if (srdiThread != null) {
                srdiThread = null;
            }
            srdi = null;
        }

        if (!localonly) {
            // Create a new SRDI
            srdi = new Srdi(group, handlerName, this, null, initialDelay, runInterval);

            // only edge peers distribute srdi
            srdiThread = new Thread(group.getHomeThreadGroup(), srdi, "Discovery Srdi Thread");
            srdiThread.setDaemon(true);
            srdiThread.start();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Started SRDIThread");
            }
        }

        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Switched to a Edge peer role.");
        }
    }
}

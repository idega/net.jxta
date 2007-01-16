/*
 *
 * $Id: RelayClient.java,v 1.1 2007/01/16 11:02:03 thomas Exp $
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

package net.jxta.impl.endpoint.relay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.protocol.RelayConfigAdv;


/**
 * RelayClient manages the relationship with the RelayServer(s)
 *
 */
public class RelayClient implements MessageReceiver, Runnable {

    /**
     *  Log4J Logger
     **/
    private final static Logger LOG = Logger.getLogger(RelayClient.class.getName());
    
    private final static long DEFAULT_EXPIRATION = 20L * TimeUtils.AMINUTE;
    private final static long DAY_EXPIRATION = TimeUtils.ADAY;
    
    private final PeerGroup group;
    private final String serviceName;
    private EndpointService endpoint;
    private final EndpointAddress publicAddress;
    private final String groupName;
    private final String peerId;

    private final int maxServers;
    private final long leaseLengthToRequest;
    private final long messengerPollInterval;

    private Thread thread = null;
    
    private volatile boolean closed = false;
    
    /**
     *  <ul>
     *      <li>Values are {@link net.jxta.peergroup.PeerGroup}.</li>
     *  </ul>
     **/
    private final List activeRelayListeners = new ArrayList();
    
    /**
     *  <ul>
     *      <li>Keys are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *      <li>Values are {@link net.jxta.protocol.RouteAdvertisement}.</li>
     *  </ul>
     **/
    private final Map activeRelays = new Hashtable();
    
    /**
     *  <ul>
     *      <li>Values are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *  </ul>
     **/
    private final Set seedRelays = new HashSet();
    
    /**
     *  URIs from which we will load seeds.
     *  
     *  <ul>
     *      <li>Values are {@link java.net.URI}.</li>
     *  </ul>
     **/
    private final Set seedingURIs = new HashSet();
    
    /**
     *  Seeds loaded from the seeding URIs.
     *
     *  <ul>
     *      <li>Values are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *  </ul>
     **/
    private final Set seededRelays = new HashSet();

    protected RelayServerConnection currentServer = null;
    
    public RelayClient(PeerGroup group, String serviceName, RelayConfigAdv relayConfig) {
        this.group = group;
        this.groupName = group.getPeerGroupID().getUniqueValue().toString();
        
        this.serviceName = serviceName;
        
        maxServers = (-1 != relayConfig.getMaxRelays()) ? relayConfig.getMaxRelays() : RelayTransport.DEFAULT_MAX_SERVERS;
        leaseLengthToRequest = (-1 != relayConfig.getClientLeaseDuration()) ? relayConfig.getClientLeaseDuration() : RelayTransport.DEFAULT_LEASE;
        messengerPollInterval = (-1 != relayConfig.getMessengerPollInterval())
                ? relayConfig.getMessengerPollInterval()
                : RelayTransport.DEFAULT_POLL_INTERVAL;
        seedRelays.addAll( Arrays.asList(relayConfig.getSeedRelays()) );
        seedingURIs.addAll( Arrays.asList(relayConfig.getSeedingURIs()) );
        
        // sanity check
        
        peerId = group.getPeerID().getUniqueValue().toString();
        publicAddress = new EndpointAddress(RelayTransport.protocolName, peerId, null, null);
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Relay Client");
            
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : " + group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : " + group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : " + group.getPeerID());
            
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tService Name : " + serviceName);
            configInfo.append("\n\t\tPublic Address : " + publicAddress);
            configInfo.append("\n\t\tMax Relay Servers : " + maxServers);
            configInfo.append("\n\t\tMax Lease Length : " + leaseLengthToRequest + "ms.");
            configInfo.append("\n\t\tMessenger Poll Interval : " + messengerPollInterval + "ms.");
            configInfo.append("\n\t\tSeed Relays : ");
            
            Iterator eachRelay = Arrays.asList(seedRelays.toArray()).iterator();
            
            if (!eachRelay.hasNext()) {
                configInfo.append("\n\t\t\t(none defined)");
            }
            
            while (eachRelay.hasNext()) {
                configInfo.append("\n\t\t\t" + eachRelay.next());
            }
            
            LOG.info(configInfo);
        }
    }
    
    public synchronized boolean startClient() {
        endpoint = group.getEndpointService();

        if (endpoint.addMessageTransport(this) == null) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Transport registration refused");
            }
            return false;
        }

        // start the client thread
        thread = new Thread(group.getHomeThreadGroup(), this, "Relay Client Worker Thread for " + publicAddress);
        thread.setDaemon(true);
        thread.start();

         if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Started client : " + publicAddress.toString());
        }
        
       return true;
    }
    
    public synchronized void stopClient() {
        if( closed ) {
            return;
        }
        
        closed = true;

        endpoint.removeMessageTransport(this);
        
        // make sure the thread is not running
        Thread tempThread = thread;

        thread = null;
        if (tempThread != null) {
            tempThread.interrupt();
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Stopped client : " + publicAddress.toString());
        }
        
    }
    
    /**
     * {@inheritDoc}
     **/
    public Iterator getPublicAddresses() {
        EndpointAddress addr = null;
        
        if (publicAddress != null) {
            addr = (EndpointAddress) publicAddress.clone();
        }
        
        return Collections.singletonList(addr).iterator();
    }
    
    /**
     * {@inheritDoc}
     **/
    public String getProtocolName() {
        return RelayTransport.protocolName;
    }
    
    /**
     * {@inheritDoc}
     **/
    public EndpointService getEndpointService() {
        return endpoint;
    }
    
    /**
     * {@inheritDoc}
     **/
    public Object transportControl(Object operation, Object Value) {
        return null;
    }

    /**
     *  Logic for the relay client
     *
     *  <ol>
     *      <li>Pick a relay server to try</li>
     *      <li>try getting a messenger to relay server, if can not get messenger, start over</li>
     *      <li>use the messenger to send a connect message</li>
     *     <li> wait for a response, if there is no response or a disconnect response, start over</li>
     *      <li>while still connected
     *          <ol>
     *          <li>renew the lease as needed and keep the messenger connected</li>
     *          <ol></li>
     *  </ol>
     *
     *  <p/>FIXME 20041102 bondolo The approach used here is really, really
     *  stupid. The calls to <code>connectToRelay()</code> will not return if a
     *  connection to a relay is achieved. This makes continued iteration over
     * seeds after return incredibly silly. <code>connectToRelay()</code> only
     *  returns when it can <b>NO LONGER CONNECT</b> to the relay. The only
     *  hack I can think of to subvert this is to stop iteration of advs/seeds
     *  if <code>connectToRelay()</code> takes a long time. bizarre.
     **/
    public void run() {
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Start relay client thread");
        }
        
        try {
            long nextReSeedAt = 0;
            long nextSeedAt = 0;
            long nextDiscoveryAt = 0;
            
            // run until the service is stopped
            while ( !closed ) {
                long nextConnectAttemptAt = Math.min( nextSeedAt, nextDiscoveryAt );
                long untilNextConnectAttempt = TimeUtils.toRelativeTimeMillis( nextConnectAttemptAt );
                
                if( untilNextConnectAttempt > 0 ) {
                    try {
                        Thread.sleep(untilNextConnectAttempt);
                    } catch (InterruptedException e) {
                        // ignore interrupted exception
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Thread Interrupted ", e);
                        }
                        
                        continue;
                    }
                }
                
                if( TimeUtils.toRelativeTimeMillis( nextDiscoveryAt ) < 0 ) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Check discovery for Relay Advertisement");
                    }
                    
                    DiscoveryService discovery = group.getDiscoveryService();
                    
                    if( null == discovery ) {
                        continue;
                    }
                    
                    Enumeration advEnum;
                    
                    try {
                        advEnum = discovery.getLocalAdvertisements(DiscoveryService.ADV, RdvAdvertisement.ServiceNameTag, serviceName);
                    } catch (IOException e) {
                        // ignore IOException
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Failure loading Relay Advertisements", e);
                        }
                        continue;
                    }
                    
                    while (advEnum.hasMoreElements() && !closed) {
                        Object obj = advEnum.nextElement();
                        
                        if (obj instanceof RdvAdvertisement) {
                            RdvAdvertisement relayAdv = (RdvAdvertisement) obj;
                            
                            // sanity check
                            if (!serviceName.equals(relayAdv.getServiceName())) {
                                continue;
                            }
                            
                            while (relayAdv != null) {
                                relayAdv = connectToRelay(new RelayServerConnection(this, relayAdv));
                            }
                        }
                    }
                    
                    nextDiscoveryAt = TimeUtils.toAbsoluteTimeMillis( 10 * TimeUtils.ASECOND );
                    continue;
                }
                
                if( TimeUtils.toRelativeTimeMillis( nextSeedAt ) < 0 ) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Use the seed relay servers");
                    }
                    
                    List allSeeds = new ArrayList( seedRelays );
                    
                    if( TimeUtils.toRelativeTimeMillis( nextReSeedAt ) < 0 ) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Loading seeds froms seeding URIs");
                        }
                        
                        seededRelays.clear();
                        
                        Iterator allSeedingURIs = seedingURIs.iterator();
                        while(allSeedingURIs.hasNext()) {
                            URI aURI = (URI) allSeedingURIs.next();
                            try {
                                seededRelays.addAll(Arrays.asList(loadSeeds(aURI)));
                            } catch( IOException failed ) {
                                if (LOG.isEnabledFor(Level.WARN)) {
                                    LOG.warn("Failed loading seeding list from : " + aURI );
                                }
                            }
                        }
                        
                        // We try not to reseed very often.
                        nextReSeedAt = TimeUtils.toAbsoluteTimeMillis( 20 * TimeUtils.AMINUTE );
                    }
                    
                    allSeeds.addAll( seededRelays );
                    Collections.shuffle( allSeeds );
                    Iterator allSeedRelays = allSeeds.iterator();
                    
                    while ( allSeedRelays.hasNext() && !closed ) {
                        EndpointAddress aSeed = new EndpointAddress( (EndpointAddress) allSeedRelays.next(), serviceName, peerId );
                        
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Attempting relay connect to : " + aSeed );
                        }
                        
                        RdvAdvertisement relayAdv = connectToRelay(new RelayServerConnection(this, aSeed));
                        
                        if (relayAdv != null) {
                            // FIXME: jice@jxta.org - 20030206 : we rely on
                            // connectToRelay() to return null when we are supposed
                            // to leave. We should rather check.
                            
                            while (relayAdv != null) {
                                relayAdv = connectToRelay(new RelayServerConnection(this, relayAdv));
                            }
                            
                            // since there should be at least one Relay Advertisement published,
                            // do not use seed relay servers any more
                            break;
                        }
                    }
                    
                    nextSeedAt = TimeUtils.toAbsoluteTimeMillis( 30 * TimeUtils.ASECOND );
                    continue;
                }
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        } finally {
            thread = null;
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("stop client thread");
            }
        }
    }
    
    protected boolean isRelayConnectDone() {
        return (thread == null || Thread.currentThread() != thread);
    }
    
    /**
     *  @param  server  The relay server to connect to
     *  @return The advertisement of an alternate relay server to try.
     **/
    RdvAdvertisement connectToRelay(RelayServerConnection server) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Connecting to " + server);
        }
        
        RdvAdvertisement alternateRelayAdv = null;
        
        // make this the current server
        currentServer = server;
        
        // try getting a messenger to the relay peer
        if (server.createMessenger(leaseLengthToRequest) == false) {
            return alternateRelayAdv;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("got messenger " + server);
        }
        
        // check the peerId of the relay peer
        if (server.logicalAddress != null && "jxta".equals(server.logicalAddress.getProtocolName())) {
            server.peerId = server.logicalAddress.getProtocolAddress();
        }
        
        // make sure that the peerId was found.
        if (server.peerId == null) {
            if (server.messenger != null) {
                server.sendDisconnectMessage();
                server.messenger.close();
            }
            return alternateRelayAdv;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("got peerId " + server);
        }
        
        synchronized (this) {
            // wait for a response from the server
            // There is no real damage other than bandwidth usage in sending
            // a message on top of the connection request, so we realy do not
            // wait very long before doing it.
            long requestTimeoutAt = TimeUtils.toAbsoluteTimeMillis(5 * TimeUtils.ASECOND);

            while (currentServer != null && currentServer.leaseLength == 0 && !isRelayConnectDone()) {
                long waitTimeout = requestTimeoutAt - System.currentTimeMillis();

                if (waitTimeout <= 0) {
                    // did not receive the response in time ?
                    break;
                }
                
                try {
                    wait(waitTimeout);
                } catch (InterruptedException e) {
                    // ignore interrupt
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("wait got interrupted early ", e);
                    }
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("wait done");
                }
            }
        }
        
        if (currentServer == null) {
            return server.alternateRelayAdv;
        }
        
        if (isRelayConnectDone()) {
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer = null;
            return server.alternateRelayAdv;
        }
        
        // If we did not get a lease in the first 5 secs, maybe it is because
        // the server knows us from a previous session. Then it will wait for
        // a lease renewal message before responding, not just the connection.
        // Send one and wait another 15.
        if (currentServer.leaseLength == 0) {
            
            currentServer.sendConnectMessage(leaseLengthToRequest);
            
            synchronized (this) {
                
                // wait for a response from the server
                long requestTimeoutAt = TimeUtils.toAbsoluteTimeMillis(15 * TimeUtils.ASECOND);

                while (currentServer != null && currentServer.leaseLength == 0 && !isRelayConnectDone()) {
                    long waitTimeout = requestTimeoutAt - System.currentTimeMillis();

                    if (waitTimeout <= 0) {
                        // did not receive the response in time ?
                        break;
                    }
                    
                    try {
                        wait(waitTimeout);
                    } catch (InterruptedException e) {
                        // ignore interrupt
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("wait got interrupted early ", e);
                        }
                    }
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("wait done");
                    }
                }
            }
        }
        
        // If we had a messenger but are going to give up that relay server because it is
        // not responsive or rejected us. Make sure that the messenger is closed.
        if (currentServer == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("did not get connect from " + server);
            }
            // return any alternate relay advertisements
            return server.alternateRelayAdv;
        }
        
        if (currentServer.relayAdv == null || currentServer.leaseLength == 0 || isRelayConnectDone()) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("did not get connect from " + server);
            }
            if (currentServer.messenger != null) {
                currentServer.sendDisconnectMessage();
                currentServer.messenger.close();
            }
            currentServer = null;
            
            // return any alternate relay advertisements
            return server.alternateRelayAdv;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Connected to " + server);
        }
        
        RouteAdvertisement holdAdv = server.relayAdv.getRouteAdv();
        EndpointAddress holdDest = server.logicalAddress;
        
        // register this relay server
        addActiveRelay(holdDest, holdAdv);
        
        // maintain the relay server connection
        alternateRelayAdv = maintainRelayConnection(server);
        
        // unregister this relay server
        removeActiveRelay(holdDest, holdAdv);
        
        return alternateRelayAdv;
    }
    
    // FIXME: jice@jxta.org 20030212. This is junk code: that should be a
    // method of RelayServerConnection and at least not refer to currentServer
    // other than to assign the reference.
    protected RdvAdvertisement maintainRelayConnection(RelayServerConnection server) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("maintainRelayConnection() start " + currentServer);
        }
        
        if (server == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("RelayConnection() failed at start " + currentServer);
            }
            return null;
        }
        
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            long renewLeaseAt = currentServer.leaseObtainedAt + currentServer.leaseLength / 3;
            long waitTimeout = 0;
            
            // This will be true if we need to do the first lease renewal early
            // (that is at the time of the next connection check).
            // We'll do that if we did not know the relay server's adv (seed).
            // In that case we told the relay server to send us its own
            // adv, else we told it to send us some alternate adv (we have to
            // chose). In the former case, we want to do a lease connect
            // request soon so that the server has an opportunity to send us
            // the alternate adv that we did not get during initial connection.
            
            boolean earlyRenew = currentServer.seeded;
            
            while (currentServer != null && !isRelayConnectDone()) {
                // calculate how long to wait
                waitTimeout = renewLeaseAt - currentTime;
                
                // check that the waitTimeout is not greater than the messengerPollInterval
                // We want to make sure that we poll. Most of the time it cost nothing.
                // Also, if we urgently need to renew our lease we may wait
                // less, but if we fail to get our lease renewed in time, the
                // delay may become negative. In that case we do not want
                // to start spinning madly. The only thing we can do is just
                // wait some arbitrary length of time for the lease to be
                // renewed. (If that gets badly overdue, we should probably
                // give up on that relay server, though).
                if (waitTimeout > messengerPollInterval || waitTimeout < 0) {
                    waitTimeout = messengerPollInterval;
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("waitTimeout=" + waitTimeout + " server=" + currentServer);
                }
                
                try {
                    wait(waitTimeout);
                } catch (InterruptedException e) {
                    // ignore interrupt
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("wait got interrupted early ", e);
                    }
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("wait done, server=" + currentServer);
                }
                
                // make sure the server did not disconnect while waiting
                if (currentServer == null) {
                    break;
                }
                
                // get the current time
                currentTime = System.currentTimeMillis();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("check messenger " + currentServer);
                }
                
                // check if the messenger is still open
                if (currentServer.messenger.isClosed()) {
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Server connection broken");
                    }
                    
                    // See if we can re-open, that happens often.
                    // That's a reason to renew the connection,
                    // Not a reason to give up on the server yet.
                    // Note we do not renew the lease. This is a transient
                    // and if the server forgot about us, it will respond
                    // to the connection alone. Otherwise, we'd rather avoid
                    // getting a response, since in some cases http connections
                    // close after each received message.
                    if (!currentServer.createMessenger(currentServer.leaseLength)) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Server connection NOT re-established");
                        }
                        // lost connection to relay server
                        currentServer = null;
                        break;
                    }
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Server connection re-established");
                    }
                    
                    // getMessenger asks for a new lease.
                    // In the meantime, we'll just assume our old lease is
                    // still current and that the messenger breakage was just
                    // a transient.
                    if (!isRelayConnectDone()) {
                        continue;
                    }
                }
                
                // We've been asked to leave. Be nice and tell the
                // server about it.
                if (isRelayConnectDone()) {
                    break;
                }
                
                // check if the lease needs to be renewed
                renewLeaseAt = currentServer.leaseObtainedAt + currentServer.leaseLength / 3;
                if (currentTime >= renewLeaseAt || earlyRenew) {
                    
                    earlyRenew = false;
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("renew lease " + currentServer);
                    }
                    
                    // If we do not receive any response to our lease renewals
                    // (that is the response is overdue badly), then we give
                    // up and try another relayServer. We give up after 4 minutes
                    // because if we go as far as 5 we start overshooting other
                    // timeouts such as the local peer becoming a rdv in a sub-group.
                    // This later timeout is usually set to 5 minutes or more.
                    
                    if ((currentTime > currentServer.leaseObtainedAt + currentServer.leaseLength / 3 + 4 * TimeUtils.AMINUTE)
                            || (currentServer.sendConnectMessage(leaseLengthToRequest) == false)) {
                        
                        if (LOG.isEnabledFor(Level.INFO)) {
                            LOG.info("renew lease failed" + currentServer);
                        }
                        if (currentServer.messenger != null) {
                            currentServer.messenger.close();
                        }
                        currentServer.messenger = null;
                        currentServer.peerId = null;
                        currentServer.leaseLength = 0;
                        currentServer.leaseObtainedAt = 0;
                        currentServer.relayAdv = null;
                        currentServer = null;
                        break;
                    }
                }
            }
        }
        
        if (isRelayConnectDone() && currentServer != null) {
            currentServer.sendDisconnectMessage();
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer.messenger = null;
            currentServer.peerId = null;
            currentServer.leaseLength = 0;
            currentServer.leaseObtainedAt = 0;
            currentServer.relayAdv = null;
            // Make sure that we will not suggest an alternate
            // since we're asked to terminate.
            currentServer.alternateRelayAdv = null;
            
            currentServer = null;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("maintainRelayConnection() terminated " + currentServer);
        }
        
        return server.alternateRelayAdv;
    }
    
    protected synchronized void handleResponse(Message message, EndpointAddress dstAddr) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleResponse " + currentServer);
        }
        
        // ignore all responses if there is not a current server
        if (currentServer == null) {
            return;
        }
        
        // get the request, make it lowercase so that case is ignored
        String response = RelayTransport.getString(message, RelayTransport.RESPONSE_ELEMENT);

        if (response == null) {
            return;
        }
        response = response.toLowerCase();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("response = " + response);
        }
        
        // check if a relay advertisement was included
        RdvAdvertisement relayAdv = null;
        
        MessageElement advElement = message.getMessageElement(RelayTransport.RELAY_NS, RelayTransport.RELAY_ADV_ELEMENT);
        
        if (null != advElement) {
            try {
                Advertisement adv = AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, advElement.getStream());
                
                if (adv instanceof RdvAdvertisement) {
                    relayAdv = (RdvAdvertisement) adv;
                }
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Could not read Relay RdvAdvertisement", e);
                }
            }
        }
        
        if (relayAdv != null) {
            // publish the relay advertisement for future use
            // NOTE: relayAdvs like padv include routes that do not have
            // a PID in the dest (because it is redundant)
            // We do publish it that way, but as soon as it's published, we
            // go and ADD the destPID in the embedded route because we'll
            // fetch that route adv for various purposes and it has to be
            // able to stand alone...this missing PID business is rather ugly.
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Got relay adv for : " + relayAdv.getPeerID());
            }

            try {
                DiscoveryService discovery = group.getDiscoveryService();

                if (discovery != null) {
                    discovery.publish(relayAdv, DAY_EXPIRATION, DAY_EXPIRATION);
                }
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not publish Relay RdvAdvertisement", e);
                }
            }
        }
        
        // WATCHOUT: this is not a pid, just the unique string portion.
        String serverPeerId = dstAddr.getServiceParameter();
        
        // only process the request if a client peer id was sent
        if (serverPeerId == null) {
            return;
        }
        
        // ignore all responses that are not from the current server
        if (!serverPeerId.equals(currentServer.peerId)) {
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("serverPeerId = " + serverPeerId);
        }
        
        // Figure out which response it is
        if (RelayTransport.CONNECTED_RESPONSE.equals(response)) {
            // Connect Response
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("connected response for " + currentServer);
            }
            
            String responseLeaseString = RelayTransport.getString(message, RelayTransport.LEASE_ELEMENT);
            
            long responseLease = 0;
            
            if (responseLeaseString != null) {
                try {
                    responseLease = Long.parseLong(responseLeaseString);
                } catch (NumberFormatException e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("could not parse response lease string", e);
                    }
                }
            }
            
            // make sure the lease is valid
            if (responseLease <= 0) {
                // invalid lease value
                return;
            }
            
            // update the lease values
            currentServer.leaseLength = responseLease;
            currentServer.leaseObtainedAt = System.currentTimeMillis();
            
            // Since we got the lease, if we requested a queue flush, it's
            // now done. We never send it with a new messenger creation, but
            // when the server already has us as a client it does not respond
            // to connections through messenger creation, so we're sure we
            // will have to send an explicit connect message before we get
            // a response. So, we're sure it's done if it was needed.
            currentServer.flushNeeded = false;
            
            if (relayAdv != null) {
                // Set it only if it is the server's own. Else it got
                // published. Still set alternateRelayAdv so that we
                // can return something that could be usefull when this
                // connection breaks.
                PeerID pidOfAdv = relayAdv.getPeerID();
                String pidOfAdvUnique = pidOfAdv.getUniqueValue().toString();

                if (currentServer.peerId.equals(pidOfAdvUnique)) {
                    currentServer.relayAdv = relayAdv;
                    // Fix the embedded route adv !
                    currentServer.relayAdv.getRouteAdv().setDestPeerID(pidOfAdv);
                } else {
                    currentServer.alternateRelayAdv = relayAdv;
                }
            }
            
            notifyAll();
            
        } else if (RelayTransport.DISCONNECTED_RESPONSE.equals(response)) {
            // Disconnect Response
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("disconnected from " + currentServer);
            }
            
            // If our request was denied, the adv that came back is
            // always an alternate one.
            currentServer.alternateRelayAdv = relayAdv;
            
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer.messenger = null;
            currentServer.peerId = null;
            currentServer.leaseLength = 0;
            currentServer.leaseObtainedAt = 0;
            currentServer.relayAdv = null;
            currentServer = null;
            notifyAll();
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("response handled for " + currentServer);
        }
    }
    
    private static class RelayServerConnection {
        final RelayClient client;
        
        Messenger messenger = null;
        EndpointAddress logicalAddress = null;
        String peerId = null;
        long leaseLength = 0;
        long leaseObtainedAt = 0;
        
        // If seeded out of a raw address, we have relayAddress.
        // relayAdv comes only later.
        RdvAdvertisement relayAdv = null;
        EndpointAddress relayAddress = null;
        
        RdvAdvertisement alternateRelayAdv = null;
        boolean seeded = false;
        boolean flushNeeded = true; // true until we know it's been done
        
        protected RelayServerConnection(RelayClient client, EndpointAddress addr) {
            this.client = client;
            relayAddress = new EndpointAddress(addr, null, null);
            seeded = true;
        }
        
        protected RelayServerConnection(RelayClient client, RdvAdvertisement relayAdv) {
            this.client = client;
            this.relayAdv = relayAdv;
            relayAdv.getRouteAdv().setDestPeerID(relayAdv.getPeerID());
        }
        
        protected boolean createMessenger(long leaseLengthToRequest) {
            
            // make sure the old messenger is closed
            if (messenger != null) {
                messenger.close();
                messenger = null;
            }
            
            List endpointAddresses = null;
            
            // check for a relay advertisement
            if (relayAdv != null) {
                RouteAdvertisement routeAdv = relayAdv.getRouteAdv();

                if (routeAdv != null) {
                    AccessPointAdvertisement accessPointAdv = routeAdv.getDest();

                    if (accessPointAdv != null) {
                        endpointAddresses = accessPointAdv.getVectorEndpointAddresses();
                    }
                }
            } else {
                // silly but if we use getVetorEndpointAddresses, we get
                // strings. It's realy simpler to have only one kind of obj
                // inthere.
                endpointAddresses = new ArrayList(1);
                endpointAddresses.add(relayAddress.toString());
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("createMessenger to " + endpointAddresses);
            }
            
            // make sure we found some endpoint addresses to try
            if (endpointAddresses == null) {
                return false;
            }
            
            // try each endpoint address until one is successful
            for (int i = 0; i < endpointAddresses.size(); i++) {
                String s = (String) endpointAddresses.get(i);

                if (s == null) {
                    continue;
                }
                EndpointAddress addr = new EndpointAddress(s);
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("find transport for " + addr);
                }
                
                // get the list of messengers on this endpoint
                Iterator transports = client.endpoint.getAllMessageTransports();
                
                while (transports.hasNext() && messenger == null) {
                    MessageTransport transport = (MessageTransport) transports.next();
                    
                    // only try transports that are senders and allow routing
                    if (transport instanceof MessageSender && ((MessageSender) transport).allowsRouting()) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("try transport " + transport);
                        }
                        
                        if (addr.getProtocolName().equals(transport.getProtocolName())) {
                            // NOTE: here we're creating a messenger.
                            // For risk management reason, we refrain from
                            // including the flush request at this time in
                            // this. There is the possibility that the
                            // connection will be repeatedly established
                            // by the transport in our bakck, and would keep
                            // including the flush request ! Normaly this
                            // does not matter because the server should
                            // disregard it when it come in that way, but
                            // still, let's be defensive. We will still send
                            // the flush in a subsequent explicit message.
                            String reqStr = RelayTransport.createConnectString(leaseLengthToRequest, (relayAdv == null), false);
                            
                            // NOTE: this is simulating address mangling by CrossgroupMessenger.
                            // The real service param is after the "/" in the below serviceParam arg.
                            EndpointAddress addrToUse = new EndpointAddress(addr, "EndpointService:" + client.groupName,
                                    client.serviceName + "/" + reqStr);

                            messenger = ((MessageSender) transport).getMessenger(addrToUse, null);
                            if (messenger != null && messenger.isClosed()) {
                                messenger = null;
                            }
                            if (messenger != null) {
                                logicalAddress = messenger.getLogicalDestinationAddress();
                                // We're using a known adv, which means that
                                // we did not ask to get the adv back.
                                // Make sure that we do not keep going with
                                // an adv for the wrong peer. That can happen.
                                if (relayAdv != null && !addr2pid(logicalAddress).equals(relayAdv.getPeerID())) {
                                    // oops, wrong guy !
                                    messenger.close();
                                    messenger = null;
                                    logicalAddress = null;
                                }

                                // In case it was not given, set relayAddress
                                // for toString purposes.
                                relayAddress = addr;
                            }
                        }
                    }
                }
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("messenger=" + messenger);
            }
            
            return (messenger != null);
        }
        
        protected boolean sendConnectMessage(long leaseLengthToRequest) {
            if (messenger == null || messenger.isClosed()) {
                return false;
            }
            
            Message message = RelayTransport.createConnectMessage(leaseLengthToRequest, (relayAdv == null), flushNeeded);
            
            try {
                messenger.sendMessage(message, "EndpointService:" + client.groupName, client.serviceName + "/" + client.peerId);
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("could not send connect message", e);
                }
                
                // connection attempt failed
                return false;
            }
            
            return true;
        }
        
        protected boolean sendDisconnectMessage() {
            if (messenger == null || messenger.isClosed()) {
                return false;
            }
            
            Message message = RelayTransport.createDisconnectMessage();
            
            try {
                messenger.sendMessage(message, "EndpointService:" + client.groupName, client.serviceName + "/" + client.peerId);
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("could not send disconnect message", e);
                }
                
                // connection attempt failed
                return false;
            }
            
            return true;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public String toString() {
            
            return
                    ((relayAddress == null) ? "(adv to " + relayAdv.getPeerID() + ")" : relayAddress.toString()) + " [" + leaseLength + ", "
                    + leaseObtainedAt + "] ";
        }
    }
    
    /**
     * Register an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     **/
    public synchronized boolean addActiveRelayListener(Object service) {
        
        boolean added = false;
        
        if (!activeRelayListeners.contains(service)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Register group to relay connection " + ((PeerGroup) service).getPeerGroupName());
            }
            
            activeRelayListeners.add(service);
            
            added = true;
        }
        
        return added;
    }
    
    /**
     * Unregister an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     **/
    public synchronized boolean removeActiveRelayListener(Object service) {
        activeRelayListeners.remove(service);
        
        return true;
    }
    
    /**
     * Notify of a new relay connection
     *
     **/
    public synchronized boolean addActiveRelay(EndpointAddress address, RouteAdvertisement relayRoute) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("notify add relay connection for " + address);
        }
        
        // need to notify all our listeners
        Iterator e = activeRelayListeners.iterator();

        while (e.hasNext()) {
            PeerGroup pg = (PeerGroup) e.next();

            addRelay(pg, relayRoute);
        }
        
        // maintain the list of active relays
        activeRelays.put(address, relayRoute);
        return true;
    }
    
    /**
     * Notify of a relay connection removal
     *
     **/
    public synchronized boolean removeActiveRelay(EndpointAddress address, RouteAdvertisement relayRoute) {
        
        // need to notify all our listeners
        Iterator e = activeRelayListeners.iterator();

        while (e.hasNext()) {
            PeerGroup pg = (PeerGroup) e.next();

            removeRelay(pg, relayRoute);
        }
        
        activeRelays.remove(address);
        
        return true;
    }
    
    /**
     * Register an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     *
     * @param address address opf the relay to add
     * @return boolean true of false if it suceeded
     **/
    private void addRelay(PeerGroup pg, RouteAdvertisement relayRoute) {
        
        DiscoveryService discovery = pg.getDiscoveryService();

        // Publish the route adv for that relay. This is not done
        // by the relay itself (which cares only for the relay's rdvAdv).
        
        try {

            if (discovery != null) {
                discovery.publish(relayRoute, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
            }
            
        } catch (Throwable theMatter) {
            // Abnormal but not criticial
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("could not publish relay route adv", theMatter);
            }
        }
        
        ID assignedID = PeerGroup.endpointClassID;
        
        try {
            // get the advertisement of the associated endpoint address as we
            // need to get the peer Id and available route
            
            // update our own peer advertisement
            PeerAdvertisement padv = pg.getPeerAdvertisement();
            StructuredDocument myParam = (StructuredDocument) padv.getServiceParam(assignedID);
            
            RouteAdvertisement route = null;

            if (myParam == null) {
                // we should have found a route here. This is not good
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("no route found in peer adv");
                }
                return;
            } else {
                Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                Element param = null;
                
                if (paramChilds.hasMoreElements()) {
                    param = (Element) paramChilds.nextElement();
                }
                
                route = (RouteAdvertisement)
                        AdvertisementFactory.newAdvertisement((TextElement) param);
            }
            
            if (route == null) { // we should have a route here
                return;
            }
            
            // ready to stich the Relay route in our route advertisement
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("found route info for local peer \n" + route.display());
            }
            
            // update the new hops info
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("OLD route info to local peer \n" + route.display());
            }
            
            // If we already have the relay in our list of hops, remove it.
            // The new version can only be more accurate.
            route.removeHop(relayRoute.getDestPeerID());
            
            // Get a hold of the hops list AFTER removing: removeHop
            // rebuilds the vector !
            Vector hops = route.getVectorHops();
            
            // Create the new relay Hop
            hops.add(relayRoute.getDest());
            
            // update the new hops info
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("NEW route info to local peer" + route.display());
            }
            
            // create the new param route
            myParam = StructuredDocumentFactory.newStructuredDocument
                    (MimeMediaType.XMLUTF8, "Parm");
            StructuredTextDocument xptDoc = (StructuredTextDocument)
                    route.getDocument(MimeMediaType.XMLUTF8);

            StructuredDocumentUtils.copyElements(myParam, myParam, xptDoc);
            
            padv.putServiceParam(assignedID, myParam);
            
            // publish the new peer advertisement
            // We could publish the radv itself, but currently
            // radvs are never searched through disco. PeerAdv is a superset.
            // Maybe we'll want to change that one day.
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("exception adding relay route ", ex);
            }
        }
    }
    
    /**
     * remove relay hop from the peer advertisement
     *
     * @param group which peer advertisement needs to be updated
     * @param address address of the relay to be removed
     * @return boolean true if operation succeeded
     */
    private void removeRelay(PeerGroup pg, RouteAdvertisement relayRoute) {
        
        // we can keep the advertisement for now (should remove it)
        // remove the relay from its active list
        ID assignedID = PeerGroup.endpointClassID;
        PeerID relayPid = relayRoute.getDestPeerID();
        
        try {
            // get the advertisement of the associated endpoint address as we
            // need to get the peer Id and available route
            
            PeerAdvertisement padv = null;
            
            // update our peer advertisement
            padv = pg.getPeerAdvertisement();
            StructuredDocument myParam = (StructuredDocument) padv.getServiceParam(assignedID);
            
            RouteAdvertisement route = null;

            if (myParam == null) {
                // no route found we should really have one
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("no route found in peer adv");
                    return;
                }
            } else {
                Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                Element param = null;
                
                if (paramChilds.hasMoreElements()) {
                    param = (Element) paramChilds.nextElement();
                }
                
                route = (RouteAdvertisement)
                        AdvertisementFactory.newAdvertisement((TextElement) param);
            }
            
            if (route == null) {
                return;
            } // we should have a route here
            
            // update the new hops info
            route.removeHop(relayPid);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("new route info to the peer" + route.display());
            }
            
            // create the new param route
            myParam = StructuredDocumentFactory.newStructuredDocument
                    (MimeMediaType.XMLUTF8, "Parm");
            StructuredTextDocument xptDoc = (StructuredTextDocument)
                    route.getDocument(MimeMediaType.XMLUTF8);

            StructuredDocumentUtils.copyElements(myParam, myParam, xptDoc);
            
            padv.putServiceParam(assignedID, myParam);
            
            // publish the new advertisement
            DiscoveryService discovery = pg.getDiscoveryService();

            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Throwable theMatter) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed adding relay route", theMatter);
            }
        }
    }
    
    /**
     * return the list of connected relays
     */
    public Vector getActiveRelays(PeerGroup pg) {
        try {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("get active Relays list");
            }
            
            if (activeRelays.size() != 0) {
                Vector hops = new Vector();

                for (Iterator e = activeRelays.values().iterator(); e.hasNext();) {
                    RouteAdvertisement route = (RouteAdvertisement) e.next();
                    
                    // publish our route if pg is not null
                    if (pg != null) {
                        DiscoveryService discovery = pg.getDiscoveryService();

                        if (discovery != null) {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("publishing route to active relay " + route.display());
                            }
                            discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
                        }
                    }
                    hops.add(route.getDest());
                }
                return hops;
            } else {
                return null;
            }
            
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("error publishing active relay", ex);
            }
            return null;
        }
    }
    
    static EndpointAddress[] loadSeeds( URI seedingURI ) throws IOException {
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
                seedURIs.add( new EndpointAddress(new URI(aSeed)) );
            } catch(URISyntaxException badURI) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("bad URI in seeding list :" + aSeed, badURI );
                }
            } catch(IllegalArgumentException badURI) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("bad URI in seeding list :" + aSeed, badURI );
                }
            }
        }
        
        is.close();
        
        return (EndpointAddress[]) seedURIs.toArray( new EndpointAddress[seedURIs.size()]);
    }
    
    // convert an endpointRouterAddress into a PeerID
    private final static PeerID addr2pid(EndpointAddress addr) {
        try {
            URI asURI = new URI(ID.URIEncodingName, ID.URNNamespace + ":" + addr.getProtocolAddress(), null);

            return (PeerID) IDFactory.fromURI(asURI);
        } catch (Exception ex) {
            return null;
        }
    }
}

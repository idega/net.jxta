/*
 *
 * $Id: EndpointRouter.java,v 1.1 2007/01/16 11:01:48 thomas Exp $
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

package net.jxta.impl.endpoint.router;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.TextElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.endpoint.LoopbackMessenger;
import net.jxta.impl.util.FastHashMap;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.TimerThreadNamer;

import net.jxta.impl.endpoint.IllegalTransportLoopException;

public class EndpointRouter implements EndpointListener,
            MessageReceiver,
            MessageSender,
            MessengerEventListener,
            Module {
    
    /**
     *    Log4J Logger
     **/
    private static transient final Logger LOG = Logger.getLogger(EndpointRouter.class.getName());
    
    /**
     * Until we decide otherwise, the router is *by definition* handling
     * peerID addressed messages.
     **/
    private final static String routerPName = "jxta";
    
    /**
     * Router Service Name
     **/
    public final static String routerSName = "EndpointRouter";
       
    /**
     *  These are peers which we know multi-hop routes for.
     *
     *  <p/><ul>
     *      <li>Key is peer id as a {@link et.jxta.endpoint.EndpointAddress}</li>
     *      <li>value is a {@link Route}.</li>
     *  </ul>
     **/
    private final Map routedRoutes = new FastHashMap(16);
    
    /**
     *  A record of failures.
     *
     *  <p/><ul>
     *      <li>keys are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *      <li>values are the time of failure as {@link java.lang.Long}.</li>
     *  </ul>
     **/
    private final Map triedAndFailed = new HashMap();
    
    /**
     * local peer ID as a endpointAddress.
     **/
    protected EndpointAddress localPeerAddr = null;
    
    /**
     * local Peer ID
     **/
    private ID localPeerId = null;
    
    /**
     * endpointservice handle
     *
     **/
    EndpointService endpoint = null;
    
    /**
     * PeerGroup handle
     **/
    private PeerGroup group = null;
    
    /**
     * Whenever we initiate connectivity to a peer (creating a direct route).
     * we remember that we need to send our route adv to that peer. So that
     * it has a chance to re-establish the connection from its side, if need
     * be. The route adv is actually sent piggy-backed on the first message
     * that goes there.
     *
     * <p>Values are {@link net.jxta.endpoint.EndpointAddress}.
     **/
    private final Set newDestinations = Collections.synchronizedSet(new HashSet());
    
    /**
     * A pool of messengers categorized by logical address.
     * This actually is the direct routes map.
     **/
    private Destinations destinations;
    
    /**
     *  A record of expiration time of known bad routes we received a NACK route
     *
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *      <li>Values are {@link net.jxta.impl.endpoint.router.BadRoute}.</li>
     *  </ul>
     *
     **/
    private final Map badRoutes = new HashMap();
    
    /**
     * We record queries when first started and keep them pending for
     * a while. Threads coming in the meanwhile wait for a result without
     * initiating a query. Thus threads may wait passed the point where
     * the query is no-longer pending, and, although they could initiate
     * a new one, they do not. However, other threads coming later may initiate
     * a new query. So a query is not re-initiated systematically on a fixed schedule.
     * This mechanism also serves to avoid infinite recursions if we're looking
     * for the route to a rendezvous (route queries will try to go there
     * themselves).
     * FIXME: jice@jxta.org 20020903 this is approximate. We can do
     * cleaner/better than that, but it's an inexpensive improvement over what
     * was there before.
     * FIXME: tra@jxta.org 20030818 the pending hashmap should be moved
     * in the routeResolver class as this will allow to only synchronize
     * on the routeResolver object rather than the router object.
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.endpoint.EndpointAddress}.</li>
     *      <li>Values are {@link ClearPendingQuery}.</li>
     *  </ul>
     **/
    protected final Map pendingQueries = new HashMap();
    
    /**
     * Timer by which we schedule the clearing of pending queries.
     **/
    private final Timer timer = new Timer(true);
    
    /**
     *  MAX timeout (seconds) for route discovery after that timeout
     *  the peer will bail out from finding a route
     **/
    private final static long MAXFINDROUTE_TIMEOUT = 60L * TimeUtils.ASECOND;
    
    /**
     * How long do we wait (seconds) before retrying to make a connection
     * to an endpoint destination
     **/
    private final static long MAXASYNC_GETMESSENGER_RETRY = 30L * TimeUtils.ASECOND;
    
    /**
     * PeerAdv tracking.
     * The peer adv is modified every time a new public address is
     * enabled/disabled. One of such cases is the connection/disconnection
     * from a relay. Since these changes are to the embedded route adv
     * and since we may embbed our route adv in messages, we must keep it
     * up-to-date.
     **/
    private PeerAdvertisement lastPeerAdv = null;
    private int lastModCount = -1;
    
    /**
     * Route info for the local peer (updated along with lastPeerAdv).
     **/
    private RouteAdvertisement localRoute = null;
    
    /**
     * Route CM Persistant cache
     **/
    private final RouteCM routeCM;
    
    /**
     * Route Resolver
     **/
    private final RouteResolver routeResolver;
    
    /**
     *  MessageTransport Control operation
     **/
    public final static Integer GET_ROUTE_CONTROL = new Integer(0); // Return RouteControl Object
    public final static int RouteControlOp = 0; // Return RouteControl Object
    
    protected class ClearPendingQuery extends TimerTask {
        EndpointAddress pid;
        volatile boolean failed = false;
        long timeToRetry = 0;
        
        ClearPendingQuery(EndpointAddress pid) {
            this.pid = pid;
            // We schedule for one tick at one minute and another at 5 minutes
            // after the second, we cancel ourselves.
            timer.schedule(this, 1L * TimeUtils.AMINUTE, 5L * TimeUtils.AMINUTE);
            timeToRetry = TimeUtils.toAbsoluteTimeMillis(20L * TimeUtils.ASECOND);
        }
        
        /**
         *  {@inheritDoc}
         **/
        public void run() {
            try {
                if (failed) {
                    // Second tick.
                    // This negative cache info is expired.
                    synchronized (EndpointRouter.this) {
                        pendingQueries.remove(pid);
                    }
                    this.cancel();
                } else {
                    // First timer tick. We're done trying. This is now a negative
                    // cache info. For the next 5 minutes that destination fails
                    // immediately unless it unexpectedly gets finaly resolved.
                    failed = true;
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable in timer task " + Thread.currentThread().getName() + " for " + pid, all);
                }
            }
        }

        public synchronized boolean isTimeToRetry() {
            if (TimeUtils.toRelativeTimeMillis(timeToRetry) > 0) {
                return false;
            }
            // timeToRetry is passed. Set the next time to retry from now.
            timeToRetry = TimeUtils.toAbsoluteTimeMillis(20L * TimeUtils.ASECOND);
            return true;
        }

        public boolean isFailed() {
            return failed;
        }
    }
    
    protected RouteAdvertisement getMyLocalRoute() {
        
        // Update our idea of the local peer adv. If it has change,
        // update our idea of the local route adv.
        // If nothing has changed, do not do any work.
        // In either case, return the local route adv as it is after this
        // refresh.
        
        // Race condition possible but tolerable: if two threads discover
        // the change in the same time, lastPeerAdv and lastModCount
        // could become inconsistent. That'll be straightened out the
        // next time someone looks. The inconsistency can only trigger
        // an extraneous update.
        
        PeerAdvertisement newPadv = group.getPeerAdvertisement();
        int newModCount = newPadv.getModCount();
        
        if ((lastPeerAdv != newPadv) || (lastModCount != newModCount) || (null == localRoute)) {
            lastPeerAdv = newPadv;
            lastModCount = newModCount;
        } else {
            // The current version is good.
            return localRoute;
        }
        
        // Get its EndpointService advertisement
        TextElement endpParam = (TextElement)
                newPadv.getServiceParam(PeerGroup.endpointClassID);
        
        if (endpParam == null) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("getMyLocalRoute: no Endpoint SVC Params");
            }
            
            // Return whatever we had so far.
            return localRoute;
        }
        
        // get the Route Advertisement element
        Enumeration paramChilds = endpParam.getChildren(RouteAdvertisement.getAdvertisementType());
        Element param = null;
        
        if (paramChilds.hasMoreElements()) {
            param = (Element) paramChilds.nextElement();
        } else {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("getMyLocalRoute: no Endpoint Route Adv");
            }
            
            // Return whatever we had so far.
            return localRoute;
        }
        
        // build the new route
        try {
            // Stick the localPeerID in-there, since that was what
            // every single caller of getMyLocalRoute did so far.
            
            RouteAdvertisement route = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement((TextElement) param);

            route.setDestPeerID((PeerID) localPeerId);
            localRoute = route;
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("getMyLocalRoute: error extracting route", ex);
            }
        }
        
        return localRoute;
    }
    
    /**
     *  listener object to synchronize on asynchronous getMessenger
     */
    private static class EndpointGetMessengerAsyncListener implements MessengerEventListener {
        
        volatile boolean hasResponse = false;
        volatile boolean isGone = false;
        private Messenger messenger = null;
        private EndpointRouter router = null;
        private EndpointAddress logDest = null;
        
        /**
         * Constructor
         */
        EndpointGetMessengerAsyncListener(EndpointRouter router, EndpointAddress logDest) {
            this.router = router;
            this.logDest = (EndpointAddress) logDest.clone();
        }
        
        /**
         *  {@inheritDoc}
         **/
        public boolean messengerReady(MessengerEvent event) {
            
            Messenger toClose = null;
            
            synchronized (this) {
                hasResponse = true;
                if (event != null) {
                    messenger = event.getMessenger();
                    if (messenger != null && !messenger.getLogicalDestinationAddress().equals(logDest)) {
                        // Ooops, wrong number !
                        toClose = messenger;
                        messenger = null;
                    }
                } else {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("null messenger event for dest :" + logDest);
                    }
                }
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                if (messenger == null) {
                    LOG.debug("error creating messenger for dest :" + logDest);
                } else {
                    LOG.debug("got a new messenger for dest :" + logDest);
                }
            }
            
            // We had to release the lock on THIS before we can get the lock
            // on the router. (Or face a dead lock - we treat this as a lower
            // level lock)
            
            if (messenger == null) {
                
                if (toClose != null) {
                    toClose.close();
                }
                
                // we failed to get a messenger, we need to update the try and
                // failed as it currently holds an infinite timeout to permit
                // another thread to retry that destination. We only retry
                // every MAXASYNC_GETMESSENGER_RETRY seconds
                
                router.noMessenger(logDest);
                
                synchronized (this) {
                    // Only thing that can happen is that we notify for nothing
                    // We took the lock when updating hasResult, so, the event
                    // will not be missed. FIXME. It would be more logical
                    // to let the waiter do the above if (!isGone) as in
                    // the case of success below. However we'll
                    // minimize changes for risk management reasons.
                    notify();
                }
                return false;
            }
            
            // It worked. Update router cache entry if we have to.
            
            synchronized (this) {
                if (!isGone) {
                    notify(); // Waiter will do the rest.
                    return true;
                }
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("async caller gone add the messenger " + logDest);
            }
            return router.newMessenger(event);
        }
        
        /**
         * Wait on the async call for ASYNC_MESSENGER_WAIT
         * then bailout. The messenger will be added whenever
         * the async getMessenger will return
         */
        public synchronized Messenger waitForMessenger(boolean quick) {
            
            if (!quick) {
                long quitAt = TimeUtils.toAbsoluteTimeMillis(ASYNC_MESSENGER_WAIT);

                while (TimeUtils.toRelativeTimeMillis(quitAt) > 0) {
                    try {
                        // check if we got a response already
                        if (hasResponse) { // ok, we got a response
                            break;
                        }
                        wait(ASYNC_MESSENGER_WAIT);
                    } catch (InterruptedException woken) {
                        Thread.interrupted();
                        break;
                    }
                }
            }
            
            // mark the fact that the caller is bailing out
            isGone = true;
            return messenger;
        }
    }
    
    /**
     * how long we are willing to wait for a response from an async
     * getMessenger. We do not wait long at all because it is non-critical
     * that we get the answer synchronously. The goal is to avoid starting
     * a route discovery if there's a chance to get a direct connection.
     * However, we will still take advantage of the direct route if it is
     * found while we wait for the route discovery result. If that happens,
     * the only wrong is that we used some bandwidth doing a route discovery
     * that wasn't needed after all.
     */
    public final static long ASYNC_MESSENGER_WAIT = 3L * TimeUtils.ASECOND;
    
    /**
     * isLocalRoute is a shalow test. It tells you that there used to be
     * a local route that worked the last time it was tried.
     **/
    protected boolean isLocalRoute(EndpointAddress pId) {
        return destinations.isCurrentlyReachable(pId);
    }
    
    /**
     *  Ensure there is a local route for a given peer id if it can at all be done.
     *
     *  @param pId  the peer who's route is desired.
     *  @param hint specify a specific route hint to use
     *  @return Messenger for local route. null if none could be found or created.
     **/

    protected Messenger ensureLocalRoute(EndpointAddress pId, Object hint) {

        // We need to make sure that there is a possible connection to that peer
        // If we have a decent (not closed, busy or available) transport messenger in
        // the pool, then we're done. Else we actively try to make one.
        
        // See if we already have a messenger.
        Messenger m = destinations.getCurrentMessenger(pId);
        
        if (m != null) {
            return m;
        }

        // Ok, try and make one. Pass the route hint info
        m = findReachableEndpoint(pId, false, hint);
        if (m == null) {
            // We must also zap it from our positive cache: if we remembered it working, we should think again.
            destinations.noOutgoingMessenger(pId);
            return null; // No way.
        }
            
        destinations.addOutgoingMessenger(pId, m);

        // We realy did bring something new. Give relief to those that
        // have been waiting for it.
        synchronized (this) {
            notifyAll();
        }
        
        // NOTE to maintainers: Do not remove any negative cache info
        // or route here. It is being managed by lower-level routines.
        // The presence of a messenger in the pool has many origins,
        // each case is different and taken care of by lower level
        // routines.
        
        return m;
    }
    
    /**
     *  Send a message to a given logical destination if it maps to some
     *  messenger in our messenger pool or if such a mapping can be found and
     *  added.
     *
     *  @param destination peer-based address to send the message to.
     *  @param message the message to be sent.
     **/
    void sendOnLocalRoute(EndpointAddress destination, Message message)
        throws IOException {
        
        IOException lastIoe = null;
        Messenger wm;
        
        // Try as long as we get a transport messenger to try with. They close when they fail, which
        // puts them out of cache or pool if any, so we will not see a broken one a second time. We'll
        // try the next one until we run out of options.
        
        while ((wm = ensureLocalRoute(destination, null)) != null) {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Sending to " + destination + " found a messenger");
            }
            
            try {

                // FIXME - jice@jxta.org 20040413: May be we should use the non-blocking mode and let excess messages be dropped
                // given the threading issue still existing in the input circuit (while routing messages through).

                wm.sendMessageB(message, EndpointRouter.routerSName, null);

                // If we reached that point, we're done.
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sending to " + destination + " worked");
                }
                return;

            } catch (IOException ioe) {
                // Can try again, with another messenger (most likely).
                lastIoe = ioe;
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Trying next messenger to " + destination);
            }
            // try the next messenger if there is one.
        }
        
        // Now see why we're here.
        // If we're here for no other reason than failing to get a messenger
        // say so. Otherwise, report the failure from the last time
        // we tried.
        if (lastIoe == null) {
            lastIoe = new IOException("No longer any reachable endpoints to destination.");
        }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Could not send to " + destination, lastIoe);
        }
        throw (IOException) lastIoe;
    }
    
    /**
     *  Default constructor
     **/
    public EndpointRouter() {

        // FIXME tra 20030818  Should be loaded as a service
        // when we have service dependency
        routeCM = new RouteCM();
        
        // FIXME tra 20030818  Should be loaded as a service
        // when we have service dependency
        routeResolver = new RouteResolver();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void init(PeerGroup g, ID assignedID, Advertisement impl)
        throws PeerGroupException {
        
        timer.schedule(new TimerThreadNamer("EndpointRouter Timer for " + g.getPeerGroupID()), 0);

        group = g;
        ModuleImplAdvertisement implAdvertisement = (ModuleImplAdvertisement) impl;
        endpoint = group.getEndpointService();
        localPeerId = group.getPeerID();
        localPeerAddr = new EndpointAddress(routerPName, group.getPeerID().getUniqueValue().toString(), null, null);
        destinations = new Destinations(endpoint);
        
        // initialize persistent CM route Cache
        // FIXME tra 20030818 Should be loaded as service when complete
        // refactoring is done. When loaded as a true service should not
        // have to pass the EnpointRouter object. The issue is we need
        // an api to obtain the real object from the PeerGroup API.
        routeCM.init(g, assignedID, impl, this);
        
        // initialize the route resolver
        // FIXME tra 20030818 Should be loaded as service when complete
        // refactoring is done. When loaded as a true service should not
        // have to pass the EnpointRouter object. The issue is we need
        // an api to obtain the real object from the PeerGroup API.
        routeResolver.init(g, assignedID, impl, this);
        
        endpoint.addIncomingMessageListener(this, routerSName, null);
        if (endpoint.addMessageTransport(this) == null) {
            throw new PeerGroupException("Transport registration refused");
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Router Transport : "+ assignedID);
            
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
            configInfo.append("\n\t\tProtocol : " + getProtocolName() );
            configInfo.append("\n\t\tPublic Address : " + localPeerAddr);
            configInfo.append("\n\t\tUse Cm : " + routeCM.useRouteCM());
            configInfo.append("\n\t\tUse RouteResolver : " + routeResolver.useRouteResolver());
            
            LOG.info(configInfo);
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int startApp(String[] arg) {
        
        int status = 0;
        
        // FIXME tra 20031015 Should be started as a service
        // when refactored work completed
        status = routeCM.startApp(arg);
        if (status != 0) {
            return status;
        }
        
        // FIXME tra 20031015 is there a risk for double
        // registration  when startApp() is recalled
        // due to failure to get the discovery service
        // in the previous statement.
        
        // NOTE: Endpoint needs to be registered before
        // we register the endpoint resolver. This is
        // bringing a more complex issue of service
        // loading dependencies.
        endpoint.addMessengerEventListener(this, EndpointService.MediumPrecedence);
        
        // FIXME tra 20031015 Should be started as a service
        // when refactored completed
        status = routeResolver.startApp(arg);
        if (status != 0) {
            return status;
        }
        
        // publish my local route adv
        routeCM.publishRoute(getMyLocalRoute());
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Router Message Transport started");
        }
        
        return status;
    }
    
    /**
     *  {@inheritDoc}
     *
     * <p/>Careful that stopApp() could in theory be called before startApp().
     */
    public void stopApp() {
        
        if (endpoint != null) {
            endpoint.removeIncomingMessageListener(routerSName, null);
            endpoint.removeMessengerEventListener(this, EndpointService.MediumPrecedence);
            endpoint.removeMessageTransport(this);
            endpoint = null;
        }

        // FIXME tra 20030818 should be unloaded as a service
        routeCM.stopApp();

        // FIXME tra 20030818 should be unloaded as a service
        routeResolver.stopApp();

        destinations.close();
        timer.cancel();
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Router Message Transport stopped");
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    public boolean isConnectionOriented() {
        return false;
    }
    
    /**
     *  {@inheritDoc}
     */
    public boolean allowsRouting() {
        // Yes, this is the router, and it does not allow routing.
        // Otherwise we would have a chicken and egg problem.
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public EndpointService getEndpointService() {
        return endpoint;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public EndpointAddress getPublicAddress() {
        return (EndpointAddress) localPeerAddr.clone();
    }
    
    /**
     *  {@inheritDoc}
     */
    public Iterator getPublicAddresses() {
        return Collections.singletonList(getPublicAddress()).iterator();
    }
    
    /**
     *  {@inheritDoc}
     */
    public String getProtocolName() {
        return routerPName;
    }

    /**
     *(@inheritdoc}
     */
    public boolean isPropagateEnabled() {
        
        return false;
    }

    /**
     *(@inheritdoc}
     */
    public boolean isPropagationSupported() {
        
        return false;
    }

    /**
     *  {@inheritDoc}
     **/
    public void propagate(Message srcMsg, String pName, String pParam, String prunePeer) throws IOException {
        // All messages are lost in the ether
    }
    
    /**
     * Given a peer id, return an address to reach that peer.
     * The address may be for a directly reachable peer, or
     * for the first gateway along a route to reach the peer.
     * If we do not have a route to the peer, we will use the
     * Peer Routing Protocol to try to discover one.  We will
     * wait up to 30 seconds for a route to be discovered.
     *
     * @param dest the peer we are trying to reach.
     * @param seekRoute whether to go as far as issuing a route query, or just fish in our cache.
     * when forwarding a message we allow ourselves to mend a broken source-issued route but we
     * won't go as far as seeking one from other peers. When originating a message, on the other end
     * we will aggressively try to find route.
     * @param hint whether we are passed a route hint to be used, in that case that route
     * hint should be used
     *
     * @return an EndpointAddress at which that peer should be reachable.
     **/
    
    EndpointAddress getGatewayAddress(EndpointAddress dest, boolean seekRoute) {
        return getGatewayAddress(dest, seekRoute, null);
    }
    
    EndpointAddress getGatewayAddress(EndpointAddress dest, boolean seekRoute, Object hint) {
        
        try {
            EndpointAddress pId = new EndpointAddress(dest, null, null);
            
            // FIXME: jice@jxta.org - 20021215 replace that junk with a background
            // task; separate the timings of route disco from the timeouts of
            // the requesting threads. EndpointAddress result = null;
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Searching local" + (seekRoute ? " & remote" : "") + " for route for " + pId);
            }
            
            // If we can't get a route within the timeout, give up for now.
            long quitAt = TimeUtils.toAbsoluteTimeMillis(MAXFINDROUTE_TIMEOUT);
            
            // Time we need to wait before we can start issue a find route request
            // to give a chance for the async messenger to respond (success or failure)
            long findRouteAt = TimeUtils.toAbsoluteTimeMillis(ASYNC_MESSENGER_WAIT);
            
            EndpointAddress addr = null;
            
            while (TimeUtils.toRelativeTimeMillis(quitAt) > 0) {
                // Then check if by any chance we can talk to it directly.
                if (ensureLocalRoute(pId, hint) != null) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Found direct address " + pId);
                    }
                    
                    return pId;
                }
                
                // Otherwise, look for a long route.
                // check if we got a hint. If that's the case use it
                RouteAdvertisement route = null;

                if (hint != null) {
                    route = (RouteAdvertisement) hint;
                } else {
                    route = getRoute(pId, seekRoute);
                }
                
                if (route != null && route.size() > 0) {
                    
                    addr = pid2addr(route.getLastHop().getPeerID());
                    if (ensureLocalRoute(addr, null) != null) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Found last hop remote address: " + pId + " -> " + route.getLastHop().getPeerID());
                        }
                        
                        // Ensure local route removes negative cache info about
                        // addr. We also need to remove that about pId.
                        return addr;
                        
                    } else { // need to try the first hop
                        addr = pid2addr(route.getFirstHop().getPeerID());
                        
                        if (ensureLocalRoute(addr, null) != null) {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Found first hop remote address first hop: " + pId + " -> " + route.getFirstHop().getPeerID());
                            }
                            
                            // Ensure local route removes negative cache info about
                            // addr.
                            return addr;
                            
                        } else {
                            
                            removeRoute(pId);
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Found no reachable route to " + pId);
                            }
                        }
                    }
                }
                
                // For messages we didn't originate we don't seek routes.
                if (!seekRoute) {
                    break;
                }
                
                // Check that route resolution is enabled if
                // not then bail out, there is nothing more
                // that we can do.
                if (!routeResolver.useRouteResolver()) {
                    break;
                }
                
                // due to the asynchronous nature of getting our messenger we
                // need to handle the multi-entrance of issueing a route
                // discovery. A route discovery needs to be generated only
                // either if we have no pending request (it completed or we had
                // no information so we did not created one), or we tried and
                // we failed, or we waited at least ASYNC_MESSENGER_WAIT to get
                // a chance for the async request to respond before we can
                // issue the route discovery
                Long nextTry = (Long) triedAndFailed.get(pId);

                if ((nextTry == null) || (nextTry.longValue() < TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY))
                        || (TimeUtils.toRelativeTimeMillis(findRouteAt) <= 0)) {
                    
                    // If it is already hopeless (negative cache), just give up.
                    // Otherwise, try and recover the route. If a query is not
                    // already pending, we may trigger a route discovery before we
                    // wait. Else, just wait. The main problem we have here is that
                    // the same may re-enter because the resolver query sent by
                    // findRoute ends up with the rendezvous service trying to
                    // resolve the same destiation if the destination  happens to be
                    // the start of the walk. In that situation we will re-enter
                    // at every findRoute attempt until the query becomes "failed".
                    // However, we do want to do more than one findRoute because
                    // just one attempt can fail for totaly fortuitous or temporary
                    // reasons. A tradeoff is to do a very limitted number of attempts
                    // but still more than one. Over the minute for which the query
                    // is not failed, isTimeToRety will return true at most twice
                    // so that'll be a total of three attempts: once every 20 seconds.
                    boolean doFind = false;
                    ClearPendingQuery t = null;

                    synchronized (this) {
                        t = (ClearPendingQuery) pendingQueries.get(pId);
                        
                        if (t == null) {
                            doFind = true;
                            t = new ClearPendingQuery(pId);
                            pendingQueries.put(pId, t);
                        } else {
                            if (t.isFailed()) {
                                break;
                            }
                            if (t.isTimeToRetry()) {
                                doFind = true;
                            }
                        }
                    }
                    
                    // protect against the async messenger request. We only
                    // look for a route after the first iteration by
                    // that time we will have bailed out from the async call
                    if (doFind) {
                        routeResolver.findRoute(pId);
                        // we do not need to check the CM, route table will
                        // be updated when the route response arrive. This reduces
                        // CM activities when we wait for the route response
                        seekRoute = false;
                    }
                }
                
                // Now, wait. Responses to our query may occur asynchronously.
                // threads.
                synchronized (this) {
                    // We can't possibly do everything above while synchronized,
                    // so we could miss an event of interrest. But some changes
                    // are not readily noticeable anyway, so we must wake up
                    // every so often to retry.
                    try {
                        // we only need to wait if we haven't got a messenger
                        // yet.
                        if (destinations.getCurrentMessenger(pId) == null) {
                            wait(ASYNC_MESSENGER_WAIT);
                        }
                    } catch (InterruptedException woken) {
                        Thread.interrupted();
                    }
                }
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No route to " + pId);
            }
            
            return null;
            
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("getGatewayAddress exception", ex);
            }
            
            return null;
        }
    }
    
    /**
     * Returns true if the target address is reachable. Otherwise
     * returns false.
     */
    public boolean ping(EndpointAddress addr) {
        
        EndpointAddress plainAddr = new EndpointAddress(addr, null, null);
        
        try {
            return (getGatewayAddress(plainAddr, true) != null);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.debug("Ping failure (exception) for : " + plainAddr, e);
            }
            return false;
        }
    }
    
    /**
     * Receives notifications of new messengers being generated by the
     * underlying network transports.
     *
     * <p/>IMPORTANT: Incoming messengers only. If/when this is used for
     * outgoing, some things have to change:
     *
     * <p/>For example we do not need to send the welcome msg, but
     * for outgoing messengers, we would need to.
     *
     * <p/>Currently, newMessenger handles the outgoing side.
     *
     *    @param event    the new messenger event.
     **/
    public boolean messengerReady(MessengerEvent event) {
        
        Messenger messenger = event.getMessenger();
        
        Object source = event.getSource();
        
        EndpointAddress logDest = messenger.getLogicalDestinationAddress();   

        if (source instanceof MessageSender && !((MessageSender) source).allowsRouting()) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Ignoring messenger to :" + logDest);
            }
            return false;
        }
        
        // We learned that a transport messenger has just been
        // announced.  Noone else took it, so far, so we'll take
        // it. Incoming messengers are not pooled by the endpoint
        // service. We do pool them for our exclusive use.

        boolean taken = destinations.addIncomingMessenger(logDest, messenger);

        // Note to maintainers: Do not remove any route or negative
        // cache info here. Here is why: The messenger we just
        // obtained was made out of an incoming connection. It brings
        // no proof whatsoever that the peer is reachable at our
        // initiative. In general, there is nothing to gain in
        // removing our knowlege of a long route, or a pending route
        // query, or a triedAndFailed record, other than to force
        // trying a newly obtained set of addresses. They will not
        // stop us from using this messenger as long as it works.
        // The only good thing we can do here, is waking up those
        // that may be waiting for a connection.
        
        synchronized (this) {
            notifyAll();
        }
        
        return taken;
    }
    
    /**
     *  call when an asynchronous new messenger could not be obtained.
     *
     *    @param logDest the failed logical destination
     */
    public void noMessenger(EndpointAddress logDest) {
        
        // Switch to short timeout if there was an infinite one.
        // Note if there's one, it is either short or inifinite. So we
        // look at the value only in the hope it is less expensive
        // than doing a redundant put.
        
        synchronized (this) {
            Long curr = (Long) triedAndFailed.get(logDest);

            if (curr != null && curr.longValue() > TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)) {
                triedAndFailed.put(logDest, new Long(TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)));
            }
        }
    }
    
    /**
     * call when an asynchronous new messenger is ready.
     * (name is not great).
     *
     * @param event the new messenger event.
     */
    public boolean newMessenger(MessengerEvent event) {
        
        Messenger messenger = event.getMessenger();
        EndpointAddress logDest = messenger.getLogicalDestinationAddress();
        
        // We learned that a new transport messenger has just been announced.
        // We pool it for our exclusive use.
        destinations.addOutgoingMessenger(logDest, messenger);
        
        // Here's a new connection. Wakeup those that may be waiting
        // for that.
        synchronized (this) {
            notifyAll();
        }
        
        return true;
    }
    
    /**
     *  Get the routed route, if any, for a given peer id.
     *
     *  @param pId  the peer who's route is desired.
     *  @param seekRoute boolean to indicate  if we should search for a route
     *         if we don't have one
     *  @return a route advertisement describing the direct route to the peer.
     **/
    protected RouteAdvertisement getRoute(EndpointAddress pId, boolean seekRoute) {
        
        // check if we have a valid route
        RouteAdvertisement route = null;
        
        synchronized (this) {
            route = (RouteAdvertisement) routedRoutes.get(pId);
        }
        
        if (route != null || !seekRoute) { // done
            return route;
        }
        
        // No known route and we're allowed to search for one
        
        // check if there is route advertisement available
        Iterator allRadvs = routeCM.getRouteAdv(pId);
        
        if( null == allRadvs ) {
            return null;
        }
       
        while (allRadvs.hasNext()) {
            
            route = (RouteAdvertisement) allRadvs.next();
            
            // let's check if we can speak to any of the hops in the route
            // we try them in reverse order so we shortcut the route
            // in the process
            RouteAdvertisement newRoute = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
            
            newRoute.setDest((AccessPointAdvertisement)
                    route.getDest().clone());
            Vector hops = route.getVectorHops();
            Vector newHops = new Vector();
            
            // no hops, uninterresting: this needs to be a route
            if (hops.size() == 0) {
                continue;
            }
            
            // build the route from the available hops
            for (int i = hops.size() - 1; i >= 0; i--) {
                EndpointAddress addr = pid2addr(((AccessPointAdvertisement)
                        hops.elementAt(i)).getPeerID());
                
                // If the local peer is one of the first reachable
                // hop in the route, that route is worthless to us.
                if (addr.equals(localPeerAddr)) {
                    break;
                }
                
                if (ensureLocalRoute(addr, null) != null) {
                    // we found a valid hop return the corresponding
                    // route from that point
                    for (int j = i; j <= hops.size() - 1; j++) {
                        newHops.add(((AccessPointAdvertisement)
                                hops.elementAt(j)).clone());
                    }
                    
                    // make sure we have a real route at the end
                    if (newHops.size() == 0) {
                        break;
                    }
                    
                    newRoute.setHops(newHops);
                    
                    // try to set the route
                    if (setRoute(newRoute, false)) {
                        // We got one; we're done.
                        return newRoute;
                    } else {
                        // For some reason the route table does not
                        // want that route. Move on to the next adv; it
                        // unlikely that a longer version of the same would
                        // be found good.
                        break;
                    }
                }
            }
        }
        
        // no route found
        return null;
    }
    
    // Check if a route is valid.
    // Currently, only loops are detected.
    private boolean checkRoute(RouteAdvertisement r) {
        
        if (r == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("route is null");
            }
            return false;
        }
        
        if (r.size() == 0) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("route is empty");
            }
            return false;
        }
        
        if (r.containsHop((PeerID) localPeerId)) {
            // The route does contain this local peer. Using this route
            // would create a loop. Discard.
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("route contains this peer - loopback");
            }
            return false;
        }
        
        PeerID destPid = r.getDest().getPeerID();

        if (r.containsHop(destPid)) {
            // May be it is fixable, may be not. See to it.
            Vector hops = r.getVectorHops();
            
            // It better be the last hop. Else this is a broken route.
            hops.remove(hops.lastElement());
            
            if (r.containsHop(destPid)) {
                // There was one other than last: broken route.
                return false;
            }
        }
        
        if (r.hasALoop()) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("route has a loop ");
            }
            return false;
        } else {
            // Seems to be a potential good route.
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("route is ok");
            }
            return true;
        }
    }
    
    // Adds a new long route provided there not a direct one already.
    // Replaces any longer route.  return true if the route was truely new.
    // The whole deal must be synch. We do not want to add a long route
    // while a direct one is being added in parallell or other stupid things like that.
    
    /**
     * set new route info
     *
     * @param r new route to learn
     * @param force true if the route was optained by receiving
     *                a message
     */
    protected boolean setRoute(RouteAdvertisement r,
            boolean force) {
        PeerID pid = null;
        EndpointAddress pidAddr = null;
        boolean pushNeeded = false;
        boolean status = false;
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("setRoute:");
        }

        if (r == null) {
            return false;
        }

        synchronized (this) {
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug(r.display());
                }
                
                pid = r.getDest().getPeerID();
                pidAddr = pid2addr(pid);
                
                // Check if we are in the case where we are
                // setting a new route as we received a message
                // always force the new route setup when we received a
                // a message
                
                if (!force) {
                    // check if we have some bad NACK route info for
                    // this destination
                    BadRoute badRoute = (BadRoute) badRoutes.get(pidAddr);

                    if (badRoute != null) {
                        Long nextTry = badRoute.getExpiration();

                        if (nextTry.longValue() > System.currentTimeMillis()) {
                            
                            // check if the route we have in the NACK cache match the
                            // new one. Need to make sure that we clean the route
                            // from any endpoint addresses as the badRoute cache only
                            // contains PeerIDs
                            RouteAdvertisement routeClean = (RouteAdvertisement) r.cloneOnlyPIDs();

                            if (routeClean.equals(badRoute.getRoute())) {
                                if (LOG.isEnabledFor(Level.DEBUG)) {
                                    LOG.debug("try to use a known bad route");
                                }
                                return false;
                            }
                        } else { // expired info, just flush NACK route cache
                            badRoutes.remove(pidAddr);
                        }
                    }
                } else {
                    // we get a new route
                    badRoutes.remove(pidAddr);
                }
                
                // Check if the route makes senses (loop detection)
                if (!checkRoute(r)) {
                    // Route is invalid. Drop it.
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Route is invalid");
                    }
                    return false;
                }
                
                // check if we can reach the first hop in the route
                // We only do a shallow test of the first hop. Whether more effort
                // is worth doing or not is decided (and done) by the invoker.
                if (!isLocalRoute(pid2addr(r.getFirstHop().getPeerID()))) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Unreachable route - ignore");
                    }
                    return false;
                }
                
            } catch (Exception ez1) {
                // The vector must be empty, which is not supposed
                // to happen.
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Got an empty route - discard" + r.display());
                }
                return false;
            }
            
            // add the new route
            try {
                // push the route to SRDI only if it is a new route. the intent is
                // to minimize SRDI traffic. The SRDIinformation is more of the order
                // this peer has a route to this destination, it does not need to be
                // updated verey time the route is updated. Information about knowing
                // that this peer has a route is more important that the precise
                // route information
                
                // SRDI is run only if the peer is acting as a rendezvous
                if (group.isRendezvous()) {
                    if (!routedRoutes.containsKey(pidAddr)) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("push new SRDI route " + pid);
                        }
                        pushNeeded = true;
                    }
                }
                
                // new route so publish the known route in our cache
                if (!routedRoutes.containsKey(pidAddr)) {
                    routeCM.createRoute(r);
                    newDestinations.add(pidAddr);
                }
                
                // Remove any endpoint addresses from the route
                // as part of the cloning. We just keep track
                // of PIDs in our route table
                RouteAdvertisement newRoute = (RouteAdvertisement) r.cloneOnlyPIDs();

                routedRoutes.put(pidAddr, newRoute);
                
                // We can get rid of any negative info we had. We have
                // a new and different route.
                badRoutes.remove(pidAddr);
                
                notifyAll(); // Wakeup those waiting for a route.
                
                status = true;
            } catch (Exception e2) {
                // We failed, leave things as they are.
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("   failed setting route with " + e2);
                }
                status = false;
            }
        }
        // due to the potential high latency of making the
        // srdi revolver push we don't want to hold the lock
        // on the EndpointRouter object as we may have to
        // discover a new route to a rendezvous
        if (pushNeeded && status) {
            // we are pushing the SRDI entry to a replica peer
            routeResolver.pushSrdi(null, pid);
        }
        return status;
    }
    
    /**
     * This method is used to remove a route
     *
     * @param pId  route to peerid to be removed
     **/
    protected void removeRoute(EndpointAddress pId) {
        
        boolean needRemove;

        synchronized (this) {
            needRemove = false;
            if (routedRoutes.containsKey(pId)) {
                if (group.isRendezvous()) {
                    // Remove the SRDI cache entry from the SRDI cache
                    needRemove = true;
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("remove SRDI route " + pId);
                    }
                }
                routedRoutes.remove(pId);
            }
        }
        
        // due to the potential high latency of pushing
        // the SRDI message we don't want to hold the EndpointRouter
        // object lock
        if (needRemove) {
            // We are trying to flush it from the replica peer
            // Note: this is not guarantee to work if the peerview
            // is out of sync. The SRDI index will be locally
            // repaired as the peerview converge
            routeResolver.removeSrdi(null, addr2pid(pId));
        }
        
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void processIncomingMessage(Message msg,
            EndpointAddress srcAddr,
            EndpointAddress dstAddr) {
        
        EndpointAddress srcPeer = null; // The originating peer
        EndpointAddress destPeer = null; // The destination peer
        EndpointAddress lastHop = null; // The last peer that routed this to us
        boolean connectLastHop = false;

        ; // true:Try connecting to lastHop
        
        EndpointAddress origSrcAddr = null; // The origin endpointAddr (jxta:)
        EndpointAddress origDstAddr = null; // The dest endpointAddr   (jxta:)
        Vector origHops = null; // original route of the message
        
        EndpointRouterMessage routerMsg = null;
        
        EndpointAddress nextHop = null;
        RouteAdvertisement radv = null;
        
        // We do not want the existing header to be ignored of course.
        routerMsg = new EndpointRouterMessage(msg, false);
        
        if (!routerMsg.msgExists()) {
            
            // The sender did not use this router
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No routing info for " + msg );
            }
            return;
        }
        
        try {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug(routerMsg.display());
            }
            
            origSrcAddr = routerMsg.getSrcAddress();
            
            origDstAddr = routerMsg.getDestAddress();
            
            // convert the src and dest addresses into canonical
            // form stripping service info
            srcPeer = new EndpointAddress(origSrcAddr, null, null);
            destPeer = new EndpointAddress(origDstAddr, null, null);
            
            if (routerMsg.getLastHop() != null) {
                lastHop = new EndpointAddress(routerMsg.getLastHop());
            }
            
            // See if there's an originator full route adv inthere.
            // That's a good thing to keep.
            radv = routerMsg.getRouteAdv();
            if (radv != null) {

                // publish the full route adv. Also, leave it the
                // message.  It turns out to be extremely usefull to
                // peers downstream, specially the destination. If
                // this here peer wants to embed his own radv, it will
                // have to wait; the one in the message may not come
                // again.
                
                // FIXME - jice@jxta.org 20040413 : all this could wait (couldn't it ?)
                // until we know it's needed, therefore parsing could wait as well.
                
                // Looks like a safe bet to try and ensure a
                // connection in the opposite direction if there
                // isn't one already.
                
                if (pid2addr(radv.getDestPeerID()).equals(lastHop)) {
                    connectLastHop = true;
                }
                
                // Make the most of that new adv.
                setRoute(radv, true);
                updateRouteAdv(radv);
            }
            
        } catch (Exception badHdr) {
            // Drop it, we do not even know the destination
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Bad routing header or bad message. Dropping " + msg );
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Exception: ", badHdr);
            }
            return;
        }
        
        // Is this a loopback ?
        if ((srcPeer != null) && srcPeer.equals(localPeerAddr)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("processIncomingMessage: dropped loopback");
            }
            return;
        }
        
        // Are we already sending to ourself. This may occur
        // if some old advertisements for our EA is still
        // floating around
        if ((lastHop != null) && lastHop.equals(localPeerAddr)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("processIncomingMessage: dropped loopback from impersonating Peer");
            }
            return;
        }
        
        // We have to try and reciprocate the connection, so that we
        // have chance to learn reverse routes early enough.  If we do
        // not already have a messenger, then we must know a route adv
        // for that peer in order to be able to connect. Otherwise,
        // the attempt will fail and we'll be left with a negative
        // entry without having realy tried anything.  To prevent that
        // we rely on the presence of a radv in the router message. If
        // there's no radv, two possibilities:
        //
        // - It is not the first contact from that peer and we already
        // have tried (with or without success) to reciprocate.
        //
        // - It is the first contact from that peer but it has not
        // embedded its radv. In the most likely case (an edge peer
        // connecting to a rdv), the edge peer will have no difficulty
        // finding the reverse route, provided that we do not make a
        // failed attempt right now.
        //
        // Conclusion: if there's no embedded radv in the message, do
        // nothing.
        
        if (connectLastHop) {
            ensureLocalRoute(lastHop, radv);
        }
        
        try {
            
            // Normalize the reverseHops vector from the message and, if it
            // looks well formed and usefull, learn from it.  Do we have a
            // direct route to the origin ?  If yes, we'll zap the revers
            // route in the message: we're a much better router.  else,
            // learn from it. As a principle we regard given routes to be
            // better than existing ones.
            Vector reverseHops = routerMsg.getReverseHops();

            if (reverseHops == null) {
                reverseHops = new Vector();
            }
            
            // check if we do not have a direct route
            // in that case we don't care to learn thelong route
            if (!isLocalRoute(srcPeer)) {
                // Check if the reverseRoute info looks correct.
                if (lastHop != null) {
                    // since we are putting the lasthop in the
                    // reverse route to indicate the validity of
                    // lastop to reach the previous hop, we have
                    // the complete route, just clone it. (newRoute
                    // owns the given vector)
                    
                    if ((reverseHops.size() > 0) && ((AccessPointAdvertisement) reverseHops.firstElement()).getPeerID().equals(addr2pid(lastHop))) {
                        
                        // Looks like a good route to learn
                        setRoute(RouteAdvertisement.newRoute(addr2pid(srcPeer), (Vector) reverseHops.clone()), true);
                        
                    }
                    
                }
            }
            
            // If this peer is the final destination, then we're done. Just let
            // it be pushed up the stack. We must not leave our header;
            // it is now meaningless and none of the upper layers business.
            // All we have to do is to supply the end-2-end src and dest
            // so that the endpoint demux routine can do its job.
            if (destPeer.equals(localPeerAddr)) {
                
                // Removing the header.
                routerMsg.clearAll();
                routerMsg.updateMessage();
                
                // receive locally
                endpoint.processIncomingMessage(msg, origSrcAddr, origDstAddr);
                
                return;
            }
            
            // WATCHOUT: if this peer is part of the reverse route
            // it means that we've seen that message already: there's
            // a loop between routers ! If that happens drop that
            // message as if it was burning our fingers
            
            // First build the ap that we might add to the reverse route.
            // We need it to look for ourselves in reverseHops. (contains
            // uses equals(). equals will work because we always include
            // in reversehops aps that have only a pid.
            
            AccessPointAdvertisement selfAp = (AccessPointAdvertisement)
                    AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

            selfAp.setPeerID(addr2pid(localPeerAddr));
            
            if (reverseHops.contains(selfAp)) {
                
                // Danger, bail out !
                // Better not to try to NACK for now, but get rid of our own
                // route. If we're sollicited again, there won't be a loop
                // and we may NACK.
                
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Routing loop detected. Message dropped");
                }
                removeRoute(destPeer);
                return;
            }
            
            // Update reverseHops. That is, add ourselves to the list.
            
            // We will only add the current hop to the reverse
            // route if we know how to talk back to the previous hop
            // it is important to point the difference between the lastHop
            // and the reverse route entry. The lastHop indicates where the
            // message came from but does not specify whether it is able to
            // route messages in the other direction. The reverse route, if
            // present, provides that information.
            
            // FIXME - jice@jxta.org 20040413 : HERE comes the use of connectLastHop. Could have waited till here.
            
            if (isLocalRoute(lastHop)) { // ok we have direct route back, at least we hope :-)
                
                reverseHops.add(0, selfAp); // Update our vector
                routerMsg.prependReverseHop(selfAp); // Update the message, this preserves the cache.
                
            } else {
                
                // We cannot talk to our previous hop, well
                // check if we have route to the src and use it as
                // our reverse route. We could do more. But let's keep
                // it to the minimum at this point.
                RouteAdvertisement newReverseRoute = (RouteAdvertisement) routedRoutes.get(srcPeer);
                
                if (newReverseRoute != null) {
                    // we found a new route back from our cache so let's use it
                    reverseHops = (Vector)
                            newReverseRoute.getVectorHops().clone();
                    
                    // ok add ourselve to the reverse route
                    reverseHops.add(0, selfAp);
                    
                } else {
                    // no new route found, sorry. In the worst
                    // case it is better to not have reverse route
                    reverseHops = null;
                }
                
                // In both cases above, we replace the hops completely.
                // The cache is of no use and is lost.
                routerMsg.setReverseHops(reverseHops);
            }
            
            // Get the next peer into the forward route
            origHops = routerMsg.getForwardHops();
            if (origHops != null) {
                nextHop = getNextHop(origHops);
            }
            
            // see if we can shortcut to the destination with no effort.
            // If that works it's all very easy.
            if (isLocalRoute(destPeer)) {
                
                // There is a local route - use it
                // Clear the forward path which is probably wrong
                routerMsg.setForwardHops(null);
                nextHop = (EndpointAddress) destPeer.clone();
                
            } else {
                
                if (nextHop == null) {
                    
                    // No next hop. Use the destPeer instead. (but, unlike when
                    // we shortcut it deliberately, don't know if we can have a direct route
                    // yet). This is perfectly normal if we're just the last
                    // hop before the destination and we have closed the direct connection
                    // with it since we declared to be a router to it.
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("No next hop in forward route - Using destination as next hop");
                    }
                    nextHop = (EndpointAddress) destPeer.clone();
                    
                    // That forward path is exhausted. It will not be usefull anymore.
                    // either we reach the destination directly and there will be
                    // no need for a NACK further down, or we will need to find an alternate
                    // route.
                    routerMsg.setForwardHops(null);
                }
                
                // We must be do better than look passively for a direct
                // route. The negative cache will take care of reducing the
                // implied load. If we do not, then we never re-establish
                // a broken local route until the originating peer seeks a
                // new route. Then the result is roughly the same plus
                // the overhead of route seeking...worse, if we're in the
                // path from the originator to it's only rdv, then the
                // originator does not stand a chance until it re-seeds !
                
                if (ensureLocalRoute(nextHop, null) == null) { // Here we go :-)
                    
                    // need to look for a long route.
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Forward route element broken - trying alternate route");
                    }
                    
                    // While we're at it, we might as well get rid of our own
                    // route to the destination if it goes through the same hop
                    // by any chance.
                    // FIXME: idealy, each time we get a broken local route
                    // we'd want to get rid of all routes that start from there
                    // but that's one more map to maintain.
                    
                    RouteAdvertisement route = getRoute(destPeer, false);

                    if (route == null) {
                        cantRoute("No new route to repair the route - drop message", null, origSrcAddr, destPeer, origHops);
                        return;
                    }
                    
                    if (pid2addr(route.getFirstHop().getPeerID()).equals(nextHop)) {
                        // Our own route is just as rotten as the sender's. Get rid
                        // of it.
                        removeRoute(destPeer);
                        cantRoute("No better route to repair the route - drop message", null, origSrcAddr, destPeer, origHops);
                        return;
                    }
                    
                    // optimization to see if we can reach
                    // directly the last hop of that route
                    EndpointAddress addr = pid2addr(route.getLastHop().getPeerID());
                    
                    if (isLocalRoute(addr)) {
                        
                        // FIXME - jice@jxta.org 20030723. Should update our route
                        // table to reflect the shortcut.
                        
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Found new remote route via : " + addr);
                        }
                        
                        // set the forward path to null no next hop
                        // FIXME: Not true. the last hop is not the destination.
                        // There could be a need for us receiving a NACK and that won't be
                        // possible. We should leave the next hop in the fw path. Just like
                        // we do when forwarding along the existing route.
                        
                        routerMsg.setForwardHops(null);
                        
                    } else { // need to check the first hop
                        
                        Vector newHops = (Vector) route.getVectorHops().clone();
                        
                        // FIXME: remove(0) seems wrong
                        // There could be a need for us receiving a NACK and that won't be
                        // possible. We should leave the next hop in the fw path. Just like
                        // we do when forwarding along the existing route.
                        
                        addr = pid2addr(((AccessPointAdvertisement) newHops.remove(0)).getPeerID());
                        
                        if (!isLocalRoute(addr)) {
                            // Our own route is provably rotten
                            // as well. Get rid of it.
                            removeRoute(destPeer);
                            cantRoute("No usable route to repair the route - drop message", null, origSrcAddr, destPeer, origHops);
                            
                            return;
                        }
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Found new remote route via : " + addr);
                        }
                        
                        // NB: setForwardHops does not clone.
                        routerMsg.setForwardHops(newHops);
                    }
                    
                    // If we're here. addr is our new nextHop.
                    nextHop = addr;
                }
            }
            
            // The first time we talk to a peer to which we have
            // initiated a connection, we must include our local
            // route adv in the routerMsg. However, we give priority to
            // a route adv that's already in the message and which we pass along.
            // In that case, our own will go next time. Note: we care only for
            // nextHop, not for the final destination. We give our radv to a far
            // destination only if we originate a message to it; not when forwarding.
            // JC: give priority to our own radv instead. It can be critical.

            // 20040301 tra: May be the case we haven't yet initialize our
            // own local route. For example still waiting for our relay connection
            RouteAdvertisement myRoute = getMyLocalRoute();

            if ((myRoute != null) && destinations.isWelcomeNeeded(nextHop)) {
                routerMsg.setRouteAdv(myRoute);
            }
            
            // We always modify the router message within the message
            routerMsg.setLastHop(localPeerAddr.toString());
            routerMsg.updateMessage();
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Trying to forward to " + nextHop);
            }
            
            sendOnLocalRoute(nextHop, msg);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Successfully forwarded to " + nextHop);
            }
            
        } catch (Exception e) {
            cantRoute("Failed to deliver or forward message for " + destPeer, e, origSrcAddr, destPeer, origHops);
        }
    }
    
    private void cantRoute(String logMsg, Exception e, EndpointAddress origSrcAddr,
            EndpointAddress destPeer, Vector origHops) {
        
        if (LOG.isEnabledFor(Level.WARN)) {
            if (e == null) {
                LOG.warn(logMsg);
            } else {
                LOG.warn(logMsg, e);
            }
        }
        routeResolver.generateNACKRoute(addr2pid(origSrcAddr), addr2pid(destPeer), origHops);
    }
    
    /**
     * Return the address of the next hop in this vector
     *
     * @param list of forward hops in the route
     * @return next hop to be used
     */
    private EndpointAddress getNextHop(Vector hops) {
        
        // check if we have a real route
        if ((hops == null) || (hops.size() == 0)) {
            return null;
        }
        
        // find the next hop.
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement ap = (AccessPointAdvertisement) e.nextElement();
            
            if (localPeerId.equals(ap.getPeerID())) {
                
                // If found at the end, no next hop
                if (!e.hasMoreElements()) {
                    return null;
                }
                
                return pid2addr(((AccessPointAdvertisement) e.nextElement()).getPeerID());
            }
        }
        
        // The peer is not into the vector. Since we have got that
        // message, the best we can do is to send it to the first gateway
        // in the forward path.
        return pid2addr(((AccessPointAdvertisement) hops.elementAt(0)).getPeerID());
    }
    
    /**
     *  lame hardcoding
     **/
    private boolean isFast(MessageTransport p) {
        String name = p.getProtocolName();
        
        return name.equals("tcp") || name.equals("beep");
    }
    
    private boolean isRelay(MessageTransport p) {
        String name = p.getProtocolName();
        
        return name.equals("relay");
    }
    
    /**
     * Given a list of addresses, find the best reachable endpoint.
     *
     * <ul>
     *      <li>The address returned must be reachable.</li>
     *      <li>We prefer an address whose protocol is, in order:</li>
     *          <ol>
     *              <li>connected and fast.</li>
     *              <li>connected and slow.</li>
     *              <li>unconnected and fast.</li>
     *              <li>unconnected and slow</li>
     *          </ol></li>
     *  </ul>
     *
     * @param addrs A list of addresses to evaulate reachability.
     * @param exist true if there already are existing messengers for
     * the given destinations but we want one more. It may lead us to reject
     * certain addresses that we would otherwise accept.
     * @return The endpoint address for which we found a local route otherwise
     * null
     */
    Messenger findBestReachableEndpoint(EndpointAddress dest, List mightWork, boolean exist) {
        
        List rankings = new ArrayList(mightWork.size());
        List worthTrying = new ArrayList(mightWork.size());
        
        // First rank the available addresses by type rejecting those which
        // cant be used.
        Iterator eachMightWork = mightWork.iterator();
        
        while (eachMightWork.hasNext()) {
            EndpointAddress addr = (EndpointAddress) eachMightWork.next();
            
            // skip our own type
            if (getProtocolName().equals(addr.getProtocolName())) {
                continue;
            }
            
            int rank = -1;
            
            Iterator eachTransport = endpoint.getAllMessageTransports();
            
            while (eachTransport.hasNext()) {
                MessageTransport transpt = (MessageTransport) eachTransport.next();
                
                if (!transpt.getProtocolName().equals(addr.getProtocolName())) {
                    continue;
                }
                
                // must be a sender
                if (!(transpt instanceof MessageSender)) {
                    continue;
                }
                
                MessageSender sender = (MessageSender) transpt;
                
                // must allow routing
                if (!sender.allowsRouting()) {
                    // This protocol should not be used for routing.
                    continue;
                }
                
                rank += 1;
                
                if (sender.isConnectionOriented()) {
                    rank += 2;
                }
                
                if (isRelay(transpt)) {
                    // That should prevent the relay for ever being used
                    // when the relay may be sending through the router.
                    if (exist) {
                        rank -= 1000;
                    }
                }
                
                if (isFast(transpt)) {
                    rank += 4;
                }
            }
            
            // if its worth trying then insert it into the rankings.
            if (rank >= 0) {
                for (int eachCurrent = 0; eachCurrent <= rankings.size(); eachCurrent++) {
                    if (rankings.size() == eachCurrent) {
                        rankings.add(new Integer(rank));
                        worthTrying.add(addr);
                        break;
                    }
                    
                    if (rank > ((Integer) rankings.get(eachCurrent)).intValue()) {
                        rankings.add(eachCurrent, new Integer(rank));
                        worthTrying.add(eachCurrent, addr);
                        break;
                    }
                }
            }
        }
        
        // now that we have them ranked, go through them until we get a
        // successful messenger.
        rankings = null;
        Iterator eachWorthTrying = worthTrying.iterator();
        
        while (eachWorthTrying.hasNext()) {
            EndpointAddress addr = null;

            try {
                addr = (EndpointAddress) eachWorthTrying.next();
                eachWorthTrying.remove();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("getBestLocalRoute - Trying : " + addr);
                }
                
                // We use an async getMessenger as we do not
                // want to wait too long to obtain our messenger
                // We will still wait ASYNCMESSENGER_WAIT to see
                // if we can get the messenger before bailing out
                Messenger messenger = null;
                
                // Create the listener object for that request
                EndpointGetMessengerAsyncListener getMessengerListener = new EndpointGetMessengerAsyncListener(this, dest);
                
                boolean stat = endpoint.getMessenger(getMessengerListener, new EndpointAddress(addr, routerSName, null), null);
                
                if (stat == false) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("failed creating async messenger, continue");
                    }
                    // we failed to get a messenger, we need to update the try and
                    // failed as it currently holds an infinite timeout to permit
                    // another thread to retry that destination. We only retry
                    // every MAXASYNC_GETMESSENGER_RETRY seconds
                    synchronized (this) {
                        triedAndFailed.put(dest, new Long(TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)));
                    }
                    continue;
                }
                
                // wait to see if we can get the Async messenger
                // If there is a long route to that destination, do not
                // wait on the direct route.
                // It may happen that we are actually
                // trying to reach a different peer and this is just part of
                // shortcuting the route via the one of the hops. In that case
                // this test is not entirely accurate. We might still decide
                // to wait when we shouldn't (we're no worse than before, then)
                // But, in most cases, this is going to help.
                boolean quick = (getRoute(dest, false) != null);

                messenger = getMessengerListener.waitForMessenger(quick);
                if (messenger == null) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("did not get our async messenger, bail out");
                    }
                    continue;
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("we got our async messenger, proceed");
                    }
                    
                    // Success we got a messenger synchronously. Remove
                    // the negative cache entry.
                    synchronized (this) {
                        triedAndFailed.remove(dest);
                        notifyAll();
                    }
                    
                    return messenger;
                }
            } catch (Throwable e) {
                // That address is somehow broken.
                // Cache that result for a while.
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("getBestLocalRoute - failed checking route : " + addr, e);
                }
            }
        }
        
        return null;
    }
    
    /**
     *  Read the route advertisement for a peer and find a suitable transport
     *  endpoint for sending to that peere either directly or via one of
     *   the advertised peer router
     **/
    Messenger findReachableEndpoint(EndpointAddress destPeer, boolean exist) {
        return  findReachableEndpoint(destPeer, exist, null);
    }
    
    Messenger findReachableEndpoint(EndpointAddress destPeer, boolean exist, Object hint) {
        
        // findEndpoint is really lazy because what it does is expensive.
        // When needed, the negative info that prevents its from working
        // too much is removed. (see calls to ensureLocalRoute).
        synchronized (this) {
            Long nextTry = (Long) triedAndFailed.get(destPeer);

            if (nextTry != null) {
                if (nextTry.longValue() > TimeUtils.timeNow()) {
                    return null;
                }
            }
            // We are the first thread trying this destination.
            // Let's preclude any other threads from attempting to do
            // anything while we are trying that destination. Other
            // threads will have a chance if they are still waiting
            // when this thread is done. We will update triedAndFailed
            // when we get the async notification that we got or we
            // failed to get a messenger.
            triedAndFailed.put(destPeer, new Long(TimeUtils.toAbsoluteTimeMillis(Long.MAX_VALUE)));
            
        }
        
        // Never tried or it was a long time ago.
        
        // Get (locally) the advertisements of this peer
        Iterator advs = null;
        
        try {
            // try to use the hint that was given to us
            if (hint != null) {
                advs = Collections.singletonList(hint).iterator();
            } else {
                // Ok extract from the CM
                advs = routeCM.getRouteAdv(destPeer);
            }
            
            // Check if we got any advertisements
            List addrs = new ArrayList();

            while (advs.hasNext()) {
                RouteAdvertisement adv = (RouteAdvertisement) advs.next();

                String saddr = null;

                // add the destination endpoint
                for (Enumeration e = adv.getDest().getEndpointAddresses(); e.hasMoreElements();) {
                    try {
                        saddr = (String) e.nextElement();
                        addrs.add(new EndpointAddress(saddr));
                    } catch (Throwable ex) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug(" bad address in route adv : " + saddr);
                        }
                    }
                }
            }

            // ok let's go and try all these addresses
            if (!addrs.isEmpty()) {
                Messenger bestMessenger = findBestReachableEndpoint(destPeer, addrs, exist);

                if (bestMessenger != null) {

                    // Found a direct route. Return it.
                    // Tried+failed has been cleaned.
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("found direct route");
                    }
                    return bestMessenger;
                }
            }
            
            // We're done trying. Since we did not find anything at all,
            // the triedFailed record is still set to infinite value.
            // Reset it to finite.
            // There is a small chance that another thread did find
            // something in parallel, but that's very unlikely and
            // if it is rare enough then the damage is small.
            synchronized (this) {
                triedAndFailed.put(destPeer, new Long(TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)));
            }
        } catch (Throwable e) {
            // If something weird happened be conservative and set a standard
            // finite timeout.
            synchronized (this) {
                triedAndFailed.put(destPeer, new Long(TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)));
            }
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failure looking for an address ", e);
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("did not find a direct route to :" + destPeer);
        }
        
        return null;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Messenger getMessenger(EndpointAddress addr, Object hint) {
        
        RouteAdvertisement routeHint = null;
        EndpointAddress plainAddr = new EndpointAddress(addr, null, null);
        
        // If the dest is the local peer, just loop it back without going
        // through the router.
        if (plainAddr.equals(localPeerAddr)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("return LoopbackMessenger");
            }
            return new LoopbackMessenger(endpoint, localPeerAddr, addr, addr);
        }
        
        try {
            // try and add that hint to our cache of routes (that may be our only route).
            if (hint != null && hint instanceof RouteAdvertisement) {
                routeHint = (RouteAdvertisement) ((RouteAdvertisement) hint).clone();
                AccessPointAdvertisement firstHop = routeHint.getFirstHop();
                PeerID firstHopPid = null;
                EndpointAddress firstHopAddr = null;
                
                // If the firstHop is equal to the destination, clean that up,
                // that's a direct route. If the first hop is the local peer
                // leave it there but treat it as a local route. That's what
                // it is from the local peer point of view.
                if (firstHop != null) {
                    
                    firstHopPid = firstHop.getPeerID();
                    firstHopAddr = pid2addr(firstHopPid);
                    
                    if (firstHopAddr.equals(addr)) {
                        routeHint.removeHop(firstHopPid);
                        firstHop = null;
                    } else if (firstHopPid.equals(localPeerId)) {
                        firstHop = null;
                    }
                    
                }
                
                if (firstHop == null) {
                    // The hint is a direct route. Make sure that
                    // we have the route adv so that we can actually connect.
                    
                    // we only need to publish this route if
                    // we don't know about it yet.
                    
                    EndpointAddress da = pid2addr(routeHint.getDestPeerID());

                    if (!(isLocalRoute(da) || routedRoutes.containsKey(da))) {
                        routeCM.publishRoute(routeHint);
                    }
                    
                } else {
                    // For the hint to be usefull, we must actively try the first hop.
                    // It is possible that we do not know it yet and that's not a reason
                    // to ignore the hint (would ruin the purpose in most cases).
                    RouteAdvertisement routeFirstHop = null;
                    
                    // Manufacture a RA just that as just the routerPeer
                    // as a destination.
                    // we only need to publish this route if
                    // we don't know about it yet.
                    if (!(isLocalRoute(firstHopAddr) || routedRoutes.containsKey(firstHopAddr))) {
                        
                        routeFirstHop = (RouteAdvertisement)
                                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
                        routeFirstHop.setDest((AccessPointAdvertisement) firstHop.clone());
                        
                        // Here we used to pass a second argument with
                        // value true which forced updateRouteAdv to
                        // ignore a pre-existing identical adv and
                        // remove negative cache information anyway.
                        // The reason for doing that was that
                        // sometimes the new route adv does already
                        // exist but has not yet been tried. We cannot
                        // do that; it exposes us too much to retrying
                        // incessantly the same address. A hint cannot
                        // be trusted to such an extent. The correct
                        // remedy is to be able to tell accurately if
                        // there really is an untried address in that
                        // radv, which requires a sizeable refactoring.
                        // in the meantime just let the negative cache
                        // play its role.
                        updateRouteAdv(routeFirstHop);
                    }
                    
                    // if we constructed the route hint then passes it
                    // in the past we were just relying on the CM
                    // now that the CM can be disabled, we have to
                    // pass the argument.
                    if (ensureLocalRoute(firstHopAddr, routeFirstHop) != null) {
                        setRoute((RouteAdvertisement) routeHint.clone(), false);
                    }
                }
            }
            
        } catch (Throwable ioe) {
            // Enforce a stronger semantic to hint. If the application passes
            // a hint that is rotten then this is an application problem
            // we should not try to fix what was given to us.
            return null;
        }
        
        try {
            // Build a persistent RouterMessenger around it that will add our header.
            // If a hint was passed to us we just use it as it. To bad if it is not the
            // the right one. In that mode it is the responsability of the application
            // to make sure that a correct hint was passed
            return new RouterMessenger(getPublicAddress(), addr, this, routeHint);
        } catch (IOException caught) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Can't generate messenger for addr " + addr, caught );
            }
            return null;
        }
    }
    
    /**
     * Updates the router element of a message and returns the pid address of the next
     * hop (where to send the message).
     * Currently, address message is only called for messages that we originate. As a result
     * we will always agressively seek a route if needed.
     * @param message the message for which to compute/update a route.
     * @param destAddress the final destination of the route which the message be set to follow.
     * @return EndpointAddress The address (logical) where to send the message next. Null if there
     * is nowhere to send it to.
     **/
    
    EndpointAddress addressMessage(Message message, EndpointAddress dstAddress) {
        if (endpoint == null) {
            return null;
        }

        // We need to create a RouterMessage
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Create a new EndpointRouterMessage " + dstAddress);
        }
        
        // Specify that we do not want an existing msg parsed.
        EndpointRouterMessage routerMsg = new EndpointRouterMessage(message, true);
        
        if (routerMsg.isDirty()) {
            
            // Oops there was one in the message already. This must be
            // a low-level protocol looping back through the
            // router. The relay can be led to do that in some corner
            // cases
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Possible transport recursion");
            }
            throw new
                    IllegalTransportLoopException("RouterMessage element already present");
        }
        
        routerMsg.setSrcAddress(localPeerAddr);
        routerMsg.setDestAddress(dstAddress);
        
        EndpointAddress theGatewayAddress = null;
        EndpointAddress dstAddressPlain = null;
        
        try {
            RouteAdvertisement route = null;
            
            theGatewayAddress = getGatewayAddress(dstAddress, true);

            if (theGatewayAddress == null) {
                // Cleanup the message, so that the invoker
                // may retry (with a different hint, for example).
                routerMsg.clearAll();
                routerMsg.updateMessage();
                return null;
            }

            dstAddressPlain = new EndpointAddress(dstAddress, null, null);
            
            // Check that we're actually going through a route; we could have one
            // but not be using it, because we know of a volatile shortcut.
            
            // FIXME: jice@jxta.org - 20030512: This is not very clean:
            // getGatewayAddress should be giving us the route that it's using, if any.
            // By doing the fetch ourselves, not only do we waste CPU hashing
            // twice, but we could also get a different route !
            
            if (!theGatewayAddress.equals(dstAddressPlain)) {
                route = getRoute(dstAddressPlain, false);
            }
            
            // If we're going through a route for that, stuff it in the
            // message. NB: setForwardHops does not clone.
            if (route != null) {
                routerMsg.setForwardHops((Vector)
                        route.getVectorHops().clone());
            }
            
            // set the last hop info to point to the local peer info
            // The recipient takes last hop to be the last peer that the message has traversed
            // before arriving.
            routerMsg.setLastHop(localPeerAddr.toString());
            
            // The first time we talk to a peer to which we have
            // initiated a connection, we must include our local
            // route adv in the routerMsg.
            RouteAdvertisement myRoute = getMyLocalRoute();

            if (myRoute != null) {
                // FIXME - jice@jxta.org 20040430 : use destinations instead of newDestinations, even for routed ones.
                boolean newDest = newDestinations.remove(dstAddressPlain);
                boolean newGatw = destinations.isWelcomeNeeded(theGatewayAddress);

                if (newDest || newGatw) {
                    routerMsg.setRouteAdv(myRoute);
                }
            }
            
            // Push the router header onto the message.
            // That's all we have to do for now.
            
            routerMsg.updateMessage();
            
        } catch (Exception ez1) {
            // Not much we can do
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not fully address message", ez1);
            }
            return null;
        }
        
        return theGatewayAddress;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Object transportControl(Object operation, Object value) {
        if (!(operation instanceof Integer)) {
            return null;
        }
        
        int op = ((Integer) operation).intValue();
        
        switch (op) {
            
        case RouteControlOp: // Get a Router Control Object
            return new RouteControl(this, localPeerId);
                
        default:
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Invalid Transport Control operation argument");
            }
                
            return null;
        }
    }
    
    /**
     * convert an endpointRouterAddress into a PeerID
     **/
    protected static PeerID addr2pid(EndpointAddress addr) {
        URI asURI = null;
        try {
            asURI = new URI(ID.URIEncodingName, ID.URNNamespace + ":" + addr.getProtocolAddress(), null);
            return (PeerID) IDFactory.fromURI(asURI);
        } catch (URISyntaxException ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Source Address : "+addr);
                LOG.warn("Error converting a source address into a virtual address", ex);
            }
        } catch (ClassCastException cce) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Source Address : "+addr+" URI :"+asURI);
                LOG.warn("Error converting a source address into a virtual address", cce);
            }
        }
        return null;
    }
    
    /**
     * convert a PeerID into an EndpointRouter Address
     **/
    protected static EndpointAddress pid2addr(PeerID pid) {
        return new EndpointAddress(routerPName, pid.getUniqueValue().toString(), null, null);
    }
    
    /**
     * check if it is a new route adv
     **/
    protected void updateRouteAdv(RouteAdvertisement route) {
        updateRouteAdv(route, false);
    }
    
    /**
     * check if it is a new route adv
     **/
    protected void updateRouteAdv(RouteAdvertisement route, boolean force) {
        try {
            PeerID pID = route.getDestPeerID();

            // check if we updated the route
            if (routeCM.updateRoute(route)) {
                // We just dumped an adv for that dest, so we want to do a real check
                // on its new addresses. Remove the entry from the negative cache.
                synchronized (this) {
                    Long nextTry = (Long) triedAndFailed.get(pid2addr(pID));

                    if (nextTry != null) {
                        // only remove if we do not have a pending request (infinite retry)
                        // we take the conservative approach to avoid creating multiple
                        // async thread blocked on the same destination
                        if (nextTry.longValue() <= TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)) {
                            triedAndFailed.remove(pid2addr(pID));
                            notifyAll();
                        }
                    }
                }
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Route for " + pID + " is same as existing route, not publishing it");
                }
                if (force) {
                    synchronized (this) {
                        Long nextTry = (Long) triedAndFailed.get(pid2addr(pID));

                        if (nextTry != null) {
                            // only remove if we do not have a pending request (infinite retry)
                            // we take the conservative approach to avoid creating multiple
                            // async thread blocked on the same destination
                            if (nextTry.longValue() <= TimeUtils.toAbsoluteTimeMillis(MAXASYNC_GETMESSENGER_RETRY)) {
                                triedAndFailed.remove(pid2addr(pID));
                                notifyAll();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to publish route advertisement", e);
            }
        }
    }
    
    /**
     * cleanup any edge peers when trying to forward an SRDI query
     * so we are guaranteed to the best of our knowledge that
     * the peer is a rendezvous. This is not perfect, as it may
     * take time for the peerview to converge but at least
     * we can remove any peers that is not a rendezvous.
     **/
    protected List cleanupAnyEdges(String src, List results) {
        List clean = new ArrayList(results.size());
        PeerID pid = null;
        // put the peerview as a vector of PIDs
        Vector rpvId = routeResolver.getGlobalPeerView();
        
        // remove any peers not in the current peerview
        // these peers may be gone or have become edges
        for (int i = 0; i < results.size(); i++) {
            pid = (PeerID) results.get(i);
            // eliminate the src of the query so we don't resend
            // the query to whom send it to us
            if (src.equals(pid.toString())) {
                continue;
            }
            
            // remove the local also, so we don't send to ourself
            if (localPeerId.equals(pid)) {
                continue;
            }
            
            if (rpvId.contains(pid)) { // ok that's a good RDV to the best
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("valid rdv for SRDI forward " + pid);
                }
                clean.add(pid);
            } else {
                // cleanup our SRDI cache for that peer
                routeResolver.removeSrdiIndex(pid);
            }
        }
        return clean;
    }
    
    /**
     * is there a pending route query for that destination
     *
     * @param addr destination address
     *
     * @return true or false
     */
    protected synchronized boolean isPendingRouteQuery(EndpointAddress addr) {
            return pendingQueries.containsKey(addr);
    }
    
    /**
     * get a pending route query info
     *
     * @param addr destination address
     *
     * @return pending route query info
     */
    protected synchronized ClearPendingQuery getPendingRouteQuery(EndpointAddress addr) {
            return (ClearPendingQuery) pendingQueries.get(addr);
    }
    
    /**
     * Do we have a longue route for that destination
     *
     * @param addr destination address
     *
     * @return true or false
     */
    protected boolean isRoutedRoute(EndpointAddress addr) {
        return routedRoutes.containsKey(addr);
    }
    
    /**
     * Snoop if we have a messenger
     *
     * @param addr destination address
     *
     * @return Messenger
     */
    protected Messenger getCachedMessenger(EndpointAddress addr) {
        return destinations.getCurrentMessenger(addr);
    }
    
    /**
     * Get all direct route destinations
     *
     * @return Iterator iterations of all endpoint destinations
     */
    protected Iterator getAllCachedMessengerDestinations() {
        return  destinations.allDestinations().iterator();
    }
    
    /**
     * Get all long route destinations
     *
     * @return Iterator iterations of all routed route destinations
     */
    protected Iterator getRoutedRouteAllDestinations() {
        return routedRoutes.entrySet().iterator();
    }
    
    /**
     * Get all long route destination addresses
     *
     * @return Iterator iterations of all routed route addresses
     */
    protected Iterator getAllRoutedRouteAddresses() {
       return routedRoutes.keySet().iterator();
    }
    
    /**
     * Get all pendingRouteQuery destinations
     *
     * @return Iterator iterations of all pending route query destinations
     */
    protected Iterator getPendingQueriesAllDestinations() {
        return pendingQueries.entrySet().iterator();
    }
    
    /**
     * Get the route CM cache Manager
     */
    protected RouteCM getRouteCM() {
        return routeCM;
    }
    
    /**
     * Get the route resolver manager
     */
    protected RouteResolver getRouteResolver() {
        return routeResolver;
    }
    
    /**
     * set bad route entry
     *
     * @param addr of the bad route
     * @param badRoute bad route info
     */
    protected synchronized void setBadRoute(EndpointAddress addr, BadRoute badRoute) {
        badRoutes.put(addr, badRoute);
    }
    
    /**
     * get bad route entry
     *
     * @param addr of the bad route
     * @return BadRoute bad route info
     */
    protected synchronized BadRoute  getBadRoute(EndpointAddress addr) {
        return (BadRoute) badRoutes.get(addr);
    }
}

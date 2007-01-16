/*
 *
 * $Id: RouteCM.java,v 1.1 2007/01/16 11:01:48 thomas Exp $
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

/**
 * This class is used to manage a persistent CM cache  of route
 * for the router
 */

package net.jxta.impl.endpoint.router;


import java.net.URI;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.protocol.AccessPointAdvertisement;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.util.TimeUtils;


class RouteCM {
    
    /**
     *    Log4j Logger
     **/
    private static transient final Logger LOG = Logger.getLogger(RouteCM.class.getName());
    
    /**
     * Default expiration time for Route advertisements. This is the amount
     * of time which advertisements will live in caches. After this time, the
     * advertisement should be refreshed from the source.
     **/
    public final static long DEFAULT_EXPIRATION = 20L * TimeUtils.AMINUTE;
    
    /**
     *  Configuration property that disables the usage
     *  of the CM to persistently store and retrieve
     *  Route advertisements as well as cache route advertisements
     *  Only the in-memory route table is used.
     **/
    private boolean useCM = false;
    
    /**
     *  Configuration useCM property specified by the
     *  user and provided in the EndpointRouter service
     * configuration. By default we use the CM.
     **/
    private boolean useCMDesired = true;
    
    /**
     * PeerGroup Service Handle
     **/
    private PeerGroup group = null;
    
    /**
     * Discovery service handle
     **/
    private DiscoveryService discovery = null;
    
    /**
     * EndpointRouter pointer
     **/
    private EndpointRouter router = null;
    
    /**
     * return routeCM usage
     */
    protected boolean useRouteCM() {
        return useCM;
    }
    
    /**
     * disable routeCM usage
     */
    protected void disableRouteCM() {
        useCM = false;
    }
    
    /**
     * disable routeCM usage
     */
    protected void enableRouteCM() {
        useCM = true;
    }
    
    /**
     *  Constructor
     */
    public RouteCM() {}
    
    /**
     * initialize CM route
     */
    public void init(PeerGroup group, ID assignedID, Advertisement impl, EndpointRouter router)
        throws PeerGroupException {
        
        // extract Router service configuration properties
        
        PlatformConfig confAdv = (PlatformConfig) group.getConfigAdvertisement();
        XMLElement paramBlock = null;
        
        if (confAdv != null) {
            paramBlock = (XMLElement) confAdv.getServiceParam(assignedID);
        }
        
        if (paramBlock != null) {
            // get our tunable router parameter
            Enumeration param;
            
            param = paramBlock.getChildren("useCM");
            if (param.hasMoreElements()) {
                useCMDesired = Boolean.getBoolean(((XMLElement) param.nextElement()).getTextValue());
            }
        }
        
        this.group = group;
        
        // save the router object pointer
        this.router = router;
        
    }
    
    /**
     * Make this protocol as up and running.
     * When this method is called, all the services are already registered
     * with the peergroup. So we do not need to delay binding any further.
     * All the public methods, which could be called between init and startApp
     * are defensive regarding the services possibly not being there.
     */
    public int startApp(String[] arg) {
        
        discovery = group.getDiscoveryService();
        
        if (null == discovery) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Endpoint Router start stalled until discovery service available");
            }
            return Module.START_AGAIN_STALLED;
        }
        
        // ok, we are initialized, go ahead and enable CM usage desired
        useCM = useCMDesired;
        
        return 0;
    }
    
    /**
     * Stop the service
     */
    public void stopApp() {
        discovery = null;
    }
    
    /**
     * get route advertisements from the local discovery cache.
     * We collect straight RouteAdvertisements as well as what can be
     * found in PeerAdvertisements.
     * We can find both, and there's no way to know which is most relevant,
     * so we have to return all and let the invoker try its luck with each.
     *
     * @param pId the target peer's logical address
     * @return Iterator of advertisements (route, peer)
     */
    protected Iterator getRouteAdv(EndpointAddress pId) {
        
        // check if we use the CM, if not then nothing
        // to retrieve
        if (!useCM) {
            return null;
        }
        
        // What we refer to in the router as PeerIDs, are generaly not
        // peerIDs, they're endpoint addresses based on a peer ID.
        String realPeerID = null;
        
        try {
            URI asUri = new URI(ID.URIEncodingName, ID.URNNamespace + ":" + pId.getProtocolAddress(), null);
            
            realPeerID = asUri.toString();
        } catch (URISyntaxException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("bad peer address: " + pId);
            }
            return null;
        }
        
        List result = new ArrayList(2);
        
        try {
            // check first if we have a route advertisement
            Enumeration advs = discovery.getLocalAdvertisements(DiscoveryService.ADV, "DstPID", realPeerID);
            
            while (advs.hasMoreElements()) {
                RouteAdvertisement adv = (RouteAdvertisement) advs.nextElement();
                
                if (!result.contains(adv)) {
                    result.add(adv);
                }
            }
        } catch (Exception e1) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("  failed with ", e1);
            }
            // Try to continue with peer advs.
        }
        
        try {
            // get the local peer advertisements
            Enumeration advs = discovery.getLocalAdvertisements(DiscoveryService.PEER, "PID", realPeerID);
            
            // extract the Route Advertisement
            while (advs.hasMoreElements()) {
                PeerAdvertisement adv = (PeerAdvertisement) advs.nextElement();
                
                // Get its EndpointService advertisement
                XMLElement endpParam = (XMLElement) adv.getServiceParam(PeerGroup.endpointClassID);
                
                if (endpParam == null) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("no Endpoint Params");
                    }
                    continue;
                }
                
                // get the Route Advertisement element
                Enumeration paramChilds = endpParam.getChildren(RouteAdvertisement.getAdvertisementType());
                XMLElement param = null;
                
                if (paramChilds.hasMoreElements()) {
                    param = (XMLElement) paramChilds.nextElement();
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("no Route Adv in Peer Adv");
                    }
                    continue;
                }
                
                // build the new route and publish it
                try {
                    RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);
                    
                    route.setDestPeerID(adv.getPeerID());
                    if (!result.contains(route)) {
                        
                        // We get a new route just publish it locally
                        try {
                            discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
                        } catch (IOException failed) {
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("Failed publishing route");
                            }
                        }
                        
                        result.add(route);
                    }
                } catch (Exception ex) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Error processing route from padv", ex);
                    }
                }
            }
        } catch (Exception e2) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("  failed with ", e2);
            }
        }
        
        return result.iterator();
    }
    
    /**
     * Create a new persistent route to the cache only if we can find
     * set of endpoint addresses
     *
     * @param route to be published
     **/
    protected void createRoute(RouteAdvertisement route) {
        
        // check if CM is used
        if (!useCM) {
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("try to publish route ");
        }
        // we need to retrieve the current adv to get all the known
        // endpoint addresses
        try {
            RouteAdvertisement newRoute = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

            PeerID pId = route.getDestPeerID();

            String realPeerID = pId.toString();

            // check first if we have a route advertisement
            Enumeration advs = discovery.getLocalAdvertisements(DiscoveryService.ADV, "DstPID", realPeerID);

            if ((advs == null) || (!advs.hasMoreElements())) {
                // No route, sorry
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("could not find a route advertisement " + realPeerID);
                }
                return;
            }

            // make sure we are returning the longest route we know either
            // from the peer or route advertisement
            Advertisement adv = (Advertisement) advs.nextElement();
            if (adv instanceof RouteAdvertisement)  {
                RouteAdvertisement dest = (RouteAdvertisement) adv;
                newRoute.setDest((AccessPointAdvertisement) dest.getDest());
            }

            // let's get the endpoint addresses for each hops
            Vector newHops = new Vector();

            for (Enumeration e = route.getHops(); e.hasMoreElements();) {
                AccessPointAdvertisement ap = (AccessPointAdvertisement) e.nextElement();

                realPeerID = ap.getPeerID().toString();

                // check first if we have a route advertisement
                advs = discovery.getLocalAdvertisements(DiscoveryService.ADV, "DstPID", realPeerID);
                if ((advs == null) || (!advs.hasMoreElements())) {
                    // No route, sorry
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("could not find a route advertisement for hop " + realPeerID);
                    }
                    return;
                }
                adv = (Advertisement) advs.nextElement();
                //  Can't always assume only RouteAdvertisement define DstPID
                if (adv instanceof RouteAdvertisement)  {
                    newHops.add( ((RouteAdvertisement) adv).getDest());
                }
            }

            // last check to see that we have a route
            if (newHops.size() == 0) {
                return;
            }

            newRoute.setHops(newHops);

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("publishing new " + newRoute.display());
            }

            discovery.publish(newRoute, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("error publishing route" + route.display(), ex);
            }
        }
    }
    
    /**
     *  Publish a route advertisement to the CM
     *
     *  @param route advertisement to be published
     **/
    protected void publishRoute(RouteAdvertisement route) {
        
        // check if CM is in used, if not nothing to do
        if (!useCM) {
            return;
        }
        
        if (route == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No publishing null route argument");
            }
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Publishing route for " + route.getDestPeerID());
        }
        
        // publish route adv
        try {
            discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("error publishing route adv \n" + route, ex);
            }
        }
    }
    
    /**
     * flush route adv from CM
     *
     * @param addr  endpoint address
     */
    protected void flushRoute(EndpointAddress addr) {
        
        // check if CM is in used, if not nothing to do
        if (!useCM) {
            return;
        }
        
        // leqt's remove any advertisements (route, peer) related to this peer
        // this should force a route query to try to find a new route
        // check first if we have a route advertisement
        String realPeerID;
        
        try {
            URI asUri = new URI(ID.URIEncodingName, ID.URNNamespace + ":" + addr.getProtocolAddress(), null);
            
            realPeerID = asUri.toString();
        } catch (URISyntaxException ex) { // ok if not there.
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Bad peer id : " + addr, ex);
            }
            
            return;
        }

        try {
            Enumeration advs = discovery.getLocalAdvertisements(DiscoveryService.ADV, "DstPID", realPeerID);
            if (advs.hasMoreElements()) {
                Advertisement badRouteAdv = (Advertisement) advs.nextElement();
                if (badRouteAdv instanceof RouteAdvertisement)  {
                    // ok so let's delete the advertisement
                    discovery.flushAdvertisement(badRouteAdv);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("remove RouteAdvertisement " + ((RouteAdvertisement) badRouteAdv).display());
                    }
                }
            }
        } catch (IOException ex) {// ok if not there.
            // protect against flush IOException when the entry is not there
        }
        
        try {
            // let's remove the peer advertisement
            discovery.flushAdvertisements(realPeerID, DiscoveryService.PEER);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("removed PeerAdvertisement " + realPeerID);
            }
        } catch (IOException ex) {// ok, if not there
            // protect against flush IOException when the entry is not there
        }
    }
    
    /**
     * publish or update new route from the advertisement cache
     *
     * @param route to be published or updated
     * @return boolean  true or false if adv cache was updated
     */
    protected boolean updateRoute(RouteAdvertisement route) {
        // check if CM is in used
        if (!useCM) {
            return false;
        }

        try {
            String realPeerID = route.getDestPeerID().toString();

            // check first if we have a route advertisement
            Enumeration advs = discovery.getLocalAdvertisements(DiscoveryService.ADV, "DstPID", realPeerID);

            if ((advs != null) && (advs.hasMoreElements())) {
                Advertisement adv = (Advertisement) advs.nextElement();
                if (adv instanceof RouteAdvertisement)  {
                    RouteAdvertisement oldRouteAdv = (RouteAdvertisement) adv;
                    // check if the old route is equal to the new route
                    if (!route.equals(oldRouteAdv)) {
                        // publish the new route
                        discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
                        return true;
                    }
                }
            } else {
                // publish the new route
                discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
                return true;
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("  failure to publish route advertisement  response" + e);
            }
        }

        return false;
    }
}


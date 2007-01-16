/*
 *
 * $Id: RouteAdvertisement.java,v 1.1 2007/01/16 11:01:34 thomas Exp $
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


package net.jxta.protocol;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.util.Enumeration;
import java.util.Vector;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.endpoint.EndpointAddress;

/**
 * This type of advertisement is used to represent a route to a
 * destination peer in the JXTA virtual network. Routes are
 * represented in a generic manner as a sequence of hops to reach the
 * destination. Each hop represent a potential relay peer in the route:
 *
 * <pre> Dest
 *       hop 1
 *       hop 2
 *       hop 3
 *       ....
 *       hop n
 *</pre>
 *
 * <p/>A route can have as many hops as necessary. Hops are implicitly
 * ordered starting from the hop with the shortest route to reach the
 * destination. If a peer cannot reach directly the dest, it should
 * try to reach in descending order one of the listed hops. Some hops
 * may have the same physical distance to the destination. Some hops may
 * define alternative route
 *
 * <p/>The destination and hops are defined using an AccessPoint
 * Advertisement as JXTA PeerIDs with a list of optional endpoint
 * addresses. The endpoint addresses defined the physical endpoint
 * addresses that can be used to reach the corresponding
 * hop.  The PeerID information is required. The endpoint address
 * information is optional.
 *
 * @see net.jxta.protocol.PeerAdvertisement
 * @see net.jxta.protocol.AccessPointAdvertisement
 *
 **/
public abstract class RouteAdvertisement extends ExtendableAdvertisement implements Cloneable {
    
    public static final String DEST_PID_TAG = "DstPID";

    /**
     *  Destination address
     **/
    private PeerID  destPeer = null;
    
    /**
     *  Destination address
     **/
    private AccessPointAdvertisement dest = null;
    
    /**
     *  Alternative hops to the destination
     **/
    private Vector hops = new Vector();
    private transient ID hashID = null;
    
    /**
     *  {@inheritDoc}
     */
    public Object clone() {
        try {
            return super.clone();
        } catch( CloneNotSupportedException impossible ) {
            throw new Error( "Object.clone() threw CloneNotSupportedException", impossible );
        }
    }
    
    /**
     * makes a copy of a route advertisement
     * that only contains PID not endpoint addresses
     *
     * @return object clone route advertisement
     **/
    public Object cloneOnlyPIDs() {
        RouteAdvertisement a;
        try {
            a = (RouteAdvertisement) super.clone();
            a.setDestEndpointAddresses(new Vector());
        } catch (CloneNotSupportedException impossible) {
            return null;
        }
        
        // deep copy of the hops
        Vector clonehops = getVectorHops();
        int size = clonehops.size();
        for (int i = 0; i < size; ++i) {
            clonehops.set( i, ((AccessPointAdvertisement) clonehops.get(i)).clone() );
        }
        
        a.setHops( clonehops );
        
        return a;
    }   
    
    /**
     * Compare if two routes are equals. Equals means
     * the same number of hops and the same endpoint addresses
     * for each hop and the destination
     *
     * @param target  the route to compare against
     * @return boolean true if the route is equal to this route otherwise false
     */
    public boolean equals(Object target) {
        
        if( this == target ) {
            return true;
        }
        
        if( !(target instanceof RouteAdvertisement)) {
            return false;
        }
        
        RouteAdvertisement route = (RouteAdvertisement) target;

        // check each of the hops
        // routes need to have the same size
        if (hops.size() != route.size()) {
            return false;
        }
        
        int index = 0;
        for (Enumeration e = route.getHops(); e.hasMoreElements();) {
            AccessPointAdvertisement hop = (AccessPointAdvertisement) e.nextElement();
            if (!hop.equals((AccessPointAdvertisement) hops.elementAt(index++))) {
                return false;
            }
        }
 
        if (dest == null && route.getDest() == null)
            return true;
        
        if (dest == null || route.getDest() == null)
            return false;
        
        // chek the destination
        return dest.equals(route.getDest());
    }
    
    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public final static String getAdvertisementType() {
        return "jxta:RA" ;
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized ID getID() {
        if (hashID == null) {
            try {
                // We have not yet built it. Do it now
                // The group id is somewhat redundant since it is already
                // part of the peer ID, but that's the way CodatID want it.
                PeerID pid = dest.getPeerID();
                byte[] seed = getAdvertisementType().getBytes();
                InputStream in =
                new ByteArrayInputStream(pid.toString().getBytes());
                
                hashID =
                IDFactory.newCodatID((PeerGroupID) pid.getPeerGroupID(),
                seed,
                in);
            } catch (Exception ez) {
                return ID.nullID;
            }
        }
        return hashID;
    }
    
    /**
     * Returns the destination access point
     *
     * @return AccessPointAdvertisement
     */
    
    public AccessPointAdvertisement getDest() {
        return dest;
    }
    
    /**
     * Sets the access point of the destination
     *
     * @param ap AccessPointAdvertisement of the destination peer
     */
    
    public void setDest(AccessPointAdvertisement ap) {
        this.dest = ap;
        
        if((null != dest) && (null != dest.getPeerID()) ) {
            setDestPeerID( dest.getPeerID() );
        }
    }
    
    /**
     * returns the list of hops
     *
     * @return Enumeration list of hops as AccessPointAdvertisement
     */
    
    public Enumeration getHops() {
        return hops.elements();
    }
    
    /**
     * returns the list of hops
     *
     * @return Vectorlist of hops as AccessPointAdvertisement
     */
    
    public Vector getVectorHops() {
        return hops;
    }   
    
    /**
     * sets the list of hops associated with this route
     *
     * @param hopsAccess Enumeration of hops as AccessPointAdvertisement
     * The vector of hops is specified as a vector of AccessPoint
     * advertisement.
     */
    
    public void setHops(Vector hopsAccess) {
        // It is legal to set it to null but it is automatically converted
        // to an empty vector. The member hops is NEVER null.
        hops = hopsAccess != null ? hopsAccess : new Vector();
    }
    
    /** add a new list of EndpointAddresses to the Route Destination access
     * point
     *
     * @deprecated Use {@link #getDest()} and modify AccessPointAdvertisement directly.
     *
      *@param addresses vector of endpoint addresses to add to the
     * destination access point. Warning: The vector of endpoint addresses
     * is specified as a vector of String. Each string representing
     * one endpoint address.
     */
    
    public void addDestEndpointAddresses(Vector addresses) {
        dest.addEndpointAddresses(addresses);
    }
    
    /**
     * remove a list of EndpointAddresses from the Route Destination
     * access point
     *
     * @deprecated Use {@link #getDest()} and modify AccessPointAdvertisement directly.
     *
     * @param addresses vector of endpoint addresses to remove from the
     * destination access point. Warning: The vector of endpoint addresses is
     * specified as a vector of String. Each string representing one
     * endpoint address.
     */
    
    public void removeDestEndpointAddresses(Vector addresses) {
        dest.removeEndpointAddresses(addresses);
    }
    
    
    /**
     * Returns the access point for the first hop
     *
     * @return AccessPointAdvertisement first hop
     */
    
    public AccessPointAdvertisement getFirstHop() {
        if (hops == null || hops.isEmpty())
            return null;
        else
            return (AccessPointAdvertisement) hops.firstElement();
    }
    
    /**
     * Sets the access point for the first hop
     *
     * @param ap AccessPointAdvertisement of the first hop
     *
     */
    
    public void setFirstHop(AccessPointAdvertisement ap) {
        hops.add(0, ap);
    }
    
    /**
     * Returns the access point for the last hop
     *
     * @return AccessPointAdvertisement last hop
     */
    
    public AccessPointAdvertisement getLastHop() {
        if (hops == null || hops.isEmpty())
            return null;
        else
            return (AccessPointAdvertisement) hops.lastElement();
    }
    
    /**
     * Sets the access point for the last hop
     *
     * @param ap AccessPointAdvertisement of the last hop
     *
     */
    
    public void setLastHop(AccessPointAdvertisement ap) {
        hops.addElement(ap);
    }
    
    /**
     * Returns the route destination Peer ID
     *
     * @return peerID of the destination of the route
     */
    
    public PeerID getDestPeerID() {
        return destPeer;
    }
    
    
    /**
     * Sets the route destination peer id
     *
     * @param pid route destination peerID
     *
     */
    
    public void setDestPeerID(PeerID pid) {
        destPeer = pid;
        
        if( null != dest )
            dest.setPeerID( pid );
    }
    
    /**
     * Set the route destination endpoint addresses
     *
     * @deprecated Use {@link #getDest()} and modify AccessPointAdvertisement directly.
     *
     * @param ea vector of endpoint addresses. Warning: The vector of endpoint
     * addresses is specified as a vector of String. Each string
     * representing one endpoint address.
     *
     */
    
    public void setDestEndpointAddresses(Vector ea) {
        dest.setEndpointAddresses(ea);
    }
    
    /**
     * Return the endpoint addresses of the destination
     *
     * @deprecated Use {@link #getDest()} and modify AccessPointAdvertisement directly.
     *
     * @return vector of endpoint addresses as String. <b>This is live data.</b>
     */
    public Vector getDestEndpointAddresses() {
        return dest.getVectorEndpointAddresses();
    }
    
    /**
     * Check if the route contains the following hop
     *
     * @param pid peer id of the hop
     * @return boolean true or false if the hop is found in the route
     */
    
    public boolean containsHop(PeerID pid) {
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement hop = (AccessPointAdvertisement)
            e.nextElement();
            PeerID hid = hop.getPeerID();
            
            if (hid == null) {
                continue; //may be null
            }
            
            if (pid.equals(hid)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * check if the route has a loop
     *
     * @return boolean true or false if the route has a loop
     */
    
    public boolean hasALoop() {
        // Now check for any other potential loops.
        Vector peers = new Vector();
        for (int i=0; i < hops.size(); ++i) {
            try {
                PeerID pid = ((AccessPointAdvertisement)
                hops.elementAt(i)).getPeerID();
                if (pid == null)
                    return true; //bad route
                if (peers.contains(pid)) {
                    // This is a loop.
                    return true;
                } else {
                    peers.add(pid);
                }
            } catch (Exception ez1) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * return the length of the route
     *
     * @return int size of the route
     */
    
    public int size() {
        return hops.size();
    }
    
    /**
     * get the nexthop after the given hop
     *
     * @param pid PeerID of the current hop
     * @return ap AccessPointAdvertisement of the next Hop
     */
    
    public AccessPointAdvertisement nextHop(PeerID pid) {
        
        AccessPointAdvertisement nextHop = null;
        
        // check if we have a real route
        if ((hops == null) || (hops.size() == 0)) {
            // Empty vector.
            return null;
        }
        
        // find the index of the route
        int index = 0;
        boolean found = false;
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement ap =
            (AccessPointAdvertisement) e.nextElement();
            if (pid.toString().equals(ap.getPeerID().toString())) {
                found = true;
                break;
            }
            index++;
        }
        
        // check if we found the local peer within the vector
        
        if (!found) {
            // The peer is not into the vector. Since we have got that
            // message, the best we can do is to send it to the first gateway
            // in the forward path.
            try {
                nextHop =  (AccessPointAdvertisement) hops.elementAt(0);
            } catch (Exception ez1) {
                // Should not fail, but if it does, there is not much we can do
                return null;
            }
            return nextHop;
        }
        // Found the peer within the vector of hops. Get the next
        // hop
        try {
            nextHop =  (AccessPointAdvertisement) hops.elementAt(index + 1);
        } catch (Exception ez1) {
            // There is no next hop
            return null;
        }
        return nextHop;
    }
    
    /**
     * Generate a string that displays the route
     * information for logging or debugging purpose
     *
     * @return String return a string containing the route info
     */
    public String display() {
        
        StringBuffer routeBuf = new StringBuffer();
        
        routeBuf.append( "Route to PID=" );
        
        PeerID peerId = getDest().getPeerID();
        if (peerId == null)
            routeBuf.append("Null Destination");
        else
            routeBuf.append(peerId.toString());
        
        for (Enumeration e = getDest().getEndpointAddresses();
        e.hasMoreElements();) {
            try {
                routeBuf.append( "\n Addr=" +(String) e.nextElement());
            } catch (ClassCastException ex) {
                routeBuf.append( "\n Addr=bad address");
            }
        }
        
        int i = 1;
        for (Enumeration e = getHops(); e.hasMoreElements();) {
            if (i == 1)
                routeBuf.append( "\n Gateways = " );
            peerId = ((AccessPointAdvertisement)
            e.nextElement()).getPeerID();
            if (peerId == null)
                routeBuf.append("Null Hop");
            else
                routeBuf.append( "\n\t[" + i++ + "] " + peerId.toString());
        }
        return routeBuf.toString();
    }
    
    /**
     * remove a hop from the list of hops
     *
     * @param pid peer id of the hop
     * @return boolean true or false if the hop is found in the route
     */
    public boolean removeHop(PeerID pid) {
        
        // FIXME: This is ridiculous, hops is a vector. We can remove
        // any item, we do not have to through the enum copying items 1 by 1.
        
        Vector newHops = new Vector();
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement hop = (AccessPointAdvertisement)
            e.nextElement();
            PeerID hid = hop.getPeerID();
            if (hid != null) {
                if (pid.toString().equals(hid.toString()))
                    continue;
            }
            // add the other one
            newHops.add(hop);
        }
        setHops(newHops);
        return true;
    }
    
    
    /**
     * return a hop from the list of hops
     *
     * @param pid peer id of the hop
     * @return accesspointadvertisement of the corresponding hop
     */
    public AccessPointAdvertisement getHop(PeerID pid) {
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement hop = (AccessPointAdvertisement)
            e.nextElement();
            PeerID hid = hop.getPeerID();
            if (hid != null) {
                if (pid.toString().equals(hid.toString()))
                    return (AccessPointAdvertisement) hop.clone();
            }
        }
        return null;
    }
    
    /**
     * replace a hop from the list of hops
     *
     * @param ap accesspointadvertisement of the hop to replace
     */
    public void replaceHop(AccessPointAdvertisement ap) {
        int index = 0;
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            AccessPointAdvertisement hop = (AccessPointAdvertisement)
            e.nextElement();
            PeerID hid = hop.getPeerID();
            if (hid != null) {
                if (ap.getPeerID().toString().equals(hid.toString())) {
                    hops.setElementAt(ap, index);
                    return;
                }
            }
        }
    }
    
    /**
     * Add a new endpointaddress to a hop
     *
     * @param pid id of the hop
     * @param addr new endpoint address to add
     */
    public void addEndpointAddressToHop(PeerID pid, EndpointAddress addr) {
        Vector ea = new Vector();
        ea.add(addr.toString());
        
        AccessPointAdvertisement oldHop = getHop(pid);
        if (oldHop != null && !oldHop.contains(addr)) {
            oldHop.addEndpointAddresses(ea);
            replaceHop(oldHop);
        }
    }
    
    /**
     * remove an endpointaddress to a hop
     *
     * @param pid id of the hop
     * @param addr new endpoint address to remove
     */
    public void removeEndpointAddressToHop(PeerID pid, EndpointAddress addr) {
        Vector ea = new Vector();
        ea.add(addr.toString());
        
        AccessPointAdvertisement oldHop = getHop(pid);
        if (oldHop != null && !oldHop.contains(addr)) {
            oldHop.removeEndpointAddresses(ea);
            if (oldHop.size() > 0) // we still have some endpoint addresses
                replaceHop(oldHop);
            else
                removeHop(pid);
        }
    }
    
    /**
     * Return hop of the route at location index in the hops list
     *
     * @param index in the list of hops
     * @return hop AccessPointAdvertisement of the hops
     */
    public AccessPointAdvertisement getHop(int index) {
        
        if (index < 0)
            return null;
        
        if (index > hops.size() - 1)
            return null;
        
        return (AccessPointAdvertisement) ((AccessPointAdvertisement)hops.elementAt(index)).clone();
    }
    
    /**
     * construct a new route
     * <p/><b>WARNING hops may be MODIFIED.</b>
     **/
    public static RouteAdvertisement newRoute(PeerID destPid,
    PeerID firsthop,
    Vector hops) {
        
        RouteAdvertisement route = (RouteAdvertisement)
        AdvertisementFactory.newAdvertisement(
        RouteAdvertisement.getAdvertisementType());
        
        // set the route destination
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
        AdvertisementFactory.newAdvertisement(
        AccessPointAdvertisement.getAdvertisementType());
        
        if (destPid == null)
            return null; // messed up destination
        ap.setPeerID(destPid);
        route.setDest(ap);
        
        // set the route hops
        for (Enumeration e = hops.elements(); e.hasMoreElements();) {
            ap = (AccessPointAdvertisement) e.nextElement();
            if (ap.getPeerID() == null)
                return null; // bad route
        }
        route.setHops(hops);
        
        // check if the given first hop is already in the route if not add it
        // (note: we do not expect it to be there, but it is acceptable).
        if (firsthop != null) {
            ap = route.getFirstHop();
            if (ap == null || ! ap.getPeerID().equals(firsthop)) {
                ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(
                AccessPointAdvertisement.getAdvertisementType());
                ap.setPeerID(firsthop);
                route.setFirstHop(ap);
            }
        }
        
        return route;
    }
    
    /**
     * construct a new route, all hops are in the hops parameter.
     **/
    public static RouteAdvertisement newRoute(PeerID destPid, Vector hops) {
        return newRoute(destPid, null, hops);
    }
    
    /**
     * Alter the given newRoute (which does not start from here) by using firstLeg, a known route to whence
     * it starts from. So that the complete route goes from here to the end-destination via firstLeg.
     * public static boolean stichRoute(RouteAdvertisement newRoute,
     **/
    public static boolean stichRoute(RouteAdvertisement newRoute, RouteAdvertisement firstLeg) {
        return stichRoute(newRoute, firstLeg, null);
    }
    
    /**
     * Alter the given newRoute (which does not start from here) by using firstLeg, a known route to whence
     ** it starts from. So that the complete route goes from here to the end-destination via firstLeg
     **  also shortcut the route by removing the local peer
     **/
    public static boolean stichRoute(RouteAdvertisement newRoute,
    RouteAdvertisement firstLeg,
    PeerID localPeer) {
        
        if ( newRoute.hasALoop() )
            return false;
        
        Vector hops = newRoute.getVectorHops();
        
        // Make room
        hops.ensureCapacity(firstLeg.getVectorHops().size() + 1 + hops.size());
        
        // prepend the routing peer unless the routing peer happens to be
        // in the route already. That happens if the routing peer is the relay.
        // or if the route does not have a first leg
        PeerID routerPid = firstLeg.getDest().getPeerID();
        if (newRoute.size() == 0 || (! newRoute.getFirstHop().getPeerID().equals(routerPid))) {
            AccessPointAdvertisement ap = (AccessPointAdvertisement)
            AdvertisementFactory.newAdvertisement(
            AccessPointAdvertisement.getAdvertisementType());
            // prepend the route with the routing peer.
            ap.setPeerID(routerPid);
            hops.add(0, ap);
        }
        
        // prepend the rest of the route
        hops.addAll(0, firstLeg.getVectorHops());
        
        // remove any llop from the root
        cleanupLoop(newRoute, localPeer);
        return true;
    }
    
    /**
     * Remove loops from the route advertisement
     * by shortcuting cycle from the route
     **/
    public static void cleanupLoop(RouteAdvertisement route, PeerID localPeer) {
        
        // Note: we cleanup all enp addresses except for the last hop (which
        // we use to shorten routes often enough).
        // If we end-up removing the last hop, it means that it is the
        // local peer and thus the route ends up with a size 0.
        
        Vector hops = route.getVectorHops();
        Vector newHops = new Vector(hops.size());
        Object lastHop = null;
        
        // Replace all by PID-only entries, but keep the last hop on the side.
        if (hops.size() > 0) {
            lastHop = hops.elementAt(hops.size() - 1);
        }
        hops = ((RouteAdvertisement) route.cloneOnlyPIDs()).getVectorHops();
        
        // remove cycle from the route
        for (int i=0; i < hops.size(); i++) {
            int loopAt = newHops.indexOf(hops.elementAt(i));
            if (loopAt != -1) { // we found a cycle
                
                // remove all entries after loopAt
                for (int j = newHops.size(); --j > loopAt;) {
                    newHops.remove(j);
                }
            } else { // did not find it so we add it
                newHops.add(hops.elementAt(i));
            }
        }
        
        // Remove the local peer in the route if we were given one
        if (localPeer != null) {
            for (int i = newHops.size(); --i >= 0;) {
                if (localPeer.equals(((AccessPointAdvertisement)
                newHops.elementAt(i)).getPeerID())) {
                    // remove all the entries up to that point we
                    // need to keep the remaining of the route from that
                    // point
                    for (int j = 0; j <= i; j++) {
                        newHops.remove(0);
                    }
                    break;
                }
            }
        }
        
        if (lastHop != null && newHops.size() > 0) {
            newHops.setElementAt(lastHop, newHops.size() - 1);
        }
        
        // update the new hops in the route
        route.setHops(newHops);
    }
}

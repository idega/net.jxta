/*
 * $Id: Destinations.java,v 1.1 2007/01/16 11:01:48 thomas Exp $
 *
 * Copyright (c) 2004 Sun Microsystems, Inc.  All rights reserved.
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


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.lang.ref.SoftReference;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Messenger;

import net.jxta.impl.util.TimeUtils;


/**
 * This class is a repository of wisdom regarding destinations. It also provides a messenger if there is one.  Currently, the
 * wisdom is very limited and is only about direct destinations (for which a messenger once existed).  The wisdom that can be
 * obtained is:
 * <ul>
 *
 * <li> is there a messenger at hand (incoming or otherwise).
 *
 * <li> is it likely that one can be made from this end, should the one we have break. (the last attempt succeeded, not only incoming,
 * and that was not long ago).
 *
 * <li> is either of the above true, (are we confident we can get a messenger as of right now one way or the other).
 *
 * <li> are we supposed to send a welcome to that destination (we can't remember having done it).
 *
 * </ul>
 *
 * This could be extended to manage more of the life cycle, such as knowing about messengers
 * being resolved or having failed to. This primitive interface is temporary; it is only meant to replace messengerPool without
 * having to change the router too much.
 **/

class Destinations implements Runnable {

    private Map wisdoms = new HashMap(64);

    private volatile boolean stopped = false;

    private Thread gcThread = null;

    private EndpointService endpoint;

    /**
     * This class stores knowlege about one particular destination.
     * It does not provide any synchronization. This is provided by the Destinations class.
     **/
    class Wisdom {

        /**
         * How long we consider that a past outgoingMessenger is an indication that one is possible in the future.
         **/
        static final long expiration = 10 * TimeUtils.AMINUTE;

        /**
         * The channel we last used, if any. They disappear faster than the canonical, but, as
         * long as the canonical is around, they can be obtained at a near-zero cost.
         **/
        private SoftReference messenger;

        /**
         * The channel we last used if it happens to be an incoming messenger. We keep 
         * a strong reference to it.
         **/
        private Messenger incomingMessenger;

        /**
         * The transport destination address of the messenger we're caching (if not incoming).
         **/
        private EndpointAddress xportDest;

        /**
         * This tells when the outgoing messenger information expires. Incoming messengers have no expiration per se.  We draw no
         * conclusion from their past presence; only current presence. A wisdom is totally expired (and may thus be removed) when
         * its outgoing messenger information is expired AND it has no incoming messenger.
         **/
        private long expiresAt = 0;
        
        /**
         * When a new destination is added, we're supposed to send our welcome along with the first message.
         * This tells whether isWelcomeNeeded was once invoked or not.
         **/
        private boolean welcomeNeeded = true;

        /**
         * @param channel The messenger to cache information about.
         * @param incoming If true, this is an incoming messenger, which means that
         * if the channel is lost it cannot be re-obtained. It must strongly referenced until it
         * closes for disuse, or breaks.
         **/
        Wisdom(Messenger messenger, boolean incoming) {
            if (incoming) {
                addIncomingMessenger(messenger);
            } else {
                addOutgoingMessenger(messenger);
            }
        }

        /**
         * Tells whether a welcome message is needed.
         *
         * The first time we're asked, we say true. Subsequently, always false; assuming it was done when said so (so,
         * ask only if you'll do it).
         * @return true If this is the first time this method is invoked.
         **/
        boolean isWelcomeNeeded() {
            boolean res = welcomeNeeded;

            welcomeNeeded = false;
            return res;
        }

        boolean addIncomingMessenger(Messenger m) {

            // If we have no other incoming, we take it. No questions asked.
            Messenger currentIncoming = getIncoming();

            if (currentIncoming == null) {
                incomingMessenger = m;
                return true;
            }

            // If it is a relay msgr, we take it too (and drop whatever we had).
            String originAddr = m.getDestinationAddress().getProtocolAddress();
            EndpointAddress logDest = m.getLogicalDestinationAddress();   
        
            if (originAddr.equals(logDest.getProtocolAddress())) {
                incomingMessenger = m;
                return true;
            }
                
            // Now, check reachability. If the old one looks better, prefer it.

            // Compute reachability of the new one.
            int srcPort = Integer.parseInt(originAddr.substring(originAddr.lastIndexOf(':') + 1));
            boolean reachable = (srcPort != 0);

            // Compute reachability of the old one.
            originAddr = currentIncoming.getDestinationAddress().getProtocolAddress();
            logDest = currentIncoming.getLogicalDestinationAddress();
            if (originAddr.equals(logDest.getProtocolAddress())) {
                // The old one is a relay messenger or the like. (and not the new one, we checked).
                return false;
            }
            srcPort = Integer.parseInt(originAddr.substring(originAddr.lastIndexOf(':') + 1));
            boolean currentReachable = (srcPort != 0);

            // The new one is less reachable than the old one. Keep the old one.
            if (currentReachable && !reachable) {
                return false;
            }

            incomingMessenger = m;
            return true;
        }

        boolean addOutgoingMessenger(Messenger m) {
            if (getOutgoing() != null) {
                return false;
            }
            this.messenger = new SoftReference(m);
            xportDest = m.getDestinationAddress();
            expiresAt = TimeUtils.toAbsoluteTimeMillis(expiration);
            return true;
        }

        void noOutgoingMessenger() {
            messenger = null;
            xportDest = null;
            expiresAt = 0;
        }

        /**
         * Returns an incoming messenger is there is one that works. Nulls reference to any broken one
         **/
        private Messenger getIncoming() {
            if (incomingMessenger != null) {
                if ((incomingMessenger.getState() & Messenger.USABLE) != 0) {
                    return incomingMessenger;
                }
                incomingMessenger = null;
            }
            return null;
        }

        /**
         * Returns an outgoingMessenger if there is one or one can be made without delay.
         * Renews a broken one if it can be. Refreshes expiration time if a messenger is returned.
         **/
        private Messenger getOutgoing() {

            if (messenger == null) {
                return null;
            }

            // (If messenger is not null, it means that we also have a xportDest).

            Messenger m = (Messenger) messenger.get();

            // If it is gone or broken, try and get a new one.
            if ((m == null) || ((m.getState() & Messenger.USABLE) == 0)) {

                m = endpoint.getMessengerImmediate(xportDest, null);

                // If this fails, it is hopeless: the address is bad or something like that. Make ourselves expired right away.
                if (m == null) {
                    messenger = null;
                    xportDest = null;
                    expiresAt = 0;
                    return null;
                }

                // Renew the ref. The xportDest is the same.
                messenger = new SoftReference(m);
            }

            // So we had one or could renew. But, does it work ?
            if ((m.getState() & (Messenger.USABLE & Messenger.RESOLVED)) == 0) {
                // We no-longer have the underlying connection. Let ourselves expire. Do not renew the expiration time.
                messenger = null;
                xportDest = null;
                return null;
            }

            // Ok, we do have an outgoing messenger at the ready after all.
            expiresAt = TimeUtils.toAbsoluteTimeMillis(expiration);
            return m;
        }

        /**
         * Returns a channel for this destination if one is there or can be obtained
         * readily and works.
         **/
        Messenger getCurrentMessenger() {
            // XXX we use outgoing first. If we have reciprocal connection, the other side will do the same and we'll
            // keep using both. Be nice if there a way to chose that pick the same cnx on both ends.
            Messenger res = getOutgoing();

            if (res != null) {
                return res;
            }
            return getIncoming();
        }

        /**
         * @return true if we do have an outgoing messenger or, failing that, we had one not too long ago.
         **/
        boolean isNormallyReachable() {
            return ((getOutgoing() != null) || (TimeUtils.toRelativeTimeMillis(expiresAt) >= 0));
        }

        /**
         * We think the destination is reachable somehow. Not sure how long.
         *
         * @return true if we have any kind of messenger or, failing that, we had an outgoing one not too long ago.
         **/ 
        boolean isCurrentlyReachable() {
            return ((getIncoming() != null) || (getOutgoing() != null) || (TimeUtils.toRelativeTimeMillis(expiresAt) >= 0));
        }
        
        /**
         * @return true if this wisdom carries no positive information whatsoever.
         **/
        boolean isExpired() {
            return !isCurrentlyReachable();
        }
    }

    /*
     * Internal mechanisms
     */

    private Wisdom getWisdom(EndpointAddress destination) {
        if (destination.getServiceName() != null) {
            destination = new EndpointAddress(destination, null, null);
        }
        return (Wisdom) wisdoms.get(destination);
    }

    private void addWisdom(EndpointAddress destination, Wisdom wisdom) {
        destination = new EndpointAddress(destination, null, null);
        wisdoms.put(destination, wisdom);
    }

    /*
     * General house keeping.
     */
 
    public Destinations(EndpointService endpoint) {

        this.endpoint = endpoint;

        ThreadGroup threadGroup = endpoint.getGroup().getHomeThreadGroup();

        gcThread = new Thread(threadGroup, this, "Destinations gc thread");
        gcThread.setDaemon(true);
        gcThread.start();
    }

    /**
     * Shutdown this cache. (stop the gc)
     **/
    public synchronized void close() {
        stopped = true;
        gcThread.interrupt();
    }

    /**
     * garbage collector. We use soft references to messengers, but we use a strong hashmap to keep the wisdom around in a more
     * predictible manner. Entries are simply removed when they no-longer carry relevant information; so there's no change in the
     * total meaning of the map when an entry is removed.
     **/
    public void run() {
        while (!stopped) {
            try {
                synchronized (this) {
                    Iterator i = wisdoms.values().iterator();

                    while (i.hasNext()) {
                        Wisdom w = (Wisdom) i.next();

                        if (w.isExpired()) {
                            i.remove();
                        }
                    }
                }
                Thread.sleep(TimeUtils.AMINUTE);
            } catch (InterruptedException ie) {
                ;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public synchronized List allDestinations() {

        // We need to make a copy. wisdoms.keySet() would track the changes made to wisdoms, thus breaking any iterator.
        // we just want to return a snapshot. While we're at it, we change it do an ArrayList, which will be cheaper since
        // it is unlikely to be modified.

        Set allKeys = wisdoms.keySet();
        ArrayList res = new ArrayList(allKeys.size());

        res.addAll(allKeys);
        return res;
    }

    /*
     * information output
     */

    /**
     * If there is a messenger at hand (incoming or otherwise), return it.
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @retun A messenger to that destination if a resolved and usable one is available or can be made instantly. null otherwise.
     **/
    public synchronized Messenger getCurrentMessenger(EndpointAddress destination) {
        Wisdom wisdom = getWisdom(destination);

        if (wisdom == null) {
            return null;
        }
        return wisdom.getCurrentMessenger();
    }

    /**
     * Is it likely that one can be made from this end. (the last attempt succeeded, not only incoming, and that was not long ago) ?
     * This is a conservative test. It means that declaring that we can route to that destination is a very safe bet, as opposed
     * to isNormallyReachable and getCurrentMessenger, which could be misleading if the only messenger we can ever get is incoming.
     * Not currently used. Should likely be.
     *
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @return true if it is likely that we can get a messenger to that destination in the future.
     **/
    public synchronized boolean isNormallyReachable(EndpointAddress destination) {
        Wisdom wisdom = getWisdom(destination);

        return ((wisdom != null) && wisdom.isNormallyReachable());
    }

    /**
     * Is there a messenger at hand, or is it likely that we can make one ? (This is more often true than isNormallyReachable, since
     * it can be true even when all we have is an incoming messenger).
     *
     * This is the equivalent of the former "exists()". Just testing that there is an entry is no-longer the same because
     * we may keep the entries beyond the point where we would keep them before, so that we can add some longer-lived
     * information in the future, and do not interfere as much with the gc thread.
     *
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @return true is we are confident that we can obtain a messenger, either because we can get one instantly, or because
     * this destination is normally reachable. (So, it is ok to try and route to that destination, now).
     **/
    public synchronized boolean isCurrentlyReachable(EndpointAddress destination) {
        Wisdom wisdom = getWisdom(destination);

        return ((wisdom != null) && wisdom.isCurrentlyReachable());
    }

    /**
     * Are we supposed to send a welcome to that destination (we can't remember having done it).
     * It is assumed that once true was returned, it will be acted upon. So, true is not returned a second time.
     *
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @return true if this a destination to whish we can't remember sending a welcome message.
     **/
    public synchronized boolean isWelcomeNeeded(EndpointAddress destination) {
        Wisdom wisdom = getWisdom(destination);

        return ((wisdom != null) && wisdom.isWelcomeNeeded());
    }

    /*
     * information input.
     */

    /**
     * Here is a messenger that we were able to obtain.
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @param messenger The incoming messenger for that destination.
     * @return true if this messenger was added (keep it open). false otherwise (do what you want with it).
     **/
    public synchronized boolean addOutgoingMessenger(EndpointAddress destination, Messenger messenger) {
        Wisdom wisdom = getWisdom(destination);

        if (wisdom != null) {
            return wisdom.addOutgoingMessenger(messenger);
        }
        addWisdom(destination, new Wisdom(messenger, false));
        return true;
    }

    /**
     * Here is an incoming messenger that just poped out.
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     * @param messenger The incoming messenger for that destination.
     * @return true if this messenger was added (keep it open). false otherwise (do what you want with it).
     **/
    public synchronized boolean addIncomingMessenger(EndpointAddress destination, Messenger messenger) {
        Wisdom wisdom = getWisdom(destination);

        if (wisdom != null) {
            return wisdom.addIncomingMessenger(messenger);
        }
        addWisdom(destination, new Wisdom(messenger, true));
        return true;
    }

    /**
     * We tried to get a messenger but could not. We know that we do not have connectivity from our end, for now.  we may still
     * have an incoming. However, if we had to try and make a messenger, there probably isn't an incoming, but that's not our
     * business here. isNormallyReachable becomes false; but we can still try when sollicited.
     *
     * @param destination The destination as an endpoint address (is automatically normalized to protocol and address only).
     **/
    public synchronized void noOutgoingMessenger(EndpointAddress destination) {
        Wisdom wisdom = getWisdom(destination);

        if (wisdom != null) {
            wisdom.noOutgoingMessenger();
        }
    }
}


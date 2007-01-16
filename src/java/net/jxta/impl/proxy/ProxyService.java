/*
 * Copyright (c) 2005 Sun Microsystems, Inc.  All rights
 * reserved.
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
 *
 *====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: ProxyService.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
 *
 */

package net.jxta.impl.proxy;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.util.Cache;
import net.jxta.impl.util.CacheEntry;
import net.jxta.impl.util.CacheEntryListener;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.service.Service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// FIXME: jice@jxta.org - 20020515
// All public methods are synchronized.
// None of them does anything blocking so that should be about OK, however
// first it is not 100% sure, second eventhough non-blocking, some of these
// operations could take a significant amount of time, which may be unfair
// to other threads that wish to enter for a quick operation.
// Making the locking finer-grain would require significant code rework, so
// it will have to do for now.

public class ProxyService implements Service,
                                     EndpointListener,
                                     PipeMsgListener,
                                     OutputPipeListener,
                                     CacheEntryListener {
    
    private final static Logger LOG = Logger.getLogger(ProxyService.class.getName());

    protected final static boolean LOG_MESSAGES = true;

    public final static int DEFAULT_THRESHOLD = 2;
    public final static int DEFAULT_LIFETIME = 1000 * 60 * 30; // 30 minutes
    /************************************************************************
     *  Define the proxy message tags
     **********************************************************************/
    public static final String REQUEST_TAG = "request";
    public static final String RESPONSE_TAG = "response";
    
    static final String REQUESTID_TAG = "requestId";
    static final String TYPE_TAG = "type";
    static final String NAME_TAG = "name";
    static final String ID_TAG = "id";
    static final String ARG_TAG = "arg";
    static final String ATTRIBUTE_TAG = "attr";
    static final String VALUE_TAG = "value";
    static final String THRESHOLD_TAG = "threshold";
    static final String ERROR_MESSAGE_TAG = "error";
    static final String PROXYNS = "proxy";
    
    /************************************************************************
     *  Define the proxy request types
     **********************************************************************/
    public static final String REQUEST_JOIN = "join";
    public static final String REQUEST_CREATE = "create";
    public static final String REQUEST_SEARCH = "search";
    public static final String REQUEST_LISTEN = "listen";
    public static final String REQUEST_CLOSE = "close";
    public static final String REQUEST_SEND = "send";
    
    /************************************************************************
     *  Define the proxy response types
     **********************************************************************/
    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_ERROR = "error";
    public static final String RESPONSE_INFO = "info";
    public static final String RESPONSE_RESULT = "result";
    public static final String RESPONSE_MESSAGE = "data";
    
    /************************************************************************
     *  Define the proxy type tags
     **********************************************************************/
    public static final String TYPE_PEER = "PEER";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_PIPE = "PIPE";
    
    private PeerGroup group = null;
    private ID assignedID = null;
    private String serviceName = null;
    private String serviceParameter = null;
    private EndpointService endpoint = null;
    private DiscoveryService discovery = null;
    private PipeService pipe = null;
    private Advertisement implAdv = null;
    
    private Map searchRequests; // Currently unused
    private Map pipeListeners;
    private Cache pendingPipes;
    private Cache resolvedPipes;
    
    private static Map proxiedGroups = new HashMap(16);
    private static Map passwords = new HashMap(16);
    
    /************************************************************************
     *  Methods that are part of the Service Interface
     **********************************************************************/
    public Service getInterface() {
        return this;
    }
    
    public Advertisement getImplAdvertisement() {
        return implAdv;
    }
    
    public void init(PeerGroup group,
    ID assignedID,
    Advertisement implAdv) throws PeerGroupException {
        this.group = group;
        this.assignedID = assignedID;
        this.serviceName = assignedID.toString();
        this.implAdv = implAdv;
        
        serviceParameter = group.getPeerGroupID().toString();
        
        searchRequests = new TreeMap();
        pipeListeners = new TreeMap();
        
        // Pending pipes cost only memory, so it is not a problrm to
        // wait for the GC to cleanup things. No CacheEntryListener.
        pendingPipes = new Cache(200, null);
        
        // Resolved pipes cost non-memory resources, so we need to close
        // them as early as we forget them. Need a CacheEntryListener (this).
        resolvedPipes = new Cache(200, this);
        
    }
    
    public int startApp(String[] args) {
        
        Service needed = group.getEndpointService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a endpoint service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        needed = group.getDiscoveryService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a discovery service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        needed = group.getPipeService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a pipe service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        endpoint = group.getEndpointService();
        discovery = group.getDiscoveryService();
        pipe = group.getPipeService();
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("addListener "+serviceName+serviceParameter);
        }
        
        endpoint.addIncomingMessageListener(this, serviceName, serviceParameter);
        
        return 0;
    }
    
    public void stopApp() {
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("removeListener "+serviceName+serviceParameter);
        }
        
        endpoint.removeIncomingMessageListener(serviceName, serviceParameter);
    }
    
    
    public synchronized void processIncomingMessage(Message message,
    EndpointAddress srcAddr,
    EndpointAddress dstAddr) {
        
        if (LOG_MESSAGES) {
            logMessage(message, LOG);
        }
        
        Requestor requestor = null;
        try {
            // requestor = Requestor.createRequestor(group, message, srcAddr);
            // Commented out the above line and added the following three lines.
            // The change allows to reduce the traffice going to a JXME peer
            // by able to remove ERM completly. As a side effect (severe one)
            // JXTA Proxy and JXTA relay need to be running on the same peer.
            // This changes should be pulled out as soon as ERM is implemented
            // in a more inteligent and effective way so that it doesn't
            // have any impact on JXME peers.
            EndpointAddress relayAddr = srcAddr;
            relayAddr.setProtocolName("relay");
            requestor = Requestor.createRequestor(group, message, relayAddr);
        } catch (IOException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("could not create requestor", e);
            }
        }
        
        String request = popString(REQUEST_TAG, message);
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("request = "+request + " requestor " + requestor);
        }
        
        if (request != null && requestor != null) {
            if (REQUEST_JOIN.equals(request)) {
                handleJoinRequest(requestor,
                popString(ID_TAG, message),
                popString(ARG_TAG, message));
            } else if (REQUEST_CREATE.equals(request)) {
                handleCreateRequest(requestor,
                popString(TYPE_TAG, message),
                popString(NAME_TAG, message),
                popString(ID_TAG, message),
                popString(ARG_TAG, message));
            } else if (REQUEST_SEARCH.equals(request)) {
                handleSearchRequest(requestor,
                popString(TYPE_TAG, message),
                popString(ATTRIBUTE_TAG, message),
                popString(VALUE_TAG, message),
                popString(THRESHOLD_TAG, message));
            } else if ("listen".equals(request)) {
                handleListenRequest(requestor,
                popString(ID_TAG, message));
            } else if ("close".equals(request)) {
                handleCloseRequest(requestor,
                popString(ID_TAG, message));
            } else if ("send".equals(request)) {
                handleSendRequest(requestor,
                popString(ID_TAG, message),
                message);
            }
        }
    }
    
    // Right now there's a security hole: passwd come in clear.
    // And not much is done for stopping clients to use the new group
    // without being authenticated. We also never get rid of these
    // additional groups.
    private synchronized void handleJoinRequest(Requestor requestor,
                                                String grpId,
                                                String passwd) {

        PeerGroup g = (PeerGroup) proxiedGroups.get(grpId);

        if (g != null) {
            if (g == this.group) {
                requestor.notifyError("Same group");
            } else if (! ((String) passwords.get(grpId)).equals(passwd)) {
                requestor.notifyError("Incorrect password");
            } else {
                requestor.notifySuccess();
            }
            return;
        }
        
        try {
            g = group.newGroup((PeerGroupID) IDFactory.fromURI(new URI(grpId)));
            g.getRendezVousService().startRendezVous();
        } catch (Exception ge) {
            requestor.notifyError(ge.getMessage());
            return;
        }

        // XXX check membership here. (would work only for single passwd grps)
        // For now, assume join is always welcome.

         // So far so good. Try to start a proxy in that grp.
         try {
             // Fork this proxy into the new grp.
             ProxyService p = new ProxyService();
             p.init(g, assignedID, implAdv);
             p.startApp(null);
         } catch(Exception e) {
             requestor.notifyError(e.getMessage());
             return;
         }
         // set non-deft passwd
         passwords.put(grpId, passwd);
         proxiedGroups.put(grpId, g);
         requestor.notifySuccess();
    }
    
    /**
     */
    private void handleCreateRequest(Requestor requestor,
                                     String type,
                                     String name,
                                     String id,
                                     String arg) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleCreateRequest type=" + type +
            " name=" + name +
            " id=" + id +
            " arg=" + arg);
        }
        
        if (name == null) {
            name = ""; // default name
        }
        
        if (TYPE_PEER.equals(type)) {
            PeerAdvertisement adv = createPeerAdvertisement(name, id);
            if (adv != null) {
                try {
                    discovery.publish(adv);
                } catch (Exception e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not publish peer advertisement", e);
                    }
                }
                
                requestor.send(adv, RESPONSE_SUCCESS);
            } else {
                requestor.notifyError("could not create advertisement");
            }
        } else if (TYPE_GROUP.equals(type)) {
            PeerGroupAdvertisement adv = createGroupAdvertisement(name, id);
            if (adv != null) {
                try {
                    discovery.publish(adv);
                } catch (Exception e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not publish group advertisement", e);
                        requestor.notifyError("could not create advertisement");
                    }
                }
                
                requestor.send(adv, RESPONSE_SUCCESS);
            } else {
                requestor.notifyError("could not create advertisement");
            }
        } else if (TYPE_PIPE.equals(type)) {
            if (arg == null) {
                arg = PipeService.UnicastType; // default pipe type
            }
            
            PipeAdvertisement adv = createPipeAdvertisement(name, id, arg);
            if (adv != null) {
                try {
                    discovery.publish(adv);
                } catch (Exception e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Could not publish pipe advertisement", e);
                    }
                }
                
                requestor.send(adv, RESPONSE_SUCCESS);
            } else {
                requestor.notifyError("could not create advertisement");
            }
        } else {
            requestor.notifyError("unsupported type");
        }
    }
    
    /**
     */
    private void handleSearchRequest(Requestor requestor,
                                     String type,
                                     String attribute,
                                     String value,
                                     String threshold) {

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleSearchRequest type=" + type +
            " attribute=" + attribute +
            " value=" + value +
            " threshold=" + threshold);
        }
        
        int discoveryType;
        
        if (TYPE_PEER.equals(type)) {
            discoveryType = DiscoveryService.PEER;
        } else if (TYPE_GROUP.equals(type)) {
            discoveryType = DiscoveryService.GROUP;
        } else {
            discoveryType = DiscoveryService.ADV;
        }
        
        Enumeration each = null;
        try {
            each = discovery.getLocalAdvertisements(discoveryType,
            attribute,
            value);
        } catch (IOException e) {
            requestor.notifyError("could not search locally");
        }
        
        Advertisement adv = null;
        while (each != null && each.hasMoreElements()) {
            adv = (Advertisement) each.nextElement();
            
            // notify the requestor of the result
            requestor.send(adv, RESPONSE_RESULT);
        }
        
        int thr = DEFAULT_THRESHOLD;
        try {
            thr = Integer.parseInt(threshold);
        } catch (NumberFormatException nex) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("handleSearchRequest failed to parse threshold " +
                threshold + ", using default " + DEFAULT_THRESHOLD);
            }
        }
        
        // start the query
        int queryId = discovery.getRemoteAdvertisements(null, // peerId
        discoveryType,
        attribute,
        value,
        thr);
        
        // register the query
        // FIXME: jice@jxta.org - 20020515
        // Right now the client API and client-proxy protocol lacks a way
        // to cancel a discovery request, so we have absolutely no way to
        // remove these requests from the list. On top of that, the above
        // does not use the listener interface at all, so we never ever get
        // an event to report to the client, thus the searchRequests list is
        // not usefull. Finally, we have no way to detect redundant queries
        // which puts us at the mercy of silly clients. So, it is better
        // to just not enqueue the request right now. From the client's point
        // of view the difference is not noticeable; all responses look
        // asynchronous, even if respond immediately with a localy found adv.
        // However, if we do not have a local response, the client will
        // never get a response until it retries, and everytime it does
        // it cost us a remote disco :-(
        // Suppressing the following line does neither improve nor worsen that.
        // but it avoids leaking resources.
        
        // searchRequests.put(new Integer(queryId), requestor);
    }
    
    
    /**
     * Finds a JXTA Pipe and starts listening to it.
     *
     * @param requestor the peer sending listen request.
     *
     * @param id the id of the Pipe.
     *
     *
     */
    private void handleListenRequest(Requestor requestor,
    String id) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleListenRequest id=" + id);
        }
        
        if (id == null) {
            requestor.notifyError("Pipe ID not specified");
            return;
        }
        
        PipeAdvertisement pipeAdv = findPipeAdvertisement(null, id, null);
        
        if (pipeAdv == null) {
            requestor.notifyError("Pipe Advertisement not found");
            return;
        }
        
        String pipeId = pipeAdv.getPipeID().toString();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("listen to pipe name=" + pipeAdv.getName() +
            " id=" + pipeAdv.getPipeID() +
            " type=" + pipeAdv.getType());
        }
        
        // check to see if the input pipe already exist
        PipeListenerList list = (PipeListenerList)pipeListeners.get(pipeId);
        if (list == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("first listener, create input pipe");
            }
            
            // create an input pipe
            try {
                list = new PipeListenerList(pipe.createInputPipe(pipeAdv, this),
                pipeListeners, pipeId);
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("could not listen to pipe", e);
                }
                requestor.notifyError("could not listen to pipe");
                return;
            }
            
            pipeListeners.put(pipeId, list);
        }
        
        // add requestor to list
        list.add(requestor);
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("add requestor=" + requestor + " id=" + pipeId +
            " list=" + list);
            LOG.debug("publish PipeAdvertisement");
        }
        // advertise the pipe locally
        try {
            discovery.publish(pipeAdv);
        } catch (IOException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not publish pipe advertisement", e);
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("done with listen request");
        }
        
        // notify requestor of success
        requestor.notifySuccess();
    }
    
    /**
     */
    private void handleCloseRequest(Requestor requestor,
    String id) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleCloseRequest id=" + id);
        }
        
        PipeListenerList list = (PipeListenerList)pipeListeners.get(id);
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleCloseRequest list = " + list);
        }
        if (list != null) {
            list.remove(requestor);
            if (list.size() == 0) {
                pipeListeners.remove(id);
            }
        }
        
        // notify requestor of success
        requestor.notifySuccess();
    }
    
    // Send the given message to the given pipe.
    private void sendToPipe(Requestor req, Message mess, OutputPipe out) {
        try {
            out.send(mess);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("output pipe send end");
            }
            // notify requestor of success
            req.notifySuccess();
        } catch (IOException e) {
            req.notifyError("could not send to pipe");
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("could not send to pipe", e);
            }
            return;
        }
    }
    
    class ClientMessage {
        private Requestor requestor;
        private Message message;
        
        public ClientMessage(Requestor req, Message mess) {
            requestor = req;
            message = mess;
        }
        
        // Send this (pending) message
        public void send(OutputPipe out) {
            sendToPipe(requestor, message, out);
        }
        
    }
    
    class PendingPipe {
        
        private ClientMessage pending;
        
        public PendingPipe() {
            pending = null;
        }
        
        // Just got resolved ! Will send the pending message(s).
        public void sendPending(OutputPipe out) {
            pending.send(out);
            pending = null;
        }
        
        // Enqueue a new pending message.
        // (for now we only enqueue 1; others get trashed)
        public void enqueue(Requestor req, Message mess) {
            if (pending != null) {
                return;
            }
            pending = new ClientMessage(req, mess);
        }
    }
    
    /**
     */
    private void handleSendRequest(Requestor requestor, String id, Message message) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("handleSendRequest id=" + id);
        }
        
        PipeAdvertisement pipeAdv = findPipeAdvertisement(null, id, null);
        
        if (pipeAdv == null) {
            requestor.notifyError("Could not find pipe");
            return;
        }
        
        String pipeId = pipeAdv.getPipeID().toString();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("send to pipe name=" + pipeAdv.getName() +
            " id=" + pipeAdv.getPipeID().toString() +
            " arg=" + pipeAdv.getType());
        }
        
        // check if there are local listeners
        
        PipeListenerList list = (PipeListenerList)pipeListeners.get(pipeId);
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("local listener list " + list);
        }
        
        if (list != null && PipeService.UnicastType.equals(pipeAdv.getType())) {
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("start sending to each requestor");
            }
            
            list.send((Message)message, pipeId);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("end sending to each requestor");
            }
            // notify requestor of success
            requestor.notifySuccess();
            return;
        }
        
        // NOTE: This part is NOT exercised by the load test because all
        // clients are local. To exercise this part, comment out the
        // optimization above.
        
        // This is not a unicast pipe with at least one local listener
        // so we need to fingure out where the message should go.
        // This may take a while and has to be done asynchronously...
        // Carefull that the resolution can occur synchronously by this
        // very thread, and java lock will not prevent re-entry.
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("output pipe creation begin");
        }
        
        // Look for the pipe in the resolved list. If not found
        // look in the pending list or add it there.
        OutputPipe out = (OutputPipe) resolvedPipes.get(pipeId);
        if (out != null) {
            sendToPipe(requestor, message, out);
            return;
        }
        PendingPipe p = (PendingPipe) pendingPipes.get(pipeId);
        if (p != null) {
            p.enqueue(requestor, message);
            return;
        }
        
        try {
            p = new PendingPipe();
            p.enqueue(requestor, message);
            pendingPipes.put(pipeId, p);
            pipe.createOutputPipe(pipeAdv, this);
        } catch (IOException e) {
            pendingPipes.remove(pipeId);
            requestor.notifyError("could not create output pipe");
            return;
        }
    }
    
    // TBD: DO WE NEED THIS FUNCTIONALITY FOR JXME?
    private PeerAdvertisement createPeerAdvertisement(String name, String id) {
        PeerAdvertisement adv = null;
        
        PeerID pid = null;
        if (id != null) {
            try {
                ID tempId = IDFactory.fromURI(new URI(id));
                    pid = (PeerID)tempId;
            } catch (URISyntaxException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not parse peerId from url", e);
                }
            } catch (ClassCastException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("id was not a peerid", e);
                }
            }
        }
        
        if (pid == null) {
            pid = IDFactory.newPeerID(group.getPeerGroupID());
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("newPeerAdvertisement name="+name+" id="+pid.toString());
        }
        
        try {
            // Create a pipe advertisement for this pipe.
            adv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(
            PeerAdvertisement.getAdvertisementType());
            
            adv.setPeerID(pid);
            adv.setPeerGroupID(group.getPeerGroupID());
            adv.setName(name);
            adv.setDescription( "Peer Advertisement created for a jxme device" );
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("newPeerAdvertisement Exception", e);
            }
        }
        
        return adv;
    }
    
    private PeerGroupAdvertisement createGroupAdvertisement(String name, String id) {
        PeerGroupAdvertisement adv = null;
        
        PeerGroupID gid = null;
        if (id != null) {
            try {
                ID tempId = IDFactory.fromURI(new URI(id));
                    gid = (PeerGroupID)tempId;
            } catch (URISyntaxException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid peergroupId", e);
                }
            } catch (ClassCastException e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("id was not a peergroup id", e);
                }
            }
        }
        
        if (gid == null) {
            gid = IDFactory.newPeerGroupID();
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("newPeerGroupAdvertisement name="+name+" id="+gid.toString());
        }
        
        adv = (PeerGroupAdvertisement)group.getPeerGroupAdvertisement().clone();
        
        try {
            // Create a PeerGroup Advertisement for this pipe.
            adv = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(
            PeerGroupAdvertisement.getAdvertisementType());
            
            adv.setName(name);
            adv.setPeerGroupID(gid);
            adv.setModuleSpecID(PeerGroup.allPurposePeerGroupSpecID);
            adv.setDescription("PeerGroup Advertisement created for a jxme device");
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("newPeerGroupAdvertisement Exception", e);
            }
        }
        
        return adv;
    }
    
    private PipeAdvertisement createPipeAdvertisement(String pipeName, String pipeId, String pipeType) {
        PipeAdvertisement adv = null;
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("newPipeAdvertisement name="+pipeName+" pipeId="+pipeId+" pipeType="+pipeType);
        }
        
        if (pipeType == null || pipeType.length() == 0) {
            pipeType = PipeService.UnicastType;
        }
        
        if (pipeId ==  null) {
            pipeId = IDFactory.newPipeID(group.getPeerGroupID()).toString();
        }
        
        try {
            // Create a pipe advertisement for this pipe.
            adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
            PipeAdvertisement.getAdvertisementType());
            
            adv.setName(pipeName);
            adv.setPipeID(IDFactory.fromURI(new URI(pipeId)));
            adv.setType(pipeType);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("newPipeAdvertisement Exception", e);
            }
        }
        
        return adv;
    }
    
    private PipeAdvertisement findPipeAdvertisement(String name, String id, String arg) {
        String attribute, value;
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("findPipeAdvertisement name=" + name +
            " id=" + id + " arg=" + arg);
        }
        
        if (id != null) {
            attribute = PipeAdvertisement.IdTag;
            value = id;
        } else if (name != null) {
            attribute = PipeAdvertisement.NameTag;
            value = name;
        } else {
            // the id or the name must be specified
            return null;
        }
        
        if (arg == null) {
            // the default pipe type
            arg = PipeService.UnicastType;
        }
        
        Enumeration each = null;
        try {
            each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
            attribute,
            value);
        } catch (IOException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("IOException in getLocalAdvertisements()", e);
            }
            return null;
        }
        
        PipeAdvertisement pipeAdv = null;
        Advertisement adv = null;
        while (each != null && each.hasMoreElements()) {
            adv = (Advertisement) each.nextElement();
            
            // take the first match
            if (adv instanceof PipeAdvertisement) {
                pipeAdv = (PipeAdvertisement)adv;
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("found PipeAdvertisement = " + pipeAdv);
                }
                break;
            }
        }
        
        return pipeAdv;
    }
    
    public synchronized void discoveryEvent(DiscoveryEvent event) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("discoveryEvent " + event);
        }
        
        Requestor requestor;
        
        requestor = (Requestor) searchRequests.get(new Integer(event.getQueryID()));
        if (requestor == null) {
            return;
        }
        
        DiscoveryResponseMsg response = event.getResponse();
        if (response == null) {
            return;
        }
        
        Enumeration each = response.getResponses();
        if (each == null || !each.hasMoreElements()) {
            return;
        }
        
        while (each.hasMoreElements()) {
            try {
                String str = (String) each.nextElement();
                
                // Create Advertisement from response.
                Advertisement adv = (Advertisement)
                AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8,
                new StringReader(str));
                
                // notify the requestor of the result
                requestor.send(adv, RESPONSE_RESULT);
            } catch (Exception e) {
                // this should not happen unless a bad result is returned
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Bad result returned by DiscoveryService", e);
                }
            }
        }
    }
    
    public synchronized void pipeMsgEvent(PipeMsgEvent event) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("pipeMsgEvent " + event.getPipeID());
        }

        String id = event.getPipeID().toString();

        PipeListenerList list = (PipeListenerList)pipeListeners.get(id);
        if (list != null) {
            Message message = event.getMessage();

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("pipeMsgEvent: start sending to each requestor");
            }
            list.send((Message)message.clone(), id);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("pipeMsgEvent: end sending to each requestor");
            }
        } else {
            // there are no listeners, close the input pipe
            ((InputPipe)event.getSource()).close();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("close pipe id=" + id);
            }
        }
    }
    
    public synchronized void outputPipeEvent(OutputPipeEvent event) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("outputPipeEvent " + event);
        }
        PendingPipe p =
        (PendingPipe) pendingPipes.remove(event.getPipeID());
        
        // Noone cares (anylonger). TBD should it be removed then??
        if (p == null) {
            event.getOutputPipe().close();
            return;
        }
        
        resolvedPipes.put(event.getPipeID(), event.getOutputPipe());
        p.sendPending(event.getOutputPipe());
    }
    
    private static String popString(String name, Message message) {
        MessageElement el = message.getMessageElement(PROXYNS, name);
        if (el != null) {
            message.removeMessageElement(el);
            return el.toString();
        }
        return null;
    }
    
    static class PipeListenerList {
        LinkedList list = new LinkedList();
        InputPipe inputPipe = null;
        Map pipeListeners = null;
        String id = null;
        
        PipeListenerList(InputPipe inputPipe,
        Map pipeListeners,
        String id) {
            this.inputPipe = inputPipe;
            this.pipeListeners = pipeListeners;
            this.id = id;
            
            if (pipeListeners != null) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("number of pipeListeners = " + pipeListeners.size());
                }
            }
        }
        
        void add(Requestor requestor) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("add " + requestor + " from " + toString());
            }
            
            if (!list.contains(requestor)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("requestor add");
                }
                list.add(requestor);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("requestor exits already");
                }
            }
        }
        
        void remove(Requestor requestor) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("remove " + requestor + " from " + toString());
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("removed = " + list.remove(requestor));
            }
            
            if (list.size() == 0) {
                // close the pipe and remove from the listenerList
                if (inputPipe != null) {
                    inputPipe.close();
                }
                
                if (id != null && pipeListeners != null) {
                    pipeListeners.remove(id);
                }
            }
        }
        
        int size() {
            int size = list.size();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("size " + size);
            }
            return size;
        }
        
        private static StringMessageElement sme = new StringMessageElement(RESPONSE_TAG, RESPONSE_MESSAGE, null);
        
        void send(Message message, String id) {
            LOG.debug("send list.size = " + list.size());
            
            message.addMessageElement(PROXYNS,sme);
            
            message.addMessageElement(PROXYNS, new StringMessageElement(ID_TAG, id, null));
            
            // removed all element that are known to be not needed
            Iterator elements = message.getMessageElements();
            
            while (elements.hasNext()) {
                MessageElement el = (MessageElement) elements.next();
                String name = el.getElementName();
                
                if (name.startsWith("RendezVousPropagate")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                } else if (name.startsWith("JxtaWireHeader")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                } else if (name.startsWith("RdvIncarnjxta")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                } else if (name.startsWith("JxtaEndpointRouter")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                } else if (name.startsWith("EndpointRouterMsg")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                }  else if (name.startsWith("EndpointHeaderSrcPeer")) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("removeMessageElement " + name);
                    }
                    elements.remove();
                }
            }
            
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                Requestor requestor = (Requestor)iterator.next();
                if (requestor.send((Message)message.clone()) == false) {
                    // could not send to listener, remove them from the list
                    remove(requestor);
                }
            }
        }
        
        public String toString() {
            String str = "PipeListenerList size=" + list.size();
            return str;
        }
    }
    
    protected static void logMessage(Message message, Logger log) {
        String out = "\n**************** begin ****************\n";
        
        Message.ElementIterator elements = message.getMessageElements();
        
        while (elements.hasNext()) {
            MessageElement  element = (MessageElement) elements.next();
            out += "[" + elements.getNamespace() + "," +
            element.getElementName() + "]=" +
            element.toString() + "\n";
        }

        if ( log.isEnabledFor(Level.DEBUG) ) {
            log.debug(out + "****************  end  ****************\n");
        }
    }
    
    
    /****************************************************************
     * Implement the CacheEntryListener                             *
     ***************************************************************/
    
    public void purged(CacheEntry ce) {
        // A resolved pipe was purged from the cache because we have to
        // many pre-resolved pipes hanging around. Close it, because
        // it may be holding critical resources that the GC will not be
        // sensitive to.
        ((OutputPipe)(ce.getValue())).close();
    }
}

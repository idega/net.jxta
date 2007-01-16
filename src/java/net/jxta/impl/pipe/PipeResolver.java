/*
 *  $Id: PipeResolver.java,v 1.1 2007/01/16 11:01:57 thomas Exp $
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
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
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
 */
package net.jxta.impl.pipe;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.OutgoingMessageEvent;
import net.jxta.id.ID;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.PipeResolverMessage;
import net.jxta.protocol.PipeResolverMessage.MessageType;
import net.jxta.protocol.ResolverQueryMsg;
import net.jxta.protocol.ResolverResponseMsg;
import net.jxta.protocol.ResolverSrdiMsg;
import net.jxta.protocol.SrdiMessage;
import net.jxta.protocol.SrdiMessage.Entry;
import net.jxta.resolver.ResolverService;
import net.jxta.resolver.SrdiHandler;

import net.jxta.impl.cm.Srdi;
import net.jxta.impl.cm.Srdi.SrdiInterface;
import net.jxta.impl.cm.SrdiIndex;
import net.jxta.impl.protocol.PipeResolverMsg;
import net.jxta.impl.protocol.ResolverQuery;
import net.jxta.impl.protocol.SrdiMessageImpl;
import net.jxta.impl.resolver.InternalQueryHandler;
import net.jxta.impl.util.TimeUtils;


/**
 * This class implements the Resolver interfaces for a PipeServiceImpl.
 *
 **/
class PipeResolver implements SrdiInterface, InternalQueryHandler, SrdiHandler, PipeRegistrar {
    
    /**
     *  Log4J Logger
     **/
    private final static Logger LOG = Logger.getLogger(PipeResolver.class.getName());
    
    private final static String PipeResolverName = "JxtaPipeResolver";
    private final static String srdiIndexerFileName = "pipeResolverSrdi";
    
    /**
     *  Local SRDI GC Interval
     **/
    private final static long GcDelay = 1 * TimeUtils.AMINUTE;
    
    /**
     *  Constant for pipe event listeners to signify any query id.
     **/
    final static int ANYQUERY = 0;
    
    /**
     *  The current query ID. The next value returned by {@link #getNextQueryID()}
     *  will be one greater than this value.
     **/
    private static int          currentQueryID = 1;
    
    /**
     *  Group we are working for
     **/
    private PeerGroup           myGroup = null;
    
    /**
     *  Resolver Service we will register with
     **/
    private ResolverService     resolver = null;
    
    /**
     *  The discovery service we will use
     **/
    private DiscoveryService   discovery = null;
    
    /**
     *  Membership Service we will use
     **/
    private MembershipService membership = null;
    
    private Srdi                    srdi = null;
    private Thread            srdiThread = null;
    private SrdiIndex          srdiIndex = null;
    
    /**
     *  Listener we register for credential changes.
     **/
    private CredentialListener membershipCredListener = null;
    
    /**
     *  The credential we will include in queries and responses.
     **/
    private Credential credential = null;
    
    /**
     *  The credential as a document.
     **/
    private StructuredDocument  credentialDoc = null;
    
    /**
     *  The locally registered {@link net.jxta.pipe.InputPipe}s
     *
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.pipe.PipeID}s</li>
     *      <li>Values are {@link net.jxta.pipe.InputPipe}.</li>
     *  </ul>
     **/
    private Map localInputPipes = new HashMap();
    
    /**
     *  Registered listeners for pipe events.
     *
     *  <p/><ul>
     *      <li>Keys are {@link net.jxta.pipe.PipeID}s</li>
     *      <li>Values are {@link java.util.HashMap}.
     *      <ul>
     *          <li>Keys are query ids as {@link java.lang.Integer}s</li>
     *          <li>Values are {@link Listener}.</li>
     *      </ul>
     *      </li>
     *  </ul>
     **/
    private Map   outputpipeListeners = new HashMap();
    
    /**
     *  Listener we use for membership property events.
     */
    private class CredentialListener implements PropertyChangeListener {
        
        /**
         *  {@inheritDoc}
         **/
        public void propertyChange(PropertyChangeEvent evt) {
            if ("defaultCredential".equals(evt.getPropertyName())) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("New default credential event");
                }
                
                synchronized (PipeResolver.this) {
                    credential = (Credential) evt.getNewValue();
                    credentialDoc = null;
                    
                    if (null != credential) {
                        try {
                            credentialDoc = credential.getDocument(MimeMediaType.XMLUTF8);
                        } catch (Exception all) {
                            if (LOG.isEnabledFor(Level.WARN)) {
                                LOG.warn("Could not generate credential document", all);
                            }
                        }
                    }
                }
            }
        }
    }
    

    /**
     *  A pipe resolver event.
     **/
    static class Event extends EventObject {
        
        private PeerID peerid = null;
        private PipeID pipeid = null;
        private String type = null;
        private int queryID = -1;
        
        /**
         *  Creates a new pipe resolution event
         *
         *  @param source The PipeResolver generating the event.
         *  @param peerid  The peer on which the pipe was found
         *  @param pipeid the pipe which was found
         *  @param type the type of pipe which was found
         *  @param queryid The query id associated with the response returned in this event
         **/
        public Event(Object source, PeerID peerid, PipeID pipeid, String type, int queryid) {
            super(source);
            this.peerid = peerid;
            this.pipeid = pipeid;
            this.type = type;
            this.queryID = queryid;
        }
        
        /**
         *  Returns the peer associated with the event
         *
         *  @return peerid
         **/
        public PeerID getPeerID() {
            return peerid;
        }
        
        /**
         *  Returns the pipe associated with the event
         *
         *  @return pipeid
         **/
        public PipeID getPipeID() {
            return pipeid;
        }
        
        /**
         *  Returns the type of the pipe that is associated with the event
         *
         *  @return type
         **/
        public String getType() {
            return type;
        }
        
        /**
         *  Returns The query id associated with the response returned in this event
         *
         *  @return query id associated with the response
         **/
        public int getQueryID() {
            return queryID;
        }
    }
    

    /**
     *  Pipe Resolver Event Listener. Implement this interface is you wish to
     *  Receive Pipe Resolver events.
     **/
    interface Listener extends EventListener {
        
        /**
         * Pipe Resolve Event
         *
         * @param PipeResolverEvent event the PipeResolver Event
         * @return true if the event was handled otherwise false
         **/
        boolean pipeResolveEvent(Event event);
        
        /**
         * A NAK Event was received for this pipe
         *
         * @param PipeResolverEvent event the PipeResolver Event
         * @return true if the event was handled otherwise false
         **/
        boolean pipeNAKEvent(Event event);
    }
    
    /**
     *  return the next query id.
     *
     *  @return the next eligible query id.
     **/
    static synchronized int getNextQueryID() {
        return currentQueryID++;
    }
    
    /**
     * Constructor for the PipeResolver object
     *
     * @param  g  group for which this PipeResolver operates in
     **/
    PipeResolver(PeerGroup g) {
        
        myGroup = g;
        resolver = myGroup.getResolverService();
        membership = myGroup.getMembershipService();
        
        // Register to the Generic ResolverServiceImpl
        resolver.registerHandler(PipeResolverName, this);
        
        // start srdi
        srdiIndex = new SrdiIndex(myGroup, srdiIndexerFileName, GcDelay);
        
        srdi = new Srdi(myGroup, PipeResolverName, this, srdiIndex, 2 * TimeUtils.AMINUTE, 1 * TimeUtils.AYEAR);
        srdiThread = new Thread(myGroup.getHomeThreadGroup(), srdi, "Pipe Resolver Srdi Thread");
        srdiThread.setDaemon(true);
        srdiThread.start();
        
        resolver.registerSrdiHandler(PipeResolverName, this);
        
        synchronized (this) {
            try {
                credential = membership.getDefaultCredential();
                
                if (null != credential) {
                    credentialDoc = credential.getDocument(MimeMediaType.XMLUTF8);
                } else {
                    credentialDoc = null;
                }
            } catch (Exception all) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("could not get credential", all);
                }
            }
            
            membershipCredListener = new CredentialListener();
            membership.addPropertyChangeListener("defaultCredential", membershipCredListener);
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int processQuery(ResolverQueryMsg query) {
        
        return processQuery(query, null);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int processQuery(ResolverQueryMsg query, EndpointAddress srcAddr) {
        
        String queryFrom;

        if (null != srcAddr) {
            if ("jxta".equals(srcAddr.getProtocolName())) {
                queryFrom = ID.URIEncodingName + ":" + ID.URNNamespace + ":" + srcAddr.getProtocolAddress();
            } else {
                // we don't know who routed us the query. Assume it came from the source.
                queryFrom = query.getSrc();
            }
        } else {
            // we don't know who routed us the query. Assume it came from the source.
            queryFrom = query.getSrc();
        }
        
        String responseDest = query.getSrc();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Starting for :" + query.getQueryId() + " from " + srcAddr);
        }
        Reader queryReader = new StringReader(query.getQuery());
        
        StructuredTextDocument doc = null;

        try {
            doc = (StructuredTextDocument)
                    StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, queryReader);
        } catch (IOException e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("discarding malformed request ", e);
            }
            // no sense in re-propagation here
            return ResolverService.OK;
        }
        finally {
            try {
                queryReader.close();
            } catch (IOException ignored) {
                ;
            }
            queryReader = null;
        }
        
        PipeResolverMessage pipeQuery;

        try {
            pipeQuery = new PipeResolverMsg(doc);
        } catch (IllegalArgumentException badDoc) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("discarding malformed request ", badDoc);
            }
            // no sense in re-propagation here
            return ResolverService.OK;
        }
        finally {
            doc = null;
        }
        
        // is it a query?
        if (!pipeQuery.getMsgType().equals(MessageType.QUERY)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("expected query - discarding.");
            }
            // no sense in re-propagation here
            return ResolverService.OK;
        }
        
        // see if it is a query directed at our peer.
        Set destPeers = pipeQuery.getPeerIDs();
        boolean directedQuery = !destPeers.isEmpty();
        boolean queryForMe = !directedQuery;

        if (directedQuery) {
            Iterator eachDestPeer = destPeers.iterator();
            
            while (eachDestPeer.hasNext()) {
                ID aPeer = (ID) eachDestPeer.next();

                if (aPeer.equals(myGroup.getPeerID())) {
                    queryForMe = true;
                    break;
                }
            }
            
            if (!queryForMe) {
                // It is an directed query, but request wasn't for this peer.
                if (query.getSrc().equals(queryFrom)) {
                    // we only respond if the original src was not the query forwarder
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("discarding query. Query not for us.");
                    }
                    
                    // tell the resolver no further action is needed.
                    return ResolverService.OK;
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("responding to 'misdirected' forwarded query.");
                }
                
                responseDest = queryFrom;
            }
        }
        
        PeerID peerID = null;

        if (queryForMe) {
            // look locally.
            InputPipe ip = findLocal((PipeID) pipeQuery.getPipeID());
            
            if ((ip != null) && (ip.getType().equals(pipeQuery.getPipeType()))) {
                peerID = myGroup.getPeerID();
            }
        }
        
        if ((null == peerID) && !directedQuery) {
            // This request was sent to everyone.
            if (myGroup.isRendezvous()) {
                // We are a RDV, allow the ResolverService to repropagate the query
                List results = srdiIndex.query(pipeQuery.getPipeType(), PipeAdvertisement.IdTag, pipeQuery.getPipeID().toString(), 20);
                
                if (results.size() > 0) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("forwarding query to " + results.size() + " peers");
                    }
                    
                    Iterator eachPeer = results.iterator();

                    while (eachPeer.hasNext()) {
                        PeerID aPeer = (PeerID) eachPeer.next();

                        srdi.forwardQuery(aPeer, query);
                    }
                    
                    // tell the resolver no further action is needed.
                    return ResolverService.OK;
                }
                
                // we don't know anything, continue the walk.
                return ResolverService.Repropagate;
            } else {
                // We are an edge
                if (query.getSrc().equals(queryFrom)) {
                    // we only respond if the original src was not the query forwarder
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("discarding query.");
                    }
                    
                    // tell the resolver no further action is needed.
                    return ResolverService.OK;
                }
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("responding to query forwarded for 'misdirected' query.");
                }
                
                responseDest = queryFrom;
            }
        }
        
        // Build the answer
        PipeResolverMessage pipeResp = new PipeResolverMsg();
        
        pipeResp.setMsgType(MessageType.ANSWER);
        pipeResp.setPipeID(pipeQuery.getPipeID());
        pipeResp.setPipeType(pipeQuery.getPipeType());
        if (null == peerID) {
            // respond negative.
            pipeResp.addPeerID(myGroup.getPeerID());
            pipeResp.setFound(false);
        } else {
            pipeResp.addPeerID(peerID);
            pipeResp.setFound(true);
            pipeResp.setInputPeerAdv(myGroup.getPeerAdvertisement());
        }
        
        // make a response from the incoming query
        ResolverResponseMsg res = query.makeResponse();

        res.setCredential(credentialDoc);
        res.setResponse(pipeResp.getDocument(MimeMediaType.XMLUTF8).toString());
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending answer for query '" + query.getQueryId() + "' to : " + responseDest);
        }
        resolver.sendResponse(responseDest, res);
        
        return ResolverService.OK;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void processResponse(ResolverResponseMsg response) {
        processResponse(response, null);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void processResponse(ResolverResponseMsg response, EndpointAddress srcAddr) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("got a response for '" + response.getQueryId() + "'");
        }
        
        Reader resp = new StringReader(response.getResponse());
        
        StructuredTextDocument doc = null;

        try {
            doc = (StructuredTextDocument)
                    StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, resp);
        } catch (Throwable e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("malformed response - discard", e);
            }
            return;
        }
        finally {
            try {
                resp.close();
            } catch (IOException ignored) {
                ;
            }
            resp = null;
        }
        
        PipeResolverMessage pipeResp;

        try {
            pipeResp = new PipeResolverMsg(doc);
        } catch (Throwable caught) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("malformed response - discarding.", caught);
            }
            return;
        }
        finally {
            doc = null;
        }
        
        // check if it's a response.
        if (!pipeResp.getMsgType().equals(MessageType.ANSWER)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("expected response - discarding.");
            }
            return;
        }
        
        PeerAdvertisement padv = pipeResp.getInputPeerAdv();

        if ((null != padv) && !(myGroup.getPeerID().equals(padv.getPeerID()))) {
            try {
                // This is not our own peer adv so we keep it only for the default
                // expiration time.
                if (null == discovery) {
                    discovery = myGroup.getDiscoveryService();
                }
                
                if (null != discovery) {
                    discovery.publish(padv, DiscoveryService.DEFAULT_EXPIRATION, DiscoveryService.DEFAULT_EXPIRATION);
                }
            } catch (IOException ignored) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("could not publish peer adv");
                }
            }
        }
        
        String ipId = pipeResp.getPipeID().toString();
        
        Set peerRsps = pipeResp.getPeerIDs();
        
        Iterator eachResp = peerRsps.iterator();
        
        while (eachResp.hasNext()) {
            // process each peer for which this response is about.
            PeerID peer = (PeerID) eachResp.next();

            if (!pipeResp.isFound()) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("NACK for pipe '" + ipId + "' from peer " + peer);
                }
                
                // We have received a NACK. Remove that entry.
                srdiIndex.add(pipeResp.getPipeType(), PipeAdvertisement.IdTag, ipId, peer, 0);
            } else {
                long exp = getEntryExp(pipeResp.getPipeType(), 
                                       PipeAdvertisement.IdTag,
                                       ipId,
                                       peer);
                if ((PipeServiceImpl.VERIFYINTERVAL / 2) > exp) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Using Expiration "+(PipeServiceImpl.VERIFYINTERVAL / 2) +" which is > "+ exp);
                    }
                    // create antry only if one does not exist,or entry exists with
                    // lesser lifetime
                    // cache the result for half the verify interval
                    srdiIndex.add(pipeResp.getPipeType(), PipeAdvertisement.IdTag, ipId, peer, (PipeServiceImpl.VERIFYINTERVAL / 2));
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("DB Expiration "+ exp + " > "+(PipeServiceImpl.VERIFYINTERVAL / 2)+ " overriding attempt to decrease lifetime");
                    }
                }
            }
            
            // call listener for pipeid
            callListener(response.getQueryId(), (PipeID) pipeResp.getPipeID(), pipeResp.getPipeType(), peer, !pipeResp.isFound());
        }
    }
    
    /**
     * Given a list of Entries returns the exp record of peerid
     * otherwise a -1
     */
    private long getEntryExp(String pkey, String skey, String value, PeerID peerid){
        List list = srdiIndex.getRecord(pkey, skey, value);
        Iterator it=list.iterator();
        while (it.hasNext()){
            SrdiIndex.Entry entry = (SrdiIndex.Entry) it.next();
            if (entry.peerid.equals(peerid)) {
                // exp in millis
                return TimeUtils.toRelativeTimeMillis(entry.expiration);
            }
        }
        return -1;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean processSrdi(ResolverSrdiMsg message) {
        
        if (message == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("no SRDI message");
            }
            return false;
        }
        
        if (message.getPayload() == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("empty SRDI message");
            }
            return false;
        }
        
        SrdiMessage srdiMsg;

        try {
            StructuredTextDocument asDoc = (StructuredTextDocument)
                    StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(message.getPayload()));
            
            srdiMsg = new SrdiMessageImpl(asDoc);
        } catch (Throwable e) {
            // we don't understand this msg, let's skip it
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Invalid SRDI message", e);
            }
            return false;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Received an SRDI messsage with " + srdiMsg.getEntries().size() + " entries from " + srdiMsg.getPeerID());
        }
        
        // add the entries too
        Iterator eachEntry = srdiMsg.getEntries().iterator();
        
        while (eachEntry.hasNext()) {
            Entry entry = (Entry) eachEntry.next();

            srdiIndex.add(srdiMsg.getPrimaryKey(), entry.key, entry.value, srdiMsg.getPeerID(), entry.expiration);
        }
        
        if (!PipeService.PropagateType.equals(srdiMsg.getPrimaryKey())) {
            // don't replicate entries for propagate pipes. For unicast type
            // pipes the replica is useful in finding pipe instances. Since
            // walking rather than searching is done for propagate pipes this
            // appropriate.
            srdi.replicateEntries(srdiMsg);
        }
        
        return true;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void messageSendFailed(PeerID peerid, OutgoingMessageEvent e) {// so what.
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void pushEntries(boolean all) {
        
        pushSrdi((PeerID) null, all);
    }
    
    /**
     * unregisters the resolver handler
     **/
    void stop() {
        
        resolver.unregisterHandler(PipeResolverName);
        
        resolver.unregisterSrdiHandler(PipeResolverName);
        
        srdiIndex.stop();
        srdiIndex = null;
        // stop the srdi thread
        if (srdiThread != null) {
            srdi.stop();
        }
        srdiThread = null;
        srdi = null;
        
        membership.removePropertyChangeListener("defaultCredential", membershipCredListener);
        membershipCredListener = null;
        credential = null;
        credentialDoc = null;
        
        // Avoid cross-reference problems with GC
        myGroup = null;
        resolver = null;
        discovery = null;
        membership = null;
        
        outputpipeListeners.clear();
        
        // close the local pipes
        Iterator eachLocalInputPipe = Arrays.asList(localInputPipes.values().toArray()).iterator();

        while (eachLocalInputPipe.hasNext()) {
            InputPipe aPipe = (InputPipe) eachLocalInputPipe.next();

            try {
                aPipe.close();
            } catch (Exception failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failure closing " + aPipe);
                }
            }
        }
        
        localInputPipes.clear();
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean register(InputPipe ip) {
        
        PipeID pipeID = (PipeID) ip.getPipeID();
        
        synchronized (this) {
            if (localInputPipes.containsKey(pipeID)) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Existing registered InputPipe for " + pipeID);
                }
                
                return false;
            }
            
            // Register this input pipe
            
            if (!ip.getType().equals(PipeService.PropagateType)) {
                boolean registered = myGroup.getEndpointService().addIncomingMessageListener((EndpointListener) ip, "PipeService", pipeID.toString());
                
                if (!registered) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Existing registered Endpoint Listener for " + pipeID);
                    }
                    
                    return false;
                }
            }
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Registering local InputPipe for " + pipeID);
            }
            
            localInputPipes.put(pipeID, ip);
        }
        
        // Call anyone who may be listening for this input pipe.
        callListener(0, pipeID, ip.getType(), myGroup.getPeerID(), false);
        
        return true;
    }
    
    /**
     *  Return the local {@link net.jxta.pipe.InputPipe InputPipe}, if any, for the
     *  specified {@link net.jxta.pipe.PipeID PipeID}.
     *
     * @param   pipeID  the PipeID who's InputPipe is desired.
     * @return       The InputPipe object.
     **/
    public InputPipe findLocal(PipeID pipeID) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Find local InputPipe for " + pipeID);
        }
        
        // First look if the pipe is a local InputPipe
        InputPipe ip = (InputPipe) localInputPipes.get(pipeID);
        
        // Found it.
        if ((null != ip) && LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("found local InputPipe for " + pipeID);
        }
        
        return ip;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean forget(InputPipe pipe) {
        
        PipeID pipeID = (PipeID) pipe.getPipeID();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Unregistering local InputPipe for " + pipeID);
        }
        
        InputPipe ip;

        synchronized (this) {
            ip = (InputPipe) localInputPipes.remove(pipeID);
        }
        
        if ((null != ip) && !ip.getType().equals(PipeService.PropagateType)) {
            // remove the queue for the general demux
            EndpointListener removed = myGroup.getEndpointService().removeIncomingMessageListener("PipeService", pipeID.toString());
            
            if ((null == removed) || (pipe != removed)) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("removeIncomingMessageListener() did not remove correct pipe!");
                }
            }
        }
        
        return (ip != null);
    }
    
    /**
     *  Add a pipe resolver listener
     *
     * @param  listener  listener
     **/
    synchronized boolean addListener(PipeID pipeID, Listener listener, int queryID) {
        
        Map perpipelisteners = (Map) outputpipeListeners.get(pipeID);
        
        // if no map for this pipeid, make one and add it to the top map.
        if (null == perpipelisteners) {
            perpipelisteners = new HashMap();
            
            outputpipeListeners.put(pipeID, perpipelisteners);
        }
        
        Integer queryKey = new Integer(queryID);
        
        boolean alreadyThere = perpipelisteners.containsKey(queryKey);
        
        if (!alreadyThere) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("adding listener for " + pipeID + " / " + queryID);
            }
            
            perpipelisteners.put(queryKey, listener);
        }
        
        return alreadyThere;
    }
    
    /**
     *  Call the listener for the specified pipe id informing it about the
     *  specified peer.
     *
     *  @param qid      The query this callback is being made in response to.
     *  @param pipeID   The pipe which is the subject of the event.
     *  @param type     The type of the pipe which is the subject of the event.
     *  @param peer     The peer on which the remote input pipe was found.
     **/
    void callListener(int qid, PipeID pipeID, String type, PeerID peer, boolean NAK) {
        
        Event newevent = new Event(this, peer, pipeID, type, qid);
        
        boolean handled = false;
        
        while (!handled) {
            Listener pl = null;

            synchronized (this) {
                Map perpipelisteners = (Map) outputpipeListeners.get(pipeID);
                
                if (null == perpipelisteners) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("No listener for pipe: " + pipeID);
                    }
                    break;
                }
                
                pl = (Listener) perpipelisteners.get(new Integer(qid));
            }
            
            if (null != pl) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Calling Pipe resolver listener " + (NAK ? "NAK " : "") + "for pipe : " + pipeID);
                }
                
                try {
                    if (NAK) {
                        handled = pl.pipeNAKEvent(newevent);
                    } else {
                        handled = pl.pipeResolveEvent(newevent);
                    }
                } catch (Throwable ignored) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Uncaught Throwable in listener for: " + pipeID + "(" + pl.getClass().getName() + ")", ignored);
                    }
                }
            }
            
            // if we havent tried it already, try it with the ANYQUERY
            if (ANYQUERY == qid) {
                break;
            }
            
            qid = ANYQUERY;
        }
    }
    
    /**
     *  Remove a pipe resolver listener
     *
     * @param pipeid listener to remove
     * @param queryid matching queryid.
     * @return listener object removed
     **/
    synchronized Listener removeListener(PipeID pipeID, int queryID) {
        
        Map perpipelisteners = (Map) outputpipeListeners.get(pipeID);
        
        if (null == perpipelisteners) {
            return null;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("removing listener for :" + pipeID + " / " + queryID);
        }
        
        Integer queryKey = new Integer(queryID);
        
        Listener removedListener = (Listener) perpipelisteners.remove(queryKey);
        
        if (0 == perpipelisteners.size()) {
            outputpipeListeners.remove(pipeID);
        }
        
        return removedListener;
    }
    
    /**
     *  Send a request to find an input pipe
     *
     *  @param  adv the advertisement for the pipe we are seeking.
     *  @param  acceptablePeers the set of peers at which we wish the pipe to
     *  be resolved. We will not accept responses from peers other than those
     *  in this set. Empty set means all peers are acceptable.
     *  @param queryID the query ID to use for the query. if zero then a query
     *  ID will be generated
     *  @return the query id under which the request was sent
     **/
    int sendPipeQuery(PipeAdvertisement adv, Set acceptablePeers, int queryID) {
        
        // choose a query id if non-prechosen.
        if (0 == queryID) {
            queryID = getNextQueryID();
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug((acceptablePeers.isEmpty() ? "Undirected" : "Directed") + " query (" + queryID + ") for " + adv.getPipeID());
        }
        
        Collection targetPeers = acceptablePeers;
        
        // check local srdi to see if we have a potential answer
        List knownLocations = srdiIndex.query(adv.getType(), PipeAdvertisement.IdTag, adv.getPipeID().toString(), 100);
        
        if (!knownLocations.isEmpty()) {
            // we think we know where the pipe might be...
            
            if (!acceptablePeers.isEmpty()) {
                // only keep those peers which are acceptable.
                knownLocations.retainAll(acceptablePeers);
            }
            
            // if the known locations contain any of the acceptable peers then
            // we will send a directed query to ONLY those peers.
            if (!knownLocations.isEmpty()) {
                targetPeers = knownLocations;
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Using SRDI cache results for directed query (" + queryID + ") for " + adv.getPipeID());
                }
            }
        }
        
        // build the pipe query message.
        PipeResolverMessage pipeQry = new PipeResolverMsg();
        
        pipeQry.setMsgType(MessageType.QUERY);
        pipeQry.setPipeID(adv.getPipeID());
        pipeQry.setPipeType(adv.getType());
        Iterator eachPeer = targetPeers.iterator();

        while (eachPeer.hasNext()) {
            pipeQry.addPeerID((PeerID) eachPeer.next());
        }
        
        StructuredTextDocument asDoc = (StructuredTextDocument) pipeQry.getDocument(MimeMediaType.XMLUTF8);
        
        // build the resolver query
        ResolverQuery query = new ResolverQuery();
        
        query.setHandlerName(PipeResolverName);
        query.setCredential(credentialDoc);
        query.setQueryId(queryID);
        query.setSrc(myGroup.getPeerID().toString());
        query.setQuery(asDoc.toString());
        
        if (targetPeers.isEmpty()) {
            // we have no idea, walk the tree
            
            if (myGroup.isRendezvous()) {
                // We are a rdv, then send it to the replica peer.
                
                PeerID peer = srdi.getReplicaPeer(pipeQry.getPipeType() + PipeAdvertisement.IdTag + pipeQry.getPipeID().toString());
                
                if (null != peer) {
                    srdi.forwardQuery(peer.toString(), query);
                    return queryID;
                }
            }
            
            resolver.sendQuery(null, query);
        } else {
            // send it only to the peers whose result we would accept.
            
            eachPeer = targetPeers.iterator();
            while (eachPeer.hasNext()) {
                resolver.sendQuery(eachPeer.next().toString(), query);
            }
        }
        
        return queryID;
    }
    
    /**
     *  {@inheritDoc}
     **/
    SrdiIndex getSrdiIndex() {
        return srdiIndex;
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>This implementation knows nothing of deltas, it just pushes it all.
     **/
    private void pushSrdi(PeerID peer, boolean all) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Pushing " + (all ? "all" : "deltas") + " SRDI to " + peer);
        }
        
        Map types = new HashMap();
        
        synchronized (this) {
            Iterator eachPipe = localInputPipes.values().iterator();
            
            while (eachPipe.hasNext()) {
                InputPipe ip = (InputPipe) eachPipe.next();
                Entry entry = new Entry(PipeAdvertisement.IdTag, ip.getPipeID().toString(), Long.MAX_VALUE);
                
                String type = ip.getType();
                
                List entries = (List) types.get(type);
                
                if (null == entries) {
                    entries = new Vector();
                    types.put(type, entries);
                }
                
                entries.add(entry);
            }
        }
        
        Iterator eachType = types.keySet().iterator();
        
        while (eachType.hasNext()) {
            String type = (String) eachType.next();
            
            Vector entries = (Vector) types.get(type);

            eachType.remove();
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Sending a Pipe SRDI messsage in " + myGroup.getPeerGroupID() + " of " + entries.size() + " entries of type " + type);
            }
            
            SrdiMessage srdiMsg = new SrdiMessageImpl(myGroup.getPeerID(), 1, // ttl
                    type, entries);
            
            if (null == peer) {
                srdi.pushSrdi(null, srdiMsg);
            } else {
                srdi.pushSrdi(peer, srdiMsg);
            }
        }
    }
    
    /**
     * Push SRDI entry for the specified pipe
     *
     * @param   ip  the pipe who's entry we are pushing
     * @param adding    adding an entry for the pipe or expiring the entry?
     **/
    void pushSrdi(InputPipe ip, boolean adding) {
        
        srdiIndex.add(ip.getType(), PipeAdvertisement.IdTag, ip.getPipeID().toString(), myGroup.getPeerID(), adding ? Long.MAX_VALUE : 0);
        
        SrdiMessage srdiMsg;

        try {
            srdiMsg = new SrdiMessageImpl(myGroup.getPeerID(), 1, // ttl
                    ip.getType(), PipeAdvertisement.IdTag, ip.getPipeID().toString(), adding ? Long.MAX_VALUE : 0);
            
            if (myGroup.isRendezvous()) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug(
                            "Replicating a" + (adding ? "n add" : " remove") + " Pipe SRDI entry for pipe [" + ip.getPipeID() + "] of type "
                            + ip.getType());
                }
                
                srdi.replicateEntries(srdiMsg);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug(
                            "Sending a" + (adding ? "n add" : " remove") + " Pipe SRDI messsage for pipe [" + ip.getPipeID() + "] of type "
                            + ip.getType());
                }
                
                srdi.pushSrdi(null, srdiMsg);
            }
        } catch (Throwable e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Uncaught throwable pushing SRDI entries", e);
            }
        }
    }
}

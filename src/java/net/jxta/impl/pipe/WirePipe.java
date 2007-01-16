/*
 *
 * $Id: WirePipe.java,v 1.1 2007/01/16 11:01:58 thomas Exp $
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

package net.jxta.impl.pipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.impl.cm.SrdiIndex;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * WirePipe (aka Propagated pipe) is very similar to IGMP, where a creation of an
 * input pipe results in a propagated pipe membership registeration with the peer's
 * rendezvous peer, and a closure results in a propagated pipe group resignation,
 * these group registeration/resignation are simply PipeService SRDI messages.
 */
public class WirePipe implements EndpointListener, InputPipe, PipeRegistrar {

    /**
     *  Log4J Logger
     */
    private final static transient Logger LOG = Logger.getLogger(WirePipe.class.getName());

    private static final int MAX_RECORDED_MSGIDS = 250;

    private volatile boolean closed = false;

    private PeerGroup myGroup = null;
    private PipeResolver pipeResolver = null;
    private WirePipeImpl pipeService = null;
    private PipeAdvertisement pipeAdv = null;

    private RendezVousService rendezvous = null;
    private final String localPeerId;
    private NonBlockingWireOutputPipe repropagater = null;

    /**
     *  Table of local input pipes listening on this pipe. Weak map so that
     *  we don't keep pipes unnaturally alive and consuming resources.
     *
     *  <p/><ul>
     *      <li>Values are {@link net.jxta.pipe.InputPipe}.</li>
     *  </ul>
     */
    private Map wireinputpipes = new WeakHashMap();

    /**
     *  Count of the number of local input pipes. Though it's mostly the same we
     *  can't use <code>wireinputpipes.size()</code> because it's too twitchy.
     *  We can guarntee that this field's value will change in predictable ways.
     */
    private int nbInputPipes = 0;

    /**
     *  The list of message ids we have already seen. The most recently seen
     *  messages are at the end of the list.
     *
     *  <p/><ul>
     *      <li>Values are {@link net.jxta.impl.id.UUID.UUID}.</li>
     *  </ul>
     *
     *  <p/>XXX 20031102 bondolo@jxta.org: We might want to consider three
     *  performance enhancements:
     *
     *  <ul>
     *      <li>Reverse the order of elements in the list. This would result
     *      in faster searching since the default strategy for ArrayList to
     *      search in index order. We are most likely to see duplicate messages
     *      amongst the messages we have seen recently. This would make
     *      additions more costly.</li>
     *
     *      <li>When we do find a duplicate in the list, exchange it's location
     *      with the newest message in the list. This will keep annoying dupes
     *      close to the start of the list.</li>
     *
     *      <li>When the array reaches MaxNbOfStoredIds treat it as a ring.</li>
     *  </ul>
     */
    private List msgIds = new ArrayList(MAX_RECORDED_MSGIDS);

    /**
     * Constructor
     *
     * @param g The Group associated with this service
     * @param pipeResolver the associated pipe resolver
     * @param pipeService The pipe service associated with this pipe
     * @param adv pipe advertisement
     */
    public WirePipe(PeerGroup g,
                    PipeResolver pipeResolver,
                    WirePipeImpl pipeService,
                    PipeAdvertisement adv) {

        this.myGroup = g;
        this.pipeResolver = pipeResolver;
        this.pipeService = pipeService;
        this.pipeAdv = adv;
        localPeerId = myGroup.getPeerID().toString();
        rendezvous = g.getRendezVousService();
        pipeResolver.register(this);
        repropagater = pipeService.createOutputPipe(adv, Collections.EMPTY_SET);
    }

    /**
     *  {@inheritDoc}
     *
     *  <p/>Closes the pipe.
     */
    protected synchronized void finalize() {

        if(!closed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Pipe is being finalized without being previously closed. This is likely a bug.");
            }
        }
        close();
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized boolean register(InputPipe wireinputpipe) {
        wireinputpipes.put(wireinputpipe, null);

        nbInputPipes++;
        if(1 == nbInputPipes) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Registering wire pipe with SRDI");
            }

            boolean registered = myGroup.getEndpointService().addIncomingMessageListener((EndpointListener) wireinputpipe, "PipeService", wireinputpipe.getPipeID().toString());

            if(!registered) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Existing Registered Endpoint Listener for " + wireinputpipe.getPipeID());
                }
            }

            pipeResolver.pushSrdi(this, true);
        }

        return true;
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized boolean forget(InputPipe wireinputpipe) {
        wireinputpipes.remove(wireinputpipe);

        nbInputPipes--;
        if(0 == nbInputPipes) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Deregistering wire pipe with SRDI");
            }
            pipeResolver.pushSrdi(this, false);

            EndpointListener removed = myGroup.getEndpointService().removeIncomingMessageListener("PipeService", wireinputpipe.getPipeID().toString());

            if((null == removed) || (this != removed)) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("removeIncomingMessageListener() did not remove correct pipe!");
                }
            }
        }

        if(nbInputPipes < 0) {
            // we reset this to zero so that re-registration works.
            nbInputPipes = 0;

            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Number of pipe listeners was < 0");
            }
        }

        return true;
    }

    // This is the InputPipe API implementation.
    // This is needed only to be able to register an InputPipe to the PipeResolver.
    // Not everything has to be implemented.

    /**
     *  {@inheritDoc}
     */
    public Message waitForMessage() throws InterruptedException {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("This method is not really supported.");
        }
        return null;
    }

    /**
     *  {@inheritDoc}
     */
    public Message poll(int timeout) throws InterruptedException {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("This method is not really supported.");
        }

        return null;
    }

    /**
     *  {@inheritDoc}
     */
    public synchronized void close() {
        if(closed) {
            return;
        }

        pipeResolver.forget(this);
        repropagater.close();
        closed = true;
    }

    /**
     *  {@inheritDoc}
     */
    public String getType() {
        return pipeAdv.getType();
    }

    /**
     *  {@inheritDoc}
     */
    public ID getPipeID() {
        return pipeAdv.getPipeID();
    }

    /**
     *  {@inheritDoc}
     */
    public String getName() {
        return pipeAdv.getName();
    }

    /**
     *  {@inheritDoc}
     */
    public PipeAdvertisement getAdvertisement() {
        return pipeAdv;
    }

    /**
     *  {@inheritDoc}
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        // Check if there is a JXTA-WIRE header
        MessageElement elem = message.getMessageElement("jxta", WirePipeImpl.WireTagName);

        if(null == elem) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No JxtaWireHeader element. Discarding " + message);
            }
            return;
        }

        WireHeader header;
        try {
            XMLDocument doc = (XMLDocument)
                              StructuredDocumentFactory.newStructuredDocument(elem.getMimeType(), elem.getStream());
            header = new WireHeader(doc);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("bad wire header", e);
            }
            return;
        }

        processIncomingMessage(message, header, srcAddr, dstAddr);
    }

    /**
     *  local version with the wire header already parsed. There are two paths
     *  to this point; via the local endpoint listener or via the general
     *  propagation listener in WirePipeImpl.
     *
     */
    void processIncomingMessage(Message message, WireHeader header, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        if (header.containsPeer(localPeerId)) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Loopback detected - discarding " + message);
            }
            return;
        }

        if (recordSeenMessage(header.getMsgId())) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Discarding duplicate " + message);
            }
            return;
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Processing " + message + " on " + pipeAdv.getPipeID());
        }

        if (myGroup.isRendezvous()) {
            // local listeners are called during repropagate
            repropagate(message, header);
        } else {
            callLocalListeners(message, srcAddr, dstAddr);
        }
    }

    /**
     *  Calls the local listeners for a given pipe
     */
    private void callLocalListeners(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        srcAddr = (null == srcAddr) ?  null : EndpointAddress.unmodifiableEndpointAddress(srcAddr);
        dstAddr = (null == dstAddr) ?  null : EndpointAddress.unmodifiableEndpointAddress(dstAddr);

        Iterator eachInput = Arrays.asList(wireinputpipes.keySet().toArray(new InputPipe[0])).iterator();
        while(eachInput.hasNext()) {
            InputPipeImpl anInputPipe = (InputPipeImpl) eachInput.next();
            Message tmpMsg = (Message) message.clone();

            try {
                anInputPipe.processIncomingMessage(tmpMsg, srcAddr, dstAddr);
            } catch(Throwable ignored) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable during callback (" + anInputPipe + ") for " + anInputPipe.getPipeID(), ignored);
                }
            }
        }
    }

    /**
     *  Repropagate a message.
     */
    void repropagate(Message message, WireHeader header) {

        if ((header.getTTL() <= 1)) {
            // This message ran out of fuel.
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No TTL remaining - discarding " + message + " on " + header.getPipeID());
            }
            return;
        }

        Message msg = (Message) message.clone();

        header.setTTL(header.getTTL() - 1);
        header.addPeer(localPeerId);

        XMLDocument headerDoc = (XMLDocument) header.getDocument(MimeMediaType.XMLUTF8);
        MessageElement elem = new TextDocumentMessageElement(WirePipeImpl.WireTagName, headerDoc, null);

        msg.replaceMessageElement("jxta", elem);

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Repropagating " + msg + " on " + header.getPipeID());
        }

        try {
            if(!repropagater.enqueue(msg)) {
                // XXX bondolo@jxta.org we don't make any attempt to retry.
                // There is a potential problem in that we have accepted the
                // message locally but didn't resend it. If we get another copy
                // of this message then we will NOT attempt to repropagate it
                // even if we should.
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failure repropagating " + msg + " on " + header.getPipeID() + ". Could not queue message.");
                }
            }
        } catch(IOException failed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failure repropagating " + msg + " on " + header.getPipeID(), failed);
            }
        }
    }

    /**
     *  Sends a message on the propagated pipe.  if set is not empty, then the 
     *  message is sent to set of peers.
     *
     *  @param msg  The message to send.
     *  @param peers    The peers to which the message will be sent. If the
     *  set is empty then the message is sent to all members of the pipe that are
     *  connected to the rendezvous, as well as walk the message throuh the network
     */
    void sendMessage(Message msg, Set peers) throws IOException {

        // do local listeners if we are to be one of the destinations
        if(peers.isEmpty() || peers.contains(myGroup.getPeerID())) {
            callLocalListeners(msg, null, null);
        }

        if(peers.isEmpty()) {
            if (myGroup.isRendezvous()) {
                // propagate to my clients
                SrdiIndex srdiIndex = pipeResolver.getSrdiIndex();
                List peerids = srdiIndex.query(PipeService.PropagateType, PipeAdvertisement.IdTag, getPipeID().toString(), Integer.MAX_VALUE);
                peerids.retainAll(rendezvous.getConnectedPeerIDs());

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Propagating " + msg + " to " + peerids.size() + " subscriber peers and walking through peerview.");
                }

                // we clone the message since we are deliberately setting the TTL very low.
                rendezvous.propagate(Collections.enumeration(peerids), (Message) msg.clone(), WirePipeImpl.WireName, pipeService.getServiceParameter(), 1);
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Propagating " + msg + " to whole network.");
                }
                // propagate to local sub-net
                rendezvous.propagateToNeighbors(msg, WirePipeImpl.WireName, pipeService.getServiceParameter(), RendezVousService.DEFAULT_TTL);
            }
            // walk the message through rdv network (edge, or rendezvous)
            rendezvous.walk(msg, WirePipeImpl.WireName, pipeService.getServiceParameter(), RendezVousService.DEFAULT_TTL);
        } else {
            // Send to specific peers
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Propagating " + msg + " to " + peers.size() + " peers.");
            }
            rendezvous.propagate(Collections.enumeration(peers), msg, WirePipeImpl.WireName, pipeService.getServiceParameter(), 1);
        }
    }

    /**
     *  Create a unique (mostly) identifier for this message
     */
    String createMsgId() {
        return UUIDFactory.newSeqUUID().toString();
    }

    /**
     *  Adds a message ID to our table or stored IDs.
     *
     *  @param ID to add.
     *  @return false if ID was added, true if it was a duplicate.
     */
    private boolean recordSeenMessage(String id) {

        UUID msgid = null;
        try {
            msgid = new UUID(id);
        } catch(IllegalArgumentException notauuid) {
            // XXX 20031024 bondolo@jxta.org these two conversions are provided
            // for backwards compatibility and should eventually be removed.
            try {
                msgid = UUIDFactory.newHashUUID(Long.parseLong(id), 0);
            } catch (NumberFormatException notanumber) {
                msgid = UUIDFactory.newHashUUID(id.hashCode(), 0);
            }
        }

        synchronized(msgIds) {
            if (msgIds.contains(msgid)) {
                // Already there. Nothing to do
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("duplicate " + msgid);
                }
                return true;
            }

            if (msgIds.size() >= MAX_RECORDED_MSGIDS) {
                // The cache is full. Remove the oldest
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Remove oldest id");
                }
                msgIds.remove(0);
            }

            msgIds.add(msgid);
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("added " + msgid);
        }

        return false;
    }
}

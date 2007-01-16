/*
 *
 * $Id: LimitedRangeGreeter.java,v 1.1 2007/01/16 11:01:59 thomas Exp $
 *
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights reserved.
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
 * This license is bansed on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.impl.rendezvous.limited;


import java.net.URI;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.LimitedRangeRdvMessage;

import net.jxta.impl.protocol.LimitedRangeRdvMsg;
import net.jxta.impl.rendezvous.RdvGreeter;
import net.jxta.impl.rendezvous.RdvWalk;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;

/**
 * The limited range rendezvous peer greeter.
 *
 **/
public class LimitedRangeGreeter extends RdvGreeter implements EndpointListener {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(LimitedRangeGreeter.class.getName());
    
    public static final String ServiceName = "LR-Greeter";
    
    private PeerGroup group = null;
    private EndpointService endpoint = null;
    private PeerView  rpv = null;
    private RdvWalk walk = null;
    
    private boolean started = false;
    
    private final String svcName;
    private final String svcParam;
    
    public LimitedRangeGreeter(PeerGroup group, RdvWalk walk) {
        super();
        this.rpv = walk.getPeerView();
        this.group = group;
        this.walk = walk;
        
        svcName = ServiceName + group.getPeerGroupID().toString();
        svcParam = walk.getServiceName() + walk.getServiceParam();
        
        this.endpoint = group.getEndpointService();
    }

    /**
     *  {@inheritDoc}
     **/
    public synchronized void start() {
        
        if (started) {
            return;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Listening on " + svcName + "/" + svcParam);
        }
        
        endpoint.addIncomingMessageListener(this, svcName, svcParam);
        
        started = true;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized void stop() {
        if (!started) {
            return;
        }
        
        endpoint.removeIncomingMessageListener(svcName, svcParam);
        
        started = false;
        
        group = null;
        endpoint = null;
        rpv = null;
        walk = null;
        
        super.stop();
    }
    
    /**
     ** This is the EndpointListener incoming message method.
     ** Currentely, all this method has to do, is to recover the upper layer
     ** Service listener, and invoke it.
     **
     ** This is the place where flow control mechanism (denying requests when
     ** the system is overloaded) should be implemented.
     **/
    public void processIncomingMessage(Message message,
            EndpointAddress srcAddr,
            EndpointAddress dstAddr) {
        
        EndpointListener listener = getEndpointListener();
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Processing " + message + " from " + srcAddr);
        }
        
        // Check and update the Limited Range Rdv Message
        if (!checkMessage(message)) {
            // Message is invalid, drop it
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Limited Range Greeter received an invalid message. Dropping it.");
            }
            return;
        }
        
        if (listener != null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Limited Range Greeter calls listener");
            }
            
            try {
                listener.processIncomingMessage(message, srcAddr, dstAddr);
            } catch (Throwable ignored) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Uncaught Throwable in listener (" + listener.getClass().getName() + ")", ignored);
                }
            }
        }
    }

    private LimitedRangeRdvMessage getRdvMessage(Message msg) {
        
        LimitedRangeRdvMsg rdvMsg = null;
        MessageElement el = msg.getMessageElement("jxta", LimitedRangeRdvMessage.Name);
        
        try {
            if (el == null) {
                // The sender did not use this protocol
                return null;
            }
            
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(el.getMimeType(), el.getStream());

            rdvMsg = new LimitedRangeRdvMsg(asDoc);
        } catch (Exception ez) {
            return null;
        }
        
        return rdvMsg;
    }
    
    private boolean checkMessage(Message msg) {
        
        LimitedRangeRdvMessage rdvMsg = getRdvMessage(msg);
        
        if (rdvMsg == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Limited Range Greeter received a message without LimitedRangeRdvMessage" + ": invalid.");
            }
            // Invalid message.
            return false;
        }
        
        if (rdvMsg.getTTL() <= 0) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Discarding " + msg + " TTL= " + rdvMsg.getTTL() + " invalid.");
            }
            // TTL is null.
            return false;
        } else {
            // Decrement TTL
            rdvMsg.setTTL(rdvMsg.getTTL() - 1);
        }
        return true;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void replyMessage(Message msg, Message reply) throws IOException {
        
        LimitedRangeRdvMessage rdvMsg = getRdvMessage(msg);
        
        if (rdvMsg == null) {
            // No RdvMessage. This message was not received by this Greeter.
            throw new IOException("LimitedRangeWalker was not able to send message" + ": not from this greeter");
        }
        
        if (rdvMsg.getTTL() <= 0) {
            // TTL is null.
            throw new IOException("LimitedRangeWalker was not able to send message" + ": ttl expired");
        }

        ID peerid;
        
        try {
            peerid = IDFactory.fromURI(new URI(rdvMsg.getSrcPeerID()));
        } catch (URISyntaxException badID) {
            throw new IllegalArgumentException("Bad ID in message");
        }
        
        PeerViewElement pve = rpv.getPeerViewElement(peerid);
        
        if (null == pve) {
            throw new IOException("LimitedRangeWalker was not able to send message" + ": no pve");
        }
        
        if (!pve.sendMessage(msg, rdvMsg.getSrcSvcName(), rdvMsg.getSrcSvcParams())) {
            throw new IOException("LimitedRangeWalker was not able to send message" + ": send failed");
        }
    }
}

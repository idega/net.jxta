/*
 *
 * $Id: LimitedRangeWalker.java,v 1.1 2007/01/16 11:01:59 thomas Exp $
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


import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.LimitedRangeRdvMessage;
import net.jxta.protocol.RouteAdvertisement;

import net.jxta.impl.protocol.LimitedRangeRdvMsg;
import net.jxta.impl.rendezvous.RdvWalker;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;


/**
 * The Limited Range Walker is designed to be used by Rendezvous Peer
 * in order to propagate a message among them. A target destination peer
 * is used in order to send the message to a primary peer. Then, depending
 * on the TTL, the message is duplicated into two messages, each of them
 * being sent in each "directions" of the RPV.
 **/
public class LimitedRangeWalker implements RdvWalker {
    
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger(LimitedRangeWalker.class.getName());
    
    private LimitedRangeWalk walk = null;
    private PeerGroup group = null;
    private EndpointService endpoint = null;
    private PeerView  rpv = null;
    private final String svcName;
    private final String svcParam;
    
    /**
     * Constructor. Instantiates a new LimitedRangeWalk
     *
     * @param group PeerGroup where this Walker is running
     * @param walk parent walk.
     **/
    public LimitedRangeWalker(PeerGroup group, LimitedRangeWalk walk) {
        this.walk = walk;
        this.group = group;
        this.endpoint = group.getEndpointService();
        this.rpv = walk.getPeerView();
        
        svcName = LimitedRangeGreeter.ServiceName + group.getPeerGroupID().toString();
        svcParam = walk.getServiceName() + walk.getServiceParam();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized void stop() {
        this.walk = null;
        this.group = null;
        this.endpoint = null;
        this.rpv = null;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void resendMessage(Message msg) throws IOException {
        
        // Check if there is already a Rdv Message
        LimitedRangeRdvMessage rdvMsg = getRdvMessage(msg);
        
        if (rdvMsg == null) {
            throw new IOException("No LimitedRangeRdvMessage in " + msg);
        }
        walkMessage(msg, rdvMsg);
    }
    
    private void walkMessage(Message msg, LimitedRangeRdvMessage rdvMsg) throws IOException {
        
        final int dir = rdvMsg.getDirection();
        
        if ((dir == LimitedRangeRdvMessage.BOTH) || (dir == LimitedRangeRdvMessage.UP)) {
            PeerViewElement upPeer = rpv.getUpPeer();
            if (upPeer != null) {
                Message newMsg = (Message) msg.clone();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sending " + newMsg + " [UP] to " + upPeer );
                }
                
                rdvMsg.setDirection(LimitedRangeRdvMessage.UP);
                
                updateRdvMessage(newMsg, rdvMsg);
                upPeer.sendMessage(newMsg, svcName, svcParam );
            }
        }
        
        if ((dir == LimitedRangeRdvMessage.BOTH) || (dir == LimitedRangeRdvMessage.DOWN)) {
            PeerViewElement downPeer = rpv.getDownPeer();
            if (downPeer != null) {
                Message newMsg = (Message) msg.clone();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Sending " + newMsg + " [DOWN] to " + downPeer);
                }
                
                rdvMsg.setDirection(LimitedRangeRdvMessage.DOWN);
                
                updateRdvMessage(newMsg, rdvMsg);
                downPeer.sendMessage(newMsg, svcName, svcParam );
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void sendMessage( PeerID destination, Message msg, String srcSvcName, String srcSvcParam, int ttl, RouteAdvertisement srcRouteAdv ) throws IOException {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending " + msg + " to " + srcSvcName + "/" + srcSvcParam);
        }
        
        // Check if there is already a Rdv Message
        LimitedRangeRdvMessage rdvMsg = getRdvMessage(msg);
        
        if (rdvMsg == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Creating new RDV message for " + msg);
            }
            
            // Create a new one.
            rdvMsg = new LimitedRangeRdvMsg();
            
            rdvMsg.setTTL(ttl);
            rdvMsg.setDirection(LimitedRangeRdvMessage.BOTH);
            rdvMsg.setSrcPeerID( group.getPeerID().toString());
            rdvMsg.setSrcSvcName(srcSvcName);
            rdvMsg.setSrcSvcParams(srcSvcParam);
            if (null != srcRouteAdv) {
                rdvMsg.setSrcRouteAdv(srcRouteAdv.toString());
            }
        }
        
        int useTTL = Math.min(ttl, rdvMsg.getTTL());
        
        useTTL = Math.min(useTTL, rpv.getView().size() + 1);
        
        if( useTTL <= 0 ) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No TTL remaining for " + msg );
            }
            
            return;
        }
        
        rdvMsg.setTTL(useTTL);
        
        // Forward the message according to the direction set in the Rdv Message.
        if( null != destination ) {
            Message tmp = (Message) msg.clone();
            
            updateRdvMessage(tmp, rdvMsg);
            sendToPeer( destination, svcName, svcParam, tmp );
        } else {
            walkMessage(msg, rdvMsg);
        }
    }
    
    /**
     * Replace the old version of the rdvMsg
     **/
    private void updateRdvMessage(Message msg, LimitedRangeRdvMessage rdvMsg)
    throws IOException {
        
        XMLDocument asDoc = (XMLDocument) rdvMsg.getDocument(MimeMediaType.XMLUTF8);
        MessageElement el = new TextDocumentMessageElement(LimitedRangeRdvMessage.Name, asDoc, null);
        
        msg.replaceMessageElement("jxta", el);
    }
    
    private void sendToPeer(PeerID dest, String svcName, String svcParam, Message msg) throws IOException {
        
        PeerViewElement pve = rpv.getPeerViewElement(dest);
        
        if (null == pve) {
            throw new IOException("LimitedRangeWalker was not able to send message" + ": no pve");
        }
        
        if (!pve.sendMessage(msg, svcName, svcParam)) {
            throw new IOException("LimitedRangeWalker was not able to send message" + ": send failed");
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
}

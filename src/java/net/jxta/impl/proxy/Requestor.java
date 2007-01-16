/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
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
 * $Id: Requestor.java,v 1.1 2007/01/16 11:02:12 thomas Exp $
 *
 */

package net.jxta.impl.proxy;

import java.io.IOException;

import net.jxta.peergroup.PeerGroup;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.Advertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class Requestor {
    private final static Logger LOG = Logger.getLogger(Requestor.class.getName());
    
    private PeerGroup group;
    private EndpointAddress address;
    private MessageElement requestId;
    private Messenger messenger;
    
    public boolean send(Message message) {
        if (LOG.isEnabledFor(Level.DEBUG))
            LOG.debug("send to " + address.toString());
        
        try {
            synchronized( this ) {
                if( (null == messenger) || messenger.isClosed() ) {
                    messenger = null;
                    messenger = group.getEndpointService().getMessenger(address);
                    
                    if( null == messenger ) {
                        LOG.warn("Could not get messenger for " + address );
                        return false;
                    }
                }
            }
            
            messenger.sendMessage(message);
        } catch (IOException e) {
            LOG.warn("Could not send message to requestor for " + address, e );
            return false;
        }
        
        if (ProxyService.LOG_MESSAGES) {
            ProxyService.logMessage(message, LOG);
        }
        
        return true;
    }
    
    public boolean send(Advertisement adv, String resultType) {
        if (LOG.isEnabledFor(Level.DEBUG))
            LOG.debug("send " + adv);
        
        Message message = new Message();
        
        if (resultType == null) {
            resultType = "";
        }
        setString(message, ProxyService.RESPONSE_TAG, resultType);
        
        if (requestId != null) {
            message.addMessageElement(ProxyService.PROXYNS, requestId);
        }
        
        if (adv instanceof PeerAdvertisement) {
            PeerAdvertisement peerAdv = (PeerAdvertisement) adv;
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.TYPE_TAG,
            ProxyService.TYPE_PEER, null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.NAME_TAG,
            peerAdv.getName(), null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.ID_TAG,
            peerAdv.getPeerID().toString(), null ) );
            
            if (LOG.isEnabledFor(Level.DEBUG))
                LOG.debug("send PeerAdvertisement name=" + peerAdv.getName() +
                " id=" + peerAdv.getPeerID().toString());
            
        } else if (adv instanceof PeerGroupAdvertisement) {
            PeerGroupAdvertisement groupAdv = (PeerGroupAdvertisement) adv;
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.TYPE_TAG,
            ProxyService.TYPE_GROUP, null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.NAME_TAG,
            groupAdv.getName(), null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.ID_TAG,
            groupAdv.getPeerGroupID().toString(),
            null ) );
            
            LOG.debug("send GroupAdvertisement name=" + groupAdv.getName() +
            " id=" + groupAdv.getPeerGroupID().toString());
            
        } else if (adv instanceof PipeAdvertisement) {
            PipeAdvertisement pipeAdv = (PipeAdvertisement) adv;
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.TYPE_TAG,
            ProxyService.TYPE_PIPE, null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.NAME_TAG,
            pipeAdv.getName(), null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.ID_TAG,
            pipeAdv.getPipeID().toString(), null ) );
            
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.ARG_TAG,
            pipeAdv.getType(), null ) );
            
            if (LOG.isEnabledFor(Level.DEBUG))
                LOG.debug("send PipeAdvertisement name=" + pipeAdv.getName() +
                " id=" + pipeAdv.getPipeID().toString() +
                " arg=" + pipeAdv.getType());
            
        } else {
            return false;
        }
        
        return send(message);
    }
    
    public boolean notifySuccess() {
        LOG.debug("notifySuccess");
        
        Message message = new Message();
        
        message.addMessageElement( ProxyService.PROXYNS,
        new StringMessageElement(
        ProxyService.RESPONSE_TAG,
        ProxyService.RESPONSE_SUCCESS, null ) );
        
        if (requestId != null) {
            message.addMessageElement(ProxyService.PROXYNS, requestId);
        }
        
        return send(message);
    }
    
    public boolean notifyError(String errorString) {
        if (LOG.isEnabledFor(Level.DEBUG))
            LOG.debug("notifyError " + errorString);
        
        Message message = new Message();
        
        if (requestId != null) {
            message.addMessageElement(ProxyService.PROXYNS, requestId);
        }
        
        if (errorString != null && errorString.length() > 0) {
            message.addMessageElement( ProxyService.PROXYNS,
            new StringMessageElement(
            ProxyService.ERROR_MESSAGE_TAG,
            errorString, null ) );
        }
        
        return send(message);
    }
    
    public boolean equals(Object obj) {
        if (LOG.isEnabledFor(Level.DEBUG))
            LOG.debug(this + " equals " + obj);
        
        if (obj instanceof Requestor) {
            Requestor dest = (Requestor)obj;
            if (address != null && dest.address != null) {
                if (dest.address.toString().equals(address.toString())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public String toString() {
        return "Requestor " + address.toString();
    }
    
    private Requestor(PeerGroup group, EndpointAddress address, MessageElement requestId) throws IOException {
        this.group = group;
        this.address = address;
        this.requestId = requestId;
    }
    
    public static Requestor createRequestor(PeerGroup group,
    Message message,
    EndpointAddress address) throws IOException {
        Requestor requestor = null;
        
        LOG.debug("create new Requestor - " + address.toString());
        
        if (address != null) {
            MessageElement elem = message.getMessageElement(ProxyService.REQUESTID_TAG);
            requestor = new Requestor(group, address, elem );
            message.removeMessageElement(elem);
        }
        
        return requestor;
    }
    private void setString(Message message, String tag, String value) {
        StringMessageElement sme = new StringMessageElement(tag, value, null);
        message.addMessageElement(ProxyService.PROXYNS, sme);
    }
}

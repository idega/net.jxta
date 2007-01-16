/*
 *
 * $Id: HttpMessageSender.java,v 1.1 2007/01/16 11:02:05 thomas Exp $
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

package net.jxta.impl.endpoint.servlethttp;


import java.util.Map;
import java.util.WeakHashMap;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.Messenger;
import net.jxta.peer.PeerID;

import net.jxta.exception.PeerGroupException;


/**
 * Simple MessageSender
 */
public class HttpMessageSender implements MessageSender {

    /**
     *    Log4j Logger
     **/
    private final static Logger LOG = Logger.getLogger(HttpMessageSender.class.getName());

    /**
     * The ServletHttpTransport that created this object
     **/
    private final ServletHttpTransport servletHttpTransport;    

    /**
     * The public address for this message sender
     **/
    private final EndpointAddress publicAddress;

    /**
     * The peerId of this peer
     **/
    private final PeerID peerId;

    private Map messengers = new WeakHashMap();

    /**
     * constructor
     **/
    public HttpMessageSender(ServletHttpTransport servletHttpTransport,
            EndpointAddress publicAddress, 
            PeerID peerId) throws PeerGroupException {

        this.servletHttpTransport = servletHttpTransport;
        this.publicAddress = publicAddress;
        this.peerId = peerId;

        if (servletHttpTransport.getEndpointService().addMessageTransport(this) == null) {
            throw new PeerGroupException("Transport registration refused");
        }
    }

    /**
     * {@inheritDoc}
     **/
    public EndpointAddress getPublicAddress() {
        return (EndpointAddress) publicAddress.clone();
    }

    /**
     * {@inheritDoc}
     **/
    public boolean isConnectionOriented() {
        return true;
    }

    /**
     * {@inheritDoc}
     **/
    public boolean allowsRouting() {
        return true;
    }

    /**
     * {@inheritDoc}
     **/
    public Object transportControl(Object operation, Object Value) {
        return null;
    }

    /**
     * shut down all client connections.
     **/
    public void shutdown() {
        HttpClientMessenger[] all;

        synchronized (messengers) {
            all = (HttpClientMessenger[]) messengers.keySet().toArray(new HttpClientMessenger[0]);
            messengers.clear();
        }
        int i = all.length;

        while (i-- > 0) {
            all[i].doshutdown();
        }
    }

    /**
     * {@inheritDoc}
     **/
    public Messenger getMessenger(EndpointAddress destAddr, Object hintIgnored) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("getMessenger for : " + destAddr);
        }

        if (!getProtocolName().equals(destAddr.getProtocolName())) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot make messenger for protocol :" + destAddr.getProtocolName());
            }

            return null;
        }

        try {
            // Right now we do not want to "announce" outgoing messengers because they get pooled and so must
            // not be grabbed by a listener. If "announcing" is to be done, that should be by the endpoint
            // and probably with a subtely different interface.

            Messenger result = new HttpClientMessenger(servletHttpTransport, peerId, destAddr);

            messengers.put(result, null);

            return result;
        } catch (SocketTimeoutException noConnect) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not connect to " + destAddr);
            }
        } catch (ConnectException noConnect) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to connect to " + destAddr);
            }
        } catch (Throwable e) {
            // could not get connected, return a null messenger
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not make messenger for " + destAddr, e);
            }
        }

        // If we got here, we failed.
        return null;
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
     * {@inheritDoc}
     **/
    public void propagate(Message msg,
            String serviceName,
            String serviceParams,
            String prunePeer) throws IOException {// propagate is not supported on this MessageSender
    }

    /**
     * {@inheritDoc}
     **/
    public boolean ping(EndpointAddress addr) {
        Messenger messenger = getMessenger(addr, null);

        // pool this messenger
        // Ping obsolete. And do not announce an outgoing messenger

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Using http client sender to ping " + addr);
        }

        return (messenger != null);
    }

    /**
     * {@inheritDoc}
     **/
    public String getProtocolName() {
        return servletHttpTransport.protocolName;
    }

    /**
     * {@inheritDoc}
     **/
    public EndpointService getEndpointService() {
        return servletHttpTransport.getEndpointService();
    }
}

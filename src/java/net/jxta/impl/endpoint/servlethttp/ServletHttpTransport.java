/*
 *
 * $Id: ServletHttpTransport.java,v 1.1 2007/01/16 11:02:06 thomas Exp $
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


import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.TextElement;
import net.jxta.document.Element;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.TransportAdvertisement;
import net.jxta.platform.Module;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.endpoint.transportMeter.*;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.protocol.HTTPAdv;

import net.jxta.meter.*;
import net.jxta.impl.meter.*;


/**
 * A JXTA Message Transport
 *
 * <p/>This class is really a facade for the following:<ul>
 *  <li>An HTTP client message sender</li>
 *  <li>An HTTP-server-based message receiver</li>
 *  </ul>
 */
public final class ServletHttpTransport implements Module {
    
    /**
     *    log4j Logger
     **/
    private static final Logger LOG = Logger.getLogger(ServletHttpTransport.class.getName());
    
    /**
     *  The name of the protocol
     */
    protected String protocolName = "http";
    
    /**
     *  PeerGroup we are working for
     **/
    private PeerGroup       peerGroup = null;
    
    /**
     * the HttpMessageSender instance
     **/
    private HttpMessageSender sender = null;
    
    /**
     * the HttpMessageReceiver instance
     **/
    private HttpMessageReceiver receiver = null;
    
    /**
     * the TransportMeter for this httpTransport
     **/
    private TransportMeter        transportMeter;
    
    /**
     * the TransportBindingMeter for unknown connections (pings/errors)
     **/
    private TransportBindingMeter unknownTransportBindingMeter;
    
    /**
     * the public address for this transport
     **/
    private EndpointAddress publicAddress;
    
    /**
     * {@inheritDoc}
     **/
    public synchronized void init(PeerGroup peerGroup, ID assignedID, Advertisement impl) throws PeerGroupException {
        
        this.peerGroup = peerGroup;
        ModuleImplAdvertisement implAdvertisement = (ModuleImplAdvertisement) impl;
        
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) impl;
        ConfigParams peerAdv = (ConfigParams) peerGroup.getConfigAdvertisement();
        
        // Get out invariable parameters from the implAdv
        Element param = implAdv.getParam();
        
        if (param != null) {
            Enumeration list = param.getChildren("Proto");
            
            if (list.hasMoreElements()) {
                TextElement pname = (TextElement) list.nextElement();
                
                protocolName = pname.getTextValue();
            }
        }
        
        param = peerAdv.getServiceParam(assignedID);
        
        Enumeration httpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());
        
        // get the TransportAdv
        if (httpChilds.hasMoreElements()) {
            param = (Element) httpChilds.nextElement();
            Attribute typeAttr = ((Attributable) param).getAttribute("type");
            
            if (!HTTPAdv.getAdvertisementType().equals(typeAttr.getValue())) {
                throw new IllegalArgumentException("Transport adv is not an http adv");
            }
            
            if (httpChilds.hasMoreElements()) {
                throw new IllegalArgumentException("Configuration contained multiple http advertisements");
            }
        } else {
            throw new IllegalArgumentException("Configuration did not contain http advertisement");
        }
        
        Advertisement paramsAdv = AdvertisementFactory.newAdvertisement((TextElement) param);
        
        if (!(paramsAdv instanceof HTTPAdv)) {
            throw new IllegalArgumentException("Provided advertisement was not a " + HTTPAdv.getAdvertisementType());
        }
        
        HTTPAdv httpAdv = (HTTPAdv) paramsAdv;
        
        // determine the local interface to use. If the user specifies
        // one, use that. Otherwise, use the all the available interfaces.
        String interfaceAddressStr = httpAdv.getInterfaceAddress();
        boolean publicAddressOnly = httpAdv.getPublicAddressOnly();
        InetAddress usingInterface = null;
        
        if (interfaceAddressStr != null) {
            try {
                usingInterface = InetAddress.getByName(interfaceAddressStr);
            } catch (UnknownHostException failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Invalid address for local interface address, using default");
                }
                usingInterface = IPUtils.ANYADDRESS;
            }
        } else {
            usingInterface = IPUtils.ANYADDRESS;
        }
        
        int serverSocketPort = httpAdv.getPort();
        
        if ((serverSocketPort < 1) || (serverSocketPort > 65535)) {
            throw new IllegalArgumentException("Illegal port value in advertisement :" + serverSocketPort);
        }
        
        List publicAddresses = getPublicAddresses(httpAdv.isServerEnabled(), httpAdv.getServer(), usingInterface, serverSocketPort, publicAddressOnly);
        
        if (!httpAdv.isServerEnabled() && !httpAdv.isClientEnabled()) {
            throw new IllegalArgumentException("Neither incoming nor outgoing connections configured.");
        }
        
        publicAddress = (EndpointAddress) publicAddresses.get(0);
        
        if (httpAdv.isClientEnabled()) {
            // create the MessageSender
            sender = new HttpMessageSender(this, publicAddress, peerGroup.getPeerID());
        }
        
        // check if the server is enabled
        if (httpAdv.isServerEnabled()) {
            // create the MessageReceiver
            receiver = new HttpMessageReceiver(this, publicAddresses, usingInterface, serverSocketPort);
        }
        
        // Tell tell the world about our configuration.
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring HTTP Message Transport : " + assignedID);
            
            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation:");
                configInfo.append("\n\t\tModule Spec ID: " + implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description: " + implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI: " + implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code: " + implAdvertisement.getCode());
            }
            
            configInfo.append("\n\tGroup Params:");
            configInfo.append("\n\t\tGroup: " + peerGroup.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID: " + peerGroup.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID: " + peerGroup.getPeerID());
            
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tProtocol: " + httpAdv.getProtocol());
            configInfo.append("\n\t\tServer Enabled: " + httpAdv.isServerEnabled());
            configInfo.append("\n\t\tClient Enabled: " + httpAdv.isClientEnabled());
            configInfo.append("\n\t\tPublic address: " + (httpAdv.getServer() == null ? "(unspecified)" : httpAdv.getServer()));
            configInfo.append("\n\t\tInterface address: " + (interfaceAddressStr == null ? "(unspecified)" : interfaceAddressStr));
            configInfo.append("\n\t\tUnicast Server Bind Addr: " + usingInterface.getHostAddress() + ":" + serverSocketPort);
            configInfo.append("\n\t\tPublic Addresses: ");
            configInfo.append("\n\t\t\tDefault Endpoint Addr : " + publicAddress);
            
            Iterator eachPublic = publicAddresses.iterator();
            
            while (eachPublic.hasNext()) {
                EndpointAddress anAddr = (EndpointAddress) eachPublic.next();
                
                configInfo.append("\n\t\t\tEndpoint Addr : " + anAddr);
            }
            
            LOG.info(configInfo);
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized int startApp(String[] args) {
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            TransportServiceMonitor transportServiceMonitor = (TransportServiceMonitor) MonitorManager.getServiceMonitor(peerGroup,
            MonitorResources.transportServiceMonitorClassID);
            
            if (transportServiceMonitor != null) {
                transportMeter = transportServiceMonitor.createTransportMeter("HTTP", publicAddress.toString());
                unknownTransportBindingMeter = transportMeter.getTransportBindingMeter(TransportMeter.UNKNOWN_PEER, TransportMeter.UNKNOWN_ADDRESS);
            }
        }
        
        if (receiver != null) {
            try {
                // Start the http server that runs the receiver.
                receiver.startServer();
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Could not start http message receiver", e);
                }
                return -1; // Can't go on; if we were configured to be a server we must make the failure obvious.
            }
        }
        
        return 0;
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized void stopApp() {
        if (receiver != null) {
            receiver.stopServer();
        }
        
        if (sender != null) {
            sender.shutdown();
        }
    }
    
    /**
     * Returns the endpoint service that this transport is attached to.
     *
     * @return EndpointService The endpoint service that this transport is attached to.
     **/
    public EndpointService getEndpointService() {
        return peerGroup.getEndpointService();
    }
    
    private List getPublicAddresses(boolean serverEnabled,
    String serverName,
    InetAddress usingInterface,
    int serverSocketPort,
    boolean publicAddressOnly
    ) {
        List publicAddresses = new ArrayList();
        
        if (serverEnabled) {
            // Build the publicAddresses
            
            // first in the list is the "public server name". We don't try to
            // resolve this since it might not be resolvable in the context
            // we are running in, we just assume it's good.
            if (serverName != null) {
                // use speced server name.
                EndpointAddress newAddr = new EndpointAddress(protocolName, serverName, null, null);
                
                publicAddresses.add(newAddr);
                if (publicAddressOnly) {
                    return publicAddresses;
                }
            }
        }
        
        // then add the rest of the local interfaces as appropriate
        if (usingInterface.equals(IPUtils.ANYADDRESS)) {
            // its wildcarded
            Iterator eachLocal = IPUtils.getAllLocalAddresses();
            List wildAddrs = new ArrayList();
            
            while (eachLocal.hasNext()) {
                InetAddress anAddress = (InetAddress) eachLocal.next();
                
                String hostAddress;
                if( anAddress instanceof Inet6Address ) {
                    hostAddress = "[" + anAddress.getHostAddress() + "]";
                } else {
                    hostAddress = anAddress.getHostAddress();
                }
                
                EndpointAddress newAddr = new EndpointAddress(protocolName, hostAddress + ":" + Integer.toString(serverSocketPort),
                null, null);
                
                // don't add it if its already in the list
                if (!publicAddresses.contains(newAddr)) {
                    wildAddrs.add(newAddr);
                }
            }
            
            // we sort them so that later equals() will be deterministic.
            // the result of IPUtils.getAllLocalAddresses() is not known
            // to be sorted.
            Collections.sort(wildAddrs, new Comparator() {
                
                public int compare(Object one, Object two) {
                    return one.toString().compareTo(two.toString());
                }
                
                public boolean equals(Object that) {
                    return (this == that);
                }
            });
            
            publicAddresses.addAll(wildAddrs);
        } else {
            // use speced interface
            String hostAddress;
            if( usingInterface instanceof Inet6Address ) {
                hostAddress = "[" + usingInterface.getHostAddress() + "]";
            } else {
                hostAddress = usingInterface.getHostAddress();
            }
            
            EndpointAddress newAddr = new EndpointAddress(protocolName, hostAddress + ":" + Integer.toString(serverSocketPort),
            null, null);
            
            // don't add it if its already in the list
            if (!publicAddresses.contains(newAddr)) {
                publicAddresses.add(newAddr);
            }
        }
        
        return publicAddresses;
    }
    
    TransportBindingMeter getTransportBindingMeter(String peerIDString, EndpointAddress destinationAddress) {
        if (transportMeter != null) {
            return transportMeter.getTransportBindingMeter((peerIDString != null) ? peerIDString : TransportMeter.UNKNOWN_PEER, destinationAddress);
        } else {
            return null;
        }
    }
    
    TransportBindingMeter getUnknownTransportBindingMeter() {
        return unknownTransportBindingMeter;
    }
}

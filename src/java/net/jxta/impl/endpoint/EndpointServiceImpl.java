/*
 *
 * $Id: EndpointServiceImpl.java,v 1.1 2007/01/16 11:01:54 thomas Exp $
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

package net.jxta.impl.endpoint;


import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Vector;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageFilterListener;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.ThreadedMessenger;
import net.jxta.endpoint.ChannelMessenger;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.ID;
import net.jxta.meter.MonitorResources;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.service.Service;

import net.jxta.exception.PeerGroupException;

import net.jxta.impl.meter.MonitorManager;
import net.jxta.impl.endpoint.endpointMeter.EndpointMeter;
import net.jxta.impl.endpoint.endpointMeter.EndpointMeterBuildSettings;
import net.jxta.impl.endpoint.endpointMeter.EndpointServiceMonitor;
import net.jxta.impl.endpoint.endpointMeter.InboundMeter;
import net.jxta.impl.endpoint.endpointMeter.OutboundMeter;
import net.jxta.impl.endpoint.endpointMeter.PropagationMeter;
import net.jxta.impl.endpoint.relay.RelayClient;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.util.FastHashMap;
import net.jxta.impl.util.SequenceIterator;


/**
 * This class implements the frontend for all the JXTA  endpoint protocols, as
 * well as the API for the implementation of the core protocols that use
 * directly the EndpointService. It theory it only needs to implement core methods.
 * legacy or convenience methods should stay out. However, that would require
 * a two-level interface for the service (internal and public). May be later.
 **/
public class EndpointServiceImpl implements EndpointService, MessengerEventListener {
    
    /**
     *  Log4J Category
     **/
    private static final Logger LOG = Logger.getLogger(EndpointServiceImpl.class.getName());
    
    // // constants ////
    
    /**
     *  The Wire Message Format we will use by default.
     **/
    public static final MimeMediaType DEFAULT_MESSAGE_TYPE = new MimeMediaType("application/x-jxta-msg").intern();
    
    /**
     *  The name of this service.
     **/
    public static final String ENDPOINTSERVICE_NAME = "EndpointService";
    
    /**
     *  The Message empty namespace. This namespace is reserved for use by
     *  applications. It will not be used by core protocols.
     **/
    public static final String  MESSAGE_EMPTY_NS = "";
    
    /**
     *  The Message "jxta" namespace. This namespace is reserved for use by
     *  core protocols. It will not be used by applications.
     **/
    public static final String  MESSAGE_JXTA_NS = "jxta";
    
    /**
     *  Namespace in which the message source address will be placed.
     **/
    public static final String  MESSAGE_SOURCE_NS = MESSAGE_JXTA_NS;
    
    /**
     *  Element name in which the message source address will be placed.
     **/
    public static final String  MESSAGE_SOURCE_NAME = "EndpointSourceAddress";
    
    /**
     *  Namespace in which the message destination address will be placed.
     **/
    public static final String  MESSAGE_DESTINATION_NS = MESSAGE_JXTA_NS;
    
    /**
     *  Element name in which the message destination address will be placed.
     *  This element is used for loopback detection during propagate. Only
     *  propagate messages currently contain this element.
     **/
    public static final String  MESSAGE_DESTINATION_NAME = "EndpointDestinationAddress";
    
    /**
     *  Namespace in which the message source peer address will be placed.
     **/
    public static final String  MESSAGE_SRCPEERHDR_NS = MESSAGE_JXTA_NS;
    
    /**
     *  Element name in which the message source peer address will be placed.
     *  This element is used for loopback detection during propagate. Only
     *  propagated messages currently contain this element.
     **/
    public static final String  MESSAGE_SRCPEERHDR_NAME = "EndpointHeaderSrcPeer";
    
    EndpointServiceMonitor endpointServiceMonitor;
    
    /**
     *  if true then this service has been initialized
     **/
    private boolean initialized = false;
    
    /** the EndpointMeter   **/
    private EndpointMeter endpointMeter;
    private PropagationMeter propagationMeter;
    
    /** tunable: the virtual messenger queue size */
    private int vmQueueSize = 20;
    
    /** tunable: should the parent endpoint be used? */
    private boolean useParentEndpoint = true;
    
    private ModuleImplAdvertisement implAdv = null;
    private ID assignedID = null;
    
    private PeerGroup group = null;
    private String localPeerId = null;
    private EndpointService parentEndpoint = null;
    private String myServiceName = null;
    private PeerGroup parentGroup = null;
    
    /**
     *  The Message Transports which are registered for this endpoint. This is
     *  only the message transport registered locally, it does not include
     *  transports which are used from other groups.
     *  We very rarely add or remove anything and we have our own policy
     *  for duplicates
     *
     *  <p/>Elements are {@see net.jxta.endpoint.MessageTransport}
     **/
    private final ArrayList messageTransports = new ArrayList();
    
    /**
     * Passive listeners for messengers. Three priority sets, so far.
     * Insertion and removal time is not critical. What is critical is
     * getting a copy of the set of elements, and, marginally the iteration
     * through whatever the result is. The best bet seems to use a vector
     * since, all operations we perform on them is synchronized and it
     * is backed by an array. However we do not use the legacy methods.
     **/
    private final List[] passiveMessengerListeners = { new Vector(), new Vector(), new Vector() };
    
    /**
     * The set of listener managed by this instance of the endpoint svc.
     *
     *  <p/>keys are {@see java.lang.String}
     *  <p/>values are {@see net.jxta.endpoint.EndpointListener}
     **/
    private final Map incomingMessageListeners = new FastHashMap(16);
    
    /**
     * The set of shared transport messengers currently ready for use.
     **/
    private final Map messengerMap = new WeakHashMap(32);
    
    /**
     *  The filter listeners.
     *
     *  We rarely add/remove, never remove without iterating
     *  and insert objects that are always unique. So using a set
     *  does not make sense. An array list is the best.
     *  <p/>elements are {@link FilterListenerAndMask}
     **/
    private final List incomingFilterListeners = new ArrayList();
    private final List outgoingFilterListeners = new ArrayList();
    
    /**
     *  Holder for a filter listener and its conditions
     **/
    private static class FilterListenerAndMask {
        public String namespace;
        public String name;
        public MessageFilterListener listener;
        
        public FilterListenerAndMask(MessageFilterListener listener, String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
            this.listener = listener;
        }
        
        public boolean equals(Object target) {
            if (this == target) {
                return true;
            }
            
            if (null == target) {
                return false;
            }
            
            if (target instanceof FilterListenerAndMask) {
                FilterListenerAndMask likeMe = (FilterListenerAndMask) target;
                
                boolean result = (null != namespace) ? (namespace.equals(likeMe.namespace)) : (null == likeMe.namespace);
                
                result &= (null != name) ? (name.equals(likeMe.name)) : (null == likeMe.name);
                result &= (listener == likeMe.listener);
            }
            
            return false;
        }
    }
    
    
    /**
     * A non blocking messenger that obtains a backing (possibly blocking) messenger on-demand.
     **/
    private class CanonicalMessenger extends ThreadedMessenger {
        
        /**
         * If the hint was not used because there already was a transport
         * messenger available, then it is saved here for the next time we
         * are forced to create a new transport messenger by the breakage
         * of the one that's here.
         * The management of hints is a bit inconsistent for now: the hint
         * used may be different dependent upon which invocation created the current canonical messenger
         * and, although we try to use the hint only once (to avoid carrying an invalid hint forever)
         * it may happen that a hint is used long after it was suggested.
         */
        Object hint;
        
        /**
         * The transport messenger that this canonical messenger currently uses.
         **/
        Messenger cachedMessenger = null;
        
        /**
         * Create a new CanonicalMessenger.
         *
         * @param destination who messages should be addressed to
         **/
        public CanonicalMessenger(int vmQueueSize, EndpointAddress destination, EndpointAddress logicalDestination, Object hint, OutboundMeter messengerMeter) {
            super(group.getPeerGroupID(), destination, logicalDestination, vmQueueSize);
            this.hint = hint;
        }
        
        /**
         * close this canonical messenger.
         **/
        public void close() {// No way. Not form the outside.
        }
        
        /**
         * Drop the current messenger.
         **/
        protected void closeImpl() {
            if (cachedMessenger != null) {
                cachedMessenger.close();
                cachedMessenger = null;
            } else {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Internal messenger error: close requested while not connected.");
                }
            }
        }
        
        /**
         * Get a transport messenger to the destination.
         *
         * FIXME - jice@jxta.org 20040413: do better hint management.
         **/
        protected boolean connectImpl() {
            if (cachedMessenger != null) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Internal messenger error: connect requested while connected.");
                }
                cachedMessenger.close();
                cachedMessenger = null;
            }
            
            // Consume the hint, if any.
            Object theHint = hint;
            
            hint = null;
            cachedMessenger = getLocalTransportMessenger(getDestinationAddress(), theHint);
            
            if (cachedMessenger == null) {
                return false;
            }
            
            // FIXME - jice@jxta.org 20040413: it's not too clean: we assume
            // that all transports use BlockingMessenger as the base class for
            // their messengers. If they don't we can't force them to hold the
            // strong reference to the canonical messenger.
            try {
                ((BlockingMessenger) cachedMessenger).setOwner(this);
            } catch (ClassCastException cce) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error(
                    "Transport messengers must all extend BlockingMessenger for now. " + cachedMessenger + " may remain open beyond its use.");
                }
            }
            return true;
        }
        
        /**
         *  {@inheritDoc}
         **/
        protected EndpointAddress getLogicalDestinationImpl() {
            if (cachedMessenger == null) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Internal messenger error: logical destination requested while not connected.");
                }
                return null;
            }
            return cachedMessenger.getLogicalDestinationAddress();
        }
        
        /**
         *  {@inheritDoc}
         **/
        protected void sendMessageBImpl(Message msg, String service, String param) throws IOException {
            if (cachedMessenger == null) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Internal messenger error: send requested while not connected.");
                }
                throw new IOException("Internal messenger error.");
            }
            try {
                cachedMessenger.sendMessageB(msg, service, param);
            } catch (IOException any) {
                // FIXME - jice@jxta.org 20040413: beware of the funky runtime ones.
                cachedMessenger = null;
                throw any;
            }
        }
    }
    
    /**
     *  Create a new EndpointService.
     **/
    public EndpointServiceImpl() {}
    
    /**
     *  Initialize the application passing it its peer group and advertisement.
     *
     *  @param group PeerGroup this application is started from
     *  @param assignedID The ID which this instance should be known by.
     *  @param impl The advertisement for this application
     *
     *  @exception PeerGroupException failure to initialize this application.
     */
    public synchronized void init(PeerGroup group, ID assignedID, Advertisement impl)
    throws PeerGroupException {
        if (initialized) {
            throw new PeerGroupException("Cannot initialize service more than once");
        }
        
        // There's no config of interest in our implAdv, but we must be able
        // to return it if queried. We also need our assigned ID; that's the
        // selector for the element of the peer adv that we have to update.
        this.implAdv = (ModuleImplAdvertisement) impl;
        this.assignedID = assignedID;
        this.group = group;
        this.localPeerId = group.getPeerID().toString();
        
        this.myServiceName = ChannelMessenger.InsertedServicePrefix + group.getPeerGroupID().getUniqueValue().toString();
        
        ConfigParams confAdv = (ConfigParams) group.getConfigAdvertisement();
        XMLElement paramBlock = null;
        
        if (confAdv != null) {
            paramBlock = (XMLElement) confAdv.getServiceParam(assignedID);
        }
        
        if (paramBlock != null) {
            // get our two tunables: virtual messenger queue size, and whether to use the parent endpoint
            Enumeration param;
            
            param = paramBlock.getChildren("MessengerQueueSize");
            if (param.hasMoreElements()) {
                String textQSz = ((XMLElement) param.nextElement()).getTextValue();
                
                try {
                    vmQueueSize = Integer.parseInt(textQSz.trim());
                } catch (NumberFormatException e) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("could not parse MessengerQueueSize string", e);
                    }
                }
            }
            
            param = paramBlock.getChildren("UseParentEndpoint");
            if (param.hasMoreElements()) { // if it's absent, the default is "true"
                String textUPE = ((XMLElement) param.nextElement()).getTextValue();
                
                useParentEndpoint = textUPE.trim().equalsIgnoreCase("true");
            }
            
        }
        
        parentGroup = group.getParentGroup();
        
        if (useParentEndpoint && parentGroup != null) {
            
            parentEndpoint = parentGroup.getEndpointService();
            
            parentEndpoint.addMessengerEventListener(this, EndpointService.LowPrecedence);
        }
        
        initialized = true;
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Endpoint Service : " + assignedID);
            
            configInfo.append("\n\tImplementation :");
            configInfo.append("\n\t\tImpl Description : " + implAdv.getDescription());
            configInfo.append("\n\t\tImpl URI : " + implAdv.getUri());
            configInfo.append("\n\t\tImpl Code : " + implAdv.getCode());
            
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : " + group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : " + group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : " + group.getPeerID());
            
            configInfo.append("\n\tConfiguration :");
            if (null == parentGroup) {
                configInfo.append("\n\t\tHome Group : (none)");
            } else {
                configInfo.append("\n\t\tHome Group : " + parentGroup.getPeerGroupName() + " / " + parentGroup.getPeerGroupID());
            }
            configInfo.append("\n\t\tVirtual Messenger Queue Size : " + vmQueueSize);
            if (group.getPeerGroupID().equals(PeerGroupID.worldPeerGroupID)) {
                configInfo.append("\n\tQuota Incoming Message Params :");
                configInfo.append("\n\t\tMax message size : " + QuotaIncomingMessageListener.GmaxMsgSize);
                configInfo.append("\n\t\tMax message senders : " + QuotaIncomingMessageListener.GmaxSenders);
            }
            
            LOG.info(configInfo);
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int startApp(String[] args) {
        // Fix-Me: when Load order Issue is resolved this should fail
        // until it is able to get a non-failing service Monitor (or
        // null = not monitoring) Fix-Me: it is ok because of the hack
        // in StdPeerGroup that starts endpoint service first
        if (EndpointMeterBuildSettings.ENDPOINT_METERING) { // Fix-Me: Move to startApp() when load order issue is resolved
            endpointServiceMonitor = (EndpointServiceMonitor) MonitorManager.getServiceMonitor(group, MonitorResources.endpointServiceMonitorClassID);
            
            if (endpointServiceMonitor != null) {
                endpointMeter = endpointServiceMonitor.getEndpointMeter();
            }
        }
        
        if (parentEndpoint != null) {
            
            Iterator parentMTs = parentEndpoint.getAllMessageTransports();
            
            synchronized (this) {
                while (parentMTs.hasNext()) {
                    addProtoToAdv((MessageTransport) parentMTs.next());
                }
            }
        }
        
        return 0;
    }
    
    /**
     *  {@inheritDoc}
     *
     * <p/>The protocols and services are going
     * to be stopped as well. When they are, they will unreference us and
     * we'll go into oblivion.
     */
    public void stopApp() {
        
        if (parentEndpoint != null) {
            parentEndpoint.removeMessengerEventListener(this, EndpointService.LowPrecedence);
        }
        
        // Clear up the passiveMessengersListeners
        for (int i = 0; i < 3; ++i) {
            List list = passiveMessengerListeners[i];
            
            if (list != null) {
                list.clear();
            }
        }
        
        // Clear up the HashMap
        if (incomingMessageListeners != null) {
            try {
                incomingMessageListeners.clear();
            } catch (Exception ez) {// Not much can be done
            }
        }
        
        // Avoid cross-reference problems with the GC
        
        // group = null;
        // parentEndpoint = null;
        // parentGroup = null;
        
        // The above is not really needed and until we have a very orderly shutdown, it causes NPEs
        // that are hard to prevent.
    }
    
    /**
     * Returns the group to which this EndpointServiceImpl is attached.
     *
     * @return PeerGroup the group.
     */
    public PeerGroup getGroup() {
        return group;
    }
    
    /**
     * Service objects are not manipulated directly to protect usage
     * of the service. A Service interface is returned to access the service
     * methods.
     *
     * @return Service public interface of the service
     *
     */
    public Service getInterface() {
        return new EndpointServiceInterface(this);
    }
    
    /**
     * Returns the advertisment for this service.
     *
     * @return Advertisement the advertisement.
     *
     */
    public Advertisement getImplAdvertisement() {
        return implAdv;
    }
    
    // A vector for statistics between propagateThroughAll and its invoker.
    private static class Metrics {
        int numFilteredOut = 0;
        int numPropagatedTo = 0;
        int numErrorsPropagated = 0;
    }
    
    private void propagateThroughAll(Iterator eachProto,
                                     Message myMsg,
                                     String serviceName,
                                     String serviceParam,
                                     Metrics metrics) {

       Message filtered = null;

        while (eachProto.hasNext()) {
            MessageTransport aTransport = (MessageTransport) eachProto.next();

            try {
                if (!(aTransport instanceof MessageSender)) {
                    continue;
                }

                MessageSender sender = (MessageSender) aTransport;
                if (!sender.isPropagateEnabled()) {
                    //no sense in consuming resources
                    continue;
                }

                if (null == filtered) {
                    // run process filters only once
                    filtered = processFilters(myMsg,
                                              sender.getPublicAddress(),
                                               new EndpointAddress(group.getPeerGroupID(), serviceName, serviceParam),
                                               false);
                }
                
                if (null == filtered) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("   message "+filtered+ " discarded upon filter decision");
                    }
                    
                    if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                        metrics.numFilteredOut++;
                    }
                    break;
                }
                
                sender.propagate((Message) filtered.clone(), serviceName, serviceParam, null);
                
                if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                    metrics.numPropagatedTo++;
                }
            } catch (Exception e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failed propagating message "+filtered+ " on message transport " + aTransport, e);
                }

                if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                    metrics.numErrorsPropagated++;
                }
                continue;
            }
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public void propagate(Message srcMsg, String serviceName, String serviceParam)
    throws IOException {
        long startPropagationTime = 0;
        
        Metrics metrics = new Metrics();
        
        // Keep the orig unchanged for metering reference and caller's benefit, but
        // we are forced to clone it here, because we add a header.
        Message myMsg = (Message) srcMsg.clone();
        
        if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
            startPropagationTime = System.currentTimeMillis();
        }
        
        // Add our header.
        MessageElement srcHdrElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NAME, localPeerId, (MessageElement) null);
        
        myMsg.replaceMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NS, srcHdrElement);
        
        // Do the local transports with the plain address.
        Iterator eachProto = getAllLocalTransports();
        
        propagateThroughAll(eachProto, (Message) myMsg.clone(), serviceName, serviceParam, metrics);
        
        // Do the parent transports with a mangled address.
        if (parentEndpoint != null) {
            eachProto = parentEndpoint.getAllMessageTransports();
            //FIXME what happens when service name, and/or param are null
            propagateThroughAll(eachProto, (Message) myMsg.clone(), myServiceName, serviceName + "/" + serviceParam, metrics);
        }
        
        if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
            PropagationMeter propagationMeter = endpointServiceMonitor.getPropagationMeter(serviceName, serviceParam);
            
            propagationMeter.registerPropagateMessageStats(metrics.numPropagatedTo, metrics.numFilteredOut, metrics.numErrorsPropagated,
            System.currentTimeMillis() - startPropagationTime);
        }
    }
    
    /**
     *  Process the filters for this message.
     **/
    private Message processFilters(Message message,
                                   EndpointAddress srcAddress,
                                   EndpointAddress dstAddress,
                                   boolean         incoming) {
        
        Iterator eachFilter = incoming ? incomingFilterListeners.iterator() : outgoingFilterListeners.iterator();
        
        while (eachFilter.hasNext()) {
            FilterListenerAndMask aFilter = (FilterListenerAndMask) eachFilter.next();
            
            Message.ElementIterator eachElement = message.getMessageElements();
            
            while (eachElement.hasNext()) {
                MessageElement anElement = (MessageElement) eachElement.next();
                
                if ((null != aFilter.namespace) && (!aFilter.namespace.equals(eachElement.getNamespace()))) {
                    continue;
                }
                
                if ((null != aFilter.name) && (!aFilter.name.equals(anElement.getElementName()))) {
                    continue;
                }
                
                message = aFilter.listener.filterMessage(message, srcAddress, dstAddress);
                
                if (null == message) {
                    return null;
                }
            }
        }
        
        // If we got here, no filter has rejected the message. Keep processing it.
        return message;
    }
    
    private static EndpointAddress demangleAddress(EndpointAddress mangled) {
        String serviceName = mangled.getServiceName();
        
        if (!serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
            // not a mangled address
            return mangled;
        }
        
        String serviceParam = mangled.getServiceParameter();
        
        if (null == serviceParam) {
            // it has no param, its a null destination.
            return new EndpointAddress(mangled, null, null);
        }
        
        int slashAt = serviceParam.indexOf('/');
        
        if (-1 == slashAt) {
            // param has no param portion.
            return new EndpointAddress(mangled, serviceParam, null);
        }
        
        return new EndpointAddress(mangled, serviceParam.substring(0, slashAt), serviceParam.substring(slashAt + 1));
    }
    
    /**
     * {@inheritDoc}
     **/
    public void processIncomingMessage(Message msg, EndpointAddress srcAddress, EndpointAddress dstAddress) {
        
        // check for propagate loopback.
        MessageElement srcPeerElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NS, EndpointServiceImpl.MESSAGE_SRCPEERHDR_NAME);
        
        if (null != srcPeerElement) {
            msg.removeMessageElement(srcPeerElement);
            String srcPeer = srcPeerElement.toString();
            
            if (localPeerId.equals(srcPeer)) {
                // This is a loopback. Discard.
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug( msg + " is a propagate loopback. Discarded");
                }
                
                if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                    endpointMeter.discardedLoopbackDemuxMessage();
                }
                
                return;
            }
        }
        
        if (null == dstAddress) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("null destination address, discarding message " + msg.toString());
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.invalidIncomingMessage();
            }
            
            return;
        }
        
        // Decode the destination address.
        // We want:
        // 1 - a version of the address that does not have the grp redirection.
        // 2 - a version of the serviceName that includes BOTH the group redirection and the original service name.
        // 3 - the original service param; without the original service name stuck to it.
        // So, basically we want the original serviceName part stuck to the group mangling, not stuck to the original
        // serviceParam. We do that by cut/pasting from both the mangled and demangled versions of the address.
        
        EndpointAddress demangledAddress = demangleAddress(dstAddress);
        String decodedServiceName = demangledAddress.getServiceName();
        String decodedServiceParam = demangledAddress.getServiceParameter();
        
        // Do filters for this message:
        // FIXME - jice 20040417 : filters are likely broken, now. They do not see messages
        // from xports in parent groups.  For those messages that are seen, demangled address seems to be the useful one.
        msg = processFilters(msg, srcAddress, demangledAddress, true);
        
        // If processFilters retuns null, the message is to be discarded.
        if (msg == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Message discarded during filter processing");
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.incomingMessageFilteredOut();
            }
            
            return;
        }
        
        if ((null == decodedServiceName) || (0 == decodedServiceName.length())) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("dest serviceName must not be null, discarding message " + msg);
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.invalidIncomingMessage();
            }
            
            return;
        }
        
        // Now that we know the original service name is valid, finish building the decoded version.
        if (demangledAddress != dstAddress) {
            decodedServiceName = dstAddress.getServiceName() + "/" + decodedServiceName;
        }
        
        // First, try the regular destination
        EndpointListener h = null;
        
        if (null != decodedServiceParam) {
            h = (EndpointListener) incomingMessageListeners.get(decodedServiceName + "/" + decodedServiceParam);
        }
        
        // Didn't find it with param, maybe there is a generic listener for the service
        if (h == null) {
            h = (EndpointListener) incomingMessageListeners.get(decodedServiceName);
        }
        
        // Didn't find it still, try the compatibility name.
        if (h == null) {
            h = (EndpointListener) incomingMessageListeners.get(decodedServiceName + decodedServiceParam);
        }
        
        // Still no listener? oh well.
        if (h == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn(
                "No listener for '" + dstAddress + "' in group " + group + "\ndecodedServiceName :" + decodedServiceName
                + "\tdecodedServiceParam :" + decodedServiceParam);
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noListenerForIncomingMessage();
            }
            
            return; // noone cares for this message
        }
        
        // call the listener
        
        try {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Calling listener '" + dstAddress + "' for " + msg.toString());
            }
            
            h.processIncomingMessage(msg, srcAddress, demangledAddress);
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.incomingMessageSentToEndpointListener();
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Uncaught throwable from listener for " + dstAddress, all);
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.errorProcessingIncomingMessage();
            }
        }
    }
    
    /**
     * Handles the given incoming message by calling the listener specified
     * by its destination as returned by the getDestAddress() method of the
     * message.
     *
     * @param msg The message to be delivered.
     */
    public void demux(Message msg) {
        
        // Get the message destination
        MessageElement dstAddressElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS,
        EndpointServiceImpl.MESSAGE_DESTINATION_NAME);
        
        if (null == dstAddressElement) {
            // No destination address... Just discard
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( msg + " has no destination address. Discarded");
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noDestinationAddressForDemuxMessage();
            }
            
            return;
        }
        
        msg.removeMessageElement(dstAddressElement);
        EndpointAddress dstAddress = new EndpointAddress(dstAddressElement.toString());
        
        // Get the message source
        MessageElement srcAddressElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, EndpointServiceImpl.MESSAGE_SOURCE_NAME);
        
        if (null == srcAddressElement) {
            // No destination address... Just discard
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( msg + " has no source address. Discarded");
            }
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noSourceAddressForDemuxMessage();
            }
            
            return;
        }
        msg.removeMessageElement(srcAddressElement);
        EndpointAddress msgScrAddress = new EndpointAddress(srcAddressElement.toString());
        
        processIncomingMessage(msg, msgScrAddress, dstAddress);
        
        if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
            endpointMeter.demuxMessageProcessed();
        }
    }
    
    /**
     * {@inheritDoc}
     *
     * @deprecated Try and get a messenger instead
     **/
    public boolean ping(EndpointAddress addr) {
        return true;
    }
    
    /**
     * {@inheritDoc}
     **/
    public MessengerEventListener addMessageTransport(MessageTransport transpt) {
        
        synchronized (messageTransports) {
            // check if it is already installed.
            if (!messageTransports.contains(transpt)) {
                
                clearProtoFromAdv(transpt); // just to be safe
                messageTransports.add(transpt);
                addProtoToAdv(transpt);
                
                // FIXME: For now, we return this. Later we might return something else, so that we can take
                // advantage of the fact that we know that the event is from a local transport.
                // That will help cleaning up the incoming messenger mess.
                return this;
            }
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean removeMessageTransport(MessageTransport transpt) {
        
        boolean removed = false;
        
        synchronized (messageTransports) {
            removed = messageTransports.remove(transpt);
        }
        
        if (removed) {
            clearProtoFromAdv(transpt);
        }
        
        return removed;
    }
    
    /**
     * {@inheritDoc}
     **/
    public Iterator getAllMessageTransports() {
        if (null != parentEndpoint) {
            return new SequenceIterator(getAllLocalTransports(), parentEndpoint.getAllMessageTransports());
        } else {
            return getAllLocalTransports();
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public MessageTransport getMessageTransport(String name) {
        Iterator allTransports = getAllMessageTransports();
            
            while (allTransports.hasNext()) {
                MessageTransport transpt = (MessageTransport) allTransports.next();
                
                if (transpt.getProtocolName().equals(name)) {
                    return transpt;
                }
        }
        return null;
    }
    
    private void addProtoToAdv(MessageTransport proto) {
        
        boolean relay = false;
        
        try {
            if (!(proto instanceof MessageReceiver)) {
                return;
            }
            
            // no value to publish for the router endpoint address
            if (proto instanceof EndpointRouter) {
                // register the corresponding group to relay connection events
                addActiveRelayListener(group);
                return;
            }
            
            // register this group to Relay connection events
            if (proto instanceof RelayClient) {
                relay = true;
                ((RelayClient) proto).addActiveRelayListener(group);
            }
            
            
            // get the list of addresses
            Iterator allAddresses = ((MessageReceiver) proto).getPublicAddresses();
            Vector ea = new Vector();
            
            while (allAddresses.hasNext()) {
                EndpointAddress anEndpointAddress = (EndpointAddress) allAddresses.next();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Adding endpoint address to route advertisement : " + anEndpointAddress);
                }
                
                ea.add(anEndpointAddress.toString());
            }
            
            PeerAdvertisement padv = group.getPeerAdvertisement();
            StructuredDocument myParam = (StructuredDocument) padv.getServiceParam(assignedID);
            
            RouteAdvertisement route = null;
            
            if (myParam != null) {
                Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                
                if( paramChilds.hasMoreElements()) {
                    // we have an advertisement just add the new access points
                    XMLElement param = (XMLElement) paramChilds.nextElement();
                    
                    route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement( param);
                    route.addDestEndpointAddresses(ea);
                    if (relay) {
                        // need to add the relay info if we have some
                        Vector hops = ((RelayClient) proto).getActiveRelays(group);
                        
                        if ((hops != null) && !hops.isEmpty()) {
                            route.setHops(hops);
                        }
                    }
                }
            }
            
            if( null == route ) {
                // None yet, so create a new Route Advertisement
                // create the RouteAdvertisement that will contain the route to
                // the peer. At this point we only know the peer endpoint addresses
                // no hops are known
                
                // create the destination access point
                AccessPointAdvertisement destAP = (AccessPointAdvertisement) AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
                
                // we don't need to include the PeerID since it's already in
                // the PeerAdv just add the set of endpoints
                destAP.setEndpointAddresses(ea);
                
                // create the route advertisement
                route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
                route.setDest(destAP);
                
                if (relay) {
                    // need to add the relay info if we have some
                    Vector hops = ((RelayClient) proto).getActiveRelays(group);
                    
                    if ((hops != null) && !hops.isEmpty()) {
                        route.setHops(hops);
                    }
                }
            }
            
            // create the param route
            XMLDocument newParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            XMLDocument xptDoc = (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8);
            
            StructuredDocumentUtils.copyElements(newParam, newParam, xptDoc);
            
            padv.putServiceParam(assignedID, newParam);
            
            // publish the new advertisement
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Exception adding message transport ", ex);
            }
        }
    }
    
    private void clearProtoFromAdv(MessageTransport transpt) {
        
        try {
            if (!(transpt instanceof MessageReceiver)) {
                return;
            }
            
            // no value to publish the router endpoint address
            if (transpt instanceof EndpointRouter) {
                // register the corresponding group in the relay
                removeActiveRelayListener(group);
                return;
            }
            
            // register this group to Relay connection events
            if (transpt instanceof RelayClient) {
                ((RelayClient) transpt).removeActiveRelayListener(group);
            }
            
            Iterator allAddresses = ((MessageReceiver) transpt).getPublicAddresses();
            Vector ea = new Vector();
            
            while (allAddresses.hasNext()) {
                EndpointAddress anEndpointAddress = (EndpointAddress) allAddresses.next();
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Removing endpoint address from route advertisement : " + anEndpointAddress);
                }
                
                ea.add(anEndpointAddress.toString());
            }
            
            PeerAdvertisement padv = group.getPeerAdvertisement();
            XMLDocument myParam = (XMLDocument) padv.getServiceParam(assignedID);
            
            if (myParam == null) {
                return;
            }
            
            Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
            
            if ( !paramChilds.hasMoreElements()) {
                return;
            }
            
            XMLElement param = (XMLElement) paramChilds.nextElement();
            
            RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);
            
            route.removeDestEndpointAddresses(ea);
            
            // update the new route to a new parm structure.
            XMLDocument newParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            
            XMLDocument xptDoc = (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8);
            
            StructuredDocumentUtils.copyElements(newParam, newParam, xptDoc);
            
            // put the parms back.
            padv.putServiceParam(assignedID, newParam);
            
            // publish the new advertisement
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Exception removing messsage transport ", ex);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean addMessengerEventListener(MessengerEventListener listener, int prio) {
        int priority = prio;
        
        if (priority > EndpointService.HighPrecedence) {
            priority = EndpointService.HighPrecedence;
        }
        
        if (priority < EndpointService.LowPrecedence) {
            priority = EndpointService.LowPrecedence;
        }
        
        passiveMessengerListeners[priority].add(listener);
        
        return true;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean removeMessengerEventListener(MessengerEventListener listener, int prio) {
        int priority = prio;
        
        if (priority > EndpointService.HighPrecedence) {
            priority = EndpointService.HighPrecedence;
        }
        if (priority < EndpointService.LowPrecedence) {
            priority = EndpointService.LowPrecedence;
        }
        passiveMessengerListeners[priority].remove(listener);
        return true;
    }
    
    /**
     * {@inheritDoc}
     **/
    public boolean addIncomingMessageListener(EndpointListener listener, String serviceName, String serviceParam) {
        
        if (null == listener) {
            throw new IllegalArgumentException("EndpointListener must be non-null");
        }
        
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        
        if (-1 != serviceName.indexOf('/')) {
            throw new IllegalArgumentException("serviceName may not contain '/' characters");
        }
        
        String address = serviceName;
        
        if (null != serviceParam) {
            address += "/" + serviceParam;
        }
        
        synchronized (incomingMessageListeners) {
            if (incomingMessageListeners.containsKey(address)) {
                return false;
            }
            
            InboundMeter incomingMessageListenerMeter = null;
            
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
                incomingMessageListenerMeter = endpointServiceMonitor.getInboundMeter(serviceName, serviceParam);
            }
            
            if (!(listener instanceof QuotaIncomingMessageListener)) {
                listener = new QuotaIncomingMessageListener(address, listener, incomingMessageListenerMeter);
            }
            
            incomingMessageListeners.put(address, listener);
        }
        
        if (parentEndpoint != null) {
            if (serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
                // The listener name is already re-written.
                // The listener is already a quota listener; we made extra sure of that before tucking it into our local map.
                parentEndpoint.addIncomingMessageListener(listener, serviceName, serviceParam);
            } else {
                parentEndpoint.addIncomingMessageListener(listener, myServiceName, address);
            }
        }
        
        return true;
    }
    
    /**
     * {@inheritDoc}
     **/
    public EndpointListener removeIncomingMessageListener(String serviceName,
    String serviceParam) {
        
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        
        if (-1 != serviceName.indexOf('/')) {
            throw new IllegalArgumentException("serviceName may not contain '/' characters");
        }
        
        String address = serviceName;
        
        if (null != serviceParam) {
            address += "/" + serviceParam;
        }
        
        QuotaIncomingMessageListener removedListener = null;
        EndpointListener result = null;
        
        synchronized (incomingMessageListeners) {
            removedListener = (QuotaIncomingMessageListener) incomingMessageListeners.remove(address);
            if (removedListener != null) {
                result = removedListener.getListener();
                // We need to explicitly close the  QuotaIncomingMessageListener
                removedListener.close();
            }
        }
        
        if (parentEndpoint != null) {
            if (serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
                parentEndpoint.removeIncomingMessageListener(serviceName, serviceParam);
            } else {
                parentEndpoint.removeIncomingMessageListener(myServiceName, address);
            }
        }
        
        return result;
    }
    
    /**
     * Returns a local transport that can send to the given address For now this
     * is based only on the protocol name.
     **/
    private MessageSender getLocalSenderForAddress(EndpointAddress addr) {
        
        Iterator localTransports = getAllLocalTransports();
        
        while (localTransports.hasNext()) {
            MessageTransport transpt = (MessageTransport) localTransports.next();
            
            if (!transpt.getProtocolName().equals(addr.getProtocolName())) {
                continue;
            }
            
            if (!(transpt instanceof MessageSender)) { // Do we allow non-senders in the list ?
                continue;
            }
            
            return (MessageSender) transpt;
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p/>Note: canonical messenger itself does not do any address rewritting.
     * Any address rewritting must be specified when getting a channel. However,
     * canonical knows the default group redirection for its owning endpoint and
     * will automatically skip redirection if it is the same.
     */
    
    public Messenger getCanonicalMessenger(EndpointAddress addr, Object hint) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            Throwable trace = new Throwable("Stack Trace");
            StackTraceElement elements[] = trace.getStackTrace();
            
            int position = 1;
            
            while (elements[position].getClassName().startsWith("net.jxta.impl.endpoint.EndpointService")) {
                position++;
            }
            
            if ((elements.length - 1) == position) {
                position--;
            }
            
            LOG.debug("Get Messenger for " + addr + " by " + elements[position]);
        }
        
        if (addr == null) {
            throw new IllegalArgumentException("null endpoint address not allowed.");
        }
        
        // Check the canonical map.
        synchronized (messengerMap) {
            Reference ref = (Reference) messengerMap.get(addr);
            
            if (ref != null) {
                Messenger found = (Messenger) ref.get();
                
                // If it is USABLE, return it.
                if ((found != null) && ((found.getState() & Messenger.USABLE) != 0)) {
                    return found;
                }
                
                // It has been GCed or is nolonger USABLE. Make room for a new one.
                messengerMap.remove(ref);
            }
            
            if (getLocalSenderForAddress(addr) != null) {
                
                OutboundMeter messengerMeter = null;
                
                if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
                    messengerMeter = endpointServiceMonitor.getOutboundMeter(addr);
                }
                
                // The hint is saved in the canonical messenger and will be used
                // when that virtual messenger first faces the need to create a
                // transport messenger. As of now, the logical dest is unknown.
                Messenger m = new CanonicalMessenger(vmQueueSize, addr, null, hint, messengerMeter);
                
                messengerMap.put(m.getDestinationAddressObject(), new SoftReference(m));
                return m;
            }
        }
        
        // If we're here, we do not have any such transport.
        // Try our ancestors enpoints, if any.
        
        if (parentEndpoint == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Couldn't create messenger for : " + addr);
            }
            return null;
        }
        
        return parentEndpoint.getCanonicalMessenger(addr, hint);
    }
    
    /**
     * Return only the message transport registered locally.
     */
    protected Iterator getAllLocalTransports() {
        List transportList;
        
        synchronized (messageTransports) {
            transportList = Arrays.asList(messageTransports.toArray());
        }
        
        return transportList.iterator();
    }
    
    /**
     * Returns a messenger from one of the transports registered with this very endpoint
     * service. Not one of of the parent ones.
     **/
    protected Messenger getLocalTransportMessenger(EndpointAddress addr, Object hint) {
        
        MessageSender sender = (MessageSender) getLocalSenderForAddress(addr);
        Messenger messenger = null;
        
        if (sender != null) {
            
            EndpointAddress addressToUse = (EndpointAddress) addr.clone();
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Trying address '" + addressToUse + "' with : " + sender);
            }
            
            messenger = sender.getMessenger(addressToUse, hint);
        }
        
            if (messenger == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Couldn't create messenger for : " + addr);
            }
        }
        
        return messenger;
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized void addIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        if (null == listener) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        
        FilterListenerAndMask aFilter = new FilterListenerAndMask(listener, namespace, name);
        
        incomingFilterListeners.add(aFilter);
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized void addOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        if (null == listener) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        
        FilterListenerAndMask aFilter = new FilterListenerAndMask(listener, namespace, name);
        
        outgoingFilterListeners.add(aFilter);
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized MessageFilterListener removeIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        Iterator eachListener = incomingFilterListeners.iterator();
        
        while (eachListener.hasNext()) {
            FilterListenerAndMask aFilter = (FilterListenerAndMask) eachListener.next();
            
            if (listener == aFilter.listener) {
                eachListener.remove();
                return listener;
            }
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     **/
    public synchronized MessageFilterListener removeOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        Iterator eachListener = outgoingFilterListeners.iterator();
        
        while (eachListener.hasNext()) {
            FilterListenerAndMask aFilter = (FilterListenerAndMask) eachListener.next();
            
            if ((listener == aFilter.listener) && ((null != namespace) ? namespace.equals(aFilter.namespace) : (null == aFilter.namespace))
            && ((null != name) ? name.equals(aFilter.name) : (null == aFilter.name))) {
                eachListener.remove();
                return listener;
            }
        }
        
        return null;
    }
    
    /**
     * A messenger from a transport is ready. Redistribute the event to those interrested.
     */
    public boolean messengerReady(MessengerEvent event) {
        
        // FIXME - jice@jxta.org 20040413: now that we share messengers, we should be able to get rid of most of this
        // mess, and in the router, and the relay too.
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("New messenger event for : " + event.getMessenger().getDestinationAddress());
        }
        
        if (!(event.getSource() instanceof MessageTransport)) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("New messenger from non-transport. Ignored");
            }
            return false;
        }
        
        MessageTransport source = (MessageTransport) event.getSource();
        Messenger messenger = event.getMessenger();
        Messenger messengerForHere = messenger;
        EndpointAddress connAddr = event.getConnectionAddress();
        
        int highestPrec = EndpointService.HighPrecedence;
        int lowestPrec = EndpointService.LowPrecedence;
        
        // If there's no connection address we just pass the messenger
        // around everywhere; it is unspecified which group it is for.
        // Else, we must figure out if it is for this group, or must be
        // passed upStack (if any).
        if (connAddr != null) {
            
            String cgServiceName = connAddr.getServiceName();
            
            // See if there is a mangling. If not, this means this
            // was sent within this group through a local xport,
            // so it is for here. Else it may be for here (from below)
            // or for upstack.
            
            if (cgServiceName == null || !cgServiceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {// FIXME: jice@jxta.org - 20030512 If we restrict use
                // to "here" we make most backchannels useless. So,
                // the statement below is commented out.  Ideally we
                // should not have to worry about the group targetting
                // of connections, only messages. However the way the
                // relay and the router have to split messengers makes
                // it necessary. This may only be fixed by
                // re-organizing globaly the management of incoming
                // messengers in the endpoint, so that router and
                // relay no-longer need to claim exclusive use of
                // messengers. Since relay clients set the group
                // properly, their messengers are not affected by this
                // branch of the code.
                // lowestPrec = EndpointService.LowPrecedence + 1;
            } else if (!myServiceName.equals(cgServiceName)) {
                
                // This is for upstack only
                highestPrec = EndpointService.LowPrecedence;
                
            } else {
                
                // Mangling present and this is for here (and therefore this is
                // from below). We must demangle. Wrapping is figured
                // later, since we may also have to wrap if there the
                
                lowestPrec = EndpointService.LowPrecedence + 1;
                
                String serviceParam = connAddr.getServiceParameter();
                String realService = null;
                String realParam = null;
                
                if (null != serviceParam) {
                    int slashAt = serviceParam.indexOf('/');
                    
                    if (-1 == slashAt) {
                        realService = serviceParam;
                    } else {
                        realService = serviceParam.substring(0, slashAt);
                        realParam = serviceParam.substring(slashAt + 1);
                    }
                }
                
                connAddr.setServiceName(realService);
                connAddr.setServiceParameter(realParam);
            }
        }
        
        // We make a channel in all cases, the channel will decide if the desired grp redirection
        // requires address rewriting or not.
        
        // As for a MessageWatcher for implementing sendMessage-with-listener, we do not provide one
        // mostly because it is difficult to get a hold on the only appropriate one: that of the endpoint
        // service interface of the listener's owner. So, incoming messengers will not support the listener-based send API.
        // Unless someone adds a watcher by hand.
        messengerForHere = event.getMessenger().getChannelMessenger(group.getPeerGroupID(), null, null);
        
        // Call the listener highest precedence first. The first one that
        // keeps the messenger wins.
        for (int prec = highestPrec + 1; prec-- > lowestPrec;) {
            
            // We need to grab the listeners and release the lock.
            // otherwise the sometimes too long operations performed
            // by the listener creates an unnecessary contention.
            // The side effect is that a listener can in theory be
            // called after remove returned. It is unlikely to be a problem
            // for messenger events, but if it is, then we'll have to add
            // reader-writer synch. FIXME: this is yet another idiom that
            // we have all over the place.
            
            List l = passiveMessengerListeners[prec];
            
            if (l == null) {
                continue;
            }
            Object[] allML = l.toArray();
            
            int i = allML.length;
            
            while (i-- > 0) {
                
                MessengerEventListener listener = (MessengerEventListener) allML[i];
                
                try {
                    if (listener.messengerReady(
                    new MessengerEvent(source, prec == EndpointService.LowPrecedence ? messenger : messengerForHere, connAddr))) {
                        // A listener has taken the messenger. we're done.
                        return true;
                    }
                } catch (Throwable all) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Uncaught Throwable in listener", all);
                    }
                }
            }
        }
        return false;
    }
    
    // public MessengerEventListener getMessengerEventListener() {
    // return this;
    // }
    
    // try to find the relay service in our
    // hierachy of endpoints to register our listener group
    // since the group has to register into the relay service. This is not
    // very pretty, but the other way was even worth to register the relay
    // into the endpoint!
    private void addActiveRelayListener(PeerGroup listeningGroup) {
        PeerGroup parentGroup = group.getParentGroup();
        
        while (parentGroup != null) {
            EndpointService parentEndpoint = parentGroup.getEndpointService();
            
            for (Iterator it = parentEndpoint.getAllMessageTransports(); it.hasNext();) {
                MessageTransport mt = (MessageTransport) it.next();
                
                if ((mt instanceof RelayClient)) {
                    ((RelayClient) mt).addActiveRelayListener(listeningGroup);
                    break;
                }
            }
            parentGroup = parentGroup.getParentGroup();
        }
    }
    
    // try to find the relay service in our
    // hierachy of endpoints to unregister our listener group
    private void removeActiveRelayListener(PeerGroup listeningGroup) {
        PeerGroup parentGroup = group.getParentGroup();
        
        while (parentGroup != null) {
            EndpointService parentEndpoint = parentGroup.getEndpointService();
            
            for (Iterator it = parentEndpoint.getAllMessageTransports(); it.hasNext();) {
                MessageTransport mt = (MessageTransport) it.next();
                
                if ((mt instanceof RelayClient)) {
                    ((RelayClient) mt).removeActiveRelayListener(listeningGroup);
                    break;
                }
            }
            parentGroup = parentGroup.getParentGroup();
        }
    }
    
    /*
     * Convenience legacy methods. They are here to reduce the complexity of the class hierarchy but are not supposed to be used.
     */
    
    /**
     * legacy method not supported here.
     *
     * @deprecated legacy method.
     **/
    public boolean getMessenger(MessengerEventListener listener, EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Legacy method not implemented. Use an interface object.");
    }
    
    /**
     * legacy method not supported here.
     *
     * @deprecated legacy method.
     **/
    public Messenger getMessenger(EndpointAddress addr) {
        throw new UnsupportedOperationException("Legacy method not implemented. Use an interface object.");
    }
    
    /**
     * convenience method not supported here.
     **/
    public Messenger getMessengerImmediate(EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Convenience method not implemented. Use an interface object.");
    }
    
    /**
     * convenience method not supported here.
     **/
    public Messenger getMessenger(EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Convenience method not implemented. Use an interface object.");
    }
}

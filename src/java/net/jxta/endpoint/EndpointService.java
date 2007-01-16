/*
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: EndpointService.java,v 1.1 2007/01/16 11:01:27 thomas Exp $
 */

package net.jxta.endpoint;

import java.util.Iterator;

import java.io.IOException;

import net.jxta.peergroup.PeerGroup;
import net.jxta.service.Service;

/**
 * The EndpointService provides the API for sending and receiving messages
 * between peers. In general, applications and services use the
 * {@link net.jxta.pipe.PipeService} rather than using this API directly.
 *
 **/
public interface EndpointService extends Service, EndpointListener {
    
    public static final int LowPrecedence = 0;
    public static final int MediumPrecedence = 1;
    public static final int HighPrecedence = 2;

    /**
     * Returns the group to which this EndpointService is attached.
     *
     * @return PeerGroup the group.
     */
    public PeerGroup getGroup();
    
    /**
     * Returns a messenger to the specified destination. The canonical messenger is shared between all channels which destination
     * contains the same protocol name and protocol address, in all groups that have access to the same transport. The
     * ChannelMessenger returned is configured to send messages to the specified service name and service param when these are not
     * specified at the time of sending.<p/>
     *
     * The channel will also ensure delivery to this EndpointService's group on arrival. The channel is not shared with any other
     * module. That is, each endpoint service interface object (as returned by {@link
     * net.jxta.peergroup.PeerGroup#getEndpointService()}) will return a different channel messenger for the same
     * destination. However, there is no guarantee that two invocations of the same endpoint service interface object for the same
     * destination will return different channel objects. Notably, if the result of a previous invocation is still strongly
     * referenced and in a {@link Messenger#USABLE} state, then that is what this method will return.<p/>
     *
     * This method returns immediately. The messenger is not necessarily resolved (the required underlying connection established,
     * for example), and it might never resolve at all.  Changes in the state of a messenger may monitored with {@link
     * Messenger#getState} and {@link Messenger#waitState}. One may monitor multiple {@link Messenger messengers} (and {@link
     * Message Messages}) at a time by using a {@link net.jxta.util.SimpleSelector}.  One may also arrange to have a listener invoked when
     * resolution is complete by using {@link ListenerAdaptor}.<p/>
     *
     * The hint is interpreted by the transport. The only transport known to consider hints is the endpoint router, and the hint
     * is a route.  As a result, if addr is in the form: jxta://uniqueID, then hint may be a RouteAdvertisement.  If that route is
     * valid the router will add it to it's cache of route and may then use it to succesfully create a messenger to the given
     * destination.  There is no garantee at this time that the route will end up being the one specified, nor that this route
     * will be used only for this messenger (likely the opposite), nor that it will remain in use in the future, nor that it will
     * be used at all. However, if there is no other route, and if the specified route is valid, it will be used rather than
     * seeking an alternative.<p/>
     *
     * @see net.jxta.endpoint.EndpointAddress
     * @see net.jxta.endpoint.ChannelMessenger
     *
     * @param addr The complete destination address.
     * @param hint A hint to be supplied to whichever transport ends-up making the the real messenger. May be null, when no
     * hint applies.
     * @return The messenger, not necessarily functional, nor resolved. May be null if the address
     * is not handled by any of the available transports.
     **/
    public Messenger getMessengerImmediate( EndpointAddress addr, Object hint );

    /**
     * Behaves like {@link #getMessengerImmediate(EndpointAddress, Object)}, except that the invoker is blocked until the
     * messenger resolves or fails to do so.
     *
     * @param addr the destination address.
     * @param hint A hint if there is one. Null, otherwise.
     * @return The messenger. null is returned if the destination address is not reachable.
     **/
    public Messenger getMessenger(EndpointAddress addr, Object hint);
    
    /**
     * Creates and maps a canonical messenger to the specified destination.<p/>
     *
     * Behaves like {@link #getMessengerImmediate(EndpointAddress, Object)} except that it returns a canonical messenger.
     *
     * The messenger is said to be canonical, because there is only one such live object for any given destination address. The
     * term live, here means that the messenger is not in any of the {@link Messenger#TERMINAL} states as defined by {@link
     * MessengerState}. Therefore, for a given destination there may be any number of messengers in a {@link
     * Messenger#TERMINAL} state, but at most one in any other state. As long as such an object exists, all calls to
     * <code>getCanonicalMessenger</code> for the same address return this very object.<p/>
     * 
     * When first created, a canonical messenger is usually in the {@link Messenger#UNRESOLVED} state. It becomes resolved by
     * obtaining an actual transport messenger to the destination upon the first attempt at using it or when first forced to
     * attempt resolution. Should resolution fail at that point, it becomes {@link Messenger#UNRESOLVABLE}. Otherwise, subsequent,
     * failures are repaired automatically by obtaining a new transport messenger when needed. If a failure cannot be repaired,
     * the messenger becomes {@link Messenger#BROKEN}.<p/>
     *
     * <code>getCanonicalMessenger</code> is a recursive function. Exploration of the parent endpoint is done automatically.<p/>
     *
     * <b>Note 1:</b> This method is the most fundamental messenger instantiation method. It creates a different messenger for
     * each variant of destination address passed to the constructor. In general invokers should use plain addresses; stripped of
     * any service-specific destination.<p/>
     *
     * <b>Note 2:</b> The messengers that this method returns, are not generally meant to be used directly. They provide a single
     * queue for all invokers, and do not perform group redirection and only support only a subset of the <code>sendMessage</code>
     * methods.  One must get a properly configured channel in order to send messages.<p/>
     *
     * If one of the other <code>getMessenger</code> methods fits the application needs, it should be preferred.
     *
     * @param addr The destination address. It is recommended, though not mandatory that the address be
     * stripped of its service name and service param elements.
     * @param hint An object, of a type specific to the protocol of the address, that may be provide additional
     * information to the transport in establishing the connection. Typically but not necessarily, this is a route advertisement.
     * If the transport cannot use the hint, or if it is null, it will be ignored.
     * @return A Canonical messenger that obtains transport messengers to the specified address, from LOCAL transports. Returns
     * null if no no local transport handles this type address.
     *
     * @see Messenger
     **/
    public Messenger getCanonicalMessenger( EndpointAddress addr, Object hint );


    /**
     *  Removes the specified listener.
     *
     *  @param listener The listener that would have been called.
     *  @param priority Priority set from which to remove this listener.
     *  @return true if the listener was removed, otherwise false.
     **/
    public boolean removeMessengerEventListener( MessengerEventListener listener, int priority );

    /**
     *  Adds the specified listener for all messenger creation.
     *
     *  @param listener The listener that will be called.
     *  @param priority Order of precedence requested (from 0 to 2). 2 has highest precedence Listeners are called in decreasing
     *  order of precedence. Listeners with equal precedence are called in unpredictible order relative to each other. There
     *  cannot be more than one listener object for a given precedence. Redundant calls have no effect.
     *  @return true if the listener was added, otherwise false.
     **/
    public boolean addMessengerEventListener( MessengerEventListener listener, int priority );
    
    /**
     * Propagates the given message through all the endpoint protocols that are available to this endpoint. Some or all of these
     * endpoint protocols may silently drop the message. Each endpoint protocol may interpret the resquest for propagation
     * differenly. The endpointService does not define which destinations the message will actually reach.
     *
     * <p/>The concatenation of the serviceName and serviceParam arguments uniquely designates the listener to which the message
     * must be delivered on arrival.
     *
     * <p/><strong>WARNING</strong>: The message object should not be reused or modified after the call is made. Concurrent
     * modifications will produce unexpected results.
     *
     * @param srcMsg the message to be propagated.
     * @param serviceName a destination service name
     * @param serviceParam a destination queue name within that service
     * @throws IOException if the message could not be propagated
     **/
    public void propagate( Message srcMsg, String serviceName, String serviceParam) throws IOException;
    
    /**
     * Verifies that the given address can be reached.  The verification is performed by the endpoint protocol designated by the
     * given address, as returned by the getProtocolName() method of this address.
     *
     * <p/>The method, and accuracy of the verification depends upon each endpoint protocol.
     *
     * @param addr is the Endpoint Address to ping.
     * @return boolean true if the address can be reached. False otherwise.
     *
     * @deprecated It now always return true. Try and get a messenger instead.
     **/
    public boolean ping( EndpointAddress addr );
    
    /**
     *  Add a listener for the specified address.
     *
     *  <p/>A single registered listener will be called for incoming messages when (in order of preference) : * <ol> <li>The
     *  service name and service parameter match exactly to the service name and service parameter specified in the destination
     *  address of the message.</li> <li>The service name matches exactly the service name from the * message destination address
     *  and service parameter is null.</li> * </ol>
     *
     *  @param listener the listener
     *  @param serviceName The name of the service destination which will be matched against destination endpoint addresses.
     *  @param serviceParam String containting the value of the service parameter which will be matched against destination
     *  endpoint addresses.  May be null.
     *  @return true if the listener was registered, otherwise false.
     *
     **/
    public boolean addIncomingMessageListener( EndpointListener listener, String serviceName, String serviceParam );
    
    /**
     *  Remove the listener for the specified address.
     *
     *  @param serviceName The name of the service destination which will be matched against destination endpoint addresses.
     *  @param serviceParam String containting the value of the service parameter which will be matched against destination
     *  endpoint addresses.  May be null.
     *  @return The listener which was removed.
     *
     **/
    public EndpointListener removeIncomingMessageListener( String serviceName, String serviceParam );
    
    /**
     *  Registers a message filter listener. Each message will be tested against the list of filters as part of its sending or
     *  receiving.
     *
     *  <p/>The listener is invoked for a message when:
     *  <ul>
     *  <li>The message contains an element which matches exactly the
     *  values specified by namespace and name.</li>
     *  <li>The message contains an element who's namespace value matches
     *  exactly the specified namespace value and the specified name is null.</li>
     *  <li>The message contains an element who's names value matches exactly
     *  the specified name value and the specified namespace is null.</li>
     *  <li>The specified name value and the specified namespace are both null.</li>
     *  </ul>
     *
     *  @param listener    The filter which will be called.
     *  @param namespace only messages containing elements of this namespace which also match the 'name' parameter will be
     *  processed. null may be use to specify all namespaces.
     *  @param name only messages containing elements of this name which also match the 'namespace' parameter will be
     *  processed. null may be use to specify all names.
     **/
    public void addIncomingMessageFilterListener( MessageFilterListener listener, String namespace, String name );
    
    /**
     *  Registers a message filter listener. Each message will be tested against the list of filters as part of its sending or
     *  receiving.
     *
     *  <p/>The listener is invoked for a message when:
     *  <ul>
     *  <li>The message contains an element which matches exactly the
     *  values specified by namespace and name.</li>
     *  <li>The message contains an element who's namespace value matches
     *  exactly the specified namespace value and the specified name is null.</li>
     *  <li>The message contains an element who's names value matches exactly
     *  the specified name value and the specified namespace is null.</li>
     *  <li>The specified name value and the specified namespace are both null.</li>
     *  </ul>
     *
     *  @param listener    The filter which will be called.
     *  @param namespace only messages containing elements of this namespace which also match the 'name' parameter will be
     *  processed. null may be use to specify all namespaces.
     *  @param name only messages containing elements of this name which also
     *  match the 'namespace' parameter will be processed. null may be use to
     *  specify all names.
     **/
    public void addOutgoingMessageFilterListener( MessageFilterListener listener, String namespace, String name );
    
    /**
     * Removes the given listener previously registered under the given element name
     *
     * @param listener the listener that was registered
     **/
    public MessageFilterListener removeIncomingMessageFilterListener( MessageFilterListener listener, String namespace, String name );
    
    /**
     * Removes the given listener previously registered under the given element name
     *
     * @param listener the listener that was registered
     **/
    public MessageFilterListener removeOutgoingMessageFilterListener( MessageFilterListener listener, String namespace, String name );
    
    /**
     *  Delivers the provided message to the correct listener as specified by the message's destination address.
     *
     * @param msg The message to be delivered.
     **/
    public void demux(Message msg);
    
    /**
     * Adds the specified MessageTransport to this endpoint. A MessageTransport may only be added if there are no other equivalent
     * MessageTransports available (as determined by {@link Object#equals(Object) equals()}).
     *
     * <p/>The MessageTransport becomes usable by the endpoint service to send unicast messages and optionaly propagation and ping
     * messages if it is a {@link net.jxta.endpoint.MessageSender}. The endpoint service becomes usable by this MessageTransport
     * to handle incoming messages if it is a {@link MessageReceiver}.
     *
     * @param transpt the MessageTransport to be installed.
     * @return A messenger event listener to invoke when incoming messengers are created.  Null if the MessageTransport was not
     * installed.
     **/
    public MessengerEventListener addMessageTransport( MessageTransport transpt );
    
    /**
     * Removes the given MessageTransport protocol from this endpoint service.
     *
     * <p/>Transports remove themselves from the list when stopped. This method is normally only called from the stoppApp method
     * of the transport. To cleanly remove a transport, call the transport's {@link net.jxta.platform.Module#stopApp() stopApp()}
     * and allow it to call this method.
     *
     * @param transpt the MessageTransport to be removed.
     * @return boolean true if the transport was removed, otherwise false.
     **/
    public boolean removeMessageTransport( MessageTransport transpt  );
    
    /**
     *  Get an iterator of the MessageTransports available to this EndpointService.
     *
     *  @return the iterator of all message transports.
     **/
    public Iterator getAllMessageTransports();


    /**
     *  Get a MessageTransport by name 
     *
     *  @param name The name of the MessageTransport
     *  @return MessageTransport associated with that name
     **/
    public MessageTransport getMessageTransport( String name );

    /**
     * Builds and returns an Messager that may be used to send messages via this endpoint to the specified destination.
     *
     * @see net.jxta.endpoint.EndpointAddress
     * @deprecated This convenience method adds little value. It is strictly equivalent to {@link #getMessenger(EndpointAddress,
     * Object) <code>getMessenger(addr, null)</code>}
     *
     * @param addr the destination address.
     * @return The messenger. null is returned if the destination address is not reachable.
     **/
    public Messenger getMessenger(EndpointAddress addr);

    /**
     * Asynchronously acquire a messenger for the specified address. The listener will be called when the messenger has been
     * constructed.
     *
     * @deprecated This method is being phased out. Prefer one of the other non-blocking variants. If a listener style paradigm is
     * required, use {@link ListenerAdaptor} which emulates this functionality.
     *
     * @param listener the listener to call when the messenger is ready.
     * @param addr the destination for the messenger.
     * @param hint the messenger hint, if any, otherwise null.
     * @return boolean true if the messenger is queued for construction otherwise false.
     **/
    public boolean getMessenger( MessengerEventListener listener, EndpointAddress addr, Object hint );
}

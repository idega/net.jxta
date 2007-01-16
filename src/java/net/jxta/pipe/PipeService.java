/*
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Copyright (c) 2001-2005 Sun Microsystems, Inc. All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 *  
 *  $Id: PipeService.java,v 1.1 2007/01/16 11:02:00 thomas Exp $
 */
package net.jxta.pipe;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;

import net.jxta.service.Service;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.endpoint.Message;
import net.jxta.peer.PeerID;

/**
 * This class defines the API to the JXTA Pipe Service.
 *
 * <p/>Pipes are the core mechanism for exchanging messages
 * between JXTA applications or services.
 *
 * <p/>Pipes are uniquely identified by a
 * {@link net.jxta.protocol.PipeAdvertisement} which is associated with each
 * pipe. Creating the advertisement of a Pipe must be done only once in the
 * lifetime of a Pipe. In fact, a {@link net.jxta.protocol.PipeAdvertisement}
 * represents the pipe on the JXTA network.
 *
 * <p/>Several types of Pipe can be used:
 *
 * <ul>
 * <li> <b>JxtaUnicast</b>: unicast, unreliable and unsecure pipe
 * <li> <b>JxtaUnicastSecure</b>: unicast and secure pipe
 * <li> <b>JxtaPropagate</b>: propagated, unreliable and unsecure pipe
 * </ul>
 *
 * <p/>The type of a Pipe is defined when creating its
 * {@link net.jxta.protocol.PipeAdvertisement}.
 *
 * <p/><b>WARNING:</b> The message object used when sending a pipe message
 * should not be reused or modified after the
 * {@link net.jxta.pipe.OutputPipe#send(Message)} call is made. Concurrent
 * modification of messages will produce unexpected result.
 *
 * @see    net.jxta.protocol.PipeAdvertisement
 * @see    net.jxta.pipe.InputPipe
 * @see    net.jxta.pipe.OutputPipe
 * @see    net.jxta.endpoint.Message
 *
 */
public interface PipeService extends Service {

    /**
     * Unicast, unreliable and unsecure type of Pipe
     */
    public final static String UnicastType = "JxtaUnicast";

    /**
     * Propagated, unsecure and unreliable type of Pipe
     */
    public final static String PropagateType = "JxtaPropagate";

    /**
     * End-to-end secured unicast pipe of Pipe
     */
    public final static String UnicastSecureType = "JxtaUnicastSecure";

    /**
     * Create an InputPipe from a pipe Advertisement
     *
     * @param  adv              is the advertisement of the PipeService.
     * @return                  InputPipe InputPipe object created
     * @exception  IOException  error creating input pipe
     */
    public InputPipe createInputPipe(PipeAdvertisement adv) throws IOException;

    /**
     * create an InputPipe from a pipe Advertisement
     *
     * @param  adv              is the advertisement of the PipeService.
     * @param  listener         PipeMsgListener to receive msgs.
     * @return                  InputPipe InputPipe object created
     * @exception  IOException  error creating input pipe
     */
    public InputPipe createInputPipe(PipeAdvertisement adv, PipeMsgListener listener) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * The pipe will be be resolved within the provided timeout.
     *
     * @param  adv           The advertisement of the pipe being resolved.
     * @param  timeout       time duration in milliseconds to wait for a successful
     * pipe resolution. <code>0</code> will wait indefinitely. All negative
     * values will cause a wait of an inplementation defined non-infinite value.
     * (this behaviour is deprecated and may eventually disappear).
     * @return               OutputPipe the successfully resolved OutputPipe.
     * @throws  IOException  If the pipe cannot be created or failed to resolve
     * within the specified time.
     */
    public OutputPipe createOutputPipe(PipeAdvertisement adv, long timeout) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * The pipe will be be resolved to one of the peers in the set of peer ids
     * provided within the provided timeout.
     *
     * @param  adv           The advertisement of the pipe being resolved.
     * @param  peerid        The peer id of the peer on which  on which the pipe may be
     * resolved. All elements of the enumeration <strong>must</strong> must be
     * of type {@link net.jxta.peer.PeerID}.
     * @param  timeout       time duration in milliseconds to wait for a successful
     * pipe resolution. <code>0</code> will wait indefinitely. All negative
     * values will cause a wait of an inplementation defined non-infinite value.
     * (this behaviour is deprecated and may eventually disappear).
     * @return               OutputPipe the successfully resolved OutputPipe.
     * @deprecated           Use {@link #createOutputPipe(PipeAdvertisement, Set, long)} with a {@link java.util.Collections#singleton(Object)} instead.
     * @throws  IOException  If the pipe cannot be created or failed to resolve
     * within the specified time.
     */
    public OutputPipe createOutputPipe(PipeAdvertisement adv, PeerID peerid, long timeout) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * The pipe will be be resolved to one of the peers in the set of peer ids
     * provided within the provided timeout.
     *
     * @param  adv              The advertisement of the pipe being resolved.
     * @param  resolvablePeers  The non-empty enumeration of peers on which the pipe may be
     * resolved. All elements of the enumeration <strong>must</strong> must be
     * of type {@link net.jxta.peer.PeerID}.
     * @param  timeout          time duration in milliseconds to wait for a successful
     * pipe resolution. <code>0</code> will wait indefinitely. All negative
     * values will cause a wait of an inplementation defined non-infinite value.
     * (this behaviour is deprecated and may eventually disappear).
     * @return                  OutputPipe the successfully resolved OutputPipe.
     * @deprecated              Use {@link #createOutputPipe(PipeAdvertisement, Set, long)} instead.
     * @throws  IOException     If the pipe cannot be created or failed to resolve
     * within the specified time.
     */
    public OutputPipe createOutputPipe(PipeAdvertisement adv, Enumeration resolvablePeers, long timeout) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * The pipe will be be resolved to one of the peers in the set of peer ids
     * provided within the provided timeout.
     *
     * @param  adv              The advertisement of the pipe being resolved.
     * @param  resolvablePeers  The set of peers on which the pipe may be resolved.
     * All elements of the set <strong>must</strong> must be of type
     * {@link net.jxta.peer.PeerID}. <strong>If the Set is empty then the pipe
     * may be resolved to any destination peer.</strong>
     * @param  timeout          time duration in milliseconds to wait for a successful
     * pipe resolution. <code>0</code> will wait indefinitely. All negative
     * values will cause a wait of an inplementation defined non-infinite value.
     * (this behaviour is deprecated and may eventually disappear).
     * @return                  OutputPipe the successfully resolved OutputPipe.
     * @throws  IOException     If the pipe cannot be created or failed to resolve
     * within the specified time.
     */
    public OutputPipe createOutputPipe(PipeAdvertisement adv, Set resolvablePeers, long timeout) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * The pipe may be resolved to any destination peer. When the pipe is
     * resolved the listener will be called.
     *
     * @param  adv           The advertisement of the pipe being resolved.
     * @param  listener      the listener to be called when the pipe is resolved.
     * @throws  IOException  If the pipe cannot be created.
     */
    public void createOutputPipe(PipeAdvertisement adv, OutputPipeListener listener) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * When the pipe is resolved to the peer id provided the listener will be
     * called.
     *
     * @param  adv           The advertisement of the pipe being resolved.
     * @param  peerid        The peer id of the peer on which  on which the pipe may be
     * resolved. All elements of the enumeration <strong>must</strong> must be
     * of type {@link net.jxta.peer.PeerID}.
     * @param  listener      the listener to be called when the pipe is resolved.
     * @deprecated           Use {@link #createOutputPipe(PipeAdvertisement, Set, long)} with a {@link java.util.Collections#singleton(Object)} instead.
     * @throws  IOException  If the pipe cannot be created.
     */
    public void createOutputPipe(PipeAdvertisement adv, PeerID peerid, OutputPipeListener listener) throws IOException;

    /**
     * Attempt to ceate an OutputPipe using the specified Pipe Advertisement.
     * When the pipe is resolved to one of the peers in the set of peer ids
     * provided the listener will be called.
     *
     * @param  pipeAdv          The advertisement of the pipe being resolved.
     * @param  resolvablePeers  The set of peers on which the pipe may be resolved.
     * All elements of the set <strong>must</strong> must be of type
     * {@link net.jxta.peer.PeerID}. <strong>If the Set is empty then the pipe
     * may be resolved to any destination peer.</strong>
     * @param  listener         the listener to be called when the pipe is resolved.
     * @throws  IOException     If the pipe cannot be created.
     */
    public void createOutputPipe(PipeAdvertisement pipeAdv, Set resolvablePeers, OutputPipeListener listener) throws IOException;

    /**
     * Creates a new Message for sending via this Pipe Service.
     *
     * @return        A newly allocated Message.
     * @deprecated    Use new {@link net.jxta.endpoint.Message#Message()} instead.
     */
    public Message createMessage();

    /**
     *  Remove an OutputPipeListener previously registered with
     *  <code>createOuputputPipe</code>.
     *
     * @param  pipeID    listener to remove
     * @param  listener  listener to remove
     * @return           the listener which was removed or null if the key did not have a mapping.
     */
    public OutputPipeListener removeOutputPipeListener(String pipeID, OutputPipeListener listener);
}

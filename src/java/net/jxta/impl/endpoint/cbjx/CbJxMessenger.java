/*
 *
 * $Id: CbJxMessenger.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
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
package net.jxta.impl.endpoint.cbjx;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.impl.endpoint.BlockingMessenger;

/**
 * This class is the Messenger used to send CbJx Messages
 */
public class CbJxMessenger extends BlockingMessenger {
    
    /**
     * Log4J Logger
     */
    private static transient final Logger LOG = Logger.getLogger(CbJxMessenger.class.getName());
    
    /**
     * the new destination address computed by the CbJx Endpoint
     * this address is of the form jxta://<peerID>/CbJxService/<peerGroupID>
     */
    private final EndpointAddress newDestAddr;
    
    /**
     *  A string which we can lock on while acquiring new messengers. We don't
     *  want to lock the whole object.
     **/
    private final Object acquireMessengerLock = new String( "Messenger Acquire Lock" );
    
    /**
     *  Cached messenger for sending to {@link newDestAddr}
     **/
    private Messenger outBoundMessenger = null;
    
    
    private final CbJxTransport transport;
    
    /**
     * constructor
     *
     * @param dest the destination address
     */
    public CbJxMessenger( CbJxTransport transport, EndpointAddress dest, Object hintIgnored ) throws IOException {
        this( transport, dest );
    }
    
    /**
     * constructor
     *
     * @param dest the destination address
     */
    public CbJxMessenger( CbJxTransport transport, EndpointAddress dest) throws IOException {

        // Do not use self destruction. There's nothing we have that can't just let be GC'ed
        super( transport.group.getPeerGroupID(), EndpointAddress.unmodifiableEndpointAddress( dest ), false );
        
        this.transport = transport;
        
        newDestAddr = EndpointAddress.unmodifiableEndpointAddress(
        new EndpointAddress( "jxta", dest.getProtocolAddress(), CbJxTransport.cbjxServiceName, null ) );
        
        outBoundMessenger = transport.endpoint.getMessenger(newDestAddr);
        
        if( null == outBoundMessenger ) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error( "Could not get messenger for " + newDestAddr );
            }
            
            throw new IOException( "Could not get messenger for " + newDestAddr );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void closeImpl() {
        // We do not use self destruction, so it is not impossible for the outBoundMessenger to become unreferenced without ever
        // being closed. No longer an issue: it is just a channel.
        super.close();
        
        synchronized( acquireMessengerLock ) {
            outBoundMessenger.close();
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public EndpointAddress getLogicalDestinationImpl() {
        return newDestAddr;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isClosed() {
        return super.isClosed();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isIdleImpl() {

        // XXX: Since we do not use self destruction, this is likely dead code.

        Messenger tmp = outBoundMessenger; // we don't get the lock.
        
        if( null != tmp )
            return tmp.isIdle();
        else
            return true;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean sendMessageBImpl( Message msg, String service, String serviceParam ) throws IOException {
        
        if ( isClosed() ) {
            IOException failure = new IOException( "Messenger was closed, it cannot be used to send messages." );
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info( failure );
            }
            
            throw failure;
        }
        
        msg = (Message) msg.clone();
        
        EndpointAddress destAddressToUse = getDestAddressToUse( service, serviceParam );
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Messenger: sending out " + msg + " to: " + destAddressToUse );
        }
        
        // add the cbjx info to the message
        msg = transport.addCryptoInfo( msg, destAddressToUse );
        
        // and sends out the message
        return sendTo( msg );
    }
    
    /**
     *  sendTo is used in order to send a message via the underlying messenger
     *
     *  @param message message to send to the remote peer.
     *  @return if true then message was sent, otherwise false.
     *  @throws IOException if there was a problem sending the message.
     **/
    boolean sendTo( Message msg ) throws IOException {
        
        synchronized( acquireMessengerLock ) {
            if( isClosed() ) {
                return false;
            }
            
            if( (null == outBoundMessenger) || outBoundMessenger.isClosed() ) {
                
                if ( LOG.isEnabledFor(Level.DEBUG) ) {
                    LOG.debug( "sendTo : Getting messenger for " + newDestAddr );
                }
                
                // Get a messenger.  FIXME - jice@jxta.org 20040413: This should absolutely never happen. We close the underlying
                // messenger only when this.closeImpl() is invoked. At which point we have no message to send. Okay, the
                // underlying messenger might have broken.  We should leave it broken. This is mostly the original
                // code. Transports should get a deeper retrofit eventually.
                outBoundMessenger = transport.endpoint.getMessenger(newDestAddr);
                
                if (outBoundMessenger == null) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("sendTo : could not get messenger for " + newDestAddr );
                    }
                    
                    // calling super.close() won't do. We must shoot an exception.
                    throw new IOException("sendTo : Underlying messenger could not be repaired");
                }
            }
        }
        
        if ( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "sendTo : Sending " + msg + " to endpoint " + newDestAddr );
        }
        
        // Good we have a messenger. Send the message.
        outBoundMessenger.sendMessageB( msg, null, null );
        return true;
    }
}

/*
 *  $Id: PipeServiceImpl.java,v 1.1 2007/01/16 11:01:58 thomas Exp $
 *
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *  must not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */
package net.jxta.impl.pipe;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownServiceException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.endpoint.Message;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.service.Service;

import net.jxta.impl.util.TimeUtils;

/**
 * A JXTA {@link net.jxta.pipe.PipeService} implementation which implements the
 * standard JXTA Pipe Resolver Protocol (PRP).
 *
 * <p/>This class provides implementation for Unicast, unicast secure and
 *  (indirectly) propagate pipes.
 *
 * @see net.jxta.pipe.PipeService
 * @see net.jxta.pipe.InputPipe
 * @see net.jxta.pipe.OutputPipe
 * @see net.jxta.endpoint.Message
 * @see net.jxta.protocol.PipeAdvertisement
 * @see net.jxta.protocol.PipeResolverMessage
 * @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#proto-pbp" target="_blank">JXTA Protocols Specification : Pipe Binding Protocol</a>
 **/
public class PipeServiceImpl implements PipeService, PipeResolver.Listener {
    
    /**
     *  The log4J Logger
     **/
    private final static Logger LOG = Logger.getLogger(PipeServiceImpl.class.getName());
    
    /**
     *  the interval at which we verify that a pipe is still resolved at a
     *  remote peer.
     **/
    static final long VERIFYINTERVAL = 20 * TimeUtils.AMINUTE;
    
    /**
     *  The group this PipeService is working for.
     **/
    private PeerGroup myGroup = null;
    
    /**
     *  Our resolver handler.
     **/
    private PipeResolver pipeResolver = null;
    
    /**
     *  Link to wire pipe impl.
     **/
    private WirePipeImpl wirePipe = null;
    
    /**
     *  the interface object we will hand out.
     **/
    private PipeService myInterface = null;
    
    /**
     *  the impl advertisement for this impl.
     **/
    private ModuleImplAdvertisement implAdvertisement = null;
    
    /**
     *  Table of listeners for asynchronous output pipe creation.
     *
     *  <p/><ul>
     *      <li>keys are {@link net.jxta.pipe.PipeID}</li>
     *      <li>values are {@link OutputPipeHolder}</li>
     *  </ul>
     **/
    private Map outputPipeListeners = new Hashtable();
    
    /**
     *  Has the pipe service been started?
     **/
    private volatile boolean started = false;
    
    /**
     *  holds a pipe adv and a listener which will be called for resolutions
     *  of the pipe.
     **/
    private static class OutputPipeHolder {
        PipeAdvertisement   adv;
        Set                 peers;
        OutputPipeListener  listener;
        
        OutputPipeHolder( PipeAdvertisement adv, Set peers, OutputPipeListener listener ) {
            this.adv = adv;
            this.peers = peers;
            this.listener = listener;
        }
    }
    
    /**
     * A listener useful for implementing synchronous behaviour.
     **/
    private static class syncListener implements OutputPipeListener {
        
        volatile OutputPipeEvent event = null;
        
        syncListener( ) {
        }
        
        /**
         * Called when a input pipe has been located for a previously registered
         * pipe. The event contains an {@link net.jxta.pipe.OutputPipe} which can
         * be used to communicate with the remote peer.
         *
         * @param outputPipeEvent event
         **/
        public synchronized void outputPipeEvent(OutputPipeEvent event) {
            
            // we only accept the first event.
            if( null == this.event) {
                this.event = event;
                notifyAll();
            }
        }
    }
    
    /**
     * Default Constructor (don't delete)
     **/
    public PipeServiceImpl() { }
    
    
    /**
     * {@inheritDoc}
     *
     * <p/>We create only a single interface object and return it over and over
     * again.
     */
    public synchronized Service getInterface() {
        if ( null == myInterface ) {
            myInterface = new PipeServiceInterface(this);
        }
        
        return myInterface;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized void init(PeerGroup pg, ID assignedID, Advertisement impl) {
        
        implAdvertisement = (ModuleImplAdvertisement) impl;
        myGroup = pg;
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer( "Configuring Pipe Service : " + assignedID );
            
            configInfo.append( "\n\tImplementation :" );
            configInfo.append( "\n\t\tModule Spec ID: " + implAdvertisement.getModuleSpecID());
            configInfo.append( "\n\t\tImpl Description : " + implAdvertisement.getDescription() );
            configInfo.append( "\n\t\tImpl URI : " + implAdvertisement.getUri() );
            configInfo.append( "\n\t\tImpl Code : " + implAdvertisement.getCode() );
            
            configInfo.append( "\n\tGroup Params :" );
            configInfo.append( "\n\t\tGroup : " + myGroup.getPeerGroupName() );
            configInfo.append( "\n\t\tGroup ID : " + myGroup.getPeerGroupID() );
            configInfo.append( "\n\t\tPeer ID : " + myGroup.getPeerID() );
            
            configInfo.append( "\n\tConfiguration :" );
            configInfo.append( "\n\t\tVerify Interval : " + VERIFYINTERVAL + "ms" );
            
            LOG.info( configInfo );
        }
    }
    
    /**
     *  {@inheritDoc}
     *
     * <p/>Currently this service does not expect arguments.
     **/
    public synchronized int startApp(String[] args) {
        
        Service needed = myGroup.getResolverService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a resolver service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        needed = myGroup.getMembershipService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a membership service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        needed = myGroup.getRendezVousService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a rendezvous service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        // create our resolver handler; it will register itself w/ the resolver.
        pipeResolver = new PipeResolver(myGroup);
        
        needed = myGroup.getRendezVousService();
        
        if( null == needed ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Stalled until there is a rendezvous service" );
            }
            
            return START_AGAIN_STALLED;
        }
        
        // Create the WirePipe (propagated pipe)
        wirePipe = new WirePipeImpl( myGroup, pipeResolver );
        wirePipe.startApp(args);
        
        started = true;
        return 0;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public synchronized void stopApp() {
        started = false;
        
        try {
            if (wirePipe != null) {
                wirePipe.stopApp();
            }
        } catch ( Throwable failed ) {
            LOG.error( "Failed to stop wire pipe", failed );
        } finally {
            wirePipe = null;
        }
        
        try {
            if (pipeResolver != null) {
                pipeResolver.stop();
            }
        } catch ( Throwable failed ) {
            LOG.error( "Failed to stop pipe resolver", failed );
        } finally {
            pipeResolver = null;
        }
        
        // Avoid cross-reference problem with GC
        myGroup = null;
        myInterface = null;
        
        outputPipeListeners.clear();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputPipe createInputPipe(PipeAdvertisement adv) throws IOException {
        return createInputPipe( adv, null );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public InputPipe createInputPipe(PipeAdvertisement adv, PipeMsgListener listener) throws IOException {
        
        if (!started) {
            throw new IllegalStateException("Pipe Service has not been started or has been stopped");
        }

        String type = adv.getType();
        if (type == null) {
            throw new IllegalArgumentException("PipeAdvertisement type may not be null");
        }

        PipeID pipeId = (PipeID) adv.getPipeID();
        if (pipeId == null) {
            throw new IllegalArgumentException("PipeAdvertisement PipeID may not be null");
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug( "Create " + type + " InputPipe for " + pipeId );
        }
        
        InputPipe ip = null;
        
        // create an InputPipe.
        if( type.equals(PipeService.UnicastType) ) {
            ip = new UnicastInputPipeImpl( pipeResolver, adv, listener);
        } else if( type.equals(PipeService.UnicastSecureType) ) {
            ip = new SecureInputPipeImpl( pipeResolver, adv, listener);
        } else if( type.equals(PipeService.PropagateType) ) {
            if ( wirePipe != null ) {
                ip = wirePipe.createInputPipe(adv, listener);
            } else {
                throw new IOException( "No propagated pipe servive available" );
            }
        } else {
            // Unknown type
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot create pipe for unknown type : " + type );
            }
            throw new IOException( "Cannot create pipe for unknown type : " + type );
        }
        
        return ip;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public OutputPipe createOutputPipe(PipeAdvertisement pipeAdv, long timeout) throws IOException {
        return createOutputPipe(pipeAdv, Collections.EMPTY_SET, timeout );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public OutputPipe createOutputPipe(PipeAdvertisement pipeAdv, PeerID peerid, long timeout) throws IOException {
        return createOutputPipe( pipeAdv, Collections.singleton(peerid), timeout );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public OutputPipe createOutputPipe(PipeAdvertisement adv, Enumeration resolvablePeers, long timeout ) throws IOException {
        
        Set peerSet = new HashSet();
        
        while( resolvablePeers.hasMoreElements() ) {
            peerSet.add( resolvablePeers.nextElement() );
        }
        
        if( peerSet.isEmpty() ) {
            throw new IllegalArgumentException("peers enumeration must not be empty");
        }
        
        return createOutputPipe( adv, Collections.unmodifiableSet(peerSet), timeout );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public OutputPipe createOutputPipe( PipeAdvertisement adv, Set resolvablePeers, long timeout ) throws IOException {
        // convert zero to max value.
        if( 0 == timeout ) {
            timeout = Long.MAX_VALUE;
        }
        
        // deprecated, but if negative convert to a fixed default.
        if( timeout < 0 ) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Negative resolve timeouts are deprecated. Using " + TimeUtils.ASECOND * 20 );
            }
            timeout = TimeUtils.ASECOND * 20;
        }
        
        long absoluteTimeOut = TimeUtils.toAbsoluteTimeMillis( timeout );
        
        // Make a listener, start async resolution and then wait until the timeout expires.
        syncListener localListener = new syncListener( );
        
        createOutputPipe( adv, resolvablePeers, localListener );
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Waiting synchronously for " + timeout + "ms to resolve OutputPipe for " + adv.getPipeID() );
        }
        
        try {
            synchronized( localListener ) {
                while( (null == localListener.event) &&
                (TimeUtils.toRelativeTimeMillis( TimeUtils.timeNow(), absoluteTimeOut ) < 0) ) {
                    try {
                        localListener.wait( TimeUtils.ASECOND );
                    } catch ( InterruptedException woken ) {
                        Thread.interrupted();
                    }
                }
            }
        } finally {
            // remove the listener we installed.
            removeOutputPipeListener( adv.getPipeID().toString(), localListener );
        }
        
        if( null != localListener.event ) {
            return localListener.event.getOutputPipe();
        } else {
            throw new IOException( "Output Pipe could not be resolved after " + timeout + "ms." );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void createOutputPipe(PipeAdvertisement pipeAdv, OutputPipeListener listener) throws IOException {
        createOutputPipe( pipeAdv, Collections.EMPTY_SET, listener );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void createOutputPipe( PipeAdvertisement pipeAdv, PeerID peerid, OutputPipeListener listener ) throws IOException {
        createOutputPipe( pipeAdv, Collections.singleton(peerid), listener );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void createOutputPipe( PipeAdvertisement pipeAdv, Set resolvablePeers, OutputPipeListener listener ) throws IOException {
        
        if ( !started ) {
            throw new IllegalStateException("Pipe Service has not been started or has been stopped");
        }
        
        // Recover the PipeId from the PipeServiceImpl Advertisement
        PipeID pipeId = (PipeID) pipeAdv.getPipeID();
        String type = pipeAdv.getType();
        
        if( null == type ) {
            IllegalArgumentException failed = new IllegalArgumentException("Pipe type was not set" );
            
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error( failed, failed );
            }
            
            throw failed;
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug( "Create " + type + " OutputPipe for " + pipeId );
        }
        
        if ( PipeService.PropagateType.equals(type) ) {
            OutputPipe op = null;
            if (wirePipe != null) {
                op = wirePipe.createOutputPipe( pipeAdv, resolvablePeers );
            } else {
                throw new IOException("No propagated pipe service available");
            }
            
            if( null != op ) {
                OutputPipeEvent newevent = new OutputPipeEvent(
                this.getInterface(), op, pipeId.toString(), PipeResolver.ANYQUERY );
                
                try {
                    listener.outputPipeEvent(newevent);
                } catch( Throwable ignored ) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Uncaught Throwable in listener for " + pipeId + " (" + listener.getClass().getName() + ")" , ignored );
                    }
                }
            }
            return;
        } else if( PipeService.UnicastType.equals( type ) || PipeService.UnicastSecureType.equals( type ) ) {
            outputPipeListeners.put( pipeId, new OutputPipeHolder( pipeAdv, resolvablePeers, listener ) );
            
            // need to create the listener first
            pipeResolver.addListener( pipeId, this, PipeResolver.ANYQUERY );
            
            int queryid = pipeResolver.sendPipeQuery( pipeAdv, resolvablePeers, PipeResolver.ANYQUERY );
            
            // look locally for the pipe
            if( resolvablePeers.isEmpty() || resolvablePeers.contains( myGroup.getPeerID() ) ) {
                InputPipe local = pipeResolver.findLocal( pipeId );
                
                // if we have a local instance, make sure the local instance is of the same type.
                if( null != local ) {
                    if ( local.getType().equals(pipeAdv.getType()) ) {
                        pipeResolver.callListener( queryid, pipeId, local.getType(), myGroup.getPeerID(), false  );
                    } else {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("rejecting local pipe (" + local.getType() + ") because type is not (" + pipeAdv.getType() + ")" );
                        }
                    }
                }
            }
        } else {
            // Unknown type
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("createOutputPipe: cannot create pipe for unknown type : " + type );
            }
            throw new IOException( "cannot create pipe for unknown type : " + type );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Message createMessage() {
        LOG.warn( "Obsoleted, call through the interface object if you want to use this API" );
        throw new UnsupportedOperationException( "Obsoleted, call through the interface object if you want to use this API" );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public OutputPipeListener removeOutputPipeListener( String opID, OutputPipeListener listener) {
        
        if (pipeResolver == null) {
            return null;
        }
        
        PipeID pipeID;
        try {
            URL aPipeID = IDFactory.jxtaURL(opID);
            pipeID = (PipeID) IDFactory.fromURL(aPipeID);
        } catch ( MalformedURLException badID ) {
            throw new IllegalArgumentException("Bad pipe ID: " + opID );
        } catch ( UnknownServiceException badID ) {
            throw new IllegalArgumentException("Unusable pipe ID: " + opID );
        } catch ( ClassCastException badID ) {
            throw new IllegalArgumentException("id was not a pipe id: " + opID );
        }
        
        synchronized ( outputPipeListeners ) {
            OutputPipeHolder pl = (OutputPipeHolder) outputPipeListeners.get( pipeID );
            
            if( (null != pl) && (pl.listener == listener) ) {
                
                pipeResolver.removeListener( pipeID, PipeResolver.ANYQUERY );
                outputPipeListeners.remove( pipeID );
                
                return listener;
            }
        }
        return null;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean pipeResolveEvent( PipeResolver.Event e ) {
        try {
            OutputPipeHolder pl = (OutputPipeHolder) outputPipeListeners.get(e.getPipeID());
            
            PeerID peerID = e.getPeerID();
            
            if ( null != pl ) {
                // check if they wanted a resolve from a specific peer.
                if( !pl.peers.isEmpty() && !pl.peers.contains(peerID) ) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn( "Event was for wrong peer '" + peerID + "'. Discarding." );
                    }
                    return false;
                }
                
                // create op
                String type = pl.adv.getType();
                OutputPipe op = null;
                if ( PipeService.UnicastType.equals(type) ) {
                    op = new NonBlockingOutputPipe(myGroup, pipeResolver, pl.adv, peerID, pl.peers );
                } else if ( PipeService.UnicastSecureType.equals(type) ) {
                    op = new SecureOutputPipe(myGroup, pipeResolver, pl.adv, peerID, pl.peers );
                } else {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn( "Could not create output pipe of type '" + type + "'. Discarding." );
                    }
                    return false;
                }
                
                // Generate an event when the output pipe was succesfully opened.
                OutputPipeEvent newevent = new OutputPipeEvent(
                this.getInterface(), op, e.getPipeID().toString(), e.getQueryID() );
                
                try {
                    pl.listener.outputPipeEvent(newevent);
                } catch( Throwable ignored ) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Uncaught Throwable in listener for " + e.getPipeID() + "(" + pl.getClass().getName() + ")" , ignored );
                    }
                }
                
                return true;
            } else {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug( "No listener for event for " + e.getPipeID() );
                }
            }
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Error creating output pipe " + e.getPipeID(), ie );
            }
        }
        
        return false;
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>We don't do anything with NAKs (yet)
     **/
    public boolean pipeNAKEvent( PipeResolver.Event e ) {
        return false;
    }
}

/************************************************************************
 *
 * $Id: RdvWalk.java,v 1.1 2007/01/16 11:02:01 thomas Exp $
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
 *********************************************************************************/

package net.jxta.impl.rendezvous;


import net.jxta.peergroup.PeerGroup;
import net.jxta.impl.rendezvous.rpv.PeerView;


/**
 * This abstract class must be extended by all rendezvous peer walking
 * policies. A Walk policy implements a particular protocol/behavior
 * for sending messages through the Rendezvous Peers. A rendezvous peer
 * is responsible for instantiating a RdvWalk object, and starting a Greeter.
 * Other peers, that want to send messages using a walk, are reponsible for
 * instantiating a RdvWalk object, and getting a walker.
 *
 * Each walk is associated to a service name and a service param. Those
 * are the name and optional parameter are those of the service that uses
 * the RdvWalk.
 *
 * @see net.jxta.impl.rendezvous.limited.LimitedRangeWalk
 *
 */
public abstract class RdvWalk {
    
    protected PeerView rpv = null;
    protected PeerGroup group = null;
    protected final String serviceName;
    protected final String serviceParam;
    
    /**
     ** Standard constructor
     **
     ** @param group peergroup in which this walk is running
     ** @param serviceName name used by the service (client) of this walk.
     ** @param serviceParam optional parameter used by the service (client) of this walk.
     ** @param rpv the rendezvous peer PeerView to be used by this walk.
     **/
    public RdvWalk(PeerGroup group,
            String serviceName,
            String serviceParam,
            PeerView  rpv) {
        
        this.group = group;
        this.rpv = rpv;
        this.serviceName = serviceName;
        this.serviceParam = serviceParam;
    }
    
    /**
     ** Get/Create a walker to be used with this walk.
     **
     ** @return RdvWalker a walker to be used with this walk. null is returned if
     ** no walker can be used.
     **/
    public RdvWalker  getWalker() {
        return null;
    }
    
    /**
     ** Get/Create a greeter to be used with this walk.
     **
     ** @return RdvGreeter a greeter to be used with this walk. null is returned if
     ** no greeter can be used.
     **/
    public RdvGreeter getGreeter() {
        return null;
    }
    
    /**
     ** Return the Rendezvous peer PeerView used by this walk.
     **
     ** @return PeerView the rendezvous peer PeerView used by this walk.
     **/
    public PeerView getPeerView() {
        return rpv;
    }
    
    /**
     ** Return the Service Name associated with this walk
     **
     ** @return String the Service Name associated with this walk
     **/
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     ** Return the Service Param associated with this walk
     **
     ** @return String the Service Param associated with this walk
     **/
    public String getServiceParam() {
        return serviceParam;
    }
    
    /**
     ** Stop this walk.
     **/
    public synchronized void stop() {
        group = null;
        rpv = null;
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected void finalize() throws Throwable {
        stop();
    }
}


/*
 * Copyright (c) 2001-2003 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: Platform.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;

import net.jxta.exception.ConfiguratorException;
import net.jxta.exception.JxtaError;
import net.jxta.exception.PeerGroupException;

import net.jxta.impl.endpoint.cbjx.CbJxDefs;
import net.jxta.impl.membership.PasswdMembershipService;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.protocol.PlatformConfig;

/**
 * Provides the implementation for the World Peer Group.
 *
 * <p/>Key differences from regular groups are:
 *
 * <ul>
 * <li>Provides a mechanism for peer group configuration parameter and for
 * reconfiguration via a plugin configurator.</li>
 * <li>Ensures that only a single instance of the World Peer Group exists
 * within the context of the current classloader.</li>
 * </ul>
 **/
public class Platform extends StdPeerGroup {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(Platform.class.getName());
    
    /**
     *  If <code>true</code> then initialization has been completed. Declared
     *  <code>static</code> to ensure that only one instance exists.
     **/
    private static boolean initialized = false;
    
    /**
     *  The Module Impl Advertisement we will return in response to
     *  {@link #getAllPurposePeerGroupImplAdvertisement()} requests.
     **/
    private ModuleImplAdvertisement allPurposeImplAdv = null;
    
    /**
     *  Default constructor
     **/
    public Platform() {
        Class c = PeerGroupFactory.getConfiguratorClass();
        
        try {
            configurator = (PlatformConfigurator) c.newInstance();
        } catch (InstantiationException ie) {
            LOG.error("Uninstantiatable configurator: " + c, ie );
            
            throw new JxtaError("Uninstantiatable configurator: " + c, ie );
        } catch (IllegalAccessException iae) {
            LOG.error("can't instantiate configurator: " + c, iae );
            
            throw new JxtaError("Can't instantiate configurator: " + c, iae );
        } catch (ClassCastException cce ) {
            LOG.error("Not a PlatformConfigurator:" + c, cce );
            
            throw new JxtaError("Not a PlatformConfigurator:" + c, cce );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected synchronized void initFirst( PeerGroup nullParent, ID assignedID, Advertisement impl)
    throws PeerGroupException {
        
        if ( initialized ) {
            LOG.fatal("You cannot initialize more than one World PeerGroup!");
            throw new PeerGroupException("You cannot initialize more than one World PeerGroup!");
        }
        
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) impl;
        
        // XXX 20040922 bondolo why is this not set in HTTP Client?
        try {
            try {
                String to = System.getProperty("sun.net.client.defaultConnectTimeout");
                if (to == null)
                    to = "notset";
                
                Integer.parseInt(to);
                
            } catch(NumberFormatException nfe) {
                System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
            }
        } catch( java.lang.SecurityException disallowed ) {
            if (LOG.isEnabledFor(Level.WARN))
                LOG.warn("Could not get/set property : " + "sun.net.client.defaultConnectTimeout" );
        }
        
        if (nullParent != null) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("World PeerGroup cannot be instantiated with a parent group!");
            }
            
            throw new PeerGroupException( "World PeerGroup cannot be instantiated with a parent group!" );
        }
        
        // if we weren't given a module impl adv then make one from scratch.
        if( null == implAdv ) {
            try {
                // Build the platform's impl adv.
                implAdv = mkPlatformImplAdv();
            } catch (Throwable e) {
                if (LOG.isEnabledFor(Level.FATAL))
                    LOG.fatal( "Fatal Error making Platform Impl Adv", e);
                throw new PeerGroupException(  "Fatal Error making Platform Impl Adv", e );
            }
        }
        
        try {
            setConfigAdvertisement(((PlatformConfigurator)configurator).getPlatformConfig());
            
            // Invalidate the config before init, just in case something screws up, in
            // which case we want the configurator to show-up by default.
            ((PlatformConfigurator)configurator).setReconfigure(true);
            
            // Initialize the group.
            super.initFirst( null, PeerGroupID.worldPeerGroupID, implAdv );
            
            // Save the config after init
            ((PlatformConfigurator)configurator).setPlatformConfig( (PlatformConfig) getConfigAdvertisement());
            
            configurator.save();
            
            // Validate the config after init
            ((PlatformConfigurator)configurator).setReconfigure(false);
        } catch( ConfiguratorException failed ) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal( "Peer Group configuration failed!", failed);
            }
            
            throw new PeerGroupException( "Peer Group configuration failed!", failed );
        }
        
        // Publish our own adv.
        try {
            publishGroup("World PeerGroup", "Standard World PeerGroup Reference Implementation");
        } catch (IOException e) {
            throw new PeerGroupException( "Failed to publish World Peer Group Advertisement", e );
        }
    }
    
    /**
     * {@inheritDoc}
     **/
    protected synchronized void initLast() throws PeerGroupException {
        // Nothing special for now, but we might want to move some steps
        // from initFirst, in the future.
        super.initLast();
        
        initialized = true;
        
        // XXX bondolo 20040415 Hack to initialize class loader with specID for password membership
        // This is to provide compatibility with apps which imported passwd membership directly.
        ModuleSpecID id = PasswdMembershipService.passwordMembershipSpecID;
    }
    
    protected static ModuleImplAdvertisement mkPlatformImplAdv() throws Exception {
        
        // Start building the implAdv for the platform intself.
        ModuleImplAdvertisement platformDef =
        mkImplAdvBuiltin(PeerGroup.refPlatformSpecID,
        "World PeerGroup",
        "Standard World PeerGroup Reference Implementation" );
        
        // Build the param section now.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();
        Hashtable protos = new Hashtable();
        Hashtable services = new Hashtable();
        Hashtable apps = new Hashtable();
        
        // Build ModuleImplAdvs for each of the modules
        ModuleImplAdvertisement moduleAdv;
        
        // Do the Services
        
        // "Core" Services
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refEndpointSpecID,
        "net.jxta.impl.endpoint.EndpointServiceImpl",
        "Reference Implementation of the Endpoint service");
        services.put(PeerGroup.endpointClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refResolverSpecID,
        "net.jxta.impl.resolver.ResolverServiceImpl",
        "Reference Implementation of the Resolver service");
        services.put(PeerGroup.resolverClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin( PeerGroup.refMembershipSpecID,
        "net.jxta.impl.membership.none.NoneMembershipService",
        "Reference Implementation of the None Membership Service");
        services.put( PeerGroup.membershipClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refAccessSpecID,
        "net.jxta.impl.access.always.AlwaysAccessService",
        "Always Access Service");
        services.put(PeerGroup.accessClassID, moduleAdv);
        
        // "Standard" Services
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID,
        "net.jxta.impl.discovery.DiscoveryServiceImpl",
        "Reference Implementation of the Discovery service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID,
        "net.jxta.impl.rendezvous.RendezVousServiceImpl",
        "Reference Implementation of the Rendezvous Service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID,
        "net.jxta.impl.peer.PeerInfoServiceImpl",
        "Reference Implementation of the Peerinfo Service" );
        services.put( PeerGroup.peerinfoClassID, moduleAdv );
        
        // Do the protocols
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refTcpProtoSpecID,
        "net.jxta.impl.endpoint.tcp.TcpTransport",
        "Reference Implementation of the TCP Message Transport");
        protos.put( PeerGroup.tcpProtoClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refHttpProtoSpecID,
        "net.jxta.impl.endpoint.servlethttp.ServletHttpTransport",
        "Reference Implementation of the HTTP Message Transport");
        protos.put( PeerGroup.httpProtoClassID, moduleAdv);
        
        // Do the Apps
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refStartNetPeerGroupSpecID,
        "net.jxta.impl.peergroup.StartNetPeerGroup",
        "Start Net Peer Group");
        apps.put(applicationClassID, moduleAdv);
        
        paramAdv.setServices(services);
        paramAdv.setProtos(protos);
        paramAdv.setApps(apps);
        
        // Pour the paramAdv in the platformDef
        platformDef.setParam((XMLDocument) paramAdv.getDocument(MimeMediaType.XMLUTF8));
        
        return platformDef;
    }
    
    /**
     * {@inheritDoc}
     */
    public void stopApp() {
        super.stopApp();
        
        initialized = false;
    }
    
    /**
     * Returns the all purpose peer group implementation advertisement that
     * is most usefull when called in the context of the platform group: the
     * description of an infrastructure group.
     *
     * <p/>This definition is always the same and has a well known ModuleSpecID.
     * It includes the basic service, high-level transports and the shell for
     * main application. It differs from the one returned by StdPeerGroup only
     * in that it includes the high-level transports (and different specID,
     * name and description, of course). However, in order to avoid confusing
     * inheritance schemes (class hierarchy is inverse of object hierarchy)
     * other possible dependency issues, we just redefine it fully, right here.
     *
     * The user must remember to change the specID if the set of services
     * protocols or applications is altered before use.
     *
     * @return ModuleImplAdvertisement The new peergroup impl adv.
     */
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
        
        // Build it only the first time; then clone it.
        if (allPurposeImplAdv != null)
            return (ModuleImplAdvertisement) allPurposeImplAdv.clone();
        
        // Make a new impl adv
        // For now, use the well know NPG naming, it is not
        // identical to the allPurpose PG because we use the class
        // ShadowPeerGroup which initializes the peer config from its
        // parent.
        ModuleImplAdvertisement implAdv =
        mkImplAdvBuiltin( PeerGroup.refNetPeerGroupSpecID,
        ShadowPeerGroup.class.getName(),
        "Default NetPeerGroup reference implementation.");
        
        XMLElement paramElement = (XMLElement) implAdv.getParam();
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv( );
        ModuleImplAdvertisement moduleAdv;
        
        // set the services
        Hashtable services = new Hashtable();
        
        // "Core" Services
        
        moduleAdv =
        mkImplAdvBuiltin( PeerGroup.refEndpointSpecID,
        "net.jxta.impl.endpoint.EndpointServiceImpl",
        "Reference Implementation of the Endpoint Service");
        services.put(PeerGroup.endpointClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin( PeerGroup.refResolverSpecID,
        "net.jxta.impl.resolver.ResolverServiceImpl",
        "Reference Implementation of the Resolver Service");
        services.put(PeerGroup.resolverClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin( PSEMembershipService.pseMembershipSpecID,
        "net.jxta.impl.membership.pse.PSEMembershipService",
        "Reference Implementation of the PSE Membership Service");
        services.put(PeerGroup.membershipClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin( PeerGroup.refAccessSpecID,
        "net.jxta.impl.access.always.AlwaysAccessService",
        "Always Access Service");
        services.put(PeerGroup.accessClassID, moduleAdv);
        
        // "Standard" Services
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID,
        "net.jxta.impl.discovery.DiscoveryServiceImpl",
        "Reference Implementation of the Discovery Service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID,
        "net.jxta.impl.rendezvous.RendezVousServiceImpl",
        "Reference Implementation of the Rendezvous Service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refPipeSpecID,
        "net.jxta.impl.pipe.PipeServiceImpl",
        "Reference Implementation of the Pipe Service" );
        services.put(PeerGroup.pipeClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID,
        "net.jxta.impl.peer.PeerInfoServiceImpl",
        "Reference Implementation of the Peerinfo Service" );
        services.put( PeerGroup.peerinfoClassID, moduleAdv );
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refProxySpecID,
        "net.jxta.impl.proxy.ProxyService",
        "Reference Implementation of the JXME Proxy Service");
        services.put(PeerGroup.proxyClassID, moduleAdv);
        
        paramAdv.setServices(services);
        
        // High-level Transports.
        Hashtable protos = new Hashtable();
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refRouterProtoSpecID,
        "net.jxta.impl.endpoint.router.EndpointRouter",
        "Reference Implementation of the Router Message Transport");
        protos.put(PeerGroup.routerProtoClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refTlsProtoSpecID,
        "net.jxta.impl.endpoint.tls.TlsTransport",
        "Reference Implementation of the JXTA TLS Message Transport");
        protos.put(PeerGroup.tlsProtoClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin( CbJxDefs.cbjxMsgTransportSpecID,
        "net.jxta.impl.endpoint.cbjx.CbJxTransport",
        "Reference Implementation of the JXTA Cryptobased-ID Message Transport");
        protos.put( CbJxDefs.msgtptClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refRelayProtoSpecID,
        "net.jxta.impl.endpoint.relay.RelayTransport",
        "Reference Implementation of the Relay Message Transport");
        protos.put(PeerGroup.relayProtoClassID, moduleAdv);
        
        paramAdv.setProtos(protos);
        
        // Main app is the shell
        // Build a ModuleImplAdv for the shell
        ModuleImplAdvertisement newAppAdv = (ModuleImplAdvertisement)
        AdvertisementFactory.newAdvertisement(
        ModuleImplAdvertisement.getAdvertisementType());
        
        // The shell's spec id is a canned one.
        newAppAdv.setModuleSpecID(PeerGroup.refShellSpecID);
        
        // Same compat than the group.
        newAppAdv.setCompat(implAdv.getCompat());
        newAppAdv.setUri(implAdv.getUri());
        newAppAdv.setProvider(implAdv.getProvider());
        
        // Make up a description
        newAppAdv.setDescription("JXTA Shell");
        
        // Tack in the class name
        newAppAdv.setCode( "net.jxta.impl.shell.bin.Shell.Shell" );
        
        // Put that in a new table of Apps and replace the entry in
        // paramAdv
        Hashtable newApps = new Hashtable();
        newApps.put(PeerGroup.applicationClassID, newAppAdv);
        paramAdv.setApps(newApps);
        
        // Pour our newParamAdv in implAdv
        paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
        
        implAdv.setParam(paramElement);
        
        allPurposeImplAdv = implAdv;
        
        return implAdv;
    }
}

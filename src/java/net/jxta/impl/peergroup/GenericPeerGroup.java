/*
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: GenericPeerGroup.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Map;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.NoSuchElementException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.access.AccessService;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointService;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.peergroup.Configurator;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.resolver.ResolverService;
import net.jxta.service.Service;

import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.exception.ViolationException;

import net.jxta.impl.loader.RefJxtaLoader;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.PSEConfigAdv;

/**
 *  Provides common services for most peer group implementations.
 **/
public abstract class GenericPeerGroup implements PeerGroup {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(GenericPeerGroup.class.getName());
    
    /**
     * The loader - use the getter and setter for modifying the classloader for a security manager
     **/
    
    private static JxtaLoader loader;
    
    static {
        try {
            String localPath = "file://" + net.jxta.impl.config.Config.JXTA_HOME + "Downloaded";
            URL fileURL = new URL( localPath );
            
            loader = new RefJxtaLoader( new URL[] {fileURL },
            new CompatibilityEquater() {
                public boolean compatible( Element test ) {
                    return StdPeerGroup.isCompatible( test );
                }
            } );
        } catch( MalformedURLException bad ) {
            LOG.fatal( "Could not initialize JXTA class loader", bad );
        }
    }
    
    /*
     * Shortcuts to well known services.
     */
    private EndpointService endpoint;
    private ResolverService resolver;
    private DiscoveryService discovery;
    private PipeService pipe;
    private MembershipService membership;
    private RendezVousService rendezvous;
    private PeerInfoService peerinfo;
    private AccessService access;
    
    /**
     * This peer's advertisement in this group.
     **/
    private PeerAdvertisement peerAdvertisement = null;
    
    /**
     * This group's advertisement.
     **/
    private PeerGroupAdvertisement peerGroupAdvertisement = null;
    
    /**
     *This group's implAdvertisement.
     **/
    private ModuleImplAdvertisement implAdvertisement = null;
    
    
    /**
     *  The configurator for this peer group.
     **/
    protected Configurator configurator = null;
    
    /**
     * This peer's config advertisement.
     **/
    protected PlatformConfig configAdvertisement = null;
    
    /**
     * This service implements a group but, being a Service, it
     * runs inside of some group. That's its home group.
     *
     * <p/>Exception: The platform itself does not have one. It has to be self
     * sufficient.
     **/
    protected PeerGroup parentGroup = null;
    
    /**
     * All the plug-ins that do the work.
     *
     * <p/><ul>
     *      <li>Keys are {@link net.jxta.platform.ModuleClassID}.</li>
     *      <li>Values are {@link net.jxta.service.Service}.</li>
     *  </ul>
     **/
    private Hashtable services = new Hashtable();
    
    /**
     * True when we have decided to stop this group.
     **/
    private volatile boolean stopping = false;
    
    /**
     * True when the PG adv has been published.
     **/
    private boolean published = false; // assume it hasn't
    
    /**
     * Counts the number of times an interface to this group has been given out.
     * This is decremented everytime an interface object is GCed or
     * its owner calls unref().
     *
     * <p/>When it reaches zero, if it is time to tear-down the group instance;
     * nomatter what the GC thinks. There are threads that need to be stopped
     * before the group instance object ever becomes un-reachable.
     **/
    private int masterRefCount = 0;
    
    /**
     * Is true when at least one interface object has been generated AFTER
     * initComplete has become true. If true, the group stops when its ref
     * count reaches zero.
     **/
    private boolean stopWhenUnreferenced = false;
    
    /**
     * Is set to true when init is completed enough that it makes sense
     * to perform ref-counting.
     **/
    private volatile boolean initComplete = false;
    
    /**
     *  The thread group in which threads created by this group or services of
     *  this group should live.
     **/
    private ThreadGroup threadGroup = null;
    
    /**
     * Get a modified version of the Jxta ClassLoader
     **/
    public static JxtaLoader getJxtaLoader() {
        return loader;
    }
    
    /**
     * Set a modified version of the Jxta ClassLoader
     **/
    public static void setJxtaLoader(JxtaLoader newLoader) {
        loader = newLoader;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals( Object target ) {
        
        if( !(target instanceof PeerGroup)) {
            return false;
        }
               
        PeerGroup targetAsPeerGroup = (PeerGroup) target;
        
        // both null or both non-null.
        if((null == parentGroup) && (null != targetAsPeerGroup.getParentGroup())) {
            return false;
        }
        
        if((null != parentGroup) && (null == targetAsPeerGroup.getParentGroup())) {
            return false;
        }
        
        if( (null != parentGroup) && !parentGroup.equals(targetAsPeerGroup.getParentGroup()) ) {
            return false;
        }
        
        // and same peer ids.
        return getPeerGroupID().equals(targetAsPeerGroup.getPeerGroupID());
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode() {
        // before init we must fail.
        if( (null == peerAdvertisement) || (null == getPeerGroupID()) ) {
            throw new IllegalStateException( "PeerGroup not sufficiently initialized" );
        }
        
        // XXX 20050907 bondolo including parentGroup would improve the hash.
        return getPeerGroupID().hashCode();
    }
    
    
    /**
     *  {@inheritDoc}
     *  
     *  <p/>An implementation suitable for debugging. <b>Don't try to parse 
     *  this string!</b> All of the information is available from other sources.
     **/
     public String toString() {
        if( null == getPeerGroupID() ) {
            return super.toString();
        }
        
        StringBuffer result = new StringBuffer();
        
        result.append( getPeerGroupID().toString() );
        String peerGroupName = peerGroupAdvertisement.getName();
        if( null != peerGroupName) {
            result.append( " \"" );
            result.append( peerGroupName );
            result.append( '\"' );
        }
        
        if( null != parentGroup ) {
            result.append( " / " );
            result.append( parentGroup.toString() );
        }
        
        return result.toString();
     }
    
    /**
     * Returns the Thread Group in which threads for this peer group will live.
     *
     * @return ThreadGroup
     **/
    public ThreadGroup getHomeThreadGroup() {
        return threadGroup;
    }
    
    // Just a small wrapper around two common idioms that we do use here and
    // there.
    private Enumeration discoverSome(int type,
    String attr,
    String value,
    int seconds,
    Class thisClass) {
        return discoverSome(discovery, type, attr, value, seconds, thisClass);
    }
    
    private Enumeration discoverSome(DiscoveryService discovery,
    int type,
    String attr,
    String value,
    int seconds,
    Class thisClass) {
        
        Vector results = new Vector();
        
        try {
            int count = 0;
            
            Enumeration res;
            
            do {
                res = discovery.getLocalAdvertisements(type, attr, value);
                while (res.hasMoreElements()) {
                    Advertisement a = (Advertisement) res.nextElement();
                    
                    if (thisClass.isInstance(a)) {
                        results.add(a);
                    }
                }
                
                if (results.size() > 0) {
                    break;
                }
                
                if (count % 30 == 0) {
                    discovery.getRemoteAdvertisements(null, type, attr, value, 20);
                }
                
                Thread.sleep(1000);
            } while (count++ < seconds);
        } catch (Exception whatever) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Failure during discovery : ", whatever);
            }
        }
        return results.elements();
    }
    
    private Advertisement discoverOne(int type, String attr,
    String value, int seconds,
    Class thisClass) {
        
        Enumeration res = discoverSome(type, attr, value, seconds, thisClass);
        
        if (!res.hasMoreElements()) {
            return null;
        }
        return (Advertisement) res.nextElement();
    }
    
    /*
     * Shortcuts to the standard basic services.
     */
    
    private void setShortCut(ID name, Service service) {
        if (endpointClassID.equals(name)) {
            endpoint = (EndpointService) service;
            return;
        }
        if (resolverClassID.equals(name)) {
            resolver = (ResolverService) service;
            return;
        }
        if (discoveryClassID.equals(name)) {
            discovery = (DiscoveryService) service;
            return;
        }
        if (pipeClassID.equals(name)) {
            pipe = (PipeService) service;
            return;
        }
        if (membershipClassID.equals(name)) {
            membership = (MembershipService) service;
            return;
        }
        if (peerinfoClassID.equals(name)) {
            peerinfo = (PeerInfoService) service;
            return;
        }
        if (rendezvousClassID.equals(name)) {
            rendezvous = (RendezVousService) service;
            return;
        }
        if (accessClassID.equals(name)) {
            access = (AccessService) service;
            return;
        }
    }
    
    private void clearShortCut(ModuleClassID name) {
        if (endpointClassID.equals(name)) {
            endpoint = null;
            return;
        }
        if (resolverClassID.equals(name)) {
            resolver = null;
            return;
        }
        if (discoveryClassID.equals(name)) {
            discovery = null;
            return;
        }
        if (pipeClassID.equals(name)) {
            pipe = null;
            return;
        }
        if (membershipClassID.equals(name)) {
            membership = null;
            return;
        }
        if (peerinfoClassID.equals(name)) {
            peerinfo = null;
            return;
        }
        if (rendezvousClassID.equals(name)) {
            rendezvous = null;
            return;
        }
        if (accessClassID.equals(name)) {
            access = null;
            return;
        }
    }
    
    /**
     * Adds a service to the set.
     * Returns any pre-existing one.
     **/
    private synchronized Service addServiceSync(ID name,
    Service service) {
        if (stopping) {
            return null;
        }
        Service p = (Service) services.remove(name);
        
        services.put(name, service);
        return p;
    }
    
    /**
     * Adds a service to the set.
     *
     * <p/>Removes any pre-existing one with the same name.
     **/
    protected void addService(ID name, Service service) {
        Service oldp = addServiceSync(name, service);
        
        setShortCut(name, service);
        
        if ((oldp != null) && (oldp != service)) {
            LOG.warn("Service replaced for " + name);
            // oldp.stopApp(); maybe ?
        }
    }
    
    /**
     * Call a service by name.
     *
     * @param name the service name
     * @return Service, the Service registered by that name
     **/
    synchronized public Service lookupService(ID name) throws ServiceNotFoundException {
        // Null services are never registered, so we do not need to test that
        // case.
        Service p = (Service) services.get(name);
        
        if (p == null) {
            throw new ServiceNotFoundException(name.toString());
        }
        return p.getInterface();
    }
    
    /**
     * {@inheritDoc}
     * Group implementations do not have to support mapping.
     * it would be nice to separate better Interfaces, so that
     * Interface Objects can do things that the real service does
     * not have to implement.
     */
    public Service lookupService(ID name, int roleIndex)
    throws ServiceNotFoundException {
        
        // If the role number is != 0, it can't be honored: we
        // do not have an explicit map.
        
        if (roleIndex != 0) {
            throw (new ServiceNotFoundException(
            "" + name + "[" + roleIndex + "]"));
            
        }
        return lookupService(name);
    }
    
    /**
     * {@inheritDoc}
     **/
    public Iterator getRoleMap(ID name) {
        // No translation; use the given name in a singleton.
        return Collections.singletonList(name).iterator();
    }
    
    /**
     * check that all required services are registered
     **/
    protected void checkServices() throws ServiceNotFoundException {
        Service ignored;
        
        ignored = lookupService(endpointClassID);
        ignored = lookupService(resolverClassID);
        ignored = lookupService(membershipClassID);
        ignored = lookupService(accessClassID);
        
        /**
         * ignored = lookupService(discoveryClassID);
         * ignored = lookupService(pipeClassID);
         * ignored = lookupService(rendezvousClassID);
         * ignored = lookupService(peerinfoClassID);
         **/
    }
    
    /**
     * We're stopping. Remove all services and stop them.
     **/
    private synchronized void removeAllServicesSync() {
        
        stopping = true;
        
        // stop everything
        Iterator allServices = services.entrySet().iterator();
        
        while (allServices.hasNext()) {
            Map.Entry s = null;
            
            try {
                s = (Map.Entry) allServices.next();
                ((Service) s.getValue()).stopApp();
            } catch (Throwable e) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed stopping service: " + s, e);
                }
            }
        }
        
        // remove everything
        allServices = services.entrySet().iterator();
        
        while (allServices.hasNext()) {
            Map.Entry s = null;
            
            try {
                s = (Map.Entry) allServices.next();
                clearShortCut(((ModuleClassID) s.getKey()));
                allServices.remove();
            } catch (Throwable e) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed remove service: " + s, e);
                }
            }
        }
    }
    
    /**
     * Ask a group to unregister and unload a service
     *
     * @param service handle to the service to be removed
     **/
    synchronized private void removeServiceSync(ModuleClassID name,
    Service service)
    throws ServiceNotFoundException, ViolationException {
        // Weak check: you need to actually posess a handle to the
        // service object you're removing, not its name, but
        // you can fake by doing getService() first.
        
        Service p = (Service) services.get(name);
        
        if (p == null) {
            throw new ServiceNotFoundException(name.toString());
        }
        if (p != service) {
            throw new ViolationException("Service registered for key was did not match provided instance :" + name);
        }
        
        services.remove(name);
    }
    
    /**
     * Ask a group to unregister and unload a service
     *
     * @param service handle to the service to be removed
     **/
    protected void removeService(ModuleClassID name, Service service)
    throws ServiceNotFoundException, ViolationException {
        removeServiceSync(name, service);
        clearShortCut(name);
        // service.finalize(); FIXME. We probably need a terminate()
        // method instead.
        // FIXME: [jice@jxta.org 20011013] to make sure the service is
        // no-longer referenced, we should always return interfaces, and
        // have a way to cut the reference to the real service in the
        // interfaces. One way of doing that would be to have to levels
        // of indirection: we should keep one and return references to it.
        // when we want to cut the service loose, we should clear the
        // reference from the interface that we own before letting it go.
        // We need to study the consequences of doing that before implementing
        // it.
    }
    
    /**
     * Evaluates if the given compatibility statement makes the module that
     * bears it is lodable by this group.
     *
     * @return boolean True if the given statement is compatible.
     **/
    abstract public boolean compatible(Element compat);
    
    /**
     * Load a module from a ModuleImplAdv.
     * Compatibility is checked and load is attempted. If compatible and loaded
     * successfuly, the resulting Module is init()ed and returned.
     **/
    public Module loadModule(ID assigned, Advertisement impl)
    throws ProtocolNotSupportedException, PeerGroupException {
        return loadModule(assigned, (ModuleImplAdvertisement) impl, false);
    }
    
    /**
     * Internal version, allows one to make a privileged module
     * (has a ref to the true group obj instead of just an interface.
     * That's for group modules and main apps normally).
     **/
    protected Module loadModule( ID assigned, ModuleImplAdvertisement implAdv, boolean privileged )
    throws ProtocolNotSupportedException, PeerGroupException {
        
        Element compat = implAdv.getCompat();
        
        if (null == compat) {
            throw new IllegalArgumentException("No compatibility statement for : " + assigned);
        }
        
        if (!compatible(compat)) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Incompatible Module : " + assigned) ;
            }
            
            throw new ProtocolNotSupportedException( "Incompatible Module : " + assigned );
        }
        
        Module newMod = null;
        
        if( (null != implAdv.getCode()) && (null != implAdv.getUri()) ) {
            try {
                // Good one. Try it.
                
                Class clazz;
                
                try {
                    clazz = loader.findClass( implAdv.getModuleSpecID() );
                } catch ( ClassNotFoundException notLoaded ) {
                    clazz = loader.defineClass( implAdv );
                }
                
                if( null == clazz ) {
                    throw new ClassNotFoundException( "Cannot load class (" + implAdv.getCode() + ") : " + assigned );
                }
                
                newMod = (Module) clazz.newInstance();
                
                newMod.init(privileged ? this : (PeerGroup) getInterface(), assigned, implAdv);
                
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Loaded" + (privileged ? " privileged" : "") + " module : " + implAdv.getDescription() + " (" + implAdv.getCode() + ")" );
                }
            } catch (RuntimeException ex) {
                try {
                    newMod.stopApp();
                } catch (Throwable t) {
                    // If this does not work, nothing needs to be done.
                }
                throw ex;
            } catch (PeerGroupException ex) {
                try {
                    newMod.stopApp();
                } catch (Throwable t) {
                    // If this does not work, nothing needs to be done.
                }
                throw ex;
            } catch (Throwable ex) {
                try {
                    newMod.stopApp();
                } catch (Throwable t) {
                    // If this does not work, nothing needs to be done.
                }
                throw new PeerGroupException("Could not load module for : " + assigned + " ("+ implAdv.getDescription() + ")", ex );
            }
            
        } else {
            throw new PeerGroupException( "Cannot load class for : " + assigned );
        }
        
        // Publish or renew the lease of this adv since we're using it.
        try {
            if (discovery != null) {
                discovery.publish( implAdv, DEFAULT_LIFETIME, DEFAULT_EXPIRATION );
            }
        } catch (Exception ignored) {;}
        
        // If we reached this point we're done.
        return newMod;
    }
    
    /**
     * Load a module from a spec id.
     * Advertisement is sought, compatibility is checked on all candidates
     * and load is attempted. The first one that is compatible and loads
     * successfuly is init()ed and returned.
     **/
    public Module loadModule(ID assigned, ModuleSpecID specID, int where) {
        return loadModule(assigned, specID, where, false);
    }
    
    /**
     * Load a module from a spec id.
     * Advertisement is sought, compatibility is checked on all candidates
     * and load is attempted. The first one that is compatible and loads
     * successfuly is init()ed and returned.
     **/
    protected Module loadModule(ID assigned, ModuleSpecID specID, int where, boolean privileged) {
        
        boolean fromHere = (where == Here || where == Both);
        boolean fromParent = (where == FromParent || where == Both);
        
        List all = new ArrayList();
        
        if( fromHere && (null != discovery) ) {
            Enumeration here = discoverSome(DiscoveryService.ADV, "MSID", specID.toString(), 120, ModuleImplAdvertisement.class);
            all.addAll( Collections.list(here) );
        }
        
        if( fromParent && (null != getParentGroup()) && (null != parentGroup.getDiscoveryService()) ) {
            Enumeration parent = discoverSome(parentGroup.getDiscoveryService(), DiscoveryService.ADV, "MSID", specID.toString(), 120, ModuleImplAdvertisement.class);
            all.addAll( Collections.list(parent) );
        }
        
        Iterator allModuleImpls = all.iterator();
        Throwable recentFailure = null;
        
        while (allModuleImpls.hasNext()) {
            ModuleImplAdvertisement found = null;
            
            try {
                found = (ModuleImplAdvertisement) allModuleImpls.next();
                
                // Here's one.
                // First check that the MSID is realy the one we're
                // looking for. It could have appeared somewhere else
                // in the adv than where we're looking, and discovery
                // doesn't know the difference.
                if (!found.getModuleSpecID().equals(specID)) {
                    continue;
                }
                // Try it. If "found" is not useable it will
                // trigger an exception and we'll try the next.
                Module newMod = loadModule(assigned, found, privileged);
                
                if( null != discovery ) {
                    try {
                        discovery.publish(found, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
                    } catch( IOException nopublish ) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Could not publish module impl adv.", nopublish);
                        }
                    }
                }
                
                // If we reach that point, the module is good.
                return newMod;
            } catch (Throwable e) {
                recentFailure = e;
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Not a usable impl adv: ", e);
                }
            }
        }
        
        // Throw an exception if there was a recent failure.
        if( null != recentFailure ) {
            if( recentFailure instanceof Error ) {
                throw (Error) recentFailure;
            } else if( recentFailure instanceof RuntimeException ) {
                throw (RuntimeException) recentFailure;
            } else {
                throw new UndeclaredThrowableException( recentFailure );
            }
        }
        
        if (LOG.isEnabledFor(Level.WARN)) {
            LOG.warn("Could not find a loadable implementation of SpecID: " + specID);
        }
        
        return null;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public ConfigParams getConfigAdvertisement() {
        return configAdvertisement;
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected void setConfigAdvertisement(ConfigParams config) {
        configAdvertisement = (PlatformConfig) config;
    }
    
    /*
     * Now comes the implementation of the public API, including the
     * API mandated by the Service interface.
     */
    
    /**
     * {@inheritDoc}
     *
     * <p/>It is not recommended to overload this method. Instead, subclassers
     * should overload either or both of
     * {@link #initFirst(PeerGroup,ID,Advertisement)} and {@link #initLast()}.
     * If this method is to be overloaded, the overloading method must
     * invoke <code>super.init</code>.
     *
     * <p/>This method invokes <code>initFirst</code>
     * with identical parameters. <code>initLast</initLast> does not take
     * parameters since the relevant information can be obtained from the
     * group following completion of the <code>initFirst</code> phase.
     * The resulting values may be different from the parameters to
     * <code>initFirst</code> since <code>initFirst</code> may
     * be overLoaded and the overloading method may modify these parameters
     * when calling <code>super.initFirst</code>. (See
     * {@link net.jxta.impl.Platform} for an example of such a case).
     *
     * <p/>Upon completion, the group object is marked as completely initialized
     * in all cases. Once a group object is completely initialized, it becomes
     * sensitive to reference counting.
     *
     * <p/>In the future this method may become final.
     **/
    public void init(PeerGroup homeGroup, ID assignedID, Advertisement impl)
    throws PeerGroupException {
        try {
            initFirst(homeGroup, assignedID, impl);
            initLast();
        }
        finally {
            // This must be done in all cases.
            initComplete = true;
        }
    }
    
    /**
     * Performs all initialization steps that need to be performed
     * before any subclass initialization is performed.
     *
     * <p/>Classes that override this method should always call
     * <code>super.initFirst()</code> <strong>before</strong> doing
     * any of their own work.
     *
     * @param homeGroup The group that serves as a parent to this group.
     * @param assignedID The unique ID assigned to this module. For
     * group this is the group ID or <code>null</code> if a group ID
     * has not yet been assigned. If null is passed, GenericPeerGroup
     * choses a new group ID.
     * @param impl The ModuleImplAdvertisement which defines this
     * group's implementation.
     **/
    protected void initFirst(PeerGroup homeGroup, ID assignedID, Advertisement impl) throws PeerGroupException {
        
        this.implAdvertisement = (ModuleImplAdvertisement) impl;
        this.parentGroup = homeGroup;
        
        try {
            // FIXME 20030919 bondolo@jxta.org This setup doesnt give us any
            // capability to use seed material or parent group.
            if (null == assignedID) {
                if( "cbid".equals( IDFactory.getDefaultIDFormat() ) ) {
                    throw new IllegalStateException( "Cannot generate group id for cbid group" );
                } else
                    assignedID = IDFactory.newPeerGroupID();
            } else {
                if (parentGroup != null) {
                    DiscoveryService disco = parentGroup.getDiscoveryService();
                    Enumeration found = disco.getLocalAdvertisements(DiscoveryService.GROUP, "GID", assignedID.toString());
                    
                    if (found.hasMoreElements()) {
                        peerGroupAdvertisement = (PeerGroupAdvertisement) found.nextElement();
                    }
                }
            }
            
            if (!(assignedID instanceof PeerGroupID)) {
                throw new PeerGroupException("assignedID must be a peer group ID");
            }
            
            // Start building our peer adv.
            peerAdvertisement = (PeerAdvertisement)
            AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
            
            peerAdvertisement.setPeerGroupID((PeerGroupID) assignedID);
            
            //            // make sure the parent group is the required group
            //            if (null != peerAdvertisement.getPeerGroupID().getParentPeerGroupID()) {
            //                if (null == parentGroup) {
            //                    throw new PeerGroupException("Group requires parent group : " + peerAdvertisement.getPeerGroupID().getParentPeerGroupID());
            //                } else if (!parentGroup.getPeerGroupID().equals(peerAdvertisement.getPeerGroupID().getParentPeerGroupID())) {
            //                    throw new PeerGroupException("Group requires parent group : " + peerAdvertisement.getPeerGroupID().getParentPeerGroupID() + ". Provided parent was : " + parentGroup.getPeerGroupID());
            //                }
            //            }
            
            // Do our part of the PeerAdv construction.
            if (configAdvertisement != null) {
                // Normally there will be a peer ID and a peer name in the config.
                PeerID configPID = (PeerID) configAdvertisement.getPeerID();
                
                if( (null == configPID) || (ID.nullID == configPID) ) {
                    if( "cbid".equals( IDFactory.getDefaultIDFormat() ) ) {
                        // Get our peer-defined parameters in the configAdv
                        XMLElement param = (XMLElement) configAdvertisement.getServiceParam( PeerGroup.membershipClassID );
                        
                        if( null == param )
                            throw new IllegalArgumentException( PSEConfigAdv.getAdvertisementType() +" could not be located" );
                        
                        Advertisement paramsAdv = null;
                        try {
                            paramsAdv = AdvertisementFactory.newAdvertisement( (XMLElement) param );
                        } catch( NoSuchElementException noadv ) {;}
                        
                        if( !(paramsAdv instanceof PSEConfigAdv) )
                            throw new IllegalArgumentException( "Provided Advertisement was not a " + PSEConfigAdv.getAdvertisementType() );
                        
                        PSEConfigAdv config = (PSEConfigAdv) paramsAdv;
                        
                        Certificate clientRoot = config.getCertificate();
                        
                        byte [] pub_der = clientRoot.getPublicKey().getEncoded();
                        
                        configAdvertisement.setPeerID( IDFactory.newPeerID( (PeerGroupID) assignedID, pub_der ) );
                    } else
                        configAdvertisement.setPeerID( IDFactory.newPeerID( (PeerGroupID) assignedID) );
                }
                
                peerAdvertisement.setPeerID(configAdvertisement.getPeerID());
                peerAdvertisement.setName(configAdvertisement.getName());
                peerAdvertisement.setDesc(configAdvertisement.getDesc());
            } else {
                if (null == parentGroup) {
                    // If we did not get a valid peer id, we'll initialize it here.
                    peerAdvertisement.setPeerID(IDFactory.newPeerID((PeerGroupID) assignedID));
                } else {
                    // We're not the platform, which is the authoritative source of these values.
                    peerAdvertisement.setPeerID(parentGroup.getPeerAdvertisement().getPeerID());
                    peerAdvertisement.setName(parentGroup.getPeerAdvertisement().getName());
                    peerAdvertisement.setDesc(parentGroup.getPeerAdvertisement().getDesc());
                }
            }
            
            if (peerGroupAdvertisement == null) {
                // No existing gadv. OK then we're creating the group or we're
                // the platform, it seems. Start a grp adv with the essentials
                // that we know.
                peerGroupAdvertisement = (PeerGroupAdvertisement)
                AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
                
                peerGroupAdvertisement.setPeerGroupID((PeerGroupID) assignedID);
                peerGroupAdvertisement.setModuleSpecID(implAdvertisement.getModuleSpecID());
            } else {
                published = true;
            }
            
            // If we still do not have a config adv, make one with the minimal info in it.
            // All groups but the Platform and the netPG are in that case.
            // In theory a plain ConfigParams would be enough for subgroups. But for now
            // GenericPeerGroup always has a full Platformconfig and there is no other concrete
            // ConfigParams subclass.
            if (configAdvertisement == null) {
                PlatformConfig conf = (PlatformConfig) AdvertisementFactory.newAdvertisement(PlatformConfig.getAdvertisementType());
                conf.setPeerID(peerAdvertisement.getPeerID());
                conf.setName(peerAdvertisement.getName());
                conf.setDesc(peerAdvertisement.getDesc());
                configAdvertisement = conf;
            }
            
            // Merge service params with those specified by the group (if any). The only
            // policy, right now, is to give peer params the precedence over group params.
            Hashtable grpParams = peerGroupAdvertisement.getServiceParams();
            Enumeration keys = grpParams.keys();
            while (keys.hasMoreElements()) {
                ID key = (ID) keys.nextElement();
                Element e = (Element) grpParams.get(key);
                if (configAdvertisement.getServiceParam(key) == null) {
                    configAdvertisement.putServiceParam(key, e);
                }
                
            }
            
            /*
             * Now seems like the right time to attempt to register the group.
             * The only trouble is that it could cause the group to
             * be used before all the services are initialized, but on the
             * other hand, we do not want to let a redundant group go through
             * it's service initialization because that would cause irreparable
             * damage to the legitimate instance. There should be a synchro on
             * on the get<service>() and lookupService() routines.
             */
            if (globalRegistry.registerInstance((PeerGroupID) assignedID, this) == false) {
                throw new PeerGroupException("Group already instantiated");
            }
        } catch (Throwable any) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error( "Group init failed", any );
            }
            
            if (any instanceof Error) {
                throw (Error) any;
            } else if (any instanceof RuntimeException) {
                throw (RuntimeException) any;
            } else if (any instanceof PeerGroupException) {
                throw (PeerGroupException) any;
            }
            
            throw new PeerGroupException( "Group init failed", any );
        }
        
        ThreadGroup parentThreadGroup = (null != this.parentGroup) ? parentGroup.getHomeThreadGroup() : Thread.currentThread().getThreadGroup();
        
        threadGroup = new ThreadGroup(parentThreadGroup, "Group " + peerGroupAdvertisement.getPeerGroupID());
        
        /*
         * The rest of construction and initialization are left to the
         * group subclass, between here and the begining for initLast.
         * That should include instanciating and setting the endpoint, and
         * finally supplying it with endpoint protocols.
         * That also includes instanciating the appropriate services
         * and registering them.
         * For an example, see the StdPeerGroup class.
         */
    }
    
    /**
     * Perform all initialization steps that need to be performed
     * after any subclass initialization is performed.
     *
     * <p/>Classes that override this method should always call super.initLast
     * <strong>after</strong> doing any of their own work.
     **/
    protected void initLast() throws PeerGroupException {
        // done with configuration
        configurator = null;
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Group : " + getPeerGroupID());
            
            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: " + implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : " + implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : " + implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : " + implAdvertisement.getCode());
            }
            
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tModule Spec ID : " + implAdvertisement.getModuleSpecID());
            configInfo.append("\n\t\tPeer Group ID : " + getPeerGroupID());
            configInfo.append("\n\t\tGroup Name : " + getPeerGroupName());
            configInfo.append("\n\t\tPeer ID in Group : " + getPeerID());
            
            configInfo.append("\n\tConfiguration :");
            if (null == parentGroup) {
                configInfo.append("\n\t\tHome Group : (none)");
            } else {
                configInfo.append("\n\t\tHome Group : \"" + parentGroup.getPeerGroupName() + "\" / " + parentGroup.getPeerGroupID());
            }
            
            configInfo.append("\n\t\tServices :");
            Iterator eachService = services.entrySet().iterator();
            
            while (eachService.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachService.next();
                ModuleClassID aMCID = (ModuleClassID) anEntry.getKey();
                ModuleImplAdvertisement anImplAdv = (ModuleImplAdvertisement) ((Service)anEntry.getValue()).getImplAdvertisement();
                
                configInfo.append( "\n\t\t\t" + aMCID + "\t"+  anImplAdv.getDescription() );
            }
            
            LOG.info(configInfo);
        }
    }
    
    /**
     * Start the peergroup.
     *
     * <p/>In practice, it means starting its main application. But that's for
     * subclasses. GenericPeerGroup does not implement the concept of
     * initial app anylonger. See subclasses.
     *
     * @return int Status.
     */
    public int startApp(String[] arg) {
        return 0;
    }
    
    /**
     * Stops the group and all its services.
     *
     * <p/>PeerGroupInterface's stopApp() does nothing. Only a real reference
     * to the group object permits to stop it without going through ref
     * counting.
     */
    public void stopApp() {
        
        globalRegistry.unRegisterInstance(peerGroupAdvertisement.getPeerGroupID(), this);
        removeAllServicesSync();
        
        // Explicitly unreference our home group in order to allow it
        // to terminate if this group object was itself the last reference
        // to it.
        if (parentGroup != null) {
            parentGroup.unref();
            parentGroup = null;
        }
    }
    
    /**
     * May be called by a module which has a direct reference to
     * the group object and wants to notify its abandoning it.
     * Has no effect on the real group object.
     */
    public void unref() {}
    
    /**
     * Called everytime an interface object that refers to this group
     * goes away, either by being finalized or by its unref method being
     * invoked explicitly.
     */
    protected void decRefCount() {
        synchronized (this) {
            --masterRefCount;
            
            if (LOG.isEnabledFor(Level.INFO)) {
                Throwable trace = new Throwable( "Stack Trace" );
                StackTraceElement elements[] = trace.getStackTrace();
                LOG.info("[" + getPeerGroupID() + "] GROUP REF COUNT DECCREMENTED TO: " + masterRefCount +
                " by\n\t" + elements[2] );
            }
            
            if ( masterRefCount != 0) {
                return;
            }
            
            if (!stopWhenUnreferenced) {
                return;
            }
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("[" + getPeerGroupID() + "] STOPPING UNREFERENCED GROUP" );
        }
        
        stopApp();
    }
    
    /*
     * Implement the Service API so that we can make groups services when we
     * decide to.
     */
    
    /**
     *  {@inheritDoc}
     **/
    public Service getInterface() {
        synchronized (this) {
            ++masterRefCount;
            
            if (LOG.isEnabledFor(Level.INFO)) {
                Throwable trace = new Throwable( "Stack Trace" );
                StackTraceElement elements[] = trace.getStackTrace();
                LOG.info("[" + getPeerGroupID() + "] GROUP REF COUNT INCREMENTED TO: " + masterRefCount +
                " by\n\t" + elements[1] );
            }
            
            if (initComplete) {
                // If init is complete the group can become sensitive to
                // its ref count reaching zero. Before there could be
                // transient references before there is a chance to give
                // a permanent reference to the invoker of newGroup.
                stopWhenUnreferenced = true;
            }
        }
        
        return new RefCountPeerGroupInterface(this);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroup getWeakInterface() {
        return new PeerGroupInterface(this);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Advertisement getImplAdvertisement() {
        return (Advertisement) implAdvertisement.clone();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void publishGroup(String name, String description) throws IOException {
        
        if (published) {
            return;
        }
        
        peerGroupAdvertisement.setName(name);
        peerGroupAdvertisement.setDescription(description);
        
        if (parentGroup == null) {
            return;
        }
        
        DiscoveryService parentDiscovery = parentGroup.getDiscoveryService();
        
        if( null == parentDiscovery ) {
            return;
        }
        
        parentDiscovery.publish(peerGroupAdvertisement, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
        
        published = true;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroup newGroup(Advertisement pgAdv) throws PeerGroupException {
        
        PeerGroupAdvertisement adv = (PeerGroupAdvertisement) pgAdv;
        
        PeerGroupID gid = adv.getPeerGroupID();
        
        if ((gid == null) || ID.nullID.equals(gid)) {
            throw new IllegalArgumentException("Advertisement did not contain a peer group ID");
        }
        
        PeerGroup theNewGroup = globalRegistry.lookupInstance(gid);
        
        if (theNewGroup != null) {
            return theNewGroup;
        }
        
        // We do not know if the grp adv had been previously published or not...  Since it may contain information essential to
        // the configuration of services, we need to make sure it is published localy, rather than letting the group publish
        // itself after the fact.
        
        // FIXME jice@jxta.org 20040713 : The downside is that we're publishing the adv even before making sure that this group
        // can really be instantiated. We're basically using the cm as a means to pass parameters to the module because it is a
        // group. We have the same parameter issue with the config adv. Eventually we need to find a clean way of passing
        // parameters specific to a certain types of module.
        
        try {
            discovery.publish(adv, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
        } catch (Exception any) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Could not publish the group advertisement: ", any);
            }
        }
        
        theNewGroup = (PeerGroup) loadModule( adv.getPeerGroupID(), adv.getModuleSpecID(), Here, false);
        
        if (theNewGroup == null) {
            throw new PeerGroupException( "Could not find group implementation with " + adv.getModuleSpecID() );
        }
        
        return (PeerGroup) theNewGroup.getInterface();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl,
    String name, String description)
    throws PeerGroupException {
        PeerGroup theNewGroup = null;
        
        if (null != gid) {
            theNewGroup = globalRegistry.lookupInstance(gid);
        }
        
        if (theNewGroup != null) {
            return theNewGroup;
        }
        
        try {
            theNewGroup = (PeerGroup) loadModule(gid, (ModuleImplAdvertisement) impl, false);
        } catch (Throwable any) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error( "Could not load group implementation", any );
            }
            
            throw new PeerGroupException( "Could not load group implementation", any );
        }
        
        try {
            // The group adv definitely needs to be published.
            theNewGroup.publishGroup(name, description);
        } catch (Exception any) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn( "Could not publish group or implementation:", any );
            }
        }
        
        return (PeerGroup) theNewGroup.getInterface();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroup newGroup(PeerGroupID gid) throws PeerGroupException {
        
        if ((gid == null) || ID.nullID.equals(gid)) {
            throw new IllegalArgumentException("Invalid peer group ID");
        }
        
        PeerGroup result = globalRegistry.lookupInstance(gid);
        
        if (result != null) {
            return result;
        }
        
        PeerGroupAdvertisement adv;
        
        try {
            adv = (PeerGroupAdvertisement)
            discoverOne(DiscoveryService.GROUP, "GID", gid.toString(), 120, PeerGroupAdvertisement.class );
        } catch ( Throwable any ) {
            throw new PeerGroupException( "Failed finding group advertisement for " + gid, any );
        }
        
        if (adv == null) {
            throw new PeerGroupException("Could not find group advertisement for group " + gid );
        }
        
        return newGroup( adv );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public JxtaLoader getLoader() {
        return loader;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getPeerName() {
        // before init we must fail.
        if(null == peerAdvertisement) {
            throw new IllegalStateException( "PeerGroup not sufficiently initialized" );
        }

        return peerAdvertisement.getName();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getPeerGroupName() {
        // before init we must fail.
        if(null == peerGroupAdvertisement) {
            throw new IllegalStateException( "PeerGroup not sufficiently initialized" );
        }

        return peerGroupAdvertisement.getName();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroupID getPeerGroupID() {
        // before init we must fail.
        if(null == peerGroupAdvertisement) {
            throw new IllegalStateException( "PeerGroup not sufficiently initialized" );
        }

        return peerGroupAdvertisement.getPeerGroupID();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerID getPeerID() {
        // before init we must fail.
        if(null == peerAdvertisement) {
            throw new IllegalStateException( "PeerGroup not sufficiently initialized" );
        }

        return peerAdvertisement.getPeerID();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerAdvertisement getPeerAdvertisement() {
        return peerAdvertisement;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        return peerGroupAdvertisement;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isRendezvous() {
        if (rendezvous == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Rendezvous service null");
            }
        }
        return (rendezvous != null) && rendezvous.isRendezVous();
    }
    
    /*
     * shortcuts to the well-known services, in order to avoid calls to lookup.
     */
    
    /**
     * {@inheritDoc}
     **/
    public EndpointService getEndpointService() {
        if (endpoint == null) {
            return null;
        }
        return (EndpointService) endpoint.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public ResolverService getResolverService() {
        if (resolver == null) {
            return null;
        }
        return (ResolverService) resolver.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public DiscoveryService getDiscoveryService() {
        if (discovery == null) {
            return null;
        }
        return (DiscoveryService) discovery.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public PeerInfoService getPeerInfoService() {
        if (peerinfo == null) {
            return null;
        }
        return (PeerInfoService) peerinfo.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public MembershipService getMembershipService() {
        if (membership == null) {
            return null;
        }
        return (MembershipService) membership.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public PipeService getPipeService() {
        if (pipe == null) {
            return null;
        }
        return (PipeService) pipe.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public RendezVousService getRendezVousService() {
        if (rendezvous == null) {
            return null;
        }
        return (RendezVousService) rendezvous.getInterface();
    }
    
    /**
     * {@inheritDoc}
     **/
    public AccessService getAccessService() {
        if (access == null) {
            return null;
        }
        
        return (AccessService) access.getInterface();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public abstract ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement();
    
    
    /**
     * Get this group's parent group.
     *
     * <p/>We do not want to count on the invoker to properly unreference
     * the group object that we return; this call is often used in a
     * loop and it is silly to increment and decrement ref-counts for
     * references that are sure to live shorter than the referee.
     * On the other hand it is dangerous for us to share our reference
     * object to the parent group. That's where weak interface objects
     * come in handy. We can safely make one and give it away.
     */
    public PeerGroup getParentGroup() {
        if (parentGroup == null) {
            return null;
        }
        return parentGroup.getWeakInterface();
    }
}

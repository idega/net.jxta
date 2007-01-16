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
 * $Id: StdPeerGroup.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */
package net.jxta.impl.peergroup;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Application;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.service.Service;

import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.exception.ViolationException;

import net.jxta.impl.cm.Cm;
import net.jxta.impl.cm.SrdiIndex;

/**
 * A subclass of GenericPeerGroup that makes a peer group out of independent
 * plugin services listed in its impl advertisement.
 */
public class StdPeerGroup extends GenericPeerGroup implements CompatibilityEquater {
    
    private static final Logger LOG = Logger.getLogger(StdPeerGroup.class.getName());
    
    private volatile boolean initialized = false;
    private volatile boolean started = false;
    
    /**
     *  A map of the applications for this group.
     *
     *  <ul>
     *  <li>keys are {@see net.jxta.platform.ModuleClassID}</li>
     *  <li>values are {@see net.jxta.platform.Module} or
     *  {@link net.jxta.protocol.ModuleImplAdvertisement}</li>
     *  </ul>
     **/
    private Hashtable applications = new Hashtable();
    
    /**
     *  A map of the protocols for this group.
     *
     *  <ul>
     *  <li>keys are {@see net.jxta.platform.ModuleClassID}</li>
     *  <li>values are {@see net.jxta.platform.Module}, but should also be
     *    {@see net.jxta.endpoint.MessageTransport}</li>
     *  </ul>
     *
     **/
    private Hashtable protocols = new Hashtable();
    
    /**
     *  Cache for this group.
     **/
    private Cm cm = null;
    
    // A few things common to all ImplAdv for built-in things.
    public static final StructuredTextDocument stdCompatStatement = mkCS();
    public static final String stdUri = "http://www.jxta.org/download/jxta.jar";
    public static final String stdProvider = "sun.com";
    
    static final String compatKey1 = "Efmt";
    static final String compatKey2 = "Bind";
    static final String compatVal1 = "JDK1.4.1";
    static final String compatVal2 = "V2.0 Ref Impl";
    
    private static StructuredTextDocument mkCS() {
        StructuredTextDocument doc = (StructuredTextDocument)
        StructuredDocumentFactory.newStructuredDocument(
        MimeMediaType.XMLUTF8,
        "Comp");
        Element e = doc.createElement(compatKey1, compatVal1);
        doc.appendChild(e);
        e = doc.createElement(compatKey2, compatVal2);
        doc.appendChild(e);
        return doc;
    }
    
    private ModuleImplAdvertisement allPurposeImplAdv = null;
    
    /**
     * constructor
     **/
    public StdPeerGroup() {
    }
    
    /**
     * An internal convenience method essentially for bootstrapping.
     * Make a standard ModuleImplAdv for any service that comes builtin this
     * reference implementation.
     * In most cases there are no params, so we do not take that argument.
     * The invoker may add params afterwards.
     **/
    protected static ModuleImplAdvertisement mkImplAdvBuiltin(
    ModuleSpecID specID,
    String code,
    String descr ) {
        
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement)
        AdvertisementFactory.newAdvertisement(
        ModuleImplAdvertisement.getAdvertisementType());
        
        implAdv.setModuleSpecID(specID);
        implAdv.setCompat(stdCompatStatement);
        implAdv.setCode(code);
        implAdv.setUri(stdUri);
        implAdv.setProvider(stdProvider);
        implAdv.setDescription(descr);
        
        return implAdv;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean compatible(Element compat) {
        return isCompatible( compat );
    }
    
    /**
     * Evaluates if the given compatibility statement makes the module that
     * bears it is loadable by this group.
     *
     * @return boolean True if the given statement is compatible.
     */
    public static boolean isCompatible(Element compat) {
        
        // Our criterion of compatibility is equality. However, we
        // must do the comparision on a logical level. This is
        // easy because the reference compat statement is manufactured
        // by this class as well. So we can make assumptions as
        // to its structure: two elements each with a specific value.
        boolean oneOk = false;
        boolean twoOk = false;
        
        try {
            Enumeration hisChildren = compat.getChildren();
            int i = 0;
            while (hisChildren.hasMoreElements()) {
                
                // Stop after 2 elements; there shall not be more.
                if (++i > 2) return false;
                
                Element e = (Element) hisChildren.nextElement();
                String key = (String) e.getKey();
                String val = (String) e.getValue();
                if (compatKey1.equals(key) && compatVal1.equals(val)) {
                    oneOk = true;
                } else if (compatKey2.equals(key) && compatVal2.equals(val)) {
                    twoOk = true;
                } else {
                    return false; // Might as well stop right now.
                }
            }
            
        } catch (Exception any) {
        }
        
        return oneOk && twoOk;
    }
    
    Vector disabledModules = new Vector();
    
    /**
     * Builds a table of modules indexed by their class ID.
     * The values are the loaded modules, the keys are their classId.
     * This routine interprets the parameter list in the advertisement.
     *
     *  @param modules  The modules to load
     *  @param thisClassOnly    load only the module specified from the map
     *  @param privileged   if true then modules will get a real reference to
     *  the group loading them, otherwise its an interface object.
     **/
    protected void loadAllModules(Hashtable modules,
    ModuleClassID thisClassOnly,
    boolean privileged) {
        
        Enumeration allKeys = modules.keys();
        while (allKeys.hasMoreElements()) {
            ModuleClassID classID = (ModuleClassID) allKeys.nextElement();
            Object value = modules.get(classID);
            
            // Class was filtered.
            if (thisClassOnly != null && ! thisClassOnly.equals(classID)) {
                continue;
            }
            
            // If it is disabled, strip it.
            if (disabledModules.contains(classID)) {
                if (value instanceof ModuleClassID) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("Module disabled by configuration : " + classID );
                    }
                } else {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("Module disabled by configuration : " + ((ModuleImplAdvertisement) value).getDescription());
                    }
                }
                
                modules.remove(classID);
                if (thisClassOnly != null) {
                    break;
                } else {
                    continue;
                }
            }
            
            // Already loaded.
            if (value instanceof Module) {
                if (thisClassOnly != null) {
                    break;
                } else {
                    continue;
                }
            }
            
            // Try and load it.
            try {
                Module theModule = null;
                if (value instanceof ModuleImplAdvertisement) {
                    // Load module will republish localy but not in the
                    // parent since that adv does not come from there.
                    theModule = loadModule(classID, (ModuleImplAdvertisement) value, privileged);
                } else if (value instanceof ModuleSpecID) {
                    // loadModule will republish both localy and in the parent
                    // Where the module was fetched.
                    theModule = loadModule(classID, (ModuleSpecID) value, FromParent, privileged);
                } else {
                    if (LOG.isEnabledFor(Level.ERROR))
                        LOG.error("Skipping: " + classID + " Unsupported module descriptor : " + value.getClass().getName() );
                    modules.remove(classID);
                    if (thisClassOnly != null) break; else continue;
                }
                
                if (theModule == null) {
                    throw new PeerGroupException("Could not find a loadable implementation for : " + classID );
                }
                
                modules.put(classID, theModule);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Could not load module for class ID " + classID, e);
                }
                if (value instanceof ModuleClassID) {
                    if (LOG.isEnabledFor(Level.WARN)) 
                        LOG.warn("Will be missing from peer group: "
                        + classID.toString()
                        + " (" + e.getMessage() + ").");
                } else {
                    if (LOG.isEnabledFor(Level.WARN)) 
                        LOG.warn("Will be missing from peer group: "
                        + ((ModuleImplAdvertisement)value).getDescription()
                        + " (" + e.getMessage() + ").");
                }
                modules.remove(classID);
            }
            if (thisClassOnly != null) break;
        }
    }
    
    /**
     * The group does not care for start args, and does not come-up
     * with args to pass to its main app. So, until we decide on something
     * more useful, the args of the group's startApp are passed-on to the
     * group's main app. NB: both the apps init and startApp methods are
     * invoked.
     *
     * @return int Status.
     **/
    public int startApp(String[] arg) {
        
        if ( !initialized) {
            if (LOG.isEnabledFor(Level.ERROR))
                LOG.error("Group has not been initialized or init failed.");
            return -1;
        }
        
        // FIXME: maybe concurrent callers should be blocked until the
        // end of startApp(). That could mean forever, though.
        if (started == true) return 0;
        started = true;
        
        // Normaly does nothing, but we have to.
        super.startApp(arg);
        
        loadAllModules(applications, null, false); // Apps are non-privileged;
        
        int res = 0;
        Enumeration appKeys = applications.keys();
        while (appKeys.hasMoreElements()) {
            Object appKey = appKeys.nextElement();
            Module app = (Module) applications.get(appKey);
            int tmp =  app.startApp(arg);
            if (tmp != 0) applications.remove(appKey);
            else applications.put(appKey, app);
            res += tmp;
        }
        return res;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void stopApp() {
        Iterator modules = applications.values().iterator();
        while (modules.hasNext()) {
            Module module = null;
            try {
                module = (Module) modules.next();
                module.stopApp();
            } catch (Exception any) {
                if (module != null) {
                    if (LOG.isEnabledFor(Level.WARN))
                        LOG.warn("Failed to stop application: " + module.getClass().getName(), any );
                }
            }
        }
        
        applications.clear();
        
        modules = protocols.values().iterator();
        while (modules.hasNext()) {
            Module module = null;
            try {
                module = (Module) modules.next();
                module.stopApp();
            } catch (Exception any) {
                if (module != null) {
                    if (LOG.isEnabledFor(Level.WARN))
                        LOG.warn("Failed to stop protocol : " + module.getClass().getName(), any );
                }
            }
        }
        
        protocols.clear();
        
        if (cm != null) {
            cm.stop();
            cm = null;
        }
        
        super.stopApp();
    }
    
    /**
     * {@inheritDoc}
     *
     * This method loads and initializes all modules
     * described in the given implementation advertisement. Then, all modules
     * are placed in a list and the list is processed iteratively. During each
     * iteration, the {@link Module#startApp(String[])} method of each module
     * is invoked once. Iterations continue until no progress is being made or
     * the list is empty.
     *
     * <p/>The status returned by the {@link Module#startApp(String[])} method
     * of each module is considered as follows:
     *
     * <ul>
     * <li>{@link Module#START_OK}: The module is removed from the list of
     * modules to be started and its {@link Module#startApp(String[])}
     * method will not be invoked again.
     * </li>
     *
     * <li>{@link Module#START_AGAIN_PROGRESS}: The module remains in the
     * list of modules to be started and its {@link Module#startApp(String[])}
     * method will be invoked during the next iteration, if there is one. </li>
     *
     * <li>{@link Module#START_AGAIN_STALLED}: The module remains in the list
     * of modules to be started and its {@link Module#startApp(String[])}
     * method will be invoked during the next iteration if there is one. </li>
     *
     * <li>Any other value: The module failed to initialize. Its
     * {@link Module#startApp(String[])}
     * method will not be invoked again.</li>
     * </ul>
     *
     * <p/>Iterations through the list stop when:
     * <ul>
     * <li>The list is empty: the group initialization proceeds.</li>
     *
     * <li>A complete iteration was performed and all modules returned
     * {@link Module#START_AGAIN_STALLED}: a {@link PeerGroupException}
     * is thrown.</li>
     *
     * <li>A number of complete iteration completed without any module
     * returning {@link Module#START_OK}: a {@link PeerGroupException}
     * is thrown. The number of complete iterations before that happens is
     * computed as 1 + the square of the number of modules currently in the
     * list.</li>
     * </ul>
     *
     **/
    protected synchronized void initFirst( PeerGroup parent, ID assignedID, Advertisement impl )
    throws PeerGroupException {
        
        if (initialized == true) {
            if (LOG.isEnabledFor(Level.WARN))
                LOG.warn("You cannot initialize a PeerGroup more than once !");
            return;
        }
        
        // Set-up the minimal GenericPeerGroup
        super.initFirst(parent, assignedID, impl);
        
        // initialize cm before starting services. Do not refer to assignedID, as it could be
        // null, in which case the group ID has been generated automatically by super.initFirst()
        try {
            cm = new Cm( getHomeThreadGroup(), getPeerGroupID().getUniqueValue().toString(), true, Cm.DEFAULT_GC_MAX_INTERVAL, false );
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Error during creation of local store", e);
            }
            throw new PeerGroupException("Error during creation of local store", e);
        }
        
        // flush srdi for this group
        SrdiIndex.clearSrdi(this);
        
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) impl;
        
        /*
         * Build the list of modules disabled by config.
         */
        ConfigParams conf = (ConfigParams) getConfigAdvertisement();
        if (conf != null) {
            Iterator eachService = conf.getServiceParamsEntrySet().iterator();
            
            while ( eachService.hasNext() ) {
                Entry anEntry = (Entry) eachService.next();
                
                TextElement e = (TextElement) anEntry.getValue();
                if (e.getChildren("isOff").hasMoreElements()) {
                    disabledModules.addElement( anEntry.getKey() );
                }
            }
            conf = null;
        }
        
        /*
         * Load all the modules from the advertisement
         */
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv(implAdv.getParam());
        
        // Applications are shelved until startApp()
        applications = paramAdv.getApps();
        
        // FIXME: we should improve services-dependencies so that we do not
        // have to do that hand-filtering. But it will do for now.
        Hashtable initServices = paramAdv.getServices();
        protocols = paramAdv.getProtos();
        
        // Load & init the designated modules; updates the "services" table.
        
        // First Load the Peer Info Service since other services need to register Monitors through it
        loadAllModules(initServices, peerinfoClassID, true);
        Module peerInfoModule = (Module) initServices.get(peerinfoClassID);
        if (peerInfoModule != null) {
            addService(peerinfoClassID, (Service) peerInfoModule);
        }
        
        // Next Load Endpoint Service since other services may look for it in their init();
        
        loadAllModules(initServices, endpointClassID, true);
        
        // Check that the endpoint module is in the table (means that it was
        // in there and could be loaded and init'ed successfully).
        Module endp = (Module) initServices.get(endpointClassID);
        
        addService(endpointClassID, (Service) endp);
        initServices.remove(endpointClassID); // Done with that one.
        // Since we have an endpoint, load the protocols. They are
        // not necessarily services, just Modules, so they do not
        // get registered in the services table.
        // Wait until other services are loaded before starting the
        // protocols though.
        
        loadAllModules(protocols, null, true);
        
        // Get all the others services now.
        
        loadAllModules(initServices, null, true);
        
        Module m;
        Enumeration allKeys = initServices.keys();
        while (allKeys.hasMoreElements()) {
            ModuleClassID classID = (ModuleClassID) allKeys.nextElement();
            m = (Module) initServices.get(classID);
            if (m instanceof Service) {
                addService(classID, (Service) m);
            } else {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Suspicious service: " + classID + " is not an instance of Service.");
                    LOG.warn("It will not be registered as a service.");
                }
            }
        }
        
        // Make sure all the required services are loaded.
        try {
            checkServices();
        } catch (ServiceNotFoundException e) {
            LOG.error( "Missing peer group service", e );
            throw new PeerGroupException( "Missing peer group service", e );
        } catch ( Throwable e) {
            LOG.error( "Unhandled Throwable", e );
            throw new PeerGroupException( "Unhandled Throwable", e );
        }
        
        // Make a list of all the things we need to start.
        // There is an a-priori order, but we'll iterate over the
        // list until all where able to complete their start phase
        // or no progress is made. Since we give to modules the opportunity
        // to pretend that they are making progress, we need to have a
        // safeguard: we will not iterate through the list more than N^2 + 1
        // times without at least one module completing; N being the number
        // of modules still in the list. That should cover the worst case
        // scenario and still allow the process to eventually fail if it has
        // no chance of success.
        
        Map allStart = new HashMap( protocols.size() + initServices.size() + 1 );
        
        allStart.put( endpointClassID, endp );
        allStart.putAll( initServices );
        allStart.putAll( protocols );
        
        long modulesToGo = allStart.size();
        long maxIterations = modulesToGo * modulesToGo + 1;
        boolean progress = true;
        
        while (progress && (modulesToGo > 0) && (maxIterations-- > 0)) {
            
            progress = false;
            
            Iterator eachModule = allStart.entrySet().iterator();
            
            while (eachModule.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachModule.next();
                
                m = (Module) anEntry.getValue();
                int res;
                try {
                    res = m.startApp(null);
                } catch ( Throwable all ) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Exception in startApp() : " + m, all );
                    }
                    res = -1;
                }
                
                switch (res) {
                    case Module.START_OK:
                        // One done. Remove from allStart and recompute maxIteration.
                        
                        if (LOG.isEnabledFor(Level.INFO)) {
                            LOG.info("Module started : " + m);
                        }
                        eachModule.remove();
                        --modulesToGo;
                        maxIterations = modulesToGo * modulesToGo + 1;
                        
                    case Module.START_AGAIN_PROGRESS:
                        progress = true;
                        
                    case Module.START_AGAIN_STALLED:
                        break;
                        
                    default: // (negative)
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn( "Module failed to start (" + res + ") : " + m );
                        }
                        eachModule.remove();
                        --modulesToGo;
                        maxIterations = modulesToGo * modulesToGo + 1;
                        
                        // remove the module from the service tables. we don't
                        // know which table its in unfortunately.
                        try {
                            if( m instanceof Service )
                                removeService( (ModuleClassID) anEntry.getKey(), (Service) m );
                        } catch ( ServiceNotFoundException ignored ) {;}
                        catch ( ViolationException ignored ) {;}
                        protocols.remove( anEntry.getKey() );
                        break;
                }
            }
        }
        
        // Uh-oh. Services co-dependency prevented them from starting.
        if (allStart.size() > 0) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                StringBuffer failed = new StringBuffer( "No progress is being made in starting services. Giving up." );
                
                failed.append( "\nThe following services refused to start: " );
                
                Iterator sequence = allStart.values().iterator();
                
                while (sequence.hasNext()) {
                    failed.append( "\n\t" );
                    failed.append( sequence.next().toString() );
                }
                
                LOG.error( failed );
            }
            
            throw new PeerGroupException("No progress is being made in starting services. Giving up.");
        }
        
        // Make sure all the required services are loaded.
        try {
            checkServices();
        } catch (ServiceNotFoundException e) {
            LOG.fatal( "Missing peer group service", e );
            throw new PeerGroupException( "Missing peer group service", e );
        } catch ( Throwable e) {
            LOG.fatal( "Unhandled Throwable", e );
            throw new PeerGroupException( "Unhandled Throwable", e );
        }
        
        /*
         * Publish a few things that have not been published in this
         * group yet.
         */
        DiscoveryService disco = getDiscoveryService();
        if (disco != null) {
            // It should work but if it does not we can survive.
            try {
                // Discovery service adv could not be published localy,
                // since at that time there was no local discovery to
                // publish to. FIXME: this is really a cherry on the cake.
                // no-one realy cares
                disco.publish(disco.getImplAdvertisement(),
                DEFAULT_LIFETIME,
                DEFAULT_EXPIRATION);
                
                // Try to publish our impl adv within this group. (it was published
                // in the parent automatically when loaded.
                disco.publish(implAdv,
                DEFAULT_LIFETIME,
                DEFAULT_EXPIRATION);
            } catch(Exception nevermind) {
                if (LOG.isEnabledFor(Level.WARN))
                    LOG.warn( "Failed to publish Impl adv within group.", nevermind );
            }
        }
        
        initialized = true;
    }
    
    /**
     * {@inheritDoc}
     */
    protected synchronized void initLast() throws PeerGroupException {
        // Nothing special for now, but we might want to move some steps
        // from initFirst, in the future.
        super.initLast();
        
        if (LOG.isEnabledFor(Level.INFO)) {
            StringBuffer configInfo = new StringBuffer("Configuring Group : " + getPeerGroupID());
            
            configInfo.append("\n\tConfiguration :");
            
            configInfo.append("\n\t\tCompatibility Statement :\n\t\t\t" );
            
            StringBuffer indent = new StringBuffer( stdCompatStatement.toString().trim() );
            int from = indent.length();
            
            while ( from > 0 ) {
                int returnAt = indent.lastIndexOf( "\n", from ) ;
                
                from = returnAt -1 ;
                
                if( (returnAt >= 0)  && (returnAt != indent.length()) ) {
                    indent.insert( returnAt + 1, "\t\t\t" );
                }
            }
            
            configInfo.append( indent );
            
            Iterator eachProto = protocols.entrySet().iterator();
            
            if( eachProto.hasNext() ) {
                configInfo.append("\n\t\tProtocols :");
            }
            
            while (eachProto.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachProto.next();
                ModuleClassID aMCID = (ModuleClassID) anEntry.getKey();
                Module anMT = (Module) anEntry.getValue();
                
                configInfo.append( "\n\t\t\t" + aMCID + "\t"+ ((anMT instanceof MessageTransport) ? ((MessageTransport)anMT).getProtocolName() : anMT.getClass().getName()) );
            }
            
            Iterator eachApp = applications.entrySet().iterator();
            
            if(eachApp.hasNext() ) {
                configInfo.append("\n\t\tApplications :");
            }
            
            while (eachApp.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachApp.next();
                ModuleClassID aMCID = (ModuleClassID) anEntry.getKey();
                Object anApp = anEntry.getValue();
                
                if( anApp instanceof ModuleImplAdvertisement ) {
                    ModuleImplAdvertisement adv = (ModuleImplAdvertisement) anApp;
                    configInfo.append( "\n\t\t\t" + aMCID + "\t"+ adv.getCode() );
                } else {
                    configInfo.append( "\n\t\t\t" + aMCID + "\t"+ anApp.getClass().getName() );
                }
            }
            
            LOG.info(configInfo);
        }
    }
    
    /**
     * Returns the all purpose peer group implementation advertisement.
     * This defines a peergroup implementation that can be used for
     * many purposes, and from which one may derive slightly different
     * peergroup implementations.
     * This definition is always the same and has a well known ModuleSpecID.
     * It includes the basic service, no protocols and the shell for main
     * application.
     * The user must remember to change the specID if the set of services
     * protocols or applications is altered before use.
     *
     * @return ModuleImplAdvertisement The new peergroup impl adv.
     */
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
        
        // Build it only the first time; then clone it.
        if (allPurposeImplAdv != null)
            return (ModuleImplAdvertisement) allPurposeImplAdv.clone();
        
        // grab an impl adv
        ModuleImplAdvertisement implAdv =
        mkImplAdvBuiltin( PeerGroup.allPurposePeerGroupSpecID,
        StdPeerGroup.class.getName(),
        "General Purpose Peer Group Implementation");
        
        TextElement paramElement = (TextElement) implAdv.getParam();
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv( );
        ModuleImplAdvertisement moduleAdv;
        
        // set the services
        Hashtable services = new Hashtable();
        
        
        // core services
        
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
        mkImplAdvBuiltin(PeerGroup.refMembershipSpecID,
        "net.jxta.impl.membership.none.NoneMembershipService",
        "Reference Implementation of the None Membership service");
        services.put(PeerGroup.membershipClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(refAccessSpecID,
        "net.jxta.impl.access.always.AlwaysAccessService",
        "Reference Implementation of the Always Access service");
        services.put(PeerGroup.accessClassID, moduleAdv);
        
        
        // standard services
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID,
        "net.jxta.impl.discovery.DiscoveryServiceImpl",
        "Reference Implementation of the Discovery service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID,
        "net.jxta.impl.rendezvous.RendezVousServiceImpl",
        "Reference Implementation of the Rendezvous service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refPipeSpecID,
        "net.jxta.impl.pipe.PipeServiceImpl",
        "Reference Implementation of the Pipe service" );
        services.put(PeerGroup.pipeClassID, moduleAdv);
        
        moduleAdv =
        mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID,
        "net.jxta.impl.peer.PeerInfoServiceImpl",
        "Reference Implementation of the Peerinfo service" );
        services.put( PeerGroup.peerinfoClassID, moduleAdv );
        
        paramAdv.setServices(services);
        
        // NO Transports.
        Hashtable protos = new Hashtable();
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
        newAppAdv.setDescription("JXTA Shell Reference Implementation");
        
        // Tack in the class name
        newAppAdv.setCode( "net.jxta.impl.shell.bin.Shell.Shell" );
        
        // Put that in a new table of Apps and replace the entry in
        // paramAdv
        Hashtable newApps = new Hashtable();
        newApps.put(PeerGroup.applicationClassID, newAppAdv);
        paramAdv.setApps(newApps);
        
        // Pour our newParamAdv in implAdv
        paramElement = (TextElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
        
        implAdv.setParam(paramElement);
        
        allPurposeImplAdv = implAdv;
        
        return (ModuleImplAdvertisement) implAdv.clone();
    }
    
    /**
     *  Returns the cache manager associated with this group.
     *
     *  @return the cache manager associated with this group.
     **/
    public Cm getCacheManager() {
        return cm;
    }
}

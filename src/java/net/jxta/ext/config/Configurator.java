/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
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
 *  the documentation and/or other materialsset provided with the
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
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: Configurator.java,v 1.1 2007/01/16 11:01:36 thomas Exp $
 */
package net.jxta.ext.config;

import net.jxta.ext.config.optimizers.RelayOptimizer;
import net.jxta.ext.http.Dispatcher;
import net.jxta.ext.http.Message;

import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.document.XMLDocument;
import net.jxta.exception.ConfiguratorException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.peergroup.PlatformConfigurator;
import net.jxta.impl.protocol.HTTPAdv;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.PSEConfigAdv;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.protocol.RelayConfigAdv;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.protocol.ConfigParams;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.TransportAdvertisement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A Configurator that is capable of instantiating and persisting JXTA configuration
 * state based upon declarative {@link net.jxta.ext.config.Profile} and {@link net.jxta.impl.protocol.PlatformConfig}
 * information.
 * 
 * <p>A Configurator serves primarily as a JXTA Configuration Bean, or property
 * sheet, and implements very little attribute association logic beyond that of
 * what is required to perform fundamental configuration integrity validation.
 *
 * <p>The principal constituents of a JXTA Configurator are:
 *
 * <ul>
 *   <li>Peer Information</li>
 *   <li>Peer Transports</li>
 *   <li>Peer Services</li>
 *   <li>JXTA Network</li>
 *   <li>Configuration Extensions</li>
 * </ul>
 *
 * <p>Peer Information describes the local JXTA instance that a specified
 * Configurator instance represents. The bulk of the Peer Information
 * configuration data is optional and includes:
 *
 * <ul>
 *   <li>name</li>
 *   <li>ID</li>
 *   <li>descriptor</li>
 *   <li>JXTA Home</li>
 *   <li>Log4J trace level</li>
 *   <li>security</li>
 *   <li>root certificate</li>
 *   <li>HTTP Proxy</li>
 * </ul>
 *
 * <p>Peer Transports describe the physical {@link net.jxta.ext.config.Address} with which a Peer
 * connects with the overall JXTA Network. Transports can be specified as
 * incoming (aka server), outgoing (aka client) and both. Transport
 * implementations include:
 *
 * <ul>
 *   <li>TCP</li>
 *   <li>HTTP</li>
 * </ul>
 *
 * <p>Peer Services represent the JXTA Services that a specified Configurator
 * instance will provision, which include:
 *
 * <ul>
 *   <li>{@link net.jxta.rendezvous.RendezVousService RendezVous}</li>
 *   <li>Relay</li>
 *   <li>{@link net.jxta.impl.proxy.ProxyService Proxy}</li>
 * </ul>
 *
 * <p>The JXTA Network information specifies JXTA Network Services upon which a
 * specified Configurator instance will rely upon, including:
 *
 * <ul>
 *   <li>{@link net.jxta.rendezvous.RendezVousService RendezVous}</li>
 *   <li>Relays</li>
 * </ul>
 *
 * <p>The Configuration Extensions information includes extensible configuration
 * features that includes end user provided {@link net.jxta.ext.config.Optimizer}.
 *
 * <p>Configuration information can be derived from declarative {@link net.jxta.ext.config.Profile},
 * {@link net.jxta.impl.protocol.PlatformConfig}, or programatically via provided APIs.
 * Further, a combination of all of the above is also possible and often the most
 * practical.
 *
 * <p>The Configurator API which is predominately comprised of a series of property
 * sheet getter/setter calls in addition to PlatformConfig creation and persistence.
 * 
 * <p>A Configurator can be optionally be instantiated by from a {@link net.jxta.ext.config.Profile},
 * which is, by and large, a toolable and person-parsible serialized construct in
 * the form of XML. A {@link net.jxta.ext.config.Profile} basically drives a series of Configurator
 * API invocations.
 *
 * <p>A {@link net.jxta.ext.config.ui.Configurator Configurator UI} is also available,
 * that provides a series of reusable Configuration UI components that, in turn,
 * leverage the afore mentioned Profile constructs.
 *
 * <p>Prior to {@link net.jxta.impl.protocol.PlatformConfig} generation, the current
 * Configurator state is exercised via normalization, optimization and validation
 * processes, the later of which can throw a {@link net.jxta.exception.ConfiguratorException}.
 * The normalization process massages the configuration state by filling in missing
 * or incomplete information necessary for further processing. The optimization
 * process performs a series of pluggable optimizer exercises, as implemented
 * via the {@link net.jxta.ext.config.Optimizer} interface, that tune and adjust the normalized
 * configuration state based on the provided optimization heuristics. Lastly, the
 * validation process performs integrity checks against the current configuration
 * state just prior to {@link net.jxta.impl.protocol.PlatformConfig} generation.
 *
 * <p>The derived {@link net.jxta.impl.protocol.PlatformConfig} instance is then
 * returned to the Configurator invoker. The {@link net.jxta.ext.config.Configurator#save()} methods will perist
 * the {@link net.jxta.impl.protocol.PlatformConfig} to the file system.
 *
 * @author  james todd [gonzo at jxta dot org]
 */

public class Configurator
implements Externalizable, PlatformConfigurator {
    
    private final static String COLON = ":";
    private final static String BRACKET_OPEN = "[";
    private static final String BRACKET_CLOSE = "]";
    private final static String EMPTY_STRING = "";
    private final static int MILLISECONDS_PER_SECOND = 1000;
    private final static char NULL_CHAR = '\0';
    private final static long MAX_WAIT = 7 * 1000;
    
    private final static Logger LOG = Logger.getLogger(Configurator.class.getName());
    
    private URI home = Env.JXTA_HOME.toURI();
    private String descriptor = Default.PEER_DESCRIPTOR;
    private String peerName = null;
    private String peerDescription = null;
    private Trace trace = Trace.DEFAULT;
    private PeerID peerId = null;
    private boolean isSecurity = Default.SECURITY_IS_ENABLED;
    private String principal = null;
    private String password = null;
    private URI rootCertificateAddress = null;
    private PSEConfigAdv pse = null;
    private URI peerProxy = null;
    private PeerGroupID infrastructureGroupId = null;
    private String infrastructureGroupName = null;
    private String infrastructureGroupDescription = null;
    private URI rendezVousBootstrap = Default.RENDEZVOUS_BOOTSTRAP_ADDRESS;
    private URI relaysBootstrap = Default.RELAYS_BOOTSTRAP_ADDRESS;
    private boolean isRelaysDiscovery = Default.RELAYS_DISCOVERY_IS_ENABLED;
    private List transports = null;
    private RelayConfigAdv relayConfig = null;
    private int endpointQueueSize = Default.ENDPOINT_SERVICE_QUEUE_SIZE;
    private boolean isProxyEnabled = Default.PROXY_SERVICE_IS_ENABLED;
    private RdvConfigAdv rdvConfig = null;
    private List optimizers = null;
    
    private final Map customParams = new HashMap();
    
    /**
     * @deprecated      will be removed when {@link net.jxta.peergroup.PeerGroupFactory}
     *                  defaults to the {@link net.jxta.impl.peergroup.NullConfigurator}
     */
    
    public static void setConfigurator(Class configurator) {
        if (configurator != null &&
            PlatformConfigurator.class.isAssignableFrom(configurator)) {
            try {
                PeerGroupFactory.setConfiguratorClass(configurator);
            } catch (Exception e) {
                // xxx: needed for 1.4.x
                //rc = new IllegalArgumentException("invalid configurator", e);
                IllegalArgumentException rc = new IllegalArgumentException("invalid configurator: " +
                    e.getMessage());
                
                rc.initCause(e);
                
                throw rc;
            }
        } else {
            throw new IllegalArgumentException("invalid configurator: " +
                (configurator != null ? configurator.getName() : null));
        }
    }

    /**
     * Convenience method which constructs a {@link java.net.URI} that represents
     * the local ANY/ALL interface(s).
     *
     * @param   base    provided address
     * @return          ANY/ALL interface
     */
    
    public static URI toAllAddresses(URI base) {
        URI u = null;
        URI b = base;
        String a = Env.ALL_ADDRESSES.getHostAddress();
        
        if (b != null) {
            try {
                u = new URI(b.getScheme(), EMPTY_STRING, a, b.getPort(),
                    b.getPath(), b.getPath(), b.getFragment());
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid transformation", use);
                }
            }
        } else {
            try {
                u = new URI(Default.ANY_TCP_ADDRESS.getScheme(), EMPTY_STRING,
                    a, Default.TCP_PORT, EMPTY_STRING, EMPTY_STRING,
                    EMPTY_STRING);
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid transformation", use);
                }
            }
        }
        
        return u;
    }
    
    // xxx: we will likely continue to need a no-arg constructor
    /**
     * Default constructor.
     *
     * <p>The default JXTA persistence directory will be set to the value of:
     * 
     * <pre>
     *   System.getProperty("user.home", "/.jxta");
     * </pre>
     *
     * <p>Existing {@link net.jxta.ext.config.Profile} and {@link net.jxta.impl.protocol.PlatformConfig}
     * files will be parsed in succession in order to contextually instantiate
     * the newly create Configurator, enabling configuration update processing.
     * In the event a "profile.xml" file does not exist in the JXTA persistence
     * directory, the default {@link net.jxta.ext.config.Profile#DEFAULT} profile will be used to
     * establish a baseline configuration.
     *
     * @deprecated It's recommended to use the four params constructor
     */
    
    public Configurator() {
        this(Env.JXTA_HOME.toURI());
    }
   
    /**
     * Constructor whereby one can specify a non-default JXTA persistence directory
     * value as a {@link java.net.URI}, typically of scheme "file" with which to
     * establish the initial context.
     *
     * @param   home    JXTA persistence directory destination.
     */
    
    public Configurator(URI home) {
        this(home, null, null, null, null);
    }
    
    // xxx: ?deprecation candidate?
    /**
     * Constructor whereby one can specify a peer name and password.
     *
     * @param   name        peer name
     * @param   password    peer password
     */
    
    public Configurator(String name, String password) {
        this(Env.JXTA_HOME.toURI(), name, password);
    }
    
    // xxx: ?deprecation candidate?
    /**
     * Constructor whereby one can specify an alternative JXTA persistence directory
     * along with the peer name and password with which to establish the inital
     * context.
     *
     * @param   home        JXTA persistence directory destination
     * @param   name        peer name
     * @param   password    peer password
     */
    
    public Configurator(URI home, String name, String password) {
        this(home, name, null, name, password);
    }
   
    /**
     * Constructor whereby one can specify a peer name, principal and password
     * with which to establish the initial context.
     *
     * @deprecated Constructors specifying 'principal' are deprecated.
     *
     * @param   name        peer name
     * @param   principal   peer principal
     * @param   password    peer password
     */
    
    public Configurator(String name, String principal, String password) {
        this(Env.JXTA_HOME.toURI(), name, principal, password);
    }
    
    /**
     * Constructor whereby one can specify a peer name, description, principal
     * and password with which to establish the initial context.
     *
     * @deprecated Constructors specifying 'principal' are deprecated.
     *
     * @param   name        peer name
     * @param  description  peer description
     * @param   principal   peer principal
     * @param   password    peer password
     */
    
    public Configurator(String name, String description, String principal,
        String password) {
        this(Env.JXTA_HOME.toURI(), name, description, principal, password);
    }
    
    /**
     * Constructor whereby one can specify an alternative JXTA persistence directory,
     * peer name, description, principal and password with which to establish the
     * initial context. 
     *
     * @param  home         JXTA persistence directory destination
     * @param  name         peer name
     * @param  description  peer description
     * @param  password     peer password
     */
    
    public Configurator(URI home, String name, String description, String password) {
        this(home, name, description, name, password );        
    }
        
    /**
     * Constructor whereby one can specify an alternative JXTA persistence directory,
     * peer name, description, principal and password with which to establish the
     * initial context. 
     *
     * @deprecated Constructors specifying 'principal' are deprecated.
     *
     * @param  home         JXTA persistence directory destination
     * @param  name         peer name
     * @param  description  peer description
     * @param  principal    peer principal
     * @param  password     peer password
     */
    
    public Configurator(URI home, String name, String description,
        String principal, String password) {

        this.home = home;
        
        process((PlatformConfig)
            AdvertisementFactory.newAdvertisement(PlatformConfig.getAdvertisementType()));
        process(Profile.SEED, true);
        
        init(name, description, principal, password);
    }
        
    /**
     * Constructor whereby one can provide an alternative {@code Profile} with
     * the specified configuration preferences with which to establish the initial
     * context.
     *
     * @param   profile     the provided {@code Profile}
     */
    
    public Configurator(Profile profile) {
        this(Env.JXTA_HOME.toURI(), profile);
    }
    
    /**
     * Constructor whereby one can specify an alternative JXTA persistence directory
     * and {@code Profile} with which to establish the initial context.
     *
     * @param   home    JXTA persistence directory destination
     * @param   profile the provided {@code Profile}
     */
    
    public Configurator(URI home, Profile profile) {
        this.home = home;

        process((PlatformConfig)
            AdvertisementFactory.newAdvertisement(PlatformConfig.getAdvertisementType()));
        process(Profile.SEED, true);

        init(null, null, null, null);
        process(profile);
    }
    
    /**
     * Constructor whereby one can specify a {@link net.jxta.impl.protocol.PlatformConfig}
     * to establish the initial context.
     *
     * @param   config  {@link net.jxta.impl.protocol.PlatformConfig} configuration
     */
    
    public Configurator(PlatformConfig config) {
        this(Env.JXTA_HOME.toURI(), config);
    }

    /**
     * Constructor whereby one can specify a JXTA persistence directory and
     * {@link net.jxta.impl.protocol.PlatformConfig} to establish the initial
     * context.
     *
     * @param   home    JXTA persistence directory destination
     * @param   config  {@code net.jxta.impl.protocol.PlatformConfig} configuration
     */
    
    public Configurator(URI home, PlatformConfig config) {
        this.home = home;

        process(config);
    }
    
    /**
     * @deprecated in favor of {@link net.jxta.ext.config.Configurator#getJxtaHome()}
     */
    
    public URI getJXTAHome() {
        return getJxtaHome();
    }

    /**
     * Accessor for the JXTA persistence directory.
     *
     * @return      the configuration JXTA persistence directory
     */
    
    public URI getJxtaHome() {
        return this.home;
    }
    
    /**
     * {@inheritDoc}
     */
    
    public ConfigParams load()
    throws ConfiguratorException {
        return load(new File(new File(getJxtaHome()), Env.PLATFORM_CONFIG));
    }
    
    /**
     * {@inheritDoc}
     */
    
    public PlatformConfig load(File loadFile)
    throws ConfiguratorException {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Reading Platform Config from : " +
            loadFile.getAbsolutePath());
        }
        
        PlatformConfig pc = null;
        FileInputStream advStream = null;
        
        try {
            advStream = new FileInputStream(loadFile);
            Reader advReader = new InputStreamReader(advStream, "UTF-8");
            
            pc = (PlatformConfig)AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8,
                advReader);
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Recovered Platform Config from : " +
                    loadFile.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Platform Config not found : " +
                    loadFile.getAbsolutePath());
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to Recover '" + loadFile.getAbsolutePath() +
                    "' due to : ", e);
            }
            
            try {
                // delete that bad file.
                loadFile.delete();
            } catch (Exception ex1) {
                LOG.fatal("Could not remove bad Configuration file", ex1);
                
                throw new ConfiguratorException( "Could not remove '" +
                    loadFile.getAbsolutePath() +
                    "'. Remove it by hand before retrying", ex1);
            }
            
            throw new ConfiguratorException("Failed to Recover PlatformConfig", e);
        } finally {
            try {
                if (advStream != null) {
                    advStream.close();
                }
                
                advStream = null;
            } catch (Exception ignored) {
            }
        }
        
        return pc;
    }
            
    /**
     * Accessor to the confguration descriptor.
     *
     * @return      the configuration descriptor.
     */
    
    public String getDescriptor() {
        return this.descriptor;
    }
    
    /**
     * Specify the configuration descriptor.
     *
     * @param   descriptor  configuration descriptor
     */
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
    
    /**
     * Accessor to the peer name.
     *
     * @return      the peer name.
     */
    
    public String getName() {
        return this.peerName;
    }
    
    /**
     * Specify the peer name.
     *
     * @param   name    the peer name.
     */
    
    public void setName(String name) {
        this.peerName = (name != null && name.trim().length() > 0 ?
            name.trim() : null);
    }
    
    /**
     * Accessor to the peer description.
     *
     * @return      the peer description.
     */
    
    public String getDescription() {
        return this.peerDescription;
    }
    
    /**
     * Specify the peer description.
     *
     * @param   description     the peer description.
     */
    
    public void setDescription(String description) {
        this.peerDescription = (description != null &&
            description.trim().length() > 0 ?
                description.trim() : null);
    }
    
    /**
     * Accessor to the <a href="http://logging.apache.org/log4">jLog4J</a> level.
     *
     * @return      the log level.
     */
    
    public Trace getTrace() {
        return this.trace;
    }
    
    /**
     * Specify the <a href="http://logging.apache.org/log4j">Log4J</a> level.
     *
     * @param   trace   the log level.
     */
    
    public void setTrace(Trace trace) {
        if (trace != null) {
            this.trace = trace;
        }
    }
    
    /**
     * Accessor to the {@link net.jxta.peer.PeerID}.
     *
     * @return      the {@link net.jxta.peer.PeerID}
     */
    
    public PeerID getPeerId() {
        return this.peerId;
    }
    
    /**
     * Specify the {@link net.jxta.peer.PeerID}.
     * 
     * <p>Unspecified {@link net.jxta.peer.PeerID} will be generated by the
     * JXTA Platform upon startup.
     *
     * @param   peerId  the {@link net.jxta.peer.PeerID}
     */
    
    public void setPeerId(PeerID peerId) {
        this.peerId = peerId;
    }
    
    /**
     * Accessor to the {@link net.jxta.rendezvous.RendezVousService} enabler which
     * indicates as to whether or not the to be configured JXTA instance is to
     * provision the {@link net.jxta.rendezvous.RendezVousService}.
     *
     * @return      {@link net.jxta.rendezvous.RendezVousService} enabler
     */
    
    public boolean isRendezVous() {
        return RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS ==
            rdvConfig.getConfiguration();
    }
    
    /**
     * Specify the {@link net.jxta.rendezvous.RendezVousService} enabler.
     *
     * @param   isEnabled   {@link net.jxta.rendezvous.RendezVousService} enabler
     */
    
    public void setRendezVous(boolean isEnabled) {
        rdvConfig.setConfiguration(isEnabled ?
            RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS :
            RdvConfigAdv.RendezVousConfiguration.EDGE);
    }
    
    /**
     * Accessor to the configured RendezVous addresses in the form of {@link java.net.URI}.
     *
     * @return      RendezVous as a {@link java.util.List} of {@link java.net.URI}
     */
    
    public List getRendezVous() {
        return new ArrayList(Arrays.asList(rdvConfig.getSeedRendezvous()));
    }
    
    /**
     * Clears and resets the RendezVous address.
     *
     * @param       rendezVous              RendezVous address
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void setRendezVous(URI rendezVous)
    throws ConfiguratorException {
        setRendezVous(Collections.singletonList(rendezVous));
    }
    
    /**
     * Clears and resets the RendezVous addresses.
     *
     * @param      rendezVous               RendezVous addresses 
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfigurationException}
     *                                      chained list of configuration errors
     */
    
    public void setRendezVous(List rendezVous)
    throws ConfiguratorException {
        this.rdvConfig.clearSeedRendezvous();
        addRendezVous(rendezVous);
    }
    
    /**
     * Add a RendezVous address.
     *
     * @param       rendezVous              RendezVous address
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void addRendezVous(URI rendezVous)
    throws ConfiguratorException {
        addRendezVous(Collections.singletonList(rendezVous));
    }
    
    /**
     * Adds a {@link java.util.List} of RendezVous addresses.
     *
     * @param       rendezVous              RendezVousService addresses
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void addRendezVous(List rendezVous)
    throws ConfiguratorException {
        for (Iterator r = rendezVous.iterator(); r.hasNext();) {
            URI u = (URI)r.next();
            
            if (Util.validateAddress(u, false).trim().length() == 0) {
                rdvConfig.addSeedRendezvous(u);
            } else {
                throw new ConfiguratorException("invalid address: " + u);
            }
        }
    }
    
    /**
     * Remove a RendezVous address.
     *
     * @param   rendezVous  RendezVousService address
     * @return              removed RendezVous address
     */
    
    public URI removeRendezVous(URI rendezVous) {
        return rdvConfig.removeSeedRendezvous(rendezVous) ? rendezVous : null;
    }
    
    /**
     * Remove all RendezVous addresses.
     */
    
    public void clearRendezVous() {
        rdvConfig.clearSeedRendezvous();
    }
    
    /**
     * Accessor to the RendezVous AutoStart enabler.
     *
     * <p>The AutoStart feature allows the JXTA Peer to opt to variably provision
     * the {@link net.jxta.rendezvous.RendezVousService} in the event no other,
     * or not enough, RendezVous are found within the specified time interval.
     *
     * @deprecated  use {@link net.jxta.ext.config.Configurator#getRendezVousAutoStart()}
     * @return      the RendezVous AutoStart value
     */
    
    public boolean isRendezVousAutoStart() {
        return (rdvConfig.getAutoRendezvousCheckInterval() != 0);
    }
    
    /**
     * Accessor to the RendezVous AutoStart value measured in milliseconds.
     *
     * <p>The RendezVous AutoStart feature allows the JXTA instance to opt to
     * variably provision the {@link net.jxta.rendezvous.RendezVousService}
     * in the event no other, or not enough, RendezVous are found within the
     * specified time interval.
     *
     * @return      RendezVous AutoStart value in milliseconds
     */
    
    public long getRendezVousAutoStart() {
        return rdvConfig.getAutoRendezvousCheckInterval();
    }
    
    /**
     * Specifies the RendezVous AutoStart value in milliseconds.
     *
     * @param   autoStart   RendezVous AutoStart value
     */
    
    public void setRendezVousAutoStart(long autoStart) {
        rdvConfig.setAutoRendezvousCheckInterval(autoStart);
    }
    
    /**
     * Accessor to the Relay Service enabler which indicates as to whether
     * or not the to be configured instance is to provision the Relay Service.
     *
     * @return      Relay enabler
     */
    
    public boolean isRelay() {
        return this.relayConfig.isClientEnabled() ||
	    this.relayConfig.isServerEnabled();
    }
    
    /**
     * Specify the Relay enabler.
     *
     * @param   isEnabled    Relay enabler
     */
    
    public void setRelay(boolean isEnabled) {
//         throw new UnsupportedOperationException("this doesn't work");
    }
    
    /**
     * Accessor to the configured Relay addresses in the form of {@link java.net.URI}.
     *
     * @return      Relays as a {@link java.util.List} of {@link java.net.URI}
     */
    
    public List getRelays() {
        List allSeeds = Arrays.asList( this.relayConfig.getSeedRelays() );
        List result = new ArrayList(allSeeds.size());
        
        Iterator eachSeed = allSeeds.iterator();
        while( eachSeed.hasNext() ) {
            try {
            result.add( new URI( eachSeed.next().toString() ));
            } catch( URISyntaxException ignored ) {
                ;
            }
        }
        
        return result;
    }
    
    /**
     * Clears and resets the Relay address.
     *
     * @param       relay                   Relay address
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void setRelay(URI relay)
    throws ConfiguratorException {
        this.relayConfig.clearSeedRelays();
        this.relayConfig.addSeedRelay(relay.toString());
    }
    
    /**
     * Clears and resets the Relay addresses.
     *
     * @param      relays                   Relay addresses 
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfigurationException}
     *                                      chained list of configuration errors
     */
    
    public void setRelays(List relays)
    throws ConfiguratorException {
        this.relayConfig.clearSeedRelays();
        
        addRelays(relays);
    }
    
    /**
     * Add a Relay address.
     *
     * @param       relay                   Relay address
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void addRelay(URI relay)
    throws ConfiguratorException {
        try {
            this.relayConfig.addSeedRelay(relay.toString());
        } catch (Exception all) {
            throw new ConfiguratorException("Bad relay", all);
        }
    }
    
    /**
     * Adds a {@link java.util.List} of Relay addresses.
     *
     * @param       relays                  Relay addresses
     * @throws      ConfiguratorException   {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void addRelays(List relays)
    throws ConfiguratorException {
        Iterator eachRelay = relays.iterator();
        while( eachRelay.hasNext() ) {
            addRelay( (URI) eachRelay.next() );
        }
    }
    
    // xxx: ?we can't remove a relay?
    /**
     * Remove a Relay address.
     *
     * @param   relay       Relay address
     * @return              removed Relay address
     */
    
    public URI removeRelay(URI relay) {
        throw new UnsupportedOperationException("this doesn't work");
    }
    
    /**
     * Remove all Relay addresses.
     */
     
    public void clearRelays() {
        this.relayConfig.clearSeedRelays();
    }
    
    /**
     * Accessor to Relay incoming attribute.
     *
     * <p>An incoming attribute indicates the associated service accepts inbound
     * communications.
     *
     * @return      the Relay incoming attribute
     */
    
    public boolean isRelayIncoming() {
        return this.relayConfig.isServerEnabled();
    }
    
    /**
     * Specify the Relay incoming attribute.
     *
     * @param   isEnabled   Relay incoming attribute
     */
    
    public void setRelayIncoming(boolean isEnabled) {
        this.relayConfig.setServerEnabled( isEnabled );
    }
    
    /**
     * Accessor to the Relay incoming maximum connection attribute.
     *
     * <p>A incoming maximum specifies the amount of maximum incoming connections
     * are allowed.
     *
     * @return      the Relay maximum incoming connection attribute
     */
    
    public int getRelayIncomingMaximum() {
        return this.relayConfig.getMaxClients( );
    }
    
    /**
     * Specify the Relay maximum incoming connection attribute.
     *
     * @param   maximumIncoming     Relay maximum incoming attribute
     */
    
    public void setRelayIncomingMaximum(int maximumIncoming) {
        this.relayConfig.setMaxClients( maximumIncoming );
    }
    
    /**
     * Accessor to the Relay incoming lease attribute in milliseconds.
     *
     * <p>A incoming lease specifies the time, in milliseconds, with which Relay
     * leases are held.
     *
     * @return      the Relay incoming lease attribute in milliseconds.
     */
    
    public long getRelayIncomingLease() {
        return this.relayConfig.getServerLeaseDuration();
    }
    
    /**
     * Specify the Relay incoming lease attribute in milliseconds.
     *
     * @param   incomingLease   the Relay incoming lease attribute in milliseconds.
     */
    
    public void setRelayIncomingLease(long incomingLease) {
        this.relayConfig.setServerLeaseDuration( incomingLease );
    }
    
    /**
     * Accessor to the Relay outgoing attribute.
     *
     * <p>An outgoing attribute indicates the associated service initiates outbound
     * communications.
     *
     * @return      Relay outgoing attribute
     */
    
    public boolean isRelayOutgoing() {
        return this.relayConfig.isClientEnabled();
    }
    
    /**
     * Specify the Relay outgoing attribute.
     *
     * @param   isEnabled   Relay outgoing attribute
     */
    
    public void setRelayOutgoing(boolean isEnabled) {
        this.relayConfig.setClientEnabled( isEnabled );
    }
    
    /**
     * Accessor to the Relay outgoing maximum connection attribute.
     *
     * <p>A outgoing maximum specifies the amount of maximum outgoing connections
     * are allowed.
     *
     * @return      the Relay maximum outgoing connection attribute
     */
    
    public int getRelayOutgoingMaximum() {
        return this.relayConfig.getMaxRelays();
    }
    
    /**
     * Specify the Relay maximum outgoing connection attribute.
     *
     * @param   maximumOutgoing     Relay maximum outgoing attribute
     */
    
    public void setRelayOutgoingMaximum(int maximumOutgoing) {
        this.relayConfig.setMaxRelays( maximumOutgoing );
    }
    
    /**
     * Accessor to the Relay outgoing lease attribute in milliseconds.
     *
     * <p>A outgoing lease specifies the time, in milliseconds, with which Relay
     * leases are held.
     *
     * @return      the Relay outgoing lease attribute in milliseconds.
     */
    
    public long getRelayOutgoingLease() {
        return this.relayConfig.getClientLeaseDuration();
    }
    
    /**
     * Specify the Relay outgoing lease attribute in milliseconds.
     *
     * @param   outgoingLease   the Relay outgoing lease attribute in milliseconds.
     */
    
    public void setRelayOutgoingLease(long outgoingLease) {
        this.relayConfig.setClientLeaseDuration( outgoingLease );
    }
    
    /**
     * Accessor to the Relay queue size.
     *
     * <p>A queue size specifies the overall message buffer size when exceeded
     * messages are dropped.
     *
     * @return      Relay queue size
     */
    
    public int getRelayQueueSize() {
        return this.relayConfig.getClientMessageQueueSize();
    } 
    
    /**
     * Specify the Relay queue size.
     *
     * @param   queueSize   the Relay queue size
     */
    
    public void setRelayQueueSize(int queueSize) {
        this.relayConfig.setClientMessageQueueSize( queueSize );
    }
    
    /**
     * Accessor to the {@link net.jxta.ext.config.Transport}.
     *
     * A {@link net.jxta.ext.config.Transport} is a networking binding.
     *
     * @return      {@link java.util.List} of {@link net.jxta.ext.config.Transport}
     */
    
    public List getTransports() {
        return this.transports != null ?
            (List)((ArrayList)this.transports).clone() :
            Collections.EMPTY_LIST;
    }
    
    /**
     * Clear and resets the {@link net.jxta.ext.config.Transport} value.
     *
     * @param   transport   {@link net.jxta.ext.config.Transport} value.
     */
    
    public void setTransport(Transport transport) {
        List t = new ArrayList();
        
        t.add(transport);
        
        setTransports(t);
    }
    
    /**
     * Clears and resets the {@link net.jxta.ext.config.Transport}.
     *
     * @param   transports      {@link net.jxta.ext.config.Transport} 
     */
    
    public void setTransports(List transports) {
        if (this.transports != null) {
            this.transports.clear();
        }
        
        addTransports(transports);
    }
    
    /**
     * Add a {@link net.jxta.ext.config.Transport}.
     *
     * @param   transport       {@link net.jxta.ext.config.Transport}
     */
    
    public void addTransport(Transport transport) {
        addTransports(Collections.singletonList(transport));
    }
    
    /**
     * Adds a {@link java.util.List} of {@link net.jxta.ext.config.Transport}.
     *
     * @param   transports      {@link net.jxta.ext.config.Transport}
     */
    
    public void addTransports(List transports) {
        for (Iterator i =  transports.iterator();
            i.hasNext(); ) {
            Transport t = (Transport)i.next();
            
            if (t != null &&
                (this.transports == null ||
                ! this.transports.contains(t))) {
                if (this.transports == null) {
                    this.transports = new ArrayList();
                }
                
                this.transports.add(t);
            }
        }
    }
    
    /**
     * Remove a {@link net.jxta.ext.config.Transport}.
     *
     * @param   transport   {@link net.jxta.ext.config.Transport}
     * @return              removed {@link net.jxta.ext.config.Transport}
     */
    
    public Transport removeTransport(Transport transport) {
        Object o = null;
        
        if (this.transports != null) {
            int i = this.transports.indexOf(transport);
            
            if (i > -1) {
                o = this.transports.remove(i);
                
                if (this.transports.size() == 0) {
                    this.transports = null;
                }
            }
        }
        
        return (Transport)o;
    }
    
    /**
     * Remove all {@link net.jxta.ext.config.Transport}.
     */
    
    public void clearTransports() {
        if (this.transports != null) {
            this.transports.clear();
            
            this.transports = null;
        }
    }
    
    // xxx: ?is this needed?
    /**
     * Accessor to the security enabler.
     *
     * <p>The security enabler specifies whether or not security is enabled. 
     *
     * @return      security enabler
     */
    
    public boolean isSecurity() {
        return this.isSecurity;
    }

    /**
     * Specify the security enabler value.
     *
     * @param   isEnabled    the security enabler value
     */
    
    public void setSecurity(boolean isEnabled) {
        this.isSecurity = isEnabled;
    }

    /**
     * Accessor to the principal attribute.
     *
     * <p>The principal is used to specify the associated {@link java.net.cert.Certificate}
     * principal.
     *
     * @return      the configuration principal
     */
    
    public String getPrincipal() {
        return this.principal;
    }
    
    /**
     * Specify the configuration principal and password.
     *
     * @param   principal   configuration principal
     * @param   password    configuration password
     */   
    public void setSecurity(String principal, String password) {
        this.principal = (principal != null &&
            principal.trim().length() > 0 ? principal.trim() : null);
        this.password = (password != null &&
            password.trim().length() > 0 ? password.trim() : null);
    }
    
    /**
     * Accessor to the Root Certificate address.
     *
     * <p>The Root Certificate is used to sign all subsequent certificates.
     *
     * @return      the Root Certificate address
     */
    
    public URI getRootCertificateAddress() {
        return this.rootCertificateAddress;
    }
    
    /**
     * Specify the Root Certificate address.
     *
     * @param   rootCertificateAddress  the Root Certificate address
     */
    
    public void setRootCertificateAddress(URI rootCertificateAddress) {
        this.rootCertificateAddress = rootCertificateAddress;
    }
    
    /**
     * Accessor to the Root Certificate.
     *
     * <p>The Root Certificate is used to sign all subsequent cerficiates.
     *
     * @return      the Root Certificate
     */
    
    public Certificate getRootCertificate() {
        return this.pse.getCertificate();
    }
    
    /**
     * Accessor to the Base64 encoded Root Certificate.
     *
     * @return      Base64 encoded Root Certificate
     */
    
    public String getRootCertificateBase64() {
        String s = null;
        
        try {
            s = this.pse.getCert();
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("can't get root cert");
            }
        }
        
        return s;
    }
    
    /**
     * Specify the Root Certificate.
     *
     * @param   rootCertificate     the Root Certificate
     */
    
    public void setRootCertificate(Certificate rootCertificate) {
        this.pse.setCertificate((X509Certificate) rootCertificate);
    }
    
    /**
     * Specify the Root Certificate in Base64 encoded format.
     *
     * @param   rootCertificate     the Root Certificate in Base64 encoded format.
     */
    
    public void setRootCertificateBase64(String rootCertificate) {
        if (rootCertificate != null) {
            this.pse.setCert(rootCertificate);
        }
    }
    
    /**
     * Accessor to the {@link net.jxta.ext.config.ProxyAddress}.
     *
     * <p>A {@link net.jxta.ext.config.ProxyAddress} is used to optionally negotiate HTTP communications.
     *
     * @return      {@link net.jxta.ext.config.ProxyAddress} address as a {link java.net.URI}
     */
    
    public URI getPeerProxyAddress() {
        return this.peerProxy;
    }
    
    /**
     * Specify a {@link net.jxta.ext.config.ProxyAddress}.
     *
     * @param  peerProxyAddress             the {@link net.jxta.ext.config.ProxyAddress} address
     * @throws     ConfiguratorException    if the address scheme is not "HTTP"
     *                                      or the port is invalid
     */
    
    public void setPeerProxyAddress(URI peerProxyAddress)
    throws ConfiguratorException {
        if (Util.validateAddress(peerProxyAddress, true).trim().length() == 0) {
            this.peerProxy = peerProxyAddress;
        } else {
            throw new ConfiguratorException("invalid proxy",
                new IllegalArgumentException("invalid proxy uri: " +
                    peerProxyAddress));
        }
    }
    
    /**
     * Accessor to the Endpoint outgoing queue size.
     *
     * <p>A queue size specifies the overall message buffer size when exceeded
     * messages are dropped.
     *
     * @return      Endpoint queue size
     */
    
    public int getEndpointOutgoingQueueSize() {
        return this.endpointQueueSize;
    }
    
    /**
     * Specify the outgoing Endpoint queue size.
     *
     * @param   size   the outgoing Endpoint queue size
     */
    
    public void setEndpointOutgoingQueueSize(int size) {
        this.endpointQueueSize = size;
    }
    
    /**
     * Accessor to the {@link net.jxta.impl.proxy.ProxyService} enabler.
     *
     * <p>The {@link net.jxta.impl.proxy.ProxyService} enables gatewaying primarily
     * for use with limited devices.
     *
     * @return      the {@link net.jxta.impl.proxy.ProxyService}
     */
    
    public boolean isProxy() {
        return this.isProxyEnabled;
    }
    
    /**
     * Specify the {@link net.jxta.impl.proxy.ProxyService} enabler.
     *
     * @param  isEnabled    the {@link net.jxta.impl.proxy.ProxyService} enabler
     */
    
    public void setProxy(boolean isEnabled) {
        this.isProxyEnabled = isEnabled;
    }
    
    /**
     * Accessor to the configuration Infrastructure {@link net.jxta.peergroup.PeerGroupID}.
     *
     * <p>A {@link net.jxta.peergroup.PeerGroupID} used to specify unique networks.
     *
     * @return      {@link net.jxta.peergroup.PeerGroupID}
     */ 

    public PeerGroupID getInfrastructurePeerGroupId() {
        return this.infrastructureGroupId;
    }

    /**
     * Specify the Infrastructure {@link net.jxta.peergroup.PeerGroupID}.
     *
     * @param   pgid  {@link net.jxta.peergroup.PeerGroupID}
     */

    public void setInfrastructurePeerGroupId(PeerGroupID pgid) {
        this.infrastructureGroupId = pgid;
    }

    /**
     * Accessor to the Infrastructure {@link net.jxta.peergroup.PeerGroup} name.
     *
     * <p>A Infrastructure PeerGroup name specifies a toolable meta value.
     *
     * @return      PeerGroup name
     */

    public String getInfrastructurePeerGroupName() {
        return this.infrastructureGroupName;
    }

    /**
     * Specify the Infrastructure {@link net.jxta.peergroup.PeerGroup} name.
     *
     * @param   name    PeerGroup name
     */

    public void setInfrastructurePeerGroupName(String name) {
        name = name != null ? name.trim() : name;
        
        this.infrastructureGroupName = name != null && name.length() > 0 ?
            name : null;
    }

    /**
     * Accessor to the Infrastructure {@link net.jxta.peergroup.PeerGroup} description.
     *
     * <p>A Infrastructure PeerGroup description specifies a toolable meta value.
     *
     * @return      PeerGroup description
     */

    public String getInfrastructurePeerGroupDescription() {
        return this.infrastructureGroupDescription;
    }

    /**
     * Specify the Infrastructure {@link net.jxta.peergroup.PeerGroup} description.
     *
     * @param   description    PeerGroup description
     */

    public void setInfrastructurePeerGroupDescription(String description) {
        description = description != null ? description.trim() : description;
        
        this.infrastructureGroupDescription = description != null &&
            description.length() > 0 ? description : null;
    }

    /**
     * Accessor to the RendezVous Bootstrap address.
     *
     * <p>A RendezVous Bootstrap address is used to establish initial, aka
     * "seeding," RendezVous addresses, in the form of {@link java.net.URI}.
     *
     * @return      the RendezVous Bootstrap address in the form of a
     *              {@link java.net.URI}
     */
    
    public URI getRendezVousBootstrapAddress() {
        //return this.rendezVousBootstrap;
        URI[] u = this.rdvConfig.getSeedingURIs();
        
        return (u.length > 0 ? u[0] : null);
    }
    
    /**
     * Specify a RendezVous Bootstrap address.
     *
     * @param  rendezVousBootstrapAddress   the RendezVous bootstrap address
     * @throws     ConfiguratorException    {@link java.net.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void setRendezVousBootstrapAddress(URI rendezVousBootstrapAddress)
    throws ConfiguratorException {
        if (rendezVousBootstrapAddress != null &&
            Util.validateAddress(rendezVousBootstrapAddress, -1).trim().length() > 0) {
            throw new ConfiguratorException("invalid address: " +
                rendezVousBootstrapAddress);
        }
        
        //this.rendezVousBootstrap = rendezVousBootstrapAddress;
	this.rdvConfig.clearSeedingURIs();

        if (rendezVousBootstrapAddress != null) {
	    this.rdvConfig.addSeedingURI(rendezVousBootstrapAddress);
        }
    }
    
    /**
     * Accessor to the RendezVous discovery attribute.
     *
     * <p>The RendezVous discovery allows the instance to  discover and utilize
     * happended upon {@link net.jxta.rendezvous.RendezVousSerivce} if true,
     * and not utilize discovered {@link net.jxta.rendezvous.RendezVousService}
     * if false.
     *
     * @return      the RendezVous discovery attribute.
     */
    
    public boolean isRendezVousDiscovery() {
        return ! rdvConfig.getUseOnlySeeds();
    }
    
    /**
     * Specify the {@code RendezVous} serivce discovery attribute.
     *
     * @param   discover    {@code RendezVous} discovery attribute
     */
    
    public void setRendezVousDiscovery(boolean discover) {
        rdvConfig.setUseOnlySeeds(! discover);
    }
    
    /**
     * Accessor to the Relays Bootstrap address.
     *
     * <p>A Relays Bootstrap address is used to establish initial, aka
     * "seeding," Relays addresses, in the form of {@link java.net.URI}.
     *
     * @return      the Relay Bootstrap address in the form of a
     *              {@link java.net.URI}
     */
    
    public URI getRelaysBootstrapAddress() {
        //return this.relaysBootstrap;
        URI[] u = this.relayConfig.getSeedingURIs();
        
        return (u.length > 0 ? u[0] : null);
    }
    
    /**
     * Specify a Relays Bootstrap address.
     *
     * @param  relaysBootstrapAddress       the Relay Bootstrap address
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public void setRelaysBootstrapAddress(URI relaysBootstrapAddress)
    throws ConfiguratorException {
        if (relaysBootstrapAddress != null &&
            Util.validateAddress(relaysBootstrapAddress, -1).trim().length() > 0) {
            throw new ConfiguratorException("invalid address: " +
                relaysBootstrapAddress);
        }
        
        //this.relaysBootstrap = relaysBootstrapAddress;
	this.relayConfig.clearSeedingURIs();

        if (relaysBootstrapAddress != null) {
	    this.relayConfig.addSeedingURI(relaysBootstrapAddress);
        }
    }
    
    /**
     * The Relay discovery allows the instance to  discover and utilize
     * happended upon if true, and not utilize discovered Relay Service if false.
     *
     * @return      the Relay discovery attribute.
     */
    
    public boolean isRelaysDiscovery() {
        return this.isRelaysDiscovery;
    }
    
    /**
     * Specify the {@code Relay} serivce discovery attribute.
     *
     * @param   isEnabled        {@code Relay} discovery attribute
     */
    
    public void setRelaysDiscovery(boolean isEnabled) {
        this.isRelaysDiscovery = isEnabled;
    }
    
    /**
     * Creates a {@link net.jxta.impl.protocol.PlatformConfig} instance that is
     * representative of the current Configurator state.
     *
     * <p>Prior to {@link net.jxta.impl.protocol.PlatformConfig} generation the
     * current Configurator state is normalized, optimized and validated. The
     * normalization process simply adds missing data per backing defaults. The
     * optimization process proceeds to excercise the normalized configuration
     * state against the series of registered {@code Optimizers} thereby providing
     * applications the opportunity to specialize the configuration process.
     * Lastly, the configuration state is validated against a set of rules which
     * ensure {@link net.jxta.impl.protocol.PlatformConfig} integrity.
     * 
     * <p>In the event the configuration information as specified is invalid or
     * the representative {@link net.jxta.impl.protocol.PlatformConfig} can't be
     * generated, a {@link net.jxta.exception.ConfiguratorException} is thrown
     * which, in turn, includes a chained exception list from which one can
     * interogate to work towards configuration resolution. 
     *
     * @return                              generated {@link net.jxta.impl.protocol.PlatformConfig}
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfiguratorException}
     *                                      chained list of configuration errors
     */
    
    public PlatformConfig getPlatformConfig()
    throws ConfiguratorException {
        normalize();
        optimize();
        validate();
        
        return constructPlatformConfig();
    }

    /**
     * Accessor for {@link net.jxta.protocol.ConfigParams}.
     *
     * @return          the corresponding {@link net.jxta.protocol.ConfigParams}
     */
    
    public ConfigParams getConfigParams()
    throws ConfiguratorException {
        return getPlatformConfig();
    }
    
    /**
     * Specify the configuration state per the provided {@link net.jxta.impl.protocol.PlatformConfig}.
     *
     * @param   pc  the provided {@link net.jxta.impl.protocol.PlatformConfig}
     */
    
    public void setPlatformConfig(PlatformConfig pc) {
        process(pc);
    }
    
    /**
     * Specify the configuration state per the provided {@link net.jxta.protocol.ConfigParams}.
     *
     * @param   pc  the provided {@link net.jxta.protocol.ConfigParams}
     */
    
    public void setConfigParams(ConfigParams pc) {
        setPlatformConfig( (PlatformConfig) pc);
    }
    
    /**
     * Retrieves the contents of the provided {@link java.net.URI} bootstrap
     * resource in the form of a {@link java.util.List} of {@link java.util.URI}.
     *
     * @param  u                bootstrap resource address
     * @return                  list of addresses
     * @throws     IOException  can't establish a connection
     */
    
    public List fetchBootstrapAddresses(URI u)
    throws IOException {
        List r = null;
        URL url = null;
        
        if (u != null) {
            try {
                url = u.toURL();
            } catch (MalformedURLException mue) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid location", mue);
                }
            }
        }
        
        if (url != null) {
            Message results = null;
            
            try {
                results = new Dispatcher(url, MAX_WAIT).dispatch();
            } catch (IOException ioe) {}
            
            String s = (results != null ? results.getBody() : null);
            
            if (s != null) {
                r = new ArrayList();
                
                StreamTokenizer st = new StreamTokenizer(new StringReader(s));
                
                st.wordChars(' ', '~');
                st.eolIsSignificant(true);
                
                try {
                    while (st.nextToken() != StreamTokenizer.TT_EOF) {
                        switch (st.ttype) {
                            case StreamTokenizer.TT_WORD:
                                try {
                                    r.add(new URI(st.sval.trim()));
                                } catch (URISyntaxException use) {
                                    if (LOG.isEnabledFor(Level.ERROR)) {
                                        LOG.error("invalid uri", use);
                                    }
                                }
                                
                                break;
                            default:
                        }
                    }
                } catch (IOException ioe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid uri tokenizer", ioe);
                    }
                }
            }
        }
        
        return r != null ? r : Collections.EMPTY_LIST;
    }
    
    /**
     * Accessor to the {@link net.jxta.ext.config.Optimizer}.
     *
     * @return          (@link net.jxta.ext.config.Optimizer} as a {@link java.util.List}
     */
    
    public Iterator getOptimizers() {
        return this.optimizers != null ?
            this.optimizers.iterator() : Collections.EMPTY_LIST.iterator();
    }

    /**
     * Adds an {@link net.jxta.ext.config.Optimizer}.
     *
     * @param       optimizer              {@link net.jxta.ext.config.Optimizer}
     */
    
    public void addOptimizer(Optimizer optimizer) {
        if (optimizer != null) {
            if (this.optimizers == null) {
                this.optimizers = new ArrayList();
            }
            
            this.optimizers.add(optimizer);
        }
    }
    
    /**
     * Adds a {@link java.util.List} of {@link net.jxta.ext.config.Optimizer}.
     *
     * @param       optimizers              {@link net.jxta.ext.config.Optimizer}
     */
    
    public void addOptimizers(List optimizers) {
        for (Iterator o = optimizers != null ?
            optimizers.iterator() : Collections.EMPTY_LIST.iterator();
        o.hasNext(); ) {
            addOptimizer((Optimizer)o.next());
        }
    }
    
    /**
     * Remove an {@code Optimizer}.
     *
     * @param   optimizer   {@code Optimizer} to be removed
     * @return              removed {@code Optimizer}
     */
    
    public Optimizer removeOptimizer(Optimizer optimizer) {
        Optimizer o = null;
        
        if (this.optimizers != null) {
            int i = this.optimizers.indexOf(optimizer);
            
            if (i > -1) {
                o = (Optimizer)this.optimizers.remove(i);
            }
        }
        
        return o;
    }
    
    /**
     * Remove all {@link net.jxta.ext.config.Optimizer}.
     */
    
    public void clearOptimizers() {
        if (this.optimizers != null) {
            this.optimizers.clear();
        }
    }
    
    /**
     * Accessor to the reconfiguration attribute.
     *
     * Provides a mechanism to trigger reconfiguration processes.
     *
     * @return      the reconfiguration state
     */
    
    public boolean isReconfigure() {
        File f = new File(new File(getJxtaHome()), Env.RECONFIGURE);
        
        return f.exists();
    }
    
    /**
     * Specify the reconfiguration attribute.
     *
     * @param  reconfigure      the reconfiguration value.
     */
    
    public void setReconfigure(boolean reconfigure) {
        File f = new File(new File(getJxtaHome()), Env.RECONFIGURE);
        
        try {
            if (reconfigure) {
                new FileOutputStream(f).close();
            } else {
                f.delete();
            }
        } catch (FileNotFoundException fnfe) {
            LOG.info("can't access reconfigure file: " + f);
        } catch (IOException ioe) {
            LOG.info("can't access reconfigure file: " + f);
        }
    }
    
    /**
     * Persist the representative {@link net.jxta.impl.protocol.PlatformConfig}
     * to the configured JXTA persistence directory.
     *
     * A {@link net.jxta.exception.ConfiguratorException} will be thrown in the
     * event the {@link net.jxta.impl.protocol.PlatformConfig} can't be generated
     * or it is not possible to write to the specified {@link java.io.File}.
     *
     * @return                              success indicator
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfigurationException}
     *                                      chained list of configuration errors
     */
    
    public boolean save()
    throws ConfiguratorException {
        File f = null;
        URI u = getJxtaHome();

        try {
            f = new File(Conversion.toFile(u), Env.PLATFORM_CONFIG);
        } catch (ConversionException ce) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("can't convert uri to file: " + u, ce);
            }
        }

        return save(f);
    }
    
    /**
     * Persist the representative {@link net.jxta.impl.protocol.PlatformConfig}
     * to the provided {@link java.io.File}.
     *
     * If the provided {@link java.io.File} is a directory then the serialized
     * {@link net.jxta.impl.protocol.PlatformConfig} will reside in a file named
     * "PlatformConfig" within the specified JXTA persistence directory. If, on
     * the other hand, the {@link java.io.File} is a file, the serialzed
     * {@link net.jxta.impl.protocol.PlatformConfig} will reside in the named
     * {@link java.io.File}.
     *
     * A {@link net.jxta.exception.ConfiguratorException} will be thrown in the
     * event the {@link net.jxta.impl.protocol.PlatformConfig} can't be generated
     * or it is not possible to write to the specified {@link java.io.File}.
     *
     * @param       platformConfig          destination file or directory
     * @return                              success indicator
     * @throws     ConfiguratorException    {@link net.jxta.exception.ConfigurationException}
     *                                      chained list of configuration errors
     */
    
    public boolean save(File platformConfig)
    throws ConfiguratorException {
        ConfigParams pc = getPlatformConfig();
        boolean isSaved = false;
        
        if (platformConfig.getParent() == null) {
            platformConfig = new File(new File(getJxtaHome()), platformConfig.getName());
        }
        
        if (platformConfig.isDirectory()) {
            platformConfig = new File(platformConfig, Env.PLATFORM_CONFIG);
        }
        
        platformConfig.getParentFile().mkdirs();
        
        FileOutputStream os = null;
        String s = null;
        Exception e = null;
        
        try {
            pc.getDocument(MimeMediaType.XMLUTF8).
                sendToStream(os = new FileOutputStream(platformConfig));
            
            isSaved = true;
        } catch (FileNotFoundException fnfe) {
            s = "can't open config file";
            e = fnfe;
            
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("can't open config file", fnfe);
            }
        } catch (IOException ioe) {
            s = "can't write config file";
            e = ioe;
            
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("can't write config file", ioe);
            }
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ioe) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("can't close config file", ioe);
                }
            }
        }
        
        if (s != null ||
            e != null) {
            throw new ConfiguratorException(s, e);
        }
        
        return isSaved;
    }
    
    /**
     * @inheritDoc
     */
    
    public void writeExternal(ObjectOutput out) {
        // xxx: implement
    }
    
    /**
     * @inheritDoc
     */
    
    public void readExternal(ObjectInput in) {
        // xxx: implement
    }
    
    /**
     * Accessor to the password information.
     *
     * @return      the password
     */
    
    protected String getPassword() {
        return this.password;
    }
    
    /**
     * Accessor to the local host information.
     *
     * @return      the local host in the form of a {@code String}
     */
    
    protected String getLocalHost() {
        return Util.getLocalHost();
    }
    
    /**
     * Compares the current transports against the provide scheme.
     *
     * @param   scheme      the scheme comparitor
     * @return  boolean     comparison test results
     */
    
    protected boolean transportsContainsScheme(String scheme) {
        boolean contains = false;
        
        if (scheme != null) {
            for (Iterator i = getTransports().iterator();
            i.hasNext() && ! contains; ) {
                if (((Transport)i.next()).getScheme().equalsIgnoreCase(scheme)) {
                    contains = true;
                }
            }
        }
        
        return contains;
    }
    
    private void init(String name, String description, String principal,
        String password) {
        PlatformConfig pc = null;
        
        try {
            pc = (PlatformConfig)load();
        } catch (ConfiguratorException ce) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Bad existing configuration", ce);
            }
        }
       
        File cf = new File(new File(getJxtaHome()), Env.PROFILE);
        Profile p = Profile.DEFAULT;
        
        if (pc == null &&
            cf.exists()) {
            
            try {
                p = new Profile(cf.toURL());
            } catch (MalformedURLException mue) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid config", mue);
                }
            }
        }
        
        if (pc != null) {
            process(pc);
        } else if (p != null) {
            process(p);
        }
        
        description = (description != null &&
        description.trim().length() > 0) ?
            description.trim() : Default.PEER_DESCRIPTION;
        
        if (name != null &&
            name.trim().length() > 0) {
            setName(name);
        }
        
        if (description.trim().length() > 0) {
            setDescription(description);
        }
        
        setSecurity(principal, password);
        setTrace(getTrace());
    }
    
    private void process(PlatformConfig platformConfig) {
        setName(platformConfig != null ? platformConfig.getName() : null);
        setDescription(platformConfig != null ?
            platformConfig.getDescription() : null);
        setPeerId(platformConfig != null ? platformConfig.getPeerID() : null);
        setTrace(platformConfig != null ?
            Trace.get(platformConfig.getDebugLevel()) :
            Trace.DEFAULT);
        
        setPse(platformConfig);
        setRdv(platformConfig);
        setRelay(platformConfig);
        setTcp(platformConfig);
        setHttp(platformConfig);
        setEndpoint(platformConfig);
        setProxy(platformConfig);
        setCustomParams(platformConfig);
    }
    
    private void setPse(ConfigParams pc) {
        StructuredDocument d = pc != null ?
            pc.getServiceParam(ModuleId.MEMBERSHIP.getId()) : null;
        
        this.pse = (PSEConfigAdv)(d != null ?
            AdvertisementFactory.newAdvertisement((TextElement)d) :
            AdvertisementFactory.newAdvertisement(PSEConfigAdv.getAdvertisementType()));
    }
    
    private void setRdv(ConfigParams pc) {
        XMLDocument d = (XMLDocument) (pc != null ?
            pc.getServiceParam(ModuleId.RENDEZVOUS.getId()) : null);
        
        try {
            if ( null != d ) {
                // XXX 20041027 backwards compatibility
                d.addAttribute( "type", RdvConfigAdv.getAdvertisementType() );
                this.rdvConfig = (RdvConfigAdv) AdvertisementFactory.newAdvertisement(d);
                d = null;
            }
        } catch ( NoSuchElementException notRdvConfigAdv ) {
        }
        
        if (null == this.rdvConfig) {
            this.rdvConfig = (RdvConfigAdv)AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType());
            
            rdvConfig.setConfiguration(Default.RENDEZVOUS_SERVICE_IS_ENABLED ?
                RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS :
                RdvConfigAdv.RendezVousConfiguration.EDGE);
            rdvConfig.setUseOnlySeeds(! Default.RENDEZVOUS_DISCOVERY_IS_ENABLED);
        }
        
        // parse old rdv params as needed.
        if (null != d) {
            Attribute a = ((Attributable)d).getAttribute(Tag.FLAGS.toString());
            String v = a != null ? a.getValue() : null;
            
            rdvConfig.setUseOnlySeeds(v != null &&
                v.indexOf(Tag.NO_DISCOVERY.toString()) != -1);
            
            Enumeration e = null;
            
            for (e = d.getChildren(Tag.ADDRESS.toString());
                e.hasMoreElements(); ) {
                String s = ((TextElement)e.nextElement()).getTextValue().trim();
                
                if (s.length() > 0) {
                    rdvConfig.addSeedRendezvous(s);
                }
            }
            
            e = d.getChildren(Tag.RENDEZVOUS.toString());
            
            rdvConfig.setConfiguration(e.hasMoreElements() &&
                Boolean.valueOf(((TextElement)e.nextElement()).getTextValue().trim()).booleanValue() ?
                    RdvConfigAdv.RendezVousConfiguration.RENDEZVOUS :
                    RdvConfigAdv.RendezVousConfiguration.EDGE );
        }
    }
    
    private void setRelay(ConfigParams pc) {
        XMLDocument d = (XMLDocument) (pc != null ?
            pc.getServiceParam(ModuleId.RELAY.getId()) : null);
        
        try {
            if ( null != d ) {
                // XXX 20041027 backwards compatibility
                d.addAttribute( "type", RelayConfigAdv.getAdvertisementType() );
                this.relayConfig = (RelayConfigAdv) AdvertisementFactory.newAdvertisement(d);
                d = null;
            }
        } catch ( NoSuchElementException notRelayConfigAdv ) {
        }
        
        if (null == this.relayConfig) {
            this.relayConfig = (RelayConfigAdv)AdvertisementFactory.newAdvertisement(RelayConfigAdv.getAdvertisementType());
            
            this.relayConfig.setClientEnabled(Default.RELAY_SERVICE_IS_ENABLED);
            this.relayConfig.setServerEnabled(Default.RELAY_SERVICE_INCOMING_IS_ENABLED);
        }
            
        // parse old relay params for backwards compatibility
        if (d != null) {
            Enumeration e = null;
            
            for (e = d.getChildren(Tag.TCP_ADDRESS.toString());
                e.hasMoreElements(); ) {
                String s = ((TextElement)e.nextElement()).getTextValue().trim();
                
                if (s.length() > 0) {
                    try {
                        addRelay(new URI(Protocol.TCP_URI + s));
                    } catch (URISyntaxException use) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid tcp relay address", use);
                        }
                    } catch (ConfiguratorException ce) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid relay address", ce);
                        }
                    }
                }
            }
            
            for (e = d.getChildren(Tag.HTTP_ADDRESS.toString());
                e.hasMoreElements(); ) {
                String s = ((TextElement)e.nextElement()).getTextValue().trim();
                
                if (s.length() > 0) {
                    try {
                        addRelay(new URI(Protocol.HTTP_URI + s));
                    } catch (URISyntaxException use) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid http relay address", use);
                        }
                    } catch (ConfiguratorException ce) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid rdv address", ce);
                        }
                    }
                }
            }
            
            e = d.getChildren(Tag.IS_SERVER.toString());
            
            setRelayIncoming(e.hasMoreElements() &&
                Boolean.valueOf(((TextElement)e.nextElement()).getTextValue().trim()).booleanValue());
            
            e = d.getChildren(Tag.IS_CLIENT.toString());
            
            setRelayOutgoing(e.hasMoreElements() &&
                Boolean.valueOf(((TextElement)e.nextElement()).getTextValue().trim()).booleanValue());
            
            e = d.getChildren(Tag.MAXIMUM_INCOMING.toString());
            
            int i = Default.INCOMING_MAXIMUM;
            
            if (e.hasMoreElements()) {
                try {
                    final String val = ((TextElement)e.nextElement()).getTextValue();
                    
                    if (val != null) {
                        i = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid maximum incoming", nfe);
                    }
                }
            }
            
            setRelayIncomingMaximum(i);
            
            e = d.getChildren(Tag.INCOMING_LEASE.toString());
            
            long l = Default.INCOMING_LEASE;
            
            if (e.hasMoreElements()) {
                try {
                    final String val = ((TextElement)e.nextElement()).getTextValue();
                    
                    if (val != null) {
                        l = Long.parseLong(val.trim()) * MILLISECONDS_PER_SECOND;
                    }
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid incoming lease", nfe);
                    }
                }
            }
            
            setRelayIncomingLease(l);
            
            e = d.getChildren(Tag.MAXIMUM_OUTGOING.toString());
            i = Default.OUTGOING_MAXIMUM;
            
            if (e.hasMoreElements()) {
                try {
                    final String val = ((TextElement)e.nextElement()).getTextValue();
                    
                    if (val != null) {
                        i = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid lease", nfe);
                    }
                    
                    i = 0;
                }
            }
            
            setRelayOutgoingMaximum(i);
            
            e = d.getChildren(Tag.QUEUE_SIZE.toString());
            i = Default.RELAY_SERVICE_QUEUE_SIZE;
            
            if (e.hasMoreElements()) {
                try {
                    i = Integer.parseInt(((TextElement)e.nextElement()).getTextValue().trim());
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid queue size", nfe);
                    }
                    
                    i = 0;
                }
            }
            
            setRelayQueueSize(i);
            
            e = d.getChildren(Tag.OUTGOING_LEASE.toString());
            l = Default.OUTGOING_LEASE;
            
            if (e.hasMoreElements()) {
                try {
                    final String val = ((TextElement)e.nextElement()).getTextValue();
                    
                    if (val != null) {
                        l = Long.parseLong(val.trim()) * MILLISECONDS_PER_SECOND;
                    }
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid outgoing lease", nfe);
                    }
                    
                    l = 0;
                }
            }
            
            setRelayOutgoingLease(l);
        }
    }
    
    private void setTcp(ConfigParams pc) {
        StructuredDocument d = pc != null ?
            pc.getServiceParam(ModuleId.TCP.getId()) : null;
        Enumeration c = d != null ?
            d.getChildren(TransportAdvertisement.getAdvertisementType()) :
            null;
        
        if (c != null) {
            TCPAdv tcp = (TCPAdv)(c != null &&
            c.hasMoreElements() ?
                AdvertisementFactory.newAdvertisement((TextElement)c.nextElement()) :
                AdvertisementFactory.newAdvertisement(TCPAdv.getAdvertisementType()));
            
            tcp.setProtocol(Default.ANY_TCP_ADDRESS.getScheme());
            tcp.setConfigMode(Default.CONFIG_MODE);
            
            TcpTransport transport = new TcpTransport();
            Enumeration e = d != null ?
                d.getChildren(Tag.IS_ENABLED.toString()) : null;
            
            transport.setEnabled(e != null && !e.hasMoreElements());
            transport.setIncoming(tcp.isServerEnabled());
            transport.setOutgoing(tcp.isClientEnabled());
            
            TcpTransportAddress address = new TcpTransportAddress();
            String a = tcp.getInterfaceAddress();
            int p = tcp.getStartPort();
            int ep = tcp.getEndPort();
            int r = (ep >= p ? ep : p) - p;
            URI u = makeAddress(Protocol.TCP_URI, a, p);
            
            address.setAddress(u);
            //address.setPortRange(r >= 0 ? r : Default.PORT_RANGE);
            address.setPortRange(r);
            
            MulticastAddress multicast = new MulticastAddress();
            
            u = null;
            a = tcp.getMulticastAddr();
            p = tcp.getMulticastPort();
            
            u = makeAddress(Protocol.UDP_URI, a, p);
            
            multicast.setMulticast(tcp.getMulticastState());
            multicast.setSize(tcp.getMulticastSize());
            multicast.setAddress(u);
            
            address.setMulticastAddress(multicast);
            
            transport.setAddress(address);
            
            u = null;
            a = tcp.getServer();
            
            if (a != null &&
                a.trim().length() > 0) {
                try {
                    u = new URI(Protocol.TCP_URI + a.trim());
                } catch (URISyntaxException use) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid tcp public address", use);
                    }
                }
            }
            
            if (u != null) {
                PublicAddress pa = new PublicAddress();
                
                pa.setExclusive(tcp.getPublicAddressOnly());
                pa.setAddress(u);
                
                transport.setPublicAddress(pa);
            }
            
            addTransport(transport);
        }
    }
    
    private void setHttp(ConfigParams pc) {
        StructuredDocument d = pc != null ?
            pc.getServiceParam(ModuleId.HTTP.getId()) : null;
        Enumeration c = d != null ?
            d.getChildren(TransportAdvertisement.getAdvertisementType()) :
            null;
        
        if (c != null) {
            HTTPAdv http = (HTTPAdv)(d != null &&
            c != null &&
            c.hasMoreElements() ?
                AdvertisementFactory.newAdvertisement((TextElement)c.nextElement()) :
                AdvertisementFactory.newAdvertisement(HTTPAdv.getAdvertisementType()));
            
            http.setProtocol(Default.ANY_HTTP_ADDRESS.getScheme());
            http.setConfigMode(Default.CONFIG_MODE);
            
            HttpTransport transport = new HttpTransport();
            Enumeration e = d != null ?
                d.getChildren(Tag.IS_ENABLED.toString()) : null;
            
            transport.setEnabled(e != null && !e.hasMoreElements());
            transport.setIncoming(http.isServerEnabled());
            transport.setOutgoing(http.isClientEnabled());
            
            Address address = new Address();
            URI u = null;
            String a = http.getInterfaceAddress();
            int p = http.getPort();
            
            u = makeAddress(Protocol.HTTP_URI, a, p );
            
            address.setAddress(u);
            address.setPortRange(Default.PORT_RANGE);
            
            transport.setAddress(address);
            
            u = null;
            a = http.getServer();
            
            if (a != null &&
                a.trim().length() > 0) {
                try {
                    u = new URI(Protocol.HTTP_URI + a.trim());
                } catch (URISyntaxException use) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid http public address", use);
                    }
                }
            }
            
            if (u != null) {
                PublicAddress pa = new PublicAddress();
                
                pa.setExclusive(http.getPublicAddressOnly());
                pa.setAddress(u);
                
                transport.setPublicAddress(pa);
            }
            
            addTransport(transport);
        }
    }
    
    private void setEndpoint(ConfigParams pc) {
        StructuredDocument d = pc != null ?
            pc.getServiceParam(ModuleId.ENDPOINT.getId()) : null;
        
        if (d != null) {
            Enumeration e = d.getChildren(Tag.ENDPOINT_QUEUE_SIZE.toString());
            int i = Default.ENDPOINT_SERVICE_QUEUE_SIZE;
            
            if (e.hasMoreElements()) {
                try {
                    i = Integer.parseInt(((TextElement)e.nextElement()).getTextValue().trim());
                } catch (NumberFormatException nfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid messenger queue size", nfe);
                    }
                }
            }
            
            setEndpointOutgoingQueueSize(i);
        } else {
            setEndpointOutgoingQueueSize(Default.ENDPOINT_SERVICE_QUEUE_SIZE);
        }
    }
    
    private void setProxy(ConfigParams pc) {
        StructuredDocument d = pc != null ?
            pc.getServiceParam(ModuleId.PROXY.getId()) : null;
        
        if (d != null) {
            Enumeration e = d.getChildren(Tag.IS_ENABLED.toString());
            
            setProxy(! e.hasMoreElements());
        } else {
            setProxy(Default.PROXY_SERVICE_IS_ENABLED);
        }
    }
    
    private void process(Profile profile) {
        process(profile, false);
    }

    private void process(Profile profile, boolean overlay) {
        if (profile != null) {
            Resource p = profile.getProfile();

            setName(p.get(Profile.Key.PEER_NAME, overlay ?
                Default.PEER_NAME : getName()));

            String s = p.get(Profile.Key.PEER_ID, "").trim();

            if (s.trim().length() > 0) {
                try {
                    setPeerId((PeerID)IDFactory.fromURI(new URI(s)));
                } catch (URISyntaxException use) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid peer id", use);
                    }
                }
            }

            setDescriptor(p.get(Profile.Key.PEER_DESCRIPTOR,
                overlay ? Default.PEER_NAME : getDescriptor()));

            URI a = null;

            try {
                a = p.getURI(Profile.Key.HOME_ADDRESS, overlay ?
                    Default.HOME_ADDRESS :
                    this.home != null ? this.home : null);
                    } catch (ConversionException ce) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid home address", ce);
                        }
                    }

            if (a != null &&
                ! a.equals(Default.HOME_ADDRESS) &&
                this.home == Env.JXTA_HOME.toURI()) {
                this.home = a;
            }

            setTrace(Trace.get(p.get(Profile.Key.TRACE, overlay ?
                Trace.DEFAULT.toString() : getTrace().toString())));
            setDescription(p.get(Profile.Key.PEER_DESCRIPTION, overlay ?
                Default.PEER_DESCRIPTION : getDescription()));

            boolean b = p.contains(Profile.Key.SECURITY_IS_ENABLED);

            setSecurity(b ?
                p.getBoolean(Profile.Key.SECURITY_IS_ENABLED) : overlay ?
                    Default.SECURITY_IS_ENABLED : isSecurity());
            setSecurity(p.get(Profile.Key.PRINCIPAL, overlay ?
                Default.PRINCIPAL : getPrincipal()), Default.PASSWORD);

            try {
                setRootCertificateAddress(p.getURI(Profile.Key.ROOT_CERTIFICATE_ADDRESS,
                    overlay ? null : getRootCertificateAddress()));
            } catch (ConversionException ce) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("root certificate address", ce);
                }
            }

            setRootCertificateBase64(p.get(Profile.Key.ROOT_CERTIFICATE,
                overlay ?
                    Default.ROOT_CERTIFICATE : getRootCertificateBase64()));

            a = null;

            try {
                a = p.getURI(Profile.Key.PEER_PROXY_ADDRESS, getPeerProxyAddress());
            } catch (ConversionException cve) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid peer proxy address conversion", cve);
                }
            }

            if (a != null) {
                try {
                    setPeerProxyAddress(a);
                } catch (ConfiguratorException cfe) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid peer proxy address", cfe);
                    }
                }
            }

            Properties cp = new Properties();
            URI u = getJxtaHome();

            if (u != null) {
                String cs = u.toString() + File.separator + Env.CONFIG_PROPERTIES;
                boolean isValid = false;

                try {
                    cp.load(new URI(cs).toURL().openStream());

                    isValid = true;
                } catch (URISyntaxException use) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid " + Env.CONFIG_PROPERTIES + " uri: " + cs,
                            use);
                    }
                } catch (MalformedURLException mue) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid " + Env.CONFIG_PROPERTIES + " url: " + cs,
                            mue);
                    }
                } catch (IOException ioe) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid " + Env.CONFIG_PROPERTIES + " io: " + cs,
                            ioe);
                    }
                }
            }

            PeerGroupID nid = null;
            String snid = cp.getProperty(Env.NETWORK_ID_KEY,
                p.get(Profile.Key.NETWORK_ID));

            snid = snid != null ? snid.trim() : snid;

            if (snid != null) {
                try {
                    nid = (PeerGroupID)PeerGroupID.create(new URI(snid));
                } catch (URISyntaxException use) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid " + Env.CONFIG_PROPERTIES + " " +
                            Env.NETWORK_ID_KEY + ": " + snid, use);
                    }
                }
            }

            setInfrastructurePeerGroupId(nid);
            setInfrastructurePeerGroupName(cp.getProperty(Env.NETWORK_NAME_KEY,
                p.get(Profile.Key.NETWORK_NAME)));
            setInfrastructurePeerGroupDescription(cp.getProperty(Env.NETWORK_DESCRIPTION_KEY,
                p.get(Profile.Key.NETWORK_DESCRIPTION)));       

            s = p.get(Profile.Key.RENDEZVOUS_BOOTSTRAP_ADDRESS);

            try {
                setRendezVousBootstrapAddress(p.getURI(Profile.Key.RENDEZVOUS_BOOTSTRAP_ADDRESS,
                    overlay ?
                        Default.RENDEZVOUS_BOOTSTRAP_ADDRESS :
                            s == null ? getRendezVousBootstrapAddress() : null));
            } catch (ConversionException ce) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("invalid rendezVous bootstrap", ce);
                }
            } catch (ConfiguratorException ce) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid rdv bootstrap address", ce);
                }
            }

            b = p.contains(Profile.Key.RENDEZVOUS_DISCOVERY_IS_ENABLED);

            setRendezVousDiscovery(b ?
                p.getBoolean(Profile.Key.RENDEZVOUS_DISCOVERY_IS_ENABLED) :
                overlay ?
                    Default.RENDEZVOUS_DISCOVERY_IS_ENABLED :
                    isRendezVousDiscovery());

            if (p.contains(Profile.Key.RENDEZVOUS_ADDRESS) &&
                ! overlay) {
                clearRendezVous();
            }

            for (Iterator r = p.getAll(Profile.Key.RENDEZVOUS_ADDRESS).iterator();
                r.hasNext(); ) {
                String rs = ((String)r.next()).trim();

                try {
                    addRendezVous(normalizeURI(rs));
                } catch (ConfiguratorException ce) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid rendezVous address: " + rs);
                    }
                }
            }

            s = p.get(Profile.Key.RELAYS_BOOTSTRAP_ADDRESS);

            try {
                setRelaysBootstrapAddress(p.getURI(Profile.Key.RELAYS_BOOTSTRAP_ADDRESS,
                    overlay ?
                        Default.RELAYS_BOOTSTRAP_ADDRESS :
                            s == null ? getRelaysBootstrapAddress() : null));
            } catch (ConversionException ce) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("invalid relay bootstrap", ce);
                }
            } catch (ConfiguratorException ce) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid rly bootstrap address", ce);
                }
            }

            b = p.contains(Profile.Key.RELAYS_DISCOVERY_IS_ENABLED);

            setRelaysDiscovery(b ?
                p.getBoolean(Profile.Key.RELAYS_DISCOVERY_IS_ENABLED) :
                    overlay ?
                        Default.RELAYS_DISCOVERY_IS_ENABLED :
                        isRelaysDiscovery());

            if (p.contains(Profile.Key.RELAYS_ADDRESS) &&
                ! overlay) {
                clearRelays();
            }

            for (Iterator r = p.getAll(Profile.Key.RELAYS_ADDRESS).iterator();
                r.hasNext(); ) {
                String rs = ((String)r.next()).trim();

                    try {
                    addRelay(normalizeURI(rs));
                    } catch (ConfiguratorException ce) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("invalid relay address: " + rs);
                    }
                }
            }

            // xxx: generalize to support any number of varying typed transports
            if (! overlay) {
                clearTransports();
            }

            if (p.contains(Profile.Key.TCP)) {
                TcpTransport tcp = new TcpTransport();

                tcp.setEnabled(p.getBoolean(Profile.Key.TCP_IS_ENABLED,
                    Default.TCP_IS_ENABLED));
                tcp.setIncoming(p.getBoolean(Profile.Key.TCP_INCOMING_IS_ENABLED,
                    Default.TCP_INCOMING_IS_ENABLED));
                tcp.setOutgoing(p.getBoolean(Profile.Key.TCP_OUTGOING_IS_ENABLED,
                    Default.TCP_OUTGOING_IS_ENABLED));

                TcpTransportAddress tcpAddress = new TcpTransportAddress();

                a = null;

                try {
                    a = p.getURI(Profile.Key.TCP_ADDRESS);
                } catch (ConversionException cve) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid tcp address conversion", cve);
                    }

                    a = Default.TCP_ADDRESS;
                }

                a = a != null ? normalizeURI(a) : null;

                if (a != null &&
                    a.toString().trim().length() > 0) {
                    if (a.getScheme() == null ||
                        a.getHost() == null ||
                        a.getPort() == Default.INVALID_PORT) {
                        try {
                            a = new URI(a.getScheme() != null ?
                                    a.getScheme() : Transport.Scheme.TCP,
                                a.getUserInfo(), a.getHost(),
                                a.getPort() != Default.INVALID_PORT ?
                                    a.getPort() : Default.TCP_PORT,
                                a.getPath(), a.getQuery(), a.getFragment());
                        } catch (URISyntaxException use) {
                            a = null;

                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("invalid tcp address transformation",
                                    use);
                            }
                        }
                    }

                    if (a != null) {
                        tcpAddress.setAddress(a);
                    } else {
                    }
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("address scheme incompatibility");
                    }
                }

                tcpAddress.setPortRange(p.getInt(Profile.Key.TCP_PORT_RANGE,
                    Default.PORT_RANGE));

                MulticastAddress multicast = new MulticastAddress();

                multicast.setMulticast(p.getBoolean(Profile.Key.MULTICAST_IS_ENABLED,
                    Default.MULTICAST_IS_ENABLED));
                multicast.setSize(p.getInt(Profile.Key.MULTICAST_SIZE,
                    Default.MULTICAST_SIZE));

                try {
                    multicast.setAddress(p.getURI(Profile.Key.MULTICAST,
                        Default.MULTICAST_ADDRESS));
                } catch (ConversionException cve) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid multicast address conversion", cve);
                    }
                } catch (IllegalArgumentException iae) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid multicast address", iae);
                    }
                }

                tcpAddress.setMulticastAddress(multicast);

                tcp.setAddress(tcpAddress);

                PublicAddress publicAddress = new PublicAddress();

                publicAddress.setExclusive(p.getBoolean(
                    Profile.Key.TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED,
                    Default.TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED));

                a = null;

                try {
                    a = p.getURI(Profile.Key.TCP_PUBLIC_ADDRESS);
                } catch (ConversionException cve) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid public tcp address conversion", cve);
                    }
                }

                if (a != null &&
                    a.toString().trim().length() > 0 &&
                    tcpAddress != null &&
                    tcpAddress.getAddress() != null &&
                    a.getScheme().equals(tcpAddress.getAddress().getScheme())) {
                    PublicAddress pa = new PublicAddress();

                    pa.setExclusive(p.getBoolean(Profile.Key.TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED,
                        Default.TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED));
                    pa.setAddress(a);

                    tcp.setPublicAddress(pa);
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("address and public address scheme incompatibility");
                    }
                }

                a = null;

                try {
                    a = p.getURI(Profile.Key.TCP_PROXY_ADDRESS);
                } catch (ConversionException ce) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid tcp proxy address", ce);
                    }
                }

                if (a != null) {
                    if (a.getScheme() == null) {
                        a = Default.ANY_TCP_ADDRESS.resolve(a);
                    }

                    if (! a.getScheme().equalsIgnoreCase(Default.ANY_TCP_ADDRESS.getScheme())) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid tcp address: " + a.toString());
                        }

                        a = null;
                    }

                    if (a != null) {
                        if (a.getHost() == null) {
                            a = null;
                        }

                        if (a != null &&
                            a.getPort() == Default.INVALID_PORT) {
                            try {
                                a = new URI(a.getScheme(), a.getUserInfo(),
                                    a.getHost(), Default.TCP_PROXY_PORT,
                                    a.getPath(), a.getQuery(), a.getFragment());
                            } catch (URISyntaxException use) {
                                a = null;

                                if (LOG.isEnabledFor(Level.ERROR)) {
                                    LOG.error("invalid tcp proxy transformation",
                                        use);
                                }
                            }
                        }
                    }
                }

                if (a != null) {
                    ProxyAddress proxy = new ProxyAddress();

                    proxy.setEnabled(p.getBoolean(Profile.Key.TCP_PROXY_IS_ENABLED,
                        Default.TCP_PROXY_IS_ENABLED));
                    proxy.setAddress(a);

                    tcp.setProxyAddress(proxy);
                } else {
                }

                addTransport(tcp);
            }

            if (p.contains(Profile.Key.HTTP)) {
                HttpTransport http = new HttpTransport();

                http.setEnabled(p.getBoolean(Profile.Key.HTTP_IS_ENABLED,
                    Default.HTTP_IS_ENABLED));
                http.setIncoming(p.getBoolean(Profile.Key.HTTP_INCOMING_IS_ENABLED,
                    Default.HTTP_INCOMING_IS_ENABLED));
                http.setOutgoing(p.getBoolean(Profile.Key.HTTP_OUTGOING_IS_ENABLED,
                    Default.HTTP_OUTGOING_IS_ENABLED));

                Address httpAddress = new Address();

                a = null;

                try {
                    a = p.getURI(Profile.Key.HTTP_ADDRESS,
                        normalizeURI(p.get(Profile.Key.HTTP_ADDRESS)));
                } catch (ConversionException cve) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid http address conversion", cve);
                    }

                    a = Default.HTTP_ADDRESS;
                }

                a = a != null ? normalizeURI(a) : null;

                if (a != null &&
                    a.toString().trim().length() > 0) {
                    if (a.getScheme() == null ||
                        a.getHost() == null ||
                        a.getPort() == Default.INVALID_PORT) {
                        try {
                            a = new URI(a.getScheme() != null ?
                                    a.getScheme() : Transport.Scheme.HTTP,
                                a.getUserInfo(), a.getHost(),
                                a.getPort() != Default.INVALID_PORT ?
                                    a.getPort() : Default.HTTP_PORT,
                                a.getPath(), a.getQuery(), a.getFragment());
                        } catch (URISyntaxException use) {
                            a = null;

                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("invalid http address transformation",
                                    use);
                            }
                        }
                    }

                    if (a != null) {
                        httpAddress.setAddress(a);
                    } else {
                    }
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("address scheme incompatibility");
                    }
                }

                httpAddress.setPortRange(p.getInt(Profile.Key.HTTP_PORT_RANGE,
                    Default.PORT_RANGE));
                http.setAddress(httpAddress);

                a = null;

                try {
                    a = p.getURI(Profile.Key.HTTP_PUBLIC_ADDRESS);
                } catch (ConversionException cve) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid public http address conversion", cve);
                    }
                }

                if (a != null &&
                    a.toString().trim().length() > 0 &&
                    httpAddress != null &&
                    httpAddress.getAddress() != null &&
                    a.getScheme() != null &&
                    a.getScheme().equals(httpAddress.getAddress().getScheme())) {
                    PublicAddress pa = new PublicAddress();

                    pa.setExclusive(p.getBoolean(Profile.Key.HTTP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED,
                        Default.HTTP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED));
                    pa.setAddress(a);

                    http.setPublicAddress(pa);
                } else {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("address and public address scheme incompatibility");
                    }
                }

                a = null;

                try {
                    a = p.getURI(Profile.Key.HTTP_PROXY_ADDRESS);
                } catch (ConversionException ce) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("invalid http proxy address", ce);
                    }
                }

                if (a != null) {
                    if (a.getScheme() == null) {
                        a = Default.ANY_HTTP_ADDRESS.resolve(a);
                    }

                    if (! a.getScheme().equalsIgnoreCase(Default.ANY_HTTP_ADDRESS.getScheme())) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("invalid http address: " + a.toString());
                        }

                        a = null;
                    }

                    if (a != null) {
                        if (a.getHost() == null) {
                            a = null;
                        }

                        if (a != null &&
                            a.getPort() == Default.INVALID_PORT) {
                            try {
                                a = new URI(a.getScheme(), a.getUserInfo(),
                                    a.getHost(), Default.HTTP_PROXY_PORT,
                                    a.getPath(), a.getQuery(), a.getFragment());
                            } catch (URISyntaxException use) {
                                a = null;

                                if (LOG.isEnabledFor(Level.ERROR)) {
                                    LOG.error("invalid http proxy transformation",
                                        use);
                                }
                            }
                        }
                    }
                }

                if (a != null) {
                    ProxyAddress proxy = new ProxyAddress();

                    proxy.setEnabled(p.getBoolean(Profile.Key.HTTP_IS_ENABLED,
                        Default.HTTP_PROXY_IS_ENABLED));
                    proxy.setAddress(a);

                    http.setProxyAddress(proxy);
                } else {
                }

                addTransport(http);
            }

            b = p.contains(Profile.Key.RENDEZVOUS_SERVICE_IS_ENABLED);

            setRendezVous(b ?
                p.getBoolean(Profile.Key.RENDEZVOUS_SERVICE_IS_ENABLED) :
                overlay ?
                    Default.RENDEZVOUS_SERVICE_IS_ENABLED : isRendezVous());

            b = p.contains(Profile.Key.RENDEZVOUS_SERVICE_AUTO_START_IS_ENABLED);

    //            setRendezVousAutoStart(b ?
    //                p.getBoolean(Profile.Key.RENDEZVOUS_SERVICE_AUTO_START_IS_ENABLED) :
    //                overlay ?
    //                    Default.RENDEZVOUS_SERVICE_AUTO_START_IS_ENABLED :
    //                    isRendezVousAutoStart());
            setRendezVousAutoStart(p.getLong(Profile.Key.RENDEZVOUS_SERVICE_AUTO_START,
                overlay ?
                    Default.RENDEZVOUS_SERVICE_AUTO_START :
                    getRendezVousAutoStart()));

            b = p.contains(Profile.Key.RELAY_SERVICE_IS_ENABLED);

            setRelayQueueSize(p.getInt(Profile.Key.RELAY_SERVICE_QUEUE_SIZE,
                overlay ?
                    Default.RELAY_SERVICE_QUEUE_SIZE : getRelayQueueSize()));

            b = p.contains(Profile.Key.RELAY_SERVICE_INCOMING_IS_ENABLED);

            setRelayIncoming(b ?
                p.getBoolean(Profile.Key.RELAY_SERVICE_INCOMING_IS_ENABLED) :
                    overlay ?
                        Default.RELAY_SERVICE_INCOMING_IS_ENABLED :
                        isRelayIncoming());
            setRelayIncomingMaximum(p.getInt(Profile.Key.RELAY_SERVICE_INCOMING_MAXIMUM,
                overlay ?
                    Default.INCOMING_MAXIMUM : getRelayIncomingMaximum()));
            setRelayIncomingLease(p.getLong(Profile.Key.RELAY_SERVICE_INCOMING_LEASE,
                overlay ? Default.INCOMING_LEASE : getRelayIncomingLease()));

            b = p.contains(Profile.Key.RELAY_SERVICE_OUTGOING_IS_ENABLED);

            setRelayOutgoing(b ?
                p.getBoolean(Profile.Key.RELAY_SERVICE_OUTGOING_IS_ENABLED) :
                overlay ? Default.RELAY_SERVICE_OUTGOING_IS_ENABLED :
                    isRelayOutgoing());
            setRelayOutgoingMaximum(p.getInt(Profile.Key.RELAY_SERVICE_OUTGOING_MAXIMUM,
                overlay ?
                    Default.OUTGOING_MAXIMUM : getRelayOutgoingMaximum()));
            setRelayOutgoingLease(p.getLong(Profile.Key.RELAY_SERVICE_OUTGOING_LEASE,
                overlay ? Default.OUTGOING_LEASE : getRelayOutgoingLease()));

            setEndpointOutgoingQueueSize(p.getInt(Profile.Key.ENDPOINT_SERVICE_QUEUE_SIZE,
                overlay ?
                    Default.ENDPOINT_SERVICE_QUEUE_SIZE :
                    getEndpointOutgoingQueueSize()));

            b = p.contains(Profile.Key.PROXY_SERVICE_IS_ENABLED);

            setProxy(b ? p.getBoolean(Profile.Key.PROXY_SERVICE_IS_ENABLED) :
                overlay ? Default.PROXY_SERVICE_IS_ENABLED : isProxy());

            for (Iterator o = p.getAll(Profile.Key.CONFIGURATION_OPTIMIZER_CLASS).iterator();
                o.hasNext(); ) {
                String cn = (String)o.next();
                Optimizer op = null;

                try {
                    Class c = Class.forName(cn);

                    op = (Optimizer)c.newInstance();
                } catch (ClassNotFoundException cnfe) {
                    if (LOG.isEnabledFor(Level.INFO)) {
                        LOG.info("optimizer not found: " + cn, cnfe);
                    }
                } catch (InstantiationException ie) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("can't instantiate optimizer: " + cn, ie);
                    }
                } catch (IllegalAccessException iae) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("can't access optimizer: " + cn, iae);
                    }
                }

                if (op != null) {
                    String pnq = MessageFormat.format(Profile.Key.OPTIMIZER_PROPERTY_NAME,
                        new Object[] { cn });
                    List pnc = new ArrayList();

                    for (Iterator opn = p.getAll(pnq).iterator();
                        opn.hasNext(); ) {
                        String pn = (String)opn.next();
                        String pvq = MessageFormat.format(Profile.Key.OPTIMIZER_PROPERTY_VALUE,
                            new Object[] { cn, pn });

                        if (! pnc.contains(pn)) {
                            for (Iterator opv = p.getAll(pvq).iterator();
                                opv.hasNext(); ) {
                                String pv = (String)opv.next();

                                if (pv != null) {
                                    op.addProperty(pn, pv);
                                }
                            }
                        }

                        pnc.add(pn);
                    }

                    addOptimizer(op);
                }
            }
        }
    }
    
    private void validate()
    throws ConfiguratorException {
        List state = new ArrayList();
        String rc = getRootCertificateBase64();
        String usr = getPrincipal();
        String pwd = getPassword();
        
        if (rc == null) {
            if ((usr == null ||
                usr.trim().length() == 0) ||
                (pwd == null ||
                    pwd.trim().length() == 0)) {
                state.add(new IllegalStateException("invalid security[principal, password]: " +
                    usr + " " + pwd));
            } else {
                state.add(new IllegalStateException("null root certificate"));
            }
        }
        
        List transports = getTransports();
        
        if (transports.size() == 0) {
            state.add(new IllegalStateException("unspecified transport"));
        }
        
        for (Iterator ti = transports.iterator(); ti.hasNext(); ) {
            Transport t = (Transport)ti.next();
            List as = t.getAddresses();
            List ps = t.getPublicAddresses();
            
            if (t.isIncoming()) {
                if (as.isEmpty() &&
                ps.isEmpty()) {
                    state.add(new IllegalStateException("unspecified transport address"));
                }
                
                for (Iterator ai = as.iterator(); ai.hasNext(); ) {
                    Address a = (Address)ai.next();
                    URI aa = a != null ? a.getAddress() : null;
                    
                    for (Iterator pi = ps.iterator(); pi.hasNext(); ) {
                        PublicAddress p = (PublicAddress)pi.next();
                        URI pa = p != null ? p.getAddress() : null;
                        
                        if (pa != null &&
                            pa.getHost() != null &&
                            pa.getHost().length() > 0) {
                            if (aa != null &&
                                (aa.getPort() < Default.MINIMUM_PORT ||
                                    aa.getPort() > Default.MAXIMUM_PORT) &&
                                (pa.getPort() < Default.MINIMUM_PORT ||
                                    pa.getPort() > Default.MAXIMUM_PORT)) {
                                state.add(new IllegalStateException("invalid port: "
                                    + aa.getPort() + " " + pa.getPort()));
                            }
                        } else if (aa != null &&
                            (aa.getPort() < Default.MINIMUM_DYNAMIC_PORT ||
                                aa.getPort() > Default.MAXIMUM_PORT)) {
                            state.add(new IllegalStateException("invalid port: " +
                                aa.getPort()));
                        }
                    }
                    
                    if (a instanceof TcpTransportAddress) {
                        for (Iterator mi = ((TcpTransportAddress)a).getMulticastAddresses().iterator();
                            mi.hasNext(); ) {
                            MulticastAddress m = (MulticastAddress)mi.next();
                            URI ma = m != null ? m.getAddress() : null;
                            
                            if (ma != null &&
                                (ma.getPort() < Default.MINIMUM_PORT ||
                                    ma.getPort() > Default.MAXIMUM_PORT)) {
                                state.add(new IllegalStateException("invalid multicast port: " +
                                    ma.getPort()));
                            }
                            
                            if (m != null &&
                                (m.getSize() < Default.MINIMUM_MULTICAST_SIZE ||
                                    m.getSize() > Default.MAXIMUM_MULTICAST_SIZE)) {
                                state.add(new IllegalStateException("invalid multicast size: " +
                                    m.getSize()));
                            }
                        }
                    }
                }
                
                // xxx: this may be the wrong proxy
                ProxyAddress px = t.getProxyAddress();
                URI pxa = px != null ? px.getAddress() : null;
                
                if (pxa != null &&
                    (pxa.getPort() < Default.MINIMUM_PORT ||
                        pxa.getPort() > Default.MAXIMUM_PORT)) {
                    state.add(new IllegalStateException("invalid proxy port " +
                        pxa.getPort()));
                }
            } else {
                //setRelayIncoming(true);
            }
        }
        
        if (isRelayOutgoing()) {
            int trc = 0;
            int hrc = 0;
            
            for (Iterator r = getRelays().iterator(); r.hasNext(); ) {
                URI uri = (URI)r.next();
                String s = uri.getScheme();
                
                if (s != null) {
                    if (s.equalsIgnoreCase(Default.ANY_TCP_ADDRESS.getScheme())) {
                        trc++;
                    } else if (s.equalsIgnoreCase(Default.ANY_HTTP_ADDRESS.getScheme())) {
                        hrc++;
                    }
                }
            }
            
            if (getRelaysBootstrapAddress() == null) {
                if (transportsContainsScheme(Transport.Scheme.TCP) &&
                    transportsContainsScheme(Transport.Scheme.HTTP)) {
                    if (trc + hrc == 0) {
                        state.add(new IllegalStateException("unspecified relay: " +
                            "tcp[" + transportsContainsScheme(Transport.Scheme.TCP) +
                            "] http[" +
                            transportsContainsScheme(Transport.Scheme.HTTP) +
                            "] relays[" + getRelays().iterator().hasNext() + "]"));
                    }
                } else if (transportsContainsScheme(Transport.Scheme.TCP) &&
                    trc == 0) {
                    state.add(new IllegalStateException("unspecified " +
                        Default.ANY_TCP_ADDRESS.getScheme() + " relay"));
                } else if (transportsContainsScheme(Transport.Scheme.HTTP) &&
                    hrc == 0) {
                    state.add(new IllegalStateException("unspecified " +
                        Default.ANY_HTTP_ADDRESS.getScheme() + " relay"));
                }
            }
        }
        
        if (state.size() > 0) {
            ConfiguratorException ce =
                new ConfiguratorException("invalid configuration", state);
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("configuration is not valid", ce);
            }
            
            throw ce;
        }
    }
    
    private void normalize() {
        String ph = System.getProperty(Env.PROXY_HOST);
        String pp = System.getProperty(Env.PROXY_PORT);
        String pu = System.getProperty(Env.PROXY_USER);
        String pa = System.getProperty(Env.PROXY_PASSWORD);
        String jp = System.getProperty(Env.PROXY);
        URI proxy = getPeerProxyAddress();

/*        
        if (proxy != null) {
            if (proxy.getHost() != null) {
                System.setProperty(Env.PROXY_HOST, proxy.getHost());
            }
            
            if (proxy.getPort() != Default.INVALID_PORT) {
                System.setProperty(Env.PROXY_PORT,
                    Integer.toString(proxy.getPort()));
            }
            
            System.setProperty(Env.PROXY, proxy.getHost() +
                COLON + proxy.getPort());
            
            String ui = proxy.getUserInfo();
            int i = (ui != null ? ui.indexOf(COLON) : -1);
            
            if (ui != null &&
                i > -1) {
                System.setProperty(Env.PROXY_USER, ui.substring(0, i));
                System.setProperty(Env.PROXY_PASSWORD, ui.substring(i + 1));
            }
        }
*/
        
        for (Iterator r = getRendezVous().iterator(); r.hasNext(); ) {
            URI u = (URI)r.next();
            
            if (Default.BOOTSTRAP_ADDRESSES.contains(u)) {
                removeRendezVous(u);
                
                URI b = getRendezVousBootstrapAddress();
                
                try {
                    for (Iterator i = fetchBootstrapAddresses(b).iterator();
                        i.hasNext(); ) {
                        URI t = (URI)i.next();
                        
                        if (u.getScheme() == null ||
                            (u.getScheme() != null &&
                            u.getScheme().equalsIgnoreCase(t.getScheme()))) {
                            addRendezVous(t);
                        }
                    }
                } catch (IOException ioe) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("inaccessible url: " + b.toString());
                    }
                } catch (ConfiguratorException ce) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("invalid rendezVou address: " + b.toString());
                    }
                }
            }
        }
        
        for (Iterator r = getRelays().iterator(); r.hasNext(); ) {
            URI u = (URI)r.next();
            
            if (Default.BOOTSTRAP_ADDRESSES.contains(u)) {
                removeRelay(u);
                
                URI b = getRelaysBootstrapAddress();
                
                try {
                    for (Iterator i = fetchBootstrapAddresses(b).iterator();
                        i.hasNext(); ) {
                        URI t = (URI)i.next();
                        
                        if (u.getScheme() == null ||
                            (u.getScheme() != null &&
                            u.getScheme().equalsIgnoreCase(t.getScheme()))) {
                            addRelay(t);
                        }
                    }
                } catch (IOException ioe) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("inaccessible url: " + b.toString());
                    }
                } catch (ConfiguratorException ce) {
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("invalid relay address: " + b.toString());
                    }
                }
            }
        }
       
/* 
        if (ph != null) {
            System.setProperty(Env.PROXY_HOST, ph);
        }
        
        if (pp != null) {
            System.setProperty(Env.PROXY_PORT, pp);
        }
        
        if (pu != null) {
            System.setProperty(Env.PROXY_USER, pu);
        }
        
        if (pa != null) {
            System.setProperty(Env.PROXY_PASSWORD, pa);
        }
        
        if (jp != null) {
            System.setProperty(Env.PROXY, jp);
        }
*/
        
        String principal = getPrincipal();
        String password = getPassword();
        
        if (principal != null &&
            principal.trim().length() > 0 &&
            password != null &&
            password.trim().length() > 0) {
            // xxx: not required
            System.setProperty(Env.PRINCIPAL, principal);
            System.setProperty(Env.PASSWORD, password);
            
            PSEUtils.IssuerInfo ii = null;
            
            try {
                ii = PSEUtils.genCert(principal, null);
            } catch (SecurityException ioe) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("can't create root certificate", ioe);
                }
            }
            
            if (ii != null) {
                setRootCertificate(ii.cert);
                
                char[] pwd = password.toCharArray();
                
                this.pse.setPrivateKey(ii.subjectPkey, pwd );
                
                for (int i = pwd.length - 1; i >= 0; i--) {
                    pwd[i] = NULL_CHAR;
                }
            }
        }
        
        List n = new ArrayList();
        List m = new ArrayList();
        
        for (Iterator ti = getTransports().iterator(); ti.hasNext(); ) {
            Transport t = (Transport)ti.next();
            
            for (Iterator tai = t.getAddresses().iterator(); tai.hasNext(); ) {
                Address a = (Address)tai.next();
                Address b = null;
                
                if (a instanceof TcpTransportAddress) {
                    if (a.getAddress() == null) {
                        a.setAddress(makeAddress(Protocol.TCP_URI,
                            Env.ALL_ADDRESSES.getHostAddress(),
                            Default.TCP_PORT));
                    }
                    
                    b = new TcpTransportAddress((TcpTransportAddress)a);
                } else if (a instanceof Address) {
                    if (a.getAddress() == null) {
                        a.setAddress(makeAddress(Protocol.HTTP_URI,
                            Env.ALL_ADDRESSES.getHostAddress(),
                            Default.HTTP_PORT));
                    }
                    
                    b = new Address(a);
                }
                
                b.setAddress(Util.normalize(a));
                n.add(b);
                
                if (a instanceof TcpTransportAddress) {
                    for (Iterator mai = ((TcpTransportAddress)a).getMulticastAddresses().iterator();
                        mai.hasNext(); ) {
                        MulticastAddress c = (MulticastAddress)mai.next();
                        Address d = new MulticastAddress(c);
                        
                        d.setAddress(Util.normalize(c, false));
                        
                        m.add(d);
                    }
                    
                    ((TcpTransportAddress)a).setMulticastAddresses(m);
                    m.clear();
                }
            }
            
            t.setAddresses(n);
            n.clear();
            
            for (Iterator pai = t.getPublicAddresses().iterator();
                pai.hasNext(); ) {
                PublicAddress a = (PublicAddress)pai.next();
                PublicAddress b = new PublicAddress(a);
                
                b.setAddress(Util.normalize(a, false));
                n.add(b);
            }
            
            t.setPublicAddresses(n);
            n.clear();
        }
    }
    
    private void optimize() {
        for (Iterator o = getOptimizers(); o.hasNext(); ) {
            ((Optimizer)o.next()).optimize(this);
        }
    }
    
    private PlatformConfig constructPlatformConfig() {
        PlatformConfig pc = (PlatformConfig)AdvertisementFactory.
            newAdvertisement(PlatformConfig.getAdvertisementType());
        
        pc.setName(getName());
        pc.setDescription(getDescription());
        pc.setPeerID(getPeerId());
        pc.setDebugLevel(getTrace() != null ?
            getTrace().toString() : Trace.DEFAULT.toString());
        
        XMLDocument d =
            (XMLDocument)rdvConfig.getDocument(MimeMediaType.XMLUTF8);
        
        pc.putServiceParam(ModuleId.RENDEZVOUS.getId(), d);
        
        d =
            (XMLDocument)StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                Tag.PARM.toString());
        
        if (! isProxy()) {
            d.appendChild(d.createElement(Tag.IS_ENABLED.toString()));
        }
        
        pc.putServiceParam(ModuleId.PROXY.getId(), d);
        
        d = (XMLDocument)relayConfig.getDocument(MimeMediaType.XMLUTF8);
        
        pc.putServiceParam(ModuleId.RELAY.getId(), d);
        
        d =
            (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                Tag.PARM.toString());
        
        d.appendChild(d.createElement(Tag.ENDPOINT_QUEUE_SIZE.toString(),
            Integer.toString(Default.ENDPOINT_SERVICE_QUEUE_SIZE)));
        
        pc.putServiceParam(ModuleId.ENDPOINT.getId(), d);
        
        for (Iterator ti = getTransports().iterator(); ti.hasNext(); ) {
            Transport t = (Transport)ti.next();
            List ports = new ArrayList();
            URI u = null;
            
            if (t instanceof TcpTransport) {
                TcpTransport tt = (TcpTransport)t;
                TCPAdv tcp =
                    (TCPAdv)AdvertisementFactory.newAdvertisement(TCPAdv.getAdvertisementType());
                
                tcp.setProtocol(tt != null ? tt.getScheme() : Protocol.TCP);
                tcp.setConfigMode(Default.CONFIG_MODE);
                tcp.setServerEnabled(tt != null ?
                    tt.isIncoming() : Default.TCP_INCOMING_IS_ENABLED);
                tcp.setClientEnabled(tt != null ?
                    tt.isOutgoing() : Default.TCP_OUTGOING_IS_ENABLED);
                
                for (Iterator ai = tt.getAddresses().iterator();
                    ai.hasNext(); ) {
                    TcpTransportAddress ta = (TcpTransportAddress)ai.next();
                    
                    u = ta != null ? ta.getAddress() : null;
                    
                    if (u != null &&
                        u.getHost() != null &&
                        ! u.getHost().equals(Util.getLocalHost()) &&
                        ! u .getHost().equals(Env.ALL_ADDRESSES.getHostAddress())) {
                        tcp.setInterfaceAddress(u.getHost());
                    }
                    
                    tcp.setPort(u != null ? u.getPort() : Default.TCP_PORT);
                    tcp.setStartPort(tcp.getPort());
                    tcp.setEndPort(tcp.getStartPort() + ta.getPortRange());
                    
                    for (Iterator mi = ta.getMulticastAddresses().iterator();
                        mi.hasNext(); ) {
                        MulticastAddress ma = (MulticastAddress)mi.next();
                        
                        u = ma != null ? ma.getAddress() : null;
                        
                        tcp.setMulticastState(ma != null ?
                            ma.isMulticast() : Default.MULTICAST_IS_ENABLED);
                        tcp.setMulticastAddr(u != null ? u.getHost() : null);
                        tcp.setMulticastPort(u != null ?
                            u.getPort() : Default.MULTICAST_PORT);
                        tcp.setMulticastSize(ma != null ?
                            ma.getSize() : Default.MULTICAST_SIZE);
                    }
                }
                
                for (Iterator pi = tt.getPublicAddresses().iterator();
                    pi.hasNext(); ) {
                    PublicAddress pa = (PublicAddress)pi.next();
                    
                    u = pa != null ? pa.getAddress() : null;
                    
                    if (u != null &&
                        u.getHost() != null &&
                        u.getHost().equals(Util.getLocalHost())) {
                        u = null;
                    }
                    
                    tcp.setServer(u != null ?
                        u.getHost() + COLON + u.getPort() : null);
                    tcp.setPublicAddressOnly(pa != null ?
                        pa.isExclusive() :
                        Default.TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED);
		    tcp.setStartPort(Default.INVALID_PORT);
                    tcp.setEndPort(tcp.getStartPort());
                }
                
                StructuredTextDocument td =
                    (StructuredTextDocument)StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                    Tag.PARM.toString());
                
                StructuredDocumentUtils.copyElements(td, td,
                    (StructuredDocument)tcp.getDocument(MimeMediaType.XMLUTF8));
                
                if (! tt.isEnabled()) {
                    td.appendChild(td.createElement(Tag.IS_ENABLED.toString()));
                }
                
                pc.putServiceParam(ModuleId.TCP.getId(), td);
            } else if (t instanceof HttpTransport) {
                HttpTransport ht = (HttpTransport)t;
                HTTPAdv http =
                    (HTTPAdv)AdvertisementFactory.newAdvertisement(HTTPAdv.getAdvertisementType());
                
                http.setProtocol(ht != null ? ht.getScheme() : Protocol.HTTP);
                http.setConfigMode(Default.CONFIG_MODE);
                http.setServerEnabled(ht != null ?
                    ht.isIncoming() : Default.HTTP_INCOMING_IS_ENABLED);
                http.setClientEnabled(ht != null ?
                    ht.isOutgoing() : Default.HTTP_OUTGOING_IS_ENABLED);
                
                for (Iterator ai = ht.getAddresses().iterator();
                    ai.hasNext(); ) {
                    Address ha = (Address)ai.next();
                    
                    u = ha != null ? ha.getAddress() : null;
                    
                    if (u != null &&
                        u.getHost() != null &&
                        ! u.getHost().equals(Util.getLocalHost()) &&
                        ! u .getHost().equals(Env.ALL_ADDRESSES.getHostAddress())) {
                        http.setInterfaceAddress(u.getHost());
                    }
                    
                    http.setPort(u != null ? u.getPort() : Default.HTTP_PORT);
                }
                
                for (Iterator pi = ht.getPublicAddresses().iterator();
                    pi.hasNext(); ) {
                    PublicAddress pa = (PublicAddress)pi.next();
                    
                    u = pa != null ? pa.getAddress() : null;
                    
                    http.setServer(u != null ?
                        u.getHost() + COLON + u.getPort() : null);
                    http.setPublicAddressOnly(pa != null ?
                        pa.isExclusive() :
                        Default.HTTP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED);
                }
                
                ProxyAddress px = ht.getProxyAddress();
                
                u = px != null ? px.getAddress() : null;
                
                http.setProxyEnabled(ht.isProxy());
                http.setProxy(u != null ?
                    u.getHost() + COLON + u.getPort() : null);
                
                StructuredTextDocument td =
                    (StructuredTextDocument)StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                        Tag.PARM.toString());
                
                StructuredDocumentUtils.copyElements(td, td,
                    (StructuredDocument)http.getDocument(MimeMediaType.XMLUTF8));
                
                if (! ht.isEnabled()) {
                    td.appendChild(td.createElement(Tag.IS_ENABLED.toString()));
                }
                
                if (u != null) {
                    System.setProperty(Env.PROXY, u.toString());
                    
                    if (u.getHost() != null) {
                        System.setProperty(Env.PROXY_HOST, u.getHost());
                    }
                    
                    int p = u.getPort();
                    
                    if (p >= Default.MINIMUM_PORT &&
                        p <= Default.MAXIMUM_PORT) {
                        System.setProperty(Env.PROXY_PORT, Integer.toString(p));
                    }
                    
                    String ui = u.getUserInfo();
                    int i = (ui != null ? ui.indexOf(COLON) : -1);
                    
                    if (ui != null &&
                        i > -1) {
                        System.setProperty(Env.PROXY_USER, ui.substring(0, i));
                        System.setProperty(Env.PROXY_PASSWORD,
                            ui.substring(i + 1));
                    }
                }
                
                pc.putServiceParam(ModuleId.HTTP.getId(), td);
            }
        }
        
        XMLDocument xd = (XMLDocument)this.pse.getDocument(MimeMediaType.XMLUTF8);
        
        xd.addAttribute(Tag.TYPE_ATTRIBUTE.toString(), xd.getName());
        
        pc.putServiceParam(ModuleId.MEMBERSHIP.getId(), xd);
        
        for (Iterator ci = customParams.keySet().iterator(); ci.hasNext(); ) {
            ID key = (ID)ci.next();
            
            if (! ModuleId.isNormalService(key)) {
                pc.putServiceParam(key, (Element)customParams.get(key));
            }
        }
        
        return pc;
    }
    
    private List orderAddresses(List a) {
        List prefix = new ArrayList();
        List postfix = new ArrayList();
        URI u = null;
        
        for (Iterator i = a.iterator(); i.hasNext(); ) {
            u = (URI)i.next();
            
            if (transportsContainsScheme(Transport.Scheme.HTTP) &&
                ! transportsContainsScheme(Transport.Scheme.TCP) &&
                u.getScheme() != null &&
                u.getScheme().equalsIgnoreCase(Default.ANY_HTTP_ADDRESS.getScheme())) {
                prefix.add(u);
            } else {
                postfix.add(u);
            }
        }
        
        List o = new ArrayList(prefix);
        
        o.addAll(postfix);
        
        return o;
    }
    
    private URI normalizeURI(URI u) {
        return normalizeURI(u != null ? u.toString() : null);
    }
    
    private URI normalizeURI(String s) {
        URI u = null;
        
        s = s != null ? s.trim() : null;
        
        if (s != null &&
            s.length() > 0) {
            try {
                u = new URI(s);
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("invalid uri: " + s);
                }
            }
            
            if (u == null ||
                u.getScheme() == null ||
                u.getHost() == null ||
                u.getPort() == -1) {
                String us = u.toString();
                String protocol = u != null ?
                    u.getScheme() + Protocol.URI_DELIMITER : null;

                if (protocol == null ||
                    protocol.trim().length() == 0) {
                    int i = us.indexOf(Protocol.URI_DELIMITER);
                
                    if (i > -1) {
                        protocol = getProtocolURI(us.substring(0, i));
                    }
                }
                
                String host = null;
                
                if (u != null) {
                    host = u.getHost();
                    if (host == null) {
                        int i = us.indexOf(Protocol.URI_DELIMITER);
                        int j = us.lastIndexOf(COLON);

                        if (i > -1 &&
                            j > i &&
                            i + Protocol.URI_DELIMITER.length() != j) {
                            host = us.substring(i +
                                Protocol.URI_DELIMITER.length(), j);
                        } else {
                            host = IPUtils.ANYADDRESS.getHostAddress();
                        }
                    }
                } else {
                    host = IPUtils.ANYADDRESS.getHostAddress();
                }
                
                if (host.indexOf(COLON) > -1) {
                    host = BRACKET_OPEN + host + BRACKET_CLOSE;
                }
                
                int port = u != null ? u.getPort() : -1;
                                
                if (port == -1) {
                    int i = us.indexOf(Protocol.URI_DELIMITER);
                    int j = us.lastIndexOf(COLON);

                    if (j > -1 &&
                        j > i) {
                        try {
                            port = Integer.valueOf(us.substring(j +
                                COLON.length()).trim()).intValue();
                        } catch (NumberFormatException nfe) {
                            if (LOG.isEnabledFor(Level.INFO)) {
                                LOG.info("invalid port: " +
                                us.substring(j + COLON.length()).trim());
                            }
                        }
                    }
                }

                u = makeAddress(protocol, host,
                    port != -1 ? port : getProtocolPort(us));
            }
        }
        
        return u;
    }
    
    private String getProtocolURI(String s) {
        String p = null;
        
        s = s != null ? s.trim().toLowerCase() : null;
        
        if (s != null &&
            s.length() > 0) {
            if (Protocol.TCP_URI.toLowerCase().indexOf(s) > -1) {
                p = Protocol.TCP_URI;
            } else if (Protocol.HTTP_URI.toLowerCase().indexOf(s) > -1) {
                p = Protocol.HTTP_URI;
            } else if (Protocol.UDP_URI.toLowerCase().indexOf(s) > -1) {
                p = Protocol.UDP_URI;
            }
        }
        
        return p;
    }
    
    private int getProtocolPort(String s) {
        int p = -1;
        
        s = s != null ? s.trim().toLowerCase() : null;
        
        if (s != null &&
            s.length() > 0) {
            if (Protocol.TCP_URI.toLowerCase().indexOf(s) > -1) {
                p = Default.TCP_PORT;
            } else if (Protocol.HTTP_URI.toLowerCase().indexOf(s) > -1) {
                p = Default.HTTP_PORT;
            }
        }
        
        return p;
    }
    
    private URI makeAddress(String protocol, String addr, int port) {
        URI u = null;
        
        if (addr != null &&
            addr.trim().length() > 0) {
            try {
                u = new URI( protocol + addr.trim() + COLON + port);
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid tcp address", use);
                }
            }
        } else {
            try {
                u = new URI(protocol + Util.getLocalHost() + COLON + port);
            } catch (URISyntaxException use) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("invalid tcp address", use);
                }
            }
        }
        
        return u;
    }
    
    private void setCustomParams(PlatformConfig pc) {
        for (Iterator si = pc.getServiceParamsEntrySet().iterator();
            si.hasNext(); ) {
            Entry entry = (Entry)si.next();
            ID key = (ID)entry.getKey();
            
            if (! ModuleId.isNormalService(key)) {
                customParams.put(key, pc.getServiceParam(key));
            }
        }
    }
}

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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: PeerGroupFactory.java,v 1.1 2007/01/16 11:02:11 thomas Exp $
 */

package net.jxta.peergroup;


import java.io.FileInputStream;
import java.net.URI;
import java.util.ResourceBundle;
import java.util.PropertyResourceBundle;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.protocol.ModuleImplAdvertisement;

import net.jxta.exception.PeerGroupException;
import net.jxta.exception.JxtaError;

import net.jxta.impl.config.Config;

/**
 * A factory for instantiating the JXTA core peer groups and application peer
 * peer groups.
 *
 * <p/>JXTA comes with two peergroup implementations:
 *
 * <dl>
 * <DT><strong>Platform</strong></DT>
 * <DD>Implements the world peer group. Every peer starts by instantiating this
 * peer group and then other peer groups are instantiated as needed.
 *
 * <p/>The world peer group provides the minimum core services needed to find
 * and instantiate other groups on a peer. <strong>Platform</strong> has the
 * privilege of assigning a new ID to the peer, if it does not already have one.
 * The <em>World</em> peer group's ID is invariant.</DD>
 *
 * <DT><strong>StdPeergroup</strong></DT>
 * <DD>this is currently used to implement all other kinds of peer groups.
 * The first such peer group that it is instantiated after starting is known as
 * <em>The Net Peer Group</em>. When the <strong>Platform</strong> starts it may
 * optionaly search for <em>The Net Peer Group</em> on the local network and,
 * if found, instantiate it. Otherwise a default built-in configuration of
 * <em>The Net Peer Group</em> is instantiated.
 *
 * <p/>A non-default configuration of <em>The Net Peer Group</em> may be set-up
 * by the administrator in charge of the network domain inside which the peer
 * is starting. <em>The Net Peer Group</em> is discovered via the DiscoveryService
 * protocol. Many such groups may be configured by an administrator.<br>
 *
 * <p/><strong>StdPeergroup</strong> may also be used to implement User-defined
 * peer groups--Users can create new peer groups which use their own set of
 * customized services.</DD>
 *</dl>
 *
 *  @see net.jxta.peergroup.PeerGroup
 */
public final class PeerGroupFactory {

    /**
     *  Log4J Logger
     */
    private final static Logger LOG = Logger.getLogger(PeerGroupFactory.class.getName());

    /**
     *  Constant for specifying no configurator.
     */
    public final static Class NULL_CONFIGURATOR = net.jxta.impl.peergroup.NullConfigurator.class;

    /**
     *  Constant for specifying the default configurator.
     */
    public final static Class DEFAULT_CONFIGURATOR = net.jxta.impl.peergroup.DefaultConfigurator.class;

    /**
     *  Platform (World) Peer Group instances will be created as instances of this class.
     */
    private Class platformClass = null;

    /**
     *  Peer Group instances, other than the Platform (World) Peer Group will be
     *  created as instances of this class.
     */
    private Class stdPeerGroupClass = null;

    /**
     *  The ID of the network peer group.
     */
    private PeerGroupID netPGID = null;

    /**
     *  The name of the network peer group.
     */
    private String netPGName = null;

    /**
     *  The description of the network peer group.
     */
    private String netPGDesc;

    /**
     *  The class which will be instantiated to configure the Platform Peer
     *  Group.
     */
    private Class configurator;

    /**
     *  Singleton which holds configuration parameters.
     */
    private final static PeerGroupFactory factory = new PeerGroupFactory();

    /**
     *  Read the configuration parameters for the Peer Group factory.
     */
    private PeerGroupFactory() {
        String platformPGClassName = "net.jxta.impl.peergroup.Platform";
        String stdPGClassName = "net.jxta.impl.peergroup.StdPeerGroup";

        netPGID = PeerGroupID.defaultNetPeerGroupID;
        netPGName = "NetPeerGroup";
        netPGDesc = "default Net Peer Group";

        String configuratorClassname = "net.jxta.impl.peergroup.DefaultConfigurator";

        try {
            try {
                ResourceBundle rsrcs = ResourceBundle.getBundle("net.jxta.impl.config");

                try {
                    platformPGClassName = rsrcs.getString("PlatformPeerGroupClassName").trim();
                } catch (Exception ignored) {
                    ;
                }

                try {
                    stdPGClassName = rsrcs.getString("StdPeerGroupClassName").trim();
                } catch (Exception ignored) {
                    ;
                }

                try {
                    configuratorClassname = rsrcs.getString("ConfiguratorClassName").trim();
                } catch (Exception ignored) {
                    ;
                }

                // Name, desc, and ID must all be set or not at all.
                // If one is missing we exception out and do none.
                // else, we set them all.
                try {
                    String idTmpStr = rsrcs.getString("NetPeerGroupID").trim();
                    PeerGroupID idTmp;

                    if (!idTmpStr.startsWith("jxta:")) {
                        idTmp = (PeerGroupID) IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + ID.URNNamespace + ":" + idTmpStr));
                    } else {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Custom Net Peer Group ID is in deprecated form--remove 'jxta:'");
                        }
                        idTmp = (PeerGroupID) IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + idTmpStr));
                    }
                    String nameTmp = rsrcs.getString("NetPeerGroupName").trim();
                    String descTmp = rsrcs.getString("NetPeerGroupDesc").trim();

                    netPGID = idTmp;
                    netPGName = nameTmp;
                    netPGDesc = descTmp;
                } catch(Exception ignored) {
                    ;
                }
            } catch(Exception ignored) {
                ;
            }

            try {
                String propertiesName = Config.JXTA_HOME + "config.properties";
                ResourceBundle rsrcs = new PropertyResourceBundle(new FileInputStream(propertiesName));

                try {
                    platformPGClassName = rsrcs.getString("PlatformPeerGroupClassName").trim();
                } catch(Exception ignored) {
                    ;
                }

                try {
                    stdPGClassName = rsrcs.getString("StdPeerGroupClassName").trim();
                } catch(Exception ignored) {
                    ;
                }

                try {
                    configuratorClassname = rsrcs.getString("ConfiguratorClassName").trim();
                } catch(Exception ignored) {
                    ;
                }

                // Name, desc, and ID must all be set or not at all.
                // If one is missing we exception out and do none.
                // else, we set them all.
                try {
                    String idTmpStr = rsrcs.getString("NetPeerGroupID").trim();
                    PeerGroupID idTmp;

                    if (!idTmpStr.startsWith("jxta:")) {
                        idTmp = (PeerGroupID) IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + ID.URNNamespace + ":" + idTmpStr));
                    } else {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("Custom Net Peer Group ID is in deprecated form--remove 'jxta:'");
                        }
                        idTmp = (PeerGroupID) IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + idTmpStr));
                    }
                    String nameTmp = rsrcs.getString("NetPeerGroupName").trim();
                    String descTmp = rsrcs.getString("NetPeerGroupDesc").trim();

                    netPGID = idTmp;
                    netPGName = nameTmp;
                    netPGDesc = descTmp;
                } catch(Exception ignored) {
                    ;
                }
            } catch(Exception nevermind) {
                ;
            }

            platformClass = Class.forName(platformPGClassName);
            stdPeerGroupClass = Class.forName(stdPGClassName);

            configurator = Class.forName(configuratorClassname);
        } catch(Throwable e) {
            LOG.fatal("Could not initialize platform and standard peer group classes", e);
            System.exit(1);         // make note that we abended
        }
    }

    /**
     * Static Method to initialize the Platform peer group class
     *
     *  @param c The Class which will be instantiated for the World Peer Group
     */
    public static void setPlatformClass(Class c) {
        if (!net.jxta.peergroup.PeerGroup.class.isAssignableFrom(c)) {
            throw new ClassCastException("Not a valid PeerGroup class");
        }

        factory.platformClass = c;
    }

    /**
     *  Static Method to initialize the std peer group class
     *
     *  @param c Class to use for for general peer group creation.
     */
    public static void setStdPeerGroupClass(Class c) {
        if (!net.jxta.peergroup.PeerGroup.class.isAssignableFrom(c)) {
            throw new ClassCastException("Not a valid PeerGroup class");
        }

        factory.stdPeerGroupClass = c;
    }

    /**
     * Static Method to initialize the net peer group description.
     *
     *  @param desc The description to use for the net peer group.
     *
     *  @since JXTA 2.1.1
     */
    public static void setNetPGDesc(String desc) {
        factory.netPGDesc = desc;
    }

    /**
     * Static Method to initialize the net peer group name.
     *
     *  @param name The name to use for the net peer group.
     *
     *  @since JXTA 2.1.1
     */
    public static void setNetPGName(String name) {
        factory.netPGName = name;
    }

    /**
     * Static Method to initialize the net peer group ID.
     *
     * @param id The id to use for the net peer group.
     *
     * @since JXTA 2.1.1
     */
    public static void setNetPGID(PeerGroupID id) {
        factory.netPGID = id;
    }

    /**
     * Get the configurator class for the platform.
     *
     * @since JXTA 2.2
     *
     * @return Class configurator class
     */
    public static Class getConfiguratorClass() {
        return factory.configurator;
    }

    /**
     * Set the configurator class for the platform.
     *
     * @since JXTA 2.2
     *
     * @param c Class to use as a configurator.
     */
    public static void setConfiguratorClass(Class c) {
        if (!net.jxta.peergroup.Configurator.class.isAssignableFrom(c)) {
            throw new ClassCastException("Not a valid configurator class");
        }

        factory.configurator = c;
    }

    /**
     * Static Method to create a new peer group instance.
     *
     * <p/>After beeing created the init method needs to be called, and
     * the startApp() method may be called, at the invoker's discretion.
     *
     *  @return PeerGroup instance of a new PeerGroup
     */
    public static PeerGroup newPeerGroup() {
        try {
            return (PeerGroup) factory.stdPeerGroupClass.newInstance();
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to construct peer group", e);
            }
            throw new JxtaError("Failed to construct peer group", e);
        }
    }

    /**
     * Instantiates the Platform Peer Group.
     *
     * <p/>The {@link PeerGroup#init(PeerGroup,ID,Advertisement)} method is
     * called automatically. The {@link PeerGroup#startApp(String[])} method
     * is left for the invoker to call if appropriate.
     *
     * <p/>Invoking this method amounts to creating an instance of JXTA.
     *
     * <p/>Since JXTA stores its persistent state in the local filesystem
     * relative to the initial current directory, it is unadvisable to
     * start more than one instance with the same current directory.
     *
     *  @return PeerGroup instance of a new Platform
     */
    public static PeerGroup newPlatform() {

        PeerGroup plat = null;
        try {
            plat = (PeerGroup) factory.platformClass.newInstance();
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Could not instantiate Platform", e);
            }
            throw new JxtaError("Could not instantiate Platform", e);
        }

        try {
            plat.init(null, PeerGroupID.worldPeerGroupID, null);
            return (PeerGroup) plat.getInterface();
        } catch(RuntimeException e) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("newPlatform failed", e);
            }
            // rethrow
            throw e;
        } catch(Exception e) {
            // should be all other checked exceptions
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("newPlatform failed", e);
            }

            // Simplify exception scheme for caller: any sort of problem wrapped
            // in a PeerGroupException.
            throw new JxtaError("newPlatform failed", e);
        }
    }


    /**
     * Instantiates the net peer group using the provided platform peer group.
     *
     * @param ppg The platform group.
     * @return PeerGroup The default netPeerGroup
     */
    public static PeerGroup newNetPeerGroup(PeerGroup ppg) throws PeerGroupException {

        try {
            // Build the group based on our config.
            PeerGroup newPg = ppg.newGroup(factory.netPGID,
                                           // Platform knows what to do
                                           ppg.getAllPurposePeerGroupImplAdvertisement(),
                                           factory.netPGName,
                                           factory.netPGDesc);
            return newPg;
        } catch(PeerGroupException failed) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("newNetPeerGroup failed", failed);
            }
            // rethrow
            throw failed;
        } catch(RuntimeException e) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("newNetPeerGroup failed", e);
            }
            // rethrow
            throw e;
        } catch(Exception e) {
            // should be all other checked exceptions
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("newNetPeerGroup failed", e);
            }
            // Simplify exception scheme for caller: any sort of problem wrapped
            // in a PeerGroupException.
            throw new PeerGroupException("newNetPeerGroup failed", e);
        }
    }

    /**
     * Instantiates the platform peergroup and then instantiates the net peer
     * group. This simplifies the method by which applications can start JXTA.
     *
     *  @return The newly instantiated net peer group.
     */
    public static PeerGroup newNetPeerGroup() throws PeerGroupException {
        // create the  default Platform Group.
        PeerGroup platformGroup = newPlatform();
        try {
            PeerGroup npg = newNetPeerGroup(platformGroup);
            return npg;
        } finally {
            platformGroup.unref();
        }
    }
}

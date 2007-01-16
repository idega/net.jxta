/*
 *  Copyright (c) 2001-2003 Sun Microsystems, Inc.  All rights reserved.
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
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: NullConfigurator.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */
package net.jxta.impl.peergroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.StringTokenizer;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.protocol.ConfigParams;

import net.jxta.exception.ConfiguratorException;

import net.jxta.impl.config.Config;
import net.jxta.impl.protocol.PlatformConfig;

/**
 * A minimal Platform Configurator. This implementation can load a
 * configuration from an existing PlatformConfig file and also save a
 * configuration to the PlatformConfig file.
 *
 * <p/>This configurator provides no explict validation of the PlatformConfig
 * as it is read from the file (Some is done by the PlatformConfig class) and
 * provides no mechanism for reconfiguration. The NullConfigurator provides a
 * useful base implementation for extending your own Configurator and also
 * provides the minimal implementation needed for applications which perform
 * their own configuration.
 */
public class NullConfigurator implements PlatformConfigurator {
    
    /**
     *  Log4J logger
     **/
    private final transient static Logger LOG = Logger.getLogger(NullConfigurator.class.getName());
    
    /**
     *  File name into which we will store the platform configuration.
     **/
    protected static final String CONFIGFILENAME = "PlatformConfig";
    
    /**
     *  File name into which we will store various configuration properties.
     **/
    protected static final String PROPERTIESFILENAME = "jxta.properties";
    
    /**
     *  The directory in which the configuration files will reside.
     **/
    protected final File jxtaHomeDir;
    
    /**
     *  The file in which contains the platform configurtation.
     **/
    protected final File configFile;
    
    /**
     *  The file in which contains the configurtation properties.
     **/
    protected final File propertiesFile;
    
    /**
     *  The platform config
     **/
    protected PlatformConfig advertisement = null;
    
    /**
     * Constructor for the NullConfigurator object
     */
    public NullConfigurator() throws ConfiguratorException {
        this( new File( Config.JXTA_HOME ) );
    }
    
    public NullConfigurator( File homeDir ) throws ConfiguratorException {
        jxtaHomeDir = homeDir;
        
        if (LOG.isEnabledFor(Level.INFO)) {
            try {
                LOG.info("JXTA_HOME = " + jxtaHomeDir.getCanonicalPath() );
            } catch ( IOException caught ) {
                LOG.info( "JXTA_HOME (which doesn't seem to exist) = " + jxtaHomeDir.getAbsolutePath() );
            }
        }
        
        if( jxtaHomeDir.exists() && !jxtaHomeDir.isDirectory() ) {
            throw new IllegalArgumentException( "'" + Config.JXTA_HOME + "' is not a directory." );
        }
        
        if( !jxtaHomeDir.exists() ) {
            if( !jxtaHomeDir.mkdirs() ) {
                throw new IllegalStateException( "Could not create '" + Config.JXTA_HOME + "'." );
            }
        }
        
        configFile = new File( jxtaHomeDir, CONFIGFILENAME );
        
        propertiesFile = new File( jxtaHomeDir, PROPERTIESFILENAME );
        
        // Reset from resource.
        resetFromResource( CONFIGFILENAME, configFile );
        resetFromResource( PROPERTIESFILENAME, propertiesFile );
        
        // Setup authentication for the http proxy
        String proxyUser = System.getProperty("jxta.proxy.user");
        String proxyPassword = System.getProperty("jxta.proxy.password");
        if (proxyUser != null && proxyPassword !=null) {
            char [] proxyPass = proxyPassword.toCharArray();
            PasswordAuthentication pass = new PasswordAuthentication(proxyUser,proxyPass);
            Authenticator.setDefault(new ProxyAuthenticator(pass));
            
            // we created PasswordAuthentication, destroy the props
            System.getProperties().remove("jxta.proxy.user");
            System.getProperties().remove("jxta.proxy.password");
        }
    }
    
    /**
     * @inheritDoc
     **/
    public URI getJXTAHome() {
        return this.jxtaHomeDir.toURI();
    }
    
    /**
     * @inheritDoc
     **/
    public PlatformConfig getPlatformConfig() throws ConfiguratorException {
        advertisement = (PlatformConfig) load();
        
        // Set the global debug level.
        adjustLog4JPriority();
        
        return advertisement;
    }
    
    /**
     * @inheritDoc
     **/
    public final void setPlatformConfig(PlatformConfig config) {
        advertisement = (PlatformConfig) config;
        
        // Set the global debug level.
        adjustLog4JPriority();
    }
    
    /**
     * @inheritDoc
     **/
    public ConfigParams getConfigParams() throws ConfiguratorException {
        return getPlatformConfig();
    }
    
    /**
     * @inheritDoc
     **/
    public void setConfigParams(ConfigParams cp) {
        setPlatformConfig( (PlatformConfig) cp );
    }
    
    /**
     * @inheritDoc
     **/
    public void setReconfigure(boolean reconfigure) {
    }
    
    /**
     * @inheritDoc
     **/
    public boolean isReconfigure() {
        return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public ConfigParams load()  throws ConfiguratorException {
        return load(configFile);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public PlatformConfig load( File loadFile ) throws ConfiguratorException {
        if( LOG.isEnabledFor(Level.DEBUG) ) {
            LOG.debug( "Reading Platform Config from : " + loadFile.getAbsolutePath() );
        }
        
        FileInputStream advStream = null;
        try {
            advStream = new FileInputStream(loadFile);
            
            Reader advReader = new InputStreamReader( advStream, "UTF-8" );
            
            PlatformConfig result = (PlatformConfig) AdvertisementFactory.newAdvertisement( MimeMediaType.XMLUTF8, advReader );
            
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Recovered Platform Config from : " + loadFile.getAbsolutePath() );
            }
            
            return result;
        } catch (FileNotFoundException e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Platform Config not found : " + loadFile.getAbsolutePath() );
            }
            
            return null;
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Failed to Recover '" + loadFile.getAbsolutePath() + "' due to : ", e);
            }
            
            try {
                // delete that bad file.
                loadFile.delete();
            } catch (Exception ex1) {
                LOG.fatal( "Could not remove bad PlatformConfig file", ex1 );
                
                throw new ConfiguratorException( "Could not remove '" +
                loadFile.getAbsolutePath() +
                "'. Remove it by hand before retrying", ex1 );
            }
            
            throw new ConfiguratorException( "Failed to recover PlatformConfig", e );
        } finally {
            try {
                if (advStream != null) {
                    advStream.close();
                }
                advStream = null;
            } catch (Exception ignored ) {
                ;
            }
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean save() throws ConfiguratorException {
        return save(configFile);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean save(File saveFile ) throws ConfiguratorException {
        
        // Save the adv as input for future reconfiguration
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(saveFile);
            
            XMLDocument aDoc = (XMLDocument) advertisement.getDocument(MimeMediaType.XMLUTF8);
            
            OutputStreamWriter os = new OutputStreamWriter(out, "UTF-8");
            
            aDoc.sendToWriter( os );
            os.flush();
        } catch (IOException e) {
            if ( LOG.isEnabledFor(Level.WARN) ) {
                LOG.warn("Could not save to : " + saveFile.getAbsolutePath(), e );
            }
            
            throw new ConfiguratorException( "Could not save to : " + saveFile.getAbsolutePath(), e );
        } finally {
            try {
                if( null != out ) {
                    out.close();
                }
            } catch (Exception ignored ) {;}
            out = null;
        }
        
        return true;
    }
    
    /**
     * Load from Resource if any.
     *
     * <p/>If there is a PlatformConfig file in a resource,
     * AND if there is no existing PlatformConfig in the
     * JXTA_HOME, then copy the file from resource into
     * JXTA_HOME. Otherwise, do nothing.
     **/
    private void resetFromResource(String resourceName, File file) {
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Checkin resource= " + resourceName + " local= " + file.getPath() );
        }
        try {
            
            if (file.exists()) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info(file.getPath() + " already exists");
                }
                // There already is a local config file.
                // Do nothing.
                return;
            }
            
            // Check of there is a config in the resource.
            InputStream in = this.getClass().getResourceAsStream("/" + resourceName);
            
            if (in == null) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error(resourceName + " does not exist");
                }
                // No config in resource.
                return;
            }
            
            // Copy the file onto disk.
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info(resourceName + " reset " + file.getPath() );
            }
            
            FileOutputStream out = null;
            
            try {
                out = new FileOutputStream(file);
                int bufferSize = 16 * 1024;
                byte[] buffer = new byte[bufferSize];
                
                while (true) {
                    
                    int got = in.read(buffer);
                    
                    if (got == -1) {
                        break;
                    }
                    out.write(buffer, 0, got);
                }
            }
            finally {
                try {
                    in.close();
                } catch ( IOException ignored ) {
                    ;
                }
                
                try {
                    if( null != out ) {
                        out.close();
                    }
                } catch ( IOException ignored ) {
                    ;
                }
            }
        } catch (IOException ez) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot reset " + resourceName + " from resource.", ez );
            }
        }
    }
    
    /**
     * Adjust the log4j priority based on the user's configuration file.
     * If the configuration is not set or the value is "user default",
     * then don't change it.
     **/
    protected void adjustLog4JPriority() {
        if (advertisement == null || advertisement.getDebugLevel() == null) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Log4J logging preference not set, using defaults");
            }
            return;
        }
        
        String requestedLevel = advertisement.getDebugLevel();
        if ("user default".equals(requestedLevel)) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Log4J [user default] requested, not adjusting logging priority");
            }
            return;
        }
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info("Setting Log4J priority to [" + requestedLevel + "] based on the user's configuration");
        }
        Logger jxtaLogger = Logger.getLogger("net.jxta");
        jxtaLogger.setLevel(Level.toLevel(requestedLevel));
    }
    
    /**
     * inner class to be used when a proxy user/pass is set
     */
    static class ProxyAuthenticator extends Authenticator {
        PasswordAuthentication password = null;
        public ProxyAuthenticator(PasswordAuthentication password) {
            this.password = password;
        }
        
        /**
         *  {@inheritDoc}
         **/
        protected PasswordAuthentication getPasswordAuthentication() {
            return password;
        }
    }
}

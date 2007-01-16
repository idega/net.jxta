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
 *  $Id: PlatformConfigurator.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */
package net.jxta.impl.peergroup;

import java.net.URI;
import java.io.File;

import net.jxta.peergroup.Configurator;
import net.jxta.protocol.ConfigParams;

import net.jxta.exception.ConfiguratorException;
import net.jxta.impl.protocol.PlatformConfig;

/**
 * Defines a configurator for the JXTA Platform peer group.
 *
 * @author     james todd [gonzo at jxta dot org]
 */
public interface PlatformConfigurator extends Configurator {
    
    /*
     * Retrieve the Platform Home as a file
     *
     * note: this api is provided for Configurator implementors only and
     *       will likely change in the future.
     *
     * @deprecated
     */
    public URI getJXTAHome();
    
    /**
     * Retrieve the associated {@link net.jxta.impl.protocol.PlatformConfig} and
     * potentially reconfigure the parameters before returning.
     *
     * @return PlatformConfig
     */
    public PlatformConfig getPlatformConfig() throws ConfiguratorException;
    
    /**
     * Sets the associated {@link net.jxta.impl.protocol.PlatformConfig}.
     *
     * @param pc
     */
    public void setPlatformConfig( PlatformConfig pc );
    
    /**
     * Persist the associated{ @link net.jxta.impl.protocol.PlatformConfig} to
     * the specified {@link java.io.File location}.
     *
     * @deprecated Sub-classes should devise their own, more appropriate method.
     *
     * @param f The file to which the configuration will be saved.
     * @return <code>true</code> if the configuration was successfully saved
     *  otherwise <code>false</code>. If the parameters are not persisted then
     *  <code>false/code> is returned.
     */
    public PlatformConfig load(File f) throws ConfiguratorException;

    /**
     * Persist the associated{ @link net.jxta.impl.protocol.PlatformConfig} to
     * the specified {@link java.io.File location}.
     *
     * @deprecated Sub-classes should devise their own, more appropriate method.
     *
     * @param f The file to which the configuration will be saved.
     * @return <code>true</code> if the configuration was successfully saved
     *  otherwise <code>false</code>. If the parameters are not persisted then
     *  <code>false/code> is returned.
     */
    public boolean save(File f) throws ConfiguratorException;
    
    /**
     * Sets the reconfiguration status to the specified status. If
     * <code>true</code> then reconfiguration will be forced the next time the
     * {@link net.jxta.impl.protocol.PlatformConfig} is retrieved.
     *
     * @param forceReconfig If <code>true</code> then a forced reconfiguration
     * will occur the next time {@link #getPlatformConfig()} is called.
     */
    public void setReconfigure(boolean forceReconfig);
    
    /**
     * Determine if a forced reconfiguration is set for the next call to
     * {@link #getPlatformConfig()}.
     *
     * @return Returns <code>true</code> if a forced reconfiguration
     * will occur the next time {@link #getPlatformConfig()} is called.
     **/
    public boolean isReconfigure();
}

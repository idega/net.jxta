/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
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
 * DISCLAIMED.  IN NO EVENT SHALL THE SUN MICROSYSTEMS OR
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
 * $Id: JxtaLoader.java,v 1.1 2007/01/16 11:02:01 thomas Exp $
 */

package net.jxta.platform;

import java.net.URL;
import java.net.URLClassLoader;

import net.jxta.protocol.ModuleImplAdvertisement;

/**
 * A ClassLoader which provides additional JXTA functionality. You can load
 * classes by ModuleSpecID. Classes are defiend with ModuleImplAdvertisements
 * and class loading will determine suitability using the provided
 * compatiblity statements.
 **/
public abstract class JxtaLoader extends URLClassLoader {
    
    /**
     *  Constuct a new loader with the specified parent loader and
     *
     *  @param parent  the parent class loader for delegation.
     **/
    public JxtaLoader( ClassLoader parent ) {
        this( new URL[0], parent );
    }
    
    /**
     * Constuct a new loader for the specified URLS with the specified parent
     * loader.
     *
     *  @param urls  the URLs from which to load classes and resources.
     *  @param parent  the parent class loader for delegation.
     **/
    public JxtaLoader( URL[] urls, ClassLoader parent ) {
        super( urls, parent );
    }
    
    /**
     * Finds and loads the class with the specified spec ID from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     *  @param spec the specid of the class to load.
     *  @throws ClassNotFoundException if the class could not be found.
     *  @return the resulting class.
     **/
    public abstract Class findClass( ModuleSpecID spec ) throws ClassNotFoundException;
    
    /**
     *  Loads the class with the specified spec ID from the URL search
     *  path.
     *
     *  @param spec the specid of the class to load.
     *  @throws ClassNotFoundException if the class could not be found.
     *  @return the resulting class.
     **/
    public abstract Class loadClass( ModuleSpecID spec ) throws ClassNotFoundException;
    
    /**
     *  Defines a new class from a Module Impl Advertisement.
     *
     *  @param impl The moduleImplAdvertisement containing the class 
        specification
     *  @return The Class object that was created from the specified class data.
     **/
    public abstract Class defineClass( ModuleImplAdvertisement impl );
}

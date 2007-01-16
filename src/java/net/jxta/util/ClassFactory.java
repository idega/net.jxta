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
 * $Id: ClassFactory.java,v 1.1 2007/01/16 11:01:35 thomas Exp $
 */

package net.jxta.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.MissingResourceException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * This util class provides methods needed by class construction factories.
 *
 * @see net.jxta.document.StructuredDocumentFactory
 * @see net.jxta.document.AdvertisementFactory
 * @see net.jxta.id.IDFactory
 * @see net.jxta.endpoint.WireFormatMessageFactory
 **/
public abstract class ClassFactory {

	/**
	 *  Log4J Logger
	 **/
	private static final transient Logger LOG = Logger.getLogger( ClassFactory.class.getName());


	/**
	 *  Constructor for ClassFactory. Instances of ClassFactory are created only
	 *  by the singleton subclasses.
	 **/
	protected ClassFactory() {
	}

	/**
	 *  Used by ClassFactory methods to get the mapping of keys to constructors.
	 *
	 *  @return the hashtable containing the mappings.
	 *
	 **/
	protected abstract Map getAssocTable();

	/**
	 *  Used by ClassFactory methods to ensure that all keys used with the
	 *  mapping are of the correct type.
	 *
	 *
	 *  @return Class object of the key type.
	 **/
	protected abstract Class getClassForKey();

	/**
	 *  Return all of the available keys for this factory. All elements will be
	 *  of the same type as the result as <code>getClassForKey</code>.
	 *
	 *  @return Iterator containing all of the available keys for this
	 *   factory. All elements will be of the same type as the result as
	 *   <code>getClassForKey</code>.
	 **/
	public Iterator getAvailableKeys() {
		return Collections.unmodifiableSet(getAssocTable().keySet()).iterator();
	}

	/**
	 *  Returns an umodifiable Set containing all of the associations stored in
     *  this ClassFactory. Each entry is of type <code>Map.Entry</code>.
	 *
	 *  @return Set Containing all of the available entries for this
	 *  factory.
	 **/
	public Set getEntrySet() {
		return Collections.unmodifiableSet(getAssocTable().entrySet());
	}

	/**
	 *  Used by ClassFactory methods to ensure that all of the instance classes
	 *  which register with this factory have the correct base class
	 *
	 *
	 *  @return Class object of the "Factory" type.
	 **/
	protected abstract Class getClassOfInstantiators();

	/**
	 * Given a resource bundle identifier and a property name register instance
	 * classes. The property must be a string containing class names which must
	 * be found on the current class path. The class names are seperated by
	 * spaces.
	 *
	 *
	 * @param resourceName name of the resource bundle
	 * @param propertyName name of the property.
	 * @return boolean true if at least one instance class could be registered 
	    with this factory.
	 * @exception MissingResourceException if the resource bundle or
	 * property cannot be located.
	 **/
	protected boolean registerFromResources( String resourceName, String propertyName ) throws MissingResourceException {

		java.util.ResourceBundle jxtaRsrcs = java.util.ResourceBundle.getBundle( resourceName );
		String fromProps = jxtaRsrcs.getString( propertyName ).trim();

		return registerFromString( fromProps );
	}

	/**
	 *  Register instance classes given a string containing class names which
	 *  must be found on the current class path. The class names are seperated
	 *  by spaces.
	 *
	 *
	 *  @param classNamesString The class name list
	 *  @return boolean true if at least one of the instance classes could be
	 *  registered otherwise false.
	 **/
	protected boolean registerFromString( String classNamesString ) {
		boolean registeredSomething = false;

		if( (null == classNamesString) || (0 == classNamesString.length()) )
			return false;

		// make sure the static initialisers for each instance class are called.
		for( java.util.StringTokenizer eachInstanceClass =
		            new java.util.StringTokenizer(classNamesString);
		        eachInstanceClass.hasMoreTokens();  ) {
			String willInitialize = (String) eachInstanceClass.nextToken();

			try {
				registeredSomething |= registerAssoc( willInitialize );
			}
			catch( Exception allElse ) {
				if (LOG.isEnabledFor(Level.WARN))
					LOG.warn( "Failed to register '" + willInitialize + "'", allElse );
			}
		}

		return registeredSomething;
	}

	/**
	 *  Register a class with the factory from its class name. Since class name
	 *  doesn't tell us much, we just load the class and hope that something
	 *  happens as a result of the class loading. This class is often overridden
	 *  in class factories to interogate the instance class before registering
	 *  the instance class.
	 *
	 *
	 * @param className The class name which will be regiestered.
	 * @return boolean true if the class was registered otherwise false.
	 * @throws Exception   when an error occurs.
	 **/
	protected boolean registerAssoc( final String className ) throws Exception {

		boolean registeredSomething = false;

		try {
			Class ignored = Class.forName( className );
			registeredSomething = true;
		} catch( ClassNotFoundException ignored ) {
			if (LOG.isEnabledFor(Level.WARN))
				LOG.warn( "Failed to locate '" + className + "'" );
		} catch( NoClassDefFoundError ignored ) {
			if (LOG.isEnabledFor(Level.WARN))
				LOG.warn( "Failed to locate '" + className + "'" );
		}

		return registeredSomething;
	}

	/**
	 * Register a key and instance class with the factory.
	 *
	 *  @param key The key to register.
	 *  @param instantiator The instantiator object which will be registered for this key.
	 *  @return boolean true if the key was successfully registered otherwise false.
	 **/
	protected boolean registerAssoc( final Object key, final Object instantiator ) {

		if( !getClassOfInstantiators().isInstance( instantiator ) )
			throw new ClassCastException( "instantiator '" +
			                              instantiator.getClass().getName()+"' does not implement '" +
			                              getClassOfInstantiators().getName() + "'" );

		// Check the class of the key to make sure it is of the right class
		Class requiredKeyClass = getClassForKey();
		Class itsA =  key.getClass();
		if( !requiredKeyClass.isAssignableFrom(itsA ) )
			throw new IllegalArgumentException( "Incorrect Class for key type" );

		// Check the association table to make sure this key is not already present.
		if( null != getAssocTable().get( key ) )
			return false;

		getAssocTable().put( key, instantiator );

		if (LOG.isEnabledFor(Level.DEBUG))
            LOG.debug( "Factory : " + getClass().getName() + " Registered instantiator '" + instantiator + "' for '" + key + "'"  );

		return true;
	}

	/**
	 *  Return the instantiator associated with the provided key.
	 *
	 *  @param key The identifier for the Instantiator class to be returned.
	 *  @return Instantiator Instantiator matching the provided key
	 *  @throws NoSuchElementException if the key has not been registerd.
	 **/
	protected Object getInstantiator( final Object key )
	throws NoSuchElementException {

		Class requiredKeyClass = getClassForKey();

		if( !requiredKeyClass.isAssignableFrom( key.getClass() ) )
			throw new IllegalArgumentException( "Incorrect Class for key type" );

		// Get the constructors for this key.
		Object instantiator = getAssocTable().get( key );

		if( null == instantiator )
			throw new NoSuchElementException( "key '" + key + "' not registered." );

		return instantiator;
	}
}

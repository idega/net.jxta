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
 * $Id: ConfigParams.java,v 1.1 2007/01/16 11:01:34 thomas Exp $
 */

package net.jxta.protocol;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

/**
 * A container for configuration parameters.
 *
 **/
public abstract class ConfigParams extends ExtendableAdvertisement {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(ConfigParams.class.getName());
    
    private static final String SVC_TAG = "Svc";
    private static final String MCID_TAG = "MCID";
    private static final String PARAM_TAG = "Parm";
    
    /**
     * A table of structured documents to be interpreted by each service.
     * For safe operation these elements should be immutable, but we're helpless
     * if they are not. Parameters for services associated with this peer. May
     * be needed for invocation of those services.
     **/
    private Map params = new HashMap();
    
    /**
     * Counts the changes made to this object. The API increments it every time
     * some change is not proven to be idempotent. We rely on implementations to
     * increment modCount every time something is changed without going through
     * the API.
     **/
    protected transient volatile int modCount = 0;
    
    /**
     * Returns the number of times this object has been modified since
     * it was created.
     * This permits the detection of local changes that require refreshing some
     * other data.
     *
     * @return int the current modification count.
     */
    public int getModCount() {
        return modCount;
    }
    
    /**
     *  Increases the modification count of the
     **/
    protected synchronized int incModCount() {
        return modCount++;
    }
    
    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public static String getAdvertisementType() {
        return "jxta:CP" ;
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * Puts a service parameter in the service parameters table
     * under the given key. The key is of a subclass of ID; usually a
     * ModuleClassID. This method makes a deep copy of the given element
     * into an independent document.
     *
     * @param key The key.
     * @param param The parameter, as an element. What is stored is a copy as
     * a stand-alone StructuredDocument which type is the element's name.
     */
    public void putServiceParam( ID key, Element param ) {
        incModCount();
        
        if (param == null) {
            params.remove(key);
            return;
        }
        Element newDoc = StructuredDocumentUtils.copyAsDocument(param);
        params.put( key, newDoc);
    }
    
    /**
     * Returns the parameter element that matches the given key from the
     * service parameters table. The key is of a subclass of ID; usually a
     * ModuleClassID.
     *
     * @param key The key.
     * @return StructuredDocument The matching parameter document or null if
     * none matched.
     **/
    public StructuredDocument getServiceParam(ID key) {
        Element param = (Element) params.get(key);
        
        if (param == null) {
            return null;
        }
        
        StructuredDocument newDoc =
        StructuredDocumentUtils.copyAsDocument(param);
        return newDoc;
    }
    
    /**
     * Removes and returns the parameter element that matches the given key
     * from the service parameters table. The key is of a subclass of ID;
     * usually a ModuleClassID.
     *
     * @param key The key.
     *
     * @return Element the removed parameter element or null if not found.
     * This is actually a StructureDocument of type "Param".
     */
    public StructuredDocument removeServiceParam(ID key) {
        
        Element param = (Element) params.remove(key);
        if (param == null) {
            return null;
        }
        
        incModCount();
        
        // It sound silly to clone it, but remember that we could be sharing
        // this element with a clone of ours, so we have the duty to still
        // protect it.
        
        StructuredDocument newDoc =
        StructuredDocumentUtils.copyAsDocument(param);
        
        return newDoc;
    }
    
    /**
     *  Returns the set of params held by this object. The parameters are not
     *  copied and any changes to the Set are reflected in this object's version.
     *  incModCount should be called as appropriate.
     **/
    public Set getServiceParamsEntrySet() {
        return Collections.unmodifiableSet( params.entrySet() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected boolean handleElement( Element raw ) {
        
        if ( super.handleElement( raw ) )
            return true;
        
        XMLElement elem = (XMLElement) raw;
        
        if (elem.getName().equals(SVC_TAG)) {
            Enumeration elems = elem.getChildren();
            ID classID = null;
            Element param = null;
            while (elems.hasMoreElements()) {
                XMLElement e = (XMLElement) elems.nextElement();
                if ( e.getName().equals(MCID_TAG) ) {
                    try {
                        URI mcid = new URI( e.getTextValue() );
                        classID = (ID) IDFactory.fromURI( mcid );
                    } catch ( URISyntaxException badID ) {
                        throw new IllegalArgumentException( "Bad ModuleClassID in advertisement: " + e.getTextValue() );
                    }
                    continue;
                }
                if ( e.getName().equals(PARAM_TAG) ) {
                    param = e;
                    continue;
                }
            }
            if (classID != null && param != null) {
                // Add this param to the table. putServiceParam()
                // clones param into a standalone document automatically.
                // (classID gets cloned too).
                putServiceParam( classID, param );
            } else {
                if ( LOG.isEnabledFor(Level.DEBUG) )
                    LOG.debug( "Incomplete Service Param : id=" + classID + " param=" + param );
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    /**
     *  Return the advertisement as a document.
     *
     *  @param adv the document to add elements to.
     *  @return true if elements were added otherwise false
     **/
    public boolean addDocumentElements( StructuredDocument adv ) {
        Iterator eachParam = getServiceParamsEntrySet().iterator();
        
        if( !eachParam.hasNext() )
            return false;
        
        while( eachParam.hasNext() ) {
            Map.Entry anEntry = (Map.Entry) eachParam.next();
            ID anID = (ID) anEntry.getKey();
            StructuredDocument aDoc = (StructuredDocument) anEntry.getValue();
            
            Element s = adv.createElement(SVC_TAG);
            adv.appendChild(s);
            
            Element e = adv.createElement(MCID_TAG, anID.toString());
            s.appendChild(e);
            
            StructuredDocumentUtils.copyElements(adv, s, aDoc, PARAM_TAG);
        }
        
        return true;
    }
    
}

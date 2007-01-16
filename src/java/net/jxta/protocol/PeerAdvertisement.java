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
 * $Id: PeerAdvertisement.java,v 1.1 2007/01/16 11:01:33 thomas Exp $
 */

package net.jxta.protocol;


import java.net.URI;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;

import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peer.PeerID;


/**
 * Generated when instantiating a group on a peer and contains all the 
 * parameters that services need to publish. It is then published within the
 * group.
 */
public abstract class PeerAdvertisement extends ExtendableAdvertisement implements Cloneable {

    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(PeerAdvertisement.class.getName());
    
    /*
     * FIXME: [20011001 jice@jxta.org]
     * ideally Advertisements should be immutable, but then they become too
     * cumbersome to construct. Therefore we would need an immutable class
     * and a mutable subclass, and provide one or the other depending on some
     * yet to defined privilege or something like that...later.
     */
    
    /**
     * The id of this peer.
     **/
    private PeerID pid = null;
    
    /**
     * The group in which this peer is located.
     **/
    private PeerGroupID gid = null;
    
    /**
     * The name of this peer. Not guaranteed to be unique in any way. May be empty or
     * null.
     **/
    private String name = null;
    
    /**
     * Descriptive meta-data about this peer.
     */
    private Element description = null;
    
    /**
     * A table of structured documents to be interpreted by each service.
     **/
    private final Map serviceParams = new HashMap();
    
    /**
     * Counts the changes made to this object.
     * The API increments it every time some change is not proven to be
     * idempotent.
     * We rely on implementations to increment modCount every time something is
     * changed without going through the API.
     **/
    protected volatile int modCount = 0;
    
    /**
     * Returns the number of times this object has been modified since
     * it was created.
     * This permits to detect local changes that require refreshing some
     * other data.
     * @return int the current modification count.
     */
    
    public int getModCount() {
        return modCount;
    }
    
    protected int incModCount() {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            Throwable trace = new Throwable("Stack Trace");
            StackTraceElement elements[] = trace.getStackTrace();
            
            LOG.debug(
                    "Modification #" + (modCount + 1) + " to PeerAdv@" + Integer.toHexString(System.identityHashCode(this)) + " caused by : " + "\n\t"
                    + elements[1] + "\n\t" + elements[2]);
        }
        
        return modCount++;
    }
    
    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public static String getAdvertisementType() {
        return "jxta:PA";
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * Make a safe clone of this PeerAdvertisement.
     *
     * @return Object an object of class PeerAdvertisement that is a deep-enough
     * copy of this one.
     */
    public Object clone() {
              
        try {
            PeerAdvertisement clone = (PeerAdvertisement) super.clone();
            
            clone.setPeerID(getPeerID());
            clone.setPeerGroupID(getPeerGroupID());
            clone.setName(getName());
            clone.setDesc(getDesc());
            clone.setServiceParams(getServiceParams());
            
            return clone;
        } catch (CloneNotSupportedException impossible) {
            return null;
        }
    }
    
    /**
     * returns the name of the peer.
     *
     * @return String name of the peer.
     */
    
    public String getName() {
        return name;
    }
    
    /**
     * sets the name of the peer.
     *
     * @param name name of the peer.
     */
    
    public void setName(String name) {
        this.name = name;
        incModCount();
    }
    
    /**
     * Returns the id of the peer.
     *
     * @return PeerID the peer id
     */
    
    public PeerID getPeerID() {
        return pid;
    }
    
    /** Sets the id of the peer.
     *
     * @param pid the id of this peer.
     */
    
    public void setPeerID(PeerID pid) {
        this.pid = pid;
        incModCount();
    }
    
    /**
     * Returns the id of the peergroup this peer advertisement is for.
     *
     * @return PeerGroupID the peergroup id
     */
    
    public PeerGroupID getPeerGroupID() {
        return gid;
    }
    
    /**
     * Returns the id of the peergroup this peer advertisement is for.
     *
     * @param gid The id of the peer.
     */
    
    public void setPeerGroupID(PeerGroupID gid) {
        this.gid = gid;
        incModCount();
    }
    
    /**
     * Returns a unique ID for that peer X group intersection. This is for indexing
     * purposes only.
     *
     * <p/>We return a composite ID that represents this peer is this group
     * rather than in the platform, which is what the regular peerId shows.
     *
     * <p/>May-be one day we'll want to name a peer differently
     * in each group, exactly in this way. In the meantime we still need
     * it to uniquely identify this adv.
     *
     * <p/>FIXME 20020604 bondolo@jxta.org This is a total hack as it assumes the
     *  format of a group id. It's supposed to be opaque. The real answer is to
     *  use a unique value within each group.
     *
     * @return ID the composite ID
     */
    
    public ID getID() {
        
        // If it is incomplete, there's no meaninfull ID that we can return.
        if (gid == null || pid == null) {
            return null;
        }
        
        String peer;

        // That's tricky; we're not realy supposed to do that...
        
        // Get the grp unique string of hex. Clip the two type bytes
        // at the end.
        if (gid.equals(PeerGroupID.defaultNetPeerGroupID) || gid.equals(PeerGroupID.worldPeerGroupID)) {
            
            peer = pid.getUniqueValue().toString();
            
        } else {
            String grp = gid.getUniqueValue().toString();

            grp = grp.substring(0, grp.length() - 2);
            
            // Get the peer unique string whih starts with the platform's unique
            // string.
            peer = pid.getUniqueValue().toString();
            // Replace the platform's unique portion with this group's id.
            peer = grp + peer.substring(grp.length());
        }
        
        // Forge a URI form for this chimaera and build a new PeerID out of it.
        try {
            return IDFactory.fromURI(new URI(ID.URIEncodingName + ":" + ID.URNNamespace + ":" + peer));
        } catch (URISyntaxException iDontMakeMistakes) {
            // Fall through,  iDontMakeMistakes sometimes makes mistakes :)
            // iDontMakeMistakes.printStackTrace();
            ;
        }
        // May be if we had an "internal error exception" we should throw it.
        return null;
    }
    
    /**
     * returns the description
     *
     * @return String the description
     */
    public String getDescription() {
        if (null != description) {
            return (String) description.getValue();
        } else {
            return null;
        }
    }
    
    /**
     * sets the description
     *
     * @since JXTA 1.0
     *
     * @param description the description
     */
    public void setDescription(String description) {
        
        if (null != description) {
            StructuredDocument newdoc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Desc", description);
            
            setDesc(newdoc);
        } else {
            this.description = null;
        }
        
        incModCount();
    }
    
    /**
     * returns the description
     *
     * @return the description
     *
     */
    public StructuredDocument getDesc() {
        if (null != description) {
            StructuredDocument newDoc = StructuredDocumentUtils.copyAsDocument(description);
            
            return newDoc;
        } else {
            return null;
        }
    }
    
    /**
     * sets the description
     *
     * @param desc the description
     *
     */
    public void setDesc(Element desc) {
        
        if (null != desc) {
            this.description = StructuredDocumentUtils.copyAsDocument(desc);
        } else {
            this.description = null;
        }
        
        incModCount();
    }
    
    /**
     *  sets the sets of parameters for all services. This method first makes a
     *  deep copy, in order to protect the active information from uncontrolled
     *  sharing. This quite an expensive operation. If only a few of the
     *  parameters need to be added, it is wise to use putServiceParam()
     *  instead.
     *
     *@param  params  The whole set of parameters.
     */
    public void setServiceParams(Hashtable params) {
        serviceParams.clear();
        
        if (params == null) {
            return;
        }
        
        Iterator services = params.entrySet().iterator();

        while (services.hasNext()) {
            Map.Entry anEntry = (Map.Entry) services.next();
            
            Element e = (Element) anEntry.getValue();
            Element newDoc = StructuredDocumentUtils.copyAsDocument(e);

            serviceParams.put(anEntry.getKey(), newDoc);
        }
        
        incModCount();
    }
    
    /**
     *  Returns the sets of parameters for all services. <p/>
     *
     *  This method returns a deep copy, in order to protect the real
     *  information from uncontrolled sharing while keeping it shared as long as
     *  it is safe. This quite an expensive operation. If only a few parameters
     *  need to be accessed, it is wise to use getServiceParam() instead.
     *
     *@return    all of the parameters.
     */
    public Hashtable getServiceParams() {
        Hashtable copy = new Hashtable();
        
        Iterator services = serviceParams.entrySet().iterator();

        while (services.hasNext()) {
            Map.Entry anEntry = (Map.Entry) services.next();
            
            Element e = (Element) anEntry.getValue();
            Element newDoc = StructuredDocumentUtils.copyAsDocument(e);

            copy.put(anEntry.getKey(), newDoc);
        }
        
        return copy;
    }
    
    /**
     *  Puts a service parameter in the service parameters table under the given
     *  key. The key is of a subclass of ID; usually a ModuleClassID. This
     *  method makes a deep copy of the given element into an independent
     *  document.
     *
     *@param  key    The key.
     *@param  param  The parameter, as an element. What is stored is a copy as a
     *      standalone StructuredDocument which type is the element's name.
     */
    public void putServiceParam(ID key, Element param) {
        if (param == null) {
            serviceParams.remove(key);
            incModCount();
            return;
        }
        
        Element newDoc = StructuredDocumentUtils.copyAsDocument(param);

        serviceParams.put(key, newDoc);
        
        incModCount();
    }
    
    /**
     *  Returns the parameter element that matches the given key from the
     *  service parameters table. The key is of a subclass of ID; usually a
     *  ModuleClassID.
     *
     *@param  key  The key.
     *@return      StructuredDocument The matching parameter document or null if
     *      none matched. The document type id "Param".
     */
    public StructuredDocument getServiceParam(ID key) {
        Element param = (Element) serviceParams.get(key);

        if (param == null) {
            return null;
        }
        
        return  StructuredDocumentUtils.copyAsDocument(param);
    }
    
    /**
     *  Removes and returns the parameter element that matches the given key
     *  from the service parameters table. The key is of a subclass of ID;
     *  usually a ModuleClassID.
     *
     *@param  key  The key.
     *@return      Element the removed parameter element or null if not found.
     *      This is actually a StructureDocument of type "Param".
     */
    public StructuredDocument removeServiceParam(ID key) {
        Element param = (Element) serviceParams.remove(key);

        if (param == null) {
            return null;
        }
        
        incModCount();
               
        return StructuredDocumentUtils.copyAsDocument(param);
    }
}

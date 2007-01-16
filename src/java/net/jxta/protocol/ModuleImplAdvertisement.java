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
 * $Id: ModuleImplAdvertisement.java,v 1.1 2007/01/16 11:01:33 thomas Exp $
 */

package net.jxta.protocol;


import net.jxta.document.Element;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.id.ID;
import net.jxta.platform.ModuleSpecID;


/**
 * A ModuleImplAdvertisement represents one of any number of published
 * implementations of a given specification.
 *
 * <p/>It is meant to be published via JXTA Discovery.
 *
 * <p/>Module specifications are referenced by their ModuleSpecID. Given a
 * ModuleSpecID, a ModuleImplAdvertisement may be searched by means of JXTA
 * Discovery, filtered according to the compatibility statement it contains,
 * and if compatible, loaded and initialized.
 * The loadModule method of a PeerGroup implementation performs this
 * task automatically, given a ModuleSpecID.
 *
 * <p/>One significant example of Modules referenced and loaded in that manner
 * are the services and protocols that constitute a StdPeerGroup in the Java
 * reference implementation.
 *
 * @see net.jxta.id.ID
 * @see net.jxta.platform.ModuleSpecID
 * @see net.jxta.document.Advertisement
 * @see net.jxta.document.StructuredDocument
 * @see net.jxta.document.Element
 * @see net.jxta.protocol.ModuleSpecAdvertisement
 * @see net.jxta.peergroup.PeerGroup
 **/
public abstract class ModuleImplAdvertisement extends ExtendableAdvertisement implements Cloneable {
    
    private ModuleSpecID sid = null;
    private Element description = null;
    
    // The group's implementation interprets it.
    private StructuredDocument compat = null;
    private String code = null;
    private String uri = null;
    private String provider = null;
    
    // The module interprets it.
    private StructuredDocument param = null;
    
    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public static String getAdvertisementType() {
        return "jxta:MIA";
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * Clone this ModuleImplAdvertisement
     *
     **/
    public Object clone() {
        
        try {
            ModuleImplAdvertisement clone = (ModuleImplAdvertisement) super.clone();
            
            clone.setModuleSpecID(getModuleSpecID());
            clone.setDesc(getDesc());
            clone.setCompat(getCompat());
            clone.setCode(getCode());
            clone.setUri(getUri());
            clone.setProvider(getProvider());
            clone.setParam(getParam());
            
            return clone;
        } catch (CloneNotSupportedException impossible) {
            return null;
        }
    }
    
    /**
     * Returns the unique ID of that advertisement for indexing purposes.
     * In that case we do not have any particular one to offer. Let the indexer
     * hash the document.
     *
     * @return ID the unique id
     **/
    public ID getID() {
        return null;
    }
    
    /**
     * Returns the id of the spec that this implements.
     * @return ID the spec id
     *
     */
    
    public ModuleSpecID getModuleSpecID() {
        return sid;
    }
    
    /**
     * Sets the id of the spec that is implemented
     *
     * @param sid The id of the spec
     **/
    public void setModuleSpecID(ModuleSpecID sid) {
        this.sid = sid;
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
     * @param description the description
     */
    public void setDescription(String description) {
        
        if (null != description) {
            StructuredDocument newdoc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Desc", description);
            
            setDesc(newdoc);
        } else {
            this.description = null;
        }
    }
    
    /**
     * returns the description
     *
     * @return the description
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
     */
    public void setDesc(Element desc) {
        
        if (null != desc) {
            this.description = StructuredDocumentUtils.copyAsDocument(desc);
        } else {
            this.description = null;
        }
    }
 
    /**
     * returns the module impl. compatibility statement.
     *
     * @return StructuredDocument the statement as a StructuredDocument
     * of unspecified content.
     **/
    public StructuredDocument getCompat() {
        return (compat == null ? null : StructuredDocumentUtils.copyAsDocument(compat));
    }
    
    /**
     * Privileged version of {@link #getCompat()} that does not clone the elements.
     *
     * @return StructuredDocument the statement as a StructuredDocument
     * of unspecified content.
     **/
    protected StructuredDocument getCompatPriv() {
        return compat;
    }
    
    /**
     * sets the module impl. compatibility statement.
     *
     * @param compat Element of an unspecified content.
     **/
    public void setCompat(Element compat) {
        this.compat = (compat == null ? null : StructuredDocumentUtils.copyAsDocument(compat));
    }
    
    /**
     * returns the code; a reference to or representation of the executable code
     * advertised by this advertisement.
     * What the code really is depends on the compatibility statement. Any compatible
     * user of this impl. adv. knows what it means. The standard group implementations
     * of the java reference implementation expect it to be a fully qualified java class
     * name.
     *
     * @return String the code
     **/
    public String getCode() {
        return code;
    }
    
    /**
     * sets the code
     *
     * @param code reference to the code
     **/
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * returns the uri; that is a reference to or representation of a package from which
     * the executable code referenced by the getCode method may be loaded.
     * What the uri really is depends on the compatibility statement. Any compatible
     * user of this impl. adv. knows what it means. The standard group implementations
     * of the java reference implementation expect it to be a reference to a jar file.
     *
     * @return String uri
     **/
    public String getUri() {
        return uri;
    }
    
    /**
     * sets the uri
     *
     * @param uri string uri
     **/
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    /**
     * returns the provider
     *
     * @return String the provider
     **/
    public String getProvider() {
        return provider;
    }
    
    /**
     * sets the provider
     *
     * @param provider the provider
     **/
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    /**
     * returns the param element.
     *
     * The interpretation of the param element is entirely up to the code
     * that this advertises. One valid use of it is to enable the code to
     * be configured so that multiple specs or multiple implementations of
     * one spec may use the same code.
     *
     * @return StructuredDocument A standalone structured document of
     * unspecified content.
     **/
    public StructuredDocument getParam() {
        return (param == null ? null : StructuredDocumentUtils.copyAsDocument(param));
    }
    
    /**
     * Privileged version of {@link #getParam()} that does not clone the elements.
     *
     * @return StructuredDocument A standalone structured document of
     * unspecified content.
     **/
    protected StructuredDocument getParamPriv() {
        return param;
    }
    
    /**
     * sets the module param
     * @param param Element of an unspecified content.
     **/
    public void setParam(Element param) {
        this.param = (param == null ? null : StructuredDocumentUtils.copyAsDocument(param));
    }
}

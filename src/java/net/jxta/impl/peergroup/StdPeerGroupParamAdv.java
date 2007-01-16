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
 * $Id: StdPeerGroupParamAdv.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.Element;
import net.jxta.document.TextElement;
import net.jxta.document.MimeMediaType;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;

import java.util.Hashtable;
import java.util.Enumeration;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class StdPeerGroupParamAdv {

    private static final Logger LOG =
        Logger.getLogger(StdPeerGroupParamAdv.class.getName());

    private static final String paramTag = "Parm";
    private static final String protoTag = "Proto";
    private static final String appTag =   "App";
    private static final String svcTag =   "Svc";
    private static final String mcidTag =  "MCID";
    private static final String msidTag =  "MSID";

    private static final String miaTag = ModuleImplAdvertisement.getAdvertisementType();

    // In the future we should be able to manipulate all modules regardless
    // of their kind, but right now it helps to keep them categorized
    // as follows.
    private Hashtable servicesTable = null;
    private Hashtable protosTable = null;
    private Hashtable appsTable = null;

    public StdPeerGroupParamAdv( ) {
        // set defaults
        servicesTable = new Hashtable();
        protosTable = new Hashtable();
        appsTable = new Hashtable();
    }

    public StdPeerGroupParamAdv( Element root ) {

            initialize( root );
    }

    public Hashtable getServices() {
        return servicesTable;
    }

    public Hashtable getProtos() {
        return protosTable;
    }

    public Hashtable getApps() {
        return appsTable;
    }

    public void setServices(Hashtable servicesTable) {
        if (servicesTable == null) this.servicesTable = new Hashtable();
        else this.servicesTable = servicesTable;
    }
    public void setProtos(Hashtable protosTable) {
        if (protosTable == null) this.protosTable = new Hashtable();
        else this.protosTable = protosTable;
    }
    public void setApps(Hashtable appsTable) {
        if (appsTable == null) this.appsTable = new Hashtable();
        else this.appsTable = appsTable;
    }

    private void initialize(Element root) {
        
        if( !TextElement.class.isInstance( root ) )
            throw new IllegalArgumentException( getClass().getName() + " only supports TextElement" );
        
        TextElement doc = (TextElement) root;
        
        if (! doc.getName().equals( paramTag ) )
            
            throw new IllegalArgumentException( "Could not construct : "
                                                + getClass().getName()
                                                + "from doc containing a "
                                                + doc.getName() );


        // set defaults
        servicesTable = new Hashtable();
        protosTable = new Hashtable();
        appsTable = new Hashtable();

        int appCount = 0;
        Enumeration modules = doc.getChildren();
        while (modules.hasMoreElements()) {
            Hashtable theTable;

            TextElement module = (TextElement) modules.nextElement();
            String tagName = module.getName();
            if (tagName.equals(svcTag)) {
                theTable = servicesTable;
            } else if (tagName.equals(appTag)) {
                theTable = appsTable;
            } else if (tagName.equals(protoTag)) {
                theTable = protosTable;
            } else continue;

            ModuleSpecID specID = null;
            ModuleClassID classID = null;
            ModuleImplAdvertisement inLineAdv = null;

            try {
                if (module.getTextValue() != null) {
                    specID = (ModuleSpecID)
                        IDFactory.fromURL(IDFactory.jxtaURL(module.getTextValue()));
                }

                // Check for children anyway.
                Enumeration fields = module.getChildren();
                while (fields.hasMoreElements()) {
                    TextElement field = (TextElement) fields.nextElement();
                    if (field.getName().equals(mcidTag)) {
                        classID = (ModuleClassID)
                            IDFactory.fromURL(IDFactory.jxtaURL(field.getTextValue()));
                        continue;
                    }
                    if (field.getName().equals(msidTag)) {
                        specID = (ModuleSpecID)
                            IDFactory.fromURL(IDFactory.jxtaURL(field.getTextValue()));
                        continue;
                    }
                    if (field.getName().equals(miaTag)) {
                        inLineAdv = (ModuleImplAdvertisement)
                            AdvertisementFactory.newAdvertisement(field);
                        continue;
                    }
                }
            } catch (Exception any) {
                if (LOG.isEnabledFor(Level.WARN)) LOG.warn("Broken entry; skipping", any);
                continue;        
            }

            if (inLineAdv == null && specID == null) {
                if (LOG.isEnabledFor(Level.WARN)) LOG.warn("Insufficent entry; skipping");
                continue;        
            }

            Object theValue;
            if (inLineAdv == null) {
                theValue = specID;
            } else {
                specID = inLineAdv.getModuleSpecID();
                theValue = inLineAdv;
            }
            if (classID == null) {
                classID = specID.getBaseClass();
            }

            // For applications, the role does not matter. We just create
            // a unique role ID on the fly.
            // When outputing the add we get rid of it to save space.

            if (theTable == appsTable) {
                // Only the first (or only) one may use the base class.
                if (appCount++ != 0) {
                    classID = IDFactory.newModuleClassID(classID);
                }
            }
            theTable.put(classID, theValue);
        }
    }

    public Document getDocument( MimeMediaType encodeAs ) {
        StructuredTextDocument doc = null;
        
            doc = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument( encodeAs,
                                                                 paramTag );

        outputModules(doc, servicesTable, svcTag, encodeAs);
        outputModules(doc, protosTable, protoTag, encodeAs);
        outputModules(doc, appsTable, appTag, encodeAs);
        return doc;
    }

    private void outputModules(StructuredTextDocument doc,
                               Hashtable modulesTable,
                               String mainTag,
                               MimeMediaType encodeAs) {

        Enumeration allClasses = modulesTable.keys();
        while (allClasses.hasMoreElements()) {
           ModuleClassID mcid = (ModuleClassID) allClasses.nextElement();
           Object val = modulesTable.get(mcid);

           // For applications, we ignore the role ID. It is not meaningfull,
           // and a new one is assigned on the fly when loading this adv.

           if (val instanceof Advertisement) {
               TextElement m = doc.createElement(mainTag);
               doc.appendChild(m);
               
               if (!(modulesTable == appsTable
                     ||
                     mcid.equals(mcid.getBaseClass()))) {
                   // It is not an app and there is a role ID. Output it.

                   TextElement i = doc.createElement(mcidTag, mcid.toString());
                   m.appendChild(i);
               }

               StructuredTextDocument advdoc = (StructuredTextDocument)
                   ((Advertisement) val).getDocument(encodeAs);

               StructuredDocumentUtils.copyElements(doc, m, advdoc);

           } else if (val instanceof ModuleSpecID) {
               TextElement m;

               if (   modulesTable == appsTable
                   || mcid.equals(mcid.getBaseClass())) {

                   // Either it is an app or there is no role ID.
                   // So the specId is good enough.
                   m = doc.createElement(mainTag,
                                         ((ModuleSpecID) val).toString());
                   doc.appendChild(m);
               } else {
                   // The role ID matters, so the classId must be separate.
                   m = doc.createElement(mainTag);
                   doc.appendChild(m);

                   TextElement i;
                   i = doc.createElement(mcidTag, mcid.toString());
                   m.appendChild(i);

                   i = doc.createElement(msidTag,
                                         ((ModuleSpecID) val).toString());
                   m.appendChild(i);
               }
           } else {
               if (LOG.isEnabledFor(Level.WARN)) LOG.warn("unsupported class in modules table");
           }
        }
    }
}


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
 * $Id: RendezvousServiceMonitorFilter.java,v 1.1 2007/01/16 11:01:55 thomas Exp $
 */

package net.jxta.impl.rendezvous.rendezvousMeter;


import net.jxta.meter.*;
import net.jxta.platform.*;
import net.jxta.document.*;
import net.jxta.util.documentSerializable.*;
import java.util.*;
import net.jxta.util.*;
import net.jxta.exception.*;
import java.net.*;
import net.jxta.id.IDFactory;

public class RendezvousServiceMonitorFilter implements ServiceMonitorFilter {
    private boolean includeClientConnectionMetrics = true;
    private boolean includeRendezvousConnectionMetrics = true;
    private ModuleClassID moduleClassID = MonitorResources.rendezvousServiceMonitorClassID;

    public ModuleClassID getModuleClassID() {
        return moduleClassID; 
    }

    public RendezvousServiceMonitorFilter() {}

    public void init(ModuleClassID moduleClassID) {
        this.moduleClassID = moduleClassID;
    }

    public boolean isIncludeClientConnectionMetrics() {
        return includeClientConnectionMetrics;
    }

    public boolean isIncludeRendezvousConnectionMetrics() {
        return includeRendezvousConnectionMetrics;
    }

    public void setIncludeClientConnectionMetrics(boolean includeClientConnectionMetrics) { 
        this.includeClientConnectionMetrics = includeClientConnectionMetrics;
    }
	
    public void setIncludeRendezvousConnectionMetrics(boolean includeClientConnectionMetrics) { 
        this.includeRendezvousConnectionMetrics = includeClientConnectionMetrics; 
    }
	
    public void serializeTo(Element element) throws DocumentSerializationException {
        DocumentSerializableUtilities.addBoolean(element, "includeClientConnectionMetrics", includeClientConnectionMetrics);
        DocumentSerializableUtilities.addBoolean(element, "includeRendezvousConnectionMetrics", includeRendezvousConnectionMetrics);
        if (moduleClassID != null) {
            DocumentSerializableUtilities.addString(element, "moduleClassID", moduleClassID.toString());		
        }
    }

    public void initializeFrom(Element element) throws DocumentSerializationException {
        for (Enumeration e = element.getChildren(); e.hasMoreElements();) {
            Element childElement = (TextElement) e.nextElement();
            String tagName = (String) childElement.getKey();
			
            if (tagName.equals("includeClientConnectionMetrics")) { 
                includeClientConnectionMetrics = DocumentSerializableUtilities.getBoolean(childElement);
            } else if (tagName.equals("includeRendezvousConnectionMetrics")) { 
                includeRendezvousConnectionMetrics = DocumentSerializableUtilities.getBoolean(childElement);
            } else if (tagName.equals("moduleClassID")) {
                try {
                    moduleClassID = (ModuleClassID) IDFactory.fromURI( new URI((DocumentSerializableUtilities.getString(childElement))));
                } catch (URISyntaxException jex) {
                    throw new DocumentSerializationException("Can't read moduleClassID", jex);
                }
            }
        }
    }
	
}

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
 * $Id: TransportMetric.java,v 1.1 2007/01/16 11:02:04 thomas Exp $
 */

package net.jxta.impl.endpoint.transportMeter;

import net.jxta.endpoint.*;
import net.jxta.peer.*;

import net.jxta.util.documentSerializable.*;
import net.jxta.document.*;

import java.util.*;

/**
  * The Metric for a single Transport
  **/
public class TransportMetric implements DocumentSerializable {
	private String protocol;
	private EndpointAddress endpointAddress;
	private HashMap transportBindingMetrics = new HashMap();

	public TransportMetric(TransportMeter transportMeter) {
		this.endpointAddress = transportMeter.getEndpointAddress();
		this.protocol = transportMeter.getProtocol();
	}

	public TransportMetric() { }

	public TransportMetric(TransportMetric prototype) {
		this.endpointAddress = prototype.endpointAddress;
		this.protocol = prototype.protocol;
	}

	public boolean equals(Object obj) {
		if (obj instanceof TransportMetric) {
			TransportMetric other = (TransportMetric) obj;
			return protocol.equals(other.protocol) && endpointAddress.equals(other.endpointAddress);
		} else
			return false;
	}

	public int hashCode() { 
		return endpointAddress.hashCode();
	}

	public EndpointAddress getEndpointAddress() { return endpointAddress; }
	public String getProtocol() { return protocol; }
	
	public synchronized void addTransportBindingMetric(TransportBindingMetric transportBindingMetric) {
		transportBindingMetrics.put(transportBindingMetric.getEndpointAddress(), transportBindingMetric);
	}

	public TransportBindingMetric getTransportBindingMetric(EndpointAddress endpointAddress) { return (TransportBindingMetric) transportBindingMetrics.get(endpointAddress); }

	public TransportBindingMetric getTransportBindingMetric(TransportBindingMetric prototype) { 
		return getTransportBindingMetric(prototype.getEndpointAddress());
	}

	public Iterator getTransportBindingMetrics() { return transportBindingMetrics.values().iterator(); }

	public int getTransportBindingMetricsCount() { return transportBindingMetrics.size(); }
	
	public void serializeTo(Element element) throws DocumentSerializationException {
		DocumentSerializableUtilities.addString(element, "endpointAddress", endpointAddress.toString());
		DocumentSerializableUtilities.addString(element, "protocol", protocol);

		for (Iterator i = getTransportBindingMetrics(); i.hasNext(); ) {
			TransportBindingMetric transportBindingMetric = (TransportBindingMetric)i.next();
			DocumentSerializableUtilities.addDocumentSerializable(element, "binding", transportBindingMetric);		
		}
	}

	public void initializeFrom(Element element) throws DocumentSerializationException {
		for (Enumeration e=element.getChildren(); e.hasMoreElements(); ) {
			Element childElement = (TextElement) e.nextElement();
			String tagName = (String) childElement.getKey();
			
			if (tagName.equals("endpointAddress")) {
				String endpointAddressString = DocumentSerializableUtilities.getString(childElement);	
				endpointAddress = new EndpointAddress(endpointAddressString);
			} else if (tagName.equals("protocol")) {
				protocol = DocumentSerializableUtilities.getString(childElement);
			} else if (tagName.equals("binding")) {
				TransportBindingMetric transportBindingMetric = (TransportBindingMetric) DocumentSerializableUtilities.getDocumentSerializable(childElement, TransportBindingMetric.class);
				transportBindingMetrics.put(transportBindingMetric.getEndpointAddress(), transportBindingMetric);
			}
		}
	}


	void mergeMetrics(TransportMetric otherTransportMetric) {
		for (Iterator i = otherTransportMetric.getTransportBindingMetrics(); i.hasNext(); ) {
			TransportBindingMetric otherTransportBindingMetric = (TransportBindingMetric)i.next();
			TransportBindingMetric transportBindingMetric = getTransportBindingMetric(otherTransportBindingMetric.getEndpointAddress());

			if (transportBindingMetric == null) {
				transportBindingMetric = new TransportBindingMetric(otherTransportBindingMetric);
				addTransportBindingMetric(transportBindingMetric);
			}
			 
			transportBindingMetric.mergeMetrics(otherTransportBindingMetric);			
		}
				
	}
}

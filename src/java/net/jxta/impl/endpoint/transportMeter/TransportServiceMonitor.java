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
 * $Id: TransportServiceMonitor.java,v 1.1 2007/01/16 11:02:04 thomas Exp $
 */

package net.jxta.impl.endpoint.transportMeter;

import net.jxta.peergroup.*;
import net.jxta.endpoint.*;
import net.jxta.meter.*;
import net.jxta.impl.meter.*;
import java.util.*;

/**
  * The Service Monitor Metric for the Transport Services
  * <p> Each Transport will register with this to create their own TransportMeter
  * 
**/
public class TransportServiceMonitor  extends GenericServiceMonitor {
	private LinkedList transportMeters = new LinkedList();
	private TransportServiceMetric cumulativeTransportServiceMetric;

	public TransportServiceMonitor() { }

	/**
	  * {@inheritDoc}
	  **/
	protected void init() {
		cumulativeTransportServiceMetric = (TransportServiceMetric) getCumulativeServiceMetric();
	}


	/** 
	  * Create a service TransportMeter for a registerd Transport Type
	  * @deprecated
	  * @see createTransportMeter(String, EndpointAddress)
	  **/
	public synchronized TransportMeter createTransportMeter(String protocol, String sourceAddressString) {
		EndpointAddress endpointAddress = new EndpointAddress(sourceAddressString);
		return createTransportMeter(protocol, endpointAddress);
	}	

	/** 
	  * Create a service TransportMeter for a registerd Transport Type
	  * @param protocol Descriptive name of protocol
	  * @param endpointAddress The common public address for this transport
	  * @return Transport Meter for this transport
	  **/
	public synchronized TransportMeter createTransportMeter(String protocol, EndpointAddress endpointAddress) {
		endpointAddress = new EndpointAddress(endpointAddress, null, null);

		for (Iterator i = transportMeters.iterator(); i.hasNext(); ) {
			TransportMeter transportMeter = (TransportMeter)i.next();
			if (transportMeter.getProtocol().equals(protocol) && transportMeter.getEndpointAddress().equals(endpointAddress))
				return transportMeter;			
		}

		TransportMeter transportMeter = new TransportMeter(protocol, endpointAddress);
		transportMeters.add(transportMeter);
		cumulativeTransportServiceMetric.addTransportMetric(transportMeter.getCumulativeMetrics());
		return transportMeter;
	}


	/**
	  * {@inheritDoc}
	  **/
	protected ServiceMetric collectServiceMetrics() {
		TransportServiceMetric transportServiceMetric = (TransportServiceMetric) createServiceMetric();

		boolean anyData = false;

		for (Iterator i = transportMeters.iterator(); i.hasNext(); ) {
			TransportMeter transportMeter = (TransportMeter)i.next();

			TransportMetric transportMetric = transportMeter.collectMetrics();

			if (transportMetric != null) {
				transportServiceMetric.addTransportMetric(transportMetric);
				anyData= true;
			}
		}

		if (anyData)
			return transportServiceMetric;
		else
			return null;
	}	

	/**
	  * {@inheritDoc}
	  **/
	public ServiceMetric getServiceMetric(ServiceMonitorFilter serviceMonitorFilter, long fromTime, long toTime, int pulseIndex, long reportRate){
		int deltaReportRateIndex = monitorManager.getReportRateIndex(reportRate);
		TransportServiceMetric origServiceMetric = (TransportServiceMetric)deltaServiceMetrics[deltaReportRateIndex];

		if(origServiceMetric == null) {
			return null;
		} 
		
		TransportServiceMonitorFilter transportServiceMonitorFilter = (TransportServiceMonitorFilter)serviceMonitorFilter;
		return origServiceMetric.shallowCopy(transportServiceMonitorFilter);
	}

	/**
	  * {@inheritDoc}
	  **/
	public ServiceMetric getCumulativeServiceMetric(ServiceMonitorFilter serviceMonitorFilter, long fromTime, long toTime){
		TransportServiceMetric origServiceMetric = (TransportServiceMetric)cumulativeServiceMetric;

		TransportServiceMonitorFilter transportServiceMonitorFilter = (TransportServiceMonitorFilter)serviceMonitorFilter;
		return origServiceMetric.deepCopy(transportServiceMonitorFilter);
	}

}

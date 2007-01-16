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
 * 4. The names "Sun", "Sun Micro//Systems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http:/www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICRO//SystemS OR
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
 * <http:/www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: EndpointMeter.java,v 1.1 2007/01/16 11:02:02 thomas Exp $
 */

 package net.jxta.impl.endpoint.endpointMeter;

import net.jxta.peer.*;
import net.jxta.peergroup.*;
import net.jxta.endpoint.*;
import net.jxta.impl.endpoint.*;

import java.net.*;
import java.util.*;

/**
  *    Aggregate Meter for Endpoint Monitoring
  **/
public class EndpointMeter {
	
	private EndpointMetric total;
	private EndpointMetric delta;

	public EndpointMeter() {
		total = new EndpointMetric();
	}

	public EndpointMetric getCumulativeMetrics() {
		total.setEndpointUpTime(System.currentTimeMillis() - total.getEndpointStartTime());
		return total; 
	}
	
	public synchronized EndpointMetric collectMetrics() {
		if (delta != null)
			delta.setEndpointUpTime(System.currentTimeMillis() - total.getEndpointStartTime());

		EndpointMetric oldDelta = delta;
		
		delta = null;
		
		return oldDelta;
	}

	public void invalidIncomingMessage(){
		if (delta == null)	
			createDeltaMetric();

		delta.invalidIncomingMessage();
		total.invalidIncomingMessage();
	}

	public void noListenerForIncomingMessage(){
		if (delta == null)	
			createDeltaMetric();

		delta.noListenerForIncomingMessage();
		total.noListenerForIncomingMessage();
	}

	public void errorProcessingIncomingMessage(){
		if (delta == null)	
			createDeltaMetric();

		delta.errorProcessingIncomingMessage();
		total.errorProcessingIncomingMessage();
	}

	public void noDestinationAddressForDemuxMessage(){
		if (delta == null)	
			createDeltaMetric();

		delta.noDestinationAddressForDemuxMessage();
		total.noDestinationAddressForDemuxMessage();
	}

	public void noSourceAddressForDemuxMessage(){
		if (delta == null)	
			createDeltaMetric();

		delta.noSourceAddressForDemuxMessage();
		total.noSourceAddressForDemuxMessage();
	}

	public void discardedLoopbackDemuxMessage(){
		if (delta == null)	
			createDeltaMetric();
	
		delta.discardedLoopbackDemuxMessage();
		total.discardedLoopbackDemuxMessage();
	}

	public void incomingMessageFilteredOut(){
		if (delta == null)	
			createDeltaMetric();
	
		delta.incomingMessageFilteredOut();
		total.incomingMessageFilteredOut();
	}

	public void incomingMessageSentToEndpointListener(){
		if (delta == null)	
			createDeltaMetric();
	
		delta.incomingMessageSentToEndpointListener();
		total.incomingMessageSentToEndpointListener();
	}

	public void demuxMessageProcessed(){
		if (delta == null)	
			createDeltaMetric();
	
		delta.demuxMessageProcessed();
		total.demuxMessageProcessed();
	}
		
	private void createDeltaMetric() {
		delta = new EndpointMetric(total);
	}

	public String toString() {
		return "EndpointMeter()";
	}

}

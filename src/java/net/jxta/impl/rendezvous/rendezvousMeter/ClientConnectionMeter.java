/*
 * Copyright (c) 2001 Sun Micro//Systems, Inc.  All rights
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
 *       Sun Micro//Systems, Inc. for Project JXTA."
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
 * $Id: ClientConnectionMeter.java,v 1.1 2007/01/16 11:01:55 thomas Exp $
 */

package net.jxta.impl.rendezvous.rendezvousMeter;


import net.jxta.peer.*;
import net.jxta.endpoint.*;


/**
 * The rendezvous's meter for a client peer interacting with a it
 **/
public class ClientConnectionMeter {
    private PeerID peerID;

    private ClientConnectionMetric cumulativeMetrics;
    private ClientConnectionMetric deltaMetrics;

    private long transitionTime = 0;
    private long lastLeaseRenewalTime = 0;

    public ClientConnectionMeter(PeerID peerID) {
        this.peerID = peerID;
        cumulativeMetrics = new ClientConnectionMetric(peerID);
    }

    public ClientConnectionMetric getCumulativeMetrics() {
        return cumulativeMetrics;
    }
	
    public synchronized ClientConnectionMetric collectMetrics() {
        ClientConnectionMetric prevDelta = deltaMetrics;

        deltaMetrics = null;
        return prevDelta;
    }
	
    private void createDeltaMetric() {
        deltaMetrics = new ClientConnectionMetric(cumulativeMetrics);
    }

    public void clientConnectionEstablished(long lease) {
        transitionTime = System.currentTimeMillis();
		
        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.clientConnectionEstablished(transitionTime, lease);
        cumulativeMetrics.clientConnectionEstablished(transitionTime, lease);
    }

    public void clientLeaseRenewed(long lease) {
        lastLeaseRenewalTime = System.currentTimeMillis();

        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.clientLeaseRenewed(lastLeaseRenewalTime, lease);
        cumulativeMetrics.clientLeaseRenewed(lastLeaseRenewalTime, lease);
    }

    public void errorAddingClient() {
        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.errorAddingClient();
        cumulativeMetrics.errorAddingClient();
    }	

    public void clientConnectionDisconnected(boolean normal) {
        transitionTime = System.currentTimeMillis();
	
        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.clientConnectionDisconnected(normal, transitionTime);
        cumulativeMetrics.clientConnectionDisconnected(normal, transitionTime);
    }

    public void unableToRespondToConnectRequest() {
        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.unableToRespondToConnectRequest();
        cumulativeMetrics.unableToRespondToConnectRequest();
    }

    public void clientConnectionRefused() { 
        transitionTime = System.currentTimeMillis();

        if (deltaMetrics == null) {	
            createDeltaMetric();
        }

        deltaMetrics.clientConnectionRefused(transitionTime);
        cumulativeMetrics.clientConnectionRefused(transitionTime);
    }

    public String toString() {
        return "ClientConnectionMeter(" + peerID + ")";
    }
}

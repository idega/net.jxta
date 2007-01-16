/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *  must not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: ModuleId.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

import java.net.URI;

import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleClassID;

/**
 * JXTA module utility.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

class ModuleId {

    public final static ModuleId TCP = new ModuleId(PeerGroup.tcpProtoClassID);

    public final static ModuleId HTTP = new ModuleId(PeerGroup.httpProtoClassID);

    public final static ModuleId RENDEZVOUS =
        new ModuleId(PeerGroup.rendezvousClassID);

    public final static ModuleId RELAY =
        new ModuleId(PeerGroup.relayProtoClassID);

    public final static ModuleId ENDPOINT =
        new ModuleId(PeerGroup.endpointClassID);

    public final static ModuleId PEERGROUP =
        new ModuleId(PeerGroup.peerGroupClassID);

    public final static ModuleId MEMBERSHIP =
        new ModuleId(PeerGroup.membershipClassID);

    public final static ModuleId PROXY = new ModuleId(PeerGroup.proxyClassID);

    private ModuleClassID mcid = null;
 
    public static boolean isNormalService(ID key) {
        URI uri = key.toURI();
        
        return (TCP.getId().toURI().equals(uri) ||
            HTTP.getId().toURI().equals(uri) ||
            RENDEZVOUS.getId().toURI().equals(uri) ||
            RELAY.getId().toURI().equals(uri) ||
            ENDPOINT.getId().toURI().equals(uri) ||
            PEERGROUP.getId().toURI().equals(uri) ||
            MEMBERSHIP.getId().toURI().equals(uri) ||
            PROXY.getId().toURI().equals(uri));       	
    }
    
    public ModuleClassID getId() {
        return this.mcid;
    }

    private ModuleId(ModuleClassID mcid) {
        this.mcid = mcid;
    }
}
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
 *  $Id: Tag.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

/**
 *  Container for registered {@link net.jxta.impl.protocol.PlatformConfig} tags.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

class Tag {

    public final static Tag PARM = new Tag("Parm");
    public final static Tag ADDRESS = new Tag("Addr");
    public final static Tag IS_ENABLED = new Tag("isOff");
    public final static Tag RENDEZVOUS = new Tag("Rdv");
    public final static Tag RENDEZVOUS_AUTO_START = new Tag("AutoRdv");
    public final static Tag FLAGS = new Tag("Flags");
    public final static Tag NO_DISCOVERY = new Tag("UseOnlySeeds");
    public final static Tag TCP_ADDRESS = new Tag("tcpaddr");
    public final static Tag HTTP_ADDRESS = new Tag("httpaddr");
    public final static Tag IS_SERVER = new Tag("isServer");
    public final static Tag IS_CLIENT = new Tag("isClient");
    public final static Tag MAXIMUM_INCOMING =
        new Tag("ServerMaximumClients");
    public final static Tag INCOMING_LEASE =
        new Tag("ServerLeaseInSeconds");
    public final static Tag MAXIMUM_OUTGOING =
        new Tag("ClientMaximumServers");
    public final static Tag OUTGOING_LEASE =
        new Tag("ClientLeaseInSeconds");
    public final static Tag QUEUE_SIZE = new Tag("ClientQueueSize");
    public final static Tag ENDPOINT_QUEUE_SIZE =
        new Tag("MessengerQueueSize");
    public final static Tag ROOT_CERT = new Tag("RootCert");
    public final static Tag TYPE_ATTRIBUTE = new Tag("type");

    private String tag = null;

    public String toString() {
        return this.tag;
    }

    private Tag(String tag) {
        this.tag = tag;
    }
}

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
 *  $Id: Profile.java,v 1.1 2007/01/16 11:01:38 thomas Exp $
 */
package net.jxta.ext.config;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import java.io.InputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Provides a means to declaratively manage {@link net.jxta.ext.config.Configurator} processes by
 * quantifing varying configuration classes into common domains in the form of
 * "profiles."
 *
 * <p>A series of profile presets exist including:
 *
 * <ul>
 *  <li>{@link net.jxta.ext.config.Profile#EDGE} - primarily a service consumer</li>
 *  <li>{@link net.jxta.ext.config.Profile#SUPER} - primarily a service provisioner</li>
 *  <li>{@link net.jxta.ext.config.Profile#LOCAL} - primarily useful for development</li>
 *  <li>{@link net.jxta.ext.config.Profile#DEFAULT} - equivalent to {@link net.jxta.ext.config.Profile#EDGE}</li>
 * </ul>
 *
 * <p>Most of the included profiles include subtle variations. Further, one can
 * construct entirely new profiles that support specific application requirements.
 *
 * <p>All addresses are of the form {@link java.net.URI}. Addresses that do not
 * specify scheme information will be defaulted accordingly to the respective
 * context. Partial {@link java.net.URI} addresses will be templated with the
 * respective context such as the local IP address, etc.
 * 
 * <p>All fields have backing defaults enabling one to specify only the required
 * overrides in order to construct complete configuration profiles.
 * 
 * <p>Following is a (as of yet unvalidated) Profile DTD:
 * 
 * <pre>
 * &lt;?xml version="1.0" encoding="utf-8" standalone="no"?&gt;
 *
 * &lt;!--
 * &lt;!DOCTYPE xsd:schema SYSTEM "http://www.w3c.org/2001/XMLSchema.dtd"&gt;
 * --&gt;
 *
 * &lt;xsd:schema xmlns:jxta="http://www.jxta.org/net/jxta/ext/config"
 *   xmlns:xsd="http://www.w3c.org/2001/XMLSchema.dtd"&gt;
 *
 *   &lt;xsd:annotation&gt;
 *     &lt;xsd:documentation xml:lang="en"&gt;
 *     JXTA Configuration
 *
 *     see http://www.jxta.org for more info.
 *     &lt;/xsd:documentation&gt;
 *   &lt;/xsd:annotation&gt;
 *
 *   &lt;xsd:complexType name="jxta"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="peer" type="jxta:Peer" minOccurs="1" maxOccurs="1"/&gt;
 *       &lt;xsd:element name="network" type="jxta:Network" minOccurs="1"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="transport" type="jxta:Transport" minOccurs="1"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="service" type="jxta:Service" minOccurs="1"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Peer"&gt;
 *     &lt;xsd:attribute name="name" type="xsd:string" use="optional"/&gt;
 *     &lt;xsd:attribute name="id" type="jxta:PeerID" use="optional"/&gt;
 *     &lt;xsd:attribute name="descriptor" type="xsd:string" use="optional"/&gt;
 *     &lt;xsd:attribute name="home" type="xsd:anyURI" use="optional"
 *       default="file://${user.home}/.jxta"/&gt;
 *     &lt;xsd:attribute name="trace" type="jxta:Trace" use="optional"
 *       default="user default"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="description" type="jxta:Description" minOccurs="0"
 *         maxOccurs="unbounded"/&gt;
 *       &lt;xsd:element name="security" type="jxta:Security" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="rootCert" type="jxta:RootCert" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="proxy" type="jxta:ProxyAddress" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Network"&gt;
 *     &lt;xsd:attribute name="id" type="xsd:string" use="xxx"/&gt;
 *     &lt;xsd:attribute name="name" type="xsd:string" use="optional"/&gt;
 *     &lt;xsd:attribute name="description" type="xsd:string" use="optional"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="rendezVous" type="jxta:RendezVous" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="relays" type="jxta:Relays" minOccurs="0" maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Transport"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="tcp" type="Tcp" minOccurs="0" maxOccurs="unbounded"/&gt;
 *       &lt;xsd:element name="http" type="Http" minOccurs="0" maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Service"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="rendezVous" type="jxta:RendezVousService" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="relay" type="jxta:RelayService" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="endpoint" type="jxta:EndpointService" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="proxy" type="jxta:ProxyService" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;xsd:complextType name="Configuration"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="optimizer" type="jxta:Optimizer" minOccurs="0"
 *         maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complextType name="Configuration"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="optimizer" type="jxta:Optimizer" minOccurs="0"
 *         maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complextType name="Configuration"&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="optimizer" type="jxta:Optimizer" minOccurs="0"
 *         maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:simpleType name="Trace"&gt;
 *     &lt;xsd:restriction base="xsd:string"&gt;
 *       &lt;xsd:enumeration value="error"/&gt;
 *       &lt;xsd:enumeration value="warn"/&gt;
 *       &lt;xsd:enumeration value="info"/&gt;
 *       &lt;xsd:enumeration value="debug"/&gt;
 *       &lt;xsd:enumeration value="user default"/&gt;
 *     &lt;/xsd:restriction&gt;
 *   &lt;/xsd:simpleType&gt;
 *
 *   &lt;xsd:complextType name="Description"&gt;
 *     &lt;xsd:all&gt;
 *     &lt;/xsd:all&gt;
 *   &lt;/xsd:complextType&gt;
 *
 *   &lt;xsd:complexType name="Security"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *     &lt;xsd:attribute name="principal" type="xsd:string" use="required"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="RootCert"&gt;
 *     &lt;xsd:simpleContext&gt;
 *       &lt;xsd:extension base="xsd:string"&gt;
 *         &lt;xsd:attribute name="address" type="xsd:anyURI" use="optional"/&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContext&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="ProxyAddress"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:anyURI"&gt;
 *         &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *           default="false"/&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="RendezVous"&gt;
 *     &lt;xsd:attribute name="bootstrap" type="xsd:anyURI" use="optional"/&gt;
 *     &lt;xsd:attribute name="discovery" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *     &lt;xsd:element name="address" type="jxta:Address" minOccurs="0"
 *       maxOccurs="unbounded" default="://"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Relays"&gt;
 *     &lt;xsd:attribute name="bootstrap" type="xsd:anyURI" use="optional"/&gt;
 *     &lt;xsd:attribute name="discovery" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *     &lt;xsd:element name="address" type="jxta:Address" minOccurs="0"
 *       maxOccurs="unbounded" default="//:"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:element name="Address" type="xsd:anyURI"
 *   <!--
 *     &lt;xsd:attribute name="isDirect" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *   -->
 *   &lt;/&gt;
 *
 *   &lt;xsd:complexType name="Tcp"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="incoming" type="jxta:TransportEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="outgoing" type="jxta:TransportEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="TcpAddress" type="TcpAddress" minOccurs="0"
 *         maxOccurs="unbounded"/&gt;
 *       &lt;xsd:element name="publicAddress" type="jxta:PublicAddress" minOccurs="0"
 *         maxOccurs="unbounded" default="http://:"/&gt;
 *       &lt;xsd:element name="proxy" type="jxta:ProxyAddress" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Http"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="true"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="incoming" type="jxta:TransportEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="outgoing" type="jxta:TransportEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="address" type="jxta:TransportAddress" minOccurs="0"
 *         maxOccurs="unbounded" default="http://:"/&gt;
 *       &lt;xsd:element name="publicAddress" type="jxta:PublicAddress" minOccurs="0"
 *         maxOccurs="unbounded" default="http://:"/&gt;
 *       &lt;xsd:element name="proxy" type="jxta:ProxyAddress" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="TransportEndpoint"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="false"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="TcpAddress"&gt;
 *       &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension bae="xsd:anyURI"&gt;
 *         &lt;xsd:attribute name="range" type="xsd:integer"&gt;
 *           &lt;xsd:restrictive&gt;
 *             &lt;xsd:minInclusive value="0"/&gt;
 *             &lt;xsd:maxInclusive value="65535"/&gt;
 *           &lt;/xsd:restrictive&gt;
 *         &lt;/xsd:attribute&gt;
 *         &lt;xsd:sequence&gt;
 *           &lt;xsd:element name="multicast" type="jxta:MulticastAddress"
 *             minOccurs="0" maxOccurs="unbounded"/&gt;
 *         &lt;/xsd:sequence&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="TransportAddress"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:anyURI"&gt;
 *         &lt;xsd:attribute name="range" type="xsd:integer"&gt;
 *           &lt;xsd:restrictive&gt;
 *             &lt;xsd:minInclusive value="0"/&gt;
 *             &lt;xsd:maxInclusive value="65535"/&gt;
 *           &lt;/xsd:restrictive&gt;
 *         &lt;/xsd:attribute&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complextType name="MulticastAddress"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:anyURI"&gt;
 *         &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *           default="true"/&gt;
 *         &lt;xsd:attribute name="size" type="xsd:nonNegativeInteger" use="optional"
 *           default="16384"/&gt;
 *         &lt;/xsd:extension&gt;
 *       &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complextType&gt;
 *
 *   &lt;xsd:complexType name="PublicAddress"&gt;
 *     &lt;xsd:attribute name="exclusive" type="xsd:boolean" use="optional"
 *       default="false"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="RendezVousService"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="false"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="autoStart" type="jxta:AutoStart" minOccurs="0"
 *         maxOccurs="1" default="0"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="RelayService"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="false"/&gt;
 *     &lt;xsd:attribute name="queueSize" type="xsd:nonNegativeInterger"
 *       use="optional" default="100"/&gt;
 *     &lt;xsd:sequence&gt;
 *       &lt;xsd:element name="incoming" type="jxta:ServiceEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *       &lt;xsd:element name="outgoing" type="jxta:ServiceEndpoint" minOccurs="0"
 *         maxOccurs="1"/&gt;
 *     &lt;/xsd:sequence&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="EndpointService"&gt;
 *     &lt;xsd:attribute name="queueSize" type="xsd:positiveInteger" use="optional"
 *       default="20"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="ProxyService"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:anyURI"&gt;
 *        &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *          default="false"/&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:element name="AutoStart"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="nonNegativeInteger"&gt;
 *         &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *           default="false"/&gt;
 *
 *   &lt;xsd:complexType name="Optimzer"&gt;
 *     &lt;xsd:attribute name="class" type="xsd:string" use="required"/&gt;
 *     &lt;xsd:sequence&gt;
 *        &lt;xsd:element name="property" type="jxta:Property" minOccurs="0"
 *          maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:complexType&gt;
 *   &lt;/xsd:complexType&gt
 *
 *   &lt;xsd:complexType name="Property"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:string"&gt;
 *         &lt;xsd:attribute name="name" type="xsd:string" use="required"/&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt
 *
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:element&gt;
 *
 *   &lt;xsd:complexType name="ServiceEndpoint"&gt;
 *     &lt;xsd:attribute name="enabled" type="xsd:boolean" use="optional"
 *       default="false"/&gt;
 *     &lt;xsd:attribute name="maximum" type="xsd:positiveInteger" use="optional"
 *       default="1"/&gt;
 *     &lt;xsd:attribute name="lease" type="xsd:positiveInteger" use="optional"
 *       default="7200000"/&gt;
 *   &lt;/xsd:complexType&gt;
 *
 *   &lt;xsd:complexType name="Optimizer"&gt;
 *     &lt;xsd:attribute name="class" type="xsd:string" use="required"/&gt;
 *     &lt;xsd:sequence&gt;
 *        &lt;xsd:element name="property" type="jxta:Property" minOccurs="0"
 *          maxOccurs="unbounded"/&gt;
 *     &lt;/xsd:complexType&gt;
 *   &lt;/xsd:complexType&gt
 *
 *   &lt;xsd:complexType name="Property"&gt;
 *     &lt;xsd:simpleContent&gt;
 *       &lt;xsd:extension base="xsd:string"&gt;
 *         &lt;xsd:attribute name="name" type="xsd:string" use="required"/&gt;
 *       &lt;/xsd:extension&gt;
 *     &lt;/xsd:simpleContent&gt;
 *   &lt;/xsd:complexType&gt
 *
 * &lt;/xsd:schema&gt;
 * </pre>
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class Profile {

    /**
     * Prototypical "edge" configuration , i.e. no
     * {@link net.jxta.rendezvous.RendezVousService} or Relay service.
     */
    
    public final static Profile EDGE = new Profile("edge.xml");
    
    /**
     * Prototypical TCP edge configuration.
     */
    
    public final static Profile EDGE_TCP = new Profile("edgeTcp.xml");
    
    /**
     * Prototypical HTTP edge configuration.
     */
    
    public final static Profile EDGE_HTTP = new Profile("edgeHttp.xml");
    
    /**
     * Prototypical "super" configuration}, i.e. provisions
     * {@link net.jxta.rendezvous.RendezVousService} and Relay services.
     */
    
    public final static Profile SUPER = new Profile("super.xml");
    
    /**
     * Prototypical TCP super configuration.
     */
    
    public final static Profile SUPER_TCP = new Profile("superTcp.xml");
    
    /**
     * Prototypical HTTP super configuration.
     */
    
    public final static Profile SUPER_HTTP = new Profile("superHttp.xml");
    
    /**
     * Prototypical RendezVous configuration, i.e. provisions
     * {@link net.jxta.rendezvous.RendezVousService}.
     */
    
    public final static Profile RENDEZVOUS = new Profile("rendezVous.xml");
    
    /**
     * Prototypical TCP RendezVous configuration.
     */
    
    public final static Profile RENDEZVOUS_TCP = new Profile("rendezVousTcp.xml");
    
    /**
     * Prototypical HTTP RendezVous configuration.
     */
    
    public final static Profile RENDEZVOUS_HTTP = new Profile("rendezVousHttp.xml");
    
    /**
     *  Prototypical Relay configuration, i.e. provisions Relay services.
     */
    
    public final static Profile RELAY = new Profile("relay.xml");
    
    /**
     * Prototypical TCP Relay configuration.
     */
    
    public final static Profile RELAY_TCP = new Profile("relayTcp.xml");
    
    /**
     * Prototypical HTTP Relay configuration.
     */
    
    public final static Profile RELAY_HTTP = new Profile("relayHttp.xml");
    
    /**
     *  Local (loopback) configuration.
     */
    
    public final static Profile LOCAL = new Profile("local.xml");
    
    /**
     * Default configuration {@link net.jxta.ext.config.Profile#EDGE}.
     */
    
    public final static Profile DEFAULT = EDGE;

    /**
     * XPath accessors to Profile resources.
     *
     * @author  james todd [gonzo at jxta dot org]
     */
    
    public static class Key {

        /**
         * Root path: {@value}
         */
        
        public final static String JXTA = "/jxta";

        /**
         * Peer: {@value}
         */
        
        public final static String PEER = JXTA + "/peer";
        
        /**
         * Peer name: {@value}
         */
        
        public final static String PEER_NAME = PEER + "/@name";
        
        /**
         * Peer ID: {@value}
         */
        
        public final static String PEER_ID = PEER + "/@id";
        
        /**
         * Peer description: {@value}
         */
        
        public final static String PEER_DESCRIPTOR = PEER + "/@descriptor";
        
        /**
         * Peer home: {@value}
         */
        
        public final static String HOME_ADDRESS = PEER + "/@home";
        
        /**
         * Peer security: {@value}
         */
        
        public final static String SECURITY = PEER + "/security";
        
        /**
         * Peer security enabler: {@value}
         */
        
        public final static String SECURITY_IS_ENABLED = SECURITY + "/@enabled";
        
        /**
         * Peer principal: {@value}
         */
        
        public final static String PRINCIPAL = SECURITY + "/@principal";
        
        /**
         * Peer root certificate: {@value}
         */
        
        public final static String ROOT_CERTIFICATE = PEER + "/rootCert";
        
        /**
         * Peer root certificate address: {@value}
         */
        
        public final static String ROOT_CERTIFICATE_ADDRESS = ROOT_CERTIFICATE +
                "/@address";
        
        /**
         * Trace level: {@value}
         */
        
        public final static String TRACE = PEER + "/@trace";
        
        /**
         * Peer Description: {@value}
         */
        
        public final static String PEER_DESCRIPTION = PEER + "/description";
        
        /**
         * Peer proxy address: {@value}
         */
        
        public final static String PEER_PROXY_ADDRESS = PEER + "/proxy";
        
        /**
         * Network: {@value}
         */
        
        public final static String NETWORK = JXTA + "/network";
        
        /**
         * Network ID: {@value}
         */

        public final static String NETWORK_ID = NETWORK + "/@id";
        
        /**
         * Network name: {@value}
         */
        
        public final static String NETWORK_NAME = NETWORK + "/@name";
        
        /**
         * Network description: {@value}
         */
        
        public final static String NETWORK_DESCRIPTION = NETWORK + "/@description";
        
        /**
         * RendezVous: {@value}
         */
        
        public final static String RENDEZVOUS = NETWORK + "/rendezVous";
        
        /**
         * RendezVous boostrap: {@value}
         */
        
        public final static String RENDEZVOUS_BOOTSTRAP_ADDRESS = RENDEZVOUS +
                "/@bootstrap";
        
        /**
         * RendezVous discovery enabler: {@value}
         */
        
        public final static String RENDEZVOUS_DISCOVERY_IS_ENABLED = RENDEZVOUS +
                "/@discovery";
        
        /**
         * RendezVous address: {@value}
         */
        
        public final static String RENDEZVOUS_ADDRESS = RENDEZVOUS +
                "/address";
        
        /**
         * Relay: {@value}
         */
        
        public final static String RELAYS = NETWORK + "/relays";
        
        /**
         * Relay bootstrap enabler: {@value}
         */
        
        public final static String RELAYS_BOOTSTRAP_ADDRESS = RELAYS + "/@bootstrap";
        
        /**
         * Relay discovery enabler: {@value}
         */
        
        public final static String RELAYS_DISCOVERY_IS_ENABLED = RELAYS + "/@discovery";
        
        /**
         * Relay address: {@value}
         */
        
        public final static String RELAYS_ADDRESS = RELAYS + "/address";
        
        /**
         * Transport: {@value}
         */
        
        public final static String TRANSPORT = JXTA + "/transport";
        
        /**
         * Tcp transport: {@value}
         */
        
        public final static String TCP = TRANSPORT + "/tcp";
        
        /**
         * Tcp transport enabler: {@value}
         */
        
        public final static String TCP_IS_ENABLED = TCP + "/@enabled";
        
        /**
         * Tcp transport incoming: {@value}
         */
        
        public final static String TCP_INCOMING = TCP + "/incoming";
        
        /**
         * Tcp transport incoming enabler: {@value}
         */
        
        public final static String TCP_INCOMING_IS_ENABLED = TCP_INCOMING +
                "/@enabled";
        
        /**
         * Tcp transport outgoing: {@value}
         */
        
        public final static String TCP_OUTGOING = TCP + "/outgoing";
        
        /**
         * Tcp transport outgoing enabler: {@value}
         */
        
        public final static String TCP_OUTGOING_IS_ENABLED = TCP +
                "/@enabled";
        
        /**
         * Tcp transport address: {@value}
         */
        
        public final static String TCP_ADDRESS = TCP + "/address";
        
        /**
         * Tcp transport port range: {@value}
         */
        
        public final static String TCP_PORT_RANGE = TCP_ADDRESS +
                "/@range";
        
        /**
         * Tcp transport public address: {@value}
         */
        
        public final static String TCP_PUBLIC_ADDRESS = TCP +
                "/publicAddress";
        
        /**
         * Tcp transport public address exclusive enabler: {@value}
         */
        
        public final static String TCP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED =
                TCP_PUBLIC_ADDRESS + "/@exclusive";
        
        /**
         * Tcp transport proxy address: {@value}
         */
        
        public final static String TCP_PROXY_ADDRESS = TCP + "/proxy";
        
        /**
         * Tcp transport proxy enabler: {@value}
         */
        
        public final static String TCP_PROXY_IS_ENABLED = TCP_PROXY_ADDRESS +
                "/@enabled";
        
        /**
         * Multicast: {@value}
         */
        
        public final static String MULTICAST = TCP_ADDRESS + "/multicast";
        
        /**
         * Multicast enabler: {@value}
         */
        
        public final static String MULTICAST_IS_ENABLED = MULTICAST + "/@enabled";
        
        /**
         * Multicast size: {@value}
         */
        
        public final static String MULTICAST_SIZE = MULTICAST + "/@size";
        
        /**
         * HTTP transport: {@value}
         */
        
        public final static String HTTP = TRANSPORT + "/http";
        
        /**
         * HTTP transport enabler: {@value}
         */
        
        public final static String HTTP_IS_ENABLED = HTTP + "/@enabled";
        
        /**
         * HTTP transport incoming: {@value}
         */
        
        public final static String HTTP_INCOMING = HTTP + "/incoming";
        
        /**
         * HTTP transport incoming enabler: {@value}
         */
        
        public final static String HTTP_INCOMING_IS_ENABLED =
                HTTP_INCOMING + "/@enabled";
        
        /**
         * HTTP transport outgoing: {@value}
         */
        
        public final static String HTTP_OUTGOING = HTTP + "/outgoing";
        
        /**
         * HTTP transport outgoing enabler: {@value}
         */
        
        public final static String HTTP_OUTGOING_IS_ENABLED =
                HTTP_OUTGOING + "/@enabled";
        
        /**
         * HTTP transport address: {@value}
         */
        
        public final static String HTTP_ADDRESS = HTTP + "/address";
        
        /**
         * HTTP transport port range: {@value}
         */
        
        public final static String HTTP_PORT_RANGE = HTTP_ADDRESS +
                "/@range";
        
        /**
         * HTTP transport public address: {@value}
         */
        
        public final static String HTTP_PUBLIC_ADDRESS = HTTP +
                "/publicAddress";
        
        /**
         * HTTP transport public address exclusive enabler: {@value}
         */
        
        public final static String HTTP_PUBLIC_ADDRESS_EXCLUSIVE_IS_ENABLED =
                HTTP_PUBLIC_ADDRESS + "/@exclusive";
        
        /**
         * HTTP transport proxy: {@value}
         */
        
        public final static String HTTP_PROXY_ADDRESS = HTTP + "/proxy";
        
        /**
         * HTTP transport enabler: {@value}
         */
        
        public final static String HTTP_PROXY_IS_ENABLED = HTTP_PROXY_ADDRESS +
                "/@enabled";
        
        /**
         * Service: {@value}
         */
        
        public final static String SERVICE = JXTA + "/service";
        
        /**
         * RendezVous service: {@value}
         */
        
        public final static String RENDEZVOUS_SERVICE = SERVICE + "/rendezVous";
        
        /**
         * RendezVous service enabler: {@value}
         */
        
        public final static String RENDEZVOUS_SERVICE_IS_ENABLED =
                RENDEZVOUS_SERVICE + "/@enabled";
        
        /**
         * RendezVous service auto start: {@value}
         */
        
        public final static String RENDEZVOUS_SERVICE_AUTO_START =
                RENDEZVOUS_SERVICE + "/@autoStart";
        
        /**
         * RendezVous service auto start enabler: {@value}
         */
        
        public final static String RENDEZVOUS_SERVICE_AUTO_START_IS_ENABLED =
                RENDEZVOUS_SERVICE + "/@enabled";
        
        /**
         * Relay service: {@value}
         */
        
        public final static String RELAY_SERVICE = SERVICE + "/relay";
        
        /**
         * Relay service enabler: {@value}
         */
        
        public final static String RELAY_SERVICE_IS_ENABLED = RELAY_SERVICE +
                "/@enabled";
        
        /**
         * Relay service queue size: {@value}
         */
        
        public final static String RELAY_SERVICE_QUEUE_SIZE = RELAY_SERVICE +
                "/@queueSize";
        
        /**
         * Relay service incoming: {@value}
         */
        
        public final static String RELAY_SERVICE_INCOMING = RELAY_SERVICE +
                "/incoming";
        
        /**
         * Relay service incoming enabler: {@value}
         */
        
        public final static String RELAY_SERVICE_INCOMING_IS_ENABLED =
                RELAY_SERVICE_INCOMING + "/@enabled";
        
        /**
         * Relay service incoming maximum connections: {@value}
         */
        
        public final static String RELAY_SERVICE_INCOMING_MAXIMUM =
                RELAY_SERVICE_INCOMING + "/@maximum";
        
        /**
         * Relay service incoming lease: {@value}
         */
        
        public final static String RELAY_SERVICE_INCOMING_LEASE =
                RELAY_SERVICE_INCOMING + "/@lease";
        
        /**
         * Relay service outgoing: {@value}
         */
        
        public final static String RELAY_SERVICE_OUTGOING = RELAY_SERVICE +
                "/outgoing";
        
        /**
         * Relay service outgoing enabler: {@value}
         */
        
        public final static String RELAY_SERVICE_OUTGOING_IS_ENABLED =
                RELAY_SERVICE_OUTGOING + "/@enabled";
        
        /**
         * Relay service outgoing maximum connections: {@value}
         */
        
        public final static String RELAY_SERVICE_OUTGOING_MAXIMUM =
                RELAY_SERVICE_OUTGOING + "/@maximum";
        
        /**
         * Relay service outgoing lease: {@value}
         */
        
        public final static String RELAY_SERVICE_OUTGOING_LEASE =
                RELAY_SERVICE_OUTGOING + "/@lease";
        
        /**
         * Endpoint: {@value}
         */
        
        public final static String ENDPOINT_SERVICE = SERVICE + "/endpoint";
        
        /**
         * Endpoint outgoing queue size: {@value}
         */
        
        public final static String ENDPOINT_SERVICE_QUEUE_SIZE = ENDPOINT_SERVICE +
                "/@queueSize";
        
        /**
         * Proxy service: {@value}
         */
        
        public final static String PROXY_SERVICE = SERVICE + "/proxy";
        
        /**
         * Proxy enabler: {@value}
         */
        
        public final static String PROXY_SERVICE_IS_ENABLED = PROXY_SERVICE +
                "/@enabled";
        
        /**
         * Configuration: {@value}
         */
        
        public final static String CONFIGURATION = JXTA + "/configuration";
        
        /**
         * Optimizer: {@value}
         */
        
        public final static String CONFIGURATION_OPTIMIZER = CONFIGURATION +
            "/optimizer";
        
        /**
         * Optmizer Class: {@value}
         */
        
        public final static String CONFIGURATION_OPTIMIZER_CLASS =
            CONFIGURATION_OPTIMIZER + "/@class";
        
        /**
         * Optmizer Property Name: {@value}
         */
        
        public final static String OPTIMIZER_PROPERTY_NAME = CONFIGURATION_OPTIMIZER +
            "[@class=''{0}'']" + "/property/@name";
        
        /**
         * Optmizer Property Value: {@value}
         */
        
        public final static String OPTIMIZER_PROPERTY_VALUE= CONFIGURATION_OPTIMIZER +
            "[@class=''{0}'']" + "/property[@name=''{1}'']";
                
        /**
         * Constructor
         */
        
        private Key() {
        }
    }

    /**
     * Specifies the Profile basis.
     */
    
    protected final static Profile SEED = new Profile("seed.xml");

    private final static String RESOURCE_BASE = "resources/";
    private final static Logger LOG =
            Logger.getLogger(Profile.class.getName());

    private Resource profile = new Resource();

    private static HashMap profiles;

    // register various convenience profiles
    
    static {
        profiles = new HashMap();

        profiles.put("Default", DEFAULT);
        profiles.put("Edge", EDGE);
        profiles.put("Edge (Tcp)", EDGE_TCP);
        profiles.put("Edge (Http)", EDGE_HTTP);
        profiles.put("Super", SUPER);
        profiles.put("Super (Tcp)", SUPER_TCP);
        profiles.put("Super (Http)", SUPER_HTTP);
        profiles.put("Rendezvous", RENDEZVOUS);
        profiles.put("Rendezvous (Tcp)", RENDEZVOUS_TCP);
        profiles.put("Rendezvous (Http)", RENDEZVOUS_HTTP);
        profiles.put("Relay", RELAY);
        profiles.put("Relay (Tcp)", RELAY_TCP);
        profiles.put("Relay (Http)", RELAY_HTTP);
        profiles.put("Local", LOCAL);

    }

    /**
     * Accessor for a named Profile.
     *
     * @param   profile     profile name
     * @return              associated Profile or the {@link net.jxta.ext.config.Profile#DEFAULT} if
     *                      unresolvable 
     */
    
    public static Profile get(String profile) {
        profile = profile != null ? profile.trim() : profile;
        
        Profile p = (Profile)profiles.get(profile);

        return p != null ? p : DEFAULT;
    }

    /**
     * Accessor for registered Profiles.
     *
     * @return      Profile ids
     */
    
    public static Iterator getProfiles() {
        return profiles.keySet().iterator();
    }

    /**
     * Add a Profile keyed by name.
     *  
     * @param name      profile name
     * @param profile   Profile to be added
     */
    
    public static void add(String name, Profile profile) {
        if (null != name && null != profile) {
            profiles.put(name, profile);
        }               
    }

    /**
     * Constructs a Profile that represents the contents of the provided
     * {@link java.net.URI} location.
     *
     * @param   profile Profile location
     */
    
    public Profile(URL profile) {
        try {
            this.profile.load(profile);
        } catch (ResourceNotFoundException rnfe) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid resource location", rnfe);
            }
        }
    }
    
    /**
     * Constructs a Profile that represents the contents of the provided
     * {@link java.io.InputStream}.
     *
     * @param   is      Profile stream
     */
    
    public Profile(InputStream is) {
         try {
            this.profile.load(is);
        } catch (ResourceNotFoundException rnfe) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid resource format", rnfe);
            }
        }
    }

    /**
     * Accessor for the backing Profile resource.
     *
     * @return      Profile resource
     */
    
    protected Resource getProfile() {
        return this.profile;
    }

    /**
     * Constructor that represents the contents of the provided resource.
     *
     * @param   profile     Profile resource name
     */
    
    private Profile(String profile) {
        try {
            this.profile.load(RESOURCE_BASE + profile, Profile.class);
        } catch (ResourceNotFoundException rnfe) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("invalid resource location", rnfe);
            }
        }
    }
}

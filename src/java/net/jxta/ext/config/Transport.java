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
 *  $Id: Transport.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract Transport base class.
 *
 * <p>A Transport represents the physical {@link net.jxta.ext.config.Address} with which a JXTA Peer
 * communicates with the overall JXTA Network. A functional Peer must specify at
 * least one transport and may, opt, to include more then one. The JXTA Platform
 * continually optimizes the appropriate network paths amongst active peers
 * based on the provided transport information.
 *
 * <p>At least one Transport must be specified as incoming unless the peer is
 * specified as a Relay client in which case JXTA messages are conveyed via poll
 * model.
 *
 * <p>Transports can specify {@link net.jxta.ext.config.PublicAddress} and {@link net.jxta.ext.config.ProxyAddress} which
 * aid in NAT and FireWall traversal.
 *
 *@author     james todd [gonzo at jxta dot org]
 */

public abstract class Transport {
    
    /**
     * {@link net.jxta.ext.config.Transport} scheme representation.
     */
    
    public static class Scheme {
        
        /**
         * TCP scheme: {@value}
         */

        public final static String TCP = "tcp";

        /**
         * HTTP scheme: {@value}
         */
        
        public final static String HTTP = "http";

        private static List schemes = null;
        
        static {
            schemes = new ArrayList();

            schemes.add(TCP);
            schemes.add(HTTP);
        }
                
        /**
         * Accessor to the registered schemes.
         *
         * @return          {@link java.util.List} of schemes
         */
        
        public static List getSchemes() {
            return schemes;
        }

        /**
         * Validity check.
         *
         * @param       t       scheme
         * @return              validity response
         */
        
        public static boolean isValid(String t) {
            return getSchemes().contains(t);
        }

        private Scheme() {
        }
    }
    
    private String scheme = Protocol.DEFAULT;
    private boolean isEnabled = true;
    private boolean isIncoming = false;
    private boolean isOutgoing = false;
    private List addresses = null;
    private List publicAddresses = null;
    private boolean isProxyEnabled = false;
    private ProxyAddress proxy = null;

    /**
     * Constructor which specifies a scheme.
     *
     * @param       scheme      specified scheme value
     */
    
    public Transport(String scheme) {
        setScheme(scheme);
    }
    
    /**
     * Accessor to the scheme attribute.
     *
     * @return              the scheme attribute
     */

    public String getScheme() {
        return this.scheme;
    }

    /**
     * Specifies the scheme.
     *
     * @param       scheme      the specified scheme value
     */
    
    public void setScheme(String scheme) {
        if (! Scheme.isValid(scheme)) {
            throw new IllegalArgumentException("invalid scheme: " + scheme);
        }
        
        this.scheme = scheme;
    }

    /**
     * Accessor to the enabled attribute.
     *
     * @return              the enabled attribute value
     */
    
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Specifies the enabled attribute.
     *
     * @param       isEnabled       specified enabled attribute
     */
    
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Accessor to the incoming attribute.
     *
     * <p>Indicates this transport reponds to externally initiated connections.
     *
     * @return          incoming attribute value
     */
    
    public boolean isIncoming() {
        return this.isIncoming;
    }

    /**
     * Specifies the incoming attribute.
     *
     * @param       isIncoming      specified incoming value
     */
    
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }

    /**
     * Accessor to the outgoing attribute.
     *
     * <p>Indicates this transport initiates outgoing connections.
     *
     * @return              outgoing attribute value
     */
    
    public boolean isOutgoing() {
        return this.isOutgoing;
    }

    /**
     * Specifies the outgoing attribute.
     *
     * @param       isOutgoing      the specified outgoing attribute
     */
    
    public void setOutgoing(boolean isOutgoing) {
        this.isOutgoing = isOutgoing;
    }

    /**
     * Accessor to the {@link net.jxta.ext.config.Address}.
     *
     * @return          {@link java.util.List} of {@link net.jxta.ext.config.Address}
     */
    
    public List getAddresses() {
        return this.addresses != null ?
            this.addresses : Collections.EMPTY_LIST;
    }

    /**
     * Clears and specifies the provided {@link net.jxta.ext.config.Address}.
     *
     * @param       address     specified {@link net.jxta.ext.config.Address}
     */
    
    public void setAddress(Address address) {
        setAddresses(Collections.singletonList(address));
    }

    /**
     * Clears and specifies a {@link java.util.List} of {@link net.jxta.ext.config.Address}.
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.Address}
     */
    
    public void setAddresses(List addresses) {
        clearAddresses();
        addAddresses(addresses);
    }

    /**
     * Adds a {@link net.jxta.ext.config.Address}.
     *
     * @param       address     specified {@link net.jxta.ext.config.Address} value
     */
    
    public void addAddress(Address address) {
        addAddresses(Collections.singletonList(address));
    }

    /**
     * Adds a {@link java.util.List} of {@link net.jxta.ext.config.Address}.
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.Address}
     */
    
    public void addAddresses(List addresses) {
        if (addresses == null) {
            addresses = Collections.EMPTY_LIST;
        }

        if (this.addresses == null) {
            this.addresses = new ArrayList();
        }

        for (Iterator i = addresses.iterator(); i.hasNext(); ) {
            Address a = (Address)i.next();

            if (a != null &&
                ! this.addresses.contains(a)) {
                this.addresses.add(a);
            }
        }
    }

    /**
     * Removes the specified {@link net.jxta.ext.config.Address}
     *
     * @param       address     specified {@link net.jxta.ext.config.Address} value
     * @return                  removed {@link net.jxta.ext.config.Address}
     */
    
    public Address removeAddress(Address address) {
        Address a = null;

        if (this.addresses != null) {
            int i = this.addresses.indexOf(address);

            if (i > -1) {
                a = (Address)this.addresses.remove(i);

                if (this.addresses.size() == 0) {
                    this.addresses = null;
                }
            }
        }

        return a;
    }
    
    /**
     * Clears the {@link net.jxta.ext.config.Address}
     */

    public void clearAddresses() {
        if (this.addresses != null) {
            this.addresses.clear();

            this.addresses = null;
        }
    }

    /**
     * Accessor to the {@link net.jxta.ext.config.PublicAddress} as a {@link java.util.List}
     *
     * @return              {@link java.util.List} of {@link net.jxta.ext.config.PublicAddress}
     */
    
    public List getPublicAddresses() {
        return this.publicAddresses != null ?
               this.publicAddresses : Collections.EMPTY_LIST;
    }

    /**
     * Add a {@link net.jxta.ext.config.PublicAddress}.
     *
     * @param       address     specified {@link net.jxta.ext.config.PublicAddress} value
     */
    
    public void addPublicAddress(PublicAddress address) {
        addPublicAddresses(Collections.singletonList(address));
    }

    /**
     * Adds a {@link java.util.List} of {@link net.jxta.ext.config.PublicAddress}.
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.PublicAddress}
     */
    
    public void addPublicAddresses(List addresses) {
        if (addresses == null) {
            addresses = Collections.EMPTY_LIST;
        }

        if (this.publicAddresses == null) {
            this.publicAddresses = new ArrayList();
        }

        for (Iterator i = addresses.iterator(); i.hasNext(); ) {
            PublicAddress pa = (PublicAddress)i.next();

            if (pa != null &&
                ! this.publicAddresses.contains(pa)) {
                this.publicAddresses.add(pa);
            }
        }
    }

    /**
     * Clears and specifies a {@link net.jxta.ext.config.PublicAddress}
     *
     * @param       address     the specified {@link net.jxta.ext.config.PublicAddress}
     */
    
    public void setPublicAddress(PublicAddress address) {
        setPublicAddresses(Collections.singletonList(address));
    }

    /**
     * Clears and specifies a {@link java.util.List} of {@link net.jxta.ext.config.PublicAddress}
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.PublicAddress}
     */
    
    public void setPublicAddresses(List addresses) {
        clearPublicAddresses();
        addPublicAddresses(addresses);
    }

    /**
     * Removes the specified {@link net.jxta.ext.config.PublicAddress}
     *
     * @param       address     specified {@link net.jxta.ext.config.PublicAddress} value
     * @return                  removed {@link net.jxta.ext.config.PublicAddress} value
     */
    
    public Address removePublicAddress(PublicAddress address) {
        Address a = null;

        if (this.publicAddresses != null) {
            int i = this.publicAddresses.indexOf(address);

            if (i > -1) {
                a = (Address)this.publicAddresses.remove(i);
            }

            if (this.publicAddresses.size() == 0) {
                this.publicAddresses = null;
            }
        }

        return a;
    }

    /**
     * Clears the {@link net.jxta.ext.config.PublicAddress}
     */
    
    public void clearPublicAddresses() {
        if (this.publicAddresses != null) {
            this.publicAddresses.clear();

            this.publicAddresses = null;
        }
    }

    /**
     * Accessor to the enabled attribute.
     *
     * @return          enabled value
     */
    
    public boolean isProxy() {
        return this.isProxyEnabled;
    }

    /**
     * Specifies the enabled attribute.
     *
     * @param       isProxyEnabled      specified proxy enabled value
     */
    
    public void setProxy(boolean isProxyEnabled) {
        this.isProxyEnabled = isProxyEnabled;
    }

    /**
     * Accessor to the {@link net.jxta.ext.config.ProxyAddress}
     *
     * @return              returns the {@link net.jxta.ext.config.ProxyAddress}
     */
    
    public ProxyAddress getProxyAddress() {
        return this.proxy;
    }

    /**
     * Specifies the {@link net.jxta.ext.config.ProxyAddress}.
     *
     * @param       proxy       specified {@link net.jxta.ext.config.ProxyAddress}
     */
    
    public void setProxyAddress(ProxyAddress proxy) {
        this.proxy = proxy;
    }
}

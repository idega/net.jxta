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
 *  $Id: TcpTransportAddress.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TCP {@link net.jxta.ext.config.Transport} {@link net.jxta.ext.config.Address} implementation.
 *
 * <p> An {@link net.jxta.ext.config.Address} which represents TCP address.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class TcpTransportAddress
    extends Address
    implements Cloneable {

    private List multicastAddresses = null;

    /**
     * Default constructor.
     */
    
    public TcpTransportAddress() {
        this(null);
    }

    /**
     * Copy constructor.
     *
     * @param       ta      specified TcpTransportAddress
     */
    
    public TcpTransportAddress(TcpTransportAddress ta) {
        super(ta);

        setMulticastAddresses(ta != null ? ta.getMulticastAddresses() : null);
    }
    
    /**
     * Accessor to the {@link net.jxta.ext.config.MulticastAddress}.
     *
     * @return      {@link java.util.List} of {@link net.jxta.ext.config.MulticastAddress}
     */

    public List getMulticastAddresses() {
        return this.multicastAddresses != null ?
               this.multicastAddresses : Collections.EMPTY_LIST;
    }

    /**
     * Clears and specifies the {@link net.jxta.ext.config.MulticastAddress}.
     *
     * @param   address     specified {@link net.jxta.ext.config.MulticastAddress}
     */
    
    public void setMulticastAddress(MulticastAddress address) {
        List r = new ArrayList();

        r.add(address);

        setMulticastAddresses(r);
    }

    /**
     * Clears and specifies the {@link net.jxta.ext.config.MulticastAddress}.
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.MulticastAddress}
     */
    
    public void setMulticastAddresses(List addresses) {
        if (this.multicastAddresses != null) {
            this.multicastAddresses.clear();
        }

        addMulticastAddresses(addresses);
    }

    /**
     * Adds a specified {@link net.jxta.ext.config.MulticastAddress}.
     *
     * @param       address     specified {@link net.jxta.ext.config.MulticastAddress}
     */
    
    public void addMulticastAddress(MulticastAddress address) {
        List r = new ArrayList();

        r.add(address);

        addMulticastAddresses(r);
    }

    /**
     * Adds a {@link java.util.List} of {@link net.jxta.ext.config.MulticastAddress}
     *
     * @param       addresses       {@link java.util.List} of {@link net.jxta.ext.config.MulticastAddress}
     */
    
    public void addMulticastAddresses(List addresses) {
        for (Iterator i = addresses != null ?
                          addresses.iterator() : Collections.EMPTY_LIST.iterator();
             i.hasNext(); ) {
            MulticastAddress a = (MulticastAddress)i.next();

            if (a != null &&
                (this.multicastAddresses == null ||
                 !this.multicastAddresses.contains(a))) {
                if (this.multicastAddresses == null) {
                    this.multicastAddresses = new ArrayList();
                }

                this.multicastAddresses.add(a);
            }
        }
    }

    /**
     * Removes the specified {@link net.jxta.ext.config.Address}
     *
     * @param       address     specified {@link net.jxta.ext.config.Address}
     * @return                  removed {@link net.jxta.ext.config.Address}
     */
    
    public Address removeMulticastAddress(Address address) {
        Object o = null;

        if (this.multicastAddresses != null) {
            int i = this.multicastAddresses.indexOf(address);

            if (i > -1) {
                o = this.multicastAddresses.remove(i);

                if (this.multicastAddresses.size() == 0) {
                    this.multicastAddresses = null;
                }
            }
        }

        return (Address)o;
    }

    /**
     * Clears the {@link net.jxta.ext.config.MulticastAddress}
     */
    
    public void clearMulticastAddresses() {
        if (this.multicastAddresses != null) {
            this.multicastAddresses.clear();

            this.multicastAddresses = null;
        }
    }
}

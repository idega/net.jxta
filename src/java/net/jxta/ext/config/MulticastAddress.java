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
 *  $Id: MulticastAddress.java,v 1.1 2007/01/16 11:01:38 thomas Exp $
 */
package net.jxta.ext.config;

import java.net.URI;

/**
 * Multicast {@link net.jxta.ext.config.Address} implementation.
 *
 * <p>An {@link net.jxta.ext.config.Address} which represents a Multicast address.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class MulticastAddress
    extends Address {

    private boolean isMulticast = Default.MULTICAST_IS_ENABLED;
    private int size = Default.MULTICAST_SIZE;

    /**
     * Default constructor.
     */
    
    public MulticastAddress() {
        this(null);
    }

    /**
     * Copy constructor.
     *
     * @param       ma      specified MulticastAddress
     */
    
    public MulticastAddress(MulticastAddress ma) {
        super(ma);

        setAddress(ma != null ? ma.getAddress() : Default.MULTICAST_ADDRESS);
        setMulticast(ma != null ? ma.isMulticast() : Default.MULTICAST_IS_ENABLED);
        setSize(ma != null ? ma.getSize() : Default.MULTICAST_SIZE);
    }

    /**
     * Accessor to multicast indicator.
     *
     * <p>Indicates this instance is enabled as a multicast address.
     *
     * @return          multicast indicator.
     */
    
    public boolean isMulticast() {
        return this.isMulticast;
    }
    
    /**
     * Specifies the multicast attribute.
     *
     * @param       isMulticast     specified multicast indicator
     */

    public void setMulticast(boolean isMulticast) {
        this.isMulticast = isMulticast;
    }

    /**
     * Accessor to the multicast size attribute.
     *
     * <p>Specifies the multicast size attribute.
     *
     * @return          size attribute
     */
    
    public int getSize() {
        return this.size;
    }
    
    /**
     * Specifies the size attribute.
     *
     * @param       size        specified multicast size
     */
    
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Specifies the multicast address.
     *
     * @param       multicastAddress        specified multicast address
     */
    
    public void setAddress(URI multicastAddress) {
        if (multicastAddress != null &&
            !multicastAddress.getScheme().equals(Default.ANY_UDP_ADDRESS.getScheme())) {
            throw new IllegalArgumentException("invalid multicast address: " +
                multicastAddress);
        }

        super.setAddress(multicastAddress);
    }
}

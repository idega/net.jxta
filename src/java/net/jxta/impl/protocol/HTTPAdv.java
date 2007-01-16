/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: HTTPAdv.java,v 1.1 2007/01/16 11:01:41 thomas Exp $
 */

package net.jxta.impl.protocol;


import java.util.Enumeration;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.protocol.TransportAdvertisement;


/**
 * Configuration parameters for HttpServelet Message Transport.
 **/
public class HTTPAdv extends TransportAdvertisement {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(HTTPAdv.class.getName());
    
    private static final String CONFIGMODES[] = { "auto", "manual" };
    private static final String INDEXFIELDS[] = {/* none */};
    
    private static final String ProtocolTag = "Protocol";
    private static final String ProxyTag = "Proxy";
    private static final String ServerTag = "Server";
    private static final String PortTag = "Port";
    private static final String IntfAddrTag = "InterfaceAddress";
    private static final String ConfModeTag = "ConfigMode";
    private static final String FlagsTag = "Flags";
    private static final String PublicAddressOnlyAttr = "PublicAddressOnly";
    private static final String ProxyOffTag = "ProxyOff";
    private static final String ServerOffTag = "ServerOff";
    private static final String ClientOffTag = "ClientOff";
    
    private String proxy = null;
    private String server = null;
    private int listenPort = -1; // The real port a server listens to
    
    private String interfaceAddress = null; // What IP to bind to locally
    
    private String configMode = CONFIGMODES[0];
    private boolean publicAddressOnly = false;
    
    // These are for configuration; They get saved in the document only if they are
    // off and the correspondig item has a non-null value. So that the value is not lost.
    // When HttpTransport is done initializing, the unused values are set to null, and thus
    // pruned from the published adv.
    
    private boolean proxyEnabled = true;
    private boolean serverEnabled = true;
    private boolean clientEnabled = true;
    
    /**
     *  Our instantiator.
     **/
    public static class Instantiator implements AdvertisementFactory.Instantiator {
        
        /**
         *  {@inheritDoc}
         **/
        public String getAdvertisementType() {
            return HTTPAdv.getAdvertisementType();
        }
        
        /**
         *  {@inheritDoc}
         **/
        public Advertisement newInstance() {
            return new HTTPAdv();
        }
        
        /**
         *  {@inheritDoc}
         **/
        public Advertisement newInstance(Element root) {
            return new HTTPAdv(root);
        }
    }


    ;
    
    /**
     *  {@inheritDoc}
     **/
    public static String getAdvertisementType() {
        return "jxta:HTTPTransportAdvertisement";
    }
    
    /**
     *  Constructor for new advertisements. Use instantiator
     **/
    private HTTPAdv() {}
    
    /**
     *  Constructor for existing advertisements. Use instantiator
     **/
    private HTTPAdv(Element root) {
        if (!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports XLMElement");
        }
        
        XMLElement doc = (XMLElement) root;
        
        String doctype = doc.getName();
        
        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }
        
        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException("Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        
        Attribute attr = doc.getAttribute(FlagsTag);

        if (attr != null) {
            String options = attr.getValue();

            publicAddressOnly = (options.indexOf(PublicAddressOnlyAttr) != -1);
        }
        
        Enumeration elements = doc.getChildren();
        
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();
            
            if (!handleElement(elem)) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Unhandled Element: " + elem.toString());
                }
            }
        }
        
        // Sanity Check!!!
        
        // For consistency we force the flags to "disabled" for items we do not
        // have data for. However, the flags truely matter only when there is
        // data.
        if (proxy == null) {
            proxyEnabled = false;
        }
        
        if (!Arrays.asList(CONFIGMODES).contains(configMode)) {
            throw new IllegalArgumentException("Unsupported configuration mode.");
        }

        // XXX 20050118 bondolo Some versions apparently don't initialize this field. Eventually make it required.
        if( null == getProtocol() ) {
            setProtocol( "http" );
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    protected boolean handleElement(Element raw) {
        
        if (super.handleElement(raw)) {
            return true;
        }
        
        XMLElement elem = (XMLElement) raw;
        
        String tag = elem.getName();
        
        if (tag.equals(ProxyOffTag)) {
            proxyEnabled = false;
            return true;
        }
        
        if (tag.equals(ServerOffTag)) {
            serverEnabled = false;
            return true;
        }
        
        if (tag.equals(ClientOffTag)) {
            clientEnabled = false;
            return true;
        }
        
        String value = elem.getTextValue();
        
        if ((null == value) || (0 == value.trim().length())) {
            return false;
        }
        
        value = value.trim();

        if (tag.equals(ProtocolTag)) {
            setProtocol(value);
            return true;
        }
        
        if (tag.equals(IntfAddrTag)) {
            setInterfaceAddress(value);
            return true;
        }
        
        if (tag.equals(ConfModeTag)) {
            setConfigMode(value);
            return true;
        }
        
        if (tag.equals(PortTag)) {
            setPort(Integer.parseInt(value.trim()));
            return true;
        }
        
        if (tag.equals(ProxyTag)) {
            proxy = value;
            return true;
        }
        
        if (tag.equals(ServerTag)) {
            server = value;
            return true;
        }
        
        return false;
    }
    
    /**
     *  {@inheritDoc}
     *
     * <p/><emphasis>NB</emphasis>: we do not try to enforce dependency rules such as
     * Proxy only when router, because we want to convey the complete
     * configuration, even items corresponding to not currently enabled
     * features. HttpTransport will gracefully disregard items that have
     * no use in the current context.
     **/
    public Document getDocument(MimeMediaType encodeAs) {
        if (listenPort < 1) {
            throw new IllegalStateException("Illegal Listen Port Value");
        }
        
        // XXX 20050118 bondolo Some versions apparently don't initialize this field. Eventually make it required.
        if( null == getProtocol() ) {
            setProtocol( "http" );
        }

        StructuredDocument adv = (StructuredDocument) super.getDocument(encodeAs);

        if (adv instanceof Attributable) {
            // Only one flag for now. Easy.
            if (publicAddressOnly) {
                ((Attributable) adv).addAttribute(FlagsTag, PublicAddressOnlyAttr);
            }
        }
        
        Element e1 = adv.createElement(ProtocolTag, getProtocol());

        adv.appendChild(e1);
        
        Element e2 = adv.createElement(IntfAddrTag, getInterfaceAddress());

        adv.appendChild(e2);
        
        Element e3 = adv.createElement(ConfModeTag, getConfigMode());

        adv.appendChild(e3);
        
        Element e4 = adv.createElement(PortTag, Integer.toString(getPort()));

        adv.appendChild(e4);
        
        Element ext;
        
        if (proxy != null) {
            ext = adv.createElement(ProxyTag, proxy);
            adv.appendChild(ext);
        }
        
        // If disabled, say it; otherwise it is assumed on. In published
        // advs, we only keep data for items that are ON, so we do not
        // have to clutter them with the flag.
        if (!proxyEnabled) {
            ext = adv.createElement(ProxyOffTag);
            adv.appendChild(ext);
        }
        
        if (server != null) {
            ext = adv.createElement(ServerTag, server);
            adv.appendChild(ext);
        }
        
        // If disabled, say it; otherwise it is assumed on. In published
        // advs, we only keep data for items that are ON, so we do not
        // have to clutter them with the flag.
        if (!serverEnabled) {
            ext = adv.createElement(ServerOffTag);
            adv.appendChild(ext);
        }
        
        // If disabled, say it; otherwise it is assumed on. In published
        // advs, we only keep data for items that are ON, so we do not
        // have to clutter them with the flag.
        if (!clientEnabled) {
            ext = adv.createElement(ClientOffTag);
            adv.appendChild(ext);
        }
        
        return adv;
    }
    
    /**
     * Returns the interfaceAddr. That is, the ip of the IF to which to bind
     * locally created sockets.
     *
     * @return string The address.
     *
     */
    
    public String getInterfaceAddress() {
        return interfaceAddress;
    }
    
    /**
     * Returns the interfaceAddr. That is, the ip of the IF to which to bind
     * locally created sockets.
     *
     * @param string The address
     *
     */
    public void setInterfaceAddress(String address) {
        this.interfaceAddress = address;
    }
    
    public boolean getPublicAddressOnly() {
        return publicAddressOnly;
    }
    
    public void setPublicAddressOnly(boolean only) {
        publicAddressOnly = only;
    }
    
    /**
     * returns the config mode. That is, how the user prefers to configure
     * the interface address: "auto", "manual"
     *
     * @return string config mode
     **/
    public String getConfigMode() {
        return configMode;
    }
    
    /**
     * set the config mode. That is, how the user prefers to configure
     * the interface address: "auto", "manual"
     *
     * This is just a pure config item. It is never in published advs. The TCP
     * transport strips it when it initializes.
     *
     * @param mode Can be "auto", "manual" other settings will act as the default
     * which is "auto".
     */
    public void setConfigMode(String mode) {
        if (!Arrays.asList(CONFIGMODES).contains(mode)) {
            throw new IllegalArgumentException("Unsupported configuration mode.");
        }
        
        configMode = mode;
    }
    
    /**
     * Returns the port number to which server sockets are locally bound.
     *
     * @return String the port
     */
    
    public int getPort() {
        return listenPort;
    }
    
    /**
     * Sets the port number to which server sockets are locally bound.
     *
     * @param Port the port
     */
    
    public void setPort(int newPort) {
        listenPort = newPort;
    }
    
    public String  getProxy() {
        return  proxy;
    }

    public String  getServer() {
        return server;
    }
    
    public boolean  isProxyEnabled() {
        return  proxyEnabled;
    }
    
    public boolean isServerEnabled() {
        return serverEnabled;
    }
    
    public boolean isClientEnabled() {
        return clientEnabled;
    }
    
    // If one of proxy, server, or router is cleared, the corresponding
    // enabled flag should be false (the opposite is not true).
    
    public void  setProxy(String name) {
        proxy = name;
        if (name == null) {
            proxyEnabled = false;
        }
    }
    
    public void setServer(String name) {
        server = name;
    }
    
    public void setProxyEnabled(boolean enabled) {
        proxyEnabled = enabled;
    }

    public void setServerEnabled(boolean enabled) {
        serverEnabled = enabled;
    }

    public void setClientEnabled(boolean enabled) {
        clientEnabled = enabled;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String[] getIndexFields() {
        return INDEXFIELDS;
    }
}

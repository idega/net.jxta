/*
 *  $Id: WirePipeImpl.java,v 1.1 2007/01/16 11:01:58 thomas Exp $
 *
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */
package net.jxta.impl.pipe;

import java.util.Set;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;

/**
 * This class implements the NetPipe interface.
 */

public class WirePipeImpl implements EndpointListener {

    private final static Logger LOG = Logger.getLogger(WirePipeImpl.class.getName());

    /**
     *  Description of the Field
     */
    public final static String WireName = "jxta.service.wirepipe";
    /**
     *  Description of the Field
     */
    public final static String WireTagName = "JxtaWireHeader";
    /**
     *  Description of the Field
     */
    public final static String WireServiceName = "PipeService.Wire";

    private PeerGroup group = null;
    private PipeResolver pipeResolver = null;
    private RendezVousService rendezvous = null;
    private final String WireParam;
    private String localPeerId = null;


    /**
     * @param  group         Description of the Parameter
     * @param  pipeResolver  Description of the Parameter
     */
    WirePipeImpl(PeerGroup group, PipeResolver pipeResolver) {

        this.group = group;
        this.pipeResolver = pipeResolver;

        this.rendezvous = group.getRendezVousService();

        if (null == rendezvous) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("Cannot run without rendezvous service");
            }

            throw new IllegalStateException("Cannot run without rendezvous service");
        }

        this.localPeerId = group.getPeerID().toString();

        this.WireParam = group.getPeerGroupID().getUniqueValue().toString();
        // Register with endpoint to support WirePrivateOutputPipe --vasha
        group.getEndpointService().addIncomingMessageListener(this, WireServiceName, null);
    }


    /**
     * To support WirePipe.send(Message, Enumeration)
     *
     * @return    The serviceParameter value
     */
    public String getServiceParameter() {
        return WireParam;
    }


    /**
     * To support WirePipe.send(Message, Enumeration)
     *
     * @return    The serviceName value
     */
    public String getServiceName() {
        return WireServiceName;
    }


    /**
     * Supply arguments and starts this service if it hadn't started by itself.
     *
     * Currently this service does not expect arguments.
     *
     * @param  arg  A table of strings arguments.
     * @return      int status indication.
     */
    public int startApp(String[] arg) {

        // Set our Propagate listener
        try {
            rendezvous.addPropagateListener(WireName, WireParam, this);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Cannot register Propagate Listener", e);
            }
            throw new IllegalStateException("Cannot register Propagate Listener");
        }

        return 0;
    }


    /**
     * Ask this service to stop.
     */
    public void stopApp() {

        // Clear our Propagate listener
        rendezvous.removePropagateListener(WireName, WireParam, this);

        group.getEndpointService().removeIncomingMessageListener(WireServiceName, null);
    }

    /**
     * create an InputPipe from a pipe Advertisement
     *
     * @param  adv              is the advertisement of the PipeServiceImpl.
     * @param  listener         PipeMsgListener to receive msgs.
     * @return                  InputPipe InputPipe object created
     * @exception  IOException  error creating input pipe
     */
    InputPipe createInputPipe(PipeAdvertisement adv, PipeMsgListener listener) throws IOException {

        WirePipe wirePipe = getWirePipe(adv);
        return new InputPipeImpl(wirePipe, adv, listener);
    }


    /**
     * create an OutputPipe from the pipe Advertisement giving a PeerId(s)
     * where the corresponding InputPipe is supposed to be.
     *
     * @param  adv           is the advertisement of the NetPipe.
     * @param  peers         is an enumeration of the PeerId of the peers where to look
     * for the corresponding Pipes
     * @return               OuputPipe
     * @throws  IOException  if none of the peers in the enumeration has the
     * corresponding OutputPipe
     */
    NonBlockingWireOutputPipe createOutputPipe(PipeAdvertisement adv, Set peers) {

        WirePipe wirePipe = getWirePipe(adv);

        return new NonBlockingWireOutputPipe(group, wirePipe, adv, peers);
    }

    /**
     * PropagateType pipes
     *
     * @param  adv  the pipe adv
     * @return      the wire pipe
     */
    private synchronized WirePipe getWirePipe(PipeAdvertisement adv) {

        WirePipe wirePipe = (WirePipe) pipeResolver.findLocal((PipeID) adv.getPipeID());

        // First see if we have already a WirePipe for this pipe
        if (null != wirePipe) {
            return wirePipe;
        }

        // No.. There is none. Create a new one.
        return new WirePipe(group, pipeResolver, this, adv);
    }

    /**
     * PropagateType pipes
     *
     * @param  pipeID  Pipe ID
     * @return         the wire pipe
     */
    private synchronized WirePipe getWirePipe(PipeID pipeID) {

        WirePipe wirePipe = (WirePipe) pipeResolver.findLocal(pipeID);

        // First see if we have already a WirePipe for this pipe
        if (null != wirePipe) {
            return wirePipe;
        }

        // No.. There is none. Create a new one.
        // XXX 20031019 bondolo@jxta.org Check for the adv in local discovery maybe?
        PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        adv.setPipeID(pipeID);
        adv.setType(PipeService.PropagateType);

        return new WirePipe(group, pipeResolver, this, adv);
    }


    /**
     *  {@inheritDoc}
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        // Check if there is a JXTA-WIRE header
        MessageElement elem = message.getMessageElement("jxta", WireTagName);

        if (null == elem) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("No JxtaWireHeader element. Discarding message.");
            }
            return;
        }

        WireHeader header;
        try {
            XMLDocument doc = (XMLDocument)
                    StructuredDocumentFactory.newStructuredDocument(elem.getMimeType(), elem.getStream());
            header = new WireHeader(doc);
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("bad wire header", e);
            }
            return;
        }

        WirePipe wirePipe = getWirePipe((PipeID) header.getPipeID());
        wirePipe.processIncomingMessage(message, header, srcAddr, dstAddr);
    }
}


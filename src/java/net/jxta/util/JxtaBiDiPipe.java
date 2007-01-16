/*
 *  $Id: JxtaBiDiPipe.java,v 1.1 2007/01/16 11:01:35 thomas Exp $
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
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *  not be used to endorse or promote products derived from this
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
 */
package net.jxta.util;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import java.io.IOException;

import net.jxta.credential.Credential;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.impl.util.UnbiasedQueue;
import net.jxta.impl.util.pipe.reliable.OutgoingMsgrAdaptor;
import net.jxta.impl.util.pipe.reliable.ReliableInputStream;
import net.jxta.impl.util.pipe.reliable.ReliableOutputStream;
import net.jxta.impl.util.pipe.reliable.FixedFlowControl;
import net.jxta.impl.util.pipe.reliable.Defs;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * JxtaBiDiPipe is a bi-directional Pipe, it creates an InputPipe for incoming
 * Messages, and EndpointMessenger for outgoing messages
 * JxtaBiDiPipe defines its own protocol for negotiating connections. Connection
 * requests arrive as a JXTA Message with the following elements :
 *
 *  <p>
 *  &lt;Credential> to determine whether requestor has the proper access to be 
 *              granted a connection
 *  <p>
 *  &lt;reqPipe> Requestor's pipe advertisement &lt;/reqPipe>
 *  <p>
 *  &lt;remPipe> remote pipe advertisement &lt;/remPipe>
 *  <p>
 *  &lt;remPeer> remote peer advertisement &lt;/remPeer>
 *  <p>
 *  &lt;reliable> Reliability setting ("true", or "false") &lt;/reliable>
 *  <p>
 *  &lt;data> data &lt;data>
 *
 */
public class JxtaBiDiPipe implements PipeMsgListener,
            OutputPipeListener,
    ReliableInputStream.MsgListener {

    private final static Logger LOG = Logger.getLogger(JxtaBiDiPipe.class.getName());
    private PipeAdvertisement remotePipeAdv;
    private PeerID peerid;
    private PeerAdvertisement remotePeerAdv;
    private int timeout = 60000;
    private UnbiasedQueue queue;

    protected PeerGroup group;
    protected PipeAdvertisement pipeAdv;
    protected PipeAdvertisement myPipeAdv;
    protected PipeService pipeSvc;
    protected InputPipe in;
    protected OutputPipe connectOutpipe;
    protected Messenger msgr;
    protected InputStream stream;
    protected final String closeLock  = new String("closeLock");
    protected final String acceptLock = new String("acceptLock");
    protected final String finalLock  = new String("finalLock");
    protected boolean closed = false;
    protected boolean bound = false;
    protected PipeMsgListener msgListener;
    protected PipeEventListener eventListener;
    protected Credential credential = null;
    protected boolean waiting;
    protected boolean isReliable = false;
    protected OutgoingMsgrAdaptor outgoing = null;
    protected ReliableInputStream ris = null;
    protected ReliableOutputStream ros = null;
    protected StructuredDocument credentialDoc = null;
    protected StructuredDocument myCredentialDoc = null;

    /**
     * Pipe close Event
     */
     public static final int PIPE_CLOSED_EVENT = 1;
     
    /**
     *  JxtaBiDiPipe A bidirectional pipe
     *
     *@param  group            group context
     *@param  msgr             lightweight output pipe
     *@param  pipe             PipeAdvertisement
     *@param  isReliable       Whether the connection is reliable or not
     *@exception  IOException  if an io error occurs
     */
    protected JxtaBiDiPipe(PeerGroup group,
                           Messenger msgr,
                           PipeAdvertisement pipe,
                           StructuredDocument credDoc,
                           boolean isReliable) throws IOException {
        if (msgr == null ) {
            throw new IOException("Null Messenger");
        }
        this.group = group;
        this.pipeAdv = pipe;
        this.credentialDoc = credDoc;
        this.pipeSvc = group.getPipeService();
        this.credentialDoc = getCredDoc(group);
        this.in = pipeSvc.createInputPipe(pipe, this);
        this.msgr = msgr;
        this.isReliable = isReliable;
        queue = UnbiasedQueue.synchronizedQueue(new UnbiasedQueue(100, false));
        createRLib();
        setBound();
    }

    /**
     * JxtaBiDiPipe A bidirectional pipe
     * Creates a new object with a default timeout of 60,000ms, and a reliability 
     * setting of false
     *
     */
    public JxtaBiDiPipe() {}

    /**
     * attempts to create a bidirectional connection to remote peer within default
     * timeout of 60,000ms, and initiates a connection
     *
     *@param  group            group context
     *@param  pipeAd           PipeAdvertisement
     *@param msgListener          application PipeMsgListener 
     *@exception  IOException  if an io error occurs
     */
    public JxtaBiDiPipe(PeerGroup group,
                        PipeAdvertisement pipeAd,
                        PipeMsgListener msgListener) throws IOException {

        connect(group, null, pipeAd, timeout, msgListener);
    }

    /**
     * attempts to create a bidirectional connection to remote peer within default
     * timeout of 1 minutes, and initiates a connection
     *
     *@param  group            group context
     *@param  pipeAd           PipeAdvertisement
     *@param msgListener          application PipeMsgListener 
     *@exception  IOException  if an io error occurs
     */
    public JxtaBiDiPipe(PeerGroup group,
                        PipeAdvertisement pipeAd,
                        int timeout,
                        PipeMsgListener msgListener) throws IOException {

        connect(group, null, pipeAd, timeout, msgListener);
    }

    /**
     * attempts to create a bidirectional connection to remote peer within default
     * timeout of 60,000ms, and initiates a connection
     *
     *@param  group            group context
     *@param  pipeAd           PipeAdvertisement
     *@param msgListener          application PipeMsgListener 
     *@exception  IOException  if an io error occurs
     */
    public JxtaBiDiPipe(PeerGroup group,
                        PipeAdvertisement pipeAd,
                        int timeout,
                        PipeMsgListener msgListener,
                        boolean reliable) throws IOException {

        connect(group, null, pipeAd, timeout, msgListener, reliable);
    }


    /**
     *  Connect to JxtaBiDiPipe with default timeout
     *
     *@param  group            group context
     *@param  pipeAd           PipeAdvertisement
     *@exception  IOException  if an io error occurs
     */
    public void connect(PeerGroup group, PipeAdvertisement pipeAd) throws IOException {
        connect(group, pipeAd, timeout);
    }


    /**
     *  Connects to a remote JxtaBiDiPipe
     *
     *@param  group            group context
     *@param  pipeAd           PipeAdvertisement
     *@param  timeout          timeout in ms, also reset object default timeout
                               to that of timeout
     *@exception  IOException  if an io error occurs
     */
    public void connect(PeerGroup group,
                        PipeAdvertisement pipeAd,
                        int timeout) throws IOException {

        connect(group, null, pipeAd, timeout, null);
    }

    /**
     *  Connects to a remote JxtaBiDiPipe
     *
     *@param  group            group context
     *@param  peerid           peer to connect to
     *@param  pipeAd           PipeAdvertisement
     *@param  timeout          timeout in ms, also reset object default timeout to that of timeout
     *@exception  IOException  if an io error occurs
     */
    public void connect(PeerGroup group,
                        PeerID peerid,
                        PipeAdvertisement pipeAd,
                        int timeout,
                        PipeMsgListener msgListener) throws IOException {

        connect(group, peerid, pipeAd, timeout, msgListener, isReliable);
    }

    /**
     *  Connects to a remote JxtaBiDiPipe
     *
     *@param  group            group context
     *@param  peerid           peer to connect to
     *@param  pipeAd           PipeAdvertisement
     *@param  timeout          timeout in ms, also reset object default timeout to that of timeout
     *@param  reliable         Reliable connection
     *@exception  IOException  if an io error occurs
     */
    public void connect(PeerGroup group,
                        PeerID peerid,
                        PipeAdvertisement pipeAd,
                        int timeout,
                        PipeMsgListener msgListener,
                        boolean reliable) throws IOException {

        if (isBound()) {
            throw new IOException("Pipe already bound");
        }
        this.pipeAdv = pipeAd;
        this.group = group;
        this.msgListener = msgListener;
        this.isReliable = reliable;
        pipeSvc = group.getPipeService();
        this.timeout = timeout;
        this.peerid = peerid;
        myPipeAdv = JxtaServerPipe.newInputPipe(group, pipeAd);
        this.in = pipeSvc.createInputPipe(myPipeAdv, this);
        this.credentialDoc = getCredDoc(group);
        Message openMsg = createOpenMessage(group, myPipeAdv);
        // create the output pipe and send this message
        if (peerid == null) {
            pipeSvc.createOutputPipe(pipeAd, this);
        } else {
            pipeSvc.createOutputPipe(pipeAd, Collections.singleton(peerid), this);
        }
        try {
            synchronized (acceptLock) {
                // check connectOutpipe within lock to prevent a race with modification.
                if (connectOutpipe == null) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Waiting for "+timeout+" msec");
                    }
                    acceptLock.wait(timeout);
                }
            }
        } catch (InterruptedException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Interrupted", ie);
            }
        }
        if (connectOutpipe == null) {
            throw new IOException("connection timeout");
        }
        // send connect message
        waiting = true;
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending a backchannel message");
        }
        connectOutpipe.send(openMsg);
        //wait for the second op
        try {
            synchronized (finalLock) {
                if(waiting) {
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Waiting for "+timeout+" msec for back channel to be established");
                    }
                    finalLock.wait(timeout);
                    //Need to check for creation
                    if (msgr == null) {
                        throw new IOException("connection timeout");
                    }
                }
            }
        } catch (InterruptedException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Interrupted", ie);
            }
            throw new IOException("Interrupted");
        }
        if (msgListener == null) {
            queue = UnbiasedQueue.synchronizedQueue(new UnbiasedQueue());
        }
        setBound();
    }

    /**
     * creates all the reliability objects
     */
    private void createRLib() {
        if(isReliable) {
            if(outgoing == null) {
                outgoing = new OutgoingMsgrAdaptor(msgr, timeout);
            }
            if (ros == null) {
                ros = new ReliableOutputStream(outgoing, new FixedFlowControl(20));
            }
            if (ris == null) {
                ris = new ReliableInputStream(outgoing, timeout, this);
            }
        }
    }


    /**
     * Toggles reliability
     * 
     *@param  reliable Toggles reliability to reliable
     * @throws IOEXecption if pipe is bound
     */
    public void setReliable(boolean reliable) throws IOException {
        if (isBound()) {
            throw new IOException("Can not set reliability after pipe is bound");
        }
        this.isReliable = reliable;
    }

    /**
     *  obtain the cred doc from the group object
     *
     *@param  group  group context
     *@return        The credDoc value
     */
    protected static StructuredDocument getCredDoc(PeerGroup group) {
        try {
            MembershipService membership = group.getMembershipService();
            Credential credential = membership.getDefaultCredential();
            if (credential != null) {
                return credential.getDocument(MimeMediaType.XMLUTF8);
            }
        } catch (Exception e) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("failed to get credential", e);
            }
        }
        return null;
    }

    /**
     *  get the remote credential doc
     *  @return Credential StructuredDocument 
     */
    public StructuredDocument getCredentialDoc() {
        return credentialDoc;
    }

    /**
     *  Sets the connection credential doc
     *  If no credentials are set, the default group credential will be used
     *  @param doc Credential StructuredDocument 
     */
    public  void setCredentialDoc(StructuredDocument doc) {
        this.myCredentialDoc = doc;
    }

    /**
     *  Create a connection request message
     *
     *@param  group   group context
     *@param  pipeAd  pipe advertisement
     *@return         the Message  object
     */
    protected Message createOpenMessage(PeerGroup group, PipeAdvertisement pipeAd) throws IOException {

        Message msg = new Message();
        PeerAdvertisement peerAdv = group.getPeerAdvertisement();
        if (myCredentialDoc == null) {
            myCredentialDoc = getCredDoc(group);
        }
        if (myCredentialDoc == null && pipeAd.getType().equals(PipeService.UnicastSecureType)) {
            throw new IOException("No credentials established to initiate a secure connection");
        }
        try {
            if (myCredentialDoc != null) {
                msg.addMessageElement(JxtaServerPipe.nameSpace,
                                  new TextDocumentMessageElement(JxtaServerPipe.credTag,
                                                                     (XMLDocument) myCredentialDoc, null));
            }
            msg.addMessageElement(JxtaServerPipe.nameSpace,
                                  new TextDocumentMessageElement(JxtaServerPipe.reqPipeTag,
                                                                 (XMLDocument) pipeAd.getDocument(MimeMediaType.XMLUTF8), null));
            msg.addMessageElement(JxtaServerPipe.nameSpace,
                                  new StringMessageElement(JxtaServerPipe.reliableTag,
                                                           Boolean.toString(isReliable),
                                                           null));

            msg.addMessageElement(JxtaServerPipe.nameSpace,
                                  new TextDocumentMessageElement(JxtaServerPipe.remPeerTag,
                                                                 (XMLDocument) peerAdv.getDocument(MimeMediaType.XMLUTF8), null));
            return msg;
        } catch (Throwable t) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("error getting element stream", t);
            }
            return null;
        }
    }

    /**
     *  Accepts a connection
     *
     *@param  s                the accepted connection.
     *@exception  IOException  if an I/O error occurs when accepting the
     *      connection.
     */
    protected void accept(JxtaBiDiPipe s) throws IOException {
        if (closed) {
            throw new IOException("Pipe is closed");
        }
        if (!isBound()) {
            throw new IOException("Pipe not bound");
        }
        try {
            synchronized (acceptLock) {
                // check connectOutpipe within lock to prevent a race with modification.
                if (connectOutpipe == null) {
                    acceptLock.wait(timeout);
                }
            }
        } catch (InterruptedException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Interrupted", ie);
            }
        }
    }

    /**
     *  Sets the bound attribute of the JxtaServerPipe object
     */
    void setBound() {
        bound = true;
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Pipe Bound :true");
        }
    }

    /**
     * Returns the binding state of the JxtaServerPipe.
     *
     * @return    true if the ServerSocket successfully bound to an address
     */
    public boolean isBound() {
        return bound;
    }

    /**
     *  Returns an input stream for this socket.
     *
     *@return                  a stream for reading from this socket.
     *@exception  IOException  if an I/O error occurs when creating the
     *      input stream.
     */
    public InputPipe getInputPipe() throws IOException {
        return in;
    }


    /**
     * Returns the messenger to the remote pipe
     * Note that this method will block until a messenger is created.
     * This is especially likely when this is a server and are waiting 
     * to connect to a client.
     *
     *@return                  the pipe messenger
     *@exception  IOException  if an I/O error occurs when
     *@deprecated use sendMessage instead
     *
     */
    public Messenger getMessenger() throws IOException {

        if (isReliable) {
            throw new IOException("Can not access the messenger in reliable mode, use sendMessage instead");
        }
        // If null, the call was made before the pipe message occured
        if (msgr == null) {
            int count = -1;
            while (msgr == null  && !closed) {
                if ((++count)*500 >= timeout) {
                    throw new IOException("JxtaBiDiPipe timed out");
                }
                waiter(500);
            }
        }
        if (closed) {
            throw new IOException("JxtaBiDiPipe is closed");
        }
        return msgr;
    }

    protected synchronized void waiter(int timeMilisecs) {
        try {
            wait(timeMilisecs);
        } catch(Exception e) {
            LOG.error("error waiting",e);
        }
    }

    /**
     * Returns remote PeerAdvertisement
     * @return remote PeerAdvertisement
     */
    public PeerAdvertisement getRemotePeerAdvertisement() {

        return remotePeerAdv;
    }

    /**
     * Returns remote PipeAdvertisement
     * @return remote PipeAdvertisement
     */
    public PipeAdvertisement getRemotePipeAdvertisement() {

        return remotePipeAdv;
    }

    /**
     * Sets the remote PeerAdvertisement
     * @param peer Remote PeerAdvertisement
     */
    protected void setRemotePeerAdvertisement(PeerAdvertisement peer) {

        this.remotePeerAdv = peer;
    }

    /**
     * Sets the remote PipeAdvertisement
     * @param pipe PipeAdvertisement
     */
    protected void setRemotePipeAdvertisement(PipeAdvertisement pipe) {

        this.remotePipeAdv = pipe;
    }

    /**
     *  Closes this pipe.
     *
     *@exception  IOException  if an I/O error occurs when closing this
     *      socket.
     */
    public void close() throws IOException {
        sendClose();
        closePipe();
    }

    protected void closePipe() throws IOException {
        // close both pipes
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
            bound = false;
        }
        if (isReliable) {
            long quitAt = System.currentTimeMillis() + timeout;
            while (true) {
                if (ros == null) {
                    // Nothing to worry about.
                    break;
                }

                // ros will not take any new message, now.
                ros.setClosing();
                if (ros.getMaxAck() == ros.getSeqNumber()) {
                    break;
                }

                // By default wait forever.
                long left = 0;

                // If timeout is not zero. Then compute the waiting time
                // left.
                if (timeout != 0) {
                    left = quitAt - System.currentTimeMillis();
                    if (left < 0) {
                        // Too late
                        sendClose();
                        throw new IOException("Close timeout");
                    }
                }

                try {
                    if (!ros.isQueueEmpty()) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Waiting for Output stream queue event");
                        }
                        ros.waitQueueEvent(left);
                    }
                    break;
                } catch (InterruptedException ie) {
                    // give up, then.
                    throw new IOException("Close interrupted");
                }
            }

            // We are initiating the close. We do not want to receive
            // anything more. So we can close the ris right away.
            ris.close();
        }
        
        if (isReliable && ros != null) {
            ros.close();
        }
        // close the pipe
        in.close();
        msgr.close();
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Pipe close complete");
        }
        if (eventListener != null) {
            try {
                eventListener.pipeEvent(PIPE_CLOSED_EVENT);
            } catch (Throwable th) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("error during pipe event callback", th);
                }
            }
        }
    }


    /**
     *  Sets the inputPipe attribute of the JxtaBiDiPipe object
     *
     *@param  in  The new inputPipe value
     */
    protected void setInputPipe(InputPipe in) {
        this.in = in;
    }


    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {
        Message message = event.getMessage();
        if (message == null) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Empty event");
            }
            return;
        }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Pipe message arrived");
        }
        MessageElement element = null;
        if (!bound) {
            // look for a remote pipe answer
            element = (MessageElement)
                      message.getMessageElement(JxtaServerPipe.nameSpace,
                                                JxtaServerPipe.remPipeTag);

            if (element != null) {
                // connect response
                try {
                    StructuredDocument CredDoc=null;
                    InputStream in = element.getStream();
                    remotePipeAdv = (PipeAdvertisement)
                                    AdvertisementFactory.newAdvertisement(element.getMimeType(), in);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Recevied a pipe Advertisement :" +remotePipeAdv.getName());
                    }
                    element = message.getMessageElement(JxtaServerPipe.nameSpace,
                                                        JxtaServerPipe.remPeerTag);
                    if (element != null) {
                        in = element.getStream();
                        remotePeerAdv = (PeerAdvertisement)
                                        AdvertisementFactory.newAdvertisement(element.getMimeType(), in);
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Recevied an Peer Advertisement :" +remotePeerAdv.getName());
                        }
                    } else {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn(" BAD connect response");
                        }
                        return;
                    }

                    element = message.getMessageElement(JxtaServerPipe.nameSpace,
                                                        JxtaServerPipe.credTag);
                    if (element != null) {
                        in = element.getStream();
                        CredDoc = (StructuredDocument)
                                  StructuredDocumentFactory.newStructuredDocument(element.getMimeType(), in);
                    }
                    if (pipeAdv.getType().equals(PipeService.UnicastSecureType) && (CredDoc ==null || !checkCred(CredDoc))) {
                        // we're done here
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("Invalid remote credential doc");
                        }
                        return;
                    }

                    element = message.getMessageElement (JxtaServerPipe.nameSpace,
                                                         JxtaServerPipe.reliableTag);
                    if (element != null) {
                        isReliable = (Boolean.valueOf(element.toString())).booleanValue();
                    }
                    msgr = lightweightOutputPipe(group, remotePipeAdv, remotePeerAdv);
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("Reliability set to :"+isReliable);
                    }
                    if (isReliable) {
                        createRLib();
                    }
                    synchronized (finalLock) {
                        waiting = false;
                        finalLock.notifyAll();
                    }
                } catch (IOException e) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("failed to process response message", e);
                    }
                }
                return;
            }
        }

        if(isReliable) {
            //let reliabilty deal with the message
            receiveMessage(message);
            return;
        }
        if (!hasClose(message)) {
            push(event);
        }
    }

    private boolean hasClose(Message message) {
        // look for close request
        MessageElement element = (MessageElement)
                                 message.getMessageElement(JxtaServerPipe.nameSpace,
                                                           JxtaServerPipe.closeTag);
        if (element != null) {
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Recevied a pipe close request, closing pipes");
                }
                closePipe();
            } catch (IOException ie) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("failed during close", ie);
                }
            }
            return true;
        }
        return false;
    }

    private void receiveMessage(Message message) {
        Iterator i =
            message.getMessageElements(Defs.NAMESPACE, Defs.MIME_TYPE_ACK);

        if (i.hasNext()) {
            if (ros != null) {
                ros.recv(message);
            }
            return;
        }

        i = message.getMessageElements(Defs.NAMESPACE, Defs.MIME_TYPE_BLOCK);
        if (i.hasNext()) {

            // It can happen that we receive messages for the input stream
            // while we have not finished creating it.
            try {
                synchronized (finalLock) {
                    while (waiting) {
                        finalLock.wait(timeout);
                    }
                }
            } catch (InterruptedException ie) {}

            if (ris != null) {
                ris.recv(message);
            }
        }

    }

    /**
     * This method is invoked by the Reliablity library for each incoming data message
     *
     * @param message Incoming message
     */
    public void processIncomingMessage(Message message) {
        if (!hasClose(message)) {
            PipeMsgEvent event = new PipeMsgEvent(this, message, (PipeID) in.getPipeID());
            push(event);
        }
    }

    private void push(PipeMsgEvent event) {
        if (msgListener == null) {
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("push message onto queue");
                }
                queue.push(event, -1);
            } catch (InterruptedException ie) {}
        }
        else {
            dequeue();
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("calling message listener");
            }
            msgListener.pipeMsgEvent(event);
        }

    }

    /**
     * Send a message
     *
     *@param  msg  Message to send to the remote side
     */

    public boolean sendMessage(Message msg) throws IOException {
        if (isReliable) {
             int seqn = ros.send(msg);
             return (seqn > 0);
        } else {
            return msgr.sendMessage(msg);
        }
    }

    private void dequeue() {
        while (queue != null && queue.getCurrentInQueue() > 0) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("dequeing messages onto message listener");
            }
            msgListener.pipeMsgEvent((PipeMsgEvent) queue.pop());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void outputPipeEvent(OutputPipeEvent event) {
        OutputPipe op = event.getOutputPipe();
        if (op.getAdvertisement() == null) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("The output pipe has no internal pipe advertisement. Continueing anyway.");
            }
        }
        if (op.getAdvertisement() == null || pipeAdv.equals(op.getAdvertisement())) {
            synchronized (acceptLock) {
                // modify op within lock to prevent a race with the if.
                if (connectOutpipe == null) {
                    connectOutpipe = op;
                    // set to null to avoid closure
                    op = null;
                }
                acceptLock.notifyAll();
            }
            // Ooops one too many, we were too fast re-trying.
            if (op != null) {
                op.close();
            }

        } else {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("Unexpected OutputPipe :"+op);
            }
        }
    }

    /**
     *  A lightweight output pipe constructor, note the return type
     * Since all the info needed is available, there's no need for to 
     * use the pipe service to resolve the pipe we have all we need
     * to construct a messenger.
     *
     *@param  group    group context
     *@param  pipeAdv  Remote Pipe Advertisement
     *@param  peer     Remote Peer advertisement
     *@return          Messenger
     */
    protected static Messenger lightweightOutputPipe(PeerGroup group, PipeAdvertisement pipeAdv,
            PeerAdvertisement peer) {

        EndpointService endpoint = group.getEndpointService();
        ID opId = pipeAdv.getPipeID();
        String destPeer = (peer.getPeerID().getUniqueValue()).toString();
        // Get an endpoint messenger to that address
        EndpointAddress addr;
        if (pipeAdv.getType().equals(PipeService.UnicastType)) {
            addr = new EndpointAddress("jxta",
                                       destPeer,
                                       "PipeService",
                                       opId.toString());
        } else
            if (pipeAdv.getType().equals(PipeService.UnicastSecureType)) {
                addr = new EndpointAddress("jxtatls",
                                           destPeer,
                                           "PipeService",
                                           opId.toString());
            } else {
                // not a supported type
                return null;
            }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Creating a lightweightOutputPipe()");
        }
        return endpoint.getMessenger(addr);
    }

    /**
     *  Not implemented yet
     */
    protected boolean checkCred(StructuredDocument cred) {

        //FIXME need to check credentials
        return true;
    }

    /**
     *  Send a close message to the remote side
     */
    private void sendClose() {

        Message msg = new Message();
        msg.addMessageElement(JxtaServerPipe.nameSpace,
                              new StringMessageElement(JxtaServerPipe.closeTag,
                                                       "close",
                                                       null));
        try {
            sendMessage(msg);
        } catch (IOException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.error("failed during close", ie);
            }
        }
    }

    /**
     * Returns the message listener for this pipe
     * @return PipeMsgListener
     * @deprecated use getMessageListener instead
     */
    public PipeMsgListener getListener() {
        return getMessageListener();
    }

    /**
     * Returns the message listener for this pipe
     * @return PipeMsgListener
     *
     */
    public PipeMsgListener getMessageListener() {
        return msgListener;
    }

    /**
     * Sets message listener for a pipe spawned by the JxtaServerPipe.
     * There is a window where a message could arrive prior to listener being
     * registered therefore a message queue is created to queue messages, once
     * a listener is registered these messages will be dequeued by calling the
     * listener until the queue is empty
     *
     * @param listener New value of property listener.
     * @deprecated use setMessageListener instead
     */
    public void setListener(PipeMsgListener msgListener) {
        setMessageListener(msgListener);
    }
  
    /**
     * Sets message listener for a pipe spawned by the JxtaServerPipe.
     * There is a window where a message could arrive prior to listener being
     * registered therefore a message queue is created to queue messages, once
     * a listener is registered these messages will be dequeued by calling the
     * listener until the queue is empty
     *
     * @param listener New value of property listener.
     *
     */
    public void setMessageListener(PipeMsgListener msgListener) {
        this.msgListener = msgListener;
        // if there are messages enqueued then dequeue them onto the msgListener
        dequeue();
    }
    
    /**
     * Sets a Pipe event listener, set listener to null to unset the listener
     *
     * @param listener New value of property listener.
     * @deprecated use setPipeEventListener instead
     */
    public void setListener(PipeEventListener eventListener) {
        setPipeEventListener(eventListener);
    }

    /**
     * Sets a Pipe event listener, set listener to null to unset the listener
     *
     * @param listener New value of property listener.
     *
     */
    public void setPipeEventListener(PipeEventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Returns the Pipe event listener for this pipe
     * @return PipeMsgListener
     *
     */
    public PipeEventListener getPipeEventListener() {
        return eventListener;
    }

    /**
     * Gets a message from the queue. If no Object is immediately available,
     * then wait the specified amount of time for a message to be inserted.
     *
     * @param timeout   Amount of time to wait in milliseconds for an object to
     * be available. Per Java convention, a timeout of zero (0) means wait an
     * infinite amount of time. Negative values mean do not wait at all.
     * @return The next message in the queue., if a listener is registered calls 
     * to this method will return null
     * @throws InterruptedException    if the operation is interrupted before
     * the timeout interval is completed.
     */
    public Message getMessage(int timeout) throws InterruptedException {
        if (queue == null || msgListener != null) {
            return null;
        } else {
            PipeMsgEvent ev = (PipeMsgEvent) queue.pop(timeout);
            if (ev != null) {
                return ev.getMessage();
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the Assigned PipeAdvertisement
     * @return the Assigned PipeAdvertisement
     */
    public PipeAdvertisement getPipeAdvertisement() {
        return pipeAdv;
    }

    /**
     *  {@inheritDoc}
     *
     *  <p/>Closes the JxtaBiDiPipe.
     */
    protected synchronized void finalize() throws Throwable {
        if(!closed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("JxtaBiDiPipe is being finalized without being previously closed. This is likely a users bug.");
            }
        }
        close();
        
        super.finalize();
    }
}

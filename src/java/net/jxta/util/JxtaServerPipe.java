/*
 *  $Id: JxtaServerPipe.java,v 1.1 2007/01/16 11:01:35 thomas Exp $
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
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.util.UnbiasedQueue;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *  JxtaServerPipe is a bi-directional Pipe, that behaves very much like
 *  ServerSocket, it creates a inputpipe and listens for pipe connection requests.
 *  JxtaServerPipe also defines it own protocol, requests arrive as a JXTA Message
 *  with the following elements :
 *
 * <p>
 *  &lt;Cred> to determine whether requestor has the proper access to be granted a connection  &lt;/Cred>
 *  <p>
 *  &lt;reqPipe> Requestor's pipe advertisement &lt;/reqPipe>
 *  <p>
 *  &lt;remPipe> remote pipe advertisement &lt;/remPipe>
 *  <p>
 *  &lt;reqPeer> remote peer advertisement &lt;/reqPeer>
 *  <p>
 *  &lt;reliable> Reliability setting ("true", or "false") &lt;/reliable>
 *  <p>
 *  &lt;data> data &lt;/data> 
 *  <p>
 *  JxtaServerPipe then creates a new private pipe, and listens for messages on that pipe
 *  resolves the Requestor's pipe, and sends <remPipe> private pipecreated </remotePipe>
 *  advertisement back, where the remove side resolves back.
 */

public class JxtaServerPipe implements PipeMsgListener {

    private static final Logger LOG = Logger.getLogger(JxtaServerPipe.class.getName());
    protected static final String  nameSpace = "JXTABIP";
    protected static final String    credTag = "Cred";
    protected static final String reqPipeTag = "reqPipe";
    protected static final String remPeerTag = "remPeer";
    protected static final String remPipeTag = "remPipe";
    protected static final String   closeTag = "close";
    protected static final String   reliableTag = "reliable";
    private PeerGroup group;
    private InputPipe serverPipe;
    private PipeAdvertisement pipeadv;
    private int backlog = 50;
    private int timeout = 60000;
    private final Object closeLock = new String("closeLock");
    private final UnbiasedQueue queue = UnbiasedQueue.synchronizedQueue(new UnbiasedQueue(backlog, false));
    private boolean bound = false;
    private boolean closed = false;
    protected StructuredDocument myCredentialDoc = null;


    /**
     * Default constructor for the JxtaServerPipe
     * <p>
     * backlog default of 50
     * <p>
     * timeout defaults to 60 seconds, i.e. blocking.
     * <p>
     * @param  group                JXTA PeerGroup
     * @param  pipeadv              PipeAdvertisement on which pipe requests are accepted
     * @exception  IOException  if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv) throws IOException {
        this(group, pipeadv, 50);
    }

    /**
     *Constructor for the JxtaServerPipe
     *
     * @param  group                JXTA PeerGroup
     * @param  pipeadv              PipeAdvertisement on which pipe requests are accepted
     * @param  backlog              the maximum length of the queue.
     * @param  timeout              the specified timeout, in milliseconds
     * @exception  IOException  if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv, int backlog, int timeout) throws IOException {
        this(group, pipeadv, backlog);
        this.timeout = timeout;
    }

    /**
     *Constructor for the JxtaServerPipe object
     *
     * @param  group                JXTA PeerGroup
     * @param  pipeadv              PipeAdvertisement on which pipe requests are accepted
     * @param  backlog              the maximum length of the queue.
     ** @exception  IOException  if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv, int backlog) throws IOException {
        this.group = group;
        this.pipeadv = pipeadv;
        this.backlog = backlog;
        queue.setMaxQueueSize(backlog);
        PipeService pipeSvc = group.getPipeService();
        serverPipe = pipeSvc.createInputPipe(pipeadv, this);
        setBound();
    }

    /**
     *  Binds the <code>JxtaServerPipe</code> to a specific pipe advertisement
     *
     * @param  group                JXTA PeerGroup
     * @param  pipeadv              PipeAdvertisement on which pipe requests are accepted
     * @exception  IOException  if an I/O error occurs
     */
    public void bind(PeerGroup group, PipeAdvertisement pipeadv) throws IOException {
        this.group = group;
        this.pipeadv = pipeadv;
        PipeService pipeSvc = group.getPipeService();
        serverPipe = pipeSvc.createInputPipe(pipeadv, this);
        setBound();
    }


    /**
     *  Binds the <code>JxtaServerPipe</code> to a specific pipe advertisement
     *
     * @param  group                JXTA PeerGroup
     * @param  pipeadv              PipeAdvertisement on which pipe requests are accepted
     * @param  backlog              the maximum length of the queue.
     * @exception  IOException  if an I/O error occurs
     */
    public void bind(PeerGroup group, PipeAdvertisement pipeadv, int backlog) throws IOException {
        this.backlog = backlog;
        bind(group, pipeadv);
        queue.setMaxQueueSize(backlog);
    }


    /**
     * Listens for a connection to be made to this socket and accepts 
            * it. The method blocks until a connection is made.
     * @return                  JxtaBiDiPipe
     * @exception  IOException  if an I/O error occurs
     */
    public JxtaBiDiPipe accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("JxtaServerPipe is closed");
        }
        if (!isBound()) {
            throw new SocketException("JxtaServerPipe is not bound yet");
        }
        try {
            while (true) {
                Message msg = (Message) queue.pop(timeout);

                if (msg == null) {
                    throw new SocketTimeoutException("Timeout reached");
                }

                JxtaBiDiPipe bidi = processMessage(msg);
                // make sure we have a socket returning
                if (bidi != null) {
                    return bidi;
                }

            }

        } catch (InterruptedException ie) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Interrupted", ie);
            }

            throw new SocketException("interrupted");
        }
    }


    /**
     *  Gets the group associated with this JxtaServerPipe
     *
     * @return    The group value
     */
    public PeerGroup getGroup() {
        return group;
    }


    /**
     *  Gets the PipeAdvertisement associated with this JxtaServerPipe
     *
     * @return    The pipeAdv value
     */
    public PipeAdvertisement getPipeAdv() {
        return pipeadv;
    }


    /**
     *  Closes this JxtaServerPipe (closes the underlying input pipe).
     *
     * @exception  IOException  if an I/O error occurs
     */
    public void close() throws IOException {
        synchronized (closeLock) {
            if (isClosed()) {
                return;
            }
            if (bound) {
                // close all the pipe
                serverPipe.close();
                queue.close();
                bound = false;
            }
            closed = true;
        }
    }

    /**
     *  Sets the bound attribute of the JxtaServerPipe
     */
    void setBound() {
        bound = true;
    }

    /**
     *  Gets the Timeout attribute of the JxtaServerPipe
     *
     * @return                  The soTimeout value
     * @exception  IOException  if an I/O error occurs
     */

    public synchronized int getPipeTimeout()
    throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        return timeout;
    }


    /**
     *  Sets the Timeout attribute of the JxtaServerPipe
     *  a timeout of 0 blocks forever, by default this JxtaServerPipe's
     *  timeout is set to 60000 ms
     *
     * @param  timeout              The new soTimeout value
     * @exception  IOException  if an I/O error occurs
     */
    public synchronized void setPipeTimeout(int timeout)
    throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        this.timeout = timeout;
    }

    /**
     * Returns the closed state of the JxtaServerPipe.
     *
     * @return    true if the socket has been closed
     */
    public boolean isClosed() {
        synchronized (closeLock) {
            return closed;
        }

    }

    /**
     * Returns the binding state of the JxtaServerPipe.
     *
     * @return    true if the ServerSocket successfully bound to an address
     */
    public boolean isBound() {
        return bound ;
    }

    /**
     *  when request messages arrive this method is called
     *
     * @param  event  the pipe message event
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        //deal with messages as they come in
        Message message = event.getMessage();

        if (message == null) {
            return;
        }

        boolean pushed = false;

        try {
            pushed = queue.push(message, -1);
        } catch (InterruptedException e) {
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Interrupted", e);
            }

        }

        if (!pushed && LOG.isEnabledFor(Level.WARN)) {
            LOG.warn("backlog queue full, connect request dropped");
        }
    }
    /**
     * Method processMessage is the heart of this class.
     * <p>
     * This takes new incoming connect messages and constructs the JxtaBiDiPipe
     * to talk to the new client.
     * <p>
     * The ResponseMessage is created and sent.
     * 
     * @param msg The client connection request (assumed not null)
     * @return JxtaBiDiPipe Which may be null if an error occurs.
     */

    private JxtaBiDiPipe processMessage(Message msg) {

        PipeAdvertisement outputPipeAdv =null;
        PeerAdvertisement  peerAdv=null;
        StructuredDocument credDoc=null;

        try {
            MessageElement el = msg.getMessageElement (nameSpace, credTag);

            if (el != null) {
                InputStream in = el.getStream();
                credDoc = (StructuredDocument)
                          StructuredDocumentFactory.newStructuredDocument(el.getMimeType(), in);
            }

            el = msg.getMessageElement (nameSpace, reqPipeTag);

            if (el != null) {
                InputStream in = el.getStream();
                outputPipeAdv = (PipeAdvertisement)
                                AdvertisementFactory.newAdvertisement(el.getMimeType(), in);
            }

            el = msg.getMessageElement (nameSpace, remPeerTag);

            if (el != null) {
                InputStream in = el.getStream();
                peerAdv = (PeerAdvertisement)
                          AdvertisementFactory.newAdvertisement(el.getMimeType(), in);
            }

            el = msg.getMessageElement (nameSpace, reliableTag);
            boolean isReliable = false;

            if (el != null) {
                isReliable = (Boolean.valueOf((el.toString()))).booleanValue();

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Connection request [isReliable] :"+isReliable );
                }

            }
            Messenger msgr = JxtaBiDiPipe.lightweightOutputPipe(group, outputPipeAdv,peerAdv);

            if (msgr != null) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Reliability set to :"+isReliable);
                }

                PipeAdvertisement newpipe = newInputPipe(group,
                                            outputPipeAdv);
                JxtaBiDiPipe pipe = new JxtaBiDiPipe(group,
                                                     msgr,
                                                     newpipe,
                                                     credDoc,
                                                     isReliable);
                pipe.setRemotePeerAdvertisement(peerAdv);
                pipe.setRemotePipeAdvertisement(outputPipeAdv);
                sendResponseMessage(group, msgr, newpipe);
                return pipe;
            }

        } catch (IOException e) {
            //deal with the error
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("IOException occured", e);
            }

        }
        return null;
    }
    /**
     * Method sendResponseMessage get the createResponseMessage and sends it.
     * 
     * @param group
     * @param msgr
     * @param pipeAd
     * @return Message The value of createResponseMessage that was sent, or null
     */
    protected void sendResponseMessage(PeerGroup group, Messenger msgr, PipeAdvertisement pipeAd) throws IOException {

        Message msg = new Message();
        PeerAdvertisement peerAdv = group.getPeerAdvertisement();

        if (myCredentialDoc == null) {
            myCredentialDoc = JxtaBiDiPipe.getCredDoc(group);
        }

        if (myCredentialDoc != null) {
            msg.addMessageElement(JxtaServerPipe.nameSpace,
                                  new InputStreamMessageElement(credTag, MimeMediaType.XMLUTF8, myCredentialDoc.getStream(), null));
        }

        msg.addMessageElement(JxtaServerPipe.nameSpace,
                              new TextDocumentMessageElement(remPipeTag, (XMLDocument) pipeAd.getDocument(MimeMediaType.XMLUTF8), null));

        msg.addMessageElement(nameSpace,
                              new TextDocumentMessageElement(remPeerTag, (XMLDocument) peerAdv.getDocument(MimeMediaType.XMLUTF8), null));
        msgr.sendMessage(msg);
    }

    /**
     * Utility method newInputPipe is used to get new pipe advertisement (w/random pipe ID) from old one.
     * <p>
     * Called by JxtaSocket to make pipe (name -> name.remote) for open message
     * <p>
     * Called by JxtaServerSocket to make pipe (name.remote -> name.remote.remote) for response message
     * 
     * @param group
     * @param pipeadv to get the basename and type from
     * @return PipeAdvertisement a new pipe advertisement
     */
    protected static PipeAdvertisement newInputPipe(PeerGroup group, PipeAdvertisement pipeadv) {
        PipeAdvertisement adv = null;
        adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType() );
        adv.setPipeID( IDFactory.newPipeID( (PeerGroupID) group.getPeerGroupID() ) );
        adv.setName(pipeadv.getName());
        adv.setType(pipeadv.getType());
        return adv;
    }
    /**
     *  get the credential doc
     *  @return Credential StructuredDocument 
     */
    public StructuredDocument getCredentialDoc() {
        return myCredentialDoc;
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
     *  {@inheritDoc}
     *
     *  <p/>Closes the JxtaServerPipe.
     */

    protected synchronized void finalize() throws Throwable {

        if(!closed) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("JxtaServerPipe is being finalized without being previously closed. This is likely a users bug.");
            }

        }
        close();
        
        super.finalize();

    }

}


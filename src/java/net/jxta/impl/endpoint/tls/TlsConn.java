/*
 *
 * $Id: TlsConn.java,v 1.1 2007/01/16 11:01:51 thomas Exp $
 *
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.impl.endpoint.tls;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.util.IgnoreFlushFilterOutputStream;

import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.util.TimeUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
/**
 * This class implements the TLS connection between two peers.
 *
 *
 * <p/>Properties:
 *
 * <p/>net.jxta.impl.endpoint.tls.TMFAlgorithm - if defined provides the name of
 * the trust manager factory alogrithm to use.
 */
class TlsConn {
    
    /**
     *  Log4J Logger
     **/
    private static final Logger LOG = Logger.getLogger(TlsConn.class.getName());
    
    static final int BOSIZE = 16000;
    
    /**
     *  TLS transport this connection is working for.
     **/
    final TlsTransport transport;
    
    /**
     *  The address of the peer to which we will be forwarding ciphertext
     *  messages.
     **/
    final EndpointAddress destAddr;
    
    /**
     *  Are we client or server?
     **/
    private boolean client;
    
    /**
     *  State of the connection
     **/
    private volatile HandshakeState currentState;
    
    /**
     *  Are we currently closing? To prevent recursion in {@link close()}
     **/
    private boolean closing = false;
    
    /**
     *  Time that something "good" last happened on the connection
     **/
    long lastAccessed;
    final String lastAccessedLock = new String("lastAccessedLock");
    final String closeLock = new String("closeLock");
    
    /**
     *  Number of retransmissions we have received.
     **/
    int retrans;
    
    /**
     *  Our synthetic socket which sends and receives the ciphertext.
     **/
    final TlsSocket tlsSocket;
    
    private final SSLContext context;
    
    /**
     * For interfacing with TLS
     **/
    private SSLSocket ssls;
    
    /**
     * We write our plaintext to this stream
     **/
    private OutputStream plaintext_out = null;
    
    /**
     *  Reads plaintext from the
     **/
    private PlaintextMessageReader readerThread = null;
    
    /**
     *  A string which we can lock on while acquiring new messengers. We don't
     *  want to lock the whole connection object.
     **/
    private String acquireMessengerLock = new String("Messenger Acquire Lock");
    
    /**
     *  Cached messenger for sending to {@link destAddr}
     **/
    private Messenger outBoundMessenger = null;
    
    /**
     *  Tracks the state of our TLS connection with a remote peer.
     **/
    static class HandshakeState {
        
        /**
         *  Handshake is ready to begin. We will be the client side.
         **/
        public static final HandshakeState CLIENTSTART = new HandshakeState() {
            public String toString() {
                return "CLIENTSTART";
            }
        };
        
        /**
         *  Handshake is ready to begin. We will be the server side.
         **/
        public static final HandshakeState SERVERSTART = new HandshakeState() {
            public String toString() {
                return "SERVERSTART";
            }
        };
        
        /**
         *  Handshake is in progress.
         **/
        public static final HandshakeState HANDSHAKESTARTED = new HandshakeState() {
            public String toString() {
                return "HANDSHAKESTARTED";
            }
        };
        
        /**
         *  Handshake failed to complete.
         **/
        public static final HandshakeState HANDSHAKEFAILED = new HandshakeState() {
            public String toString() {
                return "HANDSHAKEFAILED";
            }
        };
        
        /**
         *  Handshake completed successfully.
         **/
        public static final HandshakeState HANDSHAKEFINISHED = new HandshakeState() {
            public String toString() {
                return "HANDSHAKEFINISHED";
            }
        };
        
        /**
         *  Connection is closing.
         **/
        public static final HandshakeState CONNECTIONCLOSING = new HandshakeState() {
            public String toString() {
                return "CONNECTIONCLOSING";
            }
        };
        
        /**
         *  Connection has died.
         **/
        public static final HandshakeState CONNECTIONDEAD = new HandshakeState() {
            public String toString() {
                return "CONNECTIONDEAD";
            }
        };
        
        /**
         *  Private Constructor. This class is only constants.
         **/
        private HandshakeState() {}
    }
    
    /**
     *  Create a new connection
     **/
    TlsConn(TlsTransport tp, EndpointAddress destAddr, boolean client) throws Exception {
        this.transport = tp;
        this.destAddr = destAddr;
        this.client = client;
        this.currentState = client ? HandshakeState.CLIENTSTART : HandshakeState.SERVERSTART;
        this.lastAccessed = TimeUtils.timeNow();
        
        if (LOG.isEnabledFor(Level.INFO)) {
            LOG.info((client ? "Initiating" : "Accepting") + " new connection for : " + destAddr.getProtocolAddress());
        }
        
        boolean choseTMF = false;
        javax.net.ssl.TrustManagerFactory tmf = null;
        String overrideTMF = System.getProperty( "net.jxta.impl.endpoint.tls.TMFAlgorithm" );
        
        if( (!choseTMF) && (null != overrideTMF) ) {
            tmf = javax.net.ssl.TrustManagerFactory.getInstance( overrideTMF );
            choseTMF = true;
        }
        
        Collection providers = Arrays.asList( Security.getProviders() );
        
        Set providerNames = new HashSet();
        
        Iterator eachProvider = providers.iterator();
        while( eachProvider.hasNext() ) {
            providerNames.add( ((Provider) eachProvider.next()).getName() );
        }
        
        if( (!choseTMF) && providerNames.contains( "SunJSSE" ) ) {
            tmf = javax.net.ssl.TrustManagerFactory.getInstance( "SunX509", "SunJSSE" );
            choseTMF = true;
        }
        
        if( (!choseTMF) && providerNames.contains( "IBMJSSE" ) ) {
            tmf = javax.net.ssl.TrustManagerFactory.getInstance( "IbmX509", "IBMJSSE" );
            choseTMF = true;
        }
        
        // XXX 20040830 bondolo Other solutions go here!
        
        if( !choseTMF ) {
            tmf = javax.net.ssl.TrustManagerFactory.getInstance( javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm() );
            LOG.warn( "Using defeualt Trust Manager Factory algorithm. This may not work as expected." );
        }
        
        KeyStore trusted = transport.membership.getPSEConfig().getKeyStore();
        tmf.init( trusted );
        
        javax.net.ssl.TrustManager tms[] = tmf.getTrustManagers();
        
        javax.net.ssl.KeyManager kms[] = new javax.net.ssl.KeyManager[] { new PSECredentialKeyManager( transport.credential, trusted ) };
        
        context = SSLContext.getInstance( "TLS" );
        context.init( kms, tms, null );
        
        javax.net.ssl.SSLSocketFactory factory = context.getSocketFactory();
        
        // endpoint interface
        TlsSocket newConnect = new TlsSocket(new JTlsInputStream(this, tp.MIN_IDLE_RECONNECT), new JTlsOutputStream(transport, this));
        
        // open SSL socket and do the handshake
        ssls = (SSLSocket) factory.createSocket( newConnect, destAddr.getProtocolAddress(), JTlsDefs.FAKEPORT, true );
        ssls.setEnabledProtocols( new String[] {"TLSv1"} );
        ssls.setUseClientMode( client );
        if( !client ) {
            ssls.setNeedClientAuth( true );
        }
        
        // We have to delay initialization of this until we have set the
        // handshake mode.
        tlsSocket = newConnect;
    }
    
    /**
     *  @inheritDoc
     *
     *  <p/>An implementation which is useful for debugging.
     **/
    public String toString() {
        return super.toString() + "/" + getHandshakeState() + ":" + (client ? "Client" : "Server") + " for " + destAddr;
    }
    
    /**
     *  Returns the current state of the connection
     *
     *  @return the current state of the connection.
     **/
    HandshakeState getHandshakeState() {
        return currentState;
    }
    
    /**
     *  Changes the state of the connection. Calls
     *  {@link java.lang.Object#notifyAll()} to wake any threads waiting on
     *  connection state changes.
     *
     *  @param newstate the new connection state.
     *  @return the previous state of the connection.
     **/
    synchronized HandshakeState setHandshakeState(HandshakeState newstate) {
        
        HandshakeState oldstate = currentState;
        currentState = newstate;
        notifyAll();
        return oldstate;
    }
    
    /**
     * Open the connection with the remote peer.
     **/
    void finishHandshake() throws IOException {
        
        long startTime = 0;
        
        if (LOG.isEnabledFor(Level.INFO)) {
            startTime = TimeUtils.timeNow();
            LOG.info((client ? "Client:" : "Server:") + " Handshake START");
        }
        
        setHandshakeState(HandshakeState.HANDSHAKESTARTED);
        
        // this starts a handshake
        SSLSession newSession = ssls.getSession();
        if("SSL_NULL_WITH_NULL_NULL".equals( newSession.getCipherSuite())) {
            setHandshakeState(HandshakeState.HANDSHAKEFAILED);
            throw new IOException("Handshake failed");
        }
        
        setHandshakeState(HandshakeState.HANDSHAKEFINISHED);
        
        if (LOG.isEnabledFor(Level.INFO)) {
            long hsTime = TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), startTime) / TimeUtils.ASECOND;
            LOG.info((client ? "Client:" : "Server:") + "Handshake DONE in " + hsTime + " secs");
        }
        
        // set up plain text i/o
        // writes to be encrypted
        plaintext_out = new BufferedOutputStream(ssls.getOutputStream(), BOSIZE);
        
        // Start reader thread
        readerThread = new PlaintextMessageReader(ssls.getInputStream());
    }
    
    /**
     *  Close this connection.
     *
     *  @param finalstate state that the connection will be in after close.
     **/
    void close(HandshakeState finalstate) throws IOException {
        synchronized(lastAccessedLock) {
            lastAccessed = Long.MIN_VALUE;
        }
        synchronized(closeLock) {
            closing = true;
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Shutting down " + this);
            }
            
            setHandshakeState(HandshakeState.CONNECTIONCLOSING);
            
            try {
                if (null != tlsSocket) {
                    try {
                        tlsSocket.close();
                    } catch (IOException ignored) {
                        ;
                    }
                }
                
                if (null != ssls) {
                    try {
                        ssls.close();
                    } catch (IOException ignored) {
                        ;
                    }
                    ssls = null;
                }
                
                if (null != outBoundMessenger) {
                    outBoundMessenger.close();
                    outBoundMessenger = null;
                }
            } catch (Throwable failed) {
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Throwable during close " + this, failed);
                }
                
                IOException failure = new IOException("Throwable during close()");
                failure.initCause(failed);
            } finally {
                closeLock.notifyAll();
                closing = false;
                setHandshakeState(finalstate);
            }
        }
    }
    
    /**
     * Used by the TlsManager and the TlsConn in order to send a message,
     * either a TLS connection establishement, or TLS fragments to the remote TLS.
     *
     *  @param message message to send to the remote TLS peer.
     *  @return if true then message was sent, otherwise false.
     *  @throws IOException if there was a problem sending the message.
     **/
    boolean sendToRemoteTls(Message msg) throws IOException {
        
        synchronized (acquireMessengerLock) {
            if ((null == outBoundMessenger) || outBoundMessenger.isClosed()) {
                
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Getting messenger for " + destAddr);
                }
                
                EndpointAddress realAddr = new EndpointAddress(destAddr, JTlsDefs.ServiceName, null);
                
                // Get a messenger.
                outBoundMessenger = transport.endpoint.getMessenger(realAddr);
                
                if (outBoundMessenger == null) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("Could not get messenger for " + realAddr);
                    }
                    return false;
                }
            }
        }
        
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending " + msg + " to endpoint " + destAddr);
        }
        
        // Good we have a messenger. Send the message.
        return outBoundMessenger.sendMessage(msg);
    }
    
    /**
     * sendMessage is called by the TlsMessenger each time a service or
     * an application sends a new message over a TLS connection.
     * IOException is thrown when something goes wrong.
     *
     * <p/>The message is encrypted by TLS ultimately calling
     * JTlsOutputStream.write(byte[], int, int); with the resulting TLS
     * Record(s).
     *
     *  @param msg The plaintext message to be sent via this connection.
     *  @throws IOException for errors in sending the message.
     **/
    void sendMessage(Message msg) throws IOException {
        
        try {
            WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, JTlsDefs.MTYPE, (MimeMediaType[]) null);
            
            serialed.sendToStream(new IgnoreFlushFilterOutputStream(plaintext_out));
            
            plaintext_out.flush();
        } catch (IOException failed) {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Closing " + this + " due to exception ", failed);
            }
            
            close(HandshakeState.CONNECTIONDEAD);
            throw failed;
        }
    }
    
    /**
     * This is our message reader thread. This reads from the plaintext input
     * stream and dispatches messages received to the endpoint.
     **/
    private class PlaintextMessageReader implements Runnable {
        
        InputStream ptin = null;
        Thread workerThread = null;
        
        public PlaintextMessageReader( InputStream ptin ) {
            this.ptin = ptin;
            
            // start our thread
            workerThread = new Thread( TlsConn.this.transport.myThreadGroup, this, "JXTA TLS Plaintext Reader for " + TlsConn.this.destAddr );
            workerThread.setDaemon(true);
            workerThread.start();
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Started ReadPlaintextMessage thread for " + TlsConn.this.destAddr);
            }
        }
        
        /**
         *  @inheritDoc
         **/
        public void run() {
            try {
                while (true) {
                    try {
                        Message msg = WireFormatMessageFactory.fromWire(ptin, JTlsDefs.MTYPE, null );
                        
                        if( null == msg ) {
                            break;
                        }
                        
                        // dispatch it to TlsTransport for demuxing
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Dispatching " + msg + " to TlsTransport");
                        }
                        
                        TlsConn.this.transport.processReceivedMessage(msg);
                        
                        synchronized (TlsConn.this.lastAccessedLock) {
                            TlsConn.this.lastAccessed = TimeUtils.timeNow(); // update idle timer
                        }
                    }
                    catch (IOException iox) {
                        if (LOG.isEnabledFor(Level.WARN)) {
                            LOG.warn("I/O error while reading decrypted Message", iox);
                        }
                        
                        break;
                    }
                }
            } catch (Throwable all) {
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            } finally {
                workerThread = null;
            }
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Finishing ReadPlaintextMessage thread");
            }
        }
    }
    
    
    /**
     *  A private key manager which selects based on the key and cert chain found
     *  in a PSE Credential.
     *
     *  <p/>TODO Promote this class to a full featured interface for all of the
     *  active PSECredentials. Currently the alias "theone" is used to refer to
     *  the 
     **/
    private static class PSECredentialKeyManager implements javax.net.ssl.X509KeyManager {
        PSECredential cred;
        KeyStore trusted;
        
        public PSECredentialKeyManager( PSECredential useCred, KeyStore trusted ) {
            this.cred = useCred;
            this.trusted = trusted;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            Iterator eachKeyType = Arrays.asList( keyType ).iterator();
            
            while( eachKeyType.hasNext() ) {
                String aKeyType = (String) eachKeyType.next();
                String result = chooseServerAlias( aKeyType, issuers, socket );
                
                if( null != result ) {
                    return result;
                }
            }
            
            return null;
        }
        
        /**
         *  {@inheritDoc}
         **/
        public String chooseServerAlias(String keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            String [] available = getServerAliases( keyType, issuers );
            
            if( null != available ) {
                return available[0];
            } else {
                return null;
            }
        }
        
        /**
         *  {@inheritDoc}
         **/
        public X509Certificate[] getCertificateChain(String alias) {
            if( alias.equals( "theone" ) ) {
                return cred.getCertificateChain();
            } else {
                try {
                    return (X509Certificate[]) trusted.getCertificateChain( alias );
                } catch( KeyStoreException ignored ) {
                    return null;
                }
            }
        }
        
        /**
         *  {@inheritDoc}
         **/
        public String[] getClientAliases(String keyType, java.security.Principal[] issuers) {
            List clientAliases = new ArrayList();
            
            try {
                Enumeration eachAlias = trusted.aliases();
                
                Collection allIssuers = null;
                
                if( null != issuers ) {
                    allIssuers = Arrays.asList( issuers );
                }
                
                while( eachAlias.hasMoreElements() ) {
                    String anAlias = (String) eachAlias.nextElement();
                    if( trusted.isCertificateEntry( anAlias ) ) {
                        try {
                            X509Certificate aCert = (X509Certificate) trusted.getCertificate( anAlias );
                            
                            if( null == aCert ) {
                                // strange... it should have been there...
                                continue;
                            }
                            
                            if( !aCert.getPublicKey().getAlgorithm().equals( keyType ) ) {
                                continue;
                            }
                            
                            if( null != allIssuers ) {
                                if( allIssuers.contains(aCert.getIssuerDN()) ) {
                                    clientAliases.add( anAlias );
                                }
                            } else {
                                clientAliases.add( anAlias );
                            }
                        } catch( KeyStoreException ignored ) {
                            ;
                        }
                    }
                }
            } catch( KeyStoreException ignored ) {
                ;
            }
            
            return (String[]) clientAliases.toArray(new String[clientAliases.size()]);
        }
        
        /**
         *  {@inheritDoc}
         **/
        public java.security.PrivateKey getPrivateKey(String alias) {
            if( alias.equals( "theone" ) ) {
                return cred.getPrivateKey();
            } else {
                return null;
            }
        }
        
        /**
         *  {@inheritDoc}
         **/
        public String[] getServerAliases(String keyType, java.security.Principal[] issuers) {
            if( keyType.equals( cred.getCertificate().getPublicKey().getAlgorithm() ) ) {
                if( null == issuers ) {
                    return new String[] { "theone" };
                } else {
                    Collection allIssuers = Arrays.asList( issuers );
                    
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug( "Looking for : " + cred.getCertificate().getIssuerX500Principal() );
                        LOG.debug( "Issuers : " + allIssuers );
                        java.security.Principal prin = cred.getCertificate().getIssuerX500Principal();
                        LOG.debug( "  Principal Type :" + prin.getClass().getName());
                        Iterator it = allIssuers.iterator();
                        while (it.hasNext()) {
                            java.security.Principal tmp = (java.security.Principal) it.next();
                            LOG.debug( "Issuer Type : " + tmp.getClass().getName());
                            LOG.debug( "Issuer value : " + tmp);
                            LOG.debug( "tmp.equals(prin) : " + tmp.equals(prin));
                        }
                    }
                    
                    if( allIssuers.contains( cred.getCertificate().getIssuerX500Principal() ) ) {
                        return new String[] { "theone" };
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
    }
}

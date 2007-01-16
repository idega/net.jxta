/*
 *
 * $Id: IncomingUnicastServer.java,v 1.1 2007/01/16 11:01:51 thomas Exp $
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

package net.jxta.impl.endpoint.tcp;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;

import java.io.InterruptedIOException;
import java.net.BindException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.endpoint.MessengerEvent;
import net.jxta.impl.endpoint.IPUtils;


/**
 *  This server handles incoming unicast TCP connections
 **/
public class IncomingUnicastServer implements Runnable {
    
    /**
     *  Log4j Logger
     **/
    private static final Logger LOG = Logger.getLogger(IncomingUnicastServer.class.getName());
    
    /**
     *  The transport which owns this server.
     **/
    private final TcpTransport owner;
    
    /**
     *  The interface address the serverSocket will try to bind to.
     **/
    private final InetAddress serverBindLocalInterface;
    
    /**
     *  The beginnning of the port range the serverSocket will try to bind to.
     **/
    private final int serverBindStartLocalPort;
    
    /**
     *  The port the serverSocket will try to bind to.
     **/
    private int serverBindPreferedLocalPort;
    
    /**
     *  The end of the port range the serverSocket will try to bind to.
     **/
    private final int serverBindEndLocalPort;
    
    /**
     *  The socket we listen for connections on.
     **/
    private ServerSocket serverSocket;
    
    /**
     *  If true then the we are closed or closing.
     **/
    private volatile boolean closed = false;
    
    /**
     *  The thread on which connection accepts will take place.
     **/
    private Thread acceptThread = null;
    
    /**
     *  Constructor for the TCP server
     *
     *  @param owner            the TCP transport we are working for
     *  @param serverInterface  the network interface to use.
     *  @param preferedPort       the port we will be listening on.
     **/
    public IncomingUnicastServer(TcpTransport owner, InetAddress serverInterface, int preferedPort, int startPort, int endPort) throws IOException, SecurityException {
        this.owner = owner;
        serverBindLocalInterface = serverInterface;
        serverBindPreferedLocalPort = preferedPort;
        serverBindStartLocalPort = startPort;
        serverBindEndLocalPort = endPort;
        openServerSocket();
    }
    
    /**
     *  Start this server.
     *
     *  @param inGroup  the thread group we should create our threads in.
     */
    public synchronized boolean start(ThreadGroup inGroup) {
        
        if (acceptThread != null) {
            return false;
        }
        
        // Start daemon thread
        acceptThread = new Thread(inGroup, this, "TCP Unicast Server Connection Listener");
        acceptThread.setDaemon(true);
        acceptThread.start();
        
        return true;
    }
    
    /**
     *  Stop this server.
     */
    public synchronized void stop() {
        closed = true;
        
        Thread temp = acceptThread;
        
        if (null != temp) {
            temp.interrupt();
        }
        
        // interrupt does not seem to have an effect on threads blocked in accept.
        // Closing the socket works though.
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
    
    /**
     *  Get the address of the network interface being used.
     */
    synchronized InetSocketAddress getLocalSocketAddress() {
        ServerSocket localSocket = serverSocket;
        
        if (null != localSocket) {
            return (InetSocketAddress) localSocket.getLocalSocketAddress();
        } else {
            return null;
        }
    }
    
    /**
     *  Get the start port range we are using
     */
    int getStartPort() {
        return serverBindStartLocalPort;
    }
    
    /**
     *  Get the end port range we are using
     */
    int getEndPort() {
        return serverBindEndLocalPort;
    }
    
    /**
     *  Daemon where we wait for incoming connections.
     */
    public void run() {
        
        try {
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Server is ready to accept connections");
            }
            
            while (!closed) {
                Socket inputSocket;

                try {
                    if ((null == serverSocket) || serverSocket.isClosed()) {
                        openServerSocket();
                        
                        if (null == serverSocket) {
                            break;
                        }
                    }
                    
                    inputSocket = serverSocket.accept();
                    
                    if (closed) {
                        break;
                    }
                } catch (InterruptedIOException woken) {
                    continue;
                } catch (IOException e1) {
                    if (closed) {
                        break;
                    }
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("[1] ServerSocket.accept() failed on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort(), e1);
                    }
                    continue;
                } catch (SecurityException e2) {
                    if (closed) {
                        break;
                    }
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("[2] ServerSocket.accept() failed on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort(), e2);
                    }
                    continue;
                }
                
                try {
                    // make a connection object
                    TcpConnection newConnect = new TcpConnection(inputSocket, owner);
                    
                    // if its not DOA, then register it.
                    if (newConnect.isConnected()) {
                        TcpMessenger newMessenger = new TcpMessenger(newConnect.getDestinationAddress(), newConnect, owner);
                        
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("Registering connection from " + inputSocket.getInetAddress().getHostAddress() + ":" + inputSocket.getPort());
                        }
                        
                        try {
                            MessengerEvent event = new MessengerEvent(owner, newMessenger, newConnect.getConnectionAddress());

                            owner.messengerReadyEvent(newMessenger, newConnect.getConnectionAddress());
                        } catch (Throwable all) {
                            if (LOG.isEnabledFor(Level.FATAL)) {
                                LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                            }
                        }
                        newMessenger.start();
                    }
                } catch (OutOfMemoryError oom) {
                    // Make sure the socket is closed. Since we failed to
                    // put a TcpConnection wrapper around it, it belongs
                    // to us.
                    try {
                        // Let that client hang a bit; there's nothing
                        // we can do right now.
                        Thread.sleep(2000);
                        
                        inputSocket.close();
                    } catch (Throwable any) {// There's nothing we can do and to die is certainly
                        // not an option for this thread.
                        // Avoid the log, we may still be oom.
                    }
                } catch (Throwable all) {
                    // Make sure the socket is closed. Since we failed to
                    // put a TcpConnection wrapper around it, it belongs
                    // to us.
                    try {
                        inputSocket.close();
                    } catch (Throwable any) {
                        // There's nothing we can do and to die is certainly
                        // not an option for this thread.
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("Failed to close dead socket", any);
                        }
                    }
                    
                    if (LOG.isEnabledFor(Level.WARN)) {
                        LOG.warn("Failed to create connection", all);
                    }
                }
            }
        } catch (Throwable all) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        } finally {
            synchronized (this) {
                closed = true;
                
                ServerSocket temp = serverSocket;

                serverSocket = null;
                
                if (null != temp) {
                    try {
                        temp.close();
                    } catch (IOException ignored) {
                        ;
                    }
                }
                
                acceptThread = null;
            }
            
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("Server has been shut down.");
            }
        }
    }
    
    private synchronized void openServerSocket() throws IOException, SecurityException {
        serverSocket = null;
        
        while (true) {
            try {
                synchronized (this) {
                    if (-1 != serverBindPreferedLocalPort) {
                        serverSocket = new ServerSocket(serverBindPreferedLocalPort, TcpTransport.MaxAcceptCnxBacklog, serverBindLocalInterface);
                    } else {
                        serverSocket = IPUtils.openServerSocketInRange(serverBindStartLocalPort, serverBindEndLocalPort,
                                TcpTransport.MaxAcceptCnxBacklog, serverBindLocalInterface);
                    }
                }
                
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Server will accept connections at " + serverSocket.getLocalSocketAddress());
                }
                
                return;
            } catch (BindException e0) {
                if (-1 != serverBindStartLocalPort) {
                    serverBindPreferedLocalPort = (0 == serverBindStartLocalPort) ? 0 : -1;
                    continue;
                }
                
                closed = true;
                if (LOG.isEnabledFor(Level.FATAL)) {
                    LOG.fatal("Cannot bind ServerSocket on " + serverBindLocalInterface + ":" + serverBindPreferedLocalPort, e0);
                }
                return;
            }
        }
    }
}

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
 *  $Id: Dispatcher.java,v 1.1 2007/01/16 11:01:26 thomas Exp $
 */
package net.jxta.ext.http;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;

/**
 * HTTP connection manager.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class Dispatcher {

    private static final int SLEEP = 50;
    private static final long MAX_WAIT = 20 * 1000;
    
    private Dispatchable dispatchable = null;
    private long maxWait = MAX_WAIT;

    /**
     * {@link net.jxta.ext.http.Dispatchable} factory.
     *
     * @param       u       {@link java.net.URL} destination address
     * @param       msg     outbound {@link net.jxta.ext.http.Message}
     * @return      {@link net.jxta.ext.http.Dispatchable} implementation
     */
    
    public static Dispatchable getDispatchable(URL u, Message msg) {
        Dispatchable d = null;

        if (msg != null &&
            msg.getBody() != null) {
            d = new HttpPost(u, msg);
        } else {
            d = new HttpGet(u, msg);
        }

        return d;
    }
    
    /**
     * Constructor which specifies the destination {@link java.net.URL} address.
     *
     * @param       u       {@link java.net.URL} destination address
     */
    
    public Dispatcher(URL u) {
        this(u, null);
    }
    
    /**
     * Constructor which specifies the destination {@link java.net.URL} address
     * and a maximum connection wait time.
     *
     * @param       u       {@link java.net.URL} destination address
     * @param       maxWait maximum connection wait time
     */
    
    public Dispatcher(URL u, long maxWait) {
        this(u, null, maxWait);
    }

    /**
     * Constructor which specifies the destination {@link java.net.URL} address
     * and the outbound {@link net.jxta.ext.http.Message}.
     *
     * @param       u       {@link java.net.URL} destination address
     * @param       msg     outbound {@link net.jxta.ext.http.Message}
     */
    
    public Dispatcher(URL u, Message msg) {
        this(u, msg, MAX_WAIT);
    }
    
    /**
     * Constructor which specifies the destination {@link java.net.URL} address,
     * outbound {@link net.jxta.ext.http.Message} and maximum connection wait time.
     *
     * @param       u       {@link java.net.URL} destination address
     * @param       msg     outbound {@link net.jxta.ext.http.Message}
     * @param       maxWait maximum connection wait time
     */
    
    public Dispatcher(URL u, Message msg, long maxWait) {
        this.dispatchable = getDispatchable(u, msg);
        this.maxWait = maxWait;
    }
    
    /**
     * Accessor for the {@link net.jxta.ext.http.Dispatchable} handler.
     *
     * @return          {@link net.jxta.ext.http.Dispatchable} handler
     */
    
    public Dispatchable getDispatchable() {
        return this.dispatchable;
    }

    /**
     * Accessor for the maximum connection wait time.
     *
     * @return          maximum connection wait time
     */
    
    public long getMaxWait() {
        return this.maxWait;
    }

    /**
     * Initiate the HTTP {@link net.jxta.ext.http.Message} dispatch.
     *
     * @return          {@link net.jxta.ext.http.Message} response
     */
    
    public Message dispatch()
    throws IOException {
        Dispatch dispatch = new Dispatch(this.dispatchable);
        Thread t = new Thread(dispatch, Dispatcher.class.getName());
        
        t.setDaemon(true);
        t.start();

        Timer timer = null;

        if (getMaxWait() > 0) {
            timer = new Timer();

            timer.schedule(new DispatchTimerTask(dispatch), getMaxWait());
        }

        while (! dispatch.isDone()) {
            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException ie) {
            }
        }

        Message response = dispatch.getResponse();

        this.dispatchable.disconnect();

        if (timer != null) {
            timer.cancel();
        }

        return response;
    }
}

/**
 * {@link java.util.TimerTask} to manage the {@link net.jxta.ext.http.Dispatchable} handler.
 *
 * @author  james todd [gonzo at jxta dot org]
 */

class DispatchTimerTask
    extends TimerTask {

    private Dispatch dispatcher = null;

    /**
     * Constructor which specifies the handled {@link net.jxta.ext.http.Dispatch} {@link java.lang.Thread}.
     *
     * @param       dispatcher      {@link net.jxta.ext.http.Dispatch} handler
     */
    
    public DispatchTimerTask(Dispatch dispatcher) {
        this.dispatcher = dispatcher;
    }
    
    /**
     * Start timer.
     */

    public void run() {
        if (! this.dispatcher.isDone()) {
            this.dispatcher.interrupt();
        }
    }
}

/**
 * HTTP connection {@link java.lang.Thread}.
 *
 * @author  james todd [gonzo at jxta dot org]
 */

class Dispatch
    implements Runnable {

    private static int SLEEP = 50;
    private Dispatchable dispatchable = null;
    private Message response = null;
    private boolean isDone = false;
    private boolean interrupted = false;

    /**
     * Constructor which specifies the managed {@link net.jxta.ext.http.Dispatchable}.
     *
     * @param       dispatchable        managed {@link net.jxta.ext.http.Dispatchable}
     */
    
    public Dispatch(Dispatchable dispatchable) {
        this.dispatchable = dispatchable;
    }

    /**
     * Accessor to the HTTP response {@link net.jxta.ext.http.Message}.
     *
     * @return          response {@link net.jxta.ext.http.Message}
     */
    
    public Message getResponse() {
        return this.response;
    }

    /**
     * Accessor to the run state.
     *
     * @return          run state
     */
    
    public boolean isDone() {
        return this.isDone;
    }

    /**
     * Interrupt the HTTP connection {@link java.lang.Thread}.
     */
    
    public void interrupt() {
        setInterrupted(true);
    }

    /**
     * Start the HTTP connection {@link java.lang.Thread}.
     */
    
    public void run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    response = dispatchable.dispatch();
                } catch (IOException ioe) {}

                isDone = true;
            }
        }, Dispatch.class.getName());

        t.setDaemon(true);
        t.start();

        while (! this.isDone &&
               ! isInterrupted()) {
            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException ie) {
                setInterrupted(true);
            }
        }

        if (isInterrupted()) {
            this.dispatchable.disconnect();
            t.interrupt();
        }

        this.isDone = true;
    }

    private boolean isInterrupted() {
        return (this.interrupted = this.interrupted ?
                                   true : Thread.interrupted());
    }

    private void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}

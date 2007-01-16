/*
 *
 *  Copyright (c) 2003 Sun Microsystems, Inc.  All rights reserved.
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
 *  $Id: OutgoingMsgrAdaptor.java,v 1.1 2007/01/16 11:01:30 thomas Exp $
 */
package net.jxta.impl.util.pipe.reliable;

import java.io.IOException;
import org.apache.log4j.Logger;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.Message;
import net.jxta.impl.util.TimeUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *  OutgoingMessengerAdaptor
 */
public class OutgoingMsgrAdaptor implements Outgoing {

    private final static Logger LOG =
        Logger.getLogger(OutgoingMsgrAdaptor.class.getName());

    private Messenger msgr;
    private int timeout;
    private long lastAccessed = 0;
    private boolean closed = false;

    /**
     *  Constructor for the OutgoingMsgrAdaptor object
     *
     *@param  msgr     the messenger used to send messages
     *@param  timeout  timeout in milliseconds
     */
    public OutgoingMsgrAdaptor(Messenger msgr, int timeout) {
        if (msgr == null) {
            throw new IllegalArgumentException("messenger cannot be null");
        }
        this.msgr = msgr;
        this.timeout = timeout;

        // initialize to some reasonable value
        lastAccessed = TimeUtils.timeNow();
    }

    /**
     *  Sets the Timeout attribute. A timeout of 0 blocks forever
     *
     * @param  timeout The new soTimeout value
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     *  close the messenger (does not close the messenger)
     *
     *@exception  IOException  Description of the Exception
     */
    public void close() throws IOException {
        //noop, lower layers should not be closing the messenger
        //it is upto the consumer to make that determination

        // Not quite: reliable stream depends on this for its termination
        closed = true;
    }

    /**
     *  Gets the minIdleReconnectTime of the OutgoingMsgrAdaptor
     *  (obsolete).
     *@return    The minIdleReconnectTime value
     */
    public long getMinIdleReconnectTime() {
        return timeout;
    }

    /**
     *  Gets the idleTimeout of the OutgoingMsgrAdaptor
     * NEVER.
     *@return    The idleTimeout value
     */
    public long getIdleTimeout() {
        return Long.MAX_VALUE;
    }

    /**
     *  Gets the maxRetryAge attribute of the OutgoingMsgrAdaptor
     *
     *@return    The maxRetryAge value
     */
    public long getMaxRetryAge() {
        return timeout == 0 ? Long.MAX_VALUE : timeout;
    }

    /**
     *  Gets the lastAccessed time of OutgoingMsgrAdaptor
     *
     *@return    The lastAccessed in milliseconds
     */
    public long getLastAccessed() {
        return lastAccessed;
    }

    /**
     *  Sets the lastAccessed of OutgoingMsgrAdaptor
     *
     *@param  time  The new lastAccessed in milliseconds
     */
    public void setLastAccessed(long time) {
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Setting lastAccessed to :"+lastAccessed);
        }
        lastAccessed = time;
    }

    /**
     *  returns last accessed time as a string
     *
     *@return    last accessed time as a string
     */
    public String toString() {
        return " lastAccessed=" + Long.toString(lastAccessed);
    }

    /**
     *  Sends a message
     *
     *@param  msg              message to send
     *@return                  true if message send is successfull
     *@exception  IOException  if an io error occurs
     */
    public boolean send(Message msg) throws IOException {
        if (closed) {
            throw new IOException("broken connection");
        }

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Sending a Message");
        }
        msgr.sendMessageB(msg, null, null);
        return true;
    }
}


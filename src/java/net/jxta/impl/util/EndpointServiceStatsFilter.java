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
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
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
 *  $Id: EndpointServiceStatsFilter.java,v 1.1 2007/01/16 11:01:24 thomas Exp $
 */

package net.jxta.impl.util;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointFilterListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Hashtable;

/**
 * Instances of this clas can be registered with an EndpointService
 * to gather statistics about what kind of messages pass through it.
 *
 * This class is not MT-safe, so make sure you plug it only
 * into one endpoint service.
 *
 * @see net.jxta.endpoint.EndpointService#addFilterListener
 */

public class EndpointServiceStatsFilter implements EndpointFilterListener {

    long      lastMessageTime;
    Hashtable channelTrafficTable;
    Hashtable sourceCountTable;
    Hashtable destCountTable;

    public EndpointServiceStatsFilter () {

	channelTrafficTable = new Hashtable ();
	sourceCountTable  = new Hashtable ();
	destCountTable    = new Hashtable ();
    }

    /**
     * This method is called by the EndpointService to give us a chance
     * to look at the message before it is dispatched to any listeners.
     */
    public Message processIncomingMessage (Message msg,
					   EndpointAddress source,
					   EndpointAddress dest)
    {
	Message.ElementIterator e = msg.getMessageElements ();
	MessageElement            el;
	String                    namespace;
	String                    name;
	int                       colon;

	while (e.hasNext ()) {
	    el = (MessageElement) e.next ();
	    
		namespace = e.getNamespace();
	    name = el.getElementName ();

	    incrementCount (channelTrafficTable, 
			    source.getProtocolName () + "://" +
			    source.getProtocolAddress () + "/" +
			    namespace,
			    (int) el.getByteLength ());
	    incrementCount (channelTrafficTable, 
			    source.getProtocolName () + "://" +
			    source.getProtocolAddress () + "/" +
			    name,
			    (int) el.getByteLength ());
	}

	if (source != null)
	    incrementCount (sourceCountTable, source, 1);
	
	if (dest != null)
	    incrementCount (destCountTable, dest, 1);

	lastMessageTime = System.currentTimeMillis ();

	return msg;
    }

    
    /**
     * Get the time we last saw a message.
     *
     * @return time last message was received, in milliseconds, 
     * since Jan. 1, 1970.
     */
    
    public long getLastMessageTime () {
	return lastMessageTime;
    }

    /**
     * Get the number of messages seen with a given message element
     * namespace or full message element name.  (Both are referred
     * to as "channel" here because filters and listeners are
     * dispatched by the EndpointService based on message element
     * namespaces or fully name.)
     */
    public long getTrafficOnChannel (String channel) {
	
	return getCount (channelTrafficTable, channel);
    }

    public Enumeration getChannelNames () {
	return channelTrafficTable.keys ();
    }

    /**
     * Get the number of messages received from a given address.
     */
    public long getMessageCountFrom (EndpointAddress addr) {
	return getCount (sourceCountTable, addr);
    }

    /**
     * Get the number of messages we've seen that were adderssed
     * to a given address.
     */
    public long getMessageCountTo (EndpointAddress addr) {
	return getCount (destCountTable, addr);
    }


    private long getCount (Hashtable table, Object key) {
	Counter counter = (Counter) table.get (key);

	return counter == null ? -1 : counter.value;
    }

    private void incrementCount (Hashtable table, Object key, int incr) {
	Counter counter = (Counter) table.get (key);

	if (counter == null) {
	    counter = new Counter ();
	    
	    table.put (key, counter);
	}

	counter.value += incr;
    }

    
    private static final class Counter {
	long value;
    }
}

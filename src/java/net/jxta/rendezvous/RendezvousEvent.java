/*
 * $Id: RendezvousEvent.java,v 1.1 2007/01/16 11:01:28 thomas Exp $
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
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS  OR
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
 *
 * $Id: RendezvousEvent.java,v 1.1 2007/01/16 11:01:28 thomas Exp $
 *
 */

package net.jxta.rendezvous;

import java.util.EventObject;

import net.jxta.id.ID;

/**
 *  Container for RendezvousEvent events. The source of the event is the
 *  rendezvous service which 
 **/
public class RendezvousEvent extends EventObject {
    
    public static final int       RDVCONNECT = 0;
    public static final int     RDVRECONNECT = 1;
    public static final int    CLIENTCONNECT = 2;
    public static final int  CLIENTRECONNECT = 3;
    public static final int    RDVDISCONNECT = 4;
    public static final int        RDVFAILED = 5;
    public static final int CLIENTDISCONNECT = 6;
    public static final int     CLIENTFAILED = 7;
    public static final int        BECAMERDV = 8;
    public static final int       BECAMEEDGE = 9;
    
    private static final String EVENTNAMES [] = {
        "RDVCONNECT",
        "RDVRECONNECT",
        "CLIENTCONNECT",
        "CLIENTRECONNECT",
        "RDVDISCONNECT",
        "RDVFAILED",
        "CLIENTDISCONNECT",
        "CLIENTFAILED",
        "BECAMERDV",
        "BECAMEEDGE"
    };
    
    private int type;
    private ID  peer;
    
    /**
     *  Creates a new event
     *
     *  @param source The rendezvous service which generated the event.
     *  @param type the event type.
     *  @param peer the peer associated with the event.
     **/
    public RendezvousEvent( Object source, int type, ID peer ) {
        super( source );
        this.type = type;
        this.peer = peer;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String toString() {
        String eventType;
        
        if( (type >= RDVCONNECT) && (type <= BECAMEEDGE) ) {
            eventType = EVENTNAMES[type];
        } else {
            eventType = "UNKNOWN (" + type + ")";
        }
        
        return super.toString() + " : " + eventType + " for [" + peer + "]";
    }
    
    /**
     *  Returns the event type
     *
     *  @return int type
     **/
    public int getType() {
        return type;
    }
    
    /**
     *  Returns peerid
     *
     *  @return the peer associated with the event
     **/
    public String getPeer() {
        return peer.toString();
    }
    
    /**
     *  Returns peerid
     *
     *  @return the peer associated with the event
     **/
    public ID getPeerID() {
        return peer;
    }
}

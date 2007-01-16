/*
 *
 * $Id: AbstractSimpleSelectable.java,v 1.1 2007/01/16 11:01:34 thomas Exp $
 *
 * Copyright (c) 2004 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This a tool to implement selectable objects. It may be composed or extended.<p/>
 *
 * <code>SimpleSelectable</code> objects that are not <code>SimpleSelector</code> objects only report changes to their
 * listeners.<p/>
 *
 * The listeners of a <code>SimpleSelectable</code> may be <code>SimpleSelector</code> objects or other
 * <code>SimpleSelectable</code> objects. However the method to register non-sectors is and must remain protected since
 * it would allow the connection of arbitrary listeners.<p/>
 *
 * @see SimpleSelector
 **/

public abstract class AbstractSimpleSelectable implements SimpleSelectable {

    public final IdentityReference identityReference = new IdentityReference(this);

    /**
     * The object that is to reported to listeners as having changed.  When this class is composed rather than extended, "this" is
     * probably not the right choice.
     **/
    private final SimpleSelectable srcObject;

    /**
     * Registered Change Listeners.
     **/
    private Map myListeners = Collections.synchronizedMap(new WeakHashMap(2));


    public IdentityReference getIdentityReference() {
        return identityReference;
    }

    public AbstractSimpleSelectable() {
        this.srcObject = this;
    }

    public AbstractSimpleSelectable(SimpleSelectable srcObject) {
        this.srcObject = srcObject;
    }

    /**
     * Tells whether there are registered selectors right now, or not.  A simpleselectable that also registers with something
     * else may want to unregister (with the obvious consistency precautions) if it nolonger has selectors of its own.
     *
     * @return true if there are listeners.
     **/
    protected boolean haveListeners() {
        return ! myListeners.isEmpty();
    }

    /**
     * This method takes any listener, not just a SimpleSelector.
     * @param selectable The SimpleSelectable to register
     **/
    protected void registerListener( SimpleSelectable selectable ) {
        myListeners.put(selectable, null);
    }

    /**
     * This method takes any listener, not just a SimpleSelector.
     * @param selectable The SimpleSelectable to unregister
     **/
    protected void unregisterListener( SimpleSelectable selectable ) {
        myListeners.remove(selectable);
    }

    /**
     * {@inheritDoc}
     **/
    public void register(SimpleSelector s) {
        registerListener(s);
        s.itemChanged(this);
    }

    /**
     * {@inheritDoc}
     **/
    public void unregister(SimpleSelector s) {
        unregisterListener(s);
    }

    /**
     * This method tells us that something changed and so we need to notify our selectors by invoking their itemChanged
     * method. This is normally invoked internally by the implementation. One of the reasons for the implementation to invoke this
     * method is that a SimpleSelectable object that this one is registered with has changed and so has invoked the itemChanged
     * method.  However, the correlation between the two is left up to the implementation.<p/> No external synchronization needed,
     * nor desirable.<p/>
     *
     * @return false if there are no selectors left (that's a suggestion for the implementation to use haveListeners and possibly
     * unregister itself).
     **/
    protected final boolean notifyChange() {
        // We use a weakHashMap as a set. The elements in the set are the keys. No values.
        SimpleSelectable[] listeners = (SimpleSelectable[]) myListeners.keySet().toArray(new SimpleSelectable[0]);

        int i = listeners.length;

        while (i-->0) {
            listeners[i].itemChanged(srcObject);
        }
        return (listeners.length) > 0 ;
    }
}

/*
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights reserved.
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: RefCountPeerGroupInterface.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;

import net.jxta.id.ID;
import net.jxta.service.Service;
import net.jxta.peergroup.PeerGroup;
import net.jxta.exception.ServiceNotFoundException;

/**
 * RefCountPeerGroupInterface is a PeerGroupInterface object that
 * also serves as a peergroup very-strong reference. When the last
 * such goes away, the peergroup terminates itself despite the existence
 * of aeternal strong references from the various service's threads
 * that would prevent it from ever being finalized.
 * The alternative: to give only weak references to threads seems impractical.
 **/
class RefCountPeerGroupInterface extends PeerGroupInterface {

    private Map roleMap;

    /**
     * Constructs an interface object that front-ends a given
     * PeerGroup object.
     */
    RefCountPeerGroupInterface(GenericPeerGroup theRealThing) {
        super(theRealThing);
    }

    RefCountPeerGroupInterface(GenericPeerGroup theRealThing, Map roleMap) {
        super(theRealThing);
        this.roleMap = roleMap;
    }

    /**
     * Normaly it is ignored. By definition, the interface object
     * protects the real object's start/stop methods from being called
     * However we have to make an exception for groups: even the creator
     * of a group does not have access to the real object. So the interface
     * has to forward startApp to the group, which is responsible for
     * ensuring that it is executed only once (if needed).
     *
     * @param arg A table of strings arguments.
     * @return int status indication.
     */
    public int startApp(String[] arg) {
        // Unlike our superclass's method, we do call the real
        // startApp method.
	return ((GenericPeerGroup) groupImpl).startApp(arg);
    }

    /**
     * This is here for temporary class hierarchy reasons.
     * it is normaly ignored. By definition, the interface object
     * protects the real object's start/stop methods from being called
     *
     * In that case we have to make an exception. Most applications currently
     * assume that they do not share the group object and that they do refer
     * to the real object directly. They call stopApp to signify their
     * intention of no-longer using the group. Now that groups are shared,
     * we convert stopApp to unref for compatibility.
     * We could also just do nothing and let the interface be GC'd but
     * calling unref makes the group go away immediately if not shared,
     * which is what applications that call stopApp expect.
     */
    public void stopApp() {
        unref();
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Since THIS is already such an object, it could return itself.
     * However, we want the group to know about the number of interfaces
     * objects floating around, so, we'll have the group make a new one.
     * That way, applications which want to use unref() on interfaces can
     * avoid sharing interface objects by using getInterface() as a sort of
     * clone with the additional ref-counting semantics.
     *
     * @return Service An interface object that implements
     * this service and nothing more.
     */

    public Service getInterface() {
        return ((GenericPeerGroup) groupImpl).getInterface();
    }

    /**
     * {@inheritDoc}
     *
     * Returns a weak interface object that refers to this interface
     * object rather than to the group directly. The reason for that
     * is that we want the owner of this interface object to be able
     * to invalidate all weak interface objects made out of this interface
     * object, without them keeping a reference to the group object, and
     * without necessarily having to terminate the group.
     *
     * @return PeerGroup A weak interface object that implements
     * this group and nothing more.
     */

    public PeerGroup getWeakInterface() {
        return new PeerGroupInterface(this);
    }

    /**
     * stopApp used to be the standard way of giving up on a group
     * instance, but now that goup instance can be shared, the standard
     * of letting go of a peer group is to stop referencing it.
     * Since the peergroup has permanent referers: the threads of the services
     * we need to use the interface object as a super-strong reference.
     * When an interface is finalized, it calls the group's unref method.
     * The unref method stopApps the group when the last reference is
     * gone. To accelerate the un-referencing of groups, applications may call
     * the interface's unref method, but that takes some dicipline since the
     * interface object becomes unusable after that. So, aware applications
     * that use it must also take care of always cloning the interface
     * object instead of sharing it.
     * For compatibility with current apps which call stopApp, we have the
     * interface's stopApp() do nothing as with all other interface objects.
     * An invoker that has a reference to the true group object can still
     * call its stopApp method with the usual result.
     */

    public void finalize() {
        // The user gave up on the group, may be without calling unref.
        // Call it, just in case. It will do the right thing if it has been
        // called already.
        unref();
    }

    /**
     * Can only be called once. After that the reference is no-longer usuable.
     */
    public void unref() {
        GenericPeerGroup theGrp;
        synchronized(this) {
            if (groupImpl == null) return;
            theGrp = (GenericPeerGroup) groupImpl;
            groupImpl = null;
        }
        theGrp.decRefCount();
    }

    /**
     * Service-specific role mapping is implemented here.
     **/

    /**
     * {@inheritDoc}
     **/
    public Service lookupService(ID name)
        throws ServiceNotFoundException {

	return lookupService(name, 0);
    }

    /**
     * {@inheritDoc}
     **/
    public Service lookupService(ID name, int roleIndex)
        throws ServiceNotFoundException {

        if (roleMap != null) {
            ID[] map = (ID[]) roleMap.get(name);

            // If there is a map, remap; else, identity is the default for
            // role 0 only; the default mapping has only index 0.

            if (map != null) {
                if (roleIndex < 0 || roleIndex >= map.length) {
                    throw (new ServiceNotFoundException(
                                "" + name + "[" + roleIndex + "]"));
                }

                // We have a translation; look it up directly
                return groupImpl.lookupService(map[roleIndex]);
            }
        }

        // No translation; use the name as-is, provided roleIndex is 0.
        // Do not call groupImpl.lookupService(name, id); group impls
        // should not have to implement it at all.

        if (roleIndex != 0) {
            throw (new ServiceNotFoundException(
                   "" + name + "[" + roleIndex + "]"));
                    
        }

	return groupImpl.lookupService(name);
    }

    /**
     * {@inheritDoc}
     **/
    public Iterator getRoleMap(ID name) {

        if (roleMap != null) {
            ID[] map = (ID[]) roleMap.get(name);

            // If there is a map, remap; else, identity is the default for
            // role 0 only; the default mapping has only index 0.

            if (map != null) {
                // return an iterator on it.
                return Collections.unmodifiableList(Arrays.asList(map)).iterator();
            }
        }

        // No translation; use the given name in a singleton.
        return Collections.singletonList(name).iterator();
    }
}

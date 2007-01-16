/*
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
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
 * $Id: FastHashMap.java,v 1.1 2007/01/16 11:01:22 thomas Exp $
 */

package net.jxta.impl.util;
import java.util.HashMap;
import java.util.Map;

/*
 * This hashMap is resistent to perfoming get() without synchronization while
 * the map is being modified. Of course, in that case there is no causality
 * relationship between modification and lookup. Lookup can return a result
 * inconsistent with a modification that can be proven to have returned
 * before the lookup. However there are many cases where this causality is
 * irrelevant and the parformance of lookup is critical.
 * It would be a simple matter (one line) to modify/overload HashMap.get() so
 * that it behaves that way, but that would be relying upon HashMap's
 * implementation or violating SUN's IP, which we cannot do. So this
 * implementation has to be somewhat inelegant.
 *
 * Because there is no API for an HashMap to be resized or rehashed, and since
 * it does so internally only to increase capacity, there has only one
 * potential problem with an unsynchronized lookup:
 *
 * - It can look for the entry in the wrong bucket and never find it while it
 *   has been there all along. That's wrong and need to be avoided.
 *
 * So, if we ever get a null entry, it is worth double checking It can
 * actually be either that the real value is null or that the entry does not
 * exist.
 * The double check has to be cheap as well. We may be looking for non
 * existent entries just as often.
 * This trick performs better if null is not used as a ligitimate value. 
 */ 

public class FastHashMap extends HashMap {

    volatile int localModCount = 0;

    public Object get(Object key) {
	Object res = super.get(key);
	if (res != null) return res;

	// Ok, trouble. Try harder: make sure we were not parallel
	// with a capacity increase.
	// We sample modCount twice. Modcount is changed twice: once before
	// and once after a change. Only if we get twice the same value and
	// it is even, do we know that no changes happened while we were
	// in get. Then, even if we get null, we know it's the truth.

	while (true) {
	    int count1 = localModCount;
	    res = super.get(key);
	    if (res != null) return res;
	    int count2 = localModCount;
	    if ((count2 == count1) && (count1 % 2 == 0)) return res;
	    Thread.yield();
	}

	// The collision can realy happen only once per modification,
	// but, if we have a false negative (the result is realy null),
	// while we were realy modifying in parallel, then we can't know
	// which iteration is the good one until the thread that does the
	// modification completes it by doing the second incrementation.
	// To make sure our spinning does not prevent it, we call
	// Thread.yield. So, this is almost a spin-lock, but it is used
	// very rarely.
    }

    // The rules do not change for these methods: they have to be
    // called from a critical section. Otherwise, modcount may be
    // misleading the get() method.
    public Object put(Object key, Object value) {
	localModCount++;
	Object res = super.put(key, value);
	localModCount++;
	return res;
    }
    public void putAll(Map t) {
	localModCount++;
	super.putAll(t);
	localModCount++;
    }

    public FastHashMap(int s) {
	super(s);
    }
}

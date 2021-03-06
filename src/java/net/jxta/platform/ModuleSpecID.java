/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
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
 * $Id: ModuleSpecID.java,v 1.1 2007/01/16 11:02:01 thomas Exp $
 */

package net.jxta.platform;

import net.jxta.id.ID;

/**
 * A ModuleSpecID uniquely identifies a particular network behaviour
 * (wire protocol and choregraphy) that may be embodied by a Jxta Module.
 * There may be any number of implementations of a given SpecID. All
 * such implementations are assumed to be network compatible.
 *
 * <p>
 * The Specification that corresponds to a given ModuleSpecID may be published
 * in a ModuleSpecAdvertisement. This advertisement is uniquely identified by
 * the ModuleSpecID that it describes.
 *
 * <p>
 * The various implementations of a given SpecID may be published in
 * ModuleImplAdvertisements. These advertisements are identified by the
 * ModuleSpecID that they implement and a compatibility statement.
 * ModuleImplAdvertisements baring the same SpecID and compatibility statement
 * are theorethicaly interchangeable. However they may be subsequently discriminated
 * by a Description element.
 *
 * <p>
 * A ModuleSpecID embeds a ModuleClassID which uniquely identifies a base Module
 * class. A base module class defines a local behaviour and one API per compatible
 * JXTA implementation.
 * 
 * <p>
 * A ModuleSpecID therefore uniquely identifies an abstract module, of which an
 * implementation compatible with the local JXTA implementation may be located and
 * instantiated.
 *
 * <p>
 * In the standard PeerGroup implementation of the java reference implementation
 * the various services are specified as a list of ModuleSpecID, for each of which
 * the group locates and loads an implementation as part of the group's
 * initialization.
 *
 * @see net.jxta.peergroup.PeerGroup
 * @see net.jxta.platform.Module
 * @see net.jxta.platform.ModuleClassID
 * @see net.jxta.protocol.ModuleSpecAdvertisement
 * @see net.jxta.protocol.ModuleImplAdvertisement
 * @see net.jxta.id.ID
 * @see net.jxta.document.Advertisement
 *
 */

public abstract class ModuleSpecID extends ID {

    /**
     * Returns true if this ModuleSpecID is of the same base class than the
     * given class.
     * Note: This method is NOT named "isOfClass" because a ModuleClassID
     * may have two portions; one that denotes a class proper,
     * and an optional second one that denotes a "Role". For convenience, we refer
     * the class stripped of its role portion as "the base class" although this is not
     * a totally accurate term.
     * A ModuleSpecID, is of a base class but is not related to any kind
     * of role. So using "isOfClass" could be misleading.
     * Base classes are represented by a class with the role ID set to zero, which
     * happens to be a valid class. This routine may be used for comparison with
     * such a class, of course.
     *
     * @param id Module class id to compare with
     * @return boolean true if equals
     *
     */
    
    public abstract boolean isOfSameBaseClass( ModuleClassID id );

    /**
     * Returns true if this ModuleSpecID is of the same base class than the
     * the given ModuleSpecID.
     *
     * @param id Module spec id to compare with
     * @return boolean true if equals
     *
     */
    
    public abstract boolean isOfSameBaseClass( ModuleSpecID id );

    /**
     * Return a ModuleClassID of the same base class but with the role portion
     * set to zero. aka "the base class".
     *
     * @return ModuleClassID the base class.
     *
     */

    public abstract ModuleClassID getBaseClass( );
}

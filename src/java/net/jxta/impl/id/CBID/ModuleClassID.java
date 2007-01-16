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
 * $Id: ModuleClassID.java,v 1.1 2007/01/16 11:02:00 thomas Exp $
 */

package net.jxta.impl.id.CBID;

import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;

/**
 *  An implementation of the {@link net.jxta.platform.ModuleClassID} ID Type.
 **/
public final class ModuleClassID extends net.jxta.impl.id.UUID.ModuleClassID {
    
    /**
     *  Log4J categorgy
     **/
    private static final transient Logger LOG = Logger.getLogger( ModuleClassID.class.getName());
    
    /**
     * Constructor.
     * Intializes contents from provided ID.
     *
     * @param id    the ID data
     **/
    protected ModuleClassID( IDBytes id ) {
        super( id );
    }
    
    /**
     * Constructor.
     * Creates a ModuleClassID in a given class, with a given class unique id.
     * A UUID of a class and another UUID are provided.
     *
     * @param classCBID    the class to which this will belong.
     * @param roleCBID     the unique id of this role in that class.
     **/
    protected ModuleClassID( UUID classUUID, UUID roleUUID ) {
        super( classUUID, roleUUID );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newModuleClassID()}.
     *
     *  <p/>A new class UUID is created. The role ID is left null. This is the
     *  only way to create a new class without supplying a new UUID explicitly.
     *
     *  <p/>Note that a null role is just as valid as any other, it just has a
     *  shorter string representation. So it is not mandatory to create a new
     *  role in a new class.
     **/
    public ModuleClassID( ) {
        this( UUIDFactory.newUUID(), new UUID( 0L, 0L ) );
    }
    
    /**
     *  See {@link net.jxta.id.IDFactory.Instantiator#newModuleClassID(net.jxta.platform.ModuleClassID)}.
     **/
    public ModuleClassID( ModuleClassID classID ) {
        this( classID.getClassUUID( ), UUIDFactory.newUUID() );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }
   
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.platform.ModuleClassID getBaseClass( ) {
        return new ModuleClassID(getClassUUID( ), new UUID( 0L, 0L ));
    }
    
     /**
     * get the class' unique id
     *
     * @return UUID module class' unique id
     **/
    protected UUID getClassUUID( ) {
        return super.getClassUUID();
    }
}

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
 * $Id: ModuleClassID.java,v 1.1 2007/01/16 11:02:08 thomas Exp $
 */

package net.jxta.impl.id.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 *  An implementation of the {@link net.jxta.platform.ModuleClassID} ID Type.
 **/
public class ModuleClassID extends net.jxta.platform.ModuleClassID {
    
    /**
     *  Log4J categorgy
     **/
    private static final transient Logger LOG = Logger.getLogger( ModuleClassID.class.getName());
    
    /**
     *  Location of the class id
     **/
    private final static int moduleClassIdOffset = 0;
    
    /**
     *  Location of the role id
     **/
    private final static int moduleRoleIdOffset = moduleClassIdOffset + IDFormat.uuidSize;
    
    /**
     *  location of the start of the pad space
     **/
    private final static int padOffset = ModuleClassID.moduleRoleIdOffset + IDFormat.uuidSize;
    
    /**
     *  size of the unused space
     **/
    private final static int padSize = IDFormat.flagsOffset - ModuleClassID.padOffset;
    
    /**
     *  The id data
     **/
    protected IDBytes id;
    
    /**
     * Constructor.
     * Intializes contents from provided ID.
     *
     * @param id    the ID data
     **/
    protected ModuleClassID( IDBytes id ) {
        super();
        this.id = id;
    }
    
    /**
     * Constructor.
     * Creates a ModuleClassID in a given class, with a given class unique id.
     * A UUID of a class and another UUID are provided.
     *
     * @param classUUID    the class to which this will belong.
     * @param roleUUID     the unique id of this role in that class.
     **/
    protected ModuleClassID( UUID classUUID, UUID roleUUID ) {
        super();
        id = new IDBytes();
        id.bytes[IDFormat.flagsOffset + IDFormat.flagsIdTypeOffset] = IDFormat.flagModuleClassID;
        
        id.longIntoBytes( ModuleClassID.moduleClassIdOffset,
                classUUID.getMostSignificantBits() );
        id.longIntoBytes( ModuleClassID.moduleClassIdOffset + 8,
                classUUID.getLeastSignificantBits() );
        
        id.longIntoBytes( ModuleClassID.moduleRoleIdOffset,
                roleUUID.getMostSignificantBits() );
        id.longIntoBytes( ModuleClassID.moduleRoleIdOffset + 8,
                roleUUID.getLeastSignificantBits() );
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
    public boolean equals( Object target ) {
        if (this == target) {
            return true;
        }
        
        if (target instanceof ModuleClassID ) {
            ModuleClassID mcidTarget = (ModuleClassID)target;
            
            if( !getIDFormat().equals( mcidTarget.getIDFormat() ) ) {
                return false;
            }
            
            if( id == mcidTarget.id ) {
                return true;
            }
            
            boolean result = id.equals( mcidTarget.id );
            
            // if true then we can have the two ids share the id bytes
            if( result ) {
                mcidTarget.id = id;
            }
            
            return result;
        } else
            return false;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public int hashCode() {
        return id.hashCode();
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
    public Object getUniqueValue( ) {
        return getIDFormat() + "-" + (String) id.getUniqueValue();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public net.jxta.platform.ModuleClassID getBaseClass( ) {
        return new ModuleClassID(getClassUUID( ), new UUID( 0L, 0L ));
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isOfSameBaseClass( net.jxta.platform.ModuleClassID classId ) {
        return getClassUUID( ).equals( ((ModuleClassID) classId).getClassUUID( ) );
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean isOfSameBaseClass( net.jxta.platform.ModuleSpecID specId ) {
        return getClassUUID( ).equals( ((ModuleSpecID) specId).getClassUUID( ) );
    }
    
    /**
     * get the class' unique id
     *
     * @return UUID module class' unique id
     **/
    protected UUID getClassUUID( ) {
        UUID result =
                new UUID(id.bytesIntoLong(ModuleClassID.moduleClassIdOffset),
                id.bytesIntoLong(ModuleClassID.moduleClassIdOffset + 8));
        
        return result;
    }
    
    /**
     * get the role unique id
     *
     * @return UUID module role unique id
     **/
    protected UUID getRoleUUID( ) {
        UUID result =
                new UUID(id.bytesIntoLong(ModuleClassID.moduleRoleIdOffset),
                id.bytesIntoLong(ModuleClassID.moduleRoleIdOffset + 8));
        
        return result;
    }
}

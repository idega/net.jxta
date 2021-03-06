package net.jxta.impl.xindice.core.indexer;

/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xindice" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
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
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999-2001, The dbXML
 * Group, L.L.C., http://www.dbxmlgroup.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * $Id: Indexer.java,v 1.1 2007/01/16 11:02:12 thomas Exp $
 */

import net.jxta.impl.xindice.core.*;
import net.jxta.impl.xindice.core.data.*;

/**
 * Indexer is the abstract indexing interface for Xindice.  An Indexer
 * object is implemented in order to retrieve and manage indexes.
 * <br><br>
 * Any number of Indexer instances may be associated with a single
 * Collection.  The type of Indexer utilized by a query depends on
 * the 'Style' of Indexer and the type of QueryResolver that is being
 * used to performt he query.  Currently, Xindice only internally
 * supports one kind of Indexer: 'XPath'.
 */

public interface Indexer extends DBObject {
   
   /**
    * remove removes all references to the specified Key from the Indexer.
    *
    * @param value The value to remove
    * @param key The Object ID
    */
   void remove(Key key) throws DBException;

   /**
    * add adds a Document to the Indexer.
    *
    * @param value The value to remove
    * @param key The Object ID
    */
   void add(Key key, long pos) throws DBException;
   
   /**
    * flush forcefully flushes any unwritten buffers to disk.
    */
   void flush() throws DBException;
}


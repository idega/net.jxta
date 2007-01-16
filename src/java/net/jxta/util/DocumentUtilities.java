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
 * $Id: DocumentUtilities.java,v 1.1 2007/01/16 11:01:35 thomas Exp $
 */

package net.jxta.util;

import net.jxta.document.*;
import net.jxta.exception.*;
import java.util.*;
import java.io.*;

/**
 *  @deprecated Will be removed soon.
 **/
public final class DocumentUtilities {
    
    private DocumentUtilities() {}

	public static StructuredTextDocument createStructuredXmlDocument(String rootTagName) throws JxtaException {
		return (StructuredTextDocument)	StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, rootTagName);
	}

	public static StructuredTextDocument getStructuredXmlDocument(String buf) throws JxtaException {
		try {
			return (StructuredTextDocument)	StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader( buf));
		} catch (IOException e) {
			throw new JxtaException("Error reading document", e);
		}
	}
	
	public static StructuredTextDocument getStructuredXmlDocument(byte buf[]) throws JxtaException {
		return getStructuredXmlDocument(new ByteArrayInputStream(buf));
	}
	
	public static StructuredTextDocument getStructuredXmlDocument(InputStream in) throws JxtaException {
		try {
			return (StructuredTextDocument)	StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, in);
		} catch (IOException e) {
			throw new JxtaException("Error reading document", e);
		}
	}
	
	public static StructuredDocument createParamDocument() {
		return (StructuredDocument)	StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
	}

	public static Element getChild(Element root, Object key) {
		Enumeration e=root.getChildren(key);
		if (e == null) 
			return null;
		
		return (Element) e.nextElement();
	}
}

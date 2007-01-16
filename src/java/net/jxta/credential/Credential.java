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
 * $Id: Credential.java,v 1.1 2007/01/16 11:01:59 thomas Exp $
 */

package net.jxta.credential;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.id.ID;
import net.jxta.service.Service;

/**
 * Credentials provide the basic mechanisms for securly establishing and
 * communicating identity within JXTA. Credentials have three different roles
 * within JXTA:
 *
 * <ul>
 *  <li>Authentication credentials are associated with authentication methods
 *  and are used to provide information required for authentication. Each
 *  {@link net.jxta.credential.AuthenticationCredential AuthenticationCredential}
 *  implementation is specific to its associated
 *  {@link net.jxta.membership.Authenticator Authenticator}. Authentication
 *  Credentials are normally created by constructing a document which follows
 *  a schema provided by the authentication method.</li>
 *
 *  <li>Identity credentials associate an identity with a peer. The peer may
    request operations to be performed using that identity. Identity Credentials
    are created by successfully completing authentication with a Membership
 *  Service.</li>
 *
 *  <li>Priviledged operations associate an operation with an identity. To
 *  request a remote peer to perform some operation an application or service
 *  provides a {@link net.jxta.credential.PrivilegedOperation} and an
 *  identity credential along with the request. The remote peer determines if
 *  the operation is permitted for the specified identity and if it is permitted,
 *  completes the operation.</li>
 * </ul>
 *
 * <p/>The XML representation of a Credential uses the following very simple
 * schema. Credential implementations extend this schema as needed.
 *
 * <p/><pre>
 * &lt;xs:complexType name="Cred">
 *   &lt;xs:all>
 *   &lt;/xs:all>
 * &lt;/xs:complexType>
 * </pre>
 **/
public interface Credential {
    
    /**
     * Returns the peerGroupID associated with this credential
     *
     * @return the peerGroupID associated with this credential
     **/
    public ID   getPeerGroupID();

    /**
     * Returns the peerID associated with this credential
     *
     * @return the peerID associated with this credential
     **/
    public ID   getPeerID();
    
    /**
     * Returns the service which generated this credential.
     *
     * @return the service which generated this credential.
     **/
    public Service getSourceService();
    
    /**
     *  If true then the credential is expired. Some credential implementations
     *  may never epxire.
     **/
    public boolean isExpired();
    
    /**
     *  Returns true if this credential is currently valid.
     *
     *  @return boolean true if the credential is currently valid, otherwise
     *  false.
     **/
    public boolean isValid();
    
    /**
     *  Returns the subject of this credential. The Objects returned <b>must</b>
     *  support {@link Object#equals(Object)} and {@link Object#hashCode()}.
     *
     *  @return the subject of the credential as an abstract object.
     **/
    public Object getSubject();
    
    /**
     *  Write credential into a document. <code>asMimeType</code> is a mime
     *  media-type specification and provides the form of the document which is
     *  being requested. Two standard document forms are defined.
     *  <code>"text/plain"</code> encodes the document in a "pretty-print"
     *  format for human viewing and <code>"text/xml"<code> which provides an
     *  XML format.
     *
     *  <p/>Depending on the credential format this document may be
     *  cryptographically signed to prevent alteration.
     *
     *  @param asMimeType  MimeMediaType format representation requested
     *  @return Document   the document to be used in the construction
     **/
    public StructuredDocument getDocument( MimeMediaType asMimeType ) throws Exception;
}

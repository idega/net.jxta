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
 * $Id: ResolverService.java,v 1.1 2007/01/16 11:01:39 thomas Exp $
 */

package net.jxta.resolver;

import net.jxta.service.Service;

/**
 * ResolverService provides a generic mechanism for JXTA Services
 * to send "Queries", and receive "Responses".  It removes the burden for
 * registered handlers in deal with :
 *
 *<ul type-disc>
 *    <li><p>Setting message tags, to ensure uniqueness of tags and
 *     ensures that messages are sent to the correct address, and group
 *    <li><p>Authentication, and Verification of credentials
 *    <li><p>drop rogue messages
 *</ul>
 *
 * <p/>The ResolverService does not proccess the queries, nor does it not compose
 * reponses. Handling of queries, and composition of responses are left up
 * to the registered handlers. Services that wish to handle queries,
 * and generate reponses must implement {@link net.jxta.resolver.QueryHandler}
 *
 * <p/>Message Format:
 *
 * <ul><li>A Query message:
 *
 * <pre>&lt;?xml version="1.0" standalone='yes'?&gt;
 * &lt;ResolverQuery&gt;
 *   &lt;handlername&gt; name &lt;/handlername&gt;
 *   &lt;credentialServiecUri&gt; uri &lt;/credentialServiecUri&gt;
 *   &lt;credentialToken&gt; token &lt;/credentialToken&gt;
 *   &lt;srcpeerid&gt; srcpeerid &lt;/srcpeerid&gt;
 *   &lt;queryid&gt; id &lt;/queryid&gt;
 *   &lt;query&gt; query &lt;/query&gt;
 * &lt;/ResolverQuery&gt;</pre>
 *
 * <p/>Note: queryid is unique to the originating node only, it can be utilized to
 * match queries to responses.</p></li>
 *
 * <li>A Response Message:
 *
 * <pre>&lt;?xml version="1.0" standalone='yes'?&gt;
 * &lt;ResolverResponse&gt;
 *   &lt;handlername&gt; name &lt;/handlername&gt;
 *   &lt;credentialServiecUri&gt; uri &lt;/credentialServiecUri&gt;
 *   &lt;credentialToken&gt; token &lt;/credentialToken&gt;
 *   &lt;queryid&gt; id &lt;/queryid&gt;
 *   &lt;response&gt; response &lt;/response&gt;
 * &lt;/ResolverResponse&gt;</pre>
 *
 * <p/>Note: queryid is unique to the originating node only, it can be
 * utilized to match queries to responses.</li></ul>
 *
 * @see net.jxta.service.Service
 * @see net.jxta.resolver.GenericResolver
 * @see net.jxta.resolver.QueryHandler
 * @see net.jxta.protocol.ResolverQueryMsg
 * @see net.jxta.protocol.ResolverResponseMsg
 *
 **/
public interface ResolverService extends Service, GenericResolver {

    /**
     *  Returned by query handlers to indicate that the query should be
     *  forwarded to the rest of the network.
     **/
    public final static int Repropagate = -1;
    
    /**
     *  Returned by query handlers to indicate that the query has been resolved
     *  and a response has been sent.
     **/
    public final static int OK = 0;
    

    /**
     * Registers a given ResolveHandler, returns the previous handler registered under this name
     *
     * @param name The name under which this handler is to be registered.
     * @param handler The handler.
     * @return The previous handler registered under this name     
     *
     */
    public QueryHandler registerHandler( String name, QueryHandler handler );

   /**
     * unregisters a given ResolveHandler, returns the previous handler registered under this name
     *
     * @param name The name of the handler to unregister.
     * @return The previous handler registered under this name     
     *
     */    
    public QueryHandler unregisterHandler( String name );

    /**
     * Registers a given SrdiHandler, returns the previous handler registered under this name 
     *
     * @param name The name under which this handler is to be registered.
     * @param handler The handler.
     * @return The previous handler registered under this name     
     *
     */
    public SrdiHandler registerSrdiHandler( String name, SrdiHandler handler );

    /**
     * unregisters a given SrdiHandler, returns the previous handler registered under this name
     *
     * @param name The name of the handler to unregister.
     * @return The previous handler registered under this name
     *
     */    
    public SrdiHandler unregisterSrdiHandler( String name );

}

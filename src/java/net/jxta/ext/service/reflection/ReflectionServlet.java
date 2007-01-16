/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *  must not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: ReflectionServlet.java,v 1.1 2007/01/16 11:02:07 thomas Exp $
 */

package net.jxta.ext.service.reflection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

/**
 * Servlet to manage the reflection service systems test.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class ReflectionServlet
    extends HttpServlet {

    private static final String ORIGINATOR = "originator";
    private static final String DELIMITER = ":";
    private static final String TEXT_CONTENT = "text/plain";

    /**
     * Initialize the servlet.
     *
     * @param   config      specify the ServletConfig
     */
    
    public void init(ServletConfig config)
    throws ServletException {
        super.init(config);
    }

    /**
     * Handle inbound requests.
     *
     * @param       request             request message
     * @param       response            response message
     * @throws      ServletExcpetion    servlet errors
     * @throws      {@link java.io.IOException} io errors
     */
    
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Handle inbound requests.
     *
     * @param       request             request message
     * @param       response            response message
     * @throws      ServletException    servlet errors
     * @throws      {@link java.io.IOException} io errors
     */
    
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType(TEXT_CONTENT);

        PrintWriter pw = null;

        try {
            pw = response.getWriter();
        } catch (IOException ioe) {}

        if (pw != null) {
            pw.println(ORIGINATOR + DELIMITER + request.getRemoteAddr());

            List t = getContent(request);

            if (t.size() > 0) {
                URI u = null;
                List r = process(t);

                for (Iterator i = r != null ? r.iterator() : Collections.EMPTY_LIST.iterator(); i.hasNext(); ) {
                    u = (URI)i.next();

                    pw.println(u + DELIMITER + (r.contains(u)));
                }
            }

            pw.close();
        }
    }

    /**
     * Deconstruction process.
     */
    
    public void destroy() {}

    private List getContent(HttpServletRequest request) {
        List u = new ArrayList();

        try {
            BufferedReader br = request.getReader();
            String s = null;

            while ((s = br.readLine()) != null) {
                if (s.trim().length() > 0) {
                    try {
                        u.add(new URI(s.trim()));
                    } catch (URISyntaxException use) {}
                }
            }
        }
        catch (IOException ioe) {}

        return u;
    }

    private List process(List t) {
        ReflectionProbe rp = new ReflectionProbe();
        List r = rp.probe(t);

        return r != null ? r : Collections.EMPTY_LIST;
    }
}

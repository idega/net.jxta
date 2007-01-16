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
 *  $Id: HttpGet.java,v 1.1 2007/01/16 11:01:26 thomas Exp $
 */
package net.jxta.ext.http;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * HTTP GET {@link net.jxta.ext.http.Dispatchable} implementation.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class HttpGet
    implements Dispatchable {

    /**
     * Default buffer size: {@value BLOCK}
     */
    
    protected final static int BLOCK = 4 * 1024;
    
    /**
     * The HTTP GET method: {@value net.jxta.ext.http.Message#GET}
     */
    
    protected String method = Message.GET;
    
    /**
     * The HTTP connection.
     */
    
    protected URLConnection connection = null;

    protected static Map headers;

    private URL url = null;
    private Message message = null;

    static {
        headers = new HashMap();

        headers.put(Message.USER_AGENT, Message.TROLL);
        headers.put(Message.ACCEPT, Message.ACCEPT_ALL);
        headers.put(Message.CONNECTION, Message.KEEP_ALIVE);
    }

    /**
     * Default constructor.
     */
    
    public HttpGet() {
        this(null);
    }

    /**
     * Constructor which specifies the destination {@link java.net.URL} address.
     *
     * @param       url     destination {@link java.net.URL} address
     */
    
    public HttpGet(URL url) {
        this(url, null);
    }

    /**
     * Constructor which specifies the destination {@link java.net.URL} address
     * and {@link net.jxta.ext.http.Message} payload.
     *
     * @param       url     destination {@link java.net.URL} address
     * @param       msg     {@link net.jxta.ext.http.Message} payload
     */
    
    public HttpGet(URL url, Message msg) {
        this.url = url;
        this.message = msg;
    }

    /**
     * Accessor to the destination {@link java.net.URL} address.
     *
     * @return          destination {@link java.net.URL} address
     */
    
    public URL getURL() {
        return this.url;
    }
    
    /**
     * Specifies the destiation {@link java.net.URL} address.
     *
     * @param       url     specified destination {@link java.net.URL} address
     */

    public void setURL(URL url) {
        this.url = url;
    }

    /**
     * Accessor to the payload {@link net.jxta.ext.http.Message}.
     *
     * @return          payload {@link net.jxta.ext.http.Message}
     */
    
    public Message getMessage() {
        return this.message;
    }

    /**
     * Specifies the payload {@link net.jxta.ext.http.Message}.
     *
     * @param       msg     payload {@link net.jxta.ext.http.Message}
     */
    
    public void setMessage(Message msg) {
        this.message = msg;
    }

    /**
     * Establishes a connection with the destination {@link java.net.URL},
     * dispatches the payload {@link net.jxta.ext.http.Message} and returns the {@link net.jxta.ext.http.Message}
     * response.
     *
     * @return              response {@link net.jxta.ext.http.Message}
     * @throws     {@link java.io.IOException} Thrown in the event of a network
     *                                          failure.
     */
    
    public Message dispatch()
    throws IOException {
        return dispatch(this.url);
    }

    /**
     * Stops the network connection.
     */
    
    public void disconnect() {
        this.connection = null;
    }

    /**
     * Establishes a connection with the specified destination {@link java.net.URL},
     * dispatches the payload {@link net.jxta.ext.http.Message} and returns the {@link net.jxta.ext.http.Message}
     * response.
     *
     * @param       u       specified destination {@link java.net.URL} address
     * @return              response {@link net.jxta.ext.http.Message}
     * @throws      {@link java.io.IOException} Thrown in the event of a network
     *                                          failure.
     */
    
    protected Message dispatch(URL u)
    throws IOException {
        this.connection = openConnection(u);

        doGet();

        Message response = getResponse(u);

        closeConnection();

        return response;
    }

    /**
     * Establishes a {@link java.net.URLConnection} with the specified {@link java.net.URL}.
     *
     * @param       u       destination {@link java.net.URL} address
     * @return              resulting {@link java.net.URLConnection}
     * @throws      {@link java.io.IOException} Thrown in the event of a network
     *                                          failure.
     */
    
    protected URLConnection openConnection(URL u)
    throws IOException {
        URLConnection uc = null;

        if (u.getProtocol().equalsIgnoreCase(Message.HTTP) ||
            u.getProtocol().equalsIgnoreCase(Message.HTTPS)) {
            uc = openHttpConnection(u);
        } else {
            uc = openFileConnection(u);
        }

        return uc;
    }

    /**
     * Stops the network connection.
     */
    
    protected void closeConnection() {
        if (this.connection != null) {
            if (this.connection instanceof HttpURLConnection) {
                ((HttpURLConnection)this.connection).disconnect();
            }
        }

        this.connection = null;
    }

    /**
     *  HTTP GET handler.
     *
     * @throws      {@link java.io.IOException} Thrown in the event of a network
     *                                          failure.
     */
    
    protected void doGet()
    throws IOException {}

    /**
     * Establishes a connection with the specified destination {@link java.net.URL},
     * dispatches the payload {@link net.jxta.ext.http.Message} and returns the {@link net.jxta.ext.http.Message}
     * response.
     *
     * @return              response {@link net.jxta.ext.http.Message}
     * @throws      {@link java.io.IOException} Thrown in the event of a network
     *                                          failure.
     */
    
    protected Message getResponse(URL u)
    throws IOException {
        Message msg = new Message();

        try {
            msg.setHeaders(getResponseHeaders());

            String clh = msg.getHeader(Message.CONTENT_LENGTH);
            int cl = -1;

            try {
                cl = Integer.valueOf(clh != null ? clh : "-1").intValue();
            } catch (NumberFormatException nfe) {}

            String ct = msg.getHeader(Message.CONTENT_TYPE);

            msg.setBody((cl == -1 && ct != null) || cl > 0 ?
                        getResponseBody() : "");
        } catch (Exception e) {
            throw new IOException();
        }

        return msg;
    }

    private HttpURLConnection openHttpConnection(URL u)
    throws IOException {
        HttpURLConnection c = (HttpURLConnection)u.openConnection();

        c.setRequestMethod(this.method);

        String key = null;
        String value = null;
        Message msg = getMessage();

        if (msg != null) {
            for (Iterator k = msg.getHeaderKeys(); k.hasNext(); ) {
                key = (String)k.next();
                value = (String)msg.getHeader(key);

                c.setRequestProperty(key, value);
            }
        }

        boolean doOutput = (this.method == Message.POST &&
                            msg != null && msg.hasBody());
        boolean followRedirects = false;

        for (Iterator k = headers.keySet().iterator(); k.hasNext(); ) {
            key = (String)k.next();

            if (c.getRequestProperty(key) == null) {
                value = (String)headers.get(key);

                c.setRequestProperty(key, value);
            }
        }

        if (doOutput) {
            c.setRequestProperty(Message.CONTENT_LENGTH,
                                 Integer.toString(msg.getBody().getBytes().length));
        }

        c.setDoOutput(doOutput);
        c.setDoInput(true);
        c.setUseCaches(false);
        c.setFollowRedirects(followRedirects);
        c.setInstanceFollowRedirects(followRedirects);

        return c;
    }

    private URLConnection openFileConnection(URL u)
    throws IOException {
        return u.openConnection();
    }

    private Map getResponseHeaders() {
        Map headers = new HashMap();

        headers.putAll(this.connection.getHeaderFields());
        headers.remove(null);

        return headers;
    }

    private String getResponseBody()
    throws IOException {
        CharArrayWriter w = new CharArrayWriter();

        try {
            InputStreamReader isr =
                new InputStreamReader(this.connection.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            char[] buf = new char[BLOCK];
            int l = 0;

            while ((l = br.read(buf, 0, BLOCK)) > -1) {
                w.write(buf, 0, l);
            }

            br.close();
        } catch (Exception e) {
            throw new IOException();
        }

        return w.toString();
    }
}

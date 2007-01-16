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
 *  $Id: Message.java,v 1.1 2007/01/16 11:01:25 thomas Exp $
 */
package net.jxta.ext.http;

import java.net.URL;

import java.util.Collections;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * HTTP message container.
 *
 * @author  james todd [gonzo at jxta dot org]
 */

public class Message {

    public static final String SESSION_ID = "JSESSIONID";
    public static final String SESSION_ID_PREFIX = "=";
    public static final String SESSION_ID_POSTFIX = ";";
    public static final String COOKIE = "Cookie";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String USER_AGENT = "User-Agent";
    public static final String TROLL = "Troll/1.0 (Life is Good) Troll/1.0";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    public static final String ACCEPT_ALL = "*/*";
    public static final String CHARSET = "iso-8859-1,*,utf-8";
    public static final String URL_FORM_ENCODED = "application/x-www-form-urlencoded";
    public static final String CONNECTION = "Connection";
    public static final String KEEP_ALIVE = "Keep-Alive";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String GET = "GET";
    public static final String POST = "POST";

    private static List sessionIds = null;
    private Map headers = null;
    private String body = null;
    private URL referer = null;

    static {
        sessionIds = new ArrayList();

        sessionIds.add(SESSION_ID);
    }

    public Message() {
        this(null, null);
    }

    public Message(Map headers) {
        this(headers, null);
    }

    public Message(String body) {
        this(null, body);
    }

    public Message(Map headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    public String getHeader(String key) {
        Iterator i = getHeaders(key);

        return (i.hasNext() ? (String)i.next() : null);
    }

    public Iterator getHeaders(String key) {
        List values = new ArrayList();
        String k = null;
        Object o = null;

        for (Iterator i = getHeaderKeys(); i != null && i.hasNext(); ) {
            k = (String)i.next();

            if (k.equalsIgnoreCase(key)) {
                o = this.headers.get(k);

                if (o instanceof String) {
                    values.add((String)o);
                } else if (o instanceof Collection) {
                    values.addAll((Collection)o);
                }
            }
        }

        return values.iterator();
    }

    public Iterator getHeaderKeys() {
        return ((this.headers != null) ?
                this.headers.keySet().iterator() :
                Collections.EMPTY_LIST.iterator());
    }

    public void setHeader(String key, String value) {
        setHeader(key, (Object)value);
    }

    public void setHeader(String key, Object value) {
        if (this.headers == null) {
            this.headers = new HashMap();
        }

        this.headers.put(key, value);
    }

    public void setHeaders(Map headers) {
        String key = null;

        for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
            key = (String)i.next();

            setHeader(key, headers.get(key));
        }
    }

    public void removeHeader(String key) {
        if (this.headers != null) {
            this.headers.remove(key);

            if (this.headers.size() == 0) {
                this.headers = null;
            }
        }
    }

    public void removeHeaders() {
        if (this.headers != null) {
            this.headers.clear();
            this.headers = null;
        }
    }

    public String getSessionId() {
        String s = getHeader(COOKIE);

        return (s != null ?
                parseSessionIdFromHeader(s) :
                parseSessionIdFromBody(getBody()));
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean hasBody() {
        return (this.body != null &&
                this.body.trim().length() > 0);
    }

    public void setReferer(URL referer) {
        this.referer = referer;
    }

    public URL getReferer() {
        return this.referer;
    }

    public String toString() {
        java.lang.Class clazz = getClass();
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        java.util.Map map = new java.util.HashMap();
        java.lang.String object = null;
        java.lang.Object value = null;

        for (int i = 0; i < fields.length; i++) {
            try {
                object = fields[i].getName();
                value = fields[i].get(this);

                if (value == null) {
                    value = new String("null");
                }

                map.put(object, value);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
        }

        if (clazz.getSuperclass().getSuperclass() != null) {
            map.put("super", clazz.getSuperclass().toString());
        }

        return clazz.getName() + map;
    }

    private String parseSessionIdFromHeader(String value) {
        final String EQUAL = "=";

        String id = null;
        String prefix = null;

        if (value != null) {
            for (Iterator i = sessionIds.iterator(); i.hasNext(); ) {
                id = (String)i.next();

                if (value.indexOf(id) > -1) {
                    prefix = id;
                    value = trim(value, id + SESSION_ID_PREFIX, SESSION_ID_POSTFIX);

                    break;
                }
            }
        }

        return (value != null ? prefix + EQUAL + value : value);
    }

    private String parseSessionIdFromBody(String value) {
        final String SEMI_COLON = ";";
        final String QUOTE = "\"";
        final String QUESTION_MARK = "?";
        final String SINGLE_QUOTE = "'";
        final String GREATER_THAN = ">";

        if (value != null &&
            value.indexOf(SESSION_ID) > -1) {
            value = trim(value, SEMI_COLON + SESSION_ID.toLowerCase(), QUOTE);
            value = trim(value, null, QUESTION_MARK);
            value = trim(value, null, SINGLE_QUOTE);
            value = trim(value, null, GREATER_THAN);
        } else {
            value = null;
        }

        return value;
    }

    private String trim(String value, String prefix, String postfix) {
        StringBuffer sb = null;

        if (value != null &&
            value.length() > 0) {
            sb = new StringBuffer(value);

            int j = (prefix != null ?
                     sb.indexOf(prefix) + prefix.length() : 0);
            int k = (postfix != null ?
                     sb.indexOf(postfix, j + 1) : sb.length() - 1);

            sb = new StringBuffer(sb.substring(j, (k >= 0 ? k : sb.length())));
        }

        return (sb != null ? sb.toString() : null);
    }
}
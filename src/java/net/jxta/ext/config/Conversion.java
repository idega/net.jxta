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
 *  notice, this list of conditions and thproe following disclaimer in
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
 *  $Id: Conversion.java,v 1.1 2007/01/16 11:01:36 thomas Exp $
 */
package net.jxta.ext.config;

import java.awt.Color;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A type conversion utility.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class Conversion {

    private final static String BOGUS = "bogus";
    private final static char SLASH = '/';
    private final static char PERCENT = '%';
    private final static char COLON = ':';
    private final static char[] ESCAPE = { SLASH, COLON, '?', '#', '@', PERCENT };
    private final static String ENCODING_UTF8 = "UTF-8";
    private final static String URI_FILE = "file" + COLON + SLASH;

    /**
     * Conversion of a single character {@link java.lang.String} to it's
     * equivalent character represenation.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              character representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static char toChar(String s)
    throws ConversionException {
        char c = 0;

        try {
            c = s != null && s.length() >= 1 ?
                s.charAt(0) : BOGUS.charAt(0);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new ConversionException(ioobe);
        }

        return c;
    }
    
    /**
     * Conversion of a {@link java.lang.String} to it's int equivalent.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              int representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static int toInt(String s)
    throws ConversionException {
        int i = -1;

        try {
            i = Integer.parseInt(s != null ? s : BOGUS);
        } catch (NumberFormatException nfe) {
            throw new ConversionException(nfe);
        }

        return i;
    }

    /**
     * Conversion of a {@link java.lang.String} to it's long equivalent
     *
     * @param       s       provided {@link java.lang.String}
     * @return              long representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static long toLong(String s)
    throws ConversionException {
        long i = -1;

        try {
            i = Long.parseLong(s != null ? s : BOGUS);
        } catch (NumberFormatException nfe) {
            throw new ConversionException(nfe);
        }

        return i;
    }
    
    /**
     * Conversion of a {@link java.lang.String} to it's float equivalent.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              float representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */

    public static float toFloat(String s)
    throws ConversionException {
        float f = -1;

        try {
            f = Float.parseFloat(s != null ? s : BOGUS);
        } catch (NumberFormatException nfe) {
            throw new ConversionException(nfe);
        }

        return f;
    }
    
    /**
     * Conversion of a {@link java.lang.String} to it's boolean equivalent.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              boolean representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */

    public static boolean toBoolean(String s) {
        return Boolean.valueOf(s).booleanValue();
    }

    /**
     * Conversion of a {@link java.lang.String} to it's {@link java.net.URL}
     * equivalent.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              {@link java.net.URL} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static URL toURL(String s)
    throws ConversionException {
        URL u = null;

        try {
            u = new URL(s);
        } catch (MalformedURLException mue) {
            throw new ConversionException(mue);
        }

        return u;
    }
    
    /**
     * Conversion of a {@link java.io.File} to it's {@link java.net.URL}
     * equivalent.
     *
     * @param       f       provided {@link java.io.File}
     * @return              {@link java.net.URL} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static URL toURL(File f)
    throws ConversionException {
        String protocol = "file";
        String delimiter = ":";
        String prefix = "./";
        String filePrefix = System.getProperty("file.separator", "/");
        String s = f.toString();

        if (s.startsWith(filePrefix)) {
            s = prefix + s;
        }

        URL u = null;

        try {
            u = new URL(protocol + delimiter + s);
        } catch (MalformedURLException mue) {
            throw new ConversionException(mue);
        }

        return u;
    }

    /**
     * Conversion of a {@link java.util.List} of {@link java.lang.String} to
     * it's {@link java.util.List} of {@link java.net.URL} equivalent.
     *
     * @param       l       provided {@link java.util.List} of {@link java.util.String}
     * @return              {@link java.util.List} of {@link java.net.URL} represenations
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static List toURLs(List l)
    throws ConversionException {
        List urls = new ArrayList();

        for (Iterator i = l.iterator(); i.hasNext(); ) {
            urls.add(toURL((String)i.next()));
        }

        return urls;
    }
    
    /**
     * Conversion of a {@link java.lang.String} to it's {@link java.net.URI}
     * equivalent.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              {@link java.net.URI} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static URI toURI(String s)
    throws ConversionException {
        s = s != null ? s.trim() : s;

        URI u = null;

        if (s != null &&
            s.length() > 0) {
            s = s.replace(File.separatorChar, SLASH);

            try {
                s = URLEncoder.encode(s, ENCODING_UTF8);
            } catch (UnsupportedEncodingException usee) {}
            
            for (int i = 0; i < ESCAPE.length; i++) {
               s = s.replaceAll(PERCENT +
                    Integer.toString(ESCAPE[i], 16).toUpperCase(),
                    String.valueOf(ESCAPE[i]));
            }

            if (s.toLowerCase().startsWith(URI_FILE) &&
                s.length() > URI_FILE.length() + 1) {
                char c = s.charAt(URI_FILE.length());
                char d = s.charAt(URI_FILE.length() + 1);

                if (c == SLASH &&
                    d != SLASH) {
                    s = s.replaceFirst(URI_FILE, URI_FILE + SLASH);
                }
            }

            try {
                u = new URI(s);
            } catch (URISyntaxException use) {
                throw new ConversionException(use);
            }
        }

        return u;
    }
    
    /**
     * Conversion of a {@link java.util.List} of {@link java.lang.String} to
     * it's {@link java.util.List} of {@link java.net.URI} equivalent.
     *
     * @param       l       provided {@link java.util.List} of {@link java.lang.String}
     * @return              {@link java.util.List} of {@link java.net.URI} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static List toURIs(List l)
    throws ConversionException {
        List uris = new ArrayList();

        for (Iterator i = l.iterator(); i.hasNext(); ) {
            uris.add(toURI((String)i.next()));
        }

        return uris;
    }
    
    /**
     * Conversion of a {@link java.net.URI} of {@link java.lang.String} to it's
     * {@link java.io.File} equivalent.
     *
     * @param       u       provided {@link java.net.URI}
     * @return              {@link java.io.File} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static File toFile(URI u)
    throws ConversionException {
        File f = null;

        if (u != null) {
            String s = u.getPath();

            try {
                s = URLDecoder.decode(s, ENCODING_UTF8);
            } catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }

            f = new File(s);
        } else {
            throw new ConversionException("invalid file");
        }

        return f;
    }
    
    /**
     * Conversion of {@link java.lang.String} to it's equivalent
     * {@link java.awt.Color} represenation.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              {@link java.awt.Color} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static Color toColor(String s)
    throws ConversionException {
        String octalPrefix = "0";
        String hex1Prefix = "0x";
        String hex2Prefix = "#";
        int decimalRadix = 10;
        int octalRadix = 8;
        int hexRadix = 16;
        int radix = decimalRadix;
        int i = -1;
        Exception e = null;

        if (s != null) {
            String t = s.toLowerCase();

            if (t.startsWith(hex1Prefix)) {
                s = s.substring(hex1Prefix.length());
                radix = hexRadix;
            } else if (t.startsWith(hex2Prefix)) {
                s = s.substring(hex2Prefix.length());
                radix = hexRadix;
            } else if (t.startsWith(octalPrefix)) {
                s = s.substring(octalPrefix.length());
                radix = octalRadix;
            } else {
                radix = decimalRadix;
            }

            try {
                i = Integer.parseInt(s, radix);
            } catch (NumberFormatException nfe) {
                e = nfe;
            }
        }

        if (i == -1 ||
            e != null) {
            throw new ConversionException(e);
        }

        return new Color(i);
    }

    /**
     * Conversion of {@link java.lang.String} to it's equivalent
     * {@link java.lang.Color} represenation.
     *
     * @param       s       provided {@link java.lang.String}
     * @return              {@link java.lang.Class} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static Class toClass(String s)
    throws ConversionException {
        Class c = null;

        try {
            c = Class.forName(s);
        } catch (ClassNotFoundException cnfe) {
            throw new ConversionException(cnfe);
        }

        return c;
    }

    /**
     * Conversion of {@link java.util.List} of {@link java.lang.String} to it's
     * {@link java.util.List} of {@link java.lang.Class} equivalent.
     *
     * @param       l       provided {@link java.util.List} of {@link java.lang.String}
     * @return              {@link java.util.List} of {@link java.lang.Class} representation
     * @throws      ConversionException {@link net.jxta.ext.config.ConversionException}
     *                                  thrown when a conversion error occurs
     */
    
    public static List toClasses(List l)
    throws ConversionException {
        List classes = new ArrayList();

        for (Iterator i = l.iterator(); i.hasNext(); ) {
            classes.add(toClass((String)i.next()));
        }

        return classes;
    }
}

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
 *  $Id: Resource.java,v 1.1 2007/01/16 11:01:37 thomas Exp $
 */
package net.jxta.ext.config;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jaxen.XPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Content;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Text;

/**
 * XML resource accessor utility.
 *
 * @author     james todd [gonzo at jxta dot org]
 */

public class Resource {

    private final static String ROOT = "ROOT";
    private final static String PATH_DELIMITER = "/";
    private final static String FILE_SEPERATOR =
        System.getProperty("file.separator", "/");
    private final static String EXCEPTION_PREFIX =
        "unable to find resource: ";
    private final static Object lock = new Object();
    private final static boolean VERBOSE = false;

    private List resources = null;
    private Element root = null;

    /**
     * Convenience {@link java.net.URL} conversion method.
     *
     * @param       s       specified {@link java.net.URL} in the form of a
     *                      {@link java.lang.String}
     * @return              {@link java.net.URL} representaion of the specified
     *                      parameter
     */
    
    public static URL toURL(String s)
    throws ConversionException {
        return Conversion.toURL(s);
    }

    /**
     * Convenience {@link java.net.URL} conversion method.
     *
     * @param       f       specified {@link java.net.URL} in the form of a
     *                      {@link java.io.File}
     * @return              {@link java.net.URL} representation of the specified
     *                      parameter.
     */
    
    public static URL toURL(File f)
    throws ConversionException {
        return Conversion.toURL(f);
    }
    
    /**
     * Default constructor.
     */
    
    public Resource() {
        this(new Element(ROOT));
    }
    
    /**
     * Constructor which overrides the default backing document root.
     *
     * @param       root        root element name
     */
    
    public Resource(String root) {
        this(new Element(root));
    }

    /**
     * Loads the specified resource.
     *
     * @param       resource        resource {@link java.net.URL} address
     */
    
    public void load(URL resource)
    throws ResourceNotFoundException {
        process(resource);
    }
    
    /**
     * Loads the specified resource.
     *
     * <p>If the specified resource fails to convert to a {@link java.net.URL}
     * a series of stream instantiation will be attempted.
     *
     * @param       resource        specified resource
     * @throws      {@link net.jxta.ext.config.ResourceNotFoundException}   thrown if the specified
     *                                                  resource is not resolvable
     */
    
    public void load(String resource)
    throws ResourceNotFoundException {
        Exception e = null;

        try {
            load(Resource.toURL(resource));
        } catch (ConversionException ce) {
            e = ce;
        }
        catch (ResourceNotFoundException rnfe) {
            e = rnfe;
        }

        if (e != null) {
            e = null;

            try {
                process(resource);
            } catch (ResourceNotFoundException rnfe) {
                if (!resource.startsWith(FILE_SEPERATOR)) {
                    process(FILE_SEPERATOR + resource);
                }
            }
        }
    }

    /**
     * Loads the specified resource.
     *
     * @param       resource    specified resource as a {@link java.io.File}
     * @throws      {@link net.jxta.ext.config.ResourceNotFoundException}    thrown if the specified resource
     *                                              is not resolvable.
     * @throws      {@link java.net.MalformedURLException}  thrown if the specified
     *                                                  resource can not be converted
     *                                                  to a {@link java.net.URL}
     */
    
    public void load(File resource)
    throws ResourceNotFoundException, MalformedURLException {
        load(resource.toURL());
    }

    /**
     * Loads the specified resource relative to the provided {@link java.lang.Class}
     *
     * @param       resource        specified resource
     * @param       clazz           {@link java.lang.Class} used to load the resource
     * @throws      {@link net.jxta.ext.config.ResourceNotFoundException}   thrown if the specified
     *                                                  resource is not resolvable.
     */
    
    public void load(String resource, Class clazz)
    throws ResourceNotFoundException {
//        try {
//            load(resource, clazz);
//        } catch (ResourceNotFoundException rnfe) {
            process(resource, clazz);
//        }
    }

    /**
     * Loads the specified resource.
     *
     * @param       is      resource specified as a {@link java.io.InputStream}
     * @throws      {@link net.jxta.ext.config.ResourceNotFoundException}   thrown if the specified
     *                                                  resource is not resolvable.
     */
    
    public void load(InputStream is)
    throws ResourceNotFoundException {
        process(is);
    }

    /**
     * Accessor for the named resource as a {@link java.io.InputStream}
     *
     * @param       resource     name of the resource
     * @return      {@link java.io.InputStream}     a {@link java.io.InputStream}
     *                                              for the specified resource.
     */
    
    public InputStream getResourceAsStream(String resource) {
        return getResourceAsStream(resource, null);
    }
    
    /**
     * Accessor for the named resource as a {@link java.io.InputStream}
     *
     * @param       resource    name of the resource
     * @param       clazz       {@link java.lang.Class} used to load the resource.
     */

    public InputStream getResourceAsStream(String resource, Class clazz) {
        if (clazz == null) {
            clazz = Resource.class;
        }

        return clazz.getResourceAsStream(resource);
    }
    
    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as a {@link java.lang.String} or null
     *                      if the named resource is not resolvable
     */

    public String get(String key) {
        return get(key, null);
    }

    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a {@link java.lang.String} or the
     *                      specified default value if the named resource is not
     *                      resolvable
     */
    
    public String get(String key, String d) {
        return getValue(key, d);
    }

    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as an int
     * @throws              {link ConversionException}  thrown in the event the
     *                      named resource is not convertible to an integer or
     *                      is not resolvable.
     */
    
    public int getInt(String key)
    throws ConversionException {
        return getInt(key, null);
    }

    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as an int or the specified default
     *                      value if the named resource is not resolvable.
     */
    
    public int getInt(String key, int d) {
        int i = 0;

        try {
            i = getInt(key, new Integer(d));
        } catch (ConversionException ce) {}

        return i;
    }

    /**
     * Accessor for the named rsource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as an int or the specified default
     *                      value if the named resource is not resolvable.
     * @throws      {@link net.jxta.ext.config.ConverstionException}    thrown in the event the named
     *                                              resource is not convertible to
     *                                              an int or is not resolvable.
     */
    
    public int getInt(String key, Integer d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toInt(s);
    }

    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as a long
     * @exception   {@link net.jxta.ext.config.ConverstionException}    thrown in the event the named
     *                                              resource is not convertible to
     *                                              a long or is not resolvable.
     */
    
    public long getLong(String key)
    throws ConversionException {
        return getLong(key, null);
    }
    
    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a long or the specified default
     *                      value if the named resource is not covertible to a
     *                      long or is not resovlable.
     */
    
    public long getLong(String key, long d) {
        long i = 0;

        try {
            i = getLong(key, new Long(d));
        } catch (ConversionException ce) {}

        return i;
    }
   
    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a long or the specified default
     *                      value if the named resource is not covertible to a
     *                      long or is not resovlable.
     */
    
    public long getLong(String key, Long d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toLong(s);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as a float
     * @throws      {@link net.jxta.ext.config.ConverstionException}    thrown in the event the named
     *                                              resource is not convertible to
     *                                              a float or is not resolvable.
     */
    
    public float getFloat(String key)
    throws ConversionException {
        return getFloat(key, null);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a float or the specified default
     *                      value  the named resource is not covertible to a
     *                      long or is not resovlable.
     * @throws      {@link net.jxta.ext.config.ConversionException}     thrown in the event the named
     *                                              resource is not convertible to
     *                                              a float or is not resolvable.
     */
    
    public float getFloat(String key, Float d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toFloat(s);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as a boolean
     */
    
    public boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a boolean or the default value if
     *                      the named resourse is not convertible to a boolean
     *                      or is not resolvable.
     */
    
    public boolean getBoolean(String key, boolean d) {
        return getBoolean(key, Boolean.valueOf(d));
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a boolean or the default value if
     *                      the named resources is not convertible to a boolean
     *                      or is not resolvable.
     */
    
    public boolean getBoolean(String key, Boolean d) {
        String s = get(key, (d != null) ? d.toString() : null);

        return Boolean.valueOf(s).booleanValue();
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a char
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public char getChar(String key)
    throws ConversionException {
        return getChar(key, new Character((char)0));
    }

    /**
     * Accessor for a named resource
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a char or the default value if the named
     *                      resource is not convertible to a char or is not
     *                      resolvable.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public char getChar(String key, Character d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toChar(s);
    }

    /**
     * Accessor for a named resource
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.net.URL}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public URL getURL(String key)
    throws ConversionException {
        return getURL(key, null);
    }

    /**
     * Accessor for a named resource
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.net.URL} or the default
     *                      value if the named resource is not convertible to
     *                      a {@link java.net.URL} or is not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public URL getURL(String key, URL d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toURL(s);
    }

    /**
     * Accessor for a named resource
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.util.List} of {@link java.net.URL}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public List getURLs(String key)
    throws ConversionException {
        return getURLs(key, null);
    }

    /**
     * Accessor for a named resource
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.util.List} of {@link java.net.URL}
     *                      or the specified default value if the named resource
     *                      is not convertible to a {@link java.net.URL} or is
     *                      not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public List getURLs(String key, URL d)
    throws ConversionException {
        List values = getAll(key, (d != null) ? d.toString() : null);

        return Conversion.toURLs(values);
    }

    /**
     * Accessor for the named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.net.URI}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public URI getURI(String key)
    throws ConversionException {
        return getURI(key, null);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.net.URI} or the default
     *                      value if the named resource is not convertible to
     *                      a {@link java.net.URI} or is not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public URI getURI(String key, URI d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toURI(s);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.util.List} of {@link java.net.URI}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public List getURIs(String key)
    throws ConversionException {
        return getURIs(key, null);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.util.List} of {@link java.net.URI}
     *                      or the specified default value if the named resource
     *                      is not convertible to a {@link java.net.URI} or is
     *                      not resolvible.
     * @exception   {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public List getURIs(String key, URI d)
    throws ConversionException {
        List values = getAll(key, (d != null) ? d.toString() : null);

        return Conversion.toURIs(values);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.awt.Color}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public Color getColor(String key)
    throws ConversionException {
        return getColor(key, null);
    }

    /**
     * Accessor for a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.util.Color} or the specified
     *                      default value if the named resource is not convertible
     *                      to a {@link java.awt.Color} or is not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible or
     *                                          is not resolvable.
     */
    
    public Color getColor(String key, Color d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.toString() : null);

        return Conversion.toColor(s);
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @return              named {@link net.jxta.ext.config.Resource}
     */
    
    public Resource getResource(String key) {
        List r = getResources(key);

        return r != null ? (Resource)r.get(0) : null;
    }

    /**
     * Accessor to the resources.
     *
     * @param       key     specified resource name
     * @return              named {@link net.jxta.ext.config.Resource} as a {@link java.util.List}
     */
    
    public List getResources(String key) {
        List r = new ArrayList();

        for (Iterator v = getValues(key).iterator(); v.hasNext(); ) {
            Object o = v.next();

            if (o instanceof Element) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();

                try {
                    new XMLOutputter().output(new Document((Element)o), buf);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                Resource cr = new Resource();

                try {
                    cr.load(new ByteArrayInputStream(buf.toByteArray()));
                } catch (ResourceNotFoundException rnfe) {
                    rnfe.printStackTrace();
                }

                r.add(cr);
            }
        }

        return r;
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.lang.Class}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible to
     *                                          a {@link java.lang.Class} or is
     *                                          not resolvable.
     */
    
    public Class getClass(String key)
    throws ConversionException {
        return getClass(key, null);
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.lang.Class} or the default
     *                      value if the named resource is not converibile to a
     *                      {@link java.lang.Class} or is not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible to
     *                                          a {@link java.lang.Class} or is
     *                                          not resolvable.
     */
    
    public Class getClass(String key, Class d)
    throws ConversionException {
        String s = get(key, (d != null) ? d.getName() : null);

        return Conversion.toClass(s);
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @return              resource as a {@link java.util.List} of {@link java.lang.Class}
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible to
     *                                          a {@link java.lang.Class} or is
     *                                          not resolvable.
     */
    
    public List getClasses(String key)
    throws ConversionException {
        return getClasses(key, null);
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource as a {@link java.util.List} of {@link java.lang.Class}
     *                      or the specified default value if the named resource
     *                      is not convertible to a {@link java.lang.Class} or is
     *                      not resolvible.
     * @throws      {@link net.jxta.ext.config.ConversionException} thrown in the event the named
     *                                          resource is not convertible to
     *                                          a {@link java.lang.Class} or is
     *                                          not resovlable.
     */
    
    public List getClasses(String key, URL d)
    throws ConversionException {
        List values = getAll(key, (d != null) ? d.toString() : null);

        return Conversion.toClasses(values);
    }

    /**
     * Specifies a named resource to the null value.
     *
     * @param       key     specified resource name
     */
    
    public void set(String key) {
        set(key, null);
    }

    /**
     * Specifies a named resource to the provided value.
     *
     * @param       key     specified resource name
     * @param       value   specified resource value
     */
    
    public void set(String key, String value) {
        synchronized (this.root) {
            Object o = getValue(key, false);

            if (o != null) {
                if (o instanceof Attribute) {
                    Attribute a = (Attribute)o;
                    Element p = a.getParent();

                    if (value != null) {
                        p.setAttribute(a.detach().setValue(value));
                    } else {
                        p.removeAttribute(a);
                    }
                } else if (o instanceof Element) {
                    Element e = (Element)o;

                    if (value != null) {
                        List l = new ArrayList();

                        l.add(new Text(value));

                        e.setContent(l);
                    } else {
                        e.getParent().removeContent(e);
                    }
                }
            } else {
                // xxx: add it ... oh my
            }
        }
    }

    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @return              resource value as a {@link java.util.List}
     */
    
    public List getAll(String key) {
        return getAll(key, null);
    }
    
    /**
     * Accessor to a named resource.
     *
     * @param       key     specified resource name
     * @param       d       specified default value
     * @return              resource value as a {@link java.util.List} or the
     *                      specified default value if the named resource value
     *                      is not resolvible.
     */

    public List getAll(String key, String d) {
        return getValues(key, d);
    }

    /**
     * Resource containment validator. 
     *
     * @param       key     specified resource name
     * @return              containment test results
     */
    
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Accessor to the {@link java.lang.String} representation of this instance.
     *
     * @return              {@link java.lang.String} representation of this instance
     */
    
    public String toString() {
        String s = null;

        if (this.root != null) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            List c = this.root.getChildren();
            Element r = (Element)c.get(0);

            try {
                new XMLOutputter(Format.getPrettyFormat()).output(new Document((Element)r.clone()),
                    buf);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            s = buf.toString();
        }

        return s != null ? s : "null";
    }

    private Resource(Element root) {
        this.resources = new ArrayList();
        this.root = root;
    }
     
    private void process(Object resource)
    throws ResourceNotFoundException {
        process(resource, null);
    }

    private void process(Object resource, Class clazz)
    throws ResourceNotFoundException {
        if (!this.resources.contains(resource)) {
            Element el = null;
            Exception ex = null;

            if (resource instanceof URL) {
                try {
                    el = getRootElement((URL)resource);
                } catch (JDOMException jde) {
                    ex = jde;
                }
                catch (IOException ioe) {
                    ex = ioe;
                }
            } else if (resource instanceof String) {
                if (clazz == null) {
                    clazz = Resource.class;
                }

                try {
                    el = getElement(clazz.getResourceAsStream((String)resource));
                } catch (JDOMException jde) {
                    jde.printStackTrace();
                    
                    ex = jde;
                } catch (IOException ioe) {
                    ex = ioe;
                }
            } else if (resource instanceof InputStream) {
                try {
                    el = getElement((InputStream)resource);
                } catch (JDOMException jde) {
                    ex = jde;
                } catch (IOException ioe) {
                    ex = ioe;
                }

                resource = new String("InputStream." +
                    System.currentTimeMillis());
            }

            if (el != null &&
                ex == null) {
                load(resource, el);
            } else {
                if (VERBOSE) {
                    ex.printStackTrace();
                }

                throw new ResourceNotFoundException(EXCEPTION_PREFIX +
                                                    ": " +
                                                    resource.toString(), ex);
            }
        }
    }

    private Element getElement(InputStream is)
    throws JDOMException, IOException {
        Element element = null;

        try {
            element = getRootElement(is);
        } catch (JDOMException jde) {
            throw jde;
        }
        catch (IOException ioe) {
            throw ioe;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {}
            }
        }

        return element;
    }

    private Element getRootElement(URL resource)
    throws JDOMException, IOException {
        return new SAXBuilder().build((URL)resource).getRootElement();
    }

    private Element getRootElement(InputStream is)
    throws JDOMException, IOException {
        return new SAXBuilder().build(is).getRootElement();
    }

    private void load(Object resource, Element element) {
        if (resources != null &&
            element != null) {
            synchronized (lock) {
                this.resources.add(resource);
                this.root.addContent((Content)element.clone());
            }
        }
    }

    private String getValue(String key, String d) {
        String v = null;
        Object o = getValue(key);

        if (o != null) {
            if (o instanceof Attribute) {
                v = ((Attribute)o).getValue();
            } else if (o instanceof Element) {
                v = ((Element)o).getTextTrim();
            }
        }
        
        return Util.expand(v != null ? v : d);
    }

    private Object getValue(String key) {
        return getValue(key, true);
    }

    private Object getValue(String key, boolean isClone) {
        Object o = null;
        XPath xp = getXP(key);

        if (xp != null) {
            try {
                o = xp.selectSingleNode(getRootDocument(isClone));
            } catch (JaxenException je) {
                if (VERBOSE) {
                    je.printStackTrace();
                }
            }
        }

        return o;
    }

    private List getValues(String key, String d) {
        List v = new ArrayList();
        Object o = null;

        for (Iterator n = getValues(key).iterator(); n.hasNext(); ) {
            o = n.next();

            if (o != null) {
                if (o instanceof Attribute) {
                    v.add(((Attribute)o).getValue());
                } else if (o instanceof Element) {
                    v.add(((Element)o).getTextTrim());
                }
            }
        }

        if (v.size() == 0 &&
            d != null) {
            v.add(d);
        }

        return v;
    }

    private List getValues(String key) {
        List v = new ArrayList();
        XPath xp = getXP(key);

        if (xp != null) {

            try {
                for (Iterator n = xp.selectNodes(getRootDocument()).iterator();
                     n.hasNext(); ) {
                    v.add(n.next());
                }
            } catch (JaxenException je) {
                if (VERBOSE) {
                    je.printStackTrace();
                }
            }
        }

        return v;
    }

    private XPath getXP(String key) {
        XPath xp = null;

        try {
            xp = new JDOMXPath(PATH_DELIMITER + root.getName() + PATH_DELIMITER + key);
        } catch (JaxenException je) {
            if (VERBOSE) {
                je.printStackTrace();
            }
        }

        return xp;
    }

    private Document getRootDocument() {
        return getRootDocument(true);
    }

    private Document getRootDocument(boolean isClone) {
        Document d = null;

        if (isClone) {
            d = new Document((Element)this.root.clone());
        } else if ((d = this.root.getDocument()) == null) {
            d = new Document(this.root);
        }

        return d;
    }
}

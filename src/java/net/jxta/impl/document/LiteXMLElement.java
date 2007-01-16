/*
 * $Id: LiteXMLElement.java,v 1.1 2007/01/16 11:01:29 thomas Exp $
 ********************
 *
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 ********************
 */

package net.jxta.impl.document;


import java.io.Writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.document.TextElement;


/**
 * An element of a <CODE>StructuredDocument</CODE>. <CODE>StructuredDocument</CODE>s
 * are made up of hierarchies of elements. LiteXMLElement is part of an implementation
 * while makes use of XML-style document conventions, but without the overhead of a
 * full parser.
 *
 */
public class LiteXMLElement extends XMLElementCommon {
    
    /**
     * Defines a range of characters, probably within a string. The range is
     * deemed to be invalid if 'start' is -1.  A zero length range is, by 
     * convention, described by an 'end' value of 'start' - 1.
     */
    protected static class charRange implements Comparable {
        
        /**
         *  Contains the start position of this range.
         **/
        public int start;
        
        /**
         * Contains the end position of this range. one weird thing: if end == start -1,
         * then the item is of zero length begining at start.
         **/
        public int end;
        
        /**
         * Constructor for a null charRange.
         **/
        public charRange() {
            start = -1;
            end = -1;
        }
        
        /**
         * Constructor for which the bounds are specified.
         *
         **/
        public charRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        /**
         * {@inheritDoc}
         **/
        public boolean equals(Object aRange) {
            if (this == aRange) {
                return true;
            }
            
            if (!(aRange instanceof charRange)) {
                return false;
            }
            
            charRange someRange = (charRange) aRange;
            
            return (start == someRange.start) && (end == someRange.end);
        }
        
        /**
         * {@inheritDoc}
         **/
        public int compareTo(Object aRange) {
            if (this == aRange) {
                return 0;
            }
            
            if (!(aRange instanceof charRange)) {
                throw new ClassCastException("type mismatch error");
            }
            
            charRange someRange = (charRange) aRange;
            
            if (start < someRange.start) {
                return -1;
            }
            
            if (start > someRange.start) {
                return 1;
            }
            
            if (end < someRange.end) {
                return -1;
            }
            
            if (end > someRange.end) {
                return 1;
            }
            
            return 0;
        }
        
        /**
         * {@inheritDoc}
         **/
        public String toString() {
            return "[" + start + "," + end + "]";
        }
        
        /**
         * Returns true if the <CODE>charRange</CODE> specified by someRange is
         * contained within this range.
         *
         * @param someRange The range which must be contained within this range.
         *
         * @return true if the specified range is contained with this range otherwise false.
         **/
        public boolean contains(charRange someRange) {
            return(isValid() && someRange.isValid() && (start <= someRange.start) && (end >= someRange.end));
        }
        
        /**
         * Returns true if the <CODE>tagRange</CODE> specified by someRange is
         * contained within this range.
         *
         * @param someRange The range which must be contained within this range.
         *
         * @return true if the specified range is contained with this range otherwise false.
         **/
        public boolean contains(tagRange someRange) {
            return(isValid() && someRange.isValid() && (start <= someRange.startTag.start) && (end >= someRange.endTag.end));
        }
        
        /**
         *  Returns true if the location specified is contained in this range.
         *
         *  @param someLoc the location which is to be tested.
         *  @return true if the location is in this range, otherwise false.
         */
        public boolean contains(int someLoc) {
            return(isValid() && (someLoc >= 0) && (start <= someLoc) && (end >= someLoc));
        }
        
        /**
         * Returns true if the range is both non-null and has a length of greater
         * than or equal to zero.
         *
         * @return true if the range is a valid one, otherwise false.
         *
         **/
        public boolean isValid() {
            return length() >= 0;
        }
        
        /**
         * Returns the length of this range.
         * @return The length of the range or -1 if the range is null.
         **/
        public int length() {
            if ((-1 == start) || (-1 == end)) {
                return -1;
            }
            
            return (end - start + 1);
        }
    }
    

    /**
     * A tagRange is a collection of char ranges useful for describing XML
     * structures.
     *
     * <p/><dl>
     * <dt><code>startTag</code></dt>
     * <dd>The range of the opening tag, ie. &lt;tag></dd>
     *  <dt><code>body</code></dt>
     *  <dd>Everything between <code>startTag</code> and <code>endTag</code>.</dd>
     *  <dt><code>endTag</code></dt>
     * <dd>The range of the terminating tag, ie. &lt;/tag>.</dd>
     * </dl>
     *
     * <p/>For empty-element tags the <code>startTag</code>, <code>body</code>
     * and <code>endTag</code> will be equal.
     **/
    protected static class tagRange implements Comparable {
        public charRange startTag;
        public charRange body;
        public charRange endTag;
        
        public tagRange() {
            startTag = new charRange();
            body = new charRange();
            endTag = new charRange();
        }
        
        public tagRange(charRange startTag, charRange body, charRange endTag) {
            this.startTag = startTag;
            this.body = body;
            this.endTag = endTag;
        }
        
        /**
         * {@inheritDoc}
         **/
        public boolean equals(Object aRange) {
            if (this == aRange) {
                return true;
            }
            
            if (!(aRange instanceof tagRange)) {
                return false;
            }
            
            tagRange likeMe = (tagRange) aRange;
            
            return startTag.equals(likeMe.startTag) && body.equals(likeMe.body) && endTag.equals(likeMe.endTag);
        }
        
        /**
         * {@inheritDoc}
         **/
        public int compareTo(Object aRange) {
            if (this == aRange) {
                return 0;
            }
            
            if (!(aRange instanceof tagRange)) {
                throw new ClassCastException("type mismatch error");
            }
            
            tagRange someRange = (tagRange) aRange;
            
            int compared = startTag.compareTo(someRange.startTag);
            
            if (0 != compared) {
                return compared;
            }
            
            return endTag.compareTo(someRange.endTag);
        }
        
        /**
         * {@inheritDoc}
         **/
        public String toString() {
            return startTag + ":" + body + ":" + endTag;
        }
        
        /**
         * Returns true if the <CODE>tagRange</CODE> specified by someRange is
         * contained within the body portion of this range.
         *
         * @param someRange The range which must be contained within this range.
         *
         * @return true if the specified range is contained with this range
         * otherwise false.
         */
        public boolean contains(tagRange someRange) {
            return(isValid() && someRange.isValid() && (body.start <= someRange.startTag.start) && (body.end >= someRange.endTag.end));
        }
        
        /**
         * Returns true if the <CODE>charRange</CODE> specified by someRange is
         * contained within the body portion of this range.
         *
         * @param someRange The range which must be contained within this range.
         *
         * @return true if the specified range is contained with this range
         * otherwise false.
         */
        public boolean contains(charRange someRange) {
            return(isValid() && someRange.isValid() && (body.start <= someRange.start) && (body.end >= someRange.end));
        }
        
        /**
         *  Returns <code>true</code> if this tagRange represents and empty
         *  element.
         **/
        public boolean isEmptyElement() {
            return isValid() && startTag.equals(body) && startTag.equals(endTag);
        }
        
        /**
         *
         **/
        public boolean isValid() {
            return (null != startTag) && (null != body) && (null != endTag) && startTag.isValid() && body.isValid() && endTag.isValid();
        }
    }
    
    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger(LiteXMLElement.class.getName());
    
    /**
     * If true then every operation which modifies the state of the document will
     * perform a consistency check. This is a deadly performance killer but
     * helps a lot in isolating bugs.
     **/
    protected final static transient boolean paranoidConsistencyChecking = false;
    
    /**
     * Causes additional logging while parsing. This is a deadly performance
     * killer but helps a lot in isolating bugs.
     **/
    protected final static transient boolean verboseLogging = false;
    
    /**
     * The document associated with this Element.
     */
    protected final transient LiteXMLDocument doc;
    
    /**
     *  Identifies the element which is the parent of this element. If <code>
     *  this.parent == this</code> then this element is the root of the document.
     *  If <code>null == parent</code> then this element has not yet been
     *  inserted into the document.
     */
    protected transient Element parent;
    
    /**
     *  The portion of the source XML associated with this node
     */
    protected transient tagRange loc;
    
    /**
     *  If this node has yet to be inserted into the document then will contain
     *  the String value of this node, otherwise null.
     */
    private transient StringBuffer uninserted = null;
    
    /**
     * The child elements associated with this element
     */
    private final transient List children = new ArrayList();
    
    /**
     * Creates new LiteXMLElement
     *
     * @param loc The location of the element within the document.
     * @param doc The {@link LiteXMLDocument} which is the root of the document.
     **/
    protected LiteXMLElement(LiteXMLDocument doc, tagRange loc) {
        this.doc = doc;
        this.loc = loc;
    }
    
    /**
     * Creates new LiteElement
     *
     * @param doc The {@link LiteXMLDocument} which is the root of the document.
     * @param name The name of the element being created.
     * @param val The value of the element being created or null if there is no
     *  content to the element.
     **/
    public LiteXMLElement(LiteXMLDocument doc, final String name, final String val) {
        this(doc, new tagRange());
        
        for (int eachChar = name.length() - 1; eachChar >= 0; eachChar--) {
            if (Character.isWhitespace(name.charAt(eachChar))) {
                throw new IllegalArgumentException("Element names may not contain spaces.");
            }
        }
        
        if ((null == val) || (0 == val.length())) {
            uninserted = new StringBuffer("<" + name + "/>");
        } else {
            uninserted = new StringBuffer(val);
            encodeEscaped(uninserted);
            uninserted.insert(0, "<" + name + ">");
            uninserted.append("</" + name + ">");
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public boolean equals(Object element) {
        if (this == element) {
            return true;
        }
        
        if (!(element instanceof LiteXMLElement)) {
            return false;
        }
        
        LiteXMLElement liteElement = (LiteXMLElement) element;
        
        if (getDocument() != liteElement.getDocument()) {
            return false;
        }
        
        if (!getName().equals(liteElement.getName())) {
            return false;
        }
        
        String val1;

        if (null != uninserted) {
            val1 = uninserted.toString();
        } else {
            val1 = getTextValue();
        }
        
        String val2 = liteElement.getTextValue();
        
        if ((null == val1) && (null == val2)) {
            return true;
        }
        
        if ((null == val1) || (null == val2)) {
            return false;
        }
        
        return val1.equals(val2);
    }
    
    /**
     *  {@inheritDoc}
     *
     *  <p/>A toString implementation for debugging purposes.
     **/
    public String toString() {
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        String name = getName();

        if (name == null) {
            name = "<<null name>>";
        }
        String value = getTextValue();

        if (value == null) {
            value = "<<null value>>";
        }
        
        if ((value.length() + name.length()) >= 60) {
            int len = Math.max(20, 60 - name.length());

            value = value.substring(0, Math.min(len, value.length()));
        }
        
        // FIXME 20021125 bondolo@jxta.org should remove carriage control.
        
        return super.toString() + " / " + name + " = " + value;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public StructuredDocument getRoot() {
        return getDocument();
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Element getParent() {
        return parent;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getChildren() {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        return Collections.enumeration(children);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getName() {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        int current = loc.startTag.start + 1;

        while (current <= loc.startTag.end) {
            char inTagName = getDocument().docContent.charAt(current);
            
            if (Character.isWhitespace(inTagName) || ('/' == inTagName) || ('>' == inTagName)) {
                break;
            }
            
            current++;
        }
        
        return getDocument().docContent.substring(loc.startTag.start + 1, current);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public void appendChild(TextElement element) {
        if (!(element instanceof LiteXMLElement)) {
            throw new IllegalArgumentException("Element type not supported.");
        }
        
        LiteXMLElement newElement = (LiteXMLElement) element;
        
        if (newElement.getDocument() != getDocument()) {
            throw new IllegalArgumentException("Wrong document");
        }
        
        if (null != newElement.parent) {
            throw new IllegalArgumentException("New element is already in document");
        }
        
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        // If uninserted then this new element contains content which needs to
        // be added to the document. If uninserted is null then the child
        // element's content is already in the document, but merely needs to
        // be recognized as a child.
        if (null != newElement.uninserted) {
            if (loc.startTag.equals(loc.endTag)) {
                getDocument().docContent.deleteCharAt(loc.endTag.end - 1); // delete the /
                loc.startTag.end -= 1;
                
                // skip past the name portion
                int current = loc.startTag.start + 1;

                while (current <= loc.startTag.end) {
                    char inTagName = getDocument().docContent.charAt(current);
                    
                    if (Character.isWhitespace(inTagName) || ('>' == inTagName)) {
                        break;
                    }
                    
                    current++;
                }
                
                String tagName = getDocument().docContent.substring(loc.startTag.start + 1, current);

                getDocument().docContent.insert(loc.startTag.end + 1, "</" + tagName + ">");
                getDocument().adjustLocations(loc.startTag.end + 1, tagName.length() + 2);
                loc.endTag = new charRange(loc.startTag.end + 1, loc.startTag.end + 3 + tagName.length());
                loc.body = new charRange(loc.startTag.end + 1, loc.startTag.end);
            }
            
            getDocument().docContent.insert(loc.endTag.start, newElement.uninserted);
            
            newElement.loc.startTag.start = loc.endTag.start;
            newElement.loc.startTag.end = getDocument().docContent.indexOf(">", newElement.loc.startTag.start);
            
            if ('/' != newElement.uninserted.charAt(newElement.uninserted.length() - 2)) {
                newElement.loc.body.start = newElement.loc.startTag.end + 1;
                
                newElement.loc.endTag.end = newElement.loc.startTag.start + newElement.uninserted.length() - 1;
                newElement.loc.endTag.start = getDocument().docContent.lastIndexOf("<", newElement.loc.endTag.end);
                
                newElement.loc.body.end = newElement.loc.endTag.start - 1;
            } else {
                newElement.loc.body = new charRange(newElement.loc.startTag.start, newElement.loc.startTag.end);
                newElement.loc.endTag = new charRange(newElement.loc.startTag.start, newElement.loc.startTag.end);
            }
            
            if (0 != loc.body.length()) {
                getDocument().adjustLocations(loc.endTag.start, newElement.uninserted.length());
            } else {
                loc.body.start--;
                getDocument().adjustLocations(loc.endTag.start, newElement.uninserted.length());
                loc.body.start++;
            }
            
            loc.body.end += newElement.uninserted.length();
            
            newElement.uninserted = null;
        }
        
        newElement.parent = this;
        children.add(newElement);
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Enumeration getChildren(String name) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        List result = new ArrayList();

        for (Iterator eachChild = children.iterator(); eachChild.hasNext();) {
            TextElement aChild = (TextElement) eachChild.next();
            
            if (name.equals(aChild.getName())) {
                result.add(aChild);
            }
        }
        
        return Collections.enumeration(result);
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String getTextValue() {
        return getTextValue(false, true);
    }
    
    /**
     *  Get the value (if any) associated with an element.
     *
     *  @param getEncoded if true then the contents will be encoded such that
     *      the contents will not be interpreted as XML. see
     *      {@link <a href="http://www.w3.org/TR/REC-xml#syntax">W3C XML 1.0 Specification</a>}
     *      ie. < -> &lt; & -> &amp;
     *  @return A string containing the value of this element, if any, otherwise null.
     */
    protected String getTextValue(boolean getEncoded, boolean trim) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        StringBuffer building = new StringBuffer();
        
        List ranges = new ArrayList();
        
        /*
         * insert the ranges of the children in order. insertion method is ok
         * because the number of children is usually less than 10 or so.
         */
        for (Enumeration eachChild = getChildren(); eachChild.hasMoreElements();) {
            LiteXMLElement aChild = (LiteXMLElement) eachChild.nextElement();
            charRange childsRange = new charRange(aChild.loc.startTag.start, aChild.loc.endTag.end);
            
            // find where to insert.
            for (int eachRange = 0; eachRange < ranges.size(); eachRange++) {
                charRange rangeChild = (charRange) ranges.get(eachRange);

                if (1 == rangeChild.compareTo(childsRange)) {
                    ranges.set(eachRange, childsRange);
                    childsRange = rangeChild;
                }
            }
            ranges.add(childsRange);
        }
        
        int current = loc.body.start;
        Iterator eachRange = ranges.iterator();
        
        // add all the text not part of some child
        while (eachRange.hasNext()) {
            charRange aRange = (charRange) eachRange.next();
            
            building.append(getDocument().docContent.substring(current, aRange.start));
            
            current = aRange.end + 1;
        }
        
        // Add the last bit.
        building.append(getDocument().docContent.substring(current, loc.endTag.start));
        
        if (!getEncoded) {
            building = decodeEscaped(building);
        }
        
        // trim
        int firstNonWhiteSpace = 0;
        int lastNonWhiteSpace = building.length() - 1;
        
        if (trim) {
            while (firstNonWhiteSpace < building.length()) {
                char possibleSpace = building.charAt(firstNonWhiteSpace);

                if (!Character.isWhitespace(possibleSpace)) {
                    break;
                }
                
                firstNonWhiteSpace++;
            }
            
            // did we find no non-whitespace?
            if (firstNonWhiteSpace >= building.length()) {
                return null;
            }
            
            while (lastNonWhiteSpace >= firstNonWhiteSpace) {
                char possibleSpace = building.charAt(lastNonWhiteSpace);

                if (!Character.isWhitespace(possibleSpace)) {
                    break;
                }
                
                lastNonWhiteSpace--;
            }
        }
        
        String result = building.substring(firstNonWhiteSpace, lastNonWhiteSpace + 1);
        
        return result;
    }
    
    /**
     *  Write the contents of this element and optionally its children. The
     *  writing is done to a provided <code>java.io.Writer</code>. The writing 
     *  can optionally be indented.
     *
     *  @param into The java.io.Writer that the output will be sent to.
     *  @param indent   the number of tabs which will be inserted before each
     *      line.
     *  @param recurse  if true then also print the children of this element.
     **/
    protected void printNice(Writer into, int indent, boolean recurse) throws IOException {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        // print start tag
        StringBuffer start = new StringBuffer();
        
        if (-1 != indent) {
            // do indent
            for (int eachTab = 0; eachTab < indent; eachTab++) {
                start.append('\t');
            }
        }
        
        start.append(getDocument().docContent.substring(loc.startTag.start, loc.startTag.end + 1));
        
        if (-1 != indent) {
            start.append('\n');
        }
        
        into.write(start.toString());
        
        // print the rest if this was not an empty element.
        if (!loc.startTag.equals(loc.endTag)) {
            String itsValue = getTextValue(true, (-1 != indent));
            
            // print node value
            if (null != itsValue) {
                if (-1 != indent) {
                    // do indent
                    for (int eachTab = 0; eachTab < indent + 1; eachTab++) {
                        into.write("\t");
                    }
                }
                
                into.write(itsValue);
                
                if (-1 != indent) {
                    into.write('\n');
                }
            }
            
            // recurse as needed
            if (recurse) {
                int childIndent = indent;
                
                Enumeration childrens = getChildren();
                
                Attribute space = getAttribute("xml:space");
                
                if (null != space) {
                    if ("preserve".equals(space.getValue())) {
                        childIndent = -1;
                    } else {
                        childIndent = indent + 1;
                    }
                } else {
                    if (-1 != indent) {
                        childIndent = indent + 1;
                    } else {
                        childIndent = -1;
                    }
                }
                
                while (childrens.hasMoreElements()) {
                    LiteXMLElement aChild = (LiteXMLElement) childrens.nextElement();
                    
                    aChild.printNice(into, childIndent, recurse);
                }
            }
            
            // print end tag
            StringBuffer end = new StringBuffer();
            
            if (-1 != indent) {
                // do indent
                for (int eachTab = 0; eachTab < indent; eachTab++) {
                    end.append('\t');
                }
            }
            
            end.append(getDocument().docContent.substring(loc.endTag.start, loc.endTag.end + 1));
            
            if (-1 != indent) {
                end.append('\n');
            }
            
            into.write(end.toString());
        }
    }
    
    /**
     * Given a source string, an optional tag and a range with in the source
     * find either the tag specified or the next tag.
     *
     * The search consists of 4 phases :
     *    0.  If no tag was specified, determine if a tag can be found and
     *        learn its name.
     *    1.  Search for the start of the named tag.
     *    2.  Search for the end tag. Each time we think we have found a tag
     *        which might be the end tag we make sure it is not the end tag
     *        of another element with the same name as our tag.
     *    3.  Calculate the position of the body of the tag given the locations
     *        of the start and end.
     *
     * @param source the string to search
     * @param tag the tag to search for in the source string. If this tag is
     *    empty or null then we will search for the next tag.
     * @param range describes the range of character locations in the source
     *    string to which the search will be limited.
     * @return tagRange containing the ranges of the found tag.
     **/

     protected tagRange getTagRanges(final StringBuffer source, String tag, final charRange range) {
            // FIXME   bondolo@jxta.org 20010327    Does not handle XML comments. ie.  <!-- -->
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added to the document.");
        }
        
        tagRange result = new tagRange();
        int start = range.start;
        int end = source.length() - 1;
        int current;
        boolean foundStartTag = false;
        boolean foundEndTag = false;
        boolean emptyTag = (null == tag) || (0 == tag.length());
        
        // check for bogosity
        if ((-1 == start) || (start >= end)) {
            throw new IllegalArgumentException( "Illegal start value" );
        }
        
        // adjust end of range
        if ((-1 != range.end) && (end > range.end)) {
            end = range.end;
        }
        
        // check for empty tag and assign empty string
        if (null == tag) {
            tag = "";
        }
        
        if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug( "Searching for \"" + tag + "\" in range [" + start +"," + end+"]" );
        }

        current = start;
        
        // Begin Phase 0 : Search for any tag.
        
        if (emptyTag) {
            int foundTagText = source.indexOf("<", current);
            
            // was it not found? if not then quit
            if (-1 == foundTagText) {
                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No Tags Found");
                }
                return result;
            }
            
            // this part is about setting the tag if necessary
            foundTagText++;
            
            int afterTagText = foundTagText;

            while (afterTagText <= end) {
                char inTagName = source.charAt(afterTagText);

                if (!Character.isWhitespace(inTagName) && ('/' != inTagName) && ('>' != inTagName)) {
                    afterTagText++;
                    continue;
                }
                
                tag = source.substring(foundTagText, afterTagText);
                emptyTag = (null == tag) || (0 == tag.length());
                
                break;
            }
            
            // it better not be still empty
            if (emptyTag) {
                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("No tag found");
                }
                return result;
            }
        }
        
        if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Search for \"" + tag + "\" [" + start + "," + end +"]");
        }

        // Begin Phase 1: Search for the Start Tag
        
        while (!foundStartTag && (current < end)) {
            int foundTagText = source.indexOf(tag, current + 1); // first loc is one past current location
            int foundTagTerminator;
            int foundNextTagStart;
            int afterTagText = foundTagText + tag.length();
            
            // was it not found
            if ((-1 == foundTagText) || (afterTagText > end)) {
                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Tag \"" + tag + "\" Not Found(1)");
                }
                return result;
            }
            
            char checkChar = source.charAt(afterTagText);

            // check to see if it is the start tag
            if (('<' != source.charAt(foundTagText - 1)) || // it has the open tag delimiter before it
                    (!Character.isWhitespace(checkChar) && ('/' != checkChar) && ('>' != checkChar))) { // is immediately followed by a delimiter
                current = afterTagText;
                continue;
            }
            
            foundTagTerminator = source.indexOf(">", afterTagText);
            foundNextTagStart = source.indexOf("<", afterTagText + 1);
            
            if ((-1 == foundTagTerminator) || // the tag has no terminator
                    (foundTagTerminator > end) || // it is past the valid range
                    ((-1 != foundNextTagStart) && // there is another tag start
                    (foundNextTagStart < foundTagTerminator))) { // and it is before the terminator we found. very bad
                current = afterTagText;
                continue;
            }
            
            foundStartTag = true;
            result.startTag.start = foundTagText - 1;
            result.startTag.end = foundTagTerminator;
        }
        
        if (!foundStartTag) {
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Tag \"" + tag + "\" Not Found(2)");
                }
            return result;
        }
        
        // is this an empty element declaration?
        if ('/' == source.charAt(result.startTag.end - 1)) {
            // end is the start and there is no body
            result.body = new charRange(result.startTag.start, result.startTag.end);
            result.endTag = new charRange(result.startTag.start, result.startTag.end);
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Empty Element : \"" + tag + "\" Start : " + result.startTag );
            }
            return result;
        }
        
        current = result.startTag.end + 1;
         
        // if current is past the end then our end tag is not found.
        if (current >= end) {
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("End not found \"" + tag + "\" Start : " + result.startTag );
            }
            return result;
        }
        
        // Begin Phase 2 :  Search for the end tag
        
        String endTag = "</" + tag + ">";
        int searchFrom = result.startTag.end + 1;
        
        while (!foundEndTag && (current < end) && (searchFrom < end)) {
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Searching for \"" + endTag + "\" in range [" + current+","+ end +"]");
            }

            int foundTagText = source.indexOf(endTag, current);
            
            // was it not found or not in bounds?
            if ((-1 == foundTagText) || ((foundTagText + endTag.length() - 1) > end)) {
                break;
            } // it was not found
            
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Prospective tag pair for \"" + tag + "\" " + result.startTag + ":[" + foundTagText + "," + (foundTagText + endTag.length() - 1)+"]");
            }
            
            // We recurse here in order to exclude the end tags of any sub elements with the same name
            charRange subRange = new charRange(searchFrom, foundTagText - 1 );
            
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Recursing to search for \"" + tag + "\" in " + subRange);
            }

            tagRange subElement = getTagRanges(source, tag, subRange);
            
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Recursion result \"" + tag + "\" " + subElement );
            }

            // if there was an incomplete sub-tag with the same name, skip past it
            if (subElement.startTag.isValid()) {
                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug( "Found incomplete sub-tag \"" + tag + "\" at " + subElement + " within " + subRange);
                }
                
                
                if( subElement.endTag.isValid() ) {
                    if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug( "Complete sub-tag \"" + tag + "\" at " + subElement + " within " + subRange);
                    }
                    current = subElement.endTag.end + 1;
                    searchFrom = subElement.endTag.end + 1;
                } else {
                    current = foundTagText + endTag.length();
                }
                
                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug( "Continuing search for \"" + tag + "\" from " + searchFrom + "-[" + current + ":" + end + "]");
                }

                continue;
            }
            
            foundEndTag = true;
            result.endTag.start = foundTagText;
            result.endTag.end = foundTagText + endTag.length() - 1;
            
            if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug( "Prospective tag \"" + tag + "\" " + result.endTag + " is confirmed.");
                }
        }
        
        // Begin Phase 3 :  Calculate the location of the body.
        
        result.body.start = result.startTag.end + 1;
        
        if (foundEndTag) {
            result.body.end = result.endTag.start - 1;
        } else {
            result.body.end = end;
        }
        
        if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Found element : \"" + tag + "\" " + result );
        }
        return result;
    }
    
    /**
     *  Parse a charRange and add any tags found as content as children of a
     *  specified element. This process is repeated recursivly.
     *
     *  @param  scanRange   the range to be parsed for sub-tags
     *  @param  addTo       the element to add any discovered children to.
     **/
    protected void addChildTags(final charRange scanRange, LiteXMLElement addTo) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added to the document.");
        }
        
        int current = scanRange.start;
        
        if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("Scanning for children in range " + scanRange );
        }
        
        do {
            // scan for any tag.
            tagRange aSubtag = getTagRanges(getDocument().docContent, null, new charRange(current, scanRange.end));
            
            // did we find one?
            if (aSubtag.isValid()) {
                LiteXMLElement newChild = (LiteXMLElement) getDocument().createElement(aSubtag);

                if (verboseLogging && LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("Adding child tag \"" + 
                        getDocument().docContent.substring( aSubtag.endTag.start + 2, aSubtag.endTag.end ) + "\" " + aSubtag );
                }

                addTo.appendChild(newChild);
                
                if (paranoidConsistencyChecking) {
                    checkConsistency();
                }

                if (!aSubtag.startTag.equals(aSubtag.endTag)) {
                    addChildTags(aSubtag.body, newChild); // recurse into the new tag
                }
                
                // all done this tag, move on
                current = aSubtag.endTag.end + 1;
            } else {
                current = -1; // all done!
            }
        } while ((-1 != current) && (current < scanRange.end));
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
    }
    
    /**
     *  For this element and all its children adjust the location of its ranges
     *  by the amount specified.
     *
     *  @param beginningAt  adjust all locations which are at or past this
     * location.
     *  @param  by  amount to adjust all matching locations.
     **/
    protected void adjustLocations(final int beginningAt, final int by) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        // Check that this element is not entirely to the left of the shift
        // zone. NB: end can be < start if len is 0.
        if (loc.endTag.end < beginningAt && loc.endTag.start < beginningAt) {
            return;
        }
        
        if ((loc.startTag.end >= beginningAt) || ((loc.startTag.start >= beginningAt) && ((loc.startTag.end + 1) == loc.startTag.start))) {
            loc.startTag.end += by;
        }
        
        if (loc.startTag.start >= beginningAt) {
            loc.startTag.start += by;
        }
        
        if ((loc.body.end >= beginningAt) || ((loc.body.start >= beginningAt) && ((loc.body.end + 1) == loc.body.start))) {
            loc.body.end += by;
        }
        
        if (loc.body.start >= beginningAt) {
            loc.body.start += by;
        }
        
        if ((loc.endTag.end >= beginningAt) || ((loc.endTag.start >= beginningAt) && ((loc.endTag.end + 1) == loc.endTag.start))) {
            loc.endTag.end += by;
        }
        
        if (loc.endTag.start >= beginningAt) {
            loc.endTag.start += by;
        }
        
        for (Enumeration eachChild = getChildren(); eachChild.hasMoreElements();) {
            LiteXMLElement aChild = (LiteXMLElement) eachChild.nextElement();
            
            aChild.adjustLocations(beginningAt, by);
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
    }
    
    /**
     *  Given a StringBuffer find all occurances of escaped characters which
     *  must be decoded and convert them back to their non-escaped equivalents.
     *
     *  <p/>Also does end of line folding per: <a href="http://www.w3.org/TR/REC-xml#sec-line-ends"/>
     *
     *  @param target The stringbuffer which will be decoded.
     *  @return The decoded version of the stringbuffer.
     **/
    protected StringBuffer decodeEscaped(StringBuffer target) {
        
        int current = 0;
        
        StringBuffer result = new StringBuffer(target.length());
        
        while (current < target.length()) {
            // FIXME bondolo@jxta.org   20010422    Should process xml comments out here.
            
            // fold 0x0D and 0x0D 0x0A to 0x0A
            if ('\r' == target.charAt(current)) {
                result.append('\n');
                current++;
                if ((current < target.length()) && ('\n' == target.charAt(current))) {
                    current++;
                }
                continue;
            }
            
            if ('&' != target.charAt(current)) {
                result.append(target.charAt(current));
                current++;
                continue;
            }
            
            int terminusAt = current + 1;
            
            while ((terminusAt < target.length()) && // dont go past end
                    ((terminusAt - current) < 6) && // only look 6 chars away.
                    (';' != target.charAt(terminusAt))) { // must be a ;
                terminusAt++;
            }
            
            if ((terminusAt >= target.length()) || (';' != target.charAt(terminusAt))) {
                // if we get here then we didnt find the terminal we needed
                // so we just leave ampersand as it was, the document is
                // ill-formed but why make things worse?
                result.append(target.charAt(current));
                current++;
                continue;
            }
            
            char[] sub = new char[ terminusAt - current + 1 ];

            target.getChars(current, terminusAt + 1, sub, 0);
            String escaped = new String(sub);
            
            if ("&amp;".equals(escaped)) {
                result.append('&');
                current += 4;
            } else if ("&lt;".equals(escaped)) {
                result.append('<');
                current += 3;
            } else if ("&gt;".equals(escaped)) { // for compatibility with SGML. We dont encode these
                result.append('>');
                current += 3;
            } else if (escaped.startsWith("&#")) {
                String numericChar = escaped.substring(2, escaped.length() - 1);
                
                // is it &#; ?
                if (numericChar.length() < 1) {
                    result.append(target.charAt(current));
                    current++;
                    continue;
                }
                
                // is it hex numeric
                if (numericChar.charAt(0) == 'x') {
                    numericChar = numericChar.substring(1);
                    
                    // is it &#x; ?
                    if (numericChar.length() < 1) {
                        result.append(target.charAt(current));
                        current++;
                        continue;
                    }
                    
                    try {
                        char asChar = (char) Integer.parseInt(numericChar.toLowerCase(), 16);

                        result.append(asChar);
                        current += escaped.length();
                    } catch (NumberFormatException badref) {
                        // it was bad, we will just skip it.
                        result.append(target.charAt(current));
                        current++;
                    }
                    continue;
                }
                
                // its base 10
                try {
                    char asChar = (char) Integer.parseInt(numericChar, 10);

                    result.append(asChar);
                    current += escaped.length();
                } catch (NumberFormatException badref) {
                    // it was bad, we will just skip it.
                    result.append(target.charAt(current));
                    current++;
                }
                continue;
            } else {
                // if we get here then we didn't know what to do with the
                // entity. so we just send it unchanged.
                result.append(target.charAt(current));
                current++;
                continue;
            }
            
            current++;
        }
        
        return result;
    }
    
    /**
     *  Given a StringBuffer find all occurances of characters which must be
     *  escaped and convert them to their escaped equivalents.
     *
     *  @param target The stringbuffer which will be encoded in place.
     **/
    protected void encodeEscaped(StringBuffer target) {
        
        int current = 0;
        
        while (current < target.length()) {
            if ('&' == target.charAt(current)) {
                target.insert(current + 1, "amp;");
                current += 5;
                continue;
            } else if ('<' == target.charAt(current)) {
                target.setCharAt(current, '&');
                target.insert(current + 1, "lt;");
                current += 4;
                continue;
            } else {
                current++;
            }
        }
    }
    
    /**
     * Returns an enumerations of the attributes assosicated with this object.
     * Each element is of type Attribute.
     *
     * @return Enumeration the attributes associated with this object.
     **/
    public Enumeration getAttributes() {
        List results = new ArrayList();
        
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        // find the start of the first attribute
        int current = loc.startTag.start + 1;

        while (current <= loc.startTag.end) {
            char inTagName = getDocument().docContent.charAt(current);

            if (Character.isWhitespace(inTagName) || ('/' == inTagName) || ('>' == inTagName)) {
                break;
            }
            current++;
        }
        
        // loop and add attributes to the vector
        while (current < loc.startTag.end) {
            tagRange nextAttr = getAttributeLoc(null, new charRange(current, loc.startTag.end));
            
            if (!nextAttr.isValid()) {
                break;
            }
            
            results.add(
                    new Attribute(this, getDocument().docContent.substring(nextAttr.startTag.start, nextAttr.startTag.end + 1),
                    getDocument().docContent.substring(nextAttr.body.start, nextAttr.body.end + 1)));
            
            current = nextAttr.endTag.end + 1;
        }
        
        return Collections.enumeration(results);
    }
    
    /**
     *  Returns the tagRange of the next attribute contained in the range
     *  provided. The tag range returned consists of the startTag indicating
     *  the location of the name, body indicating the location of the value and
     *  endTag indicating the location of the final quote delimiter.
     *
     *  @param  name Name to match. null means match any name.
     *  @param  inRange the limits of the locations to scan.
     *  @return tagRange containing the location of the next attribute
     **/
    protected tagRange getAttributeLoc(String name, charRange inRange) {
        tagRange result = new tagRange();
        int current = inRange.start;
        
        do {
            // skip the whitespace
            
            while (current <= inRange.end) {
                char inTagName = getDocument().docContent.charAt(current);

                if (!Character.isWhitespace(inTagName) && ('/' != inTagName) && ('>' != inTagName)) {
                    break;
                }
                current++;
            }
            
            int equalsAt = getDocument().docContent.indexOf("=", current);
            
            // make sure there is an equals
            if ((-1 == equalsAt) || (equalsAt >= inRange.end)) {
                return result;
            }
            
            // get the name
            result.startTag.start = current;
            result.startTag.end = equalsAt - 1;
            
            // get the quote char we must match
            String requiredQuote = getDocument().docContent.substring(equalsAt + 1, equalsAt + 2);
            
            // make sure its a valid quote
            if (('\'' != requiredQuote.charAt(0)) && ('\"' != requiredQuote.charAt(0))) {
                return result;
            }
            
            // find the next occurance of this quote
            int nextQuote = getDocument().docContent.indexOf(requiredQuote, equalsAt + 2);
            
            // make sure the quote is in a good spot.
            if ((-1 == nextQuote) || (nextQuote >= inRange.end)) {
                return result;
            }
            
            result.body.start = equalsAt + 2;
            result.body.end = nextQuote - 1;
            
            result.endTag.start = nextQuote;
            result.endTag.end = nextQuote;
            
            // check if the name matches.
            if ((null != name) && !name.equals(getDocument().docContent.substring(result.startTag.start, result.startTag.end + 1))) {
                result.startTag.start = -1;
            }
            
            current = nextQuote + 1;
        } while ((current < inRange.end) && (!result.isValid()));
        
        return result;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String addAttribute(String name, String value) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (null == name) {
            throw new IllegalArgumentException("name must not be null");
        }
        
        if (null == value) {
            throw new IllegalArgumentException("value must not be null");
        }
        
        for (int eachChar = name.length() - 1; eachChar >= 0; eachChar--) {
            if (Character.isWhitespace(name.charAt(eachChar))) {
                throw new IllegalArgumentException("Attribute names may not contain spaces.");
            }
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        // skip past the name portion
        int current = loc.startTag.start + 1;

        while (current <= loc.startTag.end) {
            char inTagName = getDocument().docContent.charAt(current);
            
            if (Character.isWhitespace(inTagName) || ('/' == inTagName) || ('>' == inTagName)) {
                break;
            }
            
            current++;
        }
        
        // find out if there was a previous value for this name
        String oldValue = null;
        tagRange oldAttr = getAttributeLoc(name, new charRange(current, loc.startTag.end));
        
        // choose which kind of quote to use
        char usingQuote = (-1 != value.indexOf('"')) ? '\'' : '\"';
        
        // make sure we can use it.
        if (('\'' == usingQuote) && (-1 != value.indexOf('\''))) {
            throw new IllegalArgumentException("Value contains both \" and \'");
        }
        
        // build the new attribute string
        StringBuffer newStuff = new StringBuffer(" ");

        newStuff.append(name);
        newStuff.append("=");
        newStuff.append(usingQuote);
        newStuff.append(value);
        newStuff.append(usingQuote);
        
        // add it in.
        if (!oldAttr.isValid()) {
            // we aren't replacing an existing value
            getDocument().docContent.insert(current, newStuff.toString());
            
            // move all doc locations which follow this one based on how much we
            // inserted.
            getDocument().adjustLocations(current, newStuff.length());
        } else {
            // we are replacing an existing value
            oldValue = getDocument().docContent.substring(oldAttr.body.start, oldAttr.body.end + 1);
            
            getDocument().docContent.delete(oldAttr.body.start, oldAttr.body.end + 1);
            getDocument().docContent.insert(oldAttr.body.start, value);
            
            int delta = value.length() - (oldAttr.body.end - oldAttr.body.start + 1);
            
            // move all doc locations which follow this one based on how much we
            // inserted or deleted.
            getDocument().adjustLocations(loc.startTag.start + 1, delta);
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        return oldValue;
    }
    
    /**
     *  {@inheritDoc}
     **/
    public String addAttribute(Attribute newAttrib) {
        return addAttribute(newAttrib.getName(), newAttrib.getValue());
    }
    
    /**
     *  {@inheritDoc}
     **/
    public Attribute getAttribute(String name) {
        if (null != uninserted) {
            throw new IllegalStateException("This element has not been added.");
        }
        
        if (paranoidConsistencyChecking) {
            checkConsistency();
        }
        
        // skip past the name portion
        int current = loc.startTag.start + 1;

        while (current <= loc.startTag.end) {
            char inTagName = getDocument().docContent.charAt(current);
            
            if (Character.isWhitespace(inTagName) || ('/' == inTagName) || ('>' == inTagName)) {
                break;
            }
            
            current++;
        }
        
        // find the attribute matching this name
        tagRange attr = getAttributeLoc(name, new charRange(current, loc.startTag.end));
        
        if (!attr.isValid()) {
            return null;
        }
        
        // build the object
        return new Attribute(this, getDocument().docContent.substring(attr.startTag.start, attr.startTag.end + 1),
                getDocument().docContent.substring(attr.body.start, attr.body.end + 1));
    }
    
    protected boolean checkConsistency() {
//        assert loc.isValid();
        
        charRange elementRange = new charRange(loc.startTag.start, loc.endTag.end);
        
//        assert elementRange.contains(loc.startTag);
//        assert elementRange.contains(loc.body);
//        assert elementRange.contains(loc.endTag);
        
        Iterator eachChild = children.iterator();
        Iterator nextChilds = children.iterator();
        
        if( nextChilds.hasNext()) {
            nextChilds.next();
        }
        while (eachChild.hasNext()) {
            LiteXMLElement aChild = (LiteXMLElement) eachChild.next();
            LiteXMLElement nextChild = null;
            
//            assert loc.contains(aChild.loc);

            if( nextChilds.hasNext()) {
                nextChild = (LiteXMLElement) nextChilds.next();
//                assert aChild.loc.compareTo(nextChild.loc) < 0;
            }
            
            aChild.checkConsistency();
        }
        
        return true;
    }
    
    /**
     *  The document we are a part of.
     **/
    LiteXMLDocument getDocument() {
        return doc;
    }
}

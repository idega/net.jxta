/*
 *  Copyright (c) 2001-2003 Sun Microsystems, Inc.  All rights reserved.
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
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
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
 *  $Id: ConfiguratorException.java,v 1.1 2007/01/16 11:01:59 thomas Exp $
 */
package net.jxta.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *  This class maintains a {@link java.util.List} of all possible {@link
 *  java.lang.Throwable causes} that may be generated as a part of the
 *  configuration process.
 *
 * @author     james todd [gonzo at jxta dot org]
 * @version    $Id: ConfiguratorException.java,v 1.1 2003/11/04 21:07:26 gonzo
 *      Exp $
 */

public class ConfiguratorException
         extends JxtaException {

    private List causes = null;


    /**
     *  Constucts a {@link JxtaException} with no specified details.
     */
    public ConfiguratorException() {
        super();
    }


    /**
     *  Constructs a {@link JxtaException} with the specified message.
     *
     * @param  msg  message
     */
    public ConfiguratorException(String msg) {
        super(msg);
    }


    /**
     *  Constructs a {@link JxtaException} with the specified {@link
     *  java.lang.Throwable cause}.
     *
     * @param  ex  cause
     */
    public ConfiguratorException(Throwable ex) {
        super();

        addCause(ex);
    }


    /**
     *  Constructs a {@link JxtaException} with the specified message and {@link
     *  java.lang.Throwable cause}.
     *
     * @param  msg  message
     * @param  ex   cause
     */
    public ConfiguratorException(String msg, Throwable ex) {
        super(msg);

        addCause(ex);
    }


    /**
     *  Constructs a {@link JxtaException} with the specified {@link
     *  java.util.List} of {@link java.lang.Throwable causes}.
     *
     * @param  ex  causes
     */
    public ConfiguratorException(List ex) {
        super();

        addCauses(ex);
    }


    /**
     *  Constructs a {@link JxtaException} with the specified message in
     *  addition to the {@link java.util.List} of {@link java.lang.Throwable
     *  causes}.
     *
     * @param  msg  message
     * @param  ex   causes
     */
    public ConfiguratorException(String msg, List ex) {
        super(msg);

        addCauses(ex);
    }


    /**
     *  Retrieve the {@link java.lang.Throwable causes} as a {@link
     *  java.util.List}.
     *
     * @return    The causes
     */
    public List getCauses() {
        return this.causes != null ?
                this.causes : Collections.EMPTY_LIST;
    }


    /**
     *  Add a cause of type {@link java.lang.Throwable}.
     *
     * @param  c  The cause
     */
    public void addCause(Throwable c) {
        if (c != null) {
            if (this.causes == null) {
                this.causes = new ArrayList();
            }

            this.causes.add(c);
        }
    }


    /**
     *  Add a {@link java.util.List} of {@link java.lang.Throwable causes}.
     *
     * @param  c  The causes
     */
    public void addCauses(List c) {
        if (c != null) {
            for (Iterator t = c.iterator(); t.hasNext(); ) {
                addCause((Throwable)t.next());
            }
        }
    }


    /**
     *
     *  @inheritDoc
     *
     *  <p/>Overload printStackTrace() to support multiple {@link
     *  java.util.Throwable causes}.
     */
    public void printStackTrace() {
        super.printStackTrace();

        for (Iterator c = getCauses().iterator(); c.hasNext(); ) {
            ((Throwable)c.next()).printStackTrace();
        }
    }


    /**
     *  @inheritDoc
     *
     *  <p/>Overload printStackTrace({@link java.io.PrintStream}) to support
     *  multiple {@link java.util.Throwable causes}.
     *
     * @param  ps  Description of the Parameter
     */
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);

        ps.println("Caused by:");
        int count = 1;
        for (Iterator c = getCauses().iterator(); c.hasNext(); ) {
            Throwable t = (Throwable)c.next();

            ps.print("Cause #" + count++ + " : ");

            t.printStackTrace(ps);
        }
    }


    /**
     *  @inheritDoc
     *
     *  <p/>Overload printStackTrace({@link java.io.PrintWriter}) to support
     *  multiple {@link java.util.Throwable causes}.
     *
     * @param  pw  Description of the Parameter
     */
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);

        pw.println("Caused by:");
        int count = 1;
        for (Iterator c = getCauses().iterator(); c.hasNext(); ) {
            Throwable t = (Throwable)c.next();

            pw.print("Cause #" + count++ + " : ");

            t.printStackTrace(pw);
        }
    }
}


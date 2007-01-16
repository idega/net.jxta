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
 *  $Id: TcpTransportTableModel.java,v 1.1 2007/01/16 11:01:38 thomas Exp $
 */
package net.jxta.ext.config.ui;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *  Table model for ext:config:ui tcp transport display
 *
 * @author     volker john [volkerj at jxta dot org]
 */
public class TcpTransportTableModel extends TransportTableModel {

	private static final String[] columnNames = new String[] {
			"Local Address",
			"Local Port",
			"Hide Private",
			"Outgoing",
			"Incoming",
			"Public Address",
			"Public Port",
			"Multicast" 
		};
			
	private static final Vector rowset = new Vector();
	
	public TcpTransportTableModel() {

		// TODO might want to construct this according to Configurator.xml to avoid mismatches
		rowset.addElement(new Object[] {
				"Any/All Local Addresses",   // (private) local address
				"9701",                      // port
				new Boolean(false),          // hide private
				new Boolean(true),           // outgoing
				new Boolean(true),           // incoming
				"./.",                       // (optional) public address
				"./.",                       // (optional) public port
				new Boolean(true)            // multicast
			});
	}

	/* (non-Javadoc)
	 * @see net.jxta.ext.config.ui.TransportTableModel#getColumns()
	 */
	String[] getColumns() {
		return columnNames;
	}

	/* (non-Javadoc)
	 * @see net.jxta.ext.config.ui.TransportTableModel#getRows()
	 */
	Vector getRows() {
		return rowset;
	}
}

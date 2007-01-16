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
 *  $Id: PeerTreeSelectionListener.java,v 1.1 2007/01/16 11:01:38 thomas Exp $
 */
package net.jxta.ext.config.ui;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.swixml.SwingEngine;

/**
 *  ext:config:ui Tree selection event listener
 *
 * @author     volker john [volkerj at jxta dot org]
 */
class PeerTreeSelectionListener implements TreeSelectionListener {

	private static SwingEngine swix;
	private static JTree tree;
	private static Component current = null;


	/**
	 * @param swix
	 */
	public PeerTreeSelectionListener(SwingEngine swix) {
		tree = (JTree) swix.find("id_cfgtree");
		current = (Component) swix.find("id_cfgbasiccard");
		PeerTreeSelectionListener.swix = swix;
	}

	
	/* (non-Javadoc)
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		

		if (node == null) return;

		if (current != null) {
			current.setVisible(false);
		}

		// TODO problem is that this code heavily relies on the XML code in Configurator.xml 
		// in terms of component (panel) names etc.
		// Solution: make tree nodes objects that have the card id as value and use
		// current = swix.find(node.<VAL>.toString()) to avoid lengthy if statement
		
		if (node.toString() == "General") {
			current = swix.find("id_cfgbasiccard");
		} else if (node.toString() == "Network") { 
			current = swix.find("id_cfgnetworkcard");
		} else if (node.toString() == "Rendezvous") { 
			current = swix.find("id_cfgrdvcard");
		} else if (node.toString() == "Relays") { 
			current = swix.find("id_cfgrlycard");
		} else if (node.toString() == "tcp") { 
			current = swix.find("id_cfgtcpcard");
		} else if (node.toString() == "http") { 
			current = swix.find("id_cfghttpcard");
		} else if (node.toString() == "Security") { 
			current = swix.find("id_cfgsecuritycard");
		} else if (node.toString() == "Services") { 
			current = swix.find("id_cfgservicescard");
		} else if (node.toString() == "Rendezvous Service") { 
			current = swix.find("id_cfgrdvservicecard");
		} else if (node.toString() == "Relay Service") { 
			current = swix.find("id_cfgrlyservicecard");
		} else if (node.toString() == "Endpoint Service") { 
			current = swix.find("id_cfgendpservicecard");
		} else if (node.toString() == "Proxy Service") { 
			current = swix.find("id_cfgproxyservicecard");
		}
		
		if (current != null) {
			current.setVisible(true);
		}

		
	}

}

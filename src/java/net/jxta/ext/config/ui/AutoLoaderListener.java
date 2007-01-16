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
 *  $Id: AutoLoaderListener.java,v 1.1 2007/01/16 11:01:39 thomas Exp $
 */
package net.jxta.ext.config.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.swixml.SwingEngine;

/**
 *  Listener for rdv/rly autoloader events
 *
 * @author     volker john [volkerj at jxta dot org]
 */
public class AutoLoaderListener implements ActionListener {

    private SwingEngine ldr;
    private SwingEngine cfg;
    private JDialog dlg;
	
	private JTextField httpProxyAddr;
	private JTextField httpProxyPort;
	private JTextField rdvBootstrap;
	private JTextField rlyBootstrap;

    private JList tcprdvlist;
    private JList httprdvlist;
    private JList tcprlylist;
    private JList httprlylist;

	public AutoLoaderListener(SwingEngine ldr, SwingEngine cfg) {
		this.ldr = ldr;
        this.cfg = cfg;
		dlg = (JDialog) ldr.getRootComponent();
		
		rdvBootstrap = (JTextField) ldr.find("id_rdvbootstrap");
		rlyBootstrap = (JTextField) ldr.find("id_rlybootstrap"); 
		httpProxyAddr = (JTextField) ldr.find("id_httpproxyaddress");
		httpProxyPort = (JTextField) ldr.find("id_httpproxyport");
		
		tcprdvlist = (JList) cfg.find("id_cfgtcprdvlist");
		httprdvlist = (JList) cfg.find("id_cfghttprdvlist");
		tcprlylist = (JList) cfg.find("id_cfgtcprlylist");
		httprlylist = (JList) cfg.find("id_cfghttprlylist");

	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (("AC_OK").equals(command)) {
			if (loadlist(rdvBootstrap.getText(), true) && loadlist(rlyBootstrap.getText(), false)) {
				dlg.setVisible(false);
				dlg.dispose();
                JOptionPane.showMessageDialog(ldr.getRootComponent(), 
                        "Successfully downloaded Rendezvous and Relay lists!");
			}
		} else if (("AC_CANCEL").equals(command)) {
			dlg.setVisible(false);
			dlg.dispose();
		} else if (("enableHTTPProxy").equals(command)) {
			if (!httpProxyAddr.isEnabled()) {
                // set proxy if enabled
                System.setProperty("http.proxyHost", httpProxyAddr.getText());
                System.setProperty("http.proxyPort", httpProxyPort.getText());
				enable(httpProxyAddr);
				enable(httpProxyPort);																 
			} else {
                // _TODO is this correct?
                System.setProperty("http.proxyHost", "");
                System.setProperty("http.proxyPort", "");
				disable(httpProxyAddr);
				disable(httpProxyPort);																 
			}

		}
	}
	
	/**
	 *  
	 */
	private boolean loadlist(String urlString, boolean rdv) {
        DefaultListModel tcpListModel;
        DefaultListModel httpListModel;

        if (urlString.equals("")) return true;

        if (rdv) {
            tcpListModel = (DefaultListModel) tcprdvlist.getModel();
            httpListModel = (DefaultListModel) httprdvlist.getModel();
        } else {
            tcpListModel = (DefaultListModel) tcprlylist.getModel();
            httpListModel = (DefaultListModel) httprlylist.getModel();            
        }
        
		InputStream inp = null;
		try {
			inp = (new URL(urlString)).openStream();
			BufferedReader l = new BufferedReader(new InputStreamReader(inp));
			
			String s;
			int valid = 0;
			while ((s = l.readLine()) != null) {
				s = s.toLowerCase();
				if (s.startsWith( "tcp://")) {
					tcpListModel.addElement(s.substring(6));
                    ++valid;
				} else if (s.startsWith("http://")) {
					httpListModel.addElement(s.substring(7));
                    ++valid;
				}
			}
			if (valid == 0) {
                JOptionPane.showMessageDialog(ldr.getRootComponent(), 
                        "Retrieved lists were invalid. Please try again.",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(ldr.getRootComponent(), 
                    "You have entered an invalid URL.",
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
			return false;
		} catch (IOException e) {
            JOptionPane.showMessageDialog(ldr.getRootComponent(), 
                    "Could not download lists." 
                    + (( httpProxyAddr.isEnabled() ? "" : " You might need a proxy.")),
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		try {
			inp.close();
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * @param component
	 */
	private void requestFocus(Component component) {
		component.requestFocus();					
	}

	/**
	 * @param components
	 */
	private void disable(Component[] components) {
		for (int i = 0; i < components.length; i++) {
			disable(components[i]);
		}
	}

	/**
	 * @param component
	 */
	private void disable(Component component) {
		component.setEnabled(false);
	}

	/**
	 * @param components
	 */
	private void enable(Component[] components) {
		for (int i = 0; i < components.length; i++) {
			enable(components[i]);
		}
	}

	/**
	 * @param component
	 */
	private void enable(Component component) {
		component.setEnabled(true);		
	}
	

}

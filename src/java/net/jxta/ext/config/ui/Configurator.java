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
 *  $Id: Configurator.java,v 1.1 2007/01/16 11:01:39 thomas Exp $
 */

package net.jxta.ext.config.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.swixml.SwingEngine;

import net.jxta.exception.ConfiguratorException;
import net.jxta.ext.config.AbstractConfigurator;
import net.jxta.ext.config.Profile;
import net.jxta.impl.peergroup.DefaultConfigurator;
import net.jxta.impl.peergroup.PlatformConfigurator;
import net.jxta.impl.protocol.PlatformConfig;

/**
 *  main Configurator class for ext:config/ui extending 
 *  ext:config's AbstractConfigurator 
 *
 * @author     volker john [volkerj at jxta dot org]
 */
public class Configurator extends AbstractConfigurator {
	private static final String DESCRIPTOR = "net/jxta/ext/config/ui/resource/Configurator.xml";
	private static SwingEngine swix;
	private static net.jxta.ext.config.Configurator cfg = new net.jxta.ext.config.Configurator(Profile.EDGE);
    private final static Logger LOG = Logger.getLogger(Configurator.class.getName());
    
    /* (non-Javadoc)
     * @see net.jxta.ext.config.AbstractConfigurator#createPlatformConfig(net.jxta.impl.peergroup.PlatformConfigurator)
     */
    public PlatformConfig createPlatformConfig(Configurator configurator)
            throws ConfiguratorException {

        configurator.removeResource(PROFILE_KEY);
        configurator.addResource(PROFILE_KEY, "/net/jxta/ext/config/resources/local.xml");
        LOG.setLevel(Level.DEBUG);
        swix = new SwingEngine(this);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame cfg = (JFrame) swix.render(Configurator.DESCRIPTOR);
            init(cfg);
            cfg.setVisible(true);
            while (cfg.isShowing());
            cfg.dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //configurator.save();
        return configurator.getPlatformConfig();
    }

    /* (non-Javadoc)
     * @see net.jxta.ext.config.AbstractConfigurator#createPlatformConfig(net.jxta.impl.peergroup.PlatformConfigurator)
     */
    public PlatformConfig updatePlatformConfig(Configurator configurator)
            throws ConfiguratorException {

        if (configurator.isReconfigure()) {
            configurator.removeResource(PROFILE_KEY);
            configurator.addResource(PROFILE_KEY, "/net/jxta/ext/config/resources/local.xml");
            LOG.setLevel(Level.DEBUG);
            swix = new SwingEngine(this);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JFrame cfg = (JFrame) swix.render(Configurator.DESCRIPTOR);
                init(cfg);
                cfg.setVisible(true);
                while (cfg.isShowing());
                cfg.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        return configurator.getPlatformConfig();
    }

	/**
	 * @param cfg
	 */
	private void init(JFrame cfg) {
		swix.setActionListener(cfg, new ConfiguratorListener(swix));
		cfg.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				//System.exit(0);
			}
		});
		
		JTree tree = (JTree) swix.find("id_cfgtree");
		tree.addTreeSelectionListener(new PeerTreeSelectionListener(swix));

		JPanel tcpcard = (JPanel) swix.find("id_cfgtcpcard");

		tcpcard.addComponentListener(
				new ComponentListener() {

					public void componentHidden(ComponentEvent e) {
					}

					public void componentMoved(ComponentEvent e) {
					}

					public void componentResized(ComponentEvent e) {
						JPanel card = (JPanel) e.getSource();
						JTable table = (JTable) swix.find("id_cfgtcptransporttable");
						JPanel panel = (JPanel) swix.find("id_cfgtcptransport");
						JScrollPane pane = (JScrollPane) swix.find("id_cfgtcpscrollpane");
						Insets insets = pane.getInsets();

						int height = card.getHeight() - panel.getHeight() - insets.top - insets.bottom;
						int width = panel.getWidth() - insets.left - insets.right;

						Dimension size = new Dimension(width, height);

						pane.setPreferredSize(size);
						pane.revalidate();
						card.validate();
					} 

					public void componentShown(ComponentEvent e) {
						this.componentResized(e);
					}
				});

		JPanel httpcard = (JPanel) swix.find("id_cfghttpcard");
		
		httpcard.addComponentListener(
				new ComponentListener() {

					public void componentHidden(ComponentEvent e) {
					}

					public void componentMoved(ComponentEvent e) {
					}

					public void componentResized(ComponentEvent e) {
						JPanel card = (JPanel) e.getSource();
						JTable table = (JTable) swix.find("id_cfghttptransporttable");
						JPanel panel = (JPanel) swix.find("id_cfghttptransport");
						JScrollPane pane = (JScrollPane) swix.find("id_cfghttpscrollpane");
						Insets insets = pane.getInsets();

						int height = card.getHeight() - panel.getHeight() - insets.top - insets.bottom;
						int width = panel.getWidth() - insets.left - insets.right;

						Dimension size = new Dimension(width, height);

						pane.setPreferredSize(size);
						pane.revalidate();
						card.validate();
					} 

					public void componentShown(ComponentEvent e) {
						this.componentResized(e);
					}

				});
	}

    /**
     * @return configurator
     */
    public static net.jxta.ext.config.Configurator getConfigurator() {
        return cfg;
    }

	/* (non-Javadoc)
	 * @see net.jxta.ext.config.AbstractConfigurator#createPlatformConfig(net.jxta.ext.config.Configurator)
	 */
	public PlatformConfig createPlatformConfig(net.jxta.ext.config.Configurator configurator) throws ConfiguratorException {
		// TODO Auto-generated method stub
		return null;
	}

	public Configurator(URI jxtaHome) {
		cfg = new net.jxta.ext.config.Configurator(jxtaHome);
	}

	public PlatformConfig configure() throws ConfiguratorException {
        swix = new SwingEngine(this);
        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            JFrame cfg = (JFrame) swix.render(Configurator.DESCRIPTOR);
//            init(cfg);
//            cfg.setVisible(true);
//            while (cfg.isShowing());
//            cfg.dispose();
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                            JFrame cfg = (JFrame) swix.render(Configurator.DESCRIPTOR);
                            init(cfg);
                            cfg.setVisible(true);
                        // TODO might want to add some handling here...
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return cfg.getPlatformConfig();
	}
}

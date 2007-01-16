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
 *  $Id: ConfiguratorListener.java,v 1.1 2007/01/16 11:01:38 thomas Exp $
 */
package net.jxta.ext.config.ui;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

// import net.jxta.ext.config.Default;
import net.jxta.exception.ConfiguratorException;
import net.jxta.ext.config.Address;
import net.jxta.ext.config.Configurator;
import net.jxta.ext.config.HttpTransport;
import net.jxta.ext.config.MulticastAddress;
import net.jxta.ext.config.Profile;
import net.jxta.ext.config.Protocol;
import net.jxta.ext.config.ProxyAddress;
import net.jxta.ext.config.PublicAddress;
import net.jxta.ext.config.TcpTransport;
import net.jxta.ext.config.TcpTransportAddress;
import net.jxta.ext.config.Trace;
import net.jxta.ext.config.Transport;
import net.jxta.ext.config.Util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.swixml.SwingEngine;

/**
 *  Listener for ext:config:ui events
 *
 * @author     volker john [volkerj at jxta dot org]
 */
public class ConfiguratorListener implements ActionListener {
    
    private final transient static Logger LOG = Logger.getLogger(ConfiguratorListener.class.getName());
    
    private static final String DLG_AUTOLOAD = "net/jxta/ext/config/ui/resource/Autoloader.xml";
    
    static SwingEngine swix;
    static JTextField tcpPublicAddr;
    static JTextField tcpPublicPort;
    static JTextField httpPublicAddr;
    static JTextField httpPublicPort;
    static JTextField httpProxyAddr;
    static JTextField httpProxyPort;
    static JCheckBox tcpEnabled;
    static JCheckBox tcpManualEnabled;
    private JCheckBox tcpPublicEnabled;
    static JCheckBox httpEnabled;
    static JCheckBox httpManualEnabled;
    private JCheckBox httpPublicEnabled;
    static JCheckBox profileManualEnabled;
    static JTextField certFile;
    
    static JCheckBox rlyUsageEnabled;
    static JCheckBox rlySvcEnabled;
    static JCheckBox rdvSvcEnabled;
    static JCheckBox endpSvcEnabled;
    static JCheckBox proxySvcEnabled;
    
    static JPanel tcpPanel;
    static JPanel httpPanel;
    static JPanel rlyPanel;
    static JPanel rlySvcPanel;
    static JPanel rdvSvcPanel;
    static JPanel endpSvcPanel;
    static JPanel proxySvcPanel;
    
    static JCheckBox advEnabled;
    
    public ConfiguratorListener(SwingEngine swix) {
        ConfiguratorListener.swix = swix;
        
        // _TODO we should certainly get rid of the static variables ('twas just simple)
        profileManualEnabled = (JCheckBox) swix.find("id_peerprofilemethod");
        tcpPublicAddr = (JTextField) swix.find("id_tcppublicaddress");
        tcpPublicPort = (JTextField) swix.find("id_tcppublicport");
        httpPublicAddr = (JTextField) swix.find("id_httppublicaddress");
        httpPublicPort = (JTextField) swix.find("id_httppublicport");
        httpProxyAddr = (JTextField) swix.find("id_httpproxyaddress");
        httpProxyPort = (JTextField) swix.find("id_httpproxyport");
        tcpEnabled = (JCheckBox) swix.find("id_tcpenabled");
        tcpManualEnabled = (JCheckBox) swix.find("id_tcpmanualaddresses");
        tcpPublicEnabled = (JCheckBox) swix.find("id_tcphaspublicaddress");
        httpEnabled = (JCheckBox) swix.find("id_httpenabled");
        httpManualEnabled = (JCheckBox) swix.find("id_httpmanualaddresses");
        httpPublicEnabled = (JCheckBox) swix.find("id_httphaspublicaddress");
        certFile = (JTextField) swix.find("id_certfile");
        
        rlyUsageEnabled = (JCheckBox) swix.find("id_cfguserly");
        rlySvcEnabled = (JCheckBox) swix.find("id_cfgrlyservice");
        rdvSvcEnabled = (JCheckBox) swix.find("id_cfgrdvservice");
        endpSvcEnabled = (JCheckBox) swix.find("id_cfgendpservice");
        proxySvcEnabled = (JCheckBox) swix.find("id_cfgproxyservice");
        
        // do we have advanced settings available in the tree?
        advEnabled = (JCheckBox) swix.find("id_advenabled");
        
        // find the panels we need
        tcpPanel = (JPanel) swix.find("id_cfgtcpcard");
        httpPanel = (JPanel) swix.find("id_cfghttpcard");
        rlyPanel = (JPanel) swix.find("id_cfgrlycard");
        rlySvcPanel = (JPanel) swix.find("id_cfgrlyservicecard");
        rdvSvcPanel = (JPanel) swix.find("id_cfgrdvservicecard");
        endpSvcPanel = (JPanel) swix.find("id_cfgendpservicecard");
        proxySvcPanel = (JPanel) swix.find("id_cfgproxyservicecard");
        
        try {
            
            // get data from existing platform config, if any.
            Configurator cfg = net.jxta.ext.config.ui.Configurator.getConfigurator();
            ((JTextField)swix.find("id_peername")).setText(cfg.getName());
            
            // TODO principal does not equal secure peername!
            ((JTextField)swix.find("id_securepeername")).setText(cfg.getPrincipal());
            
            // do the transport thing!
            Iterator transports = cfg.getTransports().iterator();
            while (transports.hasNext()) {
                Transport t = (Transport)transports.next();
                if (t instanceof TcpTransport) {
                    // TODO same for http: why does getPublicAdresses return <null>?
                    Iterator a = t.getAddresses().iterator();
                    if (a.hasNext()) {
                        JComboBox auto = (JComboBox)swix.find("id_tcpautoentry");
                        TcpAddressComboModel model = ((TcpAddressComboModel) auto.getModel());
                        model.removeAllElements();
                        model.addDefault();
                        while (a.hasNext()) {
				URI addr = ((TcpTransportAddress)a.next()).getAddress();
				model.addElement(
					addr.getScheme()+Protocol.URI_DELIMITER+
					addr.getHost());
                        }
                    }
                } else if (t instanceof HttpTransport) {
                    Iterator a = t.getAddresses().iterator();
                    if (a.hasNext()) {
                        JComboBox auto = (JComboBox)swix.find("id_httpautoentry");
                        TcpAddressComboModel model = ((TcpAddressComboModel) auto.getModel());
                        model.removeAllElements();
                        model.addDefault();
                        while (a.hasNext()) {
                            URI addr = ((Address)a.next()).getAddress();
                            model.addElement(
                                addr.getScheme()+Protocol.URI_DELIMITER+
                                addr.getHost());
                        }
                    }
                }
            }
            
            // TODO how to find out about endpoint service setting?
            // maybe: if value == default then ignore? If yes, how to get the default?
            // endpSvcEnabled.setSelected(cfg.isEndpointService());
            
            // TODO the frigging isProxy() does not work?!
            proxySvcEnabled.setSelected(cfg.isProxy());
            
            JComboBox trace = ((JComboBox)swix.find("id_tracelevel"));
            trace.setSelectedItem(cfg.getTrace().toString());
            
            
            JCheckBox rdvSvcAutostart = (JCheckBox) swix.find("id_rdvautostart");
            JTextField rdvSvcAutoInterval = (JTextField) swix.find("id_rdvautostartinterval");
            
            if (cfg.isRendezVous()) {
                rdvSvcEnabled.setSelected(true);
                // TODO getRdvAutostart vs. isRdvAutostart?
                rdvSvcAutostart.setSelected(cfg.isRendezVousAutoStart());
                enable(((JPanel)(rdvSvcPanel.getComponents()[0])).getComponents());
                rdvSvcAutoInterval.setText(String.valueOf(cfg.getRendezVousAutoStart()));
            }
            
            if (cfg.isRelay()) {
                rlySvcEnabled.setSelected(true);
                enable(((JPanel)(rlySvcPanel.getComponents()[0])).getComponents());
                JTextField rlyQueueSize = (JTextField) swix.find("id_cfgrlyqueuesize");
                JCheckBox rlyIncomingEnable = (JCheckBox) swix.find("id_cfgrlyincoming");
                JTextField rlyIncomingMax = (JTextField) swix.find("id_rlyincomingmax");
                JTextField rlyIncomingLease = (JTextField) swix.find("id_rlyincominglease");
                JCheckBox rlyOutgoingEnable = (JCheckBox) swix.find("id_cfgrlyoutgoing");
                JTextField rlyOutgoingMax = (JTextField) swix.find("id_rlyoutgoingmax");
                JTextField rlyOutgoingLease = (JTextField) swix.find("id_rlyoutgoinglease");
                
                rlyQueueSize.setText(String.valueOf(cfg.getRelayQueueSize()));
                rlyIncomingEnable.setSelected(cfg.isRelayIncoming());
                int max = cfg.getRelayIncomingMaximum();
                rlyIncomingMax.setText(String.valueOf(max));
                rlyIncomingLease.setText(String.valueOf(cfg.getRelayIncomingLease()));
                rlyOutgoingEnable.setSelected(cfg.isRelayOutgoing());
                rlyOutgoingMax.setText(String.valueOf(cfg.getRelayOutgoingMaximum()));
                rlyOutgoingLease.setText(String.valueOf(cfg.getRelayOutgoingLease()));
            }
            
            // TODO any way to find out whether manual settings are to be employed?
            tcpManualEnabled = (JCheckBox) swix.find("id_tcpmanualaddresses");
            httpManualEnabled = (JCheckBox) swix.find("id_httpmanualaddresses");
            tcpManualEnabled.setSelected(false);
            httpManualEnabled.setSelected(false);
            
            Iterator i = cfg.getRendezVous().iterator();
            JList tcplist = (JList) swix.find("id_cfgtcprdvlist");
            DefaultListModel tcpmodel = (DefaultListModel) tcplist.getModel();
            JList httplist = (JList) swix.find("id_cfghttprdvlist");
            DefaultListModel httpmodel = (DefaultListModel) httplist.getModel();
            
            while (i.hasNext()) {
                URI addr = (URI)i.next();
                // XXX if there ever are other rdvs than tcp and http, this is the place to change
                // of course you didn't notice, the would require a more flexible handling of the model stuff
                if (addr.getScheme().equalsIgnoreCase(Protocol.TCP)) {
                    tcpmodel.addElement(addr.getAuthority());
                } else {
                    httpmodel.addElement(addr.getAuthority());
                }
            }
            
            
            // the relay settings.
            // how to get information on whether relays are to be used or not?
            
            i = cfg.getRelays().iterator();
            tcplist = (JList) swix.find("id_cfgtcprlylist");
            tcpmodel = (DefaultListModel) tcplist.getModel();
            httplist = (JList) swix.find("id_cfghttprlylist");
            httpmodel = (DefaultListModel) httplist.getModel();
            
            if (i.hasNext()) {
                rlyUsageEnabled.setSelected(true);
                enable(((JPanel)(rlyPanel.getComponents()[0])).getComponents());
                setPublicHttp();
                setHttpProxy();
                while (i.hasNext()) {
                    URI addr = (URI)i.next();
                    // XXX if there ever are other rdvs than tcp and http, this is the place to change
                    // of course you didn't notice, the would require a more flexible handling of the model stuff
                    if (addr.getScheme().equalsIgnoreCase(Protocol.TCP)) {
                        tcpmodel.addElement(addr.getAuthority());
                    } else {
                        httpmodel.addElement(addr.getAuthority());
                    }
                }
            } else {
                rlyUsageEnabled.setSelected(false);
                disable(((JPanel)(rlyPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(rlyUsageEnabled);
                requestFocus(rlyUsageEnabled);
            }
            
            //		tcpPublicPort = (JTextField) swix.find("id_tcppublicport");
            //		httpPublicAddr = (JTextField) swix.find("id_httppublicaddress");
            //		httpPublicPort = (JTextField) swix.find("id_httppublicport");
            //		httpProxyAddr = (JTextField) swix.find("id_httpproxyaddress");
            //		httpProxyPort = (JTextField) swix.find("id_httpproxyport");
            //		tcpEnabled = (JCheckBox) swix.find("id_tcpenabled");
            //		httpEnabled = (JCheckBox) swix.find("id_httpenabled");
            //		httpManualEnabled = (JCheckBox) swix.find("id_httpmanualaddresses");
            //		httpPublicEnabled = (JCheckBox) swix.find("id_httphaspublicaddress");
            //		certFile = (JTextField) swix.find("id_certfile");
            
        } catch (Exception e) {
            LOG.error("Could not retrieve existing configuration, aborting.", e);
        }
        
    }
    
        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
    public void actionPerformed(ActionEvent e) {
        
        // _TODO need to create constants for IDs & ACs
        String command = e.getActionCommand();
        if (("enableTCPPublicAddress").equals(command)) {
            setPublicTcp();
        } else if (("enableHTTPPublicAddress").equals(command)) {
            setPublicHttp();
        } else if (("enableHTTPProxy").equals(command)) {
            setHttpProxy();
        } else if (("enableTCPSettings").equals(command)) {
            if (tcpEnabled.isSelected()) {
                enable(((JPanel)(tcpPanel.getComponents()[0])).getComponents());
                setPublicTcp();
                addTcpTransport();
            } else {
                clearTcpTransports();
            }
        } else if (("AC_enableuserly").equals(command)) {
            if (rlyUsageEnabled.isSelected()) {
                enable(((JPanel)(rlyPanel.getComponents()[0])).getComponents());
                setPublicHttp();
                setHttpProxy();
            } else {
                disable(((JPanel)(rlyPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(rlyUsageEnabled);
                requestFocus(rlyUsageEnabled);
            }
        } else if (("AC_enablerdvservice").equals(command)) {
            if (rdvSvcEnabled.isSelected()) {
                enable(((JPanel)(rdvSvcPanel.getComponents()[0])).getComponents());
            } else {
                disable(((JPanel)(rdvSvcPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(rdvSvcEnabled);
                requestFocus(rdvSvcEnabled);
            }
        } else if (("AC_enablerlyservice").equals(command)) {
            if (rlySvcEnabled.isSelected()) {
                enable(((JPanel)(rlySvcPanel.getComponents()[0])).getComponents());
            } else {
                disable(((JPanel)(rlySvcPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(rlySvcEnabled);
                requestFocus(rlySvcEnabled);
            }
        } else if (("AC_enableendpservice").equals(command)) {
            if (endpSvcEnabled.isSelected()) {
                enable(((JPanel)(endpSvcPanel.getComponents()[0])).getComponents());
            } else {
                disable(((JPanel)(endpSvcPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(endpSvcEnabled);
                requestFocus(endpSvcEnabled);
            }
        } else if (("AC_enableproxyservice").equals(command)) {
            if (proxySvcEnabled.isSelected()) {
                enable(((JPanel)(proxySvcPanel.getComponents()[0])).getComponents());
            } else {
                disable(((JPanel)(proxySvcPanel.getComponents()[0])).getComponents());
                // make sure we can toggle settings after we disabled all components ;-)
                enable(proxySvcEnabled);
                requestFocus(proxySvcEnabled);
            }
        } else if (("enableManualTcpAddress").equals(command)) {
            JComboBox auto = (JComboBox)swix.find("id_tcpautoentry");
            JTextField manual = (JTextField)swix.find("id_tcpmanualentry");
            if (tcpManualEnabled.isSelected()) {
                manual.setMinimumSize(auto.getMinimumSize());
                manual.setPreferredSize(auto.getPreferredSize());
                manual.setMaximumSize(auto.getMaximumSize());
                show((Component)manual);
                hide((Component)auto);
            } else {
                hide((Component)manual);
                show((Component)auto);
            }
        } else if (("enableManualHttpAddress").equals(command)) {
            JComboBox auto = (JComboBox)swix.find("id_httpautoentry");
            JTextField manual = (JTextField)swix.find("id_httpmanualentry");
            if (httpManualEnabled.isSelected()) {
                manual.setMinimumSize(auto.getMinimumSize());
                manual.setPreferredSize(auto.getPreferredSize());
                manual.setMaximumSize(auto.getMaximumSize());
                show((Component)manual);
                hide((Component)auto);
            } else {
                hide((Component)manual);
                show((Component)auto);
            }
        } else if (("enableHTTPSettings").equals(command)) {
            if (httpEnabled.isSelected()) {
                enable(((JPanel)(httpPanel.getComponents()[0])).getComponents());
                setPublicHttp();
                setHttpProxy();
                addHttpTransport();
            } else {
                clearHttpTransports();
            }
        } else if (("enableManualProfile").equals(command)) {
            // slightly different logic as we determine status depending on selection
            // TODO problem is that this code heavily relies on the XML code in Configurator.xml
            // in terms of component (panel) names etc.
            JComboBox auto = (JComboBox)swix.find("id_peerprofilecombo");
            JTextField manual = (JTextField)swix.find("id_manualpeerprofile");
            if (profileManualEnabled.isSelected()) {
                hide((Component)manual);
                show((Component)auto);
            } else {
                manual.setMinimumSize(auto.getMinimumSize());
                manual.setPreferredSize(auto.getPreferredSize());
                manual.setMaximumSize(auto.getMaximumSize());
                show((Component)manual);
                hide((Component)auto);
            }
        } else if (("importCAFile").equals(command)) {
            FileDialog getFile = new FileDialog( new JFrame(), "Select Root Certificate File", FileDialog.LOAD );
            getFile.setDirectory( System.getProperty( "user.home" ) );
            getFile.show();
            
            String inDir = getFile.getDirectory();
            String theFile = getFile.getFile();
            
            if( (null != inDir) && (null != theFile) ) {
                String fullPath = inDir + theFile;
                certFile.setText( fullPath );
            }
        } else if (("downloadrdvrly").equals(command)) {
            try {
                SwingEngine swe = new SwingEngine();
                JDialog ldr = (JDialog) swe.render(DLG_AUTOLOAD);
                swe.setActionListener(ldr, new AutoLoaderListener(swe, swix));
                
                // TODO shall we make Default public to allow reflection?
                // get rdv bootstrap address from Default class
                // otherwise, we could simply make it a literal using SwiXml
                // to (possibly even get localized) adresses for the bootstrap
//				JTextField tf;
//				Class clazz = Default.class;
//
//				Field field;
//				try {
//					field = clazz.getField("RENDEZVOUS_BOOTSTRAP_ADDRESS");
//					tf  = (JTextField) swe.find("id_rdvbootstrap");
//					tf.setText(field.get(null).toString());
//					field = clazz.getField("RELAY_BOOTSTRAP_ADDRESS");
//					tf  = (JTextField) swe.find("id_rlybootstrap");
//					tf.setText(field.get(null).toString());
//
//					// TODO we should consider making this smaller
//					// by addding proper reporting of the exceptions
//				} catch (SecurityException se) {
//					// TODO Auto-generated catch block
//					se.printStackTrace();
//				} catch (NoSuchFieldException nsfe) {
//					// TODO Auto-generated catch block
//					nsfe.printStackTrace();
//				} catch (IllegalArgumentException iarge) {
//					// TODO Auto-generated catch block
//					iarge.printStackTrace();
//				} catch (IllegalAccessException iacce) {
//					// TODO Auto-generated catch block
//					iacce.printStackTrace();
//				}
                
                JTextField tf;
                tf  = (JTextField) swe.find("id_rdvbootstrap");
                // _TODO get rdv & rly addresses from somewhere else!
                tf.setText("http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?2");
                tf  = (JTextField) swe.find("id_rlybootstrap");
                tf.setText("http://rdv.jxtahosts.net/cgi-bin/relays.cgi?2");
                
                ldr.setVisible(true);
                ldr.dispose();
                swe = null;
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(swix.getRootComponent(),
                    "Could not create dialog, please check log.",
                    "Internal Error",
                    JOptionPane.ERROR_MESSAGE);
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed to create rdv/rly loader dialog", ex);
                }
            }
        } else if (("advancedAction").equals(command)) {
            if (advEnabled.isSelected()) {
                insertAdvNodes();
            } else {
                removeAdvNodes();
            }
        } else if (("AC_addtcprdv").equals(command)) {
            JList list = (JList) swix.find("id_cfgtcprdvlist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            String addr, port;
            addr = ((JTextField)swix.find("id_cfgtcprdvaddr")).getText();
            port = ((JTextField)swix.find("id_cfgtcprdvport")).getText();
            // TODO more serious address checking required.
            if (!addr.equals("") && !port.equals("")) {
                model.addElement(addr+":"+port);
            }
        } else if (("AC_deltcprdv").equals(command)) {
            JList list = (JList) swix.find("id_cfgtcprdvlist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            int i;
            if ((i = list.getSelectedIndex()) >= 0) {
                model.remove(i);
            }
        } else if (("AC_addhttprdv").equals(command)) {
            JList list = (JList) swix.find("id_cfghttprdvlist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            String addr, port;
            addr = ((JTextField)swix.find("id_cfghttprdvaddr")).getText();
            port = ((JTextField)swix.find("id_cfghttprdvport")).getText();
            // TODO more serious address checking required.
            if (!addr.equals("") && !port.equals("")) {
                model.addElement(addr+":"+port);
            }
        } else if (("AC_delhttprdv").equals(command)) {
            JList list = (JList) swix.find("id_cfghttprdvlist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            int i;
            if ((i = list.getSelectedIndex()) >= 0) {
                model.remove(i);
            }
        } else if (("AC_addtcprly").equals(command)) {
            JList list = (JList) swix.find("id_cfgtcprlylist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            String addr, port;
            addr = ((JTextField)swix.find("id_cfgtcprlyaddr")).getText();
            port = ((JTextField)swix.find("id_cfgtcprlyport")).getText();
            // TODO more serious address checking required.
            if (!addr.equals("") && !port.equals("")) {
                model.addElement(addr+":"+port);
            }
        } else if (("AC_deltcprly").equals(command)) {
            JList list = (JList) swix.find("id_cfgtcprlylist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            int i;
            if ((i = list.getSelectedIndex()) >= 0) {
                model.remove(i);
            }
        } else if (("AC_addhttprly").equals(command)) {
            JList list = (JList) swix.find("id_cfghttprlylist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            String addr, port;
            addr = ((JTextField)swix.find("id_cfghttprlyaddr")).getText();
            port = ((JTextField)swix.find("id_cfghttprlyport")).getText();
            // TODO more serious address checking required.
            if (!addr.equals("") && !port.equals("")) {
                model.addElement(addr+":"+port);
            }
        } else if (("AC_delhttprly").equals(command)) {
            JList list = (JList) swix.find("id_cfghttprlylist");
            DefaultListModel model = (DefaultListModel) list.getModel();
            int i;
            if ((i = list.getSelectedIndex()) >= 0) {
                model.remove(i);
            }
        } else if (("AC_addtcptransp").equals(command)) {
            addTcpTransport();
        } else if (("AC_deltcptransp").equals(command)) {
            deleteTcpTransport();
        } else if (("AC_addhttptransp").equals(command)) {
            addHttpTransport();
        } else if (("AC_OK").equals(command)) {
            // Validate, save (propagate to ext:config) & leave on success
            if (validate() && writeConfig()) {
                JFrame cfg = (JFrame) swix.find("id_optionsframe");
                cfg.dispatchEvent(new WindowEvent(cfg, WindowEvent.WINDOW_CLOSING));
            }
        } else if (("AC_CANCEL").equals(command)) {
            // Discard & leave
            JFrame cfg = (JFrame) swix.find("id_optionsframe");
            cfg.dispatchEvent(new WindowEvent(cfg, WindowEvent.WINDOW_CLOSING));
        }
    }
    
    /**
     * @return
     */
    private boolean validate() {
        boolean valid = false;
        
        // _TODO This could be much more sophisticated!
        
        // TODO do not forget about manual profiles
        // (JComboBox)swix.find("id_peerprofilecombo")).getSelectedItem().toString());
        
        // TODO if no rdv is configured, this should fail
        // ((JCheckBox)swix.find("id_useonlycfgedrdv")).isSelected());
        // use listmodel to check if rdvs have been entered
        // JList list = (JList) swix.find("id_cfgtcprdvlist");
        // JList list = (JList) swix.find("id_cfghttprdvlist");
        // the same applies for relays
        
        
        // TODO might want to address manual network interfaces
        // JTable table = (JTable) swix.find("id_cfgtcptransporttable");
        // TcpTransportTableModel tmodel = (TcpTransportTableModel) table.getModel();
        // same for http
        
        String peername = ((JTextField)swix.find("id_peername")).getText();
        String secpeername = ((JTextField)swix.find("id_securepeername")).getText();
        String peerpw = new String(((JPasswordField)swix.find("id_peerpassword")).getPassword());
        String verify = new String(((JPasswordField)swix.find("id_peerpasswordverify")).getPassword());
        
        if (peername.equals("")) {
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Peer name cannot be empty.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
        } else if (secpeername.equals("")) {
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Secure peer name cannot be empty.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
        } else if (peerpw.equals("") || verify.equals("")) {
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Password & Verification must not be empty.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
        } else if (!peerpw.equals(verify)) {
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Passwords do not match.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
        } else {
            valid = true;
        }
        
        return valid;
    }
    
    /**
     * @return
     *
     */
    private boolean writeConfig() {
        boolean saved = false;
        
        // _TODO need to add checking whether we have manual or auto profiles;
        // URL profileURL = new URL(((JComboBox)swix.find("id_peerprofilecombo")).getSelectedItem().toString());
        // for now, go with auto profiles.
        Profile profile = Profile.get(((JComboBox)swix.find("id_peerprofilecombo")).getSelectedItem().toString());
        //Configurator cfg = new Configurator(profile);
        Configurator cfg = net.jxta.ext.config.ui.Configurator.getConfigurator();
        
        try {
            
            // if advanced is not enabled, we can ignore many of the settings and rely
            // on the profile. Otherwise, use profile and then introduce individual changes
            // as specified in advanced configuration by the user.
            if (advEnabled.isSelected()) {
                
                // network overrides
                cfg.setRendezVousDiscovery(
                    ((JCheckBox)swix.find("id_useonlycfgedrdv")).isSelected());
                
                // someone has gone all the way to set some rdv addresses
                JList list = (JList) swix.find("id_cfgtcprdvlist");
                DefaultListModel model = (DefaultListModel) list.getModel();
                
                Vector alist = new Vector();
                Enumeration each = model.elements();
                
                while (each.hasMoreElements()) {
                    alist.addElement(new URI(Protocol.TCP_URI + each.nextElement()));
                }
                
                list = (JList) swix.find("id_cfghttprdvlist");
                model = (DefaultListModel) list.getModel();
                each = model.elements();
                
                while (each.hasMoreElements()) {
                    alist.addElement(new URI(Protocol.HTTP_URI + each.nextElement()));
                }
                
                if (!alist.isEmpty()) {
                    cfg.clearRendezVous();
                    cfg.setRendezVous(alist);
                }
                
                // Act as JXTA Proxy on NETWORK | RDV;
                // also corresponds to cfg.isProxy call
                cfg.setProxy(proxySvcEnabled.isSelected());
                
                if (rlyUsageEnabled.isSelected()) {
                    // someone has gone all the way to set some rly addresses
                    list = (JList) swix.find("id_cfgtcprlylist");
                    model = (DefaultListModel) list.getModel();
                    
                    alist = new Vector();
                    each = model.elements();
                    
                    while (each.hasMoreElements()) {
                        alist.addElement(new URI(Protocol.TCP_URI + each.nextElement()));
                    }
                    
                    list = (JList) swix.find("id_cfghttprlylist");
                    model = (DefaultListModel) list.getModel();
                    each = model.elements();
                    
                    while (each.hasMoreElements()) {
                        alist.addElement(new URI(Protocol.HTTP_URI + each.nextElement()));
                    }
                    
                    cfg.setRelaysDiscovery(true);
                    cfg.clearRelays();
                    cfg.setRelays(alist);
                } else {
                    cfg.setRelaysDiscovery(false);
                }
                
                // transport overrides, TCP
                if (tcpEnabled.isSelected()) {
                    
                    JTable table = (JTable) swix.find("id_cfgtcptransporttable");
                    TcpTransportTableModel tmodel = (TcpTransportTableModel) table.getModel();
                    
                    TcpTransport tcp = null;
                    each = tmodel.getRows().elements();
                    while (each.hasMoreElements()) {
                        
                        tcp = new TcpTransport();
                        TcpTransportAddress tcpaddr = new TcpTransportAddress();
                        Object[] row = (Object[]) each.nextElement();
                        
                        String addr;
                        
                        // _TODO externalize string
                        if (row[0].equals("Any/All Local Addresses")) {
                            // _TODO allow for more than one local address (depends
                            // on Util.getLocalHost return value (currently, a single address))
                            addr = Util.getLocalHost();
                        } else {
                            addr = (String) row[0];
                        }
                        
                        String port = (String)row[1];
                        boolean hidePrivate = ((Boolean)row[2]).booleanValue();
                        boolean enableOut = ((Boolean)row[3]).booleanValue();
                        boolean enableIn = ((Boolean)row[4]).booleanValue();
                        String publicAddr = (!((String)row[5]).equals("./.") ? (String)row[5] : null);
                        String publicPort = (!((String)row[6]).equals("./.") ? (String)row[6] : null);
                        boolean enableMC = ((Boolean)row[7]).booleanValue();
                        
                        tcpaddr.setAddress(new URI("tcp://" +
                            addr + ":" +
                            port));
                        tcpaddr.setPortRange(0);
                        
                        // Multicast address is required and we will just use default
                        // values; however, it may be disabled.
                        MulticastAddress mc = new MulticastAddress();
                        mc.setMulticast(enableMC);
                        tcpaddr.setMulticastAddress(mc);
                        
                        tcp.setAddress(tcpaddr);
                        tcp.setProxy(false);
                        tcp.setEnabled(true);
                        tcp.setIncoming(enableIn);
                        tcp.setOutgoing(enableOut);
                        
                        if (null != publicAddr && null != publicPort) {
                            PublicAddress pubAddr = new PublicAddress();
                            pubAddr.setAddress(new URI(Protocol.TCP_URI +
                                publicAddr + ":" +
                                publicPort
                                ));
                            tcp.setPublicAddress(pubAddr);
                        }
                        
                        cfg.addTransport(tcp);
                        
                    }
                    
                }
                
                // transport overrides, HTTP
                if (httpEnabled.isSelected()) {
                    
                    JTable table = (JTable) swix.find("id_cfghttptransporttable");
                    HttpTransportTableModel hmodel = (HttpTransportTableModel) table.getModel();
                    
                    HttpTransport http = null;
                    each = hmodel.getRows().elements();
                    while (each.hasMoreElements()) {
                        
                        http = new HttpTransport();
                        Address httpaddr = new Address();
                        Object[] row = (Object[]) each.nextElement();
                        
                        String addr;
                        
                        // _TODO externalize string
                        if (row[0].equals("Any/All Local Addresses")) {
                            // _TODO allow for more than one local address (depends
                            // on Util.getLocalHost return value (currently, a single address))
                            addr = Util.getLocalHost();
                        } else {
                            addr = (String) row[0];
                        }
                        
                        String port = (String)row[1];
                        boolean hidePrivate = ((Boolean)row[2]).booleanValue();
                        boolean enableOut = ((Boolean)row[3]).booleanValue();
                        boolean enableIn = ((Boolean)row[4]).booleanValue();
                        String publicAddr = (!((String)row[5]).equals("./.") ? (String)row[5] : null);
                        String publicPort = (!((String)row[6]).equals("./.") ? (String)row[6] : null);
                        boolean enableProxy = ((Boolean)row[7]).booleanValue();
                        String proxyAddr = (!((String)row[8]).equals("./.") ? (String)row[8] : null);
                        String proxyPort = (!((String)row[9]).equals("./.") ? (String)row[9] : null);
                        
                        httpaddr.setAddress(new URI(Protocol.HTTP_URI +
                            addr + ":" +
                            port
                            ));
                        httpaddr.setPortRange(0);
                        
                        http.setAddress(httpaddr);
                        http.setEnabled(true);
                        http.setIncoming(enableIn);
                        http.setOutgoing(enableOut);
                        
                        if (null != publicAddr && null != publicPort) {
                            PublicAddress pubAddr = new PublicAddress();
                            pubAddr.setAddress(new URI(Protocol.TCP_URI +
                                publicAddr + ":" +
                                publicPort
                                ));
                            http.setPublicAddress(pubAddr);
                        }
                        
                        if (enableProxy) {
                            ProxyAddress proxy = new ProxyAddress();
                            proxy.setAddress(new URI(Protocol.HTTP_URI +
                                proxyAddr + ":" +
                                proxyPort
                                ));
                            http.setProxyAddress(proxy);
                        }
                        http.setProxy(enableProxy);
                        
                        cfg.addTransport(http);
                        
                    }
                    
                }
                
                // services overrides
                if (rdvSvcEnabled.isSelected()) {
                    JCheckBox rdvSvcAutostart = (JCheckBox) swix.find("id_rdvautostart");
                    JTextField rdvSvcAutoInterval = (JTextField) swix.find("id_rdvautostartinterval");
                    if (rdvSvcAutostart.isSelected()) {
                        // deprecated
                        // cfg.setRendezVousAutoStart(true);
                        cfg.setRendezVousAutoStart(new Integer(rdvSvcAutoInterval.getText()).longValue());
                    } else {
                        cfg.setRendezVous(true);
                    }
                    
                }
                
                if (rlySvcEnabled.isSelected()) {
                    JTextField rlyQueueSize = (JTextField) swix.find("id_cfgrlyqueuesize");
                    JCheckBox rlyIncomingEnable = (JCheckBox) swix.find("id_cfgrlyincoming");
                    JTextField rlyIncomingMax = (JTextField) swix.find("id_rlyincomingmax");
                    JTextField rlyIncomingLease = (JTextField) swix.find("id_rlyincominglease");
                    JCheckBox rlyOutgoingEnable = (JCheckBox) swix.find("id_cfgrlyoutgoing");
                    JTextField rlyOutgoingMax = (JTextField) swix.find("id_rlyoutgoingmax");
                    JTextField rlyOutgoingLease = (JTextField) swix.find("id_rlyoutgoinglease");
                    
                    cfg.setRelay(true);
                    cfg.setRelayQueueSize(new Integer(rlyQueueSize.getText()).intValue());
                    
                    if (rlyIncomingEnable.isSelected()) {
                        cfg.setRelayIncoming(true);
                        cfg.setRelayIncomingMaximum(new Integer(rlyIncomingMax.getText()).intValue());
                        cfg.setRelayIncomingLease(new Integer(rlyIncomingLease.getText()).longValue());
                    }
                    if (rlyOutgoingEnable.isSelected()) {
                        cfg.setRelayOutgoing(true);
                        cfg.setRelayOutgoingMaximum(new Integer(rlyOutgoingMax.getText()).intValue());
                        cfg.setRelayOutgoingLease(new Integer(rlyOutgoingLease.getText()).longValue());
                    }
                }
                
                if (endpSvcEnabled.isSelected()) {
                    JTextField endpQueueSize = (JTextField) swix.find("id_cfgendpqueuesize");
                    cfg.setEndpointOutgoingQueueSize(new Integer(endpQueueSize.getText()).intValue());
                }
                
                if (proxySvcEnabled.isSelected()) {
                    JTextField peerProxyAddr = (JTextField) swix.find("id_prxsvcuri");
                    cfg.setPeerProxyAddress(new URI(peerProxyAddr.getText()));
                }
                
            }
            
            // the following entries do not change regardless whether we are doing away
            // with an advanced or not so advanced configuration
            cfg.setName(((JTextField)swix.find("id_peername")).getText());
            cfg.setSecurity(((JTextField)swix.find("id_securepeername")).getText(),
                new String(((JPasswordField)swix.find("id_peerpassword")).getPassword()));
            
            // _TODO if we were to fill the trace combo with objects obtained from Trace.getTraces,
            // we possibly could simply assign the obj that corresponds to getSelectedItem.
            Trace trace = Trace.get(((JComboBox)swix.find("id_tracelevel")).getSelectedItem().toString());
            cfg.setTrace(trace);
            
            cfg.save();
            saved = true;
        } catch (ConfiguratorException e) {
            // TODO we might want to add additional information on the nature of the error
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Wrong configuration data, please check log.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            // only log to LOG when extensive information has been requested. All other
            // reporting of wrong doing is left to lower layers (ext:config)
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.error("User entered invalid information", e);
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(swix.getRootComponent(),
                "Invalid URI syntax, please check.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.error("User entered invalid information", e);
            }
        }
        
        return saved;
        
    }
    
    /**
     *
     */
    private void setHttpProxy() {
        if (!httpProxyAddr.isEnabled()) {
            enable(httpProxyAddr);
            enable(httpProxyPort);
        } else {
            disable(httpProxyAddr);
            disable(httpProxyPort);
        }
    }
    
    /**
     *
     */
    private void setPublicHttp() {
        if (httpPublicEnabled.isSelected()) {
            enable(httpPublicAddr);
            enable(httpPublicPort);
        } else {
            disable(httpPublicAddr);
            disable(httpPublicPort);
        }
    }
    
    /**
     *
     */
    private void setPublicTcp() {
        if (tcpPublicEnabled.isSelected()) {
            enable(tcpPublicAddr);
            enable(tcpPublicPort);
        } else {
            disable(tcpPublicAddr);
            disable(tcpPublicPort);
        }
    }
    
    /**
     *
     */
    private void addTcpTransport() {
        JTable table = (JTable) swix.find("id_cfgtcptransporttable");
        boolean hasPublicAddr = ((JCheckBox)swix.find("id_tcphaspublicaddress")).isSelected();
        TcpTransportTableModel model = (TcpTransportTableModel) table.getModel();
        
        model.addRow(new Object[] {
            tcpManualEnabled.isSelected() ?
                ((JTextField)swix.find("id_tcpmanualentry")).getText() :
                ((JComboBox)swix.find("id_tcpautoentry")).getSelectedItem().toString(),
                ((JTextField)swix.find("id_cfgtcptranspport")).getText(),
                new Boolean(((JCheckBox)swix.find("id_tcphideprivate")).isSelected()),
                new Boolean(((JCheckBox)swix.find("id_tcpenableoutgoing")).isSelected()),
                new Boolean(((JCheckBox)swix.find("id_tcpenableincoming")).isSelected()),
                hasPublicAddr ?
                    ((JTextField)swix.find("id_tcppublicaddress")).getText() :
                    "./.",
                hasPublicAddr ?
                    ((JTextField)swix.find("id_tcppublicport")).getText() :
                    "./.",
                new Boolean(((JCheckBox)swix.find("id_tcpmulticast")).isSelected())
        });
        model.fireTableDataChanged();
    }
    
    /**
     *
     */
    private void addHttpTransport() {
        JTable table = (JTable) swix.find("id_cfghttptransporttable");
        boolean hasPublicAddr = ((JCheckBox)swix.find("id_httphaspublicaddress")).isSelected();
        boolean hasProxyAddr = ((JCheckBox)swix.find("id_httpuseproxy")).isSelected();
        HttpTransportTableModel model = (HttpTransportTableModel) table.getModel();
        
        model.addRow(new Object[] {
            httpManualEnabled.isSelected() ?
                ((JTextField)swix.find("id_httpmanualentry")).getText() :
                ((JComboBox)swix.find("id_httpautoentry")).getSelectedItem().toString(),
                ((JTextField)swix.find("id_cfghttptranspport")).getText(),
                new Boolean(((JCheckBox)swix.find("id_httphideprivate")).isSelected()),
                new Boolean(((JCheckBox)swix.find("id_httpenableoutgoing")).isSelected()),
                new Boolean(((JCheckBox)swix.find("id_httpenableincoming")).isSelected()),
                hasPublicAddr ?
                    ((JTextField)swix.find("id_httppublicaddress")).getText() :
                    "./.",
                hasPublicAddr ?
                    ((JTextField)swix.find("id_httppublicport")).getText() :
                    "./.",
                new Boolean(((JCheckBox)swix.find("id_httpuseproxy")).isSelected()),
                hasProxyAddr ?
                    ((JTextField)swix.find("id_httpproxyaddress")).getText() :
                    "./.",
                hasProxyAddr ?
                    ((JTextField)swix.find("id_httpproxyport")).getText() :
                    "./."
                
        });
        model.fireTableDataChanged();
    }
    
    /**
     *
     */
    private void deleteTcpTransport() {
        JTable table = (JTable) swix.find("id_cfgtcptransporttable");
        TcpTransportTableModel model = (TcpTransportTableModel) table.getModel();
        
        if (table.getSelectedRow() > -1) {
            model.deleteRow(table.getSelectedRow());
            model.fireTableDataChanged();
            if (model.getRowCount() == 0) {
                // just deleted the last entry in the table may as well set tcp transports to disabled
                disable(((JPanel)(tcpPanel.getComponents()[0])).getComponents());
                // create default state for public address fields; otherwise,
                // toggling the checkbox may get out of synch with the edit fields
                ((JCheckBox)swix.find("id_tcphaspublicaddress")).setSelected(false);
                tcpEnabled.setSelected(false);
                enable(tcpEnabled);
                requestFocus(tcpEnabled);
            }
        }
    }
    
    /**
     *
     */
    private void clearTcpTransports() {
        JTable table = (JTable) swix.find("id_cfgtcptransporttable");
        TcpTransportTableModel model = (TcpTransportTableModel) table.getModel();
        
        for (int i = model.getRowCount()-1; i >= 0; i-- ) {
            model.deleteRow(i);
        }
        model.fireTableDataChanged();
        
        disable(((JPanel)(tcpPanel.getComponents()[0])).getComponents());
        // make sure we can toggle settings after we disabled all components ;-)
        enable(tcpEnabled);
        requestFocus(tcpEnabled);
        
    }
    
    
    /**
     *
     */
    private void clearHttpTransports() {
        JTable table = (JTable) swix.find("id_cfghttptransporttable");
        HttpTransportTableModel model = (HttpTransportTableModel) table.getModel();
        
        for (int i = model.getRowCount()-1; i >= 0; i-- ) {
            model.deleteRow(i);
        }
        model.fireTableDataChanged();
        
        disable(((JPanel)(httpPanel.getComponents()[0])).getComponents());
        // make sure we can toggle settings after we disabled all components ;-)
        enable(httpEnabled);
        requestFocus(httpEnabled);
        
    }
    
    
    /**
     *
     * Shows the component
     *
     * @param component
     */
    private void show(Component component) {
        component.setVisible(true);
    }
    
    /**
     *
     * Hides the component
     *
     * @param component
     */
    private void hide(Component component) {
        component.setVisible(false);
    }
    
    /**
     *
     */
    private void removeAdvNodes() {
        
        JTree tree = (JTree) swix.find("id_cfgtree");
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        
        // _TODO Missing node objects to become independent of labels
        int i = root.getChildCount();
        while (i > 0) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(--i);
            if (node.toString().equals("Network")) {
                model.removeNodeFromParent(node);
            } else if (node.toString().equals("Transport")) {
                model.removeNodeFromParent(node);
            } else if (node.toString().equals("Services")) {
                model.removeNodeFromParent(node);
            }
        }
        
    }
    
    /**
     *
     */
    private void insertAdvNodes() {
        
        // _TODO Missing localization for tree label here
        // _TODO Missing node objects to become independent of labels
        JTree tree = (JTree) swix.find("id_cfgtree");
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        
        DefaultMutableTreeNode network = new DefaultMutableTreeNode("Network");
        network.add(new DefaultMutableTreeNode("Rendezvous"));
        network.add(new DefaultMutableTreeNode("Relays"));
        model.insertNodeInto(network, root, root.getChildCount());
        
        DefaultMutableTreeNode transport = new DefaultMutableTreeNode("Transport");
        transport.add(new DefaultMutableTreeNode("tcp"));
        transport.add(new DefaultMutableTreeNode("http"));
        model.insertNodeInto(transport, root, root.getChildCount());
        
        DefaultMutableTreeNode services = new DefaultMutableTreeNode("Services");
        services.add(new DefaultMutableTreeNode("Rendezvous Service"));
        services.add(new DefaultMutableTreeNode("Relay Service"));
        services.add(new DefaultMutableTreeNode("Endpoint Service"));
        services.add(new DefaultMutableTreeNode("Proxy Service"));
        model.insertNodeInto(services, root, root.getChildCount());
        
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
            // recurse into nested panels
            // TODO there must be a better way to traverse these containers
            if (components[i] instanceof JPanel) {
                disable(((JPanel)components[i]).getComponents());
            } else if (components[i] instanceof JScrollPane) {
                disable(((JScrollPane)components[i]).getComponents());
            } else if (components[i] instanceof JViewport) {
                disable(((JViewport)components[i]).getComponents());
            } else {
                disable(components[i]);
            }
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
            // recurse into nested panels
            // TODO there must be a better way to traverse these containers
            if (components[i] instanceof JPanel) {
                enable(((JPanel)components[i]).getComponents());
            } else if (components[i] instanceof JScrollPane) {
                enable(((JScrollPane)components[i]).getComponents());
            } else if (components[i] instanceof JViewport) {
                enable(((JViewport)components[i]).getComponents());
            } else {
                enable(components[i]);
            }
        }
    }
    
    /**
     * @param component
     */
    private void enable(Component component) {
        component.setEnabled(true);
    }
    
}

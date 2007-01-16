/*
 * Copyright (c) 2001-2003 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: Boot.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import java.io.File;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.membership.InteractiveAuthenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.util.AwtUtils;

import net.jxta.impl.membership.pse.StringAuthenticator;

public class Boot {
    public static void main(String args[]) {
        System.out.println("Jxta is now taking off. Please fasten your seat belts and extinguish all smoking materials.");
        
        try {
            Thread.currentThread().setName( Boot.class.getName() + ".main()" );
            
//            // add a log appender for all messages to a file in the JXTA_HOME.
//            File homedir = new File( net.jxta.impl.config.Config.JXTA_HOME );
//
//            if( homedir.exists() ) {
//                File logfile = new File( homedir, "log.out" );
//                Logger rootLog = Logger.getRootLogger();
////              PatternLayout layout = new PatternLayout("<%-5p %d{ISO8601} %c{1}::%M:%L> %m\n");
//                PatternLayout layout = new PatternLayout("<%-5p %d{ABSOLUTE} %c{1}.%M:%L> %m\n");
//                FileAppender logAppender = new FileAppender(layout, logfile.getPath() );
//                logAppender.setAppend(false);
//                logAppender.setName("logfile");
//                logAppender.activateOptions();
//                rootLog.addAppender(logAppender);
//            }

            AwtUtils.initAsDaemon();
            
            PeerGroup p = PeerGroupFactory.newNetPeerGroup();
            p.startApp( args );
            
            MembershipService membership = p.getMembershipService();
            
            Credential cred = membership.getDefaultCredential();
            
            if( null == cred ) {
                AuthenticationCredential authCred = new AuthenticationCredential( p, "StringAuthentication", null );
                
                StringAuthenticator auth = null;
                try {
                    auth = (StringAuthenticator) membership.apply( authCred );
                } catch( Exception failed ) {
                    ;
                }
                
                if( null != auth ) {
                    auth.setAuth1_KeyStorePassword( System.getProperty( "net.jxta.tls.password", "").toCharArray() );
                    auth.setAuth2Identity( p.getPeerID() );
                    auth.setAuth3_IdentityPassword( System.getProperty( "net.jxta.tls.password", "").toCharArray() );
                    
                    if( auth.isReadyForJoin() ) {
                        membership.join( auth );
                    }
                }
            }
            
            cred = membership.getDefaultCredential();
            
            if( null == cred ) {
                AuthenticationCredential authCred = new AuthenticationCredential( p, "InteractiveAuthentication", null );
                
                InteractiveAuthenticator auth = (InteractiveAuthenticator) membership.apply( authCred );
                
                if( auth.interact() && auth.isReadyForJoin() ) {
                    membership.join( auth );
                }
            }
            
            p.unref();
        } catch(Throwable e) {
            System.out.flush(); // make sure output buffering doesn't wreck console display.
            System.err.println("Uncaught Throwable caught by 'main':");
            e.printStackTrace();
            System.exit(1);         // make note that we abended
        } finally {
            System.err.flush();
            System.out.flush();
        }
    }
}

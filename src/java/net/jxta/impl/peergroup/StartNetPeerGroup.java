/*
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
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: StartNetPeerGroup.java,v 1.1 2007/01/16 11:01:52 thomas Exp $
 */

package net.jxta.impl.peergroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import net.jxta.id.ID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.discovery.DiscoveryService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.platform.Application;
import net.jxta.exception.PeerGroupException;

/**
 * This code is responsible for configuring and starting the NetPeerGroup.
 *
 * <p/>Right now it starts an arbitrary peergroup; almost identical to
 * platform. (It actually borrows its adv).
 */
public class StartNetPeerGroup implements Application {

    PeerGroup pg;
    ID assignedID;
    Advertisement impl;

    /**
     *  {@inheritDoc}
     */
    public void stopApp() {}
                                                            
    private synchronized void delay() {
        try {
            wait(2000);
        } catch (Exception e) {}
    }

    /**
     *  {@inheritDoc}
     */
    public void init(PeerGroup pg, ID assignedID, Advertisement impl)
    throws PeerGroupException {
        this.pg = pg;
        this.assignedID = assignedID;
        this.impl = impl;
    }

    /**
     *  {@inheritDoc}
     */
    public int startApp(String[] arg) {
        try {
            // Attempt the recover the NetPeerGroup from a saved adv.
            PeerGroupAdvertisement npgAdv = null;

            // Look for the NetPeerGroup ADV on the net. In theory, there
            // should be some authoritative source for it. That is, whatever
            // advertisement named "NetPeerGroup" that you find, should describe
            // the same group with the same GID. In practice, for now, anyone
            // is an authoritative source, so there might be competing versions
            // seeded by different people who did not find any instance around
            // at some point in time and decided to seed it.

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            // Offer the newbie mode; others know what to do.
            // That is, race against the boot to type "D<enter>"
            // before we run that code below...

            boolean doDisco = false;
            String answer = "";

            try {
                if (in.ready()) {
                    answer = in.readLine();
                    if (answer.equals("D")) {
                        doDisco = true;
                    }
                }
            } catch (Exception e) {}

            // If the user typed "D", we'll do discovery anyhow.
            // otherwise, we'll try to be smart; if there ever was
            // a NetPeerGroup other than the default instantiated
            // here, we'll assume that the user uses discovery for the
            // netPeerGroup. Otherwise, we'll assume that the user
            // just always uses the hardwired default.

            boolean beSmart = false;
            Hashtable advs = new Hashtable();
            boolean first = true;

            DiscoveryService disco = null;
            disco = pg.getDiscoveryService();

            // Until we find something or the user decides to make a choice.

            try {
                while (in.ready()) in.readLine();
            } catch (Exception e) {}

            int loop = 0;
            while (true) {
                try {
                    if (in.ready()) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }

                if (first) {
                    System.out.println("Looking for NetPeerGroup advertisements.");
                    if (doDisco) {
                        System.out.println("Press <enter> to stop looking and "
                                           + "choose.");
                    }
                }

                Enumeration ae = null;
                try {
                    if (first) {
                        ae = disco.getLocalAdvertisements(DiscoveryService.GROUP
                                                          , "Name"
                                                          , "NetPeerGroup");
                    } else {
                        if (loop++ % 5 == 0)
                            disco.getRemoteAdvertisements(null
                                                          , DiscoveryService.GROUP
                                                          , "Name"
                                                          , "NetPeerGroup",10);
                        delay();
                        ae = disco.getLocalAdvertisements(DiscoveryService.GROUP
                                                          , "Name"
                                                          , "NetPeerGroup");
                    }
                } catch (IOException e) {
                    // found nothing!  move on
                }


                first = false;
                while (ae != null && ae.hasMoreElements()) {
                    PeerGroupAdvertisement a =
                        (PeerGroupAdvertisement) ae.nextElement();

                    if (a.getName().equals("NetPeerGroup")) {
                        String kw = a.getDescription();
                        if (kw == null) kw = "";
                        if (! advs.containsKey(kw+a.getPeerGroupID().toString())) {
                            String self = " ";
                            // Do a quick lookup for a past local instantiation.
                            // May not find anything if we do not publish padvs
                            // in the parent grp.
                            Enumeration paEnum = (new Vector()).elements();
                            try {
                                paEnum = disco.getLocalAdvertisements(DiscoveryService.PEER,
                                                                      "PID",
                                                                      pg.getPeerID().toString());
                            } catch (Exception justGiveUp) {}
                            while (paEnum.hasMoreElements()) {
                                PeerAdvertisement pa =
                                    (PeerAdvertisement)paEnum.nextElement();
                                if (pa.getPeerGroupID().equals(a.getPeerGroupID())) {
                                    self = "*";
                                    if (!kw.equals("NetPeerGroup by default")) {
                                        // This user has used the disco
                                        // option before therefore he
                                        // wants to use it.
                                        doDisco = doDisco || beSmart;
                                        break;
                                    }

                                }
                            }
                            advs.put(kw+a.getPeerGroupID().toString(), a);
                            System.out.println(self + " " + kw);
                        }
                    }
                }
                // If doDisco is not true the first time around (after
                // a local discovery), then we don't want it.
                if (! doDisco) {
                    advs.clear();
                    break;
                }
            }

            Vector advVect = new Vector();
            Enumeration each = advs.elements();
            // Make an ordered list and display it with numbering.
            int s = 0;
            while (each.hasMoreElements()) {
                PeerGroupAdvertisement a =
                    (PeerGroupAdvertisement) each.nextElement();
                String self = " ";
                // Do a quick lookup for a past local instantiation.
                // May not find anything if we do not publish padvs
                // in the parent grp.
                Enumeration paEnum = (new Vector().elements());
                try {
                    paEnum = disco.getLocalAdvertisements(DiscoveryService.PEER,
                                                          "PID",
                                                          pg.getPeerID().toString());
                } catch (Exception forgetIt) {}
                while (paEnum.hasMoreElements()) {
                    PeerAdvertisement pa =
                        (PeerAdvertisement) paEnum.nextElement();
                    if (pa.getPeerGroupID().equals(a.getPeerGroupID())) {
                        self = "*";
                        break;
                    }
                }
                System.out.println(self + s + ": " + a.getDescription());
                advVect.addElement(a);
                ++s;
            }

            if (s > 0) {
                while (true) {
                    System.out.println(" " + s + ": None of the above");
                    System.out.println("\n* = previously instantiated here\n");
                    try {
                        while (in.ready()) in.readLine();
                        System.out.print("Chose which advertisement you want"
                                         + " to use: ");
                        answer = in.readLine();
                        int i = s + 1;
                        try {
                            i = Integer.parseInt(answer);
                        } catch(Exception ignored) {}

                        if (i == s) break;
                        if (i >= 0 && i <= s) {
                            npgAdv = (PeerGroupAdvertisement) advVect.elementAt(i);
                            break;
                        }
                        i = 0;
                        System.out.println("\n\nFound:");
                        while (i < s) {
                            PeerGroupAdvertisement a =
                                (PeerGroupAdvertisement) advVect.elementAt(i);

                            String self = " ";
                            // Do a quick lookup for a past local instantiation.
                            // May not find anything if we do not publish padvs
                            // in the parent grp.
                            Enumeration paEnum =
                                disco.getLocalAdvertisements(DiscoveryService.PEER,
                                                             "PID",
                                                             pg.getPeerID().toString());
                            while (paEnum.hasMoreElements()) {
                                PeerAdvertisement pa =
                                    (PeerAdvertisement) paEnum.nextElement();
                                if (pa.getPeerGroupID().equals(a.getPeerGroupID())) {
                                    self = "*";
                                    break;
                                }
                            }
                            String kw = a.getDescription();
                            if (kw == null) kw = "";
                            System.out.println(self + i + ": " + kw);
                            i++;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }

            if (npgAdv != null) {
                // Good, just start it then. For now, we pass-on the conf.
                PeerGroup g = pg.newGroup(npgAdv);
                g.startApp(null);
                g.unref();
                pg.unref();

                return 0;
            }


            // Ok, then, maybe we should seed that group.
            // NB: seeding the NPG right now is somewhat useless; since it creates
            // nothing different form the default one. It is just there for the
            // sake of example.

            if (doDisco) {
                System.out.println("\nNo NetPeerGroup advertisement selected.");

                try {
                    while (in.ready()) in.readLine();
                    System.out.println("Do you want to start a shell in the "
                                       + "Platform Group ? [no]:");
                    answer = in.readLine();

                    if("yes".equalsIgnoreCase(answer)) {

                        try {
                            // Build a ModuleImplAdv for the shell; cannot rely
                            // on the shell having been published yet.
                            // FIXME: Modules built-in jxta should have
                            // a static method for that.
                            ModuleImplAdvertisement newAppAdv =
                                (ModuleImplAdvertisement)
                                AdvertisementFactory.newAdvertisement(
                                    ModuleImplAdvertisement.getAdvertisementType());

                            // The shell's spec id is a canned one.
                            newAppAdv.setModuleSpecID(PeerGroup.refShellSpecID);

                            // Don't known how to ID the document itself yet.
                            // Just use the spec
                            // FIXME: we should probably hash the adv, like for
                            // codat.
                            newAppAdv.setModuleSpecID(PeerGroup.refShellSpecID);

                            // Steal the compat, provider, and uri from the
                            // group's own impl adv. We DO want them identical in
                            // this case.
                            ModuleImplAdvertisement pgImpl =
                                (ModuleImplAdvertisement)pg.getImplAdvertisement();

                            newAppAdv.setCompat(pgImpl.getCompat());
                            newAppAdv.setUri(pgImpl.getUri());
                            newAppAdv.setProvider(pgImpl.getProvider());

                            // Make up a description
                            newAppAdv.setDescription("JXTA Shell");
                            // Tack in the class name
                            newAppAdv.setCode(
                                "net.jxta.impl.shell.bin.Shell.Shell");

                            // Finaly load and init the shell.
                            Application app = (Application)
                                              pg.loadModule(PeerGroup.applicationClassID,
                                                            newAppAdv);
                            app.startApp(null);
                            pg.unref();

                            return 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Error loading Shell.");
                        }
                    }
                    while (in.ready()) in.readLine();
                    System.out.println("Do you want to start the default net peer"
                                       + " group ? [yes]:");
                    answer = in.readLine();

                    if("no".equalsIgnoreCase(answer)) {
                        System.exit(0);
                    }
                } catch(Exception e2) {
                    e2.printStackTrace();
                    System.err.println("Unable to query desired behaviour. "
                                       + "Starting the default net peer group.");
                }
            }
            // We can start the net peer group now. (For now we pass-on the conf
            PeerGroup g = PeerGroupFactory.newNetPeerGroup(pg);

            g.startApp(null);
            g.unref();
            pg.unref();

            return 0;
        } catch(Exception anythingElse) {
            anythingElse.printStackTrace();
            return 1;
        }
    }
}

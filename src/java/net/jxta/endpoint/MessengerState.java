/*
 *
 * $Id: MessengerState.java,v 1.1 2007/01/16 11:01:27 thomas Exp $
 *
 * Copyright (c) 2004 Sun Microsystems, Inc.  All rights reserved.
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.endpoint;

/**
 * This class implements the complete standard messenger life cycle state machine that all messengers must obey. Compliant
 * messengers can be built by implementing and using this class as an engine to orchestrate descreet operations.
 *
 * In order to use this class, one must implement the various abstract Action methods, so that they trigger the required
 * operations.
 *
 * Synchronization has to be externally provided and usualy needs to extend around sections wider than just
 * the invocation of this classe's methods. For example, if the user of this class maintains a queue, the state of the
 * queue must be kept consistent with the invocation of {@link #msgsEvent}, {@link #saturatedEvent}, and {@link #idleEvent}, which all
 * denote different states of that queue. It is suggested to use the instance of this class as the synchronization object.
 **/
public abstract class MessengerState {

    // All the transition map setup is rather terse because java tends to make it extremely verbose. We do not want
    // to end up with 1000 lines of code for what amounts to initializing a table.

    // Below is a method reference. It permits to put "what to do" in a variable.  The doIt method is given the target object
    // because we want our entire transition table to be a static singleton. Otherwise it would cost too much initializing each
    // instance of this class.

    private interface Action {
        public void doIt(MessengerState s); 
    }

    // Action method "pointers". 
    // The transition table is static. Otherwise it would cost too much initializing each instance of this class.

    private static Action Connect   = new Action(){public void doIt(MessengerState s){s.connectAction();}                         };
    private static Action Closein   = new Action(){public void doIt(MessengerState s){s.closeInputAction();}                      };
    private static Action Start     = new Action(){public void doIt(MessengerState s){s.startAction();}                           };
    private static Action Closeout  = new Action(){public void doIt(MessengerState s){s.closeOutputAction();}                     };
    private static Action Failall   = new Action(){public void doIt(MessengerState s){s.failAllAction();}                         };
    private static Action Closeio   = new Action(){public void doIt(MessengerState s){s.closeInputAction();s.closeOutputAction();}};
    private static Action Closefail = new Action(){public void doIt(MessengerState s){s.closeInputAction();s.failAllAction();}    };
    private static Action Nop       = new Action(){public void doIt(MessengerState s){};                                          };

    // A state: what transition each event causes when in that state.
    private static class State {
        int number;
        State stResolve   ; Action acResolve   ;
        State stMsgs      ; Action acMsgs      ;
        State stSaturated ; Action acSaturated ;
        State stClose     ; Action acClose     ;
        State stShutdown  ; Action acShutdown  ;
        State stUp        ; Action acUp        ;
        State stDown      ; Action acDown      ;
        State stIdle      ; Action acIdle      ;

        void init(int stateNum, Object[] data) {
            number = stateNum;
            stResolve   = (State) data[0];  acResolve   = (Action) data[1];
            stMsgs      = (State) data[2];  acMsgs      = (Action) data[3];
            stSaturated = (State) data[4];  acSaturated = (Action) data[5];
            stClose     = (State) data[6];  acClose     = (Action) data[7];
            stShutdown  = (State) data[8];  acShutdown  = (Action) data[9];
            stUp        = (State) data[10]; acUp        = (Action) data[11];
            stDown      = (State) data[12]; acDown      = (Action) data[13];
            stIdle      = (State) data[14]; acIdle      = (Action) data[15];
        }
    }


    // All the states. (We put them together in a class essentially to simplify initialization).
    static class TransitionMap {

        static State Unresolved  = new State();
        static State ResPending  = new State();
        static State Resolving   = new State();
        static State ResolSat    = new State();
        static State Connected   = new State();
        static State Disconned   = new State();
        static State Reconning   = new State();
        static State ReconSat    = new State();
        static State Sending     = new State();
        static State SendingSat  = new State();
        static State ResClosing  = new State();
        static State ReconClosing= new State();
        static State Closing     = new State();
        static State Disconning  = new State();
        static State Unresable   = new State();
        static State Closed      = new State();
        static State Broken      = new State();

        // The states need to exist before init, because they refer to each other.
        // We overwrite them in-place with the complete data.

        static {

            Object[][] tmpMap = {
/* STATE             resolve,          msgs,              saturated,       close,               shutdown,           up,                down,                idle */

/*UNRESOLVED      */{Resolving,Connect,ResPending,Connect,ResolSat,Connect,Closed,Closein,      Broken,Closein,     Connected,Nop,     Unresolved,Nop,      Unresolved,Nop  },
/*RESOLPENDING    */{ResPending,Nop,   ResPending,Nop,    ResolSat,Nop,    ResClosing,Closein,  Broken,Closefail,   Sending,Start,     Unresable,Closefail, Resolving,Nop   },
/*RESOLVING       */{Resolving,Nop,    ResPending,Nop,    ResolSat,Nop,    Closed,Closein,      Broken,Closein,     Connected,Nop,     Unresable,Closein,   Resolving,Nop   },
/*RESOLSATURATED  */{ResolSat,Nop,     ResPending,Nop,    ResolSat,Nop,    ResClosing,Closein,  Broken,Closefail,   SendingSat,Start,  Unresable,Closefail, Resolving,Nop   },
/*CONNECTED       */{Connected,Nop,    Sending,Start,     SendingSat,Start,Closed,Closeio,      Broken,Closeio,     Connected,Nop,     Disconned,Nop,       Connected,Nop   },
/*DISCONNECTED    */{Disconned,Nop,    Reconning,Connect, ReconSat,Connect,Closed,Closein,      Broken,Closein,     Connected,Nop,     Disconned,Nop,       Disconned,Nop   },
/*RECONNECTING    */{Reconning,Nop,    Reconning,Nop,     ReconSat,Nop,    ReconClosing,Closein,Broken,Closefail,   Sending,Start,     Broken,Closefail,    Disconned,Nop   },
/*RECONSATURATED  */{ReconSat,Nop,     Reconning,Nop,     ReconSat,Nop,    ReconClosing,Closein,Broken,Closefail,   SendingSat,Start,  Broken,Closefail,    Disconned,Nop   },
/*SENDING         */{Sending,Nop,      Sending,Nop,       SendingSat,Nop,  Closing,Closein,     Disconning,Closeio, Sending,Nop,       Reconning,Connect,   Connected,Nop   },
/*SENDINGSATURATED*/{SendingSat,Nop,   Sending,Nop,       SendingSat,Nop,  Closing,Closein,     Disconning,Closeio, SendingSat,Nop,    ReconSat,Connect,    Connected,Nop   },
/*RESOLCLOSING    */{ResClosing,Nop,   ResClosing,Nop,    ResClosing,Nop,  ResClosing,Nop,      Broken,Failall,     Closing,Start,     Unresable,Failall,   ResClosing,Nop  },
/*RECONCLOSING    */{ReconClosing,Nop, ReconClosing,Nop,  ReconClosing,Nop,ReconClosing,Nop,    Broken,Failall,     Closing,Start,     Broken,Failall,      ReconClosing,Nop},
/*CLOSING         */{Closing,Nop,      Closing,Nop,       Closing,Nop,     Closing,Nop,         Disconning,Closeout,Closing,Nop,       ReconClosing,Connect,Closed,Closeout },
/*DISCONNECTING   */{Disconning,Nop,   Disconning,Nop,    Disconning,Nop,  Disconning,Nop,      Disconning,Nop,     Disconning,Nop,    Broken,Failall,      Broken,Nop      },
/*UNRESOLVABLE    */{Unresable,Nop,    Unresable,Nop,     Unresable,Nop,   Unresable,Nop,       Unresable,Nop,      Unresable,Closeout,Unresable,Nop,       Unresable,Nop   },
/*CLOSED          */{Closed,Nop,       Closed,Nop,        Closed,Nop,      Closed,Nop,          Closed,Nop,         Closed,Closeout,   Closed,Nop,          Closed,Nop      },
/*BROKEN          */{Broken,Nop,       Broken,Nop,        Broken,Nop,      Broken,Nop,          Broken,Nop,         Broken,Closeout,   Broken,Nop,          Broken,Nop      }
};


            // install the tmp map in its proper place.
            Unresolved.init  (Messenger.UNRESOLVED      , tmpMap[0]);
            ResPending.init  (Messenger.RESOLPENDING    , tmpMap[1]);
            Resolving.init   (Messenger.RESOLVING       , tmpMap[2]);
            ResolSat.init    (Messenger.RESOLSATURATED  , tmpMap[3]);
            Connected.init   (Messenger.CONNECTED       , tmpMap[4]);
            Disconned.init   (Messenger.DISCONNECTED    , tmpMap[5]);
            Reconning.init   (Messenger.RECONNECTING    , tmpMap[6]);
            ReconSat.init    (Messenger.RECONSATURATED  , tmpMap[7]);
            Sending.init     (Messenger.SENDING         , tmpMap[8]);
            SendingSat.init  (Messenger.SENDINGSATURATED, tmpMap[9]);
            ResClosing.init  (Messenger.RESOLCLOSING    , tmpMap[10]);
            ReconClosing.init(Messenger.RECONCLOSING    , tmpMap[11]);
            Closing.init     (Messenger.CLOSING         , tmpMap[12]);
            Disconning.init  (Messenger.DISCONNECTING   , tmpMap[13]);
            Unresable.init   (Messenger.UNRESOLVABLE    , tmpMap[14]);
            Closed.init      (Messenger.CLOSED          , tmpMap[15]);
            Broken.init      (Messenger.BROKEN          , tmpMap[16]);
        }

    }

    private volatile State state = null;

    /**
     * Constructs a new messenger state machine.
     * The transistion map is static and we refer to it only to grab the first state. After that, states
     * refer to each other. The only reason they are members in the map is so that we can make references during init.
     * @param connected If true, the initial state is {@link Messenger#CONNECTED} else, {@link Messenger#UNRESOLVED}.
     **/
    protected MessengerState(boolean connected) {

        state = connected ? TransitionMap.Connected : TransitionMap.Unresolved;
    }

    /**
     * @return The current state.
     **/
    public int getState() {
        // getState is always just a peek. It needs no sync.
        return state.number;
    }

    /**
     * Event input.
     **/

    public void resolveEvent()   { Action a = state.acResolve;   state = state.stResolve;   a.doIt(this); }
    public void msgsEvent()      { Action a = state.acMsgs;      state = state.stMsgs;      a.doIt(this); }
    public void saturatedEvent() { Action a = state.acSaturated; state = state.stSaturated; a.doIt(this); }
    public void closeEvent()     { Action a = state.acClose;     state = state.stClose;     a.doIt(this); }
    public void shutdownEvent()  { Action a = state.acShutdown;  state = state.stShutdown;  a.doIt(this); }
    public void upEvent()        { Action a = state.acUp;        state = state.stUp;        a.doIt(this); }
    public void downEvent()      { Action a = state.acDown;      state = state.stDown;      a.doIt(this); }
    public void idleEvent()      { Action a = state.acIdle;      state = state.stIdle;      a.doIt(this); }


    /**
     * Actions they're always called in sequence by event methods.
     *
     * Actions must not call event methods in sequence.
     **/

    /**
     * Try to make a connection. Called whenever transitioning from a state that neither needs nor has a connection to a state
     * that needs a connection and does not have it. Call upEvent when successfull, or downEvent when failed.
     **/
    protected abstract void connectAction();

    /**
     * Start sending. Called whenever transitioning to a state that has both a connection and messages to send from a state that
     * lacked either attributes. So, will be called after sending stopped due to broken cnx or idle condition.  Call downEvent
     * when stopping due to broken or closed connection, call {@link #idleEvent} when no pending message is left.
     **/
    protected abstract void startAction();

    /**
     * Reject new messages from now on. Called whenever transitioning from a state that is {@link Messenger#USABLE} to a state
     * that is not. No event expected once done.
     **/
    protected abstract void closeInputAction();

    /**
     * Drain pending messages, all failed. Called once output is down and there are still pending messages.
     * Call {@link #idleEvent} when done, as a normal result of no pending messages being left.
     **/
    protected abstract void failAllAction();

    /**
     * Force close output. Called whenever the underlying connection is to be discarded and never to be needed again.  That is
     * either because orderly close has completed, or shutdown is in progress. No event expected once done, but this action
     * <b>must</b> cause any sending in progress to stop eventually. The fact that the sending has stopped must be reported as
     * usual: either with a {@link #downEvent}, if the output closure caused the sending process to fail, or with an {@link
     * #idleEvent} if the sending of the last message could be sent successfully despite the attempt at interrupting it.
     *
     * Sending is said to be in progress if, and only if, the last call to startAction is more recent than the last call to
     * {@link #idleEvent} or {@link #downEvent}.
     *
     * It is advisable to also cancel an ongoing connect action, but not mandatory. If a {@link #connectAction} later succeeds,
     * then {@link #upEvent} <b>must</b> be called as usual. This will result in another call to {@link #closeOutputAction}.
     **/
    protected abstract void closeOutputAction();
}

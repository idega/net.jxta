/*
 *
 * $Id: ThreadedMessenger.java,v 1.1 2007/01/16 11:01:27 thomas Exp $
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.WeakHashMap;

import net.jxta.peergroup.PeerGroupID;

import net.jxta.impl.util.UnbiasedQueue;
import net.jxta.impl.util.TimeUtils;

/**
 * This is a messenger meant to be shared by multiple channels and automatically distribute the available bandwith among the channels.
 * This one is implemented with a dedicated background thread.
 **/

public abstract class ThreadedMessenger extends AbstractMessenger implements Runnable {
    
    /**
     *  Log4J Category
     **/
    private static final Logger LOG = Logger.getLogger(ThreadedMessenger.class.getName());

    /**
     * Our thread group.
     **/
    private static final ThreadGroup myThreadGroup = new ThreadGroup( "Threaded Messengers" );


    /**
     * The logical destination address of the other party (if we know it).
     **/
    private volatile EndpointAddress logicalDestination = null;

    /**
     * true if we have deliberately closed our input queue.
     * FIXME - jice@jxta.org 20040413: rely on queue.close instead ?
     **/
    private volatile boolean inputClosed = false;

    /**
     * Need to know which group the transports we use live in, so that we can suppress channel redirection when in the same group.
     * This is currently the norm.
     **/
    private PeerGroupID homeGroupID = null;

    /**
     * How long the BG thread can remain unemployed before bailing.
     **/
    private static final long THREAD_IDLE_DEAD = 4 * TimeUtils.ASECOND;

    /*
     * Actions that we defer to after returning from event methods. In other words,
     * they cannot be done with the lock held, or they require calling more event methods.
     * Because this messenger can take only one message at a time, actions do not cascade
     * much. It may happen that the invoking thread is required to perform closure after
     * performing send. That's about it.
     * In addition, there's always only one deferred action per event. The only actions that cluster are
     * closeInput and closeOutput. We do not defer those.
     */

    /**
     * No action deferred.
     **/
    private static final int ACTION_NONE = 0;

    /**
     * Must send the current message.
     **/
    private static final int ACTION_SEND = 1;

    /**
     * Must report failure to connect.
     **/
    private static final int ACTION_CONNECT = 2;

    /**
     * The current deferred action.
     **/
    private int deferredAction = ACTION_NONE;

    /**
     * The current background thread.
     **/
    private Thread bgThread = null;

    /**
     * The size that channel queues should have.
     * Is updated at ctor time.
     **/
    private int channelQueueSize = 1;

    /**
     * The active channels queue.
     **/
    private UnbiasedQueue activeChannels = null;

    /**
     * The resolving channels set. This is unordered. We use a weak hash map because abandonned channels could otherwise
     * accumulate in-there until the resolution attempt completes. A buggy application could easily do much damage.
     *
     * Note: a channel with at least one message in it is not considered abandonned. To prevent it from disappearing we set a
     * strong reference as the value in the map. A buggy application can do much damage, still, by queuing a single message
     * and then abandonning the channel. This is has to be dealt with at another level; limitting the number of channels
     * per application, or having a global limit on messages...TBD.
     **/
    private WeakHashMap resolvingChannels = null;

    /**
     * A default channel were we put messages that are send directly through this messenger rather than via one of
     * its channels.
     **/
    private ThreadedMessengerChannel defaultChannel = null;

    /**
     * State lock and engine.
     **/
    private final ThreadedMessengerState stateMachine = new ThreadedMessengerState();

    /**
     * The implementation of channel messenger that getChannelMessenger returns:
     **/
    private class ThreadedMessengerChannel extends AsyncChannelMessenger {

        public ThreadedMessengerChannel( EndpointAddress baseAddress, PeerGroupID redirection,
                                         String origService, String origServiceParam, int queueSize, boolean connected ) {

            super(baseAddress, redirection, origService, origServiceParam, queueSize, connected);
        }

        // We're supposed to return the complete destination, including service and param specific to that channel.  It is not
        // clear, whether this should include the cross-group mangling, though. Historically, it does not.
        //
        // FIXME - jice@jxta.org 20040413: for bw compat reasons, we actually need to strip the service and param from the
        // logical.  Need to investigate how much code depends upon this quasi bug...why remove valuable information just so that
        // .equals() succeeds when the invoker is careless ? (RelayClient, EndpointRouter)

        public EndpointAddress getLogicalDestinationAddress() {
            if (logicalDestination == null) {
                return null;
            }

            // FIXME - jice@jxta.org 20040413: per the previous FIXME, alternate code could be:
            // 
            // return new EndpointAddress (logicalDestination, origService, origServiceParam);

            return (EndpointAddress) logicalDestination.clone();
        }

        protected void startImpl() {
            if (! addToActiveChannels(this)) {
                // We do not need to hold our lock to call this, and it is just as well since it could re-enter.
                down();
            }
        }

        protected void connectImpl() {

            // If it cannot be done, it is because we known that we will never be able to generate the resulting event. That means
            // that either the shared messenger is already resolved, or that it is already dead. In that case, we invoke down/up
            // in sequence accordingly.
            //
            // NOTE: the shared messenger may become dead 1 ns from now...that or 1 hour makes no difference, the channel will
            // notice when it first tries to send, in that case. The otherway around is more obvious: If the shared messenger is
            // not USABLE, it cannot come back.
            //
            // addToResolvingChannels() garantees us that if it returns true, either of the channel's down or up methods will be
            // invoked at some point.

            if (! addToResolvingChannels(this)) {
                if ((ThreadedMessenger.this.getState() & USABLE) != 0) {
                    up();
                } else {
                    down();
                }
            }
        }

        protected void resolPendingImpl() {
            // If this channel is still among the ones pending resolution, make sure
            // it becomes strongly referenced.
            strongRefResolvingChannel(this);
        }

    }

    /**
     * Our statemachine implementation; just connects the standard AbstractMessengerState action methods to
     * this object.
     **/
    private class ThreadedMessengerState extends MessengerState {

        protected ThreadedMessengerState() {
            super(false);
        }

        /*
         * The required action methods.
         */

        protected void connectAction() {
            deferAction(ACTION_CONNECT);
        }
        protected void startAction() {
            deferAction(ACTION_SEND);
        }
        // This is a synchronous action. The state machine assumes that it is done when we return. There is No need (nor means) to
        // signal completion.  No need for synchronization either: we're already synchronized.
        protected void closeInputAction() {
            inputClosed = true;
            ThreadedMessengerChannel[] channels = 
                (ThreadedMessengerChannel[]) resolvingChannels.keySet().toArray(new ThreadedMessengerChannel[0]);
            resolvingChannels.clear();
            int i = channels.length;
            while (i-->0) {
                channels[i].down();
            }
            channels = null;
        }
        protected void closeOutputAction() {
            // This will break the cnx; thereby causing a down event if we have a send in progress.
            // If the cnx does not break before the current message is sent, then the message will be sent successfully,
            // resulting in an idle event. Either of these events is enough to complete the shutdown process.
            closeImpl();
        }

        // The input is now closed, so we can rest assured that the last channel is really the last one.
        // This is a synchronous action. The state machine assumes that it is done when we return. There is
        // no need to signal completion with an idleEvent.
        // No need for synchronization either: we're already synchronized.
        protected void failAllAction() {
            while (true) {

                ThreadedMessengerChannel theChannel = null;

                theChannel = (ThreadedMessengerChannel) activeChannels.pop();

                if (theChannel == null) {
                    break;
                }
                theChannel.down();
            }
        }
    }

    /**
     *  Create a new ThreadedMessenger.
     *
     * @param homeGroupID the group that this messenger works for. This is the group of the endpoint service or transport
     * that created this messenger.
     * @param destination where messages should be addressed to
     * @param logicalDestination the expected logical address of the destination. Pass null if unknown/irrelevant
     * @param channelQueueSize the queue size that channels should have.
     **/
    public ThreadedMessenger( PeerGroupID homeGroupID,
                              EndpointAddress destination, EndpointAddress logicalDestination, int channelQueueSize ) {

        super(destination);

        this.homeGroupID = homeGroupID;

        // We tell our super class that we synchronize our state on the stateMachine object. Logic would dictate
        // that we pass it to super(), but it is not itself constructed until super() returns. No way around it.

        setStateLock(stateMachine);

        this.logicalDestination = logicalDestination;
        this.channelQueueSize = channelQueueSize;

        activeChannels = new UnbiasedQueue(Integer.MAX_VALUE, false);
        resolvingChannels = new WeakHashMap(4);
    }

    /**
     * Runs the state machine until there's nothing left to do.
     *
     * Three exposed methods may need to inject new events in the system: sendMessageN, close, and shutdown. Since they can both
     * cause actions, and since connectAction and startAction are deferred, it seems possible that one of the
     * actions caused by send, close, or shutdown be called while connectAction or startAction are in progress.
     *
     * However, the state machine gives us a few guarantees: All the actions except closeInput and closeOutput have an *end*
     * event. No state transition that results in an action other than closeInput or closeOutput, may occur until the end event
     * for an on-going action has been called.
     *
     * We perform closeInput and closeOutput on the fly, so none of the exposed methods are capable of producing deferred actions
     * while an action is already deferred. So, there is at most one deferred action after returning from an event method,
     * regardless the number of concurrent threads invoking the exposed methods, and it can only happen once per deferred action
     * performed.
     */

    public void run() {

        try {
            while (true) {
                switch(nextAction()) {
                case ACTION_NONE:
                    return;
                case ACTION_SEND:
                    send();
                    break;
                case ACTION_CONNECT:
                    connect();
                    break;
                default:
                    // huh ?
                }
            }
        } catch (Throwable any) {
            if (LOG.isEnabledFor(Level.FATAL)) {
                LOG.fatal("Uncaught throwable in background thread", any);
                // Hope the next thread has more luck. It'll need it.
            }
            bgThread = null;
        }
    }


    private void deferAction(int action) {
        deferredAction = action;
        if (bgThread == null) {
            bgThread = new Thread( myThreadGroup, this,
                                   "ThreadedMessenger for " + getDestinationAddress() );
            bgThread.setDaemon( true );
            bgThread.start();
        }
    }

    private int nextAction() {

        long quitAt = TimeUtils.toAbsoluteTimeMillis(THREAD_IDLE_DEAD);

        synchronized(stateMachine) {
            while (deferredAction == ACTION_NONE) {
                // Still nothing to do. Is it time to quit, or where we just awakened for nothing ?
                if (TimeUtils.toRelativeTimeMillis(quitAt) < 0) {
                    // Ok. Time to quit.
                    bgThread = null;
                    return ACTION_NONE;
                }
                // We do not need to wakeup exactly on the dead line, so there's no need to
                // recompute the dead line. THREAD_IDLE_DEAD is comparatively short.
                try {
                    stateMachine.wait(THREAD_IDLE_DEAD);
                } catch (InterruptedException ie) {
                    // Only shutdown can force termination.
                    Thread.interrupted(); // Whatever the urban legend says about this.
                }
            }
            int action = deferredAction;
            deferredAction = ACTION_NONE;
            return action;
        }
    }

    /**
     * Performs the ACTION_SEND deferred action: sends the messages in our channel queues until there's none left or
     * we are forced to stop by connection breakage.
     **/
    private void send() {

        ThreadedMessengerChannel theChannel = null;
        synchronized(stateMachine) {

            theChannel = (ThreadedMessengerChannel) activeChannels.peek();
            if (theChannel == null) {
                // No notifyChange: this is defensive code. NotifyChange() should have been called already.
                stateMachine.idleEvent();
                stateMachine.notifyAll();
                return;
            }
        }

        while (true) {

            AsyncChannelMessenger.PendingMessage theMsg = theChannel.peek();
            if (theMsg == null) {
                // done with that channel for now. (And it knows it). Move to the next channel. Actually
                // it should have been removed when we poped the last message, except if we went down upon sending it.
                // In that later case, we leave the channel queue as is so that we cannot have to report, idle
                // in the same time than down.

                synchronized(stateMachine) {
                    activeChannels.pop();
                    theChannel = (ThreadedMessengerChannel) activeChannels.peek();
                    if (theChannel != null) {
                        continue; // Nothing changes; we do not call msgsEvent because we never call saturatedEvent either.
                    }
                    // Done with all channels. We're now idle.
                    stateMachine.idleEvent();
                    stateMachine.notifyAll();
                }
                notifyChange();
                return;
            }

            Message currentMsg = theMsg.msg;
            String currentService = theMsg.service;
            String currentParam = theMsg.param;

            try {
                sendMessageBImpl(currentMsg, currentService, currentParam);
            } catch(Throwable any) {

                // When the current message fails, we leave it inthere. sendMessageBImpl does not report failures. So that we can retry if
                // applicable. It is up to us to report failures. See failall in AsyncChannel. However, there is a risk that a bad
                // message causes this messenger to go down repeatedly. We need some kind of safeguard. So, if there's already a failure
                // recorded for this message, we bounce it.
                synchronized(stateMachine) {
                    if (theMsg.failure != null) {
                        theChannel.pop();
                        currentMsg.setMessageProperty(Messenger.class, theMsg.failure);
                    } else {
                        theMsg.failure = any;
                    }
                    stateMachine.downEvent();
                    stateMachine.notifyAll();
                }
                notifyChange();
                return;
            }
            

            // Worked. Remove the message, Rotate the queues, get the next message from the next queue.
            synchronized(stateMachine) {

                theChannel.pop();

                // Things are quite a bit simpler if there's a single still active channel, and it's frequent, so it's worth checking.
                boolean empty = (theChannel.peek() == null);
                if ((activeChannels.getCurrentInQueue() != 1) || empty) {
                    activeChannels.pop();
                    if (! empty) {
                        // We're not done with that channel. Put it back at the end
                        activeChannels.push(theChannel);
                    }

                    // Get the next channel.
                    theChannel = (ThreadedMessengerChannel) activeChannels.peek();
                    if (theChannel == null) {
                        // Done with all channels. We're now idle.
                        stateMachine.idleEvent();
                        stateMachine.notifyAll();
                    }
                } // else, just stick to that channel
            }

            if (theChannel == null) {
                notifyChange();
                Thread.yield(); // We're about to go wait(). Yielding is a good bet. It is
                                // very inexpenssive and may be all it takes to get a new job
                                // queued.
                return;
            }
        }
    }


    /**
     * Performs the ACTION_CONNECT deferred action. Generates a down event if it does not work.
     **/
    private void connect() {
        boolean worked = connectImpl();
        ThreadedMessengerChannel[] channels = null;

        synchronized(stateMachine) {
            if (worked) {

                // we can now get the logical destination from the underlying implementation (likely obtained from a transport
                // messenger)

                EndpointAddress effectiveLogicalDest = getLogicalDestinationImpl();
                if (logicalDestination == null) {
                    // We did not know what was supposed to be on the other side. Anything will do.
                    logicalDestination = effectiveLogicalDest;
                    stateMachine.upEvent();

                    channels = (ThreadedMessengerChannel[]) resolvingChannels.keySet().toArray(new ThreadedMessengerChannel[0]);
                    resolvingChannels.clear();
                } else if (logicalDestination.equals(effectiveLogicalDest)) {
                    // Good. It's what we expected.
                    stateMachine.upEvent();

                    channels = (ThreadedMessengerChannel[]) resolvingChannels.keySet().toArray(new ThreadedMessengerChannel[0]);
                    resolvingChannels.clear();
                } else {
                    // Ooops, not what we wanted. Can't connect then. (force close the underlying cnx).
                    closeImpl();
                    stateMachine.downEvent();
                }

            } else {
                stateMachine.downEvent();
            }
            stateMachine.notifyAll();
        }

        // If it worked, we need to tell all the channels that were waiting for resolution.
        // If it did not work, the outcome depends upon what will happen after the down event.
        // It's ok to do that outside of sync. Channel.up may synchronize, but it never calls
        // this class while synchronized.
        if (channels != null) {

            int i = channels.length;
            while (i-->0) {
                channels[i].up();
            }
            channels = null;
        }

        notifyChange();
    }

    /*
     * Messenger API top level methods.
     */

    /**
     * The endpoint service may call this to cause an orderly closure of its messengers.
     **/
    protected final void shutdown() {
        synchronized(stateMachine) {
            stateMachine.shutdownEvent();
            stateMachine.notifyAll();
        }
        notifyChange();
    }

    /**
     * {@inheritDoc}
     **/
    public EndpointAddress getLogicalDestinationAddress() {

        // If it's not resolved, we can't know what the logical destination is, unless we had an expectation.
        // And if we had, the messenger will fail as soon as we discover that the expectation is wrong.
        // In most if not all cases, either we have an expectation, or the messenger comes already resolved.
        // Otherwise, if you need the logical destination, you must resolve first. We do not want this method
        // to be blocking.
        return logicalDestination;
    }

    /**
     * {@inheritDoc}
     **/
    public void close() {
        synchronized(stateMachine) {
            stateMachine.closeEvent();
            stateMachine.notifyAll();
        }
        notifyChange();
    }

    /**
     * {@inheritDoc}
     *
     * <p/> In this case, this method is here out of principle but is not really expected to be invoked.  The normal way
     * of using a ThreadedMessenger is through its channels. We do provide a default channel that all invokers that go around
     * channels will share. That could be usefull to send rare out of band messages for example.
     **/
    public final boolean sendMessageN( Message msg, String service, String serviceParam ) {
        // Need a default channel.
        synchronized(stateMachine) { // Can't do dbl check with current java memory model
            if (defaultChannel == null) {
                defaultChannel = new ThreadedMessengerChannel(getDestinationAddress(),null,null,null,channelQueueSize,
                                                              false);
            }
        }

        return defaultChannel.sendMessageN( msg, service, serviceParam );
    }

    public final void sendMessageB( Message msg, String service, String serviceParam ) throws IOException {
        // Need a default channel.
        synchronized(stateMachine) { // Can't do dbl check with current java memory model
            if (defaultChannel == null) {
                defaultChannel = new ThreadedMessengerChannel(getDestinationAddress(),null,null,null,channelQueueSize,
                                                              false);
            }
        }
        defaultChannel.sendMessageB( msg, service, serviceParam );
    }

    private final boolean addToActiveChannels( ThreadedMessengerChannel channel ) {

        synchronized(stateMachine) {
            if (inputClosed) {
                return false;
            }
            activeChannels.push(channel);

            // There are items in the queue now.
            stateMachine.msgsEvent();

            // We called an event. The state may have changed. Notify waiters.
            stateMachine.notifyAll();
        }

        notifyChange();
        return true;
    }

    private final void strongRefResolvingChannel( ThreadedMessengerChannel channel ) {

        // If, and only if, this channel is already among the resolving channels, add a strong ref
        // to it. This is invoked when a message is queued to that channel while it is still
        // resolving. However we must verify its presence in the resolvingChannels map: this method
        // may be called while the channel has been removed from the list, but has not been told
        // yet.
        synchronized(stateMachine) {
            if (resolvingChannels.containsKey(channel)) {
                resolvingChannels.put(channel, channel);
            }
        }
    }

    private final boolean addToResolvingChannels( ThreadedMessengerChannel channel ) {

        synchronized(stateMachine) {
            // If we're in a state where no resolution event will ever occur, we must not add anything to the list.
            if ((stateMachine.getState() & (RESOLVED | TERMINAL)) != 0) {
                return false;
            }

            // We use the weak map only for the weak part, not for the map part.
            resolvingChannels.put(channel, null);

            stateMachine.resolveEvent();
            stateMachine.notifyAll();
        }

        notifyChange();
        return true;
    }

    public final void resolve() {
        synchronized(stateMachine) {
            stateMachine.resolveEvent();
            stateMachine.notifyAll();
        }
        notifyChange();
    }

    public final int getState() {
        return stateMachine.getState();
    }

    public Messenger getChannelMessenger( PeerGroupID redirection, String service, String serviceParam ) {

        // Our transport is always in the same group. If the channel's target group is the same, no group
        // redirection is ever needed.
        return new ThreadedMessengerChannel( getDestinationAddress(), homeGroupID.equals(redirection) ? null : redirection,
                                             service, serviceParam, channelQueueSize,
                                             (stateMachine.getState() & (RESOLVED & USABLE)) != 0 ); // are we happily resolved ?
    }

     
   /*
    * Abstract methods to be provided by implementor. These are fully expected to be blocking and may
    * be implemented by invoking transport blocking methods, such as EndpointServiceImpl.getLocalTransportMessenger()
    * or <whateverTransportMessengerWasObtained>.sendMessageB(). Should the underlying code be non-blocking, these
    * impl methods must simulate it. If it's not obvious to do, then this base class is not a good choice.
    */

    /**
     * Close underlying connection. May fail current send.
     **/
    protected abstract void closeImpl();

    /**
     * Make underlying connection.
     **/
    protected abstract boolean connectImpl();

    /**
     * Sends message through underlying connection.
     **/
    protected abstract void sendMessageBImpl(Message msg, String service, String param) throws IOException;

    /**
     * Obtain the logical destination address from the implementer (which likely gets it from the transport messenger).
     * Might not work if unresolved, so use with care.
     */
    protected abstract EndpointAddress getLogicalDestinationImpl();
}

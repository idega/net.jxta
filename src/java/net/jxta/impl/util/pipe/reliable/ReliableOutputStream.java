/*
 *  $Id: ReliableOutputStream.java,v 1.1 2007/01/16 11:01:30 thomas Exp $
 *
 *  Copyright (c) 2003 Sun Microsystems, Inc.  All rights reserved.
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
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */
package net.jxta.impl.util.pipe.reliable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;

import net.jxta.impl.util.TimeUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Accepts data and packages it into messages for
 * sending to the remote. The messages are kept in a
 * retry queue until the remote peer acknowledges
 * receipt of the message.
 */
public class ReliableOutputStream extends OutputStream implements Incoming {

    /**
     *  Log4J Logger
     */
    private final static Logger LOG =
            Logger.getLogger(ReliableOutputStream.class.getName());

    /**
     * This maximum is only enforced if we have not heard
     * from the remote for RETRMAXAGE.
     */
    private final static int MAXRETRQSIZE = 100;

    /**
     *  Initial estimated Round Trip Time
     */
    private final static long initRTT = 10 * TimeUtils.ASECOND;

    private final static MessageElement RETELT =
            new StringMessageElement(Defs.RETRY_ELEMENT_NAME,
            Defs.RETRY_ELEMENT_VALUE, null);

    /**
     *  If true then the stream has been closed.
     */
    private volatile boolean closed = false;

    /**
     * If true then the stream is being closed.
     * It means that it still works completely for all messages already
     * queued, but no new message may be enqueued.
     */
    private volatile boolean closing = false;

    /**
     *  Sequence number of the message we most recently sent out.
     */
    private volatile int sequenceNumber = 0;

    /**
     *  Sequence number of highest sequential ACK.
     */
    private volatile int maxACK = 0;

    /**
     *  connection we are working for
     */
    private Outgoing outgoing = null;

    private Retransmitter retrThread = null;

    // for retransmission
    /**
     *  Average round trip time in milliseconds.
     */
    private volatile long aveRTT = initRTT;
    private volatile long remRTT = 0;

    /**
     * Has aveRTT been set at least once over its initial guesstimate value.
     */
    private boolean aveRTTreset = false;

    /**
     *  Number of ACK message received.
     */
    private int nACKS = 0;

    /**
     * When to start computing aveRTT
     */
    private int rttThreshold = 0;

    /**
     *  Retry Time Out measured in milliseconds.
     */
    private volatile long RTO = 0;

    /**
     *  Minimum Retry Timeout measured in milliseconds.
     */
    private volatile long minRTO = initRTT * 5;

    /**
     *  Maximum Retry Timeout measured in milliseconds.
     */
    private volatile long maxRTO = initRTT * 60;

    /**
     *  absolute time in milliseconds of last sequential ACK.
     */
    private volatile long lastACKTime = 0;

    /**
     *  absolute time in milliseconds of last SACK based retransmit.
     */
    private volatile long sackRetransTime = 0;

    /**
     *   The collection of messages available for re-transmission.
     *
     *   elements are {@link RetrQElt}
     */
    protected List retrQ = new ArrayList();

    // running average of receipients Input Queue
    private int nIQTests = 0;
    private int aveIQSize = 0;

    /**
     *  Our estimation of the current free space in the remote input queue.
     */
    private volatile int mrrIQFreeSpace = 0;

    /**
     *  Our estimation of the maximum sise of the remote input queue.
     */
    private int rmaxQSize = Defs.MAXQUEUESIZE;

    /**
     * The flow control module.
     */
    private final FlowControl fc;

    /**
     * Cache of the last rwindow recommendation by fc.
     */
    private volatile int rwindow = 0;

    /**
     * retrans queue element
     */
    private static class RetrQElt {
        int seqnum;
        // sequence number of this message.
        long enqueuedAt;
        // absolute time of original enqueing
        volatile Message msg;
        // the message
        int marked;
        // has been marked as retransmission
        long sentAt;
        // when this msg was last transmitted


        /**
         *Constructor for the RetrQElt object
         *
         * @param  seqnum  Description of the Parameter
         * @param  msg     Description of the Parameter
         */
        public RetrQElt(int seqnum, Message msg) {
            this.seqnum = seqnum;
            this.msg = msg;
            this.enqueuedAt = TimeUtils.timeNow();
            this.sentAt = this.enqueuedAt;
            this.marked = 0;
        }
    }

    /**
     *Constructor for the ReliableOutputStream object
     *
     * @param  outgoing  Description of the Parameter
     */
    public ReliableOutputStream(Outgoing outgoing) {
        // By default use the old behaviour: fixed fc with a rwin of 20
        this(outgoing, new FixedFlowControl(20));
    }


    /**
     *Constructor for the ReliableOutputStream object
     *
     * @param  outgoing  Description of the Parameter
     * @param  fc        Description of the Parameter
     */
    public ReliableOutputStream(Outgoing outgoing, FlowControl fc) {
        this.outgoing = outgoing;

        // initial RTO is set to maxRTO so as to give time
        // to the receiver to catch-up
        this.RTO = maxRTO;

        this.mrrIQFreeSpace = rmaxQSize;
        this.rttThreshold = rmaxQSize;

        // Init last ACK Time to now
        this.lastACKTime = TimeUtils.timeNow();
        this.sackRetransTime = TimeUtils.timeNow();

        // Attach the flowControl module
        this.fc = fc;

        // Update our initial rwindow to reflect fc's initial value
        this.rwindow = fc.getRwindow();

        // Start retransmission thread
        this.retrThread = new Retransmitter();
    }


    /**
     * {@inheritDoc}
     *
     *  <p/>We don't current support linger.
     */
    public synchronized void close() throws IOException {
        super.close();
        closed = true;
        // We have to use a temp because someone else
        // might be trying to null out retrThread.
        Retransmitter temp = retrThread;

        if (null != temp) {
            synchronized (temp) {
                temp.notifyAll();
            }
        }

        retrQ.clear();
    }

    /**
     * indicate that we're in the process of closing. To respect the semantics
     * of close()/isClosed(), we do not set the closed flag, yet. Instead, we
     * set the flag "closing", which simply garantees that no new message
     * will be queued.
     * This, in combination with getSequenceNumber and getMaxAck, and
     * waitQevent, enables fine grain control of the tear down process.
     */
    public void setClosing() {
        synchronized (retrQ) {
            closing = true;
            retrQ.clear();
            retrQ.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(int c) throws IOException {
        byte[] a = new byte[1];
        a[0] = (byte) (c & 0xFF);
        write(a, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("stream is closed");
        }
        if (closing) {
            throw new IOException("stream is being closed");
        }
        if (b == null) {
            throw new IllegalArgumentException("buffer is null");
        }
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }

        // Copy the data since it will be queued, and caller may
        // overwrite the same byte[] buffer.
        byte[] data = new byte[len];
        System.arraycopy(b, off, data, 0, len);

        // allocate new message
        Message jmsg = new Message();
        synchronized (retrQ) {
            while (true) {
                if (closing || closed) {
                    throw new IOException("broken connection");
                }
                if (retrQ.size() > Math.min(rwindow, mrrIQFreeSpace * 2)) {
                    try {
                        retrQ.wait(1000);
                    } catch (InterruptedException ignored) {}
                    continue;
                }
                break;
            }

            ++sequenceNumber;
            MessageElement element =
                    new ByteArrayMessageElement(Integer.toString(sequenceNumber),
                    Defs.MIME_TYPE_BLOCK, data, null);
            jmsg.addMessageElement(Defs.NAMESPACE, element);
            RetrQElt retrQel = new RetrQElt(sequenceNumber, (Message) jmsg.clone());

            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Reliable WRITE : seqn#" + sequenceNumber + " length=" + len);
            }

            // place copy on retransmission queue
            retrQ.add(retrQel);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Retrans Enqueue added seqn#" + sequenceNumber + " retrQ.size()=" + retrQ.size());
            }
        }

        outgoing.send(jmsg);
        mrrIQFreeSpace--;
        // assume we have now taken a slot
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("SENT : seqn#" + sequenceNumber + " length=" + len);
        }
    }

    /**
     *  Description of the Method
     *
     * @param  msg              Description of the Parameter
     * @return                  Description of the Return Value
     * @exception  IOException  Description of the Exception
     */
    public int send(Message msg) throws IOException {
        WireFormatMessage msgSerialized =
                WireFormatMessageFactory.toWire(msg, Defs.MIME_TYPE_MSG, null);
        ByteArrayOutputStream baos =
                new ByteArrayOutputStream((int) msg.getByteLength());
        msgSerialized.sendToStream(baos);
        baos.close();
        byte[] msgData = baos.toByteArray();
        write(msgData, 0, msgData.length);
        return sequenceNumber;
    }

    /**
     *  Gets the maxAck attribute of the ReliableOutputStream object
     *
     * @return    The maxAck value
     */
    public int getMaxAck() {
        return maxACK;
    }

    /**
     *  Gets the seqNumber attribute of the ReliableOutputStream object
     *
     * @return    The seqNumber value
     */
    public int getSeqNumber() {
        return sequenceNumber;
    }

    /**
     *  Gets the queueFull attribute of the ReliableOutputStream object
     *
     * @return    The queueFull value
     */
    public boolean isQueueFull() {
        return mrrIQFreeSpace < 1;
    }

    /**
     *  Gets the queueEmpty attribute of the ReliableOutputStream object
     *
     * @return    The queueEmpty value
     */
    public boolean isQueueEmpty() {
        return retrQ.isEmpty();
    }

    /**
     *  Description of the Method
     *
     * @param  timeout                   Description of the Parameter
     * @exception  InterruptedException  Description of the Exception
     */
    public void waitQueueEvent(long timeout) throws InterruptedException {
        synchronized (retrQ) {
            retrQ.wait(timeout);
        }
    }

    /**
     *  Description of the Method
     *
     * @param  dt         Description of the Parameter
     * @param  msgSeqNum  Description of the Parameter
     */
    private void calcRTT(long dt, int msgSeqNum) {

        nACKS++;
        if (nACKS == 1) {
            // First ACK arrived. We can start computing aveRTT on the messages
            // we send from now on.
            rttThreshold = sequenceNumber + 1;
        }
        if (msgSeqNum > rttThreshold) {
            // Compute only when it has stabilized a bit
            // Since the initial mrrIQFreeSpace is small; the first few
            // messages will be sent early on and may wait a long time
            // for the return channel to initialize. After that things
            // start flowing and RTT becomes relevant.
            // Carefull with the computation: integer division with round-down
            // causes cumulative damage: the ave never goes up if this is not
            // taken care of. We keep the reminder from one round to the other.

            if (!aveRTTreset) {
                aveRTT = dt;
                aveRTTreset = true;
            } else {
                long tmp = (8 * aveRTT) + ((8 * remRTT) / 9) + dt;
                aveRTT = tmp / 9;
                remRTT = tmp - aveRTT * 9;
            }
        }

        // Set retransmission time out: 2.5 x RTT
        //        RTO = (aveRTT << 1) + (aveRTT >> 1);
        RTO = aveRTT * 2;

        // Enforce a min/max
        RTO = Math.max(RTO, minRTO);
        RTO = Math.min(RTO, maxRTO);

        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("RTT = " + dt + "ms aveRTT = " + aveRTT + "ms" +
                    " RTO = " + RTO + "ms");
        }
    }


    /**
     * @param  iq  Description of the Parameter
     * @return     Description of the Return Value
     */
    private int calcAVEIQ(int iq) {
        int n = nIQTests;
        nIQTests += 1;
        aveIQSize = ((n * aveIQSize) + iq) / nIQTests;
        return aveIQSize;
    }

    /**
     *  Description of the Method
     *
     * @param  msg  Description of the Parameter
     */
    public void recv(Message msg) {

        Iterator eachACK =
                msg.getMessageElements(Defs.NAMESPACE, Defs.MIME_TYPE_ACK);

        while (eachACK.hasNext()) {
            MessageElement elt = (MessageElement) eachACK.next();
            eachACK.remove();
            int sackCount = ((int) elt.getByteLength() / 4) - 1;

            try {
                DataInputStream dis = new DataInputStream(elt.getStream());
                int seqack = dis.readInt();
                int[] sacs = new int[sackCount];
                for (int eachSac = 0; eachSac < sackCount; eachSac++) {
                    sacs[eachSac] = dis.readInt();
                }
                Arrays.sort(sacs);
                // take care of the ACK here;
                ackReceived(seqack, sacs);
            } catch (IOException failed) {
                if (LOG.isEnabledFor(Level.WARN)) {
                    LOG.warn("Failure processing ACK", failed);
                }
            }
        }
    }


    /**
     * Process an ACK Message. We remove ACKed
     * messages from the retry queue.  We only
     * acknowledge messages received in sequence.
     *
     * The seqnum is for the largest unacknowledged seqnum
     * the receipient has received.
     *
     * The sackList is a sequence of all of the
     * received messages in the sender's input Q. All
     * will be sequence numbers higher than the
     * sequential ACK seqnum.
     *
     * Recepients are passive and only ack upon the
     * receipt of an in sequence message.
     *
     * They depend on our RTO to fill holes in message
     * sequences.
     *
     * @param  seqnum    Description of the Parameter
     * @param  sackList  Description of the Parameter
     */
    public void ackReceived(int seqnum, int[] sackList) {

        int numberACKed = 0;
        long rttCalcDt = 0;
        int rttCalcSeqnum = -1;
        long fallBackDt = 0;
        int fallBackSeqnum = -1;

        // remove acknowledged messages from retrans Q.
        synchronized (retrQ) {
            lastACKTime = TimeUtils.timeNow();
            fc.ackEventBegin();
            maxACK = Math.max(maxACK, seqnum);

            // dump the current Retry queue and the SACK list
            if (LOG.isEnabledFor(Level.INFO)) {
                StringBuffer dumpRETRQ =
                        new StringBuffer("ACK RECEIVE : " +
                        Integer.toString(seqnum));
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    dumpRETRQ.append('\n');
                }
                dumpRETRQ.append("\tRETRQ (size=" + retrQ.size() + ")");
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    dumpRETRQ.append(" : ");
                    for (int y = 0; y < retrQ.size(); y++) {
                        if (0 != y) {
                            dumpRETRQ.append(", ");
                        }
                        RetrQElt r = (RetrQElt) retrQ.get(y);
                        dumpRETRQ.append(r.seqnum);
                    }
                }
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    dumpRETRQ.append('\n');
                }

                dumpRETRQ.append("\tSACKLIST (size=" + sackList.length + ")");
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    dumpRETRQ.append(" : ");
                    for (int y = 0; y < sackList.length; y++) {
                        if (0 != y) {
                            dumpRETRQ.append(", ");
                        }
                        dumpRETRQ.append(sackList[y]);
                    }
                }
                LOG.info(dumpRETRQ);
            }

            Iterator eachRetryQueueEntry = retrQ.iterator();
            // First remove monotonically increasing seq#s in retrans vector
            while (eachRetryQueueEntry.hasNext()) {
                RetrQElt r = (RetrQElt) eachRetryQueueEntry.next();
                if (r.seqnum > seqnum) {
                    break;
                }
                // Acknowledged
                eachRetryQueueEntry.remove();

                // Update RTT, RTO. Use only those that where acked
                // w/o retrans otherwise the number may be phony (ack
                // of first xmit received just after resending => RTT
                // seems small).  Also, we keep the worst of the bunch
                // we encounter.  If we really can't find a single
                // non-resent message, we make do with a pessimistic
                // approximation: we must not be left behind with an
                // RTT that's too short, we'd keep resending like
                // crazy.
                long enqueuetime = r.enqueuedAt;
                long dt = TimeUtils.toRelativeTimeMillis(lastACKTime, enqueuetime);
                // Update RTT, RTO
                if (r.marked == 0) {
                    if (dt > rttCalcDt) {
                        rttCalcDt = dt;
                        rttCalcSeqnum = r.seqnum;
                    }
                } else {
                    // In case we find no good candidate, make
                    // a guess by dividing by the number of attempts
                    // and keep the worst of them too. Since we
                    // know it may be too short, we will not use it
                    // if shortens rtt.
                    dt /= (r.marked + 1);
                    if (dt > fallBackDt) {
                        fallBackDt = dt;
                        fallBackSeqnum = r.seqnum;
                    }
                }
                fc.packetACKed(r.seqnum);
                r.msg.clear();
                r.msg = null;
                r = null;
                numberACKed++;
            }
            // Update last accessed time in response to getting seq acks.
            if (numberACKed > 0) {
                outgoing.setLastAccessed(TimeUtils.timeNow());
            }
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("SEQUENTIALLY ACKD SEQN = " + seqnum +
                        ", (" + numberACKed + " acked)");
            }
            // most recent remote IQ free space
            mrrIQFreeSpace = rmaxQSize - sackList.length;
            // let's look at average sacs.size(). If it is big, then this
            // probably means we must back off because the system is slow.
            // Our retrans Queue can be large and we can overwhelm the
            // receiver with retransmissions.
            // We will keep the rwin <= ave real input queue size.
            int aveIQ = calcAVEIQ(sackList.length);
            if (LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("remote IQ free space = " + mrrIQFreeSpace +
                        " remote avg IQ occupancy = " + aveIQ);
            }

            int retrans = 0;
            if (sackList.length > 0) {
                Iterator eachRetrQElement = retrQ.iterator();
                int currentSACK = 0;
                while (eachRetrQElement.hasNext()) {
                    RetrQElt r = (RetrQElt) eachRetrQElement.next();
                    while (sackList[currentSACK] < r.seqnum) {
                        currentSACK++;
                        if (currentSACK == sackList.length) {
                            break;
                        }
                    }
                    if (currentSACK == sackList.length) {
                        break;
                    }
                    if (sackList[currentSACK] == r.seqnum) {
                        fc.packetACKed(r.seqnum);
                        numberACKed++;
                        eachRetrQElement.remove();

                        // Update RTT, RTO. Use only those that where acked w/o retrans
                        // otherwise the number is completely phony.
                        // Also, we keep the worst of the bunch we encounter.
                        long enqueuetime = r.enqueuedAt;
                        long dt = TimeUtils.toRelativeTimeMillis(lastACKTime, enqueuetime);
                        // Update RTT, RTO
                        if (r.marked == 0) {
                            if (dt > rttCalcDt) {
                                rttCalcDt = dt;
                                rttCalcSeqnum = r.seqnum;
                            }
                        } else {
                            // In case we find no good candidate, make
                            // a guess by dividing by the number of attempts
                            // and keep the worst of them too. Since we
                            // know it may be too short, we will not use it
                            // if shortens rtt.
                            dt /= (r.marked + 1);
                            if (dt > fallBackDt) {
                                fallBackDt = dt;
                                fallBackSeqnum = r.seqnum;
                            }
                        }
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("SACKD SEQN = " + r.seqnum);
                        }

                        // GC this stuff
                        r.msg.clear();
                        r.msg = null;
                        r = null;

                    } else {
                        // Retransmit? Only if there is a hole in the selected
                        // acknowledgement list. Otherwise let RTO deal.

                        //    Given that this SACK acknowledged messages still
                        //    in the retrQ:
                        //      seqnum is the max consectively SACKD message.
                        //      seqnum < r.seqnum means a message has not reached
                        //      receiver. EG: sacklist == 10,11,13 seqnum == 11
                        //                  We retransmit 12.
                        if (seqnum < r.seqnum) {
                            fc.packetMissing(r.seqnum);
                            retrans++;
                            if (LOG.isEnabledFor(Level.DEBUG)) {
                                LOG.debug("RETR: Fill hole, SACK, seqn#" +
                                        r.seqnum +
                                        ", Window =" + retrans);
                            }
                        }
                    }
                }

                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("SELECTIVE ACKD (" + numberACKed + ") " +
                            retrans + " retrans wanted");
                }
            }

            // Compute aveRTT on the most representative message,
            // if any. That's the most accurate data.
            // Failing that we use the fall back, provided that it not
            // more recent than aveRTT ago - that would decrease aveRTT
            // and in the absence of solid data, we do not want to take
            // that risk.
            if (rttCalcSeqnum != -1) {
                calcRTT(rttCalcDt, rttCalcSeqnum);
                // get fc to recompute rwindow
                rwindow = fc.ackEventEnd(rmaxQSize, aveRTT, rttCalcDt);
            } else if ((fallBackSeqnum != -1) && (fallBackDt > aveRTT)) {
                calcRTT(fallBackDt, fallBackSeqnum);
                // get fc to recompute rwindow
                rwindow = fc.ackEventEnd(rmaxQSize, aveRTT, fallBackDt);
            }
            retrQ.notifyAll();
        }
    }


    /**
     * retransmit unacknowledged  messages
     *
     * @param  rwin         max number of messages to retransmit
     * @param  triggerTime  Description of the Parameter
     * @return              number of messages retransmitted.
     */
    private int retransmit(int rwin, long triggerTime) {

        List retransMsgs = new ArrayList();

        int numberToRetrans;

        // build a list of retries.
        synchronized (retrQ) {
            numberToRetrans = Math.min(retrQ.size(), rwin);
            if (numberToRetrans > 0 && LOG.isEnabledFor(Level.DEBUG)) {
                LOG.debug("Number of messages pending retransmit =" + numberToRetrans);
            }
            for (int j = 0; j < numberToRetrans; j++) {
                RetrQElt r = (RetrQElt) retrQ.get(j);
                // Mark message as retransmission
                // need to know if a msg was retr or not for RTT eval
                if (r.marked == 0) {
                    // First time: we're here because this message has not arrived, but
                    // the next one has. It may be an out of order message.
                    // Experience shows that such a message rarely arrives older than
                    // 1.2 * aveRTT. Beyond that, it's lost. It is also rare that we
                    // detect a hole within that delay. So, often enough, as soon as
                    // a hole is detected, it's time to resend...but not always.
                    if (TimeUtils.toRelativeTimeMillis(triggerTime, r.sentAt)
                             < (6 * aveRTT) / 5) {
                        // Nothing to worry about, yet.
                        continue;
                    }
                } else {
                    // That one has been retransmitted at least once already.
                    // So, we don't have much of a clue other than the age of the
                    // last transmission. It is unlikely that it arrives before aveRTT/2
                    // but we have to anticipate its loss at the risk of making dupes.
                    // Otherwise the receiver will reach the hole, and that's really
                    // expensive. (Think that we've been trying for a while already.)

                    if (TimeUtils.toRelativeTimeMillis(triggerTime, r.sentAt)
                             < aveRTT) {

                        // Nothing to worry about, yet.
                        continue;
                    }
                }
                r.marked++;
                // Make a copy to for sending
                retransMsgs.add(r);
            }
        }

        // send the retries.
        int retransmitted = 0;
        Iterator eachRetrans = retransMsgs.iterator();
        while (eachRetrans.hasNext()) {
            RetrQElt r = (RetrQElt) eachRetrans.next();
            eachRetrans.remove();
            try {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("RETRANSMIT seqn#" + r.seqnum);
                }
                Message sending = (Message) r.msg;
                // its possible that the message was
                // acked while we were working in this
                // case r.msg will have been nulled.
                if (null != sending) {
                    sending = (Message) sending.clone();
                    sending.replaceMessageElement(Defs.NAMESPACE, RETELT);
                    if (outgoing.send(sending)) {
                        r.sentAt = TimeUtils.timeNow();
                        mrrIQFreeSpace--;
                        // assume we have now taken a slot
                        retransmitted++;
                    } else {
                        break;
                        // don't bother continuing.
                    }
                }
            } catch (IOException e) {
                if (LOG.isEnabledFor(Level.DEBUG)) {
                    LOG.debug("FAILED RETRANS seqn#" + r.seqnum, e);
                }
                break;
                // don't bother continuing.
            }
        }
        if (LOG.isEnabledFor(Level.DEBUG)) {
            LOG.debug("RETRANSMITED " + retransmitted +
                    " of " + numberToRetrans);
        }
        return retransmitted;
    }


    /**
     * Retransmission daemon thread
     */
    private class Retransmitter implements Runnable {

        Thread th;
        int nAtThisRTO = 0;
        volatile int nretransmitted = 0;

        /**
         *Constructor for the Retransmitter object
         */
        public Retransmitter() {

            this.th = new Thread(this, "JXTA Reliable Retransmiter for " +
                    outgoing);
            th.setDaemon(true);
            th.start();

            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("RETRANS : STARTED Reliable Retransmit thread, " +
                        "RTO = " + RTO);
            }
        }


        /**
         *  Gets the retransCount attribute of the Retransmitter object
         *
         * @return    The retransCount value
         */
        public int getRetransCount() {
            return nretransmitted;
        }


        /**
         *  Main processing method for the Retransmitter object
         */
        public void run() {
            try {
                int idleCounter = 0;

                while (!closed) {
                    long conn_idle =
                            TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(),
                            outgoing.getLastAccessed());
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("RETRANS : " + outgoing +
                                " idle for " + conn_idle);
                    }
                    // check to see if we have not idled out.
                    if (outgoing.getIdleTimeout() < conn_idle) {
                        if (LOG.isEnabledFor(Level.INFO)) {
                            LOG.info("RETRANS : Shutting down idle " +
                                    "connection " + outgoing);
                        }
                        try {
                            // in this we close ourself
                            outgoing.close();
                            setClosing();
                            // Leave. Otherwise we'll be spinning forever.
                            return;
                        } catch (IOException ignored) {}
                        continue;
                    }
                    synchronized (retrQ) {
                        try {
                            retrQ.wait(RTO);
                        } catch (InterruptedException e) {}
                    }
                    if (closed) {
                        break;
                    }
                    // see if we recently did a retransmit triggered by a SACK
                    long sinceLastSACKRetr =
                            TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(),
                            sackRetransTime);
                    if (sinceLastSACKRetr < RTO) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("RETRANS : SACK retrans " +
                                    sinceLastSACKRetr + "ms ago");
                        }
                        continue;
                    }
                    // See how long we've waited since RTO was set
                    long sinceLastACK =
                            TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(),
                            lastACKTime);
                    long oldestInQueueWait;
                    synchronized (retrQ) {
                        if (retrQ.size() > 0) {
                            RetrQElt elt = (RetrQElt) retrQ.get(0);
                            oldestInQueueWait =
                                    TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(),
                                    elt.enqueuedAt);
                        } else {
                            oldestInQueueWait = 0;
                        }
                    }
                    if (LOG.isEnabledFor(Level.DEBUG)) {
                        LOG.debug("RETRANS : Last ACK " + sinceLastACK +
                                "ms ago. Age of oldest in Queue " +
                                oldestInQueueWait + "ms");
                    }
                    // see if the queue has gone dead
                    if (oldestInQueueWait > (outgoing.getMaxRetryAge() * 2)) {
                        if (LOG.isEnabledFor(Level.INFO)) {
                            LOG.info("RETRANS : Shutting down stale " +
                                    "connection " + outgoing);
                        }
                        try {
                            // in this we close ourself
                            outgoing.close();
                            setClosing();
                            // Leave. Otherwise we'll be spinning forever.
                            return;
                        } catch (IOException ignored) {}
                        continue;
                    }
                    // get real wait as max of age of oldest in retrQ and
                    // lastAck time
                    long realWait = Math.max(oldestInQueueWait, sinceLastACK);
                    // Retransmit only if RTO has expired.
                    //   a. real wait time is longer than RTO
                    //   b. oldest message on Q has been there longer
                    //      than RTO. This is necessary because we may
                    //      have just sent a message, and we do not
                    //      want to overrun the receiver. Also, we
                    //      do not want to restransmit a message that
                    //      has not been idle for the RTO.
                    if ((realWait >= RTO) && (oldestInQueueWait >= RTO)) {
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("RETRANS : RTO RETRANSMISSION [" +
                                    rwindow + "]");
                        }
                        // retransmit
                        int retransed = retransmit(rwindow, TimeUtils.timeNow());
                        // Total
                        nretransmitted += retransed;
                        // number at this RTO
                        nAtThisRTO += retransed;
                        // See if real wait is too long and queue is non-empty
                        //   Remote may be dead - double until max.
                        //   Double after window restransmitted msgs at this RTO
                        //   exceeds the rwindow, and we've had no response for
                        //   twice the current RTO.
                        if ((retransed > 0) &&
                                (realWait >= 2 * RTO) &&
                                (nAtThisRTO >= 2 * rwindow)) {
                            RTO = (realWait > maxRTO ? maxRTO : 2 * RTO);
                            nAtThisRTO = 0;
                        }
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("RETRANS : RETRANSMISSION "
                                     + retransed + " retrans "
                                     + nAtThisRTO + " at this RTO (" +
                                    RTO + ") "
                                     + nretransmitted + " total retrans");
                        }
                    } else {
                        idleCounter += 1;
                        // reset RTO to min if we are idle
                        if (idleCounter == 2) {
                            RTO = minRTO;
                            idleCounter = 0;
                            nAtThisRTO = 0;
                        }
                        if (LOG.isEnabledFor(Level.DEBUG)) {
                            LOG.debug("RETRANS : IDLE : RTO=" + RTO +
                                    " WAIT=" + realWait);
                        }
                    }
                }
                if (LOG.isEnabledFor(Level.INFO)) {
                    LOG.info("Retransmit thread closing");
                }
            } catch (Throwable all) {
                LOG.fatal("Uncaught Throwable in thread :" +
                        Thread.currentThread().getName(), all);
            }
            if (LOG.isEnabledFor(Level.INFO)) {
                LOG.info("STOPPED Retransmit thread");
            }
            retrThread = null;
            th = null;
        }
    }
}


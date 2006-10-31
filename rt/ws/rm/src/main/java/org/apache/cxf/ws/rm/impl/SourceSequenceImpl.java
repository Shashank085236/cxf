/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.rm.impl;

import java.math.BigInteger;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.rm.Expires;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.interceptor.SequenceTerminationPolicyType;

public class SourceSequenceImpl extends AbstractSequenceImpl implements SourceSequence {

    private static final Logger LOG = LogUtils.getL7dLogger(SourceSequenceImpl.class);
    
    private Date expires;
    private Source source;
    private BigInteger currentMessageNumber;
    private boolean lastMessage;
    private Identifier offeringId;
    private org.apache.cxf.ws.addressing.EndpointReferenceType target;
    
    public SourceSequenceImpl(Identifier i) {
        this(i, null, null);
    }
    
    public SourceSequenceImpl(Identifier i, Date e, Identifier oi) {
        this(i, e, oi, BigInteger.ZERO, false);
    }
    
    public SourceSequenceImpl(Identifier i, Date e, Identifier oi, BigInteger cmn, boolean lm) {
        super(i);
        expires = e;

        offeringId = oi;

        currentMessageNumber = cmn;
        lastMessage = lm;
        acknowledgement = RMUtils.getWSRMFactory().createSequenceAcknowledgement();
        acknowledgement.setIdentifier(id);
    }
    
    // begin RMSourceSequence interface
      
    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#getCurrentMessageNr()
     */
    public BigInteger getCurrentMessageNr() {
        return currentMessageNumber;
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#getEndpointIdentifier()
     */
    public String getEndpointIdentifier() {
        return source.getName().toString();
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#getOfferingSequenceIdentifier()
     */
    public Identifier getOfferingSequenceIdentifier() {
        return offeringId;
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#isLastMessage()
     */
    public boolean isLastMessage() {
        return lastMessage;
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#getExpiry()
     */
    public Date getExpires() {
        return expires;
    }
    
    /* (non-Javadoc)
     * @see org.apache.cxf.ws.rm.SourceSequence#offeredBy()
     */
    public boolean offeredBy(Identifier sid) {
        return null != offeringId && offeringId.getValue().equals(sid.getValue());
    }
    
    // end SourceSequence interface
    
    /**
     * Returns true if a last message had been sent for this sequence and if all
     * messages for this sequence have been acknowledged.
     * 
     * @return true if all messages have been acknowledged.
     */
    public boolean allAcknowledged() {
        if (!lastMessage) {
            return false;
        }

        if (acknowledgement.getAcknowledgementRange().size() == 1) {         
            AcknowledgementRange r = acknowledgement.getAcknowledgementRange().get(0);
            return r.getLower().equals(BigInteger.ONE) && r.getUpper().equals(currentMessageNumber);
        }
        return false;
    }

    /**
     * Used by the RM source to cache received acknowledgements for this
     * sequence.
     * 
     * @param acknowledgement an acknowledgement for this sequence
     */
    public void setAcknowledged(SequenceAcknowledgement a) {
        acknowledgement = a;    
    }
     
    void setSource(Source s) {
        source = s;
    }
    
    void setLastMessage(boolean lm) {
        lastMessage = lm;
    }
    
    /**
     * Returns true if the sequence is expired.
     * 
     * @return true if the sequence is expired.
     */

    boolean isExpired() {
        return expires == null ? false : new Date().after(expires);
        
    }
    
    void setExpires(Expires ex) {
        Duration d = null;
        expires = null;
        if (null != ex) {
            d = ex.getValue();
        }

        if (null != d && !d.equals(DatatypeFactory.PT0S)) {
            Date now = new Date();
            expires = new Date(now.getTime() + ex.getValue().getTimeInMillis(now));
        }
    }
    
    /**
     * Returns the next message number and increases the message number.
     * 
     * @return the next message number.
     */
    BigInteger nextMessageNumber() {
        return nextMessageNumber(null, null);
    }

    /**
     * Returns the next message number and increases the message number.
     * The parameters, if not null, indicate that this message is being sent as a response 
     * to the message with the specified message number in the sequence specified by the
     * by the identifier, and are used to decide if this message should be the last in
     * this sequence.
     * 
     * @return the next message number.
     */
    BigInteger nextMessageNumber(Identifier inSeqId, BigInteger inMsgNumber) {
        assert !lastMessage;
        
        BigInteger result = null;
        synchronized (this) {
            currentMessageNumber = currentMessageNumber.add(BigInteger.ONE);
            checkLastMessage(inSeqId, inMsgNumber);
            result = currentMessageNumber;
        } 
        return result;
    }
    
    void nextAndLastMessageNumber() {
        assert !lastMessage;
        
        synchronized (this) {
            currentMessageNumber = currentMessageNumber.add(BigInteger.ONE);
            lastMessage = true;
        }
    }
    

   
    
    SequenceAcknowledgement getAcknowledgement() {
        return acknowledgement;
    }
    
   
    
    
    /**
     * The target for the sequence is the first non-anonymous address that
     * a message is sent to as part of this sequence. It is subsequently used
     * for as the target of out-of-band protocol messages related to that
     * sequence that originate from the sequnce source (i.e. TerminateSequence 
     * and LastMessage, but not AckRequested or SequenceAcknowledgement as these 
     * are orignate from the sequence destination).
     * 
     * @param to
     */
    synchronized void setTarget(org.apache.cxf.ws.addressing.EndpointReferenceType to) {
        if (target == null && !ContextUtils.isGenericAddress(to)) {
            target = to;
        }
    }
    
    synchronized org.apache.cxf.ws.addressing.EndpointReferenceType getTarget() {
        return target;
    } 
   
    /**
     * Checks if the current message should be the last message in this sequence
     * and if so sets the lastMessageNumber property.
     */
    private void checkLastMessage(Identifier inSeqId, BigInteger inMsgNumber) { 
        
        // check if this is a response to a message that was is the last message in the sequence
        // that included this sequence as an offer 

        if (null != inSeqId && null != inMsgNumber) {
            Destination destination = source.getReliableEndpoint().getDestination();
            DestinationSequenceImpl inSeq = null;
            if (null != destination) {
                inSeq = destination.getSequenceImpl(inSeqId);
            }
             
            if (null != inSeq && offeredBy(inSeqId)
                && inMsgNumber.equals(inSeq.getLastMessageNumber())) {
                lastMessage = true;
            }
        } 

        
        if (!lastMessage) {
            SequenceTerminationPolicyType stp = source.getInterceptor().getSourcePolicy()
               .getSequenceTerminationPolicy();

            assert null != stp;

            if ((!stp.getMaxLength().equals(BigInteger.ZERO) && stp.getMaxLength()
                .compareTo(currentMessageNumber) <= 0)
                || (stp.getMaxRanges() > 0
                    && acknowledgement.getAcknowledgementRange().size() >= stp.getMaxRanges())
                || (stp.getMaxUnacknowledged() > 0
                    && source.getInterceptor().getRetransmissionQueue()
                        .countUnacknowledged(this) >= stp.getMaxUnacknowledged())) {
                lastMessage = true;
            }
        }
        
        if (LOG.isLoggable(Level.FINE) && lastMessage) {
            LOG.fine(currentMessageNumber + " should be the last message in this sequence.");
        }
    }
}

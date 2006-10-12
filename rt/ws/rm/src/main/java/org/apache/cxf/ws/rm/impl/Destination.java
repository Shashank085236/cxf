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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.SequenceFault;
import org.apache.cxf.ws.rm.SequenceType;
import org.apache.cxf.ws.rm.persistence.RMStore;


public class Destination extends AbstractEndpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(Destination.class);
    
  
    private Map<String, DestinationSequenceImpl> map;
    
    Destination(RMInterceptor interceptor, Endpoint endpoint) {
        super(interceptor, endpoint);
        map = new HashMap<String, DestinationSequenceImpl>();    
    }
    
    public DestinationSequence getSequence(Identifier id) {        
        return map.get(id.getValue());
    }
    
    public void addSequence(DestinationSequenceImpl seq) {
        addSequence(seq, true);
    }
    
    public void addSequence(DestinationSequenceImpl seq, boolean persist) {  
        // seq.setDestination(this);
        map.put(seq.getIdentifier().getValue(), seq);
        if (persist) {
            RMStore store = getInterceptor().getStore();
            if (null != store) {
                store.createDestinationSequence(seq);
            }
        }
    }
    
    public void removeSequence(DestinationSequence seq) {        
        map.remove(seq.getIdentifier().getValue());
        RMStore store = getInterceptor().getStore();
        if (null != store) {
            store.removeDestinationSequence(seq.getIdentifier());
        }
    }
    
    public Collection<DestinationSequence> getAllSequences() {  
        return CastUtils.cast(map.values());
    }
  
   /**
    * Acknowledges receipt of a message. If the message is the last in the sequence, 
    * sends an out-of-band SequenceAcknowledgement unless there a response will be sent
    * to the acksTo address onto which the acknowldegment can be piggybacked. 
    *  
    * @param sequenceType the sequenceType object that includes identifier and message number
    * (and possibly a lastMessage element) for the message to be acknowledged)
    * @param replyToAddress the replyTo address of the message that carried this sequence information
    * @throws SequenceFault if the sequence specified in <code>sequenceType</code> does not exist
    */
    public void acknowledge(SequenceType sequenceType, String replyToAddress) 
        throws SequenceFault {
        DestinationSequenceImpl seq = getSequenceImpl(sequenceType.getIdentifier());
        if (null != seq) {
            seq.acknowledge(sequenceType.getMessageNumber());
            
            if (null != sequenceType.getLastMessage()) {
                
                seq.setLastMessageNumber(sequenceType.getMessageNumber());
                
                seq.scheduleImmediateAcknowledgement();
                
                // if we cannot expect an outgoing message to which the acknowledgement
                // can be added we need to send an out-of-band SequenceAcknowledgement message
           
                if (!(seq.getAcksTo().getAddress().getValue().equals(replyToAddress)
                    || seq.canPiggybackAckOnPartialResponse())) {
                    try {
                        getInterceptor().getProxy().acknowledge(seq);
                    } catch (IOException ex) {
                        Message msg = new Message("SEQ_ACK_SEND_EXC", LOG, seq);
                        LOG.log(Level.SEVERE, msg.toString(), ex);
                    }
                }
            }
        } else {
            SequenceFaultFactory sff = new SequenceFaultFactory();
            throw sff.createUnknownSequenceFault(sequenceType.getIdentifier());
        }
    }
    
    DestinationSequenceImpl getSequenceImpl(Identifier sid) {
        return map.get(sid.getValue());
    }
}

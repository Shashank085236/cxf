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

package org.apache.cxf.endpoint;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;


/**
 * Strategy for retreival of a pre-existing Conduit to mediate an 
 * outbound message.
 */
public class PreexistingConduitSelector implements
    ConduitSelector {

    private Conduit selectedConduit;

    /**
     * Constructor.
     * 
     * @param c the pre-existing Conduit.
     */
    public PreexistingConduitSelector(Conduit c) {
        selectedConduit = c;
    }

    /**
     * Called prior to the interceptor chain being traversed.
     * 
     * @param message the current Message
     */
    public void prepare(Message message) {
        MessageObserver observer = 
            message.getExchange().get(MessageObserver.class);
        if (observer != null) {
            selectedConduit.setMessageObserver(observer);
        }
    }

    /**
     * Called when a Conduit is actually required.
     * 
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    public Conduit selectConduit(Message message) {
        return selectedConduit;
    }

    /**
     * Called on completion of the MEP for which the Conduit was required.
     * 
     * @param exchange represents the completed MEP
     */
    public void complete(Exchange exchange) {
        // nothing to do
    }
}

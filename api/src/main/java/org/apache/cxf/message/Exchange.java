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

package org.apache.cxf.message;

import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.Session;

public interface Exchange extends StringMap {
    Message getInMessage();
    void setInMessage(Message m);
    
    Message getOutMessage();
    void setOutMessage(Message m);
    
    Message getInFaultMessage();
    void setInFaultMessage(Message m);

    Message getOutFaultMessage();
    void setOutFaultMessage(Message m);
    
    Session getSession();
    
    /**
     * @return the associated incoming Destination (may be anonymous)
     */
    Destination getDestination();
    
    /**
     * @param destination the associated incoming Destination
     */    
    void setDestination(Destination destination);

    /**
     * @param message the associated message
     * @return the associated outgoing Conduit (may be anonymous)
     */
    Conduit getConduit(Message message);

    /**
     * @param conduit the associated outgoing Conduit 
     */
    void setConduit(Conduit conduit);
    
    /**
     * @return true if the exchange is known to be a one-way exchange
     */
    boolean isOneWay();
    
    /**
     * @return true if the frontend will be wait for the response.   Transports
     * can then optimize themselves to process the response immediately instead 
     * of using a background thread or similar.
     */
    boolean isSynchronous();
    void setSynchronous(boolean b);

    /**
     * 
     * @param b true if the exchange is known to be a one-way exchange
     */
    void setOneWay(boolean b);
    
    void clear();
}

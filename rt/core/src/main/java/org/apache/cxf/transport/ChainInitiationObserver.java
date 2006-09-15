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

package org.apache.cxf.transport;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;

public class ChainInitiationObserver implements MessageObserver {
    Endpoint endpoint;
    Bus bus;

    public ChainInitiationObserver(Endpoint endpoint, Bus bus) {
        super();
        this.endpoint = endpoint;
        this.bus = bus;
    }

    public void onMessage(Message m) {
        Message message = endpoint.getBinding().createMessage(m);
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
        message.setExchange(exchange);
        setExchangProperties(exchange, message);
        
        // setup chain
        PhaseInterceptorChain chain = new PhaseInterceptorChain(bus.getExtension(PhaseManager.class)
            .getInPhases());
        
        message.setInterceptorChain(chain);
        
        chain.add(bus.getInInterceptors());
        chain.add(endpoint.getInInterceptors());
        chain.add(endpoint.getBinding().getInInterceptors());
        chain.add(endpoint.getService().getInInterceptors());

        chain.setFaultInterceptor(endpoint.getFaultInterceptor());
       
        chain.doIntercept(message);        
    }
    
    protected void setExchangProperties(Exchange exchange, Message m) {
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Service.class, endpoint.getService());
        exchange.put(Binding.class, endpoint.getBinding());
        exchange.put(Bus.class, bus);
        exchange.setDestination(m.getDestination());
    }
    
}

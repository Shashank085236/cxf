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

package org.apache.cxf.interceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

public class FaultChainIntiatorInterceptor implements Interceptor<Message> {
    Endpoint endpoint;
    Bus bus;

    public FaultChainIntiatorInterceptor(Endpoint endpoint, Bus bus) {
        super();
        this.endpoint = endpoint;
        this.bus = bus;
    }

    public void handleMessage(Message m) {
        Message message = m.getExchange().getFaultMessage();
        message.setContent(Exception.class, m.getContent(Exception.class));
        
        // setup chain
        PhaseInterceptorChain chain = new PhaseInterceptorChain(bus.getExtension(PhaseManager.class)
            .getOutPhases());
        chain.add(bus.getOutFaultInterceptors());
        chain.add(endpoint.getOutFaultInterceptors());
        chain.add(endpoint.getBinding().getOutFaultInterceptors());
        chain.add(endpoint.getService().getOutFaultInterceptors());

        message.setInterceptorChain(chain);
        chain.doIntercept(message);
    }

    public void handleFault(Message message) {
    }
}
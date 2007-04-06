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

package org.apache.cxf.ws.policy;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.neethi.Assertion;

/**
 * 
 */
public class ClientPolicyOutInterceptor extends AbstractPolicyInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(ClientPolicyOutInterceptor.class);
    
    public ClientPolicyOutInterceptor() {
        setId(PolicyConstants.CLIENT_POLICY_OUT_INTERCEPTOR_ID);
        setPhase(Phase.SETUP);
    }
    
    public void handleMessage(Message msg) {
        if (!MessageUtils.isRequestor(msg)) {
            LOG.fine("Not a requestor.");
            return;
        }
        
        Exchange exchange = msg.getExchange();
        assert null != exchange;
        
        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        if (null == boi) {
            LOG.fine("No binding operation info.");
            return;
        }
        
        Endpoint e = exchange.get(Endpoint.class);
        if (null == e) {
            LOG.fine("No endpoint.");
            return;
        } 
        EndpointInfo ei = e.getEndpointInfo();
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }
        
        Conduit conduit = exchange.getConduit();
        
        // add the required interceptors
        
        EffectivePolicy effectivePolicy = pe.getEffectiveClientRequestPolicy(ei, boi, conduit);
        PolicyUtils.logPolicy(LOG, Level.FINE, "Using effective policy: ", effectivePolicy.getPolicy());
        
        List<Interceptor> interceptors = effectivePolicy.getInterceptors();
        for (Interceptor i : interceptors) {            
            msg.getInterceptorChain().add(i);
            LOG.log(Level.INFO, "Added interceptor of type {0}", i.getClass().getSimpleName());
        }
        
        // insert assertions of the chosen alternative into the message
        
        Collection<Assertion> assertions = effectivePolicy.getChosenAlternative();
        if (null != assertions) {
            StringBuffer buf = new StringBuffer();
            buf.append("Chosen alternative: ");
            String nl = System.getProperty("line.separator");
            buf.append(nl);
            for (Assertion a : assertions) {
                PolicyUtils.printPolicyComponent(a, buf, 1);
            }
            LOG.fine(buf.toString());
            msg.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
        }
    }
}

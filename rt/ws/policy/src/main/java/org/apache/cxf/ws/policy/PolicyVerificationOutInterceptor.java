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

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class PolicyVerificationOutInterceptor extends AbstractPolicyInterceptor {

    private static final Logger LOG 
        = LogUtils.getL7dLogger(PolicyVerificationOutInterceptor.class);
    public PolicyVerificationOutInterceptor() {
        setPhase(Phase.POST_STREAM);
    }

    /** 
     * Checks if all assertions in the chosen alternative have been asserted.
     * Note that although the alternative was chosen in such a way that at least all
     * interceptors necessary to assert the assertions are present, it is not possible
     * to predict if these interceptors actually have asserted their assertions.  
     * @param message
     * @throws PolicyException if none of the alternatives is supported
     */
    protected void handle(Message message) {
        
        if (MessageUtils.isPartialResponse(message)) {
            LOG.fine("Not verifying policies on outbound partial response.");
            return;
        } 
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }
        
        getTransportAssertions(message);
        
        aim.check();
        
        LOG.fine("Verified policies for outbound message.");
    }

}

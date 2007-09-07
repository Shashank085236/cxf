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

package org.apache.cxf.binding.soap.interceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;

public class SoapActionOutInterceptor extends AbstractSoapInterceptor {
    
    public SoapActionOutInterceptor() {
        super(Phase.POST_LOGICAL);
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        if (!(message == message.getExchange().getInMessage())) {
            setSoapAction(message);
        }
    }

    private void setSoapAction(SoapMessage message) {
        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        
        // The soap action is set on the wrapped operation.
        if (boi != null && boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        
        String action = getSoapAction(message, boi);
        
        if (message.getVersion() instanceof Soap11) {
            Map<String, List<String>> reqHeaders = CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
            if (reqHeaders == null) {
                reqHeaders = new HashMap<String, List<String>>();
            }
            
            if (reqHeaders.size() == 0) {
                message.put(Message.PROTOCOL_HEADERS, reqHeaders);
            }
            
            if (!reqHeaders.containsKey("SOAPAction")) {            
                reqHeaders.put("SOAPAction", Collections.singletonList(action));
            }
        } else if (message.getVersion() instanceof Soap12 && !"\"\"".equals(action)) {
            String ct = (String) message.get(Message.CONTENT_TYPE);
            
            if (ct.indexOf("action=\"") == -1) {
                ct = new StringBuilder().append(ct)
                    .append("; action=").append(action).toString();
                message.put(Message.CONTENT_TYPE, ct);
            }
        }
    }

    private String getSoapAction(SoapMessage message, BindingOperationInfo boi) {
        // allow an interceptor to override the SOAPAction if need be
        String action = (String) message.get(SoapConstants.SOAP_ACTION);
        
        // Fall back on the SOAPAction in the operation info
        if (action == null) {
            if (boi == null) {
                action = "\"\"";
            } else {
                SoapOperationInfo soi = (SoapOperationInfo) boi.getExtensor(SoapOperationInfo.class);
                action = soi == null ? "\"\"" : soi.getAction() == null ? "\"\"" : soi.getAction();
                if (!action.startsWith("\"")) {
                    action = new StringBuffer().append("\"").append(action).append("\"").toString();
                }
            }
        }
        
        return action;
    }

}

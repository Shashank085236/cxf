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

package org.apache.cxf.jaxws.interceptors;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.jaxb.WrapperHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class WrapperClassInInterceptor extends AbstractPhaseInterceptor<Message> {

    public WrapperClassInInterceptor() {
        super();
        setPhase(Phase.POST_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        if (boi == null) {
            return;
        }
                
        Method method = message.getExchange().get(Method.class);

        if (method != null && method.getName().endsWith("Async")) {
            Class<?> retType = method.getReturnType();
            if (retType.getName().equals("java.util.concurrent.Future") 
                || retType.getName().equals("javax.xml.ws.Response")) {
                return;
            }
        }        

        
        if (method != null && method.getName().endsWith("Async")) {
            Class<?> retType = method.getReturnType();
            if (retType.getName().equals("java.util.concurrent.Future") 
                || retType.getName().equals("javax.xml.ws.Response")) {
                return;
            }

        }
      


        if (boi != null && boi.isUnwrappedCapable()) {
            BindingOperationInfo boi2 = boi.getUnwrappedOperation();
            
            // Sometimes, an uperation can be unwrapped according to WSDLServiceFactory,
            // but not according to JAX-WS. We should unify these at some point, but
            // for now check for the wrapper class.
            if (boi2.getOperationInfo().getInput().getProperty(WrappedInInterceptor.WRAPPER_CLASS) 
                == null) {
                return;
            }
            
            OperationInfo op = boi2.getOperationInfo();
            MessageInfo messageInfo = message.get(MessageInfo.class);
            BindingMessageInfo bmi;
            if (messageInfo == boi.getOperationInfo().getInput()) {
                messageInfo = op.getInput();
                bmi = boi2.getInput();
            } else {
                messageInfo = op.getOutput();
                bmi = boi2.getOutput();
            }
                        
            List<?> lst = message.getContent(List.class);
            
            if (lst != null) {
                message.put(MessageInfo.class, messageInfo);
                message.put(BindingMessageInfo.class, bmi);
                message.getExchange().put(BindingOperationInfo.class, boi2);
                message.getExchange().put(OperationInfo.class, op);
            }
            
            if (lst != null && lst.size() == 1) {
                if (messageInfo.getMessageParts().size() > 0) {
                    Object wrappedObject = lst.get(0);
                    lst.clear();
                    
                    for (MessagePartInfo part : messageInfo.getMessageParts()) {
                        try {
                            String elementType = null;
                            if (part.isElement()) {
                                elementType = part.getElementQName().getLocalPart();
                            } else {
                                elementType = part.getTypeQName().getLocalPart();
                            }
                            Object obj = WrapperHelper.getWrappedPart(part.getName().getLocalPart(), 
                                                                      wrappedObject,
                                                                      elementType);
                        
                            CastUtils.cast(lst, Object.class).add(obj);
                        } catch (Exception e) {
                            // TODO - fault
                            throw new Fault(e);
                        }
                    }
                } else {
                    lst.clear();
                }
            }           
        }
    }

}

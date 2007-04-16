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

package org.apache.cxf.jaxws.handler;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.ws.Binding;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.MessageObserver;

public class LogicalHandlerInterceptor<T extends Message> extends AbstractJAXWSHandlerInterceptor<T> {

    public LogicalHandlerInterceptor(Binding binding) {
        super(binding);
        setPhase(Phase.USER_LOGICAL);
    }

    public void handleMessage(T message) {
        HandlerChainInvoker invoker = getInvoker(message);
        if (!invoker.getLogicalHandlers().isEmpty()) {
            LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
            invoker.setLogicalMessageContext(lctx);
            if (!invoker.invokeLogicalHandlers(isRequestor(message), lctx)) {
                //TODO: reverseHandlers();

                message.getInterceptorChain().abort();
                Message responseMsg = new MessageImpl();
                message.getExchange().setInMessage(responseMsg);

                /**
                 * 1. message.setHeaders()
                 * 2. message.setAttachments()
                 * 3. set XMLSTreamReader to element inside Body
                 * OR
                 * message.setContent(Element.class, elementInBody);
                 * 4. invoke MessageObserver.onMessage() starting after this.getID()
                 */
                MessageObserver observer =
                    (MessageObserver)message.getExchange().get(MessageObserver.class);
                responseMsg.put(PhaseInterceptorChain.STARTING_AFTER_INTERCEPTOR_ID, this.getId());
                if (observer != null) {
                    Source inSource = message.getContent(Source.class);
                    if (inSource != null) {
                        responseMsg.setContent(Source.class, inSource);
                    }
                    List inObj = message.getContent(List.class);
                    if (inObj != null) {
                        responseMsg.setContent(List.class, inObj);
                    }
                    observer.onMessage(responseMsg);
                } else if (!message.getExchange().isOneWay()) {
                    //for the server side inbound

                    //InterceptorChain chain = OutgoingChainSetupInterceptor.getOutInterceptorChain(
                    //    message.getExchange());
                    //chain.doIntercept(message);
                }
            }
        }
        onCompletion(message);
    }

    public void handleFault(T message) {
        // TODO
    }

    public void onCompletion(T message) {
        if (!isOutbound(message)) {
            getInvoker(message).mepComplete(message);
        }
    }

}

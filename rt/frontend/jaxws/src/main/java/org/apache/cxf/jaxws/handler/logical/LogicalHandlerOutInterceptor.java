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

package org.apache.cxf.jaxws.handler.logical;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.AbstractJAXWSHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.MessageObserver;


public class LogicalHandlerOutInterceptor<T extends Message> 
    extends AbstractJAXWSHandlerInterceptor<T> {
    
    public static final String ORIGINAL_WRITER = "original_writer";
    
    public LogicalHandlerOutInterceptor(Binding binding) {
        super(binding);
        setPhase(Phase.PRE_MARSHAL);
    }
    
    public void handleMessage(T message) throws Fault {
        HandlerChainInvoker invoker = getInvoker(message);
        if (invoker.getLogicalHandlers().isEmpty()) {
            return;
        }
        
        try {
            
            XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter(XMLUtils.newDocument());
        
            // Replace stax writer with DomStreamWriter
            message.setContent(XMLStreamWriter.class, writer);        
            message.put(ORIGINAL_WRITER, origWriter);
            
            message.getInterceptorChain().add(new LogicalHandlerOutEndingInterceptor<T>(
                    getBinding()));
        } catch (ParserConfigurationException e) {
            throw new Fault(e);
        }
    }
    
    
    private class LogicalHandlerOutEndingInterceptor<X extends Message> 
        extends AbstractJAXWSHandlerInterceptor<X> {
    
        public LogicalHandlerOutEndingInterceptor(Binding binding) {
            super(binding);
            
            setPhase(Phase.POST_MARSHAL);
        }
    
        public void handleMessage(X message) throws Fault {
            W3CDOMStreamWriter domWriter = (W3CDOMStreamWriter)message.getContent(XMLStreamWriter.class);
            XMLStreamWriter origWriter = (XMLStreamWriter)message
                .get(LogicalHandlerOutInterceptor.ORIGINAL_WRITER);  
            
            
            HandlerChainInvoker invoker = getInvoker(message);
            LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
            invoker.setLogicalMessageContext(lctx);
            boolean requestor = isRequestor(message);
            
            XMLStreamReader reader = (XMLStreamReader)message.get("LogicalHandlerInterceptor.INREADER");
            SOAPMessage origMessage = null;
            if (reader != null) {
                origMessage = message.getContent(SOAPMessage.class);
                message.setContent(XMLStreamReader.class, reader);
                message.removeContent(SOAPMessage.class);
            } else if (domWriter.getDocument().getDocumentElement() != null) {
                Source source = new DOMSource(domWriter.getDocument());
                message.setContent(Source.class, source);
                message.setContent(XMLStreamReader.class, 
                                   StaxUtils.createXMLStreamReader(domWriter.getDocument()));
            }            
            
            if (!invoker.invokeLogicalHandlers(requestor, lctx)) {
                if (requestor) {
                    // client side - abort
                    message.getInterceptorChain().abort();
                    Endpoint e = message.getExchange().get(Endpoint.class);
                    Message responseMsg = e.getBinding().createMessage();            

                    MessageObserver observer = (MessageObserver)message.getExchange()
                                .get(MessageObserver.class);
                    if (observer != null) {
                        //client side outbound, the request message becomes the response message
                        responseMsg.setContent(XMLStreamReader.class, message
                            .getContent(XMLStreamReader.class));                        
                        
                        message.getExchange().setInMessage(responseMsg);
                        responseMsg.put(PhaseInterceptorChain.STARTING_AT_INTERCEPTOR_ID,
                                        LogicalHandlerInInterceptor.class.getName());
                        observer.onMessage(responseMsg);
                    }
                } else {
                    // server side - abort
                    //System.out.println("Logical handler server side aborting");
                }
                return;
            }
            if (origMessage != null) {
                message.setContent(SOAPMessage.class, origMessage);
            }
            
            try {
                reader = message.getContent(XMLStreamReader.class);
                message.removeContent(XMLStreamReader.class);
                if (reader != null) {
                    StaxUtils.copy(reader, origWriter);
                } else if (domWriter.getDocument().getDocumentElement() != null) {
                    StaxUtils.copy(domWriter.getDocument(), origWriter);
                }
                message.setContent(XMLStreamWriter.class, origWriter);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
        
    }

    
}

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
package org.apache.cxf.jaxws.support;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import junit.framework.TestCase;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

public class ContextPropertiesMappingTest extends TestCase {
    private static final String ADDRESS = "test address";
    private static final String REQUEST_METHOD = "GET";
    private static final String HEADER = "header";
    private Map<String, Object> message = new HashMap<String, Object>();
    private Map<String, Object> requestContext = new HashMap<String, Object>();
    private Map<String, Object> responseContext = new HashMap<String, Object>();
    
    
    public void setUp() throws Exception {
        message.clear();
        message.put(Message.ENDPOINT_ADDRESS, ADDRESS);
        message.put(Message.HTTP_REQUEST_METHOD, REQUEST_METHOD);
        message.put(Message.PROTOCOL_HEADERS, HEADER);
        requestContext.clear();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ADDRESS + "jaxws");
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, HEADER + "jaxws");
        responseContext.clear();
    }
    
    public void testMapRequestfromJaxws2Cxf() {
        Object address = requestContext.get(Message.ENDPOINT_ADDRESS);
        assertNull("address should be null", address);
        ContextPropertiesMapping.mapRequestfromJaxws2Cxf(requestContext);
        address = requestContext.get(Message.ENDPOINT_ADDRESS);
        assertNotNull("address should not be null", address);
        assertEquals("address should get from requestContext", address, ADDRESS + "jaxws");
        message.putAll(requestContext);
        address = message.get(Message.ENDPOINT_ADDRESS);        
        assertNotNull("address should not be null", address);
        assertEquals("address should get from requestContext", address, ADDRESS + "jaxws");
        Object header = message.get(Message.PROTOCOL_HEADERS);
        assertEquals("the message PROTOCOL_HEADERS should be updated", header, HEADER + "jaxws");
    }
    
    public void testMapResponseCxf2Jaxws() {        
        responseContext.putAll(message);
        Object requestMethod = responseContext.get(MessageContext.HTTP_REQUEST_METHOD);
        assertNull("requestMethod should be null", requestMethod);
        ContextPropertiesMapping.mapResponsefromCxf2Jaxws(responseContext);
        requestMethod = responseContext.get(MessageContext.HTTP_REQUEST_METHOD);
        assertNotNull("requestMethod should not be null", requestMethod);
        assertEquals(requestMethod, REQUEST_METHOD);
        Object header = responseContext.get(MessageContext.HTTP_RESPONSE_HEADERS);
        assertNotNull("the HTTP_RESPONSE_HEADERS should not be null ", header);
        assertEquals("the HTTP_RESPONSE_HEADERS should be updated", header, HEADER);
    }
    
    public void testCreateWebServiceContext() {
        Exchange exchange = new ExchangeImpl();
        Message inMessage = new MessageImpl();
        Message outMessage = new MessageImpl();
        
        inMessage.putAll(message);
        
        exchange.setInMessage(inMessage);
        exchange.setOutMessage(outMessage);
        
        MessageContext ctx = ContextPropertiesMapping.createWebServiceContext(exchange);
        
        Object requestHeader = ctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        assertNotNull("the request header should not be null", requestHeader);
        assertEquals("we should get the request header", requestHeader, HEADER);        
        Object responseHeader = ctx.get(MessageContext.HTTP_RESPONSE_HEADERS);
        assertNotNull("the response header should not be null", responseHeader);        
        Object outMessageHeader = outMessage.get(Message.PROTOCOL_HEADERS);
        assertEquals("the outMessage PROTOCOL_HEADERS should be update", responseHeader, outMessageHeader);
        
    }
    

}

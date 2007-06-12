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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;


import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.headers.Header;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ContextPropertiesMappingTest extends Assert {
    private static final String ADDRESS = "test address";
    private static final String REQUEST_METHOD = "GET";
    private static final String HEADER = "header";
    private static final Integer RESPONSE_CODE = 401; 
    
    private Map<String, Object> message = new HashMap<String, Object>();
    private Map<String, Object> requestContext = new HashMap<String, Object>();
    private Map<String, Object> responseContext = new HashMap<String, Object>();
    
    @Before
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
    
    @Test
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
    
    @Test
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
    
    @Test
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
        assertNull("the response header should be null", responseHeader);        
        Object outMessageHeader = outMessage.get(Message.PROTOCOL_HEADERS);
        assertEquals("the outMessage PROTOCOL_HEADERS should be update", responseHeader, outMessageHeader);
        
        Object inAttachments = ctx.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertNotNull("inbound attachments object must be initialized", inAttachments);
        assertTrue("inbound attachments must be in a Map", inAttachments instanceof Map);
        assertTrue("no inbound attachments expected", ((Map)inAttachments).isEmpty());
    }
    
    @Test
    public void testUpdateWebServiceContext() {
        Exchange xchng = new ExchangeImpl();
        Message outMsg = new MessageImpl();
        List<Header> hdrList = new ArrayList<Header>();
        xchng.setOutMessage(outMsg);
        
        responseContext.put(MessageContext.HTTP_RESPONSE_CODE, RESPONSE_CODE);
        
        MessageContext ctx = EasyMock.createMock(MessageContext.class);
        ctx.containsKey(MessageContext.HTTP_RESPONSE_CODE);
        EasyMock.expectLastCall().andReturn(true);
        ctx.get(MessageContext.HTTP_RESPONSE_CODE);
        EasyMock.expectLastCall().andReturn(RESPONSE_CODE);
        
        ctx.containsKey(Header.HEADER_LIST);
        EasyMock.expectLastCall().andReturn(true);
        ctx.get(Header.HEADER_LIST);
        EasyMock.expectLastCall().andReturn(hdrList);
        ctx.get(Header.HEADER_LIST);
        EasyMock.expectLastCall().andReturn(hdrList);

        ctx.containsKey(MessageContext.HTTP_RESPONSE_HEADERS);
        EasyMock.expectLastCall().andReturn(false);

        EasyMock.replay(ctx);
        
        ContextPropertiesMapping.updateWebServiceContext(xchng, ctx);
        Integer respCode = (Integer)outMsg.get(Message.RESPONSE_CODE);
        assertNotNull("no response code set on out message", respCode);
        assertEquals("incorrect response code returned", RESPONSE_CODE, respCode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateWebServiceContextWithInAttachments() {
        Exchange exchange = new ExchangeImpl();
        Message inMessage = new MessageImpl();
        
        Collection<Attachment> attachments = new LinkedList<Attachment>();

        DataSource source = new ByteArrayDataSource(new byte[0], "text/xml");
        
        DataHandler handler1 = new DataHandler(source);
        attachments.add(new AttachmentImpl("part1", handler1));
        DataHandler handler2 = new DataHandler(source);
        attachments.add(new AttachmentImpl("part2", handler2));
        inMessage.setAttachments(attachments);
        
        inMessage.putAll(message);
        exchange.setInMessage(inMessage);
        exchange.setOutMessage(new MessageImpl());
        
        MessageContext ctx = ContextPropertiesMapping.createWebServiceContext(exchange);
        
        Object inAttachments = ctx.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertNotNull("inbound attachments object must be initialized", inAttachments);
        assertTrue("inbound attachments must be in a Map", inAttachments instanceof Map);
        Map<String, DataHandler> dataHandlers = (Map)inAttachments;
        assertEquals("two inbound attachments expected", 2, dataHandlers.size());
        
        assertTrue("part1 attachment is missing", dataHandlers.containsKey("part1"));
        // should do as it's the same instance
        assertTrue("part1 handler is missing", dataHandlers.get("part1") == handler1);
        assertTrue("part2 attachment is missing", dataHandlers.containsKey("part2"));
        assertTrue("part2 handler is missing", dataHandlers.get("part2") == handler2);
    }
}

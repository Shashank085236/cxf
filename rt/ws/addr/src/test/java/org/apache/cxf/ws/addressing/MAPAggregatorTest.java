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

package org.apache.cxf.ws.addressing;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import junit.framework.TestCase;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;


import static org.apache.cxf.binding.soap.Soap11.SOAP_NAMESPACE;
import static org.apache.cxf.message.Message.REQUESTOR_ROLE;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND;

public class MAPAggregatorTest extends TestCase {

    private MAPAggregator aggregator;
    private IMocksControl control;
    private AddressingPropertiesImpl expectedMAPs;
    private String expectedTo;
    private String expectedReplyTo;
    private String expectedRelatesTo;
    private String expectedAction;
    
    public void setUp() {
        aggregator = new MAPAggregator();
        control = EasyMock.createNiceControl();
    }

    public void tearDown() {
        expectedMAPs = null;
        expectedTo = null;
        expectedReplyTo = null;
        expectedRelatesTo = null;
        expectedAction = null;
    }

    public void testRequestorOutboundUsingAddressingMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }
    
    public void testRequestorOutboundUsingAddressingMAPsInContextZeroLengthAction() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true, true);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundUsingAddressingMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundUsingAddressingNoMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }
    
    public void testRequestorOutboundUsingAddressingNoMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    public void testRequestorOutboundNotUsingAddressingFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    public void testRequestorOutboundOnewayUsingAddressingMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundOnewayUsingAddressingMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    public void testRequestorOutboundOnewayNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, true, false);
    }
    

    public void testRequestorOutboundOnewayNotUsingAddressingFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    public void testResponderInboundValidMAPs() throws Exception {
        Message message = setUpMessage(false, false, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, false, false);
    }
    
    public void testResponderInboundDecoupled() throws Exception {
        Message message = 
            setUpMessage(false, false, false, true, false, true);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, false, true);
    }
    
    public void testResponderInboundOneway() throws Exception {
        Message message = 
            setUpMessage(false, false, true, true, false, true);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, false, true);
    }

    public void testResponderInboundValidMAPsFault() throws Exception {
        Message message = setUpMessage(false, false, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, false, false, true);
    }

    public void testResponderInboundInvalidMAPs() throws Exception {
        aggregator.messageIDs.put("urn:uuid:12345", "urn:uuid:12345");
        Message message = setUpMessage(false, false, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, false, false /*check*/);
    }

    public void testResponderInboundInvalidMAPsFault() throws Exception {
        aggregator.messageIDs.put("urn:uuid:12345", "urn:uuid:12345");
        Message message = setUpMessage(false, false, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, false, false, false /*check*/);
    }

    public void testResponderOutbound() throws Exception {
        Message message = setUpMessage(false, true, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, true, true);
    }
    
    public void testResponderOutboundZeroLengthAction() throws Exception {
        Message message = 
            setUpMessage(false, true, false, false, false, false, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, false, true, true);
    }

    public void testResponderOutboundFault() throws Exception {
        Message message = setUpMessage(new boolean[] {false,
                                                      true,
                                                      false,
                                                      false,
                                                      false,
                                                      true,
                                                      false,
                                                      true});
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, false, true, true);
    }

    public void testRequestorInbound() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.handleMessage(message);
        control.verify();
        verifyMessage(message, true, false, false /*check*/);
    }

    public void testRequestorInboundFault() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.handleFault(message);
        control.verify();
        verifyMessage(message, true, false, false /*check*/);
    }

    private Message setUpMessage(boolean requestor, 
                                 boolean outbound,
                                 boolean oneway) 
        throws Exception {
        return setUpMessage(requestor, outbound, oneway, false, false, false);
    }

    private Message setUpMessage(boolean requestor, 
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing)
        throws Exception {
        return setUpMessage(requestor,
                            outbound,
                            oneway,
                            usingAddressing,
                            false,
                            false);
    }

    private Message setUpMessage(boolean requestor, 
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext) 
        throws Exception {
        return setUpMessage(requestor,
                            outbound,
                            oneway,
                            usingAddressing,
                            mapsInContext,
                            false);
    }

    private Message setUpMessage(boolean requestor, 
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext,
                                 boolean decoupled) 
        throws Exception {
        return setUpMessage(requestor, 
                            outbound,
                            oneway,
                            usingAddressing,
                            mapsInContext,
                            decoupled,
                            false);
    }

    
    private Message setUpMessage(boolean requestor, 
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext,
                                 boolean decoupled,
                                 boolean zeroLengthAction) 
        throws Exception {
        boolean[] params = {requestor, 
                            outbound,
                            oneway,
                            usingAddressing,
                            mapsInContext,
                            decoupled,
                            zeroLengthAction,
                            false};  
        return setUpMessage(params);
    }

    /**
     * Boolean array is used to work around checkstyle rule limiting
     * parameter cardinality to 7. 
     */
    private Message setUpMessage(boolean[] params)
        throws Exception {
        boolean requestor = params[0]; 
        boolean outbound = params[1];
        boolean oneway = params[2];
        boolean usingAddressing = params[3];
        boolean mapsInContext = params[4];
        boolean decoupled = params[5];
        boolean zeroLengthAction = params[6];
        boolean fault = params[7];
        
        Message message = getMessage();        
        Exchange exchange = getExchange();
        setUpOutbound(message, exchange, outbound);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.valueOf(requestor));
        if (outbound && requestor) {
            if (usingAddressing) {
                setUpConduit(message, exchange);
            }
            setUpUsingAddressing(message, exchange, usingAddressing);
            if (usingAddressing) {
                setUpRequestor(message,
                               exchange,
                               oneway, 
                               mapsInContext,
                               decoupled,
                               zeroLengthAction);
            } 
        } else if (!requestor) {
            setUpResponder(message,
                           exchange,
                           oneway,
                           outbound,
                           decoupled,
                           zeroLengthAction,
                           fault);
        }
        control.replay();
        return message;
    }

    private void setUpUsingAddressing(Message message,
                                      Exchange exchange,
                                      boolean usingAddressing) {
        setUpMessageExchange(message, exchange);
        Endpoint endpoint = control.createMock(Endpoint.class);
        setUpExchangeGet(exchange, Endpoint.class, endpoint);
        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(endpointInfo);
        List<ExtensibilityElement> endpointExts =
            new ArrayList<ExtensibilityElement>();
        endpointInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(endpointExts);
        BindingInfo bindingInfo = control.createMock(BindingInfo.class);
        endpointInfo.getBinding();
        EasyMock.expectLastCall().andReturn(bindingInfo).times(2);
        bindingInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_LIST);
        ServiceInfo serviceInfo = control.createMock(ServiceInfo.class);
        endpointInfo.getService();
        EasyMock.expectLastCall().andReturn(serviceInfo).times(2);
        serviceInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_LIST);
        ExtensibilityElement ext = 
            control.createMock(ExtensibilityElement.class);
        if (usingAddressing) {
            QName elementType = usingAddressing 
                ? Names.WSAW_USING_ADDRESSING_QNAME 
                : new QName(SOAP_NAMESPACE, "encodingStyle");
            ext.getElementType();
            EasyMock.expectLastCall().andReturn(elementType);
            endpointExts.add(ext);
        }
    }
    
    private void setUpRequestor(Message message,
                                Exchange exchange,
                                boolean oneway,
                                boolean mapsInContext,
                                boolean decoupled,
                                boolean zeroLengthAction) throws Exception {
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.valueOf(Boolean.TRUE));
        AddressingPropertiesImpl maps = mapsInContext 
                                        ? new AddressingPropertiesImpl()
                                        : null;
        if (zeroLengthAction) {
            maps.setAction(ContextUtils.getAttributedURI(""));
        }
        setUpMessageProperty(message,
                                CLIENT_ADDRESSING_PROPERTIES,
                                maps);
        Method method = SEI.class.getMethod("op", new Class[0]);
        if (!zeroLengthAction) {
            setUpMethod(message, exchange, method);
            setUpMessageProperty(message,
                                    REQUESTOR_ROLE,
                                    Boolean.TRUE);
            expectedAction = "http://foo/bar/SEI/opRequest";
        }
        setUpMessageProperty(message,
                                REQUESTOR_ROLE,
                                Boolean.TRUE);
        setUpOneway(message, exchange, oneway);
        expectedMAPs = maps;
        expectedTo = Names.WSA_NONE_ADDRESS;
        expectedReplyTo = oneway 
                          ? Names.WSA_NONE_ADDRESS
                          : Names.WSA_ANONYMOUS_ADDRESS;
        // Now verified via verifyMessage()
        //EasyMock.eq(CLIENT_ADDRESSING_PROPERTIES_OUTBOUND);
        //EasyMock.reportMatcher(new MAPMatcher());
        //message.put(CLIENT_ADDRESSING_PROPERTIES_OUTBOUND,
        //            mapsInContext
        //            ? maps
        //            : new AddressingPropertiesImpl());
        //EasyMock.expectLastCall().andReturn(null);
    }

    private void setUpResponder(Message message,
                                Exchange exchange,
                                boolean oneway,
                                boolean outbound,
                                boolean decoupled,
                                boolean zeroLengthAction,
                                boolean fault) throws Exception {
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.FALSE);
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        EndpointReferenceType replyTo = new EndpointReferenceType();
        replyTo.setAddress(
            ContextUtils.getAttributedURI(decoupled
                                          ? "http://localhost:9999/decoupled"
                                          : Names.WSA_ANONYMOUS_ADDRESS));
        maps.setReplyTo(replyTo);
        EndpointReferenceType faultTo = new EndpointReferenceType();
        faultTo.setAddress(
            ContextUtils.getAttributedURI(decoupled
                                          ? "http://localhost:9999/fault"
                                          : Names.WSA_ANONYMOUS_ADDRESS));
        maps.setFaultTo(faultTo);
        AttributedURIType id = 
            ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        if (zeroLengthAction) {
            maps.setAction(ContextUtils.getAttributedURI(""));
        }
        setUpMessageProperty(message,
                             SERVER_ADDRESSING_PROPERTIES_INBOUND,
                             maps);
        if (!outbound) {
            setUpOneway(message, exchange, oneway);
            if (oneway || decoupled) {
                setUpRebase(message, exchange);
            }
        }
        if (outbound || aggregator.messageIDs.size() > 0) {
            if (!zeroLengthAction) {
                Method method = SEI.class.getMethod("op", new Class[0]);
                setUpMethod(message, exchange, method);
                setUpMessageProperty(message,
                                     REQUESTOR_ROLE,
                                     Boolean.FALSE);
                expectedAction = "http://foo/bar/SEI/opResponse";
            }
            setUpMessageProperty(message,
                                 REQUESTOR_ROLE,
                                 Boolean.FALSE);
            setUpMessageProperty(message,
                                 SERVER_ADDRESSING_PROPERTIES_INBOUND,
                                 maps);            
            if (fault) {
                // REVISIT test double rebase does not occur
                setUpRebase(message, exchange);
            }
            expectedTo = decoupled
                         ? "http://localhost:9999/decoupled"
                         : Names.WSA_ANONYMOUS_ADDRESS;
            expectedRelatesTo = maps.getMessageID().getValue();
            // Now verified via verifyMessage()
            //EasyMock.eq(SERVER_ADDRESSING_PROPERTIES_OUTBOUND);
            //EasyMock.reportMatcher(new MAPMatcher());
            //message.put(SERVER_ADDRESSING_PROPERTIES_OUTBOUND,
            //            new AddressingPropertiesImpl());
            //EasyMock.expectLastCall().andReturn(null);
        }
    }
    
    private void setUpRebase(Message message, Exchange exchange)
        throws Exception {
        setUpMessageProperty(message,
                             "org.apache.cxf.ws.addressing.partial.response.sent",
                             Boolean.FALSE);
        Endpoint endpoint = control.createMock(Endpoint.class);
        exchange.get(Endpoint.class);
        EasyMock.expectLastCall().andReturn(endpoint);
        Binding binding = control.createMock(Binding.class);
        endpoint.getBinding();        
        EasyMock.expectLastCall().andReturn(binding);
        Message partialResponse = getMessage();
        binding.createMessage();
        EasyMock.expectLastCall().andReturn(partialResponse);

        Destination target = control.createMock(Destination.class);
        setUpMessageDestination(message, target);
        Conduit backChannel = control.createMock(Conduit.class);
        target.getBackChannel(EasyMock.eq(message),
                              EasyMock.isA(Message.class),
                              EasyMock.isA(EndpointReferenceType.class));
        EasyMock.expectLastCall().andReturn(backChannel);
        // REVISIT test interceptor chain setup & send
    }

    private void setUpOneway(Message message, Exchange exchange, boolean oneway) {
        setUpMessageExchange(message, exchange);
        setUpExchangeOneway(exchange, oneway);
    }

    private void setUpOutbound(Message message, Exchange exchange, boolean outbound) {
        setUpMessageExchange(message, exchange);
        setUpExchangeOutbound(exchange, message, outbound);
    }

    private void setUpConduit(Message message, Exchange exchange) {
        setUpMessageExchange(message, exchange);        
        Conduit conduit = EasyMock.createMock(Conduit.class);
        setUpExchangeConduit(exchange, conduit);
        EndpointReferenceType to =
            ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        to.setAddress(ContextUtils.getAttributedURI(expectedTo));
        conduit.getTarget();
        EasyMock.expectLastCall().andReturn(to);
    }
    
    private void setUpMethod(Message message, Exchange exchange, Method method) {
        setUpMessageExchange(message, exchange);        
        OperationInfo opInfo = new OperationInfo(); 
        opInfo.setProperty(Method.class.getName(), method);
        BindingOperationInfo bindingOpInfo = new TestBindingOperationInfo(opInfo);
        setUpExchangeGet(exchange, BindingOperationInfo.class, bindingOpInfo);
        // Usual fun with EasyMock not always working as expected
        //BindingOperationInfo bindingOpInfo =
        //    EasyMock.createMock(BindingOperationInfo.class); 
        //OperationInfo opInfo = EasyMock.createMock(OperationInfo.class); 
        //bindingOpInfo.getOperationInfo();
        //EasyMock.expectLastCall().andReturn(opInfo);
        //opInfo.getProperty(EasyMock.eq(Method.class.getName()));
        //EasyMock.expectLastCall().andReturn(method);
    }
    
    private Message getMessage() {
        //return control.createMock(Message.class);
        return new MessageImpl();
    }

    private Exchange getExchange() {
        Exchange exchange = control.createMock(Exchange.class);
        //Exchange exchange = new ExchangeImpl();
        return exchange;
    }
    
    private void setUpMessageProperty(Message message, String key, Object value) {
        //message.get(key);
        //EasyMock.expectLastCall().andReturn(value);
        message.put(key, value);
    }

    private void setUpMessageExchange(Message message, Exchange exchange) {
        //message.getExchange();
        //EasyMock.expectLastCall().andReturn(exchange);
        message.setExchange(exchange);
    }
    
    private void setUpMessageDestination(Message message, Destination target) {
        //message.getDestination();
        //EasyMock.expectLastCall().andReturn(target);
        ((MessageImpl)message).setDestination(target);    
    }

    private <T> void setUpExchangeGet(Exchange exchange, Class<T> clz, T value) {
        exchange.get(clz);
        EasyMock.expectLastCall().andReturn(value);
        //exchange.put(Endpoint.class, value);
    }

    private void setUpExchangeOneway(Exchange exchange, boolean oneway) {
        exchange.isOneWay();
        EasyMock.expectLastCall().andReturn(oneway);
        //exchange.setOneWay(oneway);
    }

    private void setUpExchangeOutbound(Exchange exchange,
                                       Message message,
                                       boolean outbound) {
        exchange.getOutMessage();
        EasyMock.expectLastCall().andReturn(outbound ? message : null);
        //exchange.setOutMessage(outbound ? message : new MessageImpl());
    }

    private void setUpExchangeConduit(Exchange exchange, Conduit conduit) {
        exchange.getConduit();
        EasyMock.expectLastCall().andReturn(conduit);
        //exchange.setConduit(conduit);
    }

    private boolean verifyMAPs(Object obj) {
        if (obj instanceof AddressingPropertiesImpl) {
            AddressingPropertiesImpl other = (AddressingPropertiesImpl)obj;
            return compareExpected(other);
        }
        return false;
    }    

    private boolean compareExpected(AddressingPropertiesImpl other) {
        boolean ret = false;
        if (expectedMAPs == null || expectedMAPs == other) {
            boolean toOK = 
                expectedTo == null 
                || expectedTo.equals(other.getTo().getValue());
            boolean replyToOK = 
                expectedReplyTo == null 
                || expectedReplyTo.equals(
                       other.getReplyTo().getAddress().getValue());
            boolean relatesToOK =
                expectedRelatesTo == null 
                || expectedRelatesTo.equals(
                       other.getRelatesTo().getValue());
            boolean actionOK =
                expectedAction == null
                || expectedAction.equals(other.getAction().getValue());
            boolean messageIdOK = other.getMessageID() != null;
            ret = toOK 
                  && replyToOK 
                  && relatesToOK 
                  && actionOK
                  && messageIdOK;
        }
        return ret;
    }
    
    private String getMAPProperty(boolean requestor, boolean outbound) { 
        return requestor
               ? outbound
                 ? CLIENT_ADDRESSING_PROPERTIES_OUTBOUND
                 : CLIENT_ADDRESSING_PROPERTIES_INBOUND
               : outbound
                 ? SERVER_ADDRESSING_PROPERTIES_OUTBOUND
                 : SERVER_ADDRESSING_PROPERTIES_INBOUND;
    }

    private void verifyMessage(Message message,
                               boolean requestor,
                               boolean outbound,
                               boolean expectMapsInContext) {
        if (expectMapsInContext) {
            assertTrue("unexpected MAPs",
                       verifyMAPs(message.get(getMAPProperty(requestor, outbound))));
        } 
    }

    private static interface SEI {
        @RequestWrapper(targetNamespace = "http://foo/bar", className = "SEI", localName = "opRequest")
        @ResponseWrapper(targetNamespace = "http://foo/bar", className = "SEI", localName = "opResponse")
        String op();
    }
    
    private static class TestBindingOperationInfo extends BindingOperationInfo {
        public TestBindingOperationInfo(OperationInfo oi) {
            opInfo = oi;
        }
    }
}

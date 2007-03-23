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

package org.apache.cxf.ws.rm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RMInInterceptorTest extends TestCase {
    
    private IMocksControl control;
    private RMProperties rmps;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        rmps = control.createMock(RMProperties.class);
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testOrdering() {
        control.replay();
        Phase p = new Phase(Phase.PRE_LOGICAL, 1);
        PhaseInterceptorChain chain = 
            new PhaseInterceptorChain(Collections.singletonList(p));
        MAPAggregator map = new MAPAggregator();
        RMInInterceptor rmi = new RMInInterceptor();        
        chain.add(rmi);
        chain.add(map);
        Iterator it = chain.iterator();
        assertSame("Unexpected order.", rmi, it.next());
        assertSame("Unexpected order.", map, it.next());
    } 
    
    
    @Test
    public void testHandleCreateSequenceOnServer() throws SequenceFault {
        RMInInterceptor interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RMConstants.getCreateSequenceAction(), true);   
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        
        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleCreateSequenceOnClient() throws SequenceFault {
        RMInInterceptor interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RMConstants.getCreateSequenceAction(), false);       
        RMManager manager = control.createMock(RMManager.class);
        interceptor.setManager(manager);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(rme);
        Servant servant = control.createMock(Servant.class);
        EasyMock.expect(rme.getServant()).andReturn(servant);
        CreateSequenceResponseType csr = control.createMock(CreateSequenceResponseType.class);
        EasyMock.expect(servant.createSequence(message)).andReturn(csr);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy);
        proxy.createSequenceResponse(csr);
        EasyMock.expectLastCall();
        
        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleSequenceAckOnClient() throws SequenceFault, NoSuchMethodException {
        testHandleSequenceAck(false);
    }
    
    @Test
    public void testHandleSequenceAckOnServer() throws SequenceFault, NoSuchMethodException {
        testHandleSequenceAck(true);
    }
    
    private void testHandleSequenceAck(boolean onServer) throws SequenceFault, NoSuchMethodException {
        Method m = RMInInterceptor.class.getDeclaredMethod("processAcknowledgments",
            new Class[] {RMProperties.class});
        RMInInterceptor interceptor = control.createMock(RMInInterceptor.class, new Method[] {m});
        Message message = setupInboundMessage(RMConstants.getSequenceAckAction(), onServer);
        interceptor.processAcknowledgments(rmps);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);

        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleTerminateSequenceOnServer() throws SequenceFault {
        testHandleTerminateSequence(true);        
    }
    
    @Test
    public void testHandleTerminateSequenceOnClient() throws SequenceFault {
        testHandleTerminateSequence(false);        
    }
    
    private void testHandleTerminateSequence(boolean onServer) throws SequenceFault {
        RMInInterceptor interceptor = new RMInInterceptor();
        Message message = setupInboundMessage(RMConstants.getTerminateSequenceAction(), onServer);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);

        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testAppRequest() throws SequenceFault, NoSuchMethodException {
        testAppMessage(true);
    }
    
    @Test
    public void testAppResponse() throws SequenceFault, NoSuchMethodException {
        testAppMessage(false);
    }
    
    private void testAppMessage(boolean onServer) throws SequenceFault, NoSuchMethodException {
        Method m1 = RMInInterceptor.class.getDeclaredMethod("processAcknowledgments",
                                                            new Class[] {RMProperties.class});
        Method m2 = RMInInterceptor.class.getDeclaredMethod("processAcknowledgmentRequests",
                                                            new Class[] {RMProperties.class});
        Method m3 = RMInInterceptor.class.getDeclaredMethod("processSequence",
                                                            new Class[] {Destination.class, Message.class});
        Method m4 = RMInInterceptor.class.getDeclaredMethod("processDeliveryAssurance",
                                                            new Class[] {RMProperties.class});
        RMInInterceptor interceptor = control
            .createMock(RMInInterceptor.class, new Method[] {m1, m2, m3, m4});
        Message message = setupInboundMessage("greetMe", true);
        RMManager manager = control.createMock(RMManager.class);
        interceptor.setManager(manager);
        Destination d = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(d);
        interceptor.processAcknowledgments(rmps);
        EasyMock.expectLastCall();
        interceptor.processAcknowledgmentRequests(rmps);
        EasyMock.expectLastCall();
        interceptor.processSequence(d, message);
        EasyMock.expectLastCall();
        interceptor.processDeliveryAssurance(rmps);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);

        control.replay();
        interceptor.handle(message);
    }  
    
    @Test
    public void testProcessAcknowledgments() {
        RMInInterceptor interceptor = new RMInInterceptor();
        RMManager manager = control.createMock(RMManager.class);
        interceptor.setManager(manager);
        SequenceAcknowledgement ack1 = control.createMock(SequenceAcknowledgement.class);
        SequenceAcknowledgement ack2 = control.createMock(SequenceAcknowledgement.class);
        Collection<SequenceAcknowledgement> acks = new ArrayList<SequenceAcknowledgement>();
        acks.add(ack1);
        acks.add(ack2);
        EasyMock.expect(rmps.getAcks()).andReturn(acks);
        Identifier id1 = control.createMock(Identifier.class);
        EasyMock.expect(ack1.getIdentifier()).andReturn(id1);
        SourceSequence ss1 = control.createMock(SourceSequence.class);
        EasyMock.expect(manager.getSourceSequence(id1)).andReturn(ss1);
        ss1.setAcknowledged(ack1);
        EasyMock.expectLastCall();
        Identifier id2 = control.createMock(Identifier.class);
        EasyMock.expect(ack2.getIdentifier()).andReturn(id2);
        EasyMock.expect(manager.getSourceSequence(id2)).andReturn(null);
        EasyMock.expect(id2.getValue()).andReturn("s2");
        
        control.replay();
        try {
            interceptor.processAcknowledgments(rmps);
            fail("Expected SequenceFault not thrown");
        } catch (SequenceFault sf) {
            assertEquals(RMConstants.getUnknownSequenceFaultCode(), sf.getFaultInfo().getFaultCode());
        }
    }
    
    @Test
    public void testProcessAcknowledgmentRequests() {
        control.replay();
        // TODI
    }
    
    @Test
    public void testProcessSequence() throws SequenceFault {
        Destination destination = control.createMock(Destination.class);
        Message message = control.createMock(Message.class);
        destination.acknowledge(message);
        EasyMock.expectLastCall();        
        control.replay();
        RMInInterceptor interceptor = new RMInInterceptor();
        interceptor.processSequence(destination, message);
    }
    
    @Test
    public void testProcessDeliveryAssurance() {
        control.replay(); 
        // TODO
    }
    
    
    

    private Message setupInboundMessage(String action, boolean serverSide) {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).times(2);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);        
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(!serverSide);
        AddressingPropertiesImpl maps = control.createMock(AddressingPropertiesImpl.class);
        EasyMock.expect(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        
        AttributedURIType actionURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(maps.getAction()).andReturn(actionURI).times(2);
        EasyMock.expect(actionURI.getValue()).andReturn(action);
        
        EasyMock.expect(message.get(RMMessageConstants.ORIGINAL_REQUESTOR_ROLE)).andReturn(Boolean.FALSE);
        EasyMock.expect(message.put(Message.REQUESTOR_ROLE, Boolean.FALSE)).andReturn(null);
        
        org.apache.cxf.transport.Destination td = 
            serverSide ? control.createMock(org.apache.cxf.transport.Destination.class) : null;
        EasyMock.expect(exchange.getDestination()).andReturn(td);
        return message;
    }
    
}

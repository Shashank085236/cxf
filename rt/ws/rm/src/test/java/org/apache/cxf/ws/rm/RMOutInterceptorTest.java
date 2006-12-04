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
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class RMOutInterceptorTest extends TestCase {
    
    private IMocksControl control;
    
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    public void testOrdering() {
        Phase p = new Phase(Phase.PRE_LOGICAL, 1);
        PhaseInterceptorChain chain = 
            new PhaseInterceptorChain(Collections.singletonList(p));
        MAPAggregator map = new MAPAggregator();
        RMOutInterceptor rmi = new RMOutInterceptor();        
        chain.add(rmi);
        chain.add(map);
        Iterator it = chain.iterator();
        assertSame("Unexpected order.", map, it.next());
        assertSame("Unexpected order.", rmi, it.next());                      
    } 
    
    public void testHandleApplicationMessage() throws NoSuchMethodException, SequenceFault {
        AddressingPropertiesImpl maps = createMAPs("greetMe", "localhost:9000/GreeterPort", 
            org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
        Method[] mocked = new Method[] {
            AbstractRMInterceptor.class.getDeclaredMethod("getManager", new Class[]{}),
            RMOutInterceptor.class.getDeclaredMethod("addAcknowledgements",
                new Class[] {Destination.class, RMProperties.class, Identifier.class, 
                             AttributedURI.class})            
        };
        RMOutInterceptor interceptor = control.createMock(RMOutInterceptor.class, mocked); 
        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(interceptor.getManager()).andReturn(manager).times(5);
        
        Message message = control.createMock(Message.class);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(ex).times(2);
        EasyMock.expect(ex.getOutMessage()).andReturn(message);
        EasyMock.expect(message.getContent(List.class)).andReturn(Collections.singletonList("CXF"));        
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();        
        EasyMock.expect(message.get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND))
            .andReturn(maps).anyTimes();
        RMProperties rmpsOut = new RMProperties();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).andReturn(rmpsOut);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(null);
        InterceptorChain chain = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(chain);
        chain.add(EasyMock.isA(RetransmissionInterceptor.class));
        EasyMock.expectLastCall();
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        EasyMock.expect(manager.getRetransmissionQueue()).andReturn(queue);
        queue.start();
        EasyMock.expectLastCall();
                
        Source source = control.createMock(Source.class);
        EasyMock.expect(manager.getSource(message)).andReturn(source);
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(destination);
        SourceSequence sseq = control.createMock(SourceSequence.class);
        EasyMock.expect(manager.getSequence((Identifier)EasyMock.isNull(), EasyMock.same(message), 
                                        EasyMock.same(maps))).andReturn(sseq);
        EasyMock.expect(sseq.nextMessageNumber((Identifier)EasyMock.isNull(), 
            (BigInteger)EasyMock.isNull())).andReturn(BigInteger.TEN);
        EasyMock.expect(sseq.isLastMessage()).andReturn(false).times(2);
        interceptor.addAcknowledgements(EasyMock.same(destination), EasyMock.same(rmpsOut), 
            (Identifier)EasyMock.isNull(), EasyMock.isA(AttributedURI.class));
        EasyMock.expectLastCall();
        Identifier sid = control.createMock(Identifier.class);
        EasyMock.expect(sseq.getIdentifier()).andReturn(sid);
        EasyMock.expect(sseq.getCurrentMessageNr()).andReturn(BigInteger.TEN);

        
        control.replay();
        interceptor.handleMessage(message, false);
        assertSame(sid, rmpsOut.getSequence().getIdentifier());        
        assertEquals(BigInteger.TEN, rmpsOut.getSequence().getMessageNumber());
        assertNull(rmpsOut.getSequence().getLastMessage());
        control.verify();
    }
    
    private AddressingPropertiesImpl createMAPs(String action, String to, String replyTo) {
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        org.apache.cxf.ws.addressing.ObjectFactory factory = 
            new org.apache.cxf.ws.addressing.ObjectFactory();
        AttributedURIType uri = factory.createAttributedURIType();
        uri.setValue(action);
        maps.setAction(uri);
        uri = factory.createAttributedURIType();
        uri.setValue(to);
        maps.setTo(uri);
        EndpointReferenceType epr = RMUtils.createReference(replyTo);
        maps.setReplyTo(epr);
        return maps;
           
    }
}

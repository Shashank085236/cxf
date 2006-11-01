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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import javax.xml.namespace.QName;


import junit.framework.TestCase;

import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.apache.cxf.ws.rm.policy.RMAssertion.AcknowledgementInterval;
import org.apache.cxf.ws.rm.policy.RMAssertion.BaseRetransmissionInterval;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class DestinationSequenceTest extends TestCase {

    private IMocksControl control;
    private ObjectFactory factory;
    private Identifier id;
    private EndpointReferenceType ref;
    private Destination destination;
    private RMManager manager;
    private RMAssertion rma;
    private AcksPolicyType ap;
    private DestinationPolicyType dp;
 
    public void setUp() {
        control = EasyMock.createNiceControl();
        factory = new ObjectFactory();
        
        ref = control.createMock(EndpointReferenceType.class);                
        id = factory.createIdentifier();
        id.setValue("seq");
    }
    
    public void tearDown() {
        ref = null;
        destination = null;
        manager = null;
        rma = null;
        dp = null;
        ap = null;
        
    }

    public void testConstructors() {
  
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertEquals(id, seq.getIdentifier());
        assertNull(seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertNotNull(seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());   
        
        SequenceAcknowledgement ack = RMUtils.getWSRMFactory().createSequenceAcknowledgement();        
        seq = new DestinationSequence(id, ref, BigInteger.TEN, ack);
        assertEquals(id, seq.getIdentifier());
        assertEquals(BigInteger.TEN, seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertSame(ack, seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());  

    }
    
    public void testEqualsAndHashCode() {     
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        DestinationSequence otherSeq = null;
        assertTrue(!seq.equals(otherSeq));
        otherSeq = new DestinationSequence(id, ref, destination);
        assertEquals(seq, otherSeq);
        assertEquals(seq.hashCode(), otherSeq.hashCode());
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        otherSeq = new DestinationSequence(otherId, ref, destination);
        assertTrue(!seq.equals(otherSeq));
        assertTrue(seq.hashCode() != otherSeq.hashCode()); 
        assertTrue(!seq.equals(this));
    }
    
    public void testGetSetDestination() {
        control.replay();
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        seq.setDestination(destination);
        assertSame(destination, seq.getDestination());
    }
    
    public void testGetEndpointIdentifier() {
        setUpDestination();
        QName qn = new QName("abc", "xyz");
        EasyMock.expect(destination.getName()).andReturn(qn);
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertEquals("Unexpected endpoint identifier", "{abc}xyz", seq.getEndpointIdentifier());
        control.verify();
    }
    
    public void testGetAcknowledgementAsStream() throws SequenceFault {
        /*
        destination.getHandler();
        expectLastCall().andReturn(handler).times(3);
        handler.getStore();
        expectLastCall().andReturn(null);
        handler.getConfigurationHelper();
        expectLastCall().andReturn(configurationHelper).times(2);
        configurationHelper.getRMAssertion();
        expectLastCall().andReturn(rma);
        configurationHelper.getAcksPolicy();
        expectLastCall().andReturn(ap);
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        assertEquals(0, ranges.size());
              
        seq.acknowledge(new BigInteger("1"));  
        assertNotNull(seq.getAcknowledgmentAsStream());
        
        control.verify();
        */
    }
    
    public void testAcknowledgeBasic() throws SequenceFault {
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        assertEquals(0, ranges.size());
              
        seq.acknowledge(new BigInteger("1"));        
        assertEquals(1, ranges.size());
        AcknowledgementRange r1 = ranges.get(0);
        assertEquals(1, r1.getLower().intValue());
        assertEquals(1, r1.getUpper().intValue());
        
        seq.acknowledge(new BigInteger("2"));
        assertEquals(1, ranges.size());
        r1 = ranges.get(0);
        assertEquals(1, r1.getLower().intValue());
        assertEquals(2, r1.getUpper().intValue());
        
        control.verify();
    }
    
    public void testAcknowledgeLastMessageNumberExceeded() throws SequenceFault {  
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        
        seq.acknowledge(BigInteger.ONE);
        seq.setLastMessageNumber(BigInteger.ONE);
        try {
            seq.acknowledge(new BigInteger("2"));
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault sf) {
            assertEquals("LastMessageNumberExceeded", sf.getFaultInfo().getFaultCode().getLocalPart());
        }
        
        control.verify();
    }
    
    public void testAcknowledgeAppendRange() throws SequenceFault {
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();        
        seq.acknowledge(new BigInteger("1"));
        seq.acknowledge(new BigInteger("2"));  
        seq.acknowledge(new BigInteger("5"));
        seq.acknowledge(new BigInteger("4"));
        seq.acknowledge(new BigInteger("6"));
        assertEquals(2, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(1, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(6, r.getUpper().intValue()); 
        
        control.verify();
    }
    
    public void testAcknowledgeInsertRange() throws SequenceFault {
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();        
        seq.acknowledge(new BigInteger("1"));
        seq.acknowledge(new BigInteger("2"));
        seq.acknowledge(new BigInteger("9"));
        seq.acknowledge(new BigInteger("10"));
        seq.acknowledge(new BigInteger("4"));
        seq.acknowledge(new BigInteger("9"));
        seq.acknowledge(new BigInteger("2"));
        
        assertEquals(3, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(1, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(4, r.getUpper().intValue()); 
        r = ranges.get(2);
        assertEquals(9, r.getLower().intValue());
        assertEquals(10, r.getUpper().intValue()); 
        
        control.verify();
    }
    
    public void testAcknowledgePrependRange() throws SequenceFault { 
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        seq.acknowledge(new BigInteger("4"));
        seq.acknowledge(new BigInteger("5"));
        seq.acknowledge(new BigInteger("6"));
        seq.acknowledge(new BigInteger("4"));
        seq.acknowledge(new BigInteger("2"));
        seq.acknowledge(new BigInteger("2"));
        assertEquals(2, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(2, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(6, r.getUpper().intValue()); 
        
        control.verify();
    }
    
    public void testMonitor() throws SequenceFault {
        setUpDestination();
        control.replay();
                
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        SequenceMonitor monitor = seq.getMonitor();
        assertNotNull(monitor);
        monitor.setMonitorInterval(500);
        
        assertEquals(0, monitor.getMPM());
        
        BigInteger mn = BigInteger.ONE;
        
        for (int i = 0; i < 10; i++) {
            seq.acknowledge(mn);
            mn = mn.add(BigInteger.ONE);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        int mpm1 = monitor.getMPM();
        assertTrue(mpm1 > 0);
        
        for (int i = 0; i < 5; i++) {
            seq.acknowledge(mn);
            mn = mn.add(BigInteger.ONE);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        int mpm2 = monitor.getMPM();
        assertTrue(mpm2 > 0);
        assertTrue(mpm1 > mpm2);
        
        control.verify();
    }
    
    public void testAcknowledgeImmediate() throws SequenceFault {
        setUpDestination();
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertTrue(!seq.sendAcknowledgement());
              
        seq.acknowledge(new BigInteger("1")); 
        
        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());
        
        control.verify();
    }
    
    public void testAcknowledgeDeferred() throws SequenceFault, IOException {
        Timer timer = new Timer();
        setUpDestination(timer);
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme).anyTimes();
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy).anyTimes();        
        proxy.acknowledge(seq);
        EasyMock.expectLastCall();
        
        control.replay();
        
        ap.setIntraMessageThreshold(0);
        AcknowledgementInterval ai = new org.apache.cxf.ws.rm.policy.ObjectFactory()
            .createRMAssertionAcknowledgementInterval();
        ai.setMilliseconds(new BigInteger("200"));
        rma.setAcknowledgementInterval(ai);        

        assertTrue(!seq.sendAcknowledgement());   
              
        seq.acknowledge(new BigInteger("1")); 
        seq.acknowledge(new BigInteger("2"));
        seq.acknowledge(new BigInteger("3"));
        
        assertFalse(seq.sendAcknowledgement());
        
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            // ignore
        }
        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());
        
        control.verify();
    }
    
    public void testCorrelationID() {
        setUpDestination();
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        String correlationID = "abdc1234";
        assertNull("unexpected correlation ID", seq.getCorrelationID());
        seq.setCorrelationID(correlationID);
        assertEquals("unexpected correlation ID",
                     correlationID,
                     seq.getCorrelationID());
    }
    
    public void testApplyDeliveryAssuranceAtMostOnce() {
        setUpDestination();
        
        BigInteger mn = BigInteger.TEN;        
        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(true);                    
        
        control.replay();        
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        assertTrue("message had already been delivered", ds.applyDeliveryAssurance(mn));
        control.verify();
        
        control.reset();
        ranges.add(r);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(true);            
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        EasyMock.expect(r.getLower()).andReturn(new BigInteger("5"));
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("15"));
        control.replay();        
        assertTrue("message has not yet been delivered", !ds.applyDeliveryAssurance(mn));
        control.verify();

    }
    
    public void testInOrderNoWait() {
        setUpDestination();

        BigInteger mn = BigInteger.TEN;
        
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(false);
        EasyMock.expect(da.isSetAtLeastOnce()).andReturn(true);
        EasyMock.expect(da.isSetInOrder()).andReturn(true); 
        
        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        ranges.add(r);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("15"));
        
        control.replay(); 
        
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        assertTrue(ds.applyDeliveryAssurance(mn));
        control.verify();
    }
    
    public void testInOrderWait() {
        setUpDestination();
        
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da).anyTimes();
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(false).anyTimes();
        EasyMock.expect(da.isSetAtLeastOnce()).andReturn(true).anyTimes();
        EasyMock.expect(da.isSetInOrder()).andReturn(true).anyTimes(); 
        
        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        final int n = 5;
        final AcknowledgementRange r = 
            factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setUpper(new BigInteger(Integer.toString(n)));
        ranges.add(r);
        final DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
          
        class Acknowledger extends Thread {
            BigInteger mn;
            
            Acknowledger(String mnStr) {
                mn = new BigInteger(mnStr);
            }
            
            public void run() {
                try {
                    ds.acknowledge(mn);
                    ds.applyDeliveryAssurance(mn);
                } catch (SequenceFault ex) {
                    // ignore
                }
            }            
        }
 
        control.replay(); 
        
        Thread[] threads = new Thread[n];
        for (int i = n - 1; i >= 0; i--) {
            threads[i] = new Acknowledger(Integer.toString(i + 1));
            threads[i].start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        
        boolean timedOut = false;
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join(1000); 
            } catch (InterruptedException ex) {
                timedOut = true;
            }
        }
        assertTrue("timed out waiting for messages to be processed in order", !timedOut);
        
        control.verify();
    }
    
    public void testAllPredecessorsAcknowledged() {

        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        control.replay();
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        ranges.add(r);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(2);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.TEN);
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("5"));
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(BigInteger.TEN);
        control.replay();
        assertTrue("not all predecessors acknowledged", ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        ranges.add(r);
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
    }
    
    private void setUpDestination() {
        setUpDestination(null);
    }
    
    private void setUpDestination(Timer timer) {
        
        manager = control.createMock(RMManager.class);

        org.apache.cxf.ws.rm.manager.ObjectFactory cfgFactory =
            new org.apache.cxf.ws.rm.manager.ObjectFactory();
        dp = cfgFactory.createDestinationPolicyType();
        ap = cfgFactory.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        
        org.apache.cxf.ws.rm.policy.ObjectFactory policyFactory =
            new org.apache.cxf.ws.rm.policy.ObjectFactory();
        rma = policyFactory.createRMAssertion();
        BaseRetransmissionInterval bri =
            policyFactory.createRMAssertionBaseRetransmissionInterval();
        bri.setMilliseconds(new BigInteger("3000"));
        rma.setBaseRetransmissionInterval(bri);  

        EasyMock.expect(manager.getRMAssertion()).andReturn(rma).anyTimes();
        EasyMock.expect(manager.getDestinationPolicy()).andReturn(dp).anyTimes();
        EasyMock.expect(manager.getStore()).andReturn(null).anyTimes();
        
        destination = control.createMock(Destination.class);
        EasyMock.expect(destination.getManager()).andReturn(manager).anyTimes();
        
        if (null != timer) {
            EasyMock.expect(manager.getTimer()).andReturn(timer).anyTimes();
        }

    }


}

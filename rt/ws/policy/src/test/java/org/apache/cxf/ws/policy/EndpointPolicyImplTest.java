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

package org.apache.cxf.ws.policy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class EndpointPolicyImplTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    } 
    
    @Test
    public void testAccessors() {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();
        assertNull(epi.getPolicy());
        assertNull(epi.getChosenAlternative());
        assertNull(epi.getInterceptors());
        assertNull(epi.getFaultInterceptors());
        assertNull(epi.getVocabulary());
        assertNull(epi.getFaultVocabulary());
        
        Policy p = control.createMock(Policy.class);
        Assertion a = control.createMock(Assertion.class);
        List<Assertion> la = Collections.singletonList(a);
        Interceptor i = control.createMock(Interceptor.class);
        List<Interceptor> li = Collections.singletonList(i);
        control.replay();
        epi.setPolicy(p);
        assertSame(p, epi.getPolicy());
        epi.setChosenAlternative(la);
        assertSame(la, epi.getChosenAlternative());
        epi.setInterceptors(li);
        assertSame(li, epi.getInterceptors());
        epi.setFaultInterceptors(li);
        assertSame(li, epi.getFaultInterceptors());
        epi.setVocabulary(la);
        assertSame(la, epi.getVocabulary());
        epi.setFaultVocabulary(la);
        assertSame(la, epi.getFaultVocabulary());
        control.verify();
    }
    
    @Test
    public void testInitialise() throws NoSuchMethodException {
        Method m1 = EndpointPolicyImpl.class.getDeclaredMethod("initialisePolicy",
            new Class[] {EndpointInfo.class, PolicyEngineImpl.class});
        Method m2 = EndpointPolicyImpl.class.getDeclaredMethod("chooseAlternative",
            new Class[] {PolicyEngineImpl.class, Assertor.class});
        Method m3 = EndpointPolicyImpl.class.getDeclaredMethod("initialiseVocabulary",
            new Class[] {EndpointInfo.class, boolean.class, PolicyEngineImpl.class});
        Method m4 = EndpointPolicyImpl.class.getDeclaredMethod("initialiseInterceptors",
            new Class[] {EndpointInfo.class, boolean.class, PolicyEngineImpl.class});
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class, 
                                                    new Method[] {m1, m2, m3, m4});
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        boolean isRequestor = false;
        PolicyEngineImpl pe = control.createMock(PolicyEngineImpl.class);
        Assertor a = control.createMock(Assertor.class);
         
        epi.initialisePolicy(ei, pe);
        EasyMock.expectLastCall();
        epi.chooseAlternative(pe, a);
        EasyMock.expectLastCall();
        epi.initialiseInterceptors(ei, isRequestor, pe); 
        EasyMock.expectLastCall();
        
        control.replay();
        epi.initialise(ei, isRequestor, pe, a);
        control.verify();        
    }
    
    @Test
    public void testInitialisePolicy() {        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si);
        Policy sp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedServicePolicy(si)).andReturn(sp);
        Policy ep = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedEndpointPolicy(ei)).andReturn(ep);
        Policy merged = control.createMock(Policy.class);
        EasyMock.expect(sp.merge(ep)).andReturn(merged);
        EasyMock.expect(merged.normalize(true)).andReturn(merged);
        
        control.replay();
        EndpointPolicyImpl epi = new EndpointPolicyImpl();
        epi.initialisePolicy(ei, engine);
        assertSame(merged, epi.getPolicy());
        control.verify();
    }
       
    @Test
    public void testChooseAlternative() {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();        
        Policy policy = new Policy();
        epi.setPolicy(policy);        
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        Assertor assertor = control.createMock(Assertor.class);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor)).andReturn(null);
        
        control.replay();
        try {
            epi.chooseAlternative(engine, assertor);  
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();
        
        control.reset();        
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        Collection<Assertion> alternative = new ArrayList<Assertion>();
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor)).andReturn(alternative);
        control.replay();        
        epi.chooseAlternative(engine, assertor);
        Collection<Assertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);   
        control.verify();
    }
    
    @Test
    public void testInitialiseVocabularyServer() {
        dotestInitialiseVocabulary(false);
    }
    
    @Test
    public void testInitialiseVocabularyClient() {
        dotestInitialiseVocabulary(true);
    }
    
    private void dotestInitialiseVocabulary(boolean requestor) {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();   
        List<Assertion> alternative = new ArrayList<Assertion>();
        epi.setChosenAlternative(alternative);
        Assertion ea = control.createMock(Assertion.class);        
        alternative.add(ea);
        EasyMock.expect(ea.isOptional()).andReturn(false);
        Assertion eaOpt = control.createMock(Assertion.class);        
        alternative.add(eaOpt);
        EasyMock.expect(eaOpt.isOptional()).andReturn(true);
        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bi.getOperations()).andReturn(Collections.singletonList(boi));
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        Policy op = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedOperationPolicy(boi)).andReturn(op);
        Assertion oa = control.createMock(Assertion.class);
        EasyMock.expect(engine.getAssertions(op, false)).andReturn(Collections.singletonList(oa));
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        if (requestor) {
            EasyMock.expect(boi.getOutput()).andReturn(bmi).times(2);
        } else {
            EasyMock.expect(boi.getInput()).andReturn(bmi);
        }
        Policy mp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedMessagePolicy(bmi)).andReturn(mp);
        Assertion ma = control.createMock(Assertion.class);
        EasyMock.expect(engine.getAssertions(mp, false)).andReturn(Collections.singletonList(ma));
        Assertion fa = null;
        if (requestor) {
            BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
            EasyMock.expect(boi.getFaults()).andReturn(Collections.singletonList(bfi));
            Policy fp = control.createMock(Policy.class);
            EasyMock.expect(engine.getAggregatedFaultPolicy(bfi)).andReturn(fp);
            fa = control.createMock(Assertion.class);
            EasyMock.expect(engine.getAssertions(fp, false)).andReturn(Collections.singletonList(fa));
        }
        
        control.replay();
        epi.initialiseVocabulary(ei, requestor, engine);
        Collection<Assertion> expected = new ArrayList<Assertion>();
        expected.add(ea);
        expected.add(oa);
        expected.add(ma);
        verifyVocabulary(expected, epi.getVocabulary());
        if (requestor) {
            expected.remove(ma);
            expected.add(fa);
            verifyVocabulary(expected, epi.getFaultVocabulary());
            // 
        } else {
            assertNull(epi.getFaultVocabulary());
        }
          
        control.verify();          
    }
    
    private void verifyVocabulary(Collection<Assertion> expected, Collection<Assertion> actual) {
        
        assertEquals(expected.size(), actual.size());
        for (Iterator<Assertion> i = expected.iterator(); i.hasNext();) {
            Assertion e = i.next();
            Iterator<Assertion> j = actual.iterator();
            boolean eFound = false;
            while (j.hasNext()) {
                if (e == j.next()) {
                    eFound = true;
                    break;
                }              
            }
            assertTrue("Expected assertion not found.", eFound);
        }
    }
    
    @Test
    public void testInitialiseInterceptorsServer() {
        doTestInitialiseInterceptors(false);
    }
    
    @Test
    public void testInitialiseInterceptorsClient() {
        doTestInitialiseInterceptors(true);
    }
    
    private void doTestInitialiseInterceptors(boolean requestor) {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();        
        Collection<Assertion> v = new ArrayList<Assertion>();
        Collection<Assertion> fv = new ArrayList<Assertion>();
        Assertion a = control.createMock(Assertion.class);        
        v.add(a);
        QName aqn = new QName("http://x.y.z", "a");
        EasyMock.expect(a.getName()).andReturn(aqn).times(requestor ? 2 : 1);
        Assertion aa = control.createMock(Assertion.class);        
        v.add(aa);
        EasyMock.expect(aa.getName()).andReturn(aqn).times(requestor ? 2 : 1);
        fv.addAll(v);
        epi.setVocabulary(v);
        epi.setFaultVocabulary(fv);
        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);
        
        PolicyInterceptorProvider app = control.createMock(PolicyInterceptorProvider.class);               
        EasyMock.expect(reg.get(aqn)).andReturn(app).times(requestor ? 2 : 1);
        Interceptor api = control.createMock(Interceptor.class);
        EasyMock.expect(app.getInInterceptors())
            .andReturn(Collections.singletonList(api));
        if (requestor) {
            EasyMock.expect(app.getInFaultInterceptors())
                .andReturn(Collections.singletonList(api));
        }
        
        control.replay();
        epi.initialiseInterceptors(ei, requestor, engine);
        assertEquals(1, epi.getInterceptors().size());
        assertSame(api, epi.getInterceptors().get(0));
        if (requestor) {
            assertEquals(1, epi.getFaultInterceptors().size());
            assertSame(api, epi.getFaultInterceptors().get(0));
        } else {
            assertNull(epi.getFaultInterceptors());
        }
        control.verify();          
    }
    
    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine, 
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = control.createMock(Bus.class);        
        EasyMock.expect(engine.getBus()).andReturn(bus);
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg);
    }
    
  
}

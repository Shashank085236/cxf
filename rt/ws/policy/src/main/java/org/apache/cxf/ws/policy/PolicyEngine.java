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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;

/**
 * 
 */
public class PolicyEngine implements BusExtension {
     
    private Bus bus;
    private PolicyRegistry registry;
    private Collection<PolicyProvider> policyProviders;
    private boolean registerInterceptors;

    private Map<BindingOperation, OutPolicyInfo> clientRequestInfo;
    
    private Map<BindingOperation, OutPolicyInfo> clientResponseInfo;
    
    private Map<BindingFault, OutPolicyInfo> clientFaultInfo;
    
    private Map<Endpoint, EndpointPolicyInfo> endpointInfo;
    
    private Map<BindingOperation, OutPolicyInfo> serverRequestInfo;
    
    private Map<BindingOperation, OutPolicyInfo> serverResponseInfo;
    
    private Map<BindingFault, OutPolicyInfo> serverFaultInfo;

    public PolicyEngine() {        
        init();
    }
    
    protected final void init() {
        
        registry = new PolicyRegistryImpl();
        
        clientRequestInfo 
            = new ConcurrentHashMap<BindingOperation, OutPolicyInfo>();
    
        clientResponseInfo 
            = new ConcurrentHashMap<BindingOperation, OutPolicyInfo>();
    
        clientFaultInfo 
            = new ConcurrentHashMap<BindingFault, OutPolicyInfo>();
    
        endpointInfo 
            = new ConcurrentHashMap<Endpoint, EndpointPolicyInfo>();
    
        serverRequestInfo 
            = new ConcurrentHashMap<BindingOperation, OutPolicyInfo>();
    
        serverResponseInfo 
            = new ConcurrentHashMap<BindingOperation, OutPolicyInfo>();
    
        serverFaultInfo 
            = new ConcurrentHashMap<BindingFault, OutPolicyInfo>();
    }
    
    public Class<?> getRegistrationType() {
        return PolicyEngine.class;
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public void setPolicyProviders(Collection<PolicyProvider> p) {
        policyProviders = p;
    }
   
    public Collection<PolicyProvider> getPolicyProviders() {
        return policyProviders;
    }
    
    public void setRegistry(PolicyRegistry r) {
        registry = r;
    }
    
    public PolicyRegistry getRegistry() {
        return registry;
    }
      
    public boolean getRegisterInterceptors() {
        return registerInterceptors;
    }

    public void setRegisterInterceptors(boolean ri) {
        registerInterceptors = ri;
    }
    
    @PostConstruct
    public void addBusInterceptors() {
        if (null == bus || !registerInterceptors) {
            return;
        }

        ClientPolicyOutInterceptor clientOut = new ClientPolicyOutInterceptor();
        clientOut.setBus(bus);
        bus.getOutInterceptors().add(clientOut);
        ClientPolicyInInterceptor clientIn = new ClientPolicyInInterceptor();
        clientIn.setBus(bus);
        bus.getInInterceptors().add(clientIn);
        ClientPolicyInFaultInterceptor clientInFault = new ClientPolicyInFaultInterceptor();
        clientInFault.setBus(bus);
        bus.getInFaultInterceptors().add(clientInFault);
        
        ServerPolicyInInterceptor serverIn = new ServerPolicyInInterceptor();
        serverIn.setBus(bus);
        bus.getInInterceptors().add(serverIn);
        ServerPolicyOutInterceptor serverOut = new ServerPolicyOutInterceptor();
        serverOut.setBus(bus);
        bus.getOutInterceptors().add(serverOut);
        ServerPolicyOutFaultInterceptor serverOutFault = new ServerPolicyOutFaultInterceptor();
        serverOutFault.setBus(bus);
        bus.getOutFaultInterceptors().add(serverOutFault);
        
        PolicyVerificationOutInterceptor verifyOut = new PolicyVerificationOutInterceptor();
        verifyOut.setBus(bus);
        bus.getOutInterceptors().add(verifyOut);
        PolicyVerificationInInterceptor verifyIn = new PolicyVerificationInInterceptor();
        verifyIn.setBus(bus);
        bus.getInInterceptors().add(verifyIn);
        PolicyVerificationInFaultInterceptor verifyInFault = new PolicyVerificationInFaultInterceptor();
        verifyInFault.setBus(bus);
        bus.getInFaultInterceptors().add(verifyInFault);
    }  
    
    public Policy getAggregatedServicePolicy(ServiceInfo si) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(si);
            if (null == aggregated) {
                aggregated = p;
            } else {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    public Policy getAggregatedEndpointPolicy(EndpointInfo ei) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(ei);
            if (null == aggregated) {
                aggregated = p;
            } else {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }
    
    public Policy getAggregatedOperationPolicy(BindingOperationInfo boi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(boi);
            if (null == aggregated) {
                aggregated = p;
            } else {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }
    
    public Policy getAggregatedMessagePolicy(BindingMessageInfo bmi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bmi);
            if (null == aggregated) {
                aggregated = p;
            } else {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }
    
    public Policy getAggregatedFaultPolicy(BindingFaultInfo bfi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bfi);
            if (null == aggregated) {
                aggregated = p;
            } else {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }
    
    /**
     * Return a collection of all assertions used in the given policy component,
     * optionally including optional assertions.
     * The policy need not be normalised, so any policy references will have to be resolved.
     * @param pc the policy component
     * @param includeOptional flag indicating if optional assertions should be included
     * @return the assertions
     */
    public Collection<Assertion> getAssertions(PolicyComponent pc, boolean includeOptional) {
        
        Collection<Assertion> assertions = new ArrayList<Assertion>();
        
        if (Constants.TYPE_ASSERTION == pc.getType()) {
            Assertion a = (Assertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add(a);
            }
        } else {       
            addAssertions(pc, includeOptional, assertions);
        }
        return assertions;
    }
    
    void addAssertions(PolicyComponent pc, boolean includeOptional, 
                               Collection<Assertion> assertions) {
       
        if (Constants.TYPE_ASSERTION == pc.getType()) {
            Assertion a = (Assertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add((Assertion)pc);                
            }
            return;
        } 
        
        if (Constants.TYPE_POLICY_REF == pc.getType()) {
            PolicyReference pr = (PolicyReference)pc;
            pc = pr.normalize(registry, false);
        }

        PolicyOperator po = (PolicyOperator)pc;

        List<PolicyComponent> pcs = CastUtils.cast(po.getPolicyComponents(), PolicyComponent.class);
        for (PolicyComponent child : pcs) {
            addAssertions(child, includeOptional, assertions);
        }
    }
    
    /**
     * Return the vocabulary of a policy component, i.e. the set of QNames of
     * the assertions used in the componente, duplicates removed.
     * @param pc the policy component
     * @param includeOptional flag indicating if optional assertions should be included
     * @return the vocabulary
     */
    public Set<QName> getVocabulary(PolicyComponent pc, boolean includeOptional) {
        Collection<Assertion> assertions = getAssertions(pc, includeOptional);
        Set<QName> vocabulary = new HashSet<QName>();
        for (Assertion a : assertions) {
            vocabulary.add(a.getName());
        }
        return vocabulary;
    }
    
    public OutPolicyInfo getClientRequestPolicyInfo(Endpoint e, BindingOperationInfo boi, Conduit c) {
        BindingOperation bo = new BindingOperation(e, boi);
        OutPolicyInfo opi = clientRequestInfo.get(bo);
        if (null == opi) {
            opi = createOutPolicyInfo();
            Assertor assertor = null;
            if (c instanceof Assertor) {
                assertor = (Assertor)c;
            }
            opi.initialise(e, boi, this, assertor, true);
            clientRequestInfo.put(bo, opi);
        }
        return opi;
    }
    
    public void setClientRequestPolicyInfo(Endpoint e, BindingOperationInfo boi, OutPolicyInfo opi) {
        BindingOperation bo = new BindingOperation(e, boi);
        clientRequestInfo.put(bo, opi);
    }
    
    public OutPolicyInfo getServerRequestPolicyInfo(Endpoint e, BindingOperationInfo boi) {
        BindingOperation bo = new BindingOperation(e, boi);
        OutPolicyInfo opi = serverRequestInfo.get(bo);
        if (null == opi) {
            opi = createOutPolicyInfo();
            opi.initialisePolicy(e, boi, this, false);
            serverRequestInfo.put(bo, opi);
        }
        return opi;
    }
    
    public void setServerRequestPolicyInfo(Endpoint e, BindingOperationInfo boi, OutPolicyInfo opi) {
        BindingOperation bo = new BindingOperation(e, boi);
        serverRequestInfo.put(bo, opi);
    }
    
    public OutPolicyInfo getClientResponsePolicyInfo(Endpoint e, BindingOperationInfo boi) {
        BindingOperation bo = new BindingOperation(e, boi);
        OutPolicyInfo opi = clientResponseInfo.get(bo);
        if (null == opi) {
            opi = createOutPolicyInfo();
            opi.initialisePolicy(e, boi, this, true);
            clientResponseInfo.put(bo, opi);
        }
        return opi;
    }
    
    public void setClientResponsePolicyInfo(Endpoint e, BindingOperationInfo boi, OutPolicyInfo opi) {
        BindingOperation bo = new BindingOperation(e, boi);
        clientResponseInfo.put(bo, opi);
    }
    
    public OutPolicyInfo getClientFaultPolicyInfo(Endpoint e, BindingFaultInfo bfi) {
        BindingFault bf = new BindingFault(e, bfi);
        OutPolicyInfo opi = clientFaultInfo.get(bf);
        if (null == opi) {
            opi = createOutPolicyInfo();
            opi.initialisePolicy(e, bfi, this);
            clientFaultInfo.put(bf, opi);
        }
        return opi;
    }
    
    public void setClientFaultPolicyInfo(Endpoint e, BindingFaultInfo bfi, OutPolicyInfo opi) {
        BindingFault bf = new BindingFault(e, bfi);
        clientFaultInfo.put(bf, opi);
    }
    
    public EndpointPolicyInfo getEndpointPolicyInfo(Endpoint e, Conduit conduit) {
        EndpointPolicyInfo epi = endpointInfo.get(e);
        if (null != epi) {
            return epi;
        }
        Assertor assertor = conduit instanceof Assertor ? (Assertor)conduit : null;
        return createEndpointPolicyInfo(e, false, assertor);
    }
   
    public EndpointPolicyInfo getEndpointPolicyInfo(Endpoint e, Destination destination) {
        EndpointPolicyInfo epi = endpointInfo.get(e);
        if (null != epi) {
            return epi;
        }
        Assertor assertor = destination instanceof Assertor ? (Assertor)destination : null;
        return createEndpointPolicyInfo(e, true, assertor);
    }
    
    EndpointPolicyInfo createEndpointPolicyInfo(Endpoint e, boolean isServer, Assertor assertor) {
        EndpointPolicyInfo epi = createEndpointPolicyInfo();
        epi.initialise(e.getEndpointInfo(), isServer, this, assertor);
        endpointInfo.put(e, epi);

        return epi;
    }
    
    public void setEndpointPolicyInfo(Endpoint e, EndpointPolicyInfo epi) {
        endpointInfo.put(e, epi);
    }
    
    
    public OutPolicyInfo getServerResponsePolicyInfo(Endpoint e, BindingOperationInfo boi, 
                                                         Destination d) {
        BindingOperation bo = new BindingOperation(e, boi);
        OutPolicyInfo opi = serverResponseInfo.get(bo);
        if (null == opi) {
            opi = createOutPolicyInfo();
            Assertor assertor = null;
            if (d instanceof Assertor) {
                assertor = (Assertor)d;
            }
            opi.initialise(e, boi, this, assertor, false);
            serverResponseInfo.put(bo, opi);
        }
        return opi;
    }
    
    public void setServerResponsePolicyInfo(Endpoint e, BindingOperationInfo boi, OutPolicyInfo opi) {
        BindingOperation bo = new BindingOperation(e, boi);
        serverResponseInfo.put(bo, opi);
    }
    
    public OutPolicyInfo getServerFaultPolicyInfo(Endpoint e, BindingFaultInfo bfi, 
                                                         Destination d) {
        BindingFault bf = new BindingFault(e, bfi);
        OutPolicyInfo opi = serverFaultInfo.get(bf);
        if (null == opi) {
            opi = createOutPolicyInfo();
            Assertor assertor = null;
            if (d instanceof Assertor) {
                assertor = (Assertor)d;
            }
            opi.initialise(e, bfi, this, assertor);
            serverFaultInfo.put(bf, opi);
        }
        return opi;
    }
    
    public void setServerFaultPolicyInfo(Endpoint e, BindingFaultInfo bfi, OutPolicyInfo opi) {
        BindingFault bf = new BindingFault(e, bfi);
        serverFaultInfo.put(bf, opi);
    }
    

    /**
     * Check if a given list of assertions can potentially be supported by
     * interceptors or by an already installed assertor (a conduit or transport
     * that implements the Assertor interface).
     * 
     * @param alternative the policy alternative
     * @param Assertor the assertor
     * @return true iff the alternative can be supported
     */
    boolean supportsAlternative(List<Assertion> alternative, Assertor assertor) {
        PolicyInterceptorProviderRegistry pipr = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        for (Assertion a : alternative) {
            if (!(a.isOptional() 
                || (null != pipr.get(a.getName())) 
                || (null != assertor && assertor.asserts(a)))) {
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * Class used as key in the client request policy and server response policy maps.
     */
    class BindingOperation {
        Endpoint endpoint;
        BindingOperationInfo boi;
        
        BindingOperation(Endpoint e, BindingOperationInfo b) {
            endpoint = e;
            boi = b;
        }

        @Override
        public int hashCode() {
            return boi.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            BindingOperation other = (BindingOperation)obj;
            return boi.equals(other.boi) && endpoint.equals(other.endpoint);
        }
        
        
        
    }
    
    /**
     * Class used as key in the server fault policy map.
     */
    class BindingFault {
        Endpoint endpoint;
        BindingFaultInfo bfi;
        
        BindingFault(Endpoint e, BindingFaultInfo b) {
            endpoint = e;
            bfi = b;
        }
        
        @Override
        public int hashCode() {
            return bfi.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            BindingFault other = (BindingFault)obj;
            return bfi.equals(other.bfi) && endpoint.equals(other.endpoint);
        }
    }
    
    // for test
    
    OutPolicyInfo createOutPolicyInfo() {
        return new OutPolicyInfo();
    }
    
    EndpointPolicyInfo createEndpointPolicyInfo() {
        return new EndpointPolicyInfo();
    }


}

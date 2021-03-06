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
package org.apache.cxf.jaxrs.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.HTTPConduit;

public class ClientConfiguration implements InterceptorProvider {

    private List<Interceptor<? extends Message>> inInterceptors 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outInterceptors 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outFault 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> inFault 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private ConduitSelector conduitSelector;
    private Bus bus;
    private Map<String, Object> requestContext = new HashMap<String, Object>();
    private Map<String, Object> responseContext = new HashMap<String, Object>();
    
    
    public void setConduitSelector(ConduitSelector cs) {
        this.conduitSelector = cs;
    }
    
    public ConduitSelector getConduitSelector() {
        return conduitSelector;
    }
    
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        return inInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return outInterceptors;
    }

    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inInterceptors = interceptors;
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outInterceptors = interceptors;
    }
    
    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inFault = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outFault = interceptors;
    }
    
    public Conduit getConduit() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.put(MessageObserver.class, new ClientMessageObserver(this));
        exchange.put(Bus.class, bus);
        return getConduitSelector().selectConduit(message);
    }
    
    public HTTPConduit getHttpConduit() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.put(MessageObserver.class, new ClientMessageObserver(this));
        exchange.put(Bus.class, bus);
        Conduit conduit = getConduitSelector().selectConduit(message);
        return conduit instanceof HTTPConduit ? (HTTPConduit)conduit : null;
    }
    
    public Map<String, Object> getResponseContext() {
        return responseContext;
    }
    
    public Map<String, Object> getRequestContext() {
        return requestContext;
    }
}

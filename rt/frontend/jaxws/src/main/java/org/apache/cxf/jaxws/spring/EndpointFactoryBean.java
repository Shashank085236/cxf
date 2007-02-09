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

package org.apache.cxf.jaxws.spring;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.support.AbstractJaxWsServiceFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Creates a JAX-WS Endpoint. Implements InitializingBean to make it easier for Spring
 * users to use.
 */
public class EndpointFactoryBean implements FactoryBean, ApplicationContextAware {
    private String address;
    private Bus bus;
    private Executor executor;
    private AbstractJaxWsServiceFactoryBean serviceFactory;
    private Object implementor;
    private boolean publish = true;
    private EndpointImpl endpoint;
    private ApplicationContext context;
    private String binding;
    private Map<String, Object> properties;
    
    public void setApplicationContext(ApplicationContext c) 
        throws BeansException {
        this.context = c;
    }

    public Object getObject() throws Exception {
        if (endpoint != null) {
            return endpoint;
        }
        
        // Construct Endpoint...
        
        if (bus == null) {
            bus = (Bus) context.getBean("cxf");
            
            if (bus == null) {
                bus = BusFactory.getDefaultBus();
            }
        }

        if (serviceFactory == null) {
            endpoint = new EndpointImpl(bus, implementor, binding);
        } else {
            endpoint = new EndpointImpl(bus, implementor, serviceFactory);
        }
        
        if (executor != null) {
            endpoint.setExecutor(executor);
        }

        if (properties != null) {
            endpoint.setProperties(properties);
        }
        
        if (publish) {
            endpoint.publish(address);
        }
        return endpoint;
    }

    public Class getObjectType() {
        return Endpoint.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Object getImplementor() {
        return implementor;
    }

    public void setImplementor(Object implementor) {
        this.implementor = implementor;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public String getBinding() {
        return binding;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public AbstractJaxWsServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }

    public void setServiceFactory(AbstractJaxWsServiceFactoryBean serviceFactory) {
        this.serviceFactory = serviceFactory;
    }
}

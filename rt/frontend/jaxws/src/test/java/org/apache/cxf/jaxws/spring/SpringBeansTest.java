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

import junit.framework.TestCase;

import org.apache.cxf.binding.soap.SoapBindingInfoFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.service.Hello;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBeansTest extends TestCase {
    public void xtestEndpoints() throws Exception {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/endpoints.xml"});

        Object bean = ctx.getBean("simple");
        assertNotNull(bean);
        
        EndpointImpl ep = (EndpointImpl) bean;
        assertNotNull(ep.getImplementor());
        assertNotNull(ep.getServer());
        
        bean = ctx.getBean("simpleWithAddress");
        assertNotNull(bean);
        
        ep = (EndpointImpl) bean;
        assertNotNull(ep.getImplementor());
        assertEquals("http://localhost:8080/simpleWithAddress", 
                     ep.getServer().getEndpoint().getEndpointInfo().getAddress());
        
        bean = ctx.getBean("inlineImplementor");
        assertNotNull(bean);
        
        ep = (EndpointImpl) bean;
        assertNotNull(ep.getImplementor());
        assertNotNull(ep.getServer());
        
        bean = ctx.getBean("epWithProps");
        assertNotNull(bean);
        
        ep = (EndpointImpl) bean;
        assertEquals("bar", ep.getProperties().get("foo"));
        
        bean = ctx.getBean("classImpl");
        assertNotNull(bean);
        
        ep = (EndpointImpl) bean;
        assertTrue(ep.getImplementor() instanceof Hello);
    }
    
    public void testServers() throws Exception {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/servers.xml"});

        JaxWsServerFactoryBean bean = (JaxWsServerFactoryBean) ctx.getBean("simple");
        assertNotNull(bean);

        bean = (JaxWsServerFactoryBean) ctx.getBean("inlineSoapBinding");
        assertNotNull(bean);
        assertTrue(((SoapBindingInfoFactoryBean) bean.getBindingFactory()).isMtomEnabled());
    }
}

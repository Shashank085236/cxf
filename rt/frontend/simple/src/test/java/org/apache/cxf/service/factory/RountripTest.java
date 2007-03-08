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
package org.apache.cxf.service.factory;

import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.Test;

public class RountripTest extends AbstractSimpleFrontendTest {

    @Test
    public void testServerFactoryBean() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello");
        svrBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        svrBean.setServiceBean(new HelloServiceImpl());
        svrBean.setBus(getBus());
        
        svrBean.create();
        
        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost/Hello");
        clientBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        clientBean.setServiceClass(HelloService.class);
        clientBean.setBus(getBus());

        HelloService client = (HelloService) proxyFactory.create();
        
        ClientImpl c = (ClientImpl) clientBean.getClient();
        c.getOutInterceptors().add(new LoggingOutInterceptor());
        c.getInInterceptors().add(new LoggingInInterceptor());
        
        assertEquals("hello", client.sayHello());
    }
}

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
package org.apache.cxf.aegis.inheritance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.inheritance.ws1.WS1;
import org.apache.cxf.aegis.inheritance.ws1.WS1ExtendedException;
import org.apache.cxf.aegis.inheritance.ws1.impl.WS1Impl;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.junit.Test;

public class ExceptionInheritanceTest extends AbstractAegisTest {
    private WS1 client;
    private Map<String, Object> props;
    
    public void setUp() throws Exception {
        super.setUp();

        props = new HashMap<String, Object>();
        props.put(AegisDatabinding.WRITE_XSI_TYPE_KEY, "true");

        List<String> l = new ArrayList<String>();
        l.add(SimpleBean.class.getName());
        l.add(WS1ExtendedException.class.getName());

        props.put(AegisDatabinding.OVERRIDE_TYPES_KEY, l);

        ClientProxyFactoryBean pf = new ClientProxyFactoryBean();
        setupAegis(pf.getClientFactoryBean());
        pf.setServiceClass(WS1.class);
        pf.getServiceFactory().setProperties(props);
        pf.setAddress("local://WS1");
        pf.setProperties(props);
        
        client = (WS1) pf.create();

        Server server = createService(WS1.class, new WS1Impl(), "WS1", null);
        new LoggingFeature().initialize(server, null);
        server.getEndpoint().getService().setInvoker(new BeanInvoker(new WS1Impl()));
    }

    @Override
    protected ServerFactoryBean createServiceFactory(Class serviceClass, 
                                                     Object serviceBean, String address, QName name) {
        ServerFactoryBean sf = super.createServiceFactory(serviceClass, serviceBean, address, name);
        sf.getServiceFactory().setProperties(props);
        return sf;
    }

    @Test
    public void testClient() throws Exception {
        try {
            client.throwException(true);
            fail("No exception was thrown!");
        } catch (WS1ExtendedException ex) {
            Object sb = ex.getSimpleBean();
            assertTrue(sb instanceof SimpleBean);
        }
    }

}

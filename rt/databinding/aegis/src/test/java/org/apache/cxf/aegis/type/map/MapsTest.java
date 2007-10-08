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

package org.apache.cxf.aegis.type.map;

import java.io.StringWriter;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;

//import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.map.fortest.MapTest;
import org.apache.cxf.aegis.type.map.fortest.MapTestImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 */
public class MapsTest extends AbstractAegisTest {
    
    private MapTest clientInterface;
    private Server server;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(MapTest.class);
        sf.setServiceBean(new MapTestImpl());
        sf.setAddress("local://MapTest");
        setupAegis(sf);
        server = sf.create();
        //        server.getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
//        server.getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        server.start();
        
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setAddress("local://MapTest");
        proxyFac.setServiceClass(MapTest.class);
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        clientInterface = (MapTest)proxyFac.create(); 
    }
    
    @Ignore
    @Test
    /**
     * Until some issues in CXF-1051 are resolved, it's not clear what to test in here. 
     * 
     */
    public void testMapWsdl() throws WSDLException {
        @SuppressWarnings("unused")
        //Document wsdl = getWSDLDocument("MapTestService");
        Definition wsdlDef = getWSDLDefinition("MapTestService");
        StringWriter sink = new StringWriter();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(wsdlDef, sink);
        System.out.println(sink.toString());
    }
    
    @Test
    public void testInvocations() throws Exception {
        Map<Long, String> lts = clientInterface.getMapLongToString();
        assertEquals("twenty-seven", lts.get(Long.valueOf(27)));
    }
}

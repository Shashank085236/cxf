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

package org.apache.cxf.systest.aegis.mtom;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.systest.aegis.mtom.fortest.DataHandlerBean;
import org.apache.cxf.systest.aegis.mtom.fortest.MtomTestImpl;
import org.apache.cxf.test.TestUtilities;
import org.junit.Test;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * 
 */
public class MtomTest extends AbstractDependencyInjectionSpringContextTests {
    
    private org.apache.cxf.systest.aegis.mtom.fortest.MtomTestImpl impl;
    private org.apache.cxf.systest.aegis.mtom.fortest.MtomTest client;
    private TestUtilities testUtilities;
    
    public MtomTest() {
        testUtilities = new TestUtilities(getClass());
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:mtomTestBeans.xml"};
    }
    
    private void setupForTest(boolean enableClientMTOM) throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        aegisBinding.setMtomEnabled(enableClientMTOM);
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setDataBinding(aegisBinding);
        proxyFac.setAddress("http://localhost:9002/mtom");
        proxyFac.setServiceClass(org.apache.cxf.systest.aegis.mtom.fortest.MtomTest.class);
        Map<String, Object> props = new HashMap<String, Object>();
        if (enableClientMTOM) {
            props.put("mtom-enabled", Boolean.TRUE);
        }
        proxyFac.setProperties(props);
        proxyFac.getOutInterceptors().add(new LoggingOutInterceptor());
        client = (org.apache.cxf.systest.aegis.mtom.fortest.MtomTest)proxyFac.create();
        impl = (MtomTestImpl)applicationContext.getBean("mtomImpl");
    }
    
    @Test 
    public void testAcceptDataHandler() throws Exception {
        setupForTest(true);
        DataHandlerBean dhBean = new DataHandlerBean();
        dhBean.setName("some name");
        // some day, we might need this to be higher than some threshold.
        String someData = "This is the cereal shot from guns.";
        DataHandler dataHandler = new DataHandler(someData, "text/plain;charset=utf-8");
        dhBean.setDataHandler(dataHandler);
        client.acceptDataHandler(dhBean);
        DataHandlerBean accepted = impl.getLastDhBean();
        assertNotNull(accepted);
        String data = (String) accepted.getDataHandler().getContent();
        assertNotNull(data);
        assertEquals("This is the cereal shot from guns.", data);
    }

    @Test 
    public void testAcceptDataHandlerNoMTOM() throws Exception {
        setupForTest(false);
        DataHandlerBean dhBean = new DataHandlerBean();
        dhBean.setName("some name");
        // some day, we might need this to be higher than some threshold.
        String someData = "This is the cereal shot from guns.";
        DataHandler dataHandler = new DataHandler(someData, "text/plain;charset=utf-8");
        dhBean.setDataHandler(dataHandler);
        client.acceptDataHandler(dhBean);
        DataHandlerBean accepted = impl.getLastDhBean();
        assertNotNull(accepted);
        Object data = accepted.getDataHandler().getContent();
        assertNotNull(data);
        // we would like to see the right content type. However, without xmime:contentType, we cannot.
    }

    // we aren't ready for this one ...
    @Test
    public void testMtomSchema() throws Exception {
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        testUtilities.addNamespace("xmime", "http://www.w3.org/2005/05/xmlmime");
        Server s = testUtilities.
            getServerForService(new QName("http://fortest.mtom.aegis.systest.cxf.apache.org/", 
                                          "MtomTest"));
        Document wsdl = testUtilities.getWSDLDocument(s); 
        assertNotNull(wsdl);
        
        /*
        testUtilities.assertValid("//xsd:complexType[@name='inputDhBean']/xsd:sequence/"
                                  + "xsd:element[@name='dataHandler']/"
                                  + "@xmime:expectedContentType/text()", 
                                  wsdl);
                                  */
    }


}

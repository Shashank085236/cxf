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

package org.apache.cxf.systest.jaxws;

import java.net.URL;

import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import junit.framework.TestCase;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.CatalogWSDLLocator;
import org.apache.cxf.catalog.OASISCatalogManager;

import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.apache.ws.commons.schema.XmlSchemaException;

import org.apache.xml.resolver.Catalog;

public class OASISCatalogTest extends TestCase {
        
    private final QName serviceName = 
        new QName("http://apache.org/hello_world/services",
                  "SOAPService");    

    private final QName portName = 
        new QName("http://apache.org/hello_world/services",
                  "SoapPort");

    public void testClientWithDefaultCatalog() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        assertNotNull(greeter);
    }

    public void testClientWithoutCatalog() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);
        
        // set Catalog on the Bus
        Bus bus = BusFactory.getDefaultBus();
        OASISCatalogManager catalog = new OASISCatalogManager();
        bus.setExtension(catalog.getCatalog(), Catalog.class);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        try {
            service.getPort(portName, Greeter.class);
            fail("Test did not fail as expected");
        } catch (XmlSchemaException e) {
            // ignore
        }

        // update catalog dynamically now
        URL jaxwscatalog = 
            getClass().getResource("/META-INF/jax-ws-catalog.xml");
        assertNotNull(jaxwscatalog);

        catalog.loadCatalog(jaxwscatalog);

        Greeter greeter = service.getPort(portName, Greeter.class);
        assertNotNull(greeter);
    }

    public void testWSDLLocatorWithDefaultCatalog() throws Exception {
        URL wsdl = 
            getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);

        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
       
        CatalogWSDLLocator wsdlLocator =
            new CatalogWSDLLocator(wsdl.toString(),
                                   OASISCatalogManager.getCatalog(null));
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        wsdlReader.readWSDL(wsdlLocator);
    }

    public void testWSDLLocatorWithoutCatalog() throws Exception {
        URL wsdl = 
            getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);

        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
       
        OASISCatalogManager catalog = new OASISCatalogManager();
        CatalogWSDLLocator wsdlLocator =
            new CatalogWSDLLocator(wsdl.toString(), catalog.getCatalog());
        try {
            wsdlReader.readWSDL(wsdlLocator);
            fail("Test did not fail as expected");
        } catch (WSDLException e) {
            // ignore
        }
    }

}

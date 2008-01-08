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
package org.apache.yoko.bindings.corba;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;

import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;

import org.apache.schemas.yoko.idl.polynestedobjref.Foo;
import org.apache.schemas.yoko.idl.polynestedobjref.FooFactory;
import org.apache.schemas.yoko.idl.polynestedobjref.FooFactoryCORBAService;
import org.apache.schemas.yoko.idl.polynestedobjref.FooRefStruct;
import org.apache.schemas.yoko.idl.polynestedobjref.FooRefUnion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class CorbaPolyNestedObjReferenceTest extends Assert {

    private static final QName OBJECT_PORT_NAME = 
        new QName("http://schemas.apache.org/yoko/idl/PolyNestedObjRef", "FooCORBAPort"); 
    
    private static final QName OBJECT_PORT_TYPE = 
        new QName("http://schemas.apache.org/yoko/idl/PolyNestedObjRef", "Foo"); 
    
    private static final QName OBJECT_SERVICE_NAME = 
        new QName("http://schemas.apache.org/yoko/idl/PolyNestedObjRef", "FooCORBAService"); 
    
    private static final QName INTERFACE_PORT_NAME = 
        new QName("http://schemas.apache.org/yoko/idl/PolyNestedObjRef", "FooFactoryCORBAPort"); 
    
    private static final QName INTERFACE_SERVICE_NAME = 
        new QName("http://schemas.apache.org/yoko/idl/PolyNestedObjRef", "FooFactoryCORBAService"); 
    
    private static final String WSDL_LOCATION = "/wsdl/PolyNestedObjRef.wsdl";
    private static final int MAX_WAIT_COUNT = 15;
    
    private static TestServer server;
    private FooFactory client;
    private URL wsdlUrl;

    @Before
    public void setUp() throws Exception {
        if (server == null) {
            server = new TestServer();
            server.start();
        }

        int waitCount = 0;
        // Wait for the server to start if it hasn't already
        while (waitCount < MAX_WAIT_COUNT && !server.isReady()) {
            try {
                Thread.sleep(1000);
                waitCount++;
            } catch (Exception ex) {
                // Consume
            }
        }

        if (!server.isReady()) {
            throw new Exception("Server failed to start in a timely fashion");
        }

        // Now initialize the client-side
        if (client == null) {
            try {
                wsdlUrl = FooFactory.class.getResource(WSDL_LOCATION);
            } catch (Exception ex) {
                throw new Exception("Unable to resolve WSDL location");
            }

            FooFactoryCORBAService service = 
                new FooFactoryCORBAService(wsdlUrl, INTERFACE_SERVICE_NAME);
            client = service.getPort(INTERFACE_PORT_NAME, FooFactory.class);
        }

        if (client == null) {
            throw new Exception("Unable to create client");
        }
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        server.interrupt();

        try {
            // Sleep for 3 seconds waiting for the server to shut down
            Thread.sleep(3000);
        } catch (Exception ex) {
            // Move on to check if the server is down
        }

        if (server.isAlive()) {
            throw new Exception("Did not terminate test server!");
        }
    }

    @Test
    public void testCreateFooRefInStruct() {       
        FooRefStruct ref = client.createFooRefInStruct("FooRefInStruct");
        //EndpointReferenceType epr = createObjectFromEndpointReferenceType(ref.getRef());
        EndpointReferenceType epr = ref.getRef();

        assertNotNull(epr.getAddress().getValue());

        QName interfaceName = EndpointReferenceUtils.getInterfaceName(epr);
        assertTrue(interfaceName.equals(OBJECT_PORT_TYPE));

        String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(epr);
        assertTrue(wsdlLocation.equals(wsdlUrl.toString()));

        QName serviceName = EndpointReferenceUtils.getServiceName(epr);
        assertTrue(serviceName.equals(OBJECT_SERVICE_NAME));

        String portName = EndpointReferenceUtils.getPortName(epr);
        assertTrue(portName.equals(OBJECT_PORT_NAME.getLocalPart()));

        //Test for null for EPR.
        ref = client.createFooRefInStruct("");
        assertNull("Null EPR expected", ref.getRef());
    }

    @Test
    public void testCreateFooRefInUnion() {
        FooRefUnion ref = client.createFooRefInUnion();
        EndpointReferenceType epr = ref.getU12();
        assertNotNull(epr.getAddress().getValue());

        QName interfaceName = EndpointReferenceUtils.getInterfaceName(epr);
        assertTrue(interfaceName.equals(OBJECT_PORT_TYPE));

        String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(epr);
        assertTrue(wsdlLocation.equals(wsdlUrl.toString()));

        QName serviceName = EndpointReferenceUtils.getServiceName(epr);
        assertTrue(serviceName.equals(OBJECT_SERVICE_NAME));

        String portName = EndpointReferenceUtils.getPortName(epr);
        assertTrue(portName.equals(OBJECT_PORT_NAME.getLocalPart()));
    }
    
    @Test
    public void testInferredObjectReturn() {
        
        EndpointReferenceType ref = client.testInferredObjectReturn();

        assertNotNull(ref.getAddress().getValue());

        QName interfaceName = EndpointReferenceUtils.getInterfaceName(ref);
        assertNotNull(interfaceName);

        String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(ref);
        assertNotNull(wsdlLocation);

        QName serviceName = EndpointReferenceUtils.getServiceName(ref);
        assertNotNull(serviceName);

        String portName = EndpointReferenceUtils.getPortName(ref);
        assertNotNull(portName);
    }

    @Test
    public void testNestedInferredObjectParam() {
        FooRefStruct ref = client.createFooRefInStruct("FooRefInStruct");
        FooRefStruct ref2 = client.createFooRefInStruct("FooRefInStruct");        
        
        Holder<FooRefStruct> ref3 = new Holder<FooRefStruct>(ref2);
        Holder<FooRefStruct> ref4 = new Holder<FooRefStruct>();
        
        FooRefStruct refReturn = client.testNestedInferredObjectParam(ref, ref3, ref4);
        EndpointReferenceType epr = refReturn.getRef();

        assertNotNull(epr.getAddress().getValue());

        QName interfaceName = EndpointReferenceUtils.getInterfaceName(epr);
        assertTrue(interfaceName.equals(OBJECT_PORT_TYPE));

        String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(epr);
        assertTrue(wsdlLocation.equals(wsdlUrl.toString()));

        QName serviceName = EndpointReferenceUtils.getServiceName(epr);
        assertTrue(serviceName.equals(OBJECT_SERVICE_NAME));

        String portName = EndpointReferenceUtils.getPortName(epr);
        assertTrue(portName.equals(OBJECT_PORT_NAME.getLocalPart()));
    }
    
    // Helper methods that can be used throughout the test
    public EndpointReferenceType createEndpointReferenceType(String name, boolean serverSide) {
        String corbaAddress = null;
        // The server and client bindings need to be on two different ORBs since they can't
        // share.  So we need to make sure that the port numbers are different.
        if (serverSide) {
            corbaAddress = "corbaloc::localhost:50000/Server" + name;
        } else {
            corbaAddress = "corbaloc::localhost:51000/Client" + name;
        }

        Object testObj = new FooImpl();
        Endpoint ep = Endpoint.create("http://schemas.apache.org/yoko/bindings/corba", testObj);
        String wsdlLocation = getClass().getResource("/wsdl/PolyNestedObjRef.wsdl").toString();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("javax.xml.ws.wsdl.description", wsdlLocation);
        ep.setProperties(props);
        ep.publish(corbaAddress);

        EndpointReferenceType ref =
            EndpointReferenceUtils.getEndpointReference(wsdlUrl,
                                                        OBJECT_SERVICE_NAME,
                                                        OBJECT_PORT_NAME.getLocalPart());
        EndpointReferenceUtils.setInterfaceName(ref, OBJECT_PORT_TYPE);
        
        // edell.
        //get the real IOR rather than the corbaloc address information
        corbaAddress = resolveAddressFromEndpoint(corbaAddress, ep);
        EndpointReferenceUtils.setAddress(ref, corbaAddress);

        return ref;
    }   
    
    
    private String resolveAddressFromEndpoint(String corbaAddress, Endpoint ep) {
        String addr = corbaAddress;
        EndpointImpl epImpl = (EndpointImpl)ep;
        addr = epImpl.getServer().getDestination().getAddress().getAddress().getValue();
        return addr;
    }
    
    public Foo createObjectFromEndpointReferenceType(EndpointReferenceType epr) throws Exception {
            WSDLManager manager = null;
            manager = new WSDLManagerImpl();

            QName interfaceName = EndpointReferenceUtils.getInterfaceName(epr);
            String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(epr);
            QName serviceName = EndpointReferenceUtils.getServiceName(epr);
            String portName = EndpointReferenceUtils.getPortName(epr);

            QName port = new QName(serviceName.getNamespaceURI(), portName);

            StringBuffer seiName = new StringBuffer();
            seiName.append("org.apache.schemas.yoko.idl.polynestedobjref.");
            seiName.append(JAXBUtils.nameToIdentifier(interfaceName.getLocalPart(),
                           JAXBUtils.IdentifierType.INTERFACE));

            Class<?> sei = null;
            sei = Class.forName(seiName.toString(), true, manager.getClass().getClassLoader());

            Service service = Service.create(wsdlUrl, serviceName);
            Foo testObj = (Foo)service.getPort(port, sei);

            Map<String, Object> requestContext = ((BindingProvider)testObj).getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, epr.getAddress().getValue());

            return testObj;
    }
    
    
    // A small test server for the test case to interact with
    public class TestServer extends Thread {
        private boolean serverReady;

        public TestServer() {
            serverReady = false;
        }

        public void run() {
            Object implementor = new FooFactoryImpl();
            String address = "corbaloc::localhost:50000/PolyNestedObjRefTest";
            Endpoint ep = Endpoint.create("http://schemas.apache.org/yoko/bindings/corba", implementor);
            String wsdlLocation = getClass().getResource("/wsdl/PolyNestedObjRef.wsdl").toString();
            Map<String, Object> props = new HashMap<String, Object>();      
            props.put("javax.xml.ws.wsdl.description", wsdlLocation);
            ep.setProperties(props);
            ep.publish(address);

            serverReady = true;
        }

        public boolean isReady() {
            return serverReady;
        }
    }

    // A minimal FooFactory implementation to test object references
    @WebService(portName = "FooFactoryCORBAPort",
                serviceName = "FooFactoryCORBAService",
                targetNamespace = "http://schemas.apache.org/yoko/idl/PolyNestedObjRef",
                endpointInterface = "org.apache.schemas.yoko.idl.polynestedobjref.FooFactory")
    public class FooFactoryImpl implements FooFactory {

        // TODO: Provide methods
        public FooRefStruct createFooRefInStruct(String request) {
            FooRefStruct ref = new FooRefStruct();
            if (!request.equals("")) {
                ref.setName(request);
                EndpointReferenceType epr = createEndpointReferenceType("StructRef", true);
                ref.setRef(epr);
            } else {
                ref.setName("error!!!");
            }
            return ref;
        }

        public FooRefUnion createFooRefInUnion() {
            FooRefUnion ref = new FooRefUnion();
            EndpointReferenceType epr = createEndpointReferenceType("UnionRef", true);
            ref.setU12(epr);
            return ref;
        }
        
        public  EndpointReferenceType createFoo() {            
            return createEndpointReferenceType("FooRef", true);
            
        }
        
        
        public EndpointReferenceType testInferredObjectReturn() {
            return createEndpointReferenceType("InferredObjectReturn", true);
        }
        
        public FooRefStruct testNestedInferredObjectParam(FooRefStruct strt,                                                                                                                   
                                                          javax.xml.ws.Holder<FooRefStruct> strt2,
                                                          javax.xml.ws.Holder<FooRefStruct> strt3) {            
            //strt3.value = strt2.value;
            //strt2.value = strt;            
            strt3.value = strt;
            strt2.value = strt;
            return strt;
        }
    }
    
    // A minimal Foo implementation to test object references
    @javax.jws.WebService(portName = "FooCORBAPort",
                          serviceName = "FooCORBAService",
                          targetNamespace = "http://schemas.apache.org/yoko/idl/PolyNestedObjRef",
                          endpointInterface = "org.apache.schemas.yoko.idl.polynestedobjref.Foo")
    public class FooImpl implements Foo {

        public void doBar() {
        }
    }
}

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

package org.apache.cxf.jaxws;



import java.lang.reflect.UndeclaredThrowableException;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cxf.systest.common.ClientServerSetupBase;
import org.apache.cxf.systest.common.ClientServerTestBase;
import org.apache.cxf.systest.common.TestServerBase;
import org.apache.header_test.SOAPHeaderService;
import org.apache.header_test.TestHeader;
import org.apache.header_test.TestHeaderImpl;
import org.apache.header_test.types.TestHeader1;
import org.apache.header_test.types.TestHeader1Response;
import org.apache.header_test.types.TestHeader2;
import org.apache.header_test.types.TestHeader2Response;
import org.apache.header_test.types.TestHeader3;
import org.apache.header_test.types.TestHeader3Response;
import org.apache.header_test.types.TestHeader5;


public class HeaderClientServerTest extends ClientServerTestBase {

    private final QName serviceName = new QName("http://apache.org/header_test",
                                                "SOAPHeaderService");    
    private final QName portName = new QName("http://apache.org/header_test",
                                             "SoapHeaderPort");

    private TestHeader proxy;
    
    
    public static class MyServer extends TestServerBase {

        protected void run()  {
            Object implementor = new TestHeaderImpl();
            String address = "http://localhost:9104/SoapHeaderContext/SoapHeaderPort";
            Endpoint.publish(address, implementor);
            
        }
        

        public static void main(String[] args) {
            try { 
                MyServer s = new MyServer(); 
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally { 
                System.out.println("done!");
            }
        }
    }
    
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(HeaderClientServerTest.class);
        return new ClientServerSetupBase(suite) {
            public void startServers() throws Exception {
                assertTrue("server did not launch correctly", launchServer(MyServer.class));
            }
        };
                       
    }  

    
    
     
    public void testInHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        proxy = service.getPort(portName, TestHeader.class);
        try {
            TestHeader1 val = new TestHeader1();
            for (int idx = 0; idx < 2; idx++) {
                TestHeader1Response returnVal = proxy.testHeader1(val, val);
                assertNotNull(returnVal);
                assertEquals(TestHeader1.class.getSimpleName(), returnVal.getResponseType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    } 

    public void testOutHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        proxy = service.getPort(portName, TestHeader.class);
        try {
            TestHeader2 in = new TestHeader2();
            String val = new String(TestHeader2Response.class.getSimpleName());
            Holder<TestHeader2Response> out = new Holder<TestHeader2Response>();
            Holder<TestHeader2Response> outHeader = new Holder<TestHeader2Response>();
            for (int idx = 0; idx < 2; idx++) {
                val += idx;                
                in.setRequestType(val);
                proxy.testHeader2(in, out, outHeader);
                
                assertEquals(val, out.value.getResponseType());
                assertEquals(val, outHeader.value.getResponseType());
            }
        } catch (UndeclaredThrowableException ex) {
            ex.printStackTrace();
            throw (Exception)ex.getCause();
        } 
    } 

    public void testInOutHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        proxy = service.getPort(portName, TestHeader.class);
        
        try {
            TestHeader3 in = new TestHeader3();
            String val = new String(TestHeader3.class.getSimpleName());
            Holder<TestHeader3> inoutHeader = new Holder<TestHeader3>();
            for (int idx = 0; idx < 2; idx++) {
                val += idx;                
                in.setRequestType(val);
                inoutHeader.value = new TestHeader3();
                TestHeader3Response returnVal = proxy.testHeader3(in, inoutHeader);
                //inoutHeader copied to return
                //in copied to inoutHeader
                assertNotNull(returnVal);
                assertNull(returnVal.getResponseType());
                assertEquals(val, inoutHeader.value.getRequestType());
                
                in.setRequestType(null);
                inoutHeader.value.setRequestType(val);
                returnVal = proxy.testHeader3(in, inoutHeader);
                assertNotNull(returnVal);
                assertEquals(val, returnVal.getResponseType());
                assertNull(inoutHeader.value.getRequestType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } 
    }

    public void testReturnHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        proxy = service.getPort(portName, TestHeader.class);
        try {
            TestHeader5 in = new TestHeader5();
            String val = new String(TestHeader5.class.getSimpleName());
            for (int idx = 0; idx < 2; idx++) {
                val += idx;                
                in.setRequestType(val);
                TestHeader5 returnVal = proxy.testHeader5(in);

                //in copied to return                
                assertNotNull(returnVal);
                assertEquals(val, returnVal.getRequestType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } 
    } 
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(HeaderClientServerTest.class);
    }
}

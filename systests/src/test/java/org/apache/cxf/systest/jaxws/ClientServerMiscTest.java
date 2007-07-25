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

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.anonymous_complex_type.AnonymousComplexType;
import org.apache.cxf.anonymous_complex_type.AnonymousComplexTypeService;
import org.apache.cxf.anonymous_complex_type.RefSplitName;
import org.apache.cxf.anonymous_complex_type.RefSplitNameResponse;
import org.apache.cxf.anonymous_complex_type.SplitName;
import org.apache.cxf.anonymous_complex_type.SplitNameResponse.Names;
import org.apache.cxf.jaxb_element_test.JaxbElementTest;
import org.apache.cxf.jaxb_element_test.JaxbElementTest_Service;
import org.apache.cxf.ordered_param_holder.ComplexStruct;
import org.apache.cxf.ordered_param_holder.OrderedParamHolder;
import org.apache.cxf.ordered_param_holder.OrderedParamHolder_Service;
import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.Foo;
import org.apache.cxf.tests.inherit.Inherit;
import org.apache.cxf.tests.inherit.InheritService;
import org.apache.cxf.tests.inherit.objects.SubTypeA;
import org.apache.cxf.tests.inherit.objects.SubTypeB;
import org.apache.cxf.tests.inherit.types.ObjectInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerMiscTest extends AbstractBusClientServerTestBase {


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(ServerMisc.class));
    }

    @Test
    public void testAnonymousComplexType() throws Exception {

        AnonymousComplexTypeService actService = new AnonymousComplexTypeService();
        assertNotNull(actService);
        QName portName = new QName("http://cxf.apache.org/anonymous_complex_type/",
            "anonymous_complex_typeSOAP");
        AnonymousComplexType act = actService.getPort(portName, AnonymousComplexType.class);

        try {
            Names reply = act.splitName("Tom Li");
            assertNotNull("no response received from service", reply);
            assertEquals("Tom", reply.getFirst());
            assertEquals("Li", reply.getSecond());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testRefAnonymousComplexType() throws Exception {

        AnonymousComplexTypeService actService = new AnonymousComplexTypeService();
        assertNotNull(actService);
        QName portName = new QName("http://cxf.apache.org/anonymous_complex_type/",
            "anonymous_complex_typeSOAP");
        AnonymousComplexType act = actService.getPort(portName, AnonymousComplexType.class);

        try {
            SplitName name = new SplitName();
            name.setName("Tom Li");
            RefSplitName refName = new RefSplitName();
            refName.setSplitName(name);
            RefSplitNameResponse reply = act.refSplitName(refName);
            assertNotNull("no response received from service", reply);
            assertEquals("Tom", reply.getSplitNameResponse().getNames().getFirst());
            assertEquals("Li", reply.getSplitNameResponse().getNames().getSecond());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testMinOccursAndNillableJAXBElement() throws Exception {

        JaxbElementTest_Service service = new JaxbElementTest_Service();
        assertNotNull(service);
        JaxbElementTest port = service.getPort(JaxbElementTest.class);

        try {

            String response = port.newOperation("hello");
            assertNotNull(response);
            assertEquals("in=hello", response);

            response = port.newOperation(null);
            assertNotNull(response);
            assertEquals("in=null", response);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }
    
    @Test
    public void testOrderedParamHolder() throws Exception {
        OrderedParamHolder_Service service = new OrderedParamHolder_Service();
        OrderedParamHolder port = service.getOrderedParamHolderSOAP();
        
        try {
            Holder<ComplexStruct> part3 = new Holder<ComplexStruct>();
            part3.value = new ComplexStruct();
            part3.value.setElem1("elem1");
            part3.value.setElem2("elem2");
            part3.value.setElem3(0);
            Holder<Integer> part2 = new Holder<Integer>();
            part2.value = 0;
            Holder<String> part1 = new Holder<String>();
            part1.value = "part1";
            
            port.orderedParamHolder(part3, part2, part1);
            
            assertNotNull(part3.value);
            assertEquals("check value", "return elem1", part3.value.getElem1());
            assertEquals("check value", "return elem2", part3.value.getElem2());
            assertEquals("check value", 1, part3.value.getElem3());
            assertNotNull(part2.value);
            assertEquals("check value", 1, part2.value.intValue());
            assertNotNull(part1.value);
            assertEquals("check value", "return part1", part1.value);
            
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }
    
    @Test
    public void testStringListOutDocLitNoWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService", 
                                   "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService", 
                                   "DocLitWrappedCodeFirstService");
        
        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.DOCLIT_CODEFIRST_URL);
        DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                             DocLitWrappedCodeFirstService.class);
        runDocLitTest(port);
    }

    @Test
    public void testStringListOutDocLitWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService", 
                                   "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService", 
                                   "DocLitWrappedCodeFirstService");
        
        Service service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl"),
                                         servName);
        DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                             DocLitWrappedCodeFirstService.class);
        runDocLitTest(port);
    }
    
    private void runDocLitTest(DocLitWrappedCodeFirstService port) throws Exception {
        List<String> rev = new ArrayList<String>(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA));
        Collections.reverse(rev);
        
        String s;
        
        String arrayOut[] = port.arrayOutput();
        assertNotNull(arrayOut);
        assertEquals(3, arrayOut.length);
        for (int x = 0; x < 3; x++) {
            assertEquals(DocLitWrappedCodeFirstServiceImpl.DATA[x], arrayOut[x]);
        }
        
        List<String> listOut = port.listOutput();
        assertNotNull(listOut);
        assertEquals(3, listOut.size());
        for (int x = 0; x < 3; x++) {
            assertEquals(DocLitWrappedCodeFirstServiceImpl.DATA[x], listOut.get(x));
        }
        
        s = port.arrayInput(DocLitWrappedCodeFirstServiceImpl.DATA);
        assertEquals("string1string2string3", s);
        s = port.listInput(java.util.Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA));
        assertEquals("string1string2string3", s);
        
        s = port.multiListInput(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA),
                                rev,
                                "Hello", 24);
        assertEquals("string1string2string3string3string2string1Hello24", s);
        
        
        s = port.listInput(new ArrayList<String>());
        assertEquals("", s);

        s = port.listInput(null);
        assertEquals("", s);

        s = port.multiListInput(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA),
                                        rev,
                                        null, 24);
        assertEquals("string1string2string3string3string2string1<null>24", s);
        
        Holder<String> a = new Holder<String>();
        Holder<String> b = new Holder<String>("Hello");
        Holder<String> c = new Holder<String>();
        Holder<String> d = new Holder<String>(" ");
        Holder<String> e = new Holder<String>("world!");
        Holder<String> f = new Holder<String>();
        Holder<String> g = new Holder<String>();
        s = port.multiInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);
        
        List<Foo> foos = port.listObjectOutput();
        assertEquals(2, foos.size());
        assertEquals("a", foos.get(0).getName());
        assertEquals("b", foos.get(1).getName());
    }
    @Test
    public void testRpcLitNoWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService", 
                                   "RpcLitCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService", 
                                   "RpcLitCodeFirstService");
        
        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.RPCLIT_CODEFIRST_URL);
        RpcLitCodeFirstService port = service.getPort(portName,
                                                      RpcLitCodeFirstService.class);
        runRpcLitTest(port);
    }
    
    
    @Test
    public void testRpcLitWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService", 
            "RpcLitCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService", 
            "RpcLitCodeFirstService");
        
        Service service = Service.create(new URL(ServerMisc.RPCLIT_CODEFIRST_URL + "?wsdl"),
                                         servName);
        RpcLitCodeFirstService port = service.getPort(portName,
                                                      RpcLitCodeFirstService.class);
        runRpcLitTest(port);
    }

    private void runRpcLitTest(RpcLitCodeFirstService port) throws Exception {
        List<String> rev = new ArrayList<String>(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA));
        Collections.reverse(rev);
        
        String s;
        
        String arrayOut[] = port.arrayOutput();
        assertNotNull(arrayOut);
        assertEquals(3, arrayOut.length);
        for (int x = 0; x < 3; x++) {
            assertEquals(RpcLitCodeFirstServiceImpl.DATA[x], arrayOut[x]);
        }
        
        List<String> listOut = port.listOutput();
        assertNotNull(listOut);
        assertEquals(3, listOut.size());
        for (int x = 0; x < 3; x++) {
            assertEquals(RpcLitCodeFirstServiceImpl.DATA[x], listOut.get(x));
        }
        
        s = port.arrayInput(RpcLitCodeFirstServiceImpl.DATA);
        assertEquals("string1string2string3", s);
        s = port.listInput(java.util.Arrays.asList(RpcLitCodeFirstServiceImpl.DATA));
        assertEquals("string1string2string3", s);
        
        s = port.multiListInput(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA),
                                rev,
                                "Hello", 24);
        assertEquals("string1string2string3string3string2string1Hello24", s);
        
        
        s = port.listInput(new ArrayList<String>());
        assertEquals("", s);

        try {
            s = port.listInput(null);
            fail("RPC/Lit parts cannot be null");
        } catch (SOAPFaultException ex) {
            //ignore, expected
        }

        try {
            s = port.multiListInput(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA),
                                            rev,
                                            null, 24);
            fail("RPC/Lit parts cannot be null");
        } catch (SOAPFaultException ex) {
            //ignore, expected
        }
        
        Holder<String> a = new Holder<String>();
        Holder<String> b = new Holder<String>("Hello");
        Holder<String> c = new Holder<String>();
        Holder<String> d = new Holder<String>(" ");
        Holder<String> e = new Holder<String>("world!");
        Holder<String> f = new Holder<String>();
        Holder<String> g = new Holder<String>();
        s = port.multiInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);
        
        a = new Holder<String>();
        b = new Holder<String>("Hello");
        c = new Holder<String>();
        d = new Holder<String>(" ");
        e = new Holder<String>("world!");
        f = new Holder<String>();
        g = new Holder<String>();
        s = port.multiHeaderInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);        
    }
      
    @Test
    public void testInheritedTypesInOtherPackage() throws Exception {
        InheritService serv = new InheritService();
        Inherit port = serv.getInheritPort();
        ObjectInfo obj = port.getObject(0);
        assertNotNull(obj);
        assertNotNull(obj.getBaseObject());
        assertEquals("A", obj.getBaseObject().getName());
        assertTrue(obj.getBaseObject() instanceof SubTypeA);
        
        obj = port.getObject(1);
        assertNotNull(obj);
        assertNotNull(obj.getBaseObject());
        assertEquals("B", obj.getBaseObject().getName());
        assertTrue(obj.getBaseObject() instanceof SubTypeB);
        
    }
}

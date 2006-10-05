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

package org.apache.cxf.binding.xml.interceptor;

import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;
import org.apache.hello_world_xml_http.wrapped.types.GreetMe;

public class XMLMessageInInterceptorTest extends TestBase {

    XMLMessageInInterceptor in = new XMLMessageInInterceptor();

    public void setUp() throws Exception {
        super.setUp();
        in.setPhase("phase1");
        chain.add(in);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHandleMessageOnBareMultiParam() throws Exception {
        String ns = "http://apache.org/hello_world_xml_http/bare";
        prepareMessage("/message-bare-multi-param.xml");
        common("/wsdl/hello_world_xml_bare.wsdl", new QName(ns, "XMLPort"),
                        org.apache.hello_world_xml_http.bare.Greeter.class);
        in.handleMessage(xmlMessage);
        List list = xmlMessage.getContent(List.class);
        assertNotNull(list);
        assertEquals("expect 2 param", 2, list.size());
        assertEquals("method input in2 is MyComplexStructType", true,
                        list.get(0) instanceof MyComplexStructType);
        assertEquals("method input in1 is String tli", true, ((String) list.get(1)).indexOf("tli") >= 0);
    }

    public void testHandleMessageOnBareSingleChild() throws Exception {
        String ns = "http://apache.org/hello_world_xml_http/bare";
        prepareMessage("/message-bare-single-param-element.xml");
        common("/wsdl/hello_world_xml_bare.wsdl", new QName(ns, "XMLPort"),
                        org.apache.hello_world_xml_http.bare.Greeter.class);
        in.handleMessage(xmlMessage);
        List list = xmlMessage.getContent(List.class);
        assertNotNull(list);
        assertEquals("expect 1 param", 1, list.size());
        assertEquals("method input me is String tli", true, ((String) list.get(0)).indexOf("tli") >= 0);
    }

    public void testHandleMessageWrapped() throws Exception {
        String ns = "http://apache.org/hello_world_xml_http/wrapped";
        prepareMessage("/message-wrap.xml");
        common("/wsdl/hello_world_xml_wrapped.wsdl", new QName(ns, "XMLPort"),
                        org.apache.hello_world_xml_http.wrapped.Greeter.class);
        
        OperationInfo op = serviceInfo.getInterface().getOperation(new QName(ns, "greetMe"));
        op.getUnwrappedOperation().getInput().setProperty(WrappedInInterceptor.WRAPPER_CLASS, GreetMe.class);
        
        in.handleMessage(xmlMessage);
        List list = xmlMessage.getContent(List.class);
        assertNotNull(list);
        assertEquals("expect 1 param", 1, list.size());
        assertEquals("method input me is String tli", true, list.get(0) instanceof GreetMe);
    }

    private void prepareMessage(String messageFileName) throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(messageFileName);
        xmlMessage.setContent(InputStream.class, inputStream);
        xmlMessage.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(inputStream));
    }
}

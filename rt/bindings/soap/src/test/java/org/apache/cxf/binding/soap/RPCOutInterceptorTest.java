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

package org.apache.cxf.binding.soap;


public class RPCOutInterceptorTest extends TestBase {
//
//    private ByteArrayOutputStream baos;
//    
//    public void setUp() throws Exception {
//        super.setUp();
//        
//        baos =  new ByteArrayOutputStream();
//
//        soapMessage.getExchange().put(SoapMessage.DATAWRITER_FACTORY_KEY, "test.writer.factory");
//        soapMessage.put(SoapMessage.BINDING_INFO,
//                        getTestService("resources/wsdl/hello_world_rpc_lit.wsdl", "SoapPortRPCLit"));
//
//        soapMessage.setContent(XMLStreamWriter.class, getXMLStreamWriter(baos));
//        soapMessage.put(Message.INVOCATION_OPERATION, "sendReceiveData");
//    }
//
//    public void tearDown() throws Exception {
//        baos.close();
//    }
//
    public void testWriteOutbound() throws Exception {
//        RPCOutInterceptor interceptor = new RPCOutInterceptor();
//        soapMessage.put(Message.INVOCATION_OBJECTS, Arrays.asList(getTestObject()));
//
//        interceptor.handleMessage(soapMessage);
//
//        assertNull(soapMessage.getContent(Exception.class));
//        
//        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
//        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
//        StaxUtils.toNextElement(reader);
//        assertEquals(new QName("http://apache.org/hello_world_rpclit", "sendReceiveData"),
//                     reader.getName());
//        
//        StaxUtils.nextEvent(reader);
//        StaxUtils.toNextElement(reader);
//        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "in"),
//                     reader.getName());
//
//        StaxUtils.toNextText(reader);
//        assertEquals("this is elem1", reader.getText());
    }
//
//    public void testWriteInbound() throws Exception {
//        RPCOutInterceptor interceptor = new RPCOutInterceptor();
//        soapMessage.put(Message.INVOCATION_OBJECTS, Arrays.asList(getTestObject()));
//        soapMessage.put(Message.INBOUND_MESSAGE, Message.INBOUND_MESSAGE);
//        
//        interceptor.handleMessage(soapMessage);
//        assertNull(soapMessage.getContent(Exception.class));
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
//        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
//        StaxUtils.toNextElement(reader);
//        assertEquals(new QName("http://apache.org/hello_world_rpclit", "sendReceiveDataResponse"),
//                     reader.getName());
//        
//        StaxUtils.nextEvent(reader);
//        StaxUtils.toNextElement(reader);
//        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "out"), reader.getName());
//
//        StaxUtils.nextEvent(reader);
//        StaxUtils.toNextElement(reader);
//
//        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem1"), reader.getName());
//
//        StaxUtils.nextEvent(reader);
//        StaxUtils.toNextText(reader);
//        assertEquals("this is elem1", reader.getText());
//    }
//
//
//    public BindingInfo getTestService(String wsdlUrl, String port) throws Exception {
//        BindingInfo binding = super.getTestService(wsdlUrl, port);
//        BindingOperationInfo op = binding.getOperation(new QName("http://apache.org/hello_world_rpclit",
//                                                                 "sendReceiveData"));
//        OperationInfo oi = op.getOperationInfo();
//        oi.setProperty("test.writer.factory", getTestWriterFactory(GreeterRPCLit.class));
//        return binding;
//    }
//
//    public MyComplexStruct getTestObject() {
//        MyComplexStruct obj = new MyComplexStruct();
//        obj.setElem1("this is elem1");
//        obj.setElem2("this is elem2");
//        obj.setElem3(1982);
//        return obj;
//    }
}

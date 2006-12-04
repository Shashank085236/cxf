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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

public class RPCOutInterceptorTest extends TestBase {

    private static final String TNS = "http://apache.org/hello_world_rpclit";

    private static final String OPNAME = "sendReceiveData";

    private ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);

    private IMocksControl control = EasyMock.createNiceControl();

    public void setUp() throws Exception {
        super.setUp();
        ServiceInfo si = getMockedServiceModel(this.getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl")
                .toString());
        BindingInfo bi = si.getBinding(new QName(TNS, "Greeter_SOAPBinding_RPCLit"));
        BindingOperationInfo boi = bi.getOperation(new QName(TNS, OPNAME));
        soapMessage.getExchange().put(BindingOperationInfo.class, boi);

        control.reset();
        Service service = control.createMock(Service.class);
        JAXBDataBinding dataBinding = new JAXBDataBinding(MyComplexStruct.class);
        service.getDataBinding();
        EasyMock.expectLastCall().andReturn(dataBinding).anyTimes();
        service.getServiceInfo();
        EasyMock.expectLastCall().andReturn(si).anyTimes();

        soapMessage.getExchange().put(Service.class, service);
        soapMessage.getExchange().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE);
        control.replay();
        
        MyComplexStruct mcs = new MyComplexStruct();
        mcs.setElem1("elem1");
        mcs.setElem2("elem2");
        mcs.setElem3(45);
        List<MyComplexStruct> param = new ArrayList<MyComplexStruct>();
        param.add(mcs);
        soapMessage.setContent(List.class, param);
    }

    public void tearDown() throws Exception {
        baos.close();
    }

    public void testWriteOutbound() throws Exception {
        RPCOutInterceptor interceptor = new RPCOutInterceptor();

        soapMessage.setContent(XMLStreamWriter.class, XMLOutputFactory.newInstance().createXMLStreamWriter(
                baos));

        soapMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        interceptor.handleMessage(soapMessage);
        assertNull(soapMessage.getContent(Exception.class));
        soapMessage.getContent(XMLStreamWriter.class).flush();
        baos.flush();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit", "sendReceiveData"), reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit", "in"), reader.getName());

        StaxUtils.toNextText(reader);
        assertEquals("elem1", reader.getText());
    }

    public void testWriteInbound() throws Exception {
        RPCOutInterceptor interceptor = new RPCOutInterceptor();
        soapMessage.setContent(XMLStreamWriter.class, XMLOutputFactory.newInstance().createXMLStreamWriter(
                baos));

        interceptor.handleMessage(soapMessage);
        assertNull(soapMessage.getContent(Exception.class));
        soapMessage.getContent(XMLStreamWriter.class).flush();
        baos.flush();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit", "sendReceiveDataResponse"), reader
                .getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit", "out"), reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);

        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem1"), reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("elem1", reader.getText());
    }

}

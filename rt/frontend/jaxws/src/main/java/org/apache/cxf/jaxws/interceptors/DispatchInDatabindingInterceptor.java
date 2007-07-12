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

package org.apache.cxf.jaxws.interceptors;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.XMLMessage;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class DispatchInDatabindingInterceptor extends AbstractInDatabindingInterceptor {

    private static final Logger LOG = Logger.getLogger(DispatchInDatabindingInterceptor.class.getName());
    private Class type;
    private Service.Mode mode;
    
    public DispatchInDatabindingInterceptor(Class type, Mode mode) {
        super(Phase.READ);
        
        this.type = type;
        this.mode = mode;
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();     
        Endpoint ep = ex.get(Endpoint.class);
        
        if (ep.getEndpointInfo().getBinding().getOperations().iterator().hasNext()) {
            BindingOperationInfo bop = ep.getEndpointInfo().getBinding().getOperations().iterator().next();
            ex.put(BindingOperationInfo.class, bop);
            getMessageInfo(message, bop);
        }
        
        List<Object> params = new ArrayList<Object>();          
        
        if (isGET(message)) {
            params.add(null);
            message.setContent(List.class, params);
            LOG.info("DispatchInInterceptor skipped in HTTP GET method");
            return;
        }       
     
        try {
            InputStream is = message.getContent(InputStream.class);
            Object obj = null;
            org.apache.cxf.service.Service service = 
                message.getExchange().get(org.apache.cxf.service.Service.class);
            
            
            if (message instanceof SoapMessage) {
                SOAPMessage soapMessage = newSOAPMessage(is, ((SoapMessage)message).getVersion());

                if (type.equals(SOAPMessage.class)) {
                    obj = soapMessage;
                } else if (type.equals(SOAPBody.class)) {
                    obj = soapMessage.getSOAPBody();
                } else {
                    DataReader<Node> dataReader = getDataReader(message, Node.class);
                    Node n = null;
                    if (mode == Service.Mode.MESSAGE) {
                        n = soapMessage.getSOAPPart();
                    } else if (mode == Service.Mode.PAYLOAD) {
                        n = DOMUtils.getChild(soapMessage.getSOAPBody(), Node.ELEMENT_NODE);
                    }
                    if (Source.class.isAssignableFrom(type)) {
                        obj = dataReader.read(null, n, type);
                    } else {
                        dataReader.setProperty(JAXBDataBinding.UNWRAP_JAXB_ELEMENT, Boolean.FALSE);
                        obj = dataReader.read(n);
                    }
                }

                message.setContent(SOAPMessage.class, soapMessage);               
            } else if (message instanceof XMLMessage) {
                if (type.equals(DataSource.class)) {
                    try {
                        obj = new ByteArrayDataSource(is, (String) message.get(Message.CONTENT_TYPE));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } else {
                    new StaxInInterceptor().handleMessage(message);

                    DataReader<XMLStreamReader> dataReader = getDataReader(message);
                    Class<?> readType = type;
                    if (readType == Object.class) {
                        readType = null;
                    }
                    obj = dataReader.read(null, message.getContent(XMLStreamReader.class), readType);
                    
                    if (!Source.class.isAssignableFrom(type)) {
                        //JAXB, need to make a Source format available for Logical handler                   
                        DataWriter<XMLStreamWriter> dataWriter =
                            service.getDataBinding().createWriter(XMLStreamWriter.class);
                        W3CDOMStreamWriter xmlWriter = new W3CDOMStreamWriter();
                        dataWriter.write(obj, xmlWriter);                       

                        Source source = new DOMSource(xmlWriter.getDocument().getDocumentElement()); 
                        message.setContent(Source.class, source);
                    }
                }
            }
            params.add(obj);           
            message.setContent(Object.class, obj);    
            message.setContent(List.class, params);
            
            is.close();
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private SOAPMessage newSOAPMessage(InputStream is, SoapVersion version) throws Exception {
        // TODO: Get header from message, this interceptor should after
        // readHeadersInterceptor

        MimeHeaders headers = new MimeHeaders();
        MessageFactory msgFactory = null;
        if (version == null || version instanceof Soap11) {
            msgFactory = MessageFactory.newInstance();
        } else if (version instanceof Soap12) {
            msgFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        }
        return msgFactory.createMessage(headers, is);
    }
}

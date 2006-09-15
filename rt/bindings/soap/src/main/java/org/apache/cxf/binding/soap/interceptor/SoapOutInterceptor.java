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

package org.apache.cxf.binding.soap.interceptor;


import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;



import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;



import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.DataWriterFactory;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.StaxUtils;


public class SoapOutInterceptor extends AbstractSoapInterceptor {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SoapOutInterceptor.class);

    public SoapOutInterceptor() {
        super();
        setPhase(Phase.WRITE);
    }
    
    public void handleMessage(SoapMessage message) {
        try {
            XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
            message.setContent(XMLStreamWriter.class, xtw);
            SoapVersion soapVersion = message.getVersion();
            
            xtw.writeStartElement(soapVersion.getPrefix(), 
                                  soapVersion.getEnvelope().getLocalPart(),
                                  soapVersion.getNamespace());
            xtw.writeNamespace(soapVersion.getPrefix(), soapVersion.getNamespace());
            
            Element eleHeaders = message.getHeaders(Element.class);
            boolean preexistingHeaders = eleHeaders != null && eleHeaders.hasChildNodes();
            if (preexistingHeaders) {
                StaxUtils.writeElement(eleHeaders, xtw, true, false);
            }
            handleHeaderPart(preexistingHeaders, message);
       
            xtw.writeStartElement(soapVersion.getPrefix(), 
                                  soapVersion.getBody().getLocalPart(),
                                  soapVersion.getNamespace());
            
            // Calling for Wrapped/RPC/Doc/ Interceptor for writing SOAP body
            message.getInterceptorChain().doIntercept(message);
            xtw.writeEndElement();
            
            // Write Envelop end element
            xtw.writeEndElement();
            
            xtw.flush();
        } catch (XMLStreamException e) {
            //e.printStackTrace();
            throw new SoapFault(
                new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), e, SoapFault.SENDER);
        }
    }
    
    private void handleHeaderPart(boolean preexistingHeaders, SoapMessage message) {
        //add MessagePart to soapHeader if necessary
        Exchange exchange = message.getExchange();
        BindingOperationInfo operation = (BindingOperationInfo)exchange.get(BindingOperationInfo.class
                                                                            .getName());
        if (operation == null) {
            return;
        }
        XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
        
        boolean startedHeader = false;
                 
        int countParts = 0;
        List<MessagePartInfo> parts = null;
        if (!isRequestor(message)) {
            if (operation.getOperationInfo().hasOutput()) {
                parts = operation.getOutput().getMessageInfo().getMessageParts();
            } else {
                parts = new ArrayList<MessagePartInfo>();
            }
        } else {
            if (operation.getOperationInfo().hasInput()) {
                parts = operation.getInput().getMessageInfo().getMessageParts();
            } else {
                parts = new ArrayList<MessagePartInfo>();
            }
        }
        countParts = parts.size();
 
        if (countParts > 0) {
            List<?> objs = message.getContent(List.class);
            Object[] args = objs.toArray();
            Object[] els = parts.toArray();
 
             
            SoapVersion soapVersion = message.getVersion();
            for (int idx = 0; idx < countParts; idx++) {
                Object arg = args[idx];
                MessagePartInfo part = (MessagePartInfo)els[idx];
                if (!part.isInSoapHeader()) {
                    continue;
                } else {
                    if (!(startedHeader || preexistingHeaders)) {
                        try {
                            xtw.writeStartElement(soapVersion.getPrefix(), 
                                                  soapVersion.getHeader().getLocalPart(),
                                                  soapVersion.getNamespace());
                        } catch (XMLStreamException e) {
                            throw new SoapFault(
                                new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), 
                                e, SoapFault.SENDER);
                        }
                        
                        startedHeader = true;
                    }
                    QName elName = ServiceModelUtil.getPartName(part);
                    DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message);
                    dataWriter.write(arg, elName, xtw);
                }
                 
            }
            if (startedHeader || preexistingHeaders) {
                try {
                    xtw.writeEndElement();
                } catch (XMLStreamException e) {
                    throw new SoapFault(
                        new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), 
                        e, SoapFault.SENDER);
                }
            }
        }
    }       
    
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }

    protected DataWriter<XMLStreamWriter> getDataWriter(Message message) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        DataWriterFactory factory = service.getDataWriterFactory();

        DataWriter<XMLStreamWriter> dataWriter = null;
        for (Class<?> cls : factory.getSupportedFormats()) {
            if (cls == XMLStreamWriter.class) {
                dataWriter = factory.createWriter(XMLStreamWriter.class);
                break;
            }
        }

        if (dataWriter == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAWRITER", BUNDLE, service
                .getName()));
        }

        return dataWriter;
    }
}

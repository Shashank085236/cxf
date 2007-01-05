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

import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.xml.XMLFault;
import org.apache.cxf.bindings.xformat.XMLBindingMessageFormat;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.BareInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class XMLMessageInInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = Logger.getLogger(XMLMessageInInterceptor.class.getName());
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLMessageInInterceptor.class);
    
    // TODO: this should be part of the chain!!
    private BareInInterceptor bareInterceptor = new BareInInterceptor();
    private WrappedInInterceptor wrappedInterceptor = new WrappedInInterceptor();
    
    public XMLMessageInInterceptor() {
        super();
        setPhase(Phase.UNMARSHAL);
    }

    public void handleMessage(Message message) throws Fault {
        if (isGET(message)) {            
            LOG.info("XMLMessageInInterceptor skipped in HTTP GET method");
            return;
        }
        Endpoint ep = message.getExchange().get(Endpoint.class);
        
        XMLStreamReader xsr = message.getContent(XMLStreamReader.class);        
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xsr);        
        if (!StaxUtils.toNextElement(reader)) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION_ELEMENT", BUNDLE));
        }
        
        Exchange ex = message.getExchange();
        QName startQName = reader.getName();
        // handling xml fault message
        if (startQName.getLocalPart().equals(XMLFault.XML_FAULT_ROOT)) {            
            message.getInterceptorChain().abort();
            
            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(message);
                return;
            }
        }
        // handling xml normal inbound message
        BindingOperationInfo boi = ex.get(BindingOperationInfo.class);
        boolean isRequestor = isRequestor(message);
        if (boi == null) {
            BindingInfo service = ep.getEndpointInfo().getBinding();
            boi = getBindingOperationInfo(isRequestor, startQName, service, xsr);
            if (boi != null) {
                ex.put(BindingOperationInfo.class, boi);
                ex.put(OperationInfo.class, boi.getOperationInfo());
                ex.setOneWay(boi.getOperationInfo().isOneWay());
            }
        } else {
            BindingMessageInfo bmi = isRequestor ? boi.getOutput()
                                                 : boi.getInput();

            if (hasRootNode(bmi, startQName)) { 
                try {
                    xsr.nextTag();
                } catch (XMLStreamException xse) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_READ_EXC", BUNDLE));
                }
            }
        }

        if (boi != null) {
            OperationInfo oi = boi.getOperationInfo();
            if (!oi.isUnwrappedCapable()) {
                bareInterceptor.handleMessage(message);
            } else {
                wrappedInterceptor.handleMessage(message);
            }
        } else { 
            throw new Fault(new org.apache.cxf.common.i18n.Message("REQ_NOT_UNDERSTOOD", 
                                                                   BUNDLE, 
                                                                   startQName));
        }
    }

    private BindingOperationInfo getBindingOperationInfo(boolean isRequestor, 
                                                         QName startQName, 
                                                         BindingInfo bi, 
                                                         XMLStreamReader xsr) {

        BindingOperationInfo match = null;
        for (BindingOperationInfo boi : bi.getOperations()) {
            BindingMessageInfo bmi;
            if (!isRequestor) {
                bmi = boi.getInput();
            } else {
                bmi = boi.getOutput();
            }
            
            if (hasRootNode(bmi, startQName)) {
                match = boi;
                //Consume The rootNode tag
                try {
                    xsr.nextTag();
                } catch (XMLStreamException xse) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_READ_EXC", BUNDLE));
                }
            } else {
                Collection<MessagePartInfo> bodyParts = bmi.getMessageParts();
                if (bodyParts.size() == 1) {
                    MessagePartInfo p = bodyParts.iterator().next();
                    if (p.getConcreteName().equals(startQName)) {
                        match = boi;
                    }
                }
            }
        }
        return match;
    }

    private boolean hasRootNode(BindingMessageInfo bmi, QName elName) {
        XMLBindingMessageFormat xmf = bmi.getExtensor(XMLBindingMessageFormat.class);
        return xmf != null && xmf.getRootNode().equals(elName);
    }
}

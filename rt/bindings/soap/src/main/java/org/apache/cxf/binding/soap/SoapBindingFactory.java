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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.MultipartMessageInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCInInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.interceptor.BareInInterceptor;
import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;

public class SoapBindingFactory extends AbstractBindingFactory {

    private Map cachedBinding = new HashMap<BindingInfo, Binding>();
    
    @Resource
    private Bus bus;
    
    @Resource
    private Collection<String> activationNamespaces;
    
    @PostConstruct
    void registerSelf() {
        if (null == bus) { 
            return;
        }
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        if (null != bfm) {
            for (String ns : activationNamespaces) {
                bfm.registerBindingFactory(ns, this);
            }
        }
    }
     
    public Binding createBinding(BindingInfo binding) {
        
        if (cachedBinding.get(binding) != null) {
            return (Binding)cachedBinding.get(binding);
        }
        
        SoapBinding sb = new SoapBinding();
        SoapBindingInfo sbi = (SoapBindingInfo)binding;
                
        for (BindingOperationInfo boi : sbi.getOperations()) {
            if (boi.getUnwrappedOperation() == null) {
                sbi.setStyle(SoapConstants.STYLE_BARE);
            }
        }
        
        sb.getInInterceptors().add(new MultipartMessageInterceptor());        
        sb.getInInterceptors().add(new ReadHeadersInterceptor());        
        sb.getInInterceptors().add(new MustUnderstandInterceptor());
        sb.getInInterceptors().add(new StaxInInterceptor());

        sb.getOutInterceptors().add(new StaxOutInterceptor());
        sb.getOutInterceptors().add(new SoapOutInterceptor());

        sb.getOutFaultInterceptors().add(new AttachmentOutInterceptor());
        sb.getOutFaultInterceptors().add(new StaxOutInterceptor());
        sb.getOutFaultInterceptors().add(new SoapOutInterceptor());
        sb.getOutFaultInterceptors().add(sb.getOutFaultInterceptor());

        if (SoapConstants.STYLE_RPC.equalsIgnoreCase(sbi.getStyle())) {
            sb.getInInterceptors().add(new RPCInInterceptor());
            sb.getOutInterceptors().add(new RPCOutInterceptor());
        } else if (SoapConstants.STYLE_BARE.equalsIgnoreCase(sbi.getStyle())) {
            sb.getInInterceptors().add(new BareInInterceptor());
            sb.getOutInterceptors().add(new BareOutInterceptor());
        } else {
            sb.getInInterceptors().add(new WrappedInInterceptor());
            sb.getOutInterceptors().add(new WrappedOutInterceptor());
            sb.getOutInterceptors().add(new BareOutInterceptor());
        }        
        
                
        return sb;
    }

    public BindingInfo createBindingInfo(ServiceInfo service, javax.wsdl.Binding binding) {
        String ns = ((ExtensibilityElement)binding.getExtensibilityElements().get(0)).getElementType()
            .getNamespaceURI();
        SoapBindingInfo bi = new SoapBindingInfo(service, ns, Soap11.getInstance());

        // Copy all the extensors
        initializeBindingInfo(service, binding, bi);

        SOAPBinding wSoapBinding = bi.getExtensor(SOAPBinding.class);
        bi.setTransportURI(wSoapBinding.getTransportURI());
        bi.setStyle(wSoapBinding.getStyle());

        for (BindingOperationInfo boi : bi.getOperations()) {
            initializeBindingOperation(bi, boi);
        }

        return bi;
    }

    private void initializeBindingOperation(SoapBindingInfo bi, BindingOperationInfo boi) {
        SoapOperationInfo soi = new SoapOperationInfo();
        
        SOAPOperation soapOp = boi.getExtensor(SOAPOperation.class);
        if (soapOp != null) {
            String action = soapOp.getSoapActionURI();
            if (action == null) {
                action = "";
            }

            soi.setAction(action);
            soi.setStyle(soapOp.getStyle());
            
            
        }

        boi.addExtensor(soi);

        if (boi.getInput() != null) {
            initializeMessage(bi, boi, boi.getInput());
        }

        if (boi.getOutput() != null) {
            initializeMessage(bi, boi, boi.getOutput());
        }
    }

    private void initializeMessage(SoapBindingInfo bi, BindingOperationInfo boi, BindingMessageInfo bmsg) {
        MessageInfo msg = bmsg.getMessageInfo();

        List<MessagePartInfo> messageParts = new ArrayList<MessagePartInfo>();
        messageParts.addAll(msg.getMessageParts());

        List<SOAPHeader> headers = bmsg.getExtensors(SOAPHeader.class);
        if (headers != null) {
            for (SOAPHeader header : headers) {
                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setUse(header.getUse());

                MessagePartInfo part = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(), header
                    .getPart()));
                if (part != null) {
                    part.setInSoapHeader(true);
                }
                headerInfo.setPart(part);
                messageParts.remove(part);

                bmsg.addExtensor(headerInfo);
            }
        }

        SOAPBody soapBody = bmsg.getExtensor(SOAPBody.class);
        SoapBodyInfo bodyInfo = new SoapBodyInfo();
        bodyInfo.setUse(soapBody.getUse());

        // Initialize the body parts.
        if (soapBody.getParts() != null) {
            List<MessagePartInfo> bodyParts = new ArrayList<MessagePartInfo>();
            for (Iterator itr = soapBody.getParts().iterator(); itr.hasNext();) {
                String partName = (String)itr.next();

                MessagePartInfo part = msg
                    .getMessagePart(new QName(msg.getName().getNamespaceURI(), partName));
                bodyParts.add(part);
            }
            bodyInfo.setParts(bodyParts);
        } else {
            bodyInfo.setParts(messageParts);
        }

        bmsg.addExtensor(bodyInfo);
    }
}

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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.xml.namespace.QName;

import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.extensions.soap.SoapBinding;
import org.apache.cxf.tools.common.extensions.soap.SoapBody;
import org.apache.cxf.tools.common.extensions.soap.SoapHeader;
import org.apache.cxf.tools.common.extensions.soap.SoapOperation;
import org.apache.cxf.tools.common.model.JavaAnnotation;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.ProcessorUtil;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customiztion.JAXWSBinding;

public class ServiceProcessor extends AbstractProcessor {

    private String soapOPAction = "SOAPACTION";

    private String soapOPStyle = "STYLE";

    private BindingType bindingType;

    private final int inHEADER = 1;

    private final int outHEADER = 2;

    private final int resultHeader = 3;

    private final int noHEADER = 0;

    private Object bindingObj;
    private ServiceInfo service;
    
    private JAXWSBinding jaxwsBinding = new JAXWSBinding();

    public ServiceProcessor(ToolContext penv) {
        super(penv);
    }

    public void process(ServiceInfo si) throws ToolException {
        this.service = si;
        processService(context.get(JavaModel.class));
        
    }

    private boolean isNameCollision(String packageName, String className) {
        if (context.optionSet(ToolConstants.CFG_GEN_OVERWRITE)) {
            return false;
        }
        ClassCollector collector = context.get(ClassCollector.class);
        return collector.containTypesClass(packageName, className)
               || collector.containSeiClass(packageName, className)
               || collector.containExceptionClass(packageName, className);
    }

    private void processService(JavaModel model) throws ToolException {
        JavaServiceClass sclz = new JavaServiceClass(model);
        String name = ProcessorUtil.mangleNameToClassName(service.getName().getLocalPart());
        String namespace = service.getName().getNamespaceURI();
        String packageName = ProcessorUtil.parsePackageName(namespace, context.mapPackageName(namespace));

        while (isNameCollision(packageName, name)) {
            name = name + "_Service";
        }
        
        //customizing
        JAXWSBinding serviceBinding = service.getExtensor(JAXWSBinding.class);
        
        //TODO : Handle service customized class
        if (serviceBinding != null) {
            if (serviceBinding.getPackage() != null) {
                jaxwsBinding.setPackage(serviceBinding.getPackage());
            }
            
            if (serviceBinding.isEnableAsyncMapping()) {
                jaxwsBinding.setEnableAsyncMapping(true);
            }
            
            if (serviceBinding.isEnableMime()) {
                jaxwsBinding.setEnableMime(true);
            }
            
            if (serviceBinding.isEnableWrapperStyle()) {
                jaxwsBinding.setEnableWrapperStyle(true);
            }
        }
        
        sclz.setName(name);
        sclz.setServiceName(service.getName().getLocalPart());
        sclz.setNamespace(namespace);
        
        if (jaxwsBinding.getPackage() != null) {
            sclz.setPackageName(jaxwsBinding.getPackage());
        } else {
            sclz.setPackageName(packageName);
        }

        Collection<EndpointInfo> ports = service.getEndpoints();

        for (EndpointInfo port : ports) {
            JavaPort javaport = processPort(model, port);
            sclz.addPort(javaport);
        }
        model.addServiceClass(name, sclz);
    }

    private JavaPort processPort(JavaModel model, EndpointInfo port) throws ToolException {
        JavaPort jport = new JavaPort(ProcessorUtil.mangleNameToClassName(port.getName().getLocalPart()));
        jport.setPortName(port.getName().getLocalPart());
        BindingInfo binding = port.getBinding();
        jport.setBindingAdress(port.getAddress());
        jport.setBindingName(binding.getName().getLocalPart());
        
        String namespace = binding.getInterface().getName().getNamespaceURI();
        String packageName = ProcessorUtil.parsePackageName(namespace, context.mapPackageName(namespace));
        jport.setPackageName(packageName);

        InterfaceInfo infInfo = binding.getInterface();
              
        String portType = binding.getInterface().getName().getLocalPart();
        jport.setPortType(portType);
        
        JAXWSBinding infBinding = infInfo.getExtensor(JAXWSBinding.class);
        
        if (infBinding != null && !infBinding.isEnableAsyncMapping()) {
            jaxwsBinding.setEnableAsyncMapping(false);
        }
        
        if (infBinding != null && !infBinding.isEnableWrapperStyle()) {
            jaxwsBinding.setEnableWrapperStyle(false);
        }
        
        if (infBinding != null && infBinding.getJaxwsClass() != null 
            && infBinding.getJaxwsClass().getClassName() != null) {
            String className = ProcessorUtil.mangleNameToClassName(infBinding.getJaxwsClass().getClassName());
            jport.setInterfaceClass(className);
        } else {
            jport.setInterfaceClass(ProcessorUtil.mangleNameToClassName(portType));
        }

        bindingType = getBindingType(binding);
        
        if (bindingType == null) {
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message("BINDING_SPECIFY_ONE_PROTOCOL",
                                                       LOG, 
                                                       binding.getName());
            throw new ToolException(msg);
        }

        if (isSoapBinding()) {
            SoapBinding spbd = SOAPBindingUtil.getProxy(SoapBinding.class, this.bindingObj);
            jport.setStyle(SOAPBindingUtil.getSoapStyle(spbd.getStyle()));
            jport.setTransURI(spbd.getTransportURI());
        }

        Collection<BindingOperationInfo> operations = binding.getOperations();
        for (BindingOperationInfo bop : operations) {
            processOperation(model, bop, binding);
        }
        return jport;
    }

    private void processOperation(JavaModel model, BindingOperationInfo bop, BindingInfo binding)
        throws ToolException {
        
        JAXWSBinding bind = binding.getExtensor(JAXWSBinding.class);
 
        if (bind != null && !bind.isEnableMime()) {
            jaxwsBinding.setEnableMime(false);
        }
        
        JAXWSBinding bopBinding = bop.getExtensor(JAXWSBinding.class);
        
        if (bopBinding != null && bopBinding.isEnableMime()) {
            jaxwsBinding.setEnableMime(false);
            if (bopBinding.getJaxwsPara() != null) {
                jaxwsBinding.setJaxwsPara(bopBinding.getJaxwsPara());
            }
        }
             
        String portType = ProcessorUtil
            .mangleNameToClassName(binding.getInterface().getName().getLocalPart());
        
        JavaInterface jf = model.getInterfaces().get(portType);
       
        if (isSoapBinding()) {
            SoapBinding soapBinding = (SoapBinding)bindingObj;
            if (SOAPBindingUtil.getSoapStyle(soapBinding.getStyle()) == null) {
                jf.setSOAPStyle(javax.jws.soap.SOAPBinding.Style.DOCUMENT);
            } else {
                jf.setSOAPStyle(SOAPBindingUtil.getSoapStyle(soapBinding.getStyle()));
            }
        } else {
            // REVISIT: fix for xml binding
            jf.setSOAPStyle(javax.jws.soap.SOAPBinding.Style.DOCUMENT);
        }

        Object[] methods = jf.getMethods().toArray();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod jm = (JavaMethod)methods[i];
            if (jm.getOperationName() != null && jm.getOperationName().equals(bop.getName().getLocalPart())) {
                if (isSoapBinding()) {
                    // TODO: add customize here
                    //doCustomizeOperation(jf, jm, bop);
                    Map prop = getSoapOperationProp(bop);
                    String soapAction = prop.get(soapOPAction) == null ? "" : (String)prop.get(soapOPAction);
                    String soapStyle = prop.get(soapOPStyle) == null ? "" : (String)prop.get(soapOPStyle);
                    jm.setSoapAction(soapAction);
                    if (SOAPBindingUtil.getSoapStyle(soapStyle) == null && this.bindingObj == null) {
                        org.apache.cxf.common.i18n.Message msg = 
                            new  org.apache.cxf.common.i18n.Message("BINDING_STYLE_NOT_DEFINED",
                                                                         LOG);
                        throw new ToolException(msg);
                    }
                    if (SOAPBindingUtil.getSoapStyle(soapStyle) == null) {
                        jm.setSoapStyle(jf.getSOAPStyle());
                    } else {
                        jm.setSoapStyle(SOAPBindingUtil.getSoapStyle(soapStyle));
                    }
                } else {
                    // REVISIT: fix for xml binding
                    jm.setSoapStyle(jf.getSOAPStyle());
                }

                if (jm.getSoapStyle().equals(javax.jws.soap.SOAPBinding.Style.RPC)) {
                    jm.getAnnotationMap().remove("SOAPBinding");
                }

                OperationProcessor processor = new OperationProcessor(context);

                int headerType = isNonWrappable(bop);

                OperationInfo opinfo = bop.getOperationInfo();
                
                JAXWSBinding opBinding = (JAXWSBinding)opinfo.getExtensor(JAXWSBinding.class);
                
                if (opBinding != null && !opBinding.isEnableWrapperStyle()) {
                    jaxwsBinding.setEnableWrapperStyle(false);
                    if (!opBinding.isEnableAsyncMapping()) {
                        jaxwsBinding.setEnableAsyncMapping(false);
                    }
                }
                                
                if (jm.isWrapperStyle() && headerType > this.noHEADER 
                    || !jaxwsBinding.isEnableWrapperStyle()) {
                    // changed wrapper style

                    jm.setWrapperStyle(false);
                    processor.processMethod(jm, bop.getOperationInfo(), jaxwsBinding);
                    jm.getAnnotationMap().remove("ResponseWrapper");
                    jm.getAnnotationMap().remove("RequestWrapper");

                } else {
                    processor.processMethod(jm, bop.getOperationInfo(), jaxwsBinding);

                }

                if (headerType == this.resultHeader) {
                    JavaAnnotation resultAnno = jm.getAnnotationMap().get("WebResult");
                    if (resultAnno != null) {
                        resultAnno.addArgument("header", "true", "");
                    }
                }
                processParameter(jm, bop);
            }
        }
    }

    private void setParameterAsHeader(JavaParameter parameter) {
        parameter.setHeader(true);
        parameter.getAnnotation().addArgument("header", "true", "");
    }

    private void processParameter(JavaMethod jm, BindingOperationInfo operation) throws ToolException {

        // process input
        
        List<ExtensibilityElement> inbindings = null;
        if (operation.getInput() != null) {
            inbindings = operation.getInput().getExtensors(ExtensibilityElement.class);
        }
        String use = null;
        for (ExtensibilityElement ext : inbindings) {
            if (SOAPBindingUtil.isSOAPBody(ext)) {
                SoapBody soapBody = SOAPBindingUtil.getSoapBody(ext);
                use = soapBody.getUse();
            }            
            
            if (SOAPBindingUtil.isSOAPHeader(ext)) {
                SoapHeader soapHeader = SOAPBindingUtil.getSoapHeader(ext);
                boolean found = false;
                for (JavaParameter parameter : jm.getParameters()) {
                    if (soapHeader.getPart().equals(parameter.getPartName())) {
                        setParameterAsHeader(parameter);
                        found = true;
                    }
                }
                if (Boolean.valueOf((String)context.get(ToolConstants.CFG_EXTRA_SOAPHEADER)).booleanValue()
                    && !found) {
                    // Header can't be found in java method parameters, in
                    // different message
                    // other than messages used in porttype operation
                    ParameterProcessor processor = new ParameterProcessor(context);
                    MessagePartInfo exPart = service.getMessage(soapHeader.getMessage())
                        .getMessagePart(new QName(soapHeader.getMessage().getNamespaceURI(),
                                                  soapHeader.getPart()));
                        
                    JavaType.Style jpStyle = JavaType.Style.IN;
                    if (isInOutParam(soapHeader.getPart(), operation.getOutput())) {
                        jpStyle = JavaType.Style.INOUT;
                    }
                    JavaParameter jp = processor.addParameterFromBinding(jm, exPart, jpStyle);
                    if (soapHeader.getPart() != null && soapHeader.getPart().length() > 0) {
                        jp.getAnnotation().addArgument("partName", soapHeader.getPart());
                    }
                    setParameterAsHeader(jp);
                }
            }
        }

        // process output
        if (operation.getOutput() != null) {
            List<ExtensibilityElement> outbindings =
                operation.getOutput().getExtensors(ExtensibilityElement.class);
            for (ExtensibilityElement ext : outbindings) {
                if (SOAPBindingUtil.isSOAPHeader(ext)) {
                    SoapHeader soapHeader = SOAPBindingUtil.getSoapHeader(ext);
                    boolean found = false;
                    for (JavaParameter parameter : jm.getParameters()) {
                        if (soapHeader.getPart().equals(parameter.getPartName())) {
                            setParameterAsHeader(parameter);
                            found = true;
                        }
                    }
                    if (jm.getReturn().getName().equals(soapHeader.getPart())) {
                        found = true;
                    }
                    if (Boolean.valueOf((String)context.get(ToolConstants.CFG_EXTRA_SOAPHEADER))
                        && !found) {
                        // Header can't be found in java method parameters, in
                        // different message
                        // other than messages used in porttype operation
                        ParameterProcessor processor = new ParameterProcessor(context);
                        MessagePartInfo exPart = service.getMessage(soapHeader.getMessage())
                            .getMessagePart(new QName(soapHeader.getMessage().getNamespaceURI(),
                                                      soapHeader.getPart()));
                        JavaParameter jp = processor.addParameterFromBinding(jm, exPart, JavaType.Style.OUT);
                        setParameterAsHeader(jp);
                    }
                }
            }
        }

        jm.setSoapUse(SOAPBindingUtil.getSoapUse(use));
        if (javax.jws.soap.SOAPBinding.Style.RPC == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.ENCODED == jm.getSoapUse()) {
            System.err.println("** Unsupported RPC-Encoded Style Use **");
        }
        if (javax.jws.soap.SOAPBinding.Style.RPC == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.LITERAL == jm.getSoapUse()) {
            return;
        }
        if (javax.jws.soap.SOAPBinding.Style.DOCUMENT == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.LITERAL == jm.getSoapUse()) {
            return;
        }
    }

    private Map getSoapOperationProp(BindingOperationInfo bop) {
        Map<String, Object> soapOPProp = new HashMap<String, Object>();

        for (ExtensibilityElement ext : bop.getExtensors(ExtensibilityElement.class)) {
            if (SOAPBindingUtil.isSOAPOperation(ext)) {
                SoapOperation soapOP = SOAPBindingUtil.getSoapOperation(ext);
                soapOPProp.put(this.soapOPAction, soapOP.getSoapActionURI());
                soapOPProp.put(this.soapOPStyle, soapOP.getStyle());
            }
        }
        return soapOPProp;
    }

    private BindingType getBindingType(BindingInfo binding) {
        for (ExtensibilityElement ext : binding.getExtensors(ExtensibilityElement.class)) {
            if (SOAPBindingUtil.isSOAPBinding(ext)) {
                bindingObj = SOAPBindingUtil.getSoapBinding(ext);
                return BindingType.SOAPBinding;
            }
            if (ext instanceof HTTPBinding) {
                bindingObj = (HTTPBinding)ext;
                return BindingType.HTTPBinding;
            }
            // TBD XMLBinding
            return BindingType.XMLBinding;
            
        }
        return null;
    }
    
    private int isNonWrappable(BindingOperationInfo bop) {
        QName operationName = bop.getName();
        MessageInfo bodyMessage = null;
        QName headerMessage = null;
        SoapHeader header = null;
        boolean containParts = false;
        boolean isSameMessage = false;
        boolean isNonWrappable = false;
        boolean allPartsHeader = false;
        int result = this.noHEADER;

        // begin process input
        if (bop.getInput() != null) {
            for (ExtensibilityElement ext : bop.getInput().getExtensors(ExtensibilityElement.class)) {
                if (SOAPBindingUtil.isSOAPBody(ext)) {
                    bodyMessage = getMessage(operationName, true);
                }
                if (SOAPBindingUtil.isSOAPHeader(ext)) {
                    header = SOAPBindingUtil.getSoapHeader(ext);
                    headerMessage = header.getMessage();
                    if (header.getPart().length() > 0) {
                        containParts = true;
                    }
                }
            }

            if (headerMessage != null && bodyMessage != null
                && headerMessage.getNamespaceURI().equalsIgnoreCase(bodyMessage.getName().getNamespaceURI())
                && headerMessage.getLocalPart().equalsIgnoreCase(bodyMessage.getName().getLocalPart())) {
                isSameMessage = true;
            }

            isNonWrappable = isSameMessage && containParts;
            // if is nonwrapple then return
            if (isNonWrappable) {
                result = this.inHEADER;
            }
        }
        isSameMessage = false;
        containParts = false;

        // process output
        if (bop.getOutput() != null) {
            for (ExtensibilityElement ext : bop.getOutput().getExtensors(ExtensibilityElement.class)) {
                if (SOAPBindingUtil.isSOAPBody(ext)) {
                    bodyMessage = getMessage(operationName, false);
                }
                if (SOAPBindingUtil.isSOAPHeader(ext)) {
                    header = SOAPBindingUtil.getSoapHeader(ext);
                    headerMessage = header.getMessage();
                    if (header.getPart().length() > 0) {
                        containParts = true;
                    }
                }
            }
            if (headerMessage != null && bodyMessage != null
                && headerMessage.getNamespaceURI().equalsIgnoreCase(bodyMessage.getName().getNamespaceURI())
                && headerMessage.getLocalPart().equalsIgnoreCase(bodyMessage.getName().getLocalPart())) {
                isSameMessage = true;
                if (bodyMessage.getMessageParts().size() == 1) {
                    allPartsHeader = true;
                }

            }
            isNonWrappable = isSameMessage && containParts;
            if (isNonWrappable && allPartsHeader) {
                result = this.resultHeader;
            }
            if (isNonWrappable && !allPartsHeader) {
                result = this.outHEADER;
            }
        }

        return result;
    }

    private MessageInfo getMessage(QName operationName, boolean isIn) {
        for (OperationInfo operation : service.getInterface().getOperations()) {
            if (operationName.equals(operation.getName()) && isIn) {
                return operation.getInput();
            } else {
                return operation.getOutput();
            }
        }
        return null;
    }
    
    public enum BindingType {
        HTTPBinding, SOAPBinding, XMLBinding
    }

    private boolean isSoapBinding() {
        return bindingType != null && "SOAPBinding".equals(bindingType.name());

    }

    private boolean isInOutParam(String inPartName, BindingMessageInfo messageInfo) {
        for (ExtensibilityElement ext : messageInfo.getExtensors(ExtensibilityElement.class)) {
            if (SOAPBindingUtil.isSOAPHeader(ext)) {
                String outPartName = (SOAPBindingUtil.getSoapHeader(ext)).getPart();
                if (inPartName.equals(outPartName)) {
                    return true;
                }
            }
        }
        return false;
    }
}

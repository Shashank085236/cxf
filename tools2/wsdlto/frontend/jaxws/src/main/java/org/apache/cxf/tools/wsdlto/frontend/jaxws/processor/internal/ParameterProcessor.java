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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.OperationType;
import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaReturn;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebParamAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper.ParameterMapper;

public class ParameterProcessor extends AbstractProcessor {
    
    private DataBindingProfile dataBinding;
    
    @SuppressWarnings("unchecked")
    public ParameterProcessor(ToolContext penv) {
        super(penv);
        dataBinding = context.get(DataBindingProfile.class);
    }

    private boolean isRequestResponse(JavaMethod method) {
        return method.getStyle() == OperationType.REQUEST_RESPONSE;
    }

    public void process(JavaMethod method,
                        MessageInfo inputMessage,
                        MessageInfo outputMessage,
                        List<String> parameterOrder) throws ToolException {
        
        if (!StringUtils.isEmpty(parameterOrder)
            && isValidOrdering(parameterOrder, inputMessage, outputMessage)
            && !method.isWrapperStyle()) {
            
            buildParamModelsWithOrdering(method,
                                         inputMessage,
                                         outputMessage,
                                         parameterOrder);
        } else {
            buildParamModelsWithoutOrdering(method,
                                            inputMessage,
                                            outputMessage);
        }
    }

    /**
     * This method will be used by binding processor to change existing
     * generated java method of porttype
     * 
     * @param method
     * @param part
     * @param style
     * @throws ToolException
     */
    public JavaParameter addParameterFromBinding(JavaMethod method,
                                                 MessagePartInfo part,
                                                 JavaType.Style style)
        throws ToolException {
        return addParameter(method, getParameterFromPart(part, style));
    }

    private JavaParameter getParameterFromPart(MessagePartInfo part, JavaType.Style style) {
        return ParameterMapper.map(part, style, context);
    }

    private JavaParameter addParameter(JavaMethod method, JavaParameter parameter) throws ToolException {
        parameter.setMethod(method);
        parameter.annotate(new WebParamAnnotator());
        method.addParameter(parameter);
        return parameter;
    }

    private void processReturn(JavaMethod method, MessagePartInfo part) {
        String name = part == null ? "return" : part.getName().getLocalPart();
        String type = part == null ? "void" : ProcessorUtil.resolvePartType(part, context);
 
        String namespace = part == null ? null : ProcessorUtil.resolvePartNamespace(part);
              
        JavaReturn returnType = new JavaReturn(name, type, namespace);
        returnType.setQName(ProcessorUtil.getElementName(part));
        returnType.setStyle(JavaType.Style.OUT);
        if (namespace != null && type != null && !"void".equals(type)) {
            returnType.setClassName(ProcessorUtil.getFullClzName(part, context, false));
        }
        method.setReturn(returnType);
    }

    @SuppressWarnings("unchecked")
    private void processInput(JavaMethod method, MessageInfo inputMessage) throws ToolException {
        for (MessagePartInfo part : inputMessage.getMessageParts()) {
            addParameter(method, getParameterFromPart(part, JavaType.Style.IN));
        }
    }
    
    @SuppressWarnings("unchecked")
    private void processWrappedInput(JavaMethod method, MessageInfo inputMessage) throws ToolException {
        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
        
        if (inputParts.size() > 1) {
            processInput(method, inputMessage);
            return;
        } else if (inputParts.isEmpty()) {
            return;
        }
        MessagePartInfo part = inputParts.iterator().next();
        
        List<QName> wrappedElements = ProcessorUtil.getWrappedElement(context, part.getElementQName());
        if (wrappedElements == null || wrappedElements.size() == 0) {
            return;
        }
        for (QName item : wrappedElements) {
            addParameter(method, getParameterFromQName(part.getElementQName(), 
                                                       item, JavaType.Style.IN, part));
        }
    }

    @SuppressWarnings("unchecked")
    private void processOutput(JavaMethod method, MessageInfo inputMessage, MessageInfo outputMessage)
        throws ToolException {
        Map<QName, MessagePartInfo> inputPartsMap = inputMessage.getMessagePartsMap();
        List<MessagePartInfo> outputParts = 
            outputMessage == null ? new ArrayList<MessagePartInfo>() : outputMessage.getMessageParts();
        // figure out output parts that are not present in input parts
        List<MessagePartInfo> outParts = new ArrayList<MessagePartInfo>();
        if (isRequestResponse(method)) {

            for (MessagePartInfo outpart : outputParts) {
                MessagePartInfo inpart = inputPartsMap.get(outpart.getName());
                if (inpart == null) {
                    outParts.add(outpart);
                    continue;
                } else if (isSamePart(inpart, outpart)) {
                    addParameter(method, getParameterFromPart(outpart, JavaType.Style.INOUT));
                    continue;
                } else if (!isSamePart(inpart, outpart)) {
                    outParts.add(outpart);
                    continue;
                }
            }
        }

        if (isRequestResponse(method) && outParts.size() == 1) {
            processReturn(method, outParts.get(0));
            return;
        } else {
            processReturn(method, null);
        }
        if (isRequestResponse(method)) {
            for (MessagePartInfo part : outParts) {
                addParameter(method, getParameterFromPart(part, JavaType.Style.OUT));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processWrappedOutput(JavaMethod method,
                                      MessageInfo inputMessage,
                                      MessageInfo outputMessage) throws ToolException {
        
        List<MessagePartInfo> outputParts = outputMessage.getMessageParts();
        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();

        if (inputParts.size() > 1 || outputParts.size() > 1) {
            processOutput(method, inputMessage, outputMessage);
            return;
        }
        if (outputParts.size() == 0) {
            addVoidReturn(method);
            return;
        }
        
        MessagePartInfo inputPart = inputParts.size() > 0 ? inputParts.iterator().next() : null;
        MessagePartInfo outputPart = outputParts.size() > 0 ? outputParts.iterator().next() : null;
        
        List<QName> inputWrapElement = null;
        List<QName> outputWrapElement = null;
        
        if (inputPart != null) {
            inputWrapElement = ProcessorUtil.getWrappedElement(context, inputPart.getElementQName()); 
        }       
        
        if (outputPart != null) {
            outputWrapElement = ProcessorUtil.getWrappedElement(context, outputPart.getElementQName()); 
        }

        if (inputWrapElement == null || outputWrapElement.size() == 0) {
            addVoidReturn(method);
            return;
        }
        method.setReturn(null);
        if (outputWrapElement.size() == 1 && inputWrapElement != null) {
            QName outElement = outputWrapElement.iterator().next();
            boolean sameWrapperChild = false;
            for (QName inElement : inputWrapElement) {
                if (isSameWrapperChild(inElement, outElement)) {
                    addParameter(method, getParameterFromQName(outputPart.getElementQName(), outElement, 
                                                               JavaType.Style.INOUT, outputPart));
                    sameWrapperChild = true;
                    if (method.getReturn() == null) {
                        addVoidReturn(method);
                    }
                    break;
                }
            }
            if (!sameWrapperChild) {
                method.setReturn(getReturnFromQName(outElement, outputPart));
                return;
            }
            
        }
        for (QName outElement : outputWrapElement) {
            if ("return".equals(outElement.getLocalPart())) {
                if (method.getReturn() != null) {
                    org.apache.cxf.common.i18n.Message msg = 
                        new org.apache.cxf.common.i18n.Message("WRAPPER_STYLE_TWO_RETURN_TYPES", LOG);
                    throw new ToolException(msg);
                }
                method.setReturn(getReturnFromQName(outElement, outputPart));
                continue;
            }
            boolean sameWrapperChild = false;
            if (inputWrapElement != null) {
                for (QName inElement : inputWrapElement) {
                    if (isSameWrapperChild(inElement, outElement)) {
                        addParameter(method, getParameterFromQName(outputPart.getElementQName(), outElement, 
                                                                   JavaType.Style.INOUT, outputPart));
                        sameWrapperChild = true;
                        break;
                    }
                }
            }
            if (!sameWrapperChild) {
                addParameter(method, getParameterFromQName(outputPart.getElementQName(), outElement, 
                                                           JavaType.Style.OUT, outputPart));
            }
        }
        if (method.getReturn() == null) {
            addVoidReturn(method);
        }
    }

    private void addVoidReturn(JavaMethod method) {
        JavaReturn returnType = new JavaReturn("return", "void", null);
        method.setReturn(returnType);
    }

    private boolean isSameWrapperChild(QName in, QName out) {
        if (!in.getLocalPart().equals(out.getLocalPart())) {
            return false;
        }
        
        if (!in.getNamespaceURI().equals(out.getNamespaceURI())) {
            return false;
        }
        return true;
    }

    private JavaParameter getParameterFromQName(QName wrapperElement, QName item, JavaType.Style style, 
                                                MessagePartInfo part) {

        String fullJavaName = "";
              
        fullJavaName = this.dataBinding.getWrappedElementType(wrapperElement, item);
        
        
        String targetNamespace = item.getNamespaceURI();
        if (targetNamespace == null) {
            targetNamespace = ProcessorUtil.resolvePartNamespace(part);
        }

        
        String jpname = ProcessorUtil.mangleNameToVariableName(item.getLocalPart());
        JavaParameter parameter = new JavaParameter(jpname, fullJavaName, targetNamespace);
        parameter.setStyle(style);
        parameter.setQName(item);
        
        if (style == JavaType.Style.OUT || style == JavaType.Style.INOUT) {
            parameter.setHolder(true);
            parameter.setHolderName(javax.xml.ws.Holder.class.getName());
            String holderClass = fullJavaName;
            if (JAXBUtils.holderClass(fullJavaName) != null) {
                holderClass = JAXBUtils.holderClass(fullJavaName).getName();
            }          
            parameter.setHolderClass(holderClass);
        }
        return parameter;

    }

    private JavaReturn getReturnFromQName(QName element, MessagePartInfo part) {
        String fullJavaName = "";
        String simpleJavaName = "";
         
        fullJavaName = this.dataBinding.getWrappedElementType(part.getElementQName(), element);
        simpleJavaName = fullJavaName;
        
        int index = fullJavaName.lastIndexOf(".");
        
        if (index > -1) {
            simpleJavaName = fullJavaName.substring(index);    
        }
        
        String targetNamespace = ProcessorUtil.resolvePartNamespace(part);
        if (targetNamespace == null) {
            targetNamespace = element.getNamespaceURI();
        }
        String jpname = ProcessorUtil.mangleNameToVariableName(simpleJavaName);
        JavaReturn returnType = new JavaReturn(jpname, fullJavaName , targetNamespace);
        returnType.setQName(element);
        returnType.setStyle(JavaType.Style.OUT);
        return returnType;
    }

    private void buildParamModelsWithoutOrdering(JavaMethod method,
                                                 MessageInfo inputMessage,
                                                 MessageInfo outputMessage) throws ToolException {
        if (inputMessage != null) {
            if (method.isWrapperStyle()) {
                processWrappedInput(method, inputMessage);
            } else {
                processInput(method, inputMessage);
            }
        }
        if (outputMessage == null) {
            processReturn(method, null);
        } else {
            if (method.isWrapperStyle()) {
                processWrappedOutput(method, inputMessage, outputMessage);
            } else {
                processOutput(method, inputMessage, outputMessage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void buildParamModelsWithOrdering(JavaMethod method,
                                              MessageInfo inputMessage,
                                              MessageInfo outputMessage,
                                              List<String> parameterList) throws ToolException {

        Map<QName, MessagePartInfo> inputPartsMap = inputMessage.getMessagePartsMap();
        
        Map<QName, MessagePartInfo> outputPartsMap = outputMessage.getMessagePartsMap();

        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
        List<MessagePartInfo> outputParts = outputMessage.getMessageParts();

        List<MessagePartInfo> inputUnlistedParts = new ArrayList<MessagePartInfo>();
        List<MessagePartInfo> outputUnlistedParts = new ArrayList<MessagePartInfo>();

        for (MessagePartInfo part : inputParts) {
            if (!parameterList.contains(part.getName().getLocalPart())) {
                inputUnlistedParts.add(part);
            }
        }

        if (isRequestResponse(method)) {
            for (MessagePartInfo part : outputParts) {
                if (!parameterList.contains(part.getName().getLocalPart())) {
                    MessagePartInfo inpart = inputMessage.getMessagePart(part.getName());
                    if (inpart == null || (inpart != null && !isSamePart(inpart, part))) {
                        outputUnlistedParts.add(part);
                    }
                }
            }

            if (outputUnlistedParts.size() == 1) {
                processReturn(method, outputUnlistedParts.get(0));
                outputPartsMap.remove(outputUnlistedParts.get(0));
                outputUnlistedParts.clear();
            } else {
                processReturn(method, null);
            }
        }

        // now create list of paramModel with parts
        // first for the ordered list
        int index = 0;
        int size = parameterList.size();
        while (index < size) {
            String partName = parameterList.get(index);
            MessagePartInfo part = inputPartsMap.get(inputMessage.getMessagePartQName(partName));
            JavaType.Style style = JavaType.Style.IN;
            if (part == null) {
                part = outputPartsMap.get(inputMessage.getMessagePartQName(partName));
                style = JavaType.Style.OUT;
            } else if (outputPartsMap.get(inputMessage.getMessagePartQName(partName)) != null 
                && isSamePart(part, outputPartsMap.get(inputMessage.getMessagePartQName(partName)))) {
                style = JavaType.Style.INOUT;
            }
            if (part != null) {
                addParameter(method, getParameterFromPart(part, style));
            }
            index++;
        }
        // now from unlisted input parts
        for (MessagePartInfo part : inputUnlistedParts) {
            addParameter(method, getParameterFromPart(part, JavaType.Style.IN));
        }
        // now from unlisted output parts
        for (MessagePartInfo part : outputUnlistedParts) {
            addParameter(method, getParameterFromPart(part, JavaType.Style.INOUT));
        }
    }

    private boolean isSamePart(MessagePartInfo part1, MessagePartInfo part2) {
        QName qname1 = part1.getElementQName();
        QName qname2 = part2.getElementQName();
        if (qname1 != null && qname2 != null) {
            return qname1.equals(qname2);
        }
        qname1 = part1.getTypeQName();
        qname2 = part2.getTypeQName();
        if (qname1 != null && qname2 != null) {
            return qname1.equals(qname2);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isValidOrdering(List<String> parameterOrder, 
                                    MessageInfo inputMessage, MessageInfo outputMessage) {
        Iterator<String> params = parameterOrder.iterator();

        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
        List<MessagePartInfo> outputParts = outputMessage.getMessageParts();

        boolean partFound = false;

        while (params.hasNext()) {
            String param = params.next();
            partFound = false;
            for (MessagePartInfo part : inputParts) {
                if (param.equals(part.getName().getLocalPart())) {
                    partFound = true;
                    break;
                }
            }
            // if not found, check output parts
            if (!partFound) {
                for (MessagePartInfo part : outputParts) {
                    if (param.equals(part.getName().getLocalPart())) {
                        partFound = true;
                        break;
                    }
                }
            }
            if (!partFound) {
                break;
            }
        }
        return partFound;
    }
}

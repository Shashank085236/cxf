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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaAnnotation;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaReturn;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customiztion.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.SoapBindingAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebMethodAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebResultAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WrapperAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper.MethodMapper;

public class OperationProcessor  extends AbstractProcessor {
   
    private JavaParameter wrapperRequest;
    private JavaParameter wrapperResponse;   
    public OperationProcessor(ToolContext c) {
        super(c);
    }

    @SuppressWarnings("unchecked")
    public void process(JavaInterface intf, OperationInfo operation) throws ToolException {
        JavaMethod method = new MethodMapper().map(operation);
        method.setInterface(intf);             
        processMethod(method, operation, null);
        Collection<FaultInfo> faults = operation.getFaults();
        FaultProcessor faultProcessor = new FaultProcessor(context);
        faultProcessor.process(method, faults);
        intf.addMethod(method);
    }

    @SuppressWarnings("unchecked")
    void processMethod(JavaMethod method, OperationInfo operation, 
                              JAXWSBinding globalBinding) throws ToolException {
        if (isAsynCMethod(method)) {
            return;
        }
        MessageInfo inputMessage = operation.getInput();
        MessageInfo outputMessage = operation.getOutput();

        if (inputMessage == null) {
            LOG.log(Level.WARNING, "NO_INPUT_MESSAGE", new Object[] {operation.getName()});
            org.apache.cxf.common.i18n.Message msg 
                = new org.apache.cxf.common.i18n.Message("INVALID_MEP", LOG,
                                                               new Object[] {operation.getName()});
            throw new ToolException(msg);
        }

        ParameterProcessor paramProcessor = new ParameterProcessor(context);
        method.clear();
        paramProcessor.process(method,
                               inputMessage,
                               outputMessage,
                               operation.getParameterOrdering());

        method.annotate(new WebMethodAnnotator());
        
        if (method.isWrapperStyle()) {
            setWrapper(operation);
            method.annotate(new WrapperAnnotator(wrapperRequest, wrapperResponse));
        }

        method.annotate(new WebResultAnnotator());
        method.annotate(new SoapBindingAnnotator());
        
        JAXWSBinding opBinding = (JAXWSBinding)operation.getExtensor(JAXWSBinding.class);
        
        boolean enableAsync = false;
        if (globalBinding != null && globalBinding.isEnableAsyncMapping() 
            || opBinding != null && opBinding.isEnableAsyncMapping()) {
            enableAsync = true;
        }
        
               
        if (!method.isOneWay() 
            && enableAsync && !isAddedAsyMethod(method)) {
            addAsyncMethod(method);
        }
        
        if (globalBinding != null && globalBinding.isEnableMime() 
            || opBinding != null && opBinding.isEnableMime()) {
            method.setMimeEnable(true);
        }
    }


    private void setWrapper(OperationInfo operation) {
        MessagePartInfo inputPart = null;
        if (operation.getInput() != null && operation.getInput().getMessageParts() != null) {
            inputPart = operation.getInput().getMessageParts().iterator().next();
        }
        MessagePartInfo outputPart = null;
        if (operation.getOutput() != null && operation.getOutput().getMessageParts() != null) {
            outputPart = operation.getOutput().getMessageParts().iterator().next();
        }
        
        if (inputPart != null) {
            wrapperRequest = new JavaParameter();
            wrapperRequest.setName(ProcessorUtil.resolvePartName(inputPart));
            wrapperRequest.setType(ProcessorUtil.getPartType(inputPart));
            wrapperRequest.setTargetNamespace(ProcessorUtil.resolvePartNamespace(inputPart));

            wrapperRequest.setClassName(ProcessorUtil.getFullClzName(inputPart, 
                                                                     context, false));

        }
        if (outputPart != null) {
            wrapperResponse = new JavaParameter();
            wrapperResponse.setName(ProcessorUtil.resolvePartName(outputPart));
            wrapperResponse.setType(ProcessorUtil.getPartType(outputPart));
            wrapperResponse.setTargetNamespace(ProcessorUtil.resolvePartNamespace(outputPart));

            wrapperResponse.setClassName(ProcessorUtil.getFullClzName(outputPart,
                                                                      context, false));
        }
    }

    private boolean isAsynCMethod(JavaMethod method) {
        if (method.getName().toLowerCase()
            .equals((method.getOperationName() + ToolConstants.ASYNC_METHOD_SUFFIX).toLowerCase())) {
            return true;
        }
        return false;
    }
    
    private void addAsyncMethod(JavaMethod method) throws ToolException {
        addPollingMethod(method);
        addCallbackMethod(method);
        method.getInterface().addImport("javax.xml.ws.AsyncHandler");
        method.getInterface().addImport("java.util.concurrent.Future");
        method.getInterface().addImport("javax.xml.ws.Response");
    }

    private void addPollingMethod(JavaMethod method) throws ToolException {
        JavaMethod pollingMethod = new JavaMethod(method.getInterface());
        pollingMethod.setName(method.getName() + ToolConstants.ASYNC_METHOD_SUFFIX);
        pollingMethod.setStyle(method.getStyle());
        pollingMethod.setWrapperStyle(method.isWrapperStyle());
        pollingMethod.setSoapAction(method.getSoapAction());
        pollingMethod.setOperationName(method.getOperationName());

        JavaReturn future = new JavaReturn();
        future.setClassName("Future<?>");
        pollingMethod.setReturn(future);

        // REVISIT: test the operation name in the annotation
        pollingMethod.annotate(new WebMethodAnnotator());
        pollingMethod.addAnnotation("ResponseWrapper", method.getAnnotationMap().get("ResponseWrapper"));
        pollingMethod.addAnnotation("RequestWrapper", method.getAnnotationMap().get("RequestWrapper"));
        pollingMethod.addAnnotation("SOAPBinding", method.getAnnotationMap().get("SOAPBinding"));

        for (Iterator iter = method.getParameters().iterator(); iter.hasNext();) {
            pollingMethod.addParameter((JavaParameter)iter.next());
        }

        JavaParameter asyncHandler = new JavaParameter();
        asyncHandler.setName("asyncHandler");
        asyncHandler.setClassName(getAsyncClassName(method, "AsyncHandler"));
        JavaAnnotation asyncHandlerAnnotation = new JavaAnnotation("WebParam");
        asyncHandlerAnnotation.addArgument("name", "asyncHandler");
        asyncHandlerAnnotation.addArgument("targetNamespace", "");
        asyncHandler.setAnnotation(asyncHandlerAnnotation);

        pollingMethod.addParameter(asyncHandler);

        method.getInterface().addMethod(pollingMethod);
    }

    private void addCallbackMethod(JavaMethod method) throws ToolException {
        JavaMethod callbackMethod = new JavaMethod(method.getInterface());
        callbackMethod.setName(method.getName() + ToolConstants.ASYNC_METHOD_SUFFIX);
        callbackMethod.setStyle(method.getStyle());
        callbackMethod.setWrapperStyle(method.isWrapperStyle());
        callbackMethod.setSoapAction(method.getSoapAction());
        callbackMethod.setOperationName(method.getOperationName());
        
        JavaReturn response = new JavaReturn();
        response.setClassName(getAsyncClassName(method, "Response"));
        callbackMethod.setReturn(response);

        // REVISIT: test the operation name in the annotation
        callbackMethod.annotate(new WebMethodAnnotator());
        callbackMethod.addAnnotation("RequestWrapper", method.getAnnotationMap().get("RequestWrapper"));
        callbackMethod.addAnnotation("ResponseWrapper", method.getAnnotationMap().get("ResponseWrapper"));
        callbackMethod.addAnnotation("SOAPBinding", method.getAnnotationMap().get("SOAPBinding"));

        for (Iterator iter = method.getParameters().iterator(); iter.hasNext();) {
            callbackMethod.addParameter((JavaParameter)iter.next());
        }

        method.getInterface().addMethod(callbackMethod);
    }

    private String getAsyncClassName(JavaMethod method, String clzName) {
        String response;
        if (wrapperResponse != null) {
            response = wrapperResponse.getClassName();
        } else {
            response = method.getReturn().getClassName();
        }

        StringBuffer sb = new StringBuffer();
        sb.append(clzName);
        sb.append("<");
        sb.append(response);
        sb.append(">");
        return sb.toString();
    }
    
    private boolean isAddedAsyMethod(JavaMethod method) {
        List<JavaMethod> jmethods = method.getInterface().getMethods();
        for (JavaMethod jm : jmethods) {
            if (!jm.getName().toLowerCase().equals(method.getOperationName().toLowerCase()) 
                && jm.getOperationName().toLowerCase().equals(method.getOperationName().toLowerCase())) {
                return  true;
                
            }
        }
        return false;
    }
}

package org.objectweb.celtix.tools.generators.java2;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.soap.SOAPBinding;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;

import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.tools.common.ToolException;
import org.objectweb.celtix.tools.common.WSDLConstants;
import org.objectweb.celtix.tools.common.model.JavaMethod;
import org.objectweb.celtix.tools.common.model.JavaParameter;
import org.objectweb.celtix.tools.common.model.WSDLModel;
import org.objectweb.celtix.tools.common.model.WSDLParameter;
import org.objectweb.celtix.tools.processors.java2.JavaToWSDLProcessor;

public class BindingGenerator {
    private static final Logger LOG = LogUtils.getL7dLogger(JavaToWSDLProcessor.class);
    private WSDLModel wmodel;
    private Definition definition;
    private ExtensionRegistry extensionRegistry;

    public BindingGenerator(WSDLModel model) {
        this.wmodel = model;
        definition = model.getDefinition();
        extensionRegistry = definition.getExtensionRegistry();
    }

    public void generate() {
        Binding binding = definition.createBinding();

        binding.setQName(new QName(WSDLConstants.NS_WSDL, wmodel.getPortTypeName() + "Binding"));
        binding.setPortType(definition.getPortType(new QName(wmodel.getTargetNameSpace(), wmodel
            .getPortTypeName())));

        // genearte the soap binding

        javax.wsdl.extensions.soap.SOAPBinding soapBinding;
        try {
            soapBinding = (javax.wsdl.extensions.soap.SOAPBinding)extensionRegistry
                .createExtension(Binding.class, new QName(WSDLConstants.SOAP11_NAMESPACE, "binding"));
            soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
            soapBinding.setStyle(wmodel.getStyle().toString().toLowerCase());
            binding.addExtensibilityElement(soapBinding);
        } catch (WSDLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        generateBindingOperation(binding);
        binding.setUndefined(false);
        definition.addBinding(binding);

    }

    private void generateBindingOperation(Binding binding) {
        for (JavaMethod jmethod : wmodel.getJavaMethods()) {
            BindingOperation bindOperation = definition.createBindingOperation();
            bindOperation.setName(jmethod.getName());
            generateBindingOperationInputOutPut(bindOperation, jmethod);
            binding.addBindingOperation(bindOperation);
        }

    }

    private void generateBindingOperationInputOutPut(BindingOperation operation, JavaMethod jmethod) {
        // generate soap binding action
        SOAPOperation soapOperation = generateSoapAction();
        soapOperation.setStyle(jmethod.getSoapStyle().name().toLowerCase());
        soapOperation.setSoapActionURI(jmethod.getSoapAction());
        operation.addExtensibilityElement(soapOperation);

        generateInputSoapBody(jmethod, operation, jmethod.getRequest());

        generateOutputSoapBody(jmethod, operation, jmethod.getResponse());

        for (org.objectweb.celtix.tools.common.model.WSDLException ex : jmethod.getWSDLExceptions()) {

            BindingFault bindingFault = definition.createBindingFault();
            bindingFault.setName(ex.getExcpetionClass().getSimpleName());
            operation.addBindingFault(bindingFault);
            javax.wsdl.extensions.soap.SOAPFault soapFault = null;
            try {
                soapFault = (javax.wsdl.extensions.soap.SOAPFault)extensionRegistry
                    .createExtension(BindingFault.class, new QName(WSDLConstants.SOAP11_NAMESPACE, "fault"));
                soapFault.setUse("literal");
                soapFault.setName(ex.getExcpetionClass().getSimpleName());
            } catch (WSDLException e) {
                throw new ToolException(e.getMessage(), e);
            }
            bindingFault.addExtensibilityElement(soapFault);

        }

    }

    private SOAPOperation generateSoapAction() {
        SOAPOperation soapOperation = null;
        try {
            soapOperation = (SOAPOperation)extensionRegistry
                .createExtension(BindingOperation.class, new QName(WSDLConstants.SOAP11_NAMESPACE,
                                                                   "operation"));
        } catch (WSDLException e) {
            throw new ToolException(e.getMessage(), e);
        }

        return soapOperation;
    }

    private void generateOutputSoapBody(JavaMethod jmethod, BindingOperation operation, WSDLParameter param) {
        if (param == null) {
            return;
        }

        SOAPBody body = null;

        BindingOutput bindingOutput = definition.createBindingOutput();
        bindingOutput.setName(param.getName());

        operation.setBindingOutput(bindingOutput);

        try {
            body = (SOAPBody)extensionRegistry.createExtension(BindingOutput.class,
                                                               new QName(WSDLConstants.SOAP11_NAMESPACE,
                                                                         "body"));
        } catch (WSDLException e1) {
            throw new ToolException(e1.getMessage(), e1);
        }

        if (jmethod.getSoapUse() == SOAPBinding.Use.LITERAL) {
            body.setUse("literal");
        } else {
            Message msg = new Message("ENCODED_USE_NOT_SUPPORTED", LOG);
            throw new ToolException(msg);
        }

        List<JavaParameter> bodyParams = new ArrayList<JavaParameter>();
        List<JavaParameter> headerParams = new ArrayList<JavaParameter>();

        splitSoapHeaderBodyParams(param, bodyParams, headerParams);
        // if exists soap header,then generate soap body parts

        if (headerParams.size() > 0) {
            List<String> parts = new ArrayList<String>();
            for (JavaParameter parameter : bodyParams) {
                parts.add(parameter.getPartName());
            }
            body.setParts(parts);
            SOAPHeader soapHeader = null;
            for (JavaParameter jp : headerParams) {

                try {
                    soapHeader = (SOAPHeader)extensionRegistry
                        .createExtension(BindingOutput.class, new QName(WSDLConstants.SOAP11_NAMESPACE,
                                                                        "header"));
                    soapHeader.setMessage(new QName(param.getTargetNamespace(), param.getName()));
                    soapHeader.setPart(jp.getPartName());
                    soapHeader.setUse("literal");

                } catch (WSDLException e) {
                    throw new ToolException(e.getMessage(), e);
                }
            }

            if (jmethod.getSoapStyle() == SOAPBinding.Style.RPC) {
                body.setNamespaceURI(param.getTargetNamespace());
            }
            bindingOutput.addExtensibilityElement(soapHeader);

        }
        bindingOutput.addExtensibilityElement(body);

    }

    private void generateInputSoapBody(JavaMethod jmethod, BindingOperation operation, WSDLParameter param) {
        if (param == null) {
            return;
        }
        SOAPBody body = null;

        BindingInput bindingInput = definition.createBindingInput();
        bindingInput.setName(param.getName());

        operation.setBindingInput(bindingInput);

        try {
            body = (SOAPBody)extensionRegistry.createExtension(BindingInput.class,
                                                               new QName(WSDLConstants.SOAP11_NAMESPACE,
                                                                         "body"));
        } catch (WSDLException e1) {
            throw new ToolException(e1.getMessage(), e1);
        }

        if (jmethod.getSoapUse() == SOAPBinding.Use.LITERAL) {
            body.setUse("literal");
        } else {
            Message msg = new Message("ENCODED_USE_NOT_SUPPORTED", LOG);
            throw new ToolException(msg);
        }

        List<JavaParameter> bodyParams = new ArrayList<JavaParameter>();
        List<JavaParameter> headerParams = new ArrayList<JavaParameter>();

        splitSoapHeaderBodyParams(param, bodyParams, headerParams);

        // if exists soap header,then generate soap body parts

        if (headerParams.size() > 0) {
            List<String> parts = new ArrayList<String>();
            for (JavaParameter parameter : bodyParams) {
                parts.add(parameter.getPartName());
            }
            body.setParts(parts);
            SOAPHeader soapHeader = null;
            for (JavaParameter jp : headerParams) {

                try {
                    soapHeader = (SOAPHeader)extensionRegistry
                        .createExtension(BindingInput.class, new QName(WSDLConstants.SOAP11_NAMESPACE,
                                                                       "header"));

                    soapHeader.setMessage(new QName(param.getTargetNamespace(), param.getName()));
                    soapHeader.setPart(jp.getPartName());
                    soapHeader.setUse("literal");

                } catch (WSDLException e) {
                    throw new ToolException(e.getMessage(), e);
                }
            }

            if (jmethod.getSoapStyle() == SOAPBinding.Style.RPC) {
                body.setNamespaceURI(param.getTargetNamespace());
            }
            bindingInput.addExtensibilityElement(soapHeader);

        }
        bindingInput.addExtensibilityElement(body);

    }
    
    
    private void splitSoapHeaderBodyParams(WSDLParameter param, List<JavaParameter> bodyList,
                                           List<JavaParameter> headerList) {
        for (JavaParameter jpara : param.getChildren()) {
            if (jpara.isHeader()) {
                headerList.add(jpara);
            } else {
                bodyList.add(jpara);
            }
        }

    }

}

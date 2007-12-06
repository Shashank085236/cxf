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

package org.apache.cxf.wsdl11;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.AttributeExtensible;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


import com.ibm.wsdl.extensions.schema.SchemaImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;

/**
 * Consume a set of service definitions and produce a WSDL model. The ServiceInfo objects
 * contain the bindings, operations, and ports, plus XMLSchema schemas. 
 * 
 * Each wsdl:definition has to have a single target namespace. The first service in the list
 * defines the TNS of the overall WSDL. If a subsequent service has a divergent TNS, then
 * the code creates a new definition element (i.e., Definition object), and imports it into
 * the top-level object.
 */
public class ServiceWSDLBuilder {
    
    private final Map<String, String> ns2prefix;
    private Definition definition;
    private final List<ServiceInfo> services;
    private boolean useSchemaImports;
    private String baseFileName;
    private int xsdCount;
    private final Bus bus;
    
    /**
     * Sets up the builder on a bus with a list of services.
     * @param b the bus.
     * @param services the services.
     */
    public ServiceWSDLBuilder(Bus b, List<ServiceInfo> services) {
        this.services = services;
        bus = b;
        ns2prefix = new HashMap<String, String>();
    }
    
    /**
     * For callers who prefer varargs, an inline list of ServiceInfo objects instead of 
     * a List. Primarily used for tests or other callers with only one service in hand. 
     * @param b the bus.
     * @param services the services.
     */
    public ServiceWSDLBuilder(Bus b, ServiceInfo ... services) {
        this(b, Arrays.asList(services));
    }
    
    /**
     * Set whether to emit references to imported schema files.
     * This is only effective for {@link #build(Map)}, which is passed additional schemas for 
     * import. {@link #build()} resets this flag to false.
     * @param b true to use imports.
     */
    public void setUseSchemaImports(boolean b) {
        useSchemaImports = b;
    }
    
    /**
     * Base filename for imported files.
     * @param s pathname.
     */
    public void setBaseFileName(String s) {
        baseFileName = s;
    }
    
    /**
     * Create the WSDL Definition object and return it. This function will never create
     * imports to schemas.
     * @return the WSDL definition.
     * @throws WSDLException
     */
    public Definition build() throws WSDLException {
        useSchemaImports = false;
        return build(null);
    }
    
    /**
     * Create the WSDL Definition object and return it. This function respects the 
     * setting of {@link #setUseSchemaImports(boolean)}.
     * @param imports A set of schema imports to either reference as imports or read and 
     * then inline.
     * @return the WSDL definition
     * @throws WSDLException
     */
    public Definition build(Map<String, SchemaInfo> imports) throws WSDLException {
        try {
            definition = services.get(0).getProperty(WSDLServiceBuilder.WSDL_DEFINITION, Definition.class);
        } catch (ClassCastException e) {
            //ignore
        }
        if (definition == null) {
            ServiceInfo si = services.get(0);
            definition = newDefinition(si.getName(), si.getTargetNamespace());
            addNamespace(WSDLConstants.CONVENTIONAL_TNS_PREFIX, si.getTargetNamespace(), definition);
            addExtensibilityElements(definition, getWSDL11Extensors(si));

            Collection<PortType> portTypes = new HashSet<PortType>();
            for (ServiceInfo service : services) {
                Definition portTypeDef = definition;
                if (!isSameTNS(service)) {
                    portTypeDef = newDefinition(service.getInterface().getName(),
                                                service.getInterface().getName().getNamespaceURI());
                    Import wsdlImport = definition.createImport();
                    String tns = service.getInterface().getName().getNamespaceURI();
                    wsdlImport.setDefinition(portTypeDef);
                    wsdlImport.setNamespaceURI(tns);
                    wsdlImport.setLocationURI(service.getInterface().getName().getLocalPart() + ".wsdl");
                    definition.addImport(wsdlImport);
                    addNamespace(getPrefix(tns), tns, definition);
                }
                portTypes.add(buildPortType(service.getInterface(), portTypeDef));
                
                if (service.getSchemas() != null && service.getSchemas().size() > 0) {
                    buildTypes(service.getSchemas(), imports, portTypeDef);
                }
            }
            
            for (ServiceInfo service : services) {
                buildBinding(service.getBindings(), portTypes);
                buildService(service);
            }
        }
        return definition;
    }

    private boolean isSameTNS(final ServiceInfo service) {
        return service.getName().getNamespaceURI().equals(service.getInterface().getName().getNamespaceURI());
    }

    private Definition newDefinition(final QName name, String targetNamespace) {
        Definition d = bus.getExtension(WSDLManager.class).getWSDLFactory().newDefinition();
        d.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtenstionRegistry());
        d.setQName(name);
        d.setTargetNamespace(targetNamespace);
        addNamespace(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD, d);
        return d;
    }

    /** 
     * Return a list of ExtensibilityElements for a particular component, such as a BindingFaultInfo.
     * This perhaps should be protected.
     * @param holder The item containing the extensibility elements.
     * @return the extensibility elements.
     */
    public List<ExtensibilityElement> getWSDL11Extensors(AbstractPropertiesHolder holder) {
        return holder.getExtensors(ExtensibilityElement.class);
    }
    
    protected void addExtensibilityElements(ElementExtensible elementExtensible, 
        List<ExtensibilityElement> extensibilityElements) {
        if (extensibilityElements != null) {
            for (ExtensibilityElement element : extensibilityElements) {
                QName qn = element.getElementType();
                addNamespace(qn.getNamespaceURI());
                elementExtensible.addExtensibilityElement(element);
            }
        }
    }
    
    private void addExtensibilityAttributes(AttributeExtensible attributeExtensible, 
                                            Map<QName, Object> attributes) {
        if (attributes == null) {
            return;
        }
        for (QName qname : attributes.keySet()) {
            addNamespace(qname.getNamespaceURI());
            attributeExtensible.setExtensionAttribute(qname, attributes.get(qname));
        }
    }
    
    /**
     * For a schema, require all namespace with prefixes to be imported. In theory,
     * if a namespace has a prefix but is not used, the import is not needed, and could
     * cause a problem. In practice, the code generating these schemas should not be adding
     * spurious prefixes. Hypothetically, we could check that, in the overall WSDL, that
     * all of the schemas that we are importing are accounted for. However, that's a validation
     * that is best left to the validator.
     * @param schemaInfo Schema to process.
     */
    protected void addRequiredSchemaImports(SchemaInfo schemaInfo) {
        Element schemaElement = schemaInfo.getElement();
        // we need an import for every namespace except the main one.
        String schemaNamespace = schemaInfo.getNamespaceURI();
        Map<String, String> queryPrefixMap = new HashMap<String, String>();
        queryPrefixMap.put("xs", WSDLConstants.NS_SCHEMA_XSD);
        XPathUtils xpu = new XPathUtils(queryPrefixMap);
        NamespacePrefixList schemaPrefixes = schemaInfo.getSchema().getNamespaceContext();
        for (String prefix : schemaPrefixes.getDeclaredPrefixes()) {
            String namespace = schemaPrefixes.getNamespaceURI(prefix);
            if (!namespace.equals(schemaNamespace) 
                && !namespace.equals(WSDLConstants.NS_SCHEMA_XSD)
                && !xpu.isExist("xs:import[@namespace='" + namespace + "']", 
                                 schemaElement, 
                                 XPathConstants.NODE)) {
                Element importElement = XMLUtils.createElementNS(schemaElement.getOwnerDocument(), 
                                                                 WSDLConstants.NS_SCHEMA_XSD,
                                                                 "import");
                importElement.setAttribute("namespace", namespace);
                schemaElement.insertBefore(importElement, schemaElement.getFirstChild());
            }
        }
    }

    protected void buildTypes(final Collection<SchemaInfo> schemas,
                              final Map<String, SchemaInfo> imports,
                              final Definition def) {
        Types types = def.createTypes();
        
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("DOM configuration problem", e);
        }
        Element nd = XMLUtils.createElementNS(doc, new QName(WSDLConstants.NS_SCHEMA_XSD,
                                                             "schema"));
        nd.setAttribute("xmlns", WSDLConstants.NS_SCHEMA_XSD);
        doc.appendChild(nd);
        
        for (SchemaInfo schemaInfo : schemas) {
            
            if (!useSchemaImports) {
                SchemaImpl schemaImpl = new SchemaImpl();
                schemaImpl.setRequired(true);
                schemaImpl.setElementType(WSDLConstants.QNAME_SCHEMA);
                schemaImpl.setElement(schemaInfo.getElement());
                types.addExtensibilityElement(schemaImpl);
            } else {
                //imports
                String name = baseFileName + "_schema" + (++xsdCount) + ".xsd";
                Element imp = XMLUtils.createElementNS(doc, 
                                                       new QName(WSDLConstants.NS_SCHEMA_XSD,
                                                                  "import"));
                imp.setAttribute("schemaLocation", name);
                imp.setAttribute("namespace", schemaInfo.getNamespaceURI());
                nd.appendChild(imp);
                               
                imports.put(name, schemaInfo);
            }
            // add imports for those schemata which are inlined.
            addRequiredSchemaImports(schemaInfo);
        }
        if (useSchemaImports) {
            SchemaImpl schemaImpl = new SchemaImpl();
            schemaImpl.setRequired(true);
            schemaImpl.setElementType(WSDLConstants.QNAME_SCHEMA);
            schemaImpl.setElement(nd);
            types.addExtensibilityElement(schemaImpl);
        }
        def.setTypes(types);
    }

    protected void buildBinding(Collection<BindingInfo> bindingInfos, Collection<PortType> portTypes) {
        Binding binding = null;
        for (BindingInfo bindingInfo : bindingInfos) {
            binding = definition.createBinding();
            binding.setUndefined(false);
            for (PortType portType : portTypes) {
                if (portType.getQName().equals(bindingInfo.getInterface().getName())) {
                    binding.setPortType(portType);
                    break;
                }
            }
            binding.setQName(bindingInfo.getName());
            buildBindingOperation(definition, binding, bindingInfo.getOperations());
            addExtensibilityElements(binding, getWSDL11Extensors(bindingInfo));
            definition.addBinding(binding);
        }
    }

    protected void buildBindingOperation(Definition def, Binding binding, 
                                       Collection<BindingOperationInfo> bindingOperationInfos) {
        BindingOperation bindingOperation = null;
        for (BindingOperationInfo bindingOperationInfo : bindingOperationInfos) {
            bindingOperation = def.createBindingOperation();
            bindingOperation.setName(bindingOperationInfo.getName().getLocalPart());
            for (Operation operation 
                    : CastUtils.cast(binding.getPortType().getOperations(), Operation.class)) {
                if (operation.getName().equals(bindingOperation.getName())) {
                    bindingOperation.setOperation(operation);
                    break;
                }
            }
            buildBindingInput(def, bindingOperation, bindingOperationInfo.getInput());
            buildBindingOutput(def, bindingOperation, bindingOperationInfo.getOutput());
            buildBindingFault(def, bindingOperation, bindingOperationInfo.getFaults());
            addExtensibilityElements(bindingOperation, getWSDL11Extensors(bindingOperationInfo));
            binding.addBindingOperation(bindingOperation);
        }
    }

    protected void buildBindingFault(Definition def, BindingOperation bindingOperation, 
                                   Collection<BindingFaultInfo> bindingFaultInfos) {
        BindingFault bindingFault = null;
        for (BindingFaultInfo bindingFaultInfo 
            : bindingFaultInfos) {
            bindingFault = def.createBindingFault();
            bindingFault.setName(bindingFaultInfo.getFaultInfo().getFaultName().getLocalPart());
            bindingOperation.addBindingFault(bindingFault);
            addExtensibilityElements(bindingFault, getWSDL11Extensors(bindingFaultInfo));
        }
        
    }

    protected void buildBindingInput(Definition def, BindingOperation bindingOperation, 
                                         BindingMessageInfo bindingMessageInfo) {
        BindingInput bindingInput = null;
        if (bindingMessageInfo != null) {
            bindingInput = def.createBindingInput();
            bindingInput.setName(bindingMessageInfo.getMessageInfo().getName().getLocalPart());
            bindingOperation.setBindingInput(bindingInput);
            addExtensibilityElements(bindingInput, getWSDL11Extensors(bindingMessageInfo));
        }
    }
    
    protected void buildBindingOutput(Definition def, BindingOperation bindingOperation, 
                                   BindingMessageInfo bindingMessageInfo) {
        BindingOutput bindingOutput = null;
        if (bindingMessageInfo != null) {
            bindingOutput = def.createBindingOutput();
            bindingOutput.setName(bindingMessageInfo.getMessageInfo().getName().getLocalPart());
            bindingOperation.setBindingOutput(bindingOutput);
            addExtensibilityElements(bindingOutput, getWSDL11Extensors(bindingMessageInfo));
        }
    }

    protected void buildService(ServiceInfo serviceInfo) {
        Service serv = definition.createService();
        serv.setQName(serviceInfo.getName());
        addNamespace(serviceInfo.getName().getNamespaceURI());
        definition.addService(serv);

        for (EndpointInfo ei : serviceInfo.getEndpoints()) {
            addNamespace(ei.getTransportId());
            Port port = definition.createPort();
            port.setName(ei.getName().getLocalPart());
            port.setBinding(definition.getBinding(ei.getBinding().getName()));
            addExtensibilityElements(port, getWSDL11Extensors(ei));
            serv.addPort(port);
        }
    }

    protected PortType buildPortType(InterfaceInfo intf, final Definition def) {
        PortType portType = null;
        try {
            portType = intf.getProperty(WSDLServiceBuilder.WSDL_PORTTYPE, PortType.class);
        } catch (ClassCastException e) {
            // do nothing
        }
        
        if (portType == null) {
            portType = def.createPortType();
            portType.setQName(intf.getName());
            addNamespace(intf.getName().getNamespaceURI(), def);
            portType.setUndefined(false);
            buildPortTypeOperation(portType, intf.getOperations(), def);
        }

        def.addPortType(portType);
        return portType;
    }

    protected void addNamespace(String namespaceURI, Definition def) {
        addNamespace(getPrefix(namespaceURI), namespaceURI, def);
    }

    protected void addNamespace(String namespaceURI) {
        addNamespace(getPrefix(namespaceURI), namespaceURI);
    }

    protected void addNamespace(String prefix, String namespaceURI) {
        addNamespace(prefix, namespaceURI, definition);
    }

    protected void addNamespace(String prefix, String namespaceURI, Definition def) {
        ns2prefix.put(namespaceURI, prefix);
        def.addNamespace(prefix, namespaceURI);
    }
    
    private OperationInfo getOperationInfo(OperationInfo operation) {
        if (operation.getUnwrappedOperation() != null) {
            return operation.getUnwrappedOperation();
        }
        return operation;
    }
    
    private Map<QName, Object> getInputExtensionAttributes(OperationInfo operation) {
        return getOperationInfo(operation).getInput().getExtensionAttributes();
    }
    
    private Map<QName, Object> getOutputExtensionAttributes(OperationInfo operation) {
        return getOperationInfo(operation).getOutput().getExtensionAttributes();
    }    

    protected void buildPortTypeOperation(PortType portType,
                                          Collection<OperationInfo> operationInfos,
                                          final Definition def) {
        for (OperationInfo operationInfo : operationInfos) {
            Operation operation = null;
            try {
                operation = operationInfo.getProperty(
                    WSDLServiceBuilder.WSDL_OPERATION, Operation.class);
            } catch (ClassCastException e) {
                // do nothing
            }
            
            if (operation == null) {
                operation = def.createOperation();
                operation.setUndefined(false);
                operation.setName(operationInfo.getName().getLocalPart());
                addNamespace(operationInfo.getName().getNamespaceURI(), def);
                if (operationInfo.isOneWay()) {
                    operation.setStyle(OperationType.ONE_WAY);
                }
                Input input = def.createInput();
                input.setName(operationInfo.getInputName());
                Message message = def.createMessage();
                buildMessage(message, operationInfo.getInput(), def);
                this.addExtensibilityAttributes(input, getInputExtensionAttributes(operationInfo));
                input.setMessage(message);
                operation.setInput(input);
                
                if (operationInfo.getOutput() != null) {
                    Output output = def.createOutput();
                    output.setName(operationInfo.getOutputName());
                    message = def.createMessage();
                    buildMessage(message, operationInfo.getOutput(), def);
                    this.addExtensibilityAttributes(output, getOutputExtensionAttributes(operationInfo));
                    output.setMessage(message);
                    operation.setOutput(output);
                }
                //loop to add fault
                Collection<FaultInfo> faults = operationInfo.getFaults();
                Fault fault = null;
                for (FaultInfo faultInfo : faults) {
                    fault = def.createFault();
                    fault.setName(faultInfo.getFaultName().getLocalPart());
                    message = def.createMessage();
                    buildMessage(message, faultInfo, def);
                    this.addExtensibilityAttributes(fault, faultInfo.getExtensionAttributes());
                    fault.setMessage(message);
                    operation.addFault(fault);
                }
            }
            portType.addOperation(operation);
        }
    }

    private String getPrefix(String ns) {
        for (String namespace : WSDLConstants.NS_PREFIX_PAIR.keySet()) {
            if (namespace.equals(ns)) {
                return WSDLConstants.NS_PREFIX_PAIR.get(namespace);
            }
        }
        String prefix = ns2prefix.get(ns);
        if (prefix == null) {
            prefix = getNewPrefix();
            ns2prefix.put(ns, prefix);
        }
        return prefix;
    }
    
    private String getNewPrefix() {
        String prefix = "ns1";
        int i = 0;
        while (ns2prefix.containsValue(prefix)) {
            i++;
            prefix = "ns" + i;
        }
        return prefix;
    }

    protected void buildMessage(Message message,
                                AbstractMessageContainer messageContainer,
                                final Definition def) {
                
        message.setQName(messageContainer.getName());
        message.setUndefined(false);
        def.addMessage(message);
        
        List<MessagePartInfo> messageParts = messageContainer.getMessageParts();
        Part messagePart = null;
        for (MessagePartInfo messagePartInfo : messageParts) {
            messagePart = def.createPart();
            messagePart.setName(messagePartInfo.getName().getLocalPart());
            if (messagePartInfo.isElement()) {
                messagePart.setElementName(messagePartInfo.getElementQName());
                addNamespace(messagePartInfo.getElementQName().getNamespaceURI(), def);
            } else if (messagePartInfo.getTypeQName() != null) {
                messagePart.setTypeName(messagePartInfo.getTypeQName());
                addNamespace(messagePartInfo.getTypeQName().getNamespaceURI(), def);
            }
            message.addPart(messagePart);
        }
    }
          
}

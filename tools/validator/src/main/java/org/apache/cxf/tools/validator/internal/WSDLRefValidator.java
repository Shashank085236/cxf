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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.WSDLConstants;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.validator.internal.model.FailureLocation;
import org.apache.cxf.tools.validator.internal.model.XBinding;
import org.apache.cxf.tools.validator.internal.model.XDef;
import org.apache.cxf.tools.validator.internal.model.XFault;
import org.apache.cxf.tools.validator.internal.model.XInput;
import org.apache.cxf.tools.validator.internal.model.XMessage;
import org.apache.cxf.tools.validator.internal.model.XNode;
import org.apache.cxf.tools.validator.internal.model.XOperation;
import org.apache.cxf.tools.validator.internal.model.XOutput;
import org.apache.cxf.tools.validator.internal.model.XPort;
import org.apache.cxf.tools.validator.internal.model.XPortType;
import org.apache.cxf.tools.validator.internal.model.XService;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;


public class WSDLRefValidator {
    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLRefValidator.class);
    protected List<XNode> vNodes = new ArrayList<XNode>();
    
    private Set<QName> portTypeRefNames = new HashSet<QName>();
    private Set<QName> messageRefNames = new HashSet<QName>();

    private ValidationResult vResults = new ValidationResult();

    private Definition definition;

    private List<Definition> importedDefinitions;
    private List<XmlSchemaCollection> schemas;
    private XmlSchemaCollection schemaCollection = new XmlSchemaCollection();

    public WSDLRefValidator() {
    }

    public WSDLRefValidator(final String wsdl) {
        this(wsdl, null);
    }
    
    public WSDLRefValidator(final String wsdl, final Document doc) {
        WSDLDefinitionBuilder wsdlBuilder = new WSDLDefinitionBuilder();
        try {
            this.definition = wsdlBuilder.build(wsdl);

            if (wsdlBuilder.getImportedDefinitions().size() > 0) {
                importedDefinitions = new ArrayList<Definition>();
                importedDefinitions.addAll(wsdlBuilder.getImportedDefinitions());
            }
        } catch (Exception e) {
            throw new ToolException(e);
        }

        try {
            Document document = doc == null ? getWSDLDocument() : doc;
            schemas = ValidatorUtil.getSchemaList(document, definition.getDocumentBaseURI());
        } catch (IOException ex) {
            throw new ToolException("Cannot get schema list " + definition.getDocumentBaseURI(), ex);
        } catch (Exception ex) {
            throw new ToolException(ex);
        }
    }

    public ValidationResult getValidationResults() {
        return this.vResults;
    }

    private File getWSDLFile(String location) throws URISyntaxException {
        return new File(new URI(URIParserUtil.getAbsoluteURI(location)));
    }

    private Document getWSDLDocument(final String wsdl) throws URISyntaxException {
        return new Stax2DOM().getDocument(getWSDLFile(wsdl));
    }
    
    private Document getWSDLDocument() throws Exception {
        return getWSDLDocument(this.definition.getDocumentBaseURI());
    }
    
    private List<Document> getWSDLDocuments() {
        List<Document> docs = new ArrayList<Document>();
        try {
            docs.add(getWSDLDocument());

            if (null != importedDefinitions) {
                for (Definition d : importedDefinitions) {
                    docs.add(getWSDLDocument(d.getDocumentBaseURI()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }
        
        return docs;
    }

    private boolean isExist(List<Document> docs, XNode vNode) {
        XPathUtils xpather = new XPathUtils(vNode.getNSMap());
        String expression = vNode.toString();
        for (Document doc : docs) {
            if (xpather.isExist(expression, doc, XPathConstants.NODE)) {
                return true;
            }
        }
        return false;
    }

    private FailureLocation getFailureLocation(List<Document> docs, XNode fNode) {
        if (fNode == null) {
            return null;
        }

        XPathUtils xpather = new XPathUtils(fNode.getNSMap());
        for (Document doc : docs) {
            Node node = (Node) xpather.getValue(fNode.toString(), doc, XPathConstants.NODE);
            if (null != node) {
                return new FailureLocation((Location)node.getUserData(WSDLConstants.NODE_LOCATION),
                                           doc.getBaseURI());
            }
        }
        return null;
    }
    
    public boolean isValid() {
        try {

            collectValidationPoints();
            
            List<Document> wsdlDocs = getWSDLDocuments();

            for (XNode vNode : vNodes) {

                if (!isExist(wsdlDocs, vNode)) {
                    FailureLocation loc = getFailureLocation(wsdlDocs, vNode.getFailurePoint());
                    vResults.addError(new Message("FAILED_AT_POINT",
                                                  LOG,
                                                  loc.getLocation().getLineNumber(),
                                                  loc.getLocation().getColumnNumber(),
                                                  loc.getDocumentURI(),
                                                  vNode.getPlainText()));
                }
            }
        } catch (ToolException e) {
            this.vResults.addError(e.getMessage());
            return false;
        }
        return vResults.isSuccessful();
    }

    public void setDefinition(final Definition def) {
        this.definition = def;
    }

    private Map<QName, Service> getServices() {
        Map<QName, Service> services = new HashMap<QName, Service>();
        Iterator sNames = definition.getAllServices().keySet().iterator();
        while (sNames.hasNext()) {
            QName sName = (QName) sNames.next();
            services.put(sName, definition.getService(sName));
        }
        return services;
    }

    private Map<QName, XNode> getBindings(Service service) {
        Map<QName, XNode> bindings = new HashMap<QName, XNode>();

        if (service.getPorts().values().size() == 0) {
            throw new ToolException("Service " + service.getQName() + " does not contain any usable ports");
        }
        Iterator portIte = service.getPorts().values().iterator();
        while (portIte.hasNext()) {
            Port port = (Port)portIte.next();
            Binding binding = port.getBinding();
            bindings.put(binding.getQName(), getXNode(service, port));
            if (WSDLConstants.NS_WSDL.equals(binding.getQName().getNamespaceURI())) {
                throw new ToolException("Binding "
                                        + binding.getQName().getLocalPart()
                                        + " namespace set improperly.");
            }
        }

        return bindings;
    }

    private Map<QName, Operation> getOperations(PortType portType) {
        Map<QName, Operation> operations = new HashMap<QName, Operation>();
        for (Iterator iter = portType.getOperations().iterator(); iter.hasNext();) {
            Operation op = (Operation) iter.next();
            operations.put(new QName(portType.getQName().getNamespaceURI(), op.getName()), op);
        }
        return operations;
    }

    private XNode getXNode(Service service, Port port) {
        XNode vService = getXNode(service);

        XPort pNode = new XPort();
        pNode.setName(port.getName());
        pNode.setParentNode(vService);
        return pNode;
    }
    
    private XNode getXNode(Service service) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(service.getQName().getNamespaceURI());

        XService sNode = new XService();
        sNode.setName(service.getQName().getLocalPart());
        sNode.setParentNode(xdef);
        return sNode;
    }
    
    private XNode getXNode(Binding binding) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(binding.getQName().getNamespaceURI());

        XBinding bNode = new XBinding();
        bNode.setName(binding.getQName().getLocalPart());
        bNode.setParentNode(xdef);
        return bNode;
    }

    private XNode getXNode(PortType portType) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(portType.getQName().getNamespaceURI());

        XPortType pNode = new XPortType();
        pNode.setName(portType.getQName().getLocalPart());
        pNode.setParentNode(xdef);
        return pNode;
    }

    private XNode getOperationXNode(XNode pNode, String opName) {
        XOperation node = new XOperation();
        node.setName(opName);
        node.setParentNode(pNode);
        return node;
    }

    private XNode getInputXNode(XNode opVNode, String name) {
        XInput oNode = new XInput();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        return oNode;
    }

    private XNode getOutputXNode(XNode opVNode, String name) {
        XOutput oNode = new XOutput();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        return oNode;
    }

    private XNode getFaultXNode(XNode opVNode, String name) {
        XFault oNode = new XFault();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        return oNode;
    }

    private XNode getXNode(javax.wsdl.Message msg) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(msg.getQName().getNamespaceURI());

        XMessage mNode = new XMessage();
        mNode.setName(msg.getQName().getLocalPart());
        mNode.setParentNode(xdef);
        return mNode;
    }

    private void collectValidationPoints() {
        if (getServices().size() == 0) {
            throw new ToolException("WSDL document does not define any services");
        }
        Map<QName, XNode> vBindingNodes = new HashMap<QName, XNode>();
        for (Service service : getServices().values()) {
            vBindingNodes.putAll(getBindings(service));
        }
        
        for (QName bName : vBindingNodes.keySet()) {
            Binding binding = this.definition.getBinding(bName);
            XNode vBindingNode = getXNode(binding);
            vBindingNode.setFailurePoint(vBindingNodes.get(bName));
            vNodes.add(vBindingNode);

            if (binding.getPortType() == null) {
                continue;
            }
            portTypeRefNames.add(binding.getPortType().getQName());

            XNode vPortTypeNode = getXNode(binding.getPortType());
            vPortTypeNode.setFailurePoint(vBindingNode);
            vNodes.add(vPortTypeNode);
            for (Iterator iter = binding.getBindingOperations().iterator(); iter.hasNext();) {
                BindingOperation bop = (BindingOperation) iter.next();
                XNode vOpNode = getOperationXNode(vPortTypeNode, bop.getName());
                XNode vBopNode = getOperationXNode(vBindingNode, bop.getName());
                vOpNode.setFailurePoint(vBopNode);
                vNodes.add(vOpNode);
                String inName = bop.getBindingInput().getName();
                if (!StringUtils.isEmpty(inName)) {
                    XNode vInputNode = getInputXNode(vOpNode, inName);
                    vInputNode.setFailurePoint(getInputXNode(vBopNode, inName));
                    vNodes.add(vInputNode);
                }
                if (bop.getBindingOutput() != null) {
                    String outName = bop.getBindingOutput().getName();
                    if (!StringUtils.isEmpty(outName)) {
                        XNode vOutputNode = getOutputXNode(vOpNode, outName);
                        vOutputNode.setFailurePoint(getOutputXNode(vBopNode, outName));
                        vNodes.add(vOutputNode);
                    }
                }
                for (Iterator iter1 = bop.getBindingFaults().keySet().iterator(); iter1.hasNext();) {
                    String faultName = (String) iter1.next();
                    XNode vFaultNode = getFaultXNode(vOpNode, faultName);
                    vFaultNode.setFailurePoint(getFaultXNode(vBopNode, faultName));
                    vNodes.add(vFaultNode);
                }
            }
        }

        collectValidationPointsForPortTypes();
        collectValidationPointsForMessages();        
    }

    private void collectValidationPointsForMessages() {
        for (QName msgName : messageRefNames) {
            javax.wsdl.Message message = this.definition.getMessage(msgName);
            for (Iterator iter = message.getParts().values().iterator(); iter.hasNext();) {
                Part part = (Part) iter.next();
                QName elementName = part.getElementName();
                QName typeName = part.getTypeName();

                if (elementName == null && typeName == null) {
                    vResults.addError(new Message("PART_NO_TYPES", LOG));
                    continue;
                }

                if (elementName != null && typeName != null) {
                    vResults.addError(new Message("PART_NOT_UNIQUE", LOG));
                    continue;
                }

                if (elementName != null && typeName == null) {
                    boolean valid = validatePartType(elementName.getNamespaceURI(),
                                                     elementName.getLocalPart(), true);
                    if (!valid) {
                        vResults.addError(new Message("TYPE_REF_NOT_FOUND", LOG, message.getQName(),
                                                      part.getName(), elementName));
                    }

                }
                if (typeName != null && elementName == null) {

                    boolean valid = validatePartType(typeName.getNamespaceURI(),
                                                     typeName.getLocalPart(),
                                                     false);

                    if (!valid) {
                        vResults.addError(new Message("TYPE_REF_NOT_FOUND", LOG, message.getQName(),
                                                      part.getName(), typeName));
                    }

                }
                
                
            }
        }
    }

    private void collectValidationPointsForPortTypes() {
        for (QName ptName : portTypeRefNames) {
            PortType portType = this.definition.getPortType(ptName);
            XNode vPortTypeNode = getXNode(portType);
            for (Operation operation : getOperations(portType).values()) {
                XNode vOperationNode = getOperationXNode(vPortTypeNode, operation.getName());
                javax.wsdl.Message inMsg = operation.getInput().getMessage();
                if (inMsg == null) {
                    vResults.addWarning("Operation " + operation.getName() + " in PortType: "
                                        + portType.getQName() + " has no input message");
                } else {
                    XNode vInMsgNode = getXNode(inMsg);
                    vInMsgNode.setFailurePoint(getInputXNode(vOperationNode, operation.getInput().getName()));
                    vNodes.add(vInMsgNode);
                    messageRefNames.add(inMsg.getQName());
                }

                if (operation.getOutput() != null) {
                    javax.wsdl.Message outMsg = operation.getOutput().getMessage();

                    if (outMsg == null) {
                        vResults.addWarning("Operation " + operation.getName() + " in PortType: "
                                            + portType.getQName() + " has no output message");
                    } else {
                        XNode vOutMsgNode = getXNode(outMsg);
                        vOutMsgNode.setFailurePoint(getOutputXNode(vOperationNode,
                                                                   operation.getOutput().getName()));
                        vNodes.add(vOutMsgNode);
                        messageRefNames.add(outMsg.getQName());
                    }
                }
                for (Iterator iter = operation.getFaults().values().iterator(); iter.hasNext();) {
                    Fault fault = (Fault) iter.next();
                    javax.wsdl.Message faultMsg = fault.getMessage();
                    XNode vFaultMsgNode = getXNode(faultMsg);
                    vFaultMsgNode.setFailurePoint(getFaultXNode(vOperationNode, fault.getName()));
                    vNodes.add(vFaultMsgNode);
                    messageRefNames.add(faultMsg.getQName());
                }
            }
        }
    }

    private boolean validatePartType(String namespace, String name, boolean isElement) {

        boolean partvalid = false;

        if (namespace.equals(WSDLConstants.NS_XMLNS)) {
            if (isElement) {
                XmlSchemaElement  schemaEle = 
                    schemaCollection.getElementByQName(new QName(WSDLConstants.NS_XMLNS, name));    
                partvalid = schemaEle != null ? true : false;
            } else {
                XmlSchemaType schemaType = 
                    schemaCollection.getTypeByQName(new QName(WSDLConstants.NS_XMLNS, name));  
                partvalid = schemaType != null ? true : false;
            }
            
        } else {
            if (isElement) {
                for (XmlSchemaCollection schema : schemas) {
                    if (schema != null && schema.getElementByQName(
                            new QName(namespace, name)) != null) {
                        partvalid = true;
                        break;
                    }
                }
            } else {
                for (XmlSchemaCollection schema : schemas) {
                    if (schema != null && schema.getTypeByQName(
                            new QName(namespace, name)) != null) {
                        partvalid = true;
                        break;
                    }
                }
            }
        }
        return partvalid;
    }    
}

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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.StAXUtil;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public final class CustomizationParser {
    // For WSDL1.1
    private static final Logger LOG = LogUtils.getL7dLogger(CustomizationParser.class);

    private ToolContext env;
    //map for jaxws binding and wsdl element
    private final Map<Element, Element> jaxwsBindingsMap = new HashMap<Element, Element>();
    private final List<InputSource> jaxbBindings = new ArrayList<InputSource>();
    private final Map<String, Element> customizedElements = new HashMap<String, Element>();

    private Element handlerChains;
    private Element wsdlNode;
    private String wsdlURL;

    private CustomNodeSelector nodeSelector = new CustomNodeSelector();

    public CustomizationParser() {
        jaxwsBindingsMap.clear();
        jaxbBindings.clear();
    }

    public Element getHandlerChains() {
        return this.handlerChains;
    }

    public void parse(ToolContext pe) {
        this.env = pe;
        String[] bindingFiles;
        try {
            wsdlURL = URIParserUtil.getAbsoluteURI((String)env.get(ToolConstants.CFG_WSDLURL));
            try {
                wsdlNode = getTargetNode(this.wsdlURL);
            } catch (IOException e) {
               // do nothing
            }
            customizedElements.put(wsdlURL.toString(), wsdlNode);
            bindingFiles = (String[])env.get(ToolConstants.CFG_BINDING);
            if (bindingFiles == null) {
                return;
            }
        } catch (ClassCastException e) {
            bindingFiles = new String[1];
            bindingFiles[0] = (String)env.get(ToolConstants.CFG_BINDING);
        }

        for (int i = 0; i < bindingFiles.length; i++) {
            try {
                addBinding(bindingFiles[i]);
            } catch (XMLStreamException xse) {
                Message msg = new Message("STAX_PARSER_ERROR", LOG);
                throw new ToolException(msg, xse);
            }
        }

        for (Element element : jaxwsBindingsMap.keySet()) {
            nodeSelector.addNamespaces(element);
            Element targetNode = jaxwsBindingsMap.get(element);
            internalizeBinding(element, targetNode, "");
            String uri = element.getAttribute("wsdlLocation");
            customizedElements.put(uri, targetNode);
        }
        buildHandlerChains();
    }

    public Element getTargetNode(String wsdlLoc) throws IOException {
        Document doc = null;
        InputStream ins = null;
        try {
            URIResolver resolver = new URIResolver(wsdlLoc);
            ins = resolver.getInputStream();
        } catch (IOException e1) {
            throw e1;
            
        }

        try {
            doc = DOMUtils.readXml(ins);
        } catch (Exception e) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[]{wsdlLoc});
            throw new ToolException(msg, e);
        }

        if (doc != null) {
            return doc.getDocumentElement();
        }
        return null;
    }

    private void buildHandlerChains() {

        for (Element jaxwsBinding : jaxwsBindingsMap.keySet()) {
            NodeList nl = jaxwsBinding.getElementsByTagNameNS(ToolConstants.HANDLER_CHAINS_URI,
                                                              ToolConstants.HANDLER_CHAINS);
            if (nl.getLength() == 0) {
                continue;
            }
            // take the first one, anyway its 1 handler-config per customization
            this.handlerChains = (Element)nl.item(0);
            return;
        }

    }

    private Node[] getAnnotationNodes(final Node node) {
        Node[] nodes = new Node[2];

        Node annotationNode = nodeSelector.queryNode(node, "//xsd:annotation");

        if (annotationNode == null) {
            annotationNode = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI,
                                                                     "annotation");
        }

        nodes[0] = annotationNode;

        Node appinfoNode = nodeSelector.queryNode(annotationNode, "//xsd:appinfo");

        if (appinfoNode == null) {
            appinfoNode = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI,
                                                                  "appinfo");
            annotationNode.appendChild(appinfoNode);
        }
        nodes[1] = appinfoNode;
        return nodes;
    }

    private void appendJaxbVersion(final Element schemaElement) {
        String jaxbPrefix = schemaElement.lookupPrefix(ToolConstants.NS_JAXB_BINDINGS);
        if (jaxbPrefix == null) {
            schemaElement.setAttribute("xmlns:jaxb", ToolConstants.NS_JAXB_BINDINGS);
            schemaElement.setAttribute("jaxb:version", "2.0");
        }
    }

    protected void copyAllJaxbDeclarations(final Node schemaNode, final Element jaxwsBindingNode) {
        Element jaxbBindingElement = getJaxbBindingElement(jaxwsBindingNode);
        appendJaxbVersion((Element)schemaNode);
        if (jaxbBindingElement != null) {
            NodeList nlist = nodeSelector.queryNodes(schemaNode,
                                                    jaxbBindingElement.getAttribute("node"));
            for (int i = 0; i < nlist.getLength(); i++) {
                Node node = nlist.item(i);
                copyAllJaxbDeclarations(node, jaxbBindingElement);
            }
            return;
        }

        Node[] embededNodes  = getAnnotationNodes(schemaNode);
        Node annotationNode = embededNodes[0];
        Node appinfoNode = embededNodes[1];

        NodeList childNodes = jaxwsBindingNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (!isJaxbBindings(childNode) || isJaxbBindingsElement(childNode)) {
                continue;
            }

            final Node jaxbNode = childNode;

            Node cloneNode = ProcessorUtil.cloneNode(schemaNode.getOwnerDocument(), jaxbNode, true);
            appinfoNode.appendChild(cloneNode);
        }

        if (schemaNode.getChildNodes().getLength() > 0) {
            schemaNode.insertBefore(annotationNode, schemaNode.getChildNodes().item(0));
        } else {
            schemaNode.appendChild(annotationNode);
        }
    }

    protected void internalizeBinding(Element bindings, Element targetNode, String expression) {
        if (bindings.getAttributeNode("wsdlLocation") != null) {
            expression = "/";
        }

        if (isGlobaleBindings(bindings)) {
            String pfx = targetNode.getPrefix();
            if (pfx == null) {
                pfx = "";
            } else {
                pfx += ":";
            }

            nodeSelector.addNamespaces(wsdlNode);
            Node node = nodeSelector.queryNode(targetNode, "//" + pfx + "definitions");
            copyBindingsToWsdl(node, bindings, nodeSelector.getNamespaceContext());
        }

        if (isJAXWSBindings(bindings) && bindings.getAttributeNode("node") != null) {
            expression = expression + "/" + bindings.getAttribute("node");

            nodeSelector.addNamespaces(bindings);

            NodeList nodeList = nodeSelector.queryNodes(targetNode, expression);
            if (nodeList == null || nodeList.getLength() == 0) {
                throw new ToolException(new Message("NODE_NOT_EXISTS",
                                                    LOG, new Object[] {expression}));
            }

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (hasJaxbBindingDeclaration(bindings)) {
                    copyAllJaxbDeclarations(node, bindings);
                } else {
                    copyBindingsToWsdl(node, bindings, nodeSelector.getNamespaceContext());
                }
            }
        }

        Element[] children = getChildElements(bindings, ToolConstants.NS_JAXWS_BINDINGS);
        for (int i = 0; i < children.length; i++) {
            if (children[i].getNodeType() == Node.ELEMENT_NODE) {
                internalizeBinding(children[i], targetNode, expression);
            }
        }
        
    }

    private void copyBindingsToWsdl(Node node, Node bindings, MapNamespaceContext ctx) {
        if (bindings.getNamespaceURI().equals(ToolConstants.JAXWS_BINDINGS.getNamespaceURI())) {
            bindings.setPrefix("jaxws");
        }

        for (Map.Entry<String, String> ent : ctx.getUsedNamespaces().entrySet()) {
            if (node.lookupNamespaceURI(ent.getKey()) == null) {
                node.getOwnerDocument().getDocumentElement()
                    .setAttribute("xmlns:" + ent.getKey(), ent.getValue());
            }

        }

        for (int i = 0; i < bindings.getChildNodes().getLength(); i++) {
            Node childNode = bindings.getChildNodes().item(i);
            if (childNode.getNodeType() == Element.ELEMENT_NODE
                && childNode.getNamespaceURI().equals(ToolConstants.JAXWS_BINDINGS.getNamespaceURI())) {
                childNode.setPrefix("jaxws");
            }
        }

        Node cloneNode = ProcessorUtil.cloneNode(node.getOwnerDocument(), bindings, true);
        Node firstChild = DOMUtils.getChild(node, "jaxws:bindings");
        if (firstChild == null && cloneNode.getNodeName().indexOf("bindings") == -1) {
            wsdlNode.setAttribute("xmlns:jaxws", ToolConstants.JAXWS_BINDINGS.getNamespaceURI());
            Element jaxwsBindingElement = node.getOwnerDocument().createElement("jaxws:bindings");
            node.appendChild(jaxwsBindingElement);
            firstChild = jaxwsBindingElement;
        }

        if (firstChild == null && cloneNode.getNodeName().indexOf("bindings") > -1) {
            firstChild = node;
            if (wsdlNode.getAttributeNode("xmls:jaxws") == null) {
                wsdlNode.setAttribute("xmlns:jaxws", ToolConstants.JAXWS_BINDINGS.getNamespaceURI());
            }
        }

        Element cloneEle = (Element)cloneNode;
        cloneEle.removeAttribute("node");
        for (int i = 0; i < cloneNode.getChildNodes().getLength(); i++) {
            Node child = cloneNode.getChildNodes().item(i);
            if (child.getNodeType() == Element.ELEMENT_NODE) {
                Element childElement = (Element)child;
                Node attrNode = childElement.getAttributeNode("node");
                if (attrNode != null) {
                    cloneNode.removeChild(child);
                }

            }
        }
        firstChild.appendChild(cloneNode);
    }

    private boolean isGlobaleBindings(Element binding) {

        boolean globleNode = binding.getNamespaceURI().equals(ToolConstants.NS_JAXWS_BINDINGS)
                             && binding.getLocalName().equals("package")
                             || binding.getLocalName().equals("enableAsyncMapping")
                             || binding.getLocalName().equals("enableAdditionalSOAPHeaderMapping")
                             || binding.getLocalName().equals("enableWrapperStyle")
                             || binding.getLocalName().equals("enableMIMEContent");
        Node parentNode = binding.getParentNode();
        if (parentNode instanceof Element) {
            Element ele = (Element)parentNode;
            if (ele.getAttributeNode("wsdlLocation") != null && globleNode) {
                return true;
            }

        }
        return false;

    }

    private Element[] getChildElements(Element parent, String nsUri) {
        List<Element> a = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node item = children.item(i);
            if (!(item instanceof Element)) {
                continue;
            }
            if (nsUri.equals(item.getNamespaceURI())) {
                a.add((Element)item);
            }
        }
        return (Element[])a.toArray(new Element[a.size()]);
    }

    private void addBinding(String bindingFile) throws XMLStreamException {
        InputSource is = new InputSource(bindingFile);
        XMLStreamReader reader = StAXUtil.createFreshXMLStreamReader(is);

        StAXUtil.toStartTag(reader);

        Element root = null;
        try {
            URIResolver resolver = new URIResolver(bindingFile);
            root = DOMUtils.readXml(resolver.getInputStream()).getDocumentElement();
        } catch (Exception e1) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {bindingFile});
            throw new ToolException(msg, e1);
        }
        if (isValidJaxwsBindingFile(bindingFile, reader)) {
            
            String wsdlLocation = root.getAttribute("wsdlLocation");
            Element targetNode = null;
            if (!StringUtils.isEmpty(wsdlLocation)) {
                URI wsdlURI = null;
                try {
                    wsdlURI = new URI(wsdlLocation);
                } catch (URISyntaxException e) {
                    Message msg = new Message("JAXWSBINDINGS_WSDLLOC_ERROR",
                                              LOG, new Object[] {wsdlLocation});
                    throw new ToolException(msg);
                } 
                
                if (!wsdlURI.isAbsolute()) {
                    try {
                        String base = URIParserUtil.getAbsoluteURI(bindingFile);
                        URI baseURI = new URI(base);
                        wsdlURI = baseURI.resolve(wsdlURI);
                    } catch (URISyntaxException e) {
                        Message msg = new Message("NOT_URI", LOG, new Object[] {bindingFile});
                        throw new ToolException(msg, e);
                    }

                }
                try {
                    targetNode = this.getTargetNode(wsdlURI.toString());
                } catch (IOException e) {
                    Message msg = new Message("POINT_TO_WSDL_DOES_NOT_EXIST", 
                                              LOG, new Object[]{bindingFile, wsdlURI.toString()});
                    throw new ToolException(msg, e);
                }
                if (targetNode == null) {
                    Message msg = new Message("CAN_NOT_FIND_BINDING_WSDL", 
                                              LOG, new Object[] {wsdlURI.normalize(), bindingFile});
                    throw new ToolException(msg);
                    
                }
                root.setAttribute("wsdlLocation", wsdlURI.toString());                
            } else {
                try {
                    targetNode = getTargetNode(wsdlURL);
                } catch (IOException e) {
                    //do nothing
                }
                root.setAttribute("wsdlLocation", wsdlURL);   
            }
            jaxwsBindingsMap.put(root, targetNode);
            
        } else if (isValidJaxbBindingFile(reader)) {
            String schemaLocation = root.getAttribute("schemaLocation");
            if (StringUtils.isEmpty(schemaLocation)) {
                root.setAttribute("schemaLocation", wsdlURL);
                try {
                    File tmpFile = FileUtils.createTempFile("jaxbbinding", ".xml");
                    XMLUtils.writeTo(root, new FileOutputStream(tmpFile));
                    InputSource newis = new InputSource(URIParserUtil.getAbsoluteURI(tmpFile
                        .getAbsolutePath()));
                    jaxbBindings.add(newis);
                    tmpFile.deleteOnExit();
                } catch (Exception e) {
                    Message msg = new Message("FAILED_TO_ADD_SCHEMALOCATION", LOG, bindingFile);
                    throw new ToolException(msg, e);
                }                
            } else {
                jaxbBindings.add(is);
            }
        } else {
            Message msg = new Message("UNKNOWN_BINDING_FILE", LOG, bindingFile);
            throw new ToolException(msg);
        }
    }

    private boolean isValidJaxbBindingFile(XMLStreamReader reader) {
        if (ToolConstants.JAXB_BINDINGS.equals(reader.getName())) {
            
            return true;
        }
        return false;
    }

    private boolean isValidJaxwsBindingFile(String bindingLocation, XMLStreamReader reader) {
        if (ToolConstants.JAXWS_BINDINGS.equals(reader.getName())) {
            //Comment this check , by default wsdlLocation value will be the user input wsdl url
            /*
             String wsdlLocation = reader.getAttributeValue(null, "wsdlLocation");
            if (!StringUtils.isEmpty(wsdlLocation)) {
                return true;
            }*/
            return true;
        }
        return false;

    }

    protected void setWSDLNode(final Element node) {
        this.wsdlNode = node;
    }
    public Node getWSDLNode() {
        return this.wsdlNode;
    }

    private boolean isJAXWSBindings(Node bindings) {
        return ToolConstants.NS_JAXWS_BINDINGS.equals(bindings.getNamespaceURI())
               && "bindings".equals(bindings.getLocalName());
    }

    private boolean isJaxbBindings(Node bindings) {
        return ToolConstants.NS_JAXB_BINDINGS.equals(bindings.getNamespaceURI());
    }

    private boolean isJaxbBindingsElement(Node bindings) {
        return "bindings".equals(bindings.getLocalName());
    }

    protected Element getJaxbBindingElement(final Element bindings) {
        NodeList list = bindings.getElementsByTagNameNS(ToolConstants.NS_JAXB_BINDINGS, "bindings");
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        return null;
    }

    protected boolean hasJaxbBindingDeclaration(Node bindings) {
        NodeList childNodes = bindings.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (isJaxbBindings(childNode)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Element> getCustomizedWSDLElements() {
        return this.customizedElements;
    }

    public List<InputSource> getJaxbBindings() {
        return this.jaxbBindings;
    }

    public static JAXWSBinding mergeJawsBinding(JAXWSBinding binding1, JAXWSBinding binding2) {
        if (binding1 != null && binding2 != null) {
            if (binding2.isEnableAsyncMapping()) {
                binding1.setEnableAsyncMapping(true);
            }
            if (binding2.isEnableWrapperStyle()) {
                binding1.setEnableWrapperStyle(true);
            }
            if (binding2.isEnableMime()) {
                binding1.setEnableMime(true);
            }

            if (binding2.getJaxwsClass() != null) {
                binding1.setJaxwsClass(binding2.getJaxwsClass());
            }

            if (binding2.getJaxwsPara() != null) {
                binding1.setJaxwsPara(binding2.getJaxwsPara());
            }
            return binding1;
        }

        return binding1 == null ? binding2 : binding1;
    }

}

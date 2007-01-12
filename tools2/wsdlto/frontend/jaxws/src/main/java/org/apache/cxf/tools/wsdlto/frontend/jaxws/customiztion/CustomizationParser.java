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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customiztion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.StAXUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public final class CustomizationParser {
    // For WSDL1.1
    private static final Logger LOG = LogUtils.getL7dLogger(CustomizationParser.class);
    private static final XPathFactory XPF = XPathFactory.newInstance();
    private static CustomizationParser parser;

    private final XPath xpath = XPF.newXPath();

    private ToolContext env;
    private final List<Element> jaxwsBindings = new ArrayList<Element>();
    private final Set<InputSource> jaxbBindings = new HashSet<InputSource>();
    
    private Element handlerChains;
    private Element wsdlNode;
    private String wsdlURL;

    private CustomizationParser() {

    }

    public static CustomizationParser getInstance() {
        if (parser == null) {
            parser = new CustomizationParser();
            parser.clean();
        }
        return parser;
    }

    public void clean() {
        jaxwsBindings.clear();
        jaxbBindings.clear();
    }

    public Element getHandlerChains() {
        return this.handlerChains;
    }

    public void parse(ToolContext pe) {
        this.env = pe;
        String[] bindingFiles;
        try {
            this.wsdlURL = (String)env.get(ToolConstants.CFG_WSDLURL);           
            this.wsdlNode = this.getTargetNode(wsdlURL);
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
                Message msg = new Message("STAX_PASER_ERROR", LOG);
                throw new ToolException(msg, xse);
            }
        }

        for (Element element : jaxwsBindings) {
            internalizeBinding(element, "");
        }

        buildHandlerChains();
    }

    public Element getTargetNode(String wsdlLoc) {
        Document doc = null;
        URI uri = null;
        try {
            uri = new URI(wsdlLoc);
        } catch (URISyntaxException e1) {
           //ignore
        }
        File file = new File(uri);
        InputStream ins;
       
        try {
            ins = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Message msg = new Message("FILE_NOT_FOUND", LOG, new Object[] {file});
            throw new ToolException(msg, e); 
        }
            
       
        
        try {
            doc = DOMUtils.readXml(ins);
        } catch (Exception e) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {file});
            throw new ToolException(msg, e); 
        } 
   
        if (doc != null) {
            return doc.getDocumentElement();
        }
        return null;
    }

    private void buildHandlerChains() {

        for (Element jaxwsBinding : jaxwsBindings) {
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

    private void internalizeBinding(Element bindings, String expression) {
        if (bindings.getAttributeNode("wsdlLocation") != null) {
            expression = "/";
        }

        if (isGlobaleBindings(bindings)) {
            Node node = queryXPathNode(wsdlNode, "//wsdl:definitions");
            copyBindingsToWsdl(node, bindings);
        }

        if (isJAXWSBindings(bindings) && bindings.getAttributeNode("node") != null) {
            expression = expression + "/" + bindings.getAttribute("node");
  
            Node node = null;
            boolean nestedJaxb = nestedJaxbBinding(bindings);

            node = queryXPathNode(wsdlNode, expression);
            if (node != null && !nestedJaxb) {
                copyBindingsToWsdl(node, bindings);
            }

            if (node != null && nestedJaxb) {
                // append xmlns:jaxb and jaxb:version attribute for schema
                Node schemaNode = getSchemaNode(node);
                Element schemaElement = (Element)schemaNode;

                String jaxbPrefix = schemaElement.lookupPrefix(ToolConstants.NS_JAXB_BINDINGS);
                if (jaxbPrefix == null) {
                    schemaElement.setAttribute("xmlns:jaxb", ToolConstants.NS_JAXB_BINDINGS);
                    schemaElement.setAttribute("jaxb:version", "2.0");
                }

                // append jaxb appinfo for value node
                Element annoElement = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI,
                                                                              "annotation");
                Element appinfoEle = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI,
                                                                             "appinfo");

                annoElement.appendChild(appinfoEle);

                NodeList jxbBinds = getJaxbBindingNode((Element)bindings);

                for (int l = 0; l < jxbBinds.getLength(); l++) {
                    Node jaxbBindingNode = jxbBinds.item(l);
                    Node cloneNode = ProcessorUtil.cloneNode(node.getOwnerDocument(), jaxbBindingNode, true);
                    appinfoEle.appendChild(cloneNode);
                }

                if (node.getChildNodes().getLength() > 0) {
                    node.insertBefore(annoElement, node.getChildNodes().item(0));
                } else {
                    node.appendChild(annoElement);
                }
            }
        }

        Element[] children = getChildElements(bindings, ToolConstants.NS_JAXWS_BINDINGS);
        for (int i = 0; i < children.length; i++) {
            internalizeBinding(children[i], expression);
        }
    }

    private void copyBindingsToWsdl(Node node, Node bindings) {
        if (bindings.getNamespaceURI().equals(ToolConstants.JAXWS_BINDINGS.getNamespaceURI())) {
            bindings.setPrefix("jaxws");
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

    private Node getSchemaNode(Node node) {
        if (!"schema".equals(node.getLocalName())) {
            while (node.getParentNode() != null) {
                node = node.getParentNode();

                if ("schema".equals(node.getLocalName())) {
                    return node;
                }
            }
            return null;
        }
        return node;
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
        URI bindingURI = null;
        try {
            bindingURI = new URI(bindingFile);
        } catch (URISyntaxException e2) {
            //ignore
        }
        InputSource is = new InputSource(bindingFile);
        XMLStreamReader reader = StAXUtil.createFreshXMLStreamReader(is);

        StAXUtil.toStartTag(reader);

        if (isValidJaxwsBindingFile(bindingFile, reader)) {
            InputStream inputStream;
            Element root = null;
            try {
                inputStream = new FileInputStream(new File(bindingURI));
                root = DOMUtils.readXml(inputStream).getDocumentElement();
            } catch (Exception e1) {
                Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {bindingFile});
                throw new ToolException(msg, e1); 
            } 
            String wsdlLocation = root.getAttribute("wsdlLocation");
            URI wsdlURI = null;
            try {
                wsdlURI = new URI(wsdlLocation);
            } catch (URISyntaxException e) {
                Message msg = new Message("JAXWSBINDINGS_WSDLLOC_ERROR", LOG, new Object[] {wsdlLocation});
                throw new ToolException(msg);
            }

            if (!wsdlURI.isAbsolute()) {
                try {
                    URI baseURI = new URI(bindingFile);
                    wsdlURI = baseURI.resolve(wsdlURI);
                } catch (URISyntaxException e) {
                    Message msg = new Message("NOT_URI", LOG, new Object[] {bindingFile});
                    throw new ToolException(msg, e);
                }

            }

            
            if (wsdlURI.toString().equals(this.wsdlURL)) {
                jaxwsBindings.add(root);
            } else {
                String wsdl = (String)env.get(ToolConstants.CFG_WSDLURL);
                Message msg = new Message("NOT_POINTTO_URL", LOG, new Object[] {bindingFile, wsdl});
                throw new ToolException(msg);
            }
        } else if (isValidJaxbBindingFile(reader)) {
            jaxbBindings.add(is);
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
            String wsdlLocation = reader.getAttributeValue(null, "wsdlLocation");
            if (!StringUtils.isEmpty(wsdlLocation)) {
                return true;
            }
        }
        return false;

    }

    
    
    class ContextImpl implements NamespaceContext {
        private Node targetNode;

        public ContextImpl(Node node) {
            targetNode = node;
        }

        public String getNamespaceURI(String prefix) {
            return targetNode.getOwnerDocument().lookupNamespaceURI(prefix);
        }

        public String getPrefix(String nsURI) {
            throw new UnsupportedOperationException();
        }

        public Iterator getPrefixes(String namespaceURI) {
            throw new UnsupportedOperationException();
        }
    }
    

    private Node queryXPathNode(Node target, String expression) {
        NodeList nlst;
        try {
            xpath.setNamespaceContext(new ContextImpl(target));
            nlst = (NodeList)xpath.evaluate(expression, target, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            Message msg = new Message("XPATH_ERROR", LOG, new Object[] {expression});
            throw new ToolException(msg, e);
        }
       
        if (nlst.getLength() != 1) {
            Message msg = new Message("ERROR_TARGETNODE_WITH_XPATH", LOG, new Object[] {expression});
            throw new ToolException(msg);
        }

        Node rnode = nlst.item(0);
        if (!(rnode instanceof Element)) {
            Message msg = new Message("ERROR_TARGETNODE_WITH_XPATH", LOG, new Object[] {expression});
            throw new ToolException(msg);
        } 
        return (Element)rnode;
    }

    public Node getWSDLNode() {
        return this.wsdlNode;
    }

    private boolean isJAXWSBindings(Node bindings) {
        return ToolConstants.NS_JAXWS_BINDINGS.equals(bindings.getNamespaceURI())
               && "bindings".equals(bindings.getLocalName());
    }

    private boolean nestedJaxbBinding(Element bindings) {
        NodeList nodeList = bindings.getElementsByTagNameNS(ToolConstants.NS_JAXB_BINDINGS, "bindings");
        if (nodeList.getLength() == 1) {
            return true;
        }
        return false;
    }

    private NodeList getJaxbBindingNode(Element bindings) {
        NodeList nodeList = bindings.getElementsByTagNameNS(ToolConstants.NS_JAXB_BINDINGS, "bindings");
        return nodeList.item(0).getChildNodes();
    }
    
    public Element getCustomizedWSDLElement() {
        return this.wsdlNode;
    }
    
   /* private URI getURI(String arg) {
        String location = ProcessorUtil.absolutize(arg);
        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            Message msg = new Message("NOT_URI", LOG, new Object[] {location});
            throw new ToolException(msg, e);
        }
        return uri;
    }*/
    
    public Set<InputSource> getJaxbBindings() {
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

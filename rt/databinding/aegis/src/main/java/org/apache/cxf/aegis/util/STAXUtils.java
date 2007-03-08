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
package org.apache.cxf.aegis.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.util.stax.DepthXMLStreamReader;
import org.apache.cxf.common.classloader.ClassLoaderUtils;

/**
 * Common StAX utilities.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Oct 26, 2004
 */
public final class STAXUtils {
    private static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    private static final Log LOG = LogFactory.getLog(STAXUtils.class);

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private static final XMLOutputFactory xmlOututFactory = XMLOutputFactory.newInstance();
    
    private static final Map<String, Object> factories = new HashMap<String, Object>();
    private static boolean inFactoryConfigured;


    private STAXUtils() {
        //utility class
    }
    /**
     * Returns true if currently at the start of an element, otherwise move
     * forwards to the next element start and return true, otherwise false is
     * returned if the end of the stream is reached.
     */
    public static boolean skipToStartOfElement(XMLStreamReader in) throws XMLStreamException {
        for (int code = in.getEventType(); code != XMLStreamConstants.END_DOCUMENT; code = in.next()) {
            if (code == XMLStreamConstants.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    public static boolean toNextElement(DepthXMLStreamReader dr) {
        if (dr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            return true;
        }

        if (dr.getEventType() == XMLStreamConstants.END_ELEMENT) {
            return false;
        }

        try {
            int depth = dr.getDepth();

            for (int event = dr.getEventType(); dr.getDepth() >= depth && dr.hasNext(); event = dr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT && dr.getDepth() == depth + 1) {
                    return true;
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }

            return false;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Couldn't parse stream.", e);
        }
    }

    public static boolean skipToStartOfElement(DepthXMLStreamReader in) throws XMLStreamException {
        for (int code = in.getEventType(); code != XMLStreamConstants.END_DOCUMENT; code = in.next()) {
            if (code == XMLStreamConstants.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies the reader to the writer. The start and end document methods must
     * be handled on the writer manually. TODO: if the namespace on the reader
     * has been declared previously to where we are in the stream, this probably
     * won't work.
     * 
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        int read = 0; // number of elements read in
        int event = reader.getEventType();

        while (reader.hasNext()) {
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                read++;
                writeStartElement(reader, writer);
                break;
            case XMLStreamConstants.END_ELEMENT:
                writer.writeEndElement();
                read--;
                if (read <= 0) {
                    return;
                }
                break;
            case XMLStreamConstants.CHARACTERS:
                writer.writeCharacters(reader.getText());
                break;
            case XMLStreamConstants.START_DOCUMENT:
            case XMLStreamConstants.END_DOCUMENT:
            case XMLStreamConstants.ATTRIBUTE:
            case XMLStreamConstants.NAMESPACE:
                break;
            case XMLStreamConstants.CDATA:
                writer.writeCData(reader.getText());
                break;
            default:
                break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer)
        throws XMLStreamException {
        String local = reader.getLocalName();
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        if (prefix == null) {
            prefix = "";
        }

        String boundPrefix = writer.getPrefix(uri);
        boolean writeElementNS = false;
        if (boundPrefix == null || !prefix.equals(boundPrefix)) {
            writeElementNS = true;
        }

        // Write out the element name
        if (uri != null && uri.length() > 0) {
            if (prefix.length() == 0) {

                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri);

            } else {
                writer.writeStartElement(prefix, local, uri);
                writer.setPrefix(prefix, uri);
            }
        } else {
            writer.writeStartElement(reader.getLocalName());
        }

        // Write out the namespaces
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);

            // Why oh why does the RI suck so much?
            if (nsURI == null) {
                nsURI = "";
            }
            if (nsPrefix == null) {
                nsPrefix = "";
            }

            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
            }

            if (uri != null && nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }

        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works
        // different on Woodstox and the RI.
        if (writeElementNS && uri != null) {
            if (prefix == null || prefix.length() == 0) {
                writer.writeDefaultNamespace(uri);
            } else {
                writer.writeNamespace(prefix, uri);
            }
        }

        // Write out attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.length() == 0) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.length() == 0) {
                writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader
                    .getAttributeLocalName(i), reader.getAttributeValue(i));
            }

        }
    }

    public static void writeDocument(Document d, XMLStreamWriter writer, boolean repairing)
        throws XMLStreamException {
        writeDocument(d, writer, true, repairing);
    }

    public static void writeDocument(Document d, XMLStreamWriter writer, boolean writeProlog,
                                     boolean repairing) throws XMLStreamException {
        if (writeProlog) {
            writer.writeStartDocument();
        }

        Element root = d.getDocumentElement();
        writeElement(root, writer, repairing);

        if (writeProlog) {
            writer.writeEndDocument();
        }
    }

    /**
     * Writes an Element to an XMLStreamWriter. The writer must already have
     * started the doucment (via writeStartDocument()). Also, this probably
     * won't work with just a fragment of a document. The Element should be the
     * root element of the document.
     * 
     * @param e
     * @param writer
     * @throws XMLStreamException
     */
    public static void writeElement(Element e, XMLStreamWriter writer, boolean repairing)
        throws XMLStreamException {
        String prefix = e.getPrefix();
        String ns = e.getNamespaceURI();
        String localName = e.getLocalName();

        if (prefix == null) {
            prefix = "";
        }
        if (localName == null) {
            localName = e.getNodeName();

            if (localName == null) {
                throw new IllegalStateException("Element's local name cannot be null!");
            }
        }

        String decUri = writer.getNamespaceContext().getNamespaceURI(prefix);
        boolean declareNamespace = decUri == null || !decUri.equals(ns);

        if (ns == null || ns.length() == 0) {
            writer.writeStartElement(localName);
        } else {
            writer.writeStartElement(prefix, localName, ns);
        }

        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);

            String name = attr.getNodeName();
            String attrPrefix = "";
            int prefixIndex = name.indexOf(':');
            if (prefixIndex != -1) {
                attrPrefix = name.substring(0, prefixIndex);
                name = name.substring(prefixIndex + 1);
            }

            if ("xmlns".equals(attrPrefix)) {
                writer.writeNamespace(name, attr.getNodeValue());
                if (name.equals(prefix) && attr.getNodeValue().equals(ns)) {
                    declareNamespace = false;
                }
            } else {
                if ("xmlns".equals(name) && "".equals(attrPrefix)) {
                    writer.writeNamespace("", attr.getNodeValue());
                    if (attr.getNodeValue().equals(ns)) {
                        declareNamespace = false;
                    }
                } else {
                    writer.writeAttribute(attrPrefix, attr.getNamespaceURI(), name, attr.getNodeValue());
                }
            }
        }

        if (declareNamespace && repairing) {
            writer.writeNamespace(prefix, ns);
        }

        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            writeNode(n, writer, repairing);
        }

        writer.writeEndElement();
    }

    public static void writeNode(Node n,
                                 XMLStreamWriter writer,
                                 boolean repairing) throws XMLStreamException {
        if (n instanceof Element) {
            writeElement((Element)n, writer, repairing);
        } else if (n instanceof Text) {
            writer.writeCharacters(((Text)n).getNodeValue());
        } else if (n instanceof CDATASection) {
            writer.writeCData(((CDATASection)n).getData());
        } else if (n instanceof Comment) {
            writer.writeComment(((Comment)n).getData());
        } else if (n instanceof EntityReference) {
            writer.writeEntityRef(((EntityReference)n).getNodeValue());
        } else if (n instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction)n;
            writer.writeProcessingInstruction(pi.getTarget(), pi.getData());
        }
    }

    public static Document read(DocumentBuilder builder, XMLStreamReader reader, boolean repairing)
        throws XMLStreamException {
        Document doc = builder.newDocument();

        readDocElements(doc, reader, repairing);

        return doc;
    }

    /**
     * @param parent
     * @return
     */
    private static Document getDocument(Node parent) {
        return (parent instanceof Document) ? (Document)parent : parent.getOwnerDocument();
    }

    /**
     * @param parent
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private static Element startElement(Node parent, XMLStreamReader reader, boolean repairing)
        throws XMLStreamException {
        Document doc = getDocument(parent);

        Element e = doc.createElementNS(reader.getNamespaceURI(), reader.getLocalName());

        if (reader.getPrefix() != null) {
            e.setPrefix(reader.getPrefix());
        }

        parent.appendChild(e);

        for (int ns = 0; ns < reader.getNamespaceCount(); ns++) {
            String uri = reader.getNamespaceURI(ns);
            String prefix = reader.getNamespacePrefix(ns);

            declare(e, uri, prefix);
        }

        for (int att = 0; att < reader.getAttributeCount(); att++) {
            String name = reader.getAttributeLocalName(att);
            String prefix = reader.getAttributePrefix(att);
            if (prefix != null && prefix.length() > 0) {
                name = prefix + ":" + name;
            }

            Attr attr = doc.createAttributeNS(reader.getAttributeNamespace(att), name);
            attr.setValue(reader.getAttributeValue(att));
            e.setAttributeNode(attr);
        }

        reader.next();

        readDocElements(e, reader, repairing);

        if (repairing && !isDeclared(e, reader.getNamespaceURI(), reader.getPrefix())) {
            declare(e, reader.getNamespaceURI(), reader.getPrefix());
        }

        return e;
    }

    private static boolean isDeclared(Element e, String namespaceURI, String prefix) {
        Attr att;
        if (prefix != null && prefix.length() > 0) {
            att = e.getAttributeNodeNS(XML_NS, "xmlns:" + prefix);
        } else {
            att = e.getAttributeNode("xmlns");
        }

        if (att != null && att.getNodeValue().equals(namespaceURI)) {
            return true;
        }

        if (e.getParentNode() instanceof Element) {
            return isDeclared((Element)e.getParentNode(), namespaceURI, prefix);
        }

        return false;
    }

    /**
     * @param parent
     * @param reader
     * @throws XMLStreamException
     */
    public static void readDocElements(Node parent, XMLStreamReader reader, boolean repairing)
        throws XMLStreamException {
        Document doc = getDocument(parent);

        int event = reader.getEventType();

        while (reader.hasNext()) {
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                startElement(parent, reader, repairing);

                if (parent instanceof Document) {
                    if (reader.hasNext()) {
                        reader.next();
                    }
                    return;
                }

                break;
            case XMLStreamConstants.END_ELEMENT:
                return;
            case XMLStreamConstants.NAMESPACE:
                break;
            case XMLStreamConstants.ATTRIBUTE:
                break;
            case XMLStreamConstants.CHARACTERS:
                if (parent != null) {
                    parent.appendChild(doc.createTextNode(reader.getText()));
                }

                break;
            case XMLStreamConstants.COMMENT:
                if (parent != null) {
                    parent.appendChild(doc.createComment(reader.getText()));
                }

                break;
            case XMLStreamConstants.CDATA:
                parent.appendChild(doc.createCDATASection(reader.getText()));

                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));

                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));

                break;
            default:
                break;
            }

            if (reader.hasNext()) {
                event = reader.next();
            }
        }
    }

    private static void declare(Element node, String uri, String prefix) {
        if (prefix != null && prefix.length() > 0) {
            node.setAttributeNS(XML_NS, "xmlns:" + prefix, uri);
        } else {
            if (uri != null /* && uri.length() > 0 */) {
                node.setAttributeNS(XML_NS, "xmlns", uri);
            }
        }
    }

    /**
     * @param out
     * @param encoding
     * @return
     */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding, Context ctx) {
        XMLOutputFactory factory = getXMLOutputFactory(ctx);

        if (encoding == null) {
            encoding = "UTF-8";
        }

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, encoding);

            return writer;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Couldn't parse stream.", e);
        }
    }

    /**
     * @return
     */
    public static XMLOutputFactory getXMLOutputFactory(Context ctx) {
        if (ctx == null) {
            return xmlOututFactory;
        }

        Object outFactoryObj = ctx.get(XMLOutputFactory.class.getName());

        if (outFactoryObj instanceof XMLOutputFactory) {
            return (XMLOutputFactory)outFactoryObj;
        } else if (outFactoryObj instanceof String) {
            String outFactory = (String)outFactoryObj;
            XMLOutputFactory xof = (XMLOutputFactory)factories.get(outFactory);
            if (xof == null) {
                xof = (XMLOutputFactory)createFactory(outFactory, ctx);
                factories.put(outFactory, xof);
            }
            return xof;
        }

        return xmlOututFactory;
    }

    /**
     * @return
     */
    public static XMLInputFactory getXMLInputFactory(Context ctx) {
        if (ctx == null) {
            return xmlInputFactory;
        }

        Object inFactoryObj = ctx.get(XMLInputFactory.class.getName());

        if (inFactoryObj instanceof XMLInputFactory) {
            return (XMLInputFactory)inFactoryObj;
        } else if (inFactoryObj instanceof String) {
            String inFactory = (String)inFactoryObj;
            XMLInputFactory xif = (XMLInputFactory)factories.get(inFactory);
            if (xif == null) {
                xif = (XMLInputFactory)createFactory(inFactory, ctx);
                configureFactory(xif, ctx);
                factories.put(inFactory, xif);
            }
            return xif;
        }

        if (!inFactoryConfigured) {
            configureFactory(xmlInputFactory, ctx);
            inFactoryConfigured = true;
        }

        return xmlInputFactory;
    }

    private static Boolean getBooleanProperty(Context ctx, String name) {
        Object value = ctx.get(name);
        if (value != null) {
            return Boolean.valueOf(value.toString());

        }
        return null;
    }

    /**
     * @param xif
     * @param ctx
     */
    private static void configureFactory(XMLInputFactory xif, Context ctx) {
        Boolean value = getBooleanProperty(ctx, XMLInputFactory.IS_VALIDATING);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_VALIDATING, value);
        }
        value = getBooleanProperty(ctx, XMLInputFactory.IS_NAMESPACE_AWARE);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, value);
        }

        value = getBooleanProperty(ctx, XMLInputFactory.IS_COALESCING);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_COALESCING, value);
        }

        value = getBooleanProperty(ctx, XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, value);
        }

        value = getBooleanProperty(ctx, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
        if (value != null) {
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, value);
        }
    }

    /**
     * @param factoryClass
     * @return
     */
    private static Object createFactory(String factory, Context ctx) {
        Class factoryClass = null;
        try {
            factoryClass = ClassLoaderUtils.loadClass(factory, ctx.getClass());
            return factoryClass.newInstance();
        } catch (Exception e) {
            LOG.error("Can't create factory for class : " + factory, e);
            throw new DatabindingException("Can't create factory for class : " + factory);
        }
    }

    /**
     * @param in
     * @param encoding
     * @param ctx
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(InputStream in, String encoding, Context ctx) {
        XMLInputFactory factory = getXMLInputFactory(ctx);

        if (encoding == null) {
            encoding = "UTF-8";
        }

        try {
            return factory.createXMLStreamReader(in, encoding);
        } catch (XMLStreamException e) {
            throw new DatabindingException("Couldn't parse stream.", e);
        }
    }

    public static XMLStreamReader createXMLStreamReader(Reader reader) {
        return createXMLStreamReader(reader, null);
    }

    /**
     * @param reader
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(Reader reader, Context context) {
        XMLInputFactory factory = getXMLInputFactory(context);

        try {
            return factory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new DatabindingException("Couldn't parse stream.", e);
        }
    }

}

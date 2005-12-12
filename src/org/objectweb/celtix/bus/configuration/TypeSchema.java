package org.objectweb.celtix.bus.configuration;









import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.objectweb.celtix.bus.jaxb.JAXBUtils;
import org.objectweb.celtix.bus.resource.ResourceManagerImpl;
import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.configuration.ConfigurationException;
import org.objectweb.celtix.configuration.ConfigurationItemMetadata;

public class TypeSchema {

    static final Logger LOG = LogUtils.getL7dLogger(TypeSchema.class);
    
    private Schema schema;
    private Validator validator;
    private final String namespaceURI;
    private String packageName;
    private final Map<String, QName> elementDefinitions;
    private final Map<String, String> typeDefinitions;
    
    /**
     * prevent instantiation
     */
    protected TypeSchema(String nsuri, String location) {
        
        namespaceURI = nsuri;
        elementDefinitions = new HashMap<String, QName>();
        typeDefinitions = new HashMap<String, String>();   
        
        LOG.fine("Creating type schema for namespace " + namespaceURI);

        InputStream is = getSchemaInputStream(location);

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder parser = factory.newDocumentBuilder();
            document = parser.parse(new InputSource(is));
        } catch (ParserConfigurationException ex) {
            throw new ConfigurationException(new Message("PARSER_CONFIGURATION_ERROR_EXC", 
                                                         LOG, location), ex);
        } catch (SAXException ex) {
            throw new ConfigurationException(new Message("PARSE_ERROR_EXC", LOG), ex);
        } catch (IOException ex) {
            throw new ConfigurationException(new Message("FILE_OPEN_ERROR_EXC", LOG, location), ex);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        deserialize(document);

        Source src = new DOMSource(document);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final LSResourceResolver oldResolver = factory.getResourceResolver();
     
        LSResourceResolver resolver = new LSResourceResolver() {

            public LSInput resolveResource(String type, String nsURI,
                                           String publicId, String systemId, String baseURI) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("resolving resource type: " + type + "\n"
                            + "                   namespaceURI:" + nsURI + "\n"
                            + "                   publicId:" + publicId + "\n"
                            + "                   systemId:" + systemId + "\n"
                            + "                   baseURI:" + baseURI);
                }
                
                if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type)) {
                    LSInput lsi = new SchemaInput(type, nsURI, publicId, systemId, baseURI);
                    String resourceName = systemId;
                    // The special case for wsdl.xsd is OK - the other two can be removed
                    // once wsdl.xsd is fixed so that it does not inmport further schemata
                    if (("wsdl.xsd".equals(systemId) 
                           && "http://schemas.xmlsoap.org/wsdl/".equals(nsURI))
                        || ("jms.xsd".equals(systemId) 
                           && "http://celtix.objectweb.org/transports/jms".equals(nsURI))
                        || ("http-conf.xsd".equals(systemId) 
                               && "http://celtix.objectweb.org/transports/http/configuration"
                               .equals(nsURI))) {
                        resourceName = "schemas/wsdl/" + systemId;
                    }
                    lsi.setByteStream(getSchemaInputStream(resourceName));  
                    return lsi;
                } 
                
                if (null != oldResolver) { 
                    return oldResolver.resolveResource(type, nsURI, publicId, systemId, baseURI);
                } else {
                    return null;
                }
            }
        };
        
        factory.setResourceResolver(resolver);        
        try {
            schema = factory.newSchema(src);
        } catch (SAXException ex) {
            throw new ConfigurationException(new Message("SCHEMA_CREATION_ERROR_EXC", LOG, location), ex);
        }
        document = null;
        
        LOG.fine("Created type schema for namespace " + namespaceURI);
    }

    public Validator getValidator() {
        if (null == validator) {
            Schema s = getSchema();
            validator = s.newValidator();
            validator.setErrorHandler(new TypeSchemaErrorHandler());
        }
        return validator;
    }

    public Collection<String> getTypes() {
        return typeDefinitions.keySet();
    }

    public boolean hasType(String typeName) {
        return typeDefinitions.containsKey(typeName);
    }
    
    public Collection<String> getElements() {
        return elementDefinitions.keySet();
    }
    
    public boolean hasElement(String elementName) {
        return elementDefinitions.containsKey(elementName);
    }

    public QName getDeclaredType(String typeName) {
        return elementDefinitions.get(typeName);
    }

    public String getXMLSchemaBaseType(String typeName) {
        if (!hasType(typeName)) {
            throw new ConfigurationException(new Message("TYPE_NOT_DEFINED_IN_NAMESPACE_EXC", LOG,
                                                         typeName, namespaceURI));
                                                      
        }
        return typeDefinitions.get(typeName);
    }

    public String getPackageName() {
        return packageName;
    }

    public Schema getSchema() {
        return schema;
    }
    
    public Object unmarshalDefaultValue(ConfigurationItemMetadata item, Element data) {
        return unmarshalDefaultValue(item, data, false);
    }

    public Object unmarshalDefaultValue(ConfigurationItemMetadata item, Element data, boolean doValidate) {
        try {
            return unmarshal(item.getType(), data, doValidate);
        } catch (JAXBException ex) {
            Message msg = new Message("DEFAULT_VALUE_UNMARSHAL_ERROR_EXC", LOG, item.getName());
            throw new ConfigurationException(msg, ex);
        }
    }
    
    public Object unmarshal(QName type, Element data) throws JAXBException {
        return  unmarshal(type, data, true);
    }

    public Object unmarshal(QName type, Element data, boolean doValidate) throws JAXBException {
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("unmarshalling: element namespaceURI: " + data.getNamespaceURI() + "\n" 
                     + "                       localName: " + data.getLocalName() + "\n"
                     + "             type: " + type  + "\n"
                     + "             type schema package name: " + packageName);            
        }
        JAXBContext context = null;
        Object obj = null;

        context = JAXBContext.newInstance(packageName);
        Unmarshaller u = context.createUnmarshaller();
        if (doValidate) {
            u.setSchema(schema);
        }
        obj = u.unmarshal(data);
        if (obj instanceof JAXBElement<?>) {
            JAXBElement<?> el = (JAXBElement<?>)obj;
            obj = el.getValue();
            /*
             * if (el.getName().equals(type)) { obj = el.getValue(); }
             */
        }

        if (null != obj && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Unmarshaled default value into object of type: " + obj.getClass().getName()
                     + "    value: " + obj);
        }
        return obj;
    }

    private void deserialize(Document document) {
        deseralizePackageName(document);
        deserializeTypes(document);
        deserializeElements(document);
    }

    private void deserializeElements(Document document) {
        Element root = document.getDocumentElement();
        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "element".equals(nd.getLocalName())) {
                String elementName = ((Element)nd).getAttribute("name");

                QName type = ConfigurationMetadataUtils
                    .elementAttributeToQName(document, (Element)nd, "type");

                elementDefinitions.put(elementName, type);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Added type " + type + "  for key: " + elementName);
                }
            }
        }
    }

    private void deserializeTypes(Document document) {
        Element root = document.getDocumentElement();
        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType()
                && ("simpleType".equals(nd.getLocalName()) || ("complexType".equals(nd.getLocalName())))) {

                String typeName = ((Element)nd).getAttribute("name");

                String baseType = null;
                if ("simpleType".equals(nd.getLocalName())) {
                    baseType = getBaseType(document, typeName);
                }

                if (!typeDefinitions.containsKey(typeName)) {
                    typeDefinitions.put(typeName, baseType);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Added base type " + baseType + "  for key: " + typeName);
                    }
                }

            }
        }
    }

    private String getBaseType(Document document, String typeName) {
        String currentType = typeName;
        QName baseType;
        do {
            baseType = getBaseTypeInternal(document, currentType);
            if (null == baseType) {
                LOG.severe(new Message("UNDEFINED_SIMPLE_TYPE_MSG", LOG, typeName).toString());
                return null;
            } else if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(baseType.getNamespaceURI())) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Base type for " + typeName + ": " + baseType);
                }
                return baseType.getLocalPart();
            } else if (!namespaceURI.equals(baseType.getNamespaceURI())) {
                LOG.severe(new Message("SIMPLE_TYPE_DEFINED_IN_OTHER_NAMESPACE_MSG", LOG, typeName,
                                       namespaceURI).toString());
                return null;
            }
            currentType = baseType.getLocalPart();
        } while (true);
    }

    private QName getBaseTypeInternal(Document document, String type) {
        Element root = document.getDocumentElement();
        Element simpleTypeElement = null;

        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "simpleType".equals(nd.getLocalName())
                && ((Element)nd).getAttribute("name").equals(type)) {
                simpleTypeElement = (Element)nd;
            }
        }
        if (null == simpleTypeElement) {
            return null;
        }

        for (Node nd = simpleTypeElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "restriction".equals(nd.getLocalName())) {
                // TODO restriction element can have base attribute OR
                // simpleType
                // child element. Currently we only handle the base attribute
                // case.

                return ConfigurationMetadataUtils.elementAttributeToQName(document, (Element)nd, "base");
            }
        }
        return null;
    }

    private void deseralizePackageName(Document document) {
        Element root = document.getDocumentElement();
        Element annotationElement = null;
        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "annotation".equals(nd.getLocalName())) {
                annotationElement = (Element)nd;
                break;
            }
        }
        Element appInfoElement = null;
        if (null != annotationElement) {
            for (Node nd = annotationElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
                if (Node.ELEMENT_NODE == nd.getNodeType() && "appinfo".equals(nd.getLocalName())) {
                    appInfoElement = (Element)nd;
                    break;
                }
            }
        }
        Element schemaBindingsElement = null;
        if (null != appInfoElement) {
            for (Node nd = appInfoElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
                if (Node.ELEMENT_NODE == nd.getNodeType() && "schemaBindings".equals(nd.getLocalName())) {
                    schemaBindingsElement = (Element)nd;
                    break;
                }
            }
        }
        Element packageElement = null;
        if (null != schemaBindingsElement) {
            for (Node nd = schemaBindingsElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
                if (Node.ELEMENT_NODE == nd.getNodeType() && "package".equals(nd.getLocalName())) {
                    packageElement = (Element)nd;
                    break;
                }
            }
        }

        if (null != packageElement) {
            packageName = packageElement.getAttribute("name");
        } else {
            packageName = JAXBUtils.namespaceURIToPackage(namespaceURI);
        }

        if (null == packageName) {
            throw new ConfigurationException(new Message("MISSING_PACKAGE_NAME_EXC", LOG, namespaceURI));
        }

    }
    
    static InputStream getSchemaInputStream(String location) {
        URI uri = null;
        try {
            uri = new URI(location);
        } catch (URISyntaxException ex) {
            Message msg = new Message("SCHEMA_LOCATION_ERROR_EXC", LOG, location);
            throw new ConfigurationException(msg, ex);
        }

        InputStream is = null;

        if (uri.isAbsolute()) {
            if ("file".equals(uri.getScheme())) {
                String path = uri.getPath();
                if (null == path) {
                    Message msg = new Message("FILE_OPEN_ERROR_EXC", LOG, location);
                    throw new ConfigurationException(msg);
                }
                try {
                    is = new FileInputStream(path);
                } catch (IOException ex) {
                    Message msg = new Message("FILE_OPEN_ERROR_EXC", LOG, location);
                    throw new ConfigurationException(msg, ex);
                }
            } else {
                Message msg = new Message("SCHEMA_LOCATION_ERROR_EXC", LOG, location);
                throw new ConfigurationException(msg);
            }
        } else { 
            // uri path is a system resource             
            is = ResourceManagerImpl.instance().getResourceAsStream(location);
            if (null == is) {
                throw new ConfigurationException(new Message("SCHEMA_LOCATION_ERROR_EXC", LOG, location));
            }
            
        }
        return is;
    }

    // ErrorHandler interface

    final class TypeSchemaErrorHandler implements ErrorHandler {

        public void error(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXParseException {
            throw exception;
        }
    }
    
    static final class SchemaInput implements LSInput {
        String type;
        String namespaceURI;
        String publicId;
        String systemId;
        String baseURI;
        InputStream is;
        
        SchemaInput(String t, String nsuri, String pid, String sid, String buri) {
            type = t;
            namespaceURI = nsuri;
            publicId = pid;
            systemId = sid;
            baseURI = buri;
        }
        
        public String getBaseURI() {
            return baseURI;
        }

        public InputStream getByteStream() {
            return is;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getPublicId() {
            return publicId;
        }

        public String getStringData() {
            return null;
        }

        public String getSystemId() {
            return systemId;
        }

        public void setBaseURI(String buri) {
            baseURI = buri;
        }

        public void setByteStream(InputStream byteStream) {
            is = byteStream;
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setPublicId(String pid) {
            publicId = pid;
        }

        public void setStringData(String stringData) {
        }

        public void setSystemId(String sid) {
            systemId = sid;
        }
            
    }
}

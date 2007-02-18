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

package org.apache.cxf.jaxb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxb.io.EventDataReader;
import org.apache.cxf.jaxb.io.EventDataWriter;
import org.apache.cxf.jaxb.io.NodeDataReader;
import org.apache.cxf.jaxb.io.NodeDataWriter;
import org.apache.cxf.jaxb.io.XMLStreamDataReader;
import org.apache.cxf.jaxb.io.XMLStreamDataWriter;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

public final class JAXBDataBinding implements DataBinding {

    public static final String SCHEMA_RESOURCE = "SCHEMRESOURCE";
    
    public static final String UNWRAP_JAXB_ELEMENT = "unwrap.jaxb.element";

    private static final Logger LOG = Logger.getLogger(JAXBDataBinding.class.getName());

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXBDataBinding.class);
    
    private static final Class<?> SUPPORTED_READER_FORMATS[] = new Class<?>[] {Node.class,
                                                                               XMLEventReader.class,
                                                                               XMLStreamReader.class};
    private static final Class<?> SUPPORTED_WRITER_FORMATS[] = new Class<?>[] {Node.class,
                                                                               XMLEventWriter.class,
                                                                               XMLStreamWriter.class};

    
    JAXBContext context;

    Class cls;

    public JAXBDataBinding() {
    }
    
    public JAXBDataBinding(Class<?>...classes) throws JAXBException {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        classSet.addAll(Arrays.asList(classes));
        setContext(createJAXBContext(classSet));
    }

    public JAXBDataBinding(JAXBContext context) {
        this();
        setContext(context);
    }

    public void setContext(JAXBContext ctx) {
        context = ctx;
    }    
    
    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> c) {
        if (c == XMLStreamWriter.class) {
            return (DataWriter<T>)new XMLStreamDataWriter(context);
        } else if (c == XMLEventWriter.class) {
            return (DataWriter<T>)new EventDataWriter(context);            
        } else if (c == Node.class) {
            return (DataWriter<T>)new NodeDataWriter(context);
        }
        
        return null;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> c) {
        DataReader<T> dr = null;
        if (c == XMLStreamReader.class) {
            dr = (DataReader<T>)new XMLStreamDataReader(context);
        } else if (c == XMLEventReader.class) {
            dr = (DataReader<T>)new EventDataReader(context);
        } else if (c == Node.class) {
            dr = (DataReader<T>)new NodeDataReader(context);
        }
        
        // TODO Auto-generated method stub
        return dr;
    }

    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }
    
    public Map<String, SchemaInfo> getSchemas(ServiceInfo serviceInfo) {
        Collection<String> schemaResources = CastUtils
            .cast(serviceInfo.getProperty(SCHEMA_RESOURCE, List.class), String.class);

        return loadSchemas(schemaResources);
    }

    private Map<String, SchemaInfo> loadSchemas(Collection<String> schemaResources) {
        Map<String, SchemaInfo> schemas = new HashMap<String, SchemaInfo>();
        for (String schema : schemaResources) {
            URIResolver resolver = null;
            try {
                resolver = new URIResolver(schema);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage());
            }
            if (!resolver.isResolved()) {
                throw new UncheckedException(new Message("SCHEMA_NOT_RESOLVED", BUNDLE, schema));
            }
            if (resolver.isFile()) {
                // load schemas from file system
                loadSchemaFromFile(schema, schemas);
            } else {
                // load schemas from classpath
                loadSchemaFromClassPath(schema, schemas);
            }
        }
        return schemas;
    }

    private void loadSchemaFromClassPath(String schema, Map<String, SchemaInfo> schemas) {
        // we can reuse code in javatowsdl tool after tool refactor
    }

    private void loadSchemaFromFile(String schema, Map<String, SchemaInfo> schemas) {
        try {
            XmlSchemaCollection schemaCol = new XmlSchemaCollection();
            URIResolver resolver = new URIResolver(schema);
            schemaCol.setBaseUri(resolver.getFile().getParent());

            Document schemaDoc = DOMUtils.readXml(resolver.getInputStream());

            XmlSchema xmlSchema = schemaCol.read(schemaDoc.getDocumentElement());
            SchemaInfo schemaInfo = new SchemaInfo(null, xmlSchema.getTargetNamespace());

            schemaInfo.setElement(schemaDoc.getDocumentElement());
            schemas.put(schemaInfo.getNamespaceURI(), schemaInfo);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        } catch (SAXException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        } catch (ParserConfigurationException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        }
    }

    public void initialize(ServiceInfo serviceInfo) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        JAXBContextInitializer initializer = 
            new JAXBContextInitializer(serviceInfo, classes);
        initializer.walk();

        try {
            setContext(createJAXBContext(classes));
        } catch (JAXBException e1) {
            throw new ServiceConstructionException(e1);
        }
        
        XmlSchemaCollection col = (XmlSchemaCollection)serviceInfo
            .getProperty(WSDLServiceBuilder.WSDL_SCHEMA_LIST);

        if (col != null) {
            // someone has already filled in the types
            return;
        }

        col = new XmlSchemaCollection();

        try {
            for (DOMResult r : generateJaxbSchemas()) {
                Document d = (Document)r.getNode();
                String ns = d.getDocumentElement().getAttribute("targetNamespace");
                if (ns == null) {
                    ns = "";
                }

                NodeList nodes = d.getDocumentElement().getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    if (n instanceof Element) {
                        Element e = (Element) n;
                        if (e.getLocalName().equals("import")) {
                            d.getDocumentElement().removeChild(e);
                        }
                    }
                }
                
                // Don't include WS-Addressing bits
                if ("http://www.w3.org/2005/08/addressing/wsdl".equals(ns)) {
                    continue;
                }

                SchemaInfo schema = new SchemaInfo(serviceInfo, ns);
                schema.setElement(d.getDocumentElement());
                schema.setSystemId(r.getSystemId());
                serviceInfo.addSchema(schema);
                col.read(d.getDocumentElement());
            }
        } catch (IOException e) {
            throw new ServiceConstructionException(new Message("SCHEMA_GEN_EXC", BUNDLE), e);
        }

        serviceInfo.setProperty(WSDLServiceBuilder.WSDL_SCHEMA_LIST, col);
        JAXBSchemaInitializer schemaInit = new JAXBSchemaInitializer(serviceInfo, col);
        schemaInit.walk();

    }

    private List<DOMResult> generateJaxbSchemas() throws IOException {
        final List<DOMResult> results = new ArrayList<DOMResult>();

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();
                result.setSystemId(file);
                results.add(result);

                return result;
            }
        });

        return results;
    }
    


    public static JAXBContext createJAXBContext(Set<Class<?>> classes) throws JAXBException {
        Iterator it = classes.iterator();
        String className = "";
        Object remoteExceptionObject = null;
        while (it.hasNext()) {
            remoteExceptionObject = (Class)it.next();
            className = remoteExceptionObject.toString();
            if (!("".equals(className)) && className.contains("RemoteException")) {
                classes.remove(remoteExceptionObject);
            }
        }

        try {
            classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.AttributedQNameType"));
            classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ObjectFactory"));
            classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ServiceNameType"));
        } catch (ClassNotFoundException e) {
            // REVISIT - ignorable if WS-ADDRESSING not available?
            // maybe add a way to allow interceptors to add stuff to the
            // context?
        }
       
        return  JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
    }

}

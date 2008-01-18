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
package org.apache.cxf.aegis.databinding;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AbstractTypeCreator.TypeClassInfo;
import org.apache.cxf.aegis.type.Configuration;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.source.AbstractDataBinding;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.frontend.SimpleMethodDispatcher;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.DOMOutputter;

/**
 * CXF databinding object for Aegis. 
 */
public class AegisDatabinding extends AbstractDataBinding implements DataBinding {
    
    // these are here only for compatibility.
    /**
     * @deprecated 2.1
     */
    public static final String WRITE_XSI_TYPE_KEY = "writeXsiType";
    /**
     * @deprecated 2.1
     */
    public static final String OVERRIDE_TYPES_KEY = "overrideTypesList";
    /**
     * @deprecated 2.1
     */
    public static final String READ_XSI_TYPE_KEY = "readXsiType";
    
    protected static final int IN_PARAM = 0;
    protected static final int OUT_PARAM = 1;
    protected static final int FAULT_PARAM = 2;
    
    private static final Logger LOG = LogUtils.getL7dLogger(AegisDatabinding.class);

    private AegisContext aegisContext;
    private Map<MessagePartInfo, Type> part2Type;
    private Service service;
    private boolean isInitialized;
    private Set<String> overrideTypes;
    private Configuration configuration;
    private boolean mtomEnabled;

    public AegisDatabinding() {
        super();
        part2Type = new HashMap<MessagePartInfo, Type>();
    }
    
    /**
     * The Databinding API has initialize(Service). However, this object should be usable even if that
     * API is never called.
     */
    private void ensureInitialized() {
        if (!isInitialized) {
            if (aegisContext == null) {
                aegisContext = new AegisContext();
                if (overrideTypes != null) {
                    aegisContext.setRootClassNames(overrideTypes);
                }
                if (configuration != null) {
                    aegisContext.setConfiguration(configuration);
                }
                if (mtomEnabled) {
                    aegisContext.setMtomEnabled(true);
                }
                aegisContext.initialize();
            }
            isInitialized = true;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> cls) {
        ensureInitialized();
        if (cls.equals(XMLStreamReader.class)) {
            return (DataReader<T>) new XMLStreamDataReader(this);
        } else if (cls.equals(Node.class)) {
            return (DataReader<T>) new ElementDataReader(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> cls) {
        ensureInitialized();
        if (cls.equals(XMLStreamWriter.class)) {
            return (DataWriter<T>)new XMLStreamDataWriter(this);
        } else if (cls.equals(Node.class)) {
            return (DataWriter<T>) new ElementDataWriter(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Class<?>[] getSupportedReaderFormats() {
        return new Class[] {XMLStreamReader.class, Node.class};
    }

    /**
     * {@inheritDoc}
     */
    public Class<?>[] getSupportedWriterFormats() {
        return new Class[] {XMLStreamWriter.class, Node.class};
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(Service s) {
        
        // We want to support some compatibility configuration properties
        // definitionally, anyone doing the compatibility thing has not made their
        // own AegisContext object.
        if (aegisContext == null) {
            aegisContext = new AegisContext();

            Object val = s.get(READ_XSI_TYPE_KEY);
            if ("false".equals(val) || Boolean.FALSE.equals(val)) {
                aegisContext.setReadXsiTypes(false);
            }
            
            val = s.get(WRITE_XSI_TYPE_KEY);
            if ("true".equals(val) || Boolean.TRUE.equals(val)) {
                aegisContext.setWriteXsiTypes(true);
            }
            
            val = s.get(OVERRIDE_TYPES_KEY);
            if (val != null) {
                Collection nameCollection = (Collection) val;
                Collection<String> typeNames = CastUtils.cast(nameCollection, String.class);
                if (overrideTypes == null) {
                    overrideTypes = new HashSet<String>();
                }
                overrideTypes.addAll(typeNames);
            }
            
            val = s.get("mtom-enabled");
            if ("true".equals(val) || Boolean.TRUE.equals(val) || mtomEnabled) {
                aegisContext.setMtomEnabled(true);
            }
            
            Map<Class<?>, String> implMap = new HashMap<Class<?>, String>();
            // now for a really annoying case, the .implementation objects.
            for (String key : s.keySet()) {
                if (key.endsWith(".implementation")) {
                    String className = key.substring(0, key.length() - ".implementation".length());
                    Class<?> clazz = null;
                    try {
                        clazz = ClassLoaderUtils.loadClass(className, getClass());
                    } catch (ClassNotFoundException e) {
                        Message message = new Message("MAPPED_CLASS_NOT_FOUND", LOG, className, key);
                        LOG.warning(message.toString());
                        continue;
                    }
                    String implClassName = (String)s.get(key);
                    implMap.put(clazz, implClassName);
                }
            }

            if (overrideTypes != null) {
                aegisContext.setRootClassNames(overrideTypes);
            }
            if (configuration != null) {
                aegisContext.setConfiguration(configuration);
            }
            
            if (implMap.size() > 0) {
                aegisContext.setBeanImplementationMap(implMap);
            }
        }
        
        aegisContext.setMappingNamespaceURI(s.getServiceInfos().get(0).getName().getNamespaceURI());
        aegisContext.initialize();
        this.service = s;
        
        Set<Type> deps = new HashSet<Type>();

        for (ServiceInfo info : s.getServiceInfos()) {
            for (OperationInfo opInfo : info.getInterface().getOperations()) {
                if (opInfo.isUnwrappedCapable()) {
                    initializeOperation(s, aegisContext.getTypeMapping(), 
                                        opInfo.getUnwrappedOperation(), deps);
                } else {
                    initializeOperation(s, aegisContext.getTypeMapping(), opInfo, deps);
                }
            }
        }

        Collection<Type> additional = aegisContext.getRootTypes();

        if (additional != null) {
            for (Type t : additional) {
                if (!deps.contains(t)) {
                    deps.add(t);
                }
            }
        }

        createSchemas(s, deps);
        for (ServiceInfo info : s.getServiceInfos()) {
            for (OperationInfo opInfo : info.getInterface().getOperations()) {
                if (opInfo.isUnwrappedCapable()) {
                    initializeOperationTypes(info, opInfo.getUnwrappedOperation());
                } else {
                    initializeOperationTypes(info, opInfo);
                }
            }
        }
    }

    private void initializeOperation(Service s, TypeMapping serviceTM, OperationInfo opInfo,
                                     Set<Type> deps) {
        try {
            initializeMessage(s, serviceTM, opInfo.getInput(), IN_PARAM, deps);

            if (opInfo.hasOutput()) {
                initializeMessage(s, serviceTM, opInfo.getOutput(), OUT_PARAM, deps);
            }

            for (FaultInfo info : opInfo.getFaults()) {
                initializeMessage(s, serviceTM, info, FAULT_PARAM, deps);
            }

        } catch (DatabindingException e) {
            e.prepend("Error initializing parameters for operation " + opInfo.getName());
            throw e;
        }
    }
    private void initializeOperationTypes(ServiceInfo s, OperationInfo opInfo) {
        try {
            initializeMessageTypes(s, opInfo.getInput(), IN_PARAM);

            if (opInfo.hasOutput()) {
                initializeMessageTypes(s, opInfo.getOutput(), OUT_PARAM);
            }

            for (FaultInfo info : opInfo.getFaults()) {
                initializeMessageTypes(s, info, FAULT_PARAM);
            }

        } catch (DatabindingException e) {
            e.prepend("Error initializing parameters for operation " + opInfo.getName());
            throw e;
        }
    }

    protected void initializeMessage(Service s, TypeMapping serviceTM,
                                     AbstractMessageContainer container, 
                                     int partType, Set<Type> deps) {
        for (Iterator itr = container.getMessageParts().iterator(); itr.hasNext();) {
            MessagePartInfo part = (MessagePartInfo)itr.next();

            Type type = getParameterType(s, serviceTM, part, partType);

            if (part.getXmlSchema() == null) {
                //schema hasn't been filled in yet
                if (type.isAbstract()) {
                    part.setTypeQName(type.getSchemaType());
                } else {
                    part.setElementQName(type.getSchemaType());
                }
            }

            part2Type.put(part, type);

            // QName elName = getSuggestedName(service, op, param)
            deps.add(type);

            addDependencies(deps, type);
        }
    }

    protected void initializeMessageTypes(ServiceInfo s,
                                     AbstractMessageContainer container, 
                                     int partType) {
        SchemaCollection col = s.getXmlSchemaCollection();
        for (Iterator itr = container.getMessageParts().iterator(); itr.hasNext();) {
            MessagePartInfo part = (MessagePartInfo)itr.next();
            if (part.getXmlSchema() == null) {
                if (part.isElement()) {
                    XmlSchemaAnnotated tp = col.getElementByQName(part.getElementQName());
                    part.setXmlSchema(tp);
                } else {
                    XmlSchemaAnnotated tp = col.getTypeByQName(part.getTypeQName());
                    part.setXmlSchema(tp);
                }
            }
        }
    }
    private void addDependencies(Set<Type> deps, Type type) {
        Set<Type> typeDeps = type.getDependencies();
        if (typeDeps != null) {
            for (Type t : typeDeps) {
                if (!deps.contains(t)) {
                    deps.add(t);
                    addDependencies(deps, t);
                }
            }
        }
    }

    private void createSchemas(Service s, Set<Type> deps) {

        Map<String, Set<Type>> tns2Type = new HashMap<String, Set<Type>>();
        for (Type t : deps) {
            String ns = t.getSchemaType().getNamespaceURI();
            Set<Type> types = tns2Type.get(ns);
            if (types == null) {
                types = new HashSet<Type>();
                tns2Type.put(ns, types);
            }
            types.add(t);
        }
        for (ServiceInfo si : s.getServiceInfos()) {
            SchemaCollection col = si.getXmlSchemaCollection();
            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                continue;
            }
        }

        Map<String, String> namespaceMap = getDeclaredNamespaceMappings();
        
        for (Map.Entry<String, Set<Type>> entry : tns2Type.entrySet()) {
            String xsdPrefix = SOAPConstants.XSD_PREFIX;
            if (namespaceMap != null && namespaceMap.containsKey(SOAPConstants.XSD)) {
                xsdPrefix = namespaceMap.get(SOAPConstants.XSD);
            }
            
            Element e = new Element("schema", xsdPrefix, SOAPConstants.XSD);

            e.setAttribute(new Attribute(WSDLConstants.ATTR_TNS, entry.getKey()));
            
            if (null != namespaceMap) { // did application hand us some additional namespaces?
                for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                    // user gives us namespace->prefix mapping. 
                    e.addNamespaceDeclaration(Namespace.getNamespace(mapping.getValue(),
                                                                     mapping.getKey())); 
                }
            }

            // if the user didn't pick something else, assign 'tns' as the prefix.
            if (namespaceMap == null || !namespaceMap.containsKey(entry.getKey())) {
                // Schemas are more readable if there is a specific prefix for the TNS.
                e.addNamespaceDeclaration(Namespace.getNamespace(WSDLConstants.CONVENTIONAL_TNS_PREFIX, 
                                                                 entry.getKey()));
            }
            e.setAttribute(new Attribute("elementFormDefault", "qualified"));
            e.setAttribute(new Attribute("attributeFormDefault", "qualified"));

            for (Type t : entry.getValue()) {
                t.writeSchema(e);
            }

            if (e.getChildren().size() == 0) {
                continue;
            }

            try {
                NamespaceMap nsMap = new NamespaceMap();
                
                nsMap.add(xsdPrefix, SOAPConstants.XSD);
                
                // We prefer explicit prefixes over those generated in the types.
                // This loop may have intended to support prefixes from individual aegis files,
                // but that isn't a good idea. 
                for (Iterator itr = e.getAdditionalNamespaces().iterator(); itr.hasNext();) {
                    Namespace n = (Namespace) itr.next();
                    if (!nsMap.containsValue(n.getURI())) {
                        nsMap.add(n.getPrefix(), n.getURI());
                    }
                }

                org.w3c.dom.Document schema = new DOMOutputter().output(new Document(e));

                for (ServiceInfo si : s.getServiceInfos()) {
                    SchemaCollection col = si.getXmlSchemaCollection();
                    col.setNamespaceContext(nsMap);
                    XmlSchema xmlSchema = addSchemaDocument(si, col, schema, entry.getKey());
                    // Work around bug in JDOM DOMOutputter which fails to correctly
                    // assign namespaces to attributes. If JDOM worked right, 
                    // the collection object would get the prefixes for itself.
                    xmlSchema.setNamespaceContext(nsMap);
                }
            } catch (JDOMException e1) {
                throw new ServiceConstructionException(e1);
            }
        }

    }

    public QName getSuggestedName(Service s, TypeMapping tm, OperationInfo op, int param) {
        Method m = getMethod(s, op);
        if (m == null) {
            return null;
        }

        QName name = tm.getTypeCreator().getElementName(m, param);

        // No mapped name was specified, so if its a complex type use that name
        // instead
        if (name == null) {
            Type type = tm.getTypeCreator().createType(m, param);

            if (type.isComplex() && !type.isAbstract()) {
                name = type.getSchemaType();
            }
        }

        return name;
    }
    
    private Type getParameterType(Service s, TypeMapping tm, MessagePartInfo param, int paramtype) {
        Type type = tm.getType(param.getTypeQName());

        /*
         * if (type == null && tm.isRegistered(param.getTypeClass())) { type =
         * tm.getType(param.getTypeClass()); part2type.put(param, type); }
         */

        int offset = 0;
        if (paramtype == OUT_PARAM) {
            offset = 1;
        }
        
        TypeCreator typeCreator = tm.getTypeCreator();
        if (type == null) {
            OperationInfo op = param.getMessageInfo().getOperation();

            Method m = getMethod(s, op);
            TypeClassInfo info;
            if (paramtype != FAULT_PARAM && m != null) {
                info = typeCreator.createClassInfo(m, param.getIndex() - offset);
            } else {
                info = typeCreator.createBasicClassInfo(param.getTypeClass());
            }
            if (param.getMessageInfo().getOperation().isUnwrapped()
                && param.getTypeClass().isArray()) {
                //The service factory expects arrays going into the wrapper to be
                //mapped to the array component type and will then add
                //min=0/max=unbounded.   That doesn't work for Aegis where we
                //already created a wrapper ArrayType so we'll let it know we want the default.
                param.setProperty("minOccurs", "1");
                param.setProperty("maxOccurs", "1");
                param.setProperty("nillable", Boolean.TRUE);
            }
            if (info.getMappedName() != null) {
                param.setConcreteName(info.getMappedName());
                param.setName(info.getMappedName());
            }
            type = typeCreator.createTypeForClass(info);
            // We have to register the type if we want minOccurs and such to work.
            if (info.nonDefaultAttributes()) {
                tm.register(type);
            }
            type.setTypeMapping(tm);

            part2Type.put(param, type);
        }

        return type;
    }

    private Method getMethod(Service s, OperationInfo op) {
        MethodDispatcher md = (MethodDispatcher)s.get(MethodDispatcher.class.getName());
        SimpleMethodDispatcher smd = (SimpleMethodDispatcher)md;
        return smd.getPrimaryMethod(op);
    }

    public Type getType(MessagePartInfo part) {
        return part2Type.get(part);
    }

    public Service getService() {
        return service;
    }

    public AegisContext getAegisContext() {
        ensureInitialized();
        return aegisContext;
    }

    public void setAegisContext(AegisContext aegisContext) {
        this.aegisContext = aegisContext;
    }

    public void setOverrideTypes(Set<String> types) {
        overrideTypes = types;
    }
    
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }
}

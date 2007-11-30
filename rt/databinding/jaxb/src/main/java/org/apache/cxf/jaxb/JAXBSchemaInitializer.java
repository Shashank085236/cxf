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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.namespace.QName;

import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.bind.v2.runtime.JaxBeanInfo;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaTools;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * Walks the service model and sets up the element/type names.
 */
class JAXBSchemaInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getLogger(JAXBSchemaInitializer.class);

    private SchemaCollection schemas;
    private JAXBContextImpl context;
    
    public JAXBSchemaInitializer(ServiceInfo serviceInfo, SchemaCollection col, JAXBContextImpl context) {
        super(serviceInfo);
        schemas = col;
        this.context = context;
    }

    @Override
    public void begin(MessagePartInfo part) {
        // Check to see if the WSDL information has been filled in for us.
        if (part.getTypeQName() != null || part.getElementQName() != null) {
            checkForExistence(part);
            return;
        }
        
        Class<?> clazz = part.getTypeClass();
        if (clazz == null) {
            return;
        }

        boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
        if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
            clazz = clazz.getComponentType();
        }

        JaxBeanInfo<?> beanInfo = context.getBeanInfo(clazz);
        if (beanInfo == null) {
            if (Exception.class.isAssignableFrom(clazz)) {
                QName name = (QName)part.getMessageInfo().getProperty("elementName");
                part.setElementQName(name);
                buildExceptionType(part, clazz);
            }
            return;
        }
        
        boolean isElement = beanInfo.isElement();

        part.setElement(isElement);
        
        if (isElement) {
            QName name = new QName(beanInfo.getElementNamespaceURI(null), 
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el = schemas.getElementByQName(name);
            if (el != null && el.getRefName() != null) {
                part.setTypeQName(el.getRefName());
            } else {
                part.setElementQName(name);
            }
            part.setXmlSchema(el);
        } else  {
            QName typeName = getTypeName(beanInfo);
            if (typeName != null) {
                part.setTypeQName(typeName);
                part.setXmlSchema(schemas.getTypeByQName(typeName));
            }
        }
    }

    private QName getTypeName(JaxBeanInfo<?> beanInfo) {
        Iterator<QName> itr = beanInfo.getTypeNames().iterator();
        if (!itr.hasNext()) {
            return null;
        }
        
        return itr.next();
    } 
    public void checkForExistence(MessagePartInfo part) {
        QName qn = part.getElementQName();
        if (qn != null) {
            XmlSchemaElement el = schemas.getElementByQName(qn);
            if (el == null) {
                Class<?> clazz = part.getTypeClass();
                if (clazz == null) {
                    return;
                }

                boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
                if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
                    clazz = clazz.getComponentType();
                }
                JaxBeanInfo<?> beanInfo = context.getBeanInfo(clazz);
                if (beanInfo == null) {
                    if (Exception.class.isAssignableFrom(clazz)) {
                        QName name = (QName)part.getMessageInfo().getProperty("elementName");
                        part.setElementQName(name);
                        buildExceptionType(part, clazz);
                    }
                    return;
                }
                
                QName typeName = getTypeName(beanInfo);

                createBridgeXsElement(part, qn, typeName);
            } else if (part.getXmlSchema() == null) {
                part.setXmlSchema(el);
            }
        }
    }

    private void createBridgeXsElement(MessagePartInfo part, QName qn, QName typeName) {
        XmlSchemaElement el = null;
        SchemaInfo schemaInfo = serviceInfo.getSchema(qn.getNamespaceURI());
        if (schemaInfo != null) {
            el = schemaInfo.getElementByQName(qn);
            if (el == null) {
                el = createXsElement(part, typeName, schemaInfo);

                schemaInfo.getSchema().getElements().add(el.getQName(), el);
                schemaInfo.getSchema().getItems().add(el);
            } else if (!typeName.equals(el.getSchemaTypeName())) {
                throw new Fault(new Message("CANNOT_CREATE_ELEMENT", LOG, 
                                            qn, typeName, el.getSchemaTypeName()));
            }
            return;
        }
        schemaInfo = new SchemaInfo(serviceInfo, qn.getNamespaceURI());
        el = createXsElement(part, typeName, schemaInfo);

        XmlSchema schema = schemas.newXmlSchemaInCollection(qn.getNamespaceURI());
        schemaInfo.setSchema(schema);
        schema.getElements().add(el.getQName(), el);
        schema.getItems().add(el);

        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, schema.getTargetNamespace());
        nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NU_SCHEMA_XSD);
        schema.setNamespaceContext(nsMap);
        
        serviceInfo.addSchema(schemaInfo);
    }

    private XmlSchemaElement createXsElement(MessagePartInfo part, QName typeName, SchemaInfo schemaInfo) {
        XmlSchemaElement el = new XmlSchemaElement();
        XmlSchemaTools.setElementQName(el, part.getElementQName());
        el.setNillable(true);
        el.setSchemaTypeName(typeName);
        part.setXmlSchema(el);
        return el;
    }
    
    public void end(FaultInfo fault) {
        MessagePartInfo part = fault.getMessageParts().get(0); 
        Class<?> cls = part.getTypeClass();
        Class<?> cl2 = (Class)fault.getProperty(Class.class.getName());
        if (cls != cl2) {            
            QName name = (QName)fault.getProperty("elementName");
            part.setElementQName(name);           
            JaxBeanInfo<?> beanInfo = context.getBeanInfo(cls);
            if (beanInfo == null) {
                throw new Fault(new Message("NO_BEAN_INFO", LOG, cls.getName()));
            }
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo != null
                && !isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {
                    
                XmlSchemaElement el = new XmlSchemaElement();
                XmlSchemaTools.setElementQName(el, part.getElementQName());
                el.setNillable(true);
                
                schemaInfo.getSchema().getItems().add(el);
                schemaInfo.getSchema().getElements().add(el.getQName(), el);

                Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();
                el.setSchemaTypeName(typeName);
            }
        } else if (part.getXmlSchema() == null) {
            try {
                cls.getConstructor(new Class[] {String.class});
            } catch (Exception e) {
                try {
                    cls.getConstructor(new Class[0]);
                } catch (Exception e2) {
                    //no String or default constructor, we cannot use it
                    return;
                }
            }            
            
            //not mappable in JAXBContext directly, we'll have to do it manually :-(
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo == null
                || isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {
                return;
            }
                
            XmlSchemaElement el = new XmlSchemaElement();
            XmlSchemaTools.setElementQName(el, part.getElementQName());
            
            schemaInfo.getSchema().getItems().add(el);
            schemaInfo.getSchema().getElements().add(el.getQName(), el);

            part.setXmlSchema(el);

            XmlSchemaComplexType ct = new XmlSchemaComplexType(schemaInfo.getSchema());
            el.setSchemaType(ct);
            XmlSchemaSequence seq = new XmlSchemaSequence();
            ct.setParticle(seq);
                
            Method methods[] = cls.getMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("get")
                    || m.getName().startsWith("is")) {
                    int beginIdx = m.getName().startsWith("get") ? 3 : 2;
                    try {
                        m.getDeclaringClass().getMethod("set" + m.getName().substring(beginIdx),
                                                        m.getReturnType());
                        
                        JaxBeanInfo<?> beanInfo = context.getBeanInfo(m.getReturnType());
                        if (beanInfo != null) {
                            el = new XmlSchemaElement();
                            el.setName(m.getName().substring(beginIdx));
                            
                            String ns = schemaInfo.getSchema().getElementFormDefault()
                                .getValue().equals(XmlSchemaForm.UNQUALIFIED) 
                                ? "" : part.getElementQName().getLocalPart();
                            XmlSchemaTools.setElementQName(el, 
                                                           new QName(ns, m.getName().substring(beginIdx)));
                            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                            if (!itr.hasNext()) {
                                return;
                            }
                            QName typeName = itr.next();
                            el.setSchemaTypeName(typeName);
                        }
                        
                        seq.getItems().add(el);
                    } catch (Exception e) {
                        //not mappable
                    }
                }
            }
        }
    }

    
    private void buildExceptionType(MessagePartInfo part, Class<?> cls) {
        SchemaInfo schemaInfo = null;
        for (SchemaInfo s : serviceInfo.getSchemas()) {
            if (s.getNamespaceURI().equals(part.getElementQName().getNamespaceURI())) {
                schemaInfo = s;                
                break;
            }
        }
        XmlSchema schema;
        if (schemaInfo == null) {
            schema = schemas.newXmlSchemaInCollection(part.getElementQName().getNamespaceURI());
            schema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));

            NamespaceMap nsMap = new NamespaceMap();
            nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, schema.getTargetNamespace());
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NU_SCHEMA_XSD);
            schema.setNamespaceContext(nsMap);

            
            schemaInfo = new SchemaInfo(serviceInfo, part.getElementQName().getNamespaceURI());
            schemaInfo.setSchema(schema);
            serviceInfo.addSchema(schemaInfo);
        } else {
            schema = schemaInfo.getSchema();
        }
        
        XmlSchemaComplexType ct = new XmlSchemaComplexType(schema);
        ct.setName(part.getElementQName().getLocalPart());
        // Before updating everything, make sure we haven't added this 
        // type yet.  Multiple methods that throw the same exception 
        // types will cause duplicates. 
        if (schema.getTypeByName(ct.getQName()) != null) {
            return; 
        }
        
        XmlSchemaElement el = new XmlSchemaElement();
        XmlSchemaTools.setElementQName(el, part.getElementQName());
        schema.getItems().add(el);
        schema.getElements().add(el.getQName(), el);
        part.setXmlSchema(el);
        
        schema.getItems().add(ct);
        schema.addType(ct);
        el.setSchemaTypeName(part.getElementQName());
        
        XmlSchemaSequence seq = new XmlSchemaSequence();
        ct.setParticle(seq);
        String namespace = part.getElementQName().getNamespaceURI();
        
        XmlAccessorType accessorType = cls.getAnnotation(XmlAccessorType.class);
        if (accessorType == null && cls.getPackage() != null) {
            accessorType = cls.getPackage().getAnnotation(XmlAccessorType.class);
        }
        XmlAccessType accessType = accessorType != null ? accessorType.value() : XmlAccessType.PUBLIC_MEMBER;

        
        for (Field f : cls.getDeclaredFields()) {
            if (JAXBContextInitializer.isFieldAccepted(f, accessType)) {
                //map field
                JaxBeanInfo<?> beanInfo = context.getBeanInfo(f.getType());
                if (beanInfo != null) {
                    addElement(seq, beanInfo, new QName(namespace, f.getName()));
                }                
            }
        }
        for (Method m : cls.getMethods()) {
            if (JAXBContextInitializer.isMethodAccepted(m, accessType)) {
                //map field
                JaxBeanInfo<?> beanInfo = context.getBeanInfo(m.getReturnType());
                if (beanInfo != null) {
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    addElement(seq, beanInfo, new QName(namespace, name));
                }                
            }
        }
        part.setProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION", Boolean.TRUE);
    }
    
    public void addElement(XmlSchemaSequence seq, JaxBeanInfo<?> beanInfo, QName name) {    
        XmlSchemaElement el = new XmlSchemaElement();
        el.setName(name.getLocalPart());
        XmlSchemaTools.setElementQName(el, name);

        el.setMinOccurs(1);
        el.setMaxOccurs(1);
        el.setNillable(true);

        if (beanInfo.isElement()) {
            QName ename = new QName(beanInfo.getElementNamespaceURI(null), 
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el2 = schemas.getElementByQName(ename);
            XmlSchemaTools.setElementQName(el, null);
            XmlSchemaTools.setElementRefName(el, el2.getRefName());
        } else {
            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
            if (!itr.hasNext()) {
                return;
            }
            QName typeName = itr.next();
            el.setSchemaTypeName(typeName);
        }
        
        seq.getItems().add(el);
    }
    
    
    private boolean isExistSchemaElement(XmlSchema schema, QName qn) {
        boolean isExist = false;
        for (Iterator ite = schema.getItems().getIterator(); ite.hasNext();) {
            XmlSchemaObject obj = (XmlSchemaObject)ite.next();
            if (obj instanceof XmlSchemaElement) {
                XmlSchemaElement xsEle = (XmlSchemaElement)obj;
                if (xsEle.getQName().equals(qn)) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;
    }
}

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
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.bind.v2.runtime.JaxBeanInfo;

import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * Walks the service model and sets up the element/type names.
 */
class JAXBSchemaInitializer extends ServiceModelVisitor {

    private XmlSchemaCollection schemas;
    private JAXBContextImpl context;
    
    public JAXBSchemaInitializer(ServiceInfo serviceInfo, XmlSchemaCollection col, JAXBContextImpl context) {
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
                QName name = part.getMessageInfo().getName();
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
        } else {
            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
            if (!itr.hasNext()) {
                return;
            }
            
            QName typeName = itr.next();
            part.setTypeQName(typeName);
            part.setXmlSchema(schemas.getTypeByQName(typeName));
        }
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
                    return;
                }
                Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();

                SchemaInfo schemaInfo = null;
                for (SchemaInfo s : serviceInfo.getSchemas()) {
                    if (s.getNamespaceURI().equals(qn.getNamespaceURI())) {
                        schemaInfo = s;

                        el = new XmlSchemaElement();
                        el.setQName(part.getElementQName());
                        el.setName(part.getElementQName().getLocalPart());
                        el.setNillable(true);
                        schemaInfo.getSchema().getItems().add(el);
                        
                        el.setSchemaTypeName(typeName);
                        return;
                    }
                }
            }
        }
        
        
    }
    
    public void end(FaultInfo fault) {
        MessagePartInfo part = fault.getMessageParts().get(0); 
        Class<?> cls = part.getTypeClass();
        Class<?> cl2 = (Class)fault.getProperty(Class.class.getName());
        if (cls != cl2) {
            QName name = part.getMessageInfo().getName();
            part.setElementQName(name);
            
            JaxBeanInfo<?> beanInfo = context.getBeanInfo(cls);


            SchemaInfo schemaInfo = null;
            for (SchemaInfo s : serviceInfo.getSchemas()) {
                if (s.getNamespaceURI().equals(part.getElementQName().getNamespaceURI())) {
                    schemaInfo = s;

                    XmlSchemaElement el = new XmlSchemaElement();
                    el.setQName(part.getElementQName());
                    el.setName(part.getElementQName().getLocalPart());
                    el.setNillable(true);
                    schemaInfo.getSchema().getItems().add(el);
                    
                    Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                    if (!itr.hasNext()) {
                        continue;
                    }
                    QName typeName = itr.next();
                    el.setSchemaTypeName(typeName);

                    return;
                }
            }
        }
    }

    
    private void buildExceptionType(MessagePartInfo part, Class cls) {
        SchemaInfo schemaInfo = null;
        for (SchemaInfo s : serviceInfo.getSchemas()) {
            if (s.getNamespaceURI().equals(part.getElementQName().getNamespaceURI())) {
                schemaInfo = s;                
                break;
            }
        }
        XmlSchema schema;
        if (schemaInfo == null) {
            schema = new XmlSchema(part.getElementQName().getNamespaceURI(), schemas);
            schema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));

            NamespaceMap nsMap = new NamespaceMap();
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NU_SCHEMA_XSD);
            schema.setNamespaceContext(nsMap);

            
            schemaInfo = new SchemaInfo(serviceInfo, part.getElementQName().getNamespaceURI());
            schemaInfo.setSchema(schema);
            serviceInfo.addSchema(schemaInfo);
        } else {
            schema = schemaInfo.getSchema();
        }
        
        XmlSchemaElement el = new XmlSchemaElement();
        el.setQName(part.getElementQName());
        el.setName(part.getElementQName().getLocalPart());
        schema.getItems().add(el);
        
        XmlSchemaComplexType ct = new XmlSchemaComplexType(schema);
        ct.setName(part.getElementQName().getLocalPart());
        schema.getItems().add(ct);
        schema.addType(ct);
        el.setSchemaTypeName(part.getElementQName());
        
        XmlSchemaSequence seq = new XmlSchemaSequence();
        ct.setParticle(seq);
        String namespace = part.getElementQName().getNamespaceURI();
        for (Field f : cls.getDeclaredFields()) {
            JaxBeanInfo<?> beanInfo = context.getBeanInfo(f.getType());
            if (beanInfo != null) {
                el = new XmlSchemaElement();
                el.setName(f.getName());
                el.setQName(new QName(namespace, f.getName()));

                el.setMinOccurs(1);
                el.setMaxOccurs(1);
                el.setNillable(true);

                if (beanInfo.isElement()) {
                    QName name = new QName(beanInfo.getElementNamespaceURI(null), 
                                           beanInfo.getElementLocalName(null));
                    XmlSchemaElement el2 = schemas.getElementByQName(name);
                    el.setRefName(el2.getRefName());
                } else {
                    Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                    if (!itr.hasNext()) {
                        continue;
                    }
                    QName typeName = itr.next();
                    el.setSchemaTypeName(typeName);
                }
                
                seq.getItems().add(el);
            }
        }
        JaxBeanInfo<?> beanInfo = context.getBeanInfo(String.class);    
        el = new XmlSchemaElement();
        el.setName("message");
        el.setQName(new QName(namespace, "message"));

        el.setMinOccurs(1);
        el.setMaxOccurs(1);
        el.setNillable(true);

        if (beanInfo.isElement()) {
            el.setRefName(beanInfo.getTypeName(null));
        } else {
            el.setSchemaTypeName(beanInfo.getTypeName(null));
        }
        seq.getItems().add(el);
            
        Document[] docs;
        try {
            docs = XmlSchemaSerializer.serializeSchema(schema, false);
        } catch (XmlSchemaSerializerException e1) {
            throw new ServiceConstructionException(e1);
        }
        Element e = docs[0].getDocumentElement();
        schemaInfo.setElement(e);
        // XXX A problem can occur with the ibm jdk when the XmlSchema
        // object is serialized.  The xmlns declaration gets incorrectly
        // set to the same value as the targetNamespace attribute.
        // The aegis databinding tests demonstrate this particularly.
        if (e.getPrefix() == null && !WSDLConstants.NU_SCHEMA_XSD.equals(
            e.getAttributeNS(WSDLConstants.NU_XMLNS, WSDLConstants.NP_XMLNS))) {
            e.setAttributeNS(WSDLConstants.NU_XMLNS, 
                WSDLConstants.NP_XMLNS, WSDLConstants.NU_SCHEMA_XSD);
        }
        schemaInfo.setElement(e);
    }
}

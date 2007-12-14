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

package org.apache.cxf.javascript;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * There are a number of pitfalls in Commons Xml Schema. This class contains
 * some utilities that avoid some of the problems and centralizes some
 * repetitive tasks.
 */
public final class XmlSchemaUtils {
    public static final XmlSchemaForm QUALIFIED = new XmlSchemaForm(XmlSchemaForm.QUALIFIED);
    public static final XmlSchemaForm UNQUALIFIED = new XmlSchemaForm(XmlSchemaForm.UNQUALIFIED);
    
    public static final String XSI_NS_ATTR = WSDLConstants.NP_XMLNS + ":" 
                    + WSDLConstants.NP_SCHEMA_XSI + "='" + WSDLConstants.NS_SCHEMA_XSI + "'";
    public static final String NIL_ATTRIBUTES = XSI_NS_ATTR + " xsi:nil='true'";

    private static final Logger LOG = LogUtils.getL7dLogger(XmlSchemaUtils.class);
    private static final XmlSchemaSequence EMPTY_SEQUENCE = new XmlSchemaSequence();
    
    private XmlSchemaUtils() {
    }
    
    private static String cleanedUpSchemaSource(XmlSchemaType subject) {
        if (subject.getSourceURI() == null) {
            return "";
        } else {
            return subject.getSourceURI() + ":" + subject.getLineNumber(); 
        }
    }
    
    public static void unsupportedConstruct(String messageKey, XmlSchemaType subject) {
        Message message = new Message(messageKey, LOG, subject.getQName(), 
                                      cleanedUpSchemaSource(subject));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);
    }
    
    public static void unsupportedConstruct(String messageKey, String what, XmlSchemaType subject) {
        Message message = new Message(messageKey, LOG, what, subject.getQName(),
                                      subject == null ? "(global)" 
                                          : cleanedUpSchemaSource(subject));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);
        
    }
    
    public static XmlSchemaSequence getSequence(XmlSchemaComplexType type) {
        XmlSchemaParticle particle = type.getParticle();
        XmlSchemaSequence sequence = null;
        
        if (particle == null) {
            // the code that uses this wants to iterate. An empty one is more useful than
            // a null pointer, and certainly an exception.
            return EMPTY_SEQUENCE;
        }
        
        try {
            sequence = (XmlSchemaSequence) particle;
        } catch (ClassCastException cce) {
            unsupportedConstruct("NON_SEQUENCE_PARTICLE", type);
        }
        
        return sequence;
    }
    
    /**
     * This copes with an observed phenomenon in the schema built by the
     * ReflectionServiceFactoryBean. It is creating element such that: (a) the
     * type is not set. (b) the refName is set. (c) the namespaceURI in the
     * refName is set empty. This apparently indicates 'same Schema' to everyone
     * else, so thus function implements that convention here. It is unclear if
     * that is a correct structure, and it if changes, we can simplify or
     * eliminate this function.
     * 
     * @param name
     * @param referencingURI
     * @return
     */
    public static XmlSchemaElement findElementByRefName(SchemaCollection xmlSchemaCollection,
                                                         QName name, 
                                                         String referencingURI) {
        String uri = name.getNamespaceURI();
        if ("".equals(uri)) {
            uri = referencingURI;
        }
        QName copyName = new QName(uri, name.getLocalPart());
        XmlSchemaElement target = xmlSchemaCollection.getElementByQName(copyName);
        assert target != null;
        return target;
    }
    
    
    /**
     * Follow a chain of references from element to element until we can obtain
     * a type.
     * 
     * @param element
     * @return
     */
    public static XmlSchemaType getElementType(SchemaCollection xmlSchemaCollection,
                                               String referencingURI, 
                                               XmlSchemaElement element,
                                               XmlSchemaType containingType) {
        if (element.getSchemaTypeName() != null) {
            XmlSchemaType type = xmlSchemaCollection.getTypeByQName(element.getSchemaTypeName());
            if (type == null) {
                Message message = new Message("ELEMENT_TYPE_MISSING", LOG, element.getQName(),
                                              element.getSchemaTypeName().toString());
                throw new UnsupportedConstruct(message);
            }
            return type;
        }
        assert element != null;
        // The referencing URI only helps if there is a schema that points to
        // it.
        // It might be the URI for the wsdl TNS, which might have no schema.
        if (xmlSchemaCollection.getSchemaByTargetNamespace(referencingURI) == null) {
            referencingURI = null;
        }
        
        if (referencingURI == null && containingType != null) {
            referencingURI = containingType.getQName().getNamespaceURI();
        }
        
        XmlSchemaElement originalElement = element;
        while (element.getSchemaType() == null && element.getRefName() != null) {
            XmlSchemaElement nextElement = findElementByRefName(xmlSchemaCollection,
                                                                element.getRefName(), 
                                                                referencingURI);
            assert nextElement != null;
            element = nextElement;
        }
        if (element.getSchemaType() == null) {
            XmlSchemaUtils.unsupportedConstruct("ELEMENT_HAS_NO_TYPE", originalElement.getName(), 
                                                containingType);
        }
        return element.getSchemaType();
    }
    
    public static boolean isComplexType(XmlSchemaType type) {
        return type instanceof XmlSchemaComplexType;
    }
    
    public static boolean isElementNameQualified(XmlSchemaElement element, XmlSchema schema) {
        if (element.getRefName() != null) {
            throw new RuntimeException("isElementNameQualified on element with ref=");
        }
        if (element.getForm().equals(QUALIFIED)) {
            return true;
        }
        if (element.getForm().equals(UNQUALIFIED)) {
            return false;
        }
        return schema.getElementFormDefault().equals(QUALIFIED);
    }
    
    /**
     * due to a bug, feature, or just plain oddity of JAXB, it isn't good enough
     * to just check the for of an element and of its schema. If schema 'a'
     * (default unqualified) has a complex type with an element with a ref= to
     * schema (b) (default unqualified), JAXB seems to expect to see a
     * qualifier, anyway. <br/> So, if the element is local to a complex type,
     * all we care about is the default element form of the schema and the local
     * form of the element. <br/> If, on the other hand, the element is global,
     * we might need to compare namespaces. <br/>
     * 
     * @param element the element.
     * @param global if this element is a global element (complex type ref= to
     *                it, or in a part)
     * @param localSchema the schema of the complex type containing the
     *                reference, only used for the 'odd case'.
     * @param elementSchema the schema for the element.
     * @return if the element needs to be qualified.
     */
    public static boolean isElementQualified(XmlSchemaElement element,
                                             boolean global,
                                             XmlSchema localSchema,
                                             XmlSchema elementSchema) {
        if (element.getQName() == null) {
            throw new RuntimeException("getSchemaQualifier on anonymous element.");
        }
        if (element.getRefName() != null) {
            throw new RuntimeException("getSchemaQualified on the 'from' side of ref=.");
        }
            

        if (global) {
            return isElementNameQualified(element, elementSchema)
                || !(element.getQName().getNamespaceURI().equals(localSchema.getTargetNamespace()));
        } else {
            return isElementNameQualified(element, elementSchema);
        }
    }
    
    public static boolean isParticleArray(XmlSchemaParticle particle) {
        return particle.getMaxOccurs() > 1;
    }
    
    public static boolean isParticleOptional(XmlSchemaParticle particle) {
        return particle.getMinOccurs() == 0 && particle.getMaxOccurs() == 1;
    }
    
    public static XmlSchemaElement getReferredElement(XmlSchemaElement element, 
                                                      SchemaCollection xmlSchemaCollection) {
        if (element.getRefName() != null) {
            XmlSchemaElement refElement = xmlSchemaCollection.getElementByQName(element.getRefName());
            if (refElement == null) {
                throw new RuntimeException("Dangling reference");
            }
            return refElement;
        }
        return null;
    }
        

}

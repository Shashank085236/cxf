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

package org.apache.cxf.javascript.types;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.javascript.ElementInfo;
import org.apache.cxf.javascript.JavascriptUtils;
import org.apache.cxf.javascript.NameManager;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.UnsupportedConstruct;
import org.apache.cxf.javascript.XmlSchemaUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Generate Javascript for a schema, and provide information needed for the service builder.
 * As of this pass, there is no support for non-sequence types or for attribute mappings.
 * @author bimargulies
 */
public class SchemaJavascriptBuilder {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SchemaJavascriptBuilder.class);
    
    private SchemaCollection xmlSchemaCollection;
    private NameManager nameManager;
    private NamespacePrefixAccumulator prefixAccumulator;
    private SchemaInfo schemaInfo;
    
    public SchemaJavascriptBuilder(SchemaCollection schemaCollection,
                                   NamespacePrefixAccumulator prefixAccumulator,
                                   NameManager nameManager) {
        this.xmlSchemaCollection = schemaCollection;
        this.nameManager = nameManager;
        this.prefixAccumulator = prefixAccumulator;
    }
    
    public String generateCodeForSchema(SchemaInfo schema) {
        schemaInfo = schema;
        StringBuffer code = new StringBuffer();
        code.append("//\n");
        code.append("// Definitions for schema: " + schema.getNamespaceURI());
        if (schema.getSystemId() != null) {
            code.append("\n//  " + schema.getSystemId());
        }
        code.append("\n//\n");

        XmlSchemaObjectTable schemaTypes = schema.getSchema().getSchemaTypes();
        Iterator namesIterator = schemaTypes.getNames();
        while (namesIterator.hasNext()) {
            QName name = (QName)namesIterator.next();
            XmlSchemaObject xmlSchemaObject = (XmlSchemaObject)schemaTypes.getItem(name);
            if (xmlSchemaObject instanceof XmlSchemaComplexType) {
                try {
                    XmlSchemaComplexType complexType = (XmlSchemaComplexType)xmlSchemaObject;
                    if (complexType.getName() != null) {
                        code.append(complexTypeConstructorAndAccessors(complexType.getQName(), complexType));
                        code.append(complexTypeSerializerFunction(complexType.getQName(), complexType));
                        code.append(domDeserializerFunction(complexType.getQName(), complexType));
                    }
                } catch (UnsupportedConstruct usc) {
                    LOG.warning(usc.toString());
                    continue; // it could be empty, but the style checker would complain.
                }
            }
        }
        
        // now add in global elements with anonymous types.        
        schemaTypes = schema.getSchema().getElements();
        namesIterator = schemaTypes.getNames();
        while (namesIterator.hasNext()) {
            QName name = (QName)namesIterator.next();
            XmlSchemaObject xmlSchemaObject = (XmlSchemaObject)schemaTypes.getItem(name);
            if (xmlSchemaObject instanceof XmlSchemaElement) { // the alternative is too wierd to contemplate.
                try {
                    XmlSchemaElement element = (XmlSchemaElement)xmlSchemaObject;
                    if (element.getSchemaTypeName() == null && element.getSchemaType() == null) {
                        Message message = new Message("ELEMENT_MISSING_TYPE", LOG, 
                                                      element.getQName(),
                                                      element.getSchemaTypeName(),
                                                      schema.getNamespaceURI());
                        LOG.warning(message.toString());
                        continue;
                    }
                    XmlSchemaType type;
                    if (element.getSchemaType() != null) {
                        type = element.getSchemaType();
                    } else {
                        type = schema.getSchema().getTypeByName(element.getSchemaTypeName());
                    }
                    if (!(xmlSchemaObject instanceof XmlSchemaComplexType)) { 
                        // we never make classes for simple type.
                        continue;
                    }

                    XmlSchemaComplexType complexType = (XmlSchemaComplexType)type;
                    // for named types we don't bother to generate for the element.
                    if (complexType.getName() == null) {
                        code.append(complexTypeConstructorAndAccessors(element.getQName(), complexType));
                        code.append(complexTypeSerializerFunction(element.getQName(), complexType));
                        code.append(domDeserializerFunction(element.getQName(), complexType));
                    }
                } catch (UnsupportedConstruct usc) {
                    continue; // it could be empty, but the style checker would complain.
                }
            }
        }
        
        String returnValue = code.toString();
        LOG.finer(returnValue);
        return returnValue;
    }
    
    public String complexTypeConstructorAndAccessors(QName name, XmlSchemaComplexType type) {
        StringBuilder code = new StringBuilder();
        StringBuilder accessors = new StringBuilder();
        JavascriptUtils utils = new JavascriptUtils(code);
        XmlSchemaSequence sequence = XmlSchemaUtils.getSequence(type);
        
        final String elementPrefix = "this._";
        
        String typeObjectName = nameManager.getJavascriptName(name);
        code.append("function " + typeObjectName + " () {\n");
        
        for (int i = 0; i < sequence.getItems().getCount(); i++) {
            XmlSchemaObject thing = sequence.getItems().getItem(i);
            if (!(thing instanceof XmlSchemaElement)) {
                XmlSchemaUtils.unsupportedConstruct("NON_ELEMENT_CHILD", 
                                                    thing.getClass().getSimpleName(), type);
            }
            
            XmlSchemaElement elChild = (XmlSchemaElement)thing;
            XmlSchemaType elType = XmlSchemaUtils.getElementType(xmlSchemaCollection, null, elChild, type);

            boolean nillable = elChild.isNillable();
            if (elChild.isAbstract()) { 
                XmlSchemaUtils.unsupportedConstruct("ABSTRACT_ELEMENT", elChild.getName(), type);
            }
            
            // Assume that no lunatic has created multiple elements that differ only by namespace.
            // if elementForm is unqualified, how can that be valid?
            String elementName = elementPrefix + elChild.getName();
            String accessorSuffix = StringUtils.capitalize(elChild.getName());

            String accessorName = typeObjectName + "_get" + accessorSuffix;
            accessors.append("function " + accessorName + "() { return " + elementName + ";}\n");
            accessors.append(typeObjectName + ".prototype.get" 
                             + accessorSuffix + " = " + accessorName + ";\n");
            
            accessorName = typeObjectName + "_set" + accessorSuffix;
            accessors.append("function " 
                             + accessorName + "(value) {" + elementName + " = value;}\n");
            accessors.append(typeObjectName 
                             + ".prototype.set" + accessorSuffix + " = " + accessorName + ";\n");
            
            if (XmlSchemaUtils.isParticleOptional(elChild) 
                || (nillable && !XmlSchemaUtils.isParticleArray(elChild))) {
                utils.appendLine(elementName + " = null;");
            } else if (XmlSchemaUtils.isParticleArray(elChild)) {
                utils.appendLine(elementName + " = [];");
            } else if (elType instanceof XmlSchemaComplexType) {
                // even for required complex elements, we leave them null. 
                // otherwise, we could end up in a cycle or otherwise miserable. The 
                // application code is responsible for this.
                utils.appendLine(elementName + " = null;");
            } else {
                String defaultValueString = elChild.getDefaultValue();
                if (defaultValueString == null) {
                    defaultValueString = 
                        utils.getDefaultValueForSimpleType(elType);
                }
                utils.appendLine(elementName + " = " + defaultValueString + ";");
            }
        }
        code.append("}\n");
        return code.toString() + "\n" + accessors.toString();
    }
    
    

    /**
     * Produce a serializer function for a type.
     * These functions emit the surrounding element XML if the caller supplies an XML element name.
     * It's not quite as simple as that, though. The element name may need namespace qualification,
     * and this function will add more namespace prefixes as needed.
     * @param type
     * @return
     */
    public String complexTypeSerializerFunction(QName name, XmlSchemaComplexType type) {
        
        StringBuilder bodyCode = new StringBuilder();
        JavascriptUtils bodyUtils = new JavascriptUtils(bodyCode);
        bodyUtils.setXmlStringAccumulator("xml");

        complexTypeSerializerBody(type, "this._", bodyUtils);
        
        StringBuilder code = new StringBuilder();
        JavascriptUtils utils = new JavascriptUtils(code);
        String functionName = nameManager.getJavascriptName(name) + "_" + "serialize";
        code.append("function " + functionName + "(cxfjsutils, elementName) {\n");
        utils.startXmlStringAccumulator("xml");
        utils.startIf("elementName != null");
        utils.appendString("<");
        utils.appendExpression("elementName");
        // now add any accumulated namespaces.
        String moreNamespaces = prefixAccumulator.getAttributes();
        if (moreNamespaces.length() > 0) {
            utils.appendString(" ");
            utils.appendString(moreNamespaces);
        }
        utils.appendString(">");
        utils.endBlock();
        code.append(bodyCode);
        utils.startIf("elementName != null");
        utils.appendString("</");
        utils.appendExpression("elementName");
        utils.appendString(">");
        utils.endBlock();
        utils.appendLine("return xml;");
        code.append("}\n");

        code.append(nameManager.getJavascriptName(type) + ".prototype.serialize = " + functionName + ";\n");
        return code.toString();
    }
   

    /**
     * Build the serialization code for a complex type. At the top level, this operates on single items,
     * so it does not pay attention to minOccurs and maxOccurs. However, as it works through the sequence,
     * it manages optional elements and arrays.
     * @param type
     * @param elementPrefix
     * @param bodyNamespaceURIs 
     * @return
     */
    protected void complexTypeSerializerBody(XmlSchemaComplexType type, 
                                          String elementPrefix, 
                                          JavascriptUtils utils) {

        XmlSchemaSequence sequence = XmlSchemaUtils.getSequence(type);

        // XML Schema, please meet Iterable (not).
        for (int i = 0; i < sequence.getItems().getCount(); i++) {
            XmlSchemaElement sequenceElement = (XmlSchemaElement)sequence.getItems().getItem(i);
            if (sequenceElement.isAbstract()) {
                XmlSchemaUtils.unsupportedConstruct("ABSTRACT_ELEMENT", sequenceElement.getName(), type);
            }
            
            ElementInfo elementInfo = ElementInfo.forLocalElement(sequenceElement, 
                                                                  elementPrefix, 
                                                                  schemaInfo.getSchema(),
                                                                  xmlSchemaCollection, 
                                                                  prefixAccumulator);
            elementInfo.setContainingType(type);
            elementInfo.setUtilsVarName("cxfjsutils");
            utils.generateCodeToSerializeElement(elementInfo, xmlSchemaCollection);
        }
    }
    /**
     * Generate a JavaScript function that takes an element for a complex type and walks through
     * its children using them to fill in the values for a JavaScript object.
     * @param type schema type for the process
     * @return the string contents of the JavaScript.
     */
    public String domDeserializerFunction(QName name, XmlSchemaComplexType type) {
        StringBuilder code = new StringBuilder();
        JavascriptUtils utils = new JavascriptUtils(code);
        XmlSchemaParticle particle = type.getParticle();
        XmlSchemaSequence sequence = null;
        
        if (particle == null) {
            XmlSchemaUtils.unsupportedConstruct("NULL_PARTICLE", type);
        }
        
        try {
            sequence = (XmlSchemaSequence) particle;
        } catch (ClassCastException cce) {
            XmlSchemaUtils.unsupportedConstruct("NON_SEQUENCE_PARTICLE", type);
        }
        
        String typeObjectName = nameManager.getJavascriptName(name);
        code.append("function " + typeObjectName + "_deserialize (cxfjsutils, element) {\n");
        // create the object we are deserializing into.
        utils.appendLine("var newobject = new " + typeObjectName + "();");
        utils.appendLine("cxfjsutils.trace('element: ' + cxfjsutils.traceElementName(element));");
        utils.appendLine("var curElement = cxfjsutils.getFirstElementChild(element);");
        
        utils.appendLine("var item;");
        
        for (int i = 0; i < sequence.getItems().getCount(); i++) {
            utils.appendLine("cxfjsutils.trace('curElement: ' + cxfjsutils.traceElementName(curElement));");
            XmlSchemaObject thing = sequence.getItems().getItem(i);
            if (!(thing instanceof XmlSchemaElement)) {
                XmlSchemaUtils.unsupportedConstruct("NON_ELEMENT_CHILD", 
                                                    thing.getClass().getSimpleName(), type);
            }
            
            boolean global = false;
            XmlSchemaElement sequenceElement = (XmlSchemaElement)thing;
            XmlSchemaElement realElement = sequenceElement;
            
            if (sequenceElement.getRefName() != null) {
                XmlSchemaElement refElement = 
                    xmlSchemaCollection.getElementByQName(sequenceElement.getRefName());
                if (refElement == null) {
                    throw new RuntimeException("Dangling reference");
                }
                realElement = refElement;
                global = true;
            }
            
            XmlSchemaType elType = XmlSchemaUtils.getElementType(xmlSchemaCollection, 
                                                                 null, realElement, type);
            boolean simple = elType instanceof XmlSchemaSimpleType;

            String accessorName = "set" + StringUtils.capitalize(realElement.getName()); 
            // For optional or an array, we need to check if the element is the 
            // one we want.
            
            String elementName = realElement.getName();
            utils.appendLine("cxfjsutils.trace('processing " + elementName + "');");
            String elementNamespaceURI = realElement.getQName().getNamespaceURI();
            boolean elementNoNamespace = "".equals(elementNamespaceURI);
            XmlSchema elementSchema = null;
            if (!elementNoNamespace) {
                elementSchema = xmlSchemaCollection.getSchemaByTargetNamespace(elementNamespaceURI);
            }
            boolean qualified = !elementNoNamespace
                && XmlSchemaUtils.isElementQualified(realElement, 
                                                  global, 
                                                  schemaInfo.getSchema(),
                                                  elementSchema);
            
            if (!qualified) {
                elementNamespaceURI = "";
            }
                
            String valueTarget = "item";

            if (XmlSchemaUtils.isParticleOptional(sequenceElement) 
                || XmlSchemaUtils.isParticleArray(sequenceElement)) {
                utils.startIf("curElement != null && cxfjsutils.isNodeNamedNS(curElement, '" 
                              + elementNamespaceURI 
                              + "', '" 
                              + elementName
                              + "')");
                if (XmlSchemaUtils.isParticleArray(sequenceElement)) {
                    utils.appendLine("item = [];");
                    utils.startDo();
                    valueTarget = "arrayItem";
                    utils.appendLine("var arrayItem;");
                }
            }
                
            utils.appendLine("var value = null;");
            utils.startIf("!cxfjsutils.isElementNil(curElement)");
            if (simple) {
                utils.appendLine("value = cxfjsutils.getNodeText(curElement);");
                utils.appendLine(valueTarget 
                                 + " = " + utils.javascriptParseExpression(elType, "value") 
                                 + ";");
            } else {
                String elTypeJsName = nameManager.getJavascriptName((XmlSchemaComplexType)elType);
                utils.appendLine(valueTarget + " = " 
                                 + elTypeJsName 
                                 + "_deserialize(cxfjsutils, curElement);");
            }
             
            utils.endBlock(); // the if for the nil.
            if (XmlSchemaUtils.isParticleArray(sequenceElement)) {
                utils.appendLine("item.push(arrayItem);");
                utils.appendLine("curElement = cxfjsutils.getNextElementSibling(curElement);");
                utils.endBlock();
                utils.appendLine("  while(curElement != null && cxfjsutils.isNodeNamedNS(curElement, '" 
                                  + elementNamespaceURI + "', '" 
                                  + sequenceElement.getName() + "'));");
            }
            utils.appendLine("newobject." + accessorName + "(item);");
            if (!XmlSchemaUtils.isParticleArray(sequenceElement)) {
                utils.startIf("curElement != null");
                utils.appendLine("curElement = cxfjsutils.getNextElementSibling(curElement);");
                utils.endBlock();
            }
            if (XmlSchemaUtils.isParticleOptional(sequenceElement) 
                || XmlSchemaUtils.isParticleArray(sequenceElement)) {
                utils.endBlock();
            }
        }
        utils.appendLine("return newobject;");
        code.append("}\n");
        return code.toString() + "\n";
    }
}

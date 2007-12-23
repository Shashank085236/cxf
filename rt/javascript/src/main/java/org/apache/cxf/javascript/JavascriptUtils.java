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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * A set of functions that assist in JavaScript generation. This includes functions
 * for appending strings of JavaScript to a buffer as well as some type utilities.
 */
public class JavascriptUtils {
    private static final String NL = "\n";
    private StringBuilder code;
    private Stack<String> prefixStack;
    private String xmlStringAccumulatorVariable;
    private Map<String, String> defaultValueForSimpleType;
    private Set<String> nonStringSimpleTypes;
    private Set<String> intTypes;
    private Set<String> floatTypes;
    
    public JavascriptUtils(StringBuilder code) {
        this.code = code;
        defaultValueForSimpleType = new HashMap<String, String>();
        defaultValueForSimpleType.put("int", "0");
        defaultValueForSimpleType.put("unsignedInt", "0");
        defaultValueForSimpleType.put("long", "0");
        defaultValueForSimpleType.put("unsignedLong", "0");
        defaultValueForSimpleType.put("float", "0.0");
        defaultValueForSimpleType.put("double", "0.0");
        nonStringSimpleTypes = new HashSet<String>();
        nonStringSimpleTypes.add("int");
        nonStringSimpleTypes.add("long");
        nonStringSimpleTypes.add("unsignedInt");
        nonStringSimpleTypes.add("unsignedLong");
        nonStringSimpleTypes.add("float");
        nonStringSimpleTypes.add("double");
        
        intTypes = new HashSet<String>();
        intTypes.add("int");
        intTypes.add("long");
        intTypes.add("unsignedInt");
        intTypes.add("unsignedLong");
        floatTypes = new HashSet<String>();
        floatTypes.add("float");
        floatTypes.add("double");
        
        prefixStack = new Stack<String>();
        prefixStack.push("    ");
    }
    
    public String getDefaultValueForSimpleType(XmlSchemaType type) {
        String val = defaultValueForSimpleType.get(type.getName());
        if (val == null) { // ints and such return the appropriate 0.
            return "''";
        } else {
            return val;
        }
    }
    
    public boolean isStringSimpleType(QName typeName) {
        return !(WSDLConstants.NS_SCHEMA_XSD.equals(typeName.getNamespaceURI()) 
                 && nonStringSimpleTypes.contains(typeName.getLocalPart()));
    }
    
    public void setXmlStringAccumulator(String variableName) {
        xmlStringAccumulatorVariable = variableName;
    }
    
    public void startXmlStringAccumulator(String variableName) {
        xmlStringAccumulatorVariable = variableName;
        code.append(prefix());
        code.append("var ");
        code.append(variableName);
        code.append(" = '';" + NL);
    }
    
    public static String protectSingleQuotes(String value) {
        return value.replaceAll("'", "\\'");
    }
    
    public String escapeStringQuotes(String data) {
        return data.replace("'", "\\'");
    }
    
    /**
     * emit javascript to append a value to the accumulator. 
     * @param value
     */
    public void appendString(String value) {
        code.append(prefix());
        code.append(xmlStringAccumulatorVariable + " = " + xmlStringAccumulatorVariable + " + '");
        code.append(escapeStringQuotes(value));
        code.append("';" + NL);
    }
    
    public void appendExpression(String value) {
        code.append(prefix());
        code.append(xmlStringAccumulatorVariable + " = " + xmlStringAccumulatorVariable + " + ");
        code.append(value);
        code.append(";" + NL);
    }
    
    private String prefix() {
        return prefixStack.peek();
    }
    
    public void appendLine(String line) {
        code.append(prefix());
        code.append(line);
        code.append(NL);
    }
    
    public void startIf(String test) {
        code.append(prefix());
        code.append("if (" + test + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }
    
    public void startBlock() {
        code.append(prefix());
        code.append("{" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void appendElse() {
        prefixStack.pop();
        code.append(prefix());
        code.append("} else {" + NL);
        prefixStack.push(prefix() + " ");
    }
    
    public void endBlock() {
        prefixStack.pop();
        code.append(prefix());
        code.append("}" + NL);
    }
    
    public void startFor(String start, String test, String increment) {
        code.append(prefix());
        code.append("for (" + start + ";" + test + ";" + increment + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startForIn(String var, String collection) {
        code.append(prefix());
        code.append("for (var " + var + " in " + collection + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startWhile(String test) {
        code.append(prefix());
        code.append("while (" + test + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startDo() {
        code.append(prefix());
        code.append("do  {" + NL);
        prefixStack.push(prefix() + " ");
    }
    
    // Given a js variable and a simple type object, correctly set the variables simple type 
    public String javascriptParseExpression(XmlSchemaType type, String value) {
        if (!(type instanceof XmlSchemaSimpleType)) {
            return value;
        }
        String name = type.getName();
        if (intTypes.contains(name)) {
            return "parseInt(" + value + ")";
        } else if (floatTypes.contains(name)) {
            return "parseFloat(" + value + ")";
        } else if ("boolean".equals(name)) {
            return "(" + value + " == true)";
        } else {
            return value;
        }
    }
    
    public static String javaScriptNameToken(String token) {
        return token;
    }
    
    /**
     * Given an element, generate the serialization code.
     * @param elementInfo      description of the element we are serializing
     * @param referencePrefix  prefix to the Javascript variable. Nothing for args,
     * this._ for members.
     * @param schemaCollection caller's schema collection.
     */
    public void generateCodeToSerializeElement(ParticleInfo elementInfo,
                                               String referencePrefix,
                                               SchemaCollection schemaCollection) {
        XmlSchemaType type = elementInfo.getType();
        boolean nillable = elementInfo.isNillable();
        boolean optional = elementInfo.isOptional();
        boolean array = elementInfo.isArray();
        String jsVar = referencePrefix + elementInfo.getJavascriptName();
        
        // first question: optional?
        if (optional) {
            startIf(jsVar + " != null");
        } 
        
        // nillable and optional would be very strange together.
        // and nillable in the array case applies to the elements.
        if (nillable && !array) {
            startIf(jsVar + " == null");
            appendString("<" + elementInfo.getXmlName() + " " + XmlSchemaUtils.NIL_ATTRIBUTES + "/>");
            appendElse();
        }
        
        if (array) {
            // protected against null in arrays.
            startIf(jsVar + " != null");
            startFor("var ax = 0", "ax < " +  jsVar + ".length", "ax ++");
            jsVar = jsVar + "[ax]";
            // we need an extra level of 'nil' testing here. Or do we, depending on the type structure?
            // Recode and fiddle appropriately.
            startIf(jsVar + " == null");
            if (nillable) {
                appendString("<" + elementInfo.getXmlName() 
                             + " " + XmlSchemaUtils.NIL_ATTRIBUTES + "/>");
            } else {
                appendString("<" + elementInfo.getXmlName() + "/>");                    
            }
            appendElse();
        }
        
        // now for the thing itself.
        if (type instanceof XmlSchemaComplexType) {
            // it has a value
            appendExpression(jsVar + ".serialize(cxfjsutils, '" 
                             + elementInfo.getXmlName() + "')");
        } else { // simple type
            QName typeName = type.getQName();
            appendString("<" + elementInfo.getXmlName() + ">");
            // warning: this assumes that ordinary Javascript serialization is all we need.
            // except for &gt; ad all of that.
            if (isStringSimpleType(typeName)) {
                appendExpression("cxfjsutils.escapeXmlEntities(" + jsVar + ")");
            } else {
                appendExpression(jsVar);
            }
            appendString("</" + elementInfo.getXmlName() + ">");
        }
        
        if (array) {
            endBlock(); // for the extra level of nil checking, which might be wrong.
            endBlock(); // for the for loop.
            endBlock(); // the null protection.
        }
        
        if (nillable && !array) {
            endBlock();
        }
        
        if (optional) {
            endBlock();
        }
    }
    
    /**
     * Generate code to serialize an xs:any.
     * There is too much duplicate code the element serializer; fix that some day.
     * @param elementInfo
     * @param schemaCollection
     */
    public void generateCodeToSerializeAny(ParticleInfo itemInfo, 
                                           String prefix,
                                           SchemaCollection schemaCollection) {
        boolean optional = XmlSchemaUtils.isParticleOptional(itemInfo.getParticle());
        boolean array = XmlSchemaUtils.isParticleArray(itemInfo.getParticle());
        
        appendLine("var anyHolder = this._" + itemInfo.getJavascriptName() + ";");
        appendLine("var anySerializer = null;");
        appendLine("var anyXmlTag = null;");
        appendLine("var anyXmlNsDef = null;");
        appendLine("var anyData = null;");
        appendLine("var anyStartTag;");
        
        startIf("anyHolder != null");
        appendLine("anySerializer = "
                             + "cxfjsutils.interfaceObject.globalElementSerializers[anyHolder.qname];");
        appendLine("anyXmlTag = '" + prefix + ":' + anyHolder.localName;");
        appendLine("anyXmlNsDef = 'xmlns:" + prefix + "=' + anyHolder.namespaceURI;");
        appendLine("anyStartTag = '<' + anyXmlTag + ' ' + anyXmlNsDef + '>';");
        appendLine("anyEndTag = '</' + anyXmlTag + '>';");
        appendLine("anyEmptyTag = '<' + anyXmlTag + ' ' + anyXmlNsDef + '/>';");
        appendLine("anyData = anyHolder.object;");
        endBlock();

        // first question: optional?
        if (optional) {
            startIf("anyHolder != null && anyData != null");
        }  else {
            startIf("anyHolder == null || anyData == null");
            appendLine("throw 'null value for required any item';");
            endBlock();
        }
        
        String varRef = "anyData";
        
        if (array) {
            startFor("var ax = 0", "ax < anyData.length", "ax ++");
            varRef = "anyData[ax]";
            // we need an extra level of 'nil' testing here. Or do we, depending on the type structure?
            // Recode and fiddle appropriately.
            startIf(varRef + " == null");
            appendExpression("anyEmptyTag");
            appendElse();
        }
        
        startIf("anySerializer"); // if no constructor, a simple type.
            // it has a value
        appendExpression("anySerializer(cxfjsutils, anyXmlTag)"); 
        appendElse();
        appendExpression("anyStartTag");
        appendExpression("cxfjsutils.escapeXmlEntities(" + varRef + ")");
        appendExpression("anyEndTag");
        endBlock();
        if (array) {
            endBlock(); // for the nil/empty tag. Gorsh, we should have runtime knowledge of nillable
            // on the elements.
            endBlock(); // for the for loop.
        }
        
        if (optional) {
            endBlock();
        }
    }

}

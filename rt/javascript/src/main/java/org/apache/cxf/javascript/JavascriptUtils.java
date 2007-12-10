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

import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * 
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
        assert type.getQName().getNamespaceURI().equals(WSDLConstants.NS_SCHEMA_XSD);
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
    
    public void generateCodeToSerializeElement(ElementInfo elementInfo) {
        XmlSchemaElement element = elementInfo.getElement();
        XmlSchemaType type = elementInfo.getType();
        boolean nillable = element == null || elementInfo.getElement().isNillable();
        boolean optional = element == null || XmlSchemaUtils.isParticleOptional(elementInfo.getElement());
        boolean array = element != null && XmlSchemaUtils.isParticleArray(elementInfo.getElement());
        
        XmlSchemaType elType = type;
        // perhaps push this up into the callers.
        if (elType == null) {
            XmlSchemaUtils.getElementType(elementInfo.getXmlSchemaCollection(),
                                          elementInfo.getReferencingURI(),
                                          elementInfo.getElement(), 
                                          elementInfo.getContainingType());
            if (elType == null) {
                throw new UnsupportedConstruct("Null type");
            }
        }

        // first question: optional?
        if (optional) {
            startIf(elementInfo.getElementJavascriptName() + " != null");
        } 
        
        // nillable and optional would be very strange together.
        // and nillable in the array case applies to the elements.
        if (nillable && !array) {
            startIf(elementInfo.getElementJavascriptName() + " == null");
            appendString("<" + elementInfo.getElementXmlName() + " " + XmlSchemaUtils.NIL_ATTRIBUTES + "/>");
            appendElse();
        }
        
        if (array) {
            // protected against null in arrays.
            startIf(elementInfo.getElementJavascriptName() + " != null");
            startFor("var ax = 0", "ax < " +  elementInfo.getElementJavascriptName() + ".length", "ax ++");
            elementInfo.setElementJavascriptName(elementInfo.getElementJavascriptName() + "[ax]");
            // we need an extra level of 'nil' testing here. Or do we, depending on the type structure?
            // Recode and fiddle appropriately.
            startIf(elementInfo.getElementJavascriptName() + " == null");
            if (nillable) {
                appendString("<" + elementInfo.getElementXmlName() 
                             + " " + XmlSchemaUtils.NIL_ATTRIBUTES + "/>");
            } else {
                appendString("<" + elementInfo.getElementXmlName() + "/>");                    
            }
            appendElse();
        }
        
        // now for the thing itself.
        if (elType instanceof XmlSchemaComplexType) {
            // it has a value
            appendExpression(elementInfo.getElementJavascriptName() + ".serialize(" 
                             + elementInfo.getUtilsVarName() + ", '" 
                             + elementInfo.getElementXmlName() + "')");
        } else { // simple type
            QName typeName = elType.getQName();
            appendString("<" + elementInfo.getElementXmlName() + ">");
            // warning: this assumes that ordinary Javascript serialization is all we need.
            // except for &gt; ad all of that.
            if (isStringSimpleType(typeName)) {
                appendExpression(elementInfo.getUtilsVarName() 
                                 + ".escapeXmlEntities(" 
                                 + elementInfo.getElementJavascriptName() + ")");
            } else {
                appendExpression(elementInfo.getElementJavascriptName());
            }
            appendString("</" + elementInfo.getElementXmlName() + ">");
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
}

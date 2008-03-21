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

import javax.xml.namespace.QName;

import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class NamespacePrefixAccumulator {
    private StringBuffer attributes;
    private Set<String> prefixes;
    private Map<String, String> fallbackNamespacePrefixMap;
    private int nsCounter;
    private SchemaCollection schemaCollection;

    public NamespacePrefixAccumulator(SchemaCollection schemaCollection) {
        attributes = new StringBuffer();
        prefixes = new HashSet<String>();
        fallbackNamespacePrefixMap = new HashMap<String, String>();
        nsCounter = 0;
        this.schemaCollection = schemaCollection;
    }
    
    public void collect(String prefix, String uri) {
        if (!("".equals(uri)) && !prefixes.contains(prefix)) {
            attributes.append("xmlns:" + prefix + "='" + uri + "' ");
            prefixes.add(prefix);
        }
    }
    
    public String getAttributes() {
        return attributes.toString();
    }
    
    private String getPrefix(String namespaceURI) {
        if ("".equals(namespaceURI)) {
            throw new RuntimeException("Prefix requested for default namespace.");
        }
        String schemaPrefix = schemaCollection.getNamespaceContext().getPrefix(namespaceURI);
        // there could also be a namespace context on an individual schema info.
        // perhaps SchemaCollection should be enforcing some discipline there.
        if (schemaPrefix == null || "tns".equals(schemaPrefix)) {
            schemaPrefix = fallbackNamespacePrefixMap.get(namespaceURI);
            if (schemaPrefix == null) {
                schemaPrefix = "jns" + nsCounter;
                nsCounter++;
                fallbackNamespacePrefixMap.put(namespaceURI, schemaPrefix);
            }
        }
        return schemaPrefix;
    }
    
    /**
     * This function obtains a name, perhaps namespace-qualified, for an element.
     * @param element the element.
     * @param qualified whether to qualify.
     * @return
     */
    public String xmlElementString(XmlSchemaElement element, boolean qualified) {
        if (qualified) {
            // What if there were a prefix in the element's qname? This is not apparently 
            // something that happens in this environment.
            String prefix = getPrefix(element.getQName().getNamespaceURI());
            collect(prefix, element.getQName().getNamespaceURI());
            return prefix + ":" + element.getName();
        }
        return element.getName(); // use the non-qualified name.
    }
    
    public String xmlElementString(QName name) { // used with part concrete names
        if ("".equals(name.getNamespaceURI())) {
            return name.getLocalPart();
        }

        String prefix = getPrefix(name.getNamespaceURI());
        collect(prefix, name.getNamespaceURI());
        return prefix + ":" + name.getLocalPart();
    }
}
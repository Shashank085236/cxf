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

package org.apache.cxf.helpers;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;

public class XPathUtils {

    XPath xpath;
    
    public XPathUtils() {   
        this(null);
    }
    
    public XPathUtils(Map<String, String> ns) {
        xpath = XPathFactory.newInstance().newXPath();

        if (ns != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(ns));
        }         
    }
    
    public Object getValue(String xpathExpression, Node node, QName type) {    
        try {
            return xpath.evaluate(xpathExpression, node, type);
        } catch (Exception e) {
            return null;    
        }
    }
    
    class MapNamespaceContext implements NamespaceContext {
        private Map<String, String> namespaces;

        public MapNamespaceContext(Map<String, String> namespaces) {
            super();
            this.namespaces = namespaces;
        }

        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            for (Map.Entry<String, String> e : namespaces.entrySet()) {
                if (e.getValue().equals(namespaceURI)) {
                    return e.getKey();
                }
            }
            return null;
        }

        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }

    }
}

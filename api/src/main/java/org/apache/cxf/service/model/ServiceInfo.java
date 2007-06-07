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

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaCollection;

public class ServiceInfo extends AbstractDescriptionElement {
    QName name;
    String targetNamespace;
    InterfaceInfo intf;
    Map<QName, BindingInfo> bindings = new ConcurrentHashMap<QName, BindingInfo>(2);
    Map<QName, EndpointInfo> endpoints = new ConcurrentHashMap<QName, EndpointInfo>(2);
    Map<QName, MessageInfo> messages;
    List<SchemaInfo> schemas = new ArrayList<SchemaInfo>(4);
    private XmlSchemaCollection xmlSchemaCollection;
    
    public ServiceInfo() {
    }
    
    public String getTargetNamespace() {
        return targetNamespace;
    }
    public void setTargetNamespace(String ns) {
        targetNamespace = ns;
    }
    
    public void setName(QName n) {
        name = n;
    }
    public QName getName() {
        return name;
    }
    
    public InterfaceInfo createInterface(QName qn) {
        intf = new InterfaceInfo(this, qn);
        return intf;
    }
    public void setInterface(InterfaceInfo inf) {
        intf = inf;
    }
    public InterfaceInfo getInterface() {
        return intf;
    }
    
    public BindingInfo getBinding(QName qn) {
        return bindings.get(qn);
    }
    public void addBinding(BindingInfo binding) {
        bindings.put(binding.getName(), binding);
    }
    public EndpointInfo getEndpoint(QName qn) {
        return endpoints.get(qn);
    }
    public void addEndpoint(EndpointInfo ep) {
        endpoints.put(ep.getName(), ep);
    }
    
    public Collection<EndpointInfo> getEndpoints() {
        return Collections.unmodifiableCollection(endpoints.values());
    }
    
    public Collection<BindingInfo> getBindings() {
        return Collections.unmodifiableCollection(bindings.values());
    }
    
    public Map<QName, MessageInfo> getMessages() {
        if (messages == null) {
            initMessagesMap();           
        }
        return messages;
    }
    
    public MessageInfo getMessage(QName qname) {
        return getMessages().get(qname);
    }
    
    private void initMessagesMap() {
        messages = new ConcurrentHashMap<QName, MessageInfo>();
        for (OperationInfo operation : getInterface().getOperations()) {
            if (operation.getInput() != null) {
                messages.put(operation.getInput().getName(), operation.getInput());
            }
            if (operation.getOutput() != null) {
                messages.put(operation.getOutput().getName(), operation.getOutput());
            }
        }       
    }
    
    public void setMessages(Map<QName, MessageInfo> msgs) {
        messages = msgs;
    }
    
    public void addMessage(MessageInfo msg) {
        if (messages == null) {
            initMessagesMap();           
        }
        messages.put(msg.getName(), msg);
    }
    
    public void addSchema(SchemaInfo schemaInfo) {
        schemas.add(schemaInfo);
    }

    public SchemaInfo getSchema(String namespaceURI) {
        for (SchemaInfo s : schemas) {
            if (s.getNamespaceURI().equals(namespaceURI)) {
                return s;
            }
        }
        return null;
    }
    
    public Collection<SchemaInfo> getSchemas() {
        return Collections.unmodifiableCollection(schemas);
    }

    public void setXmlSchemaCollection(XmlSchemaCollection col) {
        this.xmlSchemaCollection = col;
    }

    public XmlSchemaCollection getXmlSchemaCollection() {
        return xmlSchemaCollection;
    }
}

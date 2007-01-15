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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

public final class ServiceModelUtil {

    private ServiceModelUtil() {
    }

    public static Service getService(Exchange exchange) {
        return exchange.get(Service.class);
    }
    
    public static String getTargetNamespace(Exchange exchange) {
        return getService(exchange).getServiceInfo().getTargetNamespace();
    }
    
    public static BindingOperationInfo getOperation(Exchange exchange, String opName) {
        Endpoint ep = exchange.get(Endpoint.class);
        BindingInfo service = ep.getEndpointInfo().getBinding();
        
        for (BindingOperationInfo b : service.getOperations()) {
            if (b.getName().getLocalPart().equals(opName)) {
                return b;
            }
        }
        return null;
    }

    public static BindingOperationInfo getOperation(Exchange exchange, QName opName) {
        Endpoint ep = exchange.get(Endpoint.class);
        BindingInfo service = ep.getEndpointInfo().getBinding();
        return service.getOperation(opName);
    }

    public static SchemaInfo getSchema(ServiceInfo serviceInfo, MessagePartInfo messagePartInfo) {
        SchemaInfo schemaInfo = null;
        String tns = null;
        if (messagePartInfo.isElement()) {
            tns = messagePartInfo.getElementQName().getNamespaceURI();
        } else {
            tns = messagePartInfo.getTypeQName().getNamespaceURI();
        }
        for (SchemaInfo schema : serviceInfo.getSchemas()) {
            if (tns.equals(schema.getNamespaceURI())) {
                schemaInfo = schema;
            }
        }
        return schemaInfo;
    }
    
    public static List<String> getOperationInputPartNames(OperationInfo operation) {
        List<String> names = new ArrayList<String>();
        List<MessagePartInfo> parts = operation.getInput().getMessageParts();
        if (parts == null && parts.size() == 0) {
            return names;
        }

        for (MessagePartInfo part : parts) {
            XmlSchemaAnnotated schema = part.getXmlSchema();

            if (schema instanceof XmlSchemaElement
                && ((XmlSchemaElement)schema).getSchemaType() instanceof XmlSchemaComplexType) {
                XmlSchemaElement element = (XmlSchemaElement)schema;    
                XmlSchemaComplexType cplxType = (XmlSchemaComplexType)element.getSchemaType();
                XmlSchemaSequence seq = (XmlSchemaSequence)cplxType.getParticle();
                if (seq == null || seq.getItems() == null) {
                    return names;
                }
                for (int i = 0; i < seq.getItems().getCount(); i++) {
                    XmlSchemaElement elChild = (XmlSchemaElement)seq.getItems().getItem(i);
                    names.add(elChild.getName());
                }
            } else {
                names.add(part.getConcreteName().getLocalPart());
            }
        }
        return names;
    }    
}

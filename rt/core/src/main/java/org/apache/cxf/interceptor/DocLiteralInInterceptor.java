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

package org.apache.cxf.interceptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
//import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class DocLiteralInInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = Logger.getLogger(DocLiteralInInterceptor.class.getName());
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(DocLiteralInInterceptor.class);

    private static Set<String> filter = new HashSet<String>();

    static {
        filter.add("void");
        filter.add("javax.activation.DataHandler");
    }

    public DocLiteralInInterceptor() {
        super();
        setPhase(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.info("BareInInterceptor skipped in HTTP GET method");
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        DataReader<Message> dr = getMessageDataReader(message);
        List<Object> parameters = new ArrayList<Object>();

        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);

        boolean client = isRequestor(message);

        //if body is empty and we have BindingOperationInfo, we do not need to match 
        //operation anymore, just return
        if (!StaxUtils.toNextElement(xmlReader) && bop != null) {
            // body may be empty for partial response to decoupled request
            return;
        }

        //bop might be a unwrpped, wrap it back so that we can get correct info 
        if (bop != null && bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }

        if (bop == null) {
            QName startQName = xmlReader.getName();
            bop = getBindingOperationInfoForWrapped(exchange, startQName, client);
        }

        if (bop != null && bop.isUnwrappedCapable()) {
            // Wrapped case
            MessageInfo msgInfo = setMessage(message, bop, client);

            // Determine if there is a wrapper class
            if (msgInfo.getMessageParts().get(0).getTypeClass() != null) {
                Object wrappedObject = dr.read(msgInfo.getMessageParts().get(0), message);
                parameters.add(wrappedObject);

            } else {
                // Unwrap each part individually if we don't have a wrapper

                bop = bop.getUnwrappedOperation();

                msgInfo = setMessage(message, bop, client);
                List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
                Iterator<MessagePartInfo> itr = messageParts.iterator();

                // advance just past the wrapped element so we don't get
                // stuck
                if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StaxUtils.nextEvent(xmlReader);
                }

                // loop through each child element
                while (StaxUtils.toNextElement(xmlReader)) {
                    MessagePartInfo part = itr.next();
                    parameters.add(dr.read(part, message));
                }
            }

        } else {
            //Bare style
            BindingMessageInfo msgInfo = null;

            if (bop != null) { //for xml binding or client side
                getMessageInfo(message, bop, exchange);
                if (client) {
                    msgInfo = bop.getOutput();
                } else {
                    msgInfo = bop.getInput();
                }

            }

            Collection<OperationInfo> operations = null;
            operations = new ArrayList<OperationInfo>();
            Endpoint ep = exchange.get(Endpoint.class);
            Service service = ep.getService();
            operations.addAll(service.getServiceInfo().getInterface().getOperations());

            if (!StaxUtils.toNextElement(xmlReader)) {
                // empty input

                // TO DO : check duplicate operation with no input
                for (OperationInfo op : operations) {
                    MessageInfo bmsg = op.getInput();
                    if (bmsg.getMessageParts().size() == 0) {
                        BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                        exchange.put(BindingOperationInfo.class, boi);
                        exchange.put(OperationInfo.class, op);
                        exchange.setOneWay(op.isOneWay());
                    }
                }
                return;
            }

            int paramNum = 0;

            do {
                QName elName = xmlReader.getName();
                Object o = null;

                MessagePartInfo p;
                if (msgInfo != null && msgInfo.getMessageParts() != null) {
                    assert msgInfo.getMessageParts().size() > paramNum;
                    p = msgInfo.getMessageParts().get(paramNum);
                } else {
                    p = findMessagePart(exchange, operations, elName, client, paramNum);
                }

                if (p == null) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", BUNDLE, elName));
                }

                o = dr.read(p, message);

                if (o != null) {
                    parameters.add(o);
                }
                paramNum++;
            } while (StaxUtils.toNextElement(xmlReader));

        }

        if (parameters.size() > 0) {
            message.setContent(List.class, parameters);
        }
    }

    private MessageInfo setMessage(Message message, BindingOperationInfo operation, boolean requestor) {
        MessageInfo msgInfo = getMessageInfo(message, operation, requestor);
        message.put(MessageInfo.class, msgInfo);

        message.getExchange().put(BindingOperationInfo.class, operation);
        message.getExchange().put(OperationInfo.class, operation.getOperationInfo());
        message.getExchange().setOneWay(operation.getOperationInfo().isOneWay());

        //Set standard MessageContext properties required by JAX_WS, but not specific to JAX_WS.
        message.put(Message.WSDL_OPERATION, operation.getName());

        Service service = message.getExchange().get(Service.class);
        QName serviceQName = service.getServiceInfo().getName();
        message.put(Message.WSDL_SERVICE, serviceQName);

        QName interfaceQName = service.getServiceInfo().getInterface().getName();
        message.put(Message.WSDL_INTERFACE, interfaceQName);

        EndpointInfo endpointInfo = message.getExchange().get(Endpoint.class).getEndpointInfo();
        QName portQName = endpointInfo.getName();
        message.put(Message.WSDL_PORT, portQName);

        String address = endpointInfo.getAddress();
        URI wsdlDescription = null;
        try {
            wsdlDescription = new URI(address + "?wsdl");
        } catch (URISyntaxException e) {
            //do nothing
        }
        message.put(Message.WSDL_DESCRIPTION, wsdlDescription);

        return msgInfo;
    }

}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamConstants;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class WrappedInInterceptor extends AbstractInDatabindingInterceptor {
    public static final String WRAPPER_CLASS = "wrapper.class";
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(WrappedInInterceptor.class);

    public WrappedInInterceptor() {
        super();
        setPhase(Phase.UNMARSHAL);
    }

    public void handleMessage(Message message) {
        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);

        // Trying to find the operation name from the XML.
        if (!StaxUtils.toNextElement(xmlReader)) {
            // body may be empty for partial response to decoupled request
            return;
        }

        BindingOperationInfo operation = message.getExchange().get(BindingOperationInfo.class);
        boolean requestor = isRequestor(message);

        if (operation == null) {
            String local = xmlReader.getLocalName();
            if (requestor && local.endsWith("Response")) {
                local = local.substring(0, local.length() - 8);
            }

            // TODO: Allow overridden methods.
            operation = ServiceModelUtil.getOperation(message.getExchange(), local);
            if (operation == null) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION", BUNDLE, local));
            }

        }

        DataReader<Message> dr = getMessageDataReader(message);
        List<Object> objects;

        // Determine if there is a wrapper class
        if ((operation.isUnwrapped() || operation.isUnwrappedCapable())
            && operation.getOperationInfo().getUnwrappedOperation().getInput()
                .getProperty(WRAPPER_CLASS) != null) {
            objects = new ArrayList<Object>();
            Object wrappedObject = dr.read(message);
            objects.add(wrappedObject);

            setMessage(message, operation, requestor);
        } else {
            // Unwrap each part individually if we don't have a wrapper
            objects = new ArrayList<Object>();

            if (operation.isUnwrappedCapable()) {
                operation = operation.getUnwrappedOperation();
            }

            MessageInfo msgInfo = setMessage(message, operation, requestor);
            List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
            Iterator<MessagePartInfo> itr = messageParts.iterator();

            // advance just past the wrapped element so we don't get stuck
            if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                StaxUtils.nextEvent(xmlReader);
            }

            // loop through each child element
            while (StaxUtils.toNextElement(xmlReader)) {
                MessagePartInfo part = itr.next();
                Class c = (Class)part.getProperty(Class.class.getName());
                objects.add(dr.read(part.getConcreteName(), message, c));
            }

        }

        message.setContent(List.class, objects);
    }

    private MessageInfo setMessage(Message message, BindingOperationInfo operation, boolean requestor) {
        MessageInfo msgInfo = getMessageInfo(message, operation, requestor);
        message.put(MessageInfo.class, msgInfo);

        message.getExchange().put(BindingOperationInfo.class, operation);
        message.getExchange().put(OperationInfo.class, operation.getOperationInfo());
        message.getExchange().setOneWay(operation.getOperationInfo().isOneWay());

        return msgInfo;
    }
}

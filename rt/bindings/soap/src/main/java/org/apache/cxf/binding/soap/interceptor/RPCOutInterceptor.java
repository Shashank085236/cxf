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

package org.apache.cxf.binding.soap.interceptor;

import org.apache.cxf.interceptor.WrappedOutInterceptor;

public class RPCOutInterceptor extends WrappedOutInterceptor {
    
//    public void handleMessage(SoapMessage message) {
//        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
//        OperationInfo opInfo = bop.getOperationInfo();
//        OperationInfo unwrapped = new UnwrappedOperationInfo(opInfo);
//        opInfo.setUnwrappedOperation(unwrapped);
//        unwrapped.setInput(opInfo.getInputName(), unwrappedInput);
//    }
//    
//    private NSStack nsStack;
//    
//    private void init() {
//        nsStack = new NSStack();
//        nsStack.push();
//    }
//
//    public void handleMessage(SoapMessage message) {
//        try {
//            init();
//            
//            BindingOperationInfo operation = ServiceModelUtil.getOperation(message,
//                                                                           getOperationName(message));
//
//            assert operation.getName() != null;
//            
//            XMLStreamWriter xmlWriter = getXMLStreamWriter(message);
//            DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message, operation.getOperationInfo());
//
//            addOperationNode(message, xmlWriter);
//
//            int countParts = 0;
//            List<MessagePartInfo> parts = null;
//            
//            if (isOutboundMessage(message)) {
//                parts = operation.getOutput().getMessageInfo().getMessageParts();
//            } else {
//                parts = operation.getInput().getMessageInfo().getMessageParts();
//            }
//            countParts = parts.size();
//
//            if (countParts > 0) {
//                List<?> objs = (List<?>) message.get(Message.INVOCATION_OBJECTS);
//                Object[] args = objs.toArray();
//                Object[] els  = parts.toArray();
//
//                if (args.length != els.length) {
//                    message.setContent(Exception.class,
//                                       new RuntimeException("The number of arguments is not equal!"));
//                }
//                
//                for (int idx = 0; idx < countParts; idx++) {
//                    Object arg = args[idx];
//                    MessagePartInfo  part = (MessagePartInfo) els[idx];
//                    QName elName = getPartName(part);
//                    dataWriter.write(arg, elName, xmlWriter);
//                }
//            }
//
//            // Finishing the writing.
//            xmlWriter.writeEndElement();
//            xmlWriter.flush();
//            xmlWriter.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//            message.setContent(Exception.class, e);
//        }
//    }
//
//    private QName getPartName(MessagePartInfo part) {
//        QName name = part.getElementQName();
//        if (name == null) {
//            name = part.getTypeQName();
//        }
//        return new QName(name.getNamespaceURI(), part.getName().getLocalPart());
//    }
//
//    protected boolean isOutboundMessage(SoapMessage message) {
//        return message.containsKey(Message.INBOUND_MESSAGE);
//    }
//
//    protected DataWriter<XMLStreamWriter> getDataWriter(SoapMessage message, OperationInfo oi) {
//        String key = (String) message.getExchange().get(SoapMessage.DATAWRITER_FACTORY_KEY);
//        DataWriterFactory factory = (DataWriterFactory) oi.getProperty(key);
//
//        DataWriter<XMLStreamWriter> dataWriter = null;
//        for (Class<?> cls : factory.getSupportedFormats()) {
//            if (cls == XMLStreamWriter.class) {
//                dataWriter = factory.createWriter(XMLStreamWriter.class);
//                break;
//            }
//        }
//        if (dataWriter == null) {
//            message.setContent(Exception.class,
//                               new RuntimeException("Could not figure out how to marshal data"));
//        }        
//        return dataWriter;
//    }
//    
//    private String getOperationName(SoapMessage message) {
//        return (String) message.get(Message.INVOCATION_OPERATION);
//    }
//
//    protected void addOperationNode(SoapMessage message, XMLStreamWriter xmlWriter)
//        throws XMLStreamException {
//        String responseSuffix = isOutboundMessage(message) ? "Response" : "";
//        String namespaceURI = ServiceModelUtil.getTargetNamespace(message);
//        nsStack.add(namespaceURI);
//        String prefix = nsStack.getPrefix(namespaceURI);
//            
//        String operationName = getOperationName(message) + responseSuffix;
//            
//        StaxUtils.writeStartElement(xmlWriter, prefix, operationName, namespaceURI);
//        xmlWriter.flush();
//    }
//
//    private XMLStreamWriter getXMLStreamWriter(Message message) {
//        return message.getContent(XMLStreamWriter.class);
//    }
}


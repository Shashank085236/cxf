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
package org.apache.cxf.binding.xml.interceptor;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.xml.XMLConstants;
import org.apache.cxf.binding.xml.XMLFault;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;

public class XMLFaultOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLFaultOutInterceptor.class);

    public XMLFaultOutInterceptor() {
        super();
        setPhase(Phase.MARSHAL);
    }

    public void handleMessage(Message message) throws Fault {        
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, new Integer(500));
        NSStack nsStack = new NSStack();
        nsStack.push();

        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        Fault f = (Fault) message.getContent(Exception.class);
        XMLFault xmlFault = XMLFault.createFault(f);
        try {
            nsStack.add(XMLConstants.NS_XML_FORMAT);
            String prefix = nsStack.getPrefix(XMLConstants.NS_XML_FORMAT);

            StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_ROOT, XMLConstants.NS_XML_FORMAT);

            StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_STRING, 
                    XMLConstants.NS_XML_FORMAT);
            Throwable t = xmlFault.getCause();
            StringBuffer str = new StringBuffer(t.toString());
            QName elName = null;
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            if (!bop.isUnwrappedCapable()) {
                bop = bop.getUnwrappedOperation();
            }
            Iterator<FaultInfo> it = bop.getOperationInfo().getFaults().iterator();
            // Boolean isWebFault = null;
            while (it.hasNext()) {
                FaultInfo fi = it.next();
                for (MessagePartInfo mpi : fi.getMessageParts()) {
                    Class cls = mpi.getProperty(Class.class.getName(), Class.class);
                    Method method = t.getClass().getMethod("getFaultInfo", new Class[0]);
                    Class sub = method.getReturnType();
                    if (cls != null && cls.equals(sub)) {
                        if (mpi.isElement()) {
                            elName = mpi.getElementQName();
                        } else {
                            elName = mpi.getTypeQName();
                        }
                        // isWebFault = mpi.getProperty("javax.xml.ws.WebFault", Boolean.class);
                        break;
                    }
                }
            }
            /*
             * do not send stack trace information to client
            if (!(isWebFault != null && isWebFault.booleanValue())) {
                str.append("\n");
                for (StackTraceElement s : t.getStackTrace()) {
                    str.append(s.toString());
                    str.append("\n");
                }
            }
             */
            writer.writeCharacters(str.toString());
            // fault string
            writer.writeEndElement();
            
            // call data writer to marshal exception
            if (elName != null) {
                StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_DETAIL,
                        XMLConstants.NS_XML_FORMAT);
                DataWriter<Message> dataWriter = getMessageDataWriter(message);
                dataWriter.write(getFaultInfo(t), elName, message);
                writer.writeEndElement();
            }
            // fault root
            writer.writeEndElement();
            
            writer.flush();

        } catch (NoSuchMethodException ne) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("UNKNOWN_EXCEPTION", BUNDLE), ne);        
        } catch (XMLStreamException xe) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), xe);
        }

    }

    private static Object getFaultInfo(Throwable fault) {
        try {
            Method faultInfoMethod = fault.getClass().getMethod("getFaultInfo");
            if (faultInfoMethod != null) {
                return faultInfoMethod.invoke(fault);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not get faultInfo out of Exception", ex);
        }

        return null;
    }
}

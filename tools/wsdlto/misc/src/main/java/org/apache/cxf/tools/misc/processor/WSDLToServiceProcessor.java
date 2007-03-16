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

package org.apache.cxf.tools.misc.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.WSDLConstants;
import org.apache.cxf.tools.common.extensions.soap.SoapAddress;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.transport.jms.AddressType;

public class WSDLToServiceProcessor extends AbstractWSDLToProcessor {

    private static final String NEW_FILE_NAME_MODIFIER = "-service";
    private static final String HTTP_PREFIX = "http://localhost:9000";

    private Map services;
    private Service service;
    private Map ports;
    private Port port;
    private Binding binding;

    public void process() throws ToolException {
        init();
        if (isServicePortExisted()) {
            Message msg = new Message("SERVICE_PORT_EXIST", LOG);
            throw new ToolException(msg);
        }
        if (!isBindingExisted()) {
            Message msg = new Message("BINDING_NOT_EXIST", LOG);
            throw new ToolException(msg);
        }
        doAppendService();
    }

    private boolean isServicePortExisted() {
        return isServiceExisted() && isPortExisted();
    }

    private boolean isServiceExisted() {
        services = wsdlDefinition.getServices();
        if (services == null) {
            return false;
        }
        Iterator it = services.keySet().iterator();
        while (it.hasNext()) {
            QName serviceQName = (QName)it.next();
            String serviceName = serviceQName.getLocalPart();
            if (serviceName.equals(env.get(ToolConstants.CFG_SERVICE))) {
                service = (Service)services.get(serviceQName);
                break;
            }
        }
        return (service == null) ? false : true;
    }

    private boolean isPortExisted() {
        ports = service.getPorts();
        if (ports == null) {
            return false;
        }
        Iterator it = ports.keySet().iterator();
        while (it.hasNext()) {
            String portName = (String)it.next();
            if (portName.equals(env.get(ToolConstants.CFG_PORT))) {
                port = (Port)ports.get(portName);
                break;
            }
        }
        return (port == null) ? false : true;
    }

    private boolean isSOAP12() {
        return env.optionSet(ToolConstants.CFG_SOAP12);
    }

    private boolean isBindingExisted() {
        Map bindings = wsdlDefinition.getBindings();
        if (bindings == null) {
            return false;
        }
        Iterator it = bindings.keySet().iterator();
        while (it.hasNext()) {
            QName bindingQName = (QName)it.next();
            String bindingName = bindingQName.getLocalPart();
            String attrBinding = (String)env.get(ToolConstants.CFG_BINDING_ATTR);
            if (attrBinding.equals(bindingName)) {
                binding = (Binding)bindings.get(bindingQName);
            }
        }
        return (binding == null) ? false : true;
    }

    protected void init() throws ToolException {
        parseWSDL((String)env.get(ToolConstants.CFG_WSDLURL));
    }

    private void doAppendService() throws ToolException {
        if (service == null) {
            service = wsdlDefinition.createService();
            service
                .setQName(new QName(WSDLConstants.WSDL_PREFIX, (String)env.get(ToolConstants.CFG_SERVICE)));
        }
        if (port == null) {
            port = wsdlDefinition.createPort();
            port.setName((String)env.get(ToolConstants.CFG_PORT));
            port.setBinding(binding);
        }
        setAddrElement();
        service.addPort(port);
        wsdlDefinition.addService(service);

        WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
        Writer outputWriter = getOutputWriter(NEW_FILE_NAME_MODIFIER);
        try {
            wsdlWriter.writeWSDL(wsdlDefinition, outputWriter);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg, wse);
        }
        try {
            outputWriter.close();
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_CLOSE_WSDL_FILE", LOG);
            throw new ToolException(msg, ioe);
        }

    }

    private void setAddrElement() throws ToolException {
        ExtensionRegistry extReg = this.wsdlReader.getExtensionRegistry();
        if (extReg == null) {
            extReg = wsdlFactory.newPopulatedExtensionRegistry();
        }
        if ("http".equalsIgnoreCase((String)env.get(ToolConstants.CFG_TRANSPORT))) {
            SoapAddress soapAddress = null;
            try {
                soapAddress = SOAPBindingUtil.createSoapAddress(extReg, isSOAP12());
            } catch (WSDLException wse) {
                Message msg = new Message("FAIL_TO_CREATE_SOAPADDRESS", LOG);
                throw new ToolException(msg, wse);
            }
            
            if (env.get(ToolConstants.CFG_ADDRESS) != null) {
                soapAddress.setLocationURI((String)env.get(ToolConstants.CFG_ADDRESS));
            } else {
                soapAddress.setLocationURI(HTTP_PREFIX + "/" + env.get(ToolConstants.CFG_SERVICE) + "/"
                                           + env.get(ToolConstants.CFG_PORT));
            }
            port.addExtensibilityElement(soapAddress);
        } else if ("jms".equalsIgnoreCase((String)env.get(ToolConstants.CFG_TRANSPORT))) {
            AddressType jmsAddress = null;
            //JMSAddress jmsAddress = null;
            //JMSAddressSerializer jmsAddressSerializer = new JMSAddressSerializer();
            try {
//extReg.registerSerializer(JMSAddress.class, ToolConstants.JMS_ADDRESS, jmsAddressSerializer);
//extReg.registerDeserializer(JMSAddress.class, ToolConstants.JMS_ADDRESS, jmsAddressSerializer);
                jmsAddress = (AddressType)extReg.createExtension(Port.class, ToolConstants.JMS_ADDRESS);
                if (env.optionSet(ToolConstants.JMS_ADDR_DEST_STYLE)) {
                    //jmsAddress.setDestinationStyle((String)env.get(ToolConstants.JMS_ADDR_DEST_STYLE));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_INIT_CTX)) {
                    //jmsAddress.setInitialContextFactory((String)env.get(ToolConstants.JMS_ADDR_INIT_CTX));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_JNDI_DEST)) {
                    jmsAddress.setJndiDestinationName((String)env.get(ToolConstants.JMS_ADDR_JNDI_DEST));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_JNDI_FAC)) {
                    jmsAddress.setJndiConnectionFactoryName((String)env.get(ToolConstants.JMS_ADDR_JNDI_FAC));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_JNDI_URL)) {
                    //jmsAddress.setJndiProviderURL((String)env.get(ToolConstants.JMS_ADDR_JNDI_URL));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_MSGID_TO_CORRID)) {
                    //jmsAddress.setUseMessageIDAsCorrelationID(Boolean.getBoolean((String)env
                    //.get(ToolConstants.JMS_ADDR_MSGID_TO_CORRID)));
                }
                if (env.optionSet(ToolConstants.JMS_ADDR_SUBSCRIBER_NAME)) {
                    //jmsAddress.setDurableSubscriberName((String)env
                    //  .get(ToolConstants.JMS_ADDR_SUBSCRIBER_NAME));
                }
            } catch (WSDLException wse) {
                Message msg = new Message("FAIL_TO_CREATE_SOAP_ADDRESS", LOG);
                throw new ToolException(msg, wse);
            }
            port.addExtensibilityElement(jmsAddress);
        }
    }

}

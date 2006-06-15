package org.objectweb.celtix.tools.utils;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.extensions.jaxws.JAXWSBinding;
import org.objectweb.celtix.tools.extensions.jaxws.JAXWSBindingDeserializer;
import org.objectweb.celtix.tools.extensions.jaxws.JAXWSBindingSerializer;
import org.objectweb.celtix.tools.extensions.jms.JMSAddress;
import org.objectweb.celtix.tools.extensions.jms.JMSAddressSerializer;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLFormat;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLFormatBinding;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLFormatBindingSerializer;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLFormatSerializer;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLHttpAddress;
import org.objectweb.celtix.tools.extensions.xmlformat.XMLHttpSerializer;

public class WSDLExtensionRegister {

    private WSDLFactory wsdlFactory;
    private WSDLReader wsdlreader;

    public WSDLExtensionRegister(WSDLFactory factory, WSDLReader reader) {
        wsdlFactory = factory;
        wsdlreader = reader;
    }

    public void registerExtenstions() {
        ExtensionRegistry registry = wsdlreader.getExtensionRegistry();
        if (registry == null) {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
        }
        registerJAXWSBinding(registry, Definition.class);
        registerJAXWSBinding(registry, PortType.class);
        registerJAXWSBinding(registry, Operation.class);

        registerJAXWSBinding(registry, Binding.class);
        registerJAXWSBinding(registry, BindingOperation.class);

        registerJMSAddress(registry, Port.class);

        registerXMLFormat(registry, BindingInput.class);
        registerXMLFormat(registry, BindingOutput.class);
        registerXMLFormatBinding(registry, Binding.class);
        registerXMLHttpAddress(registry, Port.class);

        wsdlreader.setExtensionRegistry(registry);
    }

    private void registerXMLFormat(ExtensionRegistry registry, Class clz) {
        registry.registerSerializer(clz, ToolConstants.XML_FORMAT, new XMLFormatSerializer());

        registry.registerDeserializer(clz, ToolConstants.XML_FORMAT, new XMLFormatSerializer());
        registry.mapExtensionTypes(clz, ToolConstants.XML_FORMAT, XMLFormat.class);
    }

    private void registerXMLFormatBinding(ExtensionRegistry registry, Class clz) {
        registry.registerSerializer(clz, ToolConstants.XML_BINDING_FORMAT, new XMLFormatBindingSerializer());

        registry
            .registerDeserializer(clz, ToolConstants.XML_BINDING_FORMAT, new XMLFormatBindingSerializer());
        registry.mapExtensionTypes(clz, ToolConstants.XML_BINDING_FORMAT, XMLFormatBinding.class);
    }

    private void registerXMLHttpAddress(ExtensionRegistry registry, Class clz) {
        registry.registerSerializer(clz, ToolConstants.XML_HTTP_ADDRESS, new XMLHttpSerializer());

        registry.registerDeserializer(clz, ToolConstants.XML_HTTP_ADDRESS, new XMLHttpSerializer());
        registry.mapExtensionTypes(clz, ToolConstants.XML_HTTP_ADDRESS, XMLHttpAddress.class);
    }

    private void registerJMSAddress(ExtensionRegistry registry, Class clz) {
        registry.registerSerializer(clz, ToolConstants.JMS_ADDRESS, new JMSAddressSerializer());

        registry.registerDeserializer(clz, ToolConstants.JMS_ADDRESS, new JMSAddressSerializer());
        registry.mapExtensionTypes(clz, ToolConstants.JMS_ADDRESS, JMSAddress.class);
    }

    private void registerJAXWSBinding(ExtensionRegistry registry, Class clz) {
        registry.registerSerializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingSerializer());

        registry.registerDeserializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingDeserializer());
        registry.mapExtensionTypes(clz, ToolConstants.JAXWS_BINDINGS, JAXWSBinding.class);
    }

}

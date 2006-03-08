package org.objectweb.celtix.tools.extensions.xmlformat;

import java.io.PrintWriter;
import java.io.Serializable;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

public class XMLFormatSerializer implements ExtensionSerializer, ExtensionDeserializer,
    Serializable {

    public void marshall(Class parentType, QName elementType, ExtensibilityElement extension,
                         PrintWriter pw, Definition def, ExtensionRegistry extReg)
        throws WSDLException {

    }

    public ExtensibilityElement unmarshall(Class parentType, QName elementType, Element el,
                                           Definition def, ExtensionRegistry extReg)
        throws WSDLException {

        XMLFormat xmlFormat = (XMLFormat)extReg.createExtension(parentType, elementType);
        xmlFormat.setElement(el);
        xmlFormat.setElementType(elementType);
        xmlFormat.setDocumentBaseURI(def.getDocumentBaseURI());
        XMLFormatParser xmlBindingParser = new XMLFormatParser();
        xmlBindingParser.parseElement(def, xmlFormat, el);
        return xmlFormat;
    }

}

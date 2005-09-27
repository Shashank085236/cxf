package org.objectweb.celtix.bus.bindings.soap;

import javax.xml.namespace.QName;

public final class SOAPMessageUtil {

    private SOAPMessageUtil() {
        // Utility class - never constructed
    }
    
    public static String createWrapDocLitSOAPMessage(QName wrapName, QName elName, String data) {
        StringBuffer str = new StringBuffer();
        
        str.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
        str.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        str.append("<SOAP-ENV:Body>");

        str.append("<ns2:" + wrapName.getLocalPart() + " xmlns:ns2=\"" + wrapName.getNamespaceURI() + "\">");
        str.append("<ns2:" + elName.getLocalPart() + ">");
        str.append(data);
        str.append("</ns2:" + elName.getLocalPart() + ">");
        str.append("</ns2:" + wrapName.getLocalPart() + ">");
        
        str.append("</SOAP-ENV:Body>");
        str.append("</SOAP-ENV:Envelope>");
        
        return str.toString();
    }
}

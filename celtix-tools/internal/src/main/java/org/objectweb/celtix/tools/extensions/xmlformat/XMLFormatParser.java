package org.objectweb.celtix.tools.extensions.xmlformat;

import javax.wsdl.Definition;

import org.w3c.dom.*;

import org.objectweb.celtix.helpers.XMLUtils;
import org.objectweb.celtix.tools.common.ToolConstants;

public class XMLFormatParser {

    private XMLUtils xmlUtils = new XMLUtils();
    
    public void parseElement(Definition def, XMLFormat xmlFormat, Element element) {
        Attr rootNodeAttribute = xmlUtils.getAttribute(element, ToolConstants.XMLBINDING_ROOTNODE);
        String rootNodeValue = rootNodeAttribute.getValue();
        
        if (rootNodeValue != null) {
            xmlFormat.setRootNode(xmlUtils.getNamespace(def.getNamespaces(),
                                                        rootNodeValue,
                                                        def.getTargetNamespace()));
        }
    }
}

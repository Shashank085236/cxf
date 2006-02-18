package org.objectweb.celtix.tools.generators.wsdl2;

import java.io.*;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.objectweb.celtix.tools.common.ProcessorEnvironment;
import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.common.model.JavaAnnotation;
import org.objectweb.celtix.tools.common.model.JavaInterface;
import org.objectweb.celtix.tools.common.toolspec.ToolException;
import org.objectweb.celtix.tools.generators.AbstractGenerator;
import org.objectweb.celtix.tools.utils.ProcessorUtil;
import org.objectweb.celtix.tools.utils.XMLUtil;

public class HandlerConfigGenerator extends AbstractGenerator {

    private static final String HANDLER_CHAIN_NAME = "";
    private JavaInterface intf;
    private JavaAnnotation handlerChainAnnotation; 
    public HandlerConfigGenerator() {
        this.name = ToolConstants.HANDLER_GENERATOR;
    }

    public HandlerConfigGenerator(JavaInterface i, ProcessorEnvironment env) {
        this();
        this.intf = i;
        setEnvironment(env);
    }

    public JavaAnnotation getHandlerAnnotation() {
        return handlerChainAnnotation;
    }

    public boolean passthrough() {
        if (this.intf.getHandlerChains() == null) {
            return true;
        }
        return false;
    }
    
    public void generate() throws ToolException {
        if (passthrough()) {
            return;
        }

        Element e = this.intf.getHandlerChains();
        NodeList nl = e.getElementsByTagNameNS(ToolConstants.HANDLER_CHAINS_URI,
                                               ToolConstants.HANDLER_CHAIN);
        if (nl.getLength() > 0) {
            String fName = ProcessorUtil.getHandlerConfigFileName(this.intf.getName());
            handlerChainAnnotation = new JavaAnnotation("HandlerChain");
            handlerChainAnnotation.addArgument("name", HANDLER_CHAIN_NAME);
            handlerChainAnnotation.addArgument("file", fName + ".xml");
            generateHandlerChainFile(e, parseOutputName(this.intf.getPackageName(),
                                                        fName,
                                                        ".xml"));
        }
    }

    private void generateHandlerChainFile(Element hChains, Writer writer) throws ToolException {
        XMLUtil.generateXMLFile(hChains, writer);
    }
}

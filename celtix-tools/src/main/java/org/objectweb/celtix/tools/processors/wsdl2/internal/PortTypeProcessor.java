package org.objectweb.celtix.tools.processors.wsdl2.internal;

import java.io.*;
import java.util.*;
import javax.wsdl.Operation;
import javax.wsdl.PortType;

import org.objectweb.celtix.tools.common.ProcessorEnvironment;
import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.common.model.JavaInterface;
import org.objectweb.celtix.tools.common.model.JavaModel;
import org.objectweb.celtix.tools.common.toolspec.ToolException;
import org.objectweb.celtix.tools.jaxws.CustomizationParser;
import org.objectweb.celtix.tools.jaxws.JAXWSBinding;
import org.objectweb.celtix.tools.utils.ProcessorUtil;

public class PortTypeProcessor {

    private final ProcessorEnvironment env;
    private List<String> operationMap = new ArrayList<String>();
    
    public PortTypeProcessor(ProcessorEnvironment penv) {
        this.env = penv;
    }
    
    public void process(JavaModel jmodel, PortType portType) throws ToolException {
        operationMap.clear();
        JavaInterface intf = new JavaInterface(jmodel);
        intf.setJAXWSBinding(customizing(jmodel, portType));
        intf.setHandlerChains(CustomizationParser.getInstance().getHandlerChains());
        
        String namespace = portType.getQName().getNamespaceURI();
        String packageName = ProcessorUtil.parsePackageName(namespace,
                                                            (String) env.get(ToolConstants.CFG_PACKAGENAME));
        String location = (String) env.get(ToolConstants.CFG_WSDLURL);
        try {
            location = ProcessorUtil.getAbsolutePath(location);
        } catch (IOException ioe) {
            throw new ToolException("Can not find wsdl absolute location from "
                                    + env.get(ToolConstants.CFG_WSDLURL),
                                    ioe);
        }
        String serviceName = portType.getQName().getLocalPart();
        intf.setWebServiceName(serviceName);
        intf.setName(ProcessorUtil.mangleNameToClassName(serviceName));
        intf.setNamespace(namespace);
        intf.setPackageName(packageName);
        intf.setLocation(location);

        List operations = portType.getOperations();
        for (Iterator iter = operations.iterator(); iter.hasNext();) {
            Operation operation = (Operation) iter.next();
            if (isOverloading(operation.getName())) {
                continue;
            }
            OperationProcessor operationProcessor = new OperationProcessor(env);
            operationProcessor.process(intf, operation);
        }
        jmodel.setLocation(location);
        jmodel.addInterface(intf.getName() , intf);
       
    }

    private boolean isOverloading(String operationName) {
        if (operationMap.contains(operationName)) {
            return true;
        } else {
            operationMap.add(operationName);
        }
        return false;
    }

    private JAXWSBinding customizing(JavaModel jmodel, PortType portType) {
        String portTypeName = portType.getQName().getLocalPart();
        JAXWSBinding bindings = CustomizationParser.getInstance().getPortTypeExtension(portTypeName);
        if (bindings != null) {
            return bindings;
        } else if (jmodel.getJAXWSBinding() != null) {
            return jmodel.getJAXWSBinding();
        } else {
            // TBD: There is no extensibilityelement in port type
            return new JAXWSBinding();
        }
    }
}

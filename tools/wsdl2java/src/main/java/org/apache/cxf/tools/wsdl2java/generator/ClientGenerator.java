package org.apache.cxf.tools.wsdl2java.generator;

import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.tools.common.ProcessorEnvironment;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;

public class ClientGenerator extends AbstractGenerator {

    private static final String CLT_TEMPLATE = TEMPLATE_BASE + "/client.vm";
    
    public ClientGenerator(JavaModel jmodel, ProcessorEnvironment env) {
        super(jmodel, env);
        this.name = ToolConstants.CLT_GENERATOR;
    }


    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_CLIENT) 
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT) || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        return true;
    }


    public void generate() throws ToolException {
        if (passthrough()) {
            return;
        }

        Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
        JavaServiceClass js = null;
        JavaPort jp = null;

        for (Iterator iter = interfaces.keySet().iterator(); iter.hasNext();) {
            String interfaceName = (String)iter.next();
            JavaInterface intf = interfaces.get(interfaceName);

            Iterator it = javaModel.getServiceClasses().values().iterator();
            while (it.hasNext()) {
                String serviceName = "";
                js = (JavaServiceClass)it.next();
                Iterator i = js.getPorts().iterator();
                while (i.hasNext()) {
                    jp = (JavaPort)i.next();
                    if (jp.getPortType().equals(interfaceName)) {
                        serviceName = js.getName();
                        
                        break;
                    }
                }
                if (!"".equals(serviceName)) {
                    break;
                }
            }

           
            String clientClassName = interfaceName + "Client";
            while (isCollision(intf.getPackageName(), clientClassName)) {
                clientClassName = clientClassName + "_Client";
            }
            
            clearAttributes();
            setAttributes("clientClassName", clientClassName);
            setAttributes("intf", intf);
            setAttributes("service", js);
            setAttributes("port", jp);
            
            setCommonAttributes();
           
            doWrite(CLT_TEMPLATE, parseOutputName(intf.getPackageName(),
                    clientClassName));
        }
    }
}

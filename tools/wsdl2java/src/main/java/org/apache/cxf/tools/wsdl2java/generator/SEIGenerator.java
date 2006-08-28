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

package org.apache.cxf.tools.wsdl2java.generator;

import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.tools.common.ProcessorEnvironment;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;

public class SEIGenerator extends AbstractGenerator {

    private static final String SEI_TEMPLATE = TEMPLATE_BASE + "/sei.vm";
   
    public SEIGenerator(JavaModel jmodel, ProcessorEnvironment env) {
        super(jmodel, env);
        this.name = ToolConstants.SEI_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_CLIENT) || env.optionSet(ToolConstants.CFG_GEN_SERVER)) {
            return true;
        }
        return false;
    }

    private boolean hasHandlerConfig(JavaInterface intf) {
        return intf.getHandlerChains() != null;
    }

    public void generate() throws ToolException {
        if (passthrough()) {
            return;
        }

        Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
        for (Iterator iter = interfaces.keySet().iterator(); iter.hasNext();) {
            String interfaceName = (String)iter.next();
            JavaInterface intf = interfaces.get(interfaceName);

            if (hasHandlerConfig(intf)) {
                HandlerConfigGenerator handlerGen = new HandlerConfigGenerator(intf, getEnvironment());
                handlerGen.generate();

                if (handlerGen.getHandlerAnnotation() != null) {
                    intf.addAnnotation(handlerGen.getHandlerAnnotation().toString());
                    intf.addImport("javax.jws.HandlerChain");
                }
            }
            clearAttributes();
            setAttributes("intf", intf);
            setCommonAttributes();

            doWrite(SEI_TEMPLATE, parseOutputName(intf.getPackageName(), intf.getName()));
        }
    }
}

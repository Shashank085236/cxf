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

package org.apache.cxf.tools.java2wsdl.generator.jaxws;

import java.io.File;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.WSDLModel;
import org.apache.cxf.tools.util.SOAPBindingUtil;

public class WSDLGenerator {
    private final WSDLModel wmodel;

    private final ToolContext env;

    private final Definition definition;

    private WSDLFactory wsdlFactory;

    private String wsdlFile;

    private String portTypeName;

    public WSDLGenerator(WSDLModel model, ToolContext penv) {
        wmodel = model;
        env = penv;
        definition = model.getDefinition();
        try {
            wsdlFactory = WSDLFactory.newInstance();
        } catch (javax.wsdl.WSDLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    public void generate() {
        preGenerate();
        TypesGenerator typeGen = new TypesGenerator(wmodel , env);
        typeGen.generate();
        MessagePortTypeGenerator messagePortTypeGen = new MessagePortTypeGenerator(wmodel);
        messagePortTypeGen.generate();
        BindingGenerator bindingGen = new BindingGenerator(wmodel);
        bindingGen.generate(isSOAP12());
        ServiceGenerator serviceGen = new ServiceGenerator(wmodel);
        serviceGen.generate(isSOAP12());
        writeDefinition();

    }

    private boolean isSOAP12() {
        return env.optionSet(ToolConstants.CFG_SOAP12);
    }

    private void preGenerate() {
        String wsdl = (String)env.get(ToolConstants.CFG_OUTPUTFILE);
        String dir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (dir == null) {
            dir = "./";
        }
        File dirFile = new File(dir);
        if (wsdl == null) {
            wsdlFile = new File(dirFile, wmodel.getServiceName() + ".wsdl").toString();
        } else {
            File f = new File(wsdl);
            if (!f.isAbsolute()) {
                f = new File(dirFile, wsdl);
            }
            wsdlFile = f.toString();
        }
        
        Object obj = env.get(ToolConstants.CFG_TNS);
        String targetNameSpace;
        targetNameSpace = obj == null ? wmodel.getTargetNameSpace() : (String)obj;
        wmodel.setTargetNameSpace(targetNameSpace);
        obj = env.get(ToolConstants.CFG_PORTTYPE);
        portTypeName = obj == null ? wmodel.getPortName() : (String)obj;
        wmodel.setPortName(portTypeName);

    }
   
    private boolean writeDefinition() {

        WSDLWriter writer = wsdlFactory.newWSDLWriter();

        java.io.File file = new java.io.File(wsdlFile);
        java.io.OutputStream outstream = null;
        try {
            outstream = new java.io.FileOutputStream(file);
        } catch (java.io.FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e);
        }

        try {
            SOAPBindingUtil.addSOAPNamespace(definition, isSOAP12());
            writer.writeWSDL(this.definition, outstream);
        } catch (javax.wsdl.WSDLException e) {
            throw new ToolException(e.getMessage(), e);
        }
        return true;
    }
}

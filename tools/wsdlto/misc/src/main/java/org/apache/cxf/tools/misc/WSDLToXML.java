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

package org.apache.cxf.tools.misc;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.misc.processor.WSDLToXMLProcessor;

public class WSDLToXML extends AbstractCXFToolContainer {

    static final String TOOL_NAME = "wsdl2xml";    
    static final String BINDING_NAME_POSFIX = "_XMLBinding";
    static final String SERVICE_NAME_POSFIX = "_XMLService";
    static final String PORT_NAME_POSFIX = "_XMLPort";
    
    public WSDLToXML(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }
    
    private Set getArrayKeys() {
        return new HashSet<String>();
    }
    
    public void execute(boolean exitOnFinish) {
        WSDLToXMLProcessor processor = new WSDLToXMLProcessor();
        try {
            super.execute(exitOnFinish);
            if (!hasInfoOption()) {
                ToolContext env = new ToolContext();
                env.setParameters(getParametersMap(getArrayKeys()));
                if (isVerboseOn()) {
                    env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);
                }
                env.put(ToolConstants.CFG_CMD_ARG, getArgument());

                validate(env);       
                setEnvParamDefValues(env);
                
                processor.setEnvironment(env);
                processor.process();
            }
        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            err.println();
            err.println("WSDLToXML Error: " + ex.getMessage());
            if (isVerboseOn()) {
                ex.printStackTrace(err);
            }
        } catch (Exception ex) {
            err.println();
            err.println("WSDLToXML Error: " + ex.getMessage());
            if (isVerboseOn()) {
                ex.printStackTrace(err);
            }
        } finally {
            tearDown();
        }

    }

    private void setEnvParamDefValues(ToolContext env) {
        if (!env.optionSet(ToolConstants.CFG_BINDING)) {
            env.put(ToolConstants.CFG_BINDING, env.get(ToolConstants.CFG_PORTTYPE) + BINDING_NAME_POSFIX);
        }
        if (!env.optionSet(ToolConstants.CFG_SERVICE)) {
            env.put(ToolConstants.CFG_SERVICE, env.get(ToolConstants.CFG_PORTTYPE) + SERVICE_NAME_POSFIX);
        }
        if (!env.optionSet(ToolConstants.CFG_PORT)) {
            env.put(ToolConstants.CFG_PORT, env.get(ToolConstants.CFG_PORTTYPE) + PORT_NAME_POSFIX);
        }        
    }

    private void validate(ToolContext env) throws ToolException {
        String outdir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (outdir != null) {
            File dir = new File(outdir);
            if (!dir.exists() && !dir.mkdirs()) {
                Message msg = new Message("DIRECTORY_COULD_NOT_BE_CREATED", LOG, outdir);
                throw new ToolException(msg);
            }
            if (!dir.isDirectory()) {
                Message msg = new Message("NOT_A_DIRECTORY", LOG, outdir);
                throw new ToolException(msg);
            }
        }
    }

    public static void main(String[] pargs) {
        CommandInterfaceUtils.commandCommonMain();
        try {
            ToolRunner.runTool(WSDLToXML.class,
                               WSDLToXML.class.getResourceAsStream("wsdl2xml.xml"),
                               false,
                               pargs);
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            ex.printStackTrace();
        }
    }

    public void checkParams(ErrorVisitor errors) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (!doc.hasParameter("wsdlurl")) {
            errors.add(new ErrorVisitor.UserError("WSDL/SCHEMA URL has to be specified"));
        }
        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }

}

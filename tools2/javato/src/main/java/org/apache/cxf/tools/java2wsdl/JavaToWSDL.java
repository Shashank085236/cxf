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

package org.apache.cxf.tools.java2wsdl;

import java.util.HashSet;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory;
import org.apache.cxf.tools.java2wsdl.processor.JavaToProcessor;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.apache.cxf.tools.util.AnnotationUtil;

public class JavaToWSDL extends AbstractCXFToolContainer {
   
    private static final String TOOL_NAME = "java2wsdl";
    private static String[] args;

    public JavaToWSDL(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        Processor processor = null;

        try {
            super.execute(exitOnFinish);
            if (!hasInfoOption()) {
                ToolContext env = new ToolContext();
                env.setParameters(getParametersMap(new HashSet()));
                if (isVerboseOn()) {
                    env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);
                }

                // REVISIT: We keep the previous java2wsdl tool for the jaxws generation
                //          due to there are bugs in JaxWsServiceFactoryBean
                //          if we fix those bugs, then safely remove the following condition
                //          just use processor = new JavaToProcessor();
                
                FrontendFactory frontend = FrontendFactory.getInstance();
                frontend.setServiceClass(getServiceClass(env));
                if (FrontendFactory.Style.Simple == frontend.discoverStyle()) {
                    processor = new JavaToProcessor();
                } else {
                    processor = new JavaToWSDLProcessor();
                }
                
                processor.setEnvironment(env);
                processor.process();
            }
        } catch (ToolException ex) {            
            if (ex.getCause() instanceof BadUsageException) {
                getInstance().printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            throw ex;
        } catch (Exception ex) {
            throw new ToolException(ex.getMessage(), ex.getCause());
        }
    }

    public Class getServiceClass(ToolContext context) {
        return AnnotationUtil.loadClass((String)context.get(ToolConstants.CFG_CLASSNAME),
                                        getClass().getClassLoader());
    }

    public static void main(String[] pargs) { 
        try {
            runTool(pargs);
        } catch (BadUsageException ex) {
            System.err.println("Error : " + ex.getMessage());
            getInstance().printUsageException(TOOL_NAME, ex);
            if (getInstance().isVerboseOn()) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (getInstance().isVerboseOn()) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void runTool(String[] pargs) throws Exception {
        args = pargs;
        ToolRunner.runTool(JavaToWSDL.class, JavaToWSDL.class
                .getResourceAsStream("java2wsdl.xml"), false, args);
    }

    public void checkParams(ErrorVisitor errors) throws ToolException {
        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }
}

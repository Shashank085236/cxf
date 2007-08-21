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
package org.apache.cxf.tools.java2ws;

import java.util.HashSet;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.apache.cxf.tools.java2wsdl.processor.ServiceInfoToJavaProcessor;
import org.apache.cxf.tools.util.AnnotationUtil;

public class JavaToWSContainer extends AbstractCXFToolContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(JavaToWSContainer.class);
    private static final String TOOL_NAME = "java2ws";

    public JavaToWSContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        Processor processor = null;
        ErrorVisitor errors = new ErrorVisitor();
        try {
            super.execute(exitOnFinish);
            checkParams(errors);
            if (!hasInfoOption()) {
                ToolContext env = new ToolContext();
                env.setParameters(getParametersMap(new HashSet()));
                if (isVerboseOn()) {
                    env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);
                }
                processor = new JavaToWSDLProcessor();
                processor.setEnvironment(env);
                processor.process();
                
                processor = new ServiceInfoToJavaProcessor();
                processor.setEnvironment(env);
                processor.process();
                
            }
        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
                if (isVerboseOn()) {
                    ex.printStackTrace();
                }
            }
            throw ex;
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.err.println();
            if (isVerboseOn()) {
                ex.printStackTrace();
            }

            throw new ToolException(ex.getMessage(), ex.getCause());
        } finally {
            tearDown();
        }
    }

    public Class getServiceClass(ToolContext context) {
        return AnnotationUtil.loadClass((String)context.get(ToolConstants.CFG_CLASSNAME), getClass()
            .getClassLoader());
    }

    public void checkParams(ErrorVisitor errs) throws ToolException {

        CommandDocument doc = super.getCommandDocument();

        if (doc.hasParameter("frontend")) {
            String ft = doc.getParameter("frontend");
            
            if (!"simple".equalsIgnoreCase(ft) && !"jaxws".equalsIgnoreCase(ft)) {
                Message msg = new Message("INVALID_FORNTEND", LOG, new Object[]{ft});               
                errs.add(new ErrorVisitor.UserError(msg.toString()));
            }
        }
        
        
        if (doc.hasParameter("wrapperbean")) {
            String ft = doc.getParameter("frontend");
            if (ft != null &&  !"jaxws".equalsIgnoreCase(ft)) {
                Message msg = new Message("CANT_GEN_WRAPPERBEAN", LOG);               
                errs.add(new ErrorVisitor.UserError(msg.toString()));
            }
        }

        
        
        
        
        
        if (errs.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);           
            throw new ToolException(msg, new BadUsageException(getUsage(), errs));
        }

    }
}

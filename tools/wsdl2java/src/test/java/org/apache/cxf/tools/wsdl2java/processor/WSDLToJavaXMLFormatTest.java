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

package org.apache.cxf.tools.wsdl2java.processor;

import java.net.URISyntaxException;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;

public class WSDLToJavaXMLFormatTest
    extends ProcessorTestBase {

    public void setUp() throws Exception {
        super.setUp();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
    }
    // temporarily comment, waiting for tool's validator fix
    public void testXMLFormatRootNodeValidationFail() throws Exception {
        WSDLToJavaProcessor processor = new WSDLToJavaProcessor();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/xml_format_fail.wsdl"));
        env.put(ToolConstants.CFG_VALIDATE_WSDL, ToolConstants.CFG_VALIDATE_WSDL);
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for xml format binding!");
        } catch (ToolException e) {
            if (e.toString().indexOf("missing xml format body element") == -1) {
                throw e;
            }
        }
    }

    public void testXMLFormatRootNodeValidationPass() throws Exception {
        WSDLToJavaProcessor processor = new WSDLToJavaProcessor();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/xml_format_pass.wsdl"));
        processor.setEnvironment(env);
        processor.process();
    }

    private String getLocation(String wsdlFile) throws URISyntaxException {
        return WSDLToJavaXMLFormatTest.class.getResource(wsdlFile).toURI().getPath();
    }
    
    
    
}

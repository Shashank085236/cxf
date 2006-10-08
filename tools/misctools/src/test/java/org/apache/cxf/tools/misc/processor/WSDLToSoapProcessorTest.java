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

package org.apache.cxf.tools.misc.processor;

import java.io.File;
import java.util.Iterator;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.extensions.soap.SoapBinding;
import org.apache.cxf.tools.common.extensions.soap.SoapBody;
import org.apache.cxf.tools.common.extensions.soap.SoapOperation;
import org.apache.cxf.tools.misc.WSDLToSoap;
import org.apache.cxf.tools.util.SOAPBindingUtil;


public class WSDLToSoapProcessorTest extends ProcessorTestBase {

    public void setUp() throws Exception {
        super.setUp();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
    }

    public void testDocLitWithFault() throws Exception {
        String[] args = new String[] {"-i", "Greeter", "-d", output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/hello_world_doc_lit.wsdl")};
        WSDLToSoap.main(args);

        File outputFile = new File(output, "hello_world_doc_lit-soapbinding.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Binding binding = processor.getWSDLDefinition().getBinding(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "Greeter_Binding"));
            if (binding == null) {
                fail("Element wsdl:binding Greeter_Binding Missed!");
            }
            Iterator it = binding.getExtensibilityElements().iterator();
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                SoapBinding soapBinding = SOAPBindingUtil.getSoapBinding(obj);
                if (soapBinding != null 
                    && soapBinding.getStyle().equalsIgnoreCase("document")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element soap:binding Missed!");
            }
            BindingOperation bo = binding.getBindingOperation("pingMe", null, null);
            if (bo == null) {
                fail("Element <wsdl:operation name=\"pingMe\"> Missed!");
            }
            it = bo.getExtensibilityElements().iterator();
            found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                SoapOperation soapOperation = SOAPBindingUtil.getSoapOperation(obj);
                if (soapOperation != null
                    && soapOperation.getStyle().equalsIgnoreCase("document")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element soap:operation Missed!");
            }
            BindingFault fault = bo.getBindingFault("pingMeFault");
            if (fault == null) {
                fail("Element <wsdl:fault name=\"pingMeFault\"> Missed!");
            }
            it = fault.getExtensibilityElements().iterator();
            found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                if (SOAPBindingUtil.isSOAPFault(obj)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element soap:fault Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }

    public void testRpcLitWithoutFault() throws Exception {
        String[] args = new String[] {"-i", "GreeterRPCLit", "-n",
                                      "http://apache.org/hello_world_rpclit_test", "-b",
                                      "Greeter_SOAPBinding_NewBinding", "-style", "rpc", "-use", "literal",
                                      "-d", output.getCanonicalPath(), "-o",
                                      "hello_world_rpc_lit_newbinding.wsdl",
                                      getLocation("/misctools_wsdl/hello_world_rpc_lit.wsdl")};
        WSDLToSoap.main(args);

        File outputFile = new File(output, "hello_world_rpc_lit_newbinding.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());

        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Binding binding = processor.getWSDLDefinition()
                .getBinding(
                            new QName(processor.getWSDLDefinition().getTargetNamespace(),
                                      "Greeter_SOAPBinding_NewBinding"));
            if (binding == null) {
                fail("Element wsdl:binding Greeter_SOAPBinding_NewBinding Missed!");
            }
            Iterator it = binding.getExtensibilityElements().iterator();
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                SoapBinding soapBinding = SOAPBindingUtil.getSoapBinding(obj);
                if (soapBinding != null && soapBinding.getStyle().equalsIgnoreCase("rpc")) {
                    found = true;
                    break;

                }
            }
            if (!found) {
                fail("Element soap:binding style=rpc Missed!");
            }
            BindingOperation bo = binding.getBindingOperation("sendReceiveData", null, null);
            if (bo == null) {
                fail("Element <wsdl:operation name=\"sendReceiveData\"> Missed!");
            }
            it = bo.getExtensibilityElements().iterator();
            found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                SoapOperation soapOperation = SOAPBindingUtil.getSoapOperation(obj);
                if (soapOperation != null && soapOperation.getStyle().equalsIgnoreCase("rpc")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element soap:operation style=rpc Missed!");
            }
            BindingInput bi = bo.getBindingInput();
            it = bi.getExtensibilityElements().iterator();
            found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                SoapBody soapBody = SOAPBindingUtil.getSoapBody(obj);
                if (soapBody != null && soapBody.getUse().equalsIgnoreCase("literal")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element soap:body use=literal Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }

    public void testPartValidation() throws Exception {
        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        env.put(ToolConstants.CFG_PORTTYPE, "Greeter");
        env.put(ToolConstants.CFG_BINDING, "Greeter_Binding");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_STYLE, "rpc");
        env.put(ToolConstants.CFG_USE, "literal");
        env.put(ToolConstants.CFG_NAMESPACE, "http://com.iona.hello_world/rpc");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world.wsdl"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for Part Reference illegal!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("does not use type reference not confirm to RPC style") >= 0)) {
                fail("Do not catch tool exception for Part Reference illegal, "
                     + "catch other unexpected exception: " + e.getMessage());
            }
        }
    }

    public void testBindingExist() throws Exception {
        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world_rpc_lit.wsdl"));
        env.put(ToolConstants.CFG_PORTTYPE, new String("GreeterRPCLit"));
        env.put(ToolConstants.CFG_BINDING, new String("Greeter_SOAPBinding_RPCLit"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for binding exist!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("Input binding already exist in imported contract") >= 0)) {
                fail("Do not catch tool exception for binding exist, " + "catch other unexpected exception!");
            }
        }
    }

    public void testPortTypeNotExist() throws Exception {
        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world_rpc_lit.wsdl"));
        env.put(ToolConstants.CFG_PORTTYPE, new String("NonExistPortType"));
        env.put(ToolConstants.CFG_BINDING, new String("NewBinding_RPCLit"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for  binding not exist!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("Input port type does not exist in imported contract") >= 0)) {
                fail("Do not catch tool exception for port type not exist, "
                     + "catch other unexpected exception!");
            }
        }
    }

    public void testNameSpaceMissing() throws Exception {
        WSDLToSoapProcessor processor = new WSDLToSoapProcessor();
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world_rpc_lit.wsdl"));
        env.put(ToolConstants.CFG_PORTTYPE, new String("GreeterRPCLit"));
        env.put(ToolConstants.CFG_BINDING, new String("NewBinding_RPCLit"));
        env.put(ToolConstants.CFG_STYLE, new String("rpc"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for name space missing!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("For rpc style binding, soap binding namespace (-n) must be provided") >= 0)) {
                fail("Do not catch tool exception for binding exist, " + "catch other unexpected exception!");
            }
        }
    }

    private String getLocation(String wsdlFile) {
        return WSDLToSoapProcessorTest.class.getResource(wsdlFile).getFile();
    }

}

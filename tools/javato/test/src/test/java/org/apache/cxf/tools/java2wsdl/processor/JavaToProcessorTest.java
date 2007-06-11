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

package org.apache.cxf.tools.java2wsdl.processor;

import java.io.File;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.WSDLHelper;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.java2wsdl.JavaToWSDL;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.apache.cxf.wsdl.WSDLConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JavaToProcessorTest extends ProcessorTestBase {
    JavaToProcessor processor = new JavaToProcessor();
    String classPath = "";
    private WSDLHelper wsdlHelper = new WSDLHelper();
    @Before
    public void startUp() throws Exception {
        env = new ToolContext();
        classPath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", getClassPath());
    }
    @After
    public void tearDown() {      
        super.tearDown();
        System.setProperty("java.class.path", classPath);
    }
        
    @Test
    public void testGetWSDLVersion() {
        processor.setEnvironment(new ToolContext());
        assertEquals(WSDLConstants.WSDLVersion.WSDL11, processor.getWSDLVersion());
    }

    @Test
    public void testSimpleClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/doc_wrapped_bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.simple.Hello");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "doc_wrapped_bare.wsdl");
        assertTrue("Fail to generate wsdl file: " + wsdlFile.toString(), wsdlFile.exists());

        String tns = "http://simple.fortest.tools.cxf.apache.org/";
        Definition def = wsdlHelper.getDefinition(wsdlFile);
        assertNotNull(def);
        Service wsdlService = def.getService(new QName(tns, "Hello"));
        assertNotNull("Generate WSDL Service Error", wsdlService);
    }

    @Test
    public void testCalculator() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/calculator.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME,
                    "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        processor.setEnvironment(env);
        processor.process();

        String expectedFile = getClass().getResource("expected/calculator.wsdl").getFile();
        assertFileEquals(new File(expectedFile), new File(output, "calculator.wsdl"));

    }

    @Test
    public void testIsSOAP12() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                    "org.apache.cxf.tools.fortest.withannotation.doc.Stock12Impl");
        processor.setEnvironment(env);
        assertTrue(processor.isSOAP12());

        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        assertFalse(processor.isSOAP12());

        env.put(ToolConstants.CFG_SOAP12, "soap12");
        assertTrue(processor.isSOAP12());
    }

    @Test
    // TODO the generated wsdl has two faultDetail elements
    public void testSOAP12() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        env.put(ToolConstants.CFG_SOAP12, "soap12");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello_soap12.wsdl");
        
        processor.setEnvironment(env);
        processor.process();

        String expectedFile = getClass().getResource("expected/hello_soap12.wsdl").getFile();
        assertFileEquals(new File(expectedFile), new File(output, "hello_soap12.wsdl"));
    }
    
    @Test
    public void testDocLitUseClassPathFlag() throws Exception {
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");
        classFile.mkdir();
        
        System.setProperty("java.class.path", getClassPath() + classFile.getCanonicalPath()
                           + File.separatorChar);
        
        env.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_PACKAGENAME, "org.apache.cxf.classpath");
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl/hello_world_doc_lit.wsdl"));
        JAXWSContainer w2jProcessor = new JAXWSContainer(null);
        w2jProcessor.setContext(env);
        w2jProcessor.execute();
        
        
        String tns = "http://apache.org/sepecifiedTns";
        String serviceName = "cxfService";
        String portName = "cxfPort";

        System.setProperty("java.class.path", "");
        
        //      test flag
        String[] args = new String[] {"-o",
                                      "java2wsdl.wsdl",
                                      "-cp",
                                      classFile.getCanonicalPath(),
                                      "-t",
                                      tns,
                                      "-servicename",
                                      serviceName,
                                      "-portname",
                                      portName,
                                      "-soap12",
                                      "-d",
                                      output.getPath(),
                                      "org.apache.cxf.classpath.Greeter"};
        JavaToWSDL.main(args);
        File wsdlFile = new File(output, "java2wsdl.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        Definition def = wsdlHelper.getDefinition(wsdlFile);
        Service wsdlService = def.getService(new QName(tns, serviceName));
        assertNotNull("Generate WSDL Service Error", wsdlService);
        
        Port wsdlPort = wsdlService.getPort(portName);
        assertNotNull("Generate service port error ", wsdlPort);
        
    }

    @Test
    public void testDataBase() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.cxf523.Database");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/db.wsdl");
        
        processor.setEnvironment(env);
        processor.process();

        String expectedFile = getClass().getResource("expected/db.wsdl").getFile();
        assertFileEquals(new File(expectedFile), new File(output, "db.wsdl"));
    }

    @Test
    public void testGetServiceName() throws Exception {
        processor.setEnvironment(env);
        assertNull(processor.getServiceName());

        env.put(ToolConstants.CFG_SERVICENAME, "myservice");
        processor.setEnvironment(env);
        assertEquals("myservice", processor.getServiceName());
    }

    @Test
    public void testSetServiceName() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        env.put(ToolConstants.CFG_SOAP12, "soap12");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_hello_soap12.wsdl");
        env.put(ToolConstants.CFG_SERVICENAME, "MyService");
        
        processor.setEnvironment(env);
        processor.process();

        String expectedFile = getClass().getResource("expected/my_hello_soap12.wsdl").getFile();
        assertFileEquals(new File(expectedFile), new File(output, "my_hello_soap12.wsdl"));
    }
    @Test
    public void testGenWrapperBeanClasses() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_calculator.wsdl");
        
        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }

    @Test
    public void testNoNeedGenWrapperBeanClasses() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.withannotation.doc.Stock");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_stock.wsdl");
        
        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertFalse(requestWrapperClass.exists());
        assertFalse(responseWrapperClass.exists());
    }

    @Test
    public void testSetSourceDir() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_stock.wsdl");
        env.put(ToolConstants.CFG_SOURCEDIR, output.getPath() + "/beans");
        
        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "beans/org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }
    
    
    @Test
    //test for CXF-704 and CXF-705
    public void testHello() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.Hello");
        processor.setEnvironment(env);
        processor.process();
        
        File wsdlFile = new File(output, "hello.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

    }
    
    public void testHelloNoPackage() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello-no-package.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "HelloNoPackage");
        processor.setEnvironment(env);
        processor.process();
        
        File wsdlFile = new File(output, "hello-no-package.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        
        
        String pkgBase = "defaultnamespace";
        File requestWrapperClass = new File(output, pkgBase + "/SayHi.java");
        File responseWrapperClass = new File(output, pkgBase + "/SayHiResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }
    
}

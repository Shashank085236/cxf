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
package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Modifier;

import javax.jws.WebService;
import javax.xml.ws.WebServiceClient;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;

public class CodeGenBugTest extends ProcessorTestBase {

    private JAXWSContainer processor;
    private ClassLoader classLoader;

    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");
        classFile.mkdir();
        System.setProperty("java.class.path", getClassPath() + classFile.getCanonicalPath()
                                              + File.separatorChar);
        classLoader = AnnotationUtil.getClassLoader(Thread.currentThread().getContextClassLoader());
        env.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_IMPL, "impl");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());

        processor = new JAXWSContainer(null);

    }

    
    @After
    public void tearDown() {
        super.tearDown();
        processor = null;
        env = null;
    }

    @Test
    public void testBug305729() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305729/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull("Process message with no part wsdl error", output);
    }

    @Test
    public void testBug305773() throws Exception {

        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_IMPL, ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305773/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.hello_world_soap_http.GreeterImpl");

        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertEquals("Greeter", webServiceAnn.name());
        assertFalse("Impl class should generate portName property value in webService annotation",
                    webServiceAnn.portName().equals(""));
        assertFalse("Impl class should generate serviceName property value in webService annotation",
                    webServiceAnn.serviceName().equals(""));

    }

    @Test
    public void testBug305700() throws Exception {
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305700/addNumbers.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testNamespacePackageMapping1() throws Exception {
        env.addNamespacePackageMap("http://apache.org/hello_world_soap_http/types", "org.apache.types");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File types = new File(apache, "types");
        assertTrue(types.exists());

        File[] files = apache.listFiles();
        assertEquals(2, files.length);
        files = types.listFiles();
        assertEquals(17, files.length);

        Class clz = classLoader.loadClass("org.apache.types.GreetMe");
        assertNotNull(clz);
    }

    @Test
    public void testNamespacePackageMapping2() throws Exception {
        env.addNamespacePackageMap("http://apache.org/hello_world_soap_http", "org.apache");
        env.addNamespacePackageMap("http://apache.org/hello_world_soap_http/types", "org.apache.types");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue("org directory is not found", org.exists());
        File apache = new File(org, "apache");
        assertTrue("apache directory is not found", apache.exists());
        File types = new File(apache, "types");
        assertTrue("types directory is not found", types.exists());

        Class clz = classLoader.loadClass("org.apache.types.GreetMe");
        assertTrue("Generate " + clz.getName() + "error", Modifier.isPublic(clz.getModifiers()));
        clz = classLoader.loadClass("org.apache.Greeter");
    }

    @Test
    public void testNamespacePackageMapping3() throws Exception {
        env.put(ToolConstants.CFG_PACKAGENAME, "org.cxf");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue(org.exists());

        File cxf = new File(org, "cxf");
        File[] files = cxf.listFiles();
        assertEquals(23, files.length);

        Class clz = classLoader.loadClass("org.cxf.Greeter");
        assertTrue("Generate " + clz.getName() + "error", clz.isInterface());
    }
    
    
    

    @Test
    public void testBug305772() throws Exception {
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_ANT, ToolConstants.CFG_ANT);
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        // env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305772/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();
        File file = new File(output.getCanonicalPath(), "build.xml");
        FileInputStream fileinput = new FileInputStream(file);
        BufferedInputStream filebuffer = new BufferedInputStream(fileinput);
        byte[] buffer = new byte[(int)file.length()];
        filebuffer.read(buffer);
        String content = new String(buffer);
        assertTrue("wsdl location should be url style in build.xml", content.indexOf("param1=\"file:") > -1);

    }

    @Test
    public void testExcludeNSWithPackageName() throws Exception {

        String[] args = new String[] {"-d", output.getCanonicalPath(), "-nexclude",
                                      "http://apache.org/test/types=com.iona", "-nexclude",
                                      "http://apache.org/Invoice", "-compile",
                                      "-classdir", output.getCanonicalPath() + "/classes",
                                      getLocation("/wsdl2java_wsdl/hello_world_exclude.wsdl")};
        WSDLToJava.main(args);

        assertNotNull(output);
        File com = new File(output, "com");
        assertFalse("Generated file has been excluded", com.exists());
        File iona = new File(com, "iona");
        assertFalse("Generated file has been excluded", iona.exists());
        
        File implFile = new File(output, "org/apache/hello_world_soap_http/Greeter.java");
        String str = getStringFromFile(implFile);
        assertTrue(str.indexOf("com.iona.BareDocumentResponse") > 0);
        
        File org = new File(output, "org");
        File apache = new File(org, "apache");
        File invoice = new File(apache, "Invoice");
        assertFalse("Generated file has been excluded", invoice.exists());

    }


    @Test
    public void testExcludeNSWithoutPackageName() throws Exception {

        String[] args = new String[] {"-d", output.getCanonicalPath(), "-nexclude",
                                      "http://apache.org/test/types",
                                      getLocation("/wsdl2java_wsdl/hello_world_exclude.wsdl")};
        WSDLToJava.main(args);

        assertNotNull(output);
        File com = new File(output, "test");
        assertFalse("Generated file has been excluded", com.exists());

    }
     
    @Test
    public void testCommandLine() throws Exception {
        String[] args = new String[] {"-compile", "-d", output.getCanonicalPath(), "-classdir",
                                      output.getCanonicalPath() + "/classes", "-p", "org.cxf", "-p",
                                      "http://apache.org/hello_world_soap_http/types=org.apache.types",
                                      "-server", "-impl", getLocation("/wsdl2java_wsdl/hello_world.wsdl")};
        WSDLToJava.main(args);

        Class clz = classLoader.loadClass("org.cxf.Greeter");
        assertTrue("Generate " + clz.getName() + "error", clz.isInterface());
    }

    @Test
    public void testDefaultLoadNSMappingOFF() throws Exception {
        String[] args = new String[] {"-dns", "false", "-d", output.getCanonicalPath(),
                                      getLocation("/wsdl2java_wsdl/basic_callback.wsdl")};

        WSDLToJava.main(args);

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File w3 = new File(org, "w3");
        assertTrue(w3.exists());
        File p2005 = new File(w3, "_2005");
        assertTrue(p2005.exists());
        File p08 = new File(p2005, "_08");
        assertTrue(p08.exists());
        File address = new File(p08, "addressing");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(11, files.length);
    }

    @Test
    public void testDefaultLoadNSMappingON() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(),
                                      getLocation("/wsdl2java_wsdl/basic_callback.wsdl")};

        WSDLToJava.main(args);

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File ws = new File(cxf, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "addressing");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(11, files.length);
    }

    @Test
    public void testBug305924ForNestedBinding() {
        try {
            String[] args = new String[] {"-all", "-compile", "-classdir",
                                          output.getCanonicalPath() + "/classes", "-d",
                                          output.getCanonicalPath(), "-b",
                                          getLocation("/wsdl2java_wsdl/bug305924/binding2.xml"),
                                          getLocation("/wsdl2java_wsdl/bug305924/hello_world.wsdl")};
            WSDLToJava.main(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        try {
            Class clz = classLoader
                .loadClass("org.apache.hello_world_soap_http.types.CreateProcess$MyProcess");
            assertNotNull("Customization binding code should be generated", clz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBug305924ForExternalBinding() {
        try {
            String[] args = new String[] {"-all", "-compile", "-classdir",
                                          output.getCanonicalPath() + "/classes", "-d",
                                          output.getCanonicalPath(), "-b",
                                          getLocation("/wsdl2java_wsdl/bug305924/binding1.xml"),
                                          getLocation("/wsdl2java_wsdl/bug305924/hello_world.wsdl")};
            WSDLToJava.main(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        try {
            Class clz = classLoader
                .loadClass("org.apache.hello_world_soap_http.types.CreateProcess$MyProcess");
            assertNotNull("Customization binding code should be generated", clz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLocatorWithJaxbBinding() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/locator_with_jaxbbinding.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testWsdlNoService() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/helloworld_withnoservice.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testNoServiceImport() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/helloworld_noservice_import.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class cls = classLoader.loadClass("org.apache.hello_world1.Greeter");
        assertNotNull(cls);
        cls = classLoader.loadClass("org.apache.hello_world2.Greeter2");
    }

    @Test
    public void testServiceNS() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLURL,
                getLocation("/wsdl2java_wsdl/bug321/hello_world_different_ns_service.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class clz = classLoader.loadClass("org.apache.hello_world_soap_http.service.SOAPServiceTest1");
        WebServiceClient webServiceClient = AnnotationUtil
            .getPrivClassAnnotation(clz, WebServiceClient.class);
        assertEquals("http://apache.org/hello_world_soap_http/service", webServiceClient.targetNamespace());
        File file = new File(output, "org/apache/hello_world_soap_http/GreeterClient.java");
        FileInputStream fin = new FileInputStream(file);
        byte[] buffer = new byte[30000];
        int index = -1;
        int size = fin.read(buffer);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while (size != -1) {
            bout.write(buffer, 0, size);
            index = bout.toString()
                .indexOf("new QName(\"http://apache.org/hello_world_soap_http/service\"," 
                        + " \"SOAPService_Test1\")");
            if (index > 0) {
                break;
            }
            size = fin.read(buffer);
        }
        assertTrue("Service QName in client is not correct", index > -1);
    }

    @Test
    public void testNoServiceNOPortType() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/no_port_or_service.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.cxf.no_port_or_service.types.TheComplexType");
        assertNotNull(clz);
    }

    // CXF-492
    @Test
    public void testDefatultNsMap() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf492/locator.wsdl"));
        processor.setContext(env);
        processor.execute();
        File org = new File(output, "org");
        assertTrue("org directory is not exist", org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File ws = new File(cxf, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "addressing");
        assertTrue(address.exists());
    }
    
    
    @Test
    public void testDefatultNsMapExclude() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_NEXCLUDE, 
                "http://www.w3.org/2005/08/addressing=org.apache.cxf.ws.addressing");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf492/locator.wsdl"));
        processor.setContext(env);
        processor.execute();
        
        File org = new File(output, "org");
        assertTrue("org directory is not exist", org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File ws = new File(output, "org/apache/cxf/ws/addressing");
        assertFalse(ws.exists());
        
        File orginal = new File(output, "org.w3._2005._08.addressing");
        assertFalse(orginal.exists());
    }
    
    @Test
    public void testHelloWorldExternalBindingFile() throws Exception {
        Server server = new Server(8585);
        ResourceHandler reshandler = new ResourceHandler();
        reshandler.setResourceBase(getLocation("/wsdl2java_wsdl/"));
        server.addHandler(reshandler);
        server.start();
        env.put(ToolConstants.CFG_WSDLURL, "http://localhost:8585/hello_world.wsdl");
        env.put(ToolConstants.CFG_BINDING, "http://localhost:8585/remote-hello_world_binding.xsd");
        processor.setContext(env);
        processor.execute();
        server.stop();

        
        
    }
    
    
    
    

}

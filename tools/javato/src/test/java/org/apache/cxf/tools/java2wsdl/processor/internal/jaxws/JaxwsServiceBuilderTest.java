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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.io.File;

import org.apache.cxf.BusFactory;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.fortest.classnoanno.docbare.Stock;
import org.apache.cxf.tools.fortest.withannotation.doc.Hello;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.WSDL11Generator;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JaxwsServiceBuilderTest extends ProcessorTestBase {
    JaxwsServiceBuilder builder = new JaxwsServiceBuilder();
    WSDL11Generator generator = new WSDL11Generator();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        builder.setBus(BusFactory.getDefaultBus());
    }

    @Test
    public void testGetOutputFile() {
        builder.setServiceClass(Stock.class);
        assertNull(builder.getOutputFile());

        builder.setServiceClass(Hello.class);
        assertNotNull(builder.getOutputFile());
        File expected = new File("file:///c:/tmp.wsdl");
        assertTrue(expected.equals(builder.getOutputFile()));
    }

    @Test
    @Ignore
    public void testWrapped() {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.Hello.class);
        ServiceInfo service = builder.build();
        generator.setServiceModel(service);
        File output = getOutputFile("hello_wrapped.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());
    }

    //FIXME: CXF-519
    @Test
    @Ignore
    public void testAsync() throws Exception {
        builder.setServiceClass(org.apache.hello_world_async_soap_http.GreeterAsync.class);
        ServiceInfo service = builder.build();
        generator.setServiceModel(service);
        File output = getOutputFile("hello_async.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        String expectedFile = this.getClass().getResource("expected/expected_hello_world_async.wsdl")
            .getFile();
        assertFileEquals(expectedFile, output.getAbsolutePath());
    }

    @Test
    public void testRPCLit() throws Exception {
        builder.setServiceClass(GreeterRPCLit.class);
        builder.setAddress("http://localhost");
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_lit.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

        //String expectedFile = this.getClass().getResource("resources/expected_rpc_lit.wsdl").getFile();
        //compareTextFile(expectedFile, output.getAbsolutePath());

    }

    // TODO:
    @Test
    public void testDocWrapparBare() throws Exception {

        builder.setServiceClass(org.apache.hello_world_doc_wrapped_bare.Greeter.class);
        builder.setAddress("http://localhost");
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("doc_wrapped_bare.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    //FIXME: CXF-519, CXF-533
    @Test
    @Ignore
    public void testDocLit() throws Exception {
        builder.setServiceClass(org.apache.hello_world_doc_lit.Greeter.class);
        ServiceInfo service = builder.build();
        generator.setServiceModel(service);
        File output = getOutputFile("hello_doc_lit.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        String expectedFile = this.getClass().getResource("expected/expected_hello_world_doc_lit.wsdl")
            .getFile();
        assertFileEquals(expectedFile, output.getAbsolutePath());
    }

    // TODO:
    @Test
    @Ignore
    public void testSOAP12() throws Exception {
        builder.setServiceClass(org.apache.hello_world_soap12_http.Greeter.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("soap12.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testRPCWithoutParentBindingAnnotation() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.rpc.Hello.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_lit_service_no_anno.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testDocWrappedWithoutWrapperClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("doc_lit_wrapped_no_anno.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testSOAPBindingRPCOnMethod() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.rpc.HelloWrongAnnotation.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_on_method.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testDocWrappedWithLocalName() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.Stock.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("doc_lit_wrapped_localName.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testDocWrappedNoWebParam() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.HelloWithNoWebParam.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("doc_lit_wrapped_webparam.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    // TODO:
    @Test
    @Ignore
    public void testSoapHeader() throws Exception {

        builder.setServiceClass(org.apache.samples.headers.HeaderTester.class);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("soap_header.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

    }

    // TODO:
    @Test
    @Ignore
    public void testCXF188() throws Exception {
        Class clz = AnnotationUtil.loadClass("org.apache.cxf.tools.fortest.cxf188.Demo", getClass()
            .getClassLoader());
        builder.setServiceClass(clz);
        ServiceInfo service = builder.build();

        generator.setServiceModel(service);
        File file = getOutputFile("cxf188.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    private File getOutputFile(String fileName) {
        return new File(output, fileName);
    }

}

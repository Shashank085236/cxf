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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Holder;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.wsdl2java.WSDLToJava;


public class WSDLToJavaExSoapHeaderTest
    extends ProcessorTestBase {

    private URLClassLoader classLoader;

    public void setUp() throws Exception {
        super.setUp();
        File classFile = new java.io.File(output.getCanonicalPath());
        System.setProperty("java.class.path", getClassPath() + classFile.getCanonicalPath()
                                              + File.separatorChar);
        classLoader = AnnotationUtil.getClassLoader(Thread.currentThread().getContextClassLoader());
    }

    public void testSoapBindingDiffMessage() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), 
                                      "-exsh", "true", "-compile", 
                                      getLocation("/wsdl2java_wsdl/soapheader_test.wsdl")};
        WSDLToJava.main(args);

        Class<?> clz = classLoader.loadClass("org.apache.header_test.TestHeader");        
        Method method = clz.getMethod("testHeader4", new Class[] {java.lang.String.class,
                                                                  Holder.class});
        if (method == null) {
            fail("Missing method testHeader4 of TestHeader class!");
        }
        WebParam webParamAnno = AnnotationUtil.getWebParam(method, "testHeaderMessage");
        if (webParamAnno == null) {
            fail("Missing 'inoutHeader' WebParam Annotation of method testHeader4!");
        }
        assertEquals("INOUT", webParamAnno.mode().name());
        assertEquals(true, webParamAnno.header());
        assertEquals("inoutHeader", webParamAnno.partName());
    }

    public void testSoapHeaderBinding() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), "-compile",
                                      getLocation("/wsdl2java_wsdl/soapheader_test.wsdl")};
        WSDLToJava.main(args);

        Class<?> clz = classLoader.loadClass("org.apache.header_test.TestHeader");
        Class<?> paramClz = classLoader.loadClass("org.apache.header_test.types.TestHeader5");
        assertEquals(5, clz.getMethods().length);
        
        Method method = clz.getMethod("testHeader5", new Class[] {paramClz});
        if (method == null) {
            fail("Missing method testHeader5 of TestHeader class!");
        }

        SOAPBinding soapBindingAnno = AnnotationUtil.getPrivMethodAnnotation(method, SOAPBinding.class);
        assertEquals("BARE", soapBindingAnno.parameterStyle().name());

        WebResult webResultAnno = AnnotationUtil.getWebResult(method);
        if (webResultAnno == null) {
            fail("Missing 'in' WebParam Annotation of method testHeader5!");
        }        
        assertEquals(true, webResultAnno.header());
        assertEquals("outHeader", webResultAnno.partName());
        assertEquals("testHeader5", webResultAnno.name());
    }

    private String getLocation(String wsdlFile) {
        return WSDLToJavaExSoapHeaderTest.class.getResource(wsdlFile).getFile();
    }
}

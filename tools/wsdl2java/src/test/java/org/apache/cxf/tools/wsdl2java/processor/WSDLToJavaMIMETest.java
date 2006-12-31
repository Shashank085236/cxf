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
import java.io.FileReader;
import java.net.URISyntaxException;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.wsdl2java.WSDLToJava;

public class WSDLToJavaMIMETest
    extends ProcessorTestBase {

    public void setUp() throws Exception {
        super.setUp();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
    }

    public void testDummy() {
        
    }
    
    public void testMimeInOut() throws Exception {
        String[] args = new String[] {"-d", 
                                      output.getAbsolutePath(),
                                      getLocation("/wsdl2java_wsdl/mime-inout.wsdl")};
        WSDLToJava.main(args);
        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File helloworldsoaphttp = new File(apache, "cxf/swa");
        assertTrue(helloworldsoaphttp.exists());
        File outputFile = new File(helloworldsoaphttp, "SwAServiceInterface.java");
        assertTrue("PortType file is not generated", outputFile.exists());
        FileReader fileReader = new FileReader(outputFile);
        char[] chars = new char[100];
        int size = 0;
        StringBuffer sb = new StringBuffer();
        while (size < outputFile.length()) {
            int readLen = fileReader.read(chars);
            sb.append(chars, 0, readLen);
            size = size + readLen;
        }
        String serviceString = new String(sb);
        int position1 = serviceString.indexOf("public void echoData(");
        int position2 = serviceString.indexOf("Holder<javax.activation.DataHandler> data");
        assertTrue(position1 > 0 && position2 > 0);
    }
    
    public void testHelloWorld() throws Exception {
        String[] args = new String[] {"-d", 
                                      output.getAbsolutePath(),
                                      getLocation("/wsdl2java_wsdl/binary_attachment.wsdl")};
        WSDLToJava.main(args);
        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File helloworldsoaphttp = new File(apache, "binary_attachment");
        assertTrue(helloworldsoaphttp.exists());
        File outputFile = new File(helloworldsoaphttp, "BinaryAttachmentPortType.java");
        assertTrue("PortType file is not generated", outputFile.exists());
        FileReader fileReader = new FileReader(outputFile);
        char[] chars = new char[100];
        int size = 0;
        StringBuffer sb = new StringBuffer();
        while (size < outputFile.length()) {
            int readLen = fileReader.read(chars);
            sb.append(chars, 0, readLen);
            size = size + readLen;
        }
        String serviceString = new String(sb);
        int position1 = serviceString.indexOf("public byte[] echoImage(");
        int position2 = serviceString.indexOf("byte[] para0");
        int position3 = serviceString.indexOf("java.awt.Image para1,");
        int position4 = serviceString
            .indexOf("javax.xml.ws.Holder<javax.activation.DataHandler> retn1");
        assertTrue(position1 > 0 && position2 > 0 && position3 > 0 && position4 > 0);
        assertTrue(position1 < position2 && position2 < position3 && position3 < position4);
    }

    public void xtestWithExternalBindingSwitch() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), "-b",
                                      getLocation("/wsdl2java_wsdl/mime_binding.wsdl"),
                                      getLocation("/wsdl2java_wsdl/binary_attachment.wsdl")};
        WSDLToJava.main(args);
        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File helloworldsoaphttp = new File(apache, "binary_attachment");
        assertTrue(helloworldsoaphttp.exists());
        File outputFile = new File(helloworldsoaphttp, "BinaryAttachmentPortType.java");
        assertTrue("PortType file is not generated", outputFile.exists());
        FileReader fileReader = new FileReader(outputFile);
        char[] chars = new char[100];
        int size = 0;
        StringBuffer sb = new StringBuffer();
        while (size < outputFile.length()) {
            int readLen = fileReader.read(chars);
            sb.append(chars, 0, readLen);
            size = size + readLen;
        }
        String serviceString = new String(sb);
        int position1 = serviceString.indexOf("public java.awt.Image echoImage(");
        int position2 = serviceString.indexOf("java.awt.Image para0");
        int position3 = serviceString.indexOf("public void echoMultipleImage(");
        int position4 = serviceString.indexOf("java.awt.Image para1,");
        int position5 = serviceString
            .indexOf("javax.xml.ws.Holder<javax.activation.DataHandler> retn1");
//        System.out.println("position1=" + position1 + "; position2=" + position2 + "; position3="
//                           + position3 + "; position4=" + position4 + "; position5=" + position5);
        assertTrue(position1 > 0 && position2 > 0 && position3 > 0 && position4 > 0
                   && position5 > 0);
        assertTrue(position1 < position2 && position2 < position3 && position3 < position4
                   && position4 < position5);
    }

    public void xtestMIMEValidationUniqueRoot() throws Exception {
        WSDLToJavaProcessor processor = new WSDLToJavaProcessor();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl/mime_fail_unique_root.wsdl"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for MIME unique root validation failure!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("There's more than one soap body mime part in its binding input") >= 0)) {
                fail("Do not catch expected tool exception for MIME unique root validation failure,"
                     + " catch other unexpected exception!");
            }
        }
    }

    public void xtestMIMEValidationDiffParts() throws Exception {
        WSDLToJavaProcessor processor = new WSDLToJavaProcessor();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl/mime_fail_diff_parts.wsdl"));
        processor.setEnvironment(env);
        try {
            processor.process();
            fail("Do not catch expected tool exception for MIME different parts validation failure!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("Part attribute value for meme:content elements are different") >= 0)) {
                fail("Do not catch expected tool exception for MIME different parts validation failure,"
                     + " catch other unexpected exception!");
            }
        }
    }

    private String getLocation(String wsdlFile) throws URISyntaxException {
        return WSDLToJavaMIMETest.class.getResource(wsdlFile).getFile();
    }
    
}

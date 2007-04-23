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

import org.apache.cxf.tools.common.ToolTestBase;

public class JavaToWSDLTest extends ToolTestBase {

    public void tearDown() {
        super.tearDown();
    }

    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());
    }

    public void testHelpOutput() {
        String[] args = new String[] {"-help"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());
    }

    public void testNormalArgs() throws Exception {
        System.err.println(getLocation("test.wsdl"));
        String[] args = new String[] {"-o",
                                      getLocation("normal.wsdl"),
                                      "org.apache.hello_world_soap_http.Greeter"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());
    }

    public void testBadUsage() {
        String[] args = new String[] {"-ttt", "a.ww"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());

    }

    public void testValidArgs() {
        String[] args = new String[] {"a.ww"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());

    }

    public void testNoOutPutFile() throws Exception {
        String[] args = new String[] {"-o",
                                      getLocation("nooutput.wsdl"),
                                      "org.apache.hello_world_soap_http.Greeter"};
        JavaToWSDL.main(args);
        assertNotNull(getStdOut());
    }
    
    public void testNoArg() {
        String[] args = new String[] {};
        JavaToWSDL.main(args);
        assertEquals(-1, getStdOut().indexOf("Caused by:"));
    }

}

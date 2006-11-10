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

package org.apache.cxf.wsdl;

import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.cxf.abc.test.AnotherPolicyType;
import org.apache.cxf.abc.test.TestPolicyType;

public class JAXBExtensionHelperTest extends TestCase {

    private WSDLFactory wsdlFactory;

    private WSDLReader wsdlReader;

    private Definition wsdlDefinition;

    private ExtensionRegistry registry;

    public void setUp() throws Exception {

        wsdlFactory = WSDLFactory.newInstance();
        wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        registry = wsdlReader.getExtensionRegistry();
        if (registry == null) {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
        }
    }

    public void tearDown() {

    }

    public void testAddTestExtension() throws Exception {

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
                        "org.apache.cxf.abc.test.TestPolicyType", Thread.currentThread()
                                        .getContextClassLoader());

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
                        "org.apache.cxf.abc.test.AnotherPolicyType", Thread.currentThread()
                                        .getContextClassLoader());

        String file = this.getClass().getResource("/wsdl/test_ext.wsdl").getFile();

        wsdlReader.setExtensionRegistry(registry);

        wsdlDefinition = wsdlReader.readWSDL(file);
        Service s = wsdlDefinition.getService(new QName("http://cxf.apache.org/test/hello_world",
                        "HelloWorldService"));
        Port p = s.getPort("HelloWorldPort");
        List extPortList = p.getExtensibilityElements();

        TestPolicyType tp = null;
        AnotherPolicyType ap = null;
        for (Object ext : extPortList) {
            if (ext instanceof TestPolicyType) {
                tp = (TestPolicyType) ext;
            }
            if (ext instanceof AnotherPolicyType) {
                ap = (AnotherPolicyType) ext;
            }
        }
        assertNotNull("Could not find extension element TestPolicyType", tp);
        assertNotNull("Could not find extension element AnotherPolicyType", ap);

        assertEquals("Unexpected value for TestPolicyType intAttr", 30, tp.getIntAttr());
        assertEquals("Unexpected value for TestPolicyType stringAttr", "hello", tp.getStringAttr());
        assertTrue("Unexpected value for AnotherPolicyType floatAttr",
            Math.abs(0.1F - ap.getFloatAttr()) < 0.5E-5);
    }

}

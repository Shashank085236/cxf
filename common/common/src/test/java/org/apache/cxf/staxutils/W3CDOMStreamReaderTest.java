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

package org.apache.cxf.staxutils;


import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import com.sun.xml.messaging.saaj.soap.ver1_1.Message1_1Impl;
import com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl;
import com.sun.xml.messaging.saaj.soap.ver1_1.SOAPPart1_1Impl;

import org.apache.cxf.helpers.XMLUtils;

import org.junit.Assert;
import org.junit.Test;


public class W3CDOMStreamReaderTest extends Assert {
    
    private static final String RESULT = 
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<SOAP-ENV:Header/><SOAP-ENV:Body/>"
        + "<Test xmlns=\"http://example.org/types\">"
        + "<argument>foobar</argument></Test></SOAP-ENV:Envelope>";

    @Test
    public void testReader() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(
                "<Test xmlns=\"http://example.org/types\"><argument>foobar</argument></Test>".getBytes());
        DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

        SOAPMessageFactory1_1Impl factory = new SOAPMessageFactory1_1Impl();
        Message1_1Impl msg = (Message1_1Impl)factory.createMessage();
        SOAPPart1_1Impl part = new SOAPPart1_1Impl(msg);

        Document doc = docBuilder.parse(is);

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter(part.getEnvelope());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(doc));


        StaxUtils.copy(reader, writer);
        assertEquals(RESULT, XMLUtils.toString(writer.getDocument()));

    }

}
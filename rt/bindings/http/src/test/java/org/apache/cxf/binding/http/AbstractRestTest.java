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
package org.apache.cxf.binding.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.test.AbstractCXFTest;

public abstract class AbstractRestTest extends AbstractCXFTest {

    protected Bus createBus() throws BusException {
        return new SpringBusFactory().createBus();
    }

    protected Document get(String urlStr) throws MalformedURLException, IOException, SAXException,
        ParserConfigurationException {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection)url.openConnection();

        InputStream is = c.getInputStream();
        return DOMUtils.readXml(is);
    }

    protected Document post(String urlStr, String message) throws MalformedURLException, IOException,
        SAXException, ParserConfigurationException {
        return doMethod(urlStr, message, "POST");
    }

    protected Document put(String urlStr, String message) throws MalformedURLException, IOException,
        SAXException, ParserConfigurationException {
        return doMethod(urlStr, message, "PUT");
    }

    protected Document doMethod(String urlStr, String message, String method) throws MalformedURLException,
        IOException, SAXException, ParserConfigurationException {

        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod(method);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/xml");

        OutputStream out = c.getOutputStream();
        InputStream msgIs = getResourceAsStream(message);
        assertNotNull(msgIs);

        copy(msgIs, out);

        InputStream is = c.getInputStream();
        return DOMUtils.readXml(is);
    }
}

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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerSpringBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(BookServerSpring.class));
    }
    
    @Test
    public void testGetBook123() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/books/123"; 
        URL url = new URL(endpointAddress);
        InputStream in = url.openStream();
        assertNotNull(in);           

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123.txt");

        //System.out.println("---" + getStringFromInputStream(in));
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in)); 
    }

    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        //System.out.println(bos.getOut().toString());        
        return bos.getOut().toString();        
    }

}

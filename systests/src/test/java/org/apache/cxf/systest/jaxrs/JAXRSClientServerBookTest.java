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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer.class, true));
    }
    
    @Test
    public void testWebApplicationException() throws Exception {
        getAndCompare("http://localhost:9080/bookstore/webappexception",
                      "This is a WebApplicationException",
                      "application/xml", 500);
    }
    
    @Test
    public void testAcceptTypeMismatch() throws Exception {
        // TODO : more specific message is needed
        String msg = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\"><ns1:faultstring"
            + " xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">.No operation matching request path "
            + "/bookstore/booknames/123/ is found, ContentType : */*, Accept : foo/bar.</ns1:faultstring>"
            + "</ns1:XMLFault>";
        
        getAndCompare("http://localhost:9080/bookstore/booknames/123",
                      msg,
                      "foo/bar", 500);
    }
    
    @Test
    public void testNoMessageWriterFound() throws Exception {
        // TODO : more specific message is needed
        String msg1 = ".No message body writer found for response class : GregorianCalendar.";
        String msg2 = ".No message body writer found for response class : Calendar.";
        
        getAndCompareStrings("http://localhost:9080/bookstore/timetable",
                      msg1, msg2,
                      "*/*", 406);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testNoMessageReaderFound() throws Exception {
//      TODO : more specific message is needed
        String msg = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\"><ns1:faultstring"
            + " xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">"
            + "java.lang.RuntimeException: No message body reader found for target class long[], "
            + "content type : application/octet-stream"
            + "</ns1:faultstring>"
            + "</ns1:XMLFault>";
        
        String endpointAddress =
            "http://localhost:9080/bookstore/binarybooks";

        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", "application/octet-stream");
        post.setRequestHeader("Accept", "text/xml");
        post.setRequestBody("Bar");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(500, result);
            assertEquals(msg, post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    @Test
    public void testConsumeTypeMismatch() throws Exception {
        // TODO : more specific message is needed
        String msg = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\"><ns1:faultstring"
            + " xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">.No operation matching request path "
            + "/bookstore/books/ is found, ContentType : application/bar, Accept : text/xml."
            + "</ns1:faultstring></ns1:XMLFault>";
        
        String endpointAddress =
            "http://localhost:9080/bookstore/books";

        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", "application/bar");
        post.setRequestHeader("Accept", "text/xml");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(500, result);
            assertEquals(msg, post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    @Test
    public void testGetBook123() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/query?bookId=123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/defaultquery",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/xml,application/json", 200);
    }
    
    @Test
    public void testGetBookElement() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/element",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookAdapter() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/adapter",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBook123FromSub() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/bookstore/interface/subresource",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/xml,application/json", 200);
    }
    
    @Test
    public void testGetChapter() throws Exception {
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/booksubresource/123/chapters/1",
                               "resources/expected_get_chapter1.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetChapterChapter() throws Exception {
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/booksubresource/123/chapters/sub/1/recurse",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBook123ReturnString() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/bookstore/booknames/123",
                               "resources/expected_get_book123_returnstring.txt",
                               "text/plain", 200);
    }
    
    @Test
    public void testGetBookNotFound() throws Exception {
        
        getAndCompareAsStrings("http://localhost:9080/bookstore/books/126",
                               "resources/expected_get_book_notfound.txt",
                               "application/xml", 500);
    }
    
    @Test
    public void testAddBook() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/books";

        File input = new File(getClass().getResource("resources/add_book.txt").toURI());         
        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", "application/xml");
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
            
            assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }               
    }  
    
    @Test
    public void testUpdateBook() throws Exception {
        String endpointAddress = "http://localhost:9080/bookstore/books";

        File input = new File(getClass().getResource("resources/update_book.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        // Verify result
        endpointAddress = "http://localhost:9080/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        InputStream in = connect.getInputStream();
        assertNotNull(in);

        InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));

        // Roll back changes:
        File input1 = new File(getClass().getResource("resources/expected_get_book123.txt").toURI());
        PutMethod put1 = new PutMethod(endpointAddress);
        RequestEntity entity1 = new FileRequestEntity(input1, "text/xml; charset=ISO-8859-1");
        put1.setRequestEntity(entity1);
        HttpClient httpclient1 = new HttpClient();

        try {
            int result = httpclient1.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put1.releaseConnection();
        }
    }  
    
    @Test
    public void testUpdateBookWithDom() throws Exception {
        String endpointAddress = "http://localhost:9080/bookstore/bookswithdom";

        File input = new File(getClass().getResource("resources/update_book.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
            System.out.println(put.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }
        
        InputStream expected = getClass().getResourceAsStream("resources/update_book.txt");

        assertTrue(put.getResponseBodyAsString().indexOf(getStringFromInputStream(expected)) >= 0);
    }
    
    @Test
    public void testUpdateBookWithJSON() throws Exception {
        String endpointAddress = "http://localhost:9080/bookstore/bookswithjson";

        File input = new File(getClass().getResource("resources/update_book_json.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "application/json; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        // Verify result
        endpointAddress = "http://localhost:9080/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        InputStream in = connection.getInputStream();
        assertNotNull(in);

        InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));

        // Roll back changes:
        File input1 = new File(getClass().getResource("resources/expected_get_book123.txt").toURI());
        PutMethod put1 = new PutMethod(endpointAddress);
        RequestEntity entity1 = new FileRequestEntity(input1, "text/xml; charset=ISO-8859-1");
        put1.setRequestEntity(entity1);
        HttpClient httpclient1 = new HttpClient();

        try {
            int result = httpclient1.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put1.releaseConnection();
        }
    } 
    
    @Test
    public void testUpdateBookFailed() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/books";

        File input = new File(getClass().getResource("resources/update_book_not_exist.txt").toURI());         
        PutMethod post = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(304, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }               
    } 
        
    @Test
    public void testGetCDs() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cds"; 
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        InputStream in = connect.getInputStream();
        assertNotNull(in);           

        InputStream expected124 = getClass().getResourceAsStream("resources/expected_get_cds124.txt");
        String result = getStringFromInputStream(in);
        System.out.println("---" + result);
        assertTrue(result.indexOf(getStringFromInputStream(expected124)) >= 0);
    }
    
    @Test
    public void testGetCDJSON() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cd/123"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/json");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected = getClass().getResourceAsStream("resources/expected_get_cdjson.txt");
            
            assertEquals(getStringFromInputStream(expected), get.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testGetPlainLong() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/booksplain"; 

        PostMethod post = new PostMethod(endpointAddress);
        post.addRequestHeader("Content-Type" , "text/plain");
        post.addRequestHeader("Accept" , "text/plain");
        post.setRequestBody("12345");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            assertEquals(post.getResponseBodyAsString(), "12345");
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }  
    }
    
    @Test
    public void testGetCDsJSON() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cds"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/json");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected123 = getClass().getResourceAsStream("resources/expected_get_cdsjson123.txt");
            InputStream expected124 = getClass().getResourceAsStream("resources/expected_get_cdsjson124.txt");
            
            assertTrue(get.getResponseBodyAsString().indexOf(getStringFromInputStream(expected123)) >= 0);
            assertTrue(get.getResponseBodyAsString().indexOf(getStringFromInputStream(expected124)) >= 0);

        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }  
    
    @Test
    public void testGetCDXML() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cd/123"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/xml");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected = getClass().getResourceAsStream("resources/expected_get_cd.txt");
            
            assertEquals(getStringFromInputStream(expected), get.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }
    
    
    @Test
    public void testGetCDWithMultiContentTypesXML() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cdwithmultitypes/123"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/xml");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected = getClass().getResourceAsStream("resources/expected_get_cd.txt");
            
            assertEquals(getStringFromInputStream(expected), get.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }
    
    @Test
    public void testGetCDWithMultiContentTypesJSON() throws Exception {
        String endpointAddress =
            "http://localhost:9080/bookstore/cdwithmultitypes/123"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/json");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected = getClass().getResourceAsStream("resources/expected_get_cdjson.txt");
            
            assertEquals(getStringFromInputStream(expected), get.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }
    
    private void getAndCompareAsStrings(String address, 
                                        String resourcePath,
                                        String acceptType,
                                        int status) throws Exception {
        String expected = getStringFromInputStream(
                              getClass().getResourceAsStream(resourcePath));
        getAndCompare(address,
                      expected,
                      acceptType,
                      status);
    }
    
    private void getAndCompare(String address, 
                               String expectedValue,
                               String acceptType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        get.setRequestHeader("Accept-Language", "en,da;q=0.8");
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String jsonContent = getStringFromInputStream(get.getResponseBodyAsStream());
            assertEquals("Expected value is wrong", 
                         expectedValue, jsonContent);
        } finally {
            get.releaseConnection();
        }
    }
    
    private void getAndCompareStrings(String address, 
                               String expectedValue1,
                               String expectedValue2,
                               String acceptType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String jsonContent = getStringFromInputStream(get.getResponseBodyAsStream());
            assertTrue("Expected value is wrong", 
                       expectedValue1.equals(jsonContent) || expectedValue2.equals(jsonContent));
        } finally {
            get.releaseConnection();
        }
    }
    
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

}

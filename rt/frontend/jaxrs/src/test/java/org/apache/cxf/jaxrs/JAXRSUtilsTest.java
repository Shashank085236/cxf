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
package org.apache.cxf.jaxrs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXRSUtilsTest extends Assert {
    
    public class Customer {
        @ProduceMime("text/xml")
        @ConsumeMime("text/xml")
        public void test() {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void getItAsXML() {
            // complete
        }
        @ProduceMime("text/plain")   
        public void getItPlain() {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void testQuery(@QueryParam("query") String queryString, @QueryParam("query") int queryInt) {
            // complete
        }
        
        @ProduceMime("text/xml")   
        public void testMultipleQuery(@QueryParam("query") 
                                      String queryString, @QueryParam("query2") String queryString2) {
            // complete
        }
    };
    
    @Before
    public void setUp() {
    }

    @Test
    public void testFindTargetResourceClass() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        Map<String, String> values = new HashMap<String, String>(); 
        String contentTypes = "*/*";
        String acceptContentTypes = "*/*";

        //If acceptContentTypes does not specify a specific Mime type, the  
        //method is declared with a most specific ProduceMime type is selected.
        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123/",
                                                                       "GET", values, contentTypes,
                                                                       acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethod().getName());
        
        //test
        acceptContentTypes = "application/json";
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123/",
                                                                       "GET", values, contentTypes,
                                                                       acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("getBookJSON", ori.getMethod().getName());
        
        //test 
        acceptContentTypes = "application/xml";
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123/",
                                                                       "GET", values, contentTypes,
                                                                       acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethod().getName());
        
        //test find POST
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                                                                       "POST", values, contentTypes,
                                                                       acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethod().getName());
        
        //test find PUT
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                                                                       "PUT", values, contentTypes,
                                                                       acceptContentTypes);  
        assertEquals("updateBook", ori.getMethod().getName());
        
        //test find DELETE
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
                                                                       "DELETE", values, contentTypes,
                                                                       acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethod().getName());     
        
    }
    
    @Test
    @org.junit.Ignore
    public void testFindTargetResourceClassWithSubResource() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();

        Map<String, String> values = new HashMap<String, String>(); 
        String contentTypes = "*/*";
        String acceptContentTypes = "*/*";

        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources,
                                                                       "/bookstore/books/123/chapter/1",
                                                                       "GET", values, contentTypes,
                                                                       acceptContentTypes);       
        assertNotNull(ori);
        assertEquals("getBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                                                                       "POST", values, contentTypes,
                                                                       acceptContentTypes);      
        assertNotNull(ori);
        assertEquals("addBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books",
                                                                       "PUT", values, contentTypes,
                                                                       acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("updateBook", ori.getMethod().getName());
        
        ori = JAXRSUtils.findTargetResourceClass(resources, "/bookstore/books/123",
                                                                       "DELETE", values, contentTypes,
                                                                       acceptContentTypes);        
        assertNotNull(ori);
        assertEquals("deleteBook", ori.getMethod().getName());
    }

    @Test
    public void testIntersectMimeTypes() throws Exception {
        //test basic
        List<MediaType> methodMimeTypes = new ArrayList<MediaType>(
             JAXRSUtils.parseMediaTypes("application/mytype,application/xml,application/json"));
        
        MediaType acceptContentType = MediaType.parse("application/json");
        List <MediaType> candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                 MediaType.parse("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test basic       
        methodMimeTypes = JAXRSUtils.parseMediaTypes(
            "application/mytype, application/json, application/xml");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, 
                                                      MediaType.parse("application/json"));  

        assertEquals(1, candidateList.size());
        assertTrue(candidateList.get(0).toString().equals("application/json"));
        
        //test accept wild card */*       
        candidateList = JAXRSUtils.intersectMimeTypes(
            "application/mytype,application/json,application/xml", "*/*");  

        assertEquals(3, candidateList.size());
        
        //test accept wild card application/*       
        methodMimeTypes = JAXRSUtils.parseMediaTypes("text/html,text/xml,application/xml");
        acceptContentType = MediaType.parse("text/*");
        candidateList = JAXRSUtils.intersectMimeTypes(methodMimeTypes, acceptContentType);  

        assertEquals(2, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("text/html".equals(type.toString()) 
                       || "text/xml".equals(type.toString()));            
        }
        
        //test produce wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "application/json");

        assertEquals(1, candidateList.size());
        assertTrue("application/json".equals(candidateList.get(0).toString()));
        
        //test produce wild card application/*
        candidateList = JAXRSUtils.intersectMimeTypes("application/*", "application/json");  

        assertEquals(1, candidateList.size());
        assertTrue("application/json".equals(candidateList.get(0).toString()));        
        
        //test produce wild card */*, accept wild card */*
        candidateList = JAXRSUtils.intersectMimeTypes("*/*", "*/*");  

        assertEquals(1, candidateList.size());
        assertTrue("*/*".equals(candidateList.get(0).toString()));
    }
    
    @Test
    public void testIntersectMimeTypesTwoArray() throws Exception {
        //test basic
        List <MediaType> acceptedMimeTypes = 
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");
        
        List <MediaType> candidateList = 
            JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, JAXRSUtils.ALL_TYPES);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString()) 
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }
        
        //test basic
        acceptedMimeTypes = Collections.singletonList(JAXRSUtils.ALL_TYPES);
        List<MediaType> providerMimeTypes = 
            JAXRSUtils.parseMediaTypes("application/mytype, application/xml, application/json");

        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, providerMimeTypes);

        assertEquals(3, candidateList.size());
        for (MediaType type : candidateList) {
            assertTrue("application/mytype".equals(type.toString()) 
                       || "application/xml".equals(type.toString())
                       || "application/json".equals(type.toString()));
        }
        
        //test empty
        acceptedMimeTypes = JAXRSUtils.parseMediaTypes("application/mytype,application/xml");
        
        candidateList = JAXRSUtils.intersectMimeTypes(acceptedMimeTypes, 
                                                      MediaType.parse("application/json"));

        assertEquals(0, candidateList.size());
    }
    
    @Test
    public void testParseMediaTypes() throws Exception {
        List<MediaType> types = JAXRSUtils.parseMediaTypes("*");
        assertTrue(types.size() == 1 
                   && types.get(0).equals(JAXRSUtils.ALL_TYPES));
        types = JAXRSUtils.parseMediaTypes("text/*");
        assertTrue(types.size() == 1 && types.get(0).equals(new MediaType("text", "*")));
        types = JAXRSUtils.parseMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/*".equals(types.get(0).toString())
                   && "text/plain;q=.2".equals(types.get(1).toString())
                   && "text/xml".equals(types.get(2).toString())
                   && "text/bar".equals(types.get(3).toString()));
        
    }
    
    @Test
    public void testSortMediaTypes() throws Exception {
        List<MediaType> types = 
            JAXRSUtils.sortMediaTypes("text/*,text/plain;q=.2,text/xml,TEXT/BAR");
        assertTrue(types.size() == 4
                   && "text/bar".equals(types.get(0).toString())
                   && "text/plain;q=.2".equals(types.get(1).toString())
                   && "text/xml".equals(types.get(2).toString())
                   && "text/*".equals(types.get(3).toString()));
    }
    
    @Test
    public void testCompareMediaTypes() throws Exception {
        MediaType m1 = MediaType.parse("text/xml");
        MediaType m2 = MediaType.parse("text/*");
        assertTrue("text/xml is more specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m1, m2) < 0);
        assertTrue("text/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, m1) > 0);
        assertTrue("text/xml should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m1, new MediaType("text", "xml")) == 0);
        assertTrue("text/* should be equal to itself", 
                   JAXRSUtils.compareMediaTypes(m2, new MediaType("text", "*")) == 0);
        
        assertTrue("text/plain is alphabetically earlier than text/xml", 
                   JAXRSUtils.compareMediaTypes(MediaType.parse("text/plain"), m1) < 0);
        assertTrue("text/xml is alphabetically later than text/plain", 
                   JAXRSUtils.compareMediaTypes(m1, MediaType.parse("text/plain")) > 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m1) > 0);
        assertTrue("*/* is less specific than text/xml", 
                   JAXRSUtils.compareMediaTypes(m1, JAXRSUtils.ALL_TYPES) < 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(JAXRSUtils.ALL_TYPES, m2) > 0);
        assertTrue("*/* is less specific than text/*", 
                   JAXRSUtils.compareMediaTypes(m2, JAXRSUtils.ALL_TYPES) < 0);
        
        MediaType m3 = MediaType.parse("text/xml;q=0.2");
        assertTrue("text/xml should be more preferred than than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m1, m3) < 0);
        MediaType m4 = MediaType.parse("text/xml;q=.3");
        assertTrue("text/xml;q=.3 should be more preferred than than text/xml;q=0.2", 
                   JAXRSUtils.compareMediaTypes(m4, m3) < 0);
    }
    
    @Test
    public void testAcceptTypesMatch() throws Exception {
        
        Method m = Customer.class.getMethod("test", new Class[]{});
        ClassResourceInfo cr = new ClassResourceInfo(Customer.class);
        
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "xml"), 
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "*"), 
                                             new OperationResourceInfo(m, cr)));
        assertTrue("text/xml can not be matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("*", "*"), 
                                             new OperationResourceInfo(m, cr)));
        assertFalse("text/plain was matched",
                   JAXRSUtils.matchMimeTypes(JAXRSUtils.ALL_TYPES, 
                                             new MediaType("text", "plain"), 
                                             new OperationResourceInfo(m, cr)));
    }
 
    
    @Test
    public void testQueryParameters() throws Exception {
        Class[] argType = {String.class, Integer.TYPE};
        Method m = Customer.class.getMethod("testQuery", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.QUERY_STRING, "query=24");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null),
                                                           null, 
                                                           messageImpl);
        assertEquals("Query Parameter was not matched correctly", "24", params.get(0));
        assertEquals("Primitive Query Parameter was not matched correctly", 24, params.get(1));
        
        
    }
    
    @Test
    public void testMultipleQueryParameters() throws Exception {
        Class[] argType = {String.class, String.class};
        Method m = Customer.class.getMethod("testMultipleQuery", argType);
        MessageImpl messageImpl = new MessageImpl();
        
        messageImpl.put(Message.QUERY_STRING, "query=first&query2=second");
        List<Object> params = JAXRSUtils.processParameters(new OperationResourceInfo(m, null), 
                                                           null, messageImpl);
        assertEquals("First Query Parameter of multiple was not matched correctly", "first", params.get(0));
        assertEquals("Second Query Parameter of multiple was not matched correctly", 
                     "second", params.get(1));    
    }
    
    @Test
    public void testSelectResourceMethod() throws Exception {
        ClassResourceInfo cri = new ClassResourceInfo(Customer.class);
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItAsXML", new Class[]{}), 
                                         cri);
        ori1.setHttpMethod("GET");
        ori1.setURITemplate(new URITemplate("/"));
        OperationResourceInfo ori2 = new OperationResourceInfo(
                                         Customer.class.getMethod("getItPlain", new Class[]{}), 
                                         cri);
        ori2.setHttpMethod("GET");
        ori2.setURITemplate(new URITemplate("/"));
        MethodDispatcher md = new MethodDispatcher(); 
        md.bind(ori1, Customer.class.getMethod("getItAsXML", new Class[]{}));
        md.bind(ori2, Customer.class.getMethod("getItPlain", new Class[]{}));
        cri.setMethodDispatcher(md);
        
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", null, 
                                               "*/*", "text/plain");
        
        assertSame(ori, ori2);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", null, 
                                              "*/*", "text/xml");
                         
        assertSame(ori, ori1);
        
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", null, 
                                          "*/*", "*,text/plain,text/xml");
                     
        assertSame(ori, ori2);
        ori = JAXRSUtils.findTargetMethod(cri, "/", "GET", null, 
                                          "*/*", "*,x/y,text/xml,text/plain");
                     
        assertSame(ori, ori2);
    }
}

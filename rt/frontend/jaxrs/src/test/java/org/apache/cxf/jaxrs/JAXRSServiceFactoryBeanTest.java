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

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXRSServiceFactoryBeanTest extends Assert {

    @Before
    public void setUp() throws Exception {

    }
    @Test
    public void testNoSubResources() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStoreNoSubResource.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(1, resources.size());
        
        //Verify root ClassResourceInfo: BookStoreNoSubResource
        ClassResourceInfo rootCri = resources.get(0);
        assertNotNull(rootCri.getURITemplate());
        URITemplate template = rootCri.getURITemplate();
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertTrue(template.match("/bookstore/books/123", values));     
        assertFalse(rootCri.hasSubResources());   
        MethodDispatcher md = rootCri.getMethodDispatcher();
        assertEquals(5, md.getOperationResourceInfos().size());  
        for (OperationResourceInfo ori : md.getOperationResourceInfos()) {
            if ("getBook".equals(ori.getMethod().getName())) {
                assertEquals("GET", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("getBookJSON".equals(ori.getMethod().getName())) {
                assertEquals("GET", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("addBook".equals(ori.getMethod().getName())) {
                assertEquals("POST", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("updateBook".equals(ori.getMethod().getName())) {
                assertEquals("PUT", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("deleteBook".equals(ori.getMethod().getName())) {
                assertEquals("DELETE", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else {
                fail("unexpected OperationResourceInfo" + ori.getMethod().getName());
            }
        }
    }

    @Test
    public void testSubResources() throws Exception {
        JAXRSServiceFactoryBean sf = new JAXRSServiceFactoryBean();
        sf.setResourceClasses(org.apache.cxf.jaxrs.resources.BookStore.class);
        sf.create();
        
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)sf.getService()).getClassResourceInfos();
        assertEquals(1, resources.size());
        
        //Verify root ClassResourceInfo: BookStore
        ClassResourceInfo rootCri = resources.get(0);
        assertNotNull(rootCri.getURITemplate());
        URITemplate template = rootCri.getURITemplate();
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertTrue(template.match("/bookstore/books/123", values));     
        assertTrue(rootCri.hasSubResources());   
        MethodDispatcher md = rootCri.getMethodDispatcher();
        assertEquals(4, md.getOperationResourceInfos().size());  
        for (OperationResourceInfo ori : md.getOperationResourceInfos()) {
            if ("getBook".equals(ori.getMethod().getName())) {
                assertNull(ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertTrue(ori.isSubResourceLocator());
            } else if ("addBook".equals(ori.getMethod().getName())) {
                assertEquals("POST", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("updateBook".equals(ori.getMethod().getName())) {
                assertEquals("PUT", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else if ("deleteBook".equals(ori.getMethod().getName())) {
                assertEquals("DELETE", ori.getHttpMethod());
                assertNotNull(ori.getURITemplate());
                assertFalse(ori.isSubResourceLocator());
            } else {
                fail("unexpected OperationResourceInfo" + ori.getMethod().getName());
            }
        }
        
        // Verify sub-resource ClassResourceInfo: Book
        assertEquals(1, rootCri.getSubClassResourceInfo().size());
        ClassResourceInfo subCri = rootCri.getSubClassResourceInfo().get(0);        
        assertEquals(subCri.getURITemplate().getValue(), "/");
        assertEquals(org.apache.cxf.jaxrs.resources.Book.class, subCri.getResourceClass());
        MethodDispatcher subMd = subCri.getMethodDispatcher();
        assertEquals(2, subMd.getOperationResourceInfos().size());
        //getChapter method
        OperationResourceInfo subOri = subMd.getOperationResourceInfos().iterator().next();
        assertEquals("GET", subOri.getHttpMethod());
        assertNotNull(subOri.getURITemplate());
        
        //getState method
        OperationResourceInfo subOri2 = subMd.getOperationResourceInfos().iterator().next();
        assertEquals("GET", subOri2.getHttpMethod());
        assertNotNull(subOri2.getURITemplate());
    }

}

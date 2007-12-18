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
package org.apache.cxf.jaxrs.model;


import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class URITemplateTest extends Assert {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testMatchBasic() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}",
                                                  URITemplate.UNLIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();
        
        boolean match = uriTemplate.match("/customers/123/", values);
        assertTrue(match);
        String value = values.get("id");
        assertEquals("123", value);
    }
    
    @Test
    public void testMatchBasicTwoParametersVariation1() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{name}/{department}",
                                                  URITemplate.UNLIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();
        
        boolean match = uriTemplate.match("/customers/john/CS", values);
        assertTrue(match);
        String name = values.get("name");
        String department = values.get("department");
        assertEquals("john", name);
        assertEquals("CS", department);
    }
    
    @Test
    public void testMatchBasicTwoParametersVariation2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/name/{name}/dep/{department}",
                                                  URITemplate.UNLIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();
        
        boolean match = uriTemplate.match("/customers/name/john/dep/CS", values);
        assertTrue(match);
        String name = values.get("name");
        String department = values.get("department");
        assertEquals("john", name);
        assertEquals("CS", department);
    }    
    
    @Test
    public void testURITemplateWithSubResource() throws Exception {
        //So "/customers" is the URITemplate for the root resource class
        URITemplate uriTemplate = new URITemplate("/customers", URITemplate.LIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();
        
        boolean match = uriTemplate.match("/customers/123", values);
        assertTrue(match);
        String subResourcePath = values.get(URITemplate.RIGHT_HAND_VALUE);
        assertEquals("/123", subResourcePath);
    }
        
    @Test
    public void testURITemplateWithSubResourceVariation2() throws Exception {
        //So "/customers" is the URITemplate for the root resource class
        URITemplate uriTemplate = new URITemplate("/customers", URITemplate.LIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();
        
        boolean match = uriTemplate.match("/customers/name/john/dep/CS", values);
        assertTrue(match);
        String subResourcePath = values.get(URITemplate.RIGHT_HAND_VALUE);
        assertEquals("/name/john/dep/CS", subResourcePath);
    }
        
    @Test
    /* Test a sub-resource locator method like this
     * @HttpMethod("GET") @UriTemplate("/books/{bookId}/") 
     * public Book getBook(@UriParam("bookId") String id)
     */
    public void testURITemplateWithSubResourceVariation3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId}/", URITemplate.LIMITED_REGEX_SUFFIX);
        Map<String, String> values = new HashMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        String subResourcePath = values.get(URITemplate.RIGHT_HAND_VALUE);
        assertEquals("/chapter/1", subResourcePath);
    }
}

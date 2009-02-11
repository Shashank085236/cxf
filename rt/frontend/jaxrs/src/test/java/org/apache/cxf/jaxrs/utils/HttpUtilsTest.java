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

package org.apache.cxf.jaxrs.utils;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

public class HttpUtilsTest extends Assert {

    @Test
    public void testUpdatePath() {
        assertEquals("/", HttpUtils.getPathToMatch("/", "/", true));
        assertEquals("/", HttpUtils.getPathToMatch("/", "/bar", true));
        assertEquals("/bar", HttpUtils.getPathToMatch("/bar", "/", true));
        
        assertEquals("/", HttpUtils.getPathToMatch("/bar", "/bar", true));
        assertEquals("/bar", HttpUtils.getPathToMatch("/baz/bar", "/baz", true));
        assertEquals("/baz/bar/foo/", HttpUtils.getPathToMatch("/baz/bar/foo/", "/bar", true));
        
    }
    
    @Test
    public void testParameterErrorStatus() {
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.PATH));
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.QUERY));
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.MATRIX));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.HEADER));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.FORM));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.COOKIE));
    }
    
}
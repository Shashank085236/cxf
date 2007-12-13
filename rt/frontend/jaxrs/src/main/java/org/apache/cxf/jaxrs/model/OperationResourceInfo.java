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

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.UriTemplate;
import javax.ws.rs.ext.EntityProvider;

public class OperationResourceInfo {
    private URITemplate uriTemplate;
    private ClassResourceInfo classResourceInfo;
    private Method method;
    private List<Class> parameterTypeList;
    private List<Class> annotatedParameterTypeList;
    private List<EntityProvider> entityProviderList;
    private String httpMethod;

    public OperationResourceInfo(Method m, ClassResourceInfo cri) {
        method = m;
        classResourceInfo = cri;
    }

    public URITemplate getURITemplate() {
        return uriTemplate;
    }

    public void setURITemplate(URITemplate u) {
        uriTemplate = u;
    }

    public ClassResourceInfo getClassResourceInfo() {
        return classResourceInfo;
    }

    public void setClassResourceInfo(ClassResourceInfo c) {
        classResourceInfo = c;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method m) {
        method = m;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String m) {
        httpMethod = m;
    }
    
    public boolean isSubResourceLocator() {
        if (method.getAnnotation(UriTemplate.class) != null 
            && method.getAnnotation(HttpMethod.class) == null) {
            return true;
        }
        return false;
    }

    public List<Class> getParameterTypeList() {
        return parameterTypeList;
    }

    public List<Class> getAnnotatedParameterTypeList() {
        return annotatedParameterTypeList;
    }

    public List<EntityProvider> getEntityProviderList() {
        return entityProviderList;
    }

    public String[] getProduceMimeTypes() {
        //TODO: 
        /*
         * These annotations MAY be applied to a resource class method, a
         * resource class, or to an EntityProvider. Declarations on a resource
         * class method override any on the resource class; declarations on an
         * EntityProvider for a method argument or return type override those on
         * a resource class or resource method. In the absence of either of
         * these annotations, support for any media type (��*��) is assumed.
         */   
        
        String[] mimeTypes = {"*/*"};
        ProduceMime c = method.getAnnotation(ProduceMime.class);
        if (c != null) {
            mimeTypes = c.value();               
        }
        
        return mimeTypes;
    }
}

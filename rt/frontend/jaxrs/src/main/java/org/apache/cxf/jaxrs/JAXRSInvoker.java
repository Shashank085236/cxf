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


import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.AbstractInvoker;

public class JAXRSInvoker extends AbstractInvoker {
    private List<Object> resourceObjects;

    public JAXRSInvoker() {
    }
    
    public JAXRSInvoker(List<Object> resourceObjects) {
        this.resourceObjects = resourceObjects;
    }
    
    public Object invoke(Exchange exchange, Object request) {
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);

        ClassResourceInfo classResourceInfo = ori.getClassResourceInfo();
        Method m = classResourceInfo.getMethodDispatcher().getMethod(ori);
        Object resourceObject = getServiceObject(exchange);

        List<Object> params = null;
        if (request instanceof List) {
            params = CastUtils.cast((List<?>)request);
        } else if (request != null) {
            params = new MessageContentsList(request);
        }

        Object result = invoke(exchange, resourceObject, m, params);
        
        if (ori.isSubResourceLocator()) {
            //the result becomes the object that will handle the request
            if (result != null) {
                if (result instanceof MessageContentsList) {
                    result = ((MessageContentsList)result).get(0);
                } else if (result instanceof List) {
                    result = ((List)result).get(0);
                } else if (result.getClass().isArray()) {
                    result = ((Object[])result)[0];
                } 
            }
            resourceObjects = new ArrayList<Object>();
            resourceObjects.add(result);
            
            Map<String, String> values = new HashMap<String, String>();                 
            Message msg = exchange.getInMessage();
            String subResourcePath = (String)msg.get(JAXRSInInterceptor.SUBRESOURCE_PATH);
            String httpMethod = (String)msg.get(Message.HTTP_REQUEST_METHOD); 
            ClassResourceInfo subCri = JAXRSUtils.findSubResourceClass(classResourceInfo, result.getClass());
            OperationResourceInfo subOri = JAXRSUtils.findTargetMethod(subCri, subResourcePath,
                                                                                     httpMethod, values);
            exchange.put(OperationResourceInfo.class, subOri);

            // work out request parameters for the sub-resouce class. Here we
            // presume Inputstream has not been consumed yet by the root resource class.
            //I.e., only one place either in the root resource or sub-resouce class can
            //have a parameter that read from entitybody.
            InputStream is = msg.getContent(InputStream.class);
            List<Object> newParams = JAXRSUtils.processParameters(subOri.getMethod(), subResourcePath,
                                                                             httpMethod, values, is);
            msg.setContent(List.class, newParams);
            
            return this.invoke(exchange, newParams);
        }
        
        return result;
    }    
    
    public Object getServiceObject(Exchange exchange) {
        Object serviceObject = null;
        
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        ClassResourceInfo classResourceInfo = ori.getClassResourceInfo();
        
        if (resourceObjects != null) {
            Class c  = classResourceInfo.getResourceClass();
            for (Object resourceObject : resourceObjects) {
                if (c.isInstance(resourceObject)) {
                    serviceObject = resourceObject;
                }
            }
        }
        
        if (serviceObject == null) {
            serviceObject = classResourceInfo.getResourceProvider().getInstance();
        }
        
        return serviceObject;
    }
}

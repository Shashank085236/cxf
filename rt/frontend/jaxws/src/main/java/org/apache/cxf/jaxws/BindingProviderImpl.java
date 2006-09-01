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

package org.apache.cxf.jaxws;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

public class BindingProviderImpl implements BindingProvider {
    
    private final Binding binding;
    private ThreadLocal<Map<String, Object>> requestContext;
    private Map<String, Object> responseContext;
    
    public BindingProviderImpl() {
        binding = null;
    }

    public BindingProviderImpl(Binding b) {
        binding = b;
    }
    
    public Map<String, Object> getRequestContext() {
        if (requestContext == null) {
            requestContext = new ThreadLocal<Map<String, Object>>() {
                protected synchronized Map<String, Object> initialValue() {
                    return new HashMap<String, Object>();
                }
            };
        }
        return (Map<String, Object>)requestContext.get();
    }
    
    public Map<String, Object> getResponseContext() {
        if (responseContext == null) {
            responseContext = new HashMap<String, Object>();
        }
        return responseContext;
    }

    public Binding getBinding() {
        return binding;
    }
    
    protected void populateResponseContext(MessageContext ctx) {
        
        Iterator<String> iter  = ctx.keySet().iterator();
        Map<String, Object> respCtx = getResponseContext();
        while (iter.hasNext()) {
            String obj = iter.next();
            if (MessageContext.Scope.APPLICATION.compareTo(ctx.getScope(obj)) == 0) {
                respCtx.put(obj, ctx.get(obj));
            }
        }
    }

}

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

import java.lang.reflect.Method;
import java.util.List;

import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.common.util.factory.Factory;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.support.ContextPropertiesMapping;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.ApplicationScopePolicy;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.ScopePolicy;

public class JAXWSMethodInvoker extends FactoryInvoker {

    public JAXWSMethodInvoker(final Object bean) {
        super(
            new Factory() {
                public Object create() {
                    return bean;
                }
            },
            ApplicationScopePolicy.instance());
        
    }
    
    public JAXWSMethodInvoker(Factory factory) {
        super(factory, ApplicationScopePolicy.instance());
    }
    
    public JAXWSMethodInvoker(Factory factory, ScopePolicy scope) {
        super(factory, scope);
    }

    @SuppressWarnings("unchecked")
    protected Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {
        // set up the webservice request context 
        MessageContext ctx = 
            ContextPropertiesMapping.createWebServiceContext(exchange);
        WebServiceContextImpl.setMessageContext(ctx);
        
        List<Object> res = (List<Object>) super.invoke(exchange, serviceObject, m, params);
        
        //update the webservice response context
        ContextPropertiesMapping.updateWebServiceContext(exchange, ctx);
        return res;
    }
}

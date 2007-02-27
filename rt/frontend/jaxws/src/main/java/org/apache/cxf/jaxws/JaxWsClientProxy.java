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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.jaxws.support.ContextPropertiesMapping;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.workqueue.OneShotAsyncExecutor;

public class JaxWsClientProxy extends org.apache.cxf.frontend.ClientProxy implements
    InvocationHandler, BindingProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsClientProxy.class);

    protected AtomicReference<Map<String, Object>> requestContext = 
            new AtomicReference<Map<String, Object>>();
    protected Map<String, Object> responseContext;

    private Endpoint endpoint;
    private final Binding binding;

    public JaxWsClientProxy(Client c, Binding b) {
        super(c);
        this.endpoint = c.getEndpoint();
        this.binding = b;
        setupEndpointAddressContext();
    }

    private void setupEndpointAddressContext() {
        // NOTE for jms transport the address would be null
        if (null != endpoint && null != endpoint.getEndpointInfo().getAddress()) {
            getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                    endpoint.getEndpointInfo().getAddress());
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodDispatcher dispatcher = (MethodDispatcher)endpoint.getService().get(
                                                                                  MethodDispatcher.class
                                                                                      .getName());
        BindingOperationInfo oi = dispatcher.getBindingOperation(method, endpoint);
        if (oi == null) {
            // check for method on BindingProvider and Object
            if (method.getDeclaringClass().equals(BindingProvider.class)
                || method.getDeclaringClass().equals(BindingProviderImpl.class)
                || method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this);
            }

            Message msg = new Message("NO_OPERATION_INFO", LOG, method.getName());
            throw new WebServiceException(msg.toString());
        }

        Object[] params = args;
        if (null == params) {
            params = new Object[0];
        }

        Map<String, Object> reqContext = this.getRequestContext();
        Map<String, Object> resContext = this.getResponseContext();
        Map<String, Object> context = new HashMap<String, Object>();

        // need to do context mapping from jax-ws to cxf message
        ContextPropertiesMapping.mapRequestfromJaxws2Cxf(reqContext);

        context.put(Client.REQUEST_CONTEXT, reqContext);
        context.put(Client.RESPONSE_CONTEXT, resContext);

        reqContext.put(Method.class.getName(), method);

        boolean isAsync = method.getName().endsWith("Async");

        Object result = null;
        if (isAsync) {
            result = invokeAsync(method, oi, params, context);
        } else {
            result = invokeSync(method, oi, params, context);
        }
        // need to do context mapping from cxf message to jax-ws
        ContextPropertiesMapping.mapResponsefromCxf2Jaxws(resContext);
        return result;

    }

    private Object invokeAsync(Method method, BindingOperationInfo oi, Object[] params,
                               Map<String, Object> context) {

        FutureTask<Object> f = new FutureTask<Object>(new JAXWSAsyncCallable(this, method, oi, params,
                                                                             context));

        endpoint.getService().setExecutor(OneShotAsyncExecutor.getInstance());
        endpoint.getService().getExecutor().execute(f);
        Response<?> r = new AsyncResponse<Object>(f, Object.class);
        if (params.length > 0 && params[params.length - 1] instanceof AsyncHandler) {
            // callback style
            AsyncCallbackFuture callback = 
                new AsyncCallbackFuture(r, (AsyncHandler)params[params.length - 1]);
            endpoint.getService().getExecutor().execute(callback);
            return callback;
        } else {
            return r;
        }
    }

    public Map<String, Object> getRequestContext() {
        if (null == requestContext.get()) {
            requestContext.compareAndSet(null, new ConcurrentHashMap<String, Object>(4));
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
        Iterator<String> iter = ctx.keySet().iterator();
        Map<String, Object> respCtx = getResponseContext();
        while (iter.hasNext()) {
            String obj = iter.next();
            if (MessageContext.Scope.APPLICATION.compareTo(ctx.getScope(obj)) == 0) {
                respCtx.put(obj, ctx.get(obj));
            }
        }
    }

    public EndpointReference getEndpointReference() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        // TODO
        throw new UnsupportedOperationException();
    }
}

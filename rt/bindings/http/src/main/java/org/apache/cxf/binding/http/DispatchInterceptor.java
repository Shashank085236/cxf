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
package org.apache.cxf.binding.http;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;

public class DispatchInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String RELATIVE_PATH = "relative.path";
    private static final Logger LOG = Logger.getLogger(DispatchInterceptor.class.getName());
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(DispatchInterceptor.class);

    public DispatchInterceptor() {
        super();
        setPhase(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) {
        String path = (String)message.get(Message.PATH_INFO);
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        
        String address = message.getExchange().get(Endpoint.class).getEndpointInfo().getAddress();
        int idx = address.indexOf('/', 7);
        if (idx != -1) {
            address = address.substring(idx);
        }
        
        if (path.startsWith(address)) {
            path = path.substring(address.length());
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
        
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        message.put(RELATIVE_PATH, path);
        LOG.info("Invoking " + method + " on " + path);

        URIMapper mapper = (URIMapper)message.getExchange().get(Service.class).get(URIMapper.class.getName());

        BindingOperationInfo op = mapper.getOperation(path, method, message);

        if (op == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OP", BUNDLE, method, path));
        }

        message.getExchange().put(BindingOperationInfo.class, op);
    }
}

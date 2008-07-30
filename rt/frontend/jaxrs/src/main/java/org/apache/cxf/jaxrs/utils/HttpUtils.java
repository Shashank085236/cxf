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

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public final class HttpUtils {
    
    private static final String LOCAL_IP_ADDRESS = "127.0.0.1";
    private static final String LOCAL_HOST = "localhost";
    
    
    private HttpUtils() {
    }
    
    public static URI toAbsoluteUri(URI u, Message message) { 
        if (!u.isAbsolute()) {
            HttpServletRequest httpRequest = 
                (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
            if (httpRequest != null) {
                String scheme = httpRequest.isSecure() ? "https" : "http";
                String host = httpRequest.getLocalName();
                if (LOCAL_IP_ADDRESS.equals(host)) {
                    host = LOCAL_HOST;
                }
                int port = httpRequest.getLocalPort();
                return URI.create(scheme + "://" + host + ':' + port + u.toString());
            }
        }
        return u;
    }
}

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

package org.apache.cxf.jaxrs.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class UriInfoImpl implements UriInfo {

    private MultivaluedMap<String, String> templateParams; 
    private Message message;
    
    public UriInfoImpl(Message m, MultivaluedMap<String, String> templateParams) {
        this.message = m;
        this.templateParams = templateParams;
    }
    
    public URI getAbsolutePath() {
        String path = getAbsolutePathAsString();
        return URI.create(path);
    }

    public UriBuilder getAbsolutePathBuilder() {
        return new UriBuilderImpl(getAbsolutePath());
    }

    public URI getBaseUri() {
        URI u = URI.create(HttpUtils.getEndpointAddress(message));
        return HttpUtils.toAbsoluteUri(u, message);
    }

    public UriBuilder getBaseUriBuilder() {
        return new UriBuilderImpl(getBaseUri());
    }

    public String getPath() {
        return getPath(true);
    }

    public String getPath(boolean decode) {
        
        return doGetPath(decode, true);
    }

    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    public List<PathSegment> getPathSegments(boolean decode) {
        return JAXRSUtils.getPathSegments(getPath(), decode);
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return JAXRSUtils.getStructuredParams((String)message.get(Message.QUERY_STRING),
                                              "&",
                                              decode);
    }

    public URI getRequestUri() {
        String path = getAbsolutePathAsString();
        String queries = (String)message.get(Message.QUERY_STRING);
        if (queries != null) {
            path += "?" + queries;
        }
        return URI.create(path);
    }

    public UriBuilder getRequestUriBuilder() {
        return new UriBuilderImpl(getRequestUri());
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        for (Map.Entry<String, List<String>> entry : templateParams.entrySet()) {
            if (entry.getKey().equals(URITemplate.FINAL_MATCH_GROUP)) {
                continue;
            }
            values.add(entry.getKey(), 
                       decode ? JAXRSUtils.uriDecode(entry.getValue().get(0)) 
                              : entry.getValue().get(0));
        }
        return values;
    }

    public List<String> getAncestorResourceURIs() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Object> getAncestorResources() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getAncestorResourceURIs(boolean arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    public String getPathExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    public UriBuilder getPlatonicRequestUriBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    private String doGetPath(boolean decode, boolean addSlash) {
        String path = HttpUtils.getPathToMatch(message, addSlash);
        return decode ? JAXRSUtils.uriDecode(path) : path;
    }
    
    private String getAbsolutePathAsString() {
        String address = getBaseUri().toString();
        String path = doGetPath(true, false);
        if (path.startsWith("/") && address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return address + path;
    }
}

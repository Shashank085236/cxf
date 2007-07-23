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


package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPTransportFactory;

public class ServletTransportFactory extends AbstractHTTPTransportFactory
    implements DestinationFactory {

    private Bus bus;
    private Map<String, ServletDestination> destinations = 
        new ConcurrentHashMap<String, ServletDestination>();
    
    public ServletTransportFactory(Bus b) {
        bus = b;
    }

    public ServletTransportFactory() {
    }

    public Bus getBus() {
        return bus;
    }

    @Resource
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public void removeDestination(String path) {
        destinations.remove(path);
    }
    
    public Destination getDestination(EndpointInfo endpointInfo)
        throws IOException {
        ServletDestination d = getDestinationForPath(endpointInfo.getAddress());
        if (d == null) { 
            String path = getTrimmedPath(endpointInfo.getAddress());
            d = new ServletDestination(bus, null, endpointInfo, this, path);
            destinations.put(path, d);
        }
        return d;
    }
    
    public ServletDestination getDestinationForPath(String path) {
        // to use the url context match  
        return destinations.get(getTrimmedPath(path));
    }

    static String getTrimmedPath(String path) {
        if (path == null) {
            return "/";
        }
        final String lh = "http://localhost/";
        final String lhs = "https://localhost/";
        
        if (path.startsWith(lh)) {
            path = path.substring(lh.length());
        } else if (path.startsWith(lhs)) {
            path = path.substring(lhs.length());
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
            
        }
        return path;
    }
    
    public Collection<ServletDestination> getDestinations() {
        return Collections.unmodifiableCollection(destinations.values());        
    }
    public Set<String> getDestinationsPaths() {
        return Collections.unmodifiableSet(destinations.keySet());        
    }

    
}

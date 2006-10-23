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


package org.apache.cxf.jaxws.servlet;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class ServletTransportFactory extends AbstractTransportFactory
    implements ConduitInitiator, DestinationFactory {

    EndpointReferenceType reference;
    
    private Bus bus;    
    
    public ServletTransportFactory(Bus b, EndpointReferenceType ref) {
        bus = b;      
        reference = ref;
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

    public Conduit getConduit(EndpointInfo endpointInfo)
        throws IOException {
        return null;
    }

    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target)
        throws IOException {
        return null;
    }

    public Destination getDestination(EndpointInfo endpointInfo)
        throws IOException {
        return new ServletDestination(bus, null, endpointInfo, reference);
    }

}

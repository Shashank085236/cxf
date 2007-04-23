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

package org.apache.cxf.jaxws.spi;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
//TODO JAX-WS 2.1
//import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
//TODO JAX-WS 2.1
//import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.ServiceDelegate;
//TODO JAX-WS 2.1
//import javax.xml.ws.wsaddressing.W3CEndpointReference;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.ServiceImpl;

public class ProviderImpl extends javax.xml.ws.spi.Provider {
    public static final String JAXWS_PROVIDER = ProviderImpl.class.getName();
    
    private static final Logger LOG = LogUtils.getL7dLogger(ProviderImpl.class);

    @Override
    public ServiceDelegate createServiceDelegate(URL url,
                                                 QName qname,
                                                 Class cls) {
        Bus bus = BusFactory.getDefaultBus();
        return new ServiceImpl(bus, url, qname, cls);
    }

    @Override
    public Endpoint createEndpoint(String bindingId, Object implementor) {

        Endpoint ep = null;
        if (EndpointUtils.isValidImplementor(implementor)) {
            Bus bus = BusFactory.getDefaultBus();
            ep = new EndpointImpl(bus, implementor, bindingId);
            return ep;
        } else {
            throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
        }
    }

    @Override
    public Endpoint createAndPublishEndpoint(String url, Object implementor) {
        Endpoint ep = createEndpoint(null, implementor);
        ep.publish(url);
        return ep;
    }

    // TODO JAX-WS 2.1
    /*
    public W3CEndpointReference createW3CEndpointReference(String address,
                                                           QName serviceName,
                                                           QName portName,
                                                           List<Element> metadata,
                                                           String wsdlDocumentLocation,
                                                           List<Element> referenceParameters) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public <T> T getPort(EndpointReference endpointReference,
                         Class<T> serviceEndpointInterface,
                         WebServiceFeature... features) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EndpointReference readEndpointReference(Source eprInfoset) {
        // TODO
        throw new UnsupportedOperationException();
    }
    */

}

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

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.wsdl.Port;
import javax.wsdl.extensions.http.HTTPAddress;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;
import org.xmlsoap.schemas.wsdl.http.AddressType;


/**
 * As a ConduitInitiator, this class sets up new HTTPConduits for particular
 * endpoints.
 *
 * TODO: Document DestinationFactory
 * TODO: Document WSDLEndpointFactory
 *
 */
public abstract class AbstractHTTPTransportFactory 
    extends AbstractTransportFactory 
    implements ConduitInitiator, DestinationFactory, WSDLEndpointFactory {

    /**
     * This constant holds the prefixes served by this factory.
     */
    private static final Set<String> URI_PREFIXES = new HashSet<String>();
    static {
        URI_PREFIXES.add("http://");
        URI_PREFIXES.add("https://");
    }

    /**
     * The CXF Bus which this HTTPTransportFactory
     * is governed.
     */
    private Bus bus;
  
    /**
     * This collection contains "activationNamespaces" which is synominous
     * with "transportId"s. TransportIds are already part of 
     * AbstractTransportFactory.
     * TODO: Change these to "transportIds"?
     */
    private Collection<String> activationNamespaces;

    /**
     * This method is used by Spring to inject the bus.
     * @param b The CXF bus.
     */
    @Resource(name = "bus")
    public void setBus(Bus b) {
        bus = b;
    }

    /**
     * This method returns the CXF Bus under which this HTTPTransportFactory
     * is governed.
     */
    public Bus getBus() {
        return bus;
    }

    /**
     * This call is used by spring to "inject" the transport ids.
     * TODO: Change this to "setTransportIds"?
     * @param ans The transport ids.
     */
    @Resource(name = "activationNamespaces")
    public void setActivationNamespaces(Collection<String> ans) {
        activationNamespaces = ans;
    }

    /**
     * This call gets called after this class is instantiated by Spring.
     * It registers itself as a ConduitInitiator and DestinationFactory under
     * the many names that are considered "transportIds" (which are currently
     * named "activationNamespaces").
     */
    @PostConstruct
    void registerWithBindingManager() {
        if (null == bus) {
            return;
        }
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);

        //Note, activationNamespaces can be null
        if (null != cim && null != activationNamespaces) {
            for (String ns : activationNamespaces) {
                cim.registerConduitInitiator(ns, this);
            }
        }
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        if (null != dfm && null != activationNamespaces) {
            for (String ns : activationNamespaces) {
                dfm.registerDestinationFactory(ns, this);
            }
        }
    }

    /**
     * This call creates a new HTTPConduit for the endpoint. It is equivalent
     * to calling getConduit without an EndpointReferenceType.
     */
    public Conduit getConduit(EndpointInfo endpointInfo) throws IOException {
        return getConduit(endpointInfo, null);
    }

    /**
     * This call creates a new HTTP Conduit based on the EndpointInfo and
     * EndpointReferenceType.
     * TODO: What are the formal constraints on EndpointInfo and 
     * EndpointReferenceType values?
     */
    public Conduit getConduit(
            EndpointInfo endpointInfo,
            EndpointReferenceType target
    ) throws IOException {
        HTTPConduit conduit = target == null
            ? new HTTPConduit(bus, endpointInfo)
            : new HTTPConduit(bus, endpointInfo, target);
        
        // Spring configure the conduit.  
        configure(conduit);
        conduit.finalizeConfig();
        return conduit;
    }

    public abstract Destination getDestination(EndpointInfo endpointInfo) throws IOException;


    public EndpointInfo createEndpointInfo(
        ServiceInfo serviceInfo, 
        BindingInfo b, 
        Port        port
    ) {
        List ees = port.getExtensibilityElements();
        for (Iterator itr = ees.iterator(); itr.hasNext();) {
            Object extensor = itr.next();

            if (extensor instanceof HTTPAddress) {
                HTTPAddress httpAdd = (HTTPAddress)extensor;

                EndpointInfo info = 
                    new EndpointInfo(serviceInfo, 
                            "http://schemas.xmlsoap.org/wsdl/http/");
                info.setAddress(httpAdd.getLocationURI());
                return info;
            } else if (extensor instanceof AddressType) {
                AddressType httpAdd = (AddressType)extensor;

                EndpointInfo info = 
                    new EndpointInfo(serviceInfo, 
                            "http://schemas.xmlsoap.org/wsdl/http/");
                info.setAddress(httpAdd.getLocation());
                return info;
            }
        }

        return null;
    }

    public void createPortExtensors(EndpointInfo ei, Service service) {
        // TODO
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

    /**
     * This call uses the Configurer from the bus to configure
     * a bean.
     * 
     * @param bean
     */
    protected void configure(Object bean) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(bean);
        }
    }

    /**
     * This static call creates a connection factory based on
     * the existence of the SSL (TLS) client side configuration. 
     */
    static HttpURLConnectionFactory getConnectionFactory(
        HTTPConduit configuredConduit
    ) {
        if (configuredConduit.getSslClient() == null) {
            return new HttpURLConnectionFactoryImpl();
        } else {
            return new HttpsURLConnectionFactory(
                             configuredConduit.getSslClient());
        }
    }    

}

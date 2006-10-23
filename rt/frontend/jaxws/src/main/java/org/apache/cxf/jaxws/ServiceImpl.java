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
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.ServiceDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.xml.XMLBindingInfoFactoryBean;
import org.apache.cxf.binding.xml.XMLConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingInfoFactoryBean;
import org.apache.cxf.jaxws.handler.HandlerResolverImpl;
import org.apache.cxf.jaxws.support.DummyImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractBindingInfoFactoryBean;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class ServiceImpl extends ServiceDelegate {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceImpl.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Bus bus;
    private URL wsdlURL;

    private HandlerResolver handlerResolver;
    private final Collection<QName> ports = new HashSet<QName>();
    private Map<QName, PortInfo> portInfos = new HashMap<QName, PortInfo>();
    private Executor executor;
    private QName serviceName;
    private Class<?> clazz;

    public ServiceImpl(Bus b, URL url, QName name, Class<?> cls) {
        bus = b;
        wsdlURL = url;
        this.serviceName = name;
        clazz = cls;

        handlerResolver = new HandlerResolverImpl(bus, name);
    }

    public void addPort(QName portName, String bindingId, String address) {
        PortInfo portInfo = new PortInfo(bindingId, address);
        portInfos.put(portName, portInfo);
    }

    private Endpoint getJaxwsEndpoint(QName portName, AbstractServiceFactoryBean sf) {
        Service service = sf.getService();
        ServiceInfo si = service.getServiceInfo();
        EndpointInfo ei = null;
        if (portName == null) {
            ei = si.getEndpoints().iterator().next();
        } else {
            PortInfo portInfo = getPortInfo(portName);
            if (null != portInfo) {
                try {
                    ei = createEndpointInfo(sf, portName, portInfo);
                } catch (BusException e) {
                    throw new WebServiceException(e);
                }
            } else {
                ei = si.getEndpoint(portName);
            }
        }

        try {
            return new JaxWsEndpointImpl(bus, service, ei);
        } catch (EndpointException e) {
            throw new WebServiceException(e);
        }
    }

    private AbstractServiceFactoryBean createDispatchService(Class<?> type) {
        AbstractServiceFactoryBean serviceFactory;

        Service dispatchService = null;        
        
        if (null != wsdlURL) {
            WSDLServiceFactory sf = new WSDLServiceFactory(bus, wsdlURL, serviceName);
            dispatchService = sf.create();            
            try {
                dispatchService.setDataBinding(new JAXBDataBinding(clazz));
            } catch (JAXBException e) {
                new WebServiceException(e);
            }          
            serviceFactory = sf;
        } else {
            JaxWsServiceFactoryBean sf = new JaxWsServiceFactoryBean();
            sf.setBus(bus);
            sf.setServiceName(serviceName);
            // maybe we can find another way to create service which have no SEI
            sf.setServiceClass(DummyImpl.class);
            try {                
                sf.setDataBinding(new JAXBDataBinding(type));                 
            } catch (JAXBException e) {
                new WebServiceException(e);
            } 
            dispatchService = sf.create();
            serviceFactory = sf;
        }    
        configureObject(dispatchService);
        return serviceFactory;
    }

    public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode) {
        AbstractServiceFactoryBean sf = createDispatchService(type);
        Endpoint endpoint = getJaxwsEndpoint(portName, sf);

        Dispatch<T> disp = new DispatchImpl<T>(bus, mode, type, getExecutor(), endpoint);

        configureObject(disp);

        return disp;
    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode) {

        AbstractServiceFactoryBean sf = createDispatchService(null);
        Endpoint endpoint = getJaxwsEndpoint(portName, sf);
        Dispatch<Object> disp = new DispatchImpl<Object>(bus, mode, context, Object.class, getExecutor(),
                                                         endpoint);

        configureObject(disp);

        return disp;
    }

    public Executor getExecutor() {
        return executor;
    }

    public HandlerResolver getHandlerResolver() {
        return handlerResolver;
    }

    public <T> T getPort(Class<T> type) {
        return createPort(null, type);
    }

    public <T> T getPort(QName portName, Class<T> type) {
        if (portName == null) {
            throw new WebServiceException(BUNDLE.getString("PORT_NAME_NULL_EXC"));
        }
        return createPort(portName, type);
    }

    public Iterator<QName> getPorts() {
        return ports.iterator();
    }

    public QName getServiceName() {
        return serviceName;
    }

    public URL getWSDLDocumentLocation() {
        return wsdlURL;
    }

    public void setExecutor(Executor e) {
        this.executor = e;
    }

    public void setHandlerResolver(HandlerResolver hr) {
        handlerResolver = hr;
    }

    public Bus getBus() {
        return bus;
    }

    protected <T> T createPort(QName portName, Class<T> serviceEndpointInterface) {
        LOG.log(Level.FINE, "creating port for portName", portName);
        LOG.log(Level.FINE, "endpoint interface:", serviceEndpointInterface);

        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(bus);
        serviceFactory.setServiceName(serviceName);
        serviceFactory.setServiceClass(serviceEndpointInterface);

        if (wsdlURL != null) {
            serviceFactory.setWsdlURL(wsdlURL);
        }

        Service service = serviceFactory.create();
        configureObject(service);

        QName pn = portName;
        ServiceInfo si = service.getServiceInfo();

        EndpointInfo ei = null;
        if (portName == null) {
            if (1 == si.getEndpoints().size()) {
                ei = si.getEndpoints().iterator().next();
                pn = new QName(service.getName().getNamespaceURI(), ei.getName().getLocalPart());
            }
        } else {
            // first chech the endpointInfo from portInfos
            PortInfo portInfo = portInfos.get(portName);
            if (null != portInfo) {
                try {
                    ei = createEndpointInfo(serviceFactory, portName, portInfo);
                } catch (BusException e) {
                    throw new WebServiceException(e);
                }
            } else {
                ei = si.getEndpoint(portName);
            }
        }
        if (null == pn) {
            throw new WebServiceException(BUNDLE.getString("COULD_NOT_DETERMINE_PORT"));
        }

        JaxWsEndpointImpl jaxwsEndpoint;
        try {
            jaxwsEndpoint = new JaxWsEndpointImpl(bus, service, ei);
        } catch (EndpointException e) {
            throw new WebServiceException(e);
        }
        configureObject(jaxwsEndpoint);

        Client client = new ClientImpl(bus, jaxwsEndpoint);

        InvocationHandler ih = new EndpointInvocationHandler(client, jaxwsEndpoint.getJaxwsBinding());

        // configuration stuff
        // createHandlerChainForBinding(serviceEndpointInterface, portName,
        // endpointHandler.getBinding());

        Object obj = Proxy
            .newProxyInstance(serviceEndpointInterface.getClassLoader(),
                              new Class[] {serviceEndpointInterface, BindingProvider.class}, ih);

        LOG.log(Level.FINE, "created proxy", obj);

        ports.add(pn);
        return serviceEndpointInterface.cast(obj);
    }

    private EndpointInfo createEndpointInfo(AbstractServiceFactoryBean serviceFactory, 
                                            QName portName,
                                            PortInfo portInfo) throws BusException {
        EndpointInfo ei = null;
        String address = portInfo.getAddress();

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm.getDestinationFactoryForUri(portInfo.getAddress());

        String transportId = df.getTransportIds().get(0);
        String bindingUri = portInfo.getBindingUri();

        // TODO: Replace with discovery mechanism!!
        AbstractBindingInfoFactoryBean bindingFactory = null;
        if (bindingUri.equals(XMLConstants.NS_XML_FORMAT)) {
            bindingFactory = new XMLBindingInfoFactoryBean();
        } else if ("http://schemas.xmlsoap.org/soap/".equals(bindingUri)) {
            JaxWsSoapBindingInfoFactoryBean soapBindingFactory = new JaxWsSoapBindingInfoFactoryBean();
            soapBindingFactory.setTransportURI(transportId);
            bindingFactory = soapBindingFactory;
        } else {
            bindingFactory = new XMLBindingInfoFactoryBean();
        }

        bindingFactory.setServiceFactory(serviceFactory);

        BindingInfo bindingInfo = bindingFactory.create();
        Service service = serviceFactory.getService();
        service.getServiceInfo().addBinding(bindingInfo);

        // TODO we may need to get the transportURI from Address
        ei = new EndpointInfo(service.getServiceInfo(), transportId);
        ei.setName(portName);
        ei.setAddress(address);
        ei.setBinding(bindingInfo);

        service.getServiceInfo().addEndpoint(ei);
        return ei;
    }

    private void configureObject(Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(instance);
        }
    }

    private PortInfo getPortInfo(QName portName) {
        // TODO if the portName null ?
        return portInfos.get(portName);
    }

    static class PortInfo {
        private String bindingUri;
        private String address;

        public PortInfo(String bindingUri, String address2) {
            this.bindingUri = bindingUri;
            this.address = address2;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getBindingUri() {
            return bindingUri;
        }

        public void setBindingUri(String bindingUri) {
            this.bindingUri = bindingUri;
        }
    }

}

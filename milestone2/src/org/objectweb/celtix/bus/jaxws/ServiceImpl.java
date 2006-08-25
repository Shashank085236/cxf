package org.objectweb.celtix.bus.jaxws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.rmi.Remote;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebService;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.ServiceDelegate;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bus.handlers.HandlerChainBuilder;
import org.objectweb.celtix.bus.handlers.HandlerResolverImpl;
import org.objectweb.celtix.bus.handlers.PortInfoImpl;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public class ServiceImpl extends ServiceDelegate {

    private static final Logger LOG = Logger.getLogger(ServiceImpl.class.getName());
    
    private URL wsdlLocation;
    private QName serviceName;
    private List<QName> endpointList;
    private final Bus bus;
    private Configuration configuration;
    private HandlerResolver handlerResolver; 
    
    /**
     * Create a new Service.
     * @throws WebServiceException If there is an exception creating Service.
     */
    public ServiceImpl(Bus b, URL location, QName name, Class<?> si) {
        bus = b;
        configuration = new ServiceConfiguration(bus, name);
        wsdlLocation = location;
        serviceName = name;
        endpointList = new Vector<QName>();
        handlerResolver = new HandlerResolverImpl(configuration);
    }
    
    public void createPort(QName portName, URI bindingId, String endpointAddress) {
        throw new UnsupportedOperationException("addPort not yet supported");        
    }   
    
    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
        if (portName == null) {
            throw new WebServiceException("No endpoint specified.");
        }
        
        return createPort(portName, serviceEndpointInterface);
    }

    public <T> T getPort(Class<T> serviceEndpointInterface) {
        return createPort(null, serviceEndpointInterface);
    }

    public <T> Dispatch<T> createDispatch(QName portName, Class<T> serviceEndpointInterface, 
                                    Service.Mode mode) {
        return null;
    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Service.Mode mode) {
        return null;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public Iterator<QName> getPorts() {
        return endpointList.iterator();
    }

    public URL getWSDLDocumentLocation() {
        return wsdlLocation;
    }

    protected <T> T createPort(QName portName, Class<T> serviceEndpointInterface) {

        LOG.log(Level.FINE, "creating port for portName", portName);
        LOG.log(Level.FINE, "endpoint interface:", serviceEndpointInterface);
                
        //Assuming Annotation is Present
        javax.jws.WebService wsAnnotation = serviceEndpointInterface.getAnnotation(WebService.class);

        if (wsdlLocation == null) {
            wsdlLocation = getWsdlLocation(wsAnnotation);
        }

        if (wsdlLocation == null) {
            throw new WebServiceException("No wsdl url specified");
        }
        
        if (serviceName == null) {
            serviceName = getServiceName(wsAnnotation);
        }
        
        EndpointReferenceType ref = EndpointReferenceUtils.getEndpointReference(wsdlLocation, 
                serviceName, portName.getLocalPart());
        
        EndpointInvocationHandler endpointHandler = 
                new EndpointInvocationHandler(bus, ref, serviceEndpointInterface);
        
        createHandlerChainForBinding(portName, endpointHandler.getBinding());
        
        Object obj = Proxy.newProxyInstance(serviceEndpointInterface.getClassLoader(),
                                            new Class[] {serviceEndpointInterface, Remote.class, 
                                                         BindingProvider.class},
                                            (InvocationHandler) endpointHandler);
        
        LOG.log(Level.FINE, "created proxy", obj);
                
        endpointList.add(portName);
        
        return serviceEndpointInterface.cast(obj);
    }

    
    private void createHandlerChainForBinding(QName portName, Binding binding) {

        assert handlerResolver != null; 
        PortInfoImpl portInfo = new PortInfoImpl(serviceName, portName, null);
        List<Handler> handlers = handlerResolver.getHandlerChain(portInfo);
        HandlerChainBuilder handlerChainBuilder = new HandlerChainBuilder(); 
        handlers = handlerChainBuilder.sortHandlers(handlers);
        binding.setHandlerChain(handlers);
    }
    
    private URL getWsdlLocation(WebService wsAnnotation) {

        URL url = null;
        if (wsAnnotation != null) {
            try { 
                url = new URL(wsAnnotation.wsdlLocation());
            } catch (java.net.MalformedURLException mue) {
                mue.printStackTrace();
            }
        }
        
        return url;
    }
    
    private QName getServiceName(WebService wsAnnotation) {

        QName serviceQName = null;        
        if (wsAnnotation != null) {
            serviceQName = new QName(wsAnnotation.targetNamespace(), wsAnnotation.serviceName());
        }
        
        return serviceQName;
    }

    @Override
    public void addPort(QName arg0, URI arg1, String arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HandlerResolver getHandlerResolver() {
        return handlerResolver;
    }

    @Override
    public void setHandlerResolver(HandlerResolver hr) {
        handlerResolver = hr;
    }

    @Override
    public Executor getExecutor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExecutor(Executor arg0) {
        // TODO Auto-generated method stub
        
    }
   
}


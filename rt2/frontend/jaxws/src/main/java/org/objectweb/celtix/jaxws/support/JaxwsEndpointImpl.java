package org.objectweb.celtix.jaxws.support;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Binding;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bindings.soap2.SoapBinding;
import org.objectweb.celtix.endpoint.EndpointImpl;
import org.objectweb.celtix.interceptors.Interceptor;
import org.objectweb.celtix.jaxb.JAXBDataReaderFactory;
import org.objectweb.celtix.jaxb.JAXBDataWriterFactory;
import org.objectweb.celtix.jaxws.bindings.BindingImpl;
import org.objectweb.celtix.jaxws.bindings.soap.SOAPBindingImpl;
import org.objectweb.celtix.jaxws.handlers.LogicalHandlerInterceptor;
import org.objectweb.celtix.jaxws.handlers.StreamHandlerInterceptor;
import org.objectweb.celtix.jaxws.handlers.soap.SOAPHandlerInterceptor;
import org.objectweb.celtix.service.Service;
import org.objectweb.celtix.service.model.EndpointInfo;
import org.objectweb.celtix.service.model.InterfaceInfo;
import org.objectweb.celtix.service.model.OperationInfo;

/**
 * A JAX-WS specific implementation of the Celtix {@link Endpoint} interface.
 * Extends the interceptor provider functionality of its base class by adding 
 * interceptors in which to execute the JAX-WS handlers.
 * Creates and owns an implementation of {@link Binding} in addition to the
 * Celtix {@link org.objectweb.celtix.bindings.Binding}. 
 *
 */
public class JaxwsEndpointImpl extends EndpointImpl {

    public static final String JAXWS_DATAREADER_FACTORY = 
        "org.objectweb.celtix.frontends.jaxws.databinding.reader.factory";
    public static final String JAXWS_DATAWRITER_FACTORY = 
        "org.objectweb.celtix.frontends.jaxws.databinding.writer.factory";
    
    private List<Interceptor> handlerInterceptors; 
    private Binding binding;
    
    @SuppressWarnings("unchecked")
    public JaxwsEndpointImpl(Bus bus, Service s, EndpointInfo ei) {
        super(bus, s, ei);
        
        registerFrontendDatabindings();
        
        createJaxwsBinding();
        
        handlerInterceptors = new ArrayList<Interceptor>();
        handlerInterceptors.add(new LogicalHandlerInterceptor());
        if (getBinding() instanceof SoapBinding) {
            handlerInterceptors.add(new SOAPHandlerInterceptor(binding));
        } else {
             // TODO: what for non soap bindings?
        }
        handlerInterceptors.add(new StreamHandlerInterceptor());
    }
    
    public List<Interceptor> getFaultInterceptors() {
        List<Interceptor> fault = super.getOutInterceptors();
        fault.addAll(handlerInterceptors);
        return fault;
    }
    
    public List<Interceptor> getInInterceptors() {
        List<Interceptor> in = super.getInInterceptors();
        in.addAll(handlerInterceptors);
        return in;
    }
    
    
    public List<Interceptor> getOutInterceptors() {
        List<Interceptor> out = super.getOutInterceptors();
        out.addAll(handlerInterceptors);
        return out;
    }

    void registerFrontendDatabindings() {
        InterfaceInfo ii = getService().getServiceInfo().getInterface();
        for (OperationInfo oi : ii.getOperations()) {
            if (null == oi.getProperty(JAXWS_DATAREADER_FACTORY)) {
                oi.setProperty(JAXWS_DATAREADER_FACTORY, new JAXBDataReaderFactory());
            }
            if (null == oi.getProperty(JAXWS_DATAWRITER_FACTORY)) {
                oi.setProperty(JAXWS_DATAWRITER_FACTORY, new JAXBDataWriterFactory());
            }    
        }
    }
    
    public Binding getJaxwsBinding() {
        return binding;
    }
    
    void createJaxwsBinding() {
        if (getBinding() instanceof SoapBinding) {
            binding = new SOAPBindingImpl((SoapBinding)getBinding());
        } else {
            binding = new BindingImpl();
        }
    }
    
    

}

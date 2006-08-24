package org.apache.cxf.jaxws.support;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Binding;

import org.apache.cxf.Bus;
import org.apache.cxf.bindings.soap2.SoapBinding;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptors.Interceptor;
import org.apache.cxf.jaxws.bindings.BindingImpl;
import org.apache.cxf.jaxws.bindings.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.handlers.LogicalHandlerInterceptor;
import org.apache.cxf.jaxws.handlers.StreamHandlerInterceptor;
import org.apache.cxf.jaxws.handlers.soap.SOAPHandlerInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassInInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassOutInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * A JAX-WS specific implementation of the CXF {@link Endpoint} interface.
 * Extends the interceptor provider functionality of its base class by adding 
 * interceptors in which to execute the JAX-WS handlers.
 * Creates and owns an implementation of {@link Binding} in addition to the
 * CXF {@link org.apache.cxf.bindings.Binding}. 
 *
 */
public class JaxwsEndpointImpl extends EndpointImpl {

    private Binding binding;
    
    public JaxwsEndpointImpl(Bus bus, Service s, EndpointInfo ei) {
        super(bus, s, ei);

        createJaxwsBinding();
        
        List<Interceptor> handlerInterceptors;
                
        handlerInterceptors = new ArrayList<Interceptor>();
        handlerInterceptors.add(new LogicalHandlerInterceptor(binding));
        if (getBinding() instanceof SoapBinding) {
            handlerInterceptors.add(new SOAPHandlerInterceptor(binding));
        } else {
             // TODO: what for non soap bindings?
        }
        handlerInterceptors.add(new StreamHandlerInterceptor(binding));
        
        List<Interceptor> fault = super.getFaultInterceptors();
        fault.addAll(handlerInterceptors);
        List<Interceptor> in = super.getInInterceptors();
        in.addAll(handlerInterceptors);
        in.add(new WrapperClassInInterceptor());
        
        List<Interceptor> out = super.getOutInterceptors();
        out.addAll(handlerInterceptors);
        out.add(new WrapperClassOutInterceptor());
    }
    
    public Binding getJaxwsBinding() {
        return binding;
    }
    
    final void createJaxwsBinding() {
        if (getBinding() instanceof SoapBinding) {
            binding = new SOAPBindingImpl((SoapBinding)getBinding());
        } else {
            binding = new BindingImpl();
        }
    }
}

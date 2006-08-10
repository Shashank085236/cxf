package org.objectweb.celtix.endpoint;

import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceException;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bindings.Binding;
import org.objectweb.celtix.bindings.BindingFactory;
import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.interceptors.AbstractBasicInterceptorProvider;
import org.objectweb.celtix.service.Service;
import org.objectweb.celtix.service.model.BindingInfo;
import org.objectweb.celtix.service.model.EndpointInfo;

public class EndpointImpl extends AbstractBasicInterceptorProvider implements Endpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(EndpointImpl.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Service service;
    private Binding binding;
    private EndpointInfo endpointInfo;
    private Executor executor;
    private Bus bus;
    
    public EndpointImpl(Bus bus, Service s, EndpointInfo ei) {
        this.bus = bus;
        service = s;
        endpointInfo = ei;
        createBinding(endpointInfo.getBinding());
    }

    public EndpointInfo getEndpointInfo() {
        return endpointInfo;
    }

    public Service getService() {
        return service;
    }

    public Binding getBinding() {
        return binding;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor e) {
        executor = e;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    final void createBinding(BindingInfo bi) {
        String namespace = bi.getBindingId();
        BindingFactory bf = null;
        try {
            bf = bus.getBindingManager().getBindingFactory(namespace);
            binding = bf.createBinding(bi);
        } catch (BusException ex) {
            throw new WebServiceException(ex);
        }
        if (null == bf) {
            Message msg = new Message("NO_BINDING_FACTORY", BUNDLE, namespace);
            throw new WebServiceException(msg.toString());
        }
    }

}

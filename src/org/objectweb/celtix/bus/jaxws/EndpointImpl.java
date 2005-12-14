package org.objectweb.celtix.bus.jaxws;






import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bindings.BindingFactory;
import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.ServerBinding;
import org.objectweb.celtix.bindings.ServerBindingEndpointCallback;
import org.objectweb.celtix.bus.handlers.HandlerChainBuilder;
import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.injection.ResourceInjector;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public final class EndpointImpl extends javax.xml.ws.Endpoint
    implements ServerBindingEndpointCallback {

    private static final Logger LOG = LogUtils.getL7dLogger(EndpointImpl.class);

    private final Bus bus;
    private final Configuration configuration;
    
    private final Object implementor;
    private final EndpointReferenceType reference;
    private ServerBinding serverBinding;
    private boolean published;
    private List<Source> metadata;
    private Executor executor;
    private JAXBContext context;

    public EndpointImpl(Bus b, Object impl, String bindingId) {

        bus = b;
        implementor = impl;
        
        try {
            context = JAXBEncoderDecoder.createJAXBContextForClass(impl.getClass());
        } catch (JAXBException ex1) {
            // TODO Auto-generated catch block
            ex1.printStackTrace();
            context = null;
        }
                
        reference = EndpointReferenceUtils.getEndpointReference(bus.getWSDLManager(), implementor);
        configuration = new EndpointConfiguration(bus, EndpointReferenceUtils.getServiceName(reference));
        try {           
            if (null == bindingId) {
                Port port = EndpointReferenceUtils.getPort(bus.getWSDLManager(), reference);
                ExtensibilityElement el = (ExtensibilityElement)port.getExtensibilityElements().get(0);
                bindingId = el.getElementType().getNamespaceURI();
            }
            serverBinding = createServerBinding(bindingId);
            configureHandlers();            
            injectResources();
        } catch (Exception ex) {
            if (ex instanceof WebServiceException) { 
                throw (WebServiceException)ex; 
            }
            throw new WebServiceException("Creation of Endpoint failed", ex);
        }
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#getBinding()
     */
    public Binding getBinding() {
        return serverBinding.getBinding();
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#getImplementor()
     */
    public Object getImplementor() {
        return implementor;
    }

    /*
     * (non-Javadoc) Not sure if this is meant to return the effective metadata -
     * or the default metadata previously assigned with
     * javax.xml.ws.Endpoint#setHandlerChain()
     * 
     * @see javax.xml.ws.Endpoint#getMetadata()
     */
    public List<Source> getMetadata() {
        return metadata;
    }

    /**
     * documented but not yet implemented in RI
     */

    public Executor getExecutor() {
        return executor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#isPublished()
     */
    public boolean isPublished() {
        return published;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#publish(java.lang.Object)
     */
    public void publish(Object serverContext) {
        if (isPublished()) {
            LOG.warning("ENDPOINT_ALREADY_PUBLISHED_MSG");
        }
        if (!isContextBindingCompatible(serverContext)) {
            throw new IllegalArgumentException(
                                               new BusException(new Message("BINDING_INCOMPATIBLE_CONTEXT_EXC"
                                                                            , LOG)));
        }

        // apply all changes to configuration and metadata and (re-)activate

        String address = getAddressFromContext(serverContext);
        publish(address);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#publish(java.lang.String)
     */
    public void publish(String address) {
        if (isPublished()) {
            LOG.warning("ENDPOINT_ALREADY_PUBLISHED_MSG");
        }
        if (!serverBinding.isCompatibleWithAddress(address)) {
            throw 
                new IllegalArgumentException(
                                             new BusException(new Message("BINDING_INCOMPATIBLE_ADDRESS_EXC",
                                                                          LOG)));
        }
        doPublish(address);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#setHandlerChain(java.util.List)
     */
    public void setHandlerChain(List<Handler> h) {        
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#setMetadata(java.util.List)
     */
    public void setMetadata(List<Source> m) {
        metadata = m;
    }

    /**
     * documented but not yet implemented in RI
     */
    public void setExecutor(Executor ex) {
        executor = ex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Endpoint#stop()
     */
    public void stop() {
        if (!isPublished()) {
            LOG.warning("ENDPOINT_INACTIVE_MSG");
        }
        published = false;
    }

    public Bus getBus() {
        return bus;
    }
    
    public ServerBinding getServerBinding() {
        return serverBinding;
    }
    
    public EndpointReferenceType getEndpointReferenceType() {
        return reference;
    }
    
    ServerBinding createServerBinding(String bindingId) throws BusException, WSDLException, IOException {

        BindingFactory factory = bus.getBindingManager().getBindingFactory(bindingId);
        if (null == factory) {
            throw new BusException(new Message("BINDING_FACTORY_MISSING_EXC", LOG, bindingId));
        }
        ServerBinding bindingImpl = factory.createServerBinding(reference, this, this);
        assert null != bindingImpl;
        return bindingImpl;

    }

    String getAddressFromContext(Object ctx) {
        return null;
    }

    boolean isContextBindingCompatible(Object ctx) {
        return true;
    }

    void doPublish(String address) {
        EndpointReferenceUtils.setAddress(reference, address);
        try {
            serverBinding.activate();
            published = true;
        } catch (WSDLException ex) {
            LOG.log(Level.SEVERE, "SERVER_BINDING_ACTIVATION_FAILURE_MSG", ex);
            throw new WebServiceException(ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "SERVER_BINDING_ACTIVATION_FAILURE_MSG", ex);
            throw new WebServiceException(ex);
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperties(Map<String, Object> arg0) {
        // TODO Auto-generated method stub
        
    }


    /** 
     * inject resources into servant.  The resources are injected
     * according to @Resource annotations.  See JSR 250 for more
     * information.
     */
    private void injectResources() { 
        ResourceInjector injector = new ResourceInjector(bus.getResourceManager());
        injector.inject(implementor);
    }


    /** 
     * Obtain handler chain from configuration first. If none is specified, 
     * default to the chain configured in the code, i.e. in annotations.
     *
     */
    private void configureHandlers() { 
        
        LOG.fine("loading handler chain for endpoint"); 
        HandlerChainBuilder builder = new HandlerChainBuilder();
        List<Handler> chain = builder.buildHandlerChainFromConfiguration(configuration, "handlerChain");
        if (null == chain) {
            chain = builder.buildHandlerChainFor(implementor.getClass()); 
        }
        serverBinding.getBinding().setHandlerChain(chain); 
    }

    public DataBindingCallback createDataBindingCallback(ObjectMessageContext objContext,
                                                         DataBindingCallback.Mode mode) {
        return new JAXBDataBindingCallback(objContext.getMethod(),
                                           mode,
                                           context);
    }

    public Method getMethod(Endpoint endpoint, QName operationName) {
        return EndpointUtils.getMethod(endpoint, operationName);
    }

    public ServiceMode getServiceMode(Endpoint endpoint) {
        return EndpointUtils.getServiceMode(endpoint);
    } 
}

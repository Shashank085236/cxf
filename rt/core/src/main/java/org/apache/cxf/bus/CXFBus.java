package org.apache.cxf.bus;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configuration;
import org.apache.cxf.configuration.ConfigurationBuilder;
import org.apache.cxf.configuration.impl.ConfigurationBuilderImpl;
import org.apache.cxf.extension.ExtensionManagerImpl;
import org.apache.cxf.interceptors.Interceptor;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.PropertiesResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;

public class CXFBus implements Bus {
    
    public static final String BUS_PROPERTY_NAME = "bus";
    
    private static final String BUS_EXTENSION_RESOURCE = "META-INF/bus-extensions.xml";
    
    enum State { INITIAL, RUNNING, SHUTDOWN };
    
    
    
    
    
    private List<Interceptor> inInterceptors;
    private List<Interceptor> outInterceptors;
    private List<Interceptor> faultInterceptors;
    private Map<Class, Object> extensions;
    private Configuration configuration;
    private String id;
    private State state;
    
    
    
    protected CXFBus() {
        this(new HashMap<Class, Object>());
    }

    protected CXFBus(Map<Class, Object> e) {
        this(e, null);
    }
    
    protected CXFBus(Map<Class, Object> e, Map<String, Object> properties) {
        
        extensions = e;
     
        BusConfigurationHelper helper = new BusConfigurationHelper();
        
        id = helper.getBusId(properties);
 
        ConfigurationBuilder builder = (ConfigurationBuilder)extensions.get(ConfigurationBuilder.class);
        if (null == builder) {
            builder = new ConfigurationBuilderImpl();
            extensions.put(ConfigurationBuilder.class, builder);
        }
        configuration = helper.getConfiguration(builder, id);
        
        ResourceManager resourceManager = new DefaultResourceManager();
        
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        
        properties.put(BusConfigurationHelper.BUS_ID_PROPERTY, BUS_PROPERTY_NAME);
        properties.put(BUS_PROPERTY_NAME, this);
        
        ResourceResolver propertiesResolver = new PropertiesResolver(properties);
        resourceManager.addResourceResolver(propertiesResolver);
        
        ResourceResolver busResolver = new SinglePropertyResolver(BUS_PROPERTY_NAME, this);
        resourceManager.addResourceResolver(busResolver);
   
        new ExtensionManagerImpl(BUS_EXTENSION_RESOURCE, 
                                                    Thread.currentThread().getContextClassLoader(),
                                                    extensions,
                                                    resourceManager);
        
        state = State.INITIAL;

    }
    
    public List<Interceptor> getFaultInterceptors() {
        return faultInterceptors;
    }

    public List<Interceptor> getInInterceptors() {
        return inInterceptors;
    }    

    public List<Interceptor> getOutInterceptors() {
        return outInterceptors;
    }
      
    public <T> T getExtension(Class<T> extensionType) {
        Object obj = extensions.get(extensionType);
        if (null != obj) {
            return extensionType.cast(obj);
        }
        return null;
    }

    public <T> void setExtension(T extension, Class<T> extensionType) {
        extensions.put(extensionType, extension);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getId() {
        return id;
    }

    public void run() {
        synchronized (this) {
            if (state == State.RUNNING) {
                // REVISIT
                return;
            }
            state = State.RUNNING;

            while (state == State.RUNNING) {

                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore;
                }
            }
        }
    }

    public void shutdown(boolean wait) {
        // TODO: invoke PreDestroy on all resources
        synchronized (this) {
            state = State.SHUTDOWN;
            notifyAll();
        }
    }
    
    protected State getState() {
        return state;
    }
    
    
    
}

package org.objectweb.celtix.bus.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bus.jaxws.configuration.types.HandlerChainType;
import org.objectweb.celtix.configuration.CompoundName;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationBuilder;
import org.objectweb.celtix.handlers.HandlerChainBuilder;

public class HandlerResolverImpl implements HandlerResolver {
    public static final String PORT_CONFIGURATION_URI =
        "http://celtix.objectweb.org/bus/jaxws/port-config";

    private final Map<PortInfo, List<Handler>> handlerMap = new HashMap<PortInfo, List<Handler>>();
    private Bus bus;
    private QName service;
    private ClassLoader serviceEndpointInterfaceClassLoader;

    public HandlerResolverImpl(Bus b, QName s) {
        bus = b;
        service = s;
    }

    public List<Handler> getHandlerChain(PortInfo portInfo) {

        List<Handler> handlerChain = handlerMap.get(portInfo);
        if (handlerChain == null) {
            handlerChain = createHandlerChain(portInfo);
            handlerMap.put(portInfo, handlerChain);
        }
        return handlerChain;
    }

    private List<Handler> createHandlerChain(PortInfo portInfo) {

        List<Handler> chain = null;
        Configuration portConfiguration = null;
        String id = portInfo.getPortName().getLocalPart();
        if (service != null) {
            id = service.toString() + "/" + portInfo.getPortName().getLocalPart();
        }
        if (bus != null) {
            CompoundName cn = new CompoundName(bus.getConfiguration().getId(), id);
            ConfigurationBuilder cb = bus.getConfigurationBuilder();
            portConfiguration = cb.getConfiguration(PORT_CONFIGURATION_URI, cn);
        }
       
        if (null != portConfiguration) {
            HandlerChainBuilder builder = new HandlerChainBuilder();
            builder.setHandlerClassLoader(serviceEndpointInterfaceClassLoader);
            HandlerChainType hc = (HandlerChainType)portConfiguration.getObject("handlerChain");
            chain = builder.buildHandlerChainFromConfiguration(hc);
        }
        if (null == chain) {
            chain = new ArrayList<Handler>();
        }
        return chain;
    }

    public ClassLoader getServiceEndpointInterfaceClassLoader() {
        return serviceEndpointInterfaceClassLoader;
    }

    public void setServiceEndpointInterfaceClassLoader(ClassLoader classLoader) {
        this.serviceEndpointInterfaceClassLoader = classLoader;
    }
}

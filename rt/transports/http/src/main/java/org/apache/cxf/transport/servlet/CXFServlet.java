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

package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;

/**
 * A Servlet which supports loading of JAX-WS endpoints from an
 * XML file and handling requests for endpoints created via other means
 * such as Spring beans, or the Java API. All requests are passed on
 * to the {@link ServletController}.
 *
 */
public class CXFServlet extends HttpServlet {
    static final String ADDRESS_PERFIX = "http://localhost/services";
    static final Map<String, WeakReference<Bus>> BUS_MAP = new Hashtable<String, WeakReference<Bus>>();
    static final Logger LOG = Logger.getLogger(CXFServlet.class.getName());
    
    private Bus bus;
    private ServletTransportFactory servletTransportFactory;
    private ServletController controller;
    private GenericApplicationContext childCtx;

    public ServletController createServletController() {
        return new ServletController(servletTransportFactory, this.getServletContext(), this);
    }

    public ServletController getController() {
        return controller;
    }
    
    public Bus getBus() {
        return bus;
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        
        String busid = servletConfig.getInitParameter("bus.id");
        if (null != busid) {
            WeakReference<Bus> ref = BUS_MAP.get(busid);
            if (null != ref) {
                bus = ref.get();
            }
        }
        
        String springCls = "org.springframework.context.ApplicationContext";
        try {
            ClassLoaderUtils.loadClass(springCls, getClass());
            loadSpringBus(servletConfig);
        } catch (ClassNotFoundException e) {
            loadBusNoConfig(servletConfig);
        }
            
            
        if (null != busid) {
            BUS_MAP.put(busid, new WeakReference<Bus>(bus));
        }
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }
    
    private void loadBusNoConfig(ServletConfig servletConfig) throws ServletException {
        if (bus == null) {
            bus = BusFactory.getDefaultBus();
        }
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(
                                               servletConfig.getServletContext()));
        
        // Set up the ServletController
        controller = createServletController();

        replaceDestionFactory();
        
    }

    private void loadSpringBus(ServletConfig servletConfig) throws ServletException {
        // try to pull an existing ApplicationContext out of the
        // ServletContext
        ServletContext svCtx = getServletContext();

        // Spring 1.x
        ApplicationContext ctx = (ApplicationContext)svCtx
            .getAttribute("interface org.springframework.web.context.WebApplicationContext.ROOT");

        // Spring 2.0
        if (ctx == null) {
            ctx = (ApplicationContext)svCtx
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
        }
        
        // This constructor works whether there is a context or not
        // If the ctx is null, we need to load the cxf-servlet as default
        if (ctx == null) {
            bus = new SpringBusFactory().createBus("/META-INF/cxf/cxf-servlet.xml");
        } else {
            bus = new SpringBusFactory(ctx).createBus();
        }
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(
                                               servletConfig.getServletContext()));
        
        replaceDestionFactory();

        // Set up the ServletController
        controller = createServletController();
        
        // build endpoints from the web.xml or a config file
        loadAdditionalConfig(ctx, servletConfig);
    }

    protected void loadAdditionalConfig(ApplicationContext ctx, 
                                        ServletConfig servletConfig) throws ServletException {
        String location = servletConfig.getInitParameter("config-location");
        if (location == null) {
            location = "/WEB-INF/cxf-servlet.xml";
        }
        InputStream is = null;
        try {
            is = servletConfig.getServletContext().getResourceAsStream(location);
            
            if (is == null || is.available() == -1) {
                URIResolver resolver = new URIResolver(location);

                if (resolver.isResolved()) {
                    is = resolver.getInputStream();
                }
            }
        } catch (IOException e) {
            //throw new ServletException(e);
        }
        
        if (is != null) {
            childCtx = new GenericApplicationContext(ctx);
            XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(childCtx);
            reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
            reader.loadBeanDefinitions(new InputStreamResource(is, location));
            
            childCtx.refresh();
        } 
    }

    /**
     * @return
     */
    protected DestinationFactory createServletTransportFactory() {
        if (servletTransportFactory == null) {
            servletTransportFactory = new ServletTransportFactory(bus);
        }
        return servletTransportFactory;
    }

    private void registerTransport(DestinationFactory factory, String namespace) {
        bus.getExtension(DestinationFactoryManager.class).registerDestinationFactory(namespace, factory);
    }

    private void replaceDestionFactory() throws ServletException {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            DestinationFactory df = dfm
                .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof ServletTransportFactory) {
                servletTransportFactory = (ServletTransportFactory)df;
                return;
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }

        DestinationFactory factory = createServletTransportFactory();

        registerTransport(factory, "http://schemas.xmlsoap.org/wsdl/soap/http");
        registerTransport(factory, "http://schemas.xmlsoap.org/soap/http");
        registerTransport(factory, "http://www.w3.org/2003/05/soap/bindings/HTTP/");
        registerTransport(factory, "http://schemas.xmlsoap.org/wsdl/http/");
        registerTransport(factory, "http://cxf.apache.org/transports/http/configuration");
        registerTransport(factory, "http://cxf.apache.org/bindings/xformat");
    }

    public void destroy() {
        childCtx.destroy();
        
        String s = bus.getId();
        BUS_MAP.remove(s);
        bus.shutdown(true);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        invoke(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        invoke(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        invoke(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        invoke(request, response);
    }
    
    private  void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            BusFactory.setThreadDefaultBus(getBus());
            controller.invoke(request, response);
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }
}

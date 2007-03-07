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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.EndpointPublisher;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.springframework.context.ApplicationContext;

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
    static final String JAXWS_ENDPOINT_FACTORY_BEAN = "org.apache.cxf.jaxws.spring.EndpointFactoryBean";
    
    private Bus bus;
    private ServletTransportFactory servletTransportFactory;
    private ServletController controller;

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
        
        String busid = servletConfig.getInitParameter("bus.id");
        if (null != busid) {
            WeakReference<Bus> ref = BUS_MAP.get(busid);
            if (null != ref) {
                bus = ref.get();
            }
        }
        if (null == bus) {
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
            
            SpringBusFactory.setDefaultBus(bus);
            
            initEndpointsFromContext(ctx);
             
        }
        if (null != busid) {
            BUS_MAP.put(busid, new WeakReference<Bus>(bus));
        }
                
        replaceDestionFactory();

        // Set up the servlet as the default server side destination factory
        controller = createServletController();

        // build endpoints from the web.xml or a config file
        buildEndpoints(servletConfig);
        
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver());
    }
    
    // Need to get to know all frontend's endpoint information
    private void initEndpointsFromContext(ApplicationContext ctx) throws ServletException {
        Class factoryClass;        
        if (null != ctx) {                   
            try {
                factoryClass = Class.forName(JAXWS_ENDPOINT_FACTORY_BEAN);
            } catch (ClassNotFoundException ex) {
                throw new ServletException(ex);
            }
            String[] beans = ctx.getBeanNamesForType(factoryClass);
            if (null != beans) {
                for (String bean : beans) {
                    // just remove the & from the bean's name
                    ctx.getBean(bean.substring(1));
                }
            }
        }    
    }

    protected void buildEndpoints(ServletConfig servletConfig) throws ServletException {
        String location = servletConfig.getInitParameter("config-location");
        if (location == null) {
            location = "/WEB-INF/cxf-servlet.xml";
        }
                 
        InputStream ins = servletConfig.getServletContext().getResourceAsStream(location);

        if (ins == null) {
            try {
                URIResolver resolver = new URIResolver(location);

                if (resolver.isResolved()) {
                    ins = resolver.getInputStream();
                }
            } catch (IOException e) {
                // ignore
            }

        }

        if (ins != null) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(false);

            try {
                Document doc = builderFactory.newDocumentBuilder().parse(ins);
                Node nd = doc.getDocumentElement().getFirstChild();
                while (nd != null) {
                    if ("endpoint".equals(nd.getLocalName())) {
                        buildEndpoint(servletConfig, nd);
                    }
                    nd = nd.getNextSibling();
                }
            } catch (SAXException ex) {
                throw new ServletException(ex);
            } catch (IOException ex) {
                throw new ServletException(ex);
            } catch (ParserConfigurationException ex) {
                throw new ServletException(ex);
            }
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

    public void buildEndpoint(ServletConfig servletConfig, Node node) throws ServletException {
        Element el = (Element)node;
        String publisherName = el.getAttribute("publisher");
        String implName = el.getAttribute("implementation");
        String serviceName = el.getAttribute("service");
        String wsdlName = el.getAttribute("wsdl");
        String portName = el.getAttribute("port");
        String urlPat = el.getAttribute("url-pattern");

        buildEndpoint(publisherName, implName, serviceName, wsdlName, portName, urlPat);
    }

    public void buildEndpoint(String publisherName,
                              String implName, 
                              String serviceName, 
                              String wsdlName, 
                              String portName,
                              String urlPat) throws ServletException {

        try {
            URL url = null;
            
            if (!"".equals(wsdlName)) {
                
                try {
                    url = new URL(wsdlName);
                } catch (MalformedURLException e) {
                    //ignore
                }
                if (url == null) {
                    try {
                        url = getServletConfig().getServletContext().getResource("/" + wsdlName);
                    } catch (MalformedURLException e) {
                        //ignore
                    }
                }
                if (url == null) {
                    try {
                        url = getServletConfig().getServletContext().getResource(wsdlName);
                    } catch (MalformedURLException e) {
                        //ignore
                    }
                }                
            }
            if (null == publisherName || publisherName.length() == 0) {
                publisherName = "org.apache.cxf.jaxws.EndpointPublisherImpl";
            }
            
            EndpointPublisher publisher = (EndpointPublisher)Class.forName(publisherName).newInstance();
            
            publisher.buildEndpoint(bus, implName, serviceName, url, portName);

            LOG.info("publish the servcie to {context}/ " + (urlPat.charAt(0) == '/' ? "" : "/") + urlPat);
            
            // TODO we may need to get the url-pattern from servlet context
            publisher.publish(ADDRESS_PERFIX + (urlPat.charAt(0) == '/' ? "" : "/") + urlPat);
            
        } catch (BusException ex) {
            throw new ServletException(ex.getCause());        
        } catch (ClassNotFoundException ex) {
            throw new ServletException(ex);
        } catch (InstantiationException ex) {
            throw new ServletException(ex);
        } catch (IllegalAccessException ex) {
            throw new ServletException(ex);
        }
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
        String s = bus.getId();
        BUS_MAP.remove(s);
        bus.shutdown(true);
        //clean up the defaultBus
        SpringBusFactory.setDefaultBus(null);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        controller.invoke(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        controller.invoke(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        controller.invoke(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        controller.invoke(request, response);
    }
}

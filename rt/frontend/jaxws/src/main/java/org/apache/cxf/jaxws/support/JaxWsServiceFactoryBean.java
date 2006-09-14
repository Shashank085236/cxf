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

package org.apache.cxf.jaxws.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.wsdl.WSDLException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.BusException;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.interceptors.WrapperClassOutInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.ChainInitiationObserver;

public class JaxWsServiceFactoryBean extends ReflectionServiceFactoryBean {

    public static final String HOLDER = "messagepart.isholer";
    
    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsServiceFactoryBean.class);

    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();      

    Class<?> seiClass;

    JAXBDataBinding dataBinding;

    JaxwsImplementorInfo implInfo;

    public JaxWsServiceFactoryBean() {
        super();
        getServiceConfigurations().add(new JaxWsServiceConfiguration());
    }

    public JaxWsServiceFactoryBean(JaxwsImplementorInfo i) {
        this();
        this.implInfo = i;
    }

    public void activateEndpoints() throws IOException, WSDLException, BusException, EndpointException {
        Service service = getService();

        for (EndpointInfo ei : service.getServiceInfo().getEndpoints()) {
            activateEndpoint(service, ei);
        }
    }

    public void activateEndpoint(Service service, EndpointInfo ei) throws BusException, WSDLException,
                    IOException, EndpointException {
        JaxwsEndpointImpl ep = new JaxwsEndpointImpl(getBus(), service, ei);
        ChainInitiationObserver observer = new ChainInitiationObserver(ep, getBus());

        ServerImpl server = new ServerImpl(getBus(), ep, observer);

        server.start();
    }

    @Override
    protected void initializeWSDLOperation(InterfaceInfo intf, OperationInfo o, Method selected) {
        super.initializeWSDLOperation(intf, o, selected);

        // TODO: Check for request/responsewrapper annotations
        Class responseWrapper = getResponseWrapper(selected);
        if (responseWrapper != null) {
            o.setProperty(WrapperClassOutInterceptor.SINGLE_WRAPPED_PART, responseWrapper);
        }
        Class<?> requestWrapper = getRequestWrapper(selected);
        if (requestWrapper != null) {
            o.setProperty(WrappedInInterceptor.SINGLE_WRAPPED_PART, Boolean.TRUE);
        }        
        // rpc out-message-part-info class mapping
        JaxWsUtils.setClassInfo(o, selected, null);
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {

        super.setServiceClass(serviceClass);

        try {
            dataBinding = new JAXBDataBinding(serviceClass);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        setDataReaderFactory(dataBinding.getDataReaderFactory());    
        setDataWriterFactory(dataBinding.getDataWriterFactory());

        // update wsdl location
        // TODO: replace version in EndpointreferenceUtils?
        String wsdlLocation = null;
        WebService ws = serviceClass.getAnnotation(WebService.class);
        if (null == ws && implInfo != null && implInfo.isWebServiceProvider()) {
            WebServiceProvider wsProvider = implInfo.getWsProvider();
            wsdlLocation = wsProvider.wsdlLocation();
        } else {
            if (ws == null) {
                throw new WebServiceException(BUNDLE.getString("SEI_WITHOUT_WEBSERVICE_ANNOTATION_EXC"));
            }
            wsdlLocation = ws.wsdlLocation();
            String sei = ws.endpointInterface();
            if (null != sei && !"".equals(sei)) {
                try {
                    seiClass = ClassLoaderUtils.loadClass(sei, serviceClass);
                } catch (ClassNotFoundException ex) {
                    throw new WebServiceException(BUNDLE.getString("SEI_LOAD_FAILURE_MSG"), ex);
                }
                ws = seiClass.getAnnotation(WebService.class);
                if (null == ws) {
                    throw new WebServiceException(BUNDLE.getString("SEI_WITHOUT_WEBSERVICE_ANNOTATION_EXC"));
                }
            }
        }

        if (!StringUtils.isEmpty(wsdlLocation)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Setting wsdl location to:  " + ws.wsdlLocation());
            }

            URL url = null;
            try {
                url = new URL(wsdlLocation);
            } catch (MalformedURLException ex) {
                // LOG a warning instead of throw exception.
                // url =
                // implInfo.getImplementorClass().getResource(wsdlLocation);
                if (url == null) {
                    System.err.println("Can't resolve the wsdl location " + wsdlLocation);
                }
            }
            setWsdlURL(url);
        }

    }

    protected QName getServiceQName() {
        QName qname = null;
        try {
            qname = super.getServiceQName();
        } catch (Exception e) {
            qname = implInfo.getServiceName();
        }
        return qname;
    }

    protected QName getPortQName() {
        return implInfo.getEndpointName();
    }

    protected String getBindingType() {
        return implInfo.getBindingType();
    }
}

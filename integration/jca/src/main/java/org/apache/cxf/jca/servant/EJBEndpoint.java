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

package org.apache.cxf.jca.servant;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.ejb.EJBHome;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;


public class EJBEndpoint {
    
    private static final Logger LOG = LogUtils.getL7dLogger(EJBEndpoint.class);
    
    private EJBServantConfig config;
    
    private Context jndiContext;
    
    private EJBHome ejbHome;
    
    private String ejbServantBaseURL;
    
    public EJBEndpoint(EJBServantConfig ejbConfig) {
        this.config = ejbConfig;
    }
    
    public Server publish() throws Exception {
        jndiContext = new InitialContext();
        Object obj = jndiContext.lookup(config.getJNDIName());
        ejbHome = (EJBHome) PortableRemoteObject.narrow(obj, EJBHome.class);
        
        Class<?> interfaceClass = Class.forName(getServiceClassName());
        boolean isJaxws = isJaxWsServiceInterface(interfaceClass);
        ServerFactoryBean factory = isJaxws ? new JaxWsServerFactoryBean() : new ServerFactoryBean();
        factory.setServiceClass(interfaceClass);
        
        if (config.getWsdlURL() != null) {
            factory.getServiceFactory().setWsdlURL(config.getWsdlURL());
        }
        
        factory.setInvoker(new EJBInvoker(ejbHome));
        
        String baseAddress = isNotNull(getEjbServantBaseURL()) ? getEjbServantBaseURL() 
                                                               : getDefaultEJBServantBaseURL();
        String address = baseAddress + "/" + config.getJNDIName();
        factory.setAddress(address);
        
        LOG.info("Published EJB Endpoint of [" + config.getJNDIName() + "] at [" + address + "]");
        return factory.create();
    }
    
    public String getServiceClassName() throws Exception {
        String packageName = PackageUtils.parsePackageName(config.getServiceName().getNamespaceURI(), null);
        String interfaceName = packageName + "." 
                               + config.getJNDIName().substring(0, config.getJNDIName().length() - 4);
        return interfaceName;
    }
    
    public String getDefaultEJBServantBaseURL() throws Exception {
        String hostName = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostName = "localhost";
        }
        return "http://" + hostName + ":9999";
    }
    
    private boolean isJaxWsServiceInterface(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        if (null != cls.getAnnotation(WebService.class)) {
            return true;
        }
        return false;
    }

    public String getEjbServantBaseURL() {
        return ejbServantBaseURL;
    }

    public void setEjbServantBaseURL(String ejbServantBaseURL) {
        this.ejbServantBaseURL = ejbServantBaseURL;
    }
    
    private boolean isNotNull(String value) {
        if (value != null && !"".equals(value.trim())) {
            return true;
        }
        return false;
    }
    
}

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

package org.apache.cxf.configuration.impl;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfigurationException;
import org.apache.cxf.configuration.ConfigurationProvider;
import org.apache.cxf.resource.DefaultResourceManager;


public class DefaultConfigurationProviderFactory {
    
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultConfigurationProviderFactory.class);
    
    private static final String DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME = 
        "org.apache.cxf.configuration.impl.InMemoryProvider";
    private static final String DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY = 
        "org.apache.cxf.configuration.ConfigurationProviderClass";
    
    private static DefaultConfigurationProviderFactory theInstance;
    
    
    protected DefaultConfigurationProviderFactory() {
    }
    
    public static DefaultConfigurationProviderFactory getInstance() {
        if (null == theInstance) {
            theInstance = new DefaultConfigurationProviderFactory();
        }
        return theInstance;
    }
    
    public ConfigurationProvider createDefaultProvider() {
        
        String className = getDefaultProviderClassName();       
        Class<? extends ConfigurationProvider> providerClass;
        try {
            providerClass = Class.forName(className).asSubclass(ConfigurationProvider.class);
            return providerClass.newInstance();
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigurationException(new Message("DEFAULT_PROVIDER_INSTANTIATION_EXC", LOG), ex);
        } 
    }
    
    public String getDefaultProviderClassName() {
        
        String providerClass = null;
        
        // check system properties
        providerClass = System.getProperty(DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY);
        if (null != providerClass && !"".equals(providerClass)) {
            return providerClass;
        }
    
        // next, check for the services stuff in the jar file
        String serviceId = "META-INF/services/" + DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY;
        InputStream is = DefaultResourceManager.instance().getResourceAsStream(serviceId);
  
        if (is != null) {
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                providerClass = rd.readLine();
                rd.close();
            } catch (UnsupportedEncodingException ex) {
                //we're asking for UTF-8 which is supposed to always be supported,
                //but we'll throw a ConfigurationException anyway
                throw new ConfigurationException(new Message("DEFAULT_PROVIDER_INSTANTIATION_EXC", LOG), ex);
            } catch (IOException ex) {
                throw new ConfigurationException(new Message("DEFAULT_PROVIDER_INSTANTIATION_EXC", LOG), ex);
            }
        }
        
        if (providerClass != null && !"".equals(providerClass)) {
            return providerClass;
        }
        
        // fallback to hardcoced default
        
        return DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME;
    }
}

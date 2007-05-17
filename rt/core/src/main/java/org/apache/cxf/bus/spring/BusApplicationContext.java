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

package org.apache.cxf.bus.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class BusApplicationContext extends ClassPathXmlApplicationContext {
    
    private static final String DEFAULT_CXF_CFG_FILE = "META-INF/cxf/cxf.xml";
    private static final String DEFAULT_CXF_EXT_CFG_FILE = "classpath*:META-INF/cxf/cxf.extension";

    private static final Logger LOG = LogUtils.getL7dLogger(BusApplicationContext.class);
    
    private DefaultNamespaceHandlerResolver nsHandlerResolver;
    private boolean includeDefaults;
    private String cfgFile;
    private URL cfgFileURL;
    
    public BusApplicationContext(String cf, boolean include) {
        this(cf, include, null);
    }
    
    public BusApplicationContext(URL url, boolean include) {
        this(url, include, null);
    }

    public BusApplicationContext(String cf, boolean include, ApplicationContext parent) {
        super((String[])null, false, parent);
        cfgFile = cf;
        includeDefaults = include;
        refresh();
    }
    
    public BusApplicationContext(URL url, boolean include, ApplicationContext parent) {
        super((String[])null, false, parent);
        cfgFileURL = url;
        includeDefaults = include;
        refresh();
    }
    
    @Override
    protected Resource[] getConfigResources() {
  
        List<Resource> resources = new ArrayList<Resource>();
       
        if (includeDefaults) {
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(Thread
                    .currentThread().getContextClassLoader());
                
                Collections.addAll(resources, resolver.getResources(DEFAULT_CXF_CFG_FILE));

                Resource[] exts = resolver.getResources(DEFAULT_CXF_EXT_CFG_FILE);
                for (Resource r : exts) {
                    InputStream is = r.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = rd.readLine();
                    while (line != null) {
                        if (!"".equals(line)) {
                            resources.add(resolver.getResource(line));
                        }
                        line = rd.readLine();
                    }
                    is.close();
                }

            } catch (IOException ex) {
                // ignore  
            }  
        }
        
        if (null == cfgFile) {
            cfgFile = System.getProperty(Configurer.USER_CFG_FILE_PROPERTY_NAME);
        }        
        if (null == cfgFile) {
            cfgFile = Configurer.DEFAULT_USER_CFG_FILE;
        }
        ClassPathResource cpr = new ClassPathResource(cfgFile);
        if (cpr.exists()) {
            resources.add(cpr);
        } else {
            LogUtils.log(LOG, Level.INFO, "USER_CFG_FILE_NOT_FOUND_MSG", cfgFile);
        }
        
        if (null != cfgFileURL) {
            UrlResource ur = new UrlResource(cfgFileURL);
            if (ur.exists()) {
                resources.add(ur);
            } else {
                LogUtils.log(LOG, Level.INFO, "USER_CFG_FILE_URL_NOT_FOUND_MSG", cfgFileURL);
            }    
        } 
        
        String sysCfgFileUrl = System.getProperty(Configurer.USER_CFG_FILE_PROPERTY_URL);
        if (null != sysCfgFileUrl) {
            try {
                UrlResource ur = new UrlResource(sysCfgFileUrl);
                if (ur.exists()) {
                    resources.add(ur);
                } else {
                    LogUtils.log(LOG, Level.INFO, "USER_CFG_FILE_URL_NOT_FOUND_MSG", sysCfgFileUrl);
                }            
            } catch (MalformedURLException e) {            
                LogUtils.log(LOG, Level.INFO, "USER_CFG_FILE_URL_ERROR_MSG", sysCfgFileUrl);
            }
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating application context with resources: " + resources);
        }
        
        if (0 == resources.size()) {
            return null;
        }
        Resource[] res = new Resource[resources.size()];
        res = resources.toArray(res);
        return res;
    }
    
    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        // Spring always creates a new one of these, which takes a fair amount
        // of time on startup (nearly 1/2 second) as it gets created for every
        // spring context on the classpath
        if (nsHandlerResolver == null) {
            nsHandlerResolver = new DefaultNamespaceHandlerResolver();
        }
        reader.setNamespaceHandlerResolver(nsHandlerResolver);
        
        String mode = System.getProperty("spring.validation.mode");
        if (null != mode) {
            reader.setValidationModeName(mode);
        }
        reader.setNamespaceAware(true);  
    }
}

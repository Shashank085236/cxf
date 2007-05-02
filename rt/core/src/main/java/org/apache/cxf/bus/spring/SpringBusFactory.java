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

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class SpringBusFactory extends BusFactory {
    
    private static final String DEFAULT_BUS_ID = "cxf";
    
    private static final Logger LOG = LogUtils.getL7dLogger(SpringBusFactory.class);
    
    private final ApplicationContext context;

    public SpringBusFactory() {
        this.context = null;
    }

    public SpringBusFactory(ApplicationContext context) {
        this.context = context;
    }
        
    public Bus createBus() {
        return createBus((String)null);
    }
    
    private boolean defaultBusNotExists() {
        if (null != context) {
            context.containsBean("cxf");
            return false;
        } else {
            return true;
        }
    }

    public Bus createBus(String cfgFile) {
        return createBus(cfgFile, defaultBusNotExists());
    }
        
    private Bus finishCreatingBus(BusApplicationContext bac) {
        final Bus bus = (Bus)bac.getBean(DEFAULT_BUS_ID);

        bus.setExtension(new ConfigurerImpl(bac), Configurer.class);

        possiblySetDefaultBus(bus);
        registerApplicationContextLifeCycleListener(bus, bac);
        return bus;
    }
    
    public Bus createBus(String cfgFile, boolean includeDefaults) {
        try {      
            return finishCreatingBus(new BusApplicationContext(cfgFile, includeDefaults, context));
        } catch (BeansException ex) {
            LogUtils.log(LOG, Level.WARNING, "APP_CONTEXT_CREATION_FAILED_MSG", ex, (Object[])null);
            throw new RuntimeException(ex);
        }
    }
    
    public Bus createBus(URL url) {
        return createBus(url, defaultBusNotExists());
    }
    
    public Bus createBus(URL url, boolean includeDefaults) {
        try {      
            return finishCreatingBus(new BusApplicationContext(url, includeDefaults, context));
        } catch (BeansException ex) {
            LogUtils.log(LOG, Level.WARNING, "APP_CONTEXT_CREATION_FAILED_MSG", ex, (Object[])null);
            throw new RuntimeException(ex);
        }
    }

    
    void registerApplicationContextLifeCycleListener(Bus bus, BusApplicationContext bac) {
        BusLifeCycleManager lm = bus.getExtension(BusLifeCycleManager.class);
        if (null != lm) {
            lm.registerLifeCycleListener(new BusApplicationContextLifeCycleListener(bac));
        }
    } 

    static class BusApplicationContextLifeCycleListener implements BusLifeCycleListener {
        private BusApplicationContext bac;

        BusApplicationContextLifeCycleListener(BusApplicationContext b) {
            bac = b;
        }

        public void initComplete() {
        }

        public void preShutdown() {
        }

        public void postShutdown() {
            bac.close();
        }
        
    }
}

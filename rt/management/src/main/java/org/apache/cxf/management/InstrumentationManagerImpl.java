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

package org.apache.cxf.management;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.instrumentation.types.InstrumentationPolicyType;
import org.apache.cxf.configuration.instrumentation.types.MBServerPolicyType;
import org.apache.cxf.management.jmx.JMXManagedComponentManager;
import org.apache.cxf.oldcfg.CompoundName;
import org.apache.cxf.oldcfg.Configuration;
import org.apache.cxf.oldcfg.ConfigurationBuilder;
import org.apache.cxf.oldcfg.impl.ConfigurationBuilderImpl;




public class InstrumentationManagerImpl implements InstrumentationManager {    
    static final Logger LOG = LogUtils.getL7dLogger(InstrumentationManagerImpl.class);
    static final String INSTRUMENTATION_CONFIGURATION_URI = 
        "http://cxf.apache.org/configuration/instrumentation";

    // TODO: avoid clashes with bus id

    static final String INSTRUMENTATION_CONFIGURATION_ID = 
        "instrumentation";
    
    private Configuration configuration;
    private List <Instrumentation> instrumentations;
    private JMXManagedComponentManager jmxManagedComponentManager;

    
    public InstrumentationManagerImpl() {
        this(new ConfigurationBuilderImpl());
    }

    public InstrumentationManagerImpl(ConfigurationBuilder builder) {
        LOG.info("Setting up InstrumentationManager");
        
        configuration = getConfiguration(builder);
        InstrumentationPolicyType ip = 
            configuration.getObject(InstrumentationPolicyType.class, "InstrumentationControl");   
        
        if (ip.isInstrumentationEnabled()) {
            instrumentations = new LinkedList<Instrumentation>();
        }
            
        if (ip.isJMXEnabled()) {           
            jmxManagedComponentManager = new JMXManagedComponentManager();
            MBServerPolicyType mbsp = configuration.getObject(MBServerPolicyType.class, "MBServer");
            jmxManagedComponentManager.init(mbsp);
        }
        
        
    }
    
    private Configuration getConfiguration(ConfigurationBuilder cb) {
        
        CompoundName id = new CompoundName(INSTRUMENTATION_CONFIGURATION_ID); 
        return cb.getConfiguration(INSTRUMENTATION_CONFIGURATION_URI,  id);
    }

    public void shutdown() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Shutdown InstrumentationManager ");
        }
        
        if (jmxManagedComponentManager != null) {
            jmxManagedComponentManager.shutdown();
        }
    }
    
    public void register(Instrumentation it) {
        if (it == null) {
            // just return
            return;
        } else {
            instrumentations.add(it); 
            if (jmxManagedComponentManager != null) {
                jmxManagedComponentManager.registerMBean(it);
            }
        }        
    }

    public void unregister(Object component) {
        for (Iterator<Instrumentation> i = instrumentations.iterator(); i.hasNext();) {
            Instrumentation it = i.next();
            if (it.getComponent() == component) {
                i.remove();   
                if (it != null && jmxManagedComponentManager != null) {
                    jmxManagedComponentManager.unregisterMBean(it);               
                }
                return;
            }
        }
    }
    
    public List<Instrumentation> getAllInstrumentation() {
        // TODO need to add more query interface
        return instrumentations;
    }

    public MBeanServer getMBeanServer() {        
        return jmxManagedComponentManager.getMBeanServer();
    }
      

}

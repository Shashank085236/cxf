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

package org.apache.cxf.transport;

import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.extension.ExtensionManager;

public final class DestinationFactoryManagerImpl implements DestinationFactoryManager {
  
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(DestinationFactoryManager.class);
    
    final Map<String, DestinationFactory> destinationFactories;
    Properties factoryNamespaceMappings;
    
    @Resource
    private ExtensionManager extensionManager;

    public DestinationFactoryManagerImpl() throws BusException {
        destinationFactories = new ConcurrentHashMap<String, DestinationFactory>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.DestinationFactoryManager#registerDestinationFactory(java.lang.String,
     *      org.apache.cxf.transports.DestinationFactory)
     */
    public void registerDestinationFactory(String namespace, DestinationFactory factory) {
        destinationFactories.put(namespace, factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.DestinationFactoryManager#deregisterDestinationFactory(java.lang.String)
     */
    public void deregisterDestinationFactory(String namespace) {
        destinationFactories.remove(namespace);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.DestinationFactoryManager#DestinationFactory(java.lang.String)
     */
    /**
     * Returns the conduit initiator for the given namespace, constructing it
     * (and storing in the cache for future reference) if necessary, using its
     * list of factory classname to namespace mappings.
     * 
     * @param namespace the namespace.
     */
    public DestinationFactory getDestinationFactory(String namespace) throws BusException {
        DestinationFactory factory = destinationFactories.get(namespace);
        if (null == factory) {
            extensionManager.activateViaNS(namespace);
            factory = destinationFactories.get(namespace);
        }
        if (null == factory) {
            throw new BusException(new Message("NO_CONDUIT_INITIATOR_EXC", BUNDLE, namespace));
        }
        return factory;
    }

    @PreDestroy
    public void shutdown() {
        // nothing to do
    }
}

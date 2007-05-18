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

package org.apache.cxf.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.cxf.Bus;

public class ServerLifeCycleManagerImpl implements ServerLifeCycleManager {
    
    private List<ServerLifeCycleListener> listeners = new ArrayList<ServerLifeCycleListener>();
    private Bus bus;

    public synchronized void registerListener(ServerLifeCycleListener listener) {
        listeners.add(listener);
    }

    public void startServer(Server server) {
        List<ServerLifeCycleListener> listenersToNotify = null;
        synchronized (this) {
            listenersToNotify = new ArrayList<ServerLifeCycleListener>();
            listenersToNotify.addAll(listeners);
        }
        
        for (ServerLifeCycleListener listener : listenersToNotify) {
            listener.startServer(server);
        }
    }

    public void stopServer(Server server) {
        List<ServerLifeCycleListener> listenersToNotify = null;
        synchronized (this) {
            listenersToNotify = new ArrayList<ServerLifeCycleListener>();
            listenersToNotify.addAll(listeners);
        }
        
        for (ServerLifeCycleListener listener : listenersToNotify) {
            listener.stopServer(server);
        }
    }

    public synchronized void unRegisterListener(ServerLifeCycleListener listener) {
        listeners.remove(listener);
    }
    
    public Bus getBus() {
        return bus;
    }
    
    @Resource
    public void setBus(Bus bus) {        
        this.bus = bus;        
    }
    
    @PostConstruct
    public void register() {
        if (null != bus) {
            bus.setExtension(this, ServerLifeCycleManager.class);
        }
    }
}

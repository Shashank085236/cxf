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
package org.apache.cxf.transport.http_jetty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;




/**
 * This Bus Extension handles the configuration of network port
 * numbers for use with "http" or "https". This factory 
 * caches the JettyHTTPServerEngines so that they may be 
 * retrieved if already previously configured.
 */
public class JettyHTTPServerEngineFactory implements BusLifeCycleListener {
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPServerEngineFactory.class);    
    
    /**
     * This map holds references for allocated ports.
     */
    // Still use the static map to hold the port information
    // in the same JVM
    private static Map<Integer, JettyHTTPServerEngine> portMap =
        new HashMap<Integer, JettyHTTPServerEngine>();
   
    private BusLifeCycleManager lifeCycleManager;
    /**
     * This map holds the threading parameters that are to be applied
     * to new Engines when bound to the reference id.
     */
    private Map<String, ThreadingParameters> threadingParametersMap =
        new TreeMap<String, ThreadingParameters>();
    
    /**
     * This map holds TLS Server Parameters that are to be used to
     * configure a subsequently created JettyHTTPServerEngine.
     */
    private Map<String, TLSServerParameters> tlsParametersMap =
        new TreeMap<String, TLSServerParameters>();
    
    
    /**
     * The bus.
     */
    private Bus bus;
    
    public JettyHTTPServerEngineFactory() {
        // Empty
    }    
    
    
    /**
     * This call is used to set the bus. It should only be called once.
     * @param bus
     */
    @Resource(name = "bus")
    public void setBus(Bus bus) {
        assert this.bus == null;
        this.bus = bus;
    }
    
    
    @PostConstruct
    public void registerWithBus() {
        if (bus != null) {
            bus.setExtension(this, JettyHTTPServerEngineFactory.class);
        }
        lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        if (null != lifeCycleManager) {
            lifeCycleManager.registerLifeCycleListener(this);
        }        
    }
    
    
    /**
     * This call sets TLSParametersMap for a JettyHTTPServerEngine
     * 
     */
    public void setTlsServerParametersMap(
        Map<String, TLSServerParameters>  tlsParamsMap) {
        
        tlsParametersMap = tlsParamsMap;
    }
    
    public Map<String, TLSServerParameters> getTlsServerParametersMap() {
        return tlsParametersMap;
    }
    
    public void setEnginesList(List<JettyHTTPServerEngine> enginesList) {
        for (JettyHTTPServerEngine engine : enginesList) { 
            portMap.put(engine.getPort(), engine);
        }    
    }
    
    /**
     * This call sets the ThreadingParameters for a JettyHTTPServerEngine
     * 
     */
    public void setThreadingParametersMap(
        Map<String, ThreadingParameters> threadingParamsMap) {
        
        threadingParametersMap = threadingParamsMap;
    }
    
    public Map<String, ThreadingParameters> getThreadingParametersMap() {
        return threadingParametersMap;
    }
    
    /**
     * This call sets TLSServerParameters for a JettyHTTPServerEngine
     * that will be subsequently created. It will not alter an engine
     * that has already been created for that network port.
     * @param port       The network port number to bind to the engine.
     * @param tlsParams  The tls server parameters. Cannot be null.
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    public void setTLSServerParametersForPort(
        int port, 
        TLSServerParameters tlsParams) throws GeneralSecurityException, IOException {
        if (tlsParams == null) {
            throw new IllegalArgumentException("tlsParams cannot be null");
        }
        JettyHTTPServerEngine ref = retrieveJettyHTTPServerEngine(port);
        if (null == ref) {
            ref = new JettyHTTPServerEngine(this, bus, port);
            ref.setTlsServerParameters(tlsParams);
            portMap.put(port, ref);
            ref.finalizeConfig();
        } else {
            if (ref.getConnector() != null && ref.getConnector().isRunning()) {
                throw new IOException("can't set the TLS params on the opened connector");
            }
            ref.setTlsServerParameters(tlsParams);            
        }
    }
            
    
    /**
     * This call retrieves a previously configured JettyHTTPServerEngine for the
     * given port. If none exists, this call returns null.
     */
    public synchronized JettyHTTPServerEngine retrieveJettyHTTPServerEngine(int port) {
        return portMap.get(port);
    }

    /**
     * This call creates a new JettyHTTPServerEngine initialized for "http"
     * or "https" on the given port. The determination of "http" or "https"
     * will depend on configuration of the engine's bean name.
     * 
     * If an JettyHTTPEngine already exists, or the port
     * is already in use, a BindIOException will be thrown. If the 
     * engine is being Spring configured for TLS a GeneralSecurityException
     * may be thrown.
     */
    protected synchronized JettyHTTPServerEngine createJettyHTTPServerEngine(int port, String protocol)
        throws GeneralSecurityException, IOException {
        LOG.fine("Creating Jetty HTTP Server Engine for port " + port + ".");        
        JettyHTTPServerEngine ref = retrieveJettyHTTPServerEngine(port);
        if (null == ref) {
            ref = new JettyHTTPServerEngine(this, bus, port);            
            portMap.put(port, ref);
            ref.finalizeConfig();
        } 
        // checking the protocol    
        if (!protocol.equals(ref.getProtocol())) {
            throw new IOException("Protocol mismatch: "
                        + "engine's protocol is " + ref.getProtocol()
                        + ", the url protocol is " + protocol);
        }
                
        return ref;
    }

    
    /**
     * This method removes the Server Engine from the port map and stops it.
     */
    public synchronized void destroyForPort(int port) {
        JettyHTTPServerEngine ref = portMap.remove(port);
        if (ref != null) {
            LOG.fine("Stopping Jetty HTTP Server Engine on port " + port + ".");
            try {
                ref.stop();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }            
        }
    }

    @PostConstruct
    public void finalizeConfig() {
        registerWithBus();
    }

    public void initComplete() {
        // do nothing here
        
    }

    public void postShutdown() {
        //shut down the jetty server in the portMap
        // To avoid the CurrentModificationException, 
        // do not use portMap.vaules directly       
        JettyHTTPServerEngine[] engines = portMap.values().toArray(new JettyHTTPServerEngine[0]);
        for (JettyHTTPServerEngine engine : engines) {
            engine.shutdown();
        }
        // clean up the collections
        threadingParametersMap.clear();
        tlsParametersMap.clear();
    }

    public void preShutdown() {
        // do nothing here 
        // just let server registry to call the server stop first
    }
    
}

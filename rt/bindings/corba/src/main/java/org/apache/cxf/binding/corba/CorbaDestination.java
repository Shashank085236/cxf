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

package org.apache.cxf.binding.corba;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.binding.corba.runtime.CorbaDSIServant;
import org.apache.cxf.binding.corba.utils.CorbaBindingHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

public class CorbaDestination implements Destination {
    
    public static final class OpInfoEntry {

        private BindingOperationInfo boInfo;
        private OperationType opType;

        public OpInfoEntry(BindingOperationInfo bopInfo, OperationType extensor) {
            this.boInfo = bopInfo;
            this.opType = extensor;
        }

        public BindingOperationInfo getBoInfo() {
            return boInfo;
        }

        public OperationType getOpType() {
            return opType;
        }

    }

    private static final Logger LOG = LogUtils.getL7dLogger(CorbaDestination.class);
    private AddressType address;
    private EndpointReferenceType reference;
    private ORB orb;
    private BindingInfo binding;
    private EndpointInfo endpointInfo;
    private OrbConfig orbConfig;
    private MessageObserver incomingObserver;
    private CorbaTypeMap typeMap;
    private byte[] objectId;
    private POA bindingPOA;
    private org.omg.CORBA.Object obj;
    private Map<String, OpInfoEntry> opInfoCache = new ConcurrentHashMap<String, OpInfoEntry>();

    public CorbaDestination(EndpointInfo ei, OrbConfig config) {
        this(ei, config, null);    
    }

    public CorbaDestination(EndpointInfo ei, OrbConfig config, CorbaTypeMap tm) {
        address = ei.getExtensor(AddressType.class);
        binding = ei.getBinding();
        reference = new EndpointReferenceType();
        AttributedURIType addr = new AttributedURIType();
        addr.setValue(address.getLocation());
        reference.setAddress(addr);
        endpointInfo = ei;
        orbConfig = config;
        if (tm != null) {
            typeMap = tm;
        } else {
            typeMap = TypeMapCache.get(binding.getService());
        }
    }

    public EndpointReferenceType getAddress() {
        return reference;
    }    

    public Conduit getBackChannel(Message inMessage,
                                  Message partialResponse,
                                  EndpointReferenceType ref)
        throws IOException {
        return  new CorbaServerConduit(endpointInfo, reference, obj, orbConfig, typeMap);
    }

    public BindingInfo getBindingInfo() {
        return binding;
    }
    
    public EndpointInfo getEndPointInfo() {
        return endpointInfo;
    }

    public CorbaTypeMap getCorbaTypeMap() {
        return typeMap;
    }

    public void shutdown() {
        if (orb != null) {
            try {
                // Indicate that we are done with the ORB.  We'll ask for it to be destroyed but it
                // someone else is using it, it really won't be (just its use count decremented)
                CorbaBindingHelper.destroyORB(getDestinationAddress(), orb);
            } catch (Exception ex) {
                throw new CorbaBindingException(ex);
            }
            orb = null;
        }
    }
    
    protected ORB getOrb() {
        return orb;
    }
    
    protected AddressType getAddressType() {
        return address;
    }    

    public synchronized void setMessageObserver(MessageObserver observer) {
        if (observer != incomingObserver) {
            MessageObserver old = incomingObserver;
            incomingObserver = observer;
            if (observer != null) { 
                if (old == null) {
                    activate();
                }
            } else {
                if (old != null) {
                    deactivate();
                }
            }
        }
    }

    public void activate() {
        String location = getDestinationAddress();
        LOG.info("Service address retrieved: " + location);
        
        orb = CorbaBindingHelper.getAddressSpecificORB(location);
        if (orb == null) {
            LOG.log(Level.INFO, "Creating ORB with address " + location);
            orb = CorbaBindingHelper.createAddressSpecificORB(location, orbConfig);
        }
        // Need to indicate that this ORB can't be destroyed while we are using it
        CorbaBindingHelper.keepORBAlive(location);
        
        try {
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POAManager poaManager = rootPOA.the_POAManager();

            try {
                bindingPOA = rootPOA.find_POA("BindingPOA", true);
            } catch (org.omg.PortableServer.POAPackage.AdapterNonExistent ex) {
                // An AdapterNonExistent exception will be thrown if the POA does not exist.  If
                // this is the case, then we'll create one.
                Policy[] policies = new Policy[2];
                policies[0] = rootPOA
                .create_id_uniqueness_policy(
                    org.omg.PortableServer.IdUniquenessPolicyValue.UNIQUE_ID);
                policies[1] = rootPOA
                        .create_implicit_activation_policy(
                            org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION);
                /*
                REVISIT - PERSISTENT POA with Sun ORB?
                policies[2] = rootPOA
                    .create_lifespan_policy(org.omg.PortableServer.LifespanPolicyValue.PERSISTENT);
                policies[3] = rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID);
                */

                bindingPOA = rootPOA.create_POA("BindingPOA", poaManager, policies);
            }
            
            if (bindingPOA == null) {
                throw new CorbaBindingException("Unable to create CXF CORBA Binding POA");
            }

            CorbaDSIServant servant = new CorbaDSIServant();
            servant.init(orb, bindingPOA, this, incomingObserver, typeMap);
            objectId = bindingPOA.activate_object(servant);
            obj = bindingPOA.id_to_reference(objectId);
            
            if (location.startsWith("relfile:")) {
                String iorFile = location.substring("relfile:".length(), location.length());
                // allow for up to 3 '/' to match common uses of relfile url format
                for (int n = 0; n < 3; n++) {
                    if (iorFile.charAt(0) != '/') {
                        break;
                    } else {
                        iorFile = iorFile.substring(1);
                    }
                }

                CorbaUtils.exportObjectReferenceToFile(obj, orb, iorFile);
            } else if (location.startsWith("file:")) {
                String iorFile = location.substring("file:".length(), location.length());
                // allow for up to 3 '/' to match common uses of file url format
                for (int n = 0; n < 3; n++) {
                    if (iorFile.charAt(0) != '/') {
                        break;
                    } else {
                        iorFile = iorFile.substring(1);
                    }
                }
                // to match the ORB, file must have complete path information, therefore we add
                // a '/' prefix to the address
                //iorFile = "/" + iorFile;
                CorbaUtils.exportObjectReferenceToFile(obj, orb, iorFile);
            } else if (location.startsWith("corbaloc")) {
                // Try add the key to the boot manager.  This is required for a corbaloc
                addKeyToBootManager(location, obj);
            } else {
                String ior = orb.object_to_string(obj);
                address.setLocation(ior);
                String iorFile = "endpoint.ior";
                CorbaUtils.exportObjectReferenceToFile(obj, orb, iorFile);
            }
            populateEpr(orb.object_to_string(obj));
            LOG.info("Object Reference: " + orb.object_to_string(obj));
            // TODO: Provide other export mechanisms? 
            poaManager.activate();
        } catch (Exception ex) {
            throw new CorbaBindingException("Unable to activate CORBA servant", ex);
        }
    }

    private void populateEpr(String ior) {
        AttributedURIType addr = new AttributedURIType();
        addr.setValue(ior);
        reference.setAddress(addr);
    }

    public String getDestinationAddress() {
        // We should check the endpoint first for an address.  This allows object references
        // to use the address that is associated with their endpoint instead of the single 
        // address for a particular port type that is listed in the wsdl.  Otherwise, for all 
        // object references we want to create, we would need to add the address to the wsdl 
        // file before running the application.
        String location = null;
        if (endpointInfo != null) {
            location = endpointInfo.getAddress();
        }

        if (location == null) {
            location = address.getLocation();
        }

        return location;
    }
    
    public MessageObserver getMessageObserver() {
        return incomingObserver;
    }

    public void deactivate() {
        if (orb != null) {
            if (bindingPOA == null) {
                throw new CorbaBindingException("Corba Port deactivation failed because the poa is null");
            }

            try {
                bindingPOA.deactivate_object(objectId);
            } catch (Exception ex) {
                throw new CorbaBindingException("Unable to deactivate CORBA servant", ex);
            }
        }
    }

    private void addKeyToBootManager(String location, org.omg.CORBA.Object value) {
        int keyIndex = location.indexOf('/');
        String key = location.substring(keyIndex + 1);
        try {
            Class<?> bootMgrHelperClass = Class.forName("org.apache.yoko.orb.OB.BootManagerHelper");
            Class<?> bootMgrClass = Class.forName("org.apache.yoko.orb.OB.BootManager");
            Method narrowMethod =
                bootMgrHelperClass.getMethod("narrow", org.omg.CORBA.Object.class);
            Object bootMgr = narrowMethod.invoke(null, orb.resolve_initial_references("BootManager"));
            Method addBindingMethod = 
                bootMgrClass.getMethod("add_binding", byte[].class, org.omg.CORBA.Object.class);
            addBindingMethod.invoke(bootMgr, key.getBytes(), value);
            LOG.info("Added key " + key + " to bootmanager");
        } catch (ClassNotFoundException ex) {
            //Not supported by the orb. skip it.
        } catch (java.lang.reflect.InvocationTargetException ex) {
            //Not supported by the orb. skip it.
        } catch (Exception ex) {
            throw new CorbaBindingException(ex);
        }
    }

    public OpInfoEntry getBindingOpInfo(String opName) {
        if (!opInfoCache .containsKey(opName)) {
            for (BindingOperationInfo bopInfo : binding.getOperations()) {
                if (bopInfo.getName().getLocalPart().equals(opName)) {
                    opInfoCache.put(opName, new OpInfoEntry(bopInfo,
                            bopInfo.getExtensor(OperationType.class)));
                    break;
                }
            }
        }
        return opInfoCache.get(opName);
    }
}

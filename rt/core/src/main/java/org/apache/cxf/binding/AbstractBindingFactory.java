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

package org.apache.cxf.binding;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.WSDLBindingFactory;

import static org.apache.cxf.helpers.CastUtils.cast;

public abstract class AbstractBindingFactory implements BindingFactory, WSDLBindingFactory {

    public static final String DATABINDING_DISABLED = "databinding.disabled";
    
    @Resource
    Bus bus;
    
    @Resource
    Collection<String> activationNamespaces;
    
    @PostConstruct
    void registerWithBindingManager() {
        BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
        for (String ns : activationNamespaces) {
            manager.registerBindingFactory(ns, this);
        }
    }
    
    /**
     * Creates a "default" BindingInfo object for the service.  Called by 
     * createBindingInfo(ServiceInfo service, Binding binding) to actually 
     * create the BindingInfo.  Can return a subclass which can then process
     * the extensors within the subclass.
     * @param service
     * @return
     */
    public BindingInfo createBindingInfo(ServiceInfo service, String namespace, Object config) {
        return new BindingInfo(service, namespace);
    }
    
    public BindingInfo createBindingInfo(Service service, String namespace, Object config) {
        return createBindingInfo(service.getServiceInfo(), namespace, config);
    }
    
    
    /**
     * Copies extensors from the Binding to BindingInfo.
     * @param service
     * @param binding
     * @return
     */
    public BindingInfo createBindingInfo(ServiceInfo service, Binding binding, String ns) {

        BindingInfo bi = createBindingInfo(service, ns, null);
        
        return initializeBindingInfo(service, binding, bi);
    }

    protected BindingInfo initializeBindingInfo(ServiceInfo service, Binding binding, BindingInfo bi) {
        bi.setName(binding.getQName());
        copyExtensors(bi, binding.getExtensibilityElements());

        for (BindingOperation bop : cast(binding.getBindingOperations(), BindingOperation.class)) {
            String inName = null;
            String outName = null;
            if (bop.getBindingInput() != null) {
                inName = bop.getBindingInput().getName();
            }
            if (bop.getBindingOutput() != null) {
                outName = bop.getBindingOutput().getName();
            }
            String portTypeNs = binding.getPortType().getQName().getNamespaceURI();
            QName opName = new QName(portTypeNs,
                                     bop.getName());
            BindingOperationInfo bop2 = bi.getOperation(opName);
            if (bop2 == null) {
                bop2 = bi.buildOperation(opName, inName, outName);
                if (bop2 != null) {
                    bi.addOperation(bop2);
                }
            }
            if (bop2 != null) {
                copyExtensors(bop2, bop.getExtensibilityElements());
                if (bop.getBindingInput() != null) {
                    copyExtensors(bop2.getInput(), bop.getBindingInput().getExtensibilityElements());
                }
                if (bop.getBindingOutput() != null) {
                    copyExtensors(bop2.getOutput(), bop.getBindingOutput().getExtensibilityElements());
                }
                for (BindingFault f : cast(bop.getBindingFaults().values(), BindingFault.class)) {
                    copyExtensors(bop2.getFault(new QName(service.getTargetNamespace(), f.getName())),
                                  bop.getBindingFault(f.getName()).getExtensibilityElements());
                }
            }
        }
        return bi;
    }

    private void copyExtensors(AbstractPropertiesHolder info, List<?> extList) {
        if (info != null) {
            for (ExtensibilityElement ext : cast(extList, ExtensibilityElement.class)) {
                info.addExtensor(ext);
            }
        }
    }
}

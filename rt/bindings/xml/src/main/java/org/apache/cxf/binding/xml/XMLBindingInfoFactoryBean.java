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
package org.apache.cxf.binding.xml;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingInfoFactoryBeanManager;
import org.apache.cxf.service.model.AbstractBindingInfoFactoryBean;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

public class XMLBindingInfoFactoryBean extends AbstractBindingInfoFactoryBean {
    private Bus bus;
    private Collection<String> activationNamespaces; 
    
    @Resource
    public void setBus(Bus b) {
        bus = b;
    }
    
    @Resource
    public void setActivationNamespaces(Collection<String> ans) {
        activationNamespaces = ans;
    }
    
    @PostConstruct
    void register() {
        if (null == bus) {
            return;
        }
        BindingInfoFactoryBeanManager bfm = bus.getExtension(BindingInfoFactoryBeanManager.class);
        if (null != bfm) {
            for (String ns : activationNamespaces) {
                bfm.registerBindingInfoFactoryBean(ns, this);
            }
        }
    }

    @Override
    public BindingInfo create() {
        ServiceInfo si = getServiceInfo();
        BindingInfo info = new BindingInfo(si, "http://cxf.apache.org/bindings/xformat");        
        info.setName(getBindingName());              
        for (OperationInfo op : si.getInterface().getOperations()) {                       
            BindingOperationInfo bop = 
                info.buildOperation(op.getName(), op.getInputName(), op.getOutputName());
            info.addOperation(bop);
        }
        
        return info;
    }
    
    protected QName getBindingName() {
        ServiceInfo si = getServiceInfo();
        return new QName(si.getName().getNamespaceURI(), 
                         si.getName().getLocalPart() + "XMLBinding");
    }

    @Override
    public String getTransportURI() {        
        return "http://cxf.apache.org/bindings/xformat";
    }

}

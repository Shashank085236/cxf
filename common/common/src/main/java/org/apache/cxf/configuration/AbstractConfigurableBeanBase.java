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

package org.apache.cxf.configuration;

import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

public abstract class AbstractConfigurableBeanBase implements Configurable {
    
    private List<ConfigurationProvider> fallbackProviders;

    private List<ConfigurationProvider> overwriteProviders;
    
    @XmlTransient
    public String getBeanName() {
        return getClass().getName();
    }
    
    public List<ConfigurationProvider> getFallbackProviders() {
        return fallbackProviders;
    }
    
    @XmlTransient
    public void setFallbackProviders(List<ConfigurationProvider> fallbackProviders) {
        this.fallbackProviders = fallbackProviders;
    }

    public List<ConfigurationProvider> getOverwriteProviders() {
        return overwriteProviders;
    }

    @XmlTransient
    public void setOverwriteProviders(List<ConfigurationProvider> overwriteProviders) {
        this.overwriteProviders = overwriteProviders;
    }

    protected <T> T tryOverwrite(Class<T> cls, String name) {
        if (null != overwriteProviders) {
            for (ConfigurationProvider p : overwriteProviders) {
                Object value = p.getObject(name);
                if (null != value) {
                    return cls.cast(value);
                }
            }
        }
        return null;
    }
    
    protected <T> T tryFallback(Class<T> cls, String name) {
        if (null != fallbackProviders) {
            for (ConfigurationProvider p : fallbackProviders) {
                Object value = p.getObject(name);
                if (null != value) {
                    return cls.cast(value);
                }
            }
        }
        return null;
    }
    
    protected void notifyPropertyChange(String propertyName) {
        // do nothing here for now.
    }
    
    
    
}

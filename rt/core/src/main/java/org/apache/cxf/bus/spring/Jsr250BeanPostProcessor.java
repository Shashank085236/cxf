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

import org.apache.cxf.common.injection.ResourceInjector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;

public class Jsr250BeanPostProcessor implements DestructionAwareBeanPostProcessor, Ordered {

    private ResourceInjector injector;
    
    Jsr250BeanPostProcessor() {
        injector = new ResourceInjector(null, null); 
    }

    public int getOrder() {
        return 1002;
    }
        
    public Object postProcessAfterInitialization(Object bean, String beanId) throws BeansException {
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanId) throws BeansException {
        if (bean != null) {
            injector.construct(bean);
        }
        return bean;
    }

    public void postProcessBeforeDestruction(Object bean, String beanId) {
        if (bean != null) {
            injector.destroy(bean);
        }
    }

}

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
package org.apache.cxf.frontend.spring;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

public class ServerFactoryBeanDefinitionParser extends AbstractBeanDefinitionParser {
    private static final String IMPLEMENTOR = "implementor";

    public ServerFactoryBeanDefinitionParser() {
        super();
        setBeanClass(ServerFactoryBean.class);
    }

    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element el, String name) {
        if ("properties".equals(name)) {
            Map map = ctx.getDelegate().parseMapElement(el, bean.getBeanDefinition());
            bean.addPropertyValue("properties", map);
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("invoker".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.invoker");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        }  else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name)) {
            List list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
        
    }
    

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        super.doParse(element, ctx, bean);
        
        bean.setInitMethodName("create");
        
        // We don't really want to delay the registration of our Server
        bean.setLazyInit(false);
    }

    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if (name.equals(IMPLEMENTOR)) {
            loadImplementor(bean, val);
        } else {
            super.mapAttribute(bean, e, name, val);
        }
    }

    private void loadImplementor(BeanDefinitionBuilder bean, String val) {
        if (StringUtils.hasText(val)) {
            if (val.startsWith("#")) {
                bean.addPropertyReference(IMPLEMENTOR, val.substring(1));
            } else {
                try {
                    bean.addPropertyValue(IMPLEMENTOR,
                                          ClassLoaderUtils.loadClass(val, getClass()).newInstance());
                } catch (Exception e) {
                    throw new FatalBeanException("Could not load class: " + val, e);
                }
            }
        }
    }

    @Override
    protected boolean hasBusProperty() {
        return true;
    }

}

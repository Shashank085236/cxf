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
package org.apache.cxf.jaxws.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;


public class EndpointDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String IMPLEMENTOR = "implementor";

    public EndpointDefinitionParser() {
        super();
        setBeanClass(EndpointImpl.class);
    }

    @Override
    protected String getSuffix() {
        return ".jaxws-endpoint";
    }

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        boolean isAbstract = false;
        NamedNodeMap atts = element.getAttributes();
        String bus = element.getAttribute("bus");
        if (StringUtils.isEmpty(bus)) {
            if (ctx.getRegistry().containsBeanDefinition(Bus.DEFAULT_BUS_ID)) {
                bean.addConstructorArgReference(Bus.DEFAULT_BUS_ID);
            }
        } else {
            if (ctx.getRegistry().containsBeanDefinition(bus)) {
                bean.addConstructorArgReference(bus);
            }
        }
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String pre = node.getPrefix();
            String name = node.getLocalName();

            if ("createdFromAPI".equals(name)) {
                bean.setAbstract(true);
                isAbstract = true;
            } else if (isAttribute(pre, name) && !"publish".equals(name) && !"bus".equals(name)) {
                if ("endpointName".equals(name) || "serviceName".equals(name)) {
                    QName q = parseQName(element, val);
                    bean.addPropertyValue(name, q);
                } else if ("depends-on".equals(name)) {
                    bean.addDependsOn(val);
                } else if (IMPLEMENTOR.equals(name)) {
                    loadImplementor(bean, val);
                } else if (!"name".equals(name)) {
                    mapToProperty(bean, name, val);
                }
            } else if ("abstract".equals(name)) {
                bean.setAbstract(true);
                isAbstract = true;
            }
        }
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = n.getLocalName();
                if ("properties".equals(name)) {
                    Map map = ctx.getDelegate().parseMapElement((Element) n, bean.getBeanDefinition());
                    bean.addPropertyValue("properties", map);
                } else if ("binding".equals(name)) {
                    setFirstChildAsProperty((Element) n, ctx, bean, "bindingConfig");
                } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
                    || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
                    || "features".equals(name) || "schemaLocations".equals(name)
                    || "handlers".equals(name)) {
                    List list = ctx.getDelegate().parseListElement((Element) n, bean.getBeanDefinition());
                    bean.addPropertyValue(name, list);
                } else if (IMPLEMENTOR.equals(name)) {
                    ctx.getDelegate()
                        .parseConstructorArgElement((Element)n, bean.getBeanDefinition());
                } else {
                    setFirstChildAsProperty((Element) n, ctx, bean, name);
                }
            }
        }
        if (!isAbstract) {
            bean.setInitMethodName("publish");
            bean.setDestroyMethodName("stop");
        }
        // We don't want to delay the registration of our Server
        bean.setLazyInit(false);
    }

    private void loadImplementor(BeanDefinitionBuilder bean, String val) {
        if (!StringUtils.isEmpty(val)) {
            if (val.startsWith("#")) {
                bean.addConstructorArgReference(val.substring(1));
            } else {
                try {
                    Object obj = ClassLoaderUtils.loadClass(val, getClass()).newInstance();
                    bean.addConstructorArg(obj);
                } catch (Exception e) {
                    throw new FatalBeanException("Could not load class: " + val, e);
                }
            }
        }
    }
    @Override
    protected String resolveId(Element elem, 
                               AbstractBeanDefinition definition, 
                               ParserContext ctx) 
        throws BeanDefinitionStoreException {
        String id = super.resolveId(elem, definition, ctx);
        if (StringUtils.isEmpty(id)) {
            id = getBeanClass().getName() + "--" + definition.hashCode();
        }
        
        return id;
    }

}

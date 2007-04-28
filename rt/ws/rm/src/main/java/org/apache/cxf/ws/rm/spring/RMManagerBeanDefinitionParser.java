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
package org.apache.cxf.ws.rm.spring;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.ws.rm.RMManager;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class RMManagerBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String RM_NS =
        "http://cxf.apache.org/ws/rm/manager";

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        mapElementToJaxbProperty(element, bean, 
                new QName(RM_NS, "deliveryAssurance"), "deliveryAssurance");
        mapElementToJaxbProperty(element, bean, 
                new QName(RM_NS, "sourcePolicy"), "sourcePolicy");
        mapElementToJaxbProperty(element, bean, 
                new QName(RM_NS, "destinationPolicy"), "destinationPolicy");
        mapElementToJaxbProperty(element, bean, 
                new QName("http://schemas.xmlsoap.org/ws/2005/02/rm/policy", "RMAssertion"), "RMAssertion");
        
        String bus = element.getAttribute("bus");
        if (bus == null || "".equals(bus) && ctx.getRegistry().containsBeanDefinition("cxf")) {
            bean.addPropertyReference("bus", "cxf");
        } else {
            bean.addPropertyReference("bus", bus);
        }
    }

    @Override
    protected Class getBeanClass(Element element) {
        return RMManager.class;
    }

    @Override
    protected String getJaxbPackage() {
        return "org.apache.cxf.ws.rm.manager";
    }

}

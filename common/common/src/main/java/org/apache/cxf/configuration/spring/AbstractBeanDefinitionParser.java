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
package org.apache.cxf.configuration.spring;

import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

public abstract class AbstractBeanDefinitionParser 
    extends org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser {

    private Class beanClass;

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        boolean setBus = parseAttributes(element, ctx, bean);        
        if (!setBus && ctx.getRegistry().containsBeanDefinition("cxf") && hasBusProperty()) {
            wireBus(bean, "cxf");
        }
        parseChildElements(element, ctx, bean);
    }
    
    protected boolean parseAttributes(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        NamedNodeMap atts = element.getAttributes();
        boolean setBus = false;
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String name = node.getLocalName();
            
            if ("createdFromAPI".equals(name)) {
                bean.setAbstract(true);
            } else if ("abstract".equals(name)) {
                bean.setAbstract(true);
            } else if (!"id".equals(name) && !"name".equals(name)) {
                if ("bus".equals(name)) {
                    setBus = true;
                } 
                mapAttribute(bean, element, name, val);
            }
        } 
        return setBus;
    }
    
    protected void parseChildElements(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = n.getLocalName();
                
                mapElement(ctx, bean, (Element) n, name);
            }
        }
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    protected Class getBeanClass(Element e) {
        return beanClass;
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        mapAttribute(bean, name, val);
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, String name, String val) {
        mapToProperty(bean, name, val);
    }
    
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
    }
    
    @Override
    protected String resolveId(Element elem, AbstractBeanDefinition definition, 
                               ParserContext ctx) throws BeanDefinitionStoreException {
        
        // REVISIT: use getAttributeNS instead
        
        String id = getIdOrName(elem);
        String createdFromAPI = elem.getAttribute(BeanConstants.CREATED_FROM_API_ATTR);
        
        if (null == id || "".equals(id)) {
            return super.resolveId(elem, definition, ctx);
        } 
        
        if (createdFromAPI != null && "true".equals(createdFromAPI.toLowerCase())) {
            return id + getSuffix();
        }
        return id;        
    }

    protected boolean hasBusProperty() {
        return false;
    }
    
    protected String getSuffix() {
        return "";
    }

    protected void setFirstChildAsProperty(Element element, ParserContext ctx, 
                                         BeanDefinitionBuilder bean, String propertyName) {
        String id = getAndRegisterFirstChild(element, ctx, bean, propertyName);
        bean.addPropertyReference(propertyName, id);
        
    }

    protected String getAndRegisterFirstChild(Element element, ParserContext ctx, 
                                              BeanDefinitionBuilder bean, String propertyName) {
        Element first = getFirstChild(element);
        
        if (first == null) {
            throw new IllegalStateException(propertyName + " property must have child elements!");
        }
        
        // Seems odd that we have to do the registration, I wonder if there is a better way
        String id;
        BeanDefinition child;
        if (first.getNamespaceURI().equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI)) {
            String name = first.getLocalName();
            if ("ref".equals(name)) {
                id = first.getAttribute("bean");
                if (id == null) {
                    throw new IllegalStateException("<ref> elements must have a \"bean\" attribute!");
                }
                return id;
            } else if ("bean".equals(name)) {
                BeanDefinitionHolder bdh = ctx.getDelegate().parseBeanDefinitionElement(first);
                child = bdh.getBeanDefinition();
                id = bdh.getBeanName();
            } else {
                throw new UnsupportedOperationException("Elements with the name " + name  
                                                        + " are not currently "
                                                        + "supported as sub elements of " 
                                                        + element.getLocalName());
            }
            
        } else {
            child = ctx.getDelegate().parseCustomElement(first, bean.getBeanDefinition());
            id = child.toString();
        }
       
        ctx.getRegistry().registerBeanDefinition(id, child);
        return id;
    }

    protected Element getFirstChild(Element element) {
        Element first = null;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                first = (Element) n;
            }
        }
        return first;
    }

    protected void wireBus(BeanDefinitionBuilder bean, String busId) {
        bean.addPropertyReference("bus", busId);
    }
    
    protected void mapElementToJaxbProperty(Element parent, 
                                            BeanDefinitionBuilder bean, 
                                            QName name,
                                            String propertyName) {
        mapElementToJaxbProperty(parent, bean, name, propertyName, null);
    }
   
    protected void mapElementToJaxbProperty(Element parent, 
                                            BeanDefinitionBuilder bean, 
                                            QName name,
                                            String propertyName, 
                                            Class<?> c) {
        Node data = null;
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(n.getLocalName())
                && name.getNamespaceURI().equals(n.getNamespaceURI())) {
                data = n;
                break;
            }
        }

        if (data == null) {
            return;
        }

        JAXBContext context = null;
        Object obj = null;
        try {
            String pkg = getJaxbPackage();
            if (null != c) {
                pkg = c.getPackage().getName();
            }
            context = JAXBContext.newInstance(pkg, getClass().getClassLoader());
            Unmarshaller u = context.createUnmarshaller();
            if (c != null) {
                obj = u.unmarshal(data, c);
            } else {
                obj = u.unmarshal(data);
            }

            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();

            }
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }

        if (obj != null) {
            bean.addPropertyValue(propertyName, obj);
        }
    }

    protected String getJaxbPackage() {
        return "";
    }

    protected void mapToProperty(BeanDefinitionBuilder bean, String propertyName, String val) {
        if (ID_ATTRIBUTE.equals(propertyName)) {
            return;
        }
        
        if (StringUtils.hasText(val)) {
            if (val.startsWith("#")) {
                bean.addPropertyReference(propertyName, val.substring(1));
            } else {
                bean.addPropertyValue(propertyName, val);
            }
        }
    }
    
    protected boolean isAttribute(String pre, String name) {
        return !"xmlns".equals(name) && (pre == null || !pre.equals("xmlns"))
            && !"abstract".equals(name) && !"lazy-init".equals(name) && !"id".equals(name);
    }

    protected QName parseQName(Element element, String t) {
        String ns = null;
        String pre = null;
        String local = null;

        if (t.startsWith("{")) {
            int i = t.indexOf('}');
            if (i == -1) {
                throw new RuntimeException("Namespace bracket '{' must having a closing bracket '}'.");
            }

            ns = t.substring(1, i);
            t = t.substring(i + 1);
        }

        int colIdx = t.indexOf(':');
        if (colIdx == -1) {
            local = t;
            pre = "";
            
            ns = DOMUtils.getNamespace(element, "");
        } else {
            pre = t.substring(0, colIdx);
            local = t.substring(colIdx + 1);
            
            ns = DOMUtils.getNamespace(element, pre);
        }

        return new QName(ns, local, pre);
    }

    /* This id-or-name resolution logic follows that in Spring's
     * org.springframework.beans.factory.xml.BeanDefinitionParserDelegate object
     * Intent is to have resolution of CXF custom beans follow that of Spring beans
     */    
    protected String getIdOrName(Element elem) {
        String id = elem.getAttribute(BeanDefinitionParserDelegate.ID_ATTRIBUTE);
        
        if (null == id || "".equals(id)) {
            String names = elem.getAttribute(BeanConstants.NAME_ATTR);
            if (null != names) {
                StringTokenizer st = 
                    new StringTokenizer(names, BeanDefinitionParserDelegate.BEAN_NAME_DELIMITERS);
                if (st.countTokens() > 0) {
                    id = st.nextToken();
                }
            }
        }
        return id;
    }

}

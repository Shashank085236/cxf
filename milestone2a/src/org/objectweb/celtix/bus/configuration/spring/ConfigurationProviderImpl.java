package org.objectweb.celtix.bus.configuration.spring;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.objectweb.celtix.bus.jaxb.JAXBUtils;
import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationException;
import org.objectweb.celtix.configuration.ConfigurationProvider;
import org.objectweb.celtix.configuration.Configurator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.io.UrlResource;


public class ConfigurationProviderImpl implements ConfigurationProvider {
  
    public static final String CONFIG_DIR_PROPERTY_NAME = "celtix.config.dir";
    public static final String CONFIG_FILE_PROPERTY_NAME = "celtix.config.file";
    
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurationProviderImpl.class);
    private static Map<UrlResource, CeltixXmlBeanFactory> beanFactories;
  
    private Object bean;
    private Configuration configuration;
    
    
    public void init(Configuration c) {
        configuration = c;
        
        if (null == beanFactories) {
            beanFactories = new HashMap<UrlResource, CeltixXmlBeanFactory>();
        }
        
        CeltixXmlBeanFactory beanFactory =  null;
        UrlResource urlRes = getBeanDefinitionsResource();
        if (null != urlRes) {
            if (!beanFactories.containsKey(urlRes)) {

                if (null != urlRes) {
                    try {
                        beanFactory = new CeltixXmlBeanFactory(urlRes);
                    } catch (BeansException ex) {
                        throw new ConfigurationException(new Message("BEAN_FACTORY_CREATION_EXC", LOG, urlRes
                            .toString()), ex);
                    }
                    beanFactories.put(urlRes, beanFactory);
                }
            } else {
                beanFactory = beanFactories.get(urlRes);
            }  
        }
    
        if (null != beanFactory) { 
            beanFactory.registerCustomEditors(c);
            String beanName = getBeanName();
            try {
                bean = beanFactory.getBean(beanName);
            } catch (NoSuchBeanDefinitionException ex) {
                throw new ConfigurationException(new Message("NO_SUCH_BEAN_EXC", LOG, beanName), ex);
            } catch (BeansException ex) {
                throw new ConfigurationException(new Message("BEAN_CREATION_EXC", LOG, beanName), ex);
            }
        } else {            
            LOG.fine("Not using a bean definitions file.");
        }
        
    }
    
    public Object getObject(String name) {
        // TODO use BeanWrapper instead
        if (null != bean) {
            return invokeGetter(bean, name);
        }  
        return null;
    }
    
    protected Object getBean() {
        return bean;
    }
    
    private Object invokeGetter(Object beanObject, String name) {
        
        String methodName = JAXBUtils.nameToIdentifier(name, JAXBUtils.IdentifierType.GETTER);
        try {
            Method m = beanObject.getClass().getMethod("isSet", new Class[] {String.class});
            Object o = m.invoke(beanObject, new Object[] {name});
            if (!((Boolean)o).booleanValue()) {
                return null;
            }
            m = beanObject.getClass().getMethod(methodName, new Class[] {});
            return m.invoke(beanObject);
            
        } catch (Exception ex) {
            throw new ConfigurationException(new Message("BEAN_INCOVATION_EXC", LOG), ex);
        }         
    }
    
    /**
     * get the id of the ancestor configuration and look for a correspondingly named file 
     * with extension .xml in the directory pointed to by system property
     * celtix.config.dir
     * @param id
     * @return
     */
    
    protected UrlResource getBeanDefinitionsResource() {
        
        UrlResource urlRes = null;
        String url = System.getProperty(CONFIG_FILE_PROPERTY_NAME);
        if (null != url) {
            try {
                urlRes = new UrlResource(url);                
            } catch (MalformedURLException ex) {
                throw new ConfigurationException(new Message("MALFORMED_URL_PROPERTY", LOG, 
                                                             CONFIG_FILE_PROPERTY_NAME), ex);
            }
            return urlRes;
        }
        url = System.getProperty(CONFIG_DIR_PROPERTY_NAME);
        if (null != url) {
            Configuration rootConfiguration = getRootConfiguration();
            String id = rootConfiguration.getName().getLocalPart();
            try {
                urlRes = new UrlResource(url + "/" + id + ".xml");  
            } catch (MalformedURLException ex) {
                throw new ConfigurationException(new Message("MALFORMED_URL_PROPERTY", LOG, 
                                                             CONFIG_DIR_PROPERTY_NAME), ex);
            }
            return urlRes;
        } 
        return null;
    }
    
    private Configuration getRootConfiguration() {
        Configurator parent = configuration.getConfigurator();
        Configurator hook = parent.getHook();
        while (null != hook) {
            parent = hook;
            hook = parent.getHook();
        }
        return parent.getConfiguration();
    }
    
    private String getBeanName() {
        StringBuffer buf = new StringBuffer();
        Configuration c = configuration;
        while (null != c) {
            if (buf.length() > 0) {
                buf.insert(0, ".");
            }
            buf.insert(0, c.getName().getLocalPart());
            Configurator configurator = c.getConfigurator().getHook();
            c = null == configurator ? null : configurator.getConfiguration();            
        }
        return buf.toString();
    }
}

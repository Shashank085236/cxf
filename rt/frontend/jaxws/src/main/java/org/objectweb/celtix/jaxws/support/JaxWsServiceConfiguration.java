package org.apache.cxf.jaxws.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ResourceBundle;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.ResponseWrapper;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.InterfaceInfo;

public class JaxWsServiceConfiguration extends AbstractServiceConfiguration {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JaxWsServiceConfiguration.class);

    private JaxwsImplementorInfo implInfo;
     

    @Override
    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        super.setServiceFactory(serviceFactory);
        implInfo = new JaxwsImplementorInfo(serviceFactory.getServiceClass());        
    }

    WebService getConcreteWebServiceAttribute() {
        return getServiceFactory().getServiceClass().getAnnotation(WebService.class);
    }

    WebService getPortTypeWebServiceAttribute() {
        Class<?> epi = getEndpointClass();
        WebService ws = null;
        if (epi != null) {
            ws = epi.getAnnotation(WebService.class);
        }
        if (ws == null) {
            ws = getConcreteWebServiceAttribute();
        }
        return ws;
    }

    Class getEndpointClass() {
        Class endpointInterface = implInfo.getSEIClass();
        if (null == endpointInterface) {
            endpointInterface = implInfo.getImplementorClass();
        }
        return endpointInterface;
    }

    @Override
    public String getServiceName() {
        WebService ws = getConcreteWebServiceAttribute();
        if (ws != null) {
            return ws.serviceName();
        }

        return null;
    }

    @Override
    public String getServiceNamespace() {
        WebService ws = getConcreteWebServiceAttribute();
        if (ws != null) {
            return ws.targetNamespace();
        }

        return null;
    }

    @Override
    public URL getWsdlURL() {
        WebService ws = getPortTypeWebServiceAttribute();
        if (ws != null && ws.wsdlLocation().length() > 0) {
            try {
                URIResolver resolver = new URIResolver(ws.wsdlLocation());
                if (resolver.isResolved()) {
                    return resolver.getURI().toURL();
                }
            } catch (IOException e) {
                throw new ServiceConstructionException(new Message("LOAD_WSDL_EXC", 
                                                                   BUNDLE, 
                                                                   ws.wsdlLocation()),
                                                       e);
            }
        }
        return null;
    }

    @Override
    public QName getOperationName(InterfaceInfo service, Method method) {
        method = getDeclaredMethod(method);

        WebMethod wm = method.getAnnotation(WebMethod.class);
        if (wm != null) {
            String name = wm.operationName();
            if (name == null) {
                name = method.getName();
            }

            return new QName(service.getName().getNamespaceURI(), name);
        }

        return null;
    }

    @Override
    public Boolean isOperation(Method method) {
        method = getDeclaredMethod(method);
        if (method != null) {
            WebMethod wm = method.getAnnotation(WebMethod.class);
            if (wm != null) {
                if (wm.exclude()) {
                    return Boolean.FALSE;
                } else {
                    return Boolean.TRUE;
                }
            } else if (!method.getDeclaringClass().isInterface()) {
                return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }

    private Method getDeclaredMethod(Method method) {
        Class endpointClass = getEndpointClass();

        if (!method.getDeclaringClass().equals(endpointClass)) {
            try {
                method = endpointClass.getMethod(method.getName(), (Class[])method.getParameterTypes());
            } catch (SecurityException e) {
                throw new ServiceConstructionException(e);
            } catch (NoSuchMethodException e) {
                // Do nothing
            }
        }
        return method;
    }

    @Override
    public Class getResponseWrapper(Method selected) {
        Method m = getDeclaredMethod(selected);
        
        ResponseWrapper rw = m.getAnnotation(ResponseWrapper.class);
        if (rw == null) {
            return null;
        }
        
        String clsName = rw.className();
        if (clsName.length() > 0) {
            try {
                return ClassLoaderUtils.loadClass(clsName, getClass());
            } catch (ClassNotFoundException e) {
                throw new ServiceConstructionException(e);
            }
        }
        
        return null;
    }

    @Override
    public Class getRequestWrapper(Method selected) {
        Method m = getDeclaredMethod(selected);
        
        ResponseWrapper rw = m.getAnnotation(ResponseWrapper.class);
        if (rw == null) {
            return null;
        }
        
        String clsName = rw.className();
        
        if (clsName.length() > 0) {
            try {
                return ClassLoaderUtils.loadClass(clsName, getClass());
            } catch (ClassNotFoundException e) {
                throw new ServiceConstructionException(e);
            }
        }
        
        return null;
    }
    
}

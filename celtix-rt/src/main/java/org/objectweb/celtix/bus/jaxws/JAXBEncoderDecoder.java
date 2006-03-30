package org.objectweb.celtix.bus.jaxws;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebMethod;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.ws.Holder;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.w3c.dom.Node;

import org.objectweb.celtix.common.util.StringUtils;

/**
 * JAXBEncoderDecoder
 * @author apaibir
 */
public final class JAXBEncoderDecoder {
    
    static Map<Class, JAXBContext> contextMap = new ConcurrentHashMap<Class, JAXBContext>();
    
    private JAXBEncoderDecoder() {        
    }
    
    public static JAXBContext createJAXBContextForClass(Class cls) throws JAXBException {
        JAXBContext context = contextMap.get(cls);
        if (context == null) {
            Set<Class> classes = new HashSet<Class>();
            getClassesForContext(cls, classes, cls.getClassLoader());
            try {
                context = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
                contextMap.put(cls, context);
            } catch (JAXBException ex) {
                throw ex;
            }
        }
        return context;
    }
    
    private static Class getValidClass(Class cls) {
        if (cls.isEnum()) {
            return cls;
        }
        if (cls.isArray()) {
            return getValidClass(cls.getComponentType());
        }

        if (cls == Holder.class || cls == Object.class || cls == String.class) {
            cls = null;
        } else if (cls.isPrimitive() || cls.isInterface() || cls.isAnnotation()) {
            cls = null;
        }
        if (cls != null) {
            try {
                if (cls.getConstructor(new Class[0]) == null) {
                    cls = null;
                }
            } catch (NoSuchMethodException ex) {
                cls = null;
            }
        }
        return cls;
    }
    private static void addClass(Class cls, Set<Class> classes) {
        if (cls.isArray()) {
            classes.add(cls);
            return;
            /*
            Class c2 = getValidClass(cls);
            if (c2 != null) {
                classes.add(cls);
                return;
            }*/
        }
        cls = getValidClass(cls);
        if (null != cls) {
            if (cls.isEnum()) {
                // The object factory stuff doesn't work for enums
                classes.add(cls);
            }
            String name = cls.getPackage().getName() + ".ObjectFactory";
            try {
                cls = Class.forName(name, false, cls.getClassLoader());
                if (cls != null) {
                    classes.add(cls);
                }
            } catch (ClassNotFoundException ex) {
                //cannot add factory, just add the class
                classes.add(cls);
            }
        }
    }
    private static void addType(Type cls, Set<Class> classes) {
        if (cls instanceof Class) {
            addClass((Class)cls, classes);
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                addType(t2, classes);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            addType(gt.getGenericComponentType(), classes);
        }
    }
    
    //collect ALL the classes that are accessed by the class
    private static void getClassesForContext(Class<?> theClass, Set<Class> classes, ClassLoader loader) {
        Method methods[] = theClass.getMethods();
        for (Method meth : methods) {
            //only methods marked as WebMethods are interesting to us
            WebMethod webMethod = meth.getAnnotation(WebMethod.class);
            if (webMethod == null) {
                continue;
            }
            
            for (Type t : meth.getGenericParameterTypes()) {
                addType(t, classes);
            }
            addType(meth.getGenericReturnType(), classes);
            
            if (meth.getReturnType().isArray()) {
                addClass(meth.getReturnType(), classes);
            }
            for (Class cls : meth.getParameterTypes()) {
                addClass(cls, classes);
            }
            
            for (Class<?> cls : meth.getExceptionTypes()) {
                //addClass(cls, classes);
                try {
                    Method fim = cls.getMethod("getFaultInfo", new Class[0]);
                    addClass(fim.getReturnType(), classes);
                } catch (NoSuchMethodException ex) {
                    //ignore - not a valid JAXB fault thing
                }
            }
            try {
                //Get the RequestWrapper
                RequestWrapper reqWrapper = meth.getAnnotation(RequestWrapper.class);
                if (reqWrapper != null) {
                    Class cls = Class.forName(reqWrapper.className(), false,
                                        loader);
                    addClass(cls, classes);
                }
                //Get the RequestWrapper
                ResponseWrapper respWrapper = meth.getAnnotation(ResponseWrapper.class);
                if (respWrapper != null) {
                    Class cls = Class.forName(respWrapper.className(),
                                              false,
                                              loader);
                    addClass(cls, classes);
                }
            } catch (ClassNotFoundException ex) {
                //ignore
            }
        }

        for (Class intf : theClass.getInterfaces()) {
            getClassesForContext(intf, classes, loader);
        }
        if (theClass.getSuperclass() != null) {
            getClassesForContext(theClass.getSuperclass(), classes, loader);
        }
    }
    
    public static void marshall(JAXBContext context, Schema schema,
                                Object elValue, QName elNname,  Node destNode) {
        
        try {
            if (context == null) {
                context = JAXBContext.newInstance(elValue.getClass());
            }
            Object mObj = elValue;
            Marshaller u = context.createMarshaller();
            u.setProperty(Marshaller.JAXB_ENCODING , "UTF-8");
            u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            u.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            if (elValue.getClass().isAnnotationPresent(XmlRootElement.class)) {
                String packageName = elValue.getClass().getPackage().getName();
                Class<?> objectFactory = Class.forName(packageName + ".ObjectFactory", false, 
                                                       elValue.getClass().getClassLoader());

                Method methods[] = objectFactory.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].equals(elValue.getClass())) {

                        XmlElementDecl elementDecl = method.getAnnotation(XmlElementDecl.class);
                        if (null != elementDecl) {
                            QName elementType = new QName(elementDecl.namespace(), elementDecl.name());
                            if (elementType.equals(elNname)) {
                                mObj = method.invoke(objectFactory.newInstance(),
                                                    elValue);                        
                            }
                        }
                    } 
                }
            } else {
                mObj = JAXBElement.class.getConstructor(new Class[] {QName.class, Class.class, Object.class})
                    .newInstance(elNname, mObj.getClass(), mObj);
            }
            u.setSchema(schema);
            u.marshal(mObj, destNode);
        } catch (MarshalException me) {
            // It's helpful to include the cause in the case of
            // schema validation exceptions.
            String message = "Marshalling error ";
            if (me.getCause() != null) {
                message += me.getCause();
            }
            throw new ProtocolException(message, me);
        } catch (Exception ex) {
            throw new ProtocolException("Marshalling Error", ex);
        }
    }
    
    public static Object unmarshall(JAXBContext context, Schema schema,
                                    Node srcNode, QName elName, Class<?> clazz) {
        Object obj = null;
        try {
            if (context == null) {
                context = JAXBContext.newInstance(clazz);
            }
            Unmarshaller u = context.createUnmarshaller();
            u.setSchema(schema);

            obj = (clazz != null) ? u.unmarshal(srcNode, clazz) : u.unmarshal(srcNode);
            
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                if (isSame(el.getName(), elName)) {
                    obj = el.getValue();
                }
            }
        } catch (UnmarshalException ue) {
            // It's helpful to include the cause in the case of
            // schema validation exceptions.
            String message = "Unmarshalling error ";
            if (ue.getCause() != null) {
                message += ue.getCause();
            }
            throw new ProtocolException(message, ue);
        } catch (Exception ex) {
            throw new ProtocolException("Unmarshalling error", ex);
        }
        return obj;
    }

    private static boolean isSame(QName messageQName, QName methodQName) {
        boolean same = false;
        if (StringUtils.isEmpty(messageQName.getNamespaceURI())) {
            same = messageQName.getLocalPart().equals(methodQName.getLocalPart());
        } else {
            same = messageQName.equals(methodQName);
        }
        return same;
    }

    public static Object unmarshall(JAXBContext context, Schema schema, Node srcNode, QName elName) {
        Object obj = null;
        try {
            Unmarshaller u = context.createUnmarshaller();
            u.setSchema(schema);

            obj = u.unmarshal(srcNode);
            
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                if (el.getName().equals(elName)) {
                    obj = el.getValue();
                }
            }
        } catch (UnmarshalException ue) {
            // It's helpful to include the cause in the case of
            // schema validation exceptions.
            String message = "Unmarshalling error ";
            if (ue.getCause() != null) {
                message += ue.getCause();
            }
            throw new ProtocolException(message, ue);
        } catch (Exception ex) {
            throw new ProtocolException("Unmarshalling error", ex);
        }
        return obj;
    }
    
    public static Class getClassFromType(Type t) {
        if (t instanceof Class) {
            return (Class)t;
        } else if (t instanceof GenericArrayType) {
            GenericArrayType g = (GenericArrayType)t;
            return Array.newInstance(getClassFromType(g.getGenericComponentType()), 0).getClass();
        } else if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType)t;
            return getClassFromType(p.getRawType());
        }
        //TypeVariable and WildCardType are not handled as it is unlikely such Types will 
        // JAXB Code Generated.
        assert false;
        throw new IllegalArgumentException("Cannot get Class object from unknown Type");
    }
    
    public static String toString(Object obj) throws JAXBException {
        String name = obj.getClass().getPackage().getName();
        JAXBContext context = JAXBContext.newInstance(name);
        JAXBElement<Object> el = new JAXBElement<Object>(new QName("test"), Object.class, obj);
        Marshaller m = context.createMarshaller();
        StringWriter writer = new StringWriter();
        m.marshal(el, writer);

        return writer.toString();       
    }
}

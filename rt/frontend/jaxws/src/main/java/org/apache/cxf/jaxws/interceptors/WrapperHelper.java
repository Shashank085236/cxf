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

package org.apache.cxf.jaxws.interceptors;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBUtils;

public abstract class WrapperHelper {
    private static final Class NO_PARAMS[] = new Class[0];

    
    public abstract Object createWrapperObject(List<?> lst) 
        throws Fault;
    
    public abstract List<Object> getWrapperParts(Object o) throws Fault;

    
    public static WrapperHelper createWrapperHelper(Class<?> wrapperType,
                                                    List<String> partNames,
                                                    List<String> elTypeNames,
                                                    List<Class<?>> partClasses) {
        List<Method> getMethods = new ArrayList<Method>(partNames.size());
        List<Method> setMethods = new ArrayList<Method>(partNames.size());
        List<Method> jaxbMethods = new ArrayList<Method>(partNames.size());
        List<Field> fields = new ArrayList<Field>(partNames.size());
        
        Method allMethods[] = wrapperType.getMethods();
        
        String objectFactoryClassName = wrapperType.getPackage().getName()
                                        + ".ObjectFactory";

        Object objectFactory = null;
        try {
            objectFactory = wrapperType.getClassLoader().loadClass(objectFactoryClassName).newInstance();
        } catch (Exception e) {
            //ignore, probably won't need it
        }
        Method allOFMethods[];
        if (objectFactory != null) {
            allOFMethods = objectFactory.getClass().getMethods(); 
        } else {
            allOFMethods = new Method[0];
        }
        
        for (int x = 0; x < partNames.size(); x++) {
            String partName = partNames.get(x);
            if (partName == null) {
                getMethods.add(null);
                setMethods.add(null);
                fields.add(null);
                jaxbMethods.add(null);
                continue;
            }
            
            String elementType = elTypeNames.get(x);
            
            String getAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.GETTER);
            String setAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.SETTER);
            Method getMethod = null;
            Method setMethod = null;
            try {
                getMethod = wrapperType.getMethod(getAccessor, NO_PARAMS); 
            } catch (NoSuchMethodException ex) {
                //ignore for now
            }

            Field elField = getElField(partName, wrapperType);
            if (getMethod == null
                && elementType != null
                && "boolean".equals(elementType.toLowerCase())
                && (elField == null
                    || (!Collection.class.isAssignableFrom(elField.getType())
                    && !elField.getType().isArray()))) {
        
                try {
                    String newAcc = getAccessor.replaceFirst("get", "is");
                    getMethod = wrapperType.getMethod(newAcc, NO_PARAMS); 
                } catch (NoSuchMethodException ex) {
                    //ignore for now
                }            
            }                        
            if (getMethod == null 
                && "return".equals(partName)) {
                //RI generated code uses this
                try {
                    getMethod = wrapperType.getMethod("get_return", NO_PARAMS);
                } catch (NoSuchMethodException ex) {
                    try {
                        getMethod = wrapperType.getMethod("is_return",
                                                          new Class[0]);
                    } catch (NoSuchMethodException ex2) {
                        //ignore for now
                    } 
                }                
            }
            String setAccessor2 = setAccessor;
            if ("return".equals(partName)) {
                //some versions of jaxb map "return" to "set_return" instead of "setReturn"
                setAccessor2 = "set_return";
            }

            for (Method method : allMethods) {
                if (method.getParameterTypes() != null && method.getParameterTypes().length == 1
                    && (setAccessor.equals(method.getName())
                        || setAccessor2.equals(method.getName()))) {
                    setMethod = method;
                    break;
                }
            }
            
            getMethods.add(getMethod);
            setMethods.add(setMethod);
            if (setMethod != null
                && JAXBElement.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {
                
                String methodName = "create" + wrapperType.getSimpleName()
                    + setMethod.getName().substring(3);

                for (Method m : allOFMethods) {
                    if (m.getName().equals(methodName)) {
                        jaxbMethods.add(m);
                    }
                }
            } else {
                jaxbMethods.add(null);
            }
            
            if (elField != null) {
                // JAXB Type get XmlElement Annotation
                XmlElement el = elField.getAnnotation(XmlElement.class);
                if (el != null
                    && partName.equals(el.name())) {
                    elField.setAccessible(true);
                    fields.add(elField);
                } else {
                    fields.add(null);
                } 
            } else {
                fields.add(null);
            }
            
        }
        
        return createWrapperHelper(wrapperType,
                                 setMethods.toArray(new Method[setMethods.size()]),
                                 getMethods.toArray(new Method[getMethods.size()]),
                                 jaxbMethods.toArray(new Method[jaxbMethods.size()]),
                                 fields.toArray(new Field[fields.size()]),
                                 objectFactory);
    }

    private static Field getElField(String partName, Class<?> wrapperType) {
        String fieldName = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.VARIABLE);
        for (Field field : wrapperType.getDeclaredFields()) {
            XmlElement el = field.getAnnotation(XmlElement.class);
            if (el != null
                && partName.equals(el.name())) {
                return field;
            }
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }        
        return null;
    }

    private static Object getValue(Method method, Object in) throws IllegalAccessException,
        InvocationTargetException {
        if ("javax.xml.bind.JAXBElement".equals(method.getReturnType().getCanonicalName())) {
            JAXBElement je = (JAXBElement)method.invoke(in);
            return je == null ? je : je.getValue();
        } else {
            return method.invoke(in);
        }
    }
    private static WrapperHelper createWrapperHelper(Class<?> wrapperType,
                                                      Method setMethods[],
                                                      Method getMethods[],
                                                      Method jaxbMethods[],
                                                      Field fields[],
                                                      Object objectFactory) {
        WrapperHelper wh = compileWrapperHelper(wrapperType,
                                                setMethods,
                                                getMethods,
                                                jaxbMethods,
                                                fields,
                                                objectFactory);
        if (wh == null) {
            wh = new ReflectWrapperHelper(wrapperType,
                                          setMethods,
                                          getMethods,
                                          jaxbMethods,
                                          fields,
                                          objectFactory);
        }
        return wh;
    }
    private static WrapperHelper compileWrapperHelper(Class<?> wrapperType,
                                                     Method setMethods[],
                                                     Method getMethods[],
                                                     Method jaxbMethods[],
                                                     Field fields[],
                                                     Object objectFactory) {
        try {
            Class.forName("org.objectweb.asm.ClassWriter");
            return WrapperHelperCompiler.compileWrapperHelper(wrapperType, setMethods, getMethods,
                                                              jaxbMethods, fields, objectFactory);
        } catch (ClassNotFoundException e) {
            //ASM not found, just use reflection based stuff
        }
        return null;
    }    
    
    static class ReflectWrapperHelper extends WrapperHelper {
        final Class<?> wrapperType;
        final Method setMethods[];
        final Method getMethods[];
        final Method jaxbObjectMethods[];
        final Field fields[];
        final Object objectFactory;
                     
        ReflectWrapperHelper(Class<?> wt,
                      Method sets[],
                      Method gets[],
                      Method jaxbs[],
                      Field f[],
                      Object of) {
            setMethods = sets;
            getMethods = gets;
            fields = f;
            jaxbObjectMethods = jaxbs;
            wrapperType = wt;
            objectFactory = of;
        }
        
        public Object createWrapperObject(List<?> lst) 
            throws Fault {
            
            try {
                Object ret = wrapperType.newInstance();

                for (int x = 0; x < setMethods.length; x++) {
                    if (getMethods[x] == null
                        && setMethods[x] == null 
                        && fields[x] == null) {
                        //this part is a header or something
                        //that is not part of the wrapper.
                        continue;
                    }
                    Object o = lst.get(x);
                    if (jaxbObjectMethods[x] != null) {
                        o = jaxbObjectMethods[x].invoke(objectFactory, o);
                    }
                    if (o instanceof List) {
                        List<Object> col = CastUtils.cast((List)getMethods[x].invoke(ret));
                        if (col == null) {
                            //broken generated java wrappers
                            if (setMethods[x] != null) {
                                setMethods[x].invoke(ret, o);
                            } else {
                                fields[x].set(ret, lst.get(x));
                            }
                        } else {
                            List<Object> olst = CastUtils.cast((List)o);
                            col.addAll(olst);
                        }
                    } else if (setMethods[x] != null) {
                        setMethods[x].invoke(ret, o);
                    } else if (fields[x] != null) {
                        fields[x].set(ret, lst.get(x));
                    }
                }
                return ret;
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        }
        
        public List<Object> getWrapperParts(Object o) throws Fault {
            try {
                List<Object> ret = new ArrayList<Object>(getMethods.length);
                for (int x = 0; x < getMethods.length; x++) {
                    if (getMethods[x] != null) {
                        ret.add(getValue(getMethods[x], o));
                    } else if (fields[x] != null) {
                        ret.add(fields[x].get(o));
                    } else {
                        //placeholder
                        ret.add(null);
                    }
                }
                
                return ret;
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        }
    }
    
}

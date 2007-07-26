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

package org.apache.cxf.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.jws.WebService;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Features;
import org.apache.cxf.helpers.CastUtils;

public class AnnotationInterceptors {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AnnotationInterceptors.class);
    
    private Class<?> clazz;
    
    public AnnotationInterceptors(Class<?> clz) {
        clazz = clz;
    }
    
    public List<Interceptor> getInFaultInterceptors() {
        return CastUtils.cast(getAnnotationObject(InFaultInterceptors.class), Interceptor.class);
    }
    
    @SuppressWarnings ("unchecked")
    private List getAnnotationObject(Class annotationClazz) {
        Annotation  annotation = clazz.getAnnotation(annotationClazz);
        if (annotation == null) {
            WebService ws = clazz.getAnnotation(WebService.class);
            if (ws != null && !StringUtils.isEmpty(ws.endpointInterface())) {
                String seiClassName = ws.endpointInterface().trim();
                Class seiClass = null;
                try {
                    seiClass = ClassLoaderUtils.loadClass(seiClassName, this.getClass());
                } catch (ClassNotFoundException e) {
                    throw new Fault(new Message("COULD_NOT_FIND_SEICLASS", BUNDLE, seiClass), e);
                }
                annotation = seiClass.getAnnotation(annotationClazz);
                if (annotation != null) {
                    return initializeAnnotationObjects(getAnnotationObjectNames(annotation));
                }
            }
        } else {
            return initializeAnnotationObjects(getAnnotationObjectNames(annotation));
        }
        return null;
    }
    
    private String[] getAnnotationObjectNames(Annotation ann) {
        if (ann instanceof InFaultInterceptors) {
            return ((InFaultInterceptors)ann).interceptors();
        } else if (ann instanceof InInterceptors) {
            return ((InInterceptors)ann).interceptors();
        } else if (ann instanceof OutFaultInterceptors) {
            return ((OutFaultInterceptors)ann).interceptors();
        } else if (ann instanceof OutInterceptors) {
            return ((OutInterceptors)ann).interceptors();
        } else if (ann instanceof Features) {
            return ((Features)ann).features();
        }
        
        throw new UnsupportedOperationException("Doesn't support the annotation: " + ann);
    }
    
    @SuppressWarnings("unchecked")
    private List initializeAnnotationObjects(String[] annotationObjects) {
        List theAnnotationObjects = new ArrayList();
        if (annotationObjects != null && annotationObjects.length > 0) {
            for (String annObjectName : annotationObjects) {
                Object object = null;
                try {
                    object = ClassLoaderUtils.loadClass(annObjectName, this.getClass()).newInstance();
                } catch (ClassNotFoundException e) {
                    throw new Fault(new Message("COULD_NOT_CREATE_ANNOTATION_OBJECT", 
                                                    BUNDLE, annObjectName), e);
                } catch (InstantiationException ie) {
                    throw new Fault(new Message("COULD_NOT_CREATE_ANNOTATION_OBJECT", 
                                                    BUNDLE, annObjectName), ie);
                } catch (IllegalAccessException iae) {
                    throw new Fault(new Message("COULD_NOT_CREATE_ANNOTATION_OBJECT", 
                                                    BUNDLE, annObjectName), iae);
                }
                if (object != null) {
                    theAnnotationObjects.add(object);
                }
            }
        }
        return theAnnotationObjects;
    }


    public List<Interceptor> getInInterceptors() {
        return CastUtils.cast(getAnnotationObject(InInterceptors.class), Interceptor.class);
    }

    public List<Interceptor> getOutFaultInterceptors() {
        return CastUtils.cast(getAnnotationObject(OutFaultInterceptors.class), Interceptor.class);
    }

    public List<Interceptor> getOutInterceptors() {
        return CastUtils.cast(getAnnotationObject(OutInterceptors.class), Interceptor.class);
    }
        
    public List<AbstractFeature> getFeatures() {
        return CastUtils.cast(getAnnotationObject(Features.class), AbstractFeature.class);
    }

}

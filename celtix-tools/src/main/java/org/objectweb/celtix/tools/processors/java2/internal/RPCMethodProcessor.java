package org.objectweb.celtix.tools.processors.java2.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebFault;

import com.sun.xml.bind.api.TypeReference;

import org.objectweb.celtix.tools.common.ToolException;
import org.objectweb.celtix.tools.common.model.JavaMethod;
import org.objectweb.celtix.tools.common.model.JavaParameter;
import org.objectweb.celtix.tools.common.model.JavaType;
import org.objectweb.celtix.tools.common.model.WSDLException;
import org.objectweb.celtix.tools.common.model.WSDLModel;
import org.objectweb.celtix.tools.common.model.WSDLParameter;
import org.objectweb.celtix.tools.utils.AnnotationUtil;

public class RPCMethodProcessor {

    private WSDLModel model;

    public RPCMethodProcessor(WSDLModel wmodel) {
        model = wmodel;
    }

    public void process(JavaMethod javaMethod, Method method) {
        javaMethod.setSoapStyle(SOAPBinding.Style.RPC);
        javaMethod.setWrapperStyle(true);
        setMethodUse(javaMethod, method);

        String targetNS = model.getTargetNameSpace();
        WSDLParameter request = new WSDLParameter();
        request.setName(method.getName());
        request.setStyle(JavaType.Style.IN);

        request.setTargetNamespace(targetNS);
        javaMethod.addRequest(request);

        boolean isOneway = method.isAnnotationPresent(Oneway.class);
        if (!isOneway) {
            QName resQN = new QName(targetNS, method.getName() + "Response");
            TypeReference typeRef = new TypeReference(resQN, this.getClass(), new Annotation[0]);
            WSDLParameter response = new WSDLParameter();
            response.setName(method.getName() + "Response");
            response.setStyle(JavaType.Style.OUT);
            javaMethod.addResponse(response);

            Class returnType = method.getReturnType();
            String resultName = method.getName() + "Response";
            String resultTNS = targetNS;
            String resultPartName = null;
            WebResult webResult = method.getAnnotation(WebResult.class);
            boolean webResultHeader = false;
            if (webResult != null) {
                resultName = webResult.name().length() > 0 ? webResult.name() : resultName;
                resultPartName = webResult.partName().length() > 0 ? webResult.partName() : resultName;
                resultTNS = webResult.targetNamespace().length() > 0
                    ? webResult.targetNamespace() : resultTNS;
                webResultHeader = webResult.header();
            }
            QName resultQName = new QName(resultTNS, resultName);
            if (returnType != null && (!"void".equals(returnType.getName()))) {
                // Annotation[] rann = method.getAnnotations();
                Annotation[] rann = new Annotation[0];
                typeRef = new TypeReference(resultQName, returnType, rann);
                JavaParameter returnParameter = new JavaParameter(resultName, typeRef, JavaType.Style.OUT);
                returnParameter.setPartName(resultPartName);
                returnParameter.setTargetNamespace(resultTNS);
                returnParameter.setHeader(webResultHeader);
                response.addChildren(returnParameter);
            }
        }
        // get WebParam
        List<JavaParameter> paras = processWebPara(method);
        for (JavaParameter jp : paras) {
            request.addChildren(jp);
        }
        processExceptions(javaMethod, method);
    }

    private void setMethodUse(JavaMethod javaMethod, Method method) {
        SOAPBinding binding = method.getAnnotation(SOAPBinding.class);
        if (binding != null) {
            javaMethod.setSoapUse(binding.use());
        } else {
            javaMethod.setSoapUse(this.model.getUse());
        }
    }

    private List<JavaParameter> processWebPara(Method method) {
        // processWebparam
        Class<?>[] parameterTypes = method.getParameterTypes();
        Type[] parameterGenTypes = method.getGenericParameterTypes();
        Annotation[][] paraAnns = AnnotationUtil.getPrivParameterAnnotations(method);
        List<JavaParameter> paras = new ArrayList<JavaParameter>();
        int i = 0;
        for (Class clazzType : parameterTypes) {
            String paraName = "arg" + i;
            String partName;
            String paraTNS = model.getTargetNameSpace();
            Class clazz = clazzType;
            boolean holder = isHolder(clazzType);
            if (holder) {
                clazz = getHoldedClass(clazzType, parameterGenTypes[i]);
            }
            for (Annotation anno : paraAnns[i]) {
                if (anno.annotationType() == WebParam.class) {
                    WebParam webParam = (WebParam)anno;
                    paraName = webParam.name().length() > 0 ? webParam.name() : paraName;
                    partName = webParam.partName().length() > 0 ? webParam.partName() : paraName;
                    paraTNS = webParam.targetNamespace().length() > 0
                        ? paraTNS = webParam.targetNamespace() : paraTNS;

                    QName requestQN = new QName(paraTNS, paraName);
                    TypeReference typeref = new TypeReference(requestQN, clazz, paraAnns[i]);
                    JavaParameter jp;
                    if (holder) {
                        if (webParam.mode() == WebParam.Mode.INOUT) {
                            jp = new JavaParameter(typeref.tagName.getLocalPart(), typeref,
                                                   JavaType.Style.INOUT);
                        } else {
                            jp = new JavaParameter(typeref.tagName.getLocalPart(), typeref,
                                                   JavaType.Style.OUT);
                        }
                    } else {
                        jp = new JavaParameter(typeref.tagName.getLocalPart(), typeref, JavaType.Style.IN);
                    }
                    jp.setName(paraName);
                    jp.setPartName(partName);
                    jp.setHeader(webParam.header());
                    jp.setTargetNamespace(paraTNS);
                    paras.add(jp);
                }
            }
            i++;
        }

        return paras;
    }

    private void processExceptions(JavaMethod jmethod, Method method) {
        for (Type exception : method.getGenericExceptionTypes()) {
            if (RemoteException.class.isAssignableFrom((Class)exception)) {
                continue;
            }
            Annotation[] anns = null;
            Class<?> exClass = (Class<?>)exception;
            String exNameSpace = model.getTargetNameSpace();
            String exName = exClass.getSimpleName();
            Class exReturnType = null;
            Method faultInfo = null;
            try {
                faultInfo = exClass.getMethod("getFaultInfo", new Class[0]);
            } catch (SecurityException e) {
                throw new ToolException(e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                throw new ToolException(e.getMessage(), e);
            }

            if (faultInfo != null) {
                WebFault wf = exClass.getAnnotation(WebFault.class);
                exReturnType = faultInfo.getReturnType();
                anns = faultInfo.getAnnotations();
                if (wf.targetNamespace().length() > 0) {
                    exNameSpace = wf.targetNamespace();
                }
                exName = wf.name();
            }

            QName exQName = new QName(exNameSpace, exName);
            TypeReference tf = new TypeReference(exQName, exReturnType, anns);
            WSDLException wsdlEx = new WSDLException(exClass, tf);
            jmethod.addWSDLException(wsdlEx);

        }
    }

    private boolean isHolder(Class cType) {
        return Holder.class.isAssignableFrom(cType);
        // set the actual type argument of Holder in the TypeReference
    }

    private Class getHoldedClass(Class holderClazz, Type type) {
        ParameterizedType pt = (ParameterizedType)type;
        return getClass(pt.getActualTypeArguments()[0]);
    }

    private Class getClass(Type type) {
        if (type instanceof Class) {
            return (Class)type;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)type;
            Class compType = getClass(gt.getGenericComponentType());
            return java.lang.reflect.Array.newInstance(compType, 0).getClass();
        }
        return Object.class;
    }

}

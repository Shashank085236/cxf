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

package org.apache.cxf.tools.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.jws.soap.SOAPBinding;
import javax.wsdl.OperationType;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolException;

public class JavaMethod implements JavaAnnotatable {
    private static final Logger LOG = LogUtils.getL7dLogger(JavaMethod.class);
    private String name;
    private String operationName;
    private JavaReturn javaReturn;
    private OperationType style;
    private String soapAction;
    private SOAPBinding.Style soapStyle;
    private SOAPBinding.Use soapUse;
    private boolean wrapperStyle;
    private boolean enableMime = true;
    private JavaInterface javaInterface;
    private final List<JavaParameter> parameters = new ArrayList<JavaParameter>();
    private final List<JavaException> exceptions = new ArrayList<JavaException>();
    private final Map<String, JavaAnnotation> annotations = new HashMap<String, JavaAnnotation>();

    private JavaCodeBlock block;
    
    public JavaMethod() {
        this(new JavaInterface());
    }

    public JavaMethod(JavaInterface i) {
        this.javaInterface = i;
        this.javaReturn = new JavaReturn();
    }

    public void clear() {
        parameters.clear();
        javaReturn = new JavaReturn();
    }

    public String getSignature() {
        StringBuffer sb = new StringBuffer();
        sb.append(javaReturn.getName());
        sb.append("#");
        sb.append(javaInterface.getPackageName());
        sb.append(".");
        sb.append(javaInterface.getName());
        sb.append("#");
        sb.append(name);
        sb.append("[");
        for (JavaParameter param : parameters) {
            sb.append(param.getName());
            sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public JavaInterface getInterface() {
        return this.javaInterface;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public void setOperationName(String arg) {
        this.operationName = arg;
    }

    public JavaReturn getReturn() {
        return javaReturn;
    }

    public void setReturn(JavaReturn rt) {
        this.javaReturn = rt;
    }

    public boolean hasParameter(String paramName) {
        for (int i = 0; i < parameters.size(); i++) {
            if (paramName.equals((parameters.get(i)).getName())) {
                return true;
            }
        }
        return false;
    }
    
    private void replaceParameter(JavaParameter p1, JavaParameter p2) {
        int index = ((ArrayList)parameters).indexOf(p1);
        parameters.remove(index);
        parameters.add(index, p2);
    }

    public void addParameter(JavaParameter param) {
        if (hasParameter(param.getName())) {
            JavaParameter paramInList = getParameter(param.getName());
            if (paramInList.isIN() || paramInList.isINOUT()) {
                //removeParameter(paramInList);
                replaceParameter(paramInList, param);
                return;
            } else {
                Message message = new Message("PARAMETER_ALREADY_EXIST", LOG, param.getName());
                throw new ToolException(message);
            }
        }
        parameters.add(param);
    }

    public JavaParameter getParameter(String paramName) {
        for (int i = 0; i < parameters.size(); i++) {
            JavaParameter jParam = parameters.get(i);
            if (paramName.equals(jParam.getName())) {
                return jParam;
            }
        }
        return null;
    }

    public List<JavaParameter> getParameters() {
        return parameters;
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public boolean hasException(JavaException exception) {
        return exceptions.contains(exception);
    }

    public void addException(JavaException exception) {
        if (hasException(exception)) {
            Message message = new Message("EXCEPTION_ALREADY_EXIST", LOG, exception.getName());
            throw new ToolException(message);
        }
        exceptions.add(exception);
    }

    public List<JavaException> getExceptions() {
        return exceptions;
    }

    public OperationType getStyle() {
        return this.style;
    }

    public void setStyle(OperationType ot) {
        this.style = ot;
    }

    public boolean isOneWay() {
        return OperationType.ONE_WAY.equals(getStyle());
    }

    public boolean isWrapperStyle() {
        return this.wrapperStyle;
    }

    public void setWrapperStyle(boolean w) {
        this.wrapperStyle = w;
    }
    
    
    public boolean enableMime() {
        return this.enableMime;
    }
    
    public void setMimeEnable(boolean arg) {
        enableMime = arg;
    }

    public void setSoapStyle(SOAPBinding.Style sty) {
        this.soapStyle = sty;
    }

    public SOAPBinding.Style getSoapStyle() {
        return this.soapStyle;
    }

    public void setSoapAction(String action) {
        this.soapAction = action;
    }

    public String getSoapAction() {
        return this.soapAction;
    }

    public void setSoapUse(SOAPBinding.Use u) {
        this.soapUse = u;
    }

    public SOAPBinding.Use getSoapUse() {
        return this.soapUse;
    }

    public void addAnnotation(String tag, JavaAnnotation annotation) {
        if (annotation == null) {
            return;
        }
        this.annotations.put(tag, annotation);
    }

    public Collection<JavaAnnotation> getAnnotations() {
        return this.annotations.values();
    }

    public Map<String, JavaAnnotation> getAnnotationMap() {
        return this.annotations;
    }

    public List<String> getParameterList() {
        return getParameterList(true);
    }

    public List<String> getParameterListWithoutAnnotation() {
        return getParameterList(false);
    }

    public List<String> getParameterList(boolean includeAnnotation) {
        List<String> list = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parameters.size(); i++) {
            JavaParameter parameter = parameters.get(i);
            if (includeAnnotation) {
                list.add(parameter.getAnnotation().toString());
            }
            sb.setLength(0);
            if (parameter.isHolder()) {
                sb.append(parameter.getHolderName());
                sb.append("<");
                sb.append(parameter.getHolderClass());
                sb.append(">");
            } else {
                sb.append(parameter.getClassName());
            }
            sb.append(" ");
            sb.append(parameter.getName());
            if (i != (parameters.size() - 1)) {
                sb.append(',');
            }
            list.add(sb.toString());
        }
        return list;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n========================\n");
        sb.append("\nMethod:");
        sb.append(getName());
        sb.append("\n-----------\n");
        sb.append("\nReturn:");
        sb.append(getReturn());
        sb.append("\n------------\n");
        sb.append("\nParameter:");
        sb.append(getParameterList());
        sb.append("\n------------\n");
        sb.append("\nAnnotations:");
        sb.append(getAnnotations());
        sb.append("\n========================\n");
        return sb.toString();
    }

    public void setInterface(JavaInterface intf) {
        this.javaInterface = intf;
    }

    public void annotate(Annotator annotator) {
        annotator.annotate(this);
    }

    public void setJavaCodeBlock(JavaCodeBlock b) {
        this.block = b;
    }

    public JavaCodeBlock getJavaCodeBlock() {
        return this.block;
    }
}

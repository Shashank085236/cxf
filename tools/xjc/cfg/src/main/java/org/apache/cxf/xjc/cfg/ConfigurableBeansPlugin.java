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

package org.apache.cxf.xjc.cfg;

import java.util.Collections;
import java.util.List;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

import org.apache.cxf.configuration.AbstractConfigurableBeanBase;

/**
 * Modifies the JAXB code model to initialise fields mapped from schema elements
 * with their default value.
 */
public class ConfigurableBeansPlugin extends Plugin {

    private static final String CFG_NAMESPACE_URI = "http://cxf.apache.org/configuration/cfg";
    private static final String CFG_CONFIGURATION_ELEM_NAME = "configuration";

    public ConfigurableBeansPlugin() {
    }

    public String getOptionName() {
        return "Xcfg";
    }

    public String getUsage() {
        return "-Xcfg: Generate configurable beans.";
    }

    public List<String> getCustomizationURIs() {
        return Collections.singletonList(CFG_NAMESPACE_URI);
    }

    public boolean isCustomizationTagName(String nsUri, String localName) {
        return nsUri.equals(CFG_NAMESPACE_URI) && localName.equals(CFG_CONFIGURATION_ELEM_NAME);
    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        System.out.println("Running configurable beans plugin.");
       
        for (ClassOutline co : outline.getClasses()) {
            CPluginCustomization cust = co.target.getCustomizations().find(CFG_NAMESPACE_URI,
                                                                           CFG_CONFIGURATION_ELEM_NAME);
            if (null == cust) {
                continue;
            }

            cust.markAsAcknowledged();

            // generated class extends AbstractConfigurableBeanBase

            JDefinedClass dc = co.implClass;
            dc._extends(AbstractConfigurableBeanBase.class);

            // replace default getters by getters trying the registered providers

            for (FieldOutline fo : co.getDeclaredFields()) {

                String fieldName = fo.getPropertyInfo().getName(false);
                JType type = fo.getRawType();
                String typeName = type.fullName();
                String getterName = ("java.lang.Boolean".equals(typeName) ? "is" : "get")
                                    + fo.getPropertyInfo().getName(true);


                // retain existing javadoc, modifiers, type and name
                JMethod method = dc.getMethod(getterName, new JType[0]);
                JDocComment doc = method.javadoc();
                int mods = method.mods().getValue();
                JType mtype = method.type();                
                dc.methods().remove(method);
                
                method = dc.method(mods, mtype, getterName);
                method.javadoc().append(doc);

                JFieldRef fr = JExpr.ref(fieldName);

                JExpression test = JOp.eq(fr, JExpr._null());
                JConditional jc = method.body()._if(test);
                JInvocation invocation = JExpr.invoke("tryProviders");
                invocation.arg(JExpr.dotclass(type.boxify()));
                invocation.arg(JExpr.lit(fieldName));
                jc._then()._return(invocation);
                jc._else()._return(fr);

            }

        }

        return true;
    }
}

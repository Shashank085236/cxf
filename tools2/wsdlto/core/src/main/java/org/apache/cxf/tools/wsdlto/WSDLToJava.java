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

package org.apache.cxf.tools.wsdlto;

import java.io.*;
import java.util.*;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;

public class WSDLToJava {

    private static String[] args;
    private static final String DEFAULT_FRONTEND_NAME = "jaxws";
    private static final String DEFAULT_DATABINDING_NAME = "jaxb";
    
    private PluginLoader pluginLoader = PluginLoader.getInstance();

    private FrontEndProfile loadFrontEnd(String name) {
        if (StringUtils.isEmpty(name)) {
            name = DEFAULT_FRONTEND_NAME;
        }
        if (isVerbose()) {
            System.out.println("Loading FrontEnd " + name + " ...");
        }
        return pluginLoader.getFrontEndProfile(name);
    }

    private DataBindingProfile loadDataBinding(String name) {
        if (StringUtils.isEmpty(name)) {
            name = DEFAULT_DATABINDING_NAME;
        }
        if (isVerbose()) {
            System.out.println("Loading DataBinding " + name + " ...");
        }
        return pluginLoader.getDataBindingProfile(name);
    }
    
    protected void run(ToolContext context) throws Exception {
        context.put(ToolConstants.CFG_CMD_ARG, args);
        
        FrontEndProfile frontend = loadFrontEnd(getFrontEndName(args));
        context.put(FrontEndProfile.class, frontend);
        
        DataBindingProfile databinding = loadDataBinding(getDataBindingName(args));
        context.put(DataBindingProfile.class, databinding);
        
        Class containerClass = frontend.getContainerClass();

        InputStream toolspecStream = getResourceAsStream(containerClass, frontend.getToolspec());
        
        ToolRunner.runTool(containerClass,
                           toolspecStream,
                           false,
                           args,
                           context);
    }
        
    protected static boolean isVerbose() {
        return isSet(new String[]{"-V", "-verbose"});
    }
    
    private static boolean isSet(String[] keys) {
        List<String> pargs = Arrays.asList(args);
        
        for (String key : keys) {
            if (pargs.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private String parseArguments(String[] pargs, String key) {
        if (pargs == null) {
            return null;
        }
        List<String> largs = Arrays.asList(pargs);

        int index = 0;
        if (largs.contains(key)) {
            index = largs.indexOf(key);
            if (index + 1 < largs.size()) {
                return largs.get(index + 1);
            }
        }
        return null;
    }

    private String getOptionValue(String[] pargs, String[] keys) {
        for (String key : keys) {
            String value = parseArguments(pargs, key);
            if (!StringUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }
    
    protected String getFrontEndName(String[] pargs) {
        return getOptionValue(pargs, new String[]{"-frontend", "-fe"});
    }

    protected String getDataBindingName(String[] pargs) {
        return getOptionValue(pargs, new String[]{"-databinding", "-db"});
    }

    public void setArguments(String[] pargs) {
        this.args = pargs;
    }

    public static void main(String[] pargs) {
        args = pargs;

        try {
            
            new WSDLToJava().run(new ToolContext());
            
        } catch (ToolException ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (isVerbose()) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (isVerbose()) {
                ex.printStackTrace();
            }
        }
    }

    private static InputStream getResourceAsStream(Class clz, String file) {
        return clz.getResourceAsStream(file);
    }
}

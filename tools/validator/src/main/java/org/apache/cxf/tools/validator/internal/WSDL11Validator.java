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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.wsdl.Definition;

import org.xml.sax.InputSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;

public class WSDL11Validator extends AbstractDefinitionValidator {

    private final List<AbstractValidator> validators = new ArrayList<AbstractValidator>();

    public WSDL11Validator(Definition definition) {
        super(definition);
    }

    public WSDL11Validator(Definition definition, ToolContext pe) {
        super(definition, pe);
    }

    public boolean isValid() throws ToolException {
        //boolean isValid = true;
        String schemaDir = getSchemaDir();
        SchemaValidator schemaValidator = null;
        String[] schemas = (String[])env.get(ToolConstants.CFG_SCHEMA_URL);
        // Tool will use the following sequence to find the schema files
        // 1.ToolConstants.CFG_SCHEMA_DIR from ToolContext
        // 2.ToolConstants.CXF_SCHEMA_DIR from System property
        // 3.If 1 and 2 is null , then load these schema files from jar file
        if (!StringUtils.isEmpty(schemaDir)) {
            schemaValidator = new SchemaValidator(schemaDir, (String)env.get(ToolConstants.CFG_WSDLURL),
                                                  schemas);
        } else {
            try {
                schemaValidator = new SchemaValidator(getDefaultSchemas(), (String)env
                    .get(ToolConstants.CFG_WSDLURL), schemas);
            } catch (IOException e) {
                throw new ToolException("Schemas can not be loaded before validating wsdl", e);
            }

        }
        if (!schemaValidator.isValid()) {
            this.addErrorMessage(schemaValidator.getErrorMessage());            
            throw new ToolException(this.getErrorMessage());

        } else {
            this.def = schemaValidator.def;
        }

        validators.add(new UniqueBodyPartsValidator(this.def));
        validators.add(new WSIBPValidator(this.def));
        validators.add(new MIMEBindingValidator(this.def));

        for (AbstractValidator validator : validators) {
            if (!validator.isValid()) {
                addErrorMessage(validator.getErrorMessage());                
                throw new ToolException(this.getErrorMessage());
            }
        }
        return true;
    }

    public String getSchemaDir() {
        String dir = "";
        if (env.get(ToolConstants.CFG_SCHEMA_DIR) == null) {
            dir = System.getProperty(ToolConstants.CXF_SCHEMA_DIR);
        } else {
            dir = (String)env.get(ToolConstants.CFG_SCHEMA_DIR);
        }
        return dir;
    }

    protected List<InputSource> getDefaultSchemas() throws IOException {
        List<InputSource> xsdList = new ArrayList<InputSource>();
        ClassLoader clzLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = clzLoader.getResources(ToolConstants.CXF_SCHEMAS_DIR_INJAR);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            //from jar files 
            if (url.toString().startsWith("jar")) {
                
                JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
                
                JarFile jarFile = jarConnection.getJarFile();
                
                Enumeration<JarEntry> entry = jarFile.entries();
                
                while (entry.hasMoreElements()) {
                    JarEntry ele = (JarEntry)entry.nextElement();
                    if (ele.getName().endsWith(".xsd")
                        && ele.getName().indexOf(ToolConstants.CXF_SCHEMAS_DIR_INJAR) > -1) {
                        
                        URIResolver resolver = new URIResolver(ele.getName());
                        if (resolver.isResolved()) {
                            InputSource is = new InputSource(resolver.getInputStream());
                            // Use the resolved URI of the schema if available.
                            // The ibm jdk won't resolve the schema if we set
                            // the id to the relative path.
                            if (resolver.getURI() != null) {
                                is.setSystemId(resolver.getURI().toString());
                            } else {
                                is.setSystemId(ele.getName());
                            }
                            xsdList.add(is);
                        }
                    }
                }
                //from class path direcotry
            } else if (url.toString().startsWith("file")) {
                URI loc = null;
                try {
                    loc = url.toURI();
                } catch (URISyntaxException e) {
                    //
                }
                java.io.File file = new java.io.File(loc);
                if (file.exists()) {
                    File[] files = file.listFiles(new FileFilter() {
                            public boolean accept(File pathname) {
                                if (pathname.getAbsolutePath().endsWith(".xsd")) {
                                    return true;
                                }
                                return false;
                            }
                        });
                    for (int i = 0; i < files.length; i++) {
                        InputSource is = new InputSource(files[i].toURL().openStream());
                        is.setSystemId(files[i].toURL().toString());
                        xsdList.add(is);
                    }
                }
                
            }
        }
        
        sort(xsdList);
        return xsdList;
    }

    private void sort(List<InputSource> list) {
        Collections.sort(list, new Comparator<InputSource>() {
            public int compare(InputSource i1, InputSource i2) {
                if (i1 == null && i2 == null) {
                    return -1;
                }
                return i1.getSystemId().compareTo(i2.getSystemId());
            }
        });
    }
}

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

package org.apache.cxf.jibx.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.jibx.binding.model.BindingElement;
import org.jibx.binding.model.BindingUtils;
import org.jibx.binding.model.MappingElement;
import org.jibx.binding.model.ValueElement;
import org.jibx.runtime.JiBXException;
import org.jibx.schema.ISchemaResolver;
import org.jibx.schema.codegen.CodeGen;
import org.jibx.schema.codegen.custom.SchemaCustom;
import org.jibx.schema.codegen.custom.SchemasetCustom;
import org.jibx.schema.validation.ProblemMultiHandler;

import org.w3c.dom.Element;

public class JiBXToolingDataBinding implements DataBindingProfile {
    
    private JiBXToolingProblemHandler problemHandler = new JiBXToolingProblemHandler();
    private Map<String, Element> schemaMap = new HashMap<String, Element>();

    private Map<org.jibx.runtime.QName, MappingElement> types = new HashMap<org.jibx.runtime.QName, MappingElement>();
    private Map<org.jibx.runtime.QName, MappingElement> elements = new HashMap<org.jibx.runtime.QName, MappingElement>();

    public DefaultValueWriter createDefaultValueWriter(QName qn, boolean element) {
        return null;
    }

    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName wrapperElement, QName qn) {
        return null;
    }

    public void generate(ToolContext context) throws ToolException {
        try {
            JiBXCodeGenHelper codegen = new JiBXCodeGenHelper();

            ProblemMultiHandler handler = new ProblemMultiHandler();
            handler.addHandler(problemHandler);
            codegen.setProblemHandler(handler);

            // Setting the source (or the output) directory
            String sourcePath = (String)context.get(ToolConstants.CFG_SOURCEDIR);
            if (sourcePath == null) {
                sourcePath = (new File(".")).getAbsolutePath();
            }
            File generatePath = new File(sourcePath);
            if (!generatePath.exists()) {
                generatePath.mkdir();
            }
            codegen.setGeneratePath(generatePath);

            String classPath = (String)context.get(ToolConstants.CFG_CLASSDIR);
            if (classPath == null) {
                classPath = (new File(".")).getAbsolutePath();
            }
            File compilePath = new File(classPath);
            if (!compilePath.exists()) {
                compilePath.mkdir();
            }
            codegen.setCompilePath(compilePath);

            // Set schema resolver list
            codegen.setFileset(schemaResolverList(schemaMap));

            // Set Customization
            String[] bindingFiles = (String[])context.get(ToolConstants.CFG_BINDING);
            SchemasetCustom customRoot;
            if (bindingFiles == null || bindingFiles.length == 0) {
                customRoot = defaultSchemasetCustom(schemaMap);
            } else {
                customRoot = SchemasetCustom.loadCustomizations(bindingFiles[0], handler);
            }
            // force to retrain types information in the generated binding model
            forceTypes(customRoot);
            codegen.setCustomRoot(customRoot);

            codegen.generate();

            if (Boolean.valueOf((String)context.get(ToolConstants.CFG_COMPILE))) {
                // TODO compile the generated code with the generated binding file ??
                // params.compile();
            }

            BindingElement rootBinding = codegen.getRootBinding();
            BindingUtils.getDefinitions(rootBinding, types, elements);

        } catch (Exception e) {
            problemHandler.handleSevere("", e);
        }
    }

    public String getType(QName qn, boolean element) {
        MappingElement mappingElement = (element) ? elements.get(jibxQName(qn)) : types.get(jibxQName(qn));
        return (mappingElement == null) ? null : mappingElement.getClassName();
    }

    public String getWrappedElementType(QName wrapperElement, QName item) {
        MappingElement mappingElement = elements.get(jibxQName(wrapperElement));
        return (mappingElement == null) ? null : itemType(mappingElement, item);
    }

    public void initialize(ToolContext context) throws ToolException {
        String wsdlUrl = (String)context.get(ToolConstants.CFG_WSDLURL);
        initializeJiBXCodeGenerator(wsdlUrl);
    }

    private void initializeJiBXCodeGenerator(String wsdlUrl) {
        try {
            loadWsdl(wsdlUrl, this.schemaMap);
        } catch (WSDLException e) {
            problemHandler.handleSevere("Error in loading wsdl file at :" + wsdlUrl, e);
        }
    }

    private static void loadWsdl(String wsdlUrl, Map<String, Element> schemas) throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition definition = reader.readWSDL(wsdlUrl);
        addWsdlSchemas(definition, schemas);
    }

    private static void addWsdlSchemas(Definition definition, Map<String, Element> schemaContents) {
        Types types = definition.getTypes();
        List extensibilityElements = types.getExtensibilityElements();
        for (Object extensibilityElement : extensibilityElements) {
            if (extensibilityElement instanceof Schema) {
                Schema schema = (Schema)extensibilityElement;
                addSchema(schema.getDocumentBaseURI(), schema, schemaContents);
            }
        }
    }

    private static void addSchema(String docBaseURI, Schema schema, Map<String, Element> schemaContents) {
        if (!schemaContents.containsKey(docBaseURI)) {
            schemaContents.put(docBaseURI, schema.getElement());
        } else {
            if (schemaContents.containsValue(schema.getElement())) {
                // do nothing
            } else {
                String key = schema.getDocumentBaseURI() + "#"
                             + schema.getElement().getAttribute("targetNamespace");
                if (!schemaContents.containsKey(key)) {
                    schemaContents.put(key, schema.getElement());
                }
            }
        }
        // TODO : Handle imports and includes recursively
    }

    private static List<ISchemaResolver> schemaResolverList(Map<String, Element> schemaContents) {
        List<ISchemaResolver> schemaResolverList = new ArrayList<ISchemaResolver>();
        for (String key : schemaContents.keySet()) {
            schemaResolverList.add(new JiBXSchemaResolver(key, schemaContents.get(key)));
        }
        return schemaResolverList;
    }

    private static org.jibx.runtime.QName jibxQName(QName qname) {
        return new org.jibx.runtime.QName(qname.getNamespaceURI(), qname.getLocalPart());
    }

    private static String itemType(MappingElement mappingElement, QName qName) {
        String localPart = qName.getLocalPart();
        for (Iterator childIterator = mappingElement.childIterator(); childIterator.hasNext();) {
            Object child = childIterator.next();
            if (child instanceof ValueElement) {
                ValueElement valueElement = (ValueElement)child;
                if (localPart.equals(valueElement.getName())) {
                    return valueElement.getDeclaredType();
                }
            }
        }
        return null;
    }

    private static SchemasetCustom defaultSchemasetCustom(Map<String, Element> schemaMap) {
        SchemasetCustom customRoot = new SchemasetCustom((SchemasetCustom)null);
        Set<String> schemaIds = schemaMap.keySet();
        for (String schemaId : schemaIds) {
            SchemaCustom schemaCustom = new SchemaCustom(customRoot);
            schemaCustom.setName(schemaId);
            customRoot.getChildren().add(schemaCustom);
        }
        return customRoot;
    }

    private static void forceTypes(SchemasetCustom customRoot) {
        List<?> children = customRoot.getChildren();
        for (Object child : children) {
            SchemaCustom schemaCustom = (SchemaCustom)child;
            schemaCustom.setForceTypes(Boolean.TRUE);
            // TODO setForceType recursively ??
        }
    }

    private class JiBXCodeGenHelper {
        private ProblemMultiHandler problemHandler;
        private SchemasetCustom customRoot;
        private URL schemaRoot;
        private File generatePath;
        private boolean verbose = false;
        private String usingNamespace = null;
        private String nonamespacePackage = null;
        private String bindingName = null;
        private List fileset = null;
        private List includePaths = new ArrayList();
        private File modelFile = null;
        private BindingElement rootBinding = null;
        private File compilePath;

        public void setProblemHandler(ProblemMultiHandler problemHandler) {
            this.problemHandler = problemHandler;
        }

        public void setCustomRoot(SchemasetCustom customRoot) {
            this.customRoot = customRoot;
        }

        public void setSchemaRoot(URL schemaRoot) {
            this.schemaRoot = schemaRoot;
        }

        public void setGeneratePath(File generatePath) {
            this.generatePath = generatePath;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public void setUsingNamespace(String usingNamespace) {
            this.usingNamespace = usingNamespace;
        }

        public void setNonamespacePackage(String nonamespacePackage) {
            this.nonamespacePackage = nonamespacePackage;
        }

        public void setBindingName(String bindingName) {
            this.bindingName = bindingName;
        }

        public List getFileset() {
            return fileset;
        }

        public void setFileset(List fileset) {
            this.fileset = fileset;
        }

        public void setIncludePaths(List includePaths) {
            this.includePaths = includePaths;
        }

        public void setModelFile(File modelFile) {
            this.modelFile = modelFile;
        }

        public BindingElement getRootBinding() {
            return rootBinding;
        }

        public void setRootBinding(BindingElement rootBinding) {
            this.rootBinding = rootBinding;
        }

        public void setCompilePath(File compilePath) {
            this.compilePath = compilePath;
        }

        public void generate() throws JiBXException, IOException {
            CodeGen codegen = new CodeGen(customRoot, schemaRoot, generatePath);
            codegen.generate(verbose, usingNamespace, nonamespacePackage, bindingName, fileset, includePaths,
                             modelFile, problemHandler);
            setRootBinding(codegen.getRootBinding());
        }
    }
}

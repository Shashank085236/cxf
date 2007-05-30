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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertiesLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.ClassNameProcessor;
import org.apache.cxf.tools.common.ClassUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.WSDLConstants;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.core.AbstractWSDLBuilder;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;


public class WSDLToJavaContainer extends AbstractCXFToolContainer {
    
    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToJavaContainer.class);
    private static final String DEFAULT_NS2PACKAGE = "http://www.w3.org/2005/08/addressing";
    private static final String SERVICE_VALIDATOR = "META-INF/service.validator.xml";
    String toolName;

    public WSDLToJavaContainer(String name, ToolSpec toolspec) throws Exception {
        super(name, toolspec);
        this.toolName = name;
    }

    public Set<String> getArrayKeys() {
        Set<String> set = new HashSet<String>();
        set.add(ToolConstants.CFG_PACKAGENAME);
        set.add(ToolConstants.CFG_NEXCLUDE);
        return set;
    }

    public WSDLConstants.WSDLVersion getWSDLVersion() {
        String version = (String) context.get(ToolConstants.CFG_WSDL_VERSION);
        return WSDLConstants.getVersion(version);
    }

    public Bus getBus() {
        return BusFactory.getDefaultBus();
    }

    @SuppressWarnings("unchecked")
    public void execute() throws ToolException {
        if (!hasInfoOption()) {
            buildToolContext();
            validate(context);

            FrontEndProfile frontend = context.get(FrontEndProfile.class);

            if (frontend == null) {
                Message msg = new Message("FOUND_NO_FRONTEND", LOG);
                throw new ToolException(msg);
            }

            WSDLConstants.WSDLVersion version = getWSDLVersion();

            String wsdlURL = (String)context.get(ToolConstants.CFG_WSDLURL);
            List<ServiceInfo> serviceList = new ArrayList<ServiceInfo>();

            // Build the ServiceModel from the WSDLModel
            if (version == WSDLConstants.WSDLVersion.WSDL11) {
                AbstractWSDLBuilder<Definition> builder = (AbstractWSDLBuilder<Definition>)frontend
                    .getWSDLBuilder();
                builder.setContext(context);

                // TODO: Modify builder api, let customized definition make
                // sense.
                builder.build(URIParserUtil.getAbsoluteURI(wsdlURL));
                builder.customize();
                Definition definition = builder.getWSDLModel();

                context.put(Definition.class, definition);
                if (context.optionSet(ToolConstants.CFG_VALIDATE_WSDL)) {
                    builder.validate(definition);
                }

                WSDLServiceBuilder serviceBuilder = new WSDLServiceBuilder(getBus());

                String serviceName = (String)context.get(ToolConstants.CFG_SERVICENAME);

                if (serviceName != null) {
                    List<ServiceInfo> services = serviceBuilder
                        .buildServices(definition, getServiceQName(definition));
                    serviceList.addAll(services);
                } else  if (definition.getServices().size() > 0) {
                    serviceList = serviceBuilder.buildServices(definition);
                } else  {
                    serviceList = serviceBuilder.buildMockServices(definition);
                }
                context.put(ClassCollector.class, new ClassCollector());
            } else {
                // TODO: wsdl2.0 support
            }
            Map<String, InterfaceInfo> interfaces = new HashMap<String, InterfaceInfo>();

            Map<String, Element> schemas = (Map<String, Element>)serviceList.get(0)
                .getProperty(WSDLServiceBuilder.WSDL_SCHEMA_ELEMENT_LIST);
            context.put(ToolConstants.SCHEMA_MAP, schemas);
            context.put(ToolConstants.PORTTYPE_MAP, interfaces);
            Processor processor = frontend.getProcessor();
            if (processor instanceof ClassNameProcessor) {
                processor.setEnvironment(context);
                for (ServiceInfo service : serviceList) {

                    context.put(ServiceInfo.class, service);
                    
                    ((ClassNameProcessor)processor).processClassNames();
                    
                    context.put(ServiceInfo.class, null);
                }
            }
            generateTypes();

            for (ServiceInfo service : serviceList) {

                context.put(ServiceInfo.class, service);
                if (context.optionSet(ToolConstants.CFG_VALIDATE_WSDL)) {
                    validate(service);
                }

                // Build the JavaModel from the ServiceModel
                processor.setEnvironment(context);
                processor.process();


                if (!isSuppressCodeGen()) {
                    // Generate artifacts
                    for (FrontEndGenerator generator : frontend.getGenerators()) {
                        generator.generate(context);
                    }
                }
            }

            // Build projects: compile classes and copy resources etc.
            if (context.optionSet(ToolConstants.CFG_COMPILE)) {
                new ClassUtils().compile(context);
            }

            if (context.isExcludeNamespaceEnabled()) {
                try {
                    removeExcludeFiles();
                } catch (IOException e) {
                    throw new ToolException(e);
                }
            }
        }

    }

    private boolean isSuppressCodeGen() {
        return context.optionSet(ToolConstants.CFG_SUPPRESS_GEN);
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        try {
            if (getArgument() != null) {
                super.execute(exitOnFinish);
            }
            execute();

        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(toolName, (BadUsageException)ex.getCause());
            }
            throw ex;
        } catch (Exception ex) {
            throw new ToolException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public QName getServiceQName(Definition definition) {
        String serviceName = (String)context.get(ToolConstants.CFG_SERVICENAME);
        QName qname = null;
        if (serviceName != null) {
            for (Iterator<QName> ite = definition.getServices().keySet().iterator(); ite.hasNext();) {
                QName qn = ite.next();
                if (qn.getLocalPart().equalsIgnoreCase(serviceName.toLowerCase())) {
                    return qn;
                }
            }
        } else {
            for (Iterator<QName> ite = definition.getServices().keySet().iterator(); ite.hasNext();) {
                return ite.next();
            }
        }
        if (qname == null) {
            Message msg = new Message("SERVICE_NOT_FOUND", LOG, new Object[] {serviceName});
            throw new ToolException(msg);
        }
        return qname;
    }

    public void loadDefaultNSPackageMapping(ToolContext env) {
        if (!env.hasExcludeNamespace(DEFAULT_NS2PACKAGE)
            && env.getBooleanValue(ToolConstants.CFG_DEFAULT_NS, "true")) {
            env.loadDefaultNS2Pck(getResourceAsStream("namespace2package.cfg"));
        }
        if (env.getBooleanValue(ToolConstants.CFG_DEFAULT_EX, "true")) {
            env.loadDefaultExcludes(getResourceAsStream("wsdltojavaexclude.cfg"));
        }
    }


    public void setExcludePackageAndNamespaces(ToolContext env) {
        if (env.get(ToolConstants.CFG_NEXCLUDE) != null) {
            String[] pns = null;
            try {
                pns = (String[])env.get(ToolConstants.CFG_NEXCLUDE);
            } catch (ClassCastException e) {
                pns = new String[1];
                pns[0] = (String)env.get(ToolConstants.CFG_NEXCLUDE);
            }

            for (int j = 0; j < pns.length; j++) {
                int pos = pns[j].indexOf("=");
                String excludePackagename = pns[j];
                if (pos != -1) {
                    String ns = pns[j].substring(0, pos);
                    excludePackagename = pns[j].substring(pos + 1);
                    env.addExcludeNamespacePackageMap(ns, excludePackagename);
                    env.addNamespacePackageMap(ns, excludePackagename);
                } else {
                    env.addExcludeNamespacePackageMap(pns[j], env.mapPackageName(pns[j]));
                }
            }
        }
    }

    public void setPackageAndNamespaces(ToolContext env) {
        if (env.get(ToolConstants.CFG_PACKAGENAME) != null) {
            String[] pns = null;
            try {
                pns = (String[])env.get(ToolConstants.CFG_PACKAGENAME);
            } catch (ClassCastException e) {
                pns = new String[1];
                pns[0] = (String)env.get(ToolConstants.CFG_PACKAGENAME);
            }
            for (int j = 0; j < pns.length; j++) {
                int pos = pns[j].indexOf("=");
                String packagename = pns[j];
                if (pos != -1) {
                    String ns = pns[j].substring(0, pos);
                    packagename = pns[j].substring(pos + 1);
                    env.addNamespacePackageMap(ns, packagename);
                } else {
                    env.setPackageName(packagename);
                }
            }
        }
    }

    public void validate(ToolContext env) throws ToolException {
        String outdir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (outdir != null) {
            File dir = new File(outdir);
            if (!dir.exists()) {
                Message msg = new Message("DIRECTORY_NOT_EXIST", LOG, outdir);
                throw new ToolException(msg);
            }
            if (!dir.isDirectory()) {
                Message msg = new Message("NOT_A_DIRECTORY", LOG, outdir);
                throw new ToolException(msg);
            }
        }

        if (!env.optionSet(ToolConstants.CFG_EXTRA_SOAPHEADER)) {
            env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "false");
        }

        if (env.optionSet(ToolConstants.CFG_COMPILE)) {
            String clsdir = (String)env.get(ToolConstants.CFG_CLASSDIR);
            if (clsdir != null) {
                File dir = new File(clsdir);
                if (!dir.exists()) {
                    Message msg = new Message("DIRECTORY_NOT_EXIST", LOG, clsdir);
                    throw new ToolException(msg);
                }
            }
        }

        String wsdl = (String)env.get(ToolConstants.CFG_WSDLURL);
        if (StringUtils.isEmpty(wsdl)) {
            Message msg = new Message("NO_WSDL_URL", LOG);
            throw new ToolException(msg);
        }

        env.put(ToolConstants.CFG_WSDLURL, URIParserUtil.normalize(wsdl));

        String[] bindingFiles;
        try {
            bindingFiles = (String[])env.get(ToolConstants.CFG_BINDING);
            if (bindingFiles == null) {
                return;
            }
        } catch (ClassCastException e) {
            bindingFiles = new String[1];
            bindingFiles[0] = (String)env.get(ToolConstants.CFG_BINDING);
        }

        for (int i = 0; i < bindingFiles.length; i++) {
            bindingFiles[i] = URIParserUtil.getAbsoluteURI(bindingFiles[i]);
        }

        env.put(ToolConstants.CFG_BINDING,  bindingFiles);
    }

    public void setAntProperties(ToolContext env) {
        String installDir = System.getProperty("install.dir");
        if (installDir != null) {
            env.put(ToolConstants.CFG_INSTALL_DIR, installDir);
        } else {
            env.put(ToolConstants.CFG_INSTALL_DIR, ".");
        }
    }

    protected void setLibraryReferences(ToolContext env) {
        Properties props = loadProperties(getResourceAsStream("wsdltojavalib.properties"));
        if (props != null) {
            for (Iterator keys = props.keySet().iterator(); keys.hasNext();) {
                String key = (String)keys.next();
                env.put(key, props.get(key));
            }
        }
        env.put(ToolConstants.CFG_ANT_PROP, props);
    }

    public void buildToolContext() {
        context = getContext();
        context.addParameters(getParametersMap(getArrayKeys()));

        if (context.get(ToolConstants.CFG_OUTPUTDIR) == null) {
            context.put(ToolConstants.CFG_OUTPUTDIR, ".");
        }

        if (context.containsKey(ToolConstants.CFG_ANT)) {
            setAntProperties(context);
            setLibraryReferences(context);
        }

        if (!context.containsKey(ToolConstants.CFG_WSDL_VERSION)) {
            context.put(ToolConstants.CFG_WSDL_VERSION, WSDLConstants.WSDL11);
        }
        
        loadDefaultNSPackageMapping(context);
        setPackageAndNamespaces(context);
        setExcludePackageAndNamespaces(context);
    }

    protected static InputStream getResourceAsStream(String file) {
        return WSDLToJavaContainer.class.getResourceAsStream(file);
    }

    public void checkParams(ErrorVisitor errors) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (!doc.hasParameter("wsdlurl")) {
            errors.add(new ErrorVisitor.UserError("WSDL/SCHEMA URL has to be specified"));
        }
        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }

    public void removeExcludeFiles() throws IOException {
        List<String> excludeGenFiles = context.getExcludeFileList();
        if (excludeGenFiles == null) {
            return;
        }
        String outPutDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        for (int i = 0; i < excludeGenFiles.size(); i++) {
            String excludeFile = excludeGenFiles.get(i);
            File file = new File(outPutDir, excludeFile);
            file.delete();
            File tmpFile = file.getParentFile();
            while (tmpFile != null && !tmpFile.getCanonicalPath().equalsIgnoreCase(outPutDir)) {
                if (tmpFile.isDirectory() && tmpFile.list().length == 0) {
                    tmpFile.delete();
                }
                tmpFile = tmpFile.getParentFile();
            }

            if (context.get(ToolConstants.CFG_COMPILE) != null) {
                String classDir = context.get(ToolConstants.CFG_CLASSDIR) == null
                    ? outPutDir : (String)context.get(ToolConstants.CFG_CLASSDIR);
                File classFile = new File(classDir, excludeFile.substring(0, excludeFile.indexOf(".java"))
                                          + ".class");
                classFile.delete();
                File tmpClzFile = classFile.getParentFile();
                while (tmpClzFile != null && !tmpClzFile.getCanonicalPath().equalsIgnoreCase(outPutDir)) {
                    if (tmpClzFile.isDirectory() && tmpClzFile.list().length == 0) {
                        tmpClzFile.delete();
                    }
                    tmpClzFile = tmpClzFile.getParentFile();
                }
            }
        }
    }

    public boolean passthrough() {
        if (context.optionSet(ToolConstants.CFG_GEN_TYPES)
            || context.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        if (context.optionSet(ToolConstants.CFG_GEN_ANT)
            || context.optionSet(ToolConstants.CFG_GEN_CLIENT)
            || context.optionSet(ToolConstants.CFG_GEN_IMPL)
            || context.optionSet(ToolConstants.CFG_GEN_SEI)
            || context.optionSet(ToolConstants.CFG_GEN_SERVER)
            || context.optionSet(ToolConstants.CFG_GEN_SERVICE)) {
            return true;
        }
        return false;
    }

    public void generateTypes() throws ToolException {
        if (passthrough()) {
            return;
        }

        DataBindingProfile dataBindingProfile = context.get(DataBindingProfile.class);
        if (dataBindingProfile == null) {
            Message msg = new Message("FOUND_NO_DATABINDING", LOG);
            throw new ToolException(msg);
        }
        dataBindingProfile.generate(context);
    }

    public void validate(final ServiceInfo service) throws ToolException {
        for (ServiceValidator validator : getServiceValidators()) {
            validator.setService(service);
            if (!validator.isValid()) {
                throw new ToolException(validator.getErrorMessage());
            }
        }
    }

    public List<ServiceValidator> getServiceValidators() {
        List<ServiceValidator> validators = new ArrayList<ServiceValidator>();
        
        Properties initialExtensions = null;
        try {
            initialExtensions = PropertiesLoaderUtils.loadAllProperties(SERVICE_VALIDATOR, Thread
                            .currentThread().getContextClassLoader());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        for (Iterator it = initialExtensions.values().iterator(); it.hasNext();) {
            String validatorClass = (String) it.next();
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Found service validator : " + validatorClass);
                }
                ServiceValidator validator = (ServiceValidator)Class.forName(validatorClass).newInstance();
                validators.add(validator);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
        return validators;
    }
}

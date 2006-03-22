package org.objectweb.celtix.tools.processors.xsd2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.tools.common.Processor;
import org.objectweb.celtix.tools.common.ProcessorEnvironment;
import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.common.ToolException;
import org.objectweb.celtix.tools.common.WSDLConstants;
import org.objectweb.celtix.tools.common.dom.ExtendedDocumentBuilder;
import org.objectweb.celtix.tools.jaxws.JAXWSBinding;
import org.objectweb.celtix.tools.jaxws.JAXWSBindingDeserializer;
import org.objectweb.celtix.tools.jaxws.JAXWSBindingSerializer;
import org.objectweb.celtix.tools.utils.FileWriterUtil;

public class XSDToWSDLProcessor implements Processor {
    private static final Logger LOG = LogUtils.getL7dLogger(XSDToWSDLProcessor.class);
    private static final String XSD_FILE_NAME_EXT = ".xsd";
    private static final String WSDL_FILE_NAME_EXT = ".wsdl";
    
    private Definition wsdlDefinition;
    private ExtensionRegistry registry;
    private WSDLFactory wsdlFactory;

    private String xsdUrl;
    private final ExtendedDocumentBuilder xsdBuilder = new ExtendedDocumentBuilder();
    private Document xsdDoc;
    private ProcessorEnvironment env;

    public void process() throws ToolException {
        envParamSetting();
        initXSD();
        initWSDL();
        addWSDLTypes();
    }

    public void setEnvironment(ProcessorEnvironment newEnv) {
        this.env = newEnv;
    }

    private void envParamSetting() {
        xsdUrl = (String)env.get(ToolConstants.CFG_XSDURL);

        if (!env.optionSet(ToolConstants.CFG_NAME)) {
            env.put(ToolConstants.CFG_NAME, xsdUrl.substring(0, xsdUrl.length() - 4));
        }
    }

    private void initWSDL() throws ToolException {
        try {
            wsdlFactory = WSDLFactory.newInstance();
            wsdlDefinition = wsdlFactory.newDefinition();
        } catch (WSDLException we) {
            Message msg = new Message("FAIL_TO_CREATE_WSDL_DEFINITION", LOG);
            throw new ToolException(msg, we);
        }
    }

    private void initXSD() throws ToolException {
        InputStream in;
        try {
            in = new FileInputStream(xsdUrl);
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_OPEN_XSD_FILE", LOG);
            throw new ToolException(msg, ioe);
        }
        if (in == null) {
            throw new NullPointerException("Cannot create a ToolSpec object from a null stream");
        }
        try {
            xsdBuilder.setValidating(false);
            this.xsdDoc = xsdBuilder.parse(in);
        } catch (Exception ex) {
            Message msg = new Message("FAIL_TO_PARSE_TOOLSPEC", LOG);
            throw new ToolException(msg, ex);
        }
    }

    private void addWSDLTypes() throws ToolException {

        Element sourceElement = this.xsdDoc.getDocumentElement();
        Element targetElement = (Element)sourceElement.cloneNode(true);

        this.wsdlDefinition.setTargetNamespace((String)env.get(ToolConstants.CFG_NAMESPACE));
        this.wsdlDefinition
            .setQName(new QName(WSDLConstants.NS_WSDL, (String)env.get(ToolConstants.CFG_NAME)));

        Types types = this.wsdlDefinition.createTypes();
        ExtensibilityElement extElement;
        try {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
            registerJAXWSBinding(Definition.class);
            registerJAXWSBinding(Types.class);
            registerJAXWSBinding(Schema.class);
            extElement = registry.createExtension(Types.class, new QName(WSDLConstants.XSD_NAMESPACE,
                                                                         "schema"));
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_SCHEMA_EXTENSION", LOG);
            throw new ToolException(msg, wse);
        }
        ((Schema)extElement).setElement(targetElement);
        types.addExtensibilityElement(extElement);
        this.wsdlDefinition.setTypes(types);

        WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
        Writer outputWriter = getOutputWriter();

        try {
            wsdlWriter.writeWSDL(wsdlDefinition, outputWriter);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg, wse);
        }
        try {
            outputWriter.close();
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_CLOSE_WSDL_FILE", LOG);
            throw new ToolException(msg, ioe);
        }
    }

    private void registerJAXWSBinding(Class clz) {
        registry.registerSerializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingSerializer());

        registry.registerDeserializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingDeserializer());
        registry.mapExtensionTypes(clz, ToolConstants.JAXWS_BINDINGS, JAXWSBinding.class);
    }
    
    private Writer getOutputWriter() throws ToolException {
        Writer writer = null;
        String newName = null;
        String outputDir;

        if (env.get(ToolConstants.CFG_OUTPUTFILE) != null) {
            newName = (String)env.get(ToolConstants.CFG_OUTPUTFILE);
        } else {
            String oldName = (String)env.get(ToolConstants.CFG_XSDURL);
            int position = oldName.lastIndexOf("/");
            if (position < 0) {
                position = oldName.lastIndexOf("\\");
            }
            if (position >= 0) {
                oldName = oldName.substring(position + 1, oldName.length());
            }
            if (oldName.toLowerCase().indexOf(XSD_FILE_NAME_EXT) >= 0) {
                newName = oldName.substring(0, oldName.length() - 4) + WSDL_FILE_NAME_EXT;
            } else {
                newName = oldName + WSDL_FILE_NAME_EXT;
            }
        }
        if (env.get(ToolConstants.CFG_OUTPUTDIR) != null) {
            outputDir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
            if (!("/".equals(outputDir.substring(outputDir.length() - 1))
                  || "\\".equals(outputDir.substring(outputDir.length() - 1)))) {
                outputDir = outputDir + "/";
            }
        } else {
            outputDir = "./";
        }
        FileWriterUtil fw = new FileWriterUtil(outputDir);
        try {
            writer = fw.getWriter("", newName);
        } catch (IOException ioe) {
            Message msg = new Message("FAIl_TO_WRITE_FILE", LOG, env.get(ToolConstants.CFG_OUTPUTDIR)
                                    + System.getProperty("file.seperator") + newName);
            throw new ToolException(msg, ioe);
        }
        return writer;
    }

}

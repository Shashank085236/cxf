package org.objectweb.celtix.jaxb;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.ws.commons.schema.ValidationEventHandler;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.objectweb.celtix.common.i18n.BundleUtils;
import org.objectweb.celtix.common.i18n.Message;
import org.objectweb.celtix.common.i18n.UncheckedException;
import org.objectweb.celtix.databinding.DataBinding;
import org.objectweb.celtix.databinding.DataReaderFactory;
import org.objectweb.celtix.databinding.DataWriterFactory;
import org.objectweb.celtix.helpers.CastUtils;
import org.objectweb.celtix.helpers.DOMUtils;
import org.objectweb.celtix.resource.URIResolver;
import org.objectweb.celtix.service.model.SchemaInfo;
import org.objectweb.celtix.service.model.ServiceInfo;

public final class JAXBDataBinding implements DataBinding {

    public static final String SCHEMA_RESOURCE = "SCHEMRESOURCE";
    private static final Logger LOG = Logger.getLogger(JAXBDataBinding.class.getName());
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXBDataBinding.class);

    JAXBDataReaderFactory reader;
    JAXBDataWriterFactory writer;
    JAXBContext context;
    
    public JAXBDataBinding() throws JAXBException {
        reader = new JAXBDataReaderFactory();
        writer = new JAXBDataWriterFactory();
    }
    public JAXBDataBinding(Class<?> cls) throws JAXBException {
        this();
        context = JAXBEncoderDecoder.createJAXBContextForClass(cls);
        reader.setJAXBContext(context);
        writer.setJAXBContext(context);
    }
    
    public void setContext(JAXBContext ctx) {
        context = ctx;
        reader.setJAXBContext(context);
        writer.setJAXBContext(context);        
    }
    
    public DataReaderFactory getDataReaderFactory() {
        return reader;
    }

    public DataWriterFactory getDataWriterFactory() {
        return writer;
    }

    public Map<String, SchemaInfo> getSchemas(ServiceInfo serviceInfo) {
        Collection<String> schemaResources = 
            CastUtils.cast(serviceInfo.getProperty(SCHEMA_RESOURCE, List.class), String.class);
                                                             
        
        return loadSchemas(schemaResources); 
    }

    private Map<String, SchemaInfo> loadSchemas(Collection<String> schemaResources) {
        Map<String, SchemaInfo> schemas = new HashMap<String, SchemaInfo>();
        for (String schema : schemaResources) {
            if (schema.startsWith("file:")) {
                //load schemas from file system
                loadSchemaFromFile(schema, schemas);
            } else {
                //load schemas from classpath
                loadSchemaFromClassPath(schema, schemas);
            }
        }
        return schemas;
    }

    private void loadSchemaFromClassPath(String schema, Map<String, SchemaInfo> schemas) {
        // we can reuse code in javatowsdl tool after tool refactor
    }

    private void loadSchemaFromFile(String schema, Map<String, SchemaInfo> schemas) {
        try {
            XmlSchemaCollection schemaCol = new XmlSchemaCollection();
            URIResolver resolver = new URIResolver(schema);
            if (!resolver.isResolved()) {
                throw new UncheckedException(new Message("SCHEMA_NOT_RESOLVED", BUNDLE, schema));
            }
            
            if (resolver.isFile()) {
                schemaCol.setBaseUri(resolver.getFile().getParent());
            }
            
            Document schemaDoc = DOMUtils.readXml(resolver.getInputStream());

            XmlSchema xmlSchema = schemaCol.read(schemaDoc.getDocumentElement());
            SchemaInfo schemaInfo = new SchemaInfo(null, xmlSchema.getTargetNamespace());
       
            schemaInfo.setElement(schemaDoc.getDocumentElement());
            schemas.put(schemaInfo.getNamespaceURI(), schemaInfo);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        } catch (SAXException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        } catch (ParserConfigurationException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            throw new UncheckedException(e);
        }
    }

}

package org.objectweb.celtix.jaxb;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bindings.BindingFactoryManager;
import org.objectweb.celtix.helpers.CastUtils;
import org.objectweb.celtix.service.model.SchemaInfo;
import org.objectweb.celtix.service.model.ServiceInfo;
import org.objectweb.celtix.wsdl11.WSDLServiceBuilder;

public class JAXBDataBindingTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(JAXBDataBindingTest.class.getName());
    private static final String WSDL_PATH = "/wsdl/hello_world.wsdl";
    private static final String SCHEMA1 = "/schema/wsdl.xsd";
    private static final String SCHEMA2 = "/schema/jms.xsd";
    private Definition def;
    private Service service;
    private ServiceInfo serviceInfo;

    private IMocksControl control;
    private Bus bus;
    private BindingFactoryManager bindingFactoryManager;
    private JAXBDataBinding jaxbDataBinding;
    private Map<String, SchemaInfo> schemaMap;
    
    public void setUp() throws Exception {
        jaxbDataBinding = JAXBDataBinding.getInstance();
        String wsdlUrl = getClass().getResource(WSDL_PATH).toString();
        LOG.info("the path of wsdl file is " + wsdlUrl);
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        def = wsdlReader.readWSDL(wsdlUrl);

        WSDLServiceBuilder wsdlServiceBuilder = new WSDLServiceBuilder(bus);
        for (Service serv : CastUtils.cast(def.getServices().values(), Service.class)) {
            if (serv != null) {
                service = serv;
                break;
            }
        }

        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        wsdlServiceBuilder = new WSDLServiceBuilder(bus);

        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bindingFactoryManager);

        control.replay();
        serviceInfo = wsdlServiceBuilder.buildService(def, service);
        String schema1 = getClass().getResource(SCHEMA1).toString();
        String schema2 = getClass().getResource(SCHEMA2).toString();
        List<String> schemas = new ArrayList<String>();
        
        schemas.add(schema1);
        schemas.add(schema2);
        serviceInfo.setProperty(JAXBDataBinding.SCHEMA_RESOURCE, schemas);
        schemaMap = jaxbDataBinding.getSchemas(serviceInfo);
    }
    
    public void tearDown() throws Exception {
        
    }
    
    public void testSingleton() throws Exception {
        assertTrue(jaxbDataBinding == JAXBDataBinding.getInstance());
    }
    
    public void testGetSchemas() throws Exception {
        assertEquals(schemaMap.size(), 2);
        assertTrue(schemaMap.containsKey("http://schemas.xmlsoap.org/wsdl/"));
        assertTrue(schemaMap.containsKey("http://celtix.objectweb.org/transports/jms"));
        SchemaInfo wsdlSchema = schemaMap.get("http://schemas.xmlsoap.org/wsdl/");
        SchemaInfo jmsSchema = schemaMap.get("http://celtix.objectweb.org/transports/jms");
        assertNotNull(wsdlSchema.getElement());
        assertNotNull(jmsSchema.getElement());
        assertEquals(wsdlSchema.getNamespaceURI(), "http://schemas.xmlsoap.org/wsdl/");
        assertEquals(jmsSchema.getNamespaceURI(), "http://celtix.objectweb.org/transports/jms");
    }
    
    
}

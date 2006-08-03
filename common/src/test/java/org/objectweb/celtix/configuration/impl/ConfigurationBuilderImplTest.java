package org.objectweb.celtix.configuration.impl;

import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationBuilder;
import org.objectweb.celtix.configuration.ConfigurationException;
import org.objectweb.celtix.configuration.ConfigurationMetadata;
import org.objectweb.celtix.configuration.ConfigurationProvider;

public class ConfigurationBuilderImplTest extends TestCase {
    
    private static final String TEST_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/configuration/test/meta1";
    private static final String BUS_CONFIGURATION_URI = "http://celtix.objectweb.org/bus/bus-config";
    private static final String HTTP_LISTENER_CONFIGURATION_URI =
        "http://celtix.objectweb.org/bus/transports/http/http-listener-config";
    private static final String HTTP_LISTENER_CONFIGURATION_ID = "http-listener.44959";
    private static final String UNKNOWN_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/unknown/unknown-config";  
    private static final String DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME = 
        TestProvider.class.getName();
    
    private static final String DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY = 
        "org.objectweb.celtix.configuration.ConfigurationProviderClass";
    
    
    private String orgProviderClassname;
    
    public void setUp() {
        orgProviderClassname = System.getProperty(DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY);
        System.setProperty(DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY, 
                           DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME);      
    }
    
    public void tearDown() {
        if (null != orgProviderClassname) {
            System.setProperty(DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY, orgProviderClassname);
        } else {
            System.clearProperty(DEFAULT_CONFIGURATION_PROVIDER_CLASSNAME_PROPERTY);
        }
    }
    
    public void testGetConfigurationUnknownNamespace() {
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        try {
            builder.getConfiguration(UNKNOWN_CONFIGURATION_URI, "celtix");            
        } catch (ConfigurationException ex) {
            assertEquals("UNKNOWN_NAMESPACE_EXC", ex.getCode());
        }
        Configuration parent = EasyMock.createMock(Configuration.class);
        try {
            builder.getConfiguration(UNKNOWN_CONFIGURATION_URI, "celtix", parent);            
        } catch (ConfigurationException ex) {
            assertEquals("UNKNOWN_NAMESPACE_EXC", ex.getCode());
        }
    }
    
    public void testGetAddModel() {
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        try {
            builder.getModel(UNKNOWN_CONFIGURATION_URI);
        } catch (ConfigurationException ex) {
            assertEquals("UNKNOWN_NAMESPACE_EXC", ex.getCode());
        }
        
        ConfigurationMetadata unknownModel = EasyMock.createMock(ConfigurationMetadata.class);
        unknownModel.getNamespaceURI();
        EasyMock.expectLastCall().andReturn(UNKNOWN_CONFIGURATION_URI);
        EasyMock.replay(unknownModel);
        builder.addModel(unknownModel);
        assertSame(unknownModel, builder.getModel(UNKNOWN_CONFIGURATION_URI));
        EasyMock.verify(unknownModel); 
    }
    
    
    public void testAddModel() throws Exception {
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        try {
            builder.getModel("a.wsdl");
        } catch (ConfigurationException ex) {
            assertEquals("METADATA_RESOURCE_EXC", ex.getCode());
        }
    }
    
    public void testGetConfiguration() {
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        ConfigurationMetadata model = EasyMock.createMock(ConfigurationMetadata.class);
        model.getNamespaceURI();
        EasyMock.expectLastCall().andReturn(BUS_CONFIGURATION_URI);
        EasyMock.replay(model);
        builder.addModel(model);
        assertNull(builder.getConfiguration(BUS_CONFIGURATION_URI, "celtix"));        
        EasyMock.verify(model);
        
        model = EasyMock.createMock(ConfigurationMetadata.class);
        model.getNamespaceURI();
        EasyMock.expectLastCall().andReturn(HTTP_LISTENER_CONFIGURATION_URI);
        EasyMock.replay(model);
        builder.addModel(model);
        Configuration parent = EasyMock.createMock(Configuration.class);
        assertNull(builder.getConfiguration(HTTP_LISTENER_CONFIGURATION_URI, 
                                            HTTP_LISTENER_CONFIGURATION_ID, parent));        
    }

    public void testInvalidParentConfiguration() {
        String id = "celtix";
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        ConfigurationMetadataImpl model = new ConfigurationMetadataImpl();
        model.setNamespaceURI(BUS_CONFIGURATION_URI);
        model.setParentNamespaceURI(null);
        builder.addModel(model);
        model = new ConfigurationMetadataImpl();
        model.setNamespaceURI(HTTP_LISTENER_CONFIGURATION_URI);
        model.setParentNamespaceURI(BUS_CONFIGURATION_URI);
        builder.addModel(model);
        
        Configuration parent = builder.buildConfiguration(BUS_CONFIGURATION_URI, id, null);
        assertNotNull(parent);

        try {
            builder.buildConfiguration(HTTP_LISTENER_CONFIGURATION_URI, 
                                       HTTP_LISTENER_CONFIGURATION_ID, null);
            fail("Did not throw expected exception");
        } catch (ConfigurationException e) {
            String expectedErrorMsg = "Configuration " + HTTP_LISTENER_CONFIGURATION_URI
                + " is not a valid top configuration.";
            assertEquals("Unexpected exception message", expectedErrorMsg, e.getMessage());
        } catch (Exception e) {
            fail("Caught unexpected exception");
        }
    }

   
    public void testBuildConfiguration() throws Exception {
        URL url = getClass().getResource(getClass().getName() + ".class");        
        String id = "celtix";
        ConfigurationBuilder builder = new ConfigurationBuilderImpl(url);
        ConfigurationMetadataImpl model = new ConfigurationMetadataImpl();
        model.setNamespaceURI(BUS_CONFIGURATION_URI);
        builder.addModel(model);
        model = new ConfigurationMetadataImpl();
        model.setNamespaceURI(HTTP_LISTENER_CONFIGURATION_URI);
        builder.addModel(model);
        Configuration parent = builder.buildConfiguration(BUS_CONFIGURATION_URI, id);
        assertNotNull(parent);
        List<ConfigurationProvider> providers = parent.getProviders();
        assertEquals(1, providers.size());
        TestProvider tp = (TestProvider)providers.get(0);
        assertSame(url, tp.url);
        assertSame(parent, tp.configuration);
        assertNull(tp.name);
        
        Configuration child = builder.buildConfiguration(HTTP_LISTENER_CONFIGURATION_URI, 
                                                         HTTP_LISTENER_CONFIGURATION_ID);
        assertNotNull(child);
    }
    
    
    
    public void testGetResourceName() {
        ConfigurationBuilder builder = new ConfigurationBuilderImpl();
        assertNull("Found metadata resource", builder.getModel(BUS_CONFIGURATION_URI));
        assertNotNull("Could not find metadata resource", builder.getModel(TEST_CONFIGURATION_URI));   
    }
   
}

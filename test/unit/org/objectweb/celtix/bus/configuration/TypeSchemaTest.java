package org.objectweb.celtix.bus.configuration;

import java.net.URL;
import java.util.Collection;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.xml.sax.SAXParseException;

import junit.framework.TestCase;

import org.objectweb.celtix.bus.configuration.TypeSchema.TypeSchemaErrorHandler;
import org.objectweb.celtix.configuration.ConfigurationException;

public class TypeSchemaTest extends TestCase {
    
    private final TypeSchemaHelper tsh = new TypeSchemaHelper();
    
    public void testConstructor() {

        // relative uri with relative path
        
        TypeSchema ts1 = tsh.get("http://celtix.objectweb.org/configuration/test/types",
                            "resources/test-types.xsd");
        assertNotNull(ts1);
        
        TypeSchema ts2 = tsh.get("http://celtix.objectweb.org/configuration/test/types",
                                        "resources/test-types.xsd");
        assertNotNull(ts2);
        assertTrue(ts1 == ts2);
        
        // relative uri with absolute path
        
        TypeSchema ts = new TypeSchema("http://celtix.objectweb.org/configuration/test/types",
                            "/org/objectweb/celtix/bus/configuration/resources/test-types.xsd");
        assertNotNull(ts);

        // absolute uri with relative path
        
        try {
            ts = new TypeSchema("http://celtix.objectweb.org/configuration/test/types", 
                            "file:resources/test-types.xsd");
        } catch (org.objectweb.celtix.configuration.ConfigurationException ex) {
            assertEquals("FILE_OPEN_ERROR_EXC", ex.getCode());
        }
        
        URL url = TypeSchemaTest.class.getResource("resources/test-types.xsd");
        
        // absolute uri with absolute path
        
        ts = new TypeSchema("http://celtix.objectweb.org/configuration/test/types", 
                            "file:" + url.getFile());
        assertNotNull(ts); 
    }

    public void testTypesOnly() {
        TypeSchema ts = tsh.get("http://celtix.objectweb.org/configuration/test/types-types",
            "resources/test-types-types.xsd");
        
        assertEquals(7, ts.getTypes().size()); 
        assertEquals(0, ts.getElements().size());
        
        assertTrue(ts.hasType("bool"));
        assertEquals("boolean", ts.getXMLSchemaBaseType("bool"));
        
        assertTrue(ts.hasType("int"));
        assertEquals("integer", ts.getXMLSchemaBaseType("int"));
        
        assertTrue(ts.hasType("longType"));
        assertEquals("long", ts.getXMLSchemaBaseType("longType"));
        
        assertTrue(ts.hasType("longBaseType"));
        assertEquals("long", ts.getXMLSchemaBaseType("longBaseType"));
        
        assertTrue(ts.hasType("string"));
        assertEquals("string", ts.getXMLSchemaBaseType("string"));
        
        assertTrue(ts.hasType("boolList"));        
        assertNull(ts.getXMLSchemaBaseType("boolList"));
        
        assertTrue(ts.hasType("addressType"));
        assertNull(ts.getXMLSchemaBaseType("addressType"));  
        
        assertNotNull(ts.getSchema());
        assertNotNull(ts.getValidator());
    }

    public void testElementsOnly() {
        TypeSchema ts = tsh.get("http://celtix.objectweb.org/configuration/test/types-elements",
            "resources/test-types-elements.xsd");
        
        assertEquals(0, ts.getTypes().size());
        assertEquals(7, ts.getElements().size()); 
        
        String namespace = "http://celtix.objectweb.org/configuration/test/types-types";
        assertTrue(ts.hasElement("bool"));
        assertEquals(new QName(namespace, "bool"), ts.getDeclaredType("bool"));
        assertTrue(!ts.hasType("bool"));
        try {
            ts.getXMLSchemaBaseType("bool");
        } catch (ConfigurationException ex) {
            assertEquals("TYPE_NOT_DEFINED_IN_NAMESPACE_EXC", ex.getCode());
        }
        
        assertTrue(ts.hasElement("int"));
        assertEquals(new QName(namespace, "int"), ts.getDeclaredType("int"));
        
        assertTrue(ts.hasElement("long"));
        assertEquals(new QName(namespace, "longType"), ts.getDeclaredType("long"));
        
        assertTrue(ts.hasElement("string"));
        assertEquals(new QName(namespace, "string"), ts.getDeclaredType("string"));
        
        assertTrue(ts.hasElement("boolList"));        
        assertEquals(new QName(namespace, "boolList"), ts.getDeclaredType("boolList"));
        
        assertTrue(ts.hasElement("address"));
        assertEquals(new QName(namespace, "addressType"), ts.getDeclaredType("address"));
        
        assertTrue(ts.hasElement("floatValue"));
        assertEquals(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float"), 
                     ts.getDeclaredType("floatValue"));        
        
        assertNotNull(ts.getSchema());
        assertNotNull(ts.getValidator());
        
    }
    
    public void testElementsAndTypes() {
        String namespace = "http://celtix.objectweb.org/configuration/test/types";
        TypeSchema ts = tsh.get("http://celtix.objectweb.org/configuration/test/types",
                                       "resources/test-types.xsd");
        assertNotNull(ts);
        
        assertEquals("org.objectweb.celtix.configuration.test.types", ts.getPackageName());
        assertEquals(7, ts.getTypes().size());
        assertEquals(7, ts.getElements().size()); 
        
        assertTrue(ts.hasElement("bool"));        
        assertEquals(new QName(namespace, "bool"), ts.getDeclaredType("bool"));
        assertTrue(ts.hasType("bool"));
        assertEquals("boolean", ts.getXMLSchemaBaseType("bool"));
        
        assertTrue(ts.hasElement("int"));
        assertEquals(new QName(namespace, "int"), ts.getDeclaredType("int"));
        assertTrue(ts.hasType("int"));
        assertEquals("integer", ts.getXMLSchemaBaseType("int"));
        
        assertTrue(ts.hasElement("long"));
        assertEquals(new QName(namespace, "longType"), ts.getDeclaredType("long"));
        assertTrue(ts.hasType("longType"));
        assertEquals("long", ts.getXMLSchemaBaseType("longType"));
        assertTrue(ts.hasType("longBaseType"));
        assertEquals("long", ts.getXMLSchemaBaseType("longBaseType"));
        
        assertTrue(ts.hasElement("string"));
        assertEquals(new QName(namespace, "string"), ts.getDeclaredType("string"));
        assertTrue(ts.hasType("string"));
        assertEquals("string", ts.getXMLSchemaBaseType("string"));
        
        assertTrue(ts.hasElement("boolList"));        
        assertEquals(new QName(namespace, "boolList"), ts.getDeclaredType("boolList"));
        assertTrue(ts.hasElement("boolList"));
        assertNull(ts.getXMLSchemaBaseType("boolList"));
        
        assertTrue(ts.hasElement("address"));
        assertEquals(new QName(namespace, "addressType"), ts.getDeclaredType("address"));
        assertTrue(ts.hasType("addressType"));
        assertNull(ts.getXMLSchemaBaseType("addressType"));
        
        assertTrue(ts.hasElement("floatValue"));
        assertEquals(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float"), 
                     ts.getDeclaredType("floatValue"));
        assertTrue(!ts.hasType("float"));
        
        assertNotNull(ts.getSchema());
        assertNotNull(ts.getValidator());
    }
    
    public void testAnnotatedPackageName() {
        TypeSchema ts = new TypeSchema("http://celtix.objectweb.org/configuration/test/custom/pkg",
                                       "resources/test-types-annotations.xsd");   
        assertEquals("org.objectweb.celtix.test.custom", ts.getPackageName());
    }
    
    public void testErrorHandler() {
        TypeSchema ts = tsh.get("http://celtix.objectweb.org/configuration/test/types",
            "resources/test-types.xsd");
        TypeSchemaErrorHandler eh = ts.new TypeSchemaErrorHandler();
        SAXParseException spe = new SAXParseException(null, null, null, 0, 0);
        
        try {
            eh.error(spe);
            fail("Expected SAXParseException not thrown.");
        } catch (SAXParseException ex) {
            // ignore;
        }
        
        try {
            eh.warning(spe);
            fail("Expected SAXParseException not thrown.");
        } catch (SAXParseException ex) {
             // ignore;
        }
        
        try {
            eh.fatalError(spe);
            fail("Expected SAXParseException not thrown.");
        } catch (SAXParseException ex) {
             // ignore;
        }          
    }
    
    public void testTypeSchemaHelper() {
        TypeSchema ts = org.easymock.classextension.EasyMock.createMock(TypeSchema.class);
        String namespaceURI = "http://celtix.objectweb.org/helper/test/types";
        assertNull(tsh.get(namespaceURI));
        tsh.put(namespaceURI, ts);
        assertNotNull(tsh.get(namespaceURI));
        assertNotNull(tsh.get(namespaceURI, "/helper/test/types.xsd"));
        Collection<TypeSchema> c = tsh.getTypeSchemas();
        assertTrue(c.size() > 0);
        assertTrue(c.contains(ts));               
    }
}

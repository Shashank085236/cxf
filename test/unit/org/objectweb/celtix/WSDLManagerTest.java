package org.objectweb.celtix;

import java.net.URL;
import javax.wsdl.Definition;
import junit.framework.TestCase;

public class WSDLManagerTest extends TestCase {
    
    public WSDLManagerTest(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(WSDLManagerTest.class);
    }

    /*
     * Test method for 'org.objectweb.celtix.bus.WSDLManagerImpl.getExtenstionRegistry()'
     */
    public void testGetExtenstionRegistry() throws Exception {
        Bus bus = Bus.init(new String[0]);
        try {
            assertNotNull(bus.getWSDLManager().getExtenstionRegistry());
        } finally {
            bus.shutdown(true);
        }
    }

    /*
     * Test method for 'org.objectweb.celtix.bus.WSDLManagerImpl.getDefinition(URL)'
     */
    public void testGetDefinitionURL() throws Exception {
        URL url = getClass().getResource("resources/hello_world.wsdl");
        assertNotNull("Could not find WSDL", url);
        
        Bus bus = Bus.init(new String[0]);
        try {
            Definition def = bus.getWSDLManager().getDefinition(url);
            assertNotNull(def);
            
            Definition def2 = bus.getWSDLManager().getDefinition(url);
            assertTrue(def == def2);
            
            url = null;
            System.gc();
            System.gc();
            url = getClass().getResource("resources/hello_world.wsdl");
            Definition def3 = bus.getWSDLManager().getDefinition(url);
            assertTrue(def != def3);
        } finally {
            bus.shutdown(true);
        }
    }

    /*
     * Test method for 'org.objectweb.celtix.bus.WSDLManagerImpl.getDefinition(String)'
     */
    public void testGetDefinitionString() throws Exception {
        URL neturl = getClass().getResource("resources/hello_world.wsdl");
        assertNotNull("Could not find WSDL", neturl);
        String url = neturl.toString();
        
        
        Bus bus = Bus.init(new String[0]);
        try {
            Definition def = bus.getWSDLManager().getDefinition(url);
            assertNotNull(def);
            
            Definition def2 = bus.getWSDLManager().getDefinition(url);
            assertTrue(def == def2);
            
            url = null;
            System.gc();
            System.gc();
            url = getClass().getResource("resources/hello_world.wsdl").toString();
            Definition def3 = bus.getWSDLManager().getDefinition(url);
            assertTrue(def != def3);
        } finally {
            bus.shutdown(true);
        }
    }

}

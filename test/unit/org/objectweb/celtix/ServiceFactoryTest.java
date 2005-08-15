package org.objectweb.celtix;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceFactory;
import junit.framework.TestCase;

public class ServiceFactoryTest extends TestCase {
    
    public ServiceFactoryTest(String arg0) {
        super(arg0);
        System.setProperty(ServiceFactory.SERVICEFACTORY_PROPERTY,
                "org.objectweb.celtix.bus.ServiceFactoryImpl");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ServiceFactoryTest.class);
    }

    /*
     * Test method for 'javax.xml.ws.ServiceFactory.newInstance()'
     */
    public void testNewInstance() throws Exception {
        Bus bus = Bus.init(new String[0]);
        try {
            assertNotNull(ServiceFactory.newInstance());
        } finally {
            bus.shutdown(true);
        }
    }

    /*
     * Test method for 'org.objectweb.celtix.bus.ServiceFactoryImpl.createService(URL,Class)'
     */
    public void testCreateServiceWithURLClass() throws Exception {
        Bus bus = Bus.init(new String[0]);
        ServiceFactory sf = ServiceFactory.newInstance();
        
        URL url = null;
        try {
            try {
                Class serviceInterface = null;
                Service s  = sf.createService(url, serviceInterface);
            } catch (java.lang.IllegalArgumentException iae) {
                //Expected Exception
            } catch (Exception e) {
                fail("Should have caught IllegalArgumentException");
            } 

            try {
                Class serviceInterface = String.class;
                Service s = sf.createService(url, serviceInterface);
            } catch (java.lang.IllegalArgumentException iae) {
                //Expected Exception
            } catch (Exception e) {
                fail("Should have caught IllegalArgumentException");
            } 
            
        } finally {
            bus.shutdown(true);
        }
    }
    
    /*
     * Test method for 'org.objectweb.celtix.bus.ServiceFactoryImpl.createService(QName)'
     */
    public void testCreateServiceWithServiceName() throws Exception {
        Bus bus = Bus.init(new String[0]);
        ServiceFactory sf = ServiceFactory.newInstance();
        QName serviceName = new QName("http://www.iona.com/hello_world_soap_http", "SOAPService");

        try {
            Service s  = sf.createService(serviceName);
            assertNull(s.getWSDLDocumentLocation());
            assertNotNull(s.getServiceName());
            assertEquals("ServiceName not the same", serviceName, s.getServiceName());
        } catch (Exception e) {
            fail("Should not have caught an exception");
        } finally {
            bus.shutdown(true);
        }
    }

    /*
     * Test method for 'org.objectweb.celtix.bus.ServiceFactoryImpl.createService(URL,QName)'
     */
    public void testCreateServiceWithURLAndServiceName() throws Exception {
        Bus bus = Bus.init(new String[0]);
        ServiceFactory sf = ServiceFactory.newInstance();
        
        QName serviceName = new QName("http://www.iona.com/hello_world_soap_http", "SOAPService");
        URL url = getClass().getResource("resources/hello_world.wsdl");

        try {
            Service s  = sf.createService(url, serviceName);
            assertNotNull(s.getWSDLDocumentLocation());
            assertEquals("URL not the same", url, s.getWSDLDocumentLocation());
            assertNotNull(s.getServiceName());
            assertEquals("ServiceName not the same", serviceName, s.getServiceName());
        } catch (Exception e) {
            fail("Should not have caught an exception");
        } finally {
            bus.shutdown(true);
        }
    }
    
}


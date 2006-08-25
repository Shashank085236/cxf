package org.objectweb.celtix.bus;

import java.net.URI;
import java.util.Properties;

import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointFactory;
import javax.xml.ws.soap.SOAPBinding;

import junit.framework.TestCase;

import org.objectweb.celtix.Bus;
import org.objectweb.hello_world_soap_http.AnnotatedGreeterImpl;
import org.objectweb.hello_world_soap_http.NotAnnotatedGreeterImpl;

public class EndpointFactoryImplTest extends TestCase {
    
    private Bus bus;
    private String epfClassName;
    
    protected void setUp() throws Exception {
        super.setUp();
        epfClassName = System.getProperty(EndpointFactory.ENDPOINTFACTORY_PROPERTY);
        System.setProperty(EndpointFactory.ENDPOINTFACTORY_PROPERTY, 
                           "org.objectweb.celtix.bus.EndpointFactoryImpl");
        bus = Bus.init();
    }

    protected void tearDown() throws Exception {
        bus.shutdown(true);
        if (null == epfClassName) {
            Properties properties = System.getProperties();
            properties.remove(EndpointFactory.ENDPOINTFACTORY_PROPERTY);
            System.setProperties(properties);
        } else {
            System.setProperty(EndpointFactory.ENDPOINTFACTORY_PROPERTY, epfClassName);
        }
      
        super.tearDown();
    }

    public void testNewInstance() throws Exception {  
        assertNotNull(EndpointFactory.newInstance());
    }
    
    public void testCreateWithNotAnnotatedImplementor() throws Exception {
        EndpointFactory epf = EndpointFactory.newInstance();
        Object implementor = new NotAnnotatedGreeterImpl(); 
        Endpoint ep = epf.createEndpoint(new URI(SOAPBinding.SOAP11HTTP_BINDING), implementor);     
        assertNull(ep);
    }
    
    public void testCreateWithCorrectlyAnnotatedImplementor() throws Exception {
        EndpointFactory epf = EndpointFactory.newInstance();
        Object implementor = new AnnotatedGreeterImpl(); 
        Endpoint ep = epf.createEndpoint(new URI(SOAPBinding.SOAP11HTTP_BINDING), implementor);     
        assertNotNull(ep);
    }
   
}

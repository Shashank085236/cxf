package org.objectweb.celtix.jaxws;

import javax.xml.ws.Endpoint;

import junit.framework.TestCase;


public class EndpointTest extends TestCase {
    
    public void testCreateEndpoint() throws Exception {
        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/SoapPort";
        Endpoint e = Endpoint.publish(address, implementor);
        assertNotNull(e);
    }

}

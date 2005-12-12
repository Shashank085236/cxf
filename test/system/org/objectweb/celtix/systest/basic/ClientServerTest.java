package org.objectweb.celtix.systest.basic;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Response;

import org.objectweb.celtix.systest.common.ClientServerTestBase;
import org.objectweb.hello_world_soap_http.BadRecordLitFault;
import org.objectweb.hello_world_soap_http.Greeter;
import org.objectweb.hello_world_soap_http.NoSuchCodeLitFault;
import org.objectweb.hello_world_soap_http.SOAPService;
import org.objectweb.hello_world_soap_http.types.BareDocumentResponse;
import org.objectweb.hello_world_soap_http.types.GreetMeSometimeResponse;

public class ClientServerTest extends ClientServerTestBase {

    private final QName serviceName = new QName("http://objectweb.org/hello_world_soap_http",
                                                "SOAPService");    
    private final QName portName = new QName("http://objectweb.org/hello_world_soap_http",
                                             "SoapPort");

    
    public void onetimeSetUp() { 
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    public void xtestBasicConnection() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);
        
        Greeter greeter = service.getPort(portName, Greeter.class);
        
        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {                        
            for (int idx = 0; idx < 5; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);
                
                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);

                greeter.greetMeOneWay("Milestone-" + idx);
            }            
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        
        try {
            BareDocumentResponse bareres = greeter.testDocLitBare("MySimpleDocument");
            fail("Should have thrown Exception as SOAP Doc/Lit Bare Style is not yet supported");
            assertNotNull("no response for operation testDocLitBare", bareres);
            assertEquals("Celtix", bareres.getCompany());
            assertTrue(bareres.getId() == 1);
        } catch (Exception e) {
            //e.printStackTrace();
            //Ignore as exception is expected.
        }
    } 

    public void testAsyncCall() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        String expectedString = new String("How are you Joe");
        try {
            Greeter greeter = (Greeter) service.getPort(portName, Greeter.class);
            Response<GreetMeSometimeResponse> response = greeter.greetMeSometimeAsync("Joe");
            while (!response.isDone()) {
                Thread.sleep(100);
            }
            GreetMeSometimeResponse reply = response.get();
            assertNotNull("no response received from service", reply);
            String s = reply.getResponseType();
            assertEquals(expectedString, s);    
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
 
    public void testFaults() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        String noSuchCodeFault = "NoSuchCodeLitFault";
        String badRecordFault = "BadRecordLitFault";

        Greeter greeter = service.getPort(portName, Greeter.class);
        for (int idx = 0; idx < 2; idx++) {
            try {
                greeter.testDocLitFault(noSuchCodeFault);
                fail("Should have thrown NoSuchCodeLitFault exception");
            } catch (NoSuchCodeLitFault nslf) {
                assertNotNull(nslf.getFaultInfo());
                assertNotNull(nslf.getFaultInfo().getCode());
            } 
            
            try {
                greeter.testDocLitFault(badRecordFault);
                fail("Should have thrown BadRecordLitFault exception");
            } catch (BadRecordLitFault brlf) {
                assertNotNull(brlf.getFaultInfo());
            }
        }
    } 

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClientServerTest.class);
    }
    /*
    public static void main(String[] args) {
        ClientServerTest cst = new ClientServerTest();
        
        if ("client".equals(args[0])) {
            try {
                cst.testAsyncCall();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if ("server".equals(args[0])) {
            try {
               // cst.setUp();
                cst.onetimeSetUp();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Invaid arg");
        }
    }
    */
}

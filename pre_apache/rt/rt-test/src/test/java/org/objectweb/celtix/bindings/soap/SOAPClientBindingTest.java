package org.objectweb.celtix.bindings.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import junit.framework.TestCase;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.ResponseCallback;
import org.objectweb.celtix.bindings.TestInputStreamContext;
import org.objectweb.celtix.bindings.TestOutputStreamContext;
import org.objectweb.celtix.context.GenericMessageContext;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.jaxb.JAXBDataBindingCallback;
import org.objectweb.celtix.transports.ClientTransport;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;
import org.objectweb.hello_world_soap_http.Greeter;

public class SOAPClientBindingTest extends TestCase {
    Bus bus;
    EndpointReferenceType epr;
    
    public SOAPClientBindingTest(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SOAPClientBindingTest.class);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        bus = Bus.init();
        epr = new EndpointReferenceType();
        
        URL wsdlUrl = getClass().getResource("/wsdl/hello_world.wsdl");
        QName serviceName = new QName("http://objectweb.org/hello_world_soap_http", "SOAPService");
        epr = EndpointReferenceUtils.getEndpointReference(wsdlUrl, serviceName, "SoapPort");        
    }

    public void testGetBinding() throws Exception {
        SOAPClientBinding clientBinding = new SOAPClientBinding(bus, epr);
        assertNotNull(clientBinding.getBinding());
    }

    public void testCreateObjectContext() throws Exception {
        SOAPClientBinding clientBinding = new SOAPClientBinding(bus, epr);
        assertNotNull(clientBinding.createObjectContext());
    }
    
    public void testInvokeOneWay() throws Exception {
        TestClientBinding clientBinding = new TestClientBinding(bus, epr);
        ObjectMessageContext objContext = clientBinding.createObjectContext();
        assertNotNull(objContext);
        Method method = SOAPMessageUtil.getMethod(Greeter.class, "greetMe");
        
        String arg0 = new String("TestSOAPInputPMessage");
        objContext.setMessageObjects(arg0);
        
        clientBinding.invokeOneWay(objContext,
                                   new JAXBDataBindingCallback(method,
                                                               DataBindingCallback.Mode.PARTS, null));        
    }

    public void testhasFault() throws Exception {
        TestClientBinding clientBinding = new TestClientBinding(bus, epr);
        SOAPMessageContext soapCtx = new SOAPMessageContextImpl(new GenericMessageContext());

        InputStream is =  getClass().getResourceAsStream("resources/NoSuchCodeDocLiteral.xml");
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage faultMsg = msgFactory.createMessage(null,  is);
        soapCtx.setMessage(faultMsg);
        assertTrue(clientBinding.getBindingImpl().hasFault(soapCtx));
        
        is =  getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        faultMsg = msgFactory.createMessage(null,  is);
        soapCtx.setMessage(faultMsg);
        assertFalse(clientBinding.getBindingImpl().hasFault(soapCtx));
    }

    public void testRead() throws Exception {
        TestClientBinding clientBinding = new TestClientBinding(bus, epr);
        InputStream is =  getClass().getResourceAsStream("resources/GreetMeDocLiteralResp.xml");
        TestInputStreamContext tisc = new TestInputStreamContext(null);
        tisc.setInputStream(is);
        
        SOAPMessageContext soapCtx = new SOAPMessageContextImpl(new GenericMessageContext());        
        clientBinding.getBindingImpl().read(tisc,  soapCtx);
        assertNotNull(soapCtx.getMessage());
    }

    public void testWrite() throws Exception {
        TestClientBinding clientBinding = new TestClientBinding(bus, epr);
        
        InputStream is =  getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage greetMeMsg = msgFactory.createMessage(null,  is);
        is.close();
        
        BufferedReader br = 
            new BufferedReader(
                new InputStreamReader(
                    getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml")));
        
        SOAPMessageContext soapCtx = new SOAPMessageContextImpl(new GenericMessageContext());
        soapCtx.setMessage(greetMeMsg);
        
        TestOutputStreamContext tosc = new TestOutputStreamContext(null, soapCtx);
        clientBinding.getBindingImpl().write(soapCtx, tosc);

        byte[] bArray = tosc.getOutputStreamBytes();
        assertEquals(br.readLine(), (new String(bArray)).trim());
    }
    
    class TestClientBinding extends SOAPClientBinding {

        public TestClientBinding(Bus b, EndpointReferenceType ref) 
            throws WSDLException, IOException {
            super(b, ref);
        }

        protected ClientTransport createTransport(EndpointReferenceType ref)
            throws WSDLException, IOException {
            // REVISIT: non-null response callback
            return new TestClientTransport(bus, ref);
        }

    }
    
    class TestClientTransport implements ClientTransport {
        
        public TestClientTransport(Bus b, EndpointReferenceType ref) {
        }

        public EndpointReferenceType getTargetEndpoint() {
            return null;
        }
        
        public EndpointReferenceType getDecoupledEndpoint() throws IOException {
            return null;
        }
        
        public Port getPort() {
            return null;
        }

        public OutputStreamMessageContext createOutputStreamContext(MessageContext context) 
            throws IOException {
            return new TestOutputStreamContext(null, context);
        }

        public void finalPrepareOutputStreamContext(OutputStreamMessageContext context) throws IOException {
        }

        public void invokeOneway(OutputStreamMessageContext context) throws IOException {
            //nothing to do
        }

        public InputStreamMessageContext invoke(OutputStreamMessageContext context) throws IOException {
            return context.getCorrespondingInputStreamContext();
        }

        public Future<InputStreamMessageContext> invokeAsync(OutputStreamMessageContext context, Executor ex) 
            throws IOException {
            return null;
        }
        
        public ResponseCallback getResponseCallback() {
            return null;
        }

        public void shutdown() {
            //nothing to do
        }
    }
    
    
}

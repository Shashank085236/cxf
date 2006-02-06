package org.objectweb.celtix.bus.transports.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.easymock.classextension.EasyMock;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bus.busimpl.ComponentCreatedEvent;
import org.objectweb.celtix.bus.busimpl.ComponentRemovedEvent;
import org.objectweb.celtix.bus.transports.TestResponseCallback;
import org.objectweb.celtix.bus.transports.TransportFactoryManagerImpl;
import org.objectweb.celtix.bus.workqueue.WorkQueueManagerImpl;
import org.objectweb.celtix.bus.wsdl.WSDLManagerImpl;
import org.objectweb.celtix.buslifecycle.BusLifeCycleManager;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.types.ClassNamespaceMappingListType;
import org.objectweb.celtix.configuration.types.ClassNamespaceMappingType;
import org.objectweb.celtix.configuration.types.ObjectFactory;
import org.objectweb.celtix.context.GenericMessageContext;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ClientTransport;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.ServerTransportCallback;
import org.objectweb.celtix.transports.TransportFactory;
import org.objectweb.celtix.transports.TransportFactoryManager;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;
import org.objectweb.celtix.wsdl.WSDLManager;
import static org.easymock.EasyMock.isA;

public class HTTPTransportTest extends TestCase {

    private static final QName SERVICE_NAME = new 
        QName("http://objectweb.org/hello_world_soap_http", "SOAPService");
    private static final String PORT_NAME = "SoapPort";
    private static final String ADDRESS = "http://localhost:9000/SoapContext/SoapPort";
    private static final URL WSDL_URL = HTTPTransportTest.class.getResource("/wsdl/hello_world.wsdl");
    
    private static boolean first = true;
    
    Bus bus;
    private WSDLManager wsdlManager;
    private WorkQueueManagerImpl queueManager;
    private ExecutorService executorService;
    private TestResponseCallback responseCallback;
    
    public HTTPTransportTest(String arg0) {
        super(arg0);
    }
    
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(HTTPTransportTest.class);
        return new TestSetup(suite) {
            protected void tearDown() throws Exception {
                super.tearDown();
                JettyHTTPServerEngine.destroyForPort(9000);
            }
        };
    }
    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HTTPTransportTest.class);
    }
    
    public void setUp() throws BusException {
        bus = EasyMock.createMock(Bus.class);
        wsdlManager = new WSDLManagerImpl(null);
    }
    public void tearDown() throws Exception {
        EasyMock.reset(bus);
        checkBusRemovedEvent();
        EasyMock.replay(bus);
        
        if (queueManager != null) {
            queueManager.shutdown(false);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
    
    int readBytes(byte bytes[], InputStream ins) throws IOException {
        int len = ins.read(bytes);
        int total = 0;
        while (len != -1) {
            total += len;
            len = ins.read(bytes, total, bytes.length - total);
        }
        return total;
    }

   
    public void testInvokeOneway() throws Exception {
               
        TransportFactory factory = createTransportFactory();
      
        ServerTransport server = createServerTransport(factory, WSDL_URL, SERVICE_NAME,
                                                       PORT_NAME, ADDRESS);
        byte[] buffer = new byte[64];
        activateServer(server, false, 200, buffer, true);
        
        ClientTransport client = createClientTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
        byte outBytes[] = "Hello World!!!".getBytes();

        long start = System.currentTimeMillis();
        OutputStreamMessageContext octx = doRequest(client, outBytes);
        client.invokeOneway(octx);
        long stop = System.currentTimeMillis();

        octx = doRequest(client, outBytes);
        client.invokeOneway(octx);
        octx = doRequest(client, outBytes);
        client.invokeOneway(octx);
        long stop2 = System.currentTimeMillis();
        
        server.deactivate();  
        EasyMock.reset(bus);
        checkBusRemovedEvent();
        EasyMock.replay(bus); 
        client.shutdown();
        
        assertTrue("Total one call: " + (stop - start), (stop - start) < 400);
        assertTrue("Total: " + (stop2 - start), (stop2 - start) < 400);
        assertEquals(new String(outBytes), new String(buffer, 0, outBytes.length));
        Thread.sleep(200);
    } 
    public void testInvoke() throws Exception {
        doTestInvoke(false);
        doTestInvoke(false);
    }
    public void testInvokeUsingAutomaticWorkQueue() throws Exception {
        doTestInvoke(true);
    }
    
    public void testInvokeAsync() throws Exception {      
        doTestInvokeAsync(false);
    }
    
    public void testInvokeAsyncUsingAutomaticWorkQueue() throws Exception {      
        doTestInvokeAsync(true);
    }
    

    public void testInputStreamMessageContextCallable() throws Exception {
        HTTPClientTransport.HTTPClientOutputStreamContext octx = 
            EasyMock.createMock(HTTPClientTransport.HTTPClientOutputStreamContext.class);
        HTTPClientTransport.HTTPClientInputStreamContext ictx =
            EasyMock.createMock(HTTPClientTransport.HTTPClientInputStreamContext.class);
        octx.createInputStreamContext();
        EasyMock.expectLastCall().andReturn(ictx);
        EasyMock.replay(octx);
        
        Callable c = new HTTPClientTransport.InputStreamMessageContextCallable(octx);
        assertNotNull(c);
        InputStreamMessageContext result = (InputStreamMessageContext)c.call();
        assertEquals(result, ictx); 
    }
    public void doTestInvoke(final boolean useAutomaticWorkQueue) throws Exception {
               
        TransportFactory factory = createTransportFactory();
      
        ServerTransport server = createServerTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
             
        activateServer(server, useAutomaticWorkQueue, 0, null, false);
        //short request
        ClientTransport client = createClientTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
        doRequestResponse(client, "Hello World".getBytes());
        
        //long request
        byte outBytes[] = new byte[5000];
        for (int x = 0; x < outBytes.length; x++) {
            outBytes[x] = (byte)('a' + (x % 26));
        }
        client = createClientTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
        doRequestResponse(client, outBytes);
        
        server.deactivate();
        outBytes = "HelloWorld".getBytes();
 
        try {
            OutputStreamMessageContext octx = client.createOutputStreamContext(new GenericMessageContext());
            client.finalPrepareOutputStreamContext(octx);
            octx.getOutputStream().write(outBytes);
            octx.getOutputStream().close();
            InputStreamMessageContext ictx = client.invoke(octx);
            byte bytes[] = new byte[10000];
            int len = ictx.getInputStream().read(bytes);
            if (len != -1
                && new String(bytes, 0, len).indexOf("HTTP Status 503") == -1
                && new String(bytes, 0, len).indexOf("Error 404") == -1) {
                fail("was able to process a message after the servant was deactivated: " + len 
                     + " - " + new String(bytes));
            }
        } catch (IOException ex) {
            //ignore - this is what we want
        }
        activateServer(server, useAutomaticWorkQueue, 0, null, false);
        doRequestResponse(client, "Hello World   3".getBytes());
        server.deactivate();        
        activateServer(server, useAutomaticWorkQueue, 0, null, false);
        doRequestResponse(client, "Hello World   4".getBytes());
        server.deactivate();  
        EasyMock.reset(bus);
        checkBusRemovedEvent();       
        EasyMock.replay(bus);
        client.shutdown();
    }
    
    public void doTestInvokeAsync(final boolean useAutomaticWorkQueue) throws Exception {
        
        Executor executor =  null;
        if (useAutomaticWorkQueue) {
            queueManager = new WorkQueueManagerImpl(bus);
            executor = queueManager.getAutomaticWorkQueue();
        } else {
            executorService = Executors.newFixedThreadPool(1);
            executor = executorService;
        }
        TransportFactory factory = createTransportFactory();
        
        ServerTransport server = createServerTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
        activateServer(server, false, 400, null, false);
        
        ClientTransport client = createClientTransport(factory, WSDL_URL, SERVICE_NAME, PORT_NAME, ADDRESS);
        byte outBytes[] = "Hello World!!!".getBytes();
        
        // wait then read without blocking
        OutputStreamMessageContext octx = doRequest(client, outBytes);
        Future<InputStreamMessageContext> f = client.invokeAsync(octx, executor);
        assertNotNull(f);
        assertFalse(f.isDone());
        int i = 0;
        while (i < 10) {
            Thread.sleep(100);
            if (f.isDone()) {
                break;                
            }
            i++;
        }
        assertTrue(f.isDone());
        InputStreamMessageContext ictx = f.get();
        doResponse(client, ictx, outBytes);
        
        // blocking read (on new thread)
        octx = doRequest(client, outBytes);        
        f = client.invokeAsync(octx, executor);
        ictx = f.get();
        assertTrue(f.isDone());
        doResponse(client, ictx, outBytes);
        
        // blocking read times out
        boolean timeoutImplemented = false;
        if (timeoutImplemented) {
            octx = doRequest(client, outBytes);        
            f = client.invokeAsync(octx, executor);
            try {            
                ictx = f.get(200, TimeUnit.MILLISECONDS);
                fail("Expected TimeoutException not thrown.");
            } catch (TimeoutException ex) {
                // ignore
            }
            assertTrue(!f.isDone());
        }
        server.deactivate();        
    }
    
    private void checkBusCreatedEvent() {       
        try {
            bus.sendEvent(isA(ComponentCreatedEvent.class));
        } catch (BusException e) {                  
            e.printStackTrace();
        }
        EasyMock.expectLastCall();        
    }
    
    private void checkBusRemovedEvent() {       
        try {
            bus.sendEvent(isA(ComponentRemovedEvent.class));
        } catch (BusException e) {                  
            e.printStackTrace();
        }
        EasyMock.expectLastCall();        
    }
    
    private void activateServer(ServerTransport server,
                                final boolean useAutomaticWorkQueue,
                                final int delay,
                                final byte[] buffer,
                                final boolean oneWay) throws Exception {
        ServerTransportCallback callback = new ServerTransportCallback() {
            public void dispatch(InputStreamMessageContext ctx, ServerTransport transport) {
                try {
                    byte[] bytes = buffer;
                    if (null == bytes) {
                        bytes = new byte[10000];
                    }
                    int total = readBytes(bytes, ctx.getInputStream());
                    
                    OutputStreamMessageContext octx = transport.createOutputStreamContext(ctx);
                    octx.setOneWay(oneWay);
                    transport.finalPrepareOutputStreamContext(octx);
                    
                    if (delay > 0) {                       
                        Thread.sleep(delay);
                    }
                    if (!oneWay) {
                        octx.getOutputStream().write(bytes, 0, total);
                        octx.getOutputStream().flush();
                    }
                    octx.getOutputStream().close();
                    transport.postDispatch(ctx, octx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            public synchronized Executor getExecutor() {
                EasyMock.reset(bus);
                checkBusCreatedEvent();
                EasyMock.replay(bus);
                if (useAutomaticWorkQueue) {
                    if (queueManager == null) {
                        queueManager = new WorkQueueManagerImpl(bus);
                    }
                    return queueManager.getAutomaticWorkQueue();
                } else {
                    return null;
                }
            }
        };

        EasyMock.reset(bus);
        Configuration bc = EasyMock.createMock(Configuration.class);
        bus.getConfiguration();
        EasyMock.expectLastCall().andReturn(bc);
        server.activate(callback);        
    }
    
    private void doRequestResponse(ClientTransport client, byte outBytes[]) throws Exception {
        OutputStreamMessageContext octx = doRequest(client, outBytes);
        InputStreamMessageContext ictx = client.invoke(octx);
        if (ictx != null) {
            doResponse(client, ictx, outBytes);
        } else {
            doResponse(client, responseCallback.waitForNextResponse(), outBytes); 
        }
    }
    
    private OutputStreamMessageContext doRequest(ClientTransport client, byte outBytes[]) throws Exception {
        OutputStreamMessageContext octx = client.createOutputStreamContext(new GenericMessageContext());
        client.finalPrepareOutputStreamContext(octx);
        octx.getOutputStream().write(outBytes);
        return octx;
    }
    
    private void doResponse(ClientTransport client, 
        InputStreamMessageContext ictx, byte outBytes[]) throws Exception {
        byte bytes[] = new byte[10000];
        int len = readBytes(bytes, ictx.getInputStream());
        assertTrue("Did not read anything " + len, len > 0);
        assertEquals(new String(outBytes), new String(bytes, 0, len));
    }
    
    private TransportFactory createTransportFactory() throws BusException { 
        EasyMock.reset(bus);
        Configuration bc = EasyMock.createMock(Configuration.class);
        
        String transportId = "http://celtix.objectweb.org/transports/http/configuration";
        ObjectFactory of = new ObjectFactory();
        ClassNamespaceMappingListType mappings = of.createClassNamespaceMappingListType();
        ClassNamespaceMappingType mapping = of.createClassNamespaceMappingType();
        mapping.setClassname("org.objectweb.celtix.bus.transports.http.HTTPTransportFactory");
        mapping.getNamespace().add(transportId);
        mappings.getMap().add(mapping);
        
        bus.getWSDLManager();
        EasyMock.expectLastCall().andReturn(wsdlManager);
        bus.getWSDLManager();
        EasyMock.expectLastCall().andReturn(wsdlManager);
        BusLifeCycleManager lifecycleManager = EasyMock.createNiceMock(BusLifeCycleManager.class);
        bus.getLifeCycleManager();
        EasyMock.expectLastCall().andReturn(lifecycleManager);
        bus.getConfiguration();
        EasyMock.expectLastCall().andReturn(bc);
        bc.getObject("transportFactories");
        EasyMock.expectLastCall().andReturn(mappings);    
        
        EasyMock.replay(bus);
        EasyMock.replay(bc); 
        
        TransportFactoryManager tfm = new TransportFactoryManagerImpl(bus);
        TransportFactory factory = tfm.getTransportFactory(transportId);
        responseCallback = new TestResponseCallback();
        factory.setResponseCallback(responseCallback);
        return factory;
    }
    
    private ClientTransport createClientTransport(TransportFactory factory, URL wsdlUrl, 
                                                  QName serviceName, String portName, 
                                                  String address) throws WSDLException, IOException {
        EasyMock.reset(bus);
        
        Configuration bc = EasyMock.createMock(Configuration.class);
        Configuration sc = EasyMock.createMock(Configuration.class);
        Configuration pc = EasyMock.createMock(Configuration.class);
        
        bus.getConfiguration();
        EasyMock.expectLastCall().andReturn(bc);
        bc.getChild("http://celtix.objectweb.org/bus/jaxws/service-config", serviceName);
        EasyMock.expectLastCall().andReturn(sc);
        sc.getChild("http://celtix.objectweb.org/bus/jaxws/port-config", portName);
        EasyMock.expectLastCall().andReturn(pc);  
        bus.getWSDLManager();
        EasyMock.expectLastCall().andReturn(wsdlManager);
        pc.getString("address");
        EasyMock.expectLastCall().andReturn(address);
        
        checkBusCreatedEvent();
        
        EasyMock.replay(bus);
        EasyMock.replay(bc);
        EasyMock.replay(sc);
        EasyMock.replay(pc);
        
        EndpointReferenceType ref = EndpointReferenceUtils
            .getEndpointReference(wsdlUrl, serviceName, portName);
        ClientTransport transport = factory.createClientTransport(ref);
       
        EasyMock.verify(bus);
        EasyMock.verify(bc);
        EasyMock.verify(sc);
        EasyMock.verify(pc);
        return transport;
        
    }
    
    private ServerTransport createServerTransport(TransportFactory factory, URL wsdlUrl, QName serviceName,
                                                  String portName, String address)
        throws WSDLException, IOException {
        EasyMock.reset(bus);

        Configuration bc = EasyMock.createMock(Configuration.class);
        Configuration ec = EasyMock.createMock(Configuration.class);

        bus.getConfiguration();
        EasyMock.expectLastCall().andReturn(bc);
        bc.getChild("http://celtix.objectweb.org/bus/jaxws/endpoint-config", serviceName);
        EasyMock.expectLastCall().andReturn(ec);
        bus.getWSDLManager();
        EasyMock.expectLastCall().andReturn(wsdlManager);
        if (first) {
            //first call will configure the port listener
            bus.getConfiguration();
            EasyMock.expectLastCall().andReturn(bc);
            first = false;
        }
        
        checkBusCreatedEvent();
       
        EasyMock.replay(bus);
        EasyMock.replay(bc);
        EasyMock.replay(ec);

        EndpointReferenceType ref = EndpointReferenceUtils.getEndpointReference(wsdlUrl, serviceName,
                                                                                portName);
        EndpointReferenceUtils.setAddress(ref, address);
        ServerTransport transport = factory.createServerTransport(ref);

        EasyMock.verify(bus);
        EasyMock.verify(bc);
        EasyMock.verify(ec);
        
        return transport;

    }
}

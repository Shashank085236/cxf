package org.objectweb.celtix.bus.transports.jms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executor;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.activemq.broker.BrokerContainer;
import org.activemq.broker.impl.BrokerContainerImpl;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bus.transports.TransportFactoryManagerImpl;

import org.objectweb.celtix.bus.workqueue.WorkQueueManagerImpl;
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
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public class OneWayJMSTransportTest extends TestCase {
    private ServerTransportCallback callback;
    private Bus bus;
    private String serverRcvdInOneWayCall;
    
    public OneWayJMSTransportTest(String arg0) {
        super(arg0);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(OneWayJMSTransportTest.class);
        return  new JMSBrokerSetup(suite);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OneWayJMSTransportTest.suite());
    }
    
    public void setUp() throws Exception {
        bus = Bus.init();
        serverRcvdInOneWayCall = null;
    }
    
    public void tearDown() throws Exception {
        //
    }
    
    public void xtestOneWayTextQueueJMSTransport() throws Exception {
        QName serviceName =  new QName("http://celtix.objectweb.org/hello_world_jms", 
                                                           "HelloWorldOneWayQueueService");
        doOneWayTestJMSTranport(false,  serviceName, "HelloWorldOneWayQueuePort", 
                                    "/wsdl/jms_test.wsdl");   
    }

    public void testPubSubJMSTransport() throws Exception {
        QName serviceName =  new QName("http://celtix.objectweb.org/hello_world_jms", 
                                                           "HelloWorldPubSubService");
        doOneWayTestJMSTranport(false,  serviceName, "HelloWorldPubSubPort", 
                                               "/wsdl/jms_test.wsdl");
    }

    private int readBytes(byte bytes[], InputStream ins) throws IOException {
        int len = ins.read(bytes);
        int total = 0;
        while (len != -1) {
            total += len;
            len = ins.read(bytes, total, bytes.length - total);
        }
        return total;
    }
    
    public void setupCallbackObject(final boolean useAutomaticWorkQueue) {
        callback = new ServerTransportCallback() {
            public void dispatch(InputStreamMessageContext ctx, ServerTransport transport) {
                try {
                    byte bytes[] = new byte[10000];
                    readBytes(bytes, ctx.getInputStream());
                    
                    JMSOutputStreamContext octx = 
                        (JMSOutputStreamContext) transport.createOutputStreamContext(ctx);
                    octx.setOneWay(true);
                    transport.finalPrepareOutputStreamContext(octx);
                    serverRcvdInOneWayCall = new String(bytes);
                    
                    MessageContext replyCtx = new GenericMessageContext();
                    ctx.put("ObjectMessageContext.MESSAGE_INPUT", Boolean.TRUE);
                    replyCtx.putAll(ctx);
                    replyCtx.put("ObjectMessageContext.MESSAGE_INPUT", Boolean.TRUE);
                    
                    ((JMSServerTransport) transport).postDispatch(replyCtx, octx);
                    octx.getOutputStream().close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            public Executor getExecutor() {
                if (useAutomaticWorkQueue) {
                    return new WorkQueueManagerImpl(bus).getAutomaticWorkQueue();
                } else {
                    return null;
                }
                
            }
        };
    }
    
    public void doOneWayTestJMSTranport(final boolean useAutomaticWorkQueue,
                                                            QName serviceName, 
                                                             String portName, 
                                                             String testWsdlFileName) 
        throws Exception {
        
        String address = "http://localhost:9000/SoapContext/SoapPort";
        URL wsdlUrl = getClass().getResource(testWsdlFileName);
        assertNotNull(wsdlUrl);
               
        TransportFactory factory = createTransportFactory();
      
        ServerTransport server = createServerTransport(factory, wsdlUrl, serviceName, 
                                                       portName, address);
        setupCallbackObject(useAutomaticWorkQueue);
        
        server.activate(callback);
        
        ClientTransport client = createClientTransport(factory, wsdlUrl, serviceName, portName, address);
        OutputStreamMessageContext octx = 
            client.createOutputStreamContext(new GenericMessageContext());
        client.finalPrepareOutputStreamContext(octx);
        byte outBytes[] = "Hello World!!!".getBytes(); 
        octx.getOutputStream().write(outBytes);
        client.invokeOneway(octx);
        Thread.sleep(500L);
        assertEquals(new String(outBytes), 
                          serverRcvdInOneWayCall.substring(0, outBytes.length));
        
    }
        
    private TransportFactory createTransportFactory() throws BusException { 

        String transportId = "http://celtix.objectweb.org/transports/jms";
        ObjectFactory of = new ObjectFactory();
        ClassNamespaceMappingListType mappings = of.createClassNamespaceMappingListType();
        ClassNamespaceMappingType mapping = of.createClassNamespaceMappingType();
        mapping.setClassname("org.objectweb.celtix.bus.transports.jms.JMSTransportFactory");
        mapping.getNamespace().add(transportId);
        mappings.getMap().add(mapping);

        TransportFactoryManager tfm = new TransportFactoryManagerImpl(bus);
        return tfm.getTransportFactory(transportId);   
    }
    
    private ClientTransport createClientTransport(TransportFactory factory, URL wsdlUrl, 
                                                  QName serviceName, String portName, 
                                                  String address) throws WSDLException, IOException {

        EndpointReferenceType ref = EndpointReferenceUtils
            .getEndpointReference(wsdlUrl, serviceName, portName);
        ClientTransport transport = factory.createClientTransport(ref);

        return transport;
    }
    
    private ServerTransport createServerTransport(TransportFactory factory, URL wsdlUrl, QName serviceName,
                                                  String portName, String address) throws WSDLException,
        IOException {
        EndpointReferenceType ref = EndpointReferenceUtils.getEndpointReference(wsdlUrl, serviceName,
                                                                                portName);
        EndpointReferenceUtils.setAddress(ref, address);
        return  factory.createServerTransport(ref);
    }
    
    protected static class JMSBrokerSetup extends TestSetup {
        Thread jmsBrokerThread;
        public JMSBrokerSetup(TestSuite suite) {
            super(suite);
        }
        
        public void setUp() throws Exception {
            jmsBrokerThread = new JMSEmbeddedBroker("tcp://localhost:61616");
     
            jmsBrokerThread.start();
            Thread.sleep(200L);            
        }
        
        public void tearDown() throws Exception {
            ((JMSEmbeddedBroker) jmsBrokerThread).shutdownBroker = true;
            if (jmsBrokerThread != null) {
                jmsBrokerThread.join(200L);
            }
        }
        
        class JMSEmbeddedBroker extends Thread {
            boolean shutdownBroker;
            final String brokerUrl;
            
            
            public JMSEmbeddedBroker(String url) {
                brokerUrl = url;
            }
            
            public void run() {
                try {                
                    BrokerContainer container = new BrokerContainerImpl();
                    container.addConnector(brokerUrl);
                    container.start();
                    Object lock = new Object();                
                    
                    while (!shutdownBroker) {
                        synchronized (lock) {
                            lock.wait(200L);
                        }
                    }
                    container.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

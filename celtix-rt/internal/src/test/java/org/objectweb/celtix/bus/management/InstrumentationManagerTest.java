package org.objectweb.celtix.bus.management;

import java.util.List;
import junit.framework.TestCase;

//import org.easymock.classextension.EasyMock;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bus.busimpl.ComponentCreatedEvent;
import org.objectweb.celtix.bus.busimpl.ComponentRemovedEvent;

import org.objectweb.celtix.bus.management.jmx.export.AnnotationTestInstrumentation;
// import org.objectweb.celtix.bus.transports.http.HTTPClientTransport;
// import org.objectweb.celtix.bus.transports.jms.JMSClientTransport;
//import org.objectweb.celtix.bus.management.MockComponent;
import org.objectweb.celtix.bus.workqueue.WorkQueueInstrumentation;
import org.objectweb.celtix.bus.workqueue.WorkQueueManagerImpl;
import org.objectweb.celtix.management.Instrumentation;
import org.objectweb.celtix.management.InstrumentationManager;

//import org.objectweb.celtix.management.MockComponentInstrumentation;


public class InstrumentationManagerTest extends TestCase {
    Bus bus;
    InstrumentationManager im;    
    
    public void setUp() throws Exception {
        bus = Bus.init();       
        im = bus.getInstrumentationManager();
    }
    
    public void tearDown() throws Exception {
        //test case had done the bus.shutdown         
    }
    
    // try to get WorkQueue information
    public void testWorkQueueInstrumentation() throws BusException {
        //im.getAllInstrumentation();
        WorkQueueManagerImpl wqm = new WorkQueueManagerImpl(bus);
        bus.sendEvent(new ComponentCreatedEvent(wqm));        
        bus.sendEvent(new ComponentCreatedEvent(wqm));
        //NOTE: now the bus WorkQueueManager is lazy load , if WorkQueueManager 
        //create with bus , this test could be failed.
        List<Instrumentation> list = im.getAllInstrumentation();   
        //NOTE: change for the BindingManager and TransportFactoryManager instrumentation 
        // create with the bus.
        assertEquals("Too many instrumented items", 4, list.size());
        Instrumentation it1 = list.get(2);
        Instrumentation it2 = list.get(3);
        assertTrue("Item 1 not a WorkQueueInstrumentation",
                   WorkQueueInstrumentation.class.isAssignableFrom(it1.getClass()));
        assertTrue("Item 2 not a WorkQueueInstrumentation",
                   WorkQueueInstrumentation.class.isAssignableFrom(it2.getClass()));
        
        // not check for the instrumentation unique name
        // sleep for the MBServer connector thread startup 
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // do nothing
        }
        bus.sendEvent(new ComponentRemovedEvent(wqm));
        assertEquals("Instrumented stuff not removed from list", 2, list.size());
        bus.shutdown(true);
        assertEquals("Instrumented stuff not removed from list", 0, list.size());
    }
    

    public void testMoreInstrumentation() throws BusException {
        //im.getAllInstrumentation();
        WorkQueueManagerImpl wqm = new WorkQueueManagerImpl(bus);
        bus.sendEvent(new ComponentCreatedEvent(wqm));        

        MockComponent mc = new MockComponent();
        bus.sendEvent(new ComponentCreatedEvent(mc));
//         JMSClientTransport jct = 
//             EasyMock.createMock(JMSClientTransport.class);
//         bus.sendEvent(new ComponentCreatedEvent(jct));
        
//         HTTPClientTransport hct = 
//             EasyMock.createMock(HTTPClientTransport.class);
//         bus.sendEvent(new ComponentCreatedEvent(hct));
        
        // TODO should test for the im getInstrumentation 
        List<Instrumentation> list = im.getAllInstrumentation();        
        assertEquals("Too many instrumented items", 4, list.size());
        // sleep for the MBServer connector thread startup 
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // do nothing
        }
        
        bus.sendEvent(new ComponentRemovedEvent(wqm));
        bus.sendEvent(new ComponentRemovedEvent(mc));
//         bus.sendEvent(new ComponentRemovedEvent(jct));
//         bus.sendEvent(new ComponentRemovedEvent(hct));
        assertEquals("Instrumented stuff not removed from list", 2, list.size());
        bus.shutdown(true);
        assertEquals("Instrumented stuff not removed from list", 0, list.size());
    }
    
    public void testCustemerInstrumentationByEvent() throws BusException {
        AnnotationTestInstrumentation ati = new AnnotationTestInstrumentation();
        bus.sendEvent(new ComponentCreatedEvent(ati));
        
        List<Instrumentation> list = im.getAllInstrumentation();
        assertEquals("Not exactly the number of instrumented item", 3, list.size());
        
        // get the ati for more assert
        Instrumentation instr = list.get(2);
        assertEquals("Not exactly the name of AnnotationTestInstrumentation",
                     "AnnotationTestInstrumentation",
                     instr.getInstrumentationName());
        bus.sendEvent(new ComponentRemovedEvent(ati));
        assertEquals("AnnotationTestInstrumented stuff not removed from list", 2, list.size());
        bus.shutdown(true);
        assertEquals("Instrumented stuff not removed from list", 0, list.size());
        
    }
    
    public void testCustemerInstrumentationByInstrumentationManager() throws BusException {
        AnnotationTestInstrumentation ati = new AnnotationTestInstrumentation();
        im.register(ati);
        
        List<Instrumentation> list = im.getAllInstrumentation();
        assertEquals("Not exactly the number of instrumented item", 3, list.size());
        
        // get the ati for more assert
        Instrumentation instr = list.get(2);
        assertEquals("Not exactly the name of AnnotationTestInstrumentation",
                     "AnnotationTestInstrumentation",
                     instr.getInstrumentationName());
        im.unregister(ati);
        assertEquals("AnnotationTestInstrumented stuff not removed from list", 2, list.size());
        bus.shutdown(true);
        assertEquals("Instrumented stuff not removed from list", 0, list.size());
    }
    
      

}

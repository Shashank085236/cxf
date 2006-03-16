package org.objectweb.celtix.bus.management;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusEvent;
import org.objectweb.celtix.BusEventListener;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bus.bindings.BindingManagerImpl;
import org.objectweb.celtix.bus.bindings.BindingManagerInstrumentation;
import org.objectweb.celtix.bus.busimpl.ComponentCreatedEvent;
import org.objectweb.celtix.bus.busimpl.ComponentRemovedEvent;

import org.objectweb.celtix.bus.management.jmx.JMXManagedComponentManager;
import org.objectweb.celtix.bus.transports.TransportFactoryManagerImpl;
import org.objectweb.celtix.bus.transports.TransportFactoryManagerInstrumentation;
import org.objectweb.celtix.bus.transports.http.HTTPClientTransport;
import org.objectweb.celtix.bus.transports.http.HTTPClientTransportInstrumentation;
import org.objectweb.celtix.bus.transports.http.HTTPServerTransportInstrumentation;
import org.objectweb.celtix.bus.transports.http.JettyHTTPServerTransport;
import org.objectweb.celtix.bus.transports.jms.JMSClientTransport;
import org.objectweb.celtix.bus.transports.jms.JMSClientTransportInstrumentation;
import org.objectweb.celtix.bus.transports.jms.JMSServerTransport;
import org.objectweb.celtix.bus.transports.jms.JMSServerTransportInstrumentation;
import org.objectweb.celtix.bus.workqueue.WorkQueueInstrumentation;
import org.objectweb.celtix.bus.workqueue.WorkQueueManagerImpl;
import org.objectweb.celtix.bus.wsdl.WSDLManagerImpl;
import org.objectweb.celtix.bus.wsdl.WSDLManagerInstrumentation;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.management.Instrumentation;
import org.objectweb.celtix.management.InstrumentationManager;




/** The basic manager information center for common management model
 *  Instrumentation components will be registed to InstrumenationManager.
 *  The Instrumentation mananger will send event to notifier the management 
 *  layer to expose the managed component. 
 *  Instrumentation manager also provider a qurey interface for the instrumentation.
 *  The JMX layer could query the detail information for instrumentation.
 */
public class InstrumentationManagerImpl implements InstrumentationManager, BusEventListener {    
    static final Logger LOG = LogUtils.getL7dLogger(InstrumentationManagerImpl.class);
    static final String INSTRUMENTATION_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/bus/bus-config";
    static final String INSTRUMENTATION_CONFIGURATION_ID = 
        "InstrumentationControl";
    private Bus bus;
    private List <Instrumentation> instrumentations;
    private JMXManagedComponentManager jmxManagedComponentManager;
    private ComponentEventFilter componentEventFilter;
    private boolean instrumentationEnabled;
    private boolean jmxEnabled;
    
    public InstrumentationManagerImpl(Bus b) throws BusException {
        //InstrumentationPolicyType instrumentation = null;
        //JMXConnectorPolicyType connector = null;
        bus = b;
        
        /*Object obj = bus.getConfiguration().getObject("bindingFactories");
        
        List<ClassNamespaceMappingType> factoryMappings = ((ClassNamespaceMappingListType)obj).getMap();
        for (ClassNamespaceMappingType mapping : factoryMappings) {
            System.out.println(mapping.getClassname());            
        }
        
        Configuration busConfiguration = bus.getConfiguration(); 
        connector = (JMXConnectorPolicyType) 
            bus.getConfiguration().getObject("JMXConnector");
        
        if (connector != null) {
            System.out.println(connector.getJMXSeviceURL());
        } 
        Configuration busConfiguration = bus.getConfiguration(); 
        
        if (busConfiguration != null) {        
            instrumentation = (InstrumentationPolicyType) 
                busConfiguration.getObject("InstrumentationControl");            
        } 
        
        if (instrumentation == null) {
            System.out.println("instrumentation is null");
            instrumentation = new InstrumentationPolicyType();
        }
        
        //TODO There no effect of the configuration xml change
        instrumentationEnabled = instrumentation.isInstrumentationEnabled();
        jmxEnabled = instrumentation.isJMXEnabled();*/
        instrumentationEnabled = true;
        jmxEnabled = true;
        
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Setting up InstrumentationManager for BUS");
        }    
        
        // get the configuration
        if (instrumentationEnabled) {
            instrumentations = new LinkedList<Instrumentation>();
            componentEventFilter = new ComponentEventFilter();
            bus.addListener((BusEventListener)this, 
                            componentEventFilter);
        }
        
        if (jmxEnabled) {
            jmxManagedComponentManager = new JMXManagedComponentManager(bus);
        
            jmxManagedComponentManager.init();
        
            bus.addListener((BusEventListener)jmxManagedComponentManager, 
                        jmxManagedComponentManager.getManagementEventFilter());
        }
        
        
    }
    
    public void shutdown() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Shutdown InstrumentationManager ");
        }
        if (instrumentationEnabled) {
            try {
                bus.removeListener((BusEventListener)this);
            } catch (BusException ex) {
                LOG.log(Level.SEVERE, "REMOVE_LISTENER_FAILURE_MSG", ex);
            }
        }
        
        if (jmxManagedComponentManager != null && jmxEnabled) {
            try { 
                bus.removeListener((BusEventListener)jmxManagedComponentManager);               
            } catch (BusException ex) {
                LOG.log(Level.SEVERE, "REMOVE_LISTENER_FAILURE_MSG", ex);
            }
            jmxManagedComponentManager.shutdown();
        }
    }
    
    public void regist(Instrumentation it) {
        if (it == null) {
            // can't find the right instrumentation ,just return
            return;
        } else {
            instrumentations.add(it);        
            //create the instrumentation creation event        
            bus.sendEvent(new InstrumentationCreatedEvent(it));
        }        
    }

    public void unregist(Object component) {
        for (Iterator<Instrumentation> i = instrumentations.iterator(); i.hasNext();) {
            Instrumentation it = i.next();
            if (it.getComponent() == component) {
                i.remove();   
                if (it != null) {
                    //create the instrumentation remove event           
                    bus.sendEvent(new InstrumentationRemovedEvent(it));               
                }
            }
        }
    }

    // get the instance and create the right component
    public void processEvent(BusEvent e) throws BusException {
        Instrumentation it;
        if (e.getID().equals(ComponentCreatedEvent.COMPONENT_CREATED_EVENT)) {            
            it = createInstrumentation(e.getSource());
            regist(it);          
            
        } else if (e.getID().equals(ComponentRemovedEvent.COMPONENT_REMOVED_EVENT)) {           
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Instrumetation do unregister things ");
            }    
            unregist(e.getSource());
        }
    }
    
    
    private Instrumentation createInstrumentation(Object component) {
        Instrumentation it = null; 
        if (BindingManagerImpl.class.isAssignableFrom(component.getClass())) {
            it = new BindingManagerInstrumentation(
                          (BindingManagerImpl)component);
        }
        if (TransportFactoryManagerImpl.class.isAssignableFrom(component.getClass())) {
            it = new TransportFactoryManagerInstrumentation(
                          (TransportFactoryManagerImpl)component);
        }
        if (WSDLManagerImpl.class.isAssignableFrom(component.getClass())) {
            it = new WSDLManagerInstrumentation(
                          (WSDLManagerImpl)component);
        }
        if (WorkQueueManagerImpl.class.isAssignableFrom(component.getClass())) {
            it = new WorkQueueInstrumentation(
                          (WorkQueueManagerImpl)component);            
        }
        if (HTTPClientTransport.class.isAssignableFrom(component.getClass())) {
            it = new HTTPClientTransportInstrumentation(
                          (HTTPClientTransport)component);            
        }
        if (JettyHTTPServerTransport.class.isAssignableFrom(component.getClass())) {
            it = new HTTPServerTransportInstrumentation(
                           (JettyHTTPServerTransport)component);            
        }
        if (JMSServerTransport.class.isAssignableFrom(component.getClass())) {
            it = new JMSServerTransportInstrumentation(
                           (JMSServerTransport)component);
        }        
        if (JMSClientTransport.class.isAssignableFrom(component.getClass())) {
            it = new JMSClientTransportInstrumentation(
                           (JMSClientTransport)component);
        }
        
        return it;
    }

   
    public List<Instrumentation> getAllInstrumentation() {
        // TODO need to add more qurey interface
        return instrumentations;
    }
      

}

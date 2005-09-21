package org.objectweb.celtix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.objectweb.celtix.configuration.CommandLineOption;

/**
 * Manages the <code>Bus</code> instances in a process.
 */
final class BusFactory {

    static final String BUS_CLASS_PROPERTY = "org.objectweb.celtix.BusClass";

    private static final CommandLineOption BUS_CLASS_OPT;
    private static final String DEFAULT_BUS_CLASSNAME = "org.objectweb.celtix.bus.CeltixBus";
    private static BusFactory theInstance;
    
    static {
        BUS_CLASS_OPT = new CommandLineOption("-BUSclass");
    }
    
    private BusFactory() {
    }
    
    static BusFactory getInstance() {
        if (null == theInstance) {
            theInstance = new BusFactory();
        }
        return theInstance;
    }
    
    Bus getBus(String[] args, Map<String, Object> properties, ClassLoader classLoader) throws BusException {
        
        // check command line options and properties to
        // determine bus class 
        
        String busClass = getBusClass(args, properties, classLoader);
        
        // create the bus
       
        Bus bus = createBus(busClass, classLoader, args, properties);
        return bus;    
    }
    
    private static Bus createBus(String className,
            ClassLoader classLoader,
            String[] args,
            Map<String, Object> properties) throws BusException {

        Class<? extends Bus> busClass;
        try {
            if (classLoader == null) {
                busClass = Class.forName(className).asSubclass(Bus.class);
            } else {
                busClass = classLoader.loadClass(className).asSubclass(Bus.class);
            }
        } catch (ClassCastException ex) {
            throw new BusException(ex);
        } catch (ClassNotFoundException ex) {
            throw new BusException(ex);
        }

        try {
            Bus bus = busClass.newInstance();
            bus.initialize(args, properties);
            return bus;
        } catch (IllegalAccessException ex) {
            throw new BusException(ex);
        } catch (InstantiationException ex) {
            throw new BusException(ex);
        }
    }
    
    String getBusClass(String[] args, Map<String, Object> properties, ClassLoader classLoader)
        throws BusException {
        
        String busClass = null;
    
        // first check command line arguments
        BUS_CLASS_OPT.initialize(args);
        busClass = (String)BUS_CLASS_OPT.getValue();
        if (null != busClass && !"".equals(busClass)) {
            return busClass;
        }
        
        // next check properties    
        busClass = (String)properties.get(BUS_CLASS_PROPERTY);
        if (null != busClass && !"".equals(busClass)) {
            return busClass;
        }
        
        // next check system properties
        busClass = System.getProperty(BUS_CLASS_PROPERTY);
        if (null != busClass && !"".equals(busClass)) {
            return busClass;
        }
    
        // next, check for the services stuff in the jar file
        String serviceId = "META-INF/services/" + BUS_CLASS_PROPERTY;
        InputStream is = null;
        
        if (classLoader == null) {
            try {
                classLoader = Thread.currentThread().getContextClassLoader();
            } catch (Exception ex) {
                throw new BusException(ex);
            }
        }
        
        if (classLoader == null) {
            is = ClassLoader.getSystemResourceAsStream(serviceId);
        } else {
            is = classLoader.getResourceAsStream(serviceId);
        }
        if (is != null) {
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                busClass = rd.readLine();
                rd.close();
            } catch (UnsupportedEncodingException useex) {
                //we're asking for UTF-8 which is supposed to always be supported,
                //but we'll throw a BusException anyway
                throw new BusException(useex);
            } catch (IOException useex) {
                throw new BusException(useex);
            }
        }
        if (busClass != null && !"".equals(busClass)) {
            return busClass;
        }

        // otherwise use default  
        busClass = DEFAULT_BUS_CLASSNAME;
        return busClass;
    } 
}

package org.objectweb.celtix.bus.busimpl;

import java.util.Map;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.configuration.CommandLineOption;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationBuilder;
import org.objectweb.celtix.configuration.ConfigurationBuilderFactory;

public class BusConfigurationBuilder  {
    
    public static final String BUS_ID_PROPERTY = "org.objectweb.celtix.BusId";
    private static final CommandLineOption BUS_ID_OPT;    
    private static final String DEFAULT_BUS_ID = "celtix";
    private static final String BUS_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/bus/bus-config";

    static {
        BUS_ID_OPT = new CommandLineOption("-BUSid");
    }
    
    Configuration build(String[] args, Map<String, Object> properties) {
        String id = getBusId(args, properties);
        ConfigurationBuilder builder = ConfigurationBuilderFactory.getBuilder(null);
        Configuration c = builder.getConfiguration(BUS_CONFIGURATION_URI, id);
        if (null == c) {
            c = builder.buildConfiguration(BUS_CONFIGURATION_URI, id);
        }
        return c;  
    }

    private static String getBusId(String[] args, Map<String, Object> properties) {

        String busId = null;

        // first check command line arguments
        BUS_ID_OPT.initialize(args);
        busId = (String)BUS_ID_OPT.getValue();
        if (null != busId && !"".equals(busId)) {
            return busId;
        }

        // next check properties
        busId = (String)properties.get(BUS_ID_PROPERTY);
        if (null != busId && !"".equals(busId)) {
            return busId;
        }

        // next check system properties
        busId = System.getProperty(Bus.BUS_CLASS_PROPERTY);
        if (null != busId && !"".equals(busId)) {
            return busId;
        }

        // otherwise use default  
        return DEFAULT_BUS_ID;
    } 
}

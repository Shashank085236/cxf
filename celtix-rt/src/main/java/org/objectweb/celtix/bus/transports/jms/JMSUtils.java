package org.objectweb.celtix.bus.transports.jms;


import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.transports.jms.JMSAddressPolicyType;
import org.objectweb.celtix.transports.jms.JavaNamingPropertyType;


public final class JMSUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSUtils.class);

    private JMSUtils() {

    }

    public static Context getInitialContext(JMSAddressPolicyType addrType) throws NamingException {
        Properties env = new Properties();
        populateContextEnvironment(addrType, env);

        if (LOG.isLoggable(Level.FINE)) {
            Enumeration props = env.propertyNames();

            while (props.hasMoreElements()) {
                String name = (String) props.nextElement();
                String value = env.getProperty(name);
                LOG.log(Level.FINE, "Context property: " + name + " | " + value);
            }    
        }
        
        Context context = new InitialContext(env);

        return context;
    }


    protected static void populateContextEnvironment(JMSAddressPolicyType addrType, Properties env) {
        
        java.util.ListIterator listIter =  addrType.getJavaNamingProperty().listIterator();

        while (listIter.hasNext()) {
            JavaNamingPropertyType propertyPair = (JavaNamingPropertyType) listIter.next();
            
            if (null != propertyPair.getValue()) {
                env.setProperty(propertyPair.getName(), propertyPair.getValue());
            }
        }
    }
}

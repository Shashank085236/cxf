package org.objectweb.celtix.transports.http;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bus.configuration.security.SSLServerPolicy;
import org.objectweb.celtix.configuration.CompoundName;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationBuilder;
import org.objectweb.celtix.transports.http.configuration.HTTPListenerPolicy;

/**
 * Encapsulates aspects of HTTP Destination configuration related
 * to listening (this is separated from the main destination config
 * as a servlet-based destination does not require an explicit listener).
 */
public class HTTPListenerConfiguration {
    private static final String HTTP_LISTENER_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/configuration/transport/http-listener";
    
    private Configuration config;
    private HTTPListenerPolicy policy;
    private SSLServerPolicy sslPolicy;
    
    public HTTPListenerConfiguration(Bus bus, String protocol, int port) {
        config = createConfiguration(bus, port);
        policy = config.getObject(HTTPListenerPolicy.class, "httpListener");
        sslPolicy = config.getObject(SSLServerPolicy.class, "sslServer");
        if (sslPolicy == null && "https".equals(protocol)) {
            sslPolicy = new SSLServerPolicy();
        }
    }
    
    HTTPListenerPolicy getPolicy() {
        return policy;
    }
    
    SSLServerPolicy getSSLPolicy() {
        return sslPolicy;
    }
    
    private Configuration createConfiguration(Bus bus, int p) {

        // REVISIT

        CompoundName id = new CompoundName(bus.getId(), "http-listener." + p);
        
        ConfigurationBuilder cb = bus.getExtension(ConfigurationBuilder.class);
        return cb.getConfiguration(HTTP_LISTENER_CONFIGURATION_URI, id);
    }
}

package org.objectweb.celtix.systest.routing.passthrough;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.objectweb.celtix.systest.common.ClientServerSetupBase;
import org.objectweb.celtix.systest.common.ClientServerTestBase;
import org.objectweb.celtix.systest.jms.EmbeddedJMSBrokerLauncher;
import org.objectweb.celtix.systest.routing.RouterServer;

public class PassThroughRouterTest extends ClientServerTestBase {
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SOAPHTTPToSOAPHTTPRouter.class);
        suite.addTestSuite(SOAPJMSToSOAPHTTPRouter.class);
        return new ClientServerSetupBase(suite) {
            public void startServers() throws Exception {
                Map<String, String> props = new HashMap<String, String>();                
                if (System.getProperty("activemq.store.dir") != null) {
                    props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
                }
                props.put("java.util.logging.config.file", 
                          System.getProperty("java.util.logging.config.file"));
                
                assertTrue("server did not launch correctly", launchServer(EmbeddedJMSBrokerLauncher.class,
                                                                           props, null));
                
                assertTrue("server did not launch correctly",
                           launchServer(Server.class, false));

                assertTrue("server did not launch correctly",
                           launchServer(RouterServer.class,
                                        null,
                                        new String[]{"org.objectweb.celtix.BusId", "celtix-st"}));
            }
        };
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PassThroughRouterTest.class);
    }

}

package org.apache.cxf.management.jmx;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import org.apache.cxf.common.logging.LogUtils;


public final class JMXUtils {
    
    public static final String DOMAIN_STRING = "org.apache.cxf.instrumentation";
    
    private static final Logger LOG = LogUtils.getL7dLogger(JMXUtils.class);
    private JMXUtils() {        
    }
         
    /**
     * Bus :
           org.apache.cxf.instrumentation:type=Bus,name=cxf
       
       WorkQueue :
           org.apache.cxf.instrumentation:type=Bus.WorkQueue,Bus=cxf,name=WorkQueue
        
       WSDLManager :
           org.apache.cxf.instrumentation:type=Bus.WSDLManager,Bus=cxf,name=WSDLManager       
           
         
       Endpoint :
           org.apache.cxf.instrumentation:type=Bus.Endpoint,Bus=cxf,
           Bus.Service={http://apache.org/hello_world}SOAPService",Bus.Port=SoapPort, 
           name=Endpoint
        
       HTTPServerTransport:
           org.apache.cxf.instrumentation:type=Bus.Service.Port.HTTPServerTransport,
           Bus=cxf,Bus.Service={http://apache.org/hello_world}SOAPService",Bus.Port=SoapPort,
           name=HTTPServerTransport"
       
       JMSServerTransport:
           org.apache.cxf.instrumentation:type=Bus.Service.Port.JMSServerTransport,
           Bus=cxf,Bus.Service={http://apache.org/hello_world}SOAPService",Bus.Port=SoapPort,
           name=JMSServerTransport" 
       ...
           
     */
 
    public static ObjectName getObjectName(String type, String name) {        
        String objectName = ":type=" + type + ",name=" + name;
        
        try {
            return new ObjectName(DOMAIN_STRING + objectName);
        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "OBJECT_NAME_FALUE_MSG", ex, name);
        }
        return null;
    }

   

}

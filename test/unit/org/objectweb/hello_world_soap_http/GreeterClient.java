package org.objectweb.hello_world_soap_http;

import java.net.URL;

import javax.xml.namespace.QName;

import org.objectweb.celtix.Bus;

public class GreeterClient {
    
    protected GreeterClient() {        
    }
    
    public static void main(String args[]) throws Exception {
        
        String operationName = "sayHi";
        if (args.length > 0) {
            operationName = args[0];
        }
        String[] params = null;
        if (args.length > 1) {
            params = new String[args.length - 1];
            System.arraycopy(args, 1, params, 0, params.length);
        }
        
        System.out.println("Invoking operation: " + operationName);
        System.out.print("Parameters:"); 
        for (String p : params) {
            System.out.print(" " + p);            
        }
        System.out.println();
        
        Bus bus = Bus.init();
        
        URL url = GreeterClient.class.getResource("resources/hello_world.wsdl");
        assert null != url;
        
        QName serviceName = new QName("http://objectweb.org/hello_world_soap_http", "SOAPService");
        SOAPService ss = new SOAPService(url, serviceName);
        Greeter port = ss.getSoapPort();
        
        if ("sayHi".equals(operationName)) {
            System.out.println("Invoking sayHi...");
            System.out.println("server responded with: " + port.sayHi());
        } else if ("greetMe".equals(operationName) && params != null && params.length > 0) {
            System.out.println("Invoking greetMe...");
            System.out.println("server responded with: " + port.greetMe(params[0]));
        } else {
            System.err.println("No such operation");
        }
        
        
        bus.shutdown(true);
    }

}

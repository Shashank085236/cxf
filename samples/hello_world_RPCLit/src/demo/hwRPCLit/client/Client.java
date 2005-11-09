package demo.hwRPCLit.client;

import java.io.File;
import javax.xml.namespace.QName;
import org.objectweb.celtix.Bus;
import org.objectweb.hello_world_rpclit.GreeterRPCLit;
import org.objectweb.hello_world_rpclit.SOAPServiceRPCLit;
import org.objectweb.hello_world_rpclit.types.MyComplexStruct;

public final class Client {

    private static final QName SERVICE_NAME = 
        new QName("http://objectweb.org/hello_world_rpclit", "SOAPServiceRPCLit");
    private static final QName PORT_NAME = 
        new QName("http://objectweb.org/hello_world_rpclit", "SoapPortRPCLit");

    private Client() {
    } 

    public static void main(String[] args) throws Exception {

        if (args.length == 0) { 
            System.out.println("please specify wsdl");
            System.exit(1); 
        }

        File wsdl = new File(args[0]);

        Bus bus = Bus.init();

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl.toURL(), SERVICE_NAME);
        GreeterRPCLit greeter = (GreeterRPCLit)service.getPort(PORT_NAME, GreeterRPCLit.class);

        System.out.println("Invoking sayHi...");
        System.out.println("server responded with: " + greeter.sayHi());
        System.out.println(); 

        System.out.println("Invoking greetMe...");
        System.out.println("server responded with: " + greeter.greetMe(System.getProperty("user.name")));
        System.out.println();
        
        MyComplexStruct argument = new MyComplexStruct();
        MyComplexStruct retVal = null;

        String str1 = "this is element 1"; 
        String str2 = "this is element 2"; 
        int int1 = 42; 

        argument.setElem1(str1);
        argument.setElem2(str2);
        argument.setElem3(int1);
        System.out.println("Invoking sendReceiveData...");

        retVal = greeter.sendReceiveData(argument);

        System.out.println("Response from sendReceiveData operation :");
        System.out.println("Element-1 : " + retVal.getElem1());
        System.out.println("Element-2 : " + retVal.getElem2());
        System.out.println("Element-3 : " + retVal.getElem3());
        System.out.println();

        if (bus != null) {
            bus.shutdown(true);
        }
    }
}

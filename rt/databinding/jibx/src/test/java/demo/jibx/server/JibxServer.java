package demo.jibx.server;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.xcf.jibx.JiBXDataBinding;

public class JibxServer {
	protected JibxServer() throws Exception {
        TestServiceImpl helloworldImpl = new TestServiceImpl();
        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(TestService.class);
        svrFactory.setAddress("http://localhost:8080/Hello");
        svrFactory.setServiceBean(helloworldImpl);
        svrFactory.getServiceFactory().setDataBinding(new JiBXDataBinding());
        svrFactory.create();
    }

    public static void main(String args[]) throws Exception {
        new JibxServer();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }

}

package demo.jibx.client;

import java.util.Calendar;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.xcf.jibx.JiBXDataBinding;

import demo.jibx.server.TestService;

public class JibxClient {

	private JibxClient() {
	}

	public static void main(String args[]) throws Exception {
		ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
		factory.setServiceClass(TestService.class);
		if (args != null && args.length > 0 && !"".equals(args[0])) {
			factory.setAddress(args[0]);
		} else {
			factory.setAddress("http://localhost:8080/Hello");
		}
		factory.getServiceFactory().setDataBinding(new JiBXDataBinding());
		TestService client = (TestService) factory.create();

		System.out.println("Invoke testString() ..");
		System.out.println(client.testString("Alice"));
		System.out.println("Invoke testDouble() ..");
		System.out.println(client.testDouble(1.0));
		System.out.println("Invoke testDate() ..");
		System.out.println(client.testDate(Calendar.getInstance().getTime()));
	}
}

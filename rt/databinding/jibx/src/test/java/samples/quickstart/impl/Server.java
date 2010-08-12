package samples.quickstart.impl;

import javax.xml.namespace.QName;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jibx.JiBXDataBinding;
import samples.quickstart.StockQuoteServicePortType;

public class Server {
    public static void main(String[] args) {
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceClass(StockQuoteServicePortType.class);
        factory.setAddress("http://localhost:8080/StockQuoteService");
        factory.setServiceBean(new StockQuoteServicePortTypeImpl());
        // factory.setSchemaLocations(Arrays.asList("/home/nilupa/Documents/experiment/StockQuoteService.xsd"));
        factory.setDataBinding(new JiBXDataBinding());
        factory.setServiceName(new QName("http://quickstart.samples/", "StockQuoteService"));

        factory.setWsdlURL("file:/home/nilupa/Documents/experiment/StockQuoteService.wsdl");
        
        factory.create();
    }
}

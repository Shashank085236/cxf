package org.apache.cxf.jibx.complex_types;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.xcf.jibx.JiBXDataBinding;

public class JiBXLibraryServer {
    protected JiBXLibraryServer() throws Exception {
        LibraryServiceImpl libraryServiceImpl = new LibraryServiceImpl();
        ServerFactoryBean svrFactory = new ServerFactoryBean();
        ArrayList<String> schemaLocations = new ArrayList<String>();
        schemaLocations.add("file:./src/test/java/org/apache/cxf/jibx/complex_types/types.xsd");
        svrFactory.setSchemaLocations(schemaLocations);
        svrFactory.setServiceClass(LibraryService.class);
        svrFactory.setAddress("http://localhost:8080/LibraryService");
        svrFactory.setServiceBean(libraryServiceImpl);
        svrFactory.getServiceFactory().setDataBinding(new JiBXDataBinding());
        svrFactory.create();
    }

    public static void main(String args[]) throws Exception {
        new JiBXLibraryServer();
        System.out.println("Server ready...");
        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }

}

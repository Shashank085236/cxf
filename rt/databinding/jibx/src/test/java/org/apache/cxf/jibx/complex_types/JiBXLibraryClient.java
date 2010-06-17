package org.apache.cxf.jibx.complex_types;

import com.sosnoski.ws.library.types.BookInformation;

import java.util.ArrayList;
import java.util.Calendar;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.xcf.jibx.JiBXDataBinding;
import org.apache.xcf.jibx.simple_types.SimpleTypesService;

public final class JiBXLibraryClient {
    private JiBXLibraryClient() {
    }

    public static void main(final String[] args) throws Exception {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        ArrayList<String> schemaLocations = new ArrayList<String>();
        if (args != null && args.length > 0 && !"".equals(args[0])) {
            factory.setAddress(args[0]);
        } else {
            factory.setAddress("http://localhost:9090/LibraryService");
        }
        factory.setServiceClass(LibraryService.class);
        factory.getServiceFactory().setDataBinding(new JiBXDataBinding());
        LibraryService proxy = (LibraryService)factory.create();
        BookInformation info = getBookInfo();
        System.out.println("Invoke testString() ..");
        boolean result = proxy.addBook(info);
        System.out.println(result);
    }

    private static BookInformation getBookInfo() {
        BookInformation info = new BookInformation();
        info.setIsbn("isbn");
        ArrayList<String> authors = new ArrayList<String>();
        authors.add("authors1");
        authors.add("authors2");
        info.setAuthors(authors);
        info.setTitle("title");
        info.setTitle("type");
        return info;
    }
}

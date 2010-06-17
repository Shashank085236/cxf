package org.apache.cxf.jibx.complex_types;

import com.sosnoski.ws.library.types.BookInformation;

//@DataBinding(org.apache.xcf.jibx.JiBXDataBinding.class) 
public interface LibraryService {
    public boolean addBook(BookInformation info);
}

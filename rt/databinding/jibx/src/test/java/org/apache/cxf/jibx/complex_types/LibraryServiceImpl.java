package org.apache.cxf.jibx.complex_types;

import com.sosnoski.ws.library.types.BookInformation;

public class LibraryServiceImpl implements LibraryService {

    /** {@inheritDoc}*/
    public boolean addBook(BookInformation info) {
        System.out.println(info.getIsbn() + info.getTitle() + info.getType());
        return true;
    }

}
    
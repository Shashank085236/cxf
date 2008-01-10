/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.cxf.systest.jaxrs;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.UriParam;
import javax.ws.rs.UriTemplate;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.customer.book.BookNotFoundDetails;
import org.apache.cxf.customer.book.BookNotFoundFault;
import org.apache.cxf.helpers.XMLUtils;

@UriTemplate("/bookstore/")
public class BookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;

    public BookStore() {
        init();
        System.out.println("----books: " + books.size());
    }

    @HttpMethod("GET")
    @UriTemplate("/books/{bookId}/")
    public Book getBook(@UriParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @UriTemplate("/booksubresource/{bookId}/")
    public Book getBookSubResource(@UriParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBookSubResource with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @HttpMethod("GET")
    @UriTemplate("/booknames/{bookId}/")
    @ProduceMime("text/*")
    public String getBookName(@UriParam("bookId") int id) throws BookNotFoundFault {
        System.out.println("----invoking getBookName with id: " + id);
        Book book = books.get(new Long(id));
        if (book != null) {
            return book.getName();
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(id);
            throw new BookNotFoundFault(details);
        }
    }

    @HttpMethod("POST")
    @UriTemplate("/books")
    @ProduceMime("text/xml")
    public Response addBook(Book book) {
        System.out.println("----invoking addBook, book name is: " + book.getName());
        book.setId(++bookId);
        books.put(book.getId(), book);

        return Response.Builder.ok(book).build();
    }

    @HttpMethod("PUT")
    @UriTemplate("/books/")
    public Response updateBook(Book book) {
        System.out.println("----invoking updateBook, book name is: " + book.getName());
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.Builder.ok().build();
        } else {
            r = Response.Builder.notModified().build();
        }

        return r;
    }
    
    @HttpMethod("PUT")
    @UriTemplate("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        System.out.println("----invoking updateBook with DOMSource");
        XMLUtils.printDOM(ds.getNode());
        return ds;
    }
    
    @HttpMethod("PUT")
    @UriTemplate("/bookswithjson/")
    public Response updateBookJSON(Book book) {
        System.out.println("----invoking updateBook, book name is: " + book.getName());
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.Builder.ok().build();
        } else {
            r = Response.Builder.notModified().build();
        }

        return r;
    }

    @HttpMethod("DELETE")
    @UriTemplate("/books/{bookId}/")
    public Response deleteBook(@UriParam("bookId") String id) {
        System.out.println("----invoking deleteBook with bookId: " + id);
        Book b = books.get(Long.parseLong(id));

        Response r;
        if (b != null) {
            r = Response.Builder.ok().build();
        } else {
            r = Response.Builder.notModified().build();
        }

        return r;
    }

    @HttpMethod("GET")
    @UriTemplate("/cd/{CDId}/")
    public CD getCD(@UriParam("CDId") String id) {
        System.out.println("----invoking getCD with cdId: " + id);
        CD cd = cds.get(Long.parseLong(id));

        return cd;
    }

    @HttpMethod("GET")
    @UriTemplate("/cdwithmultitypes/{CDId}/")
    @ProduceMime({"application/xml", "application/json" }) 
    public CD getCDWithMultiContentTypes(@UriParam("CDId") String id) {
        System.out.println("----invoking getCDWithMultiContentTypes with cdId: " + id);
        CD cd = cds.get(Long.parseLong(id));

        return cd;
    }
    
    @HttpMethod("GET")
    @UriTemplate("/cds/")
    public CDs getCDs() {
        System.out.println("----invoking getCDs");
        CDs c = new CDs();
        c.setCD(cds.values());
        return c;
    }

    final void init() {
        Book book = new Book();
        book.setId(bookId);
        book.setName("CXF in Action");
        books.put(book.getId(), book);

        CD cd = new CD();
        cd.setId(cdId);
        cd.setName("BOHEMIAN RHAPSODY");
        cds.put(cd.getId(), cd);
        CD cd1 = new CD();
        cd1.setId(++cdId);
        cd1.setName("BICYCLE RACE");
        cds.put(cd1.getId(), cd1);
    }
}



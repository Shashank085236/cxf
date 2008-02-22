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


import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.UriParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.customer.book.BookNotFoundDetails;
import org.apache.cxf.customer.book.BookNotFoundFault;

@Path("/bookstore/")
public class AtomBookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;
    
    public AtomBookStore() {
        init();
        System.out.println("----books: " + books.size());
    }
    
    @GET
    @Path("/books/feed")
    @ProduceMime("application/atom+xml")
    public Feed getBooksAsFeed() {
        Factory factory = Abdera.getNewFactory();
        Feed f = factory.newFeed();
        f.setTitle("Collection of Books");
        f.setId("http://www.books.com");
        f.addAuthor("BookStore Management Company");
        try {
            for (Book b : books.values()) {
                
                Entry e = AtomUtils.createBookEntry(b);
                
                f.addEntry(e);
            }
        } catch (Exception ex) {
            // ignore
        }
        return f;
    }
    
    @POST
    @Path("/books/feed")
    @ConsumeMime("application/atom+xml")
    public Response addBookAsEntry(Entry e) {
        try {
            String text = e.getContentElement().getValue();
            StringReader reader = new StringReader(text);
            JAXBContext jc = JAXBContext.newInstance(Book.class);
            Book b = (Book)jc.createUnmarshaller().unmarshal(reader);
            books.put(b.getId(), b);
            
            // this code is broken as Response does not
            URI uri = new URI("http://localhost:9080/bookstore/books/entries/" + b.getId());
            return Response.created(uri).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }
    
    @GET
    @Path("/books/entries/{bookId}/")
    @ProduceMime("application/atom+xml")
    public Entry getBookAsEntry(@UriParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            try {
                return AtomUtils.createBookEntry(book);
            } catch (Exception ex) {
                // ignore
            }
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
        return null;
    }
    
    @Path("/books/subresources/{bookId}/")
    public AtomBook getBook(@UriParam("bookId") String id) throws BookNotFoundFault {
        System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            try {
                return new AtomBook(book);
            } catch (Exception ex) {
                // ignore
            }
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
        return null;
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



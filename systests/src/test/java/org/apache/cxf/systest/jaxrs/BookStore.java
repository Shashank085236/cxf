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


import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.helpers.XMLUtils;

@Path("/bookstore")
public class BookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private Map<Long, CD> cds = new HashMap<Long, CD>();
    private long bookId = 123;
    private long cdId = 123;
    
    private String currentBookId;
    @PathParam("CDId")
    private String currentCdId;
    @Context
    private HttpHeaders httpHeaders;
    
    public BookStore() {
        init();
    }
    
    @GET
    @Path("webappexception")
    public Book throwException() {
        
        Response response = Response.serverError().entity("This is a WebApplicationException").build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("books/check/{id}")
    @Produces("text/plain")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }
    
    
    @GET
    @Path("timetable")
    public Calendar getTimetable() {
        return new GregorianCalendar();
    }
    
    @GET
    @Path("wrongparametertype")
    public void wrongParameterType(@QueryParam("p") Map p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @GET
    @Path("exceptionduringconstruction")
    public void wrongParameterType(@QueryParam("p") BadBook p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @POST
    @Path("/unsupportedcontenttype")
    @Consumes("application/xml")
    public String unsupportedContentType() {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @GET
    @Path("/bookurl/{URL}/")
    public Book getBookByURL(@PathParam("URL") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    }
    
    @GET
    @Path("/segment/{pathsegment}/")
    public Book getBookBySegment(@PathParam("pathsegment") PathSegment segment) throws Exception {
        if (!"matrix2".equals(segment.getPath())) {
            throw new RuntimeException();
        }
        MultivaluedMap<String, String> map = segment.getMatrixParameters();
        String s1 = map.getFirst("first").toString();
        String s2 = map.getFirst("second").toString();
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/segment/list/{pathsegment:.+}/")
    public Book getBookBySegment(@PathParam("pathsegment") List<PathSegment> list) 
        throws Exception {
        return doGetBook(list.get(0).getPath()
                         + list.get(1).getPath()
                         + list.get(2).getPath());
    }
    
    @GET
    @Path("/segment/matrix")
    public Book getBookByMatrixParams(@MatrixParam("first") String s1,
                                      @MatrixParam("second") String s2) throws Exception {
        
        return doGetBook(s1 + s2);
    }
    
    @GET
    @Path("/bookheaders/")
    public Book getBookByHeader(@HeaderParam("BOOK") List<String> ids) throws Exception {
        List<MediaType> types = httpHeaders.getAcceptableMediaTypes();
        if (types.size() != 2 
            || !"text/xml".equals(types.get(0).toString())
            || !MediaType.APPLICATION_XML_TYPE.isCompatible(types.get(1))) {
            throw new WebApplicationException();
        }
        List<Locale> locales = httpHeaders.getAcceptableLanguages();
        if (locales.size() != 2 
            || !"en".equals(locales.get(0).getLanguage())
            || !"da".equals(locales.get(1).getLanguage())) {
            throw new WebApplicationException();
        }
        
        return doGetBook(ids.get(0) + ids.get(1) + ids.get(2));
    }
    
    @GET
    @Path("/bookquery")
    public Book getBookByURLQuery(@QueryParam("urlid") String urlValue) throws Exception {
        String url2 = new URL(urlValue).toString();
        int index = url2.lastIndexOf('/');
        return doGetBook(url2.substring(index + 1));
    } 

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("books/custom/{bookId:\\d\\d\\d}")
    public Book getBookCustom(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/query")
    public Book getBookQuery(@QueryParam("bookId") long id) throws BookNotFoundFault {
        return doGetBook(Long.toString(id));
    }
    
    @GET
    @Path("/books/defaultquery")
    public Book getDefaultBookQuery(@DefaultValue("123") @QueryParam("bookId") String id) 
        throws BookNotFoundFault {
        return doGetBook(id);
    }
    
    @GET
    @Path("/books/missingquery")
    public Book getBookMissingQuery(@QueryParam("bookId") long id) 
        throws BookNotFoundFault {
        if (id != 0) {
            throw new RuntimeException();
        }
        return doGetBook("123");
    }
    
    @GET
    @Path("/books/element")
    public JAXBElement<Book> getBookElement() throws Exception {
        return new JAXBElement<Book>(new QName("", "Book"),
                                     Book.class,
                                     doGetBook("123"));
    }
    
    @GET
    @Path("/books/adapter")
    @XmlJavaTypeAdapter(BookInfoAdapter.class)
    public BookInfo getBookAdapter() throws Exception {
        return new BookInfo(doGetBook("123"));
    }
    
    @PathParam("bookId")
    public void setBookId(String id) {
        currentBookId = id;
    }
    
    @GET
    @Path("/books/{bookId}/")
    @Produces("application/json;q=0.9")
    public Book getBookAsJSON() throws BookNotFoundFault {
        return doGetBook(currentBookId);
    }
    
    private Book doGetBook(String id) throws BookNotFoundFault {
        //System.out.println("----invoking getBook with id: " + id);
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @Path("/booksubresource/{bookId}/")
    public Book getBookSubResource(@PathParam("bookId") String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(Long.parseLong(id));
            throw new BookNotFoundFault(details);
        }
    }
    
    @Path("/booksubresourceobject/{bookId}/")
    public Object getBookSubResourceObject(@PathParam("bookId") String id) throws BookNotFoundFault {
        return getBookSubResource(id);
    }
    
    @GET
    @Path("/booknames/{bookId}/")
    @Produces("text/*")
    public String getBookName(@PathParam("bookId") int id) throws BookNotFoundFault {
        Book book = books.get(new Long(id));
        if (book != null) {
            return book.getName();
        } else {
            BookNotFoundDetails details = new BookNotFoundDetails();
            details.setId(id);
            throw new BookNotFoundFault(details);
        }
    }

    @POST
    @Path("/books")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(Book book) {
        book.setId(++bookId);
        books.put(book.getId(), book);

        return Response.ok(book).build();
    }

    @POST
    @Path("/binarybooks")
    @Produces("text/xml")
    @Consumes("application/octet-stream")
    public Response addBinaryBook(long[] book) {
        return Response.ok(book).build();
    }
    
    @PUT
    @Path("/books/")
    public Response updateBook(Book book) {
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @PUT
    @Path("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        XMLUtils.printDOM(ds.getNode());
        return ds;
    }
    
    @PUT
    @Path("/bookswithjson/")
    @Consumes("application/json")
    public Response updateBookJSON(Book book) {
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            books.put(book.getId(), book);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @DELETE
    @Path("/books/{bookId}/")
    public Response deleteBook(@PathParam("bookId") String id) {
        Book b = books.get(Long.parseLong(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @DELETE
    @Path("/books/id")
    public Response deleteWithQuery(@QueryParam("value") @DefaultValue("-1") int id) {
        if (id != 123) {
            throw new WebApplicationException();
        }
        Book b = books.get(new Long(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @POST
    @Path("/booksplain")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Long echoBookId(long theBookId) {
        return new Long(theBookId);
    }
    
    @GET
    @Path("/cd/{CDId}/")
    public CD getCD() {
        CD cd = cds.get(Long.parseLong(currentCdId));

        return cd;
    }

    @GET
    @Path("/cdwithmultitypes/{CDId}/")
    @Produces({"application/xml", "application/json" }) 
    public CD getCDWithMultiContentTypes(@PathParam("CDId") String id) {
        CD cd = cds.get(Long.parseLong(id));

        return cd;
    }
    
    @GET
    @Path("/cds/")
    public CDs getCDs() {
        CDs c = new CDs();
        c.setCD(cds.values());
        return c;
    }
    
    @Path("/interface")
    public BookSubresource getBookFromSubresource() {
        return new BookSubresourceImpl();
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
    
    private static class BookInfo {
        private String name;
        private long id;
        
        public BookInfo(Book b) {
            this.name = b.getName();
            this.id = b.getId();
        }
        
        public String getName() {
            return name;
        }
        
        public long getId() {
            return id;
        }
    }
    
    public static class BookInfoAdapter extends XmlAdapter<Book, BookInfo> {

        @Override
        public Book marshal(BookInfo v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo unmarshal(Book v) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    private static class BadBook {
        public BadBook(String s) {
            throw new RuntimeException("The bad book");
        }
    }
}



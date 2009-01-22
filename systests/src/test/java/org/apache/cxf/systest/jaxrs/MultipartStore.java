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


import java.io.InputStream;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MultipartID;
import org.apache.cxf.jaxrs.utils.AttachmentUtils;

@Path("/bookstore")
public class MultipartStore {

    @Context
    private MessageContext context;
    
    public MultipartStore() {
    }
    
    @POST
    @Path("/books/stream")
    @ProduceMime("text/xml")
    public Response addBookFromStream(StreamSource source) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(source);
        b.setId(124);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/istream")
    @ProduceMime("text/xml")
    public Response addBookFromInputStream(InputStream is) throws Exception {
        return readBookFromInputStream(is);
    }
    
    @POST
    @Path("/books/dsource")
    @ProduceMime("text/xml")
    public Response addBookFromDataSource(DataSource ds) throws Exception {
        return readBookFromInputStream(ds.getInputStream());
    }
    
    @POST
    @Path("/books/jaxb2")
    @ProduceMime("text/xml")
    public Response addBook2(@MultipartID("rootPart") Book b1,
                             @MultipartID("book2") Book b2) 
        throws Exception {
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b1.setId(124);
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/dsource2")
    @ProduceMime("text/xml")
    public Response addBookFromDataSource2(@MultipartID("rootPart") DataSource ds1,
                                           @MultipartID("book2") DataSource ds2) 
        throws Exception {
        Response r1 = readBookFromInputStream(ds1.getInputStream());
        Response r2 = readBookFromInputStream(ds2.getInputStream());
        Book b1 = (Book)r1.getEntity();
        Book b2 = (Book)r2.getEntity();
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        return r1;
    }
    
    @POST
    @Path("/books/dhandler")
    @ProduceMime("text/xml")
    public Response addBookFromDataHandler(DataHandler dh) throws Exception {
        return readBookFromInputStream(dh.getInputStream());
    }
    
    @POST
    @Path("/books/mchandlers")
    @ProduceMime("text/xml")
    public Response addBookFromMessageContext() throws Exception {
        Map<String, DataHandler> handlers = AttachmentUtils.getAttachments(context);
        for (Map.Entry<String, DataHandler> entry : handlers.entrySet()) {
            if (entry.getKey().equals("book2")) {
                return readBookFromInputStream(entry.getValue().getInputStream());
            }
        }
        throw new WebApplicationException(500);
    }
    
    @POST
    @Path("/books/jaxb")
    @ProduceMime("text/xml")
    public Response addBook(Book b) throws Exception {
        b.setId(124);
        return Response.ok(b).build();
    }
    
    private Response readBookFromInputStream(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(is);
        b.setId(124);
        return Response.ok(b).build();
    }
    
}



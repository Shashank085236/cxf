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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.jaxrs.ext.MessageContext;

public class BookStoreSoapRestImpl implements BookStoreJaxrsJaxws {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    
    @Resource
    private WebServiceContext jaxwsContext;
    @Resource
    private MessageContext jaxrsContext;
    
    public BookStoreSoapRestImpl() {
        init();
    }
    
    public Book getBook(Long id) {
        System.out.println(getContentType());
        return books.get(id);
    }
    
    public Book addBook(Book book) {
        book.setId(124);
        books.put(book.getId(), book);
        return books.get(book.getId());
    }
    
    private void init() {
        Book book = new Book();
        book.setId(new Long(123));
        book.setName("CXF in Action");
        books.put(book.getId(), book);
    }
 
    private String getContentType() {
        
        // TODO : it may be worth indeed to introduce a shared ServiceContext
        // such that users combining JAXWS and JAXRS won't have to write if/else code 
        HttpServletRequest request = jaxrsContext.getHttpServletRequest();
        if (request == null) {
            request = (HttpServletRequest)jaxwsContext.getMessageContext().get(
                 javax.xml.ws.handler.MessageContext.SERVLET_REQUEST);
        }
        return request.getContentType();
    }
    
}

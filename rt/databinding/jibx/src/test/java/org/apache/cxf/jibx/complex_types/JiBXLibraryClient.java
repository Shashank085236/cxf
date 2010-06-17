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

package org.apache.cxf.jibx.complex_types;

import java.util.ArrayList;
import java.util.List;

import com.sosnoski.ws.library.types.BookInformation;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.xcf.jibx.JiBXDataBinding;

public final class JiBXLibraryClient {
    private JiBXLibraryClient() {
    }

    public static void main(final String[] args) throws Exception {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
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
        List<String> authors = new ArrayList<String>();
        authors.add("authors1");
        authors.add("authors2");
        info.setAuthors(authors);
        info.setTitle("title");
        info.setTitle("type");
        return info;
    }
}

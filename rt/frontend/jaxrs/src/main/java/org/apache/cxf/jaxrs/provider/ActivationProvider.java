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

package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.jaxrs.utils.multipart.MultipartInfo;

@Provider
@Consumes("multipart/related")
public class ActivationProvider implements MessageBodyReader<Object> {

    @Context
    private MessageContext mc;
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                              MediaType mt) {
        if (DataHandler.class.isAssignableFrom(type) || DataSource.class.isAssignableFrom(type)
            || (mt.getType().equals("multipart") && mt.getSubtype().equals("related"))) {
            return true;
        }
        return false;
    }

    public Object readFrom(Class<Object> c, Type t, Annotation[] anns, MediaType mt, 
                           MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException, WebApplicationException {
        MultipartInfo multipart = AttachmentUtils.getMultipart(c, anns, mt, mc, is);
        if (multipart != null) {
            if (InputStream.class.isAssignableFrom(multipart.getPart().getClass())) {
                MessageBodyReader<Object> r = 
                    mc.getProviders().getMessageBodyReader(c, t, anns, multipart.getType());
                if (r != null) {
                    return r.readFrom(c, t, anns, multipart.getType(), headers, 
                                           (InputStream)multipart.getPart());
                }
            } else {
                // it's either DataSource or DataHandler
                return multipart.getPart();
            }
        }
        throw new WebApplicationException(404);
    }
    
}

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
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.helpers.IOUtils;

@Provider
public final class StringProvider 
    implements MessageBodyWriter<String>, MessageBodyReader<String>  {

    public boolean isWriteable(Class<?> type) {
        return type == String.class;
    }
    
    public boolean isReadable(Class<?> type) {
        return type == String.class;
    }
    
    public long getSize(String s) {
        return s.length();
    }

    public String readFrom(Class<String> type, MediaType m, MultivaluedMap<String, String> headers,
                           InputStream is) {
        try {
            return IOUtils.toString(is);
        } catch (IOException e) {
            // TODO: better exception handling
        }
        return null;
    }

    public void writeTo(String obj, MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
        try {
            os.write(obj.getBytes());
        } catch (IOException e) {
            //TODO: better exception handling
        }
    }

}

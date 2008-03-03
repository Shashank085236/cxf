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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.MetadataMap;

@ConsumeMime("application/x-www-form-urlencoded")
public final class FormEncodingReaderProvider implements MessageBodyReader<Object> {

    public boolean isReadable(Class<?> type) {
        return type.isAssignableFrom(MultivaluedMap.class);
    }

    public MultivaluedMap<String, String> readFrom(Class<Object> type, MediaType m,
                                                   MultivaluedMap<String, String> headers, InputStream is) {
        try {

            String charset = "UTF-8";

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos, 1024);
            String postBody = new String(bos.toByteArray(), charset);

            MultivaluedMap<String, String> params = getParams(postBody);

            return params;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void copy(final InputStream input, final OutputStream output, final int bufferSize)
        throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        n = input.read(buffer);
        while (-1 != n) {
            output.write(buffer, 0, n);
            n = input.read(buffer);
        }
    }

    /**
     * Retrieve map of parameters from the passed in message
     * 
     * @param message
     * @return a Map of parameters.
     */
    protected static MultivaluedMap<String, String> getParams(String body) {
        MultivaluedMap<String, String> params = new MetadataMap<String, String>();
        if (!StringUtils.isEmpty(body)) {
            List<String> parts = Arrays.asList(body.split("&"));
            for (String part : parts) {
                String[] keyValue = part.split("=");
                params.add(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }
}

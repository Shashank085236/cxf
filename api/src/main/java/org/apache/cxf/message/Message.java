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

package org.apache.cxf.message;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;

public interface Message extends Map<String, Object> {
    
    String TRANSPORT = "org.apache.cxf.transport";    
    String REQUESTOR_ROLE = "org.apache.cxf.client";

    String INBOUND_MESSAGE = "org.apache.cxf.message.inbound";
    String INVOCATION_OBJECTS = "org.apache.cxf.invocation.objects";
    
    String MIME_HEADERS = "org.apache.cxf.mime.headers";
    
    String USERNAME = Message.class.getName() + ".USERNAME";
    String PASSWORD = Message.class.getName() + ".PASSWORD";
    String PROTOCOL_HEADERS = Message.class.getName() + ".PROTOCOL_HEADERS";
    String RESPONSE_CODE = Message.class.getName() + ".RESPONSE_CODE";
    String ENDPOINT_ADDRESS = Message.class.getName() + ".ENDPOINT_ADDRESS";
    String HTTP_REQUEST_METHOD = Message.class.getName() + ".HTTP_REQUEST_METHOD";
    String PATH_INFO = Message.class.getName() + ".PATH_INFO";
    String QUERY_STRING = Message.class.getName() + ".QUERY_STRING";
    String MTOM_ENABLED = Message.class.getName() + ".isMtomEnabled";
    String SCHEMA_VALIDATION_ENABLED = Message.class.getCanonicalName() + ".schemaValidationEnabled";
    String CONTENT_TYPE = Message.class.getName() + ".ContentType";
    String BASE_PATH = Message.class.getName() + ".BASE_PATH";
    String ENCODING = Message.class.getName() + ".ENCODING";
    String FIXED_PARAMETER_ORDER = Message.class.getName() + "FIXED_PARAMETER_ORDER";

    String getId();
    void setId(String id);
    
    InterceptorChain getInterceptorChain();
    void setInterceptorChain(InterceptorChain chain);
    
    /**
     * @return the associated Conduit if message is outbound, null otherwise
     */
    Conduit getConduit();

    /**
     * @return the associated Destination if message is inbound, null otherwise
     */
    Destination getDestination();
    
    Exchange getExchange();

    void setExchange(Exchange exchange);
    
    Collection<Attachment> getAttachments();

    /**
     * @return the mime type string  
     */
    String getAttachmentMimeType();

    /**
     * Retreive the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the expected content format 
     * @return the encapsulated content
     */    
    <T> T getContent(Class<T> format);

    /**
     * Provide the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the provided content format 
     * @param content the content to be encapsulated
     */    
    <T> void setContent(Class<T> format, Object content);
    
    /**
     * @return the set of currently encapsulated content formats
     */
    Set<Class<?>> getContentFormats();
    
    /**
     * Convienience method for storing/retrieving typed objects from the map.
     * equivilent to:  (T)get(key.getName());
     * @param <T> key
     * @return
     */
    <T> T get(Class<T> key);
    /**
     * Convienience method for storing/retrieving typed objects from the map.
     * equivilent to:  put(key.getName(), value);
     * @param <T> key
     * @return
     */
    <T> void put(Class<T> key, T value);
    
    Object getContextualProperty(String key);   
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;

public class MessageImpl extends HashMap<String, Object> implements Message {
    private List<Attachment> attachments = new ArrayList<Attachment>();
    private Conduit conduit;
    private Destination destination;
    private Exchange exchange;
    private String id;
    private InterceptorChain interceptorChain;
    private Map<Class<?>, Object> contents = new HashMap<Class<?>, Object>();
    
    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }
    
    public Conduit getConduit() {
        return conduit;
    }

    public Destination getDestination() {
        return destination;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public String getId() {
        return id;
    }

    public InterceptorChain getInterceptorChain() {
        return this.interceptorChain;
    }

    public <T> T getContent(Class<T> format) {
        return format.cast(contents.get(format));
    }

    public <T> void setContent(Class<T> format, Object content) {
        contents.put(format, content);
    }

    public Set<Class<?>> getContentFormats() {
        return contents.keySet();
    }

    public void setConduit(Conduit c) {
        this.conduit = c;
    }

    public void setDestination(Destination d) {
        this.destination = d;
    }

    public void setExchange(Exchange e) {
        this.exchange = e;
    }

    public void setId(String i) {
        this.id = i;
    }

    public void setInterceptorChain(InterceptorChain ic) {
        this.interceptorChain = ic;
    }
    
    public <T> T get(Class<T> key) {
        return key.cast(get(key.getName()));
    }

    public <T> void put(Class<T> key, T value) {
        put(key.getName(), value);
    }

    public Object getContextualProperty(String key) {
        Object val = get(key);
        
        if (val == null) {
            val = getExchange().get(key);
        }
        
        if (val == null) {
            OperationInfo ep = get(OperationInfo.class); 
            if (ep != null) {
                val = ep.getProperty(key);
            }
        }
        
        if (val == null) {
            EndpointInfo ep = get(EndpointInfo.class); 
            if (ep != null) {
                val = ep.getProperty(key);
            }
        }
        
        if (val == null) {
            Service ep = get(Service.class); 
            if (ep != null) {
                val = ep.get(key);
            }
        }
        
        return val;
    }
    
    
}

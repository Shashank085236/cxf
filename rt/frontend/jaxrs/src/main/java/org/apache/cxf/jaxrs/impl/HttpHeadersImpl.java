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

package org.apache.cxf.jaxrs.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class HttpHeadersImpl implements HttpHeaders {

    private Message m;
    private MultivaluedMap<String, String> headers;
    
    @SuppressWarnings("unchecked")
    public HttpHeadersImpl(Message message) {
        this.m = message;
        this.headers = new MetadataMap<String, String>(
            (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS));
    }
    
    public List<MediaType> getAcceptableMediaTypes() {
        return JAXRSUtils.sortMediaTypes((String)m.get(Message.ACCEPT_CONTENT_TYPE)); 
    }

    public Map<String, Cookie> getCookies() {
        List<String> cs = headers.get(HttpHeaders.COOKIE);
        Map<String, Cookie> cl = new HashMap<String, Cookie>(); 
        for (String c : cs) {
            Cookie cookie = Cookie.valueOf(c);
            cl.put(cookie.getName(), cookie);
        }
        return cl;
    }

    public Locale getLanguage() {
        String l = headers.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        return l == null || l.length() == 0 ? null : new Locale(l);
    }

    public MediaType getMediaType() {
        return MediaType.valueOf((String)m.get(Message.CONTENT_TYPE));
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        // should we really worry about immutability given that the Message does not ?
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(headers);
        return map;
    }

    public List<Locale> getAcceptableLanguages() {
        List<String> values = getRequestHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Locale> newLs = new ArrayList<Locale>(); 
        String[] ls =  values.get(0).split(",");
        Map<Locale, Float> prefs = new HashMap<Locale, Float>();
        for (String l : ls) {
            String[] pair = l.split(";");
            
            Locale locale = new Locale(pair[0].trim());
            
            newLs.add(locale);
            if (pair.length > 1) {
                String[] pair2 = pair[1].split("=");
                if (pair2.length > 1) {
                    prefs.put(locale, JAXRSUtils.getMediaTypeQualityFactor(pair2[1].trim()));
                } else {
                    prefs.put(locale, 1F);
                }
            } else {
                prefs.put(locale, 1F);
            }
        }
        if (newLs.size() == 1) {
            return newLs;
        }
        
        Collections.sort(newLs, new AcceptLanguageComparator(prefs));
        return newLs;
        
    }

    public List<String> getRequestHeader(String name) {
        return headers.get(name);
    }

    private static class AcceptLanguageComparator implements Comparator<Locale> {
        private Map<Locale, Float> prefs;
        
        public AcceptLanguageComparator(Map<Locale, Float> prefs) {
            this.prefs = prefs;
        }

        public int compare(Locale lang1, Locale lang2) {
            float p1 = prefs.get(lang1);
            float p2 = prefs.get(lang2);
            int result = Float.compare(p1, p2);
            return result == 0 ? result : result * -1;
        }
    }
}

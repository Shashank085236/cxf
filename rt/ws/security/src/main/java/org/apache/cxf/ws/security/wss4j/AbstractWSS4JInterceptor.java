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
package org.apache.cxf.ws.security.wss4j;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandler;
import org.apache.ws.security.handler.WSHandlerConstants;

public abstract class AbstractWSS4JInterceptor extends WSHandler implements SoapInterceptor, 
    PhaseInterceptor<SoapMessage> {
    
    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "Security"));
        HEADERS.add(new QName(WSConstants.ENC_NS, "EncryptedData"));
    }

    private Map<String, Object> properties = new HashMap<String, Object>();
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private String phase;
    private String id;
    
    public AbstractWSS4JInterceptor() {
        super();
        id = getClass().getName();
    }

    public Set<URI> getRoles() {
        return null;
    }

    public void handleFault(SoapMessage message) {
    }

    public void postHandleMessage(SoapMessage message) throws Fault {
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Object getOption(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getPassword(Object msgContext) {
        return (String)((Message)msgContext).getContextualProperty("password");
    }

    public Object getProperty(Object msgContext, String key) {
        Object obj = ((Message)msgContext).getContextualProperty(key);
        if (obj == null) {
            obj = getOption(key);
        }
        return obj;
    }

    public void setPassword(Object msgContext, String password) {
        ((Message)msgContext).put("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Message)msgContext).put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Set<String> getAfter() {
        return after;
    }

    public void setAfter(Set<String> after) {
        this.after = after;
    }

    public Set<String> getBefore() {
        return before;
    }

    public void setBefore(Set<String> before) {
        this.before = before;
    }
    
    private boolean isRequestor(SoapMessage message) {
        return Boolean.TRUE.equals(message.containsKey(
            org.apache.cxf.message.Message.REQUESTOR_ROLE));
    }  
    
    
    protected void checkPolicies(SoapMessage message, RequestData data) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        String action = getString(WSHandlerConstants.ACTION, message);
        if (action == null) {
            action = "";
        }
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.INCLUDE_TIMESTAMP);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    if (!action.contains(WSHandlerConstants.TIMESTAMP)) {
                        action = WSHandlerConstants.TIMESTAMP + " " + action;
                    }
                    ai.setAsserted(true);
                }                    
            }
            ais = aim.get(SP12Constants.LAYOUT);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    Layout lay = (Layout)ai.getAssertion();
                    //wss4j can only do "Lax"
                    if (SPConstants.Layout.Lax == lay.getValue()) {
                        ai.setAsserted(true);
                    }
                }                    
            }
            ais = aim.get(SP12Constants.TRANSPORT_BINDING);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }                    
            }
            ais = aim.get(SP12Constants.SIGNED_SUPPORTING_TOKENS);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    SupportingToken sp = (SupportingToken)ai.getAssertion();
                    action = doTokens(sp.getTokens(), action, aim, message);
                    ai.setAsserted(true);
                }                    
            }
            ais = aim.get(SP12Constants.WSS10);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }                    
            }
            ais = aim.get(SP12Constants.WSS11);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }                    
            }
            message.put(WSHandlerConstants.ACTION, action.trim());
        }
    }
    
    private String doTokens(List<Token> tokens, 
                            String action, 
                            AssertionInfoMap aim,
                            SoapMessage msg) {
        for (Token token : tokens) {
            if (token instanceof UsernameToken) {
                if (!action.contains(WSHandlerConstants.USERNAME_TOKEN)
                    && !isRequestor(msg)) {
                    action = WSHandlerConstants.USERNAME_TOKEN + " " + action;
                }
                Collection<AssertionInfo> ais2 = aim.get(SP12Constants.USERNAME_TOKEN);
                if (ais2 != null && !ais2.isEmpty()) {
                    for (AssertionInfo ai2 : ais2) {
                        if (ai2.getAssertion() == token) {
                            ai2.setAsserted(true);
                        }
                    }                    
                }
            }
        }        
        return action;
    }
}

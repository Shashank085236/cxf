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

package org.apache.cxf.transport.http;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.AbstractCachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.mortbay.http.handler.AbstractHttpHandler;

public class JettyHTTPDestinationTest extends TestCase {
    protected static final String AUTH_HEADER = "Authorization";
    protected static final String USER = "copernicus";
    protected static final String PASSWD = "epicycles";
    protected static final String BASIC_AUTH =
        "Basic " + Base64Utility.encode((USER + ":" + PASSWD).getBytes());   

    private static final String NOWHERE = "http://nada.nothing.nowhere.null/";
    private static final String PAYLOAD = "message payload";
    private static final String CHALLENGE_HEADER = "WWW-Authenticate";
    private static final String BASIC_CHALLENGE = "Basic realm=terra";
    private static final String DIGEST_CHALLENGE = "Digest realm=luna";
    private static final String CUSTOM_CHALLENGE = "Custom realm=sol";
    private Bus bus;
    private ConduitInitiator conduitInitiator;
    private Conduit decoupledBackChannel;
    private EndpointInfo endpointInfo;
    private EndpointReferenceType address;
    private EndpointReferenceType replyTo;
    private ServerEngine engine;
    private HTTPServerPolicy policy;
    private JettyHTTPDestination destination;
    private TestHttpRequest request;
    private TestHttpResponse response;
    private Message inMessage;
    private Message outMessage;
    private MessageObserver observer;
    private InputStream is;
    private OutputStream os;
    private IMocksControl control;

    
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
    }

    public void tearDown() {
        //control.verify();
        control = null;
        bus = null;
        conduitInitiator = null;
        decoupledBackChannel = null;
        address = null;
        replyTo = null;
        engine = null;
        request = null;
        response = null;
        inMessage = null;
        outMessage = null;
        is = null;
        os = null;
        destination = null;
    }
    public void testGetAddress() throws Exception {
        destination = setUpDestination();
        EndpointReferenceType ref = destination.getAddress();
        assertNotNull("unexpected null address", ref);
        assertEquals("unexpected address",
                     EndpointReferenceUtils.getAddress(ref),
                     StringUtils.addDefaultPortIfMissing(EndpointReferenceUtils.getAddress(address)));
    }
    public void testRemoveServant() throws Exception {
        destination = setUpDestination();
        setUpRemoveServant();
        destination.setMessageObserver(null);
    }

    public void testDoServiceRedirectURL() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(true);
        destination.doService(request, response);

        assertEquals("unexpected sendRedirect calls",
                     1,
                     response.getSendRedirectCallCount());
        assertEquals("unexpected commit calls",
                     1,
                     response.getCommitCallCount());
        assertEquals("unexpected setHandled calls",
                     1,
                     request.getHandledCallCount());
    }

    public void testDoService() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false);
        destination.doService(request, response);
        verifyDoService();
    }
    
    public void testDoServiceWithHttpGET() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false,
                       false,
                       false,
                       "GET",
                       "?customerId=abc&cutomerAdd=def");
        destination.doService(request, response);
        
        assertNotNull("unexpected null message", inMessage);
        assertEquals("unexpected method",
                     inMessage.get(Message.HTTP_REQUEST_METHOD),
                     "GET");
        assertEquals("unexpected path",
                     inMessage.get(Message.PATH_INFO),
                     "bar/foo");
        assertEquals("unexpected query",
                     inMessage.get(Message.QUERY_STRING),
                     "?customerId=abc&cutomerAdd=def");

    }

    public void testGetAnonBackChannel() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel = destination.getBackChannel(inMessage, null, null);
        
        assertNotNull("expected back channel", backChannel);
        assertNull("unexpected backchannel-backchannel",
                   backChannel.getBackChannel());
        assertEquals("unexpected target",
                     JettyHTTPDestination.ANONYMOUS_ADDRESS,
                     backChannel.getTarget().getAddress().getValue());
    }
    
    public void testGetBackChannelSend() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false, true);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage, null, null);
        outMessage = setUpOutMessage();
        backChannel.send(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 200);
    }

    public void testGetBackChannelSendFault() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false, true);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage, null, null);
        outMessage = setUpOutMessage();
        backChannel.send(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 500);
    }
    
    public void testGetBackChannelSendOneway() throws Exception {
        destination = setUpDestination(false);
        setUpDoService(false, true);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage, null, null);
        outMessage = setUpOutMessage();
        backChannel.send(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 500, true);
    }

    public void testGetBackChannelSendDecoupled() throws Exception {
        destination = setUpDestination(false);
        replyTo = getEPR(NOWHERE + "response/foo");
        setUpDoService(false, true, true);
        destination.doService(request, response);
        setUpInMessage();
        
        Message partialResponse = setUpOutMessage();
        Conduit partialBackChannel =
            destination.getBackChannel(inMessage, partialResponse, replyTo);
        assertEquals("unexpected response code",
                     202,
                     partialResponse.get(Message.RESPONSE_CODE));
        partialBackChannel.send(partialResponse);
        verifyBackChannelSend(partialBackChannel, partialResponse, 202);

        outMessage = setUpOutMessage();
        Conduit fullBackChannel =
            destination.getBackChannel(inMessage, null, replyTo);
        assertSame("unexpected back channel",
                   fullBackChannel,
                   decoupledBackChannel);
        fullBackChannel.send(outMessage);
    }
    
    public void testServerPolicyInServiceModel()
        throws Exception {
        address = getEPR("bar/foo");
        bus = new CXFBusImpl();
        
        conduitInitiator = control.createMock(ConduitInitiator.class);
        engine = control.createMock(ServerEngine.class);
        endpointInfo = new EndpointInfo();
        endpointInfo.setAddress(NOWHERE + "bar/foo");
       
        HTTPServerPolicy customPolicy = new HTTPServerPolicy();
        endpointInfo.addExtensor(customPolicy);

        control.replay();
        
        JettyHTTPDestination dest = new JettyHTTPDestination(bus,
                                                             conduitInitiator,
                                                             endpointInfo,
                                                             engine);
        assertEquals(customPolicy, dest.getConfig().getServer());
    }
        
    private JettyHTTPDestination setUpDestination()
        throws Exception {
        return setUpDestination(false);
    };
    
    private JettyHTTPDestination setUpDestination(boolean contextMatchOnStem)
        throws Exception {
        address = getEPR("bar/foo");
        bus = new CXFBusImpl();
        
        conduitInitiator = control.createMock(ConduitInitiator.class);
        engine = control.createMock(ServerEngine.class);
        endpointInfo = new EndpointInfo();
        endpointInfo.setAddress(NOWHERE + "bar/foo");
       
        engine.addServant(EasyMock.eq(new URL(NOWHERE + "bar/foo")),
                          EasyMock.isA(AbstractHttpHandler.class));
        
        control.replay();
        
        JettyHTTPDestination dest = new JettyHTTPDestination(bus,
                                                             conduitInitiator,
                                                             endpointInfo,
                                                             engine);
        dest.retrieveEngine();
        policy = dest.getConfig().getServer();
        observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage = m;
            }
        };
        dest.setMessageObserver(observer);
        return dest;
    }
    
    private void setUpRemoveServant() throws Exception {
        control.verify();
        control.reset();
        engine.removeServant(EasyMock.eq(new URL(NOWHERE + "bar/foo")));
        EasyMock.expectLastCall();
        control.replay();
    }
    
    private void setUpDoService(boolean setRedirectURL) throws Exception {
        setUpDoService(setRedirectURL, false);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse) throws Exception {
        setUpDoService(setRedirectURL,
                       sendResponse,
                       false);
    }        

    private void setUpDoService(boolean setRedirectURL,
            boolean sendResponse,
            boolean decoupled) throws Exception {
        String method = "POST";
        String query = "?name";
        setUpDoService(setRedirectURL, sendResponse, decoupled, method, query);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse,
                                boolean decoupled,
                                String method,
                                String query) throws Exception {

        control.verify();
        control.reset();

        is = EasyMock.createMock(InputStream.class);
        os = EasyMock.createMock(OutputStream.class);
        
        // EasyMock does not seem able to properly mock calls to HttpRequest
        // or HttpResponse - expectations set seem to be ignored.
        // Hence we use hand-crafted sub-classes instead of mocks.
        //
        //request = EasyMock.createMock(HttpRequest.class);
        //response = EasyMock.createMock(HttpResponse.class);
        request = new TestHttpRequest(method, is, "bar/foo", query);
        response = new TestHttpResponse(os);
        
        if (setRedirectURL) {
            policy.setRedirectURL(NOWHERE + "foo/bar");
            //response.sendRedirect(EasyMock.eq(NOWHERE + "foo/bar"));
            //EasyMock.expectLastCall();
            //response.commit();
            //EasyMock.expectLastCall();
            //request.setHandled(true);
            //EasyMock.expectLastCall();
        } else {
            //request.getMethod();
            //EasyMock.expectLastCall().andReturn("POST").times(2);
            //request.getInputStream();
            //EasyMock.expectLastCall().andReturn(is);
            //request.getPath();
            //EasyMock.expectLastCall().andReturn("bar/foo");
            //request.getQuery();
            //EasyMock.expectLastCall().andReturn(QUERY);
            //request.setHandled(true);
            //EasyMock.expectLastCall();  
            //response.commit();
            //EasyMock.expectLastCall();
            //if (sendResponse) {
            //    response.getOutputStream();
            //    EasyMock.expectLastCall().andReturn(os);
            //    response.commit();
            //    EasyMock.expectLastCall();                
            //}
        }
        
        if (decoupled) {
            decoupledBackChannel = EasyMock.createMock(Conduit.class);
            conduitInitiator.getConduit(EasyMock.isA(EndpointInfo.class),
                                        EasyMock.eq(replyTo));
            EasyMock.expectLastCall().andReturn(decoupledBackChannel);
            decoupledBackChannel.send(EasyMock.eq(outMessage));
            EasyMock.expectLastCall();
        }
        control.replay();
    }
    
    private void setUpInMessage() {
        inMessage.setExchange(new ExchangeImpl());
    }
    
    private Message setUpOutMessage() {
        Message outMsg = new MessageImpl();
        outMsg.putAll(inMessage);
        outMsg.setExchange(new ExchangeImpl());
        outMsg.put(Message.PROTOCOL_HEADERS,
                   new HashMap<String, List<String>>());
        return outMsg;
    }
    
    private void setUpResponseHeaders(Message outMsg) {
        Map<String, List<String>> responseHeaders =
            CastUtils.cast((Map<?, ?>)outMsg.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected response headers", responseHeaders);
        List<String> challenges = new ArrayList<String>();
        challenges.add(BASIC_CHALLENGE);
        challenges.add(DIGEST_CHALLENGE);
        challenges.add(CUSTOM_CHALLENGE);
        responseHeaders.put(CHALLENGE_HEADER, challenges);
    }

    private void verifyDoService() throws Exception {
        assertNotNull("unexpected null message", inMessage);
        assertSame("unexpected HTTP request",
                   inMessage.get(JettyHTTPDestination.HTTP_REQUEST),
                   request);
        assertSame("unexpected HTTP response",
                   inMessage.get(JettyHTTPDestination.HTTP_RESPONSE),
                   response);
        assertEquals("unexpected method",
                     inMessage.get(Message.HTTP_REQUEST_METHOD),
                     "POST");
        assertEquals("unexpected path",
                     inMessage.get(Message.PATH_INFO),
                     "bar/foo");
        assertEquals("unexpected query",
                     inMessage.get(Message.QUERY_STRING),
                     "?name");
        verifyRequestHeaders();
        
        
        assertEquals("unexpected getMethod calls",
                      2,
                     request.getMethodCallCount());
        assertEquals("unexpected getInputStream calls",
                     1,
                     request.getInputStreamCallCount());
        assertEquals("unexpected getPath calls",
                     1,
                     request.getPathCallCount());
        assertEquals("unexpected getQuery calls",
                     1,
                     request.getQueryCallCount());
        assertEquals("unexpected setHandled calls",
                     1,
                     request.getHandledCallCount());
    }

    private void verifyRequestHeaders() throws Exception {
        Map<String, List<String>> requestHeaders =
            CastUtils.cast((Map<?, ?>)inMessage.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected request headers",
                      requestHeaders);
        assertEquals("expected getFieldNames",
                     1,
                     request.getFieldNamesCallCount());
        List<String> values = requestHeaders.get("content-type");
        assertNotNull("expected field", values);
        assertEquals("unexpected values", 2, values.size());
        assertTrue("expected value", values.contains("text/xml"));
        assertTrue("expected value", values.contains("charset=utf8"));
        values = requestHeaders.get(AUTH_HEADER);
        assertNotNull("expected field", values);
        assertEquals("unexpected values", 1, values.size());
        assertTrue("expected value", values.contains(BASIC_AUTH));
        
        AuthorizationPolicy authpolicy =
            inMessage.get(AuthorizationPolicy.class);
        assertNotNull("Expected some auth tokens", policy);
        assertEquals("expected user",
                     USER,
                     authpolicy.getUserName());
        assertEquals("expected passwd",
                     PASSWD,
                     authpolicy.getPassword());
    }
    
    private void verifyResponseHeaders(Message outMsg) throws Exception {
        Map<String, List<String>> responseHeaders =
            CastUtils.cast((Map<?, ?>)outMsg.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected response headers",
                      responseHeaders);
        assertEquals("expected addField",
                     3,
                     response.getAddFieldCallCount());
        Enumeration e = response.getFieldValues(CHALLENGE_HEADER);
        List<String> challenges = new ArrayList<String>();
        while (e.hasMoreElements()) {
            challenges.add((String)e.nextElement());
        }
        assertTrue("expected challenge",
                   challenges.contains(BASIC_CHALLENGE));
        assertTrue("expected challenge",
                   challenges.contains(DIGEST_CHALLENGE));
        assertTrue("expected challenge",
                   challenges.contains(CUSTOM_CHALLENGE));
    }
    
    private void verifyBackChannelSend(Conduit backChannel,
                                       Message outMsg,
                                       int status) throws Exception {
        verifyBackChannelSend(backChannel, outMsg, status, false);
    }
    
    private void verifyBackChannelSend(Conduit backChannel,
                                       Message outMsg,
                                       int status,
                                       boolean oneway) throws Exception {
        assertTrue("unexpected back channel type",
                   backChannel instanceof JettyHTTPDestination.BackChannelConduit);
        assertTrue("unexpected content formats",
                   outMsg.getContentFormats().contains(OutputStream.class));
        OutputStream responseOS = outMsg.getContent(OutputStream.class);
        assertNotNull("expected output stream", responseOS);
        assertTrue("unexpected output stream type",
                   responseOS instanceof AbstractCachedOutputStream);
        assertEquals("expected commit",
                     1,
                     response.getCommitCallCount());
        
        outMsg.put(Message.RESPONSE_CODE, status);          
        responseOS.write(PAYLOAD.getBytes());
        
        setUpResponseHeaders(outMsg);
        
        OutputStream underlyingOS =
            ((AbstractCachedOutputStream)responseOS).getOut();
        assertTrue("unexpected underlying output stream type",
                   underlyingOS instanceof ByteArrayOutputStream);
        assertEquals("expected getOutputStream",
                     0,
                     response.getOutputStreamCallCount());
        outMsg.getExchange().setOneWay(oneway);
        responseOS.flush();
        assertEquals("expected setStatus",
                     1,
                     response.getStatusCallCount());
        assertEquals("unexpected status",
                     status,
                     response.getStatus());
        if (status == 500) {
            assertEquals("unexpected status message",
                         "Fault+Occurred",
                         response.getReason());
        }
        verifyResponseHeaders(outMsg);
        assertEquals("expected getOutputStream",
                     1,
                     response.getOutputStreamCallCount());
        underlyingOS = ((AbstractCachedOutputStream)responseOS).getOut();
        assertFalse("unexpected underlying output stream type: "
                    + underlyingOS.getClass(),
                    underlyingOS instanceof ByteArrayOutputStream);
        assertEquals("expected commit",
                     oneway ? 2 : 1,
                     response.getCommitCallCount());
        if (oneway) {
            assertNull("unexpected HTTP response",
                       outMsg.get(JettyHTTPDestination.HTTP_RESPONSE));
        } else {
            assertNotNull("expected HTTP response",
                           outMsg.get(JettyHTTPDestination.HTTP_RESPONSE));
            responseOS.close();
            assertEquals("expected commit",
                         2,
                         response.getCommitCallCount());
        }
    }
    
    static EndpointReferenceType getEPR(String s) {
        return EndpointReferenceUtils.getEndpointReference(NOWHERE + s);
    }
}

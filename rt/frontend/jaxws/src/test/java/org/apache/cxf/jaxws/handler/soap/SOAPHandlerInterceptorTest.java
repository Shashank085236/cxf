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

package org.apache.cxf.jaxws.handler.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
// import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import junit.framework.TestCase;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.io.AbstractCachedOutputStream;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

import org.easymock.classextension.IMocksControl;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceControl;

public class SOAPHandlerInterceptorTest extends TestCase {

    public void setUp() {
    }

    public void tearDown() {
    }

    // SAAJ tree is created on demand. SAAJ wont be created without
    // the calling of SOAPMessageContext.getMessage().
    public void testNoCallToGetMessageOutBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);

        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);

        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        CachedStream originalEmptyOs = new CachedStream();
        message.setContent(OutputStream.class, originalEmptyOs);

        InterceptorChain chain = new PhaseInterceptorChain((new PhaseManagerImpl()).getOutPhases());
        // This is to simulate interceptors followed by SOAPHandlerInterceptor
        // write outputStream
        chain.add(new AbstractProtocolHandlerInterceptor<SoapMessage>(binding) {
            public void handleMessage(SoapMessage message) throws Fault {
                try {
                    CachedStream os = prepareOutputStreamFromResource("resources/greetMeRpcLitResp.xml");
                    message.setContent(OutputStream.class, os);
                } catch (Exception e) {
                    // do nothing
                }
            }

        });
        message.setInterceptorChain(chain);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify outputStream
        CachedStream expectedOs = prepareOutputStreamFromResource("resources/greetMeRpcLitResp.xml");
        assertTrue("The content of outputStream should remain unchanged", compareInputStream(expectedOs
            .getInputStream(), originalEmptyOs.getInputStream()));
        // Verify SOAPMessage
        SOAPMessage resultedMessage = message.getContent(SOAPMessage.class);
        assertNull(resultedMessage);
    }

    // SAAJ tree is created on if SOAPMessageContext.getMessage() is
    // called. Any changes to SOAPMessage should be streamed back to
    // outputStream
    public void xtestSOAPBodyChangedOutBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (outboundProperty.booleanValue()) {
                    try {
                        SOAPMessage message = smc.getMessage();
                        message = preparemSOAPMessage("resources/greetMeRpcLitRespChanged.xml");
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);

        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);

        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        CachedStream originalEmptyOs = new CachedStream();
        message.setContent(OutputStream.class, originalEmptyOs);

        InterceptorChain chain = new PhaseInterceptorChain((new PhaseManagerImpl()).getOutPhases());
        // This is to simulate interceptors followed by SOAPHandlerInterceptor
        // write outputStream
        chain.add(new AbstractProtocolHandlerInterceptor<SoapMessage>(binding) {
            public void handleMessage(SoapMessage message) throws Fault {
                try {
                    CachedStream os = prepareOutputStreamFromResource(
                        "resources/greetMeRpcLitRespChanged.xml");
                    message.setContent(OutputStream.class, os);
                } catch (Exception e) {
                    // do nothing
                }
            }

        });
        message.setInterceptorChain(chain);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify outputStream
        CachedStream expectedOs = prepareOutputStreamFromResource("resources/greetMeRpcLitRespChanged.xml");
        assertTrue("The content of outputStream should remain unchanged", compareInputStream(expectedOs
            .getInputStream(), originalEmptyOs.getInputStream()));

        // Verify SOAPMessage
        SOAPMessage resultedMessage = message.getContent(SOAPMessage.class);
        assertNotNull(resultedMessage);
        SOAPBody bodyNew = resultedMessage.getSOAPBody();
        Iterator itNew = bodyNew.getChildElements(new QName("http://apache.org/hello_world_rpclit",
                                                            "sendReceiveDataResponse"));
        SOAPBodyElement bodyElementNew = (SOAPBodyElement)itNew.next();
        Iterator outIt = bodyElementNew
            .getChildElements(new QName("http://apache.org/hello_world_rpclit/types", "out"));
        Element outElement = (SOAPBodyElement)outIt.next();
        assertNotNull(outElement);
        NodeList elem3NodeList = outElement
            .getElementsByTagNameNS("http://apache.org/hello_world_rpclit/types", "elem3");
        Node elem3Element = elem3NodeList.item(0);
        assertNotNull("100", elem3Element.getTextContent());
    }

    public void xtestGetSOAPHeaderInBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                try {
                    // change mustUnderstand to false
                    SOAPMessage message = smc.getMessage();
                    SOAPHeader soapHeader = message.getSOAPHeader();
                    Iterator it = soapHeader.getChildElements();
                    SOAPHeaderElement headerElementNew = (SOAPHeaderElement)it.next();

                    SoapVersion soapVersion = Soap11.getInstance();
                    headerElementNew.setAttributeNS(soapVersion.getNamespace(), "SOAP-ENV:mustUnderstand",
                                                    "false");
                } catch (Exception e) {
                    throw new Fault(e);
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(null);

        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);

        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);
        Element headerElement = preparemSOAPHeader();
        message.setHeaders(Element.class, headerElement);
        message.put(Element.class, headerElement);

        // message.setContent(Element.class, preparemSOAPHeader());
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify SOAPMessage header
        SOAPMessage soapMessageNew = message.getContent(SOAPMessage.class);

        SOAPHeader soapHeader = soapMessageNew.getSOAPHeader();
        Iterator itNew = soapHeader.getChildElements();
        SOAPHeaderElement headerElementNew = (SOAPHeaderElement)itNew.next();
        SoapVersion soapVersion = Soap11.getInstance();
        assertEquals("false", headerElementNew.getAttributeNS(soapVersion.getNamespace(), "mustUnderstand"));

        // Verify the XMLStreamReader
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        QName qn = xmlReader.getName();
        assertEquals("sendReceiveData", qn.getLocalPart());
    }

    public void xtestGetSOAPMessageInBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                try {
                    SOAPMessage message = smc.getMessage();
                } catch (Exception e) {
                    throw new Fault(e);
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(null);

        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);

        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify SOAPMessage
        SOAPMessage soapMessageNew = message.getContent(SOAPMessage.class);
        SOAPBody bodyNew = soapMessageNew.getSOAPBody();
        Iterator itNew = bodyNew.getChildElements();
        SOAPBodyElement bodyElementNew = (SOAPBodyElement)itNew.next();
        assertEquals("sendReceiveData", bodyElementNew.getLocalName());

        // Verify the XMLStreamReader
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        QName qn = xmlReader.getName();
        assertEquals("sendReceiveData", qn.getLocalPart());
    }

    public void xtestgetUnderstoodHeadersReturnsNull() {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        SoapMessage message = control.createMock(SoapMessage.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(binding.getHandlerChain()).andReturn(list);
        expect(message.getExchange()).andReturn(exchange);
        expect(message.keySet()).andReturn(new HashSet<String>());
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        Set<QName> understood = li.getUnderstoodHeaders();
        assertNotNull(understood);
        assertTrue(understood.isEmpty());
    }

    private boolean compareInputStream(InputStream os1, InputStream os2) throws Exception {
        if (os1.available() != os2.available()) {
            return false;
        }
        for (int i = 0; i < os1.available(); i++) {
            if (os1.read() != os2.read()) {
                return false;
            }
        }
        return true;
    }

    private XMLStreamReader preparemXMLStreamReader(String resouceName) throws Exception {
        InputStream is = this.getClass().getResourceAsStream(resouceName);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(is);

        // skip until soap body
        if (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            String ns = xmlReader.getNamespaceURI();
            SoapVersion soapVersion = SoapVersionFactory.getInstance().getSoapVersion(ns);
            // message.setVersion(soapVersion);

            QName qn = xmlReader.getName();
            while (!qn.equals(soapVersion.getBody()) && !qn.equals(soapVersion.getHeader())) {
                while (xmlReader.nextTag() != XMLStreamConstants.START_ELEMENT) {
                    // nothing to do
                }
                qn = xmlReader.getName();
            }
            if (qn.equals(soapVersion.getHeader())) {
                XMLStreamReader filteredReader = new PartialXMLStreamReader(xmlReader, soapVersion.getBody());

                Document doc = StaxUtils.read(filteredReader);
            }
            // advance just past body.
            xmlReader.next();

            while (xmlReader.isWhiteSpace()) {
                xmlReader.next();
            }
        }
        return xmlReader;
    }

    private Element preparemSOAPHeader() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        SoapVersion soapVersion = Soap11.getInstance();
        Element headerElement = doc.createElementNS(soapVersion.getNamespace(), "SOAP-ENV:"
                                                                                + soapVersion.getHeader()
                                                                                    .getLocalPart());
        Element childElement = doc.createElementNS("http://apache.org/hello_world_rpclit/types",
                                                   "ns2:header1");
        childElement.setAttributeNS(soapVersion.getNamespace(), "SOAP-ENV:mustUnderstand", "true");
        headerElement.appendChild(childElement);
        return headerElement;
    }

    private SOAPMessage preparemSOAPMessage(String resouceName) throws Exception {
        InputStream is = this.getClass().getResourceAsStream(resouceName);
        SOAPMessage soapMessage = null;
        MessageFactory factory = MessageFactory.newInstance();
        MimeHeaders mhs = null;
        soapMessage = factory.createMessage(mhs, is);
        return soapMessage;
    }

    private CachedStream prepareOutputStreamFromResource(String resouceName) throws Exception {
        SOAPMessage soapMessage = preparemSOAPMessage(resouceName);
        CachedStream os = new CachedStream();
        soapMessage.writeTo(os);
        return os;
    }

    private CachedStream prepareOutputStreamFromSOAPMessage(SOAPMessage soapMessage) throws Exception {
        CachedStream os = new CachedStream();
        soapMessage.writeTo(os);
        return os;
    }

    private class CachedStream extends AbstractCachedOutputStream {
        protected void doFlush() throws IOException {
            currentStream.flush();
        }

        protected void doClose() throws IOException {
        }

        protected void onWrite() throws IOException {
        }
    }

}

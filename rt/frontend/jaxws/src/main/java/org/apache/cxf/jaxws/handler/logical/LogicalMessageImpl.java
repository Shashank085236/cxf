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

package org.apache.cxf.jaxws.handler.logical;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;


public class LogicalMessageImpl implements LogicalMessage {

    private final LogicalMessageContextImpl msgContext;
    
    public LogicalMessageImpl(LogicalMessageContextImpl lmctx) {
        msgContext = lmctx;
    }

    public Source getPayload() {
        Source source = msgContext.getWrappedMessage().getContent(Source.class);
        if (source == null) {
            //need to convert
            SOAPMessage msg = msgContext.getWrappedMessage().getContent(SOAPMessage.class);
            XMLStreamReader reader = null;
            if (msg != null) {
                try {
                    source = new DOMSource(msg.getSOAPBody().getFirstChild());
                    reader = StaxUtils.createXMLStreamReader(source);
                } catch (SOAPException e) {
                    //ignore
                }
            }

            if (source == null) {
                try {
                    W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                    reader = msgContext.getWrappedMessage().getContent(XMLStreamReader.class);
                    StaxUtils.copy(reader, writer);
                    source = new DOMSource(writer.getDocument().getDocumentElement());
                    reader = StaxUtils.createXMLStreamReader(writer.getDocument());
                } catch (ParserConfigurationException e) {
                    throw new WebServiceException(e);
                } catch (XMLStreamException e) {
                    throw new WebServiceException(e);
                }
            }
            msgContext.getWrappedMessage().setContent(XMLStreamReader.class, reader);
            msgContext.getWrappedMessage().setContent(Source.class, source);
        } else if (!(source instanceof DOMSource)) {
            W3CDOMStreamWriter writer;
            try {
                writer = new W3CDOMStreamWriter();
            } catch (ParserConfigurationException e) {
                throw new WebServiceException(e);
            }
            XMLStreamReader reader = msgContext.getWrappedMessage().getContent(XMLStreamReader.class);
            if (reader == null) {
                reader = StaxUtils.createXMLStreamReader(source);
            }
            try {
                StaxUtils.copy(reader, writer);
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            
            source = new DOMSource(writer.getDocument().getDocumentElement());
            
            reader = StaxUtils.createXMLStreamReader(writer.getDocument());
            msgContext.getWrappedMessage().setContent(XMLStreamReader.class, reader);
            msgContext.getWrappedMessage().setContent(Source.class, source);
        }
        return source;
    }

    public void setPayload(Source s) {
        msgContext.getWrappedMessage().setContent(Source.class, s);
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(s);
        msgContext.getWrappedMessage().setContent(XMLStreamReader.class, reader);                
    }

    public Object getPayload(JAXBContext arg0) {
        try {
            return arg0.createUnmarshaller().unmarshal(getPayload());
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public void setPayload(Object arg0, JAXBContext arg1) {
        try {
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            arg1.createMarshaller().marshal(arg0, writer);
            Source source = new DOMSource(writer.getDocument().getDocumentElement());            
            
            setPayload(source);
        } catch (ParserConfigurationException e) {
            throw new WebServiceException(e);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

   
}

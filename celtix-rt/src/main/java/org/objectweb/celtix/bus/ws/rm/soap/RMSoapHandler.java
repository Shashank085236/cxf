package org.objectweb.celtix.bus.ws.rm.soap;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.objectweb.celtix.bus.ws.addressing.ContextUtils;
import org.objectweb.celtix.bus.ws.rm.Names;
import org.objectweb.celtix.bus.ws.rm.RMContextUtils;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.ws.rm.AckRequestedType;
import org.objectweb.celtix.ws.rm.SequenceAcknowledgement;
import org.objectweb.celtix.ws.rm.SequenceType;


/**
 * Protocol Handler responsible for {en|de}coding the Message Addressing 
 * Properties for {outgo|incom}ing messages.
 */
public class RMSoapHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = LogUtils.getL7dLogger(RMSoapHandler.class);
    private static final String WS_RM_PACKAGE = 
        SequenceType.class.getPackage().getName();
    protected JAXBContext jaxbContext;

    /**
     * Constructor.
     */
    public RMSoapHandler() {
    } 

    /**
     * Initialize the handler.
     */
    public void init(Map<String, Object> map) {
    }

    /**
     * @return the set of SOAP headers understood by this handler 
     */
    public Set<QName> getHeaders() {
        return Names.HEADERS;
    }
    
    /**
     * Invoked for normal processing of inbound and outbound messages.
     *
     * @param context the messsage context
     */
    public boolean handleMessage(SOAPMessageContext context) {
        return mediate(context);
    }

    /**
     * Invoked for fault processing.
     *
     * @param context the messsage context
     */
    public boolean handleFault(SOAPMessageContext context) {
        return mediate(context);
    }

    /**
     * Called at the conclusion of a message exchange pattern just prior to
     * the JAX-WS runtime dispatching a message, fault or exception.
     *
     * @param context the message context
     */
    public void close(MessageContext context) {
    }

    /**
     * Release handler resources.
     */
    public void destroy() {
    }

    /**
     * Mediate message flow, peforming MAP {en|de}coding.
     * 
     * @param context the messsage context
     * @return true if processing should continue on dispatch path 
     */     
    private boolean mediate(SOAPMessageContext context) {
        if (ContextUtils.isOutbound(context)) {
            encode(context);
        } else {
            decode(context);
        }
        return true;
    }
    
    /**
     * Encode the current RM properties  in protocol-specific headers.
     *
     * @param context the message context.
     */
    private void encode(SOAPMessageContext context) {
        SOAPMessage message = context.getMessage();
        try {
            SOAPEnvelope env = message.getSOAPPart().getEnvelope();
            SOAPHeader header = env.getHeader() != null 
                                ? env.getHeader()
                                : env.addHeader(); 
                                
            discardRMHeaders(header);
            header.addNamespaceDeclaration(Names.WSRM_NAMESPACE_PREFIX,
                                           Names.WSRM_NAMESPACE_NAME);
            Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            SequenceType seq = RMContextUtils.retrieveSequence(context);
            if (null != seq) {
                encodeProperty(seq, 
                               Names.WSRM_SEQUENCE_QNAME, 
                               SequenceType.class, 
                               header,
                               marshaller);
            } 
            List<SequenceAcknowledgement> acks = RMContextUtils.retrieveAcknowledgments(context);
            if (null != acks) {
                for (SequenceAcknowledgement ack : acks) {
                    encodeProperty(ack, 
                                   Names.WSRM_SEQUENCE_ACK_QNAME, 
                                   SequenceAcknowledgement.class, 
                                   header,
                                   marshaller);
                }
            }
            List<AckRequestedType> requested = RMContextUtils.retrieveAcksRequested(context);
            if (null != requested) {
                for (AckRequestedType ar : requested) {
                    encodeProperty(ar, 
                                   Names.WSRM_ACK_REQUESTED_QNAME, 
                                   AckRequestedType.class, 
                                   header,
                                   marshaller);
                }
            }
        } catch (SOAPException se) {
            LOG.log(Level.WARNING, "SOAP_HEADER_ENCODE_FAILURE_MSG", se); 
        } catch (JAXBException je) {
            LOG.log(Level.WARNING, "SOAP_HEADER_ENCODE_FAILURE_MSG", je);
        }
    }
    
    /**
     * Decode the RM properties from protocol-specific headers.
     *  
     * @param context the messsage context
     * @param the decoded MAPs
     * @exception SOAPFaultException if decoded MAPs are invalid 
     */
    private void decode(SOAPMessageContext context) {        
        try {
            List<SequenceAcknowledgement> acks = new ArrayList<SequenceAcknowledgement>();
            List<AckRequestedType> requested = new ArrayList<AckRequestedType>();           
            
            SOAPMessage message = context.getMessage();
            SOAPEnvelope env = message.getSOAPPart().getEnvelope();
            SOAPHeader header = env.getHeader();
            
            if (header != null) {
                Unmarshaller unmarshaller = 
                    getJAXBContext().createUnmarshaller();
                Iterator headerElements = header.examineAllHeaderElements();
                while (headerElements.hasNext()) {
                    SOAPHeaderElement headerElement = 
                        (SOAPHeaderElement)headerElements.next();
                    Name headerName = headerElement.getElementName();
                    String localName = headerName.getLocalName(); 
                    LOG.log(Level.INFO, "decoding RM header {0}", localName);
                    if (Names.WSRM_NAMESPACE_NAME.equals(headerName.getURI())) {
                        if (Names.WSRM_SEQUENCE_NAME.equals(localName)) {
                            SequenceType s = decodeProperty(SequenceType.class,
                                                            headerElement,
                                                            unmarshaller);
                            RMContextUtils.storeSequence(context, s);
                        } else if (Names.WSRM_SEQUENCE_ACK_NAME.equals(localName)) {
                            SequenceAcknowledgement ack = decodeProperty(SequenceAcknowledgement.class,
                                                            headerElement,
                                                            unmarshaller);
                            acks.add(ack);                            
                        } else if (Names.WSRM_ACK_REQUESTED_NAME.equals(localName)) {
                            AckRequestedType ar = decodeProperty(AckRequestedType.class,
                                                            headerElement,
                                                            unmarshaller);
                            requested.add(ar);
                        }
                    }
                }
                if (acks.size() > 0) {
                    RMContextUtils.storeAcknowledgments(context, acks);
                }
                if (requested.size() > 0) {
                    RMContextUtils.storeAcksRequested(context, requested);
                }
            }
        } catch (SOAPException se) {
            LOG.log(Level.WARNING, "SOAP_HEADER_DECODE_FAILURE_MSG", se); 
        } catch (JAXBException je) {
            LOG.log(Level.WARNING, "SOAP_HEADER_DECODE_FAILURE_MSG", je); 
        }
    }


    /**
     * @return a JAXBContext
     */
    private synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(WS_RM_PACKAGE);
        }
        return jaxbContext;
    }
    
    /**
     * Encodes an RM property as a SOAP header.
     *
     * @param value the value to encode
     * @param qname the QName for the header 
     * @param clz the class
     * @param header the SOAP header
     * @param marshaller the JAXB marshaller to use
     */
    private <T> void encodeProperty(T value, 
                                    QName qname, 
                                    Class<T> clz, 
                                    SOAPHeader header,
                                    Marshaller marshaller) throws JAXBException {
        LOG.log(Level.INFO, "encoding RM header {0}", qname);
        if (value != null) {
            marshaller.marshal(new JAXBElement<T>(qname, clz, value), header);
        }
    }
    
    /**
     * Decodes an RM property from a SOAP header.
     * 
     * @param clz the class
     * @param headerElement the SOAP header element
     * @param marshaller the JAXB marshaller to use
     * @return the decoded EndpointReference
     */
    private <T> T decodeProperty(Class<T> clz,
                            SOAPHeaderElement headerElement,
                            Unmarshaller unmarshaller) throws JAXBException {
        JAXBElement<T> element =
            unmarshaller.unmarshal(headerElement, clz);
        return element.getValue();
    }


    /**
     * Discard any pre-existing RM headers - this may occur if the runtime
     * re-uses a SOAP message.
     *
     * @param header the SOAP header
     */
    private void discardRMHeaders(SOAPHeader header) throws SOAPException {
        Iterator headerElements = header.examineAllHeaderElements();
        while (headerElements.hasNext()) {
            SOAPHeaderElement headerElement =
                (SOAPHeaderElement)headerElements.next();
            Name headerName = headerElement.getElementName();
            if (Names.WSRM_NAMESPACE_NAME.equals(headerName.getURI())) {
                headerElement.detachNode();
            }
        }
    }

}







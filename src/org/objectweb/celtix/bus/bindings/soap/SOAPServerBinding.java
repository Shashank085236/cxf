package org.objectweb.celtix.bus.bindings.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bindings.AbstractServerBinding;
import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.ServerBindingEndpointCallback;
import org.objectweb.celtix.bus.handlers.HandlerChainInvoker;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.handlers.HandlerInvoker;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.TransportFactory;

public class SOAPServerBinding extends AbstractServerBinding {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SOAPServerBinding.class);
    
    protected final SOAPBindingImpl soapBinding;
    
    public SOAPServerBinding(Bus b,
                             EndpointReferenceType ref,
                             Endpoint ep,
                             ServerBindingEndpointCallback cbFactory) {
        super(b, ref, ep, cbFactory);
        soapBinding = new SOAPBindingImpl(true);
    }
    
    public Binding getBinding() {
        return soapBinding;
    }
    
    public boolean isCompatibleWithAddress(String address) {
        return soapBinding.isCompatibleWithAddress(address);
    }
    

    public HandlerInvoker createHandlerInvoker() {
        return new HandlerChainInvoker(getBinding().getHandlerChain()); 
    }

    protected ServerTransport createTransport(EndpointReferenceType ref) throws WSDLException, IOException {
        // TODO get from configuration
        // TODO get from reference bindingID
        try {
            TransportFactory tf = 
                    bus.getTransportFactoryManager().getTransportFactory(SOAPConstants.SOAP_URI);
            return tf.createServerTransport(ref);
        } catch (BusException ex) {
            LOG.severe("TRANSPORT_FACTORY_RETREIVAL_FAILURE_MSG");
        }
        return null;
    }
    protected OutputStreamMessageContext createOutputStreamContext(ServerTransport t,
                                                                   MessageContext bindingContext)
        throws IOException {
        if (bindingContext instanceof SOAPMessageContext) {
            SOAPMessage msg = ((SOAPMessageContext)bindingContext).getMessage();
            soapBinding.updateHeaders(bindingContext, msg);
        }
        return t.createOutputStreamContext(bindingContext);            
    }
    protected boolean isFault(ObjectMessageContext objCtx, MessageContext bindingContext) {
        if (bindingContext instanceof SOAPMessageContext) {
            SOAPMessage msg = ((SOAPMessageContext)bindingContext).getMessage();
            try {
                return msg.getSOAPPart().getEnvelope().getBody().hasFault();
            } catch (SOAPException e) {
                return false;
            }
        }
        return super.isFault(objCtx, bindingContext);
    }

    
    protected MessageContext createBindingMessageContext(MessageContext orig) {
        return new SOAPMessageContextImpl(orig);
    }

    protected void marshal(ObjectMessageContext objContext, MessageContext context) {
        try {
            SOAPMessage msg = soapBinding
                .marshalMessage(objContext,
                                context,
                                sbeCallback.createDataBindingCallback(objContext,
                                                                      DataBindingCallback.Mode.PARTS));
            ((SOAPMessageContext)context).setMessage(msg);
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_MARSHALLING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }

    protected void marshalFault(ObjectMessageContext objContext, MessageContext context) {
        SOAPMessage msg = soapBinding
            .marshalFault(objContext, context,
                          sbeCallback.createDataBindingCallback(objContext,
                                                                DataBindingCallback.Mode.PARTS));
        ((SOAPMessageContext)context).setMessage(msg);
    }
    
    protected void unmarshal(MessageContext context, ObjectMessageContext objContext) {
        try {
            soapBinding.unmarshalMessage(context,
                                         objContext,
                                         sbeCallback
                                             .createDataBindingCallback(objContext,
                                                                        DataBindingCallback.Mode.PARTS));
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_UNMARSHALLING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }
    
    protected void write(MessageContext context, OutputStreamMessageContext outCtx) throws IOException {
        
        SOAPMessageContext soapCtx = (SOAPMessageContext)context;
        try {
            OutputStream out = outCtx.getOutputStream();
            soapCtx.getMessage().writeTo(out);
            out.flush();
            
            if (LOG.isLoggable(Level.FINE)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                soapCtx.getMessage().writeTo(baos);
                LOG.log(Level.FINE, baos.toString());    
            }
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_WRITE_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }
    
    protected void read(InputStreamMessageContext instr, MessageContext mc) throws IOException {

        try {
            soapBinding.parseMessage(instr.getInputStream(), mc);
        } catch (Exception se) {
            LOG.log(Level.SEVERE, "SOAP_PARSING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }
    
    protected MessageContext invokeOnProvider(MessageContext requestCtx, ServiceMode mode) {
        SOAPMessageContext soapCtx = (SOAPMessageContext)requestCtx;
        SOAPMessage msg = soapCtx.getMessage();
        
        if (Service.Mode.MESSAGE == mode.value()) {
            return invokeOnProvider(msg, soapCtx);
        }
        
        SOAPBody body = null;
        try {
            body = msg.getSOAPBody();
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "SOAP_BODY_RETREIVAL_FAILURE_MSG", ex);
        }
        return invokeOnProvider(body, soapCtx);
    }
    
    
    @SuppressWarnings("unchecked")
    MessageContext invokeOnProvider(SOAPMessage msg, SOAPMessageContext soapCtx) {
        Provider<SOAPMessage> provider = (Provider<SOAPMessage>)getEndpoint().getImplementor();
        SOAPMessage replyMsg = provider.invoke(msg);
        SOAPMessageContextImpl replyCtx = new SOAPMessageContextImpl(soapCtx);
        replyCtx.setMessage(replyMsg);
        return replyCtx;
    }
    
    @SuppressWarnings("unchecked")
    MessageContext invokeOnProvider(SOAPBody body, SOAPMessageContext soapCtx) {
        
        try {
            Document document = body.extractContentAsDocument();
            Source request = new DOMSource(document);
            
            Provider<Source> provider = (Provider<Source>)getEndpoint().getImplementor();
            Source reply = provider.invoke(request);
            assert null != reply;
            SOAPMessageContext replyCtx = new SOAPMessageContextImpl(soapCtx);
            assert null != replyCtx;         
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "SOAP_BODY_PROVIDER_FAILURE_MSG", ex);
            throw new ProtocolException(ex);
        }
        
        return null;
    }
    
    protected QName getOperationName(MessageContext ctx) {
        
        QName ret = null;         
        try { 
            SOAPMessageContext soapContext = SOAPMessageContext.class.cast(ctx);
            SOAPMessage msg = soapContext.getMessage();
            Node node = msg.getSOAPBody().getFirstChild();
            ret = new QName(node.getNamespaceURI(), node.getLocalName());
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "OPERATION_NAME_RETREIVAL_FAILURE_MSG", ex);
            throw new ProtocolException(ex);
        }
        
        LOG.info("retrieved operation name from soap message:" + ret);
        return ret;
    }
}

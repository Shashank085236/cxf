package org.objectweb.celtix.bus.bindings.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Endpoint;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bindings.AbstractBindingImpl;
import org.objectweb.celtix.bindings.AbstractServerBinding;
import org.objectweb.celtix.bindings.ServerBindingEndpointCallback;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.helpers.NodeUtils;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.TransportFactory;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

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
    
    public AbstractBindingImpl getBindingImpl() {
        return soapBinding;
    }

    protected ServerTransport createTransport(EndpointReferenceType ref) throws WSDLException, IOException {
        
        try {
            Port port = EndpointReferenceUtils.getPort(bus.getWSDLManager(), ref);
            List<?> exts = port.getExtensibilityElements();
            if (exts.size() > 0) {                
                ExtensibilityElement el = (ExtensibilityElement)exts.get(0);
                TransportFactory tf = 
                    bus.getTransportFactoryManager().
                        getTransportFactory(el.getElementType().getNamespaceURI());
                return tf.createServerTransport(ref);
            }
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
            try {
                soapBinding.updateHeaders(bindingContext, msg);
            } catch (SOAPException ex) {
                IOException io = new IOException(ex.getMessage());
                io.initCause(ex);
                throw io;
            }
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

    /*
    protected void marshal(ObjectMessageContext objContext, MessageContext context) {
        try {
            DataBindingCallback.Mode mode = sbeCallback.getServiceMode();
            SOAPMessage msg = soapBinding
                .marshalMessage(objContext,
                                context,
                                sbeCallback.createDataBindingCallback(objContext, mode));
            ((SOAPMessageContext)context).setMessage(msg);
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_MARSHALLING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }
    */

    /*
    protected void marshalFault(ObjectMessageContext objContext, MessageContext context) {
        DataBindingCallback.Mode mode = sbeCallback.getServiceMode();
        SOAPMessage msg = soapBinding
            .marshalFault(objContext, context,
                          sbeCallback.createDataBindingCallback(objContext, mode));
        ((SOAPMessageContext)context).setMessage(msg);
    }
    */
    /*
    protected void unmarshal(MessageContext context, ObjectMessageContext objContext) {
        try {
            DataBindingCallback.Mode mode = sbeCallback.getServiceMode();
            soapBinding.unmarshalMessage(context,
                                         objContext,
                                         sbeCallback
                                             .createDataBindingCallback(objContext, mode));
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_UNMARSHALLING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
    }
    */
    
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
    
    protected QName getOperationName(MessageContext ctx) {
        
        QName ret = null;         
        try { 
            SOAPMessageContext soapContext = SOAPMessageContext.class.cast(ctx);
            SOAPMessage msg = soapContext.getMessage();
            Node node = NodeUtils.getChildElementNode(msg.getSOAPBody());

            ret = new QName(node.getNamespaceURI(), node.getLocalName());
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "OPERATION_NAME_RETREIVAL_FAILURE_MSG", ex);
            throw new ProtocolException(ex);
        }
        
        LOG.info("retrieved operation name from soap message:" + ret);
        return ret;
    }
}

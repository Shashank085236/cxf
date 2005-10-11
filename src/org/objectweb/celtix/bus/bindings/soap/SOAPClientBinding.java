package org.objectweb.celtix.bus.bindings.soap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.WSDLException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bindings.AbstractClientBinding;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;


public class SOAPClientBinding extends AbstractClientBinding {
    private static final Logger LOG = Logger.getLogger(SOAPClientBinding.class.getName());
    protected final SOAPBindingImpl soapBinding;
    
    public SOAPClientBinding(Bus b, EndpointReferenceType ref) throws WSDLException, IOException {
        super(b, ref);
        soapBinding = new SOAPBindingImpl();
    }
    
    public Binding getBinding() {
        return soapBinding;
    }
    
    public boolean isCompatibleWithAddress(String address) {
        return soapBinding.isCompatibleWithAddress(address);
    }

    protected MessageContext createBindingMessageContext(MessageContext ctx) {
        return new SOAPMessageContextImpl(ctx);
    }

    protected void marshal(ObjectMessageContext objContext, MessageContext context) {
        try {
            SOAPMessage msg = soapBinding.marshalMessage(objContext, context);
            ((SOAPMessageContext)context).setMessage(msg);
        } catch (SOAPException se) {
            //TODO
            LOG.log(Level.INFO, se.getMessage(), se);
        }
    }

    protected void unmarshal(MessageContext context, ObjectMessageContext objContext) {
        try {
            soapBinding.unmarshalMessage(context, objContext);
        } catch (SOAPException se) {
            // TODO - handle exceptions
            LOG.log(Level.INFO, se.getMessage(), se);
        }
    }

    protected void write(MessageContext context, 
            OutputStreamMessageContext outCtx) throws IOException {
        SOAPMessageContext soapCtx = (SOAPMessageContext)context;
        try {
            soapCtx.getMessage().writeTo(outCtx.getOutputStream());
        } catch (SOAPException se) {
            throw new IOException(se.getMessage());
        }
    }

    protected void read(InputStreamMessageContext inCtx,
            MessageContext context) throws IOException {
        try {
            soapBinding.parseMessage(inCtx.getInputStream(), context);
        } catch (SOAPException se) {
            throw new IOException(se.getMessage());
        }
    }
}

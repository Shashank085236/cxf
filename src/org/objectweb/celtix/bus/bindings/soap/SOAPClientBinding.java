package org.objectweb.celtix.bus.bindings.soap;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bindings.GenericClientBinding;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;


public class SOAPClientBinding extends GenericClientBinding {
    private static Logger logger = Logger.getLogger(SOAPClientBinding.class.getName());
    protected final SOAPBindingImpl soapBinding;
    
    public SOAPClientBinding(Bus b, EndpointReferenceType ref) {
        super(b, ref);
        soapBinding = new SOAPBindingImpl();
    }
    
    public Binding getBinding() {
        return soapBinding;
    }
    
    public boolean isCompatibleWithAddress(String address) {
        return soapBinding.isCompatibleWithAddress(address);
    }

    protected MessageContext createBindingMessageContext() {
        return new SOAPMessageContextImpl();
    }

    protected void marshal(ObjectMessageContext objContext, MessageContext context) {
        try {
            SOAPMessage msg = soapBinding.marhsalMessage(objContext, context);
            ((SOAPMessageContext)context).setMessage(msg);
        } catch (SOAPException se) {
            //TODO
        }
    }

    protected void unmarshal(MessageContext context, ObjectMessageContext objContext) {
        //TODO UnMarshall SAAJ to Objects using JAXB
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

    protected void read(InputStreamMessageContext instr, 
            MessageContext mc) throws IOException {
        //TODO Read Stream into SOAP Message using SAAJ API
    }
}

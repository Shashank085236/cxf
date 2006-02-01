package org.objectweb.celtix.bus.bindings.soap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.WSDLException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.bindings.AbstractBindingImpl;
import org.objectweb.celtix.bindings.AbstractClientBinding;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;


public class SOAPClientBinding extends AbstractClientBinding {
    private static final Logger LOG = LogUtils.getL7dLogger(SOAPClientBinding.class);
    protected final SOAPBindingImpl soapBinding;
    
    public SOAPClientBinding(Bus b, EndpointReferenceType ref) throws WSDLException, IOException {
        super(b, ref);
        soapBinding = new SOAPBindingImpl(false);
    }
    
    public AbstractBindingImpl getBindingImpl() {
        return soapBinding;
    }  
    
    protected OutputStreamMessageContext createOutputStreamContext(MessageContext bindingContext)
        throws IOException {
        SOAPMessage msg = ((SOAPMessageContext)bindingContext).getMessage();
        try {
            soapBinding.updateHeaders(bindingContext, msg);
        } catch (SOAPException ex) {
            IOException io = new IOException(ex.getMessage());
            io.initCause(ex);
            throw io;
        }
        return super.createOutputStreamContext(bindingContext);
    }

    protected boolean hasFault(MessageContext context) {
        SOAPMessage msg = ((SOAPMessageContext)context).getMessage();
        assert msg != null;
        boolean hasFault = false;
        try {
            hasFault = msg.getSOAPBody().hasFault();
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "SOAP_UNMARSHALLING_FAILURE_MSG", se);
            throw new ProtocolException(se);
        }
        
        return hasFault;
    }
}

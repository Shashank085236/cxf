package org.objectweb.celtix.bus.bindings.soap;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
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
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.TransportFactory;

public class SOAPServerBinding extends AbstractServerBinding {
    
    private static final Logger LOG = Logger.getLogger(SOAPServerBinding.class.getName());
    
    protected final SOAPBindingImpl soapBinding;
    
    public SOAPServerBinding(Bus b, EndpointReferenceType ref, Endpoint ep) {
        super(b, ref, ep);
        soapBinding = new SOAPBindingImpl();
    }
    
    public Binding getBinding() {
        return soapBinding;
    }
    
    public boolean isCompatibleWithAddress(String address) {
        return soapBinding.isCompatibleWithAddress(address);
    }
    
    protected ServerTransport createTransport(EndpointReferenceType ref) throws WSDLException, IOException {
        // TODO get from configuration
        // TODO get from reference bindingID
        try {
            TransportFactory tf = 
                    bus.getTransportFactoryManager().getTransportFactory(SOAPConstants.SOAP_URI);
            return tf.createServerTransport(ref);
        } catch (BusException ex) {
            LOG.severe("Failed to get default transport factory for SOAP server binding.");
        }
        return null;
    }
    
    protected MessageContext createBindingMessageContext(MessageContext orig) {
        return new SOAPMessageContextImpl(orig);
    }
    
    protected void marshal(ObjectMessageContext objContext, MessageContext context) {
        try {
            SOAPMessage msg = soapBinding.marshalMessage(objContext, context);
            ((SOAPMessageContext)context).setMessage(msg);
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "Error in marshall of SOAP Message", se);
        }
    }
    
    protected void unmarshal(MessageContext context, ObjectMessageContext objContext) {
        //super.unmarshal(context,  objContext);
        try {
            soapBinding.unmarshalMessage(context, objContext);
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "error in unmarshall of SOAP Message", se);
        }
    }
    
    protected void write(MessageContext context, OutputStreamMessageContext outCtx) throws IOException {
        
        SOAPMessageContext soapCtx = (SOAPMessageContext)context;
        try {
            soapCtx.getMessage().writeTo(outCtx.getOutputStream());
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "error in marshall of SOAP Message", se);
            throw new IOException(se.getMessage());
        }
    }
    
    protected void read(InputStreamMessageContext instr, MessageContext mc) throws IOException {
        //REVISIT InputStreamMessageContext should be copied to MessageContext
        try {
            soapBinding.parseInputMessage(instr.getInputStream(), mc);
        } catch (SOAPException se) {
            LOG.log(Level.SEVERE, "error while parsing input message", se);
            throw new IOException(se.getMessage());
        }          
    }
    
    protected MessageContext invokeOnProvider(MessageContext requestCtx, ServiceMode mode)
        throws RemoteException {
        SOAPMessageContext soapCtx = (SOAPMessageContext)requestCtx;
        SOAPMessage msg = soapCtx.getMessage();
        
        if (Service.Mode.MESSAGE == mode.value()) {
            return invokeOnProvider(msg, soapCtx);
        }
        
        SOAPBody body = null;
        try {
            body = msg.getSOAPBody();
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "Failed to obtain SOAP body.", ex);
        }
        return invokeOnProvider(body, soapCtx);
    }
    
    
    @SuppressWarnings("unchecked")
    MessageContext invokeOnProvider(SOAPMessage msg, SOAPMessageContext soapCtx) throws RemoteException {
        Provider<SOAPMessage> provider = (Provider<SOAPMessage>)getEndpoint().getImplementor();
        SOAPMessage replyMsg = provider.invoke(msg);
        SOAPMessageContextImpl replyCtx = new SOAPMessageContextImpl(soapCtx);
        replyCtx.setMessage(replyMsg);
        return replyCtx;
    }
    
    @SuppressWarnings("unchecked")
    MessageContext invokeOnProvider(SOAPBody body, SOAPMessageContext soapCtx) throws RemoteException {
        
        try {
            Document document = body.extractContentAsDocument();
            Source request = new DOMSource(document);
            
            Provider<Source> provider = (Provider<Source>)getEndpoint().getImplementor();
            Source reply = provider.invoke(request);
            assert null != reply;
            SOAPMessageContext replyCtx = new SOAPMessageContextImpl(soapCtx);
            assert null != replyCtx;
            
            // ...
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "Failed to pass SOAPBody to/from provider.", ex);
        }
        
        return null;
    }
    
    
    protected QName getOperationName(MessageContext ctx) {
        
        QName ret = null;         
        try { 
            Class<?> implementorClass = endpoint.getImplementor().getClass(); 
            SOAPBinding binding = getBindingAnnotationFromClass(implementorClass);
            
            
            if (null == binding
                || (binding.style() == Style.DOCUMENT && binding.parameterStyle() == ParameterStyle.WRAPPED
                && binding.use() == SOAPBinding.Use.LITERAL)) {
                
                SOAPMessageContext soapContext = SOAPMessageContext.class.cast(ctx);
                SOAPMessage msg = soapContext.getMessage();
                Node node = msg.getSOAPBody().getFirstChild();
                ret = new QName(node.getNamespaceURI(), node.getLocalName());
            } else { 
                LOG.severe("attempting to get operation name from soap message I do not understand");
            }
        } catch (SOAPException ex) {
            LOG.log(Level.SEVERE, "error getting operation name from soap message", ex);
        }
        
        LOG.info("retrieved operation name from soap message:" + ret);
        return ret;
    }
    
    private SOAPBinding getBindingAnnotationFromClass(Class<?> clz) {
        
        SOAPBinding annotation = null;
        
        for (Class<?> iface : clz.getInterfaces()) {
            if ((annotation = iface.getAnnotation(SOAPBinding.class)) != null) {
                break;
            }
        }
        return annotation;     
    }    
}

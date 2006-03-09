package org.objectweb.celtix.bus.ws.rm;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.objectweb.celtix.bindings.AbstractBindingBase;
import org.objectweb.celtix.bindings.AbstractClientBinding;
import org.objectweb.celtix.bindings.AbstractServerBinding;
import org.objectweb.celtix.bus.jaxws.EndpointImpl;
import org.objectweb.celtix.bus.jaxws.ServiceImpl;
import org.objectweb.celtix.bus.ws.addressing.AddressingPropertiesImpl;
import org.objectweb.celtix.bus.ws.addressing.ContextUtils;
import org.objectweb.celtix.bus.ws.addressing.VersionTransformer;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.configuration.ConfigurationBuilder;
import org.objectweb.celtix.configuration.ConfigurationBuilderFactory;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.handlers.SystemHandler;
import org.objectweb.celtix.transports.ClientTransport;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.ws.addressing.AddressingProperties;
import org.objectweb.celtix.ws.addressing.AttributedURIType;
import org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType;
import org.objectweb.celtix.ws.rm.CreateSequenceResponseType;
import org.objectweb.celtix.ws.rm.CreateSequenceType;
import org.objectweb.celtix.ws.rm.SequenceAcknowledgement;
import org.objectweb.celtix.ws.rm.SequenceType;
import org.objectweb.celtix.ws.rm.TerminateSequenceType;
import org.objectweb.celtix.ws.rm.wsdl.SequenceFault;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public class RMHandler implements LogicalHandler<LogicalMessageContext>, SystemHandler {
       
    public static final String RM_CONFIGURATION_URI = 
        "http://celtix.objectweb.org/bus/ws/rm/rm-config";
    public static final String RM_CONFIGURATION_ID = "rm-handler";
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMHandler.class);
    
    private RMSource source;
    private RMDestination destination;
    private RMProxy proxy;
    private RMServant servant;
    private Configuration configuration;
    
    private AbstractClientBinding clientBinding;
    private AbstractServerBinding serverBinding;
    private ClientTransport clientTransport;
    private ServerTransport serverTransport;
    
    public RMHandler() {
        proxy = new RMProxy(this);
        servant = new RMServant();
    }

    public void close(MessageContext context) {
        // TODO commit transaction        
    }

    public boolean handleFault(LogicalMessageContext context) {
        
        open(context);
        return false;
    }

    public boolean handleMessage(LogicalMessageContext context) {
        
        open(context);
        
        if (ContextUtils.isOutbound(context)) {
            handleOutbound(context);
        } else {
            handleInbound(context);
        }
        return true;
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    public ClientTransport getClientTransport() {
        return clientTransport;
    }
    
    public ServerTransport getServerTransport() {
        return serverTransport;
    }
    
    public AbstractClientBinding getClientBinding() {
        return clientBinding;
    }
    
    public AbstractServerBinding getServerBinding() {
        return serverBinding;
    }
    
    public AbstractBindingBase getBinding() {
        if (null != clientBinding) {
            return clientBinding;
        }
        return serverBinding;
    }
    
    
    private void open(LogicalMessageContext context) {
   
        initialise(context);
        
        // TODO begin transaction
    }
    
    private void initialise(MessageContext context) {
        if (ContextUtils.isOutbound(context)) {
            if (ContextUtils.isRequestor(context) && null == source) {
                source = new RMSource(this);
            }
        } else {
            if (!ContextUtils.isRequestor(context) && null == destination) {
                destination = new RMDestination(this);
            }
        } 
        
        if (null == clientTransport && null == serverTransport) {
            clientTransport = ContextUtils.retrieveClientTransport(context);
            if (null == clientTransport) {
                serverTransport = ContextUtils.retrieveServerTransport(context);
            }
        }
        
        if (null == clientBinding && null != clientTransport) {
            clientBinding = (AbstractClientBinding)
                ContextUtils.retrieveClientBinding(context);
        }
        
        if (null == serverBinding && null != serverTransport) {
            serverBinding = (AbstractServerBinding)
                ContextUtils.retrieveServerBinding(context);
        }
        
        if (null == configuration) {
            configuration = createConfiguration(context);
        }
    }
    
    private Configuration createConfiguration(MessageContext context) {
        Configuration busCfg = getBinding().getBus().getConfiguration();
        ConfigurationBuilder builder = ConfigurationBuilderFactory.getBuilder();
        Configuration parent;
        org.objectweb.celtix.ws.addressing.EndpointReferenceType ref = getBinding().getEndpointReference();
        
        if (ContextUtils.isRequestor(context)) {
            Configuration serviceCfg = builder.getConfiguration(
                ServiceImpl.SERVICE_CONFIGURATION_URI,
                EndpointReferenceUtils.getServiceName(ref).toString(),
                busCfg);
            parent = builder.getConfiguration(
                ServiceImpl.PORT_CONFIGURATION_URI,
                EndpointReferenceUtils.getPortName(ref),
                serviceCfg);            
        } else {
            parent = builder.getConfiguration(
                EndpointImpl.ENDPOINT_CONFIGURATION_URI,
                EndpointReferenceUtils.getServiceName(ref).toString(),
                busCfg);
        }
        
        Configuration cfg = builder.getConfiguration(RM_CONFIGURATION_URI, RM_CONFIGURATION_ID, parent);
        if (null == cfg) {
            cfg = builder.buildConfiguration(RM_CONFIGURATION_URI, RM_CONFIGURATION_ID, parent);
        }
        return cfg;
        
    }
    
    private void handleOutbound(LogicalMessageContext context) {
        AddressingPropertiesImpl maps = 
            ContextUtils.retrieveMAPs(context, false, true);
        if (maps != null) {
            // ensure the appropriate version of WS-Addressing is used
            maps.exposeAs(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
        }

        // nothing to do if this is a CreateSequence, TerminateSequence or SequenceInfo request
        
        String action = ContextUtils.getAction(context).getValue();
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }
        
        if (RMUtils.getRMConstants().getCreateSequenceAction().equals(action)
            || RMUtils.getRMConstants().getCreateSequenceResponseAction().equals(action)
            || RMUtils.getRMConstants().getTerminateSequenceAction().equals(action)
            || RMUtils.getRMConstants().getSequenceInfoAction().equals(action)) {
            return;
        }
        
        Sequence seq = source.getCurrent();
        if (null == seq) {
            // TODO: better error handling
            try {
                proxy.createSequence(source);
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (SequenceFault ex) {
                ex.printStackTrace();
            }
                
            seq = source.getCurrent();       
        }
        assert null != seq;
        
        // store a sequence type object in the context (incl. getting the next sequence number),
        
        SequenceType s = RMUtils.getWSRMFactory().createSequenceType();
        s.setIdentifier(seq.getIdentifier());
        s.setMessageNumber(seq.nextMessageNumber());   
        if (seq.isLastMessage()) {
            s.setLastMessage(new SequenceType.LastMessage());
        }
        RMContextUtils.storeSequence(context, s);
        
        // tell the source to store a copy of the message in the retransmission
        // queue and schedule the next retransmission
        
        source.addUnacknowledged(context);
        
        
        
        // add Acknowledgements       
        
    }
    
    private void handleInbound(LogicalMessageContext context) {
        
        LOG.entering(getClass().getName(), "handleInbound");
        
        // nothing to do if this is a response to a CreateSequence request
        String action = ContextUtils.getAction(context).getValue();
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }
        
        if (RMUtils.getRMConstants().getCreateSequenceResponseAction().equals(action)) {
            return;
        } else if (RMUtils.getRMConstants().getCreateSequenceAction().equals(action)) {
            Object[] parameters = (Object[])context.get(ObjectMessageContext.METHOD_PARAMETERS);            
            CreateSequenceType cs = (CreateSequenceType)parameters[0];
            LOG.fine("ContextUtils.retrieveTo returns: " + ContextUtils.retrieveTo(context));
            EndpointReferenceType to = 
                VersionTransformer.convert(ContextUtils.retrieveTo(context));
             
            try {
                LOG.fine("dispatching createSequence request to rm servant ...");
                CreateSequenceResponseType csr = servant.createSequence(destination, cs, to);
                context.put(ObjectMessageContext.METHOD_RETURN, csr);
                LOG.fine("Inserted createSequenceResponse into object context");
            } catch (SequenceFault ex) {
                // ignore for now
                ex.printStackTrace();
            }
   
            AddressingProperties maps = ContextUtils.retrieveMAPs(context, false, false);
            AttributedURIType actionURI =
                ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
            actionURI.setValue(RMUtils.getRMConstants().getCreateSequenceResponseAction());
            maps.setAction(actionURI);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Changed action in context from: " + action 
                         + " to: " + ContextUtils.getAction(context).getValue());
            }
            return;
        } else if (RMUtils.getRMConstants().getTerminateSequenceAction().equals(action)) {
            Object[] parameters = (Object[])context.get(ObjectMessageContext.METHOD_PARAMETERS);            
            TerminateSequenceType cs = (TerminateSequenceType)parameters[0];

            try {
                servant.terminateSequence(destination, cs.getIdentifier());
            } catch (SequenceFault ex) {
                // ignore for now
            }
        } 
        
        // for application AMD out of band messages

        processAcknowledgments(context);
        
        processSequence(context);   
    }
    
    private void processAcknowledgments(LogicalMessageContext context) {
        List<SequenceAcknowledgement> acks = RMContextUtils.retrieveAcknowledgments(context);
        if (null != acks) {
            for (SequenceAcknowledgement ack : acks) {
                source.setAcknowledged(ack);
            }
        }      
    }
    
    private void processSequence(LogicalMessageContext context) {
        SequenceType s = RMContextUtils.retrieveSequence(context);
        destination.acknowledge(s);
    }
    
}

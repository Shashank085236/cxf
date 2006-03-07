package org.objectweb.celtix.bus.ws.addressing;



import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Port;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebFault;
import javax.xml.ws.handler.MessageContext;
import static javax.xml.ws.handler.MessageContext.MESSAGE_OUTBOUND_PROPERTY;

import org.objectweb.celtix.bindings.ClientBinding;
import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.ServerBinding;
import org.objectweb.celtix.bus.jaxws.JAXBDataBindingCallback;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ClientTransport;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.ws.addressing.AddressingProperties;
import org.objectweb.celtix.ws.addressing.AttributedURIType;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.ws.addressing.ObjectFactory;
import org.objectweb.celtix.ws.addressing.RelatesToType;

import static org.objectweb.celtix.context.ObjectMessageContext.CORRELATION_IN;
import static org.objectweb.celtix.context.ObjectMessageContext.CORRELATION_OUT;
import static org.objectweb.celtix.context.ObjectMessageContext.REQUESTOR_ROLE_PROPERTY;
import static org.objectweb.celtix.context.OutputStreamMessageContext.ONEWAY_MESSAGE_TF;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.BINDING_PROPERTY;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND;
import static org.objectweb.celtix.ws.addressing.JAXWSAConstants.TRANSPORT_PROPERTY;


/**
 * Holder for utility methods relating to contexts.
 */
public final class ContextUtils {

    public static final ObjectFactory WSA_OBJECT_FACTORY = new ObjectFactory();

    private static final String WS_ADDRESSING_PACKAGE = 
        EndpointReferenceType.class.getPackage().getName();
    private static final Logger LOG = LogUtils.getL7dLogger(ContextUtils.class);

    private static JAXBContext jaxbContext;
     
    /**
     * Used by MAPAggregator to cache bad MAP fault name
     */
    private static final String MAP_FAULT_NAME_PROPERTY = 
        "org.objectweb.celtix.ws.addressing.map.fault.name";

    /**
     * Used by MAPAggregator to cache bad MAP fault reason
     */
    private static final String MAP_FAULT_REASON_PROPERTY = 
        "org.objectweb.celtix.ws.addressing.map.fault.reason";
 
   /**
    * Prevents instantiation.
    */
    private ContextUtils() {
    }

   /**
    * Determine if context indicates message is outbound.
    *
    * @param context the current MessageContext
    * @return true iff the message direction is outbound
    */
    public static boolean isOutbound(MessageContext context) {
        Boolean outbound = (Boolean)context.get(MESSAGE_OUTBOUND_PROPERTY);
        return outbound != null && outbound.booleanValue();
    }

   /**
    * Determine if context indicates current messaging role is that of
    * requestor.
    *
    * @param context the current MessageContext
    * @return true iff the current messaging role is that of requestor
    */
    public static boolean isRequestor(MessageContext context) {
        Boolean requestor = (Boolean)context.get(REQUESTOR_ROLE_PROPERTY);
        return requestor != null && requestor.booleanValue();
    }

    /**
     * Determine if context indicates current invocation is oneway.
     *
     * @param context the current MessageContext
     * @return true iff the current invocation is oneway
     */
    public static boolean isOneway(MessageContext context) {
        Boolean oneway = (Boolean)context.get(ONEWAY_MESSAGE_TF);
        return oneway != null && oneway.booleanValue();
    }

    /**
     * Get appropriate context property name for message addressing properties.
     *
     * @param isProviderContext true if the binding provider request context 
     * available to the client application as opposed to the message context 
     * visible to handlers
     * @param isRequestor true iff the current messaging role is that of
     * requestor
     * @param isOutbound true iff the message is outbound
     * @return the property name to use when caching the MAPs in the context
     */
    public static String getMAPProperty(boolean isRequestor, 
                                        boolean isProviderContext,
                                        boolean isOutbound) {
        return isRequestor
                ? isProviderContext
                 ? CLIENT_ADDRESSING_PROPERTIES
                 : isOutbound
                   ? CLIENT_ADDRESSING_PROPERTIES_OUTBOUND
                   : CLIENT_ADDRESSING_PROPERTIES_INBOUND
               : isOutbound
                 ? SERVER_ADDRESSING_PROPERTIES_OUTBOUND
                 : SERVER_ADDRESSING_PROPERTIES_INBOUND;
    }
    
    /**
     * Get appropriate context property name for correlation ID.
     *
     * @param isOutbound true iff the message is outbound
     * @return the property name to use when caching the 
     * correlation ID in the context
     */
    public static String getCorrelationIDProperty(boolean isOutbound) {
        return isOutbound ? CORRELATION_OUT : CORRELATION_IN;
    }


    /**
     * Store MAPs in the context.
     *
     * @param context the message context
     * @param isOutbound true iff the message is outbound
     */
    public static void storeMAPs(AddressingProperties maps,
                                 MessageContext context,
                                 boolean isOutbound) {
        storeMAPs(maps, context, isOutbound, isRequestor(context), true, false);
    }

    /**
     * Store MAPs in the context.
     *
     * @param maps the MAPs to store
     * @param context the message context
     * @param isOutbound true iff the message is outbound
     * @param isRequestor true iff the current messaging role is that of
     * requestor
     * @param handler true if HANDLER scope, APPLICATION scope otherwise
     */
    public static void storeMAPs(AddressingProperties maps,
                                 MessageContext context,
                                 boolean isOutbound, 
                                 boolean isRequestor,
                                 boolean handler) {
        storeMAPs(maps, context, isOutbound, isRequestor, handler, false);
    }
    
    /**
     * Store MAPs in the context.
     *
     * @param maps the MAPs to store
     * @param context the message context
     * @param isOutbound true iff the message is outbound
     * @param isRequestor true iff the current messaging role is that of
     * requestor
     * @param handler true if HANDLER scope, APPLICATION scope otherwise
     * @param isProviderContext true if the binding provider request context 
     */
    public static void storeMAPs(AddressingProperties maps,
                                 MessageContext context,
                                 boolean isOutbound, 
                                 boolean isRequestor,
                                 boolean handler,
                                 boolean isProviderContext) {
        if (maps != null) {
            String mapProperty = getMAPProperty(isRequestor, isProviderContext, isOutbound);
            LOG.log(Level.INFO,
                    "associating MAPs with context property {0}",
                    mapProperty);
            context.put(mapProperty, maps);
            context.setScope(mapProperty, 
                             handler
                             ? MessageContext.Scope.HANDLER
                             : MessageContext.Scope.APPLICATION);
        }
    }


    /**
     * @param context the message context
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true iff the message is outbound
     * @return the current addressing properties
     */
    public static AddressingPropertiesImpl retrieveMAPs(
                                                   MessageContext context, 
                                                   boolean isProviderContext,
                                                   boolean isOutbound) {
        boolean isRequestor = ContextUtils.isRequestor(context);
        String mapProperty =
            ContextUtils.getMAPProperty(isProviderContext, 
                                        isRequestor,
                                        isOutbound);
        LOG.log(Level.INFO,
                "retrieving MAPs from context property {0}",
                mapProperty);
        AddressingPropertiesImpl maps =
            (AddressingPropertiesImpl)context.get(mapProperty);
        if (maps != null) {
            LOG.log(Level.INFO, "current MAPs {0}", maps);
        } else if (!isProviderContext) {
            LOG.warning("MAPS_RETRIEVAL_FAILURE_MSG");
        }
        return maps;
    }

    /**
     * Helper method to get an attributed URI.
     *
     * @param uri the URI
     * @return an AttributedURIType encapsulating the URI
     */
    public static AttributedURIType getAttributedURI(String uri) {
        AttributedURIType attributedURI = 
            WSA_OBJECT_FACTORY.createAttributedURIType();
        attributedURI.setValue(uri);
        return attributedURI;
    }

    /**
     * Helper method to get a RealtesTo instance.
     *
     * @param uri the related URI
     * @return a RelatesToType encapsulating the URI
     */
    public static RelatesToType getRelatesTo(String uri) {
        RelatesToType relatesTo =
            WSA_OBJECT_FACTORY.createRelatesToType();
        relatesTo.setValue(uri);
        return relatesTo;
    }

    /**
     * Helper method to determine if an EPR address is generic (either null,
     * none or anonymous).
     *
     * @param ref the EPR under test
     * @return true iff the address is generic
     */
    public static boolean isGenericAddress(EndpointReferenceType ref) {
        return ref == null 
               || ref.getAddress() == null
               || Names.WSA_ANONYMOUS_ADDRESS.equals(ref.getAddress().getValue())
               || Names.WSA_NONE_ADDRESS.equals(ref.getAddress().getValue());
    }

    /**
     * Helper method to determine if an MAPs Action is empty (a null action
     * is considered empty, whereas a zero length action suppresses
     * the propogation of the Action property).
     *
     * @param ref the MAPs Action under test
     * @return true iff the Action is empty
     */
    public static boolean hasEmptyAction(AddressingProperties maps) {
        boolean empty = maps.getAction() == null;
        if (maps.getAction() != null 
            && maps.getAction().getValue().length() == 0) {
            maps.setAction(null);
            empty = false;
        } 
        return empty;
    }

    /**
     * Retrieve ClientTransport from the context.
     *
     * @param context the message context
     * @returned the retrieved ClientTransport
     */
    public static ClientTransport retrieveClientTransport(MessageContext context) {
        Object transport = context.get(TRANSPORT_PROPERTY);
        return transport instanceof ClientTransport
               ? (ClientTransport)transport
               : null;
    }

    /**
     * Retrieve ServerTransport from the context.
     *
     * @param context the message context
     * @returned the retrieved ServerTransport
     */
    public static ServerTransport retrieveServerTransport(MessageContext context) {
        return (ServerTransport)context.get(TRANSPORT_PROPERTY);
    }
    
    /**
     * Retrieve ClientBinding from the context.
     *
     * @param context the message context
     * @returned the retrieved ClientBinding
     */
    public static ClientBinding retrieveClientBinding(MessageContext context) {
        return (ClientBinding)context.get(BINDING_PROPERTY);
    }

    /**
     * Retrieve ServerBinding from the context.
     *
     * @param context the message context
     * @returned the retrieved ServerBinding
     */
    public static ServerBinding retrieveServerBinding(MessageContext context) {
        return (ServerBinding)context.get(BINDING_PROPERTY);
    }

    /**
     * Rebase server transport on replyTo
     * 
     * @param inMAPs the incoming MAPs
     * @param context the message context
     */
    public static void rebaseTransport(AddressingProperties inMAPs,
                                       MessageContext context) {
        // ensure there is a MAPs instance available for the outbound
        // partial response that contains appropriate To and ReplyTo
        // properties (i.e. anonymous & none respectively)
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        maps.setTo(ContextUtils.getAttributedURI(Names.WSA_ANONYMOUS_ADDRESS));
        maps.setReplyTo(WSA_OBJECT_FACTORY.createEndpointReferenceType());
        maps.getReplyTo().setAddress(getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setAction(getAttributedURI(""));
        maps.exposeAs(inMAPs.getNamespaceURI());
        storeMAPs(maps, context, true, true, true, true);

        ServerBinding serverBinding = retrieveServerBinding(context);
        ServerTransport serverTransport = retrieveServerTransport(context);
        if (serverTransport != null && serverBinding != null) {
            try {
                OutputStreamMessageContext outputContext =
                    serverTransport.rebase(context, inMAPs.getReplyTo());
                if (outputContext != null) {
                    serverBinding.partialResponse(outputContext, 
                                                  getDataBindingCallback());
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "SERVER_TRANSPORT_REBASE_FAILURE_MSG", e);
            }
        }
    }

    /**
     * Retrieve WSDL Port from the context.
     *
     * @param context the message context
     * @returned the retrieved Port
     */
    public static Port retrievePort(MessageContext context) {
        ClientTransport transport = retrieveClientTransport(context);
        return transport != null ? transport.getPort() : null;
    }

    /**
     * Retrieve To EPR from the context.
     *
     * @param context the message context
     * @returned the retrieved EPR
     */
    public static EndpointReferenceType retrieveTo(MessageContext context) {
        ClientTransport transport = retrieveClientTransport(context);
        return transport != null ? transport.getTargetEndpoint() : null;
    }

    /**
     * Retrieve To EPR from the context.
     *
     * @param context the message context
     * @returned the retrieved EPR
     */
    public static EndpointReferenceType retrieveReplyTo(MessageContext context) {
        ClientTransport transport = retrieveClientTransport(context);
        EndpointReferenceType replyTo = null;
        try {
            replyTo = transport != null ? transport.getDecoupledEndpoint() : null;
        } catch (IOException ioe) {
            // ignore
        }
        return replyTo;
    }

    /**
     * Store bad MAP fault name in the context.
     *
     * @param faultName the fault name to store
     * @param context the message context
     */
    public static void storeMAPFaultName(String faultName, 
                                         MessageContext context) {
        context.put(MAP_FAULT_NAME_PROPERTY, faultName);
        context.setScope(MAP_FAULT_NAME_PROPERTY,
                         MessageContext.Scope.HANDLER);
    }

    /**
     * Retrieve MAP fault name from the context.
     *
     * @param context the message context
     * @returned the retrieved fault name
     */
    public static String retrieveMAPFaultName(MessageContext context) {
        return (String)context.get(MAP_FAULT_NAME_PROPERTY);
    }

    /**
     * Store MAP fault reason in the context.
     *
     * @param reason the fault reason to store
     * @param context the message context
     */
    public static void storeMAPFaultReason(String reason, 
                                           MessageContext context) {
        context.put(MAP_FAULT_REASON_PROPERTY, reason);
        context.setScope(MAP_FAULT_REASON_PROPERTY,
                         MessageContext.Scope.HANDLER);
    }

    /**
     * Retrieve MAP fault reason from the context.
     *
     * @param context the message context
     * @returned the retrieved fault reason
     */
    public static String retrieveMAPFaultReason(MessageContext context) {
        return (String)context.get(MAP_FAULT_REASON_PROPERTY);
    }

    /**
     * Store correlation ID in the context
     *
     * @param id the correlation ID
     * @param isOutbound true if message is outbound
     * @param context the message context
     */   
    public static void storeCorrelationID(RelatesToType id, 
                                          boolean isOutbound,
                                          MessageContext context) {
        storeCorrelationID(id.getValue(), isOutbound, context);
    }
    
    /**
     * Store correlation ID in the context
     *
     * @param id the correlation ID
     * @param isOutbound true if message is outbound
     * @param context the message context
     */   
    public static void storeCorrelationID(AttributedURIType id, 
                                          boolean isOutbound,
                                          MessageContext context) {
        storeCorrelationID(id.getValue(), isOutbound, context);
    }
    
    /**
     * Store correlation ID in the context
     *
     * @param id the correlation ID
     * @param isOutbound true if message is outbound
     * @param context the message context
     */   
    private static void storeCorrelationID(String id, 
                                           boolean isOutbound,
                                           MessageContext context) {
        context.put(getCorrelationIDProperty(isOutbound), id);
        context.setScope(getCorrelationIDProperty(isOutbound),
                         MessageContext.Scope.APPLICATION);
    }
    
    /**
     * Retrieve correlation ID from the context.
     *
     * @param context the message context
     * @param isOutbound true if message is outbound
     * @returned the retrieved correlation ID
     */
    public static String retrieveCorrelationID(MessageContext context, 
                                               boolean isOutbound) {
        return (String)context.get(getCorrelationIDProperty(isOutbound));
    }
    
    /**
     * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated
     * types.
     *
     * @return a JAXBContext 
     */
    public static JAXBContext getJAXBContext() throws JAXBException {
        synchronized (ContextUtils.class) {
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(WS_ADDRESSING_PACKAGE);
            }
        }
        return jaxbContext;
    }

    /**
     * Set the encapsulated JAXBContext (used by unit tests).
     * 
     * @param ctx JAXBContext 
     */
    public static void setJAXBContext(JAXBContext ctx) throws JAXBException {
        synchronized (ContextUtils.class) {
            jaxbContext = ctx;
        }
    }
    
    /**
     * Construct the Action URI.
     * 
     * @param context the message context
     * @return the Action URI
     */
    public static AttributedURIType getAction(MessageContext context) {
        String action = null;
        // REVISIT: add support for @{Fault}Action annotation (generated
        // from the wsaw:Action WSDL element)
        Throwable fault = 
            (Throwable)context.get(ObjectMessageContext.METHOD_FAULT);
        Method method = (Method)context.get(ObjectMessageContext.METHOD_OBJ);
        if (method != null) {
            if (fault != null) {
                WebFault webFault = fault.getClass().getAnnotation(WebFault.class);
                action = getAction(webFault.targetNamespace(),
                                   method, 
                                   webFault.name(),
                                   true);
            } else {
                if (ContextUtils.isRequestor(context)) {
                    RequestWrapper requestWrapper =
                        method.getAnnotation(RequestWrapper.class);
                    if (requestWrapper != null) {
                        action = getAction(requestWrapper.targetNamespace(),
                                           method,
                                           requestWrapper.localName(),
                                           false);
                    }
                } else {
                    ResponseWrapper responseWrapper =
                        method.getAnnotation(ResponseWrapper.class);
                    if (responseWrapper != null) {
                        action = getAction(responseWrapper.targetNamespace(),
                                           method,
                                           responseWrapper.localName(),
                                          false);
                    }
                }
            }
        }
        return action != null ? getAttributedURI(action) : null;
    }
        

    /**
     * Construct the Action string.
     *
     * @param targetNamespace the target namespace
     * @param method the method
     * @param localName the local name
     * @param isFault true if a fault
     * @return action string
     */
    private static String getAction(String targetNamespace, 
                                    Method method, 
                                    String localName,
                                    boolean isFault) {
        String action = null;
        action = targetNamespace;
        action += Names.WSA_ACTION_DELIMITER;
        action += method.getDeclaringClass().getSimpleName();
        if (isFault) {
            action += method.getName();
            action += Names.WSA_FAULT_DELIMITER;
        }
        action += Names.WSA_ACTION_DELIMITER;
        action += localName;
        return action;
    }

    /**
     * Get a DataBindingCallback (for use with an outgoing partial response).
     *
     * @return a DataBindingCallback
     */
    private static DataBindingCallback getDataBindingCallback() 
        throws JAXBException {
        return new JAXBDataBindingCallback(null,
                                           DataBindingCallback.Mode.PARTS,
                                           getJAXBContext());
    }
}












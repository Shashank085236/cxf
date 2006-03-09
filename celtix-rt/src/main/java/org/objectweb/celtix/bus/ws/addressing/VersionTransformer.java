package org.objectweb.celtix.bus.ws.addressing;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

// importation convention: if the same class name is used for 
// 2005/08 and 2004/08, then the former version is imported
// and the latter is fully qualified when used
import org.objectweb.celtix.ws.addressing.AttributedURIType;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.ws.addressing.ReferenceParametersType;
import org.objectweb.celtix.ws.addressing.RelatesToType;
import org.objectweb.celtix.ws.addressing.v200408.AttributedQName;
import org.objectweb.celtix.ws.addressing.v200408.AttributedURI;
import org.objectweb.celtix.ws.addressing.v200408.ObjectFactory;
import org.objectweb.celtix.ws.addressing.v200408.Relationship;
import org.objectweb.celtix.ws.addressing.v200408.ServiceNameType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;


/**
 * This class is responsible for transforming between the native 
 * WS-Addressing schema version (i.e. 2005/08) and exposed
 * version (currently may be 2005/08 or 2004/08).
 * <p>
 * The native version is that used throughout the stack, were the
 * WS-A types are represented via the JAXB generated types for the
 * 2005/08 schema.
 * <p>
 * The exposed version is that used when the WS-A types are 
 * externalized, i.e. are encoded in the headers of outgoing 
 * messages. For outgoing requests, the exposed version is 
 * determined from configuration. For outgoing responses, the
 * exposed version is determined by the exposed version of
 * the corresponding request.
 * <p>
 * The motivation for using different native and exposed types
 * is usually to facilitate a WS-* standard based on an earlier 
 * version of WS-Adressing (for example WS-RM depends on the
 * 2004/08 version).
 */
public class VersionTransformer {

    protected static final String NATIVE_VERSION = Names.WSA_NAMESPACE_NAME;
        
    /**
     * Constructor.
     */
    public VersionTransformer() {
    }
    
    /**
     * @param namespace a namspace URI to consider
     * @return true if th WS-Addressing version specified by the namespace 
     * URI is supported
     */
    public boolean isSupported(String namespace) {
        return NATIVE_VERSION.equals(namespace) 
               || Names200408.WSA_NAMESPACE_NAME.equals(namespace);
    }
    
    /**
     * Convert from 2005/08 AttributedURI to 2004/08 AttributedURI.
     * 
     * @param internal the 2005/08 AttributedURIType
     * @return an equivalent 2004/08 AttributedURI
     */
    public static AttributedURI convert(AttributedURIType internal) {
        AttributedURI exposed = 
            Names200408.WSA_OBJECT_FACTORY.createAttributedURI();
        String exposedValue =
            Names.WSA_ANONYMOUS_ADDRESS.equals(internal.getValue())
            ? Names200408.WSA_ANONYMOUS_ADDRESS 
            : Names.WSA_NONE_ADDRESS.equals(internal.getValue())
              ? Names200408.WSA_NONE_ADDRESS
              : internal.getValue();
        exposed.setValue(exposedValue);
        exposed.getOtherAttributes().putAll(internal.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert from 2004/08 AttributedURI to 2005/08 AttributedURI.
     * 
     * @param exposed the 2004/08 AttributedURI
     * @return an equivalent 2005/08 AttributedURIType
     */
    public static AttributedURIType convert(AttributedURI exposed) {
        AttributedURIType internal = 
            ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
        String internalValue =
            Names200408.WSA_ANONYMOUS_ADDRESS.equals(exposed.getValue())
            ? Names.WSA_ANONYMOUS_ADDRESS 
            : Names200408.WSA_NONE_ADDRESS.equals(exposed.getValue())
              ? Names.WSA_NONE_ADDRESS
              : exposed.getValue();
        internal.setValue(internalValue);
        internal.getOtherAttributes().putAll(exposed.getOtherAttributes());
        return internal; 
    }
    
    /**
     * Convert from 2005/08 EndpointReferenceType to 2004/08 
     * EndpointReferenceType.
     * 
     * @param internal the 2005/08 EndpointReferenceType
     * @return an equivalent 2004/08 EndpointReferenceType
     */
    public static org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType convert(
            EndpointReferenceType internal) {
        org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType exposed =
            Names200408.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        exposed.setAddress(convert(internal.getAddress()));
        exposed.setReferenceParameters(
                            convert(internal.getReferenceParameters()));
        QName serviceQName = EndpointReferenceUtils.getServiceName(internal);
        if (serviceQName != null) {
            ServiceNameType serviceName =
                Names200408.WSA_OBJECT_FACTORY.createServiceNameType();
            serviceName.setValue(serviceQName);
            exposed.setServiceName(serviceName);
        }
        String portLocalName = EndpointReferenceUtils.getPortName(internal);
        if (portLocalName != null) {
            String namespace = serviceQName.getNamespaceURI() != null
                               ? serviceQName.getNamespaceURI()
                               : Names.WSDL_INSTANCE_NAMESPACE_NAME;
            QName portQName = 
                new QName(namespace, portLocalName);
            AttributedQName portType = 
                Names200408.WSA_OBJECT_FACTORY.createAttributedQName();
            portType.setValue(portQName);
            exposed.setPortType(portType);
        }
        // no direct analogue for Metadata
        exposed.getAny().addAll(internal.getAny());
        exposed.getOtherAttributes().putAll(internal.getOtherAttributes());
        return exposed;
    }
    
    /**
     * Convert from 2004/08 EndpointReferenceType to 2005/08 
     * EndpointReferenceType.
     * 
     * @param exposed the 2004/08 EndpointReferenceType
     * @return an equivalent 2005/08 EndpointReferenceType
     */
    public static EndpointReferenceType convert(
            org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType exposed) {
        EndpointReferenceType internal = 
            ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        internal.setAddress(convert(exposed.getAddress()));
        internal.setReferenceParameters(
                            convert(exposed.getReferenceParameters()));
        ServiceNameType serviceName = exposed.getServiceName();
        if (serviceName != null) {
            EndpointReferenceUtils.setServiceName(internal, 
                                                  serviceName.getValue());
        }
        AttributedQName portName = exposed.getPortType();
        if (portName != null) {
            EndpointReferenceUtils.setPortName(internal,
                                               portName.getValue().getLocalPart());
        }
        // no direct analogue for ReferenceProperties
        internal.getAny().addAll(exposed.getAny());
        internal.getOtherAttributes().putAll(exposed.getOtherAttributes());
        return internal; 
    }

    /**
     * Convert from 2005/08 ReferenceParametersType to 2004/08
     * ReferenceParametersType.
     * 
     * @param internal the 2005/08 ReferenceParametersType
     * @return an equivalent 2004/08 ReferenceParametersType
     */
    public static org.objectweb.celtix.ws.addressing.v200408.ReferenceParametersType convert(
            ReferenceParametersType internal) {
        org.objectweb.celtix.ws.addressing.v200408.ReferenceParametersType exposed = 
            null;
        if (internal != null) {
            exposed =
                Names200408.WSA_OBJECT_FACTORY.createReferenceParametersType();
            exposed.getAny().addAll(internal.getAny());
        }
        return exposed; 
    }
    
    /**
     * Convert from 2004/08 ReferenceParametersType to 2005/08
     * ReferenceParametersType.
     * 
     * @param exposed the 2004/08 ReferenceParametersType
     * @return an equivalent 2005/08 ReferenceParametersType
     */
    public static ReferenceParametersType convert(
            org.objectweb.celtix.ws.addressing.v200408.ReferenceParametersType exposed) {
        ReferenceParametersType internal = null;
        if (exposed != null) {
            internal = 
                ContextUtils.WSA_OBJECT_FACTORY.createReferenceParametersType();
            internal.getAny().addAll(exposed.getAny());
        }
        return internal; 
    }

    /**
     * Convert from 2005/08 RelatesToType to 2004/08 Relationship.
     * 
     * @param internal the 2005/08 RelatesToType
     * @return an equivalent 2004/08 Relationship
     */
    public static Relationship convert(RelatesToType internal) {
        Relationship exposed = null;
        if (internal != null) {
            exposed = Names200408.WSA_OBJECT_FACTORY.createRelationship();
            exposed.setValue(internal.getValue());
            String internalRelationshipType = internal.getRelationshipType();
            if (internalRelationshipType != null) {
                QName exposedRelationshipType = 
                    Names.WSA_RELATIONSHIP_REPLY.equals(
                                                    internalRelationshipType)
                    ? new QName(Names200408.WSA_NAMESPACE_NAME,
                                Names.WSA_REPLY_NAME)
                    : new QName(internalRelationshipType);
                exposed.setRelationshipType(exposedRelationshipType);
            }
            exposed.getOtherAttributes().putAll(internal.getOtherAttributes());
        }
        return exposed;
    }

    /**
     * Convert from 2004/08 Relationship to 2005/08 RelatesToType.
     * 
     * @param exposed the 2004/08 Relationship
     * @return an equivalent 2005/08 RelatesToType
     */
    public static RelatesToType convert(Relationship exposed) {      
        RelatesToType internal = null;
        if (exposed != null) {
            internal = ContextUtils.WSA_OBJECT_FACTORY.createRelatesToType();
            internal.setValue(exposed.getValue());
            QName exposedRelationshipType = exposed.getRelationshipType();
            if (exposedRelationshipType != null) {
                String internalRelationshipType = 
                    Names.WSA_REPLY_NAME.equals(
                                      exposedRelationshipType.getLocalPart())
                    ? Names.WSA_RELATIONSHIP_REPLY
                    : exposedRelationshipType.toString();
                internal.setRelationshipType(internalRelationshipType);
            }
            internal.getOtherAttributes().putAll(exposed.getOtherAttributes());
        }
        return internal; 
    }
    
    /** 
     * @param exposedURI specifies the version WS-Addressing
     * @return JABXContext for the exposed namespace URI
     */
    public static JAXBContext getExposedJAXBContext(String exposedURI) 
        throws JAXBException {
        return NATIVE_VERSION.equals(exposedURI)
               ? ContextUtils.getJAXBContext()
               : Names200408.getJAXBContext();
    }
        
    
    /**
     * Holder for 2004/08 Names
     */
    public static class Names200408 {
        public static final String WSA_NAMESPACE_NAME = 
            "http://schemas.xmlsoap.org/ws/2004/08/addressing";
        public static final String WSA_ANONYMOUS_ADDRESS = 
            WSA_NAMESPACE_NAME + "/anonymous";
        public static final String WSA_NONE_ADDRESS =
            WSA_NAMESPACE_NAME + "/none";
        public static final ObjectFactory WSA_OBJECT_FACTORY = 
            new ObjectFactory();
        public static final String WS_ADDRESSING_PACKAGE =
            AttributedURI.class.getPackage().getName();
        public static final Class<org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType>
        EPR_TYPE = 
            org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType.class;
        
        private static JAXBContext jaxbContext;
        
        protected Names200408() {
        }
        
        /**
         * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated
         * types for the 2004/08 version.
         *
         * @return a JAXBContext 
         */
        public static JAXBContext getJAXBContext() throws JAXBException {
            synchronized (Names200408.class) {
                if (jaxbContext == null) {
                    jaxbContext = 
                        JAXBContext.newInstance(WS_ADDRESSING_PACKAGE);
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
            synchronized (Names200408.class) {
                jaxbContext = ctx;
            }
        }        
    }
}

package org.objectweb.celtix.ws.rm;

/**
 * A container for WS-RM constants.
 */
public final class JAXWSRMConstants {
    
    /**
     * Used to cache sequence properties in context.
     */
    
    public static final String SEQUENCE_PROPERTY = 
        "org.objectweb.celtix.ws.rm.sequence";
    
    /**
     * Used to cache acknowledgements in context.
     */
    
    public static final String ACKS_PROPERTY = 
        "org.objectweb.celtix.ws.rm.acknowledgements";
    
    /**
     * Used to cache acknowledgment requests in context.
     */
    
    public static final String ACKS_REQUESTED_PROPERTY = 
        "org.objectweb.celtix.ws.rm.acknowledgements.requested";
    
    /**
     * Used to cache srever binding in the context
     */
    public static final String WSA_ACTION =
        "org.objectweb.celtix.ws.addressing.action";
    
    /**
     * Prevents instantiation. 
     */
    private JAXWSRMConstants() {
    }
}

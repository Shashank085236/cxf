package org.objectweb.celtix.ws.rm.persistence;

import java.io.InputStream;
import java.math.BigInteger;

import javax.xml.ws.handler.MessageContext;


public interface RMMessage {
    
    /**
     * @return the message number of the message within its sequence.
     */
    BigInteger getMessageNr();
    
    /**
     * @return the message context of this message.
     */
    MessageContext getContext();  

    /**
     * @return the message context of this message as an input stream.
     */
    InputStream getContextAsStream();
    
}

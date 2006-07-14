package org.objectweb.celtix.interceptors;

import org.objectweb.celtix.message.Message;

public interface Interceptor<T extends Message> {
    /**
     * Intercepts a message. 
     * Interceptors need NOT invoke handleMessage or handleFault
     * on the next interceptor - the interceptor chain will
     * take care of this.
     * 
     * @param message
     */
    void handleMessage(T message);
    
    /**
     * Called for all interceptors (in reverse order) on which handleMessage
     * had been successfully invoked, when normal execution of the chain was
     * aborted for some reason.
     * 
     * @param message
     */
    void handleFault(T message);
    
}

package org.objectweb.celtix.jaxws.handlers;

import javax.xml.ws.Binding;

import org.objectweb.celtix.message.Message;
import org.objectweb.celtix.phase.Phase;

public class StreamHandlerInterceptor extends AbstractJAXWSHandlerInterceptor<Message> {

    public StreamHandlerInterceptor(Binding binding) {
        super(binding);
        setPhase(Phase.USER_STREAM);
    }

    public void handleMessage(Message message) {
        StreamMessageContextImpl sctx = new StreamMessageContextImpl(message);
        getInvoker(message).invokeStreamHandlers(sctx);
    } 
    
    public void handleFault(Message message) {
    }
    
    
}

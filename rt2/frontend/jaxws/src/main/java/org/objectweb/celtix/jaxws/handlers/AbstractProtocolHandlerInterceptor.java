package org.objectweb.celtix.jaxws.handlers;

import javax.xml.ws.handler.MessageContext;

import org.objectweb.celtix.jaxws.context.WrappedMessageContext;
import org.objectweb.celtix.message.Message;
import org.objectweb.celtix.phase.Phase;

public abstract class AbstractProtocolHandlerInterceptor extends AbstractJAXWSHandlerInterceptor {

    protected AbstractProtocolHandlerInterceptor(HandlerChainInvoker invoker) {
        super(invoker);
        setPhase(Phase.USER_PROTOCOL);
    }
    
    public void handleMessage(Message message) {
        MessageContext context = createProtocolMessageContext(message);
        invoker.invokeProtocolHandlers(isRequestor(message), context);            
    }
    
    public void handleFault(Message message) {
        
    }
    
    protected MessageContext createProtocolMessageContext(Message message) {
        return new WrappedMessageContext(message);
    }
}

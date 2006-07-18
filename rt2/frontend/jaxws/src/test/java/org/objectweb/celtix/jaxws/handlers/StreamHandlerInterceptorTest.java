package org.objectweb.celtix.jaxws.handlers;

import junit.framework.TestCase;

import org.easymock.classextension.IMocksControl;
import org.objectweb.celtix.message.Message;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createNiceControl;


public class StreamHandlerInterceptorTest extends TestCase {
    
    private IMocksControl control;
    private HandlerChainInvoker invoker;
    private Message message;
    
    public void setUp() {
        control = createNiceControl();
        invoker = control.createMock(HandlerChainInvoker.class);
        message = control.createMock(Message.class);
    }
    
    public void tearDown() {
        control.verify();
    }

    public void testInterceptSuccess() {
        expect(invoker.invokeStreamHandlers(isA(StreamMessageContext.class))).andReturn(true);
        control.replay();
        StreamHandlerInterceptor si = new StreamHandlerInterceptor(invoker);
        assertEquals("unexpected phase", "user-stream", si.getPhase());
        si.handleMessage(message);
    }
    
    public void testInterceptFailure() {
        expect(invoker.invokeStreamHandlers(isA(StreamMessageContext.class))).andReturn(false);
        control.replay();
        StreamHandlerInterceptor si = new StreamHandlerInterceptor(invoker);
        si.handleMessage(message); 
    }
}

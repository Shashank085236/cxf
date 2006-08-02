package org.objectweb.celtix.endpoint;

import java.util.Map;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.interceptors.InterceptorChain;
import org.objectweb.celtix.message.Exchange;
import org.objectweb.celtix.message.ExchangeImpl;
import org.objectweb.celtix.message.Message;
import org.objectweb.celtix.phase.PhaseInterceptorChain;
import org.objectweb.celtix.service.model.OperationInfo;

public class ClientImpl implements Client {

    Bus bus;
    Endpoint endpoint;
    
    public ClientImpl(Bus b, Endpoint e) {
        bus = b;
        endpoint = e;
    }
    

    public Object[] invoke(OperationInfo oi, Object[] params, Map<String, Object> ctx) {

        Message message = endpoint.getBinding().createMessage();
        message.setContent(Object[].class, params);
        setOutMessageProperties(message, oi);
   

        Exchange exchange = new ExchangeImpl();
        exchange.putAll(ctx);
        exchange.setOutMessage(message);
        setExchangeProperties(exchange, ctx);

        PhaseInterceptorChain chain = new PhaseInterceptorChain(bus.getOutPhases());
        chain.add(bus.getOutInterceptors());
        chain.add(endpoint.getService().getOutInterceptors());
        chain.add(endpoint.getOutInterceptors());
        chain.add(endpoint.getBinding().getOutInterceptors());
        
        modifyChain(chain, ctx);

        // execute chain

        // create transport/channel/conduit and assign to exchange

        // correlate

        return null;

    }
    
    
    protected void setOutMessageProperties(Message message, OperationInfo oi) {
        message.put(Message.OPERATION_INFO, oi);
        message.put(Message.BINDING, endpoint.getBinding());
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
    }
    
    protected void setExchangeProperties(Exchange exchange, Map<String, Object> ctx) {
        // no-op
    }
    
    protected void modifyChain(InterceptorChain chain, Map<String, Object> ctx) {
        // no-op
    }

}

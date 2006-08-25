package org.apache.cxf.jaxws.handler;


import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;
import javax.xml.ws.LogicalMessage;


public class LogicalMessageImpl implements LogicalMessage {

    private final LogicalMessageContextImpl msgContext;
    
    public LogicalMessageImpl(LogicalMessageContextImpl lmctx) {
        msgContext = lmctx;
    }

    public Source getPayload() {
        return msgContext.getWrappedMessage().getContent(Source.class);
        // throw new UnsupportedOperationException("getPayload");
    }

    public void setPayload(Source s) {
        msgContext.getWrappedMessage().setContent(Source.class, s);
        // throw new UnsupportedOperationException("setPayload");
    }

    public Object getPayload(JAXBContext arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setPayload(Object arg0, JAXBContext arg1) {
        // TODO Auto-generated method stub
        
    }

   
}

package org.objectweb.celtix.bus.transports.http;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.ws.handler.MessageContext;

import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.MessageContextWrapper;
import org.objectweb.celtix.context.OutputStreamMessageContext;

public abstract class AbstractHTTPServerOutputStreamContext
    extends MessageContextWrapper
    implements OutputStreamMessageContext {
    
    protected final AbstractHTTPServerTransport transport;
    protected WrappedOutputStream origOut;
    protected OutputStream out;
    
    public AbstractHTTPServerOutputStreamContext(AbstractHTTPServerTransport tr, MessageContext ctx)
        throws IOException {
        
        super(ctx);
        transport = tr;
        origOut = new WrappedOutputStream();
        out = origOut;
    }
    
    protected abstract void flushHeaders() throws IOException;
    
    public void setFault(boolean isFault) {
        if (isFault) {
            put(HTTP_RESPONSE_CODE, 500);
        } else {
            put(HTTP_RESPONSE_CODE, 200);            
        }
    }

    public boolean isFault() {
        return ((Integer)get(HTTP_RESPONSE_CODE)).intValue() == 500;
    }

    public void setOneWay(boolean isOneWay) {
        put(ONEWAY_MESSAGE_TF, isOneWay);
    }
    
    public boolean isOneWay() {
        Boolean b = (Boolean)get(ONEWAY_MESSAGE_TF);
        return b == null ? false : b.booleanValue();
    }

    public OutputStream getOutputStream() {
        return out;
    }
    
    public void setOutputStream(OutputStream o) {
        out = o;
    }
    
    public InputStreamMessageContext getCorrespondingInputStreamContext()
        throws IOException {
        return null;
    }
    
    protected class WrappedOutputStream extends FilterOutputStream {
        WrappedOutputStream() {
            super(new ByteArrayOutputStream());
        }
        public void resetOut(OutputStream newOut) throws IOException {
            ByteArrayOutputStream bout = (ByteArrayOutputStream)out;
            if (bout.size() > 0) {
                bout.writeTo(newOut);
            }
            out = newOut;
        }
    }
}
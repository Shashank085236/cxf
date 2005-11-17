package org.objectweb.celtix.bus.transports.http;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

import org.apache.catalina.core.StandardWrapper;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.context.GenericMessageContext;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.MessageContextWrapper;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.ServerTransportCallback;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public class HTTPServerTransport extends StandardWrapper implements ServerTransport {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogUtils.getL7dLogger(HTTPServerTransport.class);

    EndpointReferenceType reference;
    String url;
    HTTPServerEngine engine;
    String name;
    Servlet servlet = new HTTPServlet(this);
    ServerTransportCallback callback;
    Configuration configuration;
    
    public HTTPServerTransport(Bus bus, EndpointReferenceType ref) throws WSDLException, IOException {
        
        reference = ref;
        // get url (publish address) from endpoint reference
        url = EndpointReferenceUtils.getAddress(ref);  
        configuration = createConfiguration(bus, ref);
        
        URL nurl = new URL(url);
        name = nurl.getPath();
        engine = HTTPServerEngine.getForPort(nurl.getProtocol(), nurl.getPort());
        
        try {
            super.init();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    public String getName() {
        return name;
    }

    public void load() {
        //nothing
    }
    public Servlet allocate() throws ServletException {
        return servlet;
    }
    
    public void activate(ServerTransportCallback cb) throws IOException {
        callback = cb;
        engine.addServant(url, this);
    }

    public void deactivate() throws IOException {
        // TODO Auto-generated method stub
        engine.removeServant(url, this);
    }

    public void shutdown() {
    }
    
    private Configuration createConfiguration(Bus bus, EndpointReferenceType ref) {
        Configuration busConfiguration = bus.getConfiguration();
        QName serviceName = EndpointReferenceUtils.getServiceName(ref);
        Configuration endpointConfiguration = busConfiguration
            .getChild("http://celtix.objectweb.org/bus/jaxws/endpoint-config", serviceName);
        Port port = null;
        try {
            port = EndpointReferenceUtils.getPort(bus.getWSDLManager(), ref);            
        } catch (WSDLException ex) {
            // ignore
        }
        return new HTTPServerTransportConfiguration(endpointConfiguration, port);
    }

    
    
    /**
     * @param context The associated MessageContext.
     * @return the context that will be used to obtain the OutputStream
     */
    public OutputStreamMessageContext createOutputStreamContext(MessageContext context)
        throws IOException {
        return new HTTPServerOutputStreamContext(context);
    }
    
    /**
     * @param context The <code>OutputStreamMessageContext</code> to prepare.
     */
    public void finalPrepareOutputStreamContext(OutputStreamMessageContext context)
        throws IOException {
        ((HTTPServerOutputStreamContext)context).flushHeaders();
    }
    
    static class HTTPServerInputStreamContext
        extends GenericMessageContext
        implements InputStreamMessageContext {
    
        static final String HTTP_SERVLET_REQUEST =
            HTTPServerInputStreamContext.class.getName() + ".REQUEST";
        static final String HTTP_SERVLET_RESPONSE =
            HTTPServerInputStreamContext.class.getName() + ".RESPONSE";

        private static final long serialVersionUID = 1L;

        final ServletRequest request;
        InputStream origInputStream;
        InputStream inStream;
    
        public HTTPServerInputStreamContext(ServletRequest req, ServletResponse resp)
            throws IOException {
            
            request = req;
            put(ObjectMessageContext.MESSAGE_INPUT, false);
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            for (Object nameObject : req.getParameterMap().keySet()) {
                String name = (String)nameObject;
                headers.put(name, Arrays.asList(req.getParameterValues(name)));
            }
            put(HTTP_REQUEST_HEADERS, headers); 
            
            origInputStream = req.getInputStream();
            inStream = origInputStream;
            
            put(HTTP_SERVLET_RESPONSE, resp);
            put(HTTP_SERVLET_REQUEST, req);
        }
    
        public InputStream getInputStream() {
            return inStream;
        }
    
        public void setInputStream(InputStream ins) {
            inStream = ins;
        }
    }

    static class HTTPServerOutputStreamContext
        extends MessageContextWrapper
        implements OutputStreamMessageContext {
        
        WrappedOutputStream origOut;
        OutputStream out;
        ServletResponse response;
        
        public HTTPServerOutputStreamContext(MessageContext ctx) throws IOException {
            super(ctx);
            origOut = new WrappedOutputStream();
            out = origOut;
            response = (ServletResponse)ctx.get(HTTPServerInputStreamContext.HTTP_SERVLET_RESPONSE); 
        }
        
        void flushHeaders() throws IOException {
            Map<?, ?> headers = (Map<?, ?>)super.get(HTTP_RESPONSE_HEADERS);
            if (null != headers) {
                for (Iterator<?> iter = headers.keySet().iterator(); iter.hasNext();) {
                    String header = (String)iter.next();
                    List<?> headerList = (List<?>)headers.get(header);
                    for (Object string : headerList) {
                        ((HttpServletResponse)response).addHeader(header, (String)string);
                    }
                }
            }
            origOut.resetOut(response.getOutputStream());
        }

        public void setFault(boolean isFault) {
            put(HTTP_RESPONSE_CODE, 500);
        }

        public boolean isFault() {
            return ((Integer)get(HTTP_RESPONSE_CODE)).intValue() == 500;
        }
        public OutputStream getOutputStream() {
            return out;
        }
        
        public void setOutputStream(OutputStream o) {
            out = o;
        }
        
        
        private class WrappedOutputStream extends FilterOutputStream {
            WrappedOutputStream() {
                super(new ByteArrayOutputStream());
            }
            void resetOut(OutputStream newOut) throws IOException {
                ByteArrayOutputStream bout = (ByteArrayOutputStream)out;
                if (bout.size() > 0) {
                    bout.writeTo(newOut);
                }
                out = newOut;
            }
            public void flush() throws IOException {
                super.flush();
                response.flushBuffer();
            }
            public void close() throws IOException {
                response.flushBuffer();
                super.close();
            }
        }
    }
    

    private class HTTPServlet extends GenericServlet {
        private static final long serialVersionUID = 8291179381765644068L;

        final HTTPServerTransport transport;
        HTTPServlet(HTTPServerTransport trans) {
            transport = trans;
        }
        
        
        public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Service http request on thread: " + Thread.currentThread());
            }
            
            final class Servicer implements Runnable {
                private boolean complete;
                private final ServletRequest request;
                private final ServletResponse response;
                
                Servicer(ServletRequest req, ServletResponse resp) {
                    request = req;
                    response = resp;
                }
                public void run() {
                    try {
                        doService(request, response);                        
                    } catch (IOException ex) {                        
                        // TODO handle exception
                        LOG.log(Level.SEVERE, "DISPATCH_FAILURE_MSG", ex);
                    } finally {
                        complete = true;
                        synchronized (this) {
                            notifyAll();
                        }
                    }
                } 
                
                public synchronized void waitForCompletion() {
                    while (!complete) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                    }
                }
            }
            
            if (null == callback.getExecutor()) {
                doService(req, resp);
            } else {  
                Servicer servicer = new Servicer(req, resp);
                callback.getExecutor().execute(servicer);
                servicer.waitForCompletion();
            }
           
            if (LOG.isLoggable(Level.INFO)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }
    
    void doService(ServletRequest req, ServletResponse resp) throws IOException {
        InputStreamMessageContext ctx = new HTTPServerInputStreamContext(req, resp);
        callback.dispatch(ctx, this);
    }


}

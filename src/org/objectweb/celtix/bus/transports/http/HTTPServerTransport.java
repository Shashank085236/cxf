package org.objectweb.celtix.bus.transports.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.common.util.Base64Exception;
import org.objectweb.celtix.common.util.Base64Utility;
import org.objectweb.celtix.configuration.Configuration;
import org.objectweb.celtix.context.GenericMessageContext;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.MessageContextWrapper;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.ServerTransportCallback;
import org.objectweb.celtix.transports.http.configuration.HTTPServerPolicy;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;

public class HTTPServerTransport implements ServerTransport {
    static final Logger LOG = LogUtils.getL7dLogger(HTTPServerTransport.class);
    static final String HTTP_REQUEST =
        HTTPServerInputStreamContext.class.getName() + ".REQUEST";
    static final String HTTP_RESPONSE =
        HTTPServerInputStreamContext.class.getName() + ".RESPONSE";
    
    private static final long serialVersionUID = 1L;

    EndpointReferenceType reference;
    String url;
    HTTPServerEngine engine;
    String name;
    ServerTransportCallback callback;
    Configuration configuration;
    HTTPServerPolicy policy;
    
    public HTTPServerTransport(Bus bus, EndpointReferenceType ref) throws WSDLException, IOException {
        
        reference = ref;
        // get url (publish address) from endpoint reference
        url = EndpointReferenceUtils.getAddress(ref);  
        configuration = createConfiguration(bus, ref);
        
        URL nurl = new URL(url);
        name = nurl.getPath();
        engine = HTTPServerEngine.getForPort(bus, nurl.getProtocol(), nurl.getPort());
        policy = getServerPolicy(configuration);
    }
    
    private HTTPServerPolicy getServerPolicy(Configuration conf) {
        HTTPServerPolicy pol = conf.getObject(HTTPServerPolicy.class, "httpServer");
        if (pol == null) {
            pol = new HTTPServerPolicy();
        }
        return pol;
    }

    public void activate(ServerTransportCallback cb) throws IOException {
        callback = cb;
        engine.addServant(url, this);
    }

    public void deactivate() throws IOException {
        engine.removeServant(url, this);
    }

    public void postDispatch(MessageContext bindingContext, OutputStreamMessageContext context) {
        // Do not need to do anything here. 
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
    
    class HTTPServerInputStreamContext
        extends GenericMessageContext
        implements InputStreamMessageContext {
    

        private static final long serialVersionUID = 1L;

        final HttpRequest request;
        InputStream origInputStream;
        InputStream inStream;
    
        public HTTPServerInputStreamContext(HttpRequest req, HttpResponse resp)
            throws IOException {
            
            request = req;
            put(ObjectMessageContext.MESSAGE_INPUT, true);
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            for (Enumeration e = req.getFieldNames(); e.hasMoreElements();) {
                String fname = (String)e.nextElement();
                List<String> values;
                if (headers.containsKey(fname)) {
                    values = headers.get(fname);
                } else {
                    values = new ArrayList<String>();
                    headers.put(fname, values);
                }
                for (Enumeration e2 = req.getFieldValues(fname); e2.hasMoreElements();) {
                    String val = (String)e2.nextElement();
                    values.add(val);
                }
            }
            put(HTTP_REQUEST_HEADERS, headers);
            
            if (headers.containsKey("Authorization")) {
                List<String> authorizationLines = headers.get("Authorization"); 
                String credentials = authorizationLines.get(0);
                String authType = credentials.split(" ")[0];
                if ("Basic".equals(authType)) {
                    String authEncoded = credentials.split(" ")[1];
                    try {
                        String authDecoded = new String(Base64Utility.decode(authEncoded));
                        String authInfo[] = authDecoded.split(":");
                        String username = authInfo[0];
                        String password = authInfo[1];
                        put(BindingProvider.USERNAME_PROPERTY, username);
                        put(BindingProvider.PASSWORD_PROPERTY, password);
                    } catch (Base64Exception ex) {
                        //ignore, we'll leave things alone.  They can try decoding it themselves
                    }
                }
            }

            headers = new HashMap<String, List<String>>();
            setPolicies(req, headers);
            put(HTTP_RESPONSE_HEADERS, headers); 
           
            origInputStream = req.getInputStream();
            inStream = origInputStream;
            
            put(HTTP_RESPONSE, resp);
            put(HTTP_REQUEST, req);
        }
        
        private void setPolicies(HttpRequest req, Map<String, List<String>> headers) {
            if (policy.isSetCacheControl()) {
                headers.put("Cache-Control",
                            Arrays.asList(new String[] {policy.getCacheControl().value()}));
            }
            if (policy.isSetContentLocation()) {
                headers.put("Content-Location",
                            Arrays.asList(new String[] {policy.getContentLocation()}));
            }
            if (policy.isSetContentEncoding()) {
                headers.put("Content-Encoding",
                            Arrays.asList(new String[] {policy.getContentEncoding()}));
            }
            if (policy.isSetContentType()) {
                headers.put("Content-Type",
                            Arrays.asList(new String[] {policy.getContentType()}));
            }
            if (policy.isSetServerType()) {
                headers.put("Server",
                            Arrays.asList(new String[] {policy.getServerType()}));
            }
            if (policy.isSetHonorKeepAlive() && !policy.isHonorKeepAlive()) {
                headers.put("Connection",
                            Arrays.asList(new String[] {"close"}));
            }
            if (policy.isSetReceiveTimeout()) {
                Object connection = req.getHttpConnection().getConnection();
                if (connection instanceof Socket) {
                    Socket sock = (Socket)connection;
                    try {
                        sock.setSoTimeout((int)policy.getReceiveTimeout());
                    } catch (SocketException ex) {
                        LOG.log(Level.INFO, "Could not set SoTimeout", ex);
                    }
                }                
            }
            
        /*
         * TODO - hook up these policies
        <xs:attribute name="SuppressClientSendErrors" type="xs:boolean" use="optional" default="false">
        <xs:attribute name="SuppressClientReceiveErrors" type="xs:boolean" use="optional" default="false">
        */

        }
    
        public InputStream getInputStream() {
            return inStream;
        }
    
        public void setInputStream(InputStream ins) {
            inStream = ins;
        }

        public void setFault(boolean isFault) {
            //nothing to do
        }

        public boolean isFault() {
            return false;
        }
    }

    class HTTPServerOutputStreamContext
        extends MessageContextWrapper
        implements OutputStreamMessageContext {
        
        WrappedOutputStream origOut;
        OutputStream out;
        HttpResponse response;
        
        public HTTPServerOutputStreamContext(MessageContext ctx) throws IOException {
            super(ctx);
            origOut = new WrappedOutputStream();
            out = origOut;
            response = (HttpResponse)ctx.get(HTTP_RESPONSE); 
        }
        
        void flushHeaders() throws IOException {
            Map<?, ?> headers = (Map<?, ?>)super.get(HTTP_RESPONSE_HEADERS);
            if (null != headers) {
                for (Iterator<?> iter = headers.keySet().iterator(); iter.hasNext();) {
                    String header = (String)iter.next();
                    List<?> headerList = (List<?>)headers.get(header);
                    for (Object string : headerList) {
                        response.addField(header, (String)string);
                    }
                }
            }
            origOut.resetOut(new BufferedOutputStream(response.getOutputStream(), 1024));
        }

        
        public void setFault(boolean isFault) {
            put(HTTP_RESPONSE_CODE, 500);
            response.setStatus(500, "Fault Occurred");
        }

        public boolean isFault() {
            return ((Integer)get(HTTP_RESPONSE_CODE)).intValue() == 500;
        }
       
        public void setOneWay(boolean isOneWay) {
            put(ONEWAY_MESSAGE_TF, isOneWay);
        }
        
        public boolean isOneWay() {
            return ((Boolean)get(ONEWAY_MESSAGE_TF)).booleanValue();
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
        }
    }
        
    void doService(HttpRequest req, HttpResponse resp) throws IOException {
        if (policy.isSetRedirectURL()) {
            resp.sendRedirect(policy.getRedirectURL());
            resp.commit();
            req.setHandled(true);
            return;
        }
                
        final class Servicer implements Runnable {
            private boolean complete;
            private final HttpRequest request;
            private final HttpResponse response;
            
            Servicer(HttpRequest reqs, HttpResponse resps) {
                request = reqs;
                response = resps;
            }
            public void run() {
                try {
                    serviceRequest(request, response);                        
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
            serviceRequest(req, resp);
        } else {  
            Servicer servicer = new Servicer(req, resp);
            callback.getExecutor().execute(servicer);
            servicer.waitForCompletion();
        }        
    }
    
    void serviceRequest(HttpRequest req, HttpResponse resp) throws IOException {
        try {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Service http request on thread: " + Thread.currentThread());
            }
            InputStreamMessageContext ctx = new HTTPServerInputStreamContext(req, resp);
            callback.dispatch(ctx, this);
            resp.commit();
            req.setHandled(true);
        } finally {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }


}

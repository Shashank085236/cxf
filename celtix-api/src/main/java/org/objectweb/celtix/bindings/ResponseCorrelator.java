package org.objectweb.celtix.bindings;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.handlers.HandlerInvoker;

/**
 * Class to manage correlation of decoupled responses.
 */
public class ResponseCorrelator implements ResponseCallback {

    private static final Logger LOG = LogUtils.getL7dLogger(ResponseCorrelator.class);
    
    private HandlerInvoker fixedHandlerInvoker;
    private Map<String, Response> responseMap;
    private Map<String, HandlerInvoker> relatedRequestMap;
    private AbstractBindingBase binding;

    protected ResponseCorrelator(AbstractBindingBase b) {
        // a fixed snap-shot of the stream and system handler chains
        // are used, as the incoming (possibly asynchronous) response
        // cannot yet be corellated with a particular request, hence
        // may not include any dynamic (i.e. programmatic) changes
        // made to the handler chains
        fixedHandlerInvoker = b.createHandlerInvoker();
        responseMap = new HashMap<String, Response>();
        relatedRequestMap = new HashMap<String, HandlerInvoker>();
        binding = b;
    }

    /**
     * Used by the ClientTransport to dispatch decoupled responses.
     * 
     * @param responseContext context with InputStream containing the
     *            incoming the response
     */
    
    public void dispatch(InputStreamMessageContext responseContext) {
        assert responseContext != null;
        
        Response response = new Response(binding, fixedHandlerInvoker);
        response.processProtocol(responseContext);
        
        synchronized (this) {
            String inCorrelation = response.getCorrelationId();
            if (inCorrelation != null) {
                HandlerInvoker alternate =
                    relatedRequestMap.remove(inCorrelation);
                if (alternate == null) {
                    LOG.log(Level.INFO, "response correlation ID: {0}", inCorrelation);
                    responseMap.put(inCorrelation, response);
                    notifyAll();                    
                } else {
                    DataBindingCallback callback =
                        BindingContextUtils.retrieveDataBindingCallback(responseContext);
                    if (callback != null) {              
                        response.getHandlerInvoker().adoptLogicalHandlers(alternate);
                        response.processLogical(callback);
                    }
                }
            } else {
                // this is expected for partial responses
                LOG.info("no correlation ID in incoming message");
                DataBindingCallback callback =
                    BindingContextUtils.retrieveDataBindingCallback(responseContext);
                if (callback != null) {              
                    response.processLogical(callback);
                }
            }
        }
    }

    /**
     * Wait for a correlated response.
     * 
     * @param request outgoing request containing the correlation ID property
     * @return binding-specific context for the correlated response
     */
    public Response getResponse(Request request) {
        
        String outCorrelation = request.getCorrelationId();
        Response response = null;
        if (outCorrelation != null) {
            LOG.log(Level.INFO, "request correlation ID: {0}", outCorrelation);
            synchronized (this) {
                response = responseMap.remove(outCorrelation);
                int count = 0;
                while (response == null && count < 10) {
                    try {
                        wait();
                        response = responseMap.remove(outCorrelation);
                        if (request.isRelatedRequestExpected()) {
                            relatedRequestMap.put(outCorrelation, request.getHandlerInvoker());
                        } 
                    } catch (InterruptedException ie) {
                        // ignore
                    }
      
                    count++;
                }
            }
        } else {
            LOG.warning("NO_OUTGOING_CORRELATION_ID_MSG");
        }
        return response;
    }
}

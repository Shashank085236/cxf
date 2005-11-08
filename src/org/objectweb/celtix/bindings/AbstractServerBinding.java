package org.objectweb.celtix.bindings;



import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.Oneway;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import org.objectweb.celtix.Bus;
import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bus.context.WebServiceContextImpl;
import org.objectweb.celtix.bus.handlers.HandlerChainInvoker;
import org.objectweb.celtix.bus.jaxws.EndpointUtils;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.ServerTransportCallback;


public abstract class AbstractServerBinding implements ServerBinding {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractServerBinding.class);

    protected final Bus bus;
    protected final EndpointReferenceType reference;
    protected ServerTransport transport;
    protected Endpoint endpoint;
    protected DataBindingCallbackFactory dbcbFactory;

    public AbstractServerBinding(Bus b,
                                 EndpointReferenceType ref,
                                 Endpoint ep,
                                 DataBindingCallbackFactory cbFactory) {
        bus = b;
        reference = ref;
        endpoint = ep;
        dbcbFactory = cbFactory;
    }
    
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void activate() throws WSDLException, IOException {
        transport = createTransport(reference);
        
        ServerTransportCallback tc = new ServerTransportCallback() {

                public void dispatch(InputStreamMessageContext ctx, ServerTransport t) {
                    AbstractServerBinding.this.dispatch(ctx, t);               
                }

                public Executor getExecutor() {
                    return AbstractServerBinding.this.getEndpoint().getExecutor();
                }

            };
        
        transport.activate(tc);
    }

    public void deactivate() throws IOException {
        transport.deactivate();
    }

    public ObjectMessageContext createObjectContext() {
        return new ObjectMessageContextImpl();
    }

    protected abstract ServerTransport createTransport(EndpointReferenceType ref) 
        throws WSDLException, IOException;

    protected abstract MessageContext createBindingMessageContext(MessageContext orig);

    protected abstract void marshal(ObjectMessageContext objCtx, MessageContext replyCtx);
    
    protected abstract void marshalFault(ObjectMessageContext objCtx, MessageContext replyCtx);

    protected abstract void unmarshal(MessageContext requestCtx, ObjectMessageContext objCtx);

    protected abstract void write(MessageContext replyCtx, OutputStreamMessageContext outCtx)
        throws IOException;

    protected abstract void read(InputStreamMessageContext inCtx, MessageContext context) throws IOException;

    protected abstract MessageContext invokeOnProvider(MessageContext requestCtx, ServiceMode mode);

    protected void dispatch(InputStreamMessageContext inCtx, ServerTransport t) {
        LOG.info("Dispatched to binding on thread : " + Thread.currentThread());
        ObjectMessageContext objContext = createObjectContext();

        if (inCtx != null) { 
            // this may be null during unit tests
            objContext.putAll(inCtx);
        }

        MessageContext requestCtx = createBindingMessageContext(objContext);
        //Input Message
        requestCtx.put(ObjectMessageContext.MESSAGE_INPUT, Boolean.FALSE);
        
        try {
            read(inCtx, requestCtx);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "REQUEST_UNREADABLE_MSG", ex);
            throw new WebServiceException(ex);
        }

        Method method = getMethod(requestCtx, objContext);
        assert method != null;
        initObjectContext(objContext, method);
        
        if (isOneWay(method)) {
            try {
                OutputStreamMessageContext outCtx = t.createOutputStreamContext(inCtx);
                t.finalPrepareOutputStreamContext(outCtx);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "RESPONSE_UNWRITABLE_MSG", ex);
                throw new WebServiceException(ex);
            }
        }

        ServiceMode mode = EndpointUtils.getServiceMode(endpoint);
        MessageContext replyCtx = null;
        if (null != mode) {
            replyCtx = invokeOnProvider(requestCtx, mode);
        } else {
            replyCtx = invokeOnMethod(requestCtx, objContext);
        }
        
        if (!isOneWay(method)) {
            try {
                OutputStreamMessageContext outCtx = t.createOutputStreamContext(inCtx);
                if (objContext.getException() != null) {
                    outCtx.setFault(true);
                }
                t.finalPrepareOutputStreamContext(outCtx);
                
                write(replyCtx, outCtx);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "RESPONSE_UNWRITABLE_MSG", ex);
                throw new WebServiceException(ex);
            }
        }
        
        LOG.info("Dispatch complete on thread : " + Thread.currentThread());
    }

    /** invoke the target method.  Ensure that any replies or
     * exceptions are put into the correct context for the return path
     */
    private boolean doInvocation(Method method, ObjectMessageContext objContext, 
                                 MessageContext replyCtx, HandlerChainInvoker invoker) { 

        assert method != null && objContext != null && replyCtx != null;
            
        boolean exceptionCaught = false; 
        try {

            boolean continueProcessing = invoker.invokeLogicalHandlers(false);
            if (continueProcessing) {
                new WebServiceContextImpl(objContext); 
                // get parameters from object context and invoke on implementor
                Object params[] = (Object[])objContext.getMessageObjects();
                Object result = method.invoke(getEndpoint().getImplementor(), params);
                objContext.setReturn(result);
            }

            if (!isOneWay(method)) {
                switchToResponse(objContext, replyCtx); 
                invoker.setOutbound(); 
                invoker.invokeLogicalHandlers(false);
            }
        } catch (IllegalAccessException ex) {
            LogUtils.log(LOG, Level.SEVERE, "IMPLEMENTOR_INVOCATION_FAILURE_MSG", ex, method.getName());
            objContext.setException(ex);
            exceptionCaught = true;
        } catch (InvocationTargetException ex) {
            LogUtils.log(LOG, Level.INFO, "IMPLEMENTOR_INVOCATION_EXCEPTION_MSG", ex, method.getName());
            Throwable cause = ex.getCause();
            if (cause != null) {
                objContext.setException(cause);
            } else {
                objContext.setException(ex);
            }            
            exceptionCaught = true;
        } finally { 
            invoker.setOutbound(); 
        } 
        
        return exceptionCaught || invoker.faultRaised();
    } 


    private void switchToResponse(ObjectMessageContext ctx, MessageContext replyCtx) { 
        ctx.put(ObjectMessageContext.MESSAGE_INPUT, Boolean.TRUE);
        ctx.remove(ObjectMessageContext.MESSAGE_PAYLOAD);
        replyCtx.put(ObjectMessageContext.MESSAGE_INPUT, Boolean.TRUE);
    } 
 
    private MessageContext invokeOnMethod(MessageContext requestCtx, ObjectMessageContext objContext) {


        objContext.put(ObjectMessageContext.MESSAGE_INPUT, Boolean.FALSE);

        HandlerChainInvoker invoker = new HandlerChainInvoker(getBinding().getHandlerChain(), 
                                                              objContext, false); 

        MessageContext replyCtx = createBindingMessageContext(objContext);
        assert replyCtx != null;

        try {
            boolean continueProcessing = invoker.invokeProtocolHandlers(false, requestCtx);

            if (continueProcessing) {
                try {
                    unmarshal(requestCtx, objContext);
                    
                    Method method = objContext.getMethod();
                    doInvocation(method, objContext, replyCtx, invoker); 
    
                    if (!isOneWay(method)) {
                        switchToResponse(objContext, replyCtx); 
                        if (null == objContext.getException()) {
                            marshal(objContext, replyCtx);
                        } 
                    }
                } catch (ProtocolException pe) {
                    
                    if (pe.getCause() != null) {
                        objContext.setException(pe.getCause());
                    } else {
                        objContext.setException(pe);
                    }
                } finally {
                    if (objContext.getException() != null) {
                        marshalFault(objContext, replyCtx);
                    }
                }
            }
            invoker.invokeProtocolHandlers(false, replyCtx); 

        } finally {
            invoker.mepComplete(); 
        }

        return replyCtx;
    }
    
    private boolean isOneWay(Method method) {
        return !(method.getAnnotation(Oneway.class) == null); 
    }
    
    private Method getMethod(MessageContext requestCtx, ObjectMessageContext objContext) {
        
        QName operationName = getOperationName(requestCtx); 

        if (null == operationName) {
            LOG.severe("CONTEXT_MISSING_OPERATION_NAME_MSG");
            throw new WebServiceException("Request Context does not include operation name");
        }
        
        if (objContext != null) {
            objContext.put(MessageContext.WSDL_OPERATION, operationName);
        }
        
        Method method = EndpointUtils.getMethod(endpoint, operationName);
        if (method == null) {
            LOG.log(Level.SEVERE, "IMPLEMENTOR_MISSING_METHOD_MSG", operationName);
            throw new WebServiceException("Web method: " + operationName + " not found in implementor.");
        }
        return method;       
    }
    
    private void initObjectContext(ObjectMessageContext objCtx, Method method) {
        try {
            int idx = 0;
            Object[] methodArgs = (Object[])
                Array.newInstance(Object.class, method.getParameterTypes().length);
            for (Class<?> cls : method.getParameterTypes()) {
                if (cls.isAssignableFrom(Holder.class)) {
                    methodArgs[idx] = cls.newInstance();
                } 
                idx++;
            }
            objCtx.setMessageObjects(methodArgs);
            objCtx.setMethod(method);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "INIT_OBJ_CONTEXT_FAILED");
            throw new WebServiceException(ex);
        }
    }
    
    protected abstract QName getOperationName(MessageContext ctx);

}

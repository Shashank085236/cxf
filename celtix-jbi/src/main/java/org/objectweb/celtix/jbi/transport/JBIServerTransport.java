package org.objectweb.celtix.jbi.transport;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;

import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.context.ObjectMessageContextImpl;
import org.objectweb.celtix.context.OutputStreamMessageContext;
import org.objectweb.celtix.jbi.se.CeltixServiceUnit;
import org.objectweb.celtix.jbi.se.CeltixServiceUnitManager;
import org.objectweb.celtix.transports.ServerTransport;
import org.objectweb.celtix.transports.ServerTransportCallback;
import org.objectweb.celtix.ws.addressing.EndpointReferenceType;

/**
 * Connects Celtix clients to the NormalizedMessageRouter.  Celtix
 * messages are wrapped in a NormalizedMessage before being sent to
 * the NMR and are unwrapped when being received from it.
 */
public class JBIServerTransport implements ServerTransport {
    
    private static final Logger LOG = Logger.getLogger(JBIServerTransport.class.getName());
    
    private static final String MESSAGE_EXCHANGE_PROPERTY = "celtix.jbi.message.exchange";
    private final CeltixServiceUnitManager suManager; 
    private final DeliveryChannel channel; 
    private ServerTransportCallback callback; 
    private volatile boolean running; 
    private JBIDispatcher dispatcher; 
    private final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    
    
    public JBIServerTransport(CeltixServiceUnitManager sum, DeliveryChannel dc) { 
        suManager = sum; 
        channel = dc; 
        docBuilderFactory.setNamespaceAware(true);
    }
    
    public void shutdown() { 
        running = false; 
    } 
    
    public OutputStreamMessageContext createOutputStreamContext(MessageContext context)
        throws IOException { 
        
        return new  JBIOutputStreamMessageContext(context); 
    } 
    
    
    public void finalPrepareOutputStreamContext(OutputStreamMessageContext context)
        throws IOException { 
    } 
    
    public void activate(ServerTransportCallback cb) throws IOException { 
        // activate endpoints here 
        LOG.info("activating JBI server transport");
        callback = cb;
        dispatcher = new JBIDispatcher();
        new Thread(dispatcher).start();
    } 
    
    
    public void deactivate() throws IOException { 
        running = false; 
    } 
    
    public void postDispatch(MessageContext ctx, OutputStreamMessageContext msgContext) { 
        
        try { 
            JBIOutputStreamMessageContext jbiCtx = (JBIOutputStreamMessageContext)msgContext;
            ByteArrayOutputStream baos = (ByteArrayOutputStream)jbiCtx.getOutputStream();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            LOG.finest("building document from bytes");
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(bais);
            
            MessageExchange xchng = (MessageExchange)ctx.get(MESSAGE_EXCHANGE_PROPERTY);
            LOG.fine("creating NormalizedMessage");
            NormalizedMessage msg = xchng.createMessage();
            msg.setContent(new DOMSource(doc));
            xchng.setMessage(msg, "out");
            LOG.fine("postDispatch sending out message to NWR");
            channel.send(xchng);
        } catch (Exception ex) { 
            LOG.log(Level.SEVERE, "error sending Out message", ex);
        }
    } 
    
    public OutputStreamMessageContext rebase(MessageContext context, 
                       EndpointReferenceType decoupledResponseEndpoint) 
        throws IOException {
        // TODO Auto-generated method stub
        return null;   
    }
    
    private void dispatch(MessageExchange exchange, ServerTransportCallback cb) 
        throws IOException { 
        
        try { 
            QName opName = exchange.getOperation(); 
            LOG.fine("dispatch: " + opName);
            
            NormalizedMessage nm = exchange.getMessage("in");
            final InputStream in = JBIMessageHelper.convertMessageToInputStream(nm.getContent());
            // dispatch through callback
            
            ObjectMessageContext ctx = new ObjectMessageContextImpl();
            LOG.finest("dispatching message on callback: " + cb);
            ctx.put(MESSAGE_EXCHANGE_PROPERTY, exchange);
            cb.dispatch(new JBIInputStreamMessageContext(ctx, in), this);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "error preparing message", ex);
            throw new IOException(ex.getMessage());
        }
    }

   
    private class JBIDispatcher implements Runnable { 
        
        public final void run() {
            
            try { 
                running = true;
                LOG.fine("JBIServerTransport message receiving thread started");
                do { 
                    MessageExchange exchange = channel.accept(); 
                    if (exchange != null) { 
                        // REVISIT: serialized message handling not such a
                        // good idea.
                        // REVISIT: can there be more than one ep?
                        ServiceEndpoint ep = exchange.getEndpoint();
                        CeltixServiceUnit csu = suManager.getServiceUnitForEndpoint(ep);
                        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
                        
                        try { 
                            Thread.currentThread().setContextClassLoader(csu.getClassLoader());
                            if (csu != null) { 
                                LOG.finest("dispatching to Celtix service unit");
                                dispatch(exchange, callback);
                            } else {
                                LOG.info("no CeltixServiceUnit found");
                            }
                        } finally { 
                            Thread.currentThread().setContextClassLoader(oldLoader);
                        } 
                    } 
                } while(running);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "error running dispatch thread", ex);
            } 
            LOG.fine("JBIServerTransport message processing thread exitting");
        }
    }
}

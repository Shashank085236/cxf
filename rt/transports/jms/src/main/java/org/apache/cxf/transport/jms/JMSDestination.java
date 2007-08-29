/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.jms;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;



public class JMSDestination extends AbstractMultiplexDestination implements Configurable, JMSTransport {
        
    protected static final String BASE_BEAN_NAME_SUFFIX = ".jms-destination-base";

    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);
    
    protected ServerConfig serverConfig;
    protected ServerBehaviorPolicyType runtimePolicy;
    protected AddressType address;
    protected SessionPoolType sessionPool;
     
    final ConduitInitiator conduitInitiator;
    final JMSTransportBase base;
  
    PooledSession listenerSession;
    JMSListenerThread listenerThread;
    
    public JMSDestination(Bus b,
                          ConduitInitiator ci,
                          EndpointInfo info) throws IOException {
        super(b, getTargetReference(info, b), info);    
        
        base = new JMSTransportBase(b, endpointInfo, true, BASE_BEAN_NAME_SUFFIX, this);

        conduitInitiator = ci;

        initConfig();
    }

    protected Logger getLogger() {
        return LOG;
    }
    
    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        return new BackChannelConduit(EndpointReferenceUtils.getAnonymousEndpointReference(),
                                      inMessage);
    }
     
    public void activate()  {
        getLogger().log(Level.INFO, "JMSServerTransport activate().... ");        

        try {
            getLogger().log(Level.FINE, "establishing JMS connection");
            JMSProviderHub.connect(this, serverConfig, runtimePolicy);
            //Get a non-pooled session. 
            listenerSession = base.sessionFactory.get(base.targetDestination);
            listenerThread = new JMSListenerThread(listenerSession);
            listenerThread.start();
        } catch (JMSException ex) {
            getLogger().log(Level.SEVERE, "JMS connect failed with JMSException : ", ex);
        } catch (NamingException nex) {
            getLogger().log(Level.SEVERE, "JMS connect failed with NamingException : ", nex);
        }
    }
    
    public void deactivate()  {
        try {
            listenerSession.consumer().close();
            if (listenerThread != null) {
                listenerThread.join();
            }
            base.sessionFactory.shutdown();
        } catch (InterruptedException e) {
            //Do nothing here
        } catch (JMSException ex) {
            //Do nothing here
        }
    }

    public void shutdown() {
        getLogger().log(Level.FINE, "JMSDestination shutdown()");
        this.deactivate();
    }

    public Queue getReplyToDestination(Message inMessage) 
        throws JMSException, NamingException {
        Queue replyTo;
        javax.jms.Message message = 
            (javax.jms.Message)inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);
        // If WS-Addressing had set the replyTo header.
        if  (inMessage.get(JMSConstants.JMS_REBASED_REPLY_TO) != null) {
            replyTo = base.sessionFactory.getQueueFromInitialContext(
                              (String)  inMessage.get(JMSConstants.JMS_REBASED_REPLY_TO));
        } else {
            replyTo = (null != message.getJMSReplyTo()) 
                ? (Queue)message.getJMSReplyTo() : (Queue)base.replyDestination;
        }    
        return replyTo;
    }
    
    public void setReplyCorrelationID(javax.jms.Message request, javax.jms.Message reply) 
        throws JMSException {
        
        String correlationID = request.getJMSCorrelationID();
        
        if (correlationID == null
            || "".equals(correlationID)
            && getRuntimePolicy().isUseMessageIDAsCorrelationID()) {
            correlationID = request.getJMSMessageID();
        }
    
        if (correlationID != null && !"".equals(correlationID)) {
            reply.setJMSCorrelationID(correlationID);
        }
    }
    
    protected void incoming(javax.jms.Message message) throws IOException {
        try {
            getLogger().log(Level.FINE, "server received request: ", message);
           
            Object request = base.unmarshal(message);
            getLogger().log(Level.FINE, "The Request Message is [ " + request + "]");
            byte[] bytes = null;

            if (message instanceof TextMessage) {
                String requestString = (String)request;
                getLogger().log(Level.FINE, "server received request: ", requestString);
                bytes = requestString.getBytes();
            } else {
                //Both ByteMessage and ObjectMessage would get unmarshalled to byte array.
                bytes = (byte[])request;
            }

            // get the message to be interceptor
            MessageImpl inMessage = new MessageImpl();
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(bytes));
            base.populateIncomingContext(message, inMessage, JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            inMessage.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, new JMSMessageHeadersType());
            inMessage.put(JMSConstants.JMS_REQUEST_MESSAGE, message);
                        
            inMessage.setDestination(this);            
            
            //handle the incoming message
            incomingObserver.onMessage(inMessage);
           
        } catch (JMSException jmsex) {
            //TODO: need to revisit for which exception should we throw.
            throw new IOException(jmsex.getMessage());
        } 
    }
    
    public void connected(javax.jms.Destination target, 
                          javax.jms.Destination reply, 
                          JMSSessionFactory factory) {
        base.connected(target, reply, factory);
    }

    public String getBeanName() {
        return endpointInfo.getName().toString() + ".jms-destination";
    }
    
    private void initConfig() {
        this.runtimePolicy = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                               ServerBehaviorPolicyType.class);
        this.serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        this.address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        this.sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(), SessionPoolType.class);
        
        Configurer configurer = base.bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(this);
        }
    }

    public AddressType getJMSAddress() {
        return address;
    }

    public void setJMSAddress(AddressType a) {
        this.address = a;
    }

    public ServerBehaviorPolicyType getRuntimePolicy() {
        return runtimePolicy;
    }

    public void setRuntimePolicy(ServerBehaviorPolicyType runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public SessionPoolType getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(SessionPoolType sessionPool) {
        this.sessionPool = sessionPool;
    }
    
    protected class JMSListenerThread extends Thread {
        private final PooledSession listenSession;

        public JMSListenerThread(PooledSession session) {
            listenSession = session;
        }

        public void run() {
            try {
                while (true) {
                    javax.jms.Message message = listenSession.consumer().receive();                   
                    if (message == null) {
                        getLogger().log(Level.WARNING,
                                "Null message received from message consumer.",
                                " Exiting ListenerThread::run().");
                        return;
                    }
                    while (message != null) {
                        //REVISIT  to get the thread pool                        
                        //Executor executor = jmsDestination.callback.getExecutor();
                        Executor executor = null;
                        if (executor == null) {
                            WorkQueueManager wqm =
                                base.bus.getExtension(WorkQueueManager.class);
                            if (null != wqm) {
                                executor = wqm.getAutomaticWorkQueue();
                            }    
                        }
                        if (executor != null) {
                            try {
                                executor.execute(new JMSExecutor(message));
                                message = null;
                            } catch (RejectedExecutionException ree) {
                                //FIXME - no room left on workqueue, what to do
                                //for now, loop until it WILL fit on the queue, 
                                //although we could just dispatch on this thread.
                            }                            
                        } else {
                            getLogger().log(Level.INFO, "handle the incoming message in listener thread");
                            try {
                                incoming(message);
                            } catch (IOException ex) {
                                getLogger().log(Level.WARNING, "Failed to process incoming message : ", ex);
                            }                            
                        }                        
                        message = null;
                    }
                }
            } catch (JMSException jmsex) {
                jmsex.printStackTrace();
                getLogger().log(Level.SEVERE, "Exiting ListenerThread::run(): ", jmsex.getMessage());
            } catch (Throwable jmsex) {
                jmsex.printStackTrace();
                getLogger().log(Level.SEVERE, "Exiting ListenerThread::run(): ", jmsex.getMessage());
            }
        }
    }
    
    protected class JMSExecutor implements Runnable {
        javax.jms.Message message;
        
        JMSExecutor(javax.jms.Message m) {
            message = m;
        }

        public void run() {
            getLogger().log(Level.INFO, "run the incoming message in the threadpool");
            try {
                incoming(message);
            } catch (IOException ex) {
                //TODO: Decide what to do if we receive the exception.
                getLogger().log(Level.WARNING,
                        "Failed to process incoming message : ", ex);
            }
        }
        
    }
    
    // this should deal with the cxf message 
    protected class BackChannelConduit extends AbstractConduit {
        
        protected Message inMessage;
                
        BackChannelConduit(EndpointReferenceType ref, Message message) {
            super(ref);
            inMessage = message;
        }
        
        /**
         * Register a message observer for incoming messages.
         * 
         * @param observer the observer to notify on receipt of incoming
         */
        public void setMessageObserver(MessageObserver observer) {
            // shouldn't be called for a back channel conduit
        }

        /**
         * Send an outbound message, assumed to contain all the name-value
         * mappings of the corresponding input message (if any). 
         * 
         * @param message the message to be sent.
         */
        public void prepare(Message message) throws IOException {
            // setup the message to be send back
            message.put(JMSConstants.JMS_REQUEST_MESSAGE, 
                        inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE));
            message.setContent(OutputStream.class,
                               new JMSOutputStream(inMessage));
        }
        
        protected Logger getLogger() {
            return LOG;
        }
    }
    
    private class JMSOutputStream extends CachedOutputStream {
                
        private Message inMessage;
        private javax.jms.Message reply;
        private Queue replyTo;
        private QueueSender sender;
        
        // setup the ByteArrayStream
        public JMSOutputStream(Message m) {
            super();
            inMessage = m;
        }
        
        //to prepear the message and get the send out message
        private void commitOutputMessage() throws IOException {
            
            JMSMessageHeadersType headers =
                (JMSMessageHeadersType) inMessage.get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            javax.jms.Message request = 
                (javax.jms.Message) inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);              
            
            PooledSession replySession = null;          
            
            if (base.isDestinationStyleQueue()) {
                try {
                    //setup the reply message                
                    replyTo = getReplyToDestination(inMessage);
                    replySession = base.sessionFactory.get(false);
                    sender = (QueueSender)replySession.producer();
                    
                    if (request instanceof TextMessage) {
                        reply = base.marshal(currentStream.toString(), 
                                             replySession.session(), 
                                             null, 
                                             JMSConstants.TEXT_MESSAGE_TYPE);
                        getLogger().log(Level.FINE,
                                         "The response message is ["
                                         + currentStream.toString() + "]");
                    } else if (request instanceof BytesMessage) {
                        reply = base.marshal(((ByteArrayOutputStream)currentStream).toByteArray(),
                                             replySession.session(),
                                             null, 
                                             JMSConstants.BYTE_MESSAGE_TYPE);
                        getLogger().log(Level.FINE, "The response message is [" 
                                             + new String((
                                                 (ByteArrayOutputStream)currentStream).toByteArray()) 
                                             + "]");
                    } else {
                        reply = base.marshal(((ByteArrayOutputStream)currentStream).toByteArray(),
                                             replySession.session(),
                                             null, 
                                            JMSConstants.BINARY_MESSAGE_TYPE);
                        getLogger().log(Level.FINE, "The response message is [" 
                                             + new String((
                                                 (ByteArrayOutputStream)currentStream).toByteArray()) 
                                             + "]");
                    }
                   
                    setReplyCorrelationID(request, reply);
                    
                    base.setMessageProperties(headers, reply);

                    sendResponse();
                    
                } catch (JMSException ex) {
                    getLogger().log(Level.WARNING, "Failed in post dispatch ...", ex);                
                    throw new IOException(ex.getMessage());                    
                } catch (NamingException nex) {
                    getLogger().log(Level.WARNING, "Failed in post dispatch ...", nex);                
                    throw new IOException(nex.getMessage());                    
                } finally {
                    // house-keeping
                    if (replySession != null) {
                        base.sessionFactory.recycle(replySession);
                    }
                }
            } else {
                // we will never receive a non-oneway invocation in pub-sub
                // domain from CXF client - however a mis-behaving pure JMS
                // client could conceivably make suce an invocation, in which
                // case we silently discard the reply
                getLogger().log(Level.WARNING,
                        "discarding reply for non-oneway invocation ",
                        "with 'topic' destinationStyle");
                
            }        
            
            getLogger().log(Level.FINE, "just server sending reply: ", reply);
            // Check the reply time limit Stream close will call for this
            
           
        }

        private void sendResponse() throws JMSException {
            JMSMessageHeadersType headers =
                (JMSMessageHeadersType) inMessage.get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            javax.jms.Message request = 
                (javax.jms.Message) inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);   
            
            int deliveryMode = base.getJMSDeliveryMode(headers);
            int priority = base.getJMSPriority(headers);
            long ttl = base.getTimeToLive(headers);
            
            if (ttl <= 0) {
                ttl = getServerConfig().getMessageTimeToLive();
            }
            
            long timeToLive = 0;
            if (request.getJMSExpiration() > 0) {
                TimeZone tz = new SimpleTimeZone(0, "GMT");
                Calendar cal = new GregorianCalendar(tz);
                timeToLive =  request.getJMSExpiration() - cal.getTimeInMillis();
            }
            
            if (timeToLive >= 0) {
                ttl = ttl > 0 ? ttl : timeToLive;
                getLogger().log(Level.FINE, "send out the message!");
                sender.send(replyTo, reply, deliveryMode, priority, ttl);
            } else {
                // the request message had dead
                getLogger().log(Level.INFO, "Message time to live is already expired skipping response.");
            }         
        }
           
        

        @Override
        protected void doFlush() throws IOException {
            // TODO Auto-generated method stub
            
        }

        
        @Override
        protected void doClose() throws IOException {
            
            commitOutputMessage();        
        }

        @Override
        protected void onWrite() throws IOException {
            // Do nothing here        
        }

    }

}

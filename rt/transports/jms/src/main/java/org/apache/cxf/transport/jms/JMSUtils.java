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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

public final class JMSUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSUtils.class);

    private JMSUtils() {

    }

    public static Properties getInitialContextEnv(AddressType addrType) {
        Properties env = new Properties();
        populateContextEnvironment(addrType, env);

        if (LOG.isLoggable(Level.FINE)) {
            Enumeration props = env.propertyNames();

            while (props.hasMoreElements()) {
                String name = (String)props.nextElement();
                String value = env.getProperty(name);
                LOG.log(Level.FINE, "Context property: " + name + " | " + value);
            }
        }

        return env;
    }

    protected static void populateContextEnvironment(AddressType addrType, Properties env) {

        java.util.ListIterator listIter = addrType.getJMSNamingProperty().listIterator();

        while (listIter.hasNext()) {
            JMSNamingPropertyType propertyPair = (JMSNamingPropertyType)listIter.next();

            if (null != propertyPair.getValue()) {
                env.setProperty(propertyPair.getName(), propertyPair.getValue());
            }
        }
    }

    public static int getJMSDeliveryMode(JMSMessageHeadersType headers) {
        int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

        if (headers != null && headers.isSetJMSDeliveryMode()) {
            deliveryMode = headers.getJMSDeliveryMode();
        }
        return deliveryMode;
    }

    public static int getJMSPriority(JMSMessageHeadersType headers) {
        int priority = Message.DEFAULT_PRIORITY;
        if (headers != null && headers.isSetJMSPriority()) {
            priority = headers.getJMSPriority();
        }
        return priority;
    }

    public static long getTimeToLive(JMSMessageHeadersType headers) {
        long ttl = -1;
        if (headers != null && headers.isSetTimeToLive()) {
            ttl = headers.getTimeToLive();
        }
        return ttl;
    }

    public static void setMessageProperties(JMSMessageHeadersType headers, Message message)
        throws JMSException {

        if (headers != null && headers.isSetProperty()) {
            List<JMSPropertyType> props = headers.getProperty();
            for (int x = 0; x < props.size(); x++) {
                message.setStringProperty(props.get(x).getName(), props.get(x).getValue());
            }
        }
    }

    /**
     * Create a JMS of the appropriate type populated with the given payload.
     * 
     * @param payload the message payload, expected to be either of type String or byte[] depending on payload
     *                type
     * @param session the JMS session
     * @param replyTo the ReplyTo destination if any
     * @return a JMS of the appropriate type populated with the given payload
     */
    public static Message createAndSetPayload(Object payload, Session session, String messageType)
        throws JMSException {
        Message message = null;

        if (JMSConstants.TEXT_MESSAGE_TYPE.equals(messageType)) {
            message = session.createTextMessage((String)payload);
        } else if (JMSConstants.BYTE_MESSAGE_TYPE.equals(messageType)) {
            message = session.createBytesMessage();
            ((BytesMessage)message).writeBytes((byte[])payload);
        } else {
            message = session.createObjectMessage();
            ((ObjectMessage)message).setObject((byte[])payload);
        }

        return message;
    }

    /**
     * Extract the payload of an incoming message.
     * 
     * @param message the incoming message
     * @return the message payload as byte[]
     */
    public static byte[] retrievePayload(Message message) {
        Object converted;
        try {
            converted = new SimpleMessageConverter102().fromMessage(message);
        } catch (MessageConversionException e) {
            throw new RuntimeException("Conversion failed", e);
        } catch (JMSException e) {
            throw JmsUtils.convertJmsAccessException(e);
        }
        if (converted instanceof String) {
            return ((String)converted).getBytes(); // TODO encoding
        } else if (converted instanceof byte[]) {
            return (byte[])converted;
        } else {
            return (byte[])converted; // TODO is this correct?
        }
    }

    public static JMSMessageHeadersType populateIncomingContext(javax.jms.Message message,
                                                                org.apache.cxf.message.Message inMessage,
                                                                String headerType) {
        try {
            JMSMessageHeadersType headers = null;
            headers = (JMSMessageHeadersType)inMessage.get(headerType);
            if (headers == null) {
                headers = new JMSMessageHeadersType();
                inMessage.put(headerType, headers);
            }
            headers.setJMSCorrelationID(message.getJMSCorrelationID());
            headers.setJMSDeliveryMode(new Integer(message.getJMSDeliveryMode()));
            headers.setJMSExpiration(new Long(message.getJMSExpiration()));
            headers.setJMSMessageID(message.getJMSMessageID());
            headers.setJMSPriority(new Integer(message.getJMSPriority()));
            headers.setJMSRedelivered(Boolean.valueOf(message.getJMSRedelivered()));
            headers.setJMSTimeStamp(new Long(message.getJMSTimestamp()));
            headers.setJMSType(message.getJMSType());

            Map<String, List<String>> protHeaders = new HashMap<String, List<String>>();
            List<JMSPropertyType> props = headers.getProperty();
            Enumeration enm = message.getPropertyNames();
            while (enm.hasMoreElements()) {
                String name = (String)enm.nextElement();
                String val = message.getStringProperty(name);
                JMSPropertyType prop = new JMSPropertyType();
                prop.setName(name);
                prop.setValue(val);
                props.add(prop);

                protHeaders.put(name, Collections.singletonList(val));
                if (name.equals(org.apache.cxf.message.Message.CONTENT_TYPE)
                    || name.equals(JMSConstants.JMS_CONTENT_TYPE) && val != null) {
                    inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, val);
                }

                protHeaders.put(name, Collections.singletonList(val));
                if (name.equals(org.apache.cxf.message.Message.CONTENT_TYPE)
                    || name.equals(JMSConstants.JMS_CONTENT_TYPE) && val != null) {
                    inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, val);
                }
            }
            inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);
            return headers;
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        }
    }

    protected static void addProtocolHeaders(Message message, Map<String, List<String>> headers)
        throws JMSException {
        if (headers == null) {
            return;
        }
        StringBuilder value = new StringBuilder(256);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            value.setLength(0);
            boolean first = true;
            for (String s : entry.getValue()) {
                if (!first) {
                    value.append("; ");
                }
                value.append(s);
                first = false;
            }
            // Incase if the Content-Type header key is Content-Type replace with JMS_Content_Type
            if (entry.getKey().equals(org.apache.cxf.message.Message.CONTENT_TYPE)) {
                message.setStringProperty(JMSConstants.JMS_CONTENT_TYPE, value.toString());
            } else {
                message.setStringProperty(entry.getKey(), value.toString());
            }

        }
    }

    public static Map<String, List<String>> getSetProtocolHeaders(org.apache.cxf.message.Message message) {
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (null == headers) {
            headers = new HashMap<String, List<String>>();
            message.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }

    public static void addContentTypeToProtocolHeader(org.apache.cxf.message.Message message) {
        String contentType = (String)message.get(org.apache.cxf.message.Message.CONTENT_TYPE);

        Map<String, List<String>> headers = JMSUtils.getSetProtocolHeaders(message);
        List<String> ct;
        if (headers.get(JMSConstants.JMS_CONTENT_TYPE) != null) {
            ct = headers.get(JMSConstants.JMS_CONTENT_TYPE);
        } else if (headers.get(org.apache.cxf.message.Message.CONTENT_TYPE) != null) {
            ct = headers.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        } else {
            ct = new ArrayList<String>();
            headers.put(JMSConstants.JMS_CONTENT_TYPE, ct);
        }

        ct.add(contentType);
    }

    public static boolean isDestinationStyleQueue(AddressType address) {
        return JMSConstants.JMS_QUEUE.equals(address.getDestinationStyle().value());
    }

    public static Message buildJMSMessageFromCXFMessage(org.apache.cxf.message.Message outMessage,
                                                        Object payload, String messageType, Session session,
                                                        Destination replyTo, String correlationId)
        throws JMSException {
        Message jmsMessage = JMSUtils.createAndSetPayload(payload, session, messageType);

        if (replyTo != null) {
            jmsMessage.setJMSReplyTo(replyTo);
        }

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        if (headers == null) {
            headers = new JMSMessageHeadersType();
            // throw new RuntimeException("No JMS_CLIENT_REQUEST_HEADERS set in message");
        }

        JMSUtils.setMessageProperties(headers, jmsMessage);
        // ensure that the contentType is set to the out jms message header
        JMSUtils.addContentTypeToProtocolHeader(outMessage);
        Map<String, List<String>> protHeaders = CastUtils.cast((Map<?, ?>)outMessage
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        JMSUtils.addProtocolHeaders(jmsMessage, protHeaders);
        jmsMessage.setJMSCorrelationID(correlationId);
        jmsMessage.setJMSPriority(JMSUtils.getJMSPriority(headers));
        return jmsMessage;
    }

    public static void sendMessage(MessageProducer producer, Destination destination, Message jmsMessage,
                                   long timeToLive, int deliveryMode, int priority) throws JMSException {
        /*
         * Can this be changed to producer.send(destination, jmsMessage, deliveryMode, priority, timeToLive);
         */

        if (destination instanceof Queue) {
            QueueSender sender = (QueueSender)producer;
            sender.setTimeToLive(timeToLive);
            sender.send((Queue)destination, jmsMessage, deliveryMode, priority, timeToLive);
        } else {
            TopicPublisher publisher = (TopicPublisher)producer;
            publisher.setTimeToLive(timeToLive);
            publisher.publish((Topic)destination, jmsMessage, deliveryMode, priority, timeToLive);
        }
    }

    public static Destination resolveRequestDestination(Context context, Connection connection,
                                                        AddressType addrDetails) throws JMSException,
        NamingException {
        Destination requestDestination = null;
        // see if jndiDestination is set
        if (addrDetails.getJndiDestinationName() != null) {
            requestDestination = (Destination)context.lookup(addrDetails.getJndiDestinationName());
        }

        // if no jndiDestination or it fails see if jmsDestination is set
        // and try to create it.
        if (requestDestination == null && addrDetails.getJmsDestinationName() != null) {
            if (JMSUtils.isDestinationStyleQueue(addrDetails)) {
                requestDestination = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    .createQueue(addrDetails.getJmsDestinationName());
            } else {
                requestDestination = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    .createTopic(addrDetails.getJmsDestinationName());
            }
        }
        return requestDestination;
    }

    public static Queue resolveReplyDestination(Context context, Connection connection,
                                                AddressType addrDetails) throws NamingException,
        JMSException {
        Queue replyDestination = null;

        // Reply Destination is used (if present) only if the session is
        // point-to-point session
        if (JMSUtils.isDestinationStyleQueue(addrDetails)) {
            if (addrDetails.getJndiReplyDestinationName() != null) {
                replyDestination = (Queue)context.lookup(addrDetails.getJndiReplyDestinationName());
            }
            if (replyDestination == null && addrDetails.getJmsReplyDestinationName() != null) {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                replyDestination = session.createQueue(addrDetails.getJmsReplyDestinationName());
                session.close();
            }
        }
        return replyDestination;
    }

    public static String generateUniqueSelector() {
        String host = "localhost";

        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
        } catch (UnknownHostException ukex) {
            // Default to localhost.
        }

        long time = Calendar.getInstance().getTimeInMillis();
        return host + "_" + System.getProperty("user.name") + "_" + Thread.currentThread() + time;
    }
}

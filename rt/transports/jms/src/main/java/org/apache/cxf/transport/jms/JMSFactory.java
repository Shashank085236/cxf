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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Factory to create JmsTemplates and JmsListeners from configuration and context information
 */
public final class JMSFactory {
    
    private JMSFactory() {
    }

    /**
     * Create JmsTemplate from configuration information. Most settings are taken from jmsConfig. The QoS
     * settings in headers override the settings from jmsConfig
     * 
     * @param jmsConfig configuration information
     * @param headers context headers
     * @return
     */
    public static JmsTemplate createJmsTemplate(JMSConfiguration jmsConfig, JMSMessageHeadersType headers) {
        JmsTemplate jmsTemplate = jmsConfig.isUseJms11() ? new JmsTemplate() : new JmsTemplate102();
        jmsTemplate.setConnectionFactory(jmsConfig.getConnectionFactory());
        jmsTemplate.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsTemplate.setReceiveTimeout(jmsConfig.getReceiveTimeout());
        jmsTemplate.setTimeToLive(jmsConfig.getTimeToLive());
        int priority = (headers != null && headers.isSetJMSPriority())
            ? headers.getJMSPriority() : jmsConfig.getPriority();
        jmsTemplate.setPriority(priority);
        int deliveryMode = (headers != null && headers.isSetJMSDeliveryMode()) ? headers
            .getJMSDeliveryMode() : jmsConfig.getDeliveryMode();
        jmsTemplate.setDeliveryMode(deliveryMode);
        jmsTemplate.setExplicitQosEnabled(jmsConfig.isExplicitQosEnabled());
        jmsTemplate.setSessionTransacted(jmsConfig.isSessionTransacted());
        if (jmsConfig.getDestinationResolver() != null) {
            jmsTemplate.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
        return jmsTemplate;
    }

    /**
     * Create and start listener using configuration information from jmsConfig. Uses
     * resolveOrCreateDestination to determine the destination for the listener.
     * 
     * @param jmsConfig configuration information
     * @param listenerHandler object to be called when a message arrives
     * @param destinationName null for temp dest or a destination name
     * @return
     */
    public static DefaultMessageListenerContainer createJmsListener(JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    String destinationName) {
        DefaultMessageListenerContainer jmsListener2 = jmsConfig.isUseJms11()
            ? new DefaultMessageListenerContainer() : new DefaultMessageListenerContainer102();
        jmsListener2.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsListener2.setAutoStartup(true);
        jmsListener2.setConnectionFactory(jmsConfig.getConnectionFactory());
        jmsListener2.setMessageSelector(jmsConfig.getMessageSelector());
        jmsListener2.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
        jmsListener2.setSessionTransacted(jmsConfig.isSessionTransacted());
        jmsListener2.setTransactionManager(jmsConfig.getTransactionManager());
        jmsListener2.setMessageListener(listenerHandler);
        if (jmsConfig.getDestinationResolver() != null) {
            jmsListener2.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
        JmsTemplate jmsTemplate = createJmsTemplate(jmsConfig, null);
        jmsListener2.setDestination(JMSFactory.resolveOrCreateDestination(jmsTemplate, destinationName,
                                                                          jmsConfig.isPubSubDomain()));
        jmsListener2.initialize();
        return jmsListener2;
    }

    /**
     * If the destinationName given is null then a temporary destination is created else the destination name
     * is resolved using the resolver from the jmsConfig
     * 
     * @param jmsTemplate template to use for session and resolver
     * @param replyToDestinationName null for temporary destination or a destination name
     * @param pubSubDomain true=pubSub, false=Queues
     * @return resolved destination
     */
    private static Destination resolveOrCreateDestination(final JmsTemplate jmsTemplate,
                                                          final String replyToDestinationName,
                                                          final boolean pubSubDomain) {
        return (Destination)jmsTemplate.execute(new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                if (replyToDestinationName == null) {
                    if (session instanceof QueueSession) {
                        // For JMS 1.0.2
                        return ((QueueSession)session).createTemporaryQueue();
                    } else {
                        // For JMS 1.1
                        return session.createTemporaryQueue();
                    }
                }
                DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                return resolv.resolveDestinationName(session, replyToDestinationName, pubSubDomain);
            }
        });
    }

}

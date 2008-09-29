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

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

public class JMSOldConfigHolder {
    private ClientConfig clientConfig;
    private ClientBehaviorPolicyType runtimePolicy;

    private AddressType address;
    private SessionPoolType sessionPool;
    private JMSConfiguration jmsConfig;
    private ServerConfig serverConfig;
    private ServerBehaviorPolicyType serverBehavior;

    private ConnectionFactory getConnectionFactoryFromJndi(String connectionFactoryName, String userName,
                                                           String password, JndiTemplate jt) {
        if (connectionFactoryName == null) {
            return null;
        }
        try {
            ConnectionFactory connectionFactory = (ConnectionFactory)jt.lookup(connectionFactoryName);
            UserCredentialsConnectionFactoryAdapter uccf = new UserCredentialsConnectionFactoryAdapter();
            uccf.setUsername(userName);
            uccf.setPassword(password);
            uccf.setTargetConnectionFactory(connectionFactory);

            SingleConnectionFactory scf = new SingleConnectionFactory();
            scf.setTargetConnectionFactory(uccf);
            return scf;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public JMSConfiguration createJMSConfigurationFromEndpointInfo(Bus bus, EndpointInfo endpointInfo,
                                                                   boolean isConduit) {
        jmsConfig = new JMSConfiguration();

        // Retrieve configuration information that was extracted from the WSDL
        address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                          ClientBehaviorPolicyType.class);
        serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(), SessionPoolType.class);
        serverBehavior = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                           ServerBehaviorPolicyType.class);
        String name = endpointInfo.getName().toString() + (isConduit ? ".jms-conduit" : ".jms-destination");

        // Try to retrieve configuration information from the spring
        // config. Search for a conduit or destination with name=endpoint name + ".jms-conduit"
        // or ".jms-destination"
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, this);
        }

        JndiTemplate jt = new JndiTemplate();
        jt.setEnvironment(JMSUtils.getInitialContextEnv(address));
        ConnectionFactory cf = getConnectionFactoryFromJndi(address.getJndiConnectionFactoryName(), address
            .getConnectionUserName(), address.getConnectionPassword(), jt);

        boolean pubSubDomain = false;
        if (address.isSetDestinationStyle()) {
            pubSubDomain = DestinationStyleType.TOPIC == address.getDestinationStyle();
        }
        jmsConfig.setConnectionFactory(cf);
        jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
        jmsConfig.setExplicitQosEnabled(true);
        // jmsConfig.setMessageIdEnabled(messageIdEnabled);
        jmsConfig.setMessageSelector(serverBehavior.getMessageSelector());
        // jmsConfig.setMessageTimestampEnabled(messageTimestampEnabled);
        if (runtimePolicy.isSetMessageType()) {
            jmsConfig.setMessageType(runtimePolicy.getMessageType().value());
        }
        // jmsConfig.setOneWay(oneWay);
        // jmsConfig.setPriority(priority);
        jmsConfig.setPubSubDomain(pubSubDomain);
        jmsConfig.setPubSubNoLocal(true);
        jmsConfig.setReceiveTimeout(clientConfig.getClientReceiveTimeout());
        jmsConfig.setSubscriptionDurable(serverBehavior.isSetDurableSubscriberName());
        long timeToLive = isConduit ? clientConfig.getMessageTimeToLive() : serverConfig
            .getMessageTimeToLive();
        jmsConfig.setTimeToLive(timeToLive);
        jmsConfig.setUseJms11(true);
        boolean useJndi = address.isSetJndiDestinationName();
        jmsConfig.setUseJndi(useJndi);
        jmsConfig.setSessionTransacted(serverBehavior.isSetTransactional());

        if (useJndi) {
            // Setup Destination jndi destination resolver
            final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
            jndiDestinationResolver.setJndiTemplate(jt);
            jmsConfig.setDestinationResolver(jndiDestinationResolver);
            jmsConfig.setTargetDestination(address.getJndiDestinationName());
            jmsConfig.setReplyDestination(address.getJndiReplyDestinationName());
        } else {
            // Use the default dynamic destination resolver
            jmsConfig.setTargetDestination(address.getJmsDestinationName());
            jmsConfig.setReplyDestination(address.getJmsReplyDestinationName());
        }

        jmsConfig.setConnectionFactory(cf);

        if (jmsConfig.getTargetDestination() == null || jmsConfig.getConnectionFactory() == null) {
            throw new RuntimeException("Insufficient configuration for "
                                       + (isConduit ? "Conduit" : "Destination") + ". "
                                       + "Did you configure a <jms:"
                                       + (isConduit ? "conduit" : "destination") + " name=\"" + name
                                       + "\"> and set the jndiConnectionFactoryName ?");
        }

        return jmsConfig;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public ClientBehaviorPolicyType getRuntimePolicy() {
        return runtimePolicy;
    }

    public void setRuntimePolicy(ClientBehaviorPolicyType runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    public AddressType getAddress() {
        return address;
    }

    public void setAddress(AddressType address) {
        this.address = address;
    }

    public SessionPoolType getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(SessionPoolType sessionPool) {
        this.sessionPool = sessionPool;
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public ServerBehaviorPolicyType getServerBehavior() {
        return serverBehavior;
    }

    public void setServerBehavior(ServerBehaviorPolicyType serverBehavior) {
        this.serverBehavior = serverBehavior;
    }
}

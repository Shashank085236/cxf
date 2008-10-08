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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.transport.Conduit;
import org.springframework.beans.factory.annotation.Required;

/**
 * Allows to configure the JMSConfiguration directly at the Client or Endpoint. Simply add this class to the
 * Features and reference a JMSConfiguration. The configuration inside this class takes precedence over a
 * configuration that is generated from the old configuration style.
 */
public class JMSConfigFeature extends AbstractFeature {
    JMSConfiguration jmsConfig;

    @Override
    public void initialize(Client client, Bus bus) {
        Conduit conduit = client.getConduit();
        if (conduit instanceof JMSConduit && jmsConfig != null) {
            JMSConduit jmsConduit = (JMSConduit)conduit;
            jmsConduit.setJmsConfig(jmsConfig);
        }
        super.initialize(client, bus);
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    @Required
    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

}

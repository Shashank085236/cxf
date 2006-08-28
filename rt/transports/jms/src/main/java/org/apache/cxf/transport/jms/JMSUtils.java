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


import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transports.jms.JMSAddressPolicyType;
import org.apache.cxf.transports.jms.JMSNamingPropertyType;


public final class JMSUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSUtils.class);

    private JMSUtils() {

    }

    public static Context getInitialContext(JMSAddressPolicyType addrType) throws NamingException {
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
        
        Context context = new InitialContext(env);

        return context;
    }


    protected static void populateContextEnvironment(JMSAddressPolicyType addrType, Properties env) {
        
        java.util.ListIterator listIter =  addrType.getJMSNamingProperty().listIterator();

        while (listIter.hasNext()) {
            JMSNamingPropertyType propertyPair = (JMSNamingPropertyType)listIter.next();
            
            if (null != propertyPair.getValue()) {
                env.setProperty(propertyPair.getName(), propertyPair.getValue());
            }
        }
    }
}

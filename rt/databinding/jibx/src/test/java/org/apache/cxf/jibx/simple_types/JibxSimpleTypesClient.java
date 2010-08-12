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

package org.apache.cxf.jibx.simple_types;

import java.util.Calendar;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jibx.JiBXDataBinding;


public final class JibxSimpleTypesClient {

    private JibxSimpleTypesClient() {
    }

    public static void main(final String[] args) throws Exception {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(SimpleTypesService.class);
        if (args != null && args.length > 0 && !"".equals(args[0])) {
            factory.setAddress(args[0]);
        } else {
            factory.setAddress("http://localhost:9090/Hello");
        }
        factory.getServiceFactory().setDataBinding(new JiBXDataBinding());
        SimpleTypesService client = (SimpleTypesService)factory.create();

        System.out.println("Invoke testString() ..");
        System.out.println(client.testString("Alice"));
        System.out.println("Invoke testDouble() ..");
        System.out.println(client.testDouble(1.0));
        System.out.println("Invoke testDate() ..");
        System.out.println(client.testDate(Calendar.getInstance().getTime()));
    }
}

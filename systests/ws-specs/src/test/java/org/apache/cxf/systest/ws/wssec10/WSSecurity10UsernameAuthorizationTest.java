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

package org.apache.cxf.systest.ws.wssec10;


import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec10.server.AuthorizedServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import wssec.wssec10.IPingService;
import wssec.wssec10.PingService;


/**
 *
 */
public class WSSecurity10UsernameAuthorizationTest extends AbstractBusClientServerTestBase {
    
    private static final String INPUT = "foo";
    
    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(AuthorizedServer.class, true)
        );
    }

    @Test
    public void testClientServerAuthorized() {

        IPingService port = getPort(
            "org/apache/cxf/systest/ws/wssec10/client/client_restricted.xml");
        
        final String output = port.echo(INPUT);
        assertEquals(INPUT, output);
    }
    
    @Test
    public void testClientServerUnauthorized() {

        IPingService port = getPort(
            "org/apache/cxf/systest/ws/wssec10/client/client_restricted_unauthorized.xml");
        
        try {
            port.echo(INPUT);
            fail("Frank is unauthorized");
        } catch (Exception ex) {
            assertEquals("Unauthorized", ex.getMessage());
        }
    }
    
    private static IPingService getPort(String configName) {
        Bus bus = new SpringBusFactory().createBus(configName);
        
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        PingService svc = new PingService(getWsdlLocation());
        final IPingService port = 
            svc.getPort(
                new QName(
                    "http://WSSec/wssec10",
                    "UserName_IPingService"
                ),
                IPingService.class
            );
        return port;
    }
    
    private static URL getWsdlLocation() {
        try {
            return new URL("http://localhost:9003/" + "UserName" + "?wsdl");
        } catch (MalformedURLException mue) {
            return null;
        }
        
    }

    
}

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

package org.apache.cxf.jaxws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.ws.WebServiceContext;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactoryHelper;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.hello_world_soap_http.GreeterImpl;

public class EndpointImplTest extends AbstractJaxWsTest {

    
    @Override
    protected Bus createBus() throws BusException {
        return BusFactoryHelper.newInstance().getDefaultBus();
    }


    public void testEndpoint() throws Exception {   
        GreeterImpl greeter = new GreeterImpl();
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, "anyuri");
 
        WebServiceContext ctx = greeter.getContext();
        assertNull(ctx);
        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            //assertTrue(ex.getCause() instanceof BusException);
            //assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
        ctx = greeter.getContext();
        
        assertNotNull(ctx);
        
    }
    

    public void testEndpointServiceConstructor() throws Exception {   
        GreeterImpl greeter = new GreeterImpl();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(greeter));
        serviceFactory.setServiceClass(GreeterImpl.class);
        
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, serviceFactory);
 
        WebServiceContext ctx = greeter.getContext();
        assertNull(ctx);
        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            //assertTrue(ex.getCause() instanceof BusException);
            //assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
        ctx = greeter.getContext();
        
        assertNotNull(ctx);
    }

    static class EchoObserver implements MessageObserver {

        public void onMessage(Message message) {
            try {
                Conduit backChannel = message.getDestination().getBackChannel(message, null, null);

                backChannel.send(message);

                OutputStream out = message.getContent(OutputStream.class);
                assertNotNull(out);
                InputStream in = message.getContent(InputStream.class);
                assertNotNull(in);
                
                copy(in, out, 2045);

                out.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void copy(final InputStream input, final OutputStream output, final int bufferSize)
        throws IOException {
        try {
            final byte[] buffer = new byte[bufferSize];

            int n = input.read(buffer);
            while (-1 != n) {
                output.write(buffer, 0, n);
                n = input.read(buffer);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}

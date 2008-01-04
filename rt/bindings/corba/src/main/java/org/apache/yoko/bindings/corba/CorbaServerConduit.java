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

package org.apache.yoko.bindings.corba;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;

import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.schemas.yoko.bindings.corba.AddressType;
import org.apache.yoko.bindings.corba.utils.CorbaBindingHelper;
import org.apache.yoko.bindings.corba.utils.CorbaUtils;
import org.apache.yoko.bindings.corba.utils.OrbConfig;
import org.apache.yoko.wsdl.CorbaConstants;

import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;

public final class CorbaServerConduit implements Conduit {
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaServerConduit.class);

    private EndpointInfo endpointInfo;
    private EndpointReferenceType target;
    private ORB orb;
    private CorbaTypeMap typeMap;

    public CorbaServerConduit(EndpointInfo ei,
                              EndpointReferenceType ref,
                              OrbConfig config,
                              CorbaTypeMap map) {
        endpointInfo = ei;
        target = getTargetReference(ref);
        orb = CorbaBindingHelper.getDefaultORB(config);
        typeMap = map;
    }

    public void prepare(Message message) throws IOException {
        try {
            String location = endpointInfo.getAddress();
            if (location == null) {
                AddressType address = endpointInfo.getExtensor(AddressType.class);

                if (address == null) {
                    LOG.log(Level.SEVERE, "Unable to locate a valid CORBA address");
                    throw new CorbaBindingException("Unable to locate a valid CORBA address");
                }
                location = address.getLocation();
            }
            org.omg.CORBA.Object targetObject = CorbaUtils.importObjectReference(orb, location);
            message.put(CorbaConstants.ORB, orb);
            message.put(CorbaConstants.CORBA_ENDPOINT_OBJECT, targetObject);
            message.setContent(OutputStream.class,
                               new CorbaOutputStream(message));
            ((CorbaMessage) message).setCorbaTypeMap(typeMap);
        } catch (java.lang.Exception ex) {
            LOG.log(Level.SEVERE, "Could not resolve target object");
            throw new CorbaBindingException(ex);
        }
    }

    public void close(Message message) throws IOException {        
        buildRequestResult((CorbaMessage)message);
        message.getContent(OutputStream.class).close();
    }

    public EndpointReferenceType getTarget() {
        return target;
    }

    public Destination getBackChannel() {
        return null;
    }

    public void close() {
    }

    public void setMessageObserver(MessageObserver observer) {
        //NOTHING
    }

    public EndpointReferenceType getTargetReference(EndpointReferenceType t) {
        EndpointReferenceType ref = null;
        if (null == t) {
            ref = new EndpointReferenceType();
            AttributedURIType address = new AttributedURIType();
            address.setValue(getAddress());
            ref.setAddress(address);
        } else {
            ref = t;
        }
        return ref;
    }

    protected String getAddress() {
        return endpointInfo.getAddress();
    }
    
    
    protected void buildRequestResult(CorbaMessage msg) {        
        Exchange exg = msg.getExchange();        
        ServerRequest request = exg.get(ServerRequest.class);
        try {
            if (!exg.isOneWay()) {                
                CorbaMessage inMsg = (CorbaMessage)msg.getExchange().getInMessage();
                NVList list = inMsg.getList();

                if (msg.getStreamableException() != null) {                    
                    Any exAny = orb.create_any();
                    CorbaStreamable exception = msg.getStreamableException();
                    exAny.insert_Streamable(exception);
                    request.set_exception(exAny);
                    if (msg.getExchange() != null) {
                        msg.getExchange().setOutFaultMessage(msg);
                    }
                } else {
                    CorbaStreamable[] arguments = msg.getStreamableArguments();
                    if (arguments != null) {
                        for (int i = 0; i < arguments.length; ++i) {
                            if (list.item(i).flags() != org.omg.CORBA.ARG_IN.value) {
                                list.item(i).value().insert_Streamable(arguments[i]);
                            }   
                        }
                    }

                    CorbaStreamable resultValue = msg.getStreamableReturn();
                    if (resultValue != null) {
                        Any resultAny = orb.create_any();
                        resultAny.insert_Streamable(resultValue);
                        request.set_result(resultAny);
                    }
                }
            }

        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException("Exception during buildRequestResult", ex);
        }
    }        
    
    private class CorbaOutputStream extends CachedOutputStream {
        
        CorbaOutputStream(Message m) {
        }

        /**
         * Perform any actions required on stream flush (freeze headers, reset
         * output stream ... etc.)
         */
        public void doFlush() throws IOException {

            // do nothing here
        }

        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        public void doClose() throws IOException {
        }

        public void onWrite() throws IOException {

        }
    }
}

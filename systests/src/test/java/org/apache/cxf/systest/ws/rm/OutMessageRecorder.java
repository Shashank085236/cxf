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

package org.apache.cxf.systest.ws.rm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
//import org.apache.cxf.io.AbstractCachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


/**
 * 
 */
public class OutMessageRecorder extends AbstractPhaseInterceptor {
    
    private List<SOAPMessage> outbound;
    private Set<String> before = Collections.singleton(StaxOutInterceptor.class.getName());

    public OutMessageRecorder() {
        outbound = new ArrayList<SOAPMessage>();
        //setPhase(Phase.POST_STREAM);
        setPhase(Phase.PRE_PROTOCOL);
    }
    
    public void handleMessage(Message message) throws Fault {
        OutputStream os = message.getContent(OutputStream.class);
        if (null == os) {
            return;
        }
        ForkOutputStream fos = new ForkOutputStream(os);
        message.setContent(OutputStream.class, fos);

        /*
        if (os instanceof AbstractCachedOutputStream) {
            try {
                String s = ((AbstractCachedOutputStream)os).toString();
                MessageFactory mf = MessageFactory.newInstance();
                ByteArrayInputStream bis = new ByteArrayInputStream(s.getBytes()); 
                SOAPMessage sm = mf.createMessage(null, bis);
                outbound.add(sm);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        */
    }
   
    @Override
    public Set<String> getBefore() {
        return before;
    }

    protected List<SOAPMessage> getOutboundMessages() {
        return outbound;
    } 

    /**
      * Output stream that multicasts its data to several underlying output streams.
     */
    class ForkOutputStream extends OutputStream {

        final OutputStream original;
        final ByteArrayOutputStream bos;
    
        public ForkOutputStream(OutputStream o) {
            original = o;
            bos = new ByteArrayOutputStream();
        }
    
        @Override
        public void close() throws IOException {
            bos.close();
            try {
                MessageFactory mf = MessageFactory.newInstance();
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                SOAPMessage sm = mf.createMessage(null, bis);
                outbound.add(sm);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
            original.close();
        }

        @Override
        public void flush() throws IOException {
            bos.flush();
            original.flush();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            bos.write(b, off, len);
            original.write(b, off, len);
        }
    
        @Override
        public void write(byte[] b) throws IOException {
            bos.write(b);
            original.write(b);
        }
    
        @Override
        public void write(int b) throws IOException {
            bos.write(b);
            original.write(b);
        }

        
    }

}

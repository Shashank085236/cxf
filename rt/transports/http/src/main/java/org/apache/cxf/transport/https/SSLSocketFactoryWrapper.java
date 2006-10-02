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
package org.apache.cxf.transport.https;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.common.logging.LogUtils;

class SSLSocketFactoryWrapper extends SSLSocketFactory {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SSLSocketFactoryWrapper.class);
    
    private SSLSocketFactory sslSocketFactory;
    private String[] ciphers;
    
    public SSLSocketFactoryWrapper(SSLSocketFactory sslSocketFactoryParam, String[] ciphersParam) {
        sslSocketFactory = sslSocketFactoryParam;
        ciphers = ciphersParam;
    }

    
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }
    
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites(); 
    }
    
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
        throws IOException, UnknownHostException  {
        
        SSLSocket socket = null;
        socket = (SSLSocket)sslSocketFactory.createSocket(s, host, port, autoClose);
        if ((socket != null) && (ciphers != null)) {
            socket.setEnabledCipherSuites(ciphers);
        }

        return socket; 
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        SSLSocket socket = null;
        socket = (SSLSocket)sslSocketFactory.createSocket(host, port);
        if ((socket != null) && (ciphers != null)) {
            socket.setEnabledCipherSuites(ciphers);
        }
        if (socket == null) {
            LogUtils.log(LOG, Level.SEVERE, "PROBLEM_CREATING_OUTBOUND_REQUEST_SOCKET", 
                         new Object[]{host, port});
        }
        return socket; 
    }


    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) 
        throws IOException, UnknownHostException {
        SSLSocket socket = null;
        socket = (SSLSocket)sslSocketFactory.createSocket(host, port, localHost, localPort);
        if ((socket != null) && (ciphers != null)) {
            socket.setEnabledCipherSuites(ciphers);
        }

        if (socket == null) {
            LogUtils.log(LOG, Level.SEVERE, "PROBLEM_CREATING_OUTBOUND_REQUEST_SOCKET", 
                         new Object[]{host, port});
        }
        return socket;
    }


    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = null;
        socket = (SSLSocket)sslSocketFactory.createSocket(host, port);
        if ((socket != null) && (ciphers != null)) {
            socket.setEnabledCipherSuites(ciphers);
        }

        if (socket == null) {
            LogUtils.log(LOG, Level.SEVERE, "PROBLEM_CREATING_OUTBOUND_REQUEST_SOCKET", 
                         new Object[]{host, port});
        }
        return socket;
    }


    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) 
        throws IOException {
        SSLSocket socket = null;
        socket = (SSLSocket)sslSocketFactory.createSocket(address, port, localAddress, localPort);
        if ((socket != null) && (ciphers != null)) {
            socket.setEnabledCipherSuites(ciphers);
        }

        if (socket == null) {
            LogUtils.log(LOG, Level.SEVERE, "PROBLEM_CREATING_OUTBOUND_REQUEST_SOCKET", 
                         new Object[]{address, port});
        }
        return socket;
    }
    
    /*
     * For testing only
     */
    protected void addLogHandler(Handler handler) {
        LOG.addHandler(handler);
    }
}

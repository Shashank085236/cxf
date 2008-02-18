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
package org.apache.cxf.ws.security.wss4j;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Performs WS-Security inbound actions.
 * 
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public class WSS4JInInterceptor extends AbstractWSS4JInterceptor {

    public static final String TIMESTAMP_RESULT = "wss4j.timestamp.result";
    public static final String SIGNATURE_RESULT = "wss4j.signature.result";

    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JInInterceptor.class);
    private static final Logger TIME_LOG = LogUtils.getL7dLogger(WSS4JInInterceptor.class,
                                                                 null,
                                                                 WSS4JInInterceptor.class.getName()
                                                                     + "-Time");
    
    public WSS4JInInterceptor() {
        super();

        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJInInterceptor.class.getName());
    }

    public WSS4JInInterceptor(Map<String, Object> properties) {
        this();
        setProperties(properties);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(SoapMessage msg) throws Fault {
        boolean doDebug = LOG.isLoggable(Level.FINE);
        boolean doTimeLog = TIME_LOG.isLoggable(Level.FINE);

        SoapVersion version = msg.getVersion();
        if (doDebug) {
            LOG.fine("WSS4JInSecurityHandler: enter invoke()");
        }

        long t0 = 0;
        long t1 = 0;
        long t2 = 0;
        long t3 = 0;

        if (doTimeLog) {
            t0 = System.currentTimeMillis();
        }

        RequestData reqData = new RequestData();
        /*
         * The overall try, just to have a finally at the end to perform some
         * housekeeping.
         */
        try {
            reqData.setMsgContext(msg);

            Vector actions = new Vector();
            String action = getAction(msg, version);

            int doAction = WSSecurityUtil.decodeAction(action, actions);

            String actor = (String)getOption(WSHandlerConstants.ACTOR);

            SOAPMessage doc = msg.getContent(SOAPMessage.class);

            if (doc == null) {
                throw new SoapFault(new Message("NO_SAAJ_DOC", LOG), version.getReceiver());
            }

            CallbackHandler cbHandler = getCallback(reqData, doAction);

            /*
             * Get and check the Signature specific parameters first because
             * they may be used for encryption too.
             */
            doReceiverAction(doAction, reqData);

            Vector wsResult = null;
            if (doTimeLog) {
                t1 = System.currentTimeMillis();
            }

            try {
                wsResult = secEngine.processSecurityHeader(doc.getSOAPPart(), actor, cbHandler, reqData
                    .getSigCrypto(), reqData.getDecCrypto());
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new SoapFault(new Message("SECURITY_FAILED", LOG), ex, version.getSender());
            }

            if (doTimeLog) {
                t2 = System.currentTimeMillis();
            }

            if (wsResult == null) { // no security header found
                if (doAction == WSConstants.NO_SECURITY) {
                    return;
                } else {
                    LOG.warning("Request does not contain required Security header");
                    throw new SoapFault(new Message("NO_SECURITY", LOG), version.getSender());
                }
            }

            if (reqData.getWssConfig().isEnableSignatureConfirmation()) {
                checkSignatureConfirmation(reqData, wsResult);
            }
            
            //
            // Now remove the Signature Confirmation results. This is needed to work around the
            // wsResult.size() != actions.size() comparison below. The real issue is to fix the
            // broken checkReceiverResults method in WSS4J.
            //
            removeSignatureConfirmationResults(wsResult);

            /*
             * Now we can check the certificate used to sign the message. In the
             * following implementation the certificate is only trusted if
             * either it itself or the certificate of the issuer is installed in
             * the keystore. Note: the method verifyTrust(X509Certificate)
             * allows custom implementations with other validation algorithms
             * for subclasses.
             */

            // Extract the signature action result from the action vector
            WSSecurityEngineResult actionResult = WSSecurityUtil
                .fetchActionResult(wsResult, WSConstants.SIGN);

            if (actionResult != null) {
                X509Certificate returnCert = actionResult.getCertificate();

                if (returnCert != null && !verifyTrust(returnCert, reqData)) {
                    LOG.warning("The certificate used for the signature is not trusted");
                    throw new SoapFault(new Message("UNTRUSTED_CERT", LOG), version.getSender());
                }
                msg.put(SIGNATURE_RESULT, actionResult);
            }

            /*
             * Perform further checks on the timestamp that was transmitted in
             * the header. In the following implementation the timestamp is
             * valid if it was created after (now-ttl), where ttl is set on
             * server side, not by the client. Note: the method
             * verifyTimestamp(Timestamp) allows custom implementations with
             * other validation algorithms for subclasses.
             */

            // Extract the timestamp action result from the action vector
            actionResult = WSSecurityUtil.fetchActionResult(wsResult, WSConstants.TS);

            if (actionResult != null) {
                Timestamp timestamp = actionResult.getTimestamp();

                if (timestamp != null && !verifyTimestamp(timestamp, decodeTimeToLive(reqData))) {
                    LOG.warning("The timestamp could not be validated");
                    throw new SoapFault(new Message("INVALID_TIMESTAMP", LOG), version.getSender());
                }
                msg.put(TIMESTAMP_RESULT, actionResult);
            }

            /*
             * now check the security actions: do they match, in right order?
             *
             * Added size comparison to work around
             * https://issues.apache.org/jira/browse/WSS-70
             */
            if (wsResult.size() != actions.size() || !checkReceiverResults(wsResult, actions)) {
                LOG.warning("Security processing failed (actions mismatch)");
                throw new SoapFault(new Message("ACTION_MISMATCH", LOG), version.getSender());

            }

            doResults(msg, actor, doc, wsResult);

            if (doTimeLog) {
                t3 = System.currentTimeMillis();
                TIME_LOG.fine("Receive request: total= " + (t3 - t0) 
                        + " request preparation= " + (t1 - t0)
                        + " request processing= " + (t2 - t1) 
                        + " header, cert verify, timestamp= " + (t3 - t2) + "\n");
            }

            if (doDebug) {
                LOG.fine("WSS4JInHandler: exit invoke()");
            }

        } catch (WSSecurityException e) {
            LOG.log(Level.WARNING, "", e);
            throw new SoapFault(new Message("WSSECURITY_EX", LOG), e, version.getSender());
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, version.getSender());
        } catch (SOAPException e) {
            throw new SoapFault(new Message("SAAJ_EX", LOG), e, version.getSender());
        } finally {
            reqData.clear();
            reqData = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void doResults(SoapMessage msg, String actor, SOAPMessage doc, Vector wsResult)
        throws SOAPException, XMLStreamException {
        /*
         * All ok up to this point. Now construct and setup the security result
         * structure. The service may fetch this and check it.
         */
        List<Object> results = (Vector<Object>)msg.get(WSHandlerConstants.RECV_RESULTS);
        if (results == null) {
            results = new Vector<Object>();
            msg.put(WSHandlerConstants.RECV_RESULTS, results);
        }
        WSHandlerResult rResult = new WSHandlerResult(actor, wsResult);
        results.add(0, rResult);

        SOAPBody body = doc.getSOAPBody();

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(body));
        // advance just past body
        int evt = reader.next();
        int i = 0;
        while (reader.hasNext() && i < 1
               && (evt != XMLStreamConstants.END_ELEMENT || evt != XMLStreamConstants.START_ELEMENT)) {
            reader.next();
            i++;
        }
        msg.setContent(XMLStreamReader.class, reader);
    }

    private String getAction(SoapMessage msg, SoapVersion version) {
        String action = (String)getOption(WSHandlerConstants.ACTION);
        if (action == null) {
            action = (String)msg.get(WSHandlerConstants.ACTION);
        }
        if (action == null) {
            LOG.warning("No security action was defined!");
            throw new SoapFault("No security action was defined!", version.getReceiver());
        }
        return action;
    }

    private CallbackHandler getCallback(RequestData reqData, int doAction) throws WSSecurityException {
        /*
         * To check a UsernameToken or to decrypt an encrypted message we need a
         * password.
         */
        CallbackHandler cbHandler = null;
        if ((doAction & (WSConstants.ENCR | WSConstants.UT)) != 0) {
            cbHandler = getPasswordCB(reqData);
        }
        return cbHandler;
    }
    
    private void removeSignatureConfirmationResults(List<Object> wsResult) {
        //
        // Now remove the Signature Confirmation results. This is needed to work around the
        // wsResult.size() != actions.size() comparison below. The real issue is to fix the
        // broken checkReceiverResults method in WSS4J.
        //
        for (int i = 0; i < wsResult.size(); i++) {
            if (((WSSecurityEngineResult) wsResult.get(i)).getAction() == WSConstants.SC) {
                wsResult.remove(i);
            }
        }
    }
}

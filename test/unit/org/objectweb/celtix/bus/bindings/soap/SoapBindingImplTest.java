package org.objectweb.celtix.bus.bindings.soap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebFault;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import junit.framework.TestCase;

import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.ObjectMessageContextImpl;
import org.objectweb.celtix.bus.jaxws.JAXBDataBindingCallback;
import org.objectweb.celtix.context.GenericMessageContext;
import org.objectweb.hello_world_soap_http.BadRecordLitFault;
import org.objectweb.hello_world_soap_http.Greeter;
import org.objectweb.hello_world_soap_http.NoSuchCodeLitFault;
import org.objectweb.hello_world_soap_http.types.ErrorCode;
import org.objectweb.hello_world_soap_http.types.NoSuchCodeLit;

public class SoapBindingImplTest extends TestCase {
    private SOAPBindingImpl binding;
    private ObjectMessageContextImpl objContext;
    private SOAPMessageContextImpl soapContext;
    public SoapBindingImplTest(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SoapBindingImplTest.class);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        
        binding = new SOAPBindingImpl();
        objContext = new ObjectMessageContextImpl();
        soapContext = new SOAPMessageContextImpl(new GenericMessageContext());
        
        objContext.setMethod(SOAPMessageUtil.getMethod(Greeter.class, "greetMe"));
    }

    public void testGetMessageFactory() throws Exception {
        assertNotNull(binding.getSOAPFactory());
    }

    public void testIsCompatibleWithAddress() throws Exception {
        String address = new String("http:\\www.iona.com\\soap");
        assertTrue(binding.isCompatibleWithAddress(address));
        
        address = new String("https:\\www.iona.com\\soap");
        assertTrue(binding.isCompatibleWithAddress(address));

        address = new String("12343254");
        assertFalse(binding.isCompatibleWithAddress(address));        
    }
    
    public void testMarshalWrapDocLitInputMessage() throws Exception {
        //Test The InputMessage of GreetMe Operation
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, false);

        String arg0 = new String("TestSOAPInputPMessage");
        objContext.setMessageObjects(arg0);

        SOAPMessage msg = binding.marshalMessage(objContext,
                                                 soapContext,
                                                 new JAXBDataBindingCallback(objContext.getMethod(),
                                                                             DataBindingCallback.Mode.PARTS));
        soapContext.setMessage(msg);
        assertNotNull(msg);

        assertTrue(msg.getSOAPBody().hasChildNodes());
        NodeList list = msg.getSOAPBody().getChildNodes();
        assertEquals(1, list.getLength());
        Node wrappedNode = list.item(0).getFirstChild();
        assertTrue(wrappedNode.hasChildNodes());
        assertEquals(arg0, wrappedNode.getFirstChild().getNodeValue());
    }

    public void testMarshalWrapDocLitOutputMessage() throws Exception {
        //Test The Output of GreetMe Operation
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, true);

        String arg0 = new String("TestSOAPOutputMessage");
        objContext.setReturn(arg0);
        
        SOAPMessage msg = binding.marshalMessage(objContext,
                                                 soapContext,
                                                 new JAXBDataBindingCallback(objContext.getMethod(),
                                                                             DataBindingCallback.Mode.PARTS));
        soapContext.setMessage(msg);
        assertNotNull(msg);
        assertTrue(msg.getSOAPBody().hasChildNodes());
        NodeList list = msg.getSOAPBody().getChildNodes();
        assertEquals(1, list.getLength());
        Node wrappedNode = list.item(0).getFirstChild();
        assertTrue(wrappedNode.hasChildNodes());
        assertEquals(arg0, wrappedNode.getFirstChild().getNodeValue());
    }

    public void testParseWrapDocLitInputMessage() throws Exception {
        //Test The InputMessage of GreetMe Operation
        //Assumption the Wrapper element and the inner element are in the same namespace
        //elementFormDefault is qualified
        
        QName wrapName = new QName("http://objectweb.org/hello_world_soap_http/types", "greetMe");
        QName elName = new QName("http://objectweb.org/hello_world_soap_http/types", "requestType");
        String data = new String("TestSOAPInputMessage");
        String str = SOAPMessageUtil.createWrapDocLitSOAPMessage(wrapName, elName, data);        
        
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());
        binding.parseMessage(in, soapContext);

        SOAPMessage msg = soapContext.getMessage();
        assertNotNull(msg);
        assertTrue(msg.getSOAPBody().hasChildNodes());
        NodeList list = msg.getSOAPBody().getChildNodes();

        assertEquals(1, list.getLength());
        Node wrappedNode = list.item(0).getFirstChild();
        assertTrue(wrappedNode.hasChildNodes());
        assertEquals(data, wrappedNode.getFirstChild().getNodeValue());
    }

    public void testUnmarshalWrapDocLitInputMessage() throws Exception {
        //Test The InputMessage of GreetMe Operation
        QName wrapName = new QName("http://objectweb.org/hello_world_soap_http/types", "greetMe");        
        QName elName = new QName("http://objectweb.org/hello_world_soap_http/types", "requestType");
        String data = new String("TestSOAPInputMessage");
        String str = SOAPMessageUtil.createWrapDocLitSOAPMessage(wrapName, elName, data);
        
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, false);

        assertNotNull(binding.getMessageFactory());
        SOAPMessage soapMessage = binding.getMessageFactory().createMessage(null, in);
        soapContext.setMessage(soapMessage);

        binding.unmarshalMessage(soapContext, objContext,
                                 new JAXBDataBindingCallback(objContext.getMethod(),
                                                             DataBindingCallback.Mode.PARTS));
        
        Object[] params = objContext.getMessageObjects();
        assertNotNull(params);
        assertNull(objContext.getReturn());
        assertEquals(1, params.length);
        assertEquals(data, (String)params[0]);
    }    

    public void testUnmarshalWrapDocLiteralOutputMessage() throws Exception {

        QName wrapName = 
            new QName("http://objectweb.org/hello_world_soap_http/types", "greetMeResponse");        
        QName elName = 
            new QName("http://objectweb.org/hello_world_soap_http/types", "responseType");
        String data = new String("TestSOAPOutputMessage");
        String str = SOAPMessageUtil.createWrapDocLitSOAPMessage(wrapName, elName, data);
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());

        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, true);        
        assertNotNull(binding.getMessageFactory());
        SOAPMessage soapMessage = binding.getMessageFactory().createMessage(null, in);
        soapContext.setMessage(soapMessage);

        binding.unmarshalMessage(soapContext, objContext,
                                 new JAXBDataBindingCallback(objContext.getMethod(),
                                                             DataBindingCallback.Mode.PARTS));
        
        Object[] params = objContext.getMessageObjects();
        //REVISIT Should it be null;
        assertNotNull(params);
        assertEquals(0, params.length);
        assertNotNull(objContext.getReturn());
        assertEquals(data, (String)objContext.getReturn());
    }    
    
    public void testMarshalDocLiteralUserFaults() throws Exception {
        //Test The InputMessage of GreetMe Operation
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, false);

        String exMessage = new String("Test Exception");
        ErrorCode ec = new ErrorCode();
        ec.setMajor((short)1);
        ec.setMinor((short)1);
        NoSuchCodeLit nscl = new NoSuchCodeLit();
        nscl.setCode(ec);
        NoSuchCodeLitFault ex = new NoSuchCodeLitFault(exMessage, nscl);
        objContext.setException(ex);

        SOAPMessage msg = binding.marshalFault(objContext,
                                               soapContext,
                                               new JAXBDataBindingCallback(objContext.getMethod(),
                                                                           DataBindingCallback.Mode.PARTS));
        soapContext.setMessage(msg);

        assertNotNull(msg);
        assertTrue(msg.getSOAPBody().hasFault());
        SOAPFault fault = msg.getSOAPBody().getFault();
        assertNotNull(fault);
        assertEquals(exMessage, fault.getFaultString());
        assertTrue(fault.hasChildNodes());
        Detail detail = fault.getDetail();
        assertNotNull(detail);
        
        NodeList list = detail.getChildNodes();
        assertEquals(1, list.getLength()); 
        
        WebFault wfAnnotation = ex.getClass().getAnnotation(WebFault.class);
        assertEquals(wfAnnotation.targetNamespace(), list.item(0).getNamespaceURI());
        assertEquals(wfAnnotation.name(), list.item(0).getLocalName());
    }    
    
    public void testMarshalSystemFaults() throws Exception {
        //Test The InputMessage of GreetMe Operation
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, false);

        SOAPException se = new SOAPException("SAAJ Exception");
        objContext.setException(se);

        SOAPMessage msg = binding.marshalFault(objContext, soapContext,
                                               new JAXBDataBindingCallback(objContext.getMethod(),
                                                                           DataBindingCallback.Mode.PARTS));
        soapContext.setMessage(msg);
        
        assertNotNull(msg);
        assertTrue(msg.getSOAPBody().hasFault());
        SOAPFault fault = msg.getSOAPBody().getFault();
        assertNotNull(fault);
        assertEquals(se.getMessage(), fault.getFaultString());
        assertTrue(fault.hasChildNodes());
        NodeList list = fault.getChildNodes();
        assertEquals(2, list.getLength());         
    }

    public void testUnmarshalDocLiteralUserFaults() throws Exception {
        //Test The InputMessage of GreetMe Operation
        soapContext.put(ObjectMessageContextImpl.MESSAGE_INPUT, true);
        objContext.setMethod(SOAPMessageUtil.getMethod(Greeter.class, "testDocLitFault"));

        InputStream is =  getClass().getResourceAsStream("resources/NoSuchCodeDocLiteral.xml");
        SOAPMessage faultMsg = binding.getMessageFactory().createMessage(null,  is);
        soapContext.setMessage(faultMsg);

        binding.unmarshalFault(soapContext, objContext,
                               new JAXBDataBindingCallback(objContext.getMethod(),
                                                           DataBindingCallback.Mode.PARTS));
        assertNotNull(objContext.getException());
        Object faultEx = objContext.getException();
        assertTrue(NoSuchCodeLitFault.class.isAssignableFrom(faultEx.getClass()));
        NoSuchCodeLitFault nscf = (NoSuchCodeLitFault)faultEx;
        assertNotNull(nscf.getFaultInfo());
        NoSuchCodeLit faultInfo = nscf.getFaultInfo();

        assertNotNull(faultInfo.getCode());
        ErrorCode ec = faultInfo.getCode();
        assertEquals(ec.getMajor(), (short)666);
        assertEquals(ec.getMinor(), (short)999);
        
        assertEquals(nscf.getMessage(), "Test Exception");
        
        is =  getClass().getResourceAsStream("resources/BadRecordDocLiteral.xml");
        faultMsg = binding.getMessageFactory().createMessage(null,  is);
        soapContext.setMessage(faultMsg);
        binding.unmarshalFault(soapContext, objContext,
                               new JAXBDataBindingCallback(objContext.getMethod(),
                                                           DataBindingCallback.Mode.PARTS));
        assertNotNull(objContext.getException());
        faultEx = objContext.getException();
        assertTrue(BadRecordLitFault.class.isAssignableFrom(faultEx.getClass()));
        BadRecordLitFault brlf = (BadRecordLitFault)faultEx;
        assertEquals(brlf.getFaultInfo(), "BadRecordTested");
    }    
}

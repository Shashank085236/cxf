package org.objectweb.celtix.jaxb;

import java.lang.reflect.Method;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;

import org.w3c.dom.Node;

import junit.framework.TestCase;

import org.objectweb.celtix.bindings.DataBindingCallback;
import org.objectweb.celtix.bindings.DataReader;
import org.objectweb.celtix.bindings.DataWriter;
import org.objectweb.celtix.datamodel.soap.SOAPConstants;
import org.objectweb.celtix.jaxb.io.EventDataReader;
import org.objectweb.celtix.jaxb.io.EventDataWriter;
import org.objectweb.celtix.testutil.common.TestUtil;
import org.objectweb.hello_world_rpclit.GreeterRPCLit;
import org.objectweb.hello_world_soap_http.Greeter;
import org.objectweb.hello_world_soap_http.NoSuchCodeLitFault;

public class JAXBDataBindingCallbackTest extends TestCase {
    private DataBindingCallback msgInfo;
    private DataBindingCallback rpcMsgInfo;
    private final String methodNameString = "greetMe";


    public JAXBDataBindingCallbackTest(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JAXBDataBindingCallbackTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        Method m = TestUtil.getMethod(Greeter.class, "greetMe");
        msgInfo = new JAXBDataBindingCallback(m, DataBindingCallback.Mode.PARTS, null);

        m = TestUtil.getMethod(GreeterRPCLit.class, "greetMe");
        rpcMsgInfo = new JAXBDataBindingCallback(m, DataBindingCallback.Mode.PARTS, null);
    }

    public void testGetSoapStyle() throws Exception {
        SOAPBinding.Style style = msgInfo.getSOAPStyle();
        assertEquals(SOAPBinding.Style.DOCUMENT, style);
    }

    public void testGetSoapUse() throws Exception {
        SOAPBinding.Use use = msgInfo.getSOAPUse();
        assertEquals(SOAPBinding.Use.LITERAL, use);
    }

    public void testGetSOAPParameterStyle() throws Exception {
        SOAPBinding.ParameterStyle paramStyle = msgInfo.getSOAPParameterStyle();
        assertEquals(SOAPBinding.ParameterStyle.WRAPPED, paramStyle);
    }

    public void testGetWebResult() throws Exception {
       //Wrapped Doc-Lit. : Should consider Namespace.
        assertNotNull(msgInfo.getWebResult());
        QName returnType = msgInfo.getWebResultQName();
        assertEquals(
                new QName("http://objectweb.org/hello_world_soap_http/types", "responseType"),
                returnType);

        // RPC-Lit Test if WebResult returns the partname with no namespce associated.
        assertNotNull(rpcMsgInfo.getWebResult());
        QName rpcReturnType = rpcMsgInfo.getWebResultQName();
        assertEquals(new QName("", "out"), rpcReturnType);

    }

    public void testGetWebParam() throws Exception {
        WebParam inParam = msgInfo.getWebParam(0);
        assertEquals(
                new QName("http://objectweb.org/hello_world_soap_http/types", "requestType"),
                new QName(inParam.targetNamespace(), inParam.name()));
        assertEquals(WebParam.Mode.IN, inParam.mode());
        assertFalse(inParam.header());
    }

    public void testGetOperationName() throws Exception {
        //Wrapped Doc Lit. Case.
        String opName = msgInfo.getOperationName();
        assertEquals(opName, methodNameString);

        //RPC-Lit case without any customisation.
        //(It contains WebMethod annotation without any operationName
        //so should return method name)
        String opNameRPC = rpcMsgInfo.getOperationName();
        assertEquals(opNameRPC, methodNameString);
    }

    public void testGetOperationNameCustomised() {

        JAXBDataBindingCallback customMsgInfo = null;
        Method [] methodList = CustomAnnotationTestHelper.class.getDeclaredMethods();

        for (Method mt : methodList) {
            if (mt.getName().equals(methodNameString)) {
                customMsgInfo = new JAXBDataBindingCallback(mt,
                                                            DataBindingCallback.Mode.PARTS,
                                                            null);
                break;
            }
        }

        String opNameRPC = customMsgInfo.getOperationName();
        assertEquals(opNameRPC, "customGreetMe");

    }

    public void testGetRequestWrapperQName() throws Exception {
        QName reqWrapper = msgInfo.getRequestWrapperQName();
        assertNotNull(reqWrapper);
        assertEquals(
                new QName("http://objectweb.org/hello_world_soap_http/types", "greetMe"),
                reqWrapper);
    }

    public void testGetResponseWrapperQName() throws Exception {
        QName respWrapper = msgInfo.getResponseWrapperQName();
        assertNotNull(respWrapper);
        assertEquals(
                new QName("http://objectweb.org/hello_world_soap_http/types", "greetMeResponse"),
                respWrapper);
    }

    public void testGetResponseWrapperType() throws Exception {
        String respWrapperType = ((JAXBDataBindingCallback)msgInfo).getResponseWrapperType();
        assertNotNull(respWrapperType);
        assertEquals(
                "org.objectweb.hello_world_soap_http.types.GreetMeResponse",
                respWrapperType);
    }

    public void testDefaults() throws Exception {
        JAXBDataBindingCallback info = null;

        Method[] declMethods = String.class.getDeclaredMethods();
        for (Method method : declMethods) {
            if (method.getName().equals("length")) {
                info = new JAXBDataBindingCallback(method,
                                                   DataBindingCallback.Mode.PARTS,
                                                   null);
                break;
            }
        }

        assertNotNull(info);
        assertEquals(SOAPBinding.Style.DOCUMENT, info.getSOAPStyle());
        assertEquals(SOAPBinding.Use.LITERAL, info.getSOAPUse());
        assertEquals(SOAPBinding.ParameterStyle.WRAPPED, info.getSOAPParameterStyle());
        assertEquals("length", info.getOperationName());
        assertEquals("", info.getSOAPAction());
        assertNull(info.getWebResult());
        assertEquals(SOAPConstants.EMPTY_QNAME, info.getWebResultQName());
        assertNull(info.getWebParam(1));
        assertEquals(SOAPConstants.EMPTY_QNAME, info.getRequestWrapperQName());
        assertEquals(SOAPConstants.EMPTY_QNAME, info.getResponseWrapperQName());
        assertEquals("", info.getRequestWrapperType());
        assertEquals("", info.getResponseWrapperType());
    }

    public void testHasWebFault() throws Exception {
        JAXBDataBindingCallback jaxbmi = (JAXBDataBindingCallback)msgInfo;
        QName faultName = new QName("http://objectweb.org/hello_world_soap_http/types", "NoSuchCodeLit");
        assertNull(jaxbmi.getWebFault(faultName));

        jaxbmi = new JAXBDataBindingCallback(TestUtil.getMethod(Greeter.class, "testDocLitFault"),
                                              DataBindingCallback.Mode.PARTS,
                                              null);
        Class<?> clazz = jaxbmi.getWebFault(faultName);
        assertNotNull(clazz);
        assertTrue(NoSuchCodeLitFault.class.isAssignableFrom(clazz));
    }

    public void testCreateWriter() throws Exception {
        DataWriter<XMLEventWriter> writer = msgInfo.createWriter(XMLEventWriter.class);
        assertNotNull("Should have a valid DataWriter", writer);
        assertTrue("Should be a instance of EventDataWriter", 
                   writer instanceof EventDataWriter);        
    }

    public void testCreatReader() throws Exception {
        DataReader<XMLEventReader> reader = msgInfo.createReader(XMLEventReader.class);
        assertNotNull("Should have a valid DataReader", reader);
        assertTrue("Should be a instance of EventDataReader", 
                   reader instanceof EventDataReader);
    }

    public void testGetSupportedFormats() throws Exception {
        Class<?>[] clazz = msgInfo.getSupportedFormats();
        assertNotNull(clazz);
        assertTrue("XMLEventReader should be supported", 
                   isSupportedFormat(XMLEventWriter.class));        
        assertTrue("XMLEventReader should be supported", 
                   isSupportedFormat(XMLEventReader.class));
        assertTrue("DOM Node should be supported", 
                   isSupportedFormat(Node.class));
        assertTrue("FaultDetail should be supported", 
                   isSupportedFormat(Detail.class));
        assertTrue("SOAPFault should be supported", 
                   isSupportedFormat(SOAPFault.class));
    }
    
    private boolean isSupportedFormat(Class<?> clazz) {
        if (clazz == XMLEventReader.class
            || clazz == XMLEventWriter.class
            || clazz == Node.class
            || clazz == Detail.class
            || clazz == SOAPFault.class) {
            return true;
        }
        
        return false;
    }
    
    @WebService(name = "CustomAnnotationTestHelper",
                          targetNamespace = "http://objectweb.org/hello_world_rpclit",
                          wsdlLocation = "C:\\celtix\\rpc-lit\\trunk/test/wsdl/hello_world_rpc_lit.wsdl")
    @SOAPBinding(style = Style.RPC)
    public interface CustomAnnotationTestHelper {
        @WebMethod(operationName = "customGreetMe")
        @WebResult(name = "out",
                            targetNamespace = "http://objectweb.org/hello_world_rpclit",
                            partName = "out")
        String greetMe(
            @WebParam(name = "in", partName = "in")
            String in);
    }
}

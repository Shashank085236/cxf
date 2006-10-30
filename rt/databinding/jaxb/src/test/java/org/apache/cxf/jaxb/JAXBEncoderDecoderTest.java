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

package org.apache.cxf.jaxb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.ws.RequestWrapper;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.TestCase;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.StringStruct;
import org.apache.type_test.doc.TypeTestPortType;

/**
 * JAXBEncoderDecoderTest
 * @author apaibir
 */
public class JAXBEncoderDecoderTest extends TestCase {
    public static final QName  SOAP_ENV = 
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
    public static final QName  SOAP_BODY = 
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

    RequestWrapper wrapperAnnotation;
    JAXBContext context;
    Schema schema;
    
    public JAXBEncoderDecoderTest(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JAXBEncoderDecoderTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        context = JAXBContext.newInstance(new Class[] {
            GreetMe.class,
            GreetMeResponse.class,
            StringStruct.class
        });
        Method method = TestUtil.getMethod(Greeter.class, "greetMe");
        wrapperAnnotation = method.getAnnotation(RequestWrapper.class);
        
        InputStream is =  getClass().getResourceAsStream("resources/StringStruct.xsd");
        StreamSource schemaSource = new StreamSource(is);
        assertNotNull(schemaSource);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = factory.newSchema(schemaSource);
        assertNotNull(schema);
    }

    public void testMarshallIntoDOM() throws Exception {
        String str = new String("Hello");
        QName inCorrectElName = new QName("http://test_jaxb_marshall", "requestType");
        MessagePartInfo part = new MessagePartInfo(inCorrectElName, null);
        part.setElement(true);
        part.setElementQName(inCorrectElName);
        
        SOAPFactory soapElFactory = SOAPFactory.newInstance();
        Element elNode = soapElFactory.createElement(inCorrectElName);
        assertNotNull(elNode);

        Node node;
        try {
            JAXBEncoderDecoder.marshall(context, null, null, part, elNode);
            fail("Should have thrown a Fault");
        } catch (Fault ex) {
            //expected - not a valid object
        }

        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        part.setElementQName(elName);
        JAXBEncoderDecoder.marshall(context, null, obj, part, elNode);
        node = elNode.getLastChild();
        //The XML Tree Looks like
        //<GreetMe><requestType>Hello</requestType></GreetMe>
        assertEquals(Node.ELEMENT_NODE, node.getNodeType());
        Node childNode = node.getFirstChild();
        assertEquals(Node.ELEMENT_NODE, childNode.getNodeType());
        childNode = childNode.getFirstChild();
        assertEquals(Node.TEXT_NODE, childNode.getNodeType());
        assertEquals(str, childNode.getNodeValue());

        // Now test schema validation during marshaling
        StringStruct stringStruct = new StringStruct();
        // Don't initialize one of the structure members.
        //stringStruct.setArg0("hello");
        stringStruct.setArg1("world");
        // Marshal without the schema should work.
        JAXBEncoderDecoder.marshall(context, null, stringStruct, part,  elNode);
        try {
            // Marshal with the schema should get an exception.
            JAXBEncoderDecoder.marshall(context, schema, stringStruct, part,  elNode);
            fail("Marshal with schema should have thrown a Fault");
        } catch (Fault ex) {
            //expected - not a valid object
        }
    }

    public void testMarshallIntoStax() throws Exception {
        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
                
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(baos);

        //STARTDOCUMENT/ENDDOCUMENT is not required
        //writer.add(eFactory.createStartDocument("utf-8", "1.0"));        
        JAXBEncoderDecoder.marshall(context, null, obj, part, writer);
        //writer.add(eFactory.createEndDocument());
        writer.flush();
        writer.close();
        
        //System.out.println(baos.toString());
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLInputFactory ipFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = ipFactory.createXMLEventReader(bais);
        
        Unmarshaller um = context.createUnmarshaller();        
        Object val = um.unmarshal(reader, GreetMe.class);
        assertTrue(val instanceof JAXBElement);
        val = ((JAXBElement)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(), 
                     ((GreetMe)val).getRequestType());
    }

    public void testUnmarshallFromStax() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);

        InputStream is =  getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = 
            factory.createXMLStreamReader(is);

        QName[] tags = {SOAP_ENV, SOAP_BODY};
        StaxStreamFilter filter = new StaxStreamFilter(tags);
        reader = factory.createFilteredReader(reader, filter);

        //Remove START_DOCUMENT & START_ELEMENT pertaining to Envelope and Body Tags.

        part.setTypeClass(GreetMe.class);
        Object val = JAXBEncoderDecoder.unmarshall(context, null, reader, part, null);
        assertNotNull(val);
        assertTrue(val instanceof GreetMe);
        assertEquals("TestSOAPInputPMessage", 
                     ((GreetMe)val).getRequestType());

        is.close();
    }
    
    public void testMarshalRPCLit() throws Exception {
        SOAPFactory soapElFactory = SOAPFactory.newInstance();
        QName elName = new QName("http://test_jaxb_marshall", "in");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        
        SOAPElement elNode = soapElFactory.createElement(elName);
        JAXBEncoderDecoder.marshall(context, null, new String("TestSOAPMessage"), part,  elNode);
        
        assertNotNull(elNode.getChildNodes());
        assertEquals("TestSOAPMessage", elNode.getFirstChild().getFirstChild().getNodeValue());
    }

    private SOAPElement getTestSOAPElement(QName elName) throws Exception {
        //Create a XML Tree of
        //<GreetMe><requestType>Hello</requestType></GreetMe>
        SOAPFactory soapElFactory = SOAPFactory.newInstance();
        SOAPElement elNode = soapElFactory.createElement(elName);
        elNode.addNamespaceDeclaration("", elName.getNamespaceURI()); 

        return elNode;
    }
    
    public void testUnMarshall() throws Exception {
        //Hello World Wsdl generated namespace
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());

        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(Class.forName(wrapperAnnotation.className()));
        
        SOAPElement elNode = getTestSOAPElement(elName);
        String str = new String("Hello Test");
        elNode.addChildElement("requestType").setValue(str);

        Object obj = JAXBEncoderDecoder.unmarshall(context, null,
                         elNode, part, null);
        assertNotNull(obj);

        //Add a Node and then test
        assertEquals(GreetMe.class,  obj.getClass());
        assertEquals(str, ((GreetMe)obj).getRequestType());
        
        part.setTypeClass(String.class);
        Node n = null;
        try {
            JAXBEncoderDecoder.unmarshall(context, null, n, part, null);
            fail("Should have received a Fault");
        } catch (Fault pe) {
            //Expected Exception
        } catch (Exception ex) {
            fail("Should have received a Fault, not: " + ex);
        }
        
        // Now test schema validation during unmarshaling
        elName = new QName(wrapperAnnotation.targetNamespace(),
                           "stringStruct");
        // Create an XML Tree of
        // <StringStruct><arg1>World</arg1></StringStruct>
//         elNode = soapElFactory.createElement(elName);
//         elNode.addNamespaceDeclaration("", elName.getNamespaceURI());

        part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(Class.forName("org.apache.hello_world_soap_http.types.StringStruct"));
        
        elNode = getTestSOAPElement(elName);
        str = new String("World");
        elNode.addChildElement("arg1").setValue(str);
        // Should unmarshal without problems when no schema used.
        obj = JAXBEncoderDecoder.unmarshall(context, null, elNode, part, null);
        assertNotNull(obj);
        assertEquals(StringStruct.class,  obj.getClass());
        assertEquals(str, ((StringStruct)obj).getArg1());
        
        try {
            // unmarshal with schema should raise exception.
            obj = JAXBEncoderDecoder.unmarshall(context, schema, elNode, part, null);
            fail("Should have thrown a Fault");
        } catch (Fault ex) {
            // expected - schema validation should fail.
        }
    }

    public void testUnmarshallWithoutClzInfo() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        

        SOAPElement elNode = getTestSOAPElement(elName);
        String str = new String("Hello Test");
        elNode.addChildElement("requestType").setValue(str);

        Object obj = JAXBEncoderDecoder.unmarshall(context, null, elNode);
        assertNotNull(obj);
        assertEquals(GreetMe.class,  obj.getClass());
        assertEquals(str, ((GreetMe)obj).getRequestType());
    }

    public void testMarshallWithoutQNameInfo() throws Exception {
        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(baos);

        //STARTDOCUMENT/ENDDOCUMENT is not required
        //writer.add(eFactory.createStartDocument("utf-8", "1.0"));        
        JAXBEncoderDecoder.marshall(context, null, obj, writer);
        //writer.add(eFactory.createEndDocument());
        writer.flush();
        writer.close();
        
        //System.out.println(baos.toString());
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLInputFactory ipFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = ipFactory.createXMLEventReader(bais);
        
        Unmarshaller um = context.createUnmarshaller();        
        Object val = um.unmarshal(reader, GreetMe.class);
        assertTrue(val instanceof JAXBElement);
        val = ((JAXBElement)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(), 
                     ((GreetMe)val).getRequestType());
    }
    
    public void testGetClassFromType() throws Exception {
        Method testByte = TestUtil.getMethod(TypeTestPortType.class, "testByte");
        Type[] genericParameterTypes = testByte.getGenericParameterTypes();
        Class<?>[] paramTypes = testByte.getParameterTypes();
 
        int idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertTrue(cls.equals(paramTypes[idx]));
            idx++;
        }
        
        Method testBase64Binary = TestUtil.getMethod(TypeTestPortType.class, "testBase64Binary");
        genericParameterTypes = testBase64Binary.getGenericParameterTypes();
        paramTypes = testBase64Binary.getParameterTypes();
 
        idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertTrue(cls.equals(paramTypes[idx]));
            idx++;
        }
    }
}


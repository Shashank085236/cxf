package org.apache.cxf.bindings.soap2.attachments;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.bindings.attachments.CachedOutputStream;
import org.apache.cxf.bindings.soap2.MultipartMessageInterceptor;
import org.apache.cxf.bindings.soap2.Soap11;
import org.apache.cxf.bindings.soap2.Soap12;
import org.apache.cxf.bindings.soap2.SoapMessage;
import org.apache.cxf.bindings.soap2.TestBase;
import org.apache.cxf.bindings.soap2.TestUtil;
import org.apache.cxf.bindings.soap2.attachments.types.DetailType;
import org.apache.cxf.jaxb.attachments.AttachmentDeserializer;
import org.apache.cxf.jaxb.attachments.AttachmentSerializer;
import org.apache.cxf.jaxb.attachments.JAXBAttachmentMarshaller;
import org.apache.cxf.jaxb.attachments.JAXBAttachmentUnmarshaller;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

public class AttachmentTest extends TestBase {

    private MultipartMessageInterceptor mmi;

    public void setUp() throws Exception {
        super.setUp();
        mmi = new MultipartMessageInterceptor();
        mmi.setPhase("phase1");
        chain.add(mmi);
    }

    public void testDoInterceptOfSoap12() throws Exception {
        try {
            soapMessage = TestUtil.createSoapMessage(new Soap12(), chain, this.getClass());
        } catch (IOException ioe) {
            fail(ioe.getStackTrace().toString());
        }
        InputStream is = soapMessage.getContent(Attachment.class).getDataHandler().getDataSource()
            .getInputStream();
        testHandleMessage(soapMessage, is, true);
    }

    public void testDoInterceptOfSoap11() throws Exception {
        try {
            soapMessage = TestUtil.createSoapMessage(new Soap11(), chain, this.getClass());
        } catch (IOException ioe) {
            fail(ioe.getStackTrace().toString());
        }
        InputStream is = soapMessage.getContent(Attachment.class).getDataHandler().getDataSource()
            .getInputStream();
        testHandleMessage(soapMessage, is, true);
    }

    public void testDoUnmarshallXopEnabled() {
        Object obj = null;
        try {
            soapMessage = TestUtil.createSoapMessage(new Soap12(), chain, this.getClass());
            InputStream is = soapMessage.getContent(Attachment.class).getDataHandler().getDataSource()
                .getInputStream();
            testHandleMessage(soapMessage, is, false);

            JAXBContext context = JAXBContext
                .newInstance("org.apache.cxf.bindings.soap2.attachments.types");
            Unmarshaller u = context.createUnmarshaller();

            JAXBAttachmentUnmarshaller jau = new JAXBAttachmentUnmarshaller(soapMessage);
            u.setAttachmentUnmarshaller(jau);

            XMLStreamReader r = (XMLStreamReader)soapMessage.getContent(XMLStreamReader.class);
            while (r.hasNext()) {
                r.nextTag();
                if (r.getLocalName().equals("Body")) {
                    r.nextTag();
                    break;
                }
            }
            obj = u.unmarshal(r, DetailType.class);

        } catch (UnmarshalException ue) {
            // It's helpful to include the cause in the case of
            // schema validation exceptions.
            String message = "Unmarshalling error ";
            if (ue.getCause() != null) {
                message += ue.getCause();
            }
            fail(ue.getStackTrace().toString());
        } catch (Exception ex) {
            fail(ex.getStackTrace().toString());
        }

        if (obj instanceof JAXBElement<?>) {
            JAXBElement<?> el = (JAXBElement<?>)obj;
            obj = el.getValue();
        }

        assertTrue(obj != null);
        assertTrue(obj instanceof DetailType);
        DetailType detailType = (DetailType)obj;
        assertTrue(detailType.getSName().equals("hello world"));
        assertTrue(detailType.getPhoto() instanceof Image);
        assertTrue(detailType.getSound().length > 0);
    }

    public void testDoMarshallXopEnabled() throws Exception {
        // mashalling data object
        QName elName = new QName("http://celtix.objectweb.org/bindings/soap2/attachments/types", "Detail");
        soapMessage = TestUtil.createEmptySoapMessage(new Soap12(), chain);
        try {
            DetailType detailObj = TestUtil.createDetailObject(this.getClass());
            Class<?> cls = DetailType.class;
            JAXBContext context = JAXBContext
                .newInstance("org.apache.cxf.bindings.soap2.attachments.types");
            Marshaller m = context.createMarshaller();

            JAXBAttachmentMarshaller jam = new JAXBAttachmentMarshaller(soapMessage);
            jam.setXOPPackage(true);
            m.setAttachmentMarshaller(jam);

            Object mObj = detailObj;

            CachedOutputStream cosXml = new CachedOutputStream();
            XMLOutputFactory output = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = output.createXMLStreamWriter(cosXml);

            if (null != cls && !cls.isAnnotationPresent(XmlRootElement.class)) {
                mObj = JAXBElement.class.getConstructor(new Class[] {QName.class, Class.class, Object.class})
                    .newInstance(elName, cls, mObj);
            }
            // No envelop & body generated!
            m.marshal(mObj, writer);
            // Do intercept
            testHandleMessage(soapMessage, cosXml.getInputStream(), true);

        } catch (XMLStreamException xse) {
            fail(xse.getStackTrace().toString());

        } catch (MarshalException me) {
            // It's helpful to include the cause in the case of
            // schema validation exceptions.
            String message = "Marshalling error ";
            if (me.getCause() != null) {
                message += me.getCause();
            }
            fail(me.getStackTrace().toString());

        }
        // serialize message to transport output stream

    }

    private static void testHandleMessage(SoapMessage soapMessage, InputStream is, boolean testXmlConent) {
        try {
            CachedOutputStream cos = new CachedOutputStream();
            AttachmentSerializer as = new AttachmentSerializer(soapMessage, is, cos);
            String contentType = as.serializeMultipartMessage();
            soapMessage.getAttachments().clear();

            assertTrue(cos.getInputStream() != null);
            soapMessage.setContent(InputStream.class, cos.getInputStream());

            Map<String, List<String>> mimeHttpHeaders = new HashMap<String, List<String>>();
            soapMessage.put(MessageContext.HTTP_REQUEST_HEADERS, mimeHttpHeaders);
            mimeHttpHeaders.put("Content-Type", Arrays.asList(contentType));
            mimeHttpHeaders.put("Content-Description", Arrays.asList("XML document Multi-Media attachment"));

            soapMessage.getInterceptorChain().doIntercept(soapMessage);

            Attachment primarySoapPart = (Attachment)soapMessage.getContent(Attachment.class);
            assertEquals("type header determined by Soap Version",
                         soapMessage.getVersion().getSoapMimeType(), primarySoapPart.getHeader("type"));
            assertTrue(primarySoapPart.getDataHandler() != null);

            XMLStreamReader xsr = (XMLStreamReader)soapMessage.getContent(XMLStreamReader.class);
            if (xsr == null) {
                InputStream in = (InputStream)soapMessage.getContent(InputStream.class);
                assertTrue(in != null);
                XMLInputFactory f = XMLInputFactory.newInstance();
                xsr = f.createXMLStreamReader(in);
                soapMessage.setContent(XMLStreamReader.class, xsr);
            }
            assertTrue(xsr != null);

            if (testXmlConent) {
                boolean found = false;
                while (xsr.hasNext()) {
                    xsr.nextTag();
                    // System.out.println(xsr.getName());
                    if (xsr.getName().getLocalPart().equals("Detail")) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Data Root Tag not found in message soap part!", found);
            }
            AttachmentDeserializer ad = (AttachmentDeserializer)soapMessage
                .get(Message.ATTACHMENT_DESERIALIZER);
            ad.processAttachments();
            Collection<Attachment> attachments = soapMessage.getAttachments();
            assertTrue(attachments.size() == 2);
            Iterator<Attachment> it = attachments.iterator();
            Attachment att1 = it.next();
            assertTrue(att1.getId() != null);
            assertTrue(att1.getDataHandler() != null);

            Attachment att2 = it.next();
            assertTrue(att2.getId() != null);
            assertTrue(att2.getDataHandler() != null);

        } catch (XMLStreamException xse) {
            fail(xse.getStackTrace().toString());
        } catch (IOException ioe) {
            fail(ioe.getStackTrace().toString());
        } catch (MessagingException me) {
            fail(me.getStackTrace().toString());
        }
    }
}

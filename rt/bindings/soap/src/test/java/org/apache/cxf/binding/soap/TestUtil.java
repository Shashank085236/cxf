package org.apache.cxf.binding.soap;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.binding.attachment.AttachmentImpl;
import org.apache.cxf.binding.attachment.AttachmentUtil;
import org.apache.cxf.bindings.soap.attachments.types.DetailType;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;

public final class TestUtil {

    private TestUtil() {
    }

    public static DetailType createDetailObject(Class<?> clazz)
        throws IOException {
        
        DetailType detailObj = new DetailType();
        detailObj.setSName("hello world");        
                
        URL url1 = clazz.getResource("my.wav");
        URL url2 = clazz.getResource("me.jpg");
        Image image = ImageIO.read(new File(url2.getFile()));
        detailObj.setPhoto(image);
        File file = new File(url1.getFile());
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        fi.read(buffer);
        detailObj.setSound(buffer);
        
        return detailObj;        
    }
    
    public static SoapMessage createSoapMessage(SoapVersion soapVersion,
                                                InterceptorChain chain, Class<?> clazz)
        throws IOException {        
        
        SoapMessage soapMessage = createEmptySoapMessage(soapVersion, chain);        
        // setup the message result with attachment.class
        ByteArrayDataSource bads = new ByteArrayDataSource(clazz.getResourceAsStream("primarySoapPart.xml"),
                                                           "Application/xop+xml");
        String cid = AttachmentUtil.createContentID("http://cxf.apache.org");
        soapMessage.setContent(Attachment.class, new AttachmentImpl(cid, new DataHandler(bads)));

        // setup the message attachments
        Collection<Attachment> attachments = soapMessage.getAttachments();
        String cidAtt1 = "cid:http://cxf.apache.org/me.jpg";
        bads = new ByteArrayDataSource(clazz.getResourceAsStream("me.jpg"), "image/jpg");
        AttachmentImpl att1 = new AttachmentImpl(cidAtt1, new DataHandler(bads));
        att1.setXOP(true);
        attachments.add(att1);
        String cidAtt2 = "cid:http://cxf.apache.org/my.wav";
        bads = new ByteArrayDataSource(clazz.getResourceAsStream("my.wav"),
                                       "Application/octet-stream");
        AttachmentImpl att2 = new AttachmentImpl(cidAtt2, new DataHandler(bads));
        att2.setXOP(true);
        attachments.add(att2);

        return soapMessage;
    }
    
    public static SoapMessage createEmptySoapMessage(SoapVersion soapVersion, InterceptorChain chain) {
        Exchange exchange = new ExchangeImpl();
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.setInterceptorChain(chain);
        messageImpl.setExchange(exchange);
        SoapMessage soapMessage = new SoapMessage(messageImpl);
        soapMessage.setVersion(soapVersion);
        return soapMessage;        
    }
}

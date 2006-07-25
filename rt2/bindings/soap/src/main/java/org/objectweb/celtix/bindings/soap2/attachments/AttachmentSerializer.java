package org.objectweb.celtix.bindings.soap2.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.objectweb.celtix.bindings.attachments.AttachmentDataSource;
import org.objectweb.celtix.bindings.attachments.AttachmentUtil;
import org.objectweb.celtix.bindings.soap2.SoapMessage;
import org.objectweb.celtix.bindings.soap2.SoapVersion;
import org.objectweb.celtix.message.Attachment;

public final class AttachmentSerializer {

    private static final String[] FILTER = new String[] {"Message-ID", "Mime-Version", "Content-Type"};    

    private AttachmentSerializer() {
    }

    /**
     * Using result & attachment in soapMessage to write to output stream
     * 
     * @param soapMessage
     * @param out
     * @throws CxfRioException
     */

    public static String serializeMultipartMessage(SoapMessage soapMessage, OutputStream out)
        throws MessagingException, IOException {
        
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage mimeMessage = new MimeMessage(session);
        String soapPartId = AttachmentUtil.createContentID(null);
        String subType = getMimeSubType(soapMessage.getVersion(), soapPartId);
        MimeMultipart mimeMP = new MimeMultipart(subType);
               
        InputStream in = (InputStream) soapMessage.getResult(InputStream.class);
        AttachmentDataSource ads = new AttachmentDataSource("application/xop+xml", in);
        MimeBodyPart soapPart = new MimeBodyPart();
        soapPart.setDataHandler(new DataHandler(ads));
        soapPart.setContentID("<" + soapPartId  + ">");
        soapPart.addHeader("Content-Type", "application/xop+xml");        
        soapPart.addHeader("type", soapMessage.getVersion().getSoapMimeType());
        soapPart.addHeader("charset", SoapMessage.CHARSET);
        soapPart.addHeader("Content-Transfer-Encoding", "binary");
        mimeMP.addBodyPart(soapPart);
        
        for (Iterator itr = soapMessage.getAttachments().iterator(); itr.hasNext();) {
            Attachment att = (Attachment)itr.next();
            MimeBodyPart part = new MimeBodyPart();
            part.setDataHandler(att.getDataHandler());
            part.setContentID("<" + att.getId() + ">");
            if (att.isXOP()) {
                part.addHeader("Content-Transfer-Encoding", "binary");
            }
            mimeMP.addBodyPart(part);
        }
        mimeMessage.setContent(mimeMP);
        mimeMessage.writeTo(out, FILTER);
        return mimeMP.getContentType();
    }

    /**
     * create MimeMultipart to represent the soap message
     * 
     * @param message
     * @return
     */
    public static String getMimeSubType(SoapVersion soapVersion, String soapPartId) {
        StringBuffer ct = new StringBuffer();
        ct.append("related; ");
        ct.append("type=\"application/xop+xml\"; ");
        ct.append("start=\"<" + soapPartId + ">\"; ");
        ct.append("start-info=\"" + soapVersion.getSoapMimeType() + "\"");
        return ct.toString();
    }

}

package org.objectweb.celtix.bus.jaxws.io;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.objectweb.celtix.bindings.DataWriter;
import org.objectweb.celtix.bus.jaxws.DynamicDataBindingCallback;
import org.objectweb.celtix.context.ObjectMessageContext;

public class SOAPMessageDataWriter<T> implements DataWriter<T> {

    protected SOAPMessage dest;
    final DynamicDataBindingCallback callback;
    

    public SOAPMessageDataWriter(DynamicDataBindingCallback cb) {
        callback = cb;
    }

    public void write(Object obj, T output) {
        dest = (SOAPMessage) output;
        try {
            if (DOMSource.class.isAssignableFrom(obj.getClass())) {
                DOMSource src = (DOMSource) obj;
                dest.getSOAPPart().setContent(src);
            } else if (SAXSource.class.isAssignableFrom(obj.getClass())) {
                SAXSource src = (SAXSource) obj;
                dest.getSOAPPart().setContent(src);
            } else if (StreamSource.class.isAssignableFrom(obj.getClass())) {
                StreamSource src = (StreamSource) obj;
                dest.getSOAPPart().setContent(src);
            }
        } catch (SOAPException se) {
            //TODO
        }
    }

    public void write(Object obj, QName elName, T output) {
        //Complete
    }

    public void writeWrapper(ObjectMessageContext objCtx, boolean isOutbound, T output) {
        //Complete
    }

}

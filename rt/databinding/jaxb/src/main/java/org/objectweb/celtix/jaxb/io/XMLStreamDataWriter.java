package org.objectweb.celtix.jaxb.io;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.objectweb.celtix.bindings.DataWriter;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.jaxb.JAXBDataBindingCallback;
import org.objectweb.celtix.jaxb.JAXBEncoderDecoder;

public class XMLStreamDataWriter<T> implements DataWriter<T> {
    final JAXBDataBindingCallback callback;

    public XMLStreamDataWriter(JAXBDataBindingCallback cb) {
        callback = cb;
    }
    
    public void write(Object obj, T output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, QName elName, T output) {
        if (obj != null) {
            JAXBEncoderDecoder.marshall(callback.getJAXBContext(),
                callback.getSchema(), obj, elName, (XMLStreamWriter)output);
        }
    }
    
    public void writeWrapper(ObjectMessageContext objCtx, boolean isOutBound, T output) {
        Object obj = callback.createWrapperType(objCtx, isOutBound);
        QName elName = isOutBound ? callback.getResponseWrapperQName()
                         : callback.getRequestWrapperQName();
        write(obj, elName, output);
    }
}

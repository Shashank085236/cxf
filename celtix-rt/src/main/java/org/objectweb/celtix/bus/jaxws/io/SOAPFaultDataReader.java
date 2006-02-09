package org.objectweb.celtix.bus.jaxws.io;


import java.lang.reflect.Constructor;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.NodeList;


import org.objectweb.celtix.bindings.DataReader;
import org.objectweb.celtix.bus.jaxws.JAXBDataBindingCallback;
import org.objectweb.celtix.bus.jaxws.JAXBEncoderDecoder;
import org.objectweb.celtix.context.ObjectMessageContext;

public class SOAPFaultDataReader<T> implements DataReader<T> {
    final JAXBDataBindingCallback callback;
    
    public SOAPFaultDataReader(JAXBDataBindingCallback cb) {
        callback = cb;
    }
    public Object read(QName name, int idx, T input) {
        SOAPFault fault = (SOAPFault)input;
        if (fault.getDetail() != null) {
            NodeList list = fault.getDetail().getChildNodes();

            // Axis includes multiple childNodes.
            assert list.getLength() > 0;
            QName faultName;
            for (int i = 0; i < list.getLength(); i++) {
                if (list.item(i).getLocalName() == null) {
                    continue;
                }
                faultName = new QName(list.item(i).getNamespaceURI(),
                                      list.item(i).getLocalName());
        
                Class<?> clazz = callback.getWebFault(faultName);
                try {
                    if (clazz != null) {
                        Class<?> faultInfo = clazz.getMethod("getFaultInfo").getReturnType();
                        Object obj = JAXBEncoderDecoder.unmarshall(callback.getJAXBContext(), 
                                                                   list.item(i),
                                                                   faultName,
                                                                   faultInfo);
                        Constructor<?> ctor = clazz.getConstructor(String.class,
                                                                   obj.getClass());
                        return ctor.newInstance(fault.getFaultString(), obj);
                    } else {
                        return new SOAPFaultException(fault);
                    }
                } catch (Exception ex) {
                    throw new WebServiceException("error in unmarshal of SOAPFault", ex);
                }
            }
        }
        return null;
    }

    public Object read(int idx, T input) {
        return read(null, idx, input);
    }

    public void readWrapper(ObjectMessageContext objCtx, boolean isOutBound, T input) {
        throw new UnsupportedOperationException();
    }
}

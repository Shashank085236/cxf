package org.objectweb.celtix.bindings.soap2;

import java.util.HashSet;
import java.util.Set;

import javax.wsdl.extensions.soap.SOAPHeader;
import javax.xml.namespace.QName;

import org.objectweb.celtix.message.Message;
import org.objectweb.celtix.service.model.BindingInfo;
import org.objectweb.celtix.service.model.BindingMessageInfo;
import org.objectweb.celtix.service.model.BindingOperationInfo;

public final class ServiceModelUtil {
    private static final String HEADERS_PROPERTY = ServiceModelUtil.class.getName() + ".HEADERS";

    private ServiceModelUtil() {
    }

    public static BindingOperationInfo getOperation(SoapMessage soapMessage, QName opName) {
        BindingInfo service = (BindingInfo)soapMessage.get(Message.BINDING_INFO);
        return service.getOperation(opName);
    }

    @SuppressWarnings("unchecked")
    public static Set<QName> getHeaderParts(BindingMessageInfo bmi) {
        Object obj = bmi.getProperty(HEADERS_PROPERTY);
        if (obj == null) {
            Set<QName> set = new HashSet<QName>();
            for (SOAPHeader head : bmi.getExtensors(SOAPHeader.class)) {
                String pn = head.getPart();
                set.add(new QName(bmi.getBindingOperation().getBinding().getService().getTargetNamespace(),
                                  pn));
            }
            bmi.setProperty(HEADERS_PROPERTY, set);
            return set;
        }
        return (Set<QName>)obj;
    }

    public static Set<QName> getHeaderQNameInOperationParam(SoapMessage soapMessage) {
        Set<QName> headers = new HashSet<QName>();
        BindingInfo binding = (BindingInfo)soapMessage.get(Message.BINDING_INFO);
        if (binding != null) {
            for (BindingOperationInfo opi : binding.getOperations()) {
                headers.addAll(getHeaderParts(opi.getInput()));
                headers.addAll(getHeaderParts(opi.getOutput()));
            }
        }
        return headers;
    }
}

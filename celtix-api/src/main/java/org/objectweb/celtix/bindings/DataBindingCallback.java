package org.objectweb.celtix.bindings;


import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.objectweb.celtix.context.ObjectMessageContext;

/**
 * Callback used during IO for the bindings to figure out how to properly construct the messages. 
 */
public interface DataBindingCallback {
    
    public enum Mode { 
        MESSAGE(Service.Mode.MESSAGE),
        PAYLOAD(Service.Mode.PAYLOAD),
        PARTS(null);
    
        Service.Mode jaxwsMode;
        Mode(Service.Mode m) {
            jaxwsMode = m;
        }
        public static Mode fromServiceMode(Service.Mode m) {
            if (m == Service.Mode.PAYLOAD) {
                return PAYLOAD;
            }
            return MESSAGE;
        }
        public Service.Mode getServiceMode() {
            return jaxwsMode;
        }
    };    

    Mode getMode();
    
    Class<?>[] getSupportedFormats();
    
    <T> DataWriter<T> createWriter(Class<T> cls);
    <T> DataReader<T> createReader(Class<T> cls);
       
    
    SOAPBinding.Style getSOAPStyle();
    SOAPBinding.Use getSOAPUse();
    SOAPBinding.ParameterStyle getSOAPParameterStyle();
    boolean isOneWay();

    String getOperationName();
    String getTargetNamespace();
    String getSOAPAction();
    WebResult getWebResult();
    QName getWebResultQName();
    WebParam getWebParam(int index);
    int getParamsLength();    
    
    QName getRequestWrapperQName();
    QName getResponseWrapperQName();
    
    void initObjectContext(ObjectMessageContext octx);
    
}

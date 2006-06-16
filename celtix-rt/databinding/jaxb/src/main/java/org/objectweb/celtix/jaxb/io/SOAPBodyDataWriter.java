package org.objectweb.celtix.jaxb.io;



import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.objectweb.celtix.bindings.DataWriter;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.jaxb.DynamicDataBindingCallback;

public class SOAPBodyDataWriter<T> implements DataWriter<T> {

    protected SOAPBody dest;
    final DynamicDataBindingCallback callback;
    
    public SOAPBodyDataWriter(DynamicDataBindingCallback cb) {
        callback = cb;
    }

    public void write(Object obj, T output) {
        
        dest = (SOAPBody)output;
        try {
            if (DOMSource.class.isAssignableFrom(obj.getClass())) {
                DOMSource domSource = (DOMSource)obj;
                dest.addDocument((Document)domSource.getNode());
            } else if (SAXSource.class.isAssignableFrom(obj.getClass())) {
                SAXSource saxSource = (SAXSource)obj;
                Document doc = getDocBuilder().parse(saxSource.getInputSource());
                dest.addDocument(doc); 
            } else if (StreamSource.class.isAssignableFrom(obj.getClass())) {
                StreamSource streamSource = (StreamSource)obj;
                Document doc = getDocBuilder().parse(streamSource.getInputStream());
                dest.addDocument(doc); 
            } else if (Object.class.isAssignableFrom(obj.getClass())) {
                
                JAXBContext context = callback.getJAXBContext();
                
                Marshaller u = context.createMarshaller();
                u.setProperty(Marshaller.JAXB_ENCODING , "UTF-8");
                u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                u.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);  
                
                DOMResult domResult = new DOMResult();
                u.marshal(obj, domResult);
                dest.addDocument((Document)domResult.getNode());                
                
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public void write(Object obj, QName elName, T output) {
        //Complete
    }

    public void writeWrapper(ObjectMessageContext objCtx, boolean isOutbound, T output) {
        // Complete
    }
    
    private DocumentBuilder getDocBuilder() throws ParserConfigurationException {
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder();
        
    }

}

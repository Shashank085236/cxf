package org.objectweb.celtix.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.Oneway;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.objectweb.celtix.Bus;
//import org.objectweb.celtix.BusException;

import org.objectweb.celtix.addressing.EndpointReferenceType;
import org.objectweb.celtix.bindings.BindingFactory;
//import org.objectweb.celtix.bindings.BindingManager;
import org.objectweb.celtix.bindings.ClientBinding;
import org.objectweb.celtix.context.ObjectMessageContext;
import org.objectweb.celtix.wsdl.EndpointReferenceUtils;
import org.objectweb.celtix.wsdl.WSDLManager;


public class EndpointInvocationHandler implements BindingProvider, InvocationHandler
{
    protected ClientBinding clientBinding;
    protected Map<String, Object> requestContext;
    protected Map<String, Object> responseContext;
    
    private final Class<? extends Remote> portTypeInterface;
    private final EndpointReferenceType endpointRef;
    private final Bus bus;
    
    public EndpointInvocationHandler(Bus b, EndpointReferenceType reference,
            Class<? extends Remote> portSEI) {
        bus = b;
        portTypeInterface = portSEI;
        endpointRef = reference;
        clientBinding = createBinding(reference);
    }

    public Object invoke(Object proxy, Method method, Object args[])
        throws Throwable {
        
        if (portTypeInterface.equals(method.getDeclaringClass())) {
            return invokeSEIMethod(method, args);
        } else {
            return method.invoke(this, args);
        }
    }

    public Binding getBinding() {
        return (Binding) clientBinding;
    }
    
    public Map<String, Object> getRequestContext() {
        if (requestContext == null) {
            //REVISIT Need to Create a Request/ResponseContext classs to derive out of a
            //ContextBase class.
            requestContext = new HashMap<String, Object>();
        }
        return requestContext;
    }
    
    public Map<String, Object> getResponseContext() {
        if (responseContext == null) {
            responseContext = new HashMap<String, Object>();
        }
        return responseContext;
    }
    
    private Object invokeSEIMethod(Method method, Object parameters[])
        throws Throwable {

        //REVISIT Creation of MesgContext
        ObjectMessageContext objMsgContext = clientBinding.createObjectContext();
        
        boolean isOneway = (method.getAnnotation(Oneway.class) != null) ? true : false;

        if (isOneway) {
            clientBinding.invokeOneWay(objMsgContext);
        } else {
            objMsgContext = clientBinding.invoke(objMsgContext);
        }
        
        //Retrieve the return type obj from Context and send it out.
        return null;
    }
    
    protected ClientBinding createBinding(EndpointReferenceType ref) {

        WSDLManager wsdlManager = bus.getWSDLManager();
        Port endpoint = null;
        try {
            endpoint = EndpointReferenceUtils.getPort(wsdlManager, ref);
        } catch (Exception we) {
            //TODO 
        }
        
        javax.wsdl.Binding wsdlBinding = endpoint.getBinding();
        String bindingId = getExtessionElementURI(wsdlBinding.getExtensibilityElements());

        //BindingFactoryManager bindingFactoryMgr = bus.getBindingFactoryManager();
        BindingFactory factory = null;
/*
        try {
            factory = transportFactoryMgr.getBindingFactory(bindingId);
        } catch (BusException be) {
            throw new WebServiceException(be);
        }
*/
        //Binding API Broken , EPR needs to be created for Binding.
        ClientBinding bindingImpl = factory.createClientBinding(ref);

        //Set the handle Chain on Binding
        return bindingImpl;
    }
    
    private String getExtessionElementURI(List extensionElementList) {
        String id = null;
        
        if (extensionElementList.isEmpty()) {
            throw new WebServiceException("Could not get the extension element URI");
        }
        
        ExtensibilityElement extElement = (ExtensibilityElement) extensionElementList.get(0);
        id = extElement.getElementType().getNamespaceURI();
        
        return id;
    }
}

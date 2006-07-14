package org.objectweb.celtix.servicemodel;

public class BindingMessageInfo extends AbstractPropertiesHolder {

    MessageInfo msg;
    BindingOperationInfo op;
    
    BindingMessageInfo(MessageInfo m, BindingOperationInfo boi) {
        op = boi;
        msg = m;
    }
    
    public MessageInfo getMessageInfo() {
        return msg;
    }
    
    public BindingOperationInfo getBindingOperation() {
        return op;
    }
}

package org.objectweb.celtix.service.model;

public class BindingFaultInfo extends AbstractPropertiesHolder {
    FaultInfo fault;
    BindingOperationInfo opinfo;
    
    public BindingFaultInfo(FaultInfo f, BindingOperationInfo info) {
        fault = f;
        opinfo = info;
    }
    
    public FaultInfo getFaultInfo() {
        return fault;
    }
    
    public BindingOperationInfo getBindingOperation() {
        return opinfo;
    }

}

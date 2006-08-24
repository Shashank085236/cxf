package org.apache.cxf.jaxws.interceptors;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.interceptors.Fault;
import org.apache.cxf.jaxb.WrapperHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class WrapperClassOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String SINGLE_WRAPPED_PART = "single.wrapped.out.part";

    public WrapperClassOutInterceptor() {
        super();
        setPhase(Phase.POST_LOGICAL);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) throws Fault {
        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        if (bop.isUnwrapped()) {
            List<Object> objs = message.getContent(List.class);
            
            OperationInfo op = bop.getOperationInfo();
            Class<?> wrapped = (Class)op.getProperty(SINGLE_WRAPPED_PART);
            if (wrapped != null) {
                MessageInfo messageInfo = message.get(MessageInfo.class);
                try {
                    Object wrapperType = wrapped.newInstance();
                    
                    int i = 0;
                    for (MessagePartInfo p : messageInfo.getMessageParts()) {
                        Object part = objs.get(i);
    
                        WrapperHelper.setWrappedPart(p.getName().getLocalPart(), wrapperType, part);
    
                        i++;
                    }
                    objs = Arrays.asList((Object)wrapperType);
                    message.setContent(List.class, objs);
                } catch (Exception ex) {
                    //TODO -create a fault
                    ex.printStackTrace();
                }
                
                BindingOperationInfo newbop = bop.getWrappedOperation();
                message.getExchange().put(BindingOperationInfo.class, newbop);
                message.getExchange().put(OperationInfo.class, newbop.getOperationInfo());
                
                if (messageInfo == bop.getOperationInfo().getInput()) {
                    message.put(MessageInfo.class, newbop.getOperationInfo().getInput());
                    message.put(BindingMessageInfo.class, newbop.getInput());
                } else if (messageInfo == bop.getOperationInfo().getOutput()) {
                    message.put(MessageInfo.class, newbop.getOperationInfo().getOutput());
                    message.put(BindingMessageInfo.class, newbop.getOutput());
                //} else {
                    //TODO -faults
                }
                
            }
        }
    }
}

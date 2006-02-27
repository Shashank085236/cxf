package org.objectweb.celtix.systest.handlers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.handler.MessageContext;

/**
 * Describe class TestHandlerBase here.
 *
 *
 * Created: Fri Oct 21 14:02:50 2005
 *
 * @author <a href="mailto:codea@iona.com">codea</a>
 * @version 1.0
 */
public abstract class TestHandlerBase {

    private static final Logger LOG = Logger.getLogger(TestHandlerBase.class.getName());

    private static int sid; 

    protected boolean handleMessageRet = true; 
    Map<String, Integer> methodCallCount = new HashMap<String, Integer>();
    private final int id;
    private final boolean isServerSideHandler;

    public TestHandlerBase(boolean serverSide) {
        id = ++sid; 
        isServerSideHandler = serverSide;
    }

    protected void methodCalled(String methodName) { 
        int val = 0;
        if (methodCallCount.keySet().contains(methodName)) { 
            val = methodCallCount.get(methodName);
        } 
        val++; 
        methodCallCount.put(methodName, val);
    } 

    
    public int getId() {
        return id; 
    }
    
    public abstract String getHandlerId();

    public boolean isCloseInvoked() {

        return methodCallCount.containsKey("close");
    }

    public boolean isDestroyInvoked() {
        return methodCallCount.containsKey("destroy");
    }

    public boolean isHandleFaultInvoked() {
        return methodCallCount.containsKey("handleFault");
    }

    public int getHandleFaultInvoked() {
        return getMethodCallCount("handleFault");
    }

    public boolean isHandleMessageInvoked() {
        return methodCallCount.containsKey("handleMessage");
    }

    public int getHandleMessageInvoked() {
        return getMethodCallCount("handleMessage");
    }
    
    public boolean isInitInvoked() {
        return methodCallCount.containsKey("init");
    }
    
    public void setHandleMessageRet(boolean ret) { 
        handleMessageRet = ret; 
    }

    public boolean isServerSideHandler() {
        return isServerSideHandler; 
    } 

    protected void printHandlerInfo(String methodName, boolean outbound) { 
        String info = getHandlerId() + " "
            + (outbound ? "outbound" : "inbound") + " "
            + methodName;
        LOG.info(info);
    } 


    @SuppressWarnings("unchecked")
    protected List<String> getHandlerInfoList(MessageContext ctx) { 
        List<String> handlerInfoList = null; 
        if (ctx.containsKey("handler.info")) { 
            handlerInfoList = (List<String>)ctx.get("handler.info"); 
        } else {
            handlerInfoList = new ArrayList<String>();
            ctx.put("handler.info", handlerInfoList);
            ctx.setScope("handler.info", MessageContext.Scope.APPLICATION);
        }
        return handlerInfoList;
    }
    
    protected boolean isOutbound(MessageContext ctx) {
        return (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    }

    private int getMethodCallCount(String methodName) { 
        int ret = 0;
        if (methodCallCount.containsKey(methodName)) {
            ret = methodCallCount.get(methodName);             
        }
        return ret;
    } 
    
}

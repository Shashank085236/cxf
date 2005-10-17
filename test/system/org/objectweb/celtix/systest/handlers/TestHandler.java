package org.objectweb.celtix.systest.handlers;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.objectweb.handler_test.types.PingResponse;
import org.objectweb.handler_test.types.PingWithArgs;
import org.objectweb.hello_world_soap_http.types.GreetMe;


class TestHandler<T extends LogicalMessageContext> implements LogicalHandler<T> {

    private static int sid; 

    private final int id;

    private int handleMessageInvoked; 
    private int handleFaultInvoked; 
    private int closeInvoked; 
    private int initInvoked; 
    private int destroyInvoked; 
    private boolean handleMessageRet = true; 
    private boolean isServerSideHandler;

    private final JAXBContext jaxbCtx;

    public TestHandler() {
        this(false);
        
    } 

    public TestHandler(boolean serverSide) {
        id = ++sid; 
        isServerSideHandler = serverSide;
        try {
            jaxbCtx = JAXBContext.newInstance(GreetMe.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
 
    } 

    public int getId() {
        return id; 
    }
    
    public boolean handleMessage(T ctx) {
        handleMessageInvoked++;

        if (!isServerSideHandler) {
            return true;
        }

        QName operationName = (QName)ctx.get(MessageContext.WSDL_OPERATION);
        
        boolean outbound = (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        String handlerInfo = "handler" + id + " handleMessage " + (outbound ? "outbound" : "inbound");
        
        boolean ret = handleMessageRet; 

        if ("ping".equals(operationName.getLocalPart())) {
            ret = handlePingMessage(outbound, ctx);
        } else if ("pingWithArgs".equals(operationName.getLocalPart())) {
            ret = handlePingWithArgsMessage(outbound, ctx); 
        }

        return ret;
    }


    private boolean handlePingWithArgsMessage(boolean outbound, T ctx) { 

        LogicalMessage msg = ctx.getMessage();
        addHandlerId(msg, ctx, outbound);
        Object payload = msg.getPayload(jaxbCtx); 
        String handlerId = "handler" + id; 

        boolean ret = true;
        if (payload instanceof PingWithArgs) {
            String arg = ((PingWithArgs)payload).getHandlersCommand();
            StringTokenizer strtok = new StringTokenizer(arg, " ");
            String hid = strtok.nextToken();
            String direction = strtok.nextToken();
            String command = strtok.nextToken();

            if (!outbound) {
                if ("inbound".equals(direction) && handlerId.equals(hid)) {
                    if ("stop".equals(command)) {

                        PingResponse resp = new PingResponse();
                        resp.getHandlersInfo().addAll(getHandlerInfoList(ctx));
                        msg.setPayload(resp, jaxbCtx);
                        ret = false;
                    }
                }
            }
        } 
        return ret;
    } 


    private boolean handlePingMessage(boolean outbound, T ctx) { 

        LogicalMessage msg = ctx.getMessage();
        addHandlerId(msg, ctx, outbound);
        return handleMessageRet;
    } 

    private void addHandlerId(LogicalMessage msg, T ctx, boolean outbound) { 

        Object obj = msg.getPayload(jaxbCtx);
        if (outbound && isServerSideHandler && obj != null) {
            
            PingResponse origResp = (PingResponse)obj;
            PingResponse newResp = new PingResponse();
            newResp.getHandlersInfo().addAll(origResp.getHandlersInfo());
            newResp.getHandlersInfo().add("handler" + id);
            msg.setPayload(newResp, jaxbCtx);
        } else if (!outbound && obj == null) {
            getHandlerInfoList(ctx).add("handler" + id);
        }
    } 


    public boolean handleFault(T arg0) {
        handleFaultInvoked++;
        return true;
    }

    public void close(MessageContext arg0) {
        closeInvoked++;
    }

    public void init(Map arg0) {
        initInvoked++;
    }

    public void destroy() {
        destroyInvoked++;
    }

    public boolean isCloseInvoked() {
        return closeInvoked != 0;
    }

    public boolean isDestroyInvoked() {
        return destroyInvoked != 0;
    }

    public boolean isHandleFaultInvoked() {
        return handleFaultInvoked != 0;
    }

    public boolean isHandleMessageInvoked() {
        return handleMessageInvoked != 0;
    }

    public int getHandleMessageInvoked() {
        return handleMessageInvoked;
    }
    
    public boolean isInitInvoked() {
        return initInvoked != 0;
    }
    
    public void setHandleMessageRet(boolean ret) { 
        handleMessageRet = ret; 
    }

    private List<String> getHandlerInfoList(LogicalMessageContext ctx) { 
        List<String> handlerInfoList = null; 
        if (ctx.containsKey("handler.info")) { 
            handlerInfoList = (List)ctx.get("handler.info"); 
        } else {
            handlerInfoList = new ArrayList<String>();
            ctx.put("handler.info", handlerInfoList);
            ctx.setScope("handler.info", MessageContext.Scope.APPLICATION);
        }
        return handlerInfoList;
    }
}    

package org.objectweb.celtix.bus.bindings;

import java.util.List;

import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.security.SecurityConfiguration;

public abstract class BindingImpl implements Binding {

    /* (non-Javadoc)
     * @see javax.xml.ws.Binding#getHandlerChain()
     */
    public List<Handler> getHandlerChain() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.xml.ws.Binding#getSecurityConfiguration()
     */
    public SecurityConfiguration getSecurityConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.xml.ws.Binding#setHandlerChain(java.util.List)
     */
    public void setHandlerChain(List<Handler> arg0) {
        // TODO Auto-generated method stub
        
    }

}

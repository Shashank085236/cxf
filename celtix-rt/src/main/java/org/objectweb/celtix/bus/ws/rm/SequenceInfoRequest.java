package org.objectweb.celtix.bus.ws.rm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.objectweb.celtix.bindings.AbstractBindingBase;
import org.objectweb.celtix.bindings.Request;
import org.objectweb.celtix.bus.ws.addressing.AddressingPropertiesImpl;
import org.objectweb.celtix.bus.ws.addressing.ContextUtils;
import org.objectweb.celtix.ws.addressing.AddressingProperties;
import org.objectweb.celtix.ws.addressing.AttributedURIType;
import org.objectweb.celtix.ws.rm.AckRequestedType;

public class SequenceInfoRequest extends Request {
    
    public SequenceInfoRequest(AbstractBindingBase b) {
        
        super(b, b.createObjectContext());
        getObjectMessageContext().setRequestorRole(true);
        AddressingProperties maps = new AddressingPropertiesImpl();
        AttributedURIType actionURI =
            ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
        actionURI.setValue(RMUtils.getRMConstants().getSequenceInfoAction());
        maps.setAction(actionURI);
        ContextUtils.storeMAPs(maps, getObjectMessageContext(), true, true, true, true);
        
        // NOTE: Not storing a method in the context causes BindingContextUtils.isOnewayMethod
        // to always return false (although effectively all standalone requests based on the
        // the SequenceInfo request are oneway requests). 
        // An important implication of this is that we don't expect partial
        // responses sent in response to such messages, which is fine as we normally only piggyback
        // sequence acknowledgements onto application messages.
    }
    
    public void requestAcknowledgement(Collection<Sequence> seqs) {
        List<AckRequestedType> requested = new ArrayList<AckRequestedType>();
        for (Sequence seq : seqs) {
            AckRequestedType ar = RMUtils.getWSRMFactory().createAckRequestedType();
            ar.setIdentifier(seq.getIdentifier());
            requested.add(ar);
        }
        RMPropertiesImpl rmps = new RMPropertiesImpl();        
        rmps.setAcksRequested(requested);
    }
    
    public void acknowledge(Sequence seq) {
        AddressingProperties maps = ContextUtils.retrieveMAPs(getObjectMessageContext(), true, true);
        maps.getAction().setValue(RMUtils.getRMConstants().getSequenceAcknowledgmentAction());
        AttributedURIType toAddress = ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
        toAddress.setValue(seq.getAcksTo().getAddress().getValue());
        maps.setTo(toAddress);
        // rm properties will be created (and actual acknowledgments added)
        // by rm handler upon outbound processing of this message
    }
    
    public void lastMessage(Sequence seq) {
        AddressingProperties maps = ContextUtils.retrieveMAPs(getObjectMessageContext(), true, true);
        maps.getAction().setValue(RMUtils.getRMConstants().getLastMessageAction());
        RMPropertiesImpl rmps = new RMPropertiesImpl(); 
        seq.nextAndLastMessageNumber();
        rmps.setSequence(seq);
        assert null != seq.getLastMessageNumber();
        RMContextUtils.storeRMProperties(getObjectMessageContext(), rmps, true);
    }
}

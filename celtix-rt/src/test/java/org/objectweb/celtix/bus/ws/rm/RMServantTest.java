package org.objectweb.celtix.bus.ws.rm;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import junit.framework.TestCase;


import org.easymock.classextension.IMocksControl;
import org.objectweb.celtix.bus.configuration.wsrm.DestinationPolicyType;
import org.objectweb.celtix.ws.addressing.v200408.AttributedURI;
import org.objectweb.celtix.ws.addressing.v200408.EndpointReferenceType;
import org.objectweb.celtix.ws.rm.CreateSequenceResponseType;
import org.objectweb.celtix.ws.rm.CreateSequenceType;
import org.objectweb.celtix.ws.rm.Expires;
import org.objectweb.celtix.ws.rm.Identifier;
import org.objectweb.celtix.ws.rm.OfferType;
import org.objectweb.celtix.ws.rm.wsdl.SequenceFault;

import static org.easymock.classextension.EasyMock.*;

public class RMServantTest extends TestCase {
    
    private IMocksControl control = createNiceControl();
    private RMDestination dest;
    private CreateSequenceType cs;
    private AttributedURI to;
    private DestinationPolicyType dp;
    private Identifier sid;

    public void testCreateSequenceDefault() throws DatatypeConfigurationException, SequenceFault {        
        
        setupCreateSequence(null, null, true, true);
        
        control.replay();
        
        CreateSequenceResponseType csr = (new RMServant()).createSequence(dest, cs, to);
        
        control.verify();

        assertSame(sid, csr.getIdentifier());
        assertNotNull(csr.getAccept());  
        assertTrue(!Names.WSA_NONE_ADDRESS
                   .equals(csr.getAccept().getAcksTo().getAddress().getValue()));        
    }
    
    public void testCreateSequenceRejectOffer() throws DatatypeConfigurationException, SequenceFault {        
        
        setupCreateSequence(null, null, true, false);
        
        control.replay();
        
        CreateSequenceResponseType csr = (new RMServant()).createSequence(dest, cs, to);
        
        control.verify();

        assertSame(sid, csr.getIdentifier());
        assertNotNull(csr.getAccept());  
        assertEquals(Names.WSA_NONE_ADDRESS, csr.getAccept().getAcksTo().getAddress().getValue());        
    }
    
    public void testCreateSequenceNoOfferIncluded() 
        throws DatatypeConfigurationException, SequenceFault {        
        
        setupCreateSequence(null, null, false, false);
        
        control.replay();
        
        CreateSequenceResponseType csr = (new RMServant()).createSequence(dest, cs, to);
        
        control.verify();

        assertSame(sid, csr.getIdentifier());
        assertNull(csr.getAccept());   
    }
    
    public void testCreateSequenceRequestedDurationNotSupported() 
        throws DatatypeConfigurationException, SequenceFault {        
        
        setupCreateSequence("PT24H", "PT48H", false, false);
        
        control.replay();
        
        CreateSequenceResponseType csr = (new RMServant()).createSequence(dest, cs, to);
        
        control.verify();

        assertSame(sid, csr.getIdentifier());
        assertNull(csr.getAccept());          
        assertEquals("PT24H", csr.getExpires().getValue().toString());
    }
    
    public void testTerminateSequence() throws SequenceFault {
        dest = control.createMock(RMDestination.class);
        sid = control.createMock(Identifier.class);
        
        (new RMServant()).terminateSequence(dest, sid);
        
    }
    
    // expires = "PT24H";
    
    private void setupCreateSequence(String supportedDuration, String requestedDuration, 
                                         boolean includeOffer, boolean acceptOffer)
        throws DatatypeConfigurationException {

        dest = control.createMock(RMDestination.class);
        to = control.createMock(AttributedURI.class); 
        dp = control.createMock(DestinationPolicyType.class);
        sid = control.createMock(Identifier.class);
        cs = control.createMock(CreateSequenceType.class);
        
        dest.generateSequenceIdentifier();
        expectLastCall().andReturn(sid);
        
        dest.getDestinationPolicies();
        expectLastCall().andReturn(dp);
        
        Duration d = null;
        if (null != supportedDuration) {
            d = DatatypeFactory.newInstance().newDuration(supportedDuration);
        }
        dp.getSequenceExpiration();
        expectLastCall().andReturn(d);
            
        Expires ex = null;
        if (null != requestedDuration) {
            Duration rd = DatatypeFactory.newInstance().newDuration(requestedDuration);
            ex = RMUtils.getWSRMFactory().createExpires();
            ex.setValue(rd);
        }
        cs.getExpires();
        expectLastCall().andReturn(ex);        
        
        OfferType o = null;
        if (includeOffer) {
            o = control.createMock(OfferType.class);
        }
        cs.getOffer();
        expectLastCall().andReturn(o);
        
        if (includeOffer) {
            dp.isAcceptOffers();
            expectLastCall().andReturn(acceptOffer);
        }
        
        if (includeOffer && acceptOffer) {
            RMHandler handler = control.createMock(RMHandler.class);
            dest.getHandler();
            expectLastCall().andReturn(handler);
            RMSource source = control.createMock(RMSource.class);
            handler.getSource();
            expectLastCall().andReturn(source);
            o.getIdentifier();            
            expectLastCall().andReturn(control.createMock(Identifier.class));
            o.getExpires();
            expectLastCall().andReturn(null);
            source.addSequence(isA(SourceSequence.class));
            expectLastCall();
            source.setCurrent(isA(Identifier.class), isA(SourceSequence.class));
            expectLastCall();
        }
     
        cs.getAcksTo();
        expectLastCall().andReturn(control.createMock(EndpointReferenceType.class));
 
    }
    
}

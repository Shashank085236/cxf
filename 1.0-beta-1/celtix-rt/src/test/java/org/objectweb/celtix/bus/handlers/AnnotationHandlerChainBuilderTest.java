package org.objectweb.celtix.bus.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.objectweb.hello_world_soap_http.AnnotatedGreeterImpl;
import org.objectweb.hello_world_soap_http.BadRecordLitFault;
import org.objectweb.hello_world_soap_http.Greeter;
import org.objectweb.hello_world_soap_http.NoSuchCodeLitFault;
import org.objectweb.hello_world_soap_http.types.BareDocumentResponse;
import org.objectweb.hello_world_soap_http.types.GreetMeResponse;
import org.objectweb.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.objectweb.hello_world_soap_http.types.SayHiResponse;
import org.objectweb.hello_world_soap_http.types.TestDocLitFaultResponse;

public class AnnotationHandlerChainBuilderTest extends TestCase {

    Handler[] allHandlers = {EasyMock.createMock(LogicalHandler.class),
                             EasyMock.createMock(Handler.class),
                             EasyMock.createMock(Handler.class),
                             EasyMock.createMock(LogicalHandler.class)};
    Handler[] logicalHandlers = {allHandlers[0], allHandlers[3]}; 
    Handler[] protocolHandlers = {allHandlers[1], allHandlers[2]}; 
    AnnotatedGreeterImpl greeterImpl = new AnnotatedGreeterImpl(); 

    AnnotationHandlerChainBuilder builder = new AnnotationHandlerChainBuilder();


    public void testBuildHandlerChainWithExistingHandlers() { 

        List<Handler> existingChain = new ArrayList<Handler>(); 
        existingChain.add(EasyMock.createMock(LogicalHandler.class));
        existingChain.add(EasyMock.createMock(Handler.class));

        List<Handler> chain = builder.buildHandlerChainFor(AnnotatedGreeterImpl.class, existingChain); 
        
        assertNotNull(chain); 
        assertEquals(3, chain.size()); 
        assertTrue(chain.get(0) instanceof LogicalHandler);
        assertFalse(chain.get(1) instanceof LogicalHandler);
        assertFalse(chain.get(2) instanceof LogicalHandler);
    } 


    public void testBuildHandlerChainWithExistingHandlersNoAnnotation() { 

        List<Handler> existingChain = new ArrayList<Handler>(); 
        existingChain.add(EasyMock.createMock(LogicalHandler.class));
        existingChain.add(EasyMock.createMock(Handler.class));

        List<Handler> chain = builder.buildHandlerChainFor(Greeter.class, existingChain); 
        
        assertNotNull(chain); 
        assertEquals(existingChain.size(), chain.size()); 
        assertSame(existingChain.get(0), chain.get(0));
        assertSame(existingChain.get(1), chain.get(1));

    } 


    public void testBuildHandlerChain() { 

        List<Handler> chain = builder.buildHandlerChainFor(AnnotatedGreeterImpl.class); 
        
        assertNotNull(chain); 
        assertEquals(1, chain.size()); 
    } 

    public void testBuildHandlerForServiceEndpointInterface() { 

        List<Handler> chain = builder.buildHandlerChainFor(GreeterWithHandlerChainOnInterfaceImpl.class); 

        assertNotNull(chain); 
        assertEquals(1, chain.size()); 
    } 


    public void testBuildHandlerChainNoHanlders() { 
        List<Handler> chain = builder.buildHandlerChainFor(String.class); 
        assertNotNull(chain); 
        assertEquals(0, chain.size()); 
    } 

    public void testBuildHandlerChainInvalidFile() { 

        try { 
            builder.buildHandlerChainFor(InvalidAnnotation.class); 
            fail("did not get expected exception"); 
        } catch (WebServiceException ex) { 
            // happy
        } 
    } 

    public void testBuildHandlerChainInvalidHandlerList() {

        try { 
            builder.buildHandlerChainFor(InvalidName.class); 
            fail("did not get expected exception"); 
        } catch (WebServiceException ex) { 
            // happy
        } 
    }
    
}


@HandlerChain(file = "invalid_annotation.xml", name = "yoohoo")
class InvalidAnnotation extends AnnotatedGreeterImpl {
}


@HandlerChain(file = "handlers.xml", name = "yoohoo")
class InvalidName extends AnnotatedGreeterImpl {
}

@HandlerChain(file = "./broken_handlers.xml", name = "BrokenHandlerChain")
class NoSuchClassName extends AnnotatedGreeterImpl {
}

@HandlerChain(file = "../../../hello_world_soap_http/handlers.xml", name = "HandlerChainNoInitParam")
class HandlerChainNoInit extends AnnotatedGreeterImpl {
}


@WebService(name = "Greeter")
@HandlerChain(file = "../../../hello_world_soap_http/handlers.xml", name = "TestHandlerChain")
interface GreeterWithHandlerChain extends Greeter { 

} 

class GreeterWithHandlerChainOnInterfaceImpl implements GreeterWithHandlerChain {

    public String sayHi() { 
        return "";
    } 

    public String greetMe(String requestType) { 
        return requestType;
    }
    
    public String greetMeSometime(String requestType) { 
        return requestType;
    }
    
    public Future<?> greetMeSometimeAsync(String requestType, AsyncHandler ah) { 
        return null; 
        /*not called */
    }
    
    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) { 
        return null; 
        /*not called */
    }
    
    public Future<?>  greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) { 
        return null; 
        /*not called */
    }
    
    public Response<GreetMeResponse> greetMeAsync(String requestType) { 
        return null; 
        /*not called */
    }
    
    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) { 
        return null; 
        /*not called */
    }
    
    public Response<SayHiResponse> sayHiAsync() { 
        return null; 
        /*not called */
    }

    public void greetMeOneWay(String requestType) {
    }

    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {
        return null; 
        /*not called */
    }
    
    public Future<?> testDocLitFaultAsync(String faultType, AsyncHandler ah) { 
        return null; 
        /*not called */
    }
    
    public Future<?> testDocLitBareAsync(String bare, AsyncHandler ah) {
        return null;
        /* not called*/
    }
    
    public Response<BareDocumentResponse>testDocLitBareAsync(String bare) {
        return null;
        /* not called */
    }
    
    public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
    }
    
    public BareDocumentResponse testDocLitBare(String in) {
        BareDocumentResponse res = new BareDocumentResponse();
        res.setCompany("Celtix");
        res.setId(1);
        return res;
    }
}

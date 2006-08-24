package org.apache.cxf.common.annotation;



import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import junit.framework.TestCase;
import org.easymock.EasyMock;


public class AnnotationProcessorTest extends TestCase {

    AnnotatedGreeterImpl greeterImpl = new AnnotatedGreeterImpl(); 
    AnnotationProcessor processor = new AnnotationProcessor(greeterImpl); 
    List<Class<? extends Annotation>> expectedAnnotations = new ArrayList<Class<? extends Annotation>>(); 

    AnnotationVisitor visitor = EasyMock.createMock(AnnotationVisitor.class);
    
    public void setUp() { 
        EasyMock.checkOrder(visitor, false); 
    } 

    public void testVisitClass() { 

        expectedAnnotations.add(WebService.class);

        prepareCommonExpectations(visitor);
        visitor.visitClass((Class<?>)EasyMock.eq(AnnotatedGreeterImpl.class), 
                           (Annotation)EasyMock.isA(WebService.class));

        runProcessor(visitor);
    } 

    public void testVisitField() throws Exception { 

        Field expectedField = AnnotatedGreeterImpl.class.getDeclaredField("foo"); 

        expectedAnnotations.add(Resource.class);
        prepareCommonExpectations(visitor);
        visitor.visitField(EasyMock.eq(expectedField), 
                           (Annotation)EasyMock.isA(Resource.class));
        visitor.visitMethod((Method)EasyMock.anyObject(), (Annotation)EasyMock.anyObject());

        runProcessor(visitor);
        
    } 

    public void testVisitMethod() throws Exception {

        Field expectedField = AnnotatedGreeterImpl.class.getDeclaredField("foo"); 
        Method expectedMethod1 = AnnotatedGreeterImpl.class.getDeclaredMethod("sayHi"); 
        Method expectedMethod2 = AnnotatedGreeterImpl.class.getDeclaredMethod("sayHi", String.class); 
        Method expectedMethod3 = AnnotatedGreeterImpl.class.getDeclaredMethod("greetMe", String.class); 
        Method expectedMethod4 = 
            AnnotatedGreeterImpl.class.getDeclaredMethod("setContext", WebServiceContext.class); 

        expectedAnnotations.add(WebMethod.class);
        expectedAnnotations.add(Resource.class); 

        prepareCommonExpectations(visitor);
        visitor.visitField(EasyMock.eq(expectedField), 
                           (Annotation)EasyMock.isA(Resource.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod1), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod2), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod3), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod4), 
                           (Annotation)EasyMock.isA(Resource.class));
        runProcessor(visitor);
    }

    public void testVisitMemberOverrideIgnoresClass() { 
    } 

    public void testVisitSuperClassAnnotations() {
    }

    public void testVisitDerivedClassMemberNoAnnotation() {
    }

    public void testProcessorInvalidConstructorArgs() { 
        
        try {
            new AnnotationProcessor(null); 
            fail("did not get expected argument");
        } catch (IllegalArgumentException e) {
            // happy
        }

    }

    public void testProcessorInvalidAcceptArg() { 

        try {
            processor.accept(null);
            fail("did not get expected exception");
        } catch (IllegalArgumentException e) {
            // happy
        }

    } 


    private void prepareCommonExpectations(AnnotationVisitor v) {
        v.getTargetAnnotations();
        EasyMock.expectLastCall().andReturn(expectedAnnotations);
        v.setTarget(greeterImpl);
    }

    private void runProcessor(AnnotationVisitor v) { 
        EasyMock.replay(v); 
        processor.accept(v);
        EasyMock.verify(v); 
    } 
}


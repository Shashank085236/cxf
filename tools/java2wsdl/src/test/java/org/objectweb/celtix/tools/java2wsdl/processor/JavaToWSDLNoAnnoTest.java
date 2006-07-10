package org.objectweb.celtix.tools.java2wsdl.processor;

import java.io.File;

import org.objectweb.celtix.tools.common.ToolConstants;


public class JavaToWSDLNoAnnoTest extends ProcessorTestBase {

    private JavaToWSDLProcessor j2wProcessor;

    public void setUp() throws Exception {

        super.setUp();
        j2wProcessor = new JavaToWSDLProcessor();
        System.setProperty("java.class.path", getClassPath() + getLocation("./classes") + "/" 
                                              + File.separatorChar);
    }

    public void tearDown() {
        super.tearDown();
        j2wProcessor = null;
    }
    

    public void testGeneratedWithElementryClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE,
                output.getPath() + "/doc_bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "com.iona.test.Stock");
       
        j2wProcessor.setEnvironment(env);
        j2wProcessor.process();

    }
    
    public void testGeneratedWithDocWrappedClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE,
                output.getPath() + "/doc_wrapped.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "com.iona.docwrapped.StockPortType");
       
        j2wProcessor.setEnvironment(env);
        j2wProcessor.process();

    }
    
    
    public void testGeneratedWithRPCClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE,
                output.getPath() + "/rpc.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.objectweb.test.Stock");
       
        j2wProcessor.setEnvironment(env);
        j2wProcessor.process();
    }
    

    private String getLocation(String file) {
        return JavaToWSDLNoAnnoTest.class.getClassLoader().getResource(file).getFile();
    }

}

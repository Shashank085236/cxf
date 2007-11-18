/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.javascript;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.test.TestUtilities;
import org.junit.Assert;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.debugger.Main;

/**
 * Test utilities class with some Javascript capability included. 
 */
public class JavascriptTestUtilities extends TestUtilities {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JavascriptTestUtilities.class);

    private ScriptableObject rhinoScope;
    private Context rhinoContext;
    
    public static class JavaScriptAssertionFailed extends RuntimeException {

        public JavaScriptAssertionFailed(String what) {
            super(what);
        }
    }
    
    public static class JsAssert extends ScriptableObject {

        public JsAssert() { }
        public void jsConstructor(String exp) {
            LOG.severe("Assertion failed: " + exp);
            throw new JavaScriptAssertionFailed(exp);
        }
        @Override
        public String getClassName() {
            return "Assert";
        }
    }
    
    public static class Trace extends ScriptableObject {

        public Trace() {
        }

        @Override
        public String getClassName() {
            return "org_apache_cxf_trace";
        }
        
        //CHECKSTYLE:OFF
        public static void jsStaticFunction_trace(String message) {
            LOG.fine(message);
        }
        //CHECKSTYLE:ON
    }
    
    public static class Notifier extends ScriptableObject {
        
        private boolean notified;
        
        public Notifier() {
        }

        @Override
        public String getClassName() {
            return "org_apache_cxf_notifier";
        }
        
        public synchronized boolean waitForJavascript(long timeout) {
            while (!notified) {
                try {
                    wait(timeout);
                    return notified; 
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            return true; // only here if true on entry.
        }
        
        //CHECKSTYLE:OFF
        public synchronized void jsFunction_notify() {
            notified = true;
            notifyAll();
        }
        //CHECKSTYLE:ON
    }

    public JavascriptTestUtilities(Class<?> classpathReference) {
        super(classpathReference);
    }
    
    public void initializeRhino() {
        
        if (System.getProperty("cxf.jsdebug") != null) {
            Main.mainEmbedded("Debug embedded JavaScript.");
        }

        rhinoContext = Context.enter();
        rhinoScope = rhinoContext.initStandardObjects();
        try {
            ScriptableObject.defineClass(rhinoScope, JsAssert.class);
            ScriptableObject.defineClass(rhinoScope, Trace.class);
            ScriptableObject.defineClass(rhinoScope, Notifier.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        JsSimpleDomNode.register(rhinoScope);
        JsSimpleDomParser.register(rhinoScope);
    }
    
    public void readResourceIntoRhino(String resourceClasspath) throws IOException {
        Reader js = getResourceAsReader(resourceClasspath);
        rhinoContext.evaluateReader(rhinoScope, js, resourceClasspath, 1, null);
    }
    
    public void readStringIntoRhino(String js, String sourceName) {
        LOG.fine(sourceName + ":\n" + js);
        rhinoContext.evaluateString(rhinoScope, js, sourceName, 1, null);
    }
    
    public ScriptableObject getRhinoScope() {
        return rhinoScope;
    }

    public Context getRhinoContext() {
        return rhinoContext;
    }
    
    public Object javaToJS(Object value) {
        return Context.javaToJS(value, rhinoScope);
    }
    
    public Object rhinoNewObject(String constructorName) {
        return rhinoContext.newObject(rhinoScope, constructorName);
    }
    
    /**
     * Evaluate a javascript expression, returning the raw Rhino object.
     * @param jsExpression the javascript expression.
     * @return return value.
     */
    public Object rhinoEvaluate(String jsExpression) {
        return rhinoContext.evaluateString(rhinoScope, jsExpression, "<testcase>", 1, null);
    }
    
    /**
     * Evaluate a Javascript expression, converting the return value to a convenient Java type.
     * @param <T> The desired type
     * @param jsExpression the javascript expression.
     * @param clazz the Class object for the desired type.
     * @return the result.
     */
    public <T> T rhinoEvaluateConvert(String jsExpression, Class<T> clazz) {
        return clazz.cast(Context.jsToJava(rhinoEvaluate(jsExpression), clazz));
    }
    
    /**
     * Call a JavaScript function. Optionally, require it to throw an exception equal to
     * a supplied object. If the exception is called for, this function will either return null
     * or Assert.
     * @param expectingException Exception desired, or null.
     * @param functionName Function to call.
     * @param args args for the function. Be sure to Javascript-ify them as appropriate.
     * @return
     */
    public Object rhinoCallExpectingException(Object expectingException, 
                                              String functionName, 
                                              Object ... args) {
        Object fObj = rhinoScope.get(functionName, rhinoScope);
        if (!(fObj instanceof Function)) {
            throw new RuntimeException("Missing test function " + functionName);
        }
        Function function = (Function)fObj;
        try {
            return function.call(rhinoContext, rhinoScope, rhinoScope, args);
        } catch (RhinoException angryRhino) {
            if (expectingException != null && angryRhino instanceof JavaScriptException) {
                JavaScriptException jse = (JavaScriptException)angryRhino;
                Assert.assertEquals(jse.getValue(), expectingException);
                return null;
            }
            String trace = angryRhino.getScriptStackTrace();
            Assert.fail("JavaScript error: " + angryRhino.toString() + " " + trace);
        } catch (JavaScriptAssertionFailed assertion) {
            Assert.fail(assertion.getMessage());
        }
        return null;
    }
    
    public Object rhinoCall(String functionName, Object ... args) {
        return rhinoCallExpectingException(null, functionName, args);
    }
    
    public <T> T rhinoCallConvert(String functionName, Class<T> clazz, Object ... args) {
        return clazz.cast(Context.jsToJava(rhinoCall(functionName, args), clazz));
    }
}

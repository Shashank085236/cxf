package org.apache.cxf.jaxws.handlers;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class InitParamResourceResolverTest extends TestCase {

    public static final String STRING_PARAM = "stringParam";
    public static final String STRING_VALUE = "a string";
    public static final String INT_PARAM = "intParam";
    public static final String INT_VALUE = "42";
    public static final String FLOAT_PARAM = "floatParam";
    public static final String FLOAT_VALUE = Float.toString(Float.MAX_VALUE);
    public static final String CHAR_PARAM = "charParam";
    public static final String CHAR_VALUE = "a";
    public static final String BYTE_PARAM = "byteParam";
    public static final String BYTE_VALUE = Byte.toString(Byte.MAX_VALUE);
    public static final String SHORT_PARAM = "shortParam";
    public static final String SHORT_VALUE = "12";
    public static final String LONG_PARAM = "longParam";
    public static final String LONG_VALUE = Long.toString(Long.MAX_VALUE);
    public static final String DOUBLE_PARAM = "doubleParam";
    public static final String DOUBLE_VALUE = Double.toString(Double.MAX_VALUE);
    public static final String BOOLEAN_PARAM = "booleanParam"; 
    public static final String BOOLEAN_VALUE = "true";
        
    private Map<String, String> params = new HashMap<String, String>();
    
    private InitParamResourceResolver resolver; 
    
    public void setUp() {
        params.put(STRING_PARAM, STRING_VALUE);
        params.put(INT_PARAM, INT_VALUE);
        params.put(FLOAT_PARAM, FLOAT_VALUE);
        params.put(CHAR_PARAM, CHAR_VALUE);
        params.put(BYTE_PARAM, BYTE_VALUE);
        params.put(SHORT_PARAM, SHORT_VALUE);
        params.put(LONG_PARAM, LONG_VALUE);
        params.put(DOUBLE_PARAM, DOUBLE_VALUE);
        params.put(BOOLEAN_PARAM, BOOLEAN_VALUE);
        resolver = new InitParamResourceResolver(params);
    }
    
    /*
     char, byte, short, int, long, float, double, boolean
     */
    
    public void testResolveChar() {
        doResolveTypeTest(CHAR_PARAM, Character.class, CHAR_VALUE.charAt(0));
    }
    
    public void testResolveByte() {
        doResolveTypeTest(BYTE_PARAM, Byte.class, Byte.valueOf(BYTE_VALUE));
    }
    
    public void testResolveShort() {
        doResolveTypeTest(SHORT_PARAM, Short.class, Short.valueOf(SHORT_VALUE));
    }
    
    
    public void testResolveLong() {
        doResolveTypeTest(LONG_PARAM, Long.class, Long.valueOf(LONG_VALUE));
    }
    
    public void testResolveFloat() {
        doResolveTypeTest(FLOAT_PARAM, Float.class, Float.valueOf(FLOAT_VALUE));
    }
    
    public void testResolveDouble() {
        doResolveTypeTest(DOUBLE_PARAM, Double.class, Double.valueOf(DOUBLE_VALUE));
    }
    
    public void tesResolveBoolean() {
        doResolveTypeTest(BOOLEAN_PARAM, Boolean.class, Boolean.valueOf(BOOLEAN_VALUE));
    }
    
    
    public void testResolveInt() {        
        doResolveTypeTest(INT_PARAM, Integer.class, Integer.valueOf(INT_VALUE));
    }

    public void testResolveString() {        
        String ret = resolver.resolve(STRING_PARAM, String.class);        
        assertEquals("incorrect string value returned", STRING_VALUE, ret);
    }

    private <T> void doResolveTypeTest(String param, Class<T> type, T expectedValue) {
        T ret = resolver.resolve(param, type);        
        assertEquals("incorrect string value returned", expectedValue, ret);      
    }
    
}

package org.objectweb.celtix.common.util;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

public class Base64UtilityTest extends TestCase {

    public Base64UtilityTest(String arg0) {
        super(arg0);
    }

    void assertEquals(byte b1[], byte b2[]) {
        assertEquals(b1.length, b2.length);
        for (int x = 0; x < b1.length; x++) {
            assertEquals(b1[x], b2[x]);
        }
    }
    
    public void testEncodeDecodeChunk() throws Exception {
        byte bytes[] = new byte[100];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte)x;
        }
        
        char encodedChars[] = Base64Utility.encodeChunk(bytes, 0, -2);
        assertNull(encodedChars);
        encodedChars = Base64Utility.encodeChunk(bytes, 0, bytes.length);
        assertNotNull(encodedChars);
        byte bytesDecoded[] = Base64Utility.decodeChunk(encodedChars, 0, encodedChars.length);
        assertEquals(bytes, bytesDecoded);

        //require padding
        bytes = new byte[99];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte)x;
        }
        encodedChars = Base64Utility.encodeChunk(bytes, 0, bytes.length);
        assertNotNull(encodedChars);
        bytesDecoded = Base64Utility.decodeChunk(encodedChars, 0, encodedChars.length);
        assertEquals(bytes, bytesDecoded);
        
        //require padding
        bytes = new byte[98];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte)x;
        }
        encodedChars = Base64Utility.encodeChunk(bytes, 0, bytes.length);
        assertNotNull(encodedChars);
        bytesDecoded = Base64Utility.decodeChunk(encodedChars, 0, encodedChars.length);
        assertEquals(bytes, bytesDecoded);
        
        //require padding
        bytes = new byte[97];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte)x;
        }
        encodedChars = Base64Utility.encodeChunk(bytes, 0, bytes.length);
        assertNotNull(encodedChars);
        bytesDecoded = Base64Utility.decodeChunk(encodedChars, 0, encodedChars.length);
        assertEquals(bytes, bytesDecoded);
        
        
        bytesDecoded = Base64Utility.decodeChunk(new char[3], 0, 3);
        assertNull(bytesDecoded);
        bytesDecoded = Base64Utility.decodeChunk(new char[9], 0, 9);
        assertNull(bytesDecoded);
    }

    public void testEncodeDecodeString() throws Exception {
        String in = "QWxhZGRpbjpvcGVuIHNlc2FtZQ==";
        byte bytes[] = Base64Utility.decode(in);
        assertEquals("Aladdin:open sesame", new String(bytes));
        String encoded = Base64Utility.encode(bytes);
        assertEquals(in, encoded);
    }

    public void testEncodeDecodeStreams() throws Exception {
        byte bytes[] = new byte[100];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte)x;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
        Base64Utility.encodeChunk(bytes, 0, bytes.length, bout);
        String encodedString = new String(bout.toByteArray());
        Base64Utility.decode(encodedString.toCharArray(),
                             0,
                             encodedString.length(),
                             bout2);
        assertEquals(bytes, bout2.toByteArray());
        
        
        String in = "QWxhZGRpbjpvcGVuIHNlc2FtZQ==";
        bout.reset();
        bout2.reset();
        Base64Utility.decode(in, bout);
        bytes = bout.toByteArray();
        assertEquals("Aladdin:open sesame", new String(bytes));
        StringWriter writer = new StringWriter();
        Base64Utility.encode(bytes, 0, bytes.length, writer);
        assertEquals(in, writer.toString());
        
    }


}

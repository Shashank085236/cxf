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
package org.apache.cxf.aegis.type.basic;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.aegis.type.mtom.ByteArrayType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;

/**
 * Converts back and forth to byte[] objects.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class Base64Type extends Type {
    private static ByteArrayType optimizedType = new ByteArrayType();

    public Base64Type() {
        super();
    }

    @Override
    public Object readObject(MessageReader mreader, Context context) throws DatabindingException {
        boolean mtomEnabled = context.isMtomEnabled();
        XMLStreamReader reader = mreader.getXMLStreamReader();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            reader.next();
            while (!reader.isCharacters() && !reader.isEndElement() && !reader.isStartElement()) {
                reader.next();
            }

            if (reader.isStartElement() && reader.getName().equals(AbstractXOPType.XOP_INCLUDE)) {
                if (mtomEnabled) {
                    return optimizedType.readObject(mreader, context);
                } else {
                    throw new DatabindingException("Unexpected element: " + reader.getName());
                }
            }

            if (reader.isEndElement()) {
                reader.next();
                return new byte[0];
            }

            int length = reader.getTextLength();

            char[] myBuffer = new char[length];
            for (int sourceStart = 0;; sourceStart += length) {
                int nCopied = reader.getTextCharacters(sourceStart, myBuffer, 0, length);

                if (nCopied > 0) {
                    Base64Utility.decode(myBuffer, 0, nCopied, bos);
                }

                if (nCopied < length) {
                    break;
                }
            }

            while (reader.getEventType() != XMLStreamConstants.END_ELEMENT) {
                reader.next();
            }

            // Advance just past the end element
            reader.next();

            return bos.toByteArray();
        } catch (Base64Exception e) {
            throw new DatabindingException("Could not parse base64Binary data.", e);
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not parse base64Binary data.", e);
        }
    }

    @Override
    public void writeObject(Object object,
                            MessageWriter writer,
                            Context context) throws DatabindingException {
        boolean mtomEnabled = context.isMtomEnabled();
        if (mtomEnabled) {
            optimizedType.writeObject(object, writer, context);
            return;
        }

        byte[] data = (byte[])object;

        if (data != null && data.length > 0) {
            writer.writeValue(Base64Utility.encode(data));
        }
    }
}

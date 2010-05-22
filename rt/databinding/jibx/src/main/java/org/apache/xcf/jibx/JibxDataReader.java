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

package org.apache.xcf.jibx;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;

import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

public class JibxDataReader implements DataReader<XMLStreamReader> {

    private static IBindingFactory factory;

    static {
        factory = new JibxNullBindingFactory();
    }

    public Object read(XMLStreamReader input) {
        throw new UnsupportedOperationException();
    }

    public Object read(MessagePartInfo part, XMLStreamReader input) {
        Class<?> type = part.getTypeClass();
        try {
            UnmarshallingContext ctx = getUnmarshallingContext(input, type);
            if (JibxUtil.isSimpleValue(type)) {
                QName stype = part.getTypeQName();
                QName ctype = part.getConcreteName();
                if (ctx.isAt(ctype.getNamespaceURI(), ctype.getLocalPart())) {
                    String text = ctx.parseElementText(ctype.getNamespaceURI(), ctype.getLocalPart());
                    return JibxUtil.toObject(text, stype);
                } else {
                    throw new RuntimeException("Missing required element [" + ctype + "]");
                }
            }

            throw new RuntimeException("Not Implemented Yet");

        } catch (JiBXException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Object read(QName elementQName, XMLStreamReader input, Class type) {
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String prop, Object value) {
    }

    public void setSchema(Schema s) {
    }

    @SuppressWarnings("unchecked")
    private static UnmarshallingContext getUnmarshallingContext(XMLStreamReader reader, Class type)
        throws JiBXException {
        // Following is a which we need to fix
        if (true) {
            try {
                reader = StaxUtils.createXMLStreamReader(StaxUtils.read(reader));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        UnmarshallingContext ctx = (UnmarshallingContext)factory.createUnmarshallingContext();
        StAXReaderWrapper wrapper = new StAXReaderWrapper(reader, "SOAP-message", true);
        ctx.setDocument(wrapper);
        ctx.toTag();
        return ctx;
    }
}

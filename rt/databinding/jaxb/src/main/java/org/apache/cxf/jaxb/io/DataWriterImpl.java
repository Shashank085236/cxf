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

package org.apache.cxf.jaxb.io;

import javax.xml.bind.JAXBContext;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class DataWriterImpl<T> extends JAXBDataBase implements DataWriter<T> {
    public DataWriterImpl(JAXBContext ctx) {
        super(ctx);
    }
    
    public void write(Object obj, T output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, MessagePartInfo part, T output) {
        if (obj != null
            || !(part.getXmlSchema() instanceof XmlSchemaElement)) {
            JAXBEncoderDecoder.marshall(getJAXBContext(), getSchema(), obj, part, output, 
                                        getAttachmentMarshaller());
        } else if (obj == null && needToRender(obj, part)) {
            JAXBEncoderDecoder.marshallNullElement(getJAXBContext(), getSchema(), output, part);
        } else if (obj == null && needToRender(obj, part)) {
            JAXBEncoderDecoder.marshallNullElement(getJAXBContext(), getSchema(), output, part);
        }
    }

    private boolean needToRender(Object obj, MessagePartInfo part) {
        if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            return element.isNillable() && element.getMinOccurs() > 0;
        }
        return false;
    }
}

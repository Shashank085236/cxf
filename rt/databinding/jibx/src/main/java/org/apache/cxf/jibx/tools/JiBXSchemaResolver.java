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

package org.apache.cxf.jibx.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jibx.schema.ISchemaResolver;
import org.jibx.schema.MemoryResolver;

import org.w3c.dom.Element;

/**
 * A Wrapper class that acts as a wrapper when passing schema instances to JiBX code generation framework. An
 * instance of this holds a schema instance as a {@line Element} type instance.
 */
public class JiBXSchemaResolver extends MemoryResolver implements ISchemaResolver {
    Element schemaElement;

    public JiBXSchemaResolver(String id) {
        super(id);
    }

    public JiBXSchemaResolver(String id, Element schemaElement) {
        super(id);
        this.schemaElement = schemaElement;
    }

    public InputStream getContent() throws IOException {
        return getAsStream(schemaElement);
    }

    /**
     * Converts a {@link Element} type into a @link {@link InputStream} using a {@link Transformer} instance.
     * 
     * @param element the element to convert into a {@link InputStream}
     * @return stream an InputStream that the element is converted 
     */
    private static InputStream getAsStream(Element element) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(element);
            Result streamResult = new StreamResult(baos);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(xmlSource, streamResult);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

}
